package androidx.wear.protolayout.renderer.common;

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
public interface LoggingUtils {

    /** LogD a formatted message. */
    void logD(@NonNull String tag,@NonNull  String message);

    /** LogD a formatted message. */
    @FormatMethod
    void logD(@NonNull String tag, @NonNull @FormatString String format, @NonNull Object... args);

    /**
     * Check whether debug logging is allowed or not for the given {@code tag}. This will allow
     * clients to skip building logs if it's not necessary.
     */
    boolean canLogD(@NonNull String tag);
}
