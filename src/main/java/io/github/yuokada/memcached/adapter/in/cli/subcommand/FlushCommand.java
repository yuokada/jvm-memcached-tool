package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import io.github.yuokada.memcached.adapter.in.cli.EntryCommand;
import io.github.yuokada.memcached.application.usecase.FlushUseCase;
import jakarta.inject.Inject;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.ParentCommand;

@CommandLine.Command(name = "flush", description = "Flush items on memcached")
public class FlushCommand implements Callable<Integer> {

    @Inject
    FlushUseCase flushUseCase;
    @ParentCommand
    EntryCommand entryCommand;

    @Override
    public Integer call() {
        if (entryCommand != null) {
            entryCommand.printVerboseConnectionInfo();
        }
        try {
            boolean result = flushUseCase.execute();
            if (result) {
                String host = entryCommand != null ? entryCommand.getConfigEndpoint() : "unknown";
                int port = entryCommand != null ? entryCommand.getClusterPort() : 0;
                System.out.printf("Keys on %s:%d are purged!%n", host, port);
                return ExitCode.OK;
            }
            System.err.println("Flush command failed. Please retry");
            return ExitCode.SOFTWARE;
        } catch (IllegalStateException e) {
            System.err.printf(
                "Flush command failed due to an internal error: %s%n",
                e.getMessage()
            );
            return ExitCode.SOFTWARE;
        }
    }
}
