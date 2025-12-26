package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.yuokada.memcached.application.usecase.DisplayUseCase;
import io.github.yuokada.memcached.application.usecase.DisplayUseCase.DisplayResult;
import io.github.yuokada.memcached.application.usecase.DisplayUseCase.SlabSummary;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

@Command(name = "display", description = "Display slab statistics")
public class DisplayCommand implements Callable<Integer> {

    @Option(names = {"--json"}, description = "Output as JSON")
    boolean jsonOutput;

    @Inject
    DisplayUseCase displayUseCase;

    @Override
    public Integer call() {
        try {
            List<DisplayResult> results = displayUseCase.execute();
            if (jsonOutput) {
                Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
                System.out.println(gson.toJson(results));
            } else {
                results.forEach(DisplayCommand::printResult);
            }
            return ExitCode.OK;
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            return ExitCode.SOFTWARE;
        }
    }

    private static void printResult(DisplayResult result) {
        System.out.printf("Server: %s%n", result.server());
        System.out.println("  #  Item_Size  Max_age   Pages   Count   Full?  Evicted Evict_Time OOM");
        result.slabs().forEach(DisplayCommand::printRow);
    }

    private static void printRow(SlabSummary summary) {
        String fullLabel = summary.full() ? "yes" : " no";
        System.out.printf("%3d %8s %9ds %7d %7d %7s %8d %10d %4d%n",
            summary.slabId(),
            summary.itemSize(),
            summary.maxAgeSeconds(),
            summary.pages(),
            summary.count(),
            fullLabel,
            summary.evicted(),
            summary.evictedTime(),
            summary.outOfMemory()
        );
    }
}
