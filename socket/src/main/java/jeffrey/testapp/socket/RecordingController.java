package jeffrey.testapp.socket;

import jeffrey.testapp.server.service.RecordingService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping(path = "/recordings", produces = "application/json")
public class RecordingController {

    private final RecordingService service;

    public RecordingController(RecordingService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Path>> getRecording() {
        return ResponseEntity.ok(service.listRecordings());
    }

    @GetMapping("/{recordingId}")
    public ResponseEntity<Resource> getRecording(@PathVariable("recordingId") int recordingId) {
        try {
            byte[] data = service.readRecording(recordingId);
            return ResponseEntity.ok(new ByteArrayResource(data));
        } catch (IllegalStateException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
