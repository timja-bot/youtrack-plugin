package org.jenkinsci.plugins.youtrack;

import hudson.model.AbstractProject;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 */
public class YouTrackSite {
    private String name;
    private String url;
    private String username;
    private String password;
    private transient boolean pluginEnabled;
    private transient boolean runAsEnabled;
    private transient boolean commandsEnabled;
    private transient boolean commentEnabled;
    private transient boolean annotationsEnabled;
    private transient String linkVisibility;
    private transient String stateFieldName;
    private transient String fixedValues;
    private transient boolean silentCommands;
    private transient boolean silentLinks;
    private transient String project;

    @DataBoundConstructor
    public YouTrackSite(String name, String username, String password, String url) {
        this.username = username;
        this.password = password;
        this.url = url;
        this.name = name;
    }

    public static YouTrackSite get(AbstractProject<?, ?> project) {
        YouTrackProjectProperty ypp = project.getProperty(YouTrackProjectProperty.class);
        if (ypp != null) {
            YouTrackSite site = ypp.getSite();
            if (site != null) {
                return site;
            }
        }
        YouTrackSite[] sites = YouTrackProjectProperty.DESCRIPTOR.getSites();
        if (sites.length == 1) {
            return sites[0];
        }
        return null;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPluginEnabled() {
        return pluginEnabled;
    }

    public void setPluginEnabled(boolean pluginEnabled) {
        this.pluginEnabled = pluginEnabled;
    }

    public boolean isRunAsEnabled() {
        return runAsEnabled;
    }

    public void setRunAsEnabled(boolean runAsEnabled) {
        this.runAsEnabled = runAsEnabled;
    }

    public boolean isCommandsEnabled() {
        return commandsEnabled;
    }

    public void setCommandsEnabled(boolean commandsEnabled) {
        this.commandsEnabled = commandsEnabled;
    }

    public boolean isAnnotationsEnabled() {
        return annotationsEnabled;
    }

    public void setAnnotationsEnabled(boolean annotationsEnabled) {
        this.annotationsEnabled = annotationsEnabled;
    }

    public boolean isCommentEnabled() {
        return commentEnabled;
    }

    public void setCommentEnabled(boolean commentEnabled) {
        this.commentEnabled = commentEnabled;
    }

    public String getLinkVisibility() {
        return linkVisibility;
    }

    public void setLinkVisibility(String linkVisibility) {
        this.linkVisibility = linkVisibility;
    }

    public String getStateFieldName() {
        return stateFieldName;
    }

    public void setStateFieldName(String stateFieldName) {
        this.stateFieldName = stateFieldName;
    }

    public String getFixedValues() {
        return fixedValues;
    }

    public void setFixedValues(String fixedValues) {
        this.fixedValues = fixedValues;
    }

    public boolean isSilentCommands() {
        return silentCommands;
    }

    public void setSilentCommands(boolean silentCommands) {
        this.silentCommands = silentCommands;
    }

    public boolean isSilentLinks() {
        return silentLinks;
    }

    public void setSilentLinks(boolean silentLinks) {
        this.silentLinks = silentLinks;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }
}
