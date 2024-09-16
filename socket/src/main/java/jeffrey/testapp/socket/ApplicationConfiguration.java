package jeffrey.testapp.socket;

import jeffrey.testapp.server.service.RecordingService;
import jeffrey.testapp.socket.service.ResourcesRecordingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public RecordingService personService(@Value("${recordings.path:/tmp/recordings}") String path) {
        Path recordingsPath;
        if (path.startsWith("~/")) {
            recordingsPath = Path.of(System.getProperty("user.home"), path.substring(2));
        } else {
            recordingsPath = Path.of(path);
        }

        return new ResourcesRecordingService(recordingsPath);
    }
}
