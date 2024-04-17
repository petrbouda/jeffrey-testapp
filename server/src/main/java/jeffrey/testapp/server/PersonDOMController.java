package jeffrey.testapp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jeffrey.testapp.server.service.PersonService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contains inefficiency, it's serialize/deserialize entities via JSON DOM.
 * That means that, there is a pointless intermediate representation that
 * will be visible in Diff Graphs.
 */
@RequestMapping(path = "/persons/dom", produces = "application/json")
public class PersonDOMController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PersonService service;
    private final PersonRepository repository;

    public PersonDOMController(PersonService service, PersonRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping
    public JsonNode getRandomPerson() {
        Person person = service.getRandomPerson();
        return MAPPER.valueToTree(person);
    }

    @GetMapping("/{count}")
    public ArrayNode getNPersons(@PathVariable("count") int count) {
        List<Person> persons = service.getNPersons(count);
        return MAPPER.valueToTree(persons);
    }

    @PostMapping
    public ObjectNode addNewPerson(Person person) {
        Person inserted = repository.insert(person);
        return MAPPER.valueToTree(inserted);
    }

    @GetMapping("/count")
    public JsonNode personCount() {
        int count = repository.count();
        return MAPPER.valueToTree(count);
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
