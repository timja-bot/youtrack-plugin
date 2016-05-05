package org.jenkinsci.plugins.youtrack;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.BuildListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jenkinsci.plugins.youtrack.youtrackapi.Issue;
import org.jenkinsci.plugins.youtrack.youtrackapi.Suggestion;
import org.jenkinsci.plugins.youtrack.youtrackapi.User;
import org.jenkinsci.plugins.youtrack.youtrackapi.YouTrackServer;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is command for executing arbitrary commands on issues.
 */
public class ExecuteCommandAction extends Builder {
    private static final Logger LOGGER = Logger.getLogger(ExecuteCommandAction.class.getName());

    @Getter @Setter private String command;
    @Getter @Setter private String search;
    @Getter @Setter private String issueInText;
    @Getter @Setter private String comment;
    @Getter @Setter private String commentVisibility;

    @DataBoundConstructor
    public ExecuteCommandAction(String command, String search, String issueInText, String comment, String commentVisibility) {
        this.command = command;
        this.search = search;
        this.issueInText = issueInText;
        this.comment = comment;
        this.commentVisibility = commentVisibility;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        YouTrackSite youTrackSite = getYouTrackSite(build);
        if (youTrackSite != null) {
            if (youTrackSite.isPluginEnabled()) {

                YouTrackServer youTrackServer = getYouTrackServer(youTrackSite);
                User user = youTrackServer.login(youTrackSite.getUsername(), youTrackSite.getPassword());
                if (user != null && user.isLoggedIn()) {
                    EnvVars environment = build.getEnvironment(listener);

                    try {
                        String changes = createChangesString(build,listener);
                        environment.put("YOUTRACK_CHANGES", changes);
                    } catch (InvocationTargetException e) {
                        LOGGER.error(e);
                    } catch (IllegalAccessException e) {
                        LOGGER.error(e);
                    }

                    String searchQuery = environment.expand(search);
                    String commandToExecute = environment.expand(command);
                    String expandedIssueInText = environment.expand(issueInText);

                    Set<Issue> issues = new HashSet<Issue>();
                    if (StringUtils.isNotBlank(searchQuery)) {
                        issues.addAll(youTrackServer.search(user, searchQuery));
                    }
                    if (StringUtils.isNotBlank(expandedIssueInText)) {
                        issues.addAll(findIssuesInText(build, environment, expandedIssueInText));
                    }
                    List<Command> appliedCommands = new ArrayList<Command>();
                    String expandedComment = environment.expand(comment);
                    for (Issue issue : issues) {
                        Command appliedCommand = youTrackServer.applyCommand(youTrackSite.getName(), user, issue, commandToExecute, expandedComment, commentVisibility, null, true);
                        appliedCommands.add(appliedCommand);
                        if (appliedCommand.getStatus() != Command.Status.OK) {
                            youTrackSite.failed(build);
                        }
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
                    youTrackSite.failed(build);
                    listener.getLogger().println("User not logged in");
                }
            } else {
                listener.getLogger().println("Plugin not enabled");
            }
        } else {
            listener.getLogger().println("No site configured");
        }
        return true;
    }

    String createChangesString(AbstractBuild<?, ?> build, BuildListener listener) throws InvocationTargetException, IllegalAccessException {
        StringBuilder stringBuilder = new StringBuilder();
        ChangeLogSet<? extends ChangeLogSet.Entry> changeSet = build.getChangeSet();
        EnvVars environment = null;
        try {
            environment = build.getEnvironment(listener);
        } catch (IOException e) {
            LOGGER.error(e,e);
        } catch (InterruptedException e) {
            LOGGER.error(e,e);
        }
        if (changeSet != null) {
            for (ChangeLogSet.Entry entry : changeSet) {
                AbstractProject<?, ?> project = build.getProject();
                SCM scm = null;
                if (project != null) {
                    scm = project.getScm();
                }
                String message = YoutrackIssueUpdater.getMessage(scm,entry, listener, environment, build);
                stringBuilder.append(entry.getMsg());
                stringBuilder.append("\n\n");
            }
        }
        return stringBuilder.toString();
    }

    YouTrackServer getYouTrackServer(YouTrackSite youTrackSite) {
        return new YouTrackServer(youTrackSite.getUrl());
    }

    YouTrackSite getYouTrackSite(AbstractBuild<?, ?> build) {
        return YouTrackSite.get(build.getProject());
    }

    private List<Issue> findIssuesInText(AbstractBuild<?, ?> build, EnvVars environment, String issueInText) {
        String textToSearchForIssues = environment.expand(issueInText);
        YouTrackSaveProjectShortNamesAction projectShortNamesAction = build.getAction(YouTrackSaveProjectShortNamesAction.class);
        if (projectShortNamesAction != null) {
            return findIssuesIds(projectShortNamesAction.getShortNames(), textToSearchForIssues);
        }
        return new ArrayList<Issue>();
    }

    private List<Issue> findIssuesIds(List<String> projects, String issueText) {
        ArrayList<Issue> issues = new ArrayList<Issue>();
        String projectIds = StringUtils.join(projects, "|");
        Pattern projectPattern = Pattern.compile("((" + projectIds + ")-" + "(\\d+)" + ")");
        Matcher matcher = projectPattern.matcher(issueText);
        while (matcher.find()) {
            if (matcher.groupCount() >= 1) {
                String issueId = matcher.group(0);
                issues.add(new Issue(issueId));
            }
        }
        return issues;
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
            if (youTrackSite != null) {
                YouTrackServer youTrackServer = new YouTrackServer(youTrackSite.getUrl());
                User user = youTrackServer.login(youTrackSite.getUsername(), youTrackSite.getPassword());
                if (user != null) {
                    List<Suggestion> suggestions = youTrackServer.searchSuggestions(user, value);
                    for (Suggestion suggestion : suggestions) {
                        if (suggestion.getCompletionStart() == 0) {
                            String completeSuggestion = emptyIfNull(suggestion.getPrefix()) + suggestion.getOption() + emptyIfNull(suggestion.getSuffix());
                            autoCompletionCandidates.add(completeSuggestion);
                        } else {
                            String validValue = value.substring(0, suggestion.getCompletionStart());
                            String completeSuggestion = emptyIfNull(suggestion.getPrefix()) + suggestion.getOption() + emptyIfNull(suggestion.getSuffix());
                            autoCompletionCandidates.add(validValue + completeSuggestion);
                        }
                    }
                }
            }
            return autoCompletionCandidates;
        }


        public String emptyIfNull(String text) {
            if (text == null) return "";
            return text;
        }

        @SuppressWarnings("UnusedDeclaration")
        public AutoCompletionCandidates doAutoCompleteCommentVisibility(@AncestorInPath AbstractProject project, @QueryParameter String value) {
            return YouTrackProjectProperty.getPossibleGroups(project, value);
        }

    }

}
