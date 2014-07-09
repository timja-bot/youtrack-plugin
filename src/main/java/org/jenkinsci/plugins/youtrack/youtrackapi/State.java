package org.jenkinsci.plugins.youtrack.youtrackapi;

import lombok.Getter;
import lombok.Setter;

/**
 * This object represents a state in youtrack.
 */
public class State {
    @Getter @Setter private String value;
    @Getter @Setter private String description;
    @Getter @Setter private boolean resolved;
}
