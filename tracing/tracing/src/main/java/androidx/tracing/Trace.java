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

package androidx.tracing;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Writes trace events to the system trace buffer.  These trace events can be
 * collected and visualized using the Systrace tool.
 *
 * <p>This tracing mechanism is independent of the method tracing mechanism
 * offered by {@link android.os.Debug#startMethodTracing}.  In particular, it enables
 * tracing of events that occur across multiple processes.
 * <p>For information about using the Systrace tool, read <a
 * href="{@docRoot}studio/profile/systrace/">Overview of system tracing</a>.
 */
public final class Trace {

    static final String TAG = "Trace";

    private static long sTraceTagApp;
    private static Method sIsTagEnabledMethod;
    private static Method sAsyncTraceBeginMethod;
    private static Method sAsyncTraceEndMethod;
    private static Method sTraceCounterMethod;

    /**
     * Checks whether or not tracing is currently enabled. This is useful to avoid intermediate
     * string creation for trace sections that require formatting. It is not necessary
     * to guard all Trace method calls as they internally already check this. However it is
     * recommended to use this to prevent creating any temporary objects that would then be
     * passed to those methods to reduce runtime cost when tracing isn't enabled.
     *
     * @return true if tracing is currently enabled, false otherwise
     */
    @SuppressLint("NewApi")
    public static boolean isEnabled() {
        if (Build.VERSION.SDK_INT >= 29) {
            return TraceApi29Impl.isEnabled();
        }

        return isEnabledFallback();
    }

    /**
     * Writes a trace message to indicate that a given section of code has begun. This call must
     * be followed by a corresponding call to {@link #endSection()} on the same thread.
     *
     * <p class="note"> At this time the vertical bar character '|', newline character '\n', and
     * null character '\0' are used internally by the tracing mechanism.  If sectionName contains
     * these characters they will be replaced with a space character in the trace.
     *
     * @param label The name of the code section to appear in the trace.
     */
    public static void beginSection(@NonNull String label) {
        if (Build.VERSION.SDK_INT >= 18) {
            TraceApi18Impl.beginSection(label);
        }
    }

    /**
     * Writes a trace message to indicate that a given section of code has ended. This call must
     * be preceded by a corresponding call to {@link #beginSection(String)}. Calling this method
     * will mark the end of the most recently begun section of code, so care must be taken to
     * ensure that beginSection / endSection pairs are properly nested and called from the same
     * thread.
     */
    public static void endSection() {
        if (Build.VERSION.SDK_INT >= 18) {
            TraceApi18Impl.endSection();
        }
    }

    /**
     * Writes a trace message to indicate that a given section of code has
     * begun. Must be followed by a call to {@link #endAsyncSection(String, int)} with the same
     * methodName and cookie. Unlike {@link #beginSection(String)} and {@link #endSection()},
     * asynchronous events do not need to be nested. The name and cookie used to
     * begin an event must be used to end it.
     *
     * @param methodName The method name to appear in the trace.
     * @param cookie     Unique identifier for distinguishing simultaneous events
     */
    @SuppressLint("NewApi")
    public static void beginAsyncSection(@NonNull String methodName, int cookie) {
        try {
            if (sAsyncTraceBeginMethod == null) {
                TraceApi29Impl.beginAsyncSection(methodName, cookie);
                return;
            }
        } catch (NoSuchMethodError | NoClassDefFoundError ignore) {
        }
        beginAsyncSectionFallback(methodName, cookie);
    }

    /**
     * Writes a trace message to indicate that the current method has ended.
     * Must be called exactly once for each call to {@link #beginAsyncSection(String, int)}
     * using the same name and cookie.
     *
     * @param methodName The method name to appear in the trace.
     * @param cookie     Unique identifier for distinguishing simultaneous events
     */
    @SuppressLint("NewApi")
    public static void endAsyncSection(@NonNull String methodName, int cookie) {
        try {
            if (sAsyncTraceEndMethod == null) {
                TraceApi29Impl.endAsyncSection(methodName, cookie);
                return;
            }
        } catch (NoSuchMethodError | NoClassDefFoundError ignore) {
        }
        endAsyncSectionFallback(methodName, cookie);
    }

    /**
     * Writes trace message to indicate the value of a given counter.
     *
     * @param counterName  The counter name to appear in the trace.
     * @param counterValue The counter value.
     */
    @SuppressLint("NewApi")
    public static void setCounter(@NonNull String counterName, int counterValue) {
        try {
            if (sTraceCounterMethod == null) {
                TraceApi29Impl.setCounter(counterName, counterValue);
                return;
            }
        } catch (NoSuchMethodError | NoClassDefFoundError ignore) {
        }
        setCounterFallback(counterName, counterValue);
    }

    private static boolean isEnabledFallback() {
        if (Build.VERSION.SDK_INT >= 18) {
            try {
                if (sIsTagEnabledMethod == null) {
                    Field traceTagAppField = android.os.Trace.class.getField("TRACE_TAG_APP");
                    sTraceTagApp = traceTagAppField.getLong(null);
                    sIsTagEnabledMethod =
                            android.os.Trace.class.getMethod("isTagEnabled", long.class);
                }
                return (boolean) sIsTagEnabledMethod.invoke(null, sTraceTagApp);
            } catch (Exception exception) {
                handleException("isTagEnabled", exception);
            }
        }
        // Never enabled on < API 18
        return false;
    }

    private static void beginAsyncSectionFallback(@NonNull String methodName, int cookie) {
        if (Build.VERSION.SDK_INT >= 18) {
            try {
                if (sAsyncTraceBeginMethod == null) {
                    sAsyncTraceBeginMethod = android.os.Trace.class.getMethod(
                            "asyncTraceBegin",
                            long.class,
                            String.class, int.class
                    );
                }
                sAsyncTraceBeginMethod.invoke(null, sTraceTagApp, methodName, cookie);
            } catch (Exception exception) {
                handleException("asyncTraceBegin", exception);
            }
        }
    }

    private static void endAsyncSectionFallback(@NonNull String methodName, int cookie) {
        if (Build.VERSION.SDK_INT >= 18) {
            try {
                if (sAsyncTraceEndMethod == null) {
                    sAsyncTraceEndMethod = android.os.Trace.class.getMethod(
                            "asyncTraceEnd",
                            long.class,
                            String.class, int.class
                    );
                }
                sAsyncTraceEndMethod.invoke(null, sTraceTagApp, methodName, cookie);
            } catch (Exception exception) {
                handleException("asyncTraceEnd", exception);
            }
        }
    }

    private static void setCounterFallback(@NonNull String counterName, int counterValue) {
        if (Build.VERSION.SDK_INT >= 18) {
            try {
                if (sTraceCounterMethod == null) {
                    sTraceCounterMethod = android.os.Trace.class.getMethod(
                            "traceCounter",
                            long.class,
                            String.class,
                            int.class
                    );
                }
                sTraceCounterMethod.invoke(null, sTraceTagApp, counterName, counterValue);
            } catch (Exception exception) {
                handleException("traceCounter", exception);
            }
        }
    }

    private static void handleException(@NonNull String methodName, @NonNull Exception exception) {
        if (exception instanceof InvocationTargetException) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        }
        Log.v(TAG, "Unable to call " + methodName + " via reflection", exception);
    }

    private Trace() {
    }
}
