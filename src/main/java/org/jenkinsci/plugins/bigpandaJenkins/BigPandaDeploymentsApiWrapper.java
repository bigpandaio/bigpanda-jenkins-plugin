package org.jenkinsci.plugins.bigpandaJenkins;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.PrintStream;

public class BigPandaDeploymentsApiWrapper {
    private final String bigPandaToken;
    private final String bigPandaUrl;
    private final boolean bigPandaUseNodeNameInsteadOfLabels;

    public  BigPandaDeploymentsApiWrapper(
            final String bigPandaToken,
            final String bigPandaUrl,
            boolean bigPandaUseNodeNameInsteadOfLabels){
        this.bigPandaToken = bigPandaToken;
        this.bigPandaUrl = bigPandaUrl;
        this.bigPandaUseNodeNameInsteadOfLabels = bigPandaUseNodeNameInsteadOfLabels;
    }

    public boolean notifyBigPanda(
            final PrintStream logger,
            final AbstractBuild<?, ?> build,
            final BuildListener listener,
            final BigPandaNotifierState state) throws Exception {
        HttpEntity bigPandaBuildNotificationEntity
                = new BigPandaJobToJsonConverter(this.bigPandaUseNodeNameInsteadOfLabels)
                    .newBigPandaBuildNotificationEntity(build, state);
        HttpPost req = createRequest(bigPandaBuildNotificationEntity, state);
        HttpClient client = new DefaultHttpClient();
        try {
            HttpResponse res = client.execute(req);

            if (res.getStatusLine().getStatusCode() != 201) {
                logger.println("Failed to execute BigPanda Notifier");
                return false;
            } else {
                logger.println("BigPanda Notifier executed");
                return true;
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    private HttpPost createRequest(
            final HttpEntity bigPandaBuildNotificationEntity,
            final BigPandaNotifierState state) {

        final String url = this.bigPandaUrl + "/data/events/deployments/" + (state.equals(BigPandaNotifierState.STARTED) ? "start" : "end");

        HttpPost req = new HttpPost(url);

        req.setHeader("Authorization", "Bearer " + this.bigPandaToken);
        req.setHeader("Content-type", "application/json");
        req.setEntity(bigPandaBuildNotificationEntity);

        return req;
    }
}
