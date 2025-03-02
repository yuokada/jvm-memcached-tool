package io.github.yuokada.memcached.subcommand;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.yuokada.memcached.EntryCommand;
import io.github.yuokada.memcached.util.MemcachedCommandUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@CommandLine.Command(name = "settings", description = "Shows settings stats")
public class SettingsCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(SettingsCommand.class);

    @ParentCommand
    private EntryCommand entryCommand;

    @Option(names = {"--json"}, description = "Flag to output with JSON format")
    boolean jsonOutputFlag;

    @Option(names = {"--sort"}, description = "Sort by field name")
    Boolean sortKeys = false;

    @Override
    public Integer call() throws IOException {
        MemcachedClient client = entryCommand.memcachedClient;

        client.getVersions().forEach((key, value) -> logger.debug(key + " : " + value));
        var servers = client.getAvailableServers().stream().toList();
        if (servers.isEmpty()) {
            logger.error("No available servers");
            return ExitCode.SOFTWARE;
        }
        SocketAddress socketAddress = servers.get(0);

        SettingsResult result;
        try (Socket socket = new Socket()) {
            socket.connect(socketAddress);
            MemcachedCommandUtil commandUtil = new MemcachedCommandUtil();
            commandUtil.sendCommand(socket, "stats settings");

            BufferedReader reader = commandUtil.getReader(socket);
            String line;
            Map<String, String> settings = new HashMap<>();
            while ((line = reader.readLine()) != null && !line.equals("END")) {
                if (line.startsWith("STAT")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 3) {
                        settings.put(parts[1], parts[2]);
                    }
                }
            }
            result = new SettingsResult(socketAddress.toString(), settings);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (jsonOutputFlag) {
            Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
            System.out.println(gson.toJson(result));
        } else {
            List<String> lines = new ArrayList<>();
            if (sortKeys) {
                Collection<String> keys = result.settings.keySet();
                keys.stream().sorted().forEach(k -> {
                    lines.add(String.format("%24s %12s", k, result.settings.get(k)));
                });
            } else {
                result.settings.forEach((k, v) -> {
                    lines.add(String.format("%24s %12s", k, v));
                });
            }

            System.out.printf("#%-17s %5s %11s\n", socketAddress, "Field", "Value");
            lines.forEach(System.out::println);
        }
        return 0;
    }

    public record SettingsResult(
        String server,
        Map<String, String> settings
    ) {
    }
}
