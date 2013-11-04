package org.jenkinsci.plugins.youtrack;

import hudson.model.AbstractBuild;
import hudson.model.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * This action shows the commands that the build tried to execute.
 */
public class YouTrackCommandAction implements Action {
    private List<Command> commands;
    private AbstractBuild build;

    public YouTrackCommandAction(AbstractBuild build) {
        this.build = build;
        commands = new ArrayList<Command>();
    }

    public List<Command> getCommands() {
        return commands;
    }


    public AbstractBuild getBuild() {
        return build;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getIssueUrl() {
        YouTrackSite youTrackSite = YouTrackSite.get(build.getProject());
        return youTrackSite.getUrl() + "/issue/";
    }

    public boolean addCommand(Command command) {
        return commands.add(command);
    }

    public int getNumCommands() {
        return commands.size();
    }

    public String getIconFileName() {
        return "plugin.png";
    }

    public String getDisplayName() {
        return "YouTrack Commands";
    }

    public String getUrlName() {
        return "youtrackCommands";
    }
}
