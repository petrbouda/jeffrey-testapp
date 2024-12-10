package jeffrey.testapp.client;

import java.util.concurrent.ThreadLocalRandom;

public abstract class Helpers {

    public static Long generateId(long max) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return random.nextLong(max);
    }
}
