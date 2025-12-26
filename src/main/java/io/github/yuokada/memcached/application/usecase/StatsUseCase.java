package io.github.yuokada.memcached.application.usecase;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class StatsUseCase {

    private final MemcachedPort memcachedPort;

    @Inject
    public StatsUseCase(MemcachedPort memcachedPort) {
        this.memcachedPort = memcachedPort;
    }

    public Map<SocketAddress, Map<String, String>> execute(String operation) {
        if (operation == null || operation.isEmpty()) {
            return memcachedPort.stats();
        }

        try {
            StatsSubCommands subcommand = StatsSubCommands.valueOf(operation);
            return memcachedPort.stats(subcommand.name());
        } catch (IllegalArgumentException e) {
            List<String> availableCommands = StatsSubCommands.availableCommands();
            String message = String.format(
                "Unsupported extra command: %s\nAvailable commands: %s",
                operation,
                String.join(", ", availableCommands)
            );
            throw new IllegalArgumentException(message, e);
        }
    }
}
