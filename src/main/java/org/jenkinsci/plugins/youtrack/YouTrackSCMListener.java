package org.jenkinsci.plugins.youtrack;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.ChangeLogSet;
import hudson.tasks.Mailer;
import lombok.Data;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.youtrack.youtrackapi.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTrackSCMListener extends SCMListener {

    @Override
    public void onChangeLogParsed(AbstractBuild<?, ?> build, BuildListener listener, ChangeLogSet<?> changeLogSet) throws Exception {
        if (build.getRootBuild().equals(build)) {

            YoutrackIssueUpdater youtrackIssueUpdater = getYoutrackIssueUpdater();
            youtrackIssueUpdater.update(build, listener, changeLogSet);
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
