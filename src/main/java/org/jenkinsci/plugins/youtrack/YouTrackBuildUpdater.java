package org.jenkinsci.plugins.youtrack;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.CopyOnWriteList;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.youtrack.youtrackapi.*;
import org.jenkinsci.plugins.youtrack.youtrackapi.User;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Updates build bundle.
 */
public class YouTrackBuildUpdater extends Recorder {

    /**
     * This was the name, where there was an implicit ${BUILD_NUMBER} (name) format.
     *
     * @deprecated {@link #buildName} should be used instead.
     */
    private String name;
    /**
     * Name of build to create and use for setting Fixed in build.
     */
    @Setter
    private String buildName;
    @Getter
    @Setter
    private String bundleName;
    @Getter
    @Setter
    private boolean markFixedIfUnstable;
    @Getter
    @Setter
    private boolean onlyAddIfHasFixedIssues;
    @Getter
    @Setter
    private boolean runSilently;
    @Getter
    @Setter
    private String buildUpdateCommand;
    @Getter
    @Setter
    private List<BundleFieldProject> bundles = new ArrayList<>();

    @DataBoundConstructor
    public YouTrackBuildUpdater(String name, String bundleName, String buildName, boolean markFixedIfUnstable, boolean onlyAddIfHasFixedIssues, boolean runSilently, String buildUpdateCommand, List<BundleFieldProject> bundles) {
        this.name = name;
        this.bundleName = bundleName;

        this.buildName = buildName;
        this.markFixedIfUnstable = markFixedIfUnstable;
        this.onlyAddIfHasFixedIssues = onlyAddIfHasFixedIssues;
        this.runSilently = runSilently;
        this.buildUpdateCommand = buildUpdateCommand;
        this.bundles = bundles;
        if (buildUpdateCommand == null) {
            this.buildUpdateCommand = "Fixed in build: ${YOUTRACK_BUILD_NAME}";
        }
    }

    @Deprecated
    public String getName() {
        return name;
    }

    @Deprecated
    public void setName(String name) {
        this.name = name;
    }

    public String getBuildName() {
        if (name != null && buildName == null) {
            this.buildName = "${BUILD_NUMBER} (" + name + ")";
        }
        return buildName;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        YouTrackSite youTrackSite = getYouTrackSite(build);
        if (youTrackSite == null || !youTrackSite.isPluginEnabled()) {
            listener.getLogger().println("No YouTrack site configured");
            return true;
        }


        YouTrackSaveFixedIssues action = build.getAction(YouTrackSaveFixedIssues.class);

        YouTrackCommandAction youTrackCommandAction = build.getAction(YouTrackCommandAction.class);
        if (youTrackCommandAction == null) {
            youTrackCommandAction = new YouTrackCommandAction(build);
            build.addAction(youTrackCommandAction);
        }

        //Return early if there is no build to be added
        if (onlyAddIfHasFixedIssues) {
            if (action == null || action.getIssueIds().isEmpty()) {
                listener.getLogger().println("No build to add");
                return true;
            }
        }

        YouTrackServer youTrackServer = getYouTrackServer(youTrackSite);
        User user = youTrackServer.login(youTrackSite.getUsername(), youTrackSite.getPassword());
        if (user == null || !user.isLoggedIn()) {
            listener.getLogger().println("FAILED: to log in to youtrack");
            youTrackSite.failed(build);
            return true;
        }
        EnvVars environment = build.getEnvironment(listener);
        String buildName;
        if (getBuildName() == null || getBuildName().equals("")) {
            buildName = String.valueOf(build.getNumber());
        } else {

            buildName = environment.expand(getBuildName());

        }


        List<String> projectIds = new ArrayList<String>();
        if (action != null) {
            List<String> issueIds = action.getIssueIds();
            for (String issueId : issueIds) {
                String[] split = issueId.split("\\-");
                projectIds.add(split[0]);
            }
        }

        Set<String> bundleNames = new HashSet<String>();
        if (bundles != null) {
            for (BundleFieldProject bundleFieldProject : bundles) {
                String inputBundleName = youTrackServer.getBuildBundleNameForField(user, bundleFieldProject.getProjectId(), bundleFieldProject.getFieldName());
                if (inputBundleName != null && !inputBundleName.isEmpty()) {

                    bundleNames.add(inputBundleName);
                }
            }
        }
        if (buildName != null && buildName.length() != 0) {
            bundleNames.add(environment.expand(getBundleName()));
        }

        for (String bundleName : bundleNames) {
            if (bundleName != null && !bundleName.isEmpty()) {


                Command addedBuild = youTrackServer.addBuildToBundle(youTrackSite.getName(), user, bundleName, buildName);
                if (addedBuild.getStatus() == Command.Status.OK) {
                    listener.getLogger().println("Added build " + buildName + " to bundle: " + bundleName);
                } else {
                    listener.getLogger().println("FAILED: adding build " + buildName + " to bundle: " + bundleName);
                    youTrackSite.failed(build);
                }
                youTrackCommandAction.addCommand(addedBuild);
            }
        }


        if (action != null) {
            List<String> issueIds = action.getIssueIds();
            boolean stable = build.getResult().isBetterOrEqualTo(Result.SUCCESS);
            boolean unstable = build.getResult().isBetterOrEqualTo(Result.UNSTABLE);


            if (stable || (isMarkFixedIfUnstable() && unstable)) {

                for (String issueId : issueIds) {
                    Issue issue = new Issue(issueId);

                    environment.put("YOUTRACK_BUILD_NAME", buildName);

                    String commandValue = environment.expand(buildUpdateCommand);
                    Command command = youTrackServer.applyCommand(youTrackSite.getName(), user, issue, commandValue, null, null, null, !runSilently);
                    if (command.getStatus() == Command.Status.OK) {
                        listener.getLogger().println("Updated Fixed in build to " + buildName + " for " + issueId);
                    } else {
                        youTrackSite.failed(build);
                        listener.getLogger().println("FAILED: updating Fixed in build to " + buildName + " for " + issueId);
                    }
                    youTrackCommandAction.addCommand(command);
                }
            }

        }

        return true;
    }

    YouTrackServer getYouTrackServer(YouTrackSite youTrackSite) {
        return new YouTrackServer(youTrackSite.getUrl());
    }

    YouTrackSite getYouTrackSite(AbstractBuild<?, ?> build) {
        return YouTrackSite.get(build.getProject());
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "YouTrack Build Updater";
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(YouTrackBuildUpdater.class, formData);
        }

        @SuppressWarnings("UnusedDeclaration")
        public AutoCompletionCandidates doAutoCompleteBundleName(@AncestorInPath AbstractProject project, @QueryParameter String value) {
            YouTrackSite youTrackSite = YouTrackSite.get(project);
            AutoCompletionCandidates autoCompletionCandidates = new AutoCompletionCandidates();
            if (youTrackSite != null) {
                YouTrackServer youTrackServer = new YouTrackServer(youTrackSite.getUrl());
                User user = youTrackServer.login(youTrackSite.getUsername(), youTrackSite.getPassword());
                if (user != null) {
                    List<BuildBundle> bundles = youTrackServer.getBuildBundles(user);
                    for (BuildBundle bundle : bundles) {
                        if (bundle.getName().toLowerCase().contains(value.toLowerCase())) {
                            autoCompletionCandidates.add(bundle.getName());
                        }
                    }
                }
            }
            return autoCompletionCandidates;
        }

        @SuppressWarnings("UnusedDeclaration")
        public AutoCompletionCandidates doAutoCompleteFieldBuildBundleToUpdate(@AncestorInPath AbstractProject project, @QueryParameter String value) {
            return YouTrackProjectProperty.getFields(project, value);
        }

        @SuppressWarnings("UnusedDeclaration")
        public AutoCompletionCandidates doAutoCompleteProjectId(@AncestorInPath AbstractProject project, @QueryParameter String value) {
            return YouTrackProjectProperty.getProjects(project, value);
        }

        @SuppressWarnings("UnusedDeclaration")
        public AutoCompletionCandidates doAutoCompleteFieldName(@AncestorInPath AbstractProject project, @QueryParameter String value) {
            return YouTrackProjectProperty.getFields(project, value);
        }
    }

    public static class BundleFieldProject {
        private String projectId;
        private String fieldName;

        public BundleFieldProject() {
        }

        @DataBoundConstructor
        public BundleFieldProject(String projectId, String fieldName) {
            this.projectId = projectId;
            this.fieldName = fieldName;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }
    }
}
