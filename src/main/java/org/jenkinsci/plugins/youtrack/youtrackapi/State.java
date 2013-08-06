package org.jenkinsci.plugins.youtrack.youtrackapi;

/**
 * This object represents a state in youtrack.
 */
public class State {
    private String value;
    private String description;
    private boolean resolved;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}
