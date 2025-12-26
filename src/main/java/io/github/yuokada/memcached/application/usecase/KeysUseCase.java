package io.github.yuokada.memcached.application.usecase;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class KeysUseCase {

    private final MemcachedPort memcachedPort;

    @Inject
    public KeysUseCase(MemcachedPort memcachedPort) {
        this.memcachedPort = memcachedPort;
    }

    public List<String> execute(int limit) {
        return memcachedPort.fetchKeys(limit);
    }
}
