package io.github.yuokada;

import io.github.yuokada.subcommand.FlushCommand;
import io.github.yuokada.subcommand.GenerateCommand;
import io.github.yuokada.subcommand.StatsCommand;
import io.github.yuokada.util.FakeDataGenerator;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.sql.DataSource;
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

@TopCommand
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
public class EntryCommand implements QuarkusApplication, Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(EntryCommand.class);
    @Option(names = {"--host"}, description = "Cluster hostname.", defaultValue = "localhost",
        showDefaultValue = Visibility.ALWAYS)
    @Named("host")
    public String configEndpoint;
    @Option(names = {"-p", "--port"}, description = "Cluster port number.", defaultValue = "11211",
        showDefaultValue = Visibility.ALWAYS)
    @Named("port")
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

    // NOTE: Now testing
    @CommandLine.Option(names = "-c", description = "JDBC connection string")
    String connectionString;

    @Inject
    @Named("pg")
    public DataSource dataSource;

    public static void main(String[] args) throws IOException {
        logger.warn("Called main method");
        int exitCode = new CommandLine(new EntryCommand()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        // Quarkus.waitForExit();
        return ExitCode.OK;
    }

    @Inject
    CommandLine.IFactory factory;

    public static void run() throws IOException {
        logger.warn("Called run method");
    }

    @Override
    public int run(String... args) throws Exception {
        logger.warn("Called run-with-int method");
        return new CommandLine(this, factory).execute(args);
    }

    // Sub-commands
    @Inject
    MemcachedService memcachedService;

    @Command(name = "foo", description = "TBD")
    public Integer fooSub() {
        logger.warn("Called fooSub method");
        System.out.println(this);
        System.out.println("Called fooSub method");
        return 0;
    }

}
