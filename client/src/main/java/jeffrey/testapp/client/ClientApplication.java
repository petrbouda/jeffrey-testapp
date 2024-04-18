package jeffrey.testapp.client;

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

@SpringBootApplication
public class ClientApplication implements ApplicationRunner {

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


    }
}
