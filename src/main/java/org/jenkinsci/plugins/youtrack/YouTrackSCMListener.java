package org.jenkinsci.plugins.youtrack;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.ChangeLogSet;
import hudson.tasks.Mailer;
import org.jenkinsci.plugins.youtrack.youtrackapi.Issue;
import org.jenkinsci.plugins.youtrack.youtrackapi.Project;
import org.jenkinsci.plugins.youtrack.youtrackapi.User;
import org.jenkinsci.plugins.youtrack.youtrackapi.YouTrackServer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTrackSCMListener extends SCMListener {

    @Override
    public void onChangeLogParsed(AbstractBuild<?, ?> build, BuildListener listener, ChangeLogSet<?> changelog) throws Exception {
        if (build.getRootBuild().equals(build)) {
            YouTrackSite youTrackSite = YouTrackSite.get(build.getProject());
            if (youTrackSite == null || !youTrackSite.isPluginEnabled()) {
                return;
            }

            Iterator<? extends ChangeLogSet.Entry> changeLogIterator = changelog.iterator();

            YouTrackServer youTrackServer = new YouTrackServer(youTrackSite.getUrl());
            User user = youTrackServer.login(youTrackSite.getUsername(), youTrackSite.getPassword());
            if (user == null) {
                listener.getLogger().append("FALIED: log in with set YouTrack user");
                return;
            }
            build.addAction(new YouTrackIssueAction(build.getProject()));

            List<Project> projects = youTrackServer.getProjects(user);
            build.addAction(new YouTrackSaveProjectShortNamesAction(projects));

            List<Issue> fixedIssues = new ArrayList<Issue>();

            while (changeLogIterator.hasNext()) {
                ChangeLogSet.Entry next = changeLogIterator.next();
                String msg = next.getMsg();

                addCommentIfEnabled(build, youTrackSite, youTrackServer, user, projects, msg, listener);

                executeCommandsIfEnabled(listener, youTrackSite, youTrackServer, user, projects, fixedIssues, next, msg);
            }

            build.addAction(new YouTrackSaveFixedIssues(fixedIssues));

        }
        super.onChangeLogParsed(build, listener, changelog);
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
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);

                    String comment = null;
                    String issueStart = line.substring(line.indexOf("#") + 1);
                    boolean isSilent = false;
                    int hashPosition = line.indexOf("#");
                    if (hashPosition != 0) {
                        char charBefore = line.charAt(hashPosition - 1);
                        if(charBefore == '!') {
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
                    Command cmd = new Command();
                    cmd.setCommand(command);
                    boolean isSilent = youTrackSite.isSilentCommands() || silent;
                    cmd.setSilent(isSilent);
                    cmd.setIssueId(issueId);
                    cmd.setUsername(user.getUsername());
                    cmd.setDate(new Date());
                    cmd.setSiteName(youTrackSite.getUrl());
                    cmd.setStatus(Command.Status.OK);
                    commands.add(cmd);
                    boolean applied = youTrackServer.applyCommand(user, new Issue(issueId), command, comment, userByEmail, !isSilent);
                    if (applied) {
                        listener.getLogger().println("Applied command: " + command + " to issue: " + issueId);
                    } else {
                        listener.getLogger().println("FAILED: Applying command: " + command + " to issue: " + issueId);
                    }
                    Issue after = youTrackServer.getIssue(user, issueId, stateFieldName);

                    Set<String> fixedValues = new HashSet<String>();
                    if (youTrackSite.getFixedValues() != null && !youTrackSite.getFixedValues().equals("")) {
                        String values = youTrackSite.getFixedValues();
                        String[] fixedValueArray = values.split(",");
                        for (String fixedValueFromArray : fixedValueArray) {
                            fixedValues.add(fixedValueFromArray);
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

    private void addCommentIfEnabled(AbstractBuild<?, ?> build, YouTrackSite youTrackSite, YouTrackServer youTrackServer, User user, List<Project> projects, String msg, BuildListener listener) {
        if (youTrackSite.isCommentEnabled()) {
            for (Project project1 : projects) {
                String shortName = project1.getShortName();
                Pattern projectPattern = Pattern.compile("(" + shortName + "-" + "(\\d+)" + ")");
                Matcher matcher = projectPattern.matcher(msg);
                while (matcher.find()) {
                    if (matcher.groupCount() >= 1) {
                        String issueId = shortName + "-" + matcher.group(2);
                        //noinspection deprecation
                        boolean comment = youTrackServer.comment(user, new Issue(issueId), "Related build: " + build.getAbsoluteUrl(), youTrackSite.getLinkVisibility(), youTrackSite.isSilentLinks());
                        if (comment) {
                            listener.getLogger().println("Commented on " + issueId);
                        } else {
                            listener.getLogger().println("FAILED: Commented on " + issueId);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof YouTrackSCMListener;
    }
}
