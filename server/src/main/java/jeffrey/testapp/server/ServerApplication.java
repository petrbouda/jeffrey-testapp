package jeffrey.testapp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication(exclude = JdbcTemplateAutoConfiguration.class)
public class ServerApplication implements ApplicationListener<ApplicationStartedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ServerApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        PersonRepository repository = event.getApplicationContext()
                .getBeanFactory()
                .getBean(PersonRepository.class);

        IDHolder.IDS.addAll(repository.findAllIds());
        LOG.info("Loaded {} person IDs from database", IDHolder.IDS.size());

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("backup"));
        executor.scheduleAtFixedRate(() -> {
            LOG.info("Backup and Reloading Initiated");
            repository.backupAndReload();
            LOG.info("Backup and Reloading Finished");
        }, 5, 5, TimeUnit.MINUTES);
    }
}
