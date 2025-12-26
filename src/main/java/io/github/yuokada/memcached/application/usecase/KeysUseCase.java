package io.github.yuokada.memcached.application.usecase;

import io.github.yuokada.memcached.application.port.KeysPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class KeysUseCase {

    private final KeysPort keysPort;

    @Inject
    public KeysUseCase(KeysPort keysPort) {
        this.keysPort = keysPort;
    }

    public List<String> execute(int limit) {
        return keysPort.fetchKeys(limit);
    }
}
