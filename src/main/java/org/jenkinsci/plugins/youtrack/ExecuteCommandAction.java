package org.jenkinsci.plugins.youtrack;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.youtrack.youtrackapi.BuildBundle;
import org.jenkinsci.plugins.youtrack.youtrackapi.Issue;
import org.jenkinsci.plugins.youtrack.youtrackapi.User;
import org.jenkinsci.plugins.youtrack.youtrackapi.YouTrackServer;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is command for executing arbitrary commands on issues.
 */
public class ExecuteCommandAction extends Builder {
    @Getter @Setter private String command;
    @Getter @Setter private String search;
    @Getter @Setter private String comment;
    @Getter @Setter private boolean notify;

    @DataBoundConstructor
    public ExecuteCommandAction(String command, String search, String comment, boolean notify) {
        this.command = command;
        this.search = search;
        this.comment = comment;
        this.notify = notify;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        YouTrackSite youTrackSite = YouTrackSite.get(build.getProject());
        if (youTrackSite != null) {
            if (youTrackSite.isPluginEnabled()) {
                EnvVars environment = build.getEnvironment(listener);
                String searchQuery = environment.expand(search);
                String commandToExecute = environment.expand(command);

                YouTrackServer youTrackServer = new YouTrackServer(youTrackSite.getUrl());
                User user = youTrackServer.login(youTrackSite.getUsername(), youTrackSite.getPassword());
                if (user != null && user.isLoggedIn()) {
                    List<Issue> issues = youTrackServer.search(user, searchQuery);
                    List<Command> appliedCommands = new ArrayList<Command>();
                    for (Issue issue : issues) {
                        Command appliedCommand = youTrackServer.applyCommand(youTrackSite.getName(), user, issue, commandToExecute, comment, null, notify);
                        appliedCommands.add(appliedCommand);
                    }
                    if (!appliedCommands.isEmpty()) {
                        YouTrackCommandAction youTrackCommandAction = build.getAction(YouTrackCommandAction.class);
                        if (youTrackCommandAction == null) {
                            youTrackCommandAction = new YouTrackCommandAction(build);
                            build.addAction(youTrackCommandAction);
                        }
                        for (Command appliedCommand : appliedCommands) {
                            youTrackCommandAction.addCommand(appliedCommand);
                        }
                    }
                    if (issues.isEmpty()) {
                        listener.getLogger().println("No issues to apply command for");
                    }
                } else {
                    listener.getLogger().println("User not logged in");
                }
            } else {
                listener.getLogger().print("Plugin not enabled");
            }
        } else {
            listener.getLogger().println("No site configured");
        }
        return true;
    }


    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Execute YouTrack Command";
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(ExecuteCommandAction.class, formData);
        }

        @SuppressWarnings("UnusedDeclaration")
        public AutoCompletionCandidates doAutoCompleteSearch(@AncestorInPath AbstractProject project, @QueryParameter String value) {
            YouTrackSite youTrackSite = YouTrackSite.get(project);
            AutoCompletionCandidates autoCompletionCandidates = new AutoCompletionCandidates();
            if(youTrackSite != null) {
                YouTrackServer youTrackServer = new YouTrackServer(youTrackSite.getUrl());
                User user = youTrackServer.login(youTrackSite.getUsername(), youTrackSite.getPassword());
                if(user != null) {
                    List<String> strings = youTrackServer.searchSuggestions(user, value);
                    for (String string : strings) {
                        autoCompletionCandidates.add(value + " " + string);
                    }
                }
            }
            return autoCompletionCandidates;
        }

    }
}
