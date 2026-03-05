package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.yuokada.memcached.application.port.MemcachedPort;
import io.github.yuokada.memcached.application.usecase.FlushUseCase;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.ExitCode;

class FlushCommandTest {

    @Test
    void callReturnsOkWhenFlushSucceeds() throws Exception {
        FlushCommand cmd = new FlushCommand();
        cmd.flushUseCase = new FlushUseCase(new StubPort(true));

        assertEquals(ExitCode.OK, cmd.call());
    }

    @Test
    void callReturnsSoftwareWhenFlushFails() throws Exception {
        FlushCommand cmd = new FlushCommand();
        cmd.flushUseCase = new FlushUseCase(new StubPort(false));

        assertEquals(ExitCode.SOFTWARE, cmd.call());
    }

    @Test
    void callReturnsSoftwareWhenFlushThrows() throws Exception {
        FlushCommand cmd = new FlushCommand();
        cmd.flushUseCase = new FlushUseCase(new ThrowingPort());

        assertEquals(ExitCode.SOFTWARE, cmd.call());
    }

    @Test
    void callWithNoEntryCommandDoesNotThrow() throws Exception {
        FlushCommand cmd = new FlushCommand();
        cmd.flushUseCase = new FlushUseCase(new StubPort(true));
        cmd.entryCommand = null;

        assertEquals(ExitCode.OK, cmd.call());
    }

    static class StubPort implements MemcachedPort {
        private final boolean flushResult;
        StubPort(boolean flushResult) { this.flushResult = flushResult; }

        @Override public Map<SocketAddress, Map<String, String>> stats() { return Map.of(); }
        @Override public Map<SocketAddress, Map<String, String>> stats(String s) { return Map.of(); }
        @Override public void set(String key, int exp, Object v) {}
        @Override public Object get(String key) { return null; }
        @Override public Map<String, Object> getBulk(Collection<String> keys) { return Map.of(); }
        @Override public boolean flush(int t) throws ExecutionException, InterruptedException, TimeoutException { return flushResult; }
        @Override public List<DumpMetadata> fetchMetadata(int l) { return List.of(); }
        @Override public List<String> fetchKeys(int l) { return List.of(); }
    }

    static class ThrowingPort implements MemcachedPort {
        @Override public Map<SocketAddress, Map<String, String>> stats() { return Map.of(); }
        @Override public Map<SocketAddress, Map<String, String>> stats(String s) { return Map.of(); }
        @Override public void set(String key, int exp, Object v) {}
        @Override public Object get(String key) { return null; }
        @Override public Map<String, Object> getBulk(Collection<String> keys) { return Map.of(); }
        @Override public boolean flush(int t) throws ExecutionException, InterruptedException, TimeoutException {
            throw new ExecutionException("simulated failure", new RuntimeException());
        }
        @Override public List<DumpMetadata> fetchMetadata(int l) { return List.of(); }
        @Override public List<String> fetchKeys(int l) { return List.of(); }
    }
}
