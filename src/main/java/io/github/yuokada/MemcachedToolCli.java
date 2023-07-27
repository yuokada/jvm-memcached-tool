package io.github.yuokada;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.ClientMode;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

@Command(name = "memcached-tool",
    mixinStandardHelpOptions = true,
    version = "memcached-tool 0.1",
    description = "Simple tool to handle memcached")
public class MemcachedToolCli implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(MemcachedToolCli.class);

    @Option(
        names = {"-v", "--verbose"},
        description = "Enable verbose mode.",
        defaultValue = "false"
    )
    private boolean verbose;

    @Option(names = {"-h", "--host"}, description = "Cluster hostname.", defaultValue = "localhost",
        showDefaultValue = Visibility.ALWAYS)
    private String configEndpoint;

    @Option(names = {"-p", "--port"}, description = "Cluster port number.", defaultValue = "11211",
        showDefaultValue = Visibility.ALWAYS)
    private int clusterPort;

    @Option(
        names = {"--execute-flush"},
        description = "Execute flush command before writing records",
        defaultValue = "false"
    )
    private boolean executeFlush;

    @Option(
        names = {"--size"},
        description = "item size to write. 0 is random size"
    )
    private int itemSize = 0;


    private static boolean isConfigEndpoint(String host) {
        return host.contains(".cfg.");
    }

    public static void main(String[] args) throws IOException {
        int exitCode = new CommandLine(new MemcachedToolCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        logger.debug(String.format("server: %s:%d", configEndpoint, clusterPort));
        InetSocketAddress address = new InetSocketAddress(configEndpoint, clusterPort);

        MemcachedClient client = getMemcachedClient(configEndpoint, address);
        logger.debug("is Configuration Initialized?: " + client.isConfigurationInitialized());
        logger.debug(
            "is Configuration protocol supported?: " + client.isConfigurationProtocolSupported()
        );
        if (executeFlush) {
            executeFlush(client);
        }

        if (itemSize == 0) {
            itemSize = FakeDataGenerator.getRandomNumber(1024);
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

    private MemcachedClient getMemcachedClient(String endpoint, InetSocketAddress address)
        throws IOException {
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

    private void executeFlush(MemcachedClient client) throws InterruptedException, ExecutionException {
        logger.info("Execute flush command");
        OperationFuture<Boolean> flush = client.flush();
        if (flush.get()) {
            logger.info("Succeeded flush command");
        } else {
            logger.info("Failed flush command");
        }
    }
}