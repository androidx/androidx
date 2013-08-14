package android.support.v7.lint;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import com.android.tools.lint.Main;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.LintRequest;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Severity;

import junit.framework.TestCase;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class AppCompatDetectorTest extends TestCase {
    public void testArguments() throws Exception {
        File tempDir = Files.createTempDir();
        File dir = new File(tempDir, "testProject");
        boolean mkdirs = dir.mkdirs();
        assertTrue(dir.getPath(), mkdirs);
        copy("AndroidManifest.xml.txt", new File(dir, "AndroidManifest.xml"));
        copy("classpath.txt", new File(dir, ".classpath"));
        copy("AppCompatTest.java.txt", new File(dir, "src/test/pkg/AppCompatTest.java"));
        copy("AppCompatTest.class.data", new File(dir, "bin/classes/test/pkg/AppCompatTest.class"));

        // TODO: Writing unit tests this way is a bit painful. We should expose the lint unit
        // test framework for use by external lint checks.

        final StringBuilder output = new StringBuilder(1000);
        LintClient client = new Main() {
            @Override
            public void report(Context context, Issue issue, Severity severity, Location location,
                    String message, Object data) {
                output.append(location.getFile().getName());
                output.append(':').append(location.getStart().getLine());
                output.append(": ").append(severity.getDescription());
                output.append(": ").append(message);
                output.append(" [").append(issue.getId()).append(']');
                output.append('\n');
            }
        };
        IssueRegistry registry = new IssueRegistry() {
            @Override
            public List<Issue> getIssues() {
                return Collections.singletonList(AppCompatDetector.ISSUE);
            }
        };
        LintDriver driver = new LintDriver(registry, client);
        LintRequest request = new LintRequest(client, Collections.singletonList(dir));

        driver.analyze(request);
        String expected = ""
                + "AppCompatTest.java:6: Warning: Should use getSupportActionBar instead of getActionBar name [AppCompatMethod]\n"
                + "AppCompatTest.java:9: Warning: Should use startSupportActionMode instead of startActionMode name [AppCompatMethod]\n"
                + "AppCompatTest.java:12: Warning: Should use supportRequestWindowFeature instead of requestWindowFeature name [AppCompatMethod]\n"
                + "AppCompatTest.java:15: Warning: Should use setSupportProgressBarVisibility instead of setProgressBarVisibility name [AppCompatMethod]\n"
                + "AppCompatTest.java:16: Warning: Should use setSupportProgressBarIndeterminate instead of setProgressBarIndeterminate name [AppCompatMethod]\n"
                + "AppCompatTest.java:17: Warning: Should use setSupportProgressBarIndeterminateVisibility instead of setProgressBarIndeterminateVisibility name [AppCompatMethod]\n";
        assertEquals(expected, output.toString());
    }

    private static void copy(String name, File target) throws Exception {
        target = new File(target.getPath().replace('/', File.separatorChar));
        @SuppressWarnings("SpellCheckingInspection")
        InputStream stream = AppCompatDetectorTest.class.getResourceAsStream("testdata/" + name);
        byte[] data = ByteStreams.toByteArray(stream);
        stream.close();
        File parentFile = target.getParentFile();
        if (!parentFile.exists()) {
            boolean mkdirs = parentFile.mkdirs();
            assertTrue(mkdirs);
        }
        Files.write(data, target);
    }
}
