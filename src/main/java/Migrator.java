import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.List;
import java.util.Objects;

public class Migrator {
    private static final String PATH_TO_FILES = "/files";
    private static final String PATH_TO_FILE = "/files/";
    private static final int DEFAULT_CONNECT_ATTEMPTS = 5;
    private final String currentStorageUrl;

    public Migrator(String currentStorageUrl) {
        this.currentStorageUrl = currentStorageUrl;
    }

    public boolean migrateTo(String newStorageUrl) {
        List<String> fileList = getFileList(currentStorageUrl);
        if (fileList == null) {
            return false;
        }

        boolean result = true;
        for (String fileName : fileList) {
            result &= copyFile(currentStorageUrl, newStorageUrl, fileName);
        }

        return result;
    }

    private static List<String> getFileList(String storageServerUrl) {
        storageServerUrl += PATH_TO_FILES;
        return RequestMaster.sendGetRequest(storageServerUrl, DEFAULT_CONNECT_ATTEMPTS, null,
                (inputStream, none) -> {
            List<String> filesList;
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                filesList = objectMapper.readValue(inputStream, new TypeReference<List<String>>() {});
            } catch (IOException e) {
                filesList = null;
            }
            return filesList;
        });
    }

    private static boolean copyFile(String fromUrlAddress, String toUrlAddress, String fileName) {
        fromUrlAddress += PATH_TO_FILE + fileName;
        String[] toData = {toUrlAddress, fileName};
        Boolean result = RequestMaster.sendGetRequest(fromUrlAddress, DEFAULT_CONNECT_ATTEMPTS, toData,
                (inputStream, data) -> sendFileTo(data[0], data[1], inputStream));
        return Objects.requireNonNullElse(result, false);
    }

    public static Boolean sendFileTo(String storageServerUrl, String fileName, InputStream fileData) {
        storageServerUrl += PATH_TO_FILES;
        return RequestMaster.sendPostRequest(storageServerUrl, DEFAULT_CONNECT_ATTEMPTS, fileName, fileData);
    }

    public static boolean deleteFile(String storageServerUrl, String fileName) {
        storageServerUrl += PATH_TO_FILE + fileName;
        return RequestMaster.sendDeleteRequest(storageServerUrl, DEFAULT_CONNECT_ATTEMPTS);
    }

    public static void main(String[] args) {
//        List<String> filesList = Migrator.getFilesList("http://127.0.0.1:8080/oldStorage");
//        if (filesList == null) {
//            System.out.println("Files list is null");
//        } else {
//            System.out.println(filesList.get(0));
//        }
//        InputStream stream = new ByteArrayInputStream("Test1".getBytes(StandardCharsets.UTF_8));
//        boolean result = Migrator.sendFileTo("http://127.0.0.1:8080/newStorage", "test2", stream);
//        boolean result = Migrator.deleteFile("http://127.0.0.1:8080/newStorage", "test2");
//        System.out.println(result);
//        System.out.println(Migrator.copyFile(
//                "http://127.0.0.1:8080/oldStorage",
//                "http://127.0.0.1:8080/newStorage",
//                "953.txt"
//        ));
        Migrator migrator = new Migrator("http://127.0.0.1:8080/oldStorage");
        System.out.println(migrator.migrateTo("http://127.0.0.1:8080/newStorage"));
    }
}
