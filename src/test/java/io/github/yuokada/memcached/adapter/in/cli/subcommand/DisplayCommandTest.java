package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import io.github.yuokada.memcached.application.usecase.DisplayUseCase;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.ExitCode;

class DisplayCommandTest {

    @Test
    void callReturnsOkWithDefaultFlags() throws Exception {
        DisplayCommand cmd = new DisplayCommand();
        cmd.displayUseCase = new DisplayUseCase(new StubPort());

        assertEquals(ExitCode.OK, cmd.call());
    }

    @Test
    void callReturnsOkWithJsonFlag() throws Exception {
        DisplayCommand cmd = new DisplayCommand();
        cmd.displayUseCase = new DisplayUseCase(new StubPort());
        cmd.jsonOutput = true;

        assertEquals(ExitCode.OK, cmd.call());
    }

    @Test
    void callWithNoEntryCommandDoesNotThrow() throws Exception {
        DisplayCommand cmd = new DisplayCommand();
        cmd.displayUseCase = new DisplayUseCase(new StubPort());
        cmd.entryCommand = null;

        assertEquals(ExitCode.OK, cmd.call());
    }

    static class StubPort implements MemcachedPort {
        static final SocketAddress ADDR = new InetSocketAddress("localhost", 11211);

        @Override
        public Map<SocketAddress, Map<String, String>> stats() { return Map.of(); }

        @Override
        public Map<SocketAddress, Map<String, String>> stats(String subcommand) {
            if ("items".equals(subcommand)) {
                return Map.of(ADDR, Map.of("items:1:number", "10", "items:1:age", "60",
                    "items:1:evicted", "0", "items:1:evicted_time", "0", "items:1:outofmemory", "0"));
            }
            if ("slabs".equals(subcommand)) {
                return Map.of(ADDR, Map.of("1:chunk_size", "96", "1:total_pages", "1",
                    "1:total_chunks", "10", "1:used_chunks", "10"));
            }
            return Map.of();
        }

        @Override public void set(String key, int exp, Object v) {}
        @Override public Object get(String key) { return null; }
        @Override public boolean flush(int t) throws ExecutionException, InterruptedException, TimeoutException { return true; }
        @Override public List<DumpMetadata> fetchMetadata(int l) { return List.of(); }
        @Override public List<String> fetchKeys(int l) { return List.of(); }
    }
}
