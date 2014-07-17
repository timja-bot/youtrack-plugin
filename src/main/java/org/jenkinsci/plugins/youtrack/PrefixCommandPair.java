package org.jenkinsci.plugins.youtrack;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * The represents
 */
public class PrefixCommandPair extends AbstractDescribableImpl<PrefixCommandPair> {
    private String prefix;
    private String command;

    @DataBoundConstructor
    public PrefixCommandPair(String prefix, String command) {
        this.prefix = prefix;
        this.command = command;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public PrefixCommandPairDescriptor getDescriptor() {
        return (PrefixCommandPairDescriptor) super.getDescriptor();
    }

    @Extension
    public static class PrefixCommandPairDescriptor extends Descriptor<PrefixCommandPair> {
        @Override
        public String getDisplayName() {
            return "Prefix Command Pair";
        }
    }
}
