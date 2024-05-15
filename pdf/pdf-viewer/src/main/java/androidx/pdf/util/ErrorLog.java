/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RestrictTo;

/**
 * Error logging utility. Logs errors on the internal Log system and tracks analytics.
 *
 * <p>Guidelines for this class:
 *
 * <ul>
 *   <li>Privacy: Do not track any PII (user personal info)
 *   <li>Only track Exception classes and generic messages (no getMessage(), no user info)
 *   <li>(Re-)throw RuntimeExceptions only in Dev builds, swallow them on release builds.
 * </ul>
 */
// TODO: Track errors.
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ErrorLog {

    private ErrorLog() {
    }

    /** Logs and tracks the error message. */
    public static void log(String tag, String message) {
        Log.e(tag, message);
    }

    /** Logs and tracks the error message. The 'details' arg is only used locally. */
    public static void log(String tag, String message, String details) {
        Log.e(tag, message + " " + details);
    }

    /**
     * Logs and tracks the exception (tracks only the exception name). Doesn't re-throw the
     * exception.
     */
    public static void log(String tag, String method, Throwable e) {
        Log.e(tag, method, e);
    }

    /** Logs and tracks the exception. If running a dev build, re-throws the exception. */
    public static void logAndThrow(String tag, String method, Throwable e) {
        log(tag, method, e);
        if (isDebuggable()) {
            Log.e(tag, "In method " + method + ": ", e);
            throw asRuntimeException(e);
        }
    }

    /** Logs and tracks the error message. If running a dev build, throws a runtime exception. */
    public static void logAndThrow(String tag, String method, String message) {
        Log.e(tag, method + ": " + message);
        if (isDebuggable()) {
            throw new RuntimeException(message);
        }
    }

    private static RuntimeException asRuntimeException(Throwable e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException(e);
    }

    /** Logs and tracks the exception, then rethrows it. */
    public static void logAndAlwaysThrow(String tag, String method, Throwable e) {
        log(tag, method, e);
        throw asRuntimeException(e);
    }

    /**
     * A safer version of Preconditions.checkState that will log instead of throw in release builds.
     *
     * <p>Checks <code>condition</code> and {@link #logAndThrow} the error message as an {@link
     * IllegalArgumentException} if it is false.
     */
    public static void checkState(boolean condition, String tag, String method, String message) {
        if (!condition) {
            IllegalArgumentException e = new IllegalArgumentException(message);
            logAndThrow(tag, method, e);
        }
    }

    /** Convert int value to range. */
    public static String bracketValue(int value) {
        if (value == 0) {
            return "0";
        } else if (value == 1) {
            return "1";
        } else if (value <= 10) {
            return "up to 10";
        } else if (value <= 100) {
            return "up to 100";
        } else if (value <= 1000) {
            return "up to 1000";
        } else {
            return "more than 1000";
        }
    }

    private static boolean isDebuggable() {
        return "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
    }
}
