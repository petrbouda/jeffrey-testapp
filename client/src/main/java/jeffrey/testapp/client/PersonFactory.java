package jeffrey.testapp.client;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;

public class PersonFactory {

    private static final Lorem LOREM = LoremIpsum.getInstance();

    public static Person createPerson() {
        return new Person(
                LOREM.getFirstName(),
                LOREM.getLastName(),
                LOREM.getCity(),
                LOREM.getCountry(),
                LOREM.getPhone(),
                LOREM.getParagraphs(1, 5)
        );
    }
}
