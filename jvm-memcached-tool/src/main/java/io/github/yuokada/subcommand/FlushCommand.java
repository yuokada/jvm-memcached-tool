package io.github.yuokada.subcommand;


import static io.github.yuokada.MemcachedClientProvider.getMemcachedClient;

import jakarta.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import picocli.CommandLine;

@CommandLine.Command(name = "flush", description = "Flush items on memcached!")
public class FlushCommand extends BaseSubCommand {

    @Override
    public void run() {
        MemcachedClient client = null;
        try {
            client = getMemcachedClient(configEndpoint, clusterPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        OperationFuture<Boolean> flushResult = client.flush();
        try {
            if (flushResult.get(15, TimeUnit.SECONDS)) {
                System.out.printf("Keys on %s:%d are purged!%n", configEndpoint, clusterPort);
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
