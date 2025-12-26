package io.github.yuokada.memcached.application.usecase;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.SocketAddress;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class DisplayUseCase {

    private static final Pattern ITEMS_PATTERN = Pattern.compile("items:(\\d+):(\\w+)");
    private static final Pattern SLABS_PATTERN = Pattern.compile("(\\d+):(\\w+)");
    private static final DecimalFormat SIZE_DECIMAL = new DecimalFormat("0.0");

    private final MemcachedPort memcachedPort;

    @Inject
    public DisplayUseCase(MemcachedPort memcachedPort) {
        this.memcachedPort = memcachedPort;
    }

    public List<DisplayResult> execute() {
        Map<SocketAddress, Map<String, String>> itemsStats = memcachedPort.stats("items");
        Map<SocketAddress, Map<String, String>> slabsStats = memcachedPort.stats("slabs");

        Map<SocketAddress, List<SlabSummary>> perServer = new LinkedHashMap<>();
        itemsStats.keySet().forEach(address -> perServer.putIfAbsent(address, new ArrayList<>()));
        slabsStats.keySet().forEach(address -> perServer.putIfAbsent(address, new ArrayList<>()));

        perServer.replaceAll((address, ignored) -> {
            Map<Integer, MutableSlab> slabs = new TreeMap<>();
            Map<String, String> itemMetrics = itemsStats.getOrDefault(address, Map.of());
            Map<String, String> slabMetrics = slabsStats.getOrDefault(address, Map.of());

            populateFromItems(itemMetrics, slabs);
            populateFromSlabs(slabMetrics, slabs);

            return slabs.values().stream()
                .sorted(Comparator.comparingInt(slab -> slab.slabId))
                .map(DisplayUseCase::toSummary)
                .filter(summary -> summary != null)
                .toList();
        });

        List<DisplayResult> results = new ArrayList<>();
        perServer.forEach((address, slabs) -> results.add(new DisplayResult(address.toString(), slabs)));
        return results;
    }

    private static void populateFromItems(Map<String, String> stats, Map<Integer, MutableSlab> slabs) {
        stats.forEach((key, value) -> {
            Matcher matcher = ITEMS_PATTERN.matcher(key);
            if (!matcher.matches()) {
                return;
            }
            int slabId = Integer.parseInt(matcher.group(1));
            String name = matcher.group(2);
            MutableSlab slab = slabs.computeIfAbsent(slabId, MutableSlab::new);
            switch (name) {
                case "number" -> slab.count = parseLong(value);
                case "age" -> slab.ageSeconds = parseLong(value);
                case "evicted" -> slab.evicted = parseLong(value);
                case "evicted_time" -> slab.evictedTime = parseLong(value);
                case "outofmemory" -> slab.outOfMemory = parseLong(value);
                default -> {
                }
            }
        });
    }

    private static void populateFromSlabs(Map<String, String> stats, Map<Integer, MutableSlab> slabs) {
        stats.forEach((key, value) -> {
            Matcher matcher = SLABS_PATTERN.matcher(key);
            if (!matcher.matches()) {
                return;
            }
            int slabId = Integer.parseInt(matcher.group(1));
            String name = matcher.group(2);
            MutableSlab slab = slabs.computeIfAbsent(slabId, MutableSlab::new);
            switch (name) {
                case "chunk_size" -> slab.chunkSize = parseLong(value);
                case "total_pages" -> slab.totalPages = parseLong(value);
                case "total_chunks" -> slab.totalChunks = parseLong(value);
                case "used_chunks" -> slab.usedChunks = parseLong(value);
                default -> {
                }
            }
        });
    }

    private static SlabSummary toSummary(MutableSlab slab) {
        if (slab.totalPages == 0) {
            return null;
        }
        String sizeLabel;
        if (slab.chunkSize < 1024) {
            sizeLabel = slab.chunkSize + "B";
        } else {
            sizeLabel = SIZE_DECIMAL.format((double) slab.chunkSize / 1024.0) + "K";
        }
        boolean full = slab.totalChunks > 0 && slab.usedChunks == slab.totalChunks;
        return new SlabSummary(
            slab.slabId,
            sizeLabel,
            slab.ageSeconds,
            slab.totalPages,
            slab.count,
            full,
            slab.evicted,
            slab.evictedTime,
            slab.outOfMemory
        );
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public record DisplayResult(String server, List<SlabSummary> slabs) {

    }

    public record SlabSummary(
        int slabId,
        String itemSize,
        long maxAgeSeconds,
        long pages,
        long count,
        boolean full,
        long evicted,
        long evictedTime,
        long outOfMemory
    ) {

    }

    private static class MutableSlab {

        private final int slabId;
        private long chunkSize;
        private long ageSeconds;
        private long totalPages;
        private long count;
        private long usedChunks;
        private long totalChunks;
        private long evicted;
        private long evictedTime;
        private long outOfMemory;

        MutableSlab(int slabId) {
            this.slabId = slabId;
        }
    }
}
