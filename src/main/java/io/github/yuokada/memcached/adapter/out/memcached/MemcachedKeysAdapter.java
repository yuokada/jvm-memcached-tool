package io.github.yuokada.memcached.adapter.out.memcached;

import io.github.yuokada.memcached.application.port.KeysPort;
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
import java.util.regex.Pattern;
import net.spy.memcached.MemcachedClient;

@ApplicationScoped
public class MemcachedKeysAdapter implements KeysPort {

    private static final Pattern PATTERN = Pattern.compile("^key=(\\S+) exp=(-?\\d+) .*");
    private final MemcachedClient memcachedClient;

    @Inject
    public MemcachedKeysAdapter(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
    }

    @Override
    public List<String> fetchKeys(int limit) {
        InetSocketAddress endpoint = resolveEndpoint();
        try (Socket socket = new Socket(endpoint.getHostString(), endpoint.getPort());
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            var writer = new OutputStreamWriter(socket.getOutputStream())) {

            writer.write("lru_crawler metadump all\r\n");
            writer.flush();

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
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch keys from memcached", e);
        }
    }

    private InetSocketAddress resolveEndpoint() {
        Collection<SocketAddress> servers = memcachedClient.getAvailableServers();
        return servers.stream()
            .filter(InetSocketAddress.class::isInstance)
            .map(InetSocketAddress.class::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No available memcached servers"));
    }
}
