package io.github.yuokada.memcached.presentation.cli.subcommand;

import io.github.yuokada.memcached.presentation.cli.EntryCommand;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.ParentCommand;

@CommandLine.Command(name = "flush", description = "Flush items on memcached")
public class FlushCommand implements Callable<Integer> {

    @ParentCommand
    private EntryCommand entryCommand;

    @Override
    public Integer call() {
        MemcachedClient client = entryCommand.memcachedClient;

        OperationFuture<Boolean> flushResult = client.flush();
        try {
            if (flushResult.get(15, TimeUnit.SECONDS)) {
                System.out.printf("Keys on %s:%d are purged!%n", entryCommand.configEndpoint,
                    entryCommand.clusterPort);
                return ExitCode.OK;
            } else {
                System.err.println("Flush command failed. Please retry");
                return ExitCode.SOFTWARE;
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
