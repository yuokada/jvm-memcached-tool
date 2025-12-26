package io.github.yuokada.memcached.adapter.in.cli.subcommand;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.yuokada.memcached.application.usecase.SizesUseCase;
import io.github.yuokada.memcached.application.usecase.SizesUseCase.SizeCount;
import io.github.yuokada.memcached.application.usecase.SizesUseCase.SizesResult;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

@Command(name = "sizes", description = "Display item size histogram")
public class SizesCommand implements Callable<Integer> {

    @Option(names = {"--json"}, description = "Output as JSON")
    boolean jsonOutput;

    @Inject
    SizesUseCase sizesUseCase;

    @Override
    public Integer call() {
        try {
            List<SizesResult> results = sizesUseCase.execute();
            if (jsonOutput) {
                Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
                System.out.println(gson.toJson(results));
            } else {
                results.forEach(SizesCommand::printResult);
            }
            return ExitCode.OK;
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            return ExitCode.SOFTWARE;
        }
    }

    private static void printResult(SizesResult result) {
        System.out.printf("#%-17s %5s %11s%n", result.server(), "Size", "Count");
        result.entries().forEach(SizesCommand::printLine);
    }

    private static void printLine(SizeCount entry) {
        System.out.printf("%24d %12s%n", entry.sizeBytes(), entry.count());
    }
}
