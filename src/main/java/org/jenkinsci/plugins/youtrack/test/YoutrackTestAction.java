package org.jenkinsci.plugins.youtrack.test;

import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestAction;
import org.jenkinsci.plugins.youtrack.Command;
import org.jenkinsci.plugins.youtrack.YouTrackSite;
import org.jenkinsci.plugins.youtrack.youtrackapi.User;
import org.jenkinsci.plugins.youtrack.youtrackapi.YouTrackServer;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 *
 */
public class YoutrackTestAction extends TestAction {
    private YoutrackTestDataPublisher.Data data;
    private CaseResult careResult;
    private final String id;
    private String youtrackIssueId;

    public YoutrackTestAction(YoutrackTestDataPublisher.Data data, CaseResult careResult, String id, String youtrackIssueId) {
        this.data = data;
        this.careResult = careResult;
        this.id = id;
        this.youtrackIssueId = youtrackIssueId;
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return "youtrack";
    }

    public String getYoutrackIssueId() {
        return youtrackIssueId;
    }

    public String getYoutrackServerUrl() {
        YouTrackSite youTrackSite = YouTrackSite.get(data.getBuild().getProject());
        return youTrackSite.getUrl();
    }

    public void setYoutrackIssueId(String youtrackIssueId) {
        this.youtrackIssueId = youtrackIssueId;
    }

    public String getId() {
        return id;
    }

    public boolean isLinked() {
        return youtrackIssueId != null;
    }


    public void doCreateIssue(StaplerRequest req, StaplerResponse resp)
            throws ServletException, IOException {
        YouTrackSite youTrackSite = YouTrackSite.get(data.getBuild().getProject());
        YouTrackServer youTrackServer = new YouTrackServer(youTrackSite.getUrl());
        User mainUser = youTrackServer.login(youTrackSite.getUsername(), youTrackSite.getPassword());

        Command issue = youTrackServer.createIssue(youTrackSite.getName(), mainUser, youTrackSite.getProject(), "Test case: " + id, careResult.getErrorStackTrace(), null);
        youtrackIssueId = issue.getIssueId();
        data.addLink(id, this);
        data.save();
        resp.forwardToPreviousPage(req);
    }

    public void linkIssue(StaplerRequest req, StaplerResponse resp) {

    }
}
