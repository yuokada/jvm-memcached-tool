package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import io.github.yuokada.memcached.application.usecase.DumpUseCase;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.ExitCode;

class DumpCommandTest {

    @Test
    void callReturnsOkWithNoLimit() throws Exception {
        DumpCommand cmd = new DumpCommand();
        cmd.dumpUseCase = new DumpUseCase(new StubPort());
        cmd.limit = 0;

        assertEquals(ExitCode.OK, cmd.call());
    }

    @Test
    void callReturnsOkWithPositiveLimit() throws Exception {
        DumpCommand cmd = new DumpCommand();
        cmd.dumpUseCase = new DumpUseCase(new StubPort());
        cmd.limit = 5;

        assertEquals(ExitCode.OK, cmd.call());
    }

    @Test
    void callReturnsUsageErrorWhenLimitIsNegative() throws Exception {
        DumpCommand cmd = new DumpCommand();
        cmd.dumpUseCase = new DumpUseCase(new StubPort());
        cmd.limit = -1;

        assertEquals(ExitCode.USAGE, cmd.call());
    }

    @Test
    void callWithNoEntryCommandDoesNotThrow() throws Exception {
        DumpCommand cmd = new DumpCommand();
        cmd.dumpUseCase = new DumpUseCase(new StubPort());
        cmd.limit = 0;
        cmd.entryCommand = null;

        assertEquals(ExitCode.OK, cmd.call());
    }

    static class StubPort implements MemcachedPort {
        @Override public Map<SocketAddress, Map<String, String>> stats() { return Map.of(); }
        @Override public Map<SocketAddress, Map<String, String>> stats(String s) { return Map.of(); }
        @Override public void set(String key, int exp, Object v) {}
        @Override public Object get(String key) { return "value-for-" + key; }
        @Override public boolean flush(int t) throws ExecutionException, InterruptedException, TimeoutException { return true; }
        @Override public List<DumpMetadata> fetchMetadata(int l) {
            return List.of(new DumpMetadata("key1", 3600), new DumpMetadata("key2", -1));
        }
        @Override public List<String> fetchKeys(int l) { return List.of(); }
    }
}
