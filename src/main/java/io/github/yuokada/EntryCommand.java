package io.github.yuokada;

import io.github.yuokada.subcommand.FlushCommand;
import io.github.yuokada.subcommand.StatsCommand;
import io.github.yuokada.util.FakeDataGenerator;
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
    @Option(names = "--help", usageHelp = true, description = "display this help and exit")
    boolean help;

    public static void main(String[] args) throws IOException {
        int exitCode = new CommandLine(new EntryCommand()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return ExitCode.OK;
    }

    @Command(name = "generate")
    public Integer generate(
        @Option(
            names = {"--size"}, description = "item size to write. 0 is random size",
            defaultValue = "0"
        )
        int itemSize
    ) throws IOException {
        MemcachedClient client = getMemcachedClient(configEndpoint, clusterPort);
        logger.debug("is Configuration Initialized?: " + client.isConfigurationInitialized());
        logger.debug(
            "is Configuration protocol supported?: " + client.isConfigurationProtocolSupported()
        );

        if (itemSize == 0) {
            itemSize = FakeDataGenerator.getRandomNumber();
            logger.debug(String.format("Number of item size: %d", itemSize));
        }
        for (int i = 0; i < itemSize; i++) {
            client.set(String.format("key_%d", i), 3600, FakeDataGenerator.getFullName());
        }

        Map<SocketAddress, Map<String, String>> stats = client.getStats();
        Objects.requireNonNull(stats);
        stats.forEach((socketAddress, stat) -> {
            stat
                .forEach((k, v) -> {
                    if (k.contains("item")) {
                        System.out.printf("%s -> %s%n", k, v);
                    }
                });
        });
        return 0;
    }

    private MemcachedClient getMemcachedClient(String endpoint, Integer clusterPort)
        throws IOException {
        logger.debug(String.format("server: %s:%d", configEndpoint, clusterPort));
        InetSocketAddress address = new InetSocketAddress(configEndpoint, clusterPort);
        MemcachedClient client;
        if (isConfigEndpoint(endpoint)) {
            client = new MemcachedClient(
                new BinaryConnectionFactory(ClientMode.Dynamic),
                List.of(address)
            );
        } else {
            client = new MemcachedClient(
                new BinaryConnectionFactory(ClientMode.Static),
                List.of(address)
            );
        }
        return client;
    }

    private static boolean isConfigEndpoint(String host) {
        return host.contains(".cfg.");
    }
}
