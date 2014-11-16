package org.jenkinsci.plugins.youtrack;

import hudson.model.AbstractProject;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

public class YouTrackSite {
    @Getter @Setter private String name;
    @Getter @Setter private String url;
    @Getter @Setter private String username;
    @Getter @Setter private String password;
    @Getter @Setter private transient boolean pluginEnabled;
    @Getter @Setter private transient boolean runAsEnabled;
    @Getter @Setter private transient boolean commandsEnabled;
    @Getter @Setter private transient boolean commentEnabled;
    @Getter @Setter private transient String commentText;
    @Getter @Setter private transient boolean annotationsEnabled;
    @Getter @Setter private transient String linkVisibility;
    @Getter @Setter private transient String stateFieldName;
    @Getter @Setter private transient String fixedValues;
    @Getter @Setter private transient boolean silentCommands;
    @Getter @Setter private transient boolean silentLinks;
    @Getter @Setter private transient String project;
    @Getter @Setter private transient String executeProjectLimits;
    @Getter @Setter private transient List<PrefixCommandPair> prefixCommandPairs;
    @Getter @Setter private boolean trackCommits;

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
}
