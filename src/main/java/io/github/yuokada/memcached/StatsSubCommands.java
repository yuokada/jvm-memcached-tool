package io.github.yuokada.memcached;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum StatsSubCommands {
    sizes("sizes"),
    items("items"),
    settings("settings");

    private final String name;

    StatsSubCommands(String name) {
        this.name = name;
    }

    public static List<String> availableCommands() {
        return Stream.of(
            sizes,
            items,
            settings
        ).map(Enum::name).collect(Collectors.toList());
    }
}
