package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import io.github.yuokada.memcached.application.usecase.SizesUseCase;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.ExitCode;

class SizesCommandTest {

    @Test
    void callReturnsOkWithDefaultFlags() throws Exception {
        SizesCommand cmd = new SizesCommand();
        cmd.sizesUseCase = new SizesUseCase(new StubPort());

        assertEquals(ExitCode.OK, cmd.call());
    }

    @Test
    void callReturnsOkWithJsonFlag() throws Exception {
        SizesCommand cmd = new SizesCommand();
        cmd.sizesUseCase = new SizesUseCase(new StubPort());
        cmd.jsonOutput = true;

        assertEquals(ExitCode.OK, cmd.call());
    }

    @Test
    void callWithNoEntryCommandDoesNotThrow() throws Exception {
        SizesCommand cmd = new SizesCommand();
        cmd.sizesUseCase = new SizesUseCase(new StubPort());
        cmd.entryCommand = null;

        assertEquals(ExitCode.OK, cmd.call());
    }

    static class StubPort implements MemcachedPort {
        static final SocketAddress ADDR = new InetSocketAddress("localhost", 11211);

        @Override public Map<SocketAddress, Map<String, String>> stats() { return Map.of(); }

        @Override
        public Map<SocketAddress, Map<String, String>> stats(String subcommand) {
            return Map.of(ADDR, Map.of("96", "5", "120", "3"));
        }

        @Override public void set(String key, int exp, Object v) {}
        @Override public Object get(String key) { return null; }
        @Override public Map<String, Object> getBulk(Collection<String> keys) { return Map.of(); }
        @Override public boolean flush(int t) throws ExecutionException, InterruptedException, TimeoutException { return true; }
        @Override public List<DumpMetadata> fetchMetadata(int l) { return List.of(); }
        @Override public List<String> fetchKeys(int l) { return List.of(); }
    }
}
