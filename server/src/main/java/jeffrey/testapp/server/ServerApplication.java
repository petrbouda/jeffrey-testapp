package jeffrey.testapp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication(exclude = JdbcTemplateAutoConfiguration.class)
public class ServerApplication implements ApplicationListener<ApplicationStartedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ServerApplication.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        new SpringApplicationBuilder(ServerApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .initializers(new CockroachInitializer())
                .run(args);
    }

    /**
     * Not so efficient to have some kind of processing at the beginning of the application.
     * We will have the second implementation - more efficient to see differences in Startup performance.
     */
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        PersonRepository repository = event.getApplicationContext()
                .getBeanFactory()
                .getBean(PersonRepository.class);

        try (InputStream is = ServerApplication.class.getClassLoader().getResourceAsStream("data.json")) {
            Objects.requireNonNull(is, "Cannot find `data.json` file");

            String content = new String(is.readAllBytes());
            Person[] persons = MAPPER.readValue(content, Person[].class);

            Arrays.stream(persons)
                    .map(ServerApplication::toSQLInsert)
                    .forEach(repository::insertRaw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("backup"));
        executor.scheduleAtFixedRate(() -> {
            LOG.info("Backup and Reloading Initiated");
            repository.backupAndReload();
            LOG.info("Backup and Reloading Finished");
        }, 5, 5, TimeUnit.MINUTES);
    }

    private static final String INSERT_TEMPLATE = """
            INSERT INTO person (firstname, lastname, city, country, phone, political_opinion)
            VALUES ('%s', '%s', '%s', '%s', '%s', '%s');
            """;

    private static String toSQLInsert(Person person) {
        return INSERT_TEMPLATE.formatted(
                person.firstname(),
                person.lastname(),
                person.city(),
                person.country(),
                person.phone(),
                person.politicalOpinion()
        );
    }
}
