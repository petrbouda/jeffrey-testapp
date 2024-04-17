package jeffrey.testapp.server.service;

import jeffrey.testapp.server.Helpers;
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
        Integer latestPersonCount = repository.count();
        Long personId = Helpers.generateId(latestPersonCount);
        return repository.findById(personId);
    }

    @Override
    public List<Person> getNPersons(int count) {
        Integer latestPersonCount = repository.count();
        Collection<Long> ids = Helpers.generateIds(latestPersonCount, count);
        return repository.findByIds(ids);
    }
}
