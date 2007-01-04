package hudson.plugins.japex;

import com.sun.japex.report.ChartGenerator;
import com.sun.japex.report.TestSuiteReport;
import hudson.model.Build;

import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates trend charts.
 *
 * @author Kohsuke Kawaguchi
 */
final class HudsonChartGenerator extends ChartGenerator {
    final Calendar timestamp;

    private final Set<String> testNames = new LinkedHashSet<String>();

    public HudsonChartGenerator(List<? extends TestSuiteReport> reports, Build b) {
        super(reports);
        timestamp = b==null ? null : b.getTimestamp();

        // Populate set of test cases across all reports
        for (TestSuiteReport report : reports) {
            for (TestSuiteReport.Driver driver : report.getDrivers()) {
                for (TestSuiteReport.TestCase test : driver.getTestCases()) {
                    testNames.add(test.getName());
                }
            }
        }
    }

    public Collection<String> getTestNames() {
        return testNames;
    }

}
