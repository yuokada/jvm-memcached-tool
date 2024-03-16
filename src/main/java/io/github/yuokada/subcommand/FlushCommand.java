package io.github.yuokada.subcommand;

import static io.github.yuokada.MemcachedClientProvider.getMemcachedClient;

import io.github.yuokada.EntryCommand;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Dependent
@CommandLine.Command(name = "flush", description = "Flush items on memcached!")
public class FlushCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(FlushCommand.class);

    @ParentCommand
    private EntryCommand entryCommand;

    @Inject
    MemcachedClient iClient;

    @Option(names = {"--help", "-h"}, usageHelp = true)
    boolean help;

    @Override
    public Integer call() {
        MemcachedClient client = null;
        client = iClient;
        logger.error("client: {}", client);
        System.err.println(String.format("client: {}", client));
        System.err.println(String.format("ds: {}", entryCommand.dataSource));
//        try {
//            client = getMemcachedClient(entryCommand.configEndpoint, entryCommand.clusterPort);
//        } catch (IOException e) {
//            return ExitCode.SOFTWARE;
//            // throw new RuntimeException(e);
//        }

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
