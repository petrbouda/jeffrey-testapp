package jeffrey.testapp.client;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class ClientApplication implements ApplicationListener<ApplicationStartedEvent> {

    private static final ScheduledExecutorService EXECUTOR =
            Executors.newScheduledThreadPool(Integer.MAX_VALUE, Thread.ofVirtual().factory());

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
                .build();

        var personClient = new SimplifiedPersonClient(client);

        Long getPerson = environment.getProperty("load.get-person", Long.class, 20L);
        EXECUTOR.scheduleAtFixedRate(guard(personClient::getPerson), 0, getPerson, TimeUnit.MILLISECONDS);

        Long getNPerson = environment.getProperty("load.get-n-person", Long.class, 100L);
        EXECUTOR.scheduleAtFixedRate(guard(personClient::getNPerson), 0, getNPerson, TimeUnit.MILLISECONDS);

        Long addPerson = environment.getProperty("load.add-person", Long.class, 100L);
        EXECUTOR.scheduleAtFixedRate(guard(personClient::addPerson), 0, addPerson, TimeUnit.MILLISECONDS);

        Long personCount = environment.getProperty("load.get-person-count", Long.class, 10L);
        EXECUTOR.scheduleAtFixedRate(guard(personClient::getPersonCount), 0, personCount, TimeUnit.MILLISECONDS);

        Long removePerson = environment.getProperty("load.remove-person", Long.class, 125L);
        EXECUTOR.scheduleAtFixedRate(guard(personClient::removePerson), 0, removePerson, TimeUnit.MILLISECONDS);
    }


    private static Runnable guard(Runnable delegate) {
        return () -> {
            try {
                delegate.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
}
