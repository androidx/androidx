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

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Writes trace events to the system trace buffer.
 *
 * <p>These trace events can be collected and visualized using the Android Studio System
 * Trace, Perfetto, and Systrace tools.
 *
 * <p>Tracing should generally be performed in a non-debuggable app for more accurate
 * measurements, representative of real user experience. In a non-debuggable app, tracing is
 * {@link #isEnabled() enabled} if a trace is currently being captured, as well as one of the
 * following:
 * <ul>
 *   <li>Android 12 (API 31) or greater: On by default, unless
 *     <pre>&lt;profileable enabled=false/&gt;</pre>
 *     or <pre>&lt;profileable shell=false/&gt;</pre> is set in the manifest.</li>
 *   <li>Android 10 or 11 (API 29 or 30): <pre>&lt;profileable shell=true/&gt;</pre> is set in the
 *     manifest, or {@link #forceEnableAppTracing()} has been called</li>
 *   <li>JellyBean through Android 11 (API 18 through API 28): {@link #forceEnableAppTracing()} has
 *     been called</li>
 * </ul>
 *
 * <p>This tracing mechanism is independent of the method tracing mechanism offered by
 * {@link android.os.Debug#startMethodTracing}.  In particular, it enables tracing of events that
 * occur across multiple processes.
 *
 * <p>For information see
 * <a href="{@docRoot}studio/profile/systrace/">Overview of system tracing</a>.
 */
public final class Trace {
    static final String TAG = "Trace";
    static final int MAX_TRACE_LABEL_LENGTH = 127;

    private static long sTraceTagApp;
    private static Method sIsTagEnabledMethod;
    private static Method sAsyncTraceBeginMethod;
    private static Method sAsyncTraceEndMethod;
    private static Method sTraceCounterMethod;
    private static boolean sHasAppTracingEnabled;

    /**
     * Checks whether or not tracing is currently enabled.
     *
     * <p>This is useful to avoid intermediate string creation for trace sections that require
     * formatting. It is not necessary to guard all Trace method calls as they internally already
     * check this. However it is recommended to use this to prevent creating any temporary
     * objects that would then be passed to those methods to reduce runtime cost when tracing
     * isn't enabled.
     *
     * @return true if tracing is currently enabled, false otherwise
     */
    public static boolean isEnabled() {
        if (Build.VERSION.SDK_INT >= 29) {
            return TraceApi29Impl.isEnabled();
        }
        return isEnabledFallback();
    }

    /**
     * Enables the app tracing tag in a non-debuggable process.
     *
     * Beginning in Android 12 (API 31), app tracing - custom tracing performed by app code via
     * this class or android.os.Trace - is always enabled in all apps. Prior to this, app tracing
     * was only enabled in debuggable apps (as well as profileable apps, on API 29/30).
     *
     * Calling this method enables the app to record custom trace content without debuggable=true
     * on any platform version that supports tracing. Tracing of non-debuggable apps is highly
     * recommended, to ensure accurate performance measurements.
     *
     * As app tracing is always enabled on Android 12 (API 31) and above, this does nothing after
     * API 31.
     */
    public static void forceEnableAppTracing() {
        if (Build.VERSION.SDK_INT >= 18 && Build.VERSION.SDK_INT < 31) {
            try {
                if (!sHasAppTracingEnabled) {
                    sHasAppTracingEnabled = true; // only attempt once
                    @SuppressWarnings("JavaReflectionMemberAccess")
                    Method setAppTracingAllowed = android.os.Trace.class.getMethod(
                            "setAppTracingAllowed",
                            boolean.class
                    );
                    setAppTracingAllowed.invoke(null, true);
                }
            } catch (Exception exception) {
                handleException("setAppTracingAllowed", exception);
            }
        }
    }

    /**
     * Writes a trace message to indicate that a given section of code has begun.
     *
     * <p>This call must be followed by a corresponding call to {@link #endSection()} on the same
     * thread.
     *
     * <p class="note"> At this time the vertical bar character '|', newline character '\n', and
     * null character '\0' are used internally by the tracing mechanism.  If sectionName contains
     * these characters they will be replaced with a space character in the trace.
     *
     * @param label The name of the code section to appear in the trace.
     */
    public static void beginSection(@NonNull String label) {
        if (Build.VERSION.SDK_INT >= 18) {
            TraceApi18Impl.beginSection(truncatedTraceSectionLabel(label));
        }
    }

    /**
     * Writes a trace message to indicate that a given section of code has ended.
     *
     * <p>This call must be preceded by a corresponding call to {@link #beginSection(String)}.
     * Calling this method will mark the end of the most recently begun section of code, so care
     * must be taken to ensure that beginSection / endSection pairs are properly nested and
     * called from the same thread.
     */
    public static void endSection() {
        if (Build.VERSION.SDK_INT >= 18) {
            TraceApi18Impl.endSection();
        }
    }

    /**
     * Writes a trace message to indicate that a given section of code has begun.
     *
     * <p>Must be followed by a call to {@link #endAsyncSection(String, int)} with the same
     * methodName and cookie. Unlike {@link #beginSection(String)} and {@link #endSection()},
     * asynchronous events do not need to be nested. The name and cookie used to begin an event
     * must be used to end it.
     *
     * The cookie must be unique to any overlapping events. If events don't overlap, you can
     * simply always pass the same integer (e.g. `0`). If they do overlap, the cookie is used to
     * disambiguate between overlapping events, like the following scenario:
     * <pre>
     * [==========================]
     *           [=====================================]
     *                                      [====]
     * </pre>
     * Without unique cookies, these start/stop timestamps could be misinterpreted by the trace
     * display like the following, to show very different ranges:
     * <pre>
     * [=========================================]
     *           [================]
     *                                      [==========]
     * </pre>
     *
     * @param methodName The method name to appear in the trace.
     * @param cookie     Unique identifier for distinguishing simultaneous events with the same
     *                   methodName
     * @see #endAsyncSection
     */
    public static void beginAsyncSection(@NonNull String methodName, int cookie) {
        if (Build.VERSION.SDK_INT >= 29) {
            TraceApi29Impl.beginAsyncSection(truncatedTraceSectionLabel(methodName), cookie);
        } else {
            beginAsyncSectionFallback(truncatedTraceSectionLabel(methodName), cookie);
        }
    }

    /**
     * Writes a trace message to indicate that the current method has ended.
     *
     * <p>Must be called exactly once for each call to {@link #beginAsyncSection(String, int)}
     * using the same name and cookie.
     *
     * @param methodName The method name to appear in the trace.
     * @param cookie     Unique identifier for distinguishing simultaneous events with the same
     *                   methodName
     * @see #beginAsyncSection
     */
    public static void endAsyncSection(@NonNull String methodName, int cookie) {
        if (Build.VERSION.SDK_INT >= 29) {
            TraceApi29Impl.endAsyncSection(truncatedTraceSectionLabel(methodName), cookie);
        } else {
            endAsyncSectionFallback(truncatedTraceSectionLabel(methodName), cookie);
        }
    }

    /**
     * Writes trace message to indicate the value of a given counter.
     *
     * @param counterName  The counter name to appear in the trace.
     * @param counterValue The counter value.
     */
    public static void setCounter(@NonNull String counterName, int counterValue) {
        if (Build.VERSION.SDK_INT >= 29) {
            TraceApi29Impl.setCounter(truncatedTraceSectionLabel(counterName), counterValue);
        } else {
            setCounterFallback(truncatedTraceSectionLabel(counterName), counterValue);
        }
    }

    @SuppressWarnings({"JavaReflectionMemberAccess", "ConstantConditions"})
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

    @SuppressWarnings("JavaReflectionMemberAccess")
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

    @SuppressWarnings("JavaReflectionMemberAccess")
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

    @SuppressWarnings("JavaReflectionMemberAccess")
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

    @NonNull
    private static String truncatedTraceSectionLabel(@NonNull String labelName) {
        if (labelName.length() <= MAX_TRACE_LABEL_LENGTH) {
            return labelName;
        }
        return labelName.substring(0, MAX_TRACE_LABEL_LENGTH);
    }

    private Trace() {
    }
}
