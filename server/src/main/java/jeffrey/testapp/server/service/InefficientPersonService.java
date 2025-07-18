package jeffrey.testapp.server.service;

import jeffrey.testapp.server.Helpers;
import jeffrey.testapp.server.IDHolder;
import jeffrey.testapp.server.Person;
import jeffrey.testapp.server.PersonRepository;
import jeffrey.testapp.server.leak.NativeMemoryAllocator;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class InefficientPersonService implements PersonService {
    private final PersonRepository repository;
    private final NativeMemoryAllocator nativeMemoryAllocator;

    public InefficientPersonService(PersonRepository repository, NativeMemoryAllocator nativeMemoryAllocator) {
        this.repository = repository;
        this.nativeMemoryAllocator = nativeMemoryAllocator;
    }

    @Override
    public Optional<Person> getRandomPerson() {
        try (var __ = nativeMemoryAllocator.allocate()) {
            int latestPersonCount = repository.count().intValue();
            int personIndex = Helpers.generateId(latestPersonCount);
            long personId = safeIdLookup(personIndex);
            return repository.findById(personId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Person> getNPersons(int count) {
        try (var __ = nativeMemoryAllocator.allocate()) {
            int latestPersonCount = repository.count().intValue();
            Collection<Integer> indices = Helpers.generateIds(latestPersonCount, count);
            List<Long> personIds = indices.stream()
                    .map(InefficientPersonService::safeIdLookup)
                    .toList();

            return repository.findByIds(personIds);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static long safeIdLookup(int index) {
        try {
            return IDHolder.IDS.get(index);
        } catch (IndexOutOfBoundsException ex) {
            return IDHolder.IDS.getLast();
        }
    }
}
