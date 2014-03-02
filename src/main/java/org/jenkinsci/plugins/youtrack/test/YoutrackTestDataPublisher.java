package org.jenkinsci.plugins.youtrack.test;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Saveable;
import hudson.tasks.junit.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class annotates the test result page with an action to create/view issue.
 */
public class YoutrackTestDataPublisher extends TestDataPublisher {
    @DataBoundConstructor
    public YoutrackTestDataPublisher() {
    }

    @Override
    public TestResultAction.Data getTestData(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, TestResult testResult) throws IOException, InterruptedException {
        return new Data(build);
    }

    public static class Data extends TestResultAction.Data implements Saveable {

        private final AbstractBuild<?, ?> build;
        private Map<String, YouTrackTestAction> links = new HashMap<String, YouTrackTestAction>();

        public AbstractBuild<?, ?> getBuild() {
            return build;
        }

        public Data(AbstractBuild<?, ?> build) {
            this.build = build;
        }

        @Override
        public List<TestAction> getTestAction(TestObject testObject) {
            String id = testObject.getId();

            if (testObject instanceof CaseResult) {
                CaseResult cr = (CaseResult) testObject;
                if (!cr.isPassed() && !cr.isSkipped()) {

                    YouTrackTestAction youTrackTestAction = null;
                    CaseResult previousResult = cr.getPreviousResult();

                    youTrackTestAction = links.get(id);

                    if (youTrackTestAction == null && !links.containsKey(id)) {
                        if (previousResult != null) {
                            YouTrackTestAction testAction = previousResult.getTestAction(YouTrackTestAction.class);
                            if (testAction != null) {
                                youTrackTestAction = new YouTrackTestAction(this, (CaseResult) testObject, id, testAction.getYoutrackIssueId());
                            }
                        }
                    }

                    if (youTrackTestAction == null) {
                        youTrackTestAction = new YouTrackTestAction(this, (CaseResult) testObject, id, null);
                    }

                    return Collections.<TestAction>singletonList(youTrackTestAction);
                }
            }

            return Collections.emptyList();
        }

        public void addLink(String id, YouTrackTestAction action) {
            links.put(id, action);
        }

        public void save() throws IOException {
            build.save();
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<TestDataPublisher> {

        @Override
        public String getDisplayName() {
            return "Enable manual linking of failed test to Youtrack issues";
        }
    }
}
