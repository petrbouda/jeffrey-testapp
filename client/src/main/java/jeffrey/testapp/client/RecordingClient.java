package jeffrey.testapp.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class RecordingClient {

    private static final AtomicLong counter = new AtomicLong();

    private static final int RECORDING_COUNT = 8;

    private static final ParameterizedTypeReference<List<String>> LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient client;

    public RecordingClient(RestClient client) {
        this.client = client;
    }

    public List<String> getAllRecordings() {
        return client.get()
                .retrieve()
                .body(LIST_TYPE);
    }

    public byte[] getRecording() {
        long i = counter.incrementAndGet();
        return client.get()
                .uri("/{id}", i % RECORDING_COUNT)
                .retrieve()
                .body(byte[].class);
    }
}
