package io.github.yuokada.memcached.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class KeysUseCase {

    private static final Pattern PATTERN = Pattern.compile("^key=(\\S+) exp=(-?\\d+) .*");

    public List<String> execute(String host, int port, int limit) {
        try (Socket socket = new Socket(host, port);
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            var writer = new OutputStreamWriter(socket.getOutputStream())) {

            writer.write("lru_crawler metadump all\r\n");
            writer.flush();

            List<String> results = new ArrayList<>();
            int counter = 0;
            String response;
            while ((response = reader.readLine()) != null) {
                if (limit > 0 && counter >= limit) {
                    break;
                }
                if ("END".equals(response)) {
                    break;
                }
                if (PATTERN.matcher(response).matches()) {
                    results.add(response);
                    counter++;
                }
            }
            return results;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch keys from memcached", e);
        }
    }
}
