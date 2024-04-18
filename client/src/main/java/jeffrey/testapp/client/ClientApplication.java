package jeffrey.testapp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class ClientApplication implements ApplicationRunner {

    private static final ScheduledExecutorService EXECUTOR =
            Executors.newScheduledThreadPool(Integer.MAX_VALUE, Thread.ofVirtual().factory());

    public static void main(String[] args) {
        new SpringApplicationBuilder(ClientApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .version(Version.HTTP_2)
                .build();

        RestClient client = RestClient.builder()
                .baseUrl("http://localhost:8081/persons")
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();

        var personClient = new SimplifiedPersonClient(client);
        EXECUTOR.scheduleAtFixedRate(guard(personClient::getPerson), 0, 10, TimeUnit.MILLISECONDS);
        EXECUTOR.scheduleAtFixedRate(guard(personClient::getNPerson), 0, 50, TimeUnit.MILLISECONDS);
        EXECUTOR.scheduleAtFixedRate(guard(personClient::addPerson), 0, 20, TimeUnit.MILLISECONDS);
        EXECUTOR.scheduleAtFixedRate(guard(personClient::getPersonCount), 0, 5, TimeUnit.MILLISECONDS);
        EXECUTOR.scheduleAtFixedRate(guard(personClient::removePerson), 0, 100, TimeUnit.MILLISECONDS);

        Thread.currentThread().join();
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
