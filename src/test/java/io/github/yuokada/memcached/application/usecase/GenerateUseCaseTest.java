package io.github.yuokada.memcached.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yuokada.memcached.application.port.DataGeneratorPort;
import io.github.yuokada.memcached.application.port.MemcachedPort;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class GenerateUseCaseTest {

    @Test
    void executeWithFixedSizeWritesExactCount() {
        RecordingPort port = new RecordingPort();
        FakeGenerator generator = new FakeGenerator(99, "Jane Doe");
        GenerateUseCase useCase = new GenerateUseCase(port, generator);

        GenerateUseCase.Result result = useCase.execute(5);

        assertEquals(5, result.generatedCount());
        assertEquals(5, port.sets.size());
    }

    @Test
    void executeWithZeroSizeUsesRandomSizeFromGenerator() {
        RecordingPort port = new RecordingPort();
        FakeGenerator generator = new FakeGenerator(3, "Random Name");
        GenerateUseCase useCase = new GenerateUseCase(port, generator);

        GenerateUseCase.Result result = useCase.execute(0);

        assertEquals(3, result.generatedCount());
        assertEquals(3, port.sets.size());
    }

    @Test
    void executeWithNegativeSizeUsesRandomSizeFromGenerator() {
        RecordingPort port = new RecordingPort();
        FakeGenerator generator = new FakeGenerator(2, "Name");
        GenerateUseCase useCase = new GenerateUseCase(port, generator);

        GenerateUseCase.Result result = useCase.execute(-10);

        assertEquals(2, result.generatedCount());
        assertEquals(2, port.sets.size());
    }

    @Test
    void executeStoresItemsWithExpectedKeyPattern() {
        RecordingPort port = new RecordingPort();
        GenerateUseCase useCase = new GenerateUseCase(port, new FakeGenerator(3, "val"));

        useCase.execute(3);

        Set<String> keys = port.sets.stream().map(SetCall::key).collect(Collectors.toSet());
        assertTrue(keys.containsAll(List.of("key_0", "key_1", "key_2")));
    }

    @Test
    void resultContainsStatsFromPort() {
        RecordingPort port = new RecordingPort();
        GenerateUseCase useCase = new GenerateUseCase(port, new FakeGenerator(1, "v"));

        GenerateUseCase.Result result = useCase.execute(1);

        assertEquals(port.statsResult, result.stats());
    }

    record SetCall(String key, int expiration, Object value) {}

    static class RecordingPort implements MemcachedPort {

        final List<SetCall> sets = new ArrayList<>();
        final Map<SocketAddress, Map<String, String>> statsResult = Map.of();

        @Override
        public Map<SocketAddress, Map<String, String>> stats() { return statsResult; }

        @Override
        public Map<SocketAddress, Map<String, String>> stats(String subcommand) { return statsResult; }

        @Override
        public void set(String key, int expirationSeconds, Object value) {
            sets.add(new SetCall(key, expirationSeconds, value));
        }

        @Override
        public Object get(String key) { return null; }

        @Override
        public boolean flush(int timeoutSeconds)
            throws ExecutionException, InterruptedException, TimeoutException {
            return true;
        }

        @Override
        public List<DumpMetadata> fetchMetadata(int limit) { return List.of(); }

        @Override
        public List<String> fetchKeys(int limit) { return List.of(); }
    }

    static class FakeGenerator implements DataGeneratorPort {

        private final int size;
        private final String name;

        FakeGenerator(int size, String name) {
            this.size = size;
            this.name = name;
        }

        @Override
        public int randomSize() { return size; }

        @Override
        public String fullName() { return name; }
    }
}
