package io.github.yuokada.memcached.application.usecase;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DumpUseCase {

    private static final Logger log = LoggerFactory.getLogger(DumpUseCase.class);
    private static final Pattern PATTERN = Pattern.compile("^key=(\\S+) exp=(-?\\d+) .*");
    private final MemcachedPort memcachedPort;

    @Inject
    public DumpUseCase(MemcachedPort memcachedPort) {
        this.memcachedPort = memcachedPort;
    }

    public List<DumpResult> execute(String host, int port, int limit) {
        Collection<InetSocketAddress> endpoints = memcachedPort.getAvailableServers();
        List<DumpResult> allResults = new ArrayList<>();
        int totalCounter = 0;

        for (InetSocketAddress endpoint : endpoints) {
            if (limit > 0 && totalCounter >= limit) {
                break;
            }

            try {
                int serverLimit = limit > 0 ? limit - totalCounter : 0;
                List<DumpResult> serverResults = fetchMetadataFromServer(endpoint, serverLimit);
                allResults.addAll(serverResults);
                totalCounter += serverResults.size();
            } catch (IOException e) {
                log.warn("Failed to dump metadata from {}: {}", endpoint, e.getMessage());
            }
        }

        return allResults;
    }

    private List<DumpResult> fetchMetadataFromServer(InetSocketAddress endpoint, int limit) throws IOException {
        try (Socket socket = new Socket(endpoint.getHostString(), endpoint.getPort());
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            var writer = new OutputStreamWriter(socket.getOutputStream())) {

            writer.write("lru_crawler metadump all\r\n");
            writer.flush();

            int counter = 0;
            String response;
            List<DumpResult> results = new ArrayList<>();
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
                Object value = memcachedPort.get(key);
                String serialized = value == null ? "" : value.toString();
                results.add(new DumpResult(key, expiration, serialized));
                counter++;
            }
            return results;
        }
    }

    public record DumpResult(String key, int expiration, String value) {
    }
}
