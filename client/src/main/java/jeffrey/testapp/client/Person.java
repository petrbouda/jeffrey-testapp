package jeffrey.testapp.client;

public record Person(
        Long id,
        String firstname,
        String lastname,
        String city,
        String country,
        String phone,
        String politicalOpinion) {

    public Person(
            String firstname,
            String lastname,
            String city,
            String country,
            String phone,
            String politicalOpinion) {

        this(null, firstname, lastname, city, country, phone, politicalOpinion);
    }

    public Person copyWithId(Long id) {
        return new Person(id, firstname, lastname, city, country, phone, politicalOpinion);
    }
}
