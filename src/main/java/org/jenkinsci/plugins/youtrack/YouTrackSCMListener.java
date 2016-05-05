package org.jenkinsci.plugins.youtrack;

import hudson.model.*;
import hudson.model.listeners.SCMListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;

import java.lang.reflect.InvocationTargetException;

public class YouTrackSCMListener extends SCMListener {


    @Override
    public void onChangeLogParsed(AbstractBuild<?, ?> build, BuildListener listener, ChangeLogSet<?> changeLogSet) throws Exception {
        if (build.getRootBuild().equals(build)) {
            DescribableList<Publisher, Descriptor<Publisher>> publishersList = build.getProject().getPublishersList();
            boolean hasRecorder = false;
            if (publishersList != null) {
                for (Publisher publisher : publishersList) {
                    if (publisher instanceof YoutrackUpdateIssuesRecorder) {
                        hasRecorder = true;
                        break;
                    }
                }
            }
            if (!hasRecorder) {
                YoutrackIssueUpdater youtrackIssueUpdater = getYoutrackIssueUpdater();
                youtrackIssueUpdater.update(build.getProject().getScm(), build, listener, changeLogSet);
            }
        }
    }

    YoutrackIssueUpdater getYoutrackIssueUpdater() {
        return new YoutrackIssueUpdater();
    }


    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof YouTrackSCMListener;
    }
}
