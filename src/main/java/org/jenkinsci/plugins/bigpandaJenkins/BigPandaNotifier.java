package org.jenkinsci.plugins.bigpandaJenkins;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;


public class BigPandaNotifier extends Notifier {
    private final boolean bigPandaUseNodeNameInsteadOfLabels;

    @DataBoundConstructor
    public BigPandaNotifier(boolean bigPandaUseNodeNameInsteadOfLabels) {
        this.bigPandaUseNodeNameInsteadOfLabels = bigPandaUseNodeNameInsteadOfLabels;
    }

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

    public boolean getBigPandaUseNodeNameInsteadOfLabels(){
        return bigPandaUseNodeNameInsteadOfLabels;
    }

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		return processEvent(build, listener, BigPandaNotifierState.STARTED);
	}
	
	@Override
	public boolean perform(
			AbstractBuild<?, ?> build, 
			Launcher launcher, 
			BuildListener listener) {
		
		if ((build.getResult() == null) 
				|| (!build.getResult().equals(Result.SUCCESS))) {
			return processEvent(
					build, listener, BigPandaNotifierState.FAILED);
		} else {
			return processEvent(
					build, listener, BigPandaNotifierState.SUCCESSFUL);
		}
	}

    private boolean processEvent(
            final AbstractBuild<?, ?> build, 
            final BuildListener listener,
            final BigPandaNotifierState state){
	   PrintStream logger = listener.getLogger();
        BigPandaNotifierDescriptor descriptor = getDescriptor();
       if (!descriptor.isEnabled()){
           logger.println("WARNING: BigPanda Notification Disabled - Not configured.");
           logger.println("Configure your BigPanda Notification at the global system settings...");
           return true; //NOTE: Don't fail build because of our notification...
       }
       
      
       try {
           new BigPandaDeploymentsApiWrapper(
                   descriptor.getBigPandaToken(),
                   descriptor.getBigPandaUrl(),
                   this.bigPandaUseNodeNameInsteadOfLabels).notifyBigPanda(logger, build, listener, state);
       }
       catch (Exception err){
           logger.println(err.toString());
           return false;
       }
       return true;
    }


    @Override
    public BigPandaNotifierDescriptor getDescriptor() {
        return (BigPandaNotifierDescriptor)super.getDescriptor();
    }
    @Extension
    public static final class BigPandaNotifierDescriptor
            extends BuildStepDescriptor<Publisher> {

        private String bigPandaToken;
        private String bigPandaUrl;
        private boolean bigPandaUseNodeNameInsteadOfLabels;
        private final String bigPandaDefaultUrl = "https://api.bigPanda.io";


        public BigPandaNotifierDescriptor() {
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
            if (bigPandaUrl.trim().equals("")) {
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
                        "Please specify a valid BigPanda token here");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckBigPandaUrl(
                @QueryParameter("bigPandaUrl") String value)
                throws IOException, ServletException {

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

            bigPandaToken
                    = formData.getString("bigPandaToken");
            bigPandaUrl
                    = formData.getString("bigPandaUrl");
            save();
            return super.configure(req, formData);
        }
    }

}
