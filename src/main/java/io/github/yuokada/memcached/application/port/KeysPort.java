package io.github.yuokada.memcached.application.port;

import java.util.List;

public interface KeysPort {

    List<String> fetchKeys(int limit);
}
