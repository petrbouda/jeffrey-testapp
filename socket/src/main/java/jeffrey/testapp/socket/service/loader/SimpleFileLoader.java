package jeffrey.testapp.socket.service.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimpleFileLoader implements FileLoader {

    @Override
    public byte[] loadFile(Path path) {
        try (var stream = Files.newInputStream(path)){
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
