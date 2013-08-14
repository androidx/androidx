package android.support.v7.lint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;

import java.util.Collections;
import java.util.List;

public class AppCompatIssueRegistry extends IssueRegistry {
    public AppCompatIssueRegistry() {
    }

    @Override
    public List<Issue> getIssues() {
        return Collections.singletonList(AppCompatDetector.ISSUE);
    }
}
