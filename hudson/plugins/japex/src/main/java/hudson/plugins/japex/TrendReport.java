package hudson.plugins.japex;

import com.sun.japex.report.MeanMode;
import hudson.model.ModelObject;
import hudson.model.Project;
import hudson.util.ChartUtil;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a trend report.
 *
 * @author Kohsuke Kawaguchi
 */
public class TrendReport implements ModelObject {

    /*package*/ final HudsonChartGenerator chartGen;

    private final String configName;
    private final Project project;
    /**
     * Conversion from escaped test case names to original test case names,
     * since test case names may contain URL-unsafe characters.
     */
    private final Map<String,String> testCaseNames = new HashMap<String, String>();

    TrendReport(Project project, String configName, HudsonChartGenerator chartGen) {
        this.project = project;
        this.configName = configName;
        this.chartGen = chartGen;
        for (String name : chartGen.getTestNames()) {
            testCaseNames.put( name.replace('/','_'), name );
        }
    }

    /**
     * This is the configuration file name.
     */
    public String getDisplayName() {
        return configName;
    }

    /**
     * Gets all the test case names from safe names to unsafe names.
     */
    public Map<String,String> getTestCaseNames() throws IOException {
        return testCaseNames;
    }

    public Project getProject() {
        return project;
    }

//
//
// Web methods
//
//

    /**
     * Gets to the object that represents individual test case result.
     */
    public TestCaseGraph getTestCaseGraph(String safeName) {
        return new TestCaseGraph(this,testCaseNames.get(safeName));
    }

    public void doArithmeticMeanGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
        doMeanGraph(req,rsp, MeanMode.ARITHMETIC);
    }

    public void doGeometricMeanGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
        doMeanGraph(req,rsp, MeanMode.GEOMETRIC);
    }

    public void doHarmonicMeanGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
        doMeanGraph(req,rsp, MeanMode.HARMONIC);
    }

    private void doMeanGraph(StaplerRequest req, StaplerResponse rsp, MeanMode mean) throws IOException {
        if(ChartUtil.awtProblem) {
            // not available. send out error message
            rsp.sendRedirect2(req.getContextPath()+"/images/headless.png");
            return;
        }

        if(chartGen.timestamp!=null && req.checkIfModified(chartGen.timestamp,rsp))
            return; // up to date

        ChartUtil.generateGraph(req,rsp,chartGen.createTrendChart(mean),400,200);
    }
}
