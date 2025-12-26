package io.github.yuokada.memcached.adapter.out.memcached;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class MemcachedAdapter extends AbstractMemcachedSocketAdapter implements MemcachedPort {

    private static final Pattern METADUMP_PATTERN = Pattern.compile("^key=(\\S+) exp=(-?\\d+) .*");

    @Inject
    public MemcachedAdapter(MemcachedClient memcachedClient) {
        super(memcachedClient);
    }

    @Override
    public Map<SocketAddress, Map<String, String>> stats() {
        return memcachedClient.getStats();
    }

    @Override
    public Map<SocketAddress, Map<String, String>> stats(String subcommand) {
        return memcachedClient.getStats(subcommand);
    }

    @Override
    public void set(String key, int expirationSeconds, Object value) {
        memcachedClient.set(key, expirationSeconds, value);
    }

    @Override
    public Object get(String key) {
        return memcachedClient.get(key);
    }

    @Override
    public boolean flush(int timeoutSeconds)
        throws ExecutionException, InterruptedException, TimeoutException {
        OperationFuture<Boolean> flushResult = memcachedClient.flush();
        return flushResult.get(timeoutSeconds, TimeUnit.SECONDS);
    }

    @Override
    public List<DumpMetadata> fetchMetadata(int limit) {
        return executeCommand("lru_crawler metadump all\r\n", reader -> {
            List<DumpMetadata> results = new ArrayList<>();
            int counter = 0;
            String response;
            while ((response = reader.readLine()) != null) {
                if (shouldStop(limit, counter, response)) {
                    break;
                }
                Matcher matcher = METADUMP_PATTERN.matcher(response);
                if (!matcher.matches()) {
                    continue;
                }
                String key = decodeKey(matcher.group(1));
                int expiration = Integer.parseInt(matcher.group(2));
                results.add(new DumpMetadata(key, expiration));
                counter++;
            }
            return results;
        });
    }

    @Override
    public List<String> fetchKeys(int limit) {
        return executeCommand("lru_crawler metadump all\r\n", reader -> {
            List<String> results = new ArrayList<>();
            int counter = 0;
            String response;
            while ((response = reader.readLine()) != null) {
                if (shouldStop(limit, counter, response)) {
                    break;
                }
                if (METADUMP_PATTERN.matcher(response).matches()) {
                    results.add(response);
                    counter++;
                }
            }
            return results;
        });
    }

    private boolean shouldStop(int limit, int counter, String response) {
        return (limit > 0 && counter >= limit) || "END".equals(response);
    }

    private String decodeKey(String encodedKey) {
        return URLDecoder.decode(encodedKey, StandardCharsets.UTF_8);
    }
}
