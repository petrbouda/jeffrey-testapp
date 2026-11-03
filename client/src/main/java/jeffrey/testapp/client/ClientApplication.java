package jeffrey.testapp.client;

import cafe.jeffrey.jfr.events.spring.JfrClientHttpRequestInterceptor;
import cafe.jeffrey.jfr.events.trace.Tracer;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@SpringBootApplication
public class ClientApplication implements ApplicationListener<ApplicationStartedEvent> {

    private static final ScheduledExecutorService EXECUTOR =
            Executors.newScheduledThreadPool(Integer.MAX_VALUE, Thread.ofVirtual().factory());

    /**
     * A numeric path segment ({@code /persons/42}) is an id or a count, never part of the endpoint.
     */
    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("/\\d+(?=/|$)");
    private static final String NUMERIC_SEGMENT_TEMPLATE = "/{n}";

    // Every scheduled load task is the root span of its own trace, with the HTTP calls nested in it
    private static final String SPAN_GET_PERSON = "load.get-person";
    private static final String SPAN_GET_N_PERSONS = "load.get-n-persons";
    private static final String SPAN_ADD_PERSON = "load.add-person";
    private static final String SPAN_GET_PERSON_COUNT = "load.get-person-count";
    private static final String SPAN_REMOVE_PERSON = "load.remove-person";

    /**
     * Records every outbound call as a {@code jeffrey.HttpClientExchange} leaf span. The URI is
     * named by host and path with numeric segments collapsed, so the HTTP Client dashboard shows one
     * endpoint per route rather than one per person id.
     */
    private static final JfrClientHttpRequestInterceptor HTTP_CLIENT_TRACING =
            new JfrClientHttpRequestInterceptor(ClientApplication::endpointOf);

    public static void main(String[] args) {
        new SpringApplicationBuilder(ClientApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .run(args);
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();

        HttpClient httpClient = HttpClient.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();

        ConfigurableEnvironment environment = context.getEnvironment();
        String baseUrls = environment.getProperty("base-urls", String.class, "http://localhost:8080");

        String[] urls = baseUrls.split(",");

        for (String url : urls) {
            initiatePersonInvocations(environment, httpClient, url);
        }

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initiatePersonInvocations(
            ConfigurableEnvironment environment, HttpClient httpClient, String baseUrl) {

        RestClient client = RestClient.builder()
                .baseUrl(baseUrl + "/persons")
                .requestFactory(new JdkClientHttpRequestFactory(httpClient, Executors.newVirtualThreadPerTaskExecutor()))
                .requestInterceptor(HTTP_CLIENT_TRACING)
                .build();

        var personClient = new SimplifiedPersonClient(client);

        Long getPerson = environment.getProperty("load.get-person", Long.class, 20L);
        EXECUTOR.scheduleAtFixedRate(traced(SPAN_GET_PERSON, personClient::getPerson), 0, getPerson, TimeUnit.MILLISECONDS);

        Long getNPerson = environment.getProperty("load.get-n-person", Long.class, 100L);
        EXECUTOR.scheduleAtFixedRate(traced(SPAN_GET_N_PERSONS, personClient::getNPerson), 0, getNPerson, TimeUnit.MILLISECONDS);

        Long addPerson = environment.getProperty("load.add-person", Long.class, 100L);
        EXECUTOR.scheduleAtFixedRate(traced(SPAN_ADD_PERSON, personClient::addPerson), 0, addPerson, TimeUnit.MILLISECONDS);

        Long personCount = environment.getProperty("load.get-person-count", Long.class, 10L);
        EXECUTOR.scheduleAtFixedRate(traced(SPAN_GET_PERSON_COUNT, personClient::getPersonCount), 0, personCount, TimeUnit.MILLISECONDS);

        Long removePerson = environment.getProperty("load.remove-person", Long.class, 125L);
        EXECUTOR.scheduleAtFixedRate(traced(SPAN_REMOVE_PERSON, personClient::removePerson), 0, removePerson, TimeUnit.MILLISECONDS);
    }


    /**
     * Runs the task as a root span; a failure marks the span ERROR before it is logged and swallowed
     * here, so one failed call does not cancel the schedule.
     */
    private static Runnable traced(String spanName, Runnable task) {
        return () -> {
            try {
                Tracer.run(spanName, task);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    private static String endpointOf(URI uri) {
        String host = uri.getHost() == null ? "" : uri.getHost();
        String path = uri.getPath() == null ? "" : uri.getPath();
        return host + NUMERIC_SEGMENT.matcher(path).replaceAll(NUMERIC_SEGMENT_TEMPLATE);
    }
}
