package jeffrey.testapp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import jeffrey.testapp.server.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "/persons", produces = "application/json")
public class PersonController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(PersonController.class);

    private final boolean efficientMode;
    private final PersonService service;
    private final PersonRepository repository;

    public PersonController(
            @Value("${efficient.mode:true}") boolean efficientMode,
            PersonService service,
            PersonRepository repository) {

        this.efficientMode = efficientMode;
        this.service = service;
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity getRandomPerson() {
        Optional<Person> personOpt = service.getRandomPerson();
        if (personOpt.isPresent()) {
            Person person = personOpt.get();
            LOG.info("Get Person: id={} firstname={} lastname={}",
                    person.id(), person.firstname(), person.lastname());
        }

        if (efficientMode) {
            return ResponseEntity.of(personOpt);
        } else {
            return ResponseEntity.of(personOpt.map(MAPPER::valueToTree));
        }
    }

    @GetMapping("/{count}")
    public ResponseEntity getNPersons(@PathVariable("count") int count) {
        List<Person> persons = service.getNPersons(count);
        List<Long> ids = persons.stream()
                .map(Person::id)
                .toList();
        LOG.info("Get NPersons: IDs={} count={}", ids, count);

        if (efficientMode) {
            return ResponseEntity.of(Optional.of(persons));
        } else {
            return ResponseEntity.of(Optional.of(MAPPER.valueToTree(persons)));
        }
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity addNewPerson(@RequestBody Person person) {
        Person addedPerson = repository.insert(person);
        LOG.info("Added Person: {}", addedPerson);

        if (efficientMode) {
            return ResponseEntity.of(Optional.of(addedPerson));
        } else {
            return ResponseEntity.of(Optional.of(MAPPER.valueToTree(addedPerson)));
        }
    }

    @GetMapping("/count")
    public ResponseEntity personCount() {
        int count = repository.count().intValue();
        LOG.info("Person Count: {}", count);

        if (efficientMode) {
            return ResponseEntity.of(Optional.of(count));
        } else {
            return ResponseEntity.of(Optional.of(MAPPER.valueToTree(count)));
        }
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
