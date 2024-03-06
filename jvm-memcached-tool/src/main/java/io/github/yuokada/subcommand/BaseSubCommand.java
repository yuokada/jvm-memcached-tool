package io.github.yuokada.subcommand;

import picocli.CommandLine;
import picocli.CommandLine.Help.Visibility;

public abstract class BaseSubCommand implements Runnable {
    @CommandLine.Option(
        names = {"-v", "--verbose"},
        description = "Enable verbose mode.",
        defaultValue = "false"
    )
    boolean verbose;

    @CommandLine.Option(names = {
        "--host"}, description = "Cluster hostname.", defaultValue = "localhost",
        showDefaultValue = Visibility.ALWAYS)
    static String configEndpoint;

    @CommandLine.Option(names = {"-p",
        "--port"}, description = "Cluster port number.", defaultValue = "11211",
        showDefaultValue = Visibility.ALWAYS)
    static int clusterPort;

}
