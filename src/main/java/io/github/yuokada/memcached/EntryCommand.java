package io.github.yuokada.memcached;

import io.github.yuokada.memcached.subcommand.FlushCommand;
import io.github.yuokada.memcached.subcommand.GenerateCommand;
import io.github.yuokada.memcached.subcommand.KeysCommand;
import io.github.yuokada.memcached.subcommand.StatsCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.concurrent.Callable;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

@TopCommand
@CommandLine.Command(name = "memcached-tool",
    subcommands = {
        GenerateCommand.class,
        KeysCommand.class,
        StatsCommand.class,
        FlushCommand.class
    },
    mixinStandardHelpOptions = true,
    version = "memcached-tool 0.1",
    description = "Simple tool to handle memcached")
public class EntryCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(EntryCommand.class);
    @Option(names = {"--host"}, description = "Cluster hostname.", defaultValue = "localhost",
        showDefaultValue = Visibility.ALWAYS)
    public String configEndpoint;
    @Option(names = {"--port", "-p"}, description = "Cluster port number.", defaultValue = "11211",
        showDefaultValue = Visibility.ALWAYS)
    public int clusterPort;
    @Option(
        names = {"-v", "--verbose"},
        description = "Enable verbose mode.",
        defaultValue = "false"
    )
    public boolean verbose;
    @Option(names = {"-V", "--version"},
        versionHelp = true,
        description = "print version information and exit")
    boolean versionRequested;

    @Option(names = {"--help", "-h"}, usageHelp = true)
    boolean help;

    public static void main(String[] args) throws IOException {
        int exitCode = new CommandLine(new EntryCommand()).execute(args);
        System.exit(exitCode);
    }

    @Inject
    public MemcachedClient memcachedClient;

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        // Quarkus.waitForExit();
        return ExitCode.OK;
    }
}
