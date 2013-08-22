package android.support.v7.lint;

import com.android.tools.lint.checks.ApiLookup;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.List;

public class AppCompatDetector extends Detector implements Detector.ClassScanner {

    public static final Issue ISSUE = Issue.create(
            "AppCompatMethod",
            "Using Wrong AppCompat Method",
            "Finds cases where a custom `appcompat` method should be used instead",
            "When using the appcompat library, there are some methods you should be calling " +
            "instead of the normal ones; for example, `getSupportActionBar()` instead of " +
            "`getActionBar()`. This lint check looks for calls to the wrong method.",
            Category.CORRECTNESS, 6, Severity.WARNING,
            new Implementation(
                    AppCompatDetector.class,
                    Scope.CLASS_FILE_SCOPE)).
            addMoreInfo("http://developer.android.com/tools/support-library/index.html");

    private static final String GET_ACTION_BAR = "getActionBar";
    private static final String START_ACTION_MODE = "startActionMode";
    private static final String SET_PROGRESS_BAR_VIS = "setProgressBarVisibility";
    private static final String SET_PROGRESS_BAR_IN_VIS = "setProgressBarIndeterminateVisibility";
    private static final String SET_PROGRESS_BAR_INDETERMINATE = "setProgressBarIndeterminate";
    private static final String REQUEST_WINDOW_FEATURE = "requestWindowFeature";

    public AppCompatDetector() {
    }

    @Override
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    @Override
    public List<String> getApplicableCallNames() {
        return Arrays.asList(
                GET_ACTION_BAR,
                START_ACTION_MODE,
                SET_PROGRESS_BAR_VIS,
                SET_PROGRESS_BAR_IN_VIS,
                SET_PROGRESS_BAR_INDETERMINATE,
                REQUEST_WINDOW_FEATURE);
    }

    @Override
    public void checkCall(ClassContext context, ClassNode classNode, MethodNode method,
            MethodInsnNode call) {
        String owner = call.owner;

        while (!owner.equals("android/support/v7/app/ActionBarActivity")) {
            if (!ApiLookup.isRelevantOwner(owner)) {
                owner = context.getDriver().getSuperClass(owner);
                if (owner == null) {
                    return;
                }
            } else {
                return;
            }
        }

        String name = call.name;
        String replace = null;
        if (GET_ACTION_BAR.equals(name)) {
            replace = "getSupportActionBar";
        } else if (START_ACTION_MODE.equals(name)) {
            replace = "startSupportActionMode";
        } else if (SET_PROGRESS_BAR_VIS.equals(name)) {
            replace = "setSupportProgressBarVisibility";
        } else if (SET_PROGRESS_BAR_IN_VIS.equals(name)) {
            replace = "setSupportProgressBarIndeterminateVisibility";
        } else if (SET_PROGRESS_BAR_INDETERMINATE.equals(name)) {
            replace = "setSupportProgressBarIndeterminate";
        } else if (REQUEST_WINDOW_FEATURE.equals(name)) {
            replace = "supportRequestWindowFeature";
        }

        if (replace != null) {
            String message = String.format("Should use %1$s instead of %2$s name", replace, name);
            context.report(ISSUE, context.getLocation(call), message, null);
        }
    }
}
