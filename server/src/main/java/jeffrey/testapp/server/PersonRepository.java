package jeffrey.testapp.server;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class PersonRepository {

    private static final String FIND_BY_ID = """
            SELECT * FROM person WHERE id = :id
            """;

    private static final String FIND_ALL_BY_IDS = """
            SELECT * FROM person WHERE id IN (:ids)
            """;

    private static final String ALL_QUERY = """
            SELECT * FROM person
            """;

    private static final String COUNT_QUERY = """
            SELECT COUNT(*) FROM person
            """;

    private static final String REMOVE_QUERY_BY_ID = """
            DELETE FROM person WHERE id = :id
            """;

    private static final String INSERT_PERSON = """
            INSERT INTO person (firstname, lastname, city, country, phone, political_opinion)
            VALUES (:firstname, :lastname, :city, :country, :phone, :political_opinion)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PersonRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Return a single Person according to provided {@code id} parameter.
     *
     * @param id id of the expected Person entity
     * @return Person entity, or {@code null} if the entity does not exist
     */
    public Person findById(long id) {
        SqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        return jdbcTemplate.queryForObject(FIND_BY_ID, params, personMapper());
    }

    /**
     * Tries to find the Person entities according to a provided list of {@code ids}.
     *
     * @param ids IDs of the expected Person entities
     * @return a list of the resolved entities
     */
    public List<Person> findByIds(Collection<Long> ids) {
        SqlParameterSource params = new MapSqlParameterSource("ids", ids);
        return jdbcTemplate.query(FIND_ALL_BY_IDS, params, personMapper());
    }

    /**
     * Current number of persons in the table.
     *
     * @return count of all persons.
     */
    public int count() {
        return jdbcTemplate.queryForObject(COUNT_QUERY, Map.of(), Integer.class);
    }

    /**
     * Streams all persons from the table.
     *
     * @return a stream of all persons.
     */
    public Stream<Person> streamAll() {
        return jdbcTemplate.queryForStream(ALL_QUERY, Map.of(), personMapper());
    }

    /**
     * Remove a person with a provided {@code id}
     *
     * @param id person's ID to delete
     */
    public void remove(long id) {
        jdbcTemplate.update(REMOVE_QUERY_BY_ID, Map.of("id", id));
    }

    /**
     * Add a new person represented by {@code person} parameter
     *
     * @param person person entity to be inserted
     * @return full populated person with a new ID
     */
    public Person insert(Person person) {
        Map<String, Object> parameters = Map.of(
                "firstname", person.firstname(),
                "lastname", person.lastname(),
                "city", person.city(),
                "country", person.country(),
                "phone", person.phone(),
                "political_opinion", person.politicalOpinion());

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(INSERT_PERSON, new MapSqlParameterSource(parameters), keyHolder);
        return person.copyWithId(keyHolder.getKey().longValue());
    }

    /**
     * Execute a single raw statement. Dangerous, use it sparingly and with cautions.
     *
     * @param statement statement to be executed.
     */
    public void insertRaw(String statement) {
        jdbcTemplate.update(statement, Map.of());
    }

    private static RowMapper<Person> personMapper() {
        return (rs, __) -> new Person(
                rs.getLong("id"),
                rs.getString("firstname"),
                rs.getString("lastname"),
                rs.getString("city"),
                rs.getString("country"),
                rs.getString("phone"),
                rs.getString("political_opinion"));
    }
}
