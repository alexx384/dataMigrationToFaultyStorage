import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.List;

public class Migrator {
    private static final String PATH_TO_FILES = "/files";
    private static final String PATH_TO_FILE = "/files/";

    public Migrator(String currentStorageUrl) {
        this.currentStorageUrl = currentStorageUrl;
    }
    private final String currentStorageUrl;

    private static List<String> getFilesList(String storageUrl) {
        return RequestMaster.sendGetRequest(storageUrl + PATH_TO_FILES, 5, inputStream -> {
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

    public void migrateToNew(String newStorageUrl) {

    }

    public static void main(String[] args) {
        List<String> filesList = Migrator.getFilesList("http://127.0.0.1:8080/oldStorage");
        if (filesList == null) {
            System.out.println("Files list is null");
        } else {
            System.out.println(filesList.get(0));
        }
    }
}
