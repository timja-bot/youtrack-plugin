package org.jenkinsci.plugins.youtrack;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import hudson.model.*;
import hudson.scm.ChangeLogSet;
import junit.framework.Assert;
import org.jenkinsci.plugins.youtrack.youtrackapi.Issue;
import org.jenkinsci.plugins.youtrack.youtrackapi.Project;
import org.jenkinsci.plugins.youtrack.youtrackapi.User;
import org.jenkinsci.plugins.youtrack.youtrackapi.YouTrackServer;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hudson.test.FakeChangeLogSCM;
import org.mockito.Answers;
import org.mockito.Matchers;
import org.mockito.Mockito;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * Test the SCM listener.
 */
public class YouTrackSCMListenerTest {
    private static class MockEntry extends ChangeLogSet.Entry {

        private final String msg;

        public MockEntry(String msg) {
            this.msg = msg;
        }

        @Override
        public Collection<String> getAffectedPaths() {
            return null;
        }

        @Override
        public hudson.model.User getAuthor() {
            return null;
        }

        @Override
        public String getMsg() {
            return this.msg;
        }
    }


    @Test
    @Ignore
    public void testExecuteCommand() throws Exception {
        FreeStyleProject project = mock(FreeStyleProject.class);
        FreeStyleBuild freeStyleBuild = mock(FreeStyleBuild.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        BuildListener listener = mock(BuildListener.class);
        YouTrackServer server = mock(YouTrackServer.class);

        User user = new User();
        user.setUsername("tester");
        user.setLoggedIn(true);

        when(freeStyleBuild.getProject()).thenReturn(project);
        when((FreeStyleBuild) freeStyleBuild.getRootBuild()).thenReturn(freeStyleBuild);
        when(listener.getLogger()).thenReturn(new PrintStream(new ByteArrayOutputStream()));
        when(freeStyleBuild.getAction(Matchers.any(Class.class))).thenCallRealMethod();
        when(freeStyleBuild.getActions()).thenCallRealMethod();
        Mockito.doCallRealMethod().when(freeStyleBuild).addAction(Matchers.<Action>anyObject());

        Command command = new Command();
        command.setDate(new Date());
        command.setComment(null);
        command.setCommand("Fixed");
        command.setSilent(false);
        command.setIssueId("TP1-1");
        command.setStatus(Command.Status.OK);
        command.setUsername(user.getUsername());
        when(server.applyCommand("testsite", user, new Issue("TP1-1"), "Fixed", null, null, true)).thenReturn(command);


        ArrayList<Project> projects = new ArrayList<Project>();
        Project project1 = new Project();
        project1.setShortName("TP1");
        projects.add(project1);
        when(server.getProjects(user)).thenReturn(projects);

        HashSet<MockEntry> scmLogEntries = Sets.newHashSet(new MockEntry("#TP1-1 Fixed"));

        when(changeLogSet.iterator()).thenReturn(scmLogEntries.iterator());

        YouTrackSite youTrackSite = new YouTrackSite("testsite", "test", "test", "http://test.com");
        youTrackSite.setCommandsEnabled(true);

        youTrackSite.setPluginEnabled(true);

        YouTrackProjectProperty youTrackProjectProperty = new YouTrackProjectProperty("testsite", true, false, true, false, false, null, null, null, false, false, null, false, null);
        YouTrackProjectProperty.DescriptorImpl descriptor = (YouTrackProjectProperty.DescriptorImpl) youTrackProjectProperty.getDescriptor();
        descriptor.setSites(youTrackSite);


        when(project.getProperty(YouTrackProjectProperty.class)).thenReturn(youTrackProjectProperty);


        YouTrackSCMListener youTrackSCMListener = new YouTrackSCMListener();
        youTrackSCMListener.performActions(freeStyleBuild, listener, youTrackSite, changeLogSet.iterator(), server, user);

        YouTrackCommandAction youTrackCommandAction = freeStyleBuild.getAction(YouTrackCommandAction.class);
        List<Command> commands = youTrackCommandAction.getCommands();
        assertEquals(1, commands.size());
    }


}
