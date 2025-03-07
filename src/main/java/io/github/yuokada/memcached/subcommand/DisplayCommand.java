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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@CommandLine.Command(name = "display", description = "Shows settings stats")
public class DisplayCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(DisplayCommand.class);

    @ParentCommand
    private EntryCommand entryCommand;

    @Option(names = {"--json"}, description = "Flag to output with JSON format")
    boolean jsonOutputFlag;

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

        DisplayResult result = null;
        try (Socket socket = new Socket()) {
            socket.connect(socketAddress);
            MemcachedCommandUtil commandUtil = new MemcachedCommandUtil();
            commandUtil.sendCommand(socket, "stats sizes");
            BufferedReader reader = commandUtil.getReader(socket);
            String line;
            Map<String, Map<String, Integer>> items = new LinkedHashMap<>();
            while ((line = reader.readLine()) != null && !line.equals("END")) {
                if (line.startsWith("STAT items:")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 3) {
                        String slabInfo = parts[1];
                        String[] slabParts = slabInfo.split(":");
                        if (slabParts.length == 2 || slabParts.length == 3) {
                            String slab = slabParts[1];
                            String field = slabParts.length == 3 ? slabParts[2] : "value";
                            int value = Integer.parseInt(parts[2]);
                            items.computeIfAbsent(slab, k -> new LinkedHashMap<>()).put(field, value);
                        }
                    }
                }
            }
            // Now get slab stats
            commandUtil.sendCommand(socket, "stats slabs");
            BufferedReader slabReader = commandUtil.getReader(socket);
            int max = 0;
            while ((line = slabReader.readLine()) != null && !line.equals("END")) {
                if (line.startsWith("STAT")) {
                    String[] parts = line.split("\\s+");
                    // Expect: STAT <slab>:<field> <value>
                    if (parts.length >= 3) {
                        String[] keyParts = parts[1].split(":");
                        if (keyParts.length == 2) {
                            int slab = Integer.parseInt(keyParts[0]);
                            items.computeIfAbsent(String.valueOf(slab), k -> new LinkedHashMap<>())
                                .put(keyParts[1], Integer.parseInt(parts[2]));
                            if (slab > max) {
                                max = slab;
                            }
                        }
                    }
                }
            }
            result = new DisplayResult(socketAddress.toString(), items);
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
            // TODO: Implement this
//            List<String> lines = new ArrayList<>();
//            result.settings.forEach((k, v) -> {
//                lines.add(String.format("%24s %12s", k, v));
//            });
//            lines.forEach(System.out::println);
        }
        return 0;
    }

    public record DisplayResult(String address, Map<String, Map<String, Integer>> items) {

    }

//    public void display() throws Exception {
//        Socket socket = connect();
//        // Get items stats and slab stats
//        sendCommand(socket, "stats items");
//        Map<String, Map<String, Integer>> items = new LinkedHashMap<>();
//        BufferedReader reader = getReader(socket);
//        String line;
//        while ((line = reader.readLine()) != null && !line.equals("END")) {
//            // Parse: STAT items:<slab>:<field> <value>
//            if (line.startsWith("STAT items:")) {
//                String[] parts = line.split("\\s+");
//                if (parts.length >= 3) {
//                    String slabInfo = parts[1];
//                    String[] slabParts = slabInfo.split(":");
//                    if (slabParts.length == 2 || slabParts.length == 3) {
//                        String slab = slabParts[1];
//                        String field = slabParts.length == 3 ? slabParts[2] : "value";
//                        int value = Integer.parseInt(parts[2]);
//                        items.computeIfAbsent(slab, k -> new LinkedHashMap<>()).put(field, value);
//                    }
//                }
//            }
//        }
//        // Now get slab stats
//        sendCommand(socket, "stats slabs");
//        int max = 0;
//        while ((line = reader.readLine()) != null && !line.equals("END")) {
//            if (line.startsWith("STAT")) {
//                String[] parts = line.split("\\s+");
//                // Expect: STAT <slab>:<field> <value>
//                if (parts.length >= 3) {
//                    String[] keyParts = parts[1].split(":");
//                    if (keyParts.length == 2) {
//                        int slab = Integer.parseInt(keyParts[0]);
//                        items.computeIfAbsent(String.valueOf(slab), k -> new LinkedHashMap<>())
//                            .put(keyParts[1], Integer.parseInt(parts[2]));
//                        if (slab > max) {
//                            max = slab;
//                        }
//                    }
//                }
//            }
//        }
//        // Display results (simplified table output)
//        System.out.print("  #  Item_Size  Max_age   Pages   Count   Full?  Evicted Evict_Time OOM\n");
//        for (int i = 1; i <= max; i++) {
//            Map<String, Integer> it = items.get(String.valueOf(i));
//            if (it == null || it.getOrDefault("total_pages", 0) == 0) {
//                continue;
//            }
//            int chunkSize = it.getOrDefault("chunk_size", 0);
//            String sizeStr = chunkSize < 1024 ? chunkSize + "B" : String.format("%.1fK", chunkSize / 1024.0);
//            String full = it.getOrDefault("used_chunks", 0).equals(it.getOrDefault("total_chunks", 0)) ? "yes" : " no";
//            System.out.printf("%3d %8s %9ds %7d %7d %7s %8d %8d %4d\n",
//                i,
//                sizeStr,
//                it.getOrDefault("age", 0),
//                it.getOrDefault("total_pages", 0),
//                it.getOrDefault("number", 0),
//                full,
//                it.getOrDefault("evicted", 0),
//                it.getOrDefault("evicted_time", 0),
//                it.getOrDefault("outofmemory", 0));
//        }
//        socket.close();
//    }
}
