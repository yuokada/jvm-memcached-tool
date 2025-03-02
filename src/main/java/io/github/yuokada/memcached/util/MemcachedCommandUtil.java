package io.github.yuokada.memcached.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MemcachedCommandUtil {
    public BufferedReader getReader(Socket socket) throws Exception {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
    }

    private OutputStream getWriter(Socket socket) throws Exception {
        return socket.getOutputStream();
    }

    public void sendCommand(Socket socket, String cmd) throws Exception {
        OutputStream out = getWriter(socket);
        out.write((cmd + "\r\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }
}
