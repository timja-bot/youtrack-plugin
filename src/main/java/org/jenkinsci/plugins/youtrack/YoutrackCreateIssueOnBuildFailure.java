package org.jenkinsci.plugins.youtrack;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.youtrack.youtrackapi.*;
import org.jenkinsci.plugins.youtrack.youtrackapi.Project;
import org.jenkinsci.plugins.youtrack.youtrackapi.User;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.List;

/**
 * Post-build step to create an issue in Youtrack if the build fails.
 */
public class YoutrackCreateIssueOnBuildFailure extends Notifier {
    private String project;
    private String summary;
    private String description;
    private String threshold;
    private String visibility;
    private String command;

    public static final String FAILURE = "failure";

    public static final String FAILUREORUNSTABL = "failureOrUnstable";


    @DataBoundConstructor
    public YoutrackCreateIssueOnBuildFailure(String project, String summary, String description, String threshold, String visibility, String command) {
        this.project = project;
        this.summary = summary;
        this.description = description;
        this.threshold = threshold;
        this.visibility = visibility;
        this.command = command;
    }

    public String getThreshold() {
        return threshold;
    }

    public void setThreshold(String threshold) {
        this.threshold = threshold;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        YouTrackSite youTrackSite = getYouTrackSite(build);
        if (youTrackSite == null) {
            listener.getLogger().println("No YouTrack site configured");
            return true;
        }

        if (shouldCreateIssue(build)) {
            YouTrackServer server = getYouTrackServer(youTrackSite);
            User user = server.login(youTrackSite.getUsername(), youTrackSite.getPassword());
            if (user == null) {
                listener.getLogger().println("Could not login user to YouTrack");
                return true;
            }

            EnvVars environment = build.getEnvironment(listener);
            String title = environment.expand(this.summary);
            String description = environment.expand(this.description);
            String command = environment.expand(this.command);

            if (title == null || "".equals(title)) {
                title = "Build failure in build " + build.getNumber();
            }
            if (description == null || "".equals(description)) {
                description = getAbsoluteUrl(build);
            }

            Command issue = server.createIssue(youTrackSite.getName(), user, project, title, description, command);

        }

        return true;

    }

    public String getAbsoluteUrl(AbstractBuild<?, ?> build) {
        return build.getAbsoluteUrl();
    }

    YouTrackServer getYouTrackServer(YouTrackSite youTrackSite) {
        return new YouTrackServer(youTrackSite.getUrl());
    }

    YouTrackSite getYouTrackSite(AbstractBuild<?, ?> build) {
        return YouTrackSite.get(build.getProject());
    }


    private boolean shouldCreateIssue(AbstractBuild<?, ?> build) {
        Result result = build.getResult();
        if (FAILURE.equals(threshold) && result.isBetterThan(Result.FAILURE)) {
            return false;
        } else if (FAILUREORUNSTABL.equals(threshold) && result.isBetterThan(Result.UNSTABLE)) {
            return false;
        }
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return org.jenkinsci.plugins.youtrack.Messages.create_issue_on_build_failure_header();
        }


        @Override
        public Publisher newInstance(final StaplerRequest req, final JSONObject formData) {
            return req.bindJSON(YoutrackCreateIssueOnBuildFailure.class, formData);
        }


        public FormValidation doCheckProject(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }


        public AutoCompletionCandidates doAutoCompleteProject(@AncestorInPath AbstractProject project, @QueryParameter String value) {
            YouTrackSite youTrackSite = YouTrackSite.get(project);
            AutoCompletionCandidates autoCompletionCandidates = new AutoCompletionCandidates();
            if(youTrackSite != null) {
                YouTrackServer youTrackServer = new YouTrackServer(youTrackSite.getUrl());
                User user = youTrackServer.login(youTrackSite.getUsername(), youTrackSite.getPassword());
                if(user != null) {
                    List<Project> groups = youTrackServer.getProjects(user);
                    for (Project youtrackProject : groups) {
                        if(youtrackProject.getShortName().toLowerCase().contains(value.toLowerCase())) {
                            autoCompletionCandidates.add(youtrackProject.getShortName());
                        }
                    }
                }
            }
            return autoCompletionCandidates;
        }

    }
}
