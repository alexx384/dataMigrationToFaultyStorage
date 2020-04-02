import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.io.InputStream;

public class RequestMaster {
    private static final int STATUS_OK = 200;
    private static final ContentType DEFAULT_CONTENT_TYPE = ContentType.create("text/plain");
    private static final Header ACCEPT_ALL_HEADER = new BasicHeader("accept", "*/*");

    interface Function2In1Out<FI, SI, R> {
        R apply(FI firstInput, SI secondInput);
    }

    public static <I, R> R sendGetRequest(String urlAddress, int attemptsToConnect, I passedData,
                                          Function2In1Out<InputStream, I, R> mapper) {
        R result = null;
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(urlAddress);

            for (int i = 0; i < attemptsToConnect && result == null; i++) {
                try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                    StatusLine statusLine = response.getStatusLine();
                    // Get hold of the response entity
                    HttpEntity entity = response.getEntity();
                    if (entity != null && statusLine.getStatusCode() == STATUS_OK) {
                        try (InputStream inStream = entity.getContent()) {
                            if (inStream != null) {
                                result = mapper.apply(inStream, passedData);
                            }
                        } catch (IOException ex) {
                            result = null;
                        }
                    }
                }
            }
        } catch (IOException e) {
            result = null;
        }
        return result;
    }

    public static boolean sendPostRequest(String urlAddress, int attemptsToConnect,
                                          String fileName, InputStream inputFileStream) {
        boolean isCompleted = false;
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(urlAddress);
            httpPost.addHeader(ACCEPT_ALL_HEADER);

            HttpEntity entity = MultipartEntityBuilder
                    .create()
                    .addBinaryBody("file", inputFileStream, DEFAULT_CONTENT_TYPE, fileName)
                    .build();
            httpPost.setEntity(entity);

            for (int i = 0; i < attemptsToConnect && !isCompleted; i++) {
                try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                    StatusLine statusLine = response.getStatusLine();
                    String responseMessage = new String(response.getEntity().getContent().readAllBytes());
                    System.out.println(responseMessage);
                    if (responseMessage.length() > 10) {
                        System.out.println("Find");
                    }
                    // Get hold of the response entity
                    if (statusLine.getStatusCode() == STATUS_OK) {
                        isCompleted = true;
                    }
                }
            }
        } catch (IOException e) {
            isCompleted = false;
        }
        return isCompleted;
    }

    public static boolean sendDeleteRequest(String urlAddress, int attemptsToConnect) {
        boolean isCompleted = false;
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpDelete httpDelete = new HttpDelete(urlAddress);

            for (int i = 0; i < attemptsToConnect && !isCompleted; i++) {
                try (CloseableHttpResponse response = httpclient.execute(httpDelete)) {
                    StatusLine statusLine = response.getStatusLine();
                    // Get hold of the response entity
                    if (statusLine.getStatusCode() == STATUS_OK) {
                        isCompleted = true;
                    }
                }
            }
        } catch (IOException e) {
            isCompleted = false;
        }
        return isCompleted;
    }
}
