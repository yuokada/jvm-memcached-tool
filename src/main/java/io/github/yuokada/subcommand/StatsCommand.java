package io.github.yuokada.subcommand;

import static io.github.yuokada.MemcachedClientProvider.getMemcachedClient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.yuokada.EntryCommand;
import io.github.yuokada.StatsSubCommands;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import net.spy.memcached.MemcachedClient;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@CommandLine.Command(name = "stats", description = "TBD")
public class StatsCommand implements Callable<Integer> {

    @ParentCommand
    private EntryCommand entryCommand;

    @Option(names = {"--json"},
        description = "Flag to output with JSON format"
    )
    boolean jsonOutputFlag;

    @Parameters(arity = "0", paramLabel = "extra", defaultValue = "")
    String operation;

    @Override
    public Integer call() throws IOException {
        MemcachedClient client = getMemcachedClient(
            entryCommand.configEndpoint, entryCommand.clusterPort);
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
}
