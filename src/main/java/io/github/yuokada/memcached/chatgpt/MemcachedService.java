package io.github.yuokada.memcached.chatgpt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemcachedService {

    private final String address;

    public MemcachedService(String address) {
        this.address = address;
    }

    private Socket connect() throws Exception {
        if (address.contains("/")) {
            // Assuming a Unix Domain Socket; Java 16+ supports Unix domain sockets.
            UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(address);
            SocketChannel channel = SocketChannel.open(socketAddress);
            return channel.socket();
        } else {
            String host;
            int port = 11211; // default port
            if (address.contains(":")) {
                String[] parts = address.split(":");
                host = parts[0];
                port = Integer.parseInt(parts[1]);
            } else {
                host = address;
            }
            return new Socket(host, port);
        }
    }

    private BufferedReader getReader(Socket socket) throws Exception {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
    }

    private OutputStream getWriter(Socket socket) throws Exception {
        return socket.getOutputStream();
    }

    public void display() throws Exception {
        Socket socket = connect();
        // Get items stats and slab stats
        sendCommand(socket, "stats items");
        Map<String, Map<String, Integer>> items = new LinkedHashMap<>();
        BufferedReader reader = getReader(socket);
        String line;
        while ((line = reader.readLine()) != null && !line.equals("END")) {
            // Parse: STAT items:<slab>:<field> <value>
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
        sendCommand(socket, "stats slabs");
        int max = 0;
        while ((line = reader.readLine()) != null && !line.equals("END")) {
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
        // Display results (simplified table output)
        System.out.print("  #  Item_Size  Max_age   Pages   Count   Full?  Evicted Evict_Time OOM\n");
        for (int i = 1; i <= max; i++) {
            Map<String, Integer> it = items.get(String.valueOf(i));
            if (it == null || it.getOrDefault("total_pages", 0) == 0) {
                continue;
            }
            int chunkSize = it.getOrDefault("chunk_size", 0);
            String sizeStr = chunkSize < 1024 ? chunkSize + "B" : String.format("%.1fK", chunkSize / 1024.0);
            String full = it.getOrDefault("used_chunks", 0).equals(it.getOrDefault("total_chunks", 0)) ? "yes" : " no";
            System.out.printf("%3d %8s %9ds %7d %7d %7s %8d %8d %4d\n",
                i,
                sizeStr,
                it.getOrDefault("age", 0),
                it.getOrDefault("total_pages", 0),
                it.getOrDefault("number", 0),
                full,
                it.getOrDefault("evicted", 0),
                it.getOrDefault("evicted_time", 0),
                it.getOrDefault("outofmemory", 0));
        }
        socket.close();
    }

    public void stats() throws Exception {
        Socket socket = connect();
        sendCommand(socket, "stats");
        BufferedReader reader = getReader(socket);
        System.out.printf("#%-22s %5s %13s\n", address, "Field", "Value");
        String line;
        while ((line = reader.readLine()) != null && !line.equals("END")) {
            if (line.startsWith("STAT")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    System.out.printf("%29s %14s\n", parts[1], parts[2]);
                }
            }
        }
        socket.close();
    }

    public void settings() throws Exception {
        Socket socket = connect();
        sendCommand(socket, "stats settings");
        BufferedReader reader = getReader(socket);
        System.out.printf("#%-17s %5s %11s\n", address, "Field", "Value");
        String line;
        while ((line = reader.readLine()) != null && !line.equals("END")) {
            if (line.startsWith("STAT")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    System.out.printf("%24s %12s\n", parts[1], parts[2]);
                }
            }
        }
        socket.close();
    }

    public void sizes() throws Exception {
        Socket socket = connect();
        sendCommand(socket, "stats sizes");
        BufferedReader reader = getReader(socket);
        System.out.printf("#%-17s %5s %11s\n", address, "Size", "Count");
        String line;
        while ((line = reader.readLine()) != null && !line.equals("END")) {
            if (line.startsWith("STAT")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    System.out.printf("%24s %12s\n", parts[1], parts[2]);
                }
            }
        }
        socket.close();
    }

    public void dump(Integer limit) throws Exception {
        Socket socket = connect();
        // Issue metadump command
        String limText = (limit != null) ? (" (limiting to " + limit + " keys)") : "";
        System.err.println("Dumping memcache contents" + limText);
        sendCommand(socket, "lru_crawler metadump all");
        BufferedReader reader = getReader(socket);
        Map<String, Integer> keyExp = new LinkedHashMap<>();
        String line;
        int keyCount = 0;
        while ((line = reader.readLine()) != null) {
            if (line.equals("END") || (limit != null && keyCount == limit)) {
                break;
            }
            if (line.startsWith("key=")) {
                // Parse a line like: key=foo exp=2147483647 la=...
                String[] parts = line.split("\\s+");
                String key = null;
                int exp = 0;
                for (String part : parts) {
                    if (part.startsWith("key=")) {
                        key = part.substring(4);
                        // Decode hex escapes
                        key = decodeHexEscapes(key);
                    } else if (part.startsWith("exp=")) {
                        exp = Integer.parseInt(part.substring(4));
                        if (exp == -1) {
                            exp = 0;
                        }
                    }
                }
                if (key != null) {
                    keyExp.put(key, exp);
                }
            }
            keyCount++;
        }
        // In case limit was reached, reopen the connection (simplified)
        if (limit != null && keyCount == limit) {
            socket.close();
            socket = connect();
            reader = getReader(socket);
        }
        // For each key, get its value.
        for (String k : keyExp.keySet()) {
            sendCommand(socket, "get " + k);
            String response = reader.readLine();
            if (response != null && response.startsWith("VALUE")) {
                String[] parts = response.split("\\s+");
                if (parts.length >= 4) {
                    int flags = Integer.parseInt(parts[2]);
                    int len = Integer.parseInt(parts[3]);
                    char[] buf = new char[len];
                    reader.read(buf, 0, len);
                    // Consume the trailing lines ("END")
                    reader.readLine();
                    reader.readLine();
                    System.out.println("add " + k + " " + flags + " " + keyExp.get(k) + " " + len);
                    System.out.println(new String(buf));
                }
            }
        }
        socket.close();
    }

    /**
     * Helper method to decode hex escapes like %20 in the input string.
     */
    private String decodeHexEscapes(String input) {
        Pattern pattern = Pattern.compile("%([0-9a-fA-F]{2})");
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int hex = Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(Character.toString((char) hex)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public void keys(Integer limit) throws Exception {
        Socket socket = connect();
        String limText = (limit != null) ? (" (limiting to " + limit + " keys)") : "";
        System.err.println("Dumping memcache keys" + limText);
        sendCommand(socket, "lru_crawler metadump all");
        BufferedReader reader = getReader(socket);
        int keyCount = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals("END") || (limit != null && keyCount == limit)) {
                break;
            }
            if (line.startsWith("key=")) {
                System.out.println(line);
            }
            keyCount++;
        }
        socket.close();
    }

    private void sendCommand(Socket socket, String cmd) throws Exception {
        OutputStream out = getWriter(socket);
        out.write((cmd + "\r\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }
}
