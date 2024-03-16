package io.github.yuokada;

import io.github.yuokada.subcommand.FlushCommand;
import io.github.yuokada.subcommand.GenerateCommand;
import io.github.yuokada.subcommand.StatsCommand;
import io.github.yuokada.util.FakeDataGenerator;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.ClientMode;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

@QuarkusMain
@CommandLine.Command(name = "memcached-tool",
    subcommands = {
        GenerateCommand.class,
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
    @Option(names = {"-p", "--port"}, description = "Cluster port number.", defaultValue = "11211",
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

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        Quarkus.waitForExit();
        return ExitCode.OK;
    }
}
