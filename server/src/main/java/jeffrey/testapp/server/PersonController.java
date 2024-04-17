package jeffrey.testapp.server;

import jeffrey.testapp.server.service.PersonService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/persons", produces = "application/json")
public class PersonController {

    private final PersonService service;
    private final PersonRepository repository;

    public PersonController(PersonService service, PersonRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping
    public Person getRandomPerson() {
        return service.getRandomPerson();
    }

    @GetMapping("/{count}")
    public List<Person> getNPersons(@PathVariable("count") int count) {
        return service.getNPersons(count);
    }

    @PostMapping
    public Person addNewPerson(Person person) {
        return repository.insert(person);
    }

    @GetMapping("/count")
    public int personCount() {
        return repository.count();
    }

    @DeleteMapping("/{id}")
    public void removePerson(@PathVariable("id") Long id) {
        repository.remove(id);
    }

    @PostMapping("/reload")
    public void resetAndLoadSnapshot() {
        throw new UnsupportedOperationException();
    }
}
