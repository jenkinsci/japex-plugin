package hudson.plugins.japex;

import hudson.util.ChartUtil;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;

/**
 * Model object that represents the generated trend chart for one test case.
 *
 * @author Kohsuke Kawaguchi
 */
public class TestCaseGraph {
    private final TrendReport owner;
    /**
     * Test case name.
     */
    private final String name;

    public TestCaseGraph(TrendReport owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
        if(ChartUtil.awtProblem) {
            // not available. send out error message
            rsp.sendRedirect2(req.getContextPath()+"/images/headless.png");
            return;
        }

        HudsonChartGenerator gen = owner.chartGen;

        if(gen.timestamp!=null && req.checkIfModified(gen.timestamp,rsp))
            return; // up to date

        ChartUtil.generateGraph(req,rsp,gen.createTrendChart(name),400,200);
    }

}
