import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class RequestMaster {
    private static final int STATUS_OK = 200;

    public static List<String> sendGetRequest(String urlAddress, int attemptsToConnect,
                                              Function<InputStream, List<String>> mapper) {
        List<String> result = null;
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(urlAddress);

            for (int i = 0; i < attemptsToConnect && result == null; i++) {
                try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                    System.out.println("----------------------------------------");
                    StatusLine statusLine = response.getStatusLine();
                    System.out.println(statusLine);

                    // Get hold of the response entity
                    HttpEntity entity = response.getEntity();
                    if (entity != null && statusLine.getStatusCode() == STATUS_OK) {
                        try (InputStream inStream = entity.getContent()) {
                            result = mapper.apply(inStream);
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
}
