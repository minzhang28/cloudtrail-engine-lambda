package cloudengine.AlertServices;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class DatadogAlertService {
    final static Logger logger = LoggerFactory.getLogger(DatadogAlertService.class);

    public void report(Map<String, Object> event, String ruleName) throws IOException {
        logger.info("rule triggered: " + ruleName);

        JSONObject json = new JSONObject();
        json.put("tags", "cloud-engine");
        json.put("title", "Rule Triggered: " + ruleName);
        json.put("text", event.toString());
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        try {
            HttpPost request = new HttpPost("https://api.datadoghq.com/api/v1/events?api_key=API_KEY");
            StringEntity params = new StringEntity(json.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            logger.info(EntityUtils.toString(entity));
        } catch (Exception ex) {
            // handle exception here
        } finally {
            httpClient.close();
        }
    }
}
