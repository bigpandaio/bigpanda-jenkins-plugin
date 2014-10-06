package org.jenkinsci.plugins.bigpandaJenkins;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.*;
import hudson.util.FormValidation;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpEntity;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.kohsuke.stapler.DataBoundConstructor;
import org.apache.http.impl.client.DefaultHttpClient;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import jenkins.model.Jenkins;


public class BigPandaNotifier extends Notifier {
	// public members ----------------------------------------------------------

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

    private final boolean bigPandaUseNodeNameInsteadOfLabels;
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public BigPandaNotifier(boolean bigPandaUseNodeNameInsteadOfLabels) {
        this.bigPandaUseNodeNameInsteadOfLabels = bigPandaUseNodeNameInsteadOfLabels;
    }

    public boolean getBigPandaUseNodeNameInsteadOfLabels(){
        return bigPandaUseNodeNameInsteadOfLabels;
    }

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		return processEvent(build, listener, "INPROGRESS");
	}
	
	@Override
	public boolean perform(
			AbstractBuild<?, ?> build, 
			Launcher launcher, 
			BuildListener listener) {
		
		if ((build.getResult() == null) 
				|| (!build.getResult().equals(Result.SUCCESS))) {
			return processEvent(
					build, listener, "FAILED");
		} else {
			return processEvent(
					build, listener, "SUCCESSFUL");
		}
	}

    private boolean processEvent(
            final AbstractBuild<?, ?> build, 
            final BuildListener listener,
            final String state){
	   PrintStream logger = listener.getLogger();
       if (!getDescriptor().isEnabled()){
           logger.println("WARNING: BigPanda Notification Disabled - Not configured.");
           logger.println("Configure your BigPanda Notification at the global system settings...");
           return true; //NOTE: Don't fail build because of our notification...
       }
       
      
       try {
           notifyBigPanda(logger, build, listener, state);
       }
       catch (Exception err){
           logger.println(err.toString());
           return false;
       }
       return true;
    }


    private boolean notifyBigPanda(
            final PrintStream logger,
            final AbstractBuild<?, ?> build,
            final BuildListener listener,
            final String state) throws Exception {
        HttpEntity bigPandaBuildNotificationEntity
                = newBigPandaBuildNotificationEntity(build, state, logger, listener);
        HttpPost req = createRequest(bigPandaBuildNotificationEntity, state, logger);
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
            final String state,
            final PrintStream logger) {

        final String url = getDescriptor().getBigPandaUrl() + "/data/events/deployments/" + (state == "INPROGRESS" ? "start" : "end");

        HttpPost req = new HttpPost(url);

        req.setHeader("Authorization", "Bearer " + getDescriptor().getBigPandaToken());
        req.setHeader("Content-type", "application/json");
        req.setEntity(bigPandaBuildNotificationEntity);

        return req;
    }

    private HttpEntity newBigPandaBuildNotificationEntity(
            final AbstractBuild<?, ?> build,
            final String state,
            final PrintStream logger,
            final BuildListener listener) throws UnsupportedEncodingException {


        final AbstractBuild<?, ?> rootBuild = build.getRootBuild();
        JSONObject json = new JSONObject();

        json.put("version", String.valueOf(rootBuild.getNumber()));
        json.put("source", "Jenkins: " + Jenkins.getInstance().getRootUrl() + " Version: " + Jenkins.getVersion().toString()); 


        JSONArray hosts = new JSONArray();
        String nodeName = build.getBuiltOn().getNodeName(); 
        if (!bigPandaUseNodeNameInsteadOfLabels && build.getProject().getName() != rootBuild.getProject().getName()) {
           nodeName = build.getProject().getName();  //label
        }
        if (nodeName == null || nodeName == ""){
            nodeName = "master";
        }
        hosts.add(nodeName);
        json.put("hosts", hosts);

        String description = getBuildDescription(build);

        ChangeLogSet<? extends ChangeLogSet.Entry> changeSet = rootBuild.getChangeSet();
        List<Entry> entries = new LinkedList<Entry>();
        for (Object o : changeSet.getItems()) {
            Entry entry = (Entry) o;
            entries.add(entry);
        }
        if (entries.isEmpty()) {
            json.put("owner", "unknown");
        } else {
            Set<String> authors = new HashSet<String>();
            Set<String> commitAndMessages = new HashSet<String>();
            for (Entry entry : entries) {
                authors.add(entry.getAuthor().getDisplayName());
                commitAndMessages.add(entry.getCommitId() + ":" + entry.getMsg());
            }
            json.put("owner", StringUtils.join(authors, ", "));
            if (description != null){
                json.put("description", description);
            } else {
                json.put("description", StringUtils.join(commitAndMessages, ", "));
            }
         }
        if (state != "INPROGRESS"){
            Result result = build.getResult();
            boolean isSuccess = result == Result.SUCCESS;
            json.put("status",  isSuccess ? "success" : "failure");
            if (!isSuccess){
                String errorMessage = "Unknown";
                if (result == Result.SUCCESS) errorMessage ="Success";
                if (result == Result.FAILURE) errorMessage = "FAILURE";
                if (result == Result.ABORTED) errorMessage = "ABORTED";
                if (result == Result.NOT_BUILT) errorMessage = "Not built";
                if (result == Result.UNSTABLE) errorMessage = "Unstable";
                json.put("errorMessage", errorMessage);
            }
        }
        // Taken from StashNotifier
        String fullName = StringEscapeUtils.
                escapeJavaScript(rootBuild.getProject().getName()).
                replaceAll("\\\\u00BB", "\\/");
        json.put("component", fullName);

        return new StringEntity(json.toString());
    }


    private String getBuildDescription(
            final AbstractBuild<?, ?> build) {

        if (build.getDescription() != null
                && build.getDescription().trim().length() > 0) {

            return build.getDescription();
        } else {
            return null;
        }
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
	public static final class DescriptorImpl 
		extends BuildStepDescriptor<Publisher> {

        private String bigPandaToken;
        private String bigPandaUrl;
        private boolean bigPandaUseNodeNameInsteadOfLabels;
        private final String bigPandaDefaultUrl = "https://api.bigPanda.io";


        public DescriptorImpl() {
            load();
        }

        public boolean isEnabled(){
            return getBigPandaToken() != null && getBigPandaUrl() != null;
        }

        public boolean getBigPandaUseNodeNameInsteadOfLabels(){
            return bigPandaUseNodeNameInsteadOfLabels;
        }

        public String getBigPandaDefaultUrl(){
            return bigPandaDefaultUrl;
        }

        public String getBigPandaToken() {
        	if ((bigPandaToken != null) && (bigPandaToken.trim().equals(""))) {
        		return null;
        	} else {
	            return bigPandaToken;
        	}
        }

        public String getBigPandaUrl() {
            if (bigPandaUrl == null){
                return bigPandaDefaultUrl;
            }
        	if ((bigPandaUrl != null) && (bigPandaUrl.trim().equals(""))) {
        		return bigPandaDefaultUrl;
        	} else {
	            return bigPandaUrl;
        	}
        }

  		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return "Notify BigPanda";
		}

        public FormValidation doCheckBigPandaToken(
					@QueryParameter("bigPandaToken") String value) 
				throws IOException, ServletException {

			String token = value;
			if ((token != null) && (!token.trim().equals(""))) {
				token = token.trim();
			} else {
				token = bigPandaToken != null ? bigPandaToken.trim() : null;
			}

			if ((token == null) || token.equals("")) {
				return FormValidation.error(
						"Please specify a valid bigPanda token here");
			} else {
                return FormValidation.ok();
			}
		}

        public FormValidation doCheckBigPandaUrl(
					@QueryParameter("bigPandaUrl") String value) 
				throws IOException, ServletException {

			// calculate effective url from global and local config
			String url = value;
			if ((url != null) && (!url.trim().equals(""))) {
				url = url.trim();
			} else {
				url = bigPandaUrl != null ? bigPandaUrl.trim() : null;
			}

            try {
                new URL(url);
                return FormValidation.ok();
            } catch (Exception e) {
                return FormValidation.error(
                    "Please specify a valid URL here");
            }
		}


		@Override
		public boolean configure(
				StaplerRequest req, 
				JSONObject formData) throws FormException {

            // to persist global configuration information,
            // set that to properties and call save().
            bigPandaToken
            	= formData.getString("bigPandaToken");
            bigPandaUrl
                = formData.getString("bigPandaUrl");
            save();
			return super.configure(req, formData);
		}
	}
}
