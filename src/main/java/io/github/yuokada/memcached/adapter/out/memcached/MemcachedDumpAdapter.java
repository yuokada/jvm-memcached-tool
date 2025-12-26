package io.github.yuokada.memcached.adapter.out.memcached;

import io.github.yuokada.memcached.application.port.DumpPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MemcachedDumpAdapter implements DumpPort {

    private static final Logger log = LoggerFactory.getLogger(MemcachedDumpAdapter.class);
    private static final Pattern PATTERN = Pattern.compile("^key=(\\S+) exp=(-?\\d+) .*");
    private final MemcachedClient memcachedClient;

    @Inject
    public MemcachedDumpAdapter(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
    }

    @Override
    public List<DumpMetadata> fetchMetadata(int limit) {
        Collection<InetSocketAddress> endpoints = resolveEndpoints();
        List<DumpMetadata> allResults = new ArrayList<>();
        int totalCounter = 0;

        for (InetSocketAddress endpoint : endpoints) {
            if (limit > 0 && totalCounter >= limit) {
                break;
            }

            try {
                int serverLimit = limit > 0 ? limit - totalCounter : 0;
                List<DumpMetadata> serverResults = fetchMetadataFromServer(endpoint, serverLimit);
                allResults.addAll(serverResults);
                totalCounter += serverResults.size();
            } catch (IOException e) {
                log.warn("Failed to dump metadata from {}: {}", endpoint, e.getMessage());
            }
        }

        return allResults;
    }

    private List<DumpMetadata> fetchMetadataFromServer(InetSocketAddress endpoint, int limit) throws IOException {
        try (Socket socket = new Socket(endpoint.getHostString(), endpoint.getPort());
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            var writer = new OutputStreamWriter(socket.getOutputStream())) {

            writer.write("lru_crawler metadump all\r\n");
            writer.flush();

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
        }
    }

    private Collection<InetSocketAddress> resolveEndpoints() {
        Collection<SocketAddress> servers = memcachedClient.getAvailableServers();
        List<InetSocketAddress> endpoints = servers.stream()
            .filter(InetSocketAddress.class::isInstance)
            .map(InetSocketAddress.class::cast)
            .toList();

        if (endpoints.isEmpty()) {
            throw new IllegalStateException("No available memcached servers");
        }

        return endpoints;
    }
}
