package jeffrey.testapp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class PersonRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    private static final String REMOVE_ALL = """
            DELETE FROM person
            """;

    private static final String INSERT_PERSON = """
            INSERT INTO person (firstname, lastname, city, country, phone, political_opinion)
            VALUES (:firstname, :lastname, :city, :country, :phone, :political_opinion)
            """;

    private final Path backupFile;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    // It's FAIR scheduling to not starve the writer thread
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private final Map<Person, Boolean> cache = new ConcurrentHashMap<>();

    public PersonRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        try {
            this.backupFile = Files.createTempFile("jeffrey-testapp", null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return a single Person according to provided {@code id} parameter.
     *
     * @param id id of the expected Person entity
     * @return Person entity, or {@code null} if the entity does not exist
     */
    public Optional<Person> findById(long id) {
        readLock.lock();
        try {
            SqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
            Person person = jdbcTemplate.queryForObject(FIND_BY_ID, params, personMapper());
            if (person != null) {
                return Optional.of(modifyOpinion(person));
            } else {
                return Optional.empty();
            }
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Tries to find the Person entities according to a provided list of {@code ids}.
     *
     * @param ids IDs of the expected Person entities
     * @return a list of the resolved entities
     */
    public List<Person> findByIds(Collection<Long> ids) {
        readLock.lock();
        try {
            SqlParameterSource params = new MapSqlParameterSource("ids", ids);
            return jdbcTemplate.query(FIND_ALL_BY_IDS, params, personMapper()).stream()
                    .map(PersonRepository::modifyOpinion)
                    .toList();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Current number of persons in the table.
     *
     * @return count of all persons.
     */
    public int count() {
        readLock.lock();
        try {
            return jdbcTemplate.queryForObject(COUNT_QUERY, Map.of(), Integer.class);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Remove a person with a provided {@code id}
     *
     * @param id person's ID to delete
     */
    public void remove(long id) {
        readLock.lock();
        try {
            IDHolder.IDS.remove(id);
            jdbcTemplate.update(REMOVE_QUERY_BY_ID, Map.of("id", id));
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Add a new person represented by {@code person} parameter
     *
     * @param person person entity to be inserted
     * @return full populated person with a new ID
     */
    public Person insert(Person person) {
        readLock.lock();
        try {
            Map<String, Object> parameters = Map.of(
                    "firstname", person.firstname(),
                    "lastname", person.lastname(),
                    "city", person.city(),
                    "country", person.country(),
                    "phone", person.phone(),
                    "political_opinion", person.politicalOpinion());

            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(INSERT_PERSON, new MapSqlParameterSource(parameters), keyHolder);
            Long id = extractId(keyHolder);
            IDHolder.IDS.add(id);
            return person.copyWithId(id);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Streams all persons from the table.
     *
     * @return a stream of all persons.
     */
    private Stream<Person> streamAll() {
        return jdbcTemplate.queryForStream(ALL_QUERY, Map.of(), personMapper());
    }

    /**
     * Execute a single raw statement. Dangerous, use it sparingly and with cautions.
     *
     * @param statement statement to be executed.
     */
    public void insertRaw(String statement) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(statement, new EmptySqlParameterSource(), keyHolder);
        Long id = extractId(keyHolder);
        IDHolder.IDS.add(id);
    }

    public void backupAndReload() {
        writeLock.lock();
        try {
            dumpToFile();
            deleteAll();
            loadFromFile();
        } finally {
            writeLock.unlock();
        }
    }

    private void deleteAll() {
        jdbcTemplate.update(REMOVE_ALL, Map.of());
    }

    private void dumpToFile() {
        try (BufferedWriter writer = Files.newBufferedWriter(backupFile)) {
            streamAll()
                    .map(PersonRepository::convertToString)
                    .forEach(json -> write(writer, json));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadFromFile() {
        try (Stream<String> stream = Files.lines(backupFile)) {
            stream.map(PersonRepository::parseJsonValue)
                    .forEach(this::insert);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Person parseJsonValue(String json) {
        try {
            return MAPPER.readValue(json, Person.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String convertToString(Person person) {
        try {
            return MAPPER.writeValueAsString(person);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void write(BufferedWriter writer, String json) {
        try {
            writer.write(json);
            writer.newLine();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static RowMapper<Person> personMapper() {
        return (rs, __) -> new Person(
                rs.getLong("id"),
                rs.getString("firstname"),
                rs.getString("lastname"),
                rs.getString("city"),
                rs.getString("country"),
                rs.getString("phone"),
                rs.getString("political_opinion")
        );
    }

    private static Person modifyOpinion(Person person) {
        return new Person(
                person.id(),
                person.firstname(),
                person.lastname(),
                person.city(),
                person.country(),
                person.phone(),
                person.politicalOpinion().replaceAll("\\sm.+?\\s", " - "));
    }

    private static Long extractId(GeneratedKeyHolder keyHolder) {
        Object id = keyHolder.getKeys().get("id");
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return null;
    }
}
