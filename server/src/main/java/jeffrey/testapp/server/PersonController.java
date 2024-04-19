package jeffrey.testapp.server;

import jeffrey.testapp.server.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "/persons", produces = "application/json")
public class PersonController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonController.class);

    private final PersonService service;
    private final PersonRepository repository;

    public PersonController(PersonService service, PersonRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping
    public Optional<Person> getRandomPerson() {
        Optional<Person> personOpt = service.getRandomPerson();
        if (personOpt.isPresent()) {
            Person person = personOpt.get();
            LOG.info("Get Person: id={} firstname={} lastname={}",
                    person.id(), person.firstname(), person.lastname());
        }
        return personOpt;
    }

    @GetMapping("/{count}")
    public List<Person> getNPersons(@PathVariable("count") int count) {
        List<Person> persons = service.getNPersons(count);
        List<Long> ids = persons.stream()
                .map(Person::id)
                .toList();

        LOG.info("Get NPersons: IDs={} count={}", ids, count);
        return persons;
    }

    @PostMapping(consumes = "application/json")
    public Person addNewPerson(@RequestBody Person person) {
        Person addedPerson = repository.insert(person);
        LOG.info("Added Person: id={} firstname={} lastname={}",
                person.id(), person.firstname(), person.lastname());
        return addedPerson;
    }

    @GetMapping("/count")
    public int personCount() {
        int count = repository.count();
        LOG.info("Person Count: {}", count);
        return count;
    }

    @DeleteMapping("/{id}")
    public void removePerson(@PathVariable("id") Long id) {
        repository.remove(id);
        LOG.info("Removed Person: {}", id);
    }

    @PostMapping("/reload")
    public void resetAndLoadSnapshot() {
        throw new UnsupportedOperationException();
    }
}
