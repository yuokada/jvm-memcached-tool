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

    @ParentCommand
    private EntryCommand entryCommand;
    @Inject
    FlushUseCase flushUseCase;

    @Override
    public Integer call() {
        boolean result = flushUseCase.execute();
        if (result) {
            System.out.printf("Keys on %s:%d are purged!%n",
                entryCommand.configEndpoint,
                entryCommand.clusterPort
            );
            return ExitCode.OK;
        }
        System.err.println("Flush command failed. Please retry");
        return ExitCode.SOFTWARE;
    }
}
