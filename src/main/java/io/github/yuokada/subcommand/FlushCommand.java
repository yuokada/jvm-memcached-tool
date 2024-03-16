package io.github.yuokada.subcommand;

import static io.github.yuokada.MemcachedClientProvider.getMemcachedClient;

import io.github.yuokada.EntryCommand;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import picocli.CommandLine;
import picocli.CommandLine.ParentCommand;

@CommandLine.Command(name = "flush_bang", description = "Flush items on memcached!")
public class FlushCommand implements Runnable {

    @ParentCommand
    private EntryCommand entryCommand;

    @Override
    public void run() {
        MemcachedClient client = null;
        try {
            client = getMemcachedClient(entryCommand.configEndpoint, entryCommand.clusterPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        OperationFuture<Boolean> flushResult = client.flush();
        try {
            if (flushResult.get(15, TimeUnit.SECONDS)) {
                System.out.printf("Keys on %s:%d are purged!%n", entryCommand.configEndpoint,
                    entryCommand.clusterPort);
                return;
            } else {
                System.err.println("Flush command failed. Please retry");
                return;
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
