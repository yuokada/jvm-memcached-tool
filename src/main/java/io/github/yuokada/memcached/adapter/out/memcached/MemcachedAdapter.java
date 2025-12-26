package io.github.yuokada.memcached.adapter.out.memcached;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;

@ApplicationScoped
public class MemcachedAdapter implements MemcachedPort {

    private final MemcachedClient memcachedClient;

    @Inject
    public MemcachedAdapter(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
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
    public Collection<InetSocketAddress> getAvailableServers() {
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
