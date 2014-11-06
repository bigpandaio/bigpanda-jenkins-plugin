package org.jenkinsci.plugins.bigpandaJenkins;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Cause;
import hudson.model.Cause.UserIdCause;
import hudson.scm.ChangeLogSet;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BigPandaJobToJsonConverter {
    private final boolean bigPandaUseNodeNameInsteadOfLabels;

    public BigPandaJobToJsonConverter(
            boolean bigPandaUseNodeNameInsteadOfLabels){
        this.bigPandaUseNodeNameInsteadOfLabels = bigPandaUseNodeNameInsteadOfLabels;
    }

    public HttpEntity newBigPandaBuildNotificationEntity(
            final AbstractBuild<?, ?> build,
            final BigPandaNotifierState state) throws UnsupportedEncodingException {


        final AbstractBuild<?, ?> rootBuild = build.getRootBuild();
        JSONObject json = new JSONObject();

        json.put("version", String.valueOf(rootBuild.getNumber()));
        json.put("source", "Jenkins: " + Jenkins.getInstance().getRootUrl() + " Version: " + Jenkins.getVersion().toString());
        json.put("source_system", "jenkins");


        JSONArray hosts = new JSONArray();
        String nodeName = build.getBuiltOn().getNodeName();
        if (!this.bigPandaUseNodeNameInsteadOfLabels && !build.getProject().getName().equals(rootBuild.getProject().getName())) {
            nodeName = build.getProject().getName();  //label
        }
        if (nodeName == null || nodeName.equals("")){
            nodeName = "master";
        }
        hosts.add(nodeName);
        json.put("hosts", hosts);

        String description = getBuildDescription(build);

        List<Cause> causes = build.getCauses();


        ChangeLogSet<? extends ChangeLogSet.Entry> changeSet = rootBuild.getChangeSet();
        List<ChangeLogSet.Entry> entries = new LinkedList<ChangeLogSet.Entry>();
        for (Object o : changeSet.getItems()) {
            ChangeLogSet.Entry entry = (ChangeLogSet.Entry) o;
            entries.add(entry);
        }
        if (entries.isEmpty()) {
            if (causes.size() > 0 && (causes.get(0) instanceof Cause.UserIdCause)){
                json.put("owner", ((UserIdCause)causes.get(0)).getUserName());
            } else {
                json.put("owner", "unknown");
            }
            if (description != null){
                json.put("description", description);
            } else { 
                json.put("description", causes.get(0).getShortDescription());
            }

        } else {
            Set<String> authors = new HashSet<String>();
            Set<String> commitAndMessages = new HashSet<String>();
            for (ChangeLogSet.Entry entry : entries) {
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
        if (!state.equals(BigPandaNotifierState.STARTED)){
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
}
