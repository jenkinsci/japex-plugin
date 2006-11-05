package hudson.plugins.japex;

import hudson.Plugin;
import hudson.tasks.BuildStep;

/**
 * @author Kohsuke Kawaguchi
 */
public class PluginImpl extends Plugin {
    public void start() throws Exception {
        BuildStep.PUBLISHERS.add(JapexPublisher.DESCRIPTOR);
    }
}
