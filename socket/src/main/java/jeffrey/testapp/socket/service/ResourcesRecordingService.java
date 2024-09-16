package jeffrey.testapp.socket.service;

import jeffrey.testapp.server.service.RecordingService;
import jeffrey.testapp.socket.service.loader.BufferedFileLoader;
import jeffrey.testapp.socket.service.loader.FileLoader;
import jeffrey.testapp.socket.service.loader.JDKFileLoader;
import jeffrey.testapp.socket.service.loader.SimpleFileLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class ResourcesRecordingService implements RecordingService {

    private final AtomicLong counter = new AtomicLong(0);

    private final Path baseDirectory;

    private final FileLoader simpleFileLoader = new SimpleFileLoader();
    private final FileLoader jdkFileLoader = new JDKFileLoader();
    private final FileLoader bufferedFileLoader = new BufferedFileLoader();

    public ResourcesRecordingService(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    @Override
    public List<Path> listRecordings() {
        try (Stream<Path> stream = Files.list(baseDirectory)){
            return stream.map(Path::getFileName).toList();
        } catch (IOException e) {
            throw new RuntimeException("Cannot list recordings", e);
        }
    }

    @Override
    public byte[] readRecording(int recordingId) {
        Path path = listRecordings().get(recordingId);
        Path fullpath = baseDirectory.resolve(path);

        long i = counter.incrementAndGet();
        long type = i % 4;
        if (type == 0) {
            return simpleFileLoader.loadFile(fullpath);
        } else if (type == 1) {
            return jdkFileLoader.loadFile(fullpath);
        } else if (type == 2) {
            return bufferedFileLoader.loadFile(fullpath);
        } else {
            throw new IllegalStateException("Unknown strategy");
        }
    }
}
