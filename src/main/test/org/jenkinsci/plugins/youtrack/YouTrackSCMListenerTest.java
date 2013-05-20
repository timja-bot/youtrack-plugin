package org.jenkinsci.plugins.youtrack;

import hudson.model.StreamBuildListener;
import junit.framework.Assert;
import org.jenkinsci.plugins.youtrack.youtrackapi.Issue;
import org.jenkinsci.plugins.youtrack.youtrackapi.Project;
import org.jenkinsci.plugins.youtrack.youtrackapi.User;
import org.jenkinsci.plugins.youtrack.youtrackapi.YouTrackServer;
import org.junit.Test;
import org.jvnet.hudson.test.FakeChangeLogSCM;

import java.util.ArrayList;
import java.util.List;

/**
 * Test the SCM listener.
 */
public class YouTrackSCMListenerTest {
    @Test
    public void noCommandButComment() throws Exception {
        YouTrackSCMListener youTrackSCMListener = new YouTrackSCMListener();
        String url = "url";
        String username = "username";
        YouTrackSite youTrackSite = new YouTrackSite(username, "password", url);
        youTrackSite.setCommandsEnabled(true);
        YouTrackServer server = new YouTrackServer(url);
        User user = new User();
        user.setUsername(username);

        List<Project> projectList = new ArrayList<Project>();
        Project project = new Project();
        project.setShortName("test");
        projectList.add(project);

        ArrayList<Issue> fixedIssues = new ArrayList<Issue>();
        FakeChangeLogSCM.EntryImpl changeLogEntry = new FakeChangeLogSCM.EntryImpl();
        List<Command> commands = youTrackSCMListener.executeCommandsIfEnabled(new StreamBuildListener(System.out), youTrackSite, server, user, projectList, fixedIssues, changeLogEntry, "#test-1\ntest comment");
        Assert.assertEquals(1, commands.size());
    }

    @Test
    public void noCommandWithComment() throws Exception {
        YouTrackSCMListener youTrackSCMListener = new YouTrackSCMListener();
        String url = "url";
        String username = "username";
        YouTrackSite youTrackSite = new YouTrackSite(username, "password", url);
        youTrackSite.setCommandsEnabled(true);
        YouTrackServer server = new YouTrackServer(url);
        User user = new User();
        user.setUsername(username);

        List<Project> projectList = new ArrayList<Project>();
        Project project = new Project();
        project.setShortName("test");
        projectList.add(project);

        ArrayList<Issue> fixedIssues = new ArrayList<Issue>();
        FakeChangeLogSCM.EntryImpl changeLogEntry = new FakeChangeLogSCM.EntryImpl();
        List<Command> commands = youTrackSCMListener.executeCommandsIfEnabled(new StreamBuildListener(System.out), youTrackSite, server, user, projectList, fixedIssues, changeLogEntry, "#test-1 Fixed\ntest comment");
        Assert.assertEquals(1, commands.size());
    }
}
