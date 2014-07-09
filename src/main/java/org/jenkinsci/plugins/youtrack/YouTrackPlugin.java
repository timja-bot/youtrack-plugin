package org.jenkinsci.plugins.youtrack;

import hudson.Plugin;

/**
 * Listens to SCM changes.
 */
public class YouTrackPlugin extends Plugin {
    private transient YouTrackSCMListener scmListener;
    /**
     * For saving which ids has been processed.
     */
    private YoutrackProcessedRevisionsSaver revisionsSaver;


    @Override
    public void start() throws Exception {
        super.start();

        scmListener = new YouTrackSCMListener();
        scmListener.register();
    }

    @Override
    public void stop() throws Exception {
        scmListener.unregister();
        super.stop();
    }

    public synchronized YoutrackProcessedRevisionsSaver getRevisionsSaver() {
        if (revisionsSaver == null) {
            revisionsSaver = new YoutrackProcessedRevisionsSaver();
        }
        return revisionsSaver;
    }
}
