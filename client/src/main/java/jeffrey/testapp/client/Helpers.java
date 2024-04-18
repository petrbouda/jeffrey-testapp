package jeffrey.testapp.client;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public abstract class Helpers {

    public static Collection<Long> generateIds(long max, int count) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return Stream.generate(() -> random.nextLong(max))
                .limit(count)
                .toList();
    }

    public static Long generateId(long max) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return random.nextLong(max);
    }
}
