import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FileBufferTest {

    private static final int BIG_BUFFER_SIZE = 6 * 1024;

    private static FileBuffer fileBuffer;
    private static byte[] buf1 = new byte[BIG_BUFFER_SIZE];
    private static byte[] buf2 = new byte[BIG_BUFFER_SIZE];

    @BeforeAll
    static void initialize() {
        try {
            fileBuffer = new FileBuffer("temp-test");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void storeSmallContent() {
        String message = "Hello World";
        InputStream messageStream1 = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
        InputStream expectedInputStream = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));

        int len1 = 0;
        int len2 = 0;
        try {
            fileBuffer.setStream(messageStream1);
            InputStream inputStream = fileBuffer.getStream();

            while (len1 != -1) {
                len1 = inputStream.read(buf1, len1, BIG_BUFFER_SIZE - len1);
            }

            while (len2 != -1) {
                len2 = expectedInputStream.read(buf2, len2, BIG_BUFFER_SIZE - len2);
            }
        } catch (IOException e) {
            fail();
        }

        assertEquals(len1, len2);
        assertEquals(0,
                Arrays.compare(buf1, 0, message.length(), buf2, 0, message.length()));
    }

    @Test
    void storeLargeContent() {
        byte[] message = new byte[BIG_BUFFER_SIZE];
        message[0] = 12;
        message[message.length - 1] = 123;
        InputStream messageStream1 = new ByteArrayInputStream(message);
        InputStream expectedInputStream = new ByteArrayInputStream(message);

        int len1 = 0;
        int len2 = 0;
        try {
            fileBuffer.setStream(messageStream1);
            InputStream inputStream = fileBuffer.getStream();

            while (len1 != -1) {
                len1 = inputStream.read(buf1, len1, BIG_BUFFER_SIZE - len1);
            }

            while (len2 != -1) {
                len2 = expectedInputStream.read(buf2, len2, BIG_BUFFER_SIZE - len2);
            }
        } catch (IOException e) {
            fail();
        }

        assertEquals(len1, len2);
        assertEquals(0,
                Arrays.compare(buf1, 0, message.length, buf2, 0, message.length));
    }
}