/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.wear.protolayout.renderer.common;

import android.content.ComponentName;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

    private final @NonNull String mComponent;

    public LoggingUtilsImpl(@NonNull ComponentName component) {
        this.mComponent = component.flattenToShortString();
    }

    @Override
    public void logD(@NonNull String tag, @NonNull String message) {
        if (canLogD(tag)) {
            logDInternal(tag, message);
        }
    }

    @Override
    @FormatMethod
    public void logD(
            @NonNull String tag, @FormatString @NonNull String format, Object @NonNull ... args) {
        logD(tag, String.format(format, args));
    }

    @Override
    public boolean canLogD(@NonNull String tag) {
        return Log.isLoggable(tag, Log.DEBUG);
    }

    /** Logs message as debug if level is set or if the build type is not user. */
    @Override
    public void logDOrNotUser(@NonNull String tag, @NonNull String message) {
        if (canLogD(tag) || !isUserBuild()) {
            logDInternal(tag, message);
        }
    }

    /** Logs message as debug if level is set or if the build type is not user. */
    @Override
    @FormatMethod
    public void logDOrNotUser(
            @NonNull String tag,
            @FormatString @NonNull String format,
            @Nullable Object @NonNull ... args) {
        if (canLogD(tag) || !isUserBuild()) {
            logDInternal(tag, String.format(format, args));
        }
    }

    @Override
    public void logW(@NonNull String tag, @NonNull String message) {
        Log.w(tag, message + " " + mComponent);
    }

    @Override
    public void logE(@NonNull String tag, @NonNull String message, @Nullable Throwable tr) {
        Log.e(tag, message + " " + mComponent, tr);
    }

    private void logDInternal(@NonNull String tag, @NonNull String message) {
        Log.d(tag, message + " " + mComponent);
    }

    private static boolean isUserBuild() {
        return Build.TYPE.equals("user");
    }
}
