package jeffrey.testapp.socket.service.loader;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class BufferedFileLoader implements FileLoader {

    @Override
    public byte[] loadFile(Path path) {
        try(var bis = new BufferedInputStream(new FileInputStream(path.toFile()))) {
            return bis.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
