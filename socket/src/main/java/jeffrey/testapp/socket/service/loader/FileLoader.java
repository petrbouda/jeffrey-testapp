package jeffrey.testapp.socket.service.loader;

import java.nio.file.Path;

public interface FileLoader {

    byte[] loadFile(Path path);
}
