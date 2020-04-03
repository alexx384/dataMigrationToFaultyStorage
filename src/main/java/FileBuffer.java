import java.io.*;

public class FileBuffer {
    private static final int DEFAULT_BUFFER_CHUNK_SIZE = 4096;
    private static final int DEFAULT_DATA_LENGTH = 0;
    private final byte[] buffer;
    private int dataLength;
    private final File tempFile;

    public FileBuffer(String tempFileName) throws IOException {
        buffer = new byte[DEFAULT_BUFFER_CHUNK_SIZE];
        dataLength = DEFAULT_DATA_LENGTH;
        tempFile = File.createTempFile(tempFileName, ".tmp");
    }

    public InputStream getStream() {
        if (dataLength > DEFAULT_BUFFER_CHUNK_SIZE) {
            try {
                return new FileInputStream(tempFile);
            } catch (IOException e) {
                return InputStream.nullInputStream();
            }
        } else {
            return new ByteArrayInputStream(buffer, 0, dataLength);
        }
    }

    public void setStream(InputStream stream) throws IOException {
        int readed = stream.read(buffer, 0, DEFAULT_BUFFER_CHUNK_SIZE);
        int readedPortion = readed;
        while (readedPortion != -1 && readed != DEFAULT_BUFFER_CHUNK_SIZE) {
            readedPortion = stream.read(buffer, readed, DEFAULT_BUFFER_CHUNK_SIZE - readed);
            readed += readedPortion;
        }
        if (readedPortion != -1) {
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            fileOutputStream.write(buffer);
            dataLength = readed + Math.abs((int) stream.transferTo(fileOutputStream));
            fileOutputStream.close();
        } else {
            dataLength = readed + 1;
        }
    }
}
