package io.github.yuokada.subcommand;

import static io.github.yuokada.MemcachedClientProvider.getMemcachedClient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.yuokada.EntryCommand;
import io.github.yuokada.StatsSubCommands;
import io.github.yuokada.util.FakeDataGenerator;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@CommandLine.Command(
    name = "generate",
    aliases = "gen",
    description = "Generate items on memcached!"
)
public class GenerateCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(GenerateCommand.class);

    @ParentCommand
    private EntryCommand entryCommand;

    @Option(
        names = {"--size"}, description = "item size to write. 0 is random size",
        defaultValue = "0"
    )
    int itemSize;

    @Option(names = {"--help", "-h"}, usageHelp = true)
    boolean help;

    @Option(names = {"--json"},
        description = "Flag to output with JSON format"
    )
    boolean jsonOutputFlag;

    @Override
    public Integer call() throws IOException {
        MemcachedClient client = getMemcachedClient(
            entryCommand.configEndpoint,
            entryCommand.clusterPort
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
        if (jsonOutputFlag) {
            stats.forEach((socketAddress, stat) -> {
                // filter out only item related stats
                stat.entrySet().removeIf(entry -> !entry.getKey().contains("item"));
            });

            Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
            System.out.println(gson.toJson(stats));
        } else {
            stats.forEach((socketAddress, stat) -> {
                stat
                    .forEach((k, v) -> {
                        if (k.contains("item")) {
                            System.out.printf("%s -> %s%n", k, v);
                        }
                    });
            });
        }

        return 0;
    }
}
