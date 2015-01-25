package org.jenkinsci.plugins.youtrack;

/**
 * Modes of failure for the build if the plugin cannot apply its commands.
 */
public enum YoutrackBuildFailureMode {
    NONE, UNSTABLE, FAILURE
}
