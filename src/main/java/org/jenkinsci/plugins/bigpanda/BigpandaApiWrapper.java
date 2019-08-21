package org.jenkinsci.plugins.bigpanda;

import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

public class BigpandaApiWrapper {
    private final String bigpandaApiKey;
    private final String bigpandaAppKey;
    private final String webhookUrl;
    private final JSONObject payload = new JSONObject();

    public BigpandaApiWrapper(final String bigpandaApiKey, final String bigpandaAppKey, final String webhookUrl) {
        this.bigpandaApiKey = bigpandaApiKey;
        this.bigpandaAppKey = bigpandaAppKey;
        this.webhookUrl = webhookUrl;
    }

    public boolean notifyBigPanda(final JSONObject change) throws Exception {

        // Adding required fields
        this.payload.put("apiKey", this.bigpandaApiKey);
        this.payload.put("appKey", this.bigpandaAppKey);
        this.payload.put("change", change);

        StringEntity payloadEntity = new StringEntity(this.payload.toString());
        
        HttpPost req = createRequest(payloadEntity);

        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse res = client.execute(req);

        if (res.getStatusLine().getStatusCode() != 200) {
            String errorMessage = res.getStatusLine().getReasonPhrase() + " - "+ res.getEntity().toString();
            throw new HttpException(errorMessage);
        } else {
            return true;
        }

    }

    private HttpPost createRequest(final HttpEntity payloadEntity) {

        final String url = this.webhookUrl;

        HttpPost req = new HttpPost(url);

        req.setHeader("Content-type", "application/json");
        req.setEntity(payloadEntity);

        return req;
    }
}
