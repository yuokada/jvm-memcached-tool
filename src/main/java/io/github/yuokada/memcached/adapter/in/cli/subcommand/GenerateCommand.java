package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.yuokada.memcached.adapter.in.cli.EntryCommand;
import io.github.yuokada.memcached.application.usecase.GenerateUseCase;
import jakarta.inject.Inject;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
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
    @ParentCommand
    private EntryCommand entryCommand;

    @Override
    public Integer call() {
        GenerateUseCase.Result result = generateUseCase.execute(itemSize);
        int generatedCount = result.generatedCount();
        logger.debugf("Number of item size: %d", generatedCount);

        Map<SocketAddress, Map<String, String>> stats = result.stats();
        if (jsonOutputFlag) {
            Map<SocketAddress, Map<String, String>> filtered = stats.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().entrySet().stream()
                        .filter(entry -> entry.getKey().contains("item"))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                ));

            Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
            System.out.println(gson.toJson(filtered));
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
