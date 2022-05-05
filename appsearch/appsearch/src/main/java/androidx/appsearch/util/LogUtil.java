/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.util;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.Size;

/**
 * Utilities for logging to logcat.
 *
 * <p>Logcat logging through this class obeys the most permissive setting between the priority
 * setting for the individual log tag provided with the message, as well as the global tag
 * {@code "AppSearch"}. This means logging can be enabled via adb either by enabling the specific
 * tag of the class of interest, or by enabling the global tag {@code "AppSearch"}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class LogUtil {
    /** A special log tag that enables all other logging if it is enabled. */
    private static final String APP_SEARCH_GLOBAL_TAG = "AppSearch";

    /**
     * The {@link #piiTrace} logs are intended for sensitive data that can't be enabled in
     * production, so they are build-gated by this constant.
     *
     * <p><ul>
     * <li>0: no tracing.
     * <li>1: fast tracing (statuses/counts only)
     * <li>2: full tracing (complete messages)
     * </ul>
     */
    private static final int PII_TRACE_LEVEL = 0;

    private LogUtil() {}

    /**
     * Checks to see whether or not a log for the specified tag is loggable at the specified level.
     *
     * <p>Logging can be enabled either via the specific tag, or for the global tag "AppSearch".
     * This method returns true if either of these tags is loggable.
     *
     * @param tag   The tag to check.
     * @param level The level to check.
     * @return Whether or not that this is allowed to be logged.
     * @throws IllegalArgumentException is thrown if the tag.length() > 23
     *                                  for Nougat (7.0) and prior releases (API <= 25), there is no
     *                                  tag limit of concern after this API level.
     * @see Log#isLoggable
     */
    public static boolean isLoggable(@Size(min = 0, max = 23) @NonNull String tag, int level) {
        return Log.isLoggable(tag, level) || Log.isLoggable(APP_SEARCH_GLOBAL_TAG, level);
    }

    /**
     * What a Terrible Failure: Report a condition that should never happen.
     *
     * <p>The error will always be logged at level ASSERT with the call stack.
     * Depending on system configuration, a report may be added to the
     * {@link android.os.DropBoxManager} and/or the process may be terminated
     * immediately with an error dialog.
     *
     * @param tag Used to identify the source of a log message.
     * @param msgFirst The first part of the message you would like logged.
     * @param msgRest The remaining segments of the message you would like logged. Each segment
     *                will be stringified and appended to the final message string in order.
     */
    public static void wtf(
            @Size(min = 0, max = 23) @NonNull String tag,
            @NonNull String msgFirst,
            @Nullable Object... msgRest) {
        wtf(tag, /*tr=*/null, msgFirst, msgRest);
    }

    /**
     * What a Terrible Failure: Report an exception that should never happen.
     *
     * <p>Similar to {@link #wtf(String, String, Object...)}, with an exception to log.
     *
     * @param tag Used to identify the source of a log message.
     * @param tr An exception to log.
     * @param msgFirst The first part of the message you would like logged.
     * @param msgRest The remaining segments of the message you would like logged. Each segment
     *                will be stringified and appended to the final message string in order.
     */
    public static void wtf(
            @Size(min = 0, max = 23) @NonNull String tag,
            @Nullable Throwable tr,
            @NonNull String msgFirst,
            @Nullable Object... msgRest) {
        log(Log.ASSERT, tag, tr, msgFirst, msgRest);
    }

    /**
     * Send a {@link Log#ERROR} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msgFirst The first part of the message you would like logged.
     * @param msgRest The remaining segments of the message you would like logged. Each segment
     *                will be stringified and appended to the final message string in order.
     */
    public static void e(
            @Size(min = 0, max = 23) @NonNull String tag,
            @NonNull String msgFirst,
            @Nullable Object... msgRest) {
        e(tag, /*tr=*/null, msgFirst, msgRest);
    }

    /**
     * Send a {@link Log#ERROR} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     * @param msgFirst The first part of the message you would like logged.
     * @param msgRest The remaining segments of the message you would like logged. Each segment
     *                will be stringified and appended to the final message string in order.
     */
    public static void e(
            @Size(min = 0, max = 23) @NonNull String tag,
            @Nullable Throwable tr,
            @NonNull String msgFirst,
            @Nullable Object... msgRest) {
        log(Log.ERROR, tag, tr, msgFirst, msgRest);
    }

    /**
     * Send a {@link Log#WARN} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msgFirst The first part of the message you would like logged.
     * @param msgRest The remaining segments of the message you would like logged. Each segment
     *                will be stringified and appended to the final message string in order.
     */
    public static void w(
            @Size(min = 0, max = 23) @NonNull String tag,
            @NonNull String msgFirst,
            @Nullable Object... msgRest) {
        w(tag, /*tr=*/null, msgFirst, msgRest);
    }

    /**
     * Send a {@link Log#WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     * @param msgFirst The first part of the message you would like logged.
     * @param msgRest The remaining segments of the message you would like logged. Each segment
     *                will be stringified and appended to the final message string in order.
     */
    public static void w(
            @Size(min = 0, max = 23) @NonNull String tag,
            @Nullable Throwable tr,
            @NonNull String msgFirst,
            @Nullable Object... msgRest) {
        log(Log.WARN, tag, tr, msgFirst, msgRest);
    }

    /**
     * Send a {@link Log#INFO} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msgFirst The first part of the message you would like logged.
     * @param msgRest The remaining segments of the message you would like logged. Each segment
     *                will be stringified and appended to the final message string in order.
     */
    public static void i(
            @Size(min = 0, max = 23) @NonNull String tag,
            @NonNull String msgFirst,
            @Nullable Object... msgRest) {
        i(tag, /*tr=*/null, msgFirst, msgRest);
    }

    /**
     * Send a {@link Log#INFO} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     * @param msgFirst The first part of the message you would like logged.
     * @param msgRest The remaining segments of the message you would like logged. Each segment
     *                will be stringified and appended to the final message string in order.
     */
    public static void i(
            @Size(min = 0, max = 23) @NonNull String tag,
            @Nullable Throwable tr,
            @NonNull String msgFirst,
            @Nullable Object... msgRest) {
        log(Log.INFO, tag, tr, msgFirst, msgRest);
    }

    /**
     * Send a {@link Log#DEBUG} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msgFirst The first part of the message you would like logged.
     * @param msgRest The remaining segments of the message you would like logged. Each segment
     *                will be stringified and appended to the final message string in order.
     */
    public static void d(
            @Size(min = 0, max = 23) @NonNull String tag,
            @NonNull String msgFirst,
            @Nullable Object... msgRest) {
        d(tag, /*tr=*/null, msgFirst, msgRest);
    }

    /**
     * Send a {@link Log#DEBUG} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     * @param msgFirst The first part of the message you would like logged.
     * @param msgRest The remaining segments of the message you would like logged. Each segment
     *                will be stringified and appended to the final message string in order.
     */
    public static void d(
            @Size(min = 0, max = 23) @NonNull String tag,
            @Nullable Throwable tr,
            @NonNull String msgFirst,
            @Nullable Object... msgRest) {
        log(Log.DEBUG, tag, tr, msgFirst, msgRest);
    }

    /**
     * Send a {@link Log#VERBOSE} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msgFirst The first part of the message you would like logged.
     * @param msgRest The remaining segments of the message you would like logged. Each segment
     *                will be stringified and appended to the final message string in order.
     */
    public static void v(
            @Size(min = 0, max = 23) @NonNull String tag,
            @NonNull String msgFirst,
            @Nullable Object... msgRest) {
        v(tag, /*tr=*/null, msgFirst, msgRest);
    }

    /**
     * Send a {@link Log#VERBOSE} log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     * @param msgFirst The first part of the message you would like logged.
     * @param msgRest The remaining segments of the message you would like logged. Each segment
     *                will be stringified and appended to the final message string in order.
     */
    public static void v(
            @Size(min = 0, max = 23) @NonNull String tag,
            @Nullable Throwable tr,
            @NonNull String msgFirst,
            @Nullable Object... msgRest) {
        log(Log.VERBOSE, tag, tr, msgFirst, msgRest);
    }

    /**
     * Generic method to send a log message with optional formatting and optional exception.
     *
     * @param priority The priority at which to log the message (one of the constants in
     *                 {@link Log}).
     * @param tag      Used to identify the source of a log message.  It usually identifies the
     *                 class or activity where the log call occurs.
     * @param tr       An optional exception to log
     * @param msgFirst The first part of the message you would like logged.
     * @param msgRest  The remaining segments of the message you would like logged, if any. Each
     *                 segment will be stringified and appended to the final message string in
     *                 order.
     */
    private static void log(
            int priority,
            @Size(min = 0, max = 23) @NonNull String tag,
            @Nullable Throwable tr,
            @NonNull String msgFirst,
            @Nullable Object... msgRest) {
        if (!isLoggable(tag, priority)) {
            return;
        }
        String msg = formatMessage(msgFirst, msgRest);
        switch (priority) {
            case Log.ASSERT:
                Log.wtf(tag, msg, tr);
                break;
            case Log.ERROR:
                Log.e(tag, msg, tr);
                break;
            case Log.WARN:
                Log.w(tag, msg, tr);
                break;
            case Log.INFO:
                Log.i(tag, msg, tr);
                break;
            case Log.DEBUG:
                Log.d(tag, msg, tr);
                break;
            case Log.VERBOSE:
                Log.v(tag, msg, tr);
                break;
            default:
                Log.wtf(APP_SEARCH_GLOBAL_TAG, "Message logged at unknown priority: " + priority);
                Log.e(tag, msg, tr);
                break;
        }
    }

    /** Returns whether piiTrace() is enabled (PII_TRACE_LEVEL > 0). */
    public static boolean isPiiTraceEnabled() {
        return PII_TRACE_LEVEL > 0;
    }

    /**
     * If icing lib interaction tracing is enabled via {@link #PII_TRACE_LEVEL}, logs the provided
     * message to logcat.
     *
     * <p>If {@link #PII_TRACE_LEVEL} is 0, nothing is logged and this method returns immediately.
     */
    public static void piiTrace(
            @Size(min = 0, max = 23) @NonNull String tag, @NonNull String message) {
        piiTrace(tag, message, /*fastTraceObj=*/null, /*fullTraceObj=*/null);
    }

    /**
     * If icing lib interaction tracing is enabled via {@link #PII_TRACE_LEVEL}, logs the provided
     * message and object to logcat.
     *
     * <p>If {@link #PII_TRACE_LEVEL} is 0, nothing is logged and this method returns immediately.
     * <p>Otherwise, {@code traceObj} is logged if it is non-null.
     */
    public static void piiTrace(
            @Size(min = 0, max = 23) @NonNull String tag,
            @NonNull String message,
            @Nullable Object traceObj) {
        piiTrace(tag, message, /*fastTraceObj=*/traceObj, /*fullTraceObj=*/null);
    }

    /**
     * If icing lib interaction tracing is enabled via {@link #PII_TRACE_LEVEL}, logs the provided
     * message and objects to logcat.
     *
     * <p>If {@link #PII_TRACE_LEVEL} is 0, nothing is logged and this method returns immediately.
     * <p>If {@link #PII_TRACE_LEVEL} is 1, {@code fastTraceObj} is logged if it is non-null.
     * <p>If {@link #PII_TRACE_LEVEL} is 2, {@code fullTraceObj} is logged if it is non-null, else
     *   {@code fastTraceObj} is logged if it is non-null..
     */
    public static void piiTrace(
            @Size(min = 0, max = 23) @NonNull String tag,
            @NonNull String message,
            @Nullable Object fastTraceObj,
            @Nullable Object fullTraceObj) {
        if (PII_TRACE_LEVEL == 0) {
            return;
        }
        StringBuilder builder = new StringBuilder("(trace) ").append(message);
        if (PII_TRACE_LEVEL == 1 && fastTraceObj != null) {
            builder.append(": ").append(fastTraceObj);
        } else if (PII_TRACE_LEVEL == 2 && fullTraceObj != null) {
            builder.append(": ").append(fullTraceObj);
        } else if (PII_TRACE_LEVEL == 2 && fastTraceObj != null) {
            builder.append(": ").append(fastTraceObj);
        }
        Log.i(tag, builder.toString());
    }

    @NonNull
    private static String formatMessage(@NonNull String msgFirst, @Nullable Object[] msgRest) {
        if (msgRest == null || msgRest.length == 0) {
            return msgFirst;
        }
        StringBuilder stringBuilder = new StringBuilder(msgFirst);
        for (int i = 0; i < msgRest.length; i++) {
            stringBuilder.append(msgRest[i]);
        }
        return stringBuilder.toString();
    }
}
