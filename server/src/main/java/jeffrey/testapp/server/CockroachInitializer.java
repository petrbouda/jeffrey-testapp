package jeffrey.testapp.server;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.testcontainers.containers.CockroachContainer;

import java.util.Map;

public class CockroachInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Boolean isDatabaseInMemory = applicationContext.getEnvironment()
                .getProperty("database.in-memory", Boolean.class, false);

        if (!isDatabaseInMemory) {
            var container = new CockroachContainer("cockroachdb/cockroach")
                    .withEnv("DOCKER_HOST", "unix:///run/user/1000/podman/podman.sock")
                    .withInitScript("data.sql");

            container.start();

            System.out.printf("CockroachDB started! DB_PORT: %s, HTTP_PORT %s\n",
                    container.getMappedPort(26257),
                    container.getMappedPort(8080));

            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            MutablePropertySources propertySources = environment.getPropertySources();
            Map<String, Object> database = Map.of(
                    "database.host", container.getHost(),
                    "database.port", container.getMappedPort(26257));

            propertySources.addFirst(new MapPropertySource("cockroach-map", database));
        }
    }
}
