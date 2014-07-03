package org.jenkinsci.plugins.youtrack;

import hudson.MarkupText;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.scm.ChangeLogSet;
import junit.framework.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class YouTrackChangeLogAnnotatorTest {

    /**
     * The plugin does not use a generic regular expression to guess whats issue numbers, instead on each build valid
     * project ids are fetches from YouTrack, therefore there is no markup when there is no previous build.
     */
    @Test
    public void testNoLastBuild() {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        MarkupText markupText = new MarkupText("ISSUE-1");
        Project project = mock(Project.class);

        YouTrackSite youTrackSite = new YouTrackSite("site", "user", "password", "http://example.com");
        youTrackSite.setPluginEnabled(true);

        when(build.getProject()).thenReturn(project);
        YouTrackChangeLogAnnotator youTrackChangeLogAnnotator = spy(new YouTrackChangeLogAnnotator());
        doReturn(youTrackSite).when(youTrackChangeLogAnnotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        when(youTrackChangeLogAnnotator.getSiteForProject(project)).thenReturn(youTrackSite);

        youTrackChangeLogAnnotator.annotate(build, entry, markupText);

        Assert.assertEquals(markupText.getText(), "ISSUE-1");
    }

    /**
     * Tests that the plugin does not annotate the change log if the plugin as not enabled for the project.
     */
    @Test
    public void testPluginNotEnabled() {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        MarkupText markupText = new MarkupText("ISSUE-1");
        Project project = mock(Project.class);



        YouTrackSite youTrackSite = new YouTrackSite("site", "user", "password", "http://example.com");
        youTrackSite.setPluginEnabled(false);

        when(build.getProject()).thenReturn(project);
        YouTrackChangeLogAnnotator youTrackChangeLogAnnotator = spy(new YouTrackChangeLogAnnotator());
        doReturn(youTrackSite).when(youTrackChangeLogAnnotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        when(youTrackChangeLogAnnotator.getSiteForProject(project)).thenReturn(youTrackSite);
        ArrayList<org.jenkinsci.plugins.youtrack.youtrackapi.Project> projects = new ArrayList<org.jenkinsci.plugins.youtrack.youtrackapi.Project>();
        org.jenkinsci.plugins.youtrack.youtrackapi.Project youtrackProject = new org.jenkinsci.plugins.youtrack.youtrackapi.Project();
        youtrackProject.setShortName("ISSUE");
        projects.add(youtrackProject);
        when(build.getAction(YouTrackSaveProjectShortNamesAction.class)).thenReturn(new YouTrackSaveProjectShortNamesAction(projects));
        when(project.getLastSuccessfulBuild()).thenReturn(build);
        when(project.getName()).thenReturn("TestProject");

        youTrackChangeLogAnnotator.annotate(build, entry, markupText);

        Assert.assertEquals(markupText.getText(), "ISSUE-1");
    }

    /**
     * Tests that the plugin does not annotate the change log if the plugin as not enabled for the project.
     */
    @Test
    public void testAnnotationNotEnabled() {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        MarkupText markupText = new MarkupText("ISSUE-1");
        Project project = mock(Project.class);



        YouTrackSite youTrackSite = new YouTrackSite("site", "user", "password", "http://example.com");
        youTrackSite.setPluginEnabled(true);
        youTrackSite.setAnnotationsEnabled(false);

        when(build.getProject()).thenReturn(project);
        YouTrackChangeLogAnnotator youTrackChangeLogAnnotator = spy(new YouTrackChangeLogAnnotator());
        doReturn(youTrackSite).when(youTrackChangeLogAnnotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        when(youTrackChangeLogAnnotator.getSiteForProject(project)).thenReturn(youTrackSite);
        ArrayList<org.jenkinsci.plugins.youtrack.youtrackapi.Project> projects = new ArrayList<org.jenkinsci.plugins.youtrack.youtrackapi.Project>();
        org.jenkinsci.plugins.youtrack.youtrackapi.Project youtrackProject = new org.jenkinsci.plugins.youtrack.youtrackapi.Project();
        youtrackProject.setShortName("ISSUE");
        projects.add(youtrackProject);
        when(build.getAction(YouTrackSaveProjectShortNamesAction.class)).thenReturn(new YouTrackSaveProjectShortNamesAction(projects));
        when(project.getLastSuccessfulBuild()).thenReturn(build);

        youTrackChangeLogAnnotator.annotate(build, entry, markupText);

        Assert.assertEquals(markupText.getText(), "ISSUE-1");
    }

    /**
     * Tests that the plugin does not annotate the change log if the plugin as not enabled for the project.
     */
    @Test
    public void testSingleIssue() {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        MarkupText markupText = new MarkupText("ISSUE-1");
        Project project = mock(Project.class);

        YouTrackSite youTrackSite = new YouTrackSite("site", "user", "password", "http://example.com");
        youTrackSite.setPluginEnabled(true);
        youTrackSite.setAnnotationsEnabled(true);

        when(build.getProject()).thenReturn(project);
        when(entry.getMsg()).thenReturn("ISSUE-1");

        YouTrackChangeLogAnnotator youTrackChangeLogAnnotator = spy(new YouTrackChangeLogAnnotator());
        doReturn(youTrackSite).when(youTrackChangeLogAnnotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        doReturn("http://jenkins.example.com/").when(youTrackChangeLogAnnotator).getRootUrl();

        when(youTrackChangeLogAnnotator.getSiteForProject(project)).thenReturn(youTrackSite);
        ArrayList<org.jenkinsci.plugins.youtrack.youtrackapi.Project> projects = new ArrayList<org.jenkinsci.plugins.youtrack.youtrackapi.Project>();
        org.jenkinsci.plugins.youtrack.youtrackapi.Project youtrackProject = new org.jenkinsci.plugins.youtrack.youtrackapi.Project();
        youtrackProject.setShortName("ISSUE");
        projects.add(youtrackProject);
        when(build.getAction(YouTrackSaveProjectShortNamesAction.class)).thenReturn(new YouTrackSaveProjectShortNamesAction(projects));
        when(project.getLastSuccessfulBuild()).thenReturn(build);
        when(build.getUrl()).thenReturn("1/");
        when(project.getName()).thenReturn("TestProject");


        youTrackChangeLogAnnotator.annotate(build, entry, markupText);

        assertFalse(markupText.toString(false).equals("ISSUE-1"));
        //TODO: Should probably also check the generated js
    }

    /**
     * Tests that the plugin does not annotate the change log if the plugin as not enabled for the project.
     */
    @Test
    public void testSingleIssueWithCommand() {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        MarkupText markupText = new MarkupText("#ISSUE-1 Fixed");
        Project project = mock(Project.class);

        YouTrackSite youTrackSite = new YouTrackSite("site", "user", "password", "http://example.com");
        youTrackSite.setPluginEnabled(true);
        youTrackSite.setAnnotationsEnabled(true);

        when(build.getProject()).thenReturn(project);
        when(entry.getMsg()).thenReturn("ISSUE-1");

        YouTrackChangeLogAnnotator youTrackChangeLogAnnotator = spy(new YouTrackChangeLogAnnotator());
        doReturn(youTrackSite).when(youTrackChangeLogAnnotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        doReturn("http://jenkins.example.com/").when(youTrackChangeLogAnnotator).getRootUrl();

        when(youTrackChangeLogAnnotator.getSiteForProject(project)).thenReturn(youTrackSite);
        ArrayList<org.jenkinsci.plugins.youtrack.youtrackapi.Project> projects = new ArrayList<org.jenkinsci.plugins.youtrack.youtrackapi.Project>();
        org.jenkinsci.plugins.youtrack.youtrackapi.Project youtrackProject = new org.jenkinsci.plugins.youtrack.youtrackapi.Project();
        youtrackProject.setShortName("ISSUE");
        projects.add(youtrackProject);
        when(build.getAction(YouTrackSaveProjectShortNamesAction.class)).thenReturn(new YouTrackSaveProjectShortNamesAction(projects));
        when(project.getLastSuccessfulBuild()).thenReturn(build);
        when(build.getUrl()).thenReturn("1/");
        when(project.getName()).thenReturn("TestProject");


        youTrackChangeLogAnnotator.annotate(build, entry, markupText);

        assertFalse(markupText.toString(false).equals("ISSUE-1"));
        //TODO: Should probably also check the generated js
    }


    /**
     * Tests that the plugin does not annotate the change log if the plugin as not enabled for the project.
     */
    @Test
    public void testMultipleIssuesOnOneLine() {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        MarkupText markupText = new MarkupText("#ISSUE-1 duplicates ISSUE-2");
        Project project = mock(Project.class);

        YouTrackSite youTrackSite = new YouTrackSite("site", "user", "password", "http://example.com");
        youTrackSite.setPluginEnabled(true);
        youTrackSite.setAnnotationsEnabled(true);

        when(build.getProject()).thenReturn(project);
        when(entry.getMsg()).thenReturn("ISSUE-1");

        YouTrackChangeLogAnnotator youTrackChangeLogAnnotator = spy(new YouTrackChangeLogAnnotator());
        doReturn(youTrackSite).when(youTrackChangeLogAnnotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        doReturn("http://jenkins.example.com/").when(youTrackChangeLogAnnotator).getRootUrl();

        when(youTrackChangeLogAnnotator.getSiteForProject(project)).thenReturn(youTrackSite);
        ArrayList<org.jenkinsci.plugins.youtrack.youtrackapi.Project> projects = new ArrayList<org.jenkinsci.plugins.youtrack.youtrackapi.Project>();
        org.jenkinsci.plugins.youtrack.youtrackapi.Project youtrackProject = new org.jenkinsci.plugins.youtrack.youtrackapi.Project();
        youtrackProject.setShortName("ISSUE");
        projects.add(youtrackProject);
        when(build.getAction(YouTrackSaveProjectShortNamesAction.class)).thenReturn(new YouTrackSaveProjectShortNamesAction(projects));
        when(project.getLastSuccessfulBuild()).thenReturn(build);
        when(build.getUrl()).thenReturn("1/");
        when(project.getName()).thenReturn("TestProject");


        youTrackChangeLogAnnotator.annotate(build, entry, markupText);

        assertFalse(markupText.toString(false).equals("ISSUE-1"));
        //TODO: Should probably also check the generated js
    }

    /**
     * Tests that the plugin does not annotate the change log if the plugin as not enabled for the project.
     */
    @Test
    public void testIssuesOnMultipleLines() {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        MarkupText markupText = new MarkupText("#ISSUE-1 Fixed\nBla bla ISSUE-2");
        Project project = mock(Project.class);

        YouTrackSite youTrackSite = new YouTrackSite("site", "user", "password", "http://example.com");
        youTrackSite.setPluginEnabled(true);
        youTrackSite.setAnnotationsEnabled(true);

        when(build.getProject()).thenReturn(project);
        when(entry.getMsg()).thenReturn("ISSUE-1");

        YouTrackChangeLogAnnotator youTrackChangeLogAnnotator = spy(new YouTrackChangeLogAnnotator());
        doReturn(youTrackSite).when(youTrackChangeLogAnnotator).getSiteForProject((AbstractProject<?, ?>) Mockito.any());
        doReturn("http://jenkins.example.com/").when(youTrackChangeLogAnnotator).getRootUrl();

        when(youTrackChangeLogAnnotator.getSiteForProject(project)).thenReturn(youTrackSite);
        ArrayList<org.jenkinsci.plugins.youtrack.youtrackapi.Project> projects = new ArrayList<org.jenkinsci.plugins.youtrack.youtrackapi.Project>();
        org.jenkinsci.plugins.youtrack.youtrackapi.Project youtrackProject = new org.jenkinsci.plugins.youtrack.youtrackapi.Project();
        youtrackProject.setShortName("ISSUE");
        projects.add(youtrackProject);
        when(build.getAction(YouTrackSaveProjectShortNamesAction.class)).thenReturn(new YouTrackSaveProjectShortNamesAction(projects));
        when(project.getLastSuccessfulBuild()).thenReturn(build);
        when(build.getUrl()).thenReturn("1/");
        when(project.getName()).thenReturn("TestProject");


        youTrackChangeLogAnnotator.annotate(build, entry, markupText);

        assertFalse(markupText.toString(false).equals("ISSUE-1"));
        //TODO: Should probably also check the generated js
    }
}