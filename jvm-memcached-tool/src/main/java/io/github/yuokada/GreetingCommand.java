package io.github.yuokada;

import io.github.yuokada.subcommand.FlushCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


@TopCommand
@CommandLine.Command(
    subcommands = {FlushCommand.class},
    name = "memcached-tool",
    mixinStandardHelpOptions = true)
public class GreetingCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(GreetingCommand.class);

    @CommandLine.Option(
        names = {"-v", "--verbose"},
        description = "Enable verbose mode.",
        defaultValue = "false"
    )
    private boolean verbose;

    @CommandLine.Option(names = {
        "--host"}, description = "Cluster hostname.", defaultValue = "localhost",
        showDefaultValue = Visibility.ALWAYS)
    private static String configEndpoint;

    @CommandLine.Option(names = {"-p",
        "--port"}, description = "Cluster port number.", defaultValue = "11211",
        showDefaultValue = Visibility.ALWAYS)
    private static int clusterPort;

    @CommandLine.Option(
        names = {"--size"},
        description = "item size to write. 0 is random size"
    )
    private int itemSize = 0;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    String name = "picocli";

    @Override
    public void run() {
        System.out.printf("Hello %s!\n", name);
        System.out.println("Spec is: " + spec.name());
    }

}
