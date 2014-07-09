package org.jenkinsci.plugins.youtrack;

import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.youtrack.youtrackapi.Issue;
import org.jenkinsci.plugins.youtrack.youtrackapi.User;
import org.jenkinsci.plugins.youtrack.youtrackapi.YouTrackServer;
import org.junit.Assert;
import org.junit.Test;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class YouTrackIssueActionTest {
    @Test
    public void testNotConfigured() throws IOException, ServletException {
        FreeStyleProject project = mock(FreeStyleProject.class);
        StaplerRequest request = mock(StaplerRequest.class);
        StaplerResponse response = mock(StaplerResponse.class);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(out);
        when(response.getWriter()).thenReturn(printWriter);

        YouTrackIssueAction youTrackIssueAction = spy(new YouTrackIssueAction(project));
        YouTrackSite youTrackSite = new YouTrackSite("site", "user", "password", "http://www.example.com");
        youTrackSite.setPluginEnabled(false);
        doReturn(youTrackSite).when(youTrackIssueAction).getYouTrackSite();


        HttpResponse httpResponse = youTrackIssueAction.doIssue();
        httpResponse.generateResponse(request, response, null);
        printWriter.flush();
        assertThat(out.toString().replace("\r\n","\n"), equalTo("YouTrack integration not set up for this project"));
    }

    @Test
    public void testCouldNotLogIn() throws IOException, ServletException {
        FreeStyleProject project = mock(FreeStyleProject.class);
        StaplerRequest request = mock(StaplerRequest.class);
        StaplerResponse response = mock(StaplerResponse.class);
        YouTrackServer youTrackServer = mock(YouTrackServer.class);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(out);
        when(response.getWriter()).thenReturn(printWriter);

        YouTrackIssueAction youTrackIssueAction = spy(new YouTrackIssueAction(project));
        YouTrackSite youTrackSite = new YouTrackSite("site", "user", "password", "http://www.example.com");
        youTrackSite.setPluginEnabled(true);
        doReturn(youTrackSite).when(youTrackIssueAction).getYouTrackSite();
        doReturn(youTrackServer).when(youTrackIssueAction).getYouTrackServer(youTrackSite);


        HttpResponse httpResponse = youTrackIssueAction.doIssue();
        httpResponse.generateResponse(request, response, null);
        printWriter.flush();
        assertThat(out.toString().replace("\r\n","\n"), equalTo("Could not log in to YouTrack"));
    }

    @Test
    public void testIssueWithoutImages() throws IOException, ServletException {
        FreeStyleProject project = mock(FreeStyleProject.class);
        StaplerRequest request = mock(StaplerRequest.class);
        StaplerResponse response = mock(StaplerResponse.class);
        YouTrackServer youTrackServer = mock(YouTrackServer.class);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(out);
        when(response.getWriter()).thenReturn(printWriter);

        YouTrackIssueAction youTrackIssueAction = spy(new YouTrackIssueAction(project));
        YouTrackSite youTrackSite = new YouTrackSite("site", "user", "password", "http://www.example.com");
        youTrackSite.setPluginEnabled(true);
        youTrackSite.setStateFieldName("State");


        doReturn(youTrackSite).when(youTrackIssueAction).getYouTrackSite();
        doReturn(youTrackServer).when(youTrackIssueAction).getYouTrackServer(youTrackSite);
        doReturn("ISSUE-1").when(request).getParameter("id");

        User user = new User();
        user.setLoggedIn(true);
        user.setUsername("user");
        when(youTrackServer.login("user", "password")).thenReturn(user);
        Issue issue = new Issue("ISSUE-1");
        issue.setSummary("Summary of issue");
        issue.setDescription("Description of issue");
        issue.setState("Fixed");
        when(youTrackServer.getIssue(user, "ISSUE-1", "State")).thenReturn(issue);

        HttpResponse httpResponse = youTrackIssueAction.doIssue();
        httpResponse.generateResponse(request, response, null);
        printWriter.flush();
        String expected = "{\"id\":\"ISSUE-1\",\"state\":\"Fixed\",\"summary\":\"Summary of issue\",\"description\":\"\\u003chtml\\u003e\\n \\u003chead\\u003e\\u003c/head\\u003e\\n \\u003cbody\\u003e\\n  Description of issue\\n \\u003c/body\\u003e\\n\\u003c/html\\u003e\"}";
        String actual = out.toString().trim().replace("\r\n", "\n");
        StringBuilder actualStr = new StringBuilder();
        StringBuilder expectedStr = new StringBuilder();
        for (int i = 0; i < expected.length(); i++) {
            char expectedChar = expected.charAt(i);
            char actualChar = actual.charAt(i);

            expectedStr.append(expectedChar);
            actualStr.append(actualChar);


            if (actualChar != expectedChar) {
                System.out.println("Expected>" + expectedStr.toString());
                System.out.println("Actual  >" +actualStr.toString());

                System.out.println(actualChar);
                System.out.println(expectedChar);

                System.out.println(i);
                break;
            }
        }

        assertThat(actual, equalTo(expected));
    }
}