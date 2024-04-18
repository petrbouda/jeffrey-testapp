package jeffrey.testapp.server.service;

import jeffrey.testapp.server.Helpers;
import jeffrey.testapp.server.IDHolder;
import jeffrey.testapp.server.Person;
import jeffrey.testapp.server.PersonRepository;

import java.util.Collection;
import java.util.List;

public class InefficientPersonService implements PersonService {

    private final PersonRepository repository;

    public InefficientPersonService(PersonRepository repository) {
        this.repository = repository;
    }

    @Override
    public Person getRandomPerson() {
        int latestPersonCount = repository.count();
        int personIndex = Helpers.generateId(latestPersonCount);
        long personId = safeIdLookup(personIndex);
        return repository.findById(personId)
                .orElseGet(this::getRandomPerson);
    }

    @Override
    public List<Person> getNPersons(int count) {
        int latestPersonCount = repository.count();
        Collection<Integer> indices = Helpers.generateIds(latestPersonCount, count);
        List<Long> personIds = indices.stream()
                .map(InefficientPersonService::safeIdLookup)
                .toList();

        return repository.findByIds(personIds);
    }

    private static long safeIdLookup(int index) {
        try {
            return IDHolder.IDS.get(index);
        } catch (IndexOutOfBoundsException ex) {
            return IDHolder.IDS.getLast();
        }
    }
}
