package jeffrey.testapp.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Helps to autoconfigure Tomcat WebServer
@RestController
public class FakeController {
    @GetMapping
    public String fake() {
        return "fake";
    }
}
