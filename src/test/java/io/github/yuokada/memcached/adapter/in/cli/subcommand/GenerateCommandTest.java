package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.yuokada.memcached.application.port.DataGeneratorPort;
import io.github.yuokada.memcached.application.port.MemcachedPort;
import io.github.yuokada.memcached.application.usecase.GenerateUseCase;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.ExitCode;

class GenerateCommandTest {

    @Test
    void callReturnsOkWithDefaultFlags() throws Exception {
        GenerateCommand cmd = new GenerateCommand();
        cmd.generateUseCase = new GenerateUseCase(new StubPort(), new StubGenerator(3));
        cmd.itemSize = 3;

        assertEquals(ExitCode.OK, cmd.call());
    }

    @Test
    void callReturnsOkWithJsonFlag() throws Exception {
        GenerateCommand cmd = new GenerateCommand();
        cmd.generateUseCase = new GenerateUseCase(new StubPort(), new StubGenerator(2));
        cmd.itemSize = 2;
        cmd.jsonOutputFlag = true;

        assertEquals(ExitCode.OK, cmd.call());
    }

    @Test
    void callWithNoEntryCommandDoesNotThrow() throws Exception {
        GenerateCommand cmd = new GenerateCommand();
        cmd.generateUseCase = new GenerateUseCase(new StubPort(), new StubGenerator(1));
        cmd.itemSize = 1;
        cmd.entryCommand = null;

        assertEquals(ExitCode.OK, cmd.call());
    }

    static class StubPort implements MemcachedPort {
        final List<String> keys = new ArrayList<>();

        @Override public Map<SocketAddress, Map<String, String>> stats() { return Map.of(); }
        @Override public Map<SocketAddress, Map<String, String>> stats(String s) { return Map.of(); }
        @Override public void set(String key, int exp, Object value) { keys.add(key); }
        @Override public Object get(String key) { return null; }
        @Override public boolean flush(int t) throws ExecutionException, InterruptedException, TimeoutException { return true; }
        @Override public List<DumpMetadata> fetchMetadata(int l) { return List.of(); }
        @Override public List<String> fetchKeys(int l) { return List.of(); }
    }

    static class StubGenerator implements DataGeneratorPort {
        private final int size;
        StubGenerator(int size) { this.size = size; }
        @Override public int randomSize() { return size; }
        @Override public String fullName() { return "Test User"; }
    }
}
