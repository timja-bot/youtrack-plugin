package org.jenkinsci.plugins.youtrack;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.youtrack.youtrackapi.User;
import org.jenkinsci.plugins.youtrack.youtrackapi.YouTrackServer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;

public class YouTrackProjectProperty extends JobProperty<AbstractProject<?, ?>> {
    /**
     * The name of the site.
     */
    private String siteName;

    /**
     * If the YouTrack plugin is enabled.
     */
    private boolean pluginEnabled;
    /**
     * If ping back comments is enabled.
     */
    private boolean commentsEnabled;
    /**
     * If executing commands is enabled.
     */
    private boolean commandsEnabled;
    /**
     * If the commands should be run as the vcs user.
     */
    private boolean runAsEnabled;

    /**
     * If ChangeLog annotations is enabled.
     */
    private boolean annotationsEnabled;

    /**
     * The name of the group comment links should be visible for.
     */
    private String linkVisibility;
    /**
     * Name of state field to check for weather an issue is selected.
     */
    private String stateFieldName;
    /**
     * Comma-separated list of values that are seen as fixed.
     */
    private String fixedValues;

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();


    @DataBoundConstructor
    public YouTrackProjectProperty(String siteName, boolean pluginEnabled, boolean commentsEnabled, boolean commandsEnabled, boolean runAsEnabled, boolean annotationsEnabled, String linkVisibility, String stateFieldName, String fixedValues) {
        this.siteName = siteName;
        this.pluginEnabled = pluginEnabled;
        this.commentsEnabled = commentsEnabled;
        this.commandsEnabled = commandsEnabled;
        this.runAsEnabled = runAsEnabled;
        this.annotationsEnabled = annotationsEnabled;
        this.linkVisibility = linkVisibility;
        this.stateFieldName = stateFieldName;
        this.fixedValues = fixedValues;
    }

    @Override
    public JobPropertyDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public boolean isPluginEnabled() {
        return pluginEnabled;
    }

    public void setPluginEnabled(boolean pluginEnabled) {
        this.pluginEnabled = pluginEnabled;
    }

    public boolean isCommentsEnabled() {
        return commentsEnabled;
    }

    public void setCommentsEnabled(boolean commentsEnabled) {
        this.commentsEnabled = commentsEnabled;
    }

    public boolean isCommandsEnabled() {
        return commandsEnabled;
    }

    public void setCommandsEnabled(boolean commandsEnabled) {
        this.commandsEnabled = commandsEnabled;
    }

    public boolean isRunAsEnabled() {
        return runAsEnabled;
    }

    public void setRunAsEnabled(boolean runAsEnabled) {
        this.runAsEnabled = runAsEnabled;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public boolean isAnnotationsEnabled() {
        return annotationsEnabled;
    }

    public void setAnnotationsEnabled(boolean annotationsEnabled) {
        this.annotationsEnabled = annotationsEnabled;
    }

    public String getLinkVisibility() {
        return linkVisibility;
    }

    public void setLinkVisibility(String linkVisibility) {
        this.linkVisibility = linkVisibility;
    }

    public static final class DescriptorImpl extends JobPropertyDescriptor {
        private final CopyOnWriteList<YouTrackSite> sites = new CopyOnWriteList<YouTrackSite>();

        public DescriptorImpl() {
            super(YouTrackProjectProperty.class);
            load();

        }

        public void setSites(YouTrackSite site) {
            sites.add(site);
        }

        public YouTrackSite[] getSites() {
            return sites.toArray(new YouTrackSite[0]);
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            YouTrackProjectProperty ypp = req.bindParameters(YouTrackProjectProperty.class, "youtrack.");
            if (ypp.siteName == null) {
                ypp = null;
            }
            return ypp;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {
            sites.replaceBy(req.bindParametersToList(YouTrackSite.class,
                    "youtrack."));
            save();
            return true;
        }


        @Override
        public String getDisplayName() {
            return "YouTrack Plugin";
        }

        public FormValidation doVersionCheck(@QueryParameter final String value) throws IOException, ServletException {
            return new FormValidation.URLCheck() {

                @Override
                protected FormValidation check() throws IOException, ServletException {
                    YouTrackServer youTrackServer = new YouTrackServer(value);
                    String[] version = youTrackServer.getVersion();
                    if(version == null) {
                        return FormValidation.warning("Could not get version, maybe because version is below 4.x");
                    } else {
                        return FormValidation.ok();
                    }
                }
            }.check();
        }

        public FormValidation doTestConnection(
                @QueryParameter("youtrack.url") final String url,
                @QueryParameter("youtrack.username") final String username,
                @QueryParameter("youtrack.password") final String password) {

            YouTrackServer youTrackServer = new YouTrackServer(url);
            if (username != null && !username.equals("")) {
                User login = youTrackServer.login(username, password);
                if(login != null) {
                    return FormValidation.ok("Connection ok!");
                } else {
                    return FormValidation.error("Could not login with given options");
                }
            } else {
                return FormValidation.ok();
            }
        }
    }

    public YouTrackSite getSite() {
        YouTrackSite result = null;
        YouTrackSite[] sites = DESCRIPTOR.getSites();
        if (siteName == null && sites.length > 0) {
            result = sites[0];
        }

        for (YouTrackSite site : sites) {
            if (site.getName().equals(siteName)) {
                result = site;
                break;
            }
        }
        if (result != null) {
            result.setPluginEnabled(pluginEnabled);
            result.setCommentEnabled(commentsEnabled);
            result.setCommandsEnabled(commandsEnabled);
            result.setAnnotationsEnabled(annotationsEnabled);
            result.setRunAsEnabled(runAsEnabled);
            result.setLinkVisibility(linkVisibility);
            result.setStateFieldName(stateFieldName);
            result.setFixedValues(fixedValues);
        }
        return result;
    }
}
