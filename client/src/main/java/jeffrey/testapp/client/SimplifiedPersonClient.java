package jeffrey.testapp.client;

import org.springframework.web.client.RestClient;

import java.util.List;

public class SimplifiedPersonClient {

    private static final int MAX_COUNT = 50;

    private final PersonClient client;

    public SimplifiedPersonClient(RestClient client) {
        this.client = new PersonClient(client);
    }

    public Person getPerson() {
        return client.getPerson();
    }

    public List<Person> getNPerson() {
        return client.getNPerson(Helpers.generateId(MAX_COUNT));
    }

    public Person addPerson() {
        return client.addPerson(PersonFactory.create());
    }

    public Long getPersonCount() {
        return client.getPersonCount();
    }

    public void removePerson() {
        Long count = client.getPersonCount();
        client.removePerson(Helpers.generateId(count));
    }
}
