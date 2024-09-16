package jeffrey.testapp.server.service;

import java.nio.file.Path;
import java.util.List;

public interface RecordingService {

    List<Path> listRecordings();

    byte[] readRecording(int recordingId);
}
