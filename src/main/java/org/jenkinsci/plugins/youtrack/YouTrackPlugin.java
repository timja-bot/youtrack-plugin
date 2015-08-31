package org.jenkinsci.plugins.youtrack;

import hudson.Plugin;
import org.apache.commons.beanutils.Converter;
import org.kohsuke.stapler.Stapler;

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
        Stapler.CONVERT_UTILS.register(new Converter() {
            public Object convert(Class type, Object value) {
                if (value == null) {
                    return YoutrackBuildFailureMode.NONE;
                } else {
                    return Enum.valueOf(YoutrackBuildFailureMode.class, String.valueOf(value));
                }
            }
        }, YoutrackBuildFailureMode.class);
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
