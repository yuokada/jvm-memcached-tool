package io.github.yuokada.memcached.application.usecase;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DumpUseCase {

    private final MemcachedPort memcachedPort;

    @Inject
    public DumpUseCase(MemcachedPort memcachedPort) {
        this.memcachedPort = memcachedPort;
    }

    public List<DumpResult> execute(int limit) {
        List<MemcachedPort.DumpMetadata> metadata = memcachedPort.fetchMetadata(limit);
        List<String> keys = metadata.stream().map(MemcachedPort.DumpMetadata::key).toList();
        Map<String, Object> values = memcachedPort.getBulk(keys);
        return metadata.stream()
            .map(entry -> {
                Object value = values.get(entry.key());
                String serialized = value == null ? "" : value.toString();
                return new DumpResult(entry.key(), entry.expiration(), serialized);
            })
            .toList();
    }

    public record DumpResult(String key, int expiration, String value) {

    }
}
