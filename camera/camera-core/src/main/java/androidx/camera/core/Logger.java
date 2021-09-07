/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core;

import android.os.Build;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * Handles logging requests inside CameraX. Log messages are output only if:
 * - The minimum logging level allows for it. The minimum logging level is set via
 * {@link #setMinLogLevel(int)}, which should typically be called during the process of configuring
 * CameraX.
 * - The log tag is {@linkplain Log#isLoggable(String, int) loggable}. This can be configured
 * by setting the system property `setprop log.tag.TAG LEVEL`, where TAG is the log tag, and
 * LEVEL is {@link Log#DEBUG}, {@link Log#INFO}, {@link Log#WARN} or {@link Log#ERROR}.
 * <p> A typical usage of the Logger looks as follows:
 * <pre>
 *     try {
 *         int quotient = dividend / divisor;
 *     } catch (ArithmeticException exception) {
 *         Logger.e(TAG, "Divide operation error", exception);
 *     }
 * </pre>
 * <p> If an action has to be performed alongside logging, or if building the log message is costly,
 * perform a log level check before attempting to log.
 * <pre>
 *     try {
 *         int quotient = dividend / divisor;
 *     } catch (ArithmeticException exception) {
 *         if (Logger.isErrorEnabled(TAG)) {
 *             Logger.e(TAG, "Divide operation error", exception);
 *             doSomething();
 *         }
 *     }
 * </pre>
 *
 * @hide
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Logger {

    /** On API levels strictly below 24, the log tag's length must not exceed 23 characters. */
    private static final int MAX_TAG_LENGTH = 23;

    static final int DEFAULT_MIN_LOG_LEVEL = Log.DEBUG;
    private static int sMinLogLevel = DEFAULT_MIN_LOG_LEVEL;

    private Logger() {
    }

    /**
     * Sets the minimum logging level to use in {@link Logger}. After calling this method, only logs
     * at the level {@code logLevel} and above are output.
     */
    static void setMinLogLevel(@IntRange(from = Log.DEBUG, to = Log.ERROR) int logLevel) {
        sMinLogLevel = logLevel;
    }

    /**
     * Returns current minimum logging level.
     */
    static int getMinLogLevel() {
        return sMinLogLevel;
    }

    /**
     * Resets the minimum logging level to use in {@link Logger} to the default minimum logging
     * level. After calling this method, only logs at the default level and above are output.
     */
    static void resetMinLogLevel() {
        sMinLogLevel = DEFAULT_MIN_LOG_LEVEL;
    }

    /**
     * Returns {@code true} if logging with the tag {@code tag} is enabled at the {@link Log#DEBUG}
     * level. This is true when the minimum logging level is less than or equal to
     * {@link Log#DEBUG}, or if the log level of {@code tag} was explicitly set to
     * {@link Log#DEBUG} at least.
     */
    public static boolean isDebugEnabled(@NonNull String tag) {
        return sMinLogLevel <= Log.DEBUG || Log.isLoggable(truncateTag(tag), Log.DEBUG);
    }

    /**
     * Returns {@code true} if logging with the tag {@code tag} is enabled at the {@link Log#INFO}
     * level. This is true when the minimum logging level is less than or equal to
     * {@link Log#INFO}, or if the log level of {@code tag} was explicitly set to
     * {@link Log#INFO} at least.
     */
    public static boolean isInfoEnabled(@NonNull String tag) {
        return sMinLogLevel <= Log.INFO || Log.isLoggable(truncateTag(tag), Log.INFO);
    }

    /**
     * Returns {@code true} if logging with the tag {@code tag} is enabled at the {@link Log#WARN}
     * level. This is true when the minimum logging level is less than or equal to
     * {@link Log#WARN}, or if the log level of {@code tag} was explicitly set to
     * {@link Log#WARN} at least.
     */
    public static boolean isWarnEnabled(@NonNull String tag) {
        return sMinLogLevel <= Log.WARN || Log.isLoggable(truncateTag(tag), Log.WARN);
    }

    /**
     * Returns {@code true} if logging with the tag {@code tag} is enabled at the {@link Log#ERROR}
     * level. This is true when the minimum logging level is less than or equal to
     * {@link Log#ERROR}, or if the log level of {@code tag} was explicitly set to
     * {@link Log#ERROR} at least.
     */
    public static boolean isErrorEnabled(@NonNull String tag) {
        return sMinLogLevel <= Log.ERROR || Log.isLoggable(truncateTag(tag), Log.ERROR);
    }

    /**
     * Logs the given {@link Log#DEBUG} message if the tag is
     * {@linkplain #isDebugEnabled(String) loggable}.
     */
    public static void d(@NonNull String tag, @NonNull String message) {
        d(tag, message, null);
    }

    /**
     * Logs the given {@link Log#DEBUG} message and the exception's stacktrace if the tag is
     * {@linkplain #isDebugEnabled(String) loggable}.
     */
    public static void d(@NonNull String tag, @NonNull String message,
            @Nullable final Throwable throwable) {
        if (isDebugEnabled(tag)) {
            Log.d(truncateTag(tag), message, throwable);
        }
    }

    /**
     * Logs the given {@link Log#INFO} message if the tag is
     * {@linkplain #isInfoEnabled(String) loggable}.
     */
    public static void i(@NonNull String tag, @NonNull String message) {
        i(tag, message, null);
    }

    /**
     * Logs the given {@link Log#INFO} message and the exception's stacktrace if the tag is
     * {@linkplain #isInfoEnabled(String) loggable}.
     */
    public static void i(@NonNull String tag, @NonNull String message,
            @Nullable final Throwable throwable) {
        if (isInfoEnabled(tag)) {
            Log.i(truncateTag(tag), message, throwable);
        }
    }

    /**
     * Logs the given {@link Log#WARN} message if the tag is
     * {@linkplain #isWarnEnabled(String) loggable}.
     */
    public static void w(@NonNull String tag, @NonNull String message) {
        w(tag, message, null);
    }

    /**
     * Logs the given {@link Log#WARN} message and the exception's stacktrace if the tag is
     * {@linkplain #isWarnEnabled(String) loggable}.
     */
    public static void w(@NonNull String tag, @NonNull String message,
            @Nullable final Throwable throwable) {
        if (isWarnEnabled(tag)) {
            Log.w(truncateTag(tag), message, throwable);
        }
    }

    /**
     * Logs the given {@link Log#ERROR} message if the tag is
     * {@linkplain #isErrorEnabled(String) loggable}.
     */
    public static void e(@NonNull String tag, @NonNull String message) {
        e(tag, message, null);
    }

    /**
     * Logs the given {@link Log#ERROR} message and the exception's stacktrace if the tag is
     * {@linkplain #isErrorEnabled(String) loggable}.
     */
    public static void e(@NonNull String tag, @NonNull String message,
            @Nullable final Throwable throwable) {
        if (isErrorEnabled(tag)) {
            Log.e(truncateTag(tag), message, throwable);
        }
    }

    /**
     * Truncates the tag so it can be used to log.
     * <p>
     * On API 24, the tag length limit of 23 characters was removed.
     */
    @NonNull
    private static String truncateTag(@NonNull String tag) {
        if (MAX_TAG_LENGTH < tag.length() && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return tag.substring(0, MAX_TAG_LENGTH);
        }
        return tag;
    }
}
