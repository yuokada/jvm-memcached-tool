package io.github.yuokada.memcached.application.port;

import java.util.List;

public interface DumpPort {

    List<DumpMetadata> fetchMetadata(int limit);

    record DumpMetadata(String key, int expiration) {

    }
}
