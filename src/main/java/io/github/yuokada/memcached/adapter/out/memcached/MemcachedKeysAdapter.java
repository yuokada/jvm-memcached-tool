package io.github.yuokada.memcached.adapter.out.memcached;

import io.github.yuokada.memcached.application.port.KeysPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.spy.memcached.MemcachedClient;

@ApplicationScoped
public class MemcachedKeysAdapter extends AbstractMemcachedSocketAdapter implements KeysPort {

    private static final Pattern PATTERN = Pattern.compile("^key=(\\S+) exp=(-?\\d+) .*");

    @Inject
    public MemcachedKeysAdapter(MemcachedClient memcachedClient) {
        super(memcachedClient);
    }

    @Override
    public List<String> fetchKeys(int limit) {
        return executeCommand("lru_crawler metadump all\r\n", reader -> {
            List<String> results = new ArrayList<>();
            int counter = 0;
            String response;
            while ((response = reader.readLine()) != null) {
                if (limit > 0 && counter >= limit) {
                    break;
                }
                if ("END".equals(response)) {
                    break;
                }
                if (PATTERN.matcher(response).matches()) {
                    results.add(response);
                    counter++;
                }
            }
            return results;
        });
    }
}
