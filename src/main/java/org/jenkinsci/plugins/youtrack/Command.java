package org.jenkinsci.plugins.youtrack;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * This class represents a command performed by this plugin.
 */
@ToString
public class Command {
    public enum Status {OK, FAILED, NOT_LOGGED_IN}

    @Getter @Setter private String siteName;
    @Getter @Setter private String issueId;
    @Getter @Setter private String username;
    @Getter @Setter private String comment;
    @Getter @Setter private String command;
    @Getter @Setter private String response;
    @Getter @Setter private Status status;
    @Getter @Setter private String group;
    private Date date;
    @Getter @Setter private boolean silent;

    public Date getDate() {
        return new Date(date.getTime());
    }

    public void setDate(Date date) {
        this.date = new Date(date.getTime());
    }
}
