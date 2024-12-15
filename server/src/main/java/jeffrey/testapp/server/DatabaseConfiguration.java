package jeffrey.testapp.server;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(value = "database.in-memory", havingValue = "false", matchIfMissing = true)
public class DatabaseConfiguration {

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
}
