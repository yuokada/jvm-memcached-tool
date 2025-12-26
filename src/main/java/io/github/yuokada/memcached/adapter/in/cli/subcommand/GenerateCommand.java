package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.yuokada.memcached.adapter.in.cli.EntryCommand;
import io.github.yuokada.memcached.application.usecase.GenerateUseCase;
import jakarta.inject.Inject;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.jboss.logging.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@CommandLine.Command(
    name = "generate",
    aliases = "gen",
    description = "Generate items on memcached!"
)
public class GenerateCommand implements Callable<Integer> {

    private static final Logger logger = Logger.getLogger(GenerateCommand.class);

    @ParentCommand
    private EntryCommand entryCommand;
    @Inject
    GenerateUseCase generateUseCase;

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
    public Integer call() {
        GenerateUseCase.Result result = generateUseCase.execute(itemSize);
        int generatedCount = result.generatedCount();
        logger.debug(String.format("Number of item size: %d", generatedCount));

        Map<SocketAddress, Map<String, String>> stats = result.stats();
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
