package org.jenkinsci.plugins.bigpanda;

import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

@Extension
public class BigpandaGlobalNotifier extends RunListener<Run<?, ?>> implements Describable<BigpandaGlobalNotifier> {

    private Run<?,?> build;
    private JSONObject change;


    @Override
    public void onCompleted(Run<?, ?> run, TaskListener listener) {
        publish(run, listener);
    }

    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {
        publish(run, listener);
    }

    public Descriptor<BigpandaGlobalNotifier> getDescriptor() {
        return getDescriptorImpl();
    }

    public DescriptorImpl getDescriptorImpl() {
        return (DescriptorImpl) Jenkins.get().getDescriptorOrDie(BigpandaGlobalNotifier.class);
    }

    private void publish(Run<?, ?> r, TaskListener listener) {

        this.build = r;
        ChangesBuilder changeBuilder = new ChangesBuilder(this.build, listener);

        Descriptor<BigpandaGlobalNotifier> descriptor = getDescriptor();

        change = changeBuilder.create();

        BigpandaApiWrapper bpApi = new BigpandaApiWrapper( ((DescriptorImpl) descriptor).getBigpandaApiKey(), 
                                                            ((DescriptorImpl) descriptor).getBigpandaAppKey(), ((DescriptorImpl) descriptor).getWebhookUrl());

        try {
            bpApi.notifyBigPanda(change);

            listener.getLogger().println("BigPanda Notifier: Success");
        } catch (Exception e) {
            listener.getLogger().println("BigPanda Notifier:  Failed: " + e.getMessage());
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<BigpandaGlobalNotifier> {
        private String bigpandaApiKey;
        private String bigpandaAppKey;
        private String webhookUrl;
        private static String defaultWebhookUrl = "https://inbound.bigpanda.io/jenkins/changes";

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "BigPanda Global Notifications";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {
            req.bindJSON(this, formData);

            save();
            return true;
        }

        /**
         * @return Bigpanda Api Key
         */
        public String getBigpandaApiKey() {
            return this.bigpandaApiKey;
        }

        /**
         * @return Bigpanda App Key
         */
        public String getBigpandaAppKey() {
            return this.bigpandaAppKey;
        }

        /**
         * Provides the webhook for the API calls
         * @return Webhook URL
         */
        public String getWebhookUrl() {
            if (this.webhookUrl == null) {
                return this.defaultWebhookUrl;
            } else {
                return this.webhookUrl;
            }
        }

        /**
         * Provides the default webhook for the API calls
         * @return Webhook URL
         */
        public String getDefaultWebhookUrl() {
            return this.defaultWebhookUrl;
        }

        @DataBoundSetter
        public void setBigpandaApiKey(String bigpandaApiKey) {
            this.bigpandaApiKey = bigpandaApiKey;
        }

        @DataBoundSetter
        public void setBigpandaAppKey(String bigpandaAppKey) {
            this.bigpandaAppKey = bigpandaAppKey;
        }

        @DataBoundSetter
        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }

        public FormValidation doCheckBigpandaApiKey(@QueryParameter("bigpandaApiKey") String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please provide an API Key");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckBigpandaAppKey(@QueryParameter("bigpandaAppKey") String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please provide an App Key");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckWebhookUrl(@QueryParameter("webhookUrl") String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please provide an endpoint URL");
            }

            try {
                new URL(value);
                return FormValidation.ok();
            } catch (Exception e) {
                return FormValidation.error("Please specify a valid URL here");
            }
        }
    }
}