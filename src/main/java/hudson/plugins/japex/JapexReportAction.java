package hudson.plugins.japex;

import com.sun.japex.report.TestSuiteReport;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.Project;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map.Entry;

/**
 * Project action to display trend reports.
 *
 * @author Kohsuke Kawaguchi
 */
public class JapexReportAction implements Action, StaplerProxy {
    private final Project<?,?> project;

    public JapexReportAction(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public String getDisplayName() {
        return "Japex Trend Report";
    }

    public String getIconFileName() {
        return "graph.gif";
    }

    public String getUrlName() {
        return "japex";
    }

    /**
     * Cached {@link TrendReport}s, keyed by their configuration name.
     */
    private WeakReference<Parsed> cache;

    private final class Parsed {
        final Map<String,TrendReport> reports = new HashMap<String,TrendReport>();
        final int buildNumber;
        final TrendReport singleton;

        public Parsed(Build build, Map<String,List<TestSuiteReport>> reports) {
            this.buildNumber = build.getNumber();
            for (Entry<String,List<TestSuiteReport>> e : reports.entrySet()) {
                this.reports.put(e.getKey(),
                    new TrendReport(project, e.getKey(), new HudsonChartGenerator(e.getValue(),build)));
            }
            if(reports.size()==1)
                singleton = this.reports.values().iterator().next();
            else
                singleton = null;
        }
    }

    public TrendReport getReport(String configName) throws IOException {
        return parseReports().reports.get(configName);
    }

    public TrendReport getDynamic(String token, StaplerRequest req, StaplerResponse rsp ) throws IOException {
        return getReport(token);
    }

    public boolean hasReports() throws IOException {
        return !parseReports().reports.isEmpty();
    }

    public Collection<TrendReport> getReports() throws IOException {
        return parseReports().reports.values();
    }

    /**
     * If there's only one {@link TrendReport}, simply display that report
     * on this view.
     */
    public Object getTarget() {
        try {
            Parsed parsed = parseReports();
            if(parsed.singleton!=null) {
                // forward to that single test report
                return parsed.singleton;
            } else {
                return this;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,"Failed to parse Japex reports",e);
            // this should cause index view to be displayed on this object,
            // which should report the parse failure
            return this;
        }
    }

    /**
     * Creates the {@link HudsonChartGenerator} (or reuse the last one.)
     */
    /*package*/ synchronized Parsed parseReports() throws IOException {
        Build lb = project.getLastBuild();

        if(cache!=null) {
            Parsed parsed = cache.get();
            if(parsed!=null && lb!=null && parsed.buildNumber==lb.getNumber())
                return parsed; // reuse the cached instance
        }

        // parse reports
        Map<String,List<TestSuiteReport>> reports = new HashMap<String,List<TestSuiteReport>>();
        for (Build build : project.getBuilds()) {
            File dir = JapexPublisher.getJapexReport(build);
            File[] files = dir.listFiles(REPORT_FILTER);
            if(files!=null) {
                for (File f : files) {
                    try {
                        TestSuiteReport rpt = new TestSuiteReport(f);
                        String configName = rpt.getParameters().get("configFile").replace('/','.');

                        List<TestSuiteReport> reportList = reports.get(configName);
                        if(reportList==null) {
                            reportList = new ArrayList<TestSuiteReport>();
                            reports.put(configName,reportList);
                        }

                        reportList.add(rpt);
                    } catch (SAXException e) {
                        IOException x = new IOException("Failed to parse " + f);
                        x.initCause(e);
                        throw x;
                    } catch (RuntimeException e) {
                        // Japex sometimes intentionally send RuntimeException
                        IOException x = new IOException("Failed to parse " + f);
                        x.initCause(e);
                        throw x;
                    }
                }
            }
        }

        Parsed parsed = new Parsed(lb,reports);

        cache = new WeakReference<Parsed>(parsed);

        return parsed;
    }

    private static final FileFilter REPORT_FILTER = new FileFilter() {
        public boolean accept(File f) {
            return f.getName().endsWith(".xml");
        }
    };

    private static final Logger LOGGER = Logger.getLogger(JapexReportAction.class.getName());
}
