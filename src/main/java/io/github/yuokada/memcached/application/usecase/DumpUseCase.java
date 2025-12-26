package io.github.yuokada.memcached.application.usecase;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class DumpUseCase {

    private final MemcachedPort memcachedPort;

    @Inject
    public DumpUseCase(MemcachedPort memcachedPort) {
        this.memcachedPort = memcachedPort;
    }

    public List<DumpResult> execute(int limit) {
        List<MemcachedPort.DumpMetadata> metadata = memcachedPort.fetchMetadata(limit);
        return metadata.stream()
            .map(entry -> {
                Object value = memcachedPort.get(entry.key());
                String serialized = value == null ? "" : value.toString();
                return new DumpResult(entry.key(), entry.expiration(), serialized);
            })
            .toList();
    }

    public record DumpResult(String key, int expiration, String value) {

    }
}
