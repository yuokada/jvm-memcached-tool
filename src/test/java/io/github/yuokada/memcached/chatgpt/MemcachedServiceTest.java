package io.github.yuokada.memcached.chatgpt;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.junit.QuarkusTest;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.Socket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@QuarkusTest
@ExtendWith(MockitoExtension.class)
public class MemcachedServiceTest {

    @Mock
    private Socket socket;

    @Mock
    private BufferedReader reader;

    @Mock
    private OutputStream writer;

    @InjectMocks
    private MemcachedService memcachedService;

    @BeforeEach
    public void setUp() throws Exception {
        when(socket.getInputStream()).thenReturn(mock(java.io.InputStream.class));
        when(socket.getOutputStream()).thenReturn(writer);
    }

    @Test
    public void testDisplay() throws Exception {
        when(reader.readLine()).thenReturn("STAT items:1:number 1", "END", "STAT 1:chunk_size 1024", "END");
        memcachedService.display();
        verify(writer).write("stats items\r\n".getBytes());
        verify(writer).write("stats slabs\r\n".getBytes());
    }

    @Test
    public void testStats() throws Exception {
        when(reader.readLine()).thenReturn("STAT pid 1234", "END");
        memcachedService.stats();
        verify(writer).write("stats\r\n".getBytes());
    }

    @Test
    public void testSettings() throws Exception {
        when(reader.readLine()).thenReturn("STAT maxbytes 67108864", "END");
        memcachedService.settings();
        verify(writer).write("stats settings\r\n".getBytes());
    }

    @Test
    public void testSizes() throws Exception {
        when(reader.readLine()).thenReturn("STAT 96 1", "END");
        memcachedService.sizes();
        verify(writer).write("stats sizes\r\n".getBytes());
    }

    @Test
    public void testDump() throws Exception {
        when(reader.readLine()).thenReturn("key=foo exp=2147483647", "END", "VALUE foo 0 3", "bar", "END", "END");
        memcachedService.dump(10);
        verify(writer).write("lru_crawler metadump all\r\n".getBytes());
        verify(writer).write("get foo\r\n".getBytes());
    }

    @Test
    public void testKeys() throws Exception {
        when(reader.readLine()).thenReturn("key=foo exp=2147483647", "END");
        memcachedService.keys(10);
        verify(writer).write("lru_crawler metadump all\r\n".getBytes());
    }
}