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
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class LogUtil {
    /** Whether to log {@link Log#VERBOSE} and {@link Log#DEBUG} logs. */
    // TODO(b/232285376): If it becomes possible to detect an eng build, turn this on by default
    //  for eng builds.
    public static final boolean DEBUG = false;

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
}
