package io.github.yuokada.memcached.application.usecase;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SizesUseCase {

    private final MemcachedPort memcachedPort;

    @Inject
    public SizesUseCase(MemcachedPort memcachedPort) {
        this.memcachedPort = memcachedPort;
    }

    public List<SizesResult> execute() {
        Map<SocketAddress, Map<String, String>> stats = memcachedPort.stats("sizes");
        List<SizesResult> results = new ArrayList<>();

        stats.forEach((address, values) -> {
            List<SizeCount> entries = values.entrySet().stream()
                .map(entry -> new SizeCount(parseSize(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparingLong(SizeCount::sizeBytes))
                .toList();
            results.add(new SizesResult(address.toString(), entries));
        });

        return results;
    }

    private static long parseSize(String sizeLabel) {
        try {
            return Long.parseLong(sizeLabel.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public record SizesResult(String server, List<SizeCount> entries) {
    }

    public record SizeCount(long sizeBytes, String count) {
    }
}
