package jeffrey.testapp.socket.service.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JDKFileLoader implements FileLoader {

    @Override
    public byte[] loadFile(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
