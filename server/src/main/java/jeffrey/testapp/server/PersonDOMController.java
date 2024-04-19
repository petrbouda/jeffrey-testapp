package jeffrey.testapp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jeffrey.testapp.server.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Contains inefficiency, it's serialize/deserialize entities via JSON DOM.
 * That means that, there is a pointless intermediate representation that
 * will be visible in Diff Graphs.
 */
@RequestMapping(path = "/persons/dom", produces = "application/json")
public class PersonDOMController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonDOMController.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PersonService service;
    private final PersonRepository repository;

    public PersonDOMController(PersonService service, PersonRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping
    public Optional<JsonNode> getRandomPerson() {
        Optional<Person> personOpt = service.getRandomPerson();
        if (personOpt.isPresent()) {
            Person person = personOpt.get();
            LOG.info("Get Person: id={} firstname={} lastname={}",
                    person.id(), person.firstname(), person.lastname());
        }
        return personOpt.map(MAPPER::valueToTree);
    }

    @GetMapping("/{count}")
    public ArrayNode getNPersons(@PathVariable("count") int count) {
        List<Person> persons = service.getNPersons(count);
        List<Long> ids = persons.stream()
                .map(Person::id)
                .toList();

        LOG.info("Get NPersons: IDs={} count={}", ids, count);
        return MAPPER.valueToTree(persons);
    }

    @PostMapping
    public ObjectNode addNewPerson(Person person) {
        Person addedPerson = repository.insert(person);
        LOG.info("Added Person: {}", addedPerson);
        return MAPPER.valueToTree(addedPerson);
    }

    @GetMapping("/count")
    public JsonNode personCount() {
        int count = repository.count();
        return MAPPER.valueToTree(count);
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
