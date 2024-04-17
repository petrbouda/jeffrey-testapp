package jeffrey.testapp.server.service;

import jeffrey.testapp.server.Person;

import java.util.List;

public interface PersonService {

    Person getRandomPerson();

    List<Person> getNPersons(int count);
}
