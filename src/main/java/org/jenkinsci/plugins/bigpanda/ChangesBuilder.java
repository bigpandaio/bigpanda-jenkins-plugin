package org.jenkinsci.plugins.bigpanda;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import hudson.EnvVars;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.triggers.TimerTrigger;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Will convert the build data into a JSON object to send to BigPanda
 */
public class ChangesBuilder {

    private Run<?, ?> build;
    private TaskListener listener;
    private JSONObject json = new JSONObject();
    private JSONObject tags = new JSONObject();

    public ChangesBuilder(Run<?, ?> build, TaskListener listener) {
        this.build = build;
        this.listener = listener;
    }

    public JSONObject create() {
        Result result = this.build.getResult();

        addChangeInfo(result);
        addUserInfo();
        addSCMInfo();

        // Adding remaining fields to tags object
        this.tags.put("description", getBuildDescription());

        // if return is null, add empty string
        VersionNumber jenkinsVersion = Jenkins.getVersion();
        String version = "";

        if (jenkinsVersion != null) {
            version = jenkinsVersion.toString();
        }

        if (jenkinsVersion == null) {
            version = "";
        }

        // adding jenkins data
        this.tags.put("jenkins_version", version);

        // Add the tags object
        this.json.put("tags", this.tags);

        return this.json;
    }

    /**
     * Get the data needed and populate the Change Object
     */
    private void addChangeInfo(Result result) {
        // map status
        if (this.build.isBuilding()) {
            this.json.put("status", "In Progress");
        } else if (result == Result.FAILURE || result == Result.ABORTED || result == Result.NOT_BUILT) {
            this.json.put("status", "Canceled");
        } else {
            this.json.put("status", "Done");
        }

        String identifier = getMd5(this.build.getFullDisplayName().replaceAll("[^a-zA-Z0-9-# ]", "") + this.build.getStartTimeInMillis());

        // Converting the identifier into a unique hash
        this.json.put("identifier", String.valueOf(identifier));
        this.json.put("start", (this.build.getStartTimeInMillis() / 1000));
        this.json.put("summary", this.build.getFullDisplayName().replaceAll("[^a-zA-Z0-9-# ]", ""));
        this.json.put("ticket_url", Jenkins.get().getRootUrl() + this.build.getUrl());

        if (getEndTime() != 0) {
            this.json.put("end", getEndTime());
        }
    }

    private void addUserInfo() {

        List<Cause> causes = this.build.getCauses();

        for (Cause cause : causes) {
            if (cause instanceof Cause.UserIdCause) {
                String userId = ((Cause.UserIdCause) cause).getUserId();
                userId = (userId == null || userId.isEmpty()) ? "Anonymous" : userId;

                this.tags.put("user_id", userId);

                String userName = ((Cause.UserIdCause) cause).getUserName();
                userName = (userName == null || userName.isEmpty() ? "Anonymous" : userName);
                
                this.tags.put("user_name", userName);
                break;

            } else if (cause instanceof Cause.UpstreamCause) {
                this.tags.put("user_id", "upstream");
                break;
            } else if (cause instanceof TimerTrigger.TimerTriggerCause) {
                this.tags.put("user_id", "timer");
                break;
            } else {
                this.tags.put("user_id", "Unknown");
                this.tags.put("user_name", "SYSTEM");
                break;
            }
        }
    }

    /**
     * Get the SCM info from the build and add it to tags.
     *
     */
    private void addSCMInfo() {
        EnvVars environment = null;
        try {
            environment = this.build.getEnvironment(this.listener);

            this.listener.getLogger().println(environment);
        } catch (IOException e) {
            this.listener.getLogger().println("BigPanda Notifier: Could not retrieve environment.");
        } catch (InterruptedException e) {
            this.listener.getLogger().println("BigPanda Notifier: Could not retrieve environment.");
        }

        if (environment != null) {
            if (environment.get("GIT_URL") != null) {
                this.tags.put("git_url", environment.get("GIT_URL"));
            }
            if (environment.get("GIT_COMMIT") != null) {
                this.tags.put("git_commit", environment.get("GET_COMMIT"));
            }
            if (environment.get("BRANCH_NAME") != null) {
                this.tags.put("branch_name", environment.get("BRANCH_NAME"));
            }
            if (environment.get("NODE_NAME") != null) {
                this.tags.put("node_names", environment.get("NODE_NAME"));
            }
            if (environment.get("NODE_LABELS") != null) {
                this.tags.put("node_labels", environment.get("NODE_LABELS"));
            }
        }
    }

    /**
     * Returns the end time of the Build if the Duration is non-zero
     * 
     * @return String representation of the end time of the Build
     */
    private long getEndTime() {
        if (this.build.getDuration() == 0) {
            return 0;
        } else {
            return (((this.build.getStartTimeInMillis() + this.build.getDuration()) / 1000));
        }
    }

    /**
     * Will attempt to get the description of the current build. If non is provided
     * it will attempt to retrieve the description of the parent.
     * 
     * @return String description of build
     */
    private String getBuildDescription() {
        Run<?, ?> bld = this.build;
        Job<?, ?> parent;

        if (bld.getDescription() != null && bld.getDescription().trim().length() > 0) {
            return bld.getDescription();
        } else {
            parent = bld.getParent();

            if (parent.getDescription() == null || parent.getDescription().trim().length() <= 0) {
                return parent.getDescription();
            } else {
                return "No Description";
            }
        }
    }

    /**
     * Generates a MD5 hascode
     * 
     * @param {String} input - string to convert
     * @return String hascode representation of input string
     */
    private String getMd5(String input) {
        String hashText = "";

        // Static getInstance method is called with hashing MD5
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            // digest() method is called to calculate message digest 
            //  of an input digest() return array of byte 
            byte[] messageDigest = md.digest(input.getBytes(Charset.forName("UTF-8"))); 
  
            // Convert byte array into signum representation 
            BigInteger no = new BigInteger(1, messageDigest); 
  
            // Convert message digest into hex value 
            hashText = no.toString(16);
            while (hashText.length() < 32) {
                hashText = "0" + hashText;
            } 
        } catch (NoSuchAlgorithmException e) {
            this.listener.getLogger().println(e.getMessage());
        } 

        return hashText;
    }
}
