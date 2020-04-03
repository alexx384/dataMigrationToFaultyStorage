import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

public class Migrator {
    private static final int DEFAULT_CONNECT_ATTEMPTS = 10;
    private static final int DEFAULT_THREAD_COUNT = 4;
    private static final String PATH_TO_FILES = "/files";
    private static final String PATH_TO_FILE = "/files/";
    private static final String DEFAULT_TEMP_FILE_PREFIX = "temp-";

    private final String currentStorageUrl;

    public Migrator(String currentStorageUrl) {
        this.currentStorageUrl = currentStorageUrl;
    }

    /**
     * Creates N file buffers that based on temp files
     * and will be used if the file content on server is too big
     *
     * @param bufferCount number of file buffers to create
     * @return list of file buffers
     */
    public static List<FileBuffer> createNFileBuffers(int bufferCount) {
        List<FileBuffer> fileBufferList = new ArrayList<>(bufferCount);
        try {
            for (int i = 0; i < bufferCount; i++) {
                fileBufferList.add(new FileBuffer(DEFAULT_TEMP_FILE_PREFIX + i));
            }
            return fileBufferList;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Returns the files which present on server
     *
     * @param storageServerUrl server url, for example "http://127.0.0.1:8080/oldStorage"
     * @return list of file names which present on server
     */
    public static List<String> getFileList(String storageServerUrl) {
        storageServerUrl += PATH_TO_FILES;
        return RequestMaster.sendGetRequest(storageServerUrl, DEFAULT_CONNECT_ATTEMPTS, null,
                (inputStream, none) -> {
                    List<String> filesList;
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        filesList = objectMapper.readValue(inputStream, new TypeReference<>() {
                        });
                    } catch (IOException e) {
                        filesList = null;
                    }
                    return filesList;
                });
    }

    /**
     * Sends file content on server {@code storageServerUrl} from {@code fileBuffer}
     *
     * @param storageServerUrl server url, for example "http://127.0.0.1:8080/oldStorage"
     * @param fileName name of file which need to create on server
     * @param fileBuffer data to send on server
     * @return true if file created on server with content from {@code fileBuffer}, otherwise - false
     */
    public static boolean sendFileTo(String storageServerUrl, String fileName, FileBuffer fileBuffer) {
        storageServerUrl += PATH_TO_FILES;
        return RequestMaster.sendPostRequest(storageServerUrl, DEFAULT_CONNECT_ATTEMPTS, fileName, fileBuffer,
                FileBuffer::getStream);
    }

    /**
     * Deletes file with name {@code fileName} from server {@code storageServerUrl}
     *
     * @param storageServerUrl server url, for example "http://127.0.0.1:8080/oldStorage"
     * @param fileName name of file to delete from server
     * @return true if file deleted from server, otherwise - false
     */
    public static boolean deleteFileFrom(String storageServerUrl, String fileName) {
        storageServerUrl += PATH_TO_FILE + fileName;
        return RequestMaster.sendDeleteRequest(storageServerUrl, DEFAULT_CONNECT_ATTEMPTS);
    }

    /**
     * Receives file content from server {@code fromUrlAddress} and sends it on server {@code toUrlAddress}
     *
     * @param fromUrlAddress server url, for example "http://127.0.0.1:8080/oldStorage"
     * @param toUrlAddress server url, for example "http://127.0.0.1:8080/oldStorage"
     * @param fileName name of file to retrieve from {@code fromUrlAddress} and send to {@code toUrlAddress}
     * @param fb temporary storage for the file content
     * @return true if file copied from {@code fromUrlAddress} to {@code toUrlAddress}, otherwise - false
     */
    public static boolean copyFile(String fromUrlAddress, String toUrlAddress, String fileName, FileBuffer fb) {
        fromUrlAddress += PATH_TO_FILE + fileName;
        Boolean result = RequestMaster.sendGetRequest(fromUrlAddress, DEFAULT_CONNECT_ATTEMPTS, fb,
                (inputStream, fileBuffer) -> {
                    try {
                        fileBuffer.setStream(inputStream);
                        return Boolean.TRUE;
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                        return Boolean.FALSE;
                    }
                });
        if (Objects.requireNonNullElse(result, false)) {
            return sendFileTo(toUrlAddress, fileName, fb);
        }

        return false;
    }

    /**
     * Retrieves file list from server {@code currentStorageUrl} and copy each file to {@code newStorageUrl}
     *
     * @param newStorageUrl server url, for example "http://127.0.0.1:8080/oldStorage"
     * @return true if all files from {@code currentStorageUrl} migrated to {@code newStorageUrl}, otherwise - false
     */
    public boolean migrateTo(String newStorageUrl) {
        return migrateTo(newStorageUrl, DEFAULT_THREAD_COUNT);
    }

    /**
     * Retrieves file list from server {@code currentStorageUrl} and copy each file to {@code newStorageUrl}
     *
     * @param newStorageUrl server url, for example "http://127.0.0.1:8080/oldStorage"
     * @param threadsNum use threads to copy files
     * @return true if all files from {@code currentStorageUrl} migrated to {@code newStorageUrl}, otherwise - false
     */
    public boolean migrateTo(String newStorageUrl, int threadsNum) {
        List<FileBuffer> fileBufferList = createNFileBuffers(threadsNum);
        if (fileBufferList == null) {
            return false;
        }

        List<String> fileList = getFileList(currentStorageUrl);
        if (fileList == null) {
            return false;
        }

        ExecutorService exec = Executors.newFixedThreadPool(threadsNum);
        int itemsNum = fileList.size();
        int minItemsPerThread = itemsNum / threadsNum;
        int maxItemsPerThread = minItemsPerThread + 1;
        int threadsWithMaxItems = itemsNum - threadsNum * minItemsPerThread;
        int startIdx = 0;
        List<Future<Boolean>> futureList = new ArrayList<>(threadsNum);
        for (int i = 0; i < threadsNum; i++) {
            int itemsCount = (i < threadsWithMaxItems) ? maxItemsPerThread : minItemsPerThread;
            int endIdx = startIdx + itemsCount;
            if ((endIdx - startIdx) != 0) {
                Callable<Boolean> task = new MoveItemsProcess(fileList.subList(startIdx, endIdx), fileBufferList.get(i),
                        currentStorageUrl, newStorageUrl);
                futureList.add(exec.submit(task));
                startIdx = endIdx;
            }
        }

        boolean result = true;
        for (Future<Boolean> f : futureList) {
            try {
                result &= f.get();
            } catch (InterruptedException | ExecutionException e) {
                System.out.println(e.getMessage());
                result = false;
            }
        }
        exec.shutdown();

        return result;
    }

    static class MoveItemsProcess implements Callable<Boolean> {
        private final List<String> fileNameList;
        private final FileBuffer fileBuffer;
        private final String oldStorageUrl;
        private final String newStorageUrl;

        public MoveItemsProcess(List<String> fileNameList, FileBuffer fileBuffer, String oldStorageUrl,
                                String newStorageUrl) {
            this.fileNameList = fileNameList;
            this.fileBuffer = fileBuffer;
            this.oldStorageUrl = oldStorageUrl;
            this.newStorageUrl = newStorageUrl;
        }

        @Override
        public Boolean call() {
            boolean result = true;
            List<String> lostFiles = new LinkedList<>();
            for (String fileName : fileNameList) {
                boolean isComplete = copyFile(oldStorageUrl, newStorageUrl, fileName, fileBuffer);
                if (isComplete) {
                    if (!deleteFileFrom(oldStorageUrl, fileName)) {
                        lostFiles.add(fileName);
                    }
                } else {
                    lostFiles.add(fileName);
                }
                result &= isComplete;
            }

            if (!result) {
                System.out.println("Could not migrate the following files:");
                for (String fileName : lostFiles) {
                    System.out.println(fileName + " ");
                }
            }
            return result;
        }
    }

    public static void main(String[] args) {
        Migrator migrator = new Migrator("http://127.0.0.1:8080/oldStorage");
        System.out.println(migrator.migrateTo("http://127.0.0.1:8080/newStorage"));
    }
}
