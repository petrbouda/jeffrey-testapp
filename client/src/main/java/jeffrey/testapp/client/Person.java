package jeffrey.testapp.client;

public record Person(
        Long id,
        String firstname,
        String lastname,
        String city,
        String country,
        String phone,
        String politicalOpinion) {
}
