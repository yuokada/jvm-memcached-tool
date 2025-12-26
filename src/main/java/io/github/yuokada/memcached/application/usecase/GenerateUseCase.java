package io.github.yuokada.memcached.application.usecase;

import io.github.yuokada.memcached.application.port.DataGeneratorPort;
import io.github.yuokada.memcached.application.port.MemcachedPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.SocketAddress;
import java.util.Map;

@ApplicationScoped
public class GenerateUseCase {

    private final MemcachedPort memcachedPort;
    private final DataGeneratorPort dataGeneratorPort;

    @Inject
    public GenerateUseCase(MemcachedPort memcachedPort, DataGeneratorPort dataGeneratorPort) {
        this.memcachedPort = memcachedPort;
        this.dataGeneratorPort = dataGeneratorPort;
    }

    public Result execute(int requestedSize) {
        int size = requestedSize;
        if (size <= 0) {
            size = dataGeneratorPort.randomSize();
        }

        for (int i = 0; i < size; i++) {
            memcachedPort.set(String.format("key_%d", i), 3600, dataGeneratorPort.fullName());
        }

        Map<SocketAddress, Map<String, String>> stats = memcachedPort.stats();
        return new Result(size, stats);
    }

    public record Result(int generatedCount, Map<SocketAddress, Map<String, String>> stats) {
    }
}
