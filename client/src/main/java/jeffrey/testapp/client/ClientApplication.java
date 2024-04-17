package jeffrey.testapp.client;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

@SpringBootApplication
public class ClientApplication implements ApplicationRunner {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ClientApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        HttpClient httpClient = HttpClient.create()
                .protocol(HttpProtocol.H2C, HttpProtocol.H2, HttpProtocol.HTTP11);
        //  .wiretap("reactor.client.ProductWebClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);

        WebClient client = WebClient.builder()
                .baseUrl("http://localhost:8081/")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        Person person = client.get()
                .uri("/persons")
                .retrieve()
                .bodyToMono(Person.class)
                .block();

        System.out.println(person);
    }
}
