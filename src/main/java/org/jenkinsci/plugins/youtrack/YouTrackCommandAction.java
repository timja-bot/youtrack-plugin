package org.jenkinsci.plugins.youtrack;

import hudson.model.AbstractBuild;
import hudson.model.Action;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * This action shows the commands that the build tried to execute.
 */
public class YouTrackCommandAction implements Action {
    @Getter private List<Command> commands;
    @Getter private AbstractBuild build;

    public YouTrackCommandAction(AbstractBuild build) {
        this.build = build;
        commands = new ArrayList<Command>();
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
