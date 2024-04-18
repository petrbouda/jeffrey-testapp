package jeffrey.testapp.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

public class PersonClient {

    private static final ParameterizedTypeReference<List<Person>> PERSON_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient client;

    public PersonClient(RestClient client) {
        this.client = client;
    }

    public Person getPerson() {
        return client.get()
                .retrieve()
                .body(Person.class);
    }

    public List<Person> getNPerson(long count) {
        return client.get()
                .uri("/{count}", count)
                .retrieve()
                .body(PERSON_LIST_TYPE);
    }

    public Person addPerson(Person person) {
        return client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(person)
                .retrieve()
                .body(Person.class);
    }

    public Long getPersonCount() {
        return client.get()
                .uri("/count")
                .retrieve()
                .body(Long.class);
    }

    public void removePerson(Long id) {
        client.delete()
                .uri("/{id}", id)
                .retrieve();
    }

    public void reloadSnapshot() {
        client.post()
                .uri("/reload")
                .retrieve();
    }
}
