package jeffrey.testapp.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.metrics.PoolStats;
import jeffrey.testapp.server.metrics.JfrHikariDataSource;
import jeffrey.testapp.server.metrics.JfrPoolMetricsTracker;
import jeffrey.testapp.server.metrics.JfrPoolStatisticsPeriodicRecorder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "database.in-memory", havingValue = "false", matchIfMissing = true)
public class DatabaseConfiguration {

    @Bean(destroyMethod = "close")
    public JfrHikariDataSource dataSource(
            @Value("${database.host:localhost}") String host,
            @Value("${database.port:26257}") int port,
            @Value("${database.username:root}") String username,
            @Value("${database.password:}") String password,
            @Value("${database.name:postgres}") String name) {


        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(200);
        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + name);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMetricsTrackerFactory((String poolName, PoolStats poolStats) -> {
            JfrPoolStatisticsPeriodicRecorder.addPool(poolName, poolStats);
            return new JfrPoolMetricsTracker(poolName);
        });

        JfrPoolStatisticsPeriodicRecorder.registerToFlightRecorder();
        return new JfrHikariDataSource(config);
    }
}
