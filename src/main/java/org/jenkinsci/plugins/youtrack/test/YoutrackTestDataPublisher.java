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
        Data data = new Data(build);

        List<CaseResult> failedTests = testResult.getFailedTests();
        for (CaseResult failedTest : failedTests) {
            CaseResult previousResult = failedTest.getPreviousResult();
            if (previousResult != null) {
                YouTrackTestAction previousAction = previousResult.getTestAction(YouTrackTestAction.class);
                if (previousAction != null && previousAction.getYoutrackIssueId() != null) {
                    YouTrackTestAction youTrackTestAction = new YouTrackTestAction(data, failedTest, failedTest.getId(), previousAction.getYoutrackIssueId());
                    data.addLink(testResult.getId(), youTrackTestAction);
                }
            }
        }
        return data;

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

            YouTrackTestAction result = links.get(id);

            if (result != null) {
                return Collections.<TestAction>singletonList(result);
            }

            if (testObject instanceof CaseResult) {
                CaseResult caseResult = (CaseResult) testObject;
                if (!caseResult.isPassed() && !caseResult.isSkipped()) {
                    return Collections.<TestAction>singletonList(new YouTrackTestAction(this, caseResult, id, null));
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
