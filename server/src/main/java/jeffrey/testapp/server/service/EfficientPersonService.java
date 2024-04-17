package jeffrey.testapp.server.service;

import jeffrey.testapp.server.Helpers;
import jeffrey.testapp.server.Person;
import jeffrey.testapp.server.PersonRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public class EfficientPersonService implements PersonService {

    private final PersonRepository repository;

    private static final Duration COUNT_REFRESH_DELAY = Duration.ofSeconds(15);

    private volatile Instant lastCountRefresh = Instant.MIN;
    private volatile long lastCountValue = -1;

    public EfficientPersonService(PersonRepository repository) {
        this.repository = repository;
    }

    @Override
    public Person getRandomPerson() {
        long latestPersonCount = getLastCountValue();
        Long personId = Helpers.generateId(latestPersonCount);
        return repository.findById(personId);
    }

    @Override
    public List<Person> getNPersons(int count) {
        long latestPersonCount = getLastCountValue();
        Collection<Long> ids = Helpers.generateIds(latestPersonCount, count);
        return repository.findByIds(ids);
    }

    private long getLastCountValue() {
        Instant now = Instant.now();
        long diffMillis = now.toEpochMilli() - lastCountRefresh.toEpochMilli();

        // don't care about atomic update
        if (COUNT_REFRESH_DELAY.minusMillis(diffMillis).isNegative()) {
            lastCountValue = repository.count();
            lastCountRefresh = now;
        }

        return lastCountValue;
    }
}
