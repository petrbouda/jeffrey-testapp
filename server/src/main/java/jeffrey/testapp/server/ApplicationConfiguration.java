package jeffrey.testapp.server;

import com.zaxxer.hikari.HikariDataSource;
import jeffrey.testapp.server.service.EfficientPersonService;
import jeffrey.testapp.server.service.InefficientPersonService;
import jeffrey.testapp.server.service.PersonService;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.Queue;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public DataSource dataSource(
            @Value("${database.host:localhost}") String host,
            @Value("${database.port:26257}") int port,
            @Value("${database.username:root}") String username,
            @Value("${database.password:}") String password,
            @Value("${database.name:postgres}") String name) {

        HikariDataSource ds = new HikariDataSource();
        ds.setMaximumPoolSize(200);
        ds.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + name);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("org.postgresql.Driver");
        return ds;
    }

    @Bean
    public NamedParameterJdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public PersonService personService(
            @Value("${serviceMode:inefficient}") String serviceMode,
            PersonRepository personRepository) {

        return switch (serviceMode) {
            case "efficient" -> new EfficientPersonService(personRepository);
            case "inefficient" -> new InefficientPersonService(personRepository);
            default -> throw new RuntimeException("Unknown service mode: " + serviceMode);
        };
    }

    @Bean
    public PersonRepository personRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new PersonRepository(jdbcTemplate);
    }

    @Bean
    public Queue<Person> personQueue() {
        Integer cacheSize = Integer.getInteger("cacheSize");
        // It's not properly synchronized, but I don't care :)
        // I just need to keep some data in Old Generation
        return cacheSize == null
                ? new NoOpQueue<>()
                : new CircularFifoQueue<>(cacheSize);
    }
}
