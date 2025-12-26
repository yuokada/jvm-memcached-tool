package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.yuokada.memcached.application.usecase.StatsUseCase;
import jakarta.inject.Inject;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@CommandLine.Command(
    name = "stats",
    description = "Perform stats command. Pass an optional extra argument (items/settings/sizes)"
)
public class StatsCommand implements Callable<Integer> {

    @Option(names = {"--json"},
        description = "Flag to output with JSON format"
    )
    boolean jsonOutputFlag;

    @Parameters(arity = "0", paramLabel = "extra", defaultValue = "",
        description = "Optional stats scope: items, settings, or sizes")
    String operation;

    @Inject
    StatsUseCase statsUseCase;

    @Override
    public Integer call() {
        Map<SocketAddress, Map<String, String>> stats = statsUseCase.execute(operation);
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

}
