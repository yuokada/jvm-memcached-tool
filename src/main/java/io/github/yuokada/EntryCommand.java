package io.github.yuokada;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.yuokada.subcommand.FlushCommand;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.ClientMode;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@QuarkusMain
@CommandLine.Command(name = "memcached-tool",
    subcommands = {
        FlushCommand.class
    },
    mixinStandardHelpOptions = true,
    version = "memcached-tool 0.1",
    description = "Simple tool to handle memcached")
public class EntryCommand implements QuarkusApplication, Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(EntryCommand.class);

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

    @Option(names = {"--host"}, description = "Cluster hostname.", defaultValue = "localhost",
        showDefaultValue = Visibility.ALWAYS)
    public static String configEndpoint;

    @Option(names = {"-p", "--port"}, description = "Cluster port number.", defaultValue = "11211",
        showDefaultValue = Visibility.ALWAYS)
    public static int clusterPort;


    private static boolean isConfigEndpoint(String host) {
        return host.contains(".cfg.");
    }

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

    @Command(name = "stats")
    public Integer stats(
        @Option(names = {"--json"},
            description = "Flag to output with JSON format"
        ) boolean jsonOutputFlag,
        @Parameters(arity = "1", paramLabel = "extra", defaultValue = "") String operation
    ) throws IOException {
        MemcachedClient client = getMemcachedClient(configEndpoint, clusterPort);
        Map<SocketAddress, Map<String, String>> stats = fetchStats(client, operation);
        Objects.requireNonNull(stats);

        if (jsonOutputFlag) {
            Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
            System.out.println(gson.toJson(stats));
        } else {
            List<String> lines = new ArrayList<>();
            stats.forEach((socketAddress, stat) -> {
                stat
                    .forEach((k, v) -> {
                        lines.add(String.format("%s -> %s", k, v));
                    });
            });
            lines.stream().sorted()
                .forEach(System.out::println);
        }
        return 0;
    }

    private Map<SocketAddress, Map<String, String>> fetchStats(
        MemcachedClient client,
        String args) {
        if (args.isEmpty()) {
            return client.getStats();
        } else {
            StatsSubCommands subcommand;
            try {
                subcommand = StatsSubCommands.valueOf(args);
            } catch (IllegalArgumentException e) {
                List<String> availableCommands = StatsSubCommands.availableCommands();
                String message = String.format(
                    "Unsupported extra command: %s\nAvailable commands: %s", args,
                    String.join(", ", availableCommands)
                );
                throw new IllegalArgumentException(message);
            }
            return client.getStats(subcommand.name());
        }
    }

    @Command(name = "flush")
    public Integer flush() throws IOException {
        MemcachedClient client = getMemcachedClient(configEndpoint, clusterPort);

        OperationFuture<Boolean> flushResult = client.flush();
        try {
            if (flushResult.get(15, TimeUnit.SECONDS)) {
                System.out.printf("Keys on %s:%d are purged!%n", configEndpoint, clusterPort);
                return 0;
            } else {
                System.err.println("Flush command failed. Please retry");
                return 1;
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
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

    @Override
    public int run(String... args) throws Exception {
        System.out.println("Hello world.");
        return 0;
    }
}
