package io.github.yuokada.memcached.adapter.out.memcached;

import io.github.yuokada.memcached.application.port.DumpPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.spy.memcached.MemcachedClient;

@ApplicationScoped
public class MemcachedDumpAdapter extends AbstractMemcachedSocketAdapter implements DumpPort {

    private static final Pattern PATTERN = Pattern.compile("^key=(\\S+) exp=(-?\\d+) .*");

    @Inject
    public MemcachedDumpAdapter(MemcachedClient memcachedClient) {
        super(memcachedClient);
    }

    @Override
    public List<DumpMetadata> fetchMetadata(int limit) {
        return executeCommand("lru_crawler metadump all\r\n", reader -> {
            List<DumpMetadata> results = new ArrayList<>();
            int counter = 0;
            String response;
            while ((response = reader.readLine()) != null) {
                if ((limit > 0 && counter >= limit) || "END".equals(response)) {
                    break;
                }

                Matcher matcher = PATTERN.matcher(response);
                if (!matcher.matches()) {
                    continue;
                }
                String key = matcher.group(1);
                int expiration = Integer.parseInt(matcher.group(2));
                results.add(new DumpMetadata(key, expiration));
                counter++;
            }
            return results;
        });
    }
}
