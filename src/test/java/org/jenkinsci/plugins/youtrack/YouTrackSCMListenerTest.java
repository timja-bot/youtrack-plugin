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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import static junit.framework.Assert.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

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

        YouTrackServer youTrackServer = mock(YouTrackServer.class);

        YouTrackSCMListener youTrackSCMListener = spy(new YouTrackSCMListener());
        doReturn(youTrackSite).when(youTrackSCMListener).getYouTrackSite(freeStyleBuild);
        doReturn(youTrackServer).when(youTrackSCMListener).getYouTrackServer(youTrackSite);

        youTrackSCMListener.performActions(freeStyleBuild, listener, youTrackSite, changeLogSet.iterator(), server, user);

        YouTrackCommandAction youTrackCommandAction = freeStyleBuild.getAction(YouTrackCommandAction.class);
        List<Command> commands = youTrackCommandAction.getCommands();
        assertEquals(1, commands.size());
    }


    @Test
    public void testSingleLinkPerBuild() throws Exception {
        YouTrackSCMListener youTrackSCMListener = spy(new YouTrackSCMListener());
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        BuildListener listener = mock(BuildListener.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        YouTrackServer server = mock(YouTrackServer.class);

        User user = new User();
        user.setUsername("tester");
        user.setLoggedIn(true);

        doReturn(Lists.newArrayList(new Project("TP1"))).when(server).getProjects(user);
        doReturn(Lists.newArrayList(new MockEntry("#TP1-1 In Progress"), new MockEntry("#TP1-1 Fixed")).iterator()).when(changeLogSet).iterator();
        doReturn(build).when(build).getRootBuild();
        doReturn(new PrintStream(new ByteArrayOutputStream())).when(listener).getLogger();
        doReturn("http://test.com/buildurl").when(youTrackSCMListener).getAbsoluteUrlForBuild(Matchers.<AbstractBuild<?,?>>any());


        final List<Command> commentCommands = new ArrayList<Command>();
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Command command = new Command();
                command.setStatus(Command.Status.OK);
                command.setSiteName((String) invocationOnMock.getArguments()[0]);
                command.setUsername(((User) invocationOnMock.getArguments()[1]).getUsername());
                command.setIssueId(((Issue) invocationOnMock.getArguments()[2]).getId());
                command.setComment(((String) invocationOnMock.getArguments()[3]));
                command.setGroup(((String) invocationOnMock.getArguments()[4]));
                command.setSilent(((Boolean) invocationOnMock.getArguments()[5]));
                commentCommands.add(command);

                return command;
            }
        }).when(server).comment(anyString(), Matchers.<User>any(), Matchers.<Issue>any(), anyString(), anyString(), anyBoolean());




        YouTrackSite youTrackSite = new YouTrackSite("testsite", "test", "test", "http://test.com");
        youTrackSite.setCommentEnabled(true);


        youTrackSCMListener.performActions(build, listener, youTrackSite, changeLogSet.iterator(), server, user);

        assertThat(commentCommands.size(), is(1));
    }

    @Test
    public void testLinkForAllIssue() throws Exception {
        YouTrackSCMListener youTrackSCMListener = spy(new YouTrackSCMListener());
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        BuildListener listener = mock(BuildListener.class);
        ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        YouTrackServer server = mock(YouTrackServer.class);

        User user = new User();
        user.setUsername("tester");
        user.setLoggedIn(true);

        doReturn(Lists.newArrayList(new Project("TP1"), new Project("TP2"))).when(server).getProjects(user);
        doReturn(Lists.newArrayList(new MockEntry("#TP1-1 In Progress"), new MockEntry("#TP1-2 Fixed")).iterator()).when(changeLogSet).iterator();
        doReturn(build).when(build).getRootBuild();
        doReturn(new PrintStream(new ByteArrayOutputStream())).when(listener).getLogger();
        doReturn("http://test.com/buildurl").when(youTrackSCMListener).getAbsoluteUrlForBuild(Matchers.<AbstractBuild<?,?>>any());


        final List<Command> commentCommands = new ArrayList<Command>();
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Command command = new Command();
                command.setStatus(Command.Status.OK);
                command.setSiteName((String) invocationOnMock.getArguments()[0]);
                command.setUsername(((User) invocationOnMock.getArguments()[1]).getUsername());
                command.setIssueId(((Issue) invocationOnMock.getArguments()[2]).getId());
                command.setComment(((String) invocationOnMock.getArguments()[3]));
                command.setGroup(((String) invocationOnMock.getArguments()[4]));
                command.setSilent(((Boolean) invocationOnMock.getArguments()[5]));
                commentCommands.add(command);

                return command;
            }
        }).when(server).comment(anyString(), Matchers.<User>any(), Matchers.<Issue>any(), anyString(), anyString(), anyBoolean());

        YouTrackSite youTrackSite = new YouTrackSite("testsite", "test", "test", "http://test.com");
        youTrackSite.setCommentEnabled(true);


        youTrackSCMListener.performActions(build, listener, youTrackSite, changeLogSet.iterator(), server, user);

        assertThat(commentCommands.size(), is(2));
    }


    @Test
    public void testMultilineComment() throws Exception {
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

        String fullCommitMessage = "#TP1-1 Fixed\n\nFoo\nBar\n #TP1-2\nBaz";
        String partialFirstComment = "Foo\nBar";
        String partialSecondComment = "Baz";

        Command command1 = new Command();
        command1.setDate(new Date());
        command1.setComment(partialFirstComment);
        command1.setCommand("Fixed");
        command1.setSilent(false);
        command1.setIssueId("TP1-1");
        command1.setStatus(Command.Status.OK);
        command1.setUsername(user.getUsername());
        Command command2 = new Command();
        command2.setDate(new Date());
        command2.setComment(partialSecondComment);
        command2.setCommand("Fixed");
        command2.setSilent(false);
        command2.setIssueId("TP1-1");
        command2.setStatus(Command.Status.OK);
        command2.setUsername(user.getUsername());
        when(server.applyCommand("testsite", user, new Issue("TP1-1"), "Fixed", partialFirstComment, null, true)).thenReturn(command1);
        when(server.applyCommand("testsite", user, new Issue("TP1-2"), "", partialSecondComment, null, true)).thenReturn(command2);

        ArrayList<Project> projects = new ArrayList<Project>();
        Project project1 = new Project();
        project1.setShortName("TP1");
        projects.add(project1);
        when(server.getProjects(user)).thenReturn(projects);

        HashSet<MockEntry> scmLogEntries = Sets.newHashSet(new MockEntry(fullCommitMessage));

        when(changeLogSet.iterator()).thenReturn(scmLogEntries.iterator());

        YouTrackSite youTrackSite = new YouTrackSite("testsite", "test", "test", "http://test.com");
        youTrackSite.setCommandsEnabled(true);

        youTrackSite.setPluginEnabled(true);

        YouTrackServer youTrackServer = mock(YouTrackServer.class);

        YouTrackSCMListener youTrackSCMListener = spy(new YouTrackSCMListener());
        doReturn(youTrackSite).when(youTrackSCMListener).getYouTrackSite(freeStyleBuild);
        doReturn(youTrackServer).when(youTrackSCMListener).getYouTrackServer(youTrackSite);

        youTrackSCMListener.performActions(freeStyleBuild, listener, youTrackSite, changeLogSet.iterator(), server, user);

        YouTrackCommandAction youTrackCommandAction = freeStyleBuild.getAction(YouTrackCommandAction.class);
        List<Command> commands = youTrackCommandAction.getCommands();
        assertEquals(2, commands.size());
    }

    @Test
    public void testExecutePrefixCommand() throws Exception {
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

        Command command1 = new Command();
        command1.setDate(new Date());
        command1.setComment(null);
        command1.setCommand("Foo");
        command1.setSilent(false);
        command1.setIssueId("TP1-1");
        command1.setStatus(Command.Status.OK);
        command1.setUsername(user.getUsername());
        when(server.applyCommand("testsite", user, new Issue("TP1-1"), "Foo", null, null, true)).thenReturn(command1);
        Command command2 = new Command();
        command2.setDate(new Date());
        command2.setComment(null);
        command2.setCommand("Fix");
        command2.setSilent(false);
        command2.setIssueId("TP1-1");
        command2.setStatus(Command.Status.OK);
        command2.setUsername(user.getUsername());
        when(server.applyCommand("testsite", user, new Issue("TP1-1"), "Fix", null, null, true)).thenReturn(command2);

        ArrayList<Project> projects = new ArrayList<Project>();
        Project project1 = new Project();
        project1.setShortName("TP1");
        projects.add(project1);
        when(server.getProjects(user)).thenReturn(projects);

        HashSet<MockEntry> scmLogEntries = Sets.newHashSet(new MockEntry("Fixes #TP1-1 Foo"));

        when(changeLogSet.iterator()).thenReturn(scmLogEntries.iterator());

        YouTrackSite youTrackSite = new YouTrackSite("testsite", "test", "test", "http://test.com");
        youTrackSite.setCommandsEnabled(true);
        youTrackSite.setPluginEnabled(true);
        // For the way the tests are mocked, should this be necessary?
        // I don't see YouTrackProjectProperty.getSite getting invoked that should push these through.
        youTrackSite.setPrefixCommandPairs(Lists.newArrayList(new PrefixCommandPair("Fixes", "Fix"), new PrefixCommandPair("Fixed", "Fix")));


        YouTrackServer youTrackServer = mock(YouTrackServer.class);

        YouTrackSCMListener youTrackSCMListener = spy(new YouTrackSCMListener());
        doReturn(youTrackSite).when(youTrackSCMListener).getYouTrackSite(freeStyleBuild);
        doReturn(youTrackServer).when(youTrackSCMListener).getYouTrackServer(youTrackSite);

        youTrackSCMListener.performActions(freeStyleBuild, listener, youTrackSite, changeLogSet.iterator(), server, user);

        YouTrackCommandAction youTrackCommandAction = freeStyleBuild.getAction(YouTrackCommandAction.class);
        List<Command> commands = youTrackCommandAction.getCommands();
        assertEquals(2, commands.size());
    }

    YouTrackProjectProperty getProjectProperty() {
        return new YouTrackProjectProperty("testsite", true, false, true, false, false, null, null, null, false, false, null, false, null, "Fixes,Fixed", "Fix");
    }
}