package org.jenkinsci.plugins.youtrack;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.listeners.SCMListener;
import hudson.scm.ChangeLogSet;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.youtrack.youtrackapi.Issue;
import org.jenkinsci.plugins.youtrack.youtrackapi.Project;
import org.jenkinsci.plugins.youtrack.youtrackapi.User;
import org.jenkinsci.plugins.youtrack.youtrackapi.YouTrackServer;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTrackSCMListener extends SCMListener {

    @Override
    public void onChangeLogParsed(AbstractBuild<?, ?> build, BuildListener listener, ChangeLogSet<?> changeLogSet) throws Exception {
        if (build.getRootBuild().equals(build)) {
            YouTrackSite youTrackSite = YouTrackSite.get(build.getProject());
            if (youTrackSite == null || !youTrackSite.isPluginEnabled()) {
                return;
            }

            Iterator<? extends ChangeLogSet.Entry> changeLogIterator = changeLogSet.iterator();

            YouTrackServer youTrackServer = new YouTrackServer(youTrackSite.getUrl());
            User user = youTrackServer.login(youTrackSite.getUsername(), youTrackSite.getPassword());
            if (user == null || !user.isLoggedIn()) {
                listener.getLogger().append("FAILED: log in with set YouTrack user");
            }
            build.addAction(new YouTrackIssueAction(build.getProject()));

            List<Project> projects = youTrackServer.getProjects(user);


            if (projects != null) {
                build.addAction(new YouTrackSaveProjectShortNamesAction(projects));
            } else {
                AbstractBuild<?, ?> lastSuccessfulBuild = build.getProject().getLastStableBuild();
                YouTrackSaveProjectShortNamesAction action = lastSuccessfulBuild.getAction(YouTrackSaveProjectShortNamesAction.class);
                if (action != null) {
                    List<String> shortNames = action.getShortNames();
                    List<Project> previousProjects = new ArrayList<Project>();
                    for (String shortName : shortNames) {
                        Project prevProject = new Project();
                        prevProject.setShortName(shortName);
                        previousProjects.add(prevProject);
                        projects = previousProjects;
                    }
                } else {
                    projects = new ArrayList<Project>();
                }
            }


            YouTrackCommandAction commandAction = new YouTrackCommandAction(build);

            List<Issue> fixedIssues = new ArrayList<Issue>();

            while (changeLogIterator.hasNext()) {
                ChangeLogSet.Entry next = changeLogIterator.next();

                String msg;
                if (next.getClass().getCanonicalName().equals("hudson.plugins.git.GitChangeSet")) {

                    try {
                        Method getComment = next.getClass().getMethod("getComment");
                        Object message = getComment.invoke(next);
                        msg = (String) message;
                    } catch (NoSuchMethodException e) {
                        msg = next.getMsg();
                    } catch (SecurityException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    msg = next.getMsg();
                }

                List<Command> commands = addCommentIfEnabled(build, youTrackSite, youTrackServer, user, projects, msg, listener);
                for (Command command : commands) {
                    commandAction.addCommand(command);
                }


                if (projects != null) {
                    List<Project> youtrackProjects = new ArrayList<Project>(projects.size());
                    Set<String> excludedProjectNames = getExcludedProjects(youTrackSite);


                    for (Project project : projects) {
                        if (!excludedProjectNames.contains(project.getShortName())) {
                            youtrackProjects.add(project);
                        }
                    }

                    YouTrackPlugin plugin = Jenkins.getInstance().getPlugin(YouTrackPlugin.class);
                    YoutrackProcessedRevisionsSaver revisionsSaver = plugin.getRevisionsSaver();
                    if ((youTrackSite.isTrackCommits() && !revisionsSaver.isProcessed(next.getCommitId())) || !youTrackSite.isTrackCommits()) {
                        List<Command> commandList = executeCommandsIfEnabled(listener, youTrackSite, youTrackServer, user, youtrackProjects, fixedIssues, next, msg);
                        for (Command command : commandList) {
                            commandAction.addCommand(command);
                        }
                        if (youTrackSite.isTrackCommits() && !commandList.isEmpty()) {
                            revisionsSaver.addProcessed(next.getCommitId());
                        }
                    }
                }

            }

            int numCommands = commandAction.getNumCommands();

            if (numCommands > 0) {
                build.addAction(commandAction);
            }

            build.addAction(new YouTrackSaveFixedIssues(fixedIssues));
        }
        super.onChangeLogParsed(build, listener, changeLogSet);
    }

    private Set<String> getExcludedProjects(YouTrackSite youTrackSite) {
        String executeProjectLimits = youTrackSite.getExecuteProjectLimits();
        if (executeProjectLimits == null || !executeProjectLimits.trim().equals("")) {
            return new HashSet<String>();
        } else {
            HashSet<String> nameSet = new HashSet<String>();
            String[] names = executeProjectLimits.split(",");
            for (String name : names) {
                if (!name.trim().equals("")) {
                    nameSet.add(name);
                }
            }
            return nameSet;
        }
    }

    /**
     * Executes the commands if execute commands is enabled;
     *
     * @param listener       the listener.
     * @param youTrackSite   YouTrack site.
     * @param youTrackServer YouTrack server.
     * @param user           user.
     * @param projects       projects.
     * @param fixedIssues    list to fill with fixed issues.
     * @param changeLogEntry the ChangeLogEntry.
     * @param msg            the message to parse.
     * @return the list of commands tried to be executed.
     */
    List<Command> executeCommandsIfEnabled(BuildListener listener, YouTrackSite youTrackSite, YouTrackServer youTrackServer, User user, List<Project> projects, List<Issue> fixedIssues, ChangeLogSet.Entry changeLogEntry, String msg) {
        List<Command> commands = new ArrayList<Command>();
        if (youTrackSite.isCommandsEnabled()) {
            String[] lines = msg.split("\n");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.contains("#")) {

                    StringBuilder stringBuilder = new StringBuilder();
                    for (Project project : projects) {
                        stringBuilder.append("#").append(project.getShortName()).append("|");
                    }
                    if (stringBuilder.length() > 0) {
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    }

                    String comment = null;
                    String issueStart = line.substring(line.indexOf("#") + 1);
                    boolean isSilent = false;
                    int hashPosition = line.indexOf("#");
                    if (hashPosition != 0) {
                        char charBefore = line.charAt(hashPosition - 1);
                        if (charBefore == '!') {
                            isSilent = true;
                        }
                    }

                    if (i + 1 < lines.length) {
                        String l = lines[i + 1];
                        if (!l.contains("#")) {
                            comment = l;
                        }
                    }

                    Project p = null;
                    for (Project project : projects) {
                        if (issueStart.startsWith(project.getShortName() + "-")) {
                            p = project;
                        }
                    }

                    findIssueId(youTrackSite, youTrackServer, user, fixedIssues, changeLogEntry, comment, issueStart, p, listener, commands, isSilent);
                }
            }
        }
        return commands;
    }

    private void findIssueId(YouTrackSite youTrackSite, YouTrackServer youTrackServer, User user, List<Issue> fixedIssues, ChangeLogSet.Entry next, String comment, String issueStart, Project p, BuildListener listener, List<Command> commands, boolean silent) {
        if (p != null) {
            Pattern projectPattern = Pattern.compile("(" + p.getShortName() + "-" + "(\\d+)" + ")( )?(.*)");

            Matcher matcher = projectPattern.matcher(issueStart);
            while (matcher.find()) {
                if (matcher.groupCount() >= 1) {
                    String issueId = p.getShortName() + "-" + matcher.group(2);
                    User userByEmail = null;
                    if (youTrackSite.isRunAsEnabled()) {
                        String address = next.getAuthor().getProperty(Mailer.UserProperty.class).getAddress();
                        userByEmail = youTrackServer.getUserByEmail(user, address);
                        if (userByEmail == null) {
                            listener.getLogger().println("Failed to find user with e-mail: " + address);
                        }
                    }

                    String stateFieldName = "State";
                    if (youTrackSite.getStateFieldName() != null && !youTrackSite.getStateFieldName().equals("")) {
                        stateFieldName = youTrackSite.getStateFieldName();
                    }

                    //Get the issue state, then apply command, and get the issue state again.
                    //to know whether the command has been marked as fixed, instead of trying to
                    //interpret the command. This means however that there is a possibility for
                    //the user to change state between the before and the after call, so the after
                    //state can be affected by something else than the command.
                    Issue before = youTrackServer.getIssue(user, issueId, stateFieldName);
                    String command = matcher.group(4);
                    boolean isSilent = youTrackSite.isSilentCommands() || silent;
                    Command cmd = youTrackServer.applyCommand(youTrackSite.getName(), user, new Issue(issueId), command, comment, userByEmail, !isSilent);
                    if (cmd.getStatus() == Command.Status.OK) {
                        listener.getLogger().println("Applied command: " + command + " to issue: " + issueId);
                    } else {
                        listener.getLogger().println("FAILED: Applying command: " + command + " to issue: " + issueId);
                    }
                    commands.add(cmd);
                    Issue after = youTrackServer.getIssue(user, issueId, stateFieldName);

                    Set<String> fixedValues = new HashSet<String>();
                    if (youTrackSite.getFixedValues() != null && !youTrackSite.getFixedValues().equals("")) {
                        String values = youTrackSite.getFixedValues();
                        String[] fixedValueArray = values.split(",");
                        for (String fixedValueFromArray : fixedValueArray) {
                            if (!fixedValueFromArray.trim().equals("")) {
                                fixedValues.add(fixedValueFromArray.trim());
                            }
                        }
                    } else {
                        fixedValues.add("Fixed");
                    }

                    if (before != null && after != null && !fixedValues.contains(before.getState()) && fixedValues.contains(after.getState())) {
                        fixedIssues.add(after);
                    }

                }

            }

        }
    }

    private List<Command> addCommentIfEnabled(AbstractBuild<?, ?> build, YouTrackSite youTrackSite, YouTrackServer youTrackServer, User user, List<Project> projects, String msg, BuildListener listener) {
        List<Command> commands = new ArrayList<Command>();
        if (youTrackSite.isCommentEnabled()) {
            for (Project project1 : projects) {
                String shortName = project1.getShortName();
                Pattern projectPattern = Pattern.compile("(" + shortName + "-" + "(\\d+)" + ")");
                Matcher matcher = projectPattern.matcher(msg);
                while (matcher.find()) {
                    if (matcher.groupCount() >= 1) {
                        String issueId = shortName + "-" + matcher.group(2);
                        //noinspection deprecation
                        String commentText = "Related build: " + build.getAbsoluteUrl();
                        Command comment = youTrackServer.comment(youTrackSite.getName(), user, new Issue(issueId), commentText, youTrackSite.getLinkVisibility(), youTrackSite.isSilentLinks());
                        commands.add(comment);
                        if (comment.getStatus() == Command.Status.OK) {
                            listener.getLogger().println("Commented on " + issueId);
                        } else {
                            listener.getLogger().println("FAILED: Commented on " + issueId);
                        }
                    }
                }
            }
        }
        return commands;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof YouTrackSCMListener;
    }
}
