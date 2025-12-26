package io.github.yuokada.memcached.adapter.out.memcached;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collection;
import net.spy.memcached.MemcachedClient;

/**
 * Abstract base class for Memcached adapters that communicate via raw sockets.
 * Provides common functionality for resolving endpoints and executing commands.
 */
public abstract class AbstractMemcachedSocketAdapter {

    protected final MemcachedClient memcachedClient;

    /**
     * No-args constructor required for CDI proxying.
     */
    protected AbstractMemcachedSocketAdapter() {
        this.memcachedClient = null;
    }

    protected AbstractMemcachedSocketAdapter(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
    }

    /**
     * Resolves the first available Memcached server endpoint.
     *
     * @return the InetSocketAddress of an available server
     * @throws IllegalStateException if no servers are available
     */
    protected InetSocketAddress resolveEndpoint() {
        Collection<SocketAddress> servers = memcachedClient.getAvailableServers();
        return servers.stream()
            .filter(InetSocketAddress.class::isInstance)
            .map(InetSocketAddress.class::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No available memcached servers"));
    }

    /**
     * Executes a Memcached command via raw socket and processes the response.
     *
     * @param command           the command to send to Memcached
     * @param responseProcessor the processor to handle the response lines
     * @param <T>               the type of result returned by the processor
     * @return the result from the processor
     * @throws IllegalStateException if the socket communication fails
     */
    protected <T> T executeCommand(String command, ResponseProcessor<T> responseProcessor) {
        InetSocketAddress endpoint = resolveEndpoint();
        try (Socket socket = new Socket(endpoint.getHostString(), endpoint.getPort());
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            var writer = new OutputStreamWriter(socket.getOutputStream())) {

            writer.write(command);
            writer.flush();

            return responseProcessor.process(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute command: " + command, e);
        }
    }

    /**
     * Functional interface for processing Memcached response lines.
     *
     * @param <T> the type of result to return
     */
    @FunctionalInterface
    protected interface ResponseProcessor<T> {

        T process(BufferedReader reader) throws IOException;
    }
}
