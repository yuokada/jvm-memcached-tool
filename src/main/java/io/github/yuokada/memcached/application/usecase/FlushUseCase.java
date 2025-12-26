package io.github.yuokada.memcached.application.usecase;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class FlushUseCase {

    private static final int DEFAULT_TIMEOUT_SECONDS = 15;
    private final MemcachedPort memcachedPort;

    @Inject
    public FlushUseCase(MemcachedPort memcachedPort) {
        this.memcachedPort = memcachedPort;
    }

    public boolean execute() {
        try {
            return memcachedPort.flush(DEFAULT_TIMEOUT_SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Flush command interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Failed to flush memcached", e);
        }
    }
}
