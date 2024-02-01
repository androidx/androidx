package androidx.wear.protolayout.renderer.common;

import android.content.ComponentName;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;

/**
 * Logger used for extensive logging. Note that all logs will contain the component name. To enable
 * logs use the following command:
 *
 * <pre>
 *   adb shell setprop log.tag.class_tag DEBUG
 * </pre>
 */
@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
public class LoggingUtilsImpl implements LoggingUtils {

    @NonNull private final ComponentName mComponent;

    public LoggingUtilsImpl(@NonNull ComponentName component) {
        this.mComponent = component;
    }

    @Override
    public void logD(@NonNull String tag, @NonNull String message) {
        logInternal(tag, message);
    }

    @Override
    @FormatMethod
    public void logD(
        @NonNull String tag,
        @NonNull @FormatString String format,
        @NonNull Object... args) {
        logD(tag, String.format(format, args));
    }

    private void logInternal(@NonNull String tag, @NonNull String message) {
        if (canLogD(tag)) {
            Log.d(tag, "Logs for: " + mComponent + "\n" + message);
        }
    }

    @Override
    public boolean canLogD(@NonNull String tag) {
        return Log.isLoggable(tag, Log.DEBUG);
    }
}
