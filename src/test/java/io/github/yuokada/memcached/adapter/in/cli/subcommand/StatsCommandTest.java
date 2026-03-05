package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import io.github.yuokada.memcached.application.usecase.StatsUseCase;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.ExitCode;

class StatsCommandTest {

    @Test
    void callReturnsOkWithDefaultOperation() throws Exception {
        StatsCommand cmd = new StatsCommand();
        cmd.statsUseCase = new StatsUseCase(new StubPort());
        cmd.operation = "";

        assertEquals(ExitCode.OK, cmd.call());
    }

    @Test
    void callReturnsOkWithJsonFlag() throws Exception {
        StatsCommand cmd = new StatsCommand();
        cmd.statsUseCase = new StatsUseCase(new StubPort());
        cmd.operation = "";
        cmd.jsonOutputFlag = true;

        assertEquals(ExitCode.OK, cmd.call());
    }

    @Test
    void callReturnsOkWithItemsOperation() throws Exception {
        StatsCommand cmd = new StatsCommand();
        cmd.statsUseCase = new StatsUseCase(new StubPort());
        cmd.operation = "items";

        assertEquals(ExitCode.OK, cmd.call());
    }

    @Test
    void callWithNoEntryCommandDoesNotThrow() throws Exception {
        StatsCommand cmd = new StatsCommand();
        cmd.statsUseCase = new StatsUseCase(new StubPort());
        cmd.operation = "";
        cmd.entryCommand = null;

        assertEquals(ExitCode.OK, cmd.call());
    }

    @Test
    void callReturnsUsageForUnsupportedOperation() throws Exception {
        StatsCommand cmd = new StatsCommand();
        cmd.statsUseCase = new StatsUseCase(new StubPort());
        cmd.operation = "unknown_op";

        assertEquals(ExitCode.USAGE, cmd.call());
    }

    static class StubPort implements MemcachedPort {
        static final SocketAddress ADDR = new InetSocketAddress("localhost", 11211);

        @Override
        public Map<SocketAddress, Map<String, String>> stats() {
            return Map.of(ADDR, Map.of("curr_items", "42", "bytes", "1024"));
        }

        @Override
        public Map<SocketAddress, Map<String, String>> stats(String s) {
            return Map.of(ADDR, Map.of("items:1:number", "10"));
        }

        @Override public void set(String key, int exp, Object v) {}
        @Override public Object get(String key) { return null; }
        @Override public Map<String, Object> getBulk(Collection<String> keys) { return Map.of(); }
        @Override public boolean flush(int t) throws ExecutionException, InterruptedException, TimeoutException { return true; }
        @Override public List<DumpMetadata> fetchMetadata(int l) { return List.of(); }
        @Override public List<String> fetchKeys(int l) { return List.of(); }
    }
}
