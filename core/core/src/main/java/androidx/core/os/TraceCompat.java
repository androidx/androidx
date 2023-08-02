/*
 * Copyright (C) 2015 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.os;

import android.os.Build;
import android.os.Trace;
import android.util.Log;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Field;
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
 *
 * @deprecated TraceCompat is deprecated in favor of androidx.tracing.Trace. Please use that
 * instead.
 */
@SuppressWarnings("JavaReflectionMemberAccess")
@Deprecated
public final class TraceCompat {

    private static final String TAG = "TraceCompat";

    private static long sTraceTagApp;
    private static Method sIsTagEnabledMethod;
    private static Method sAsyncTraceBeginMethod;
    private static Method sAsyncTraceEndMethod;
    private static Method sTraceCounterMethod;

    static {
        if (Build.VERSION.SDK_INT >= 18 && Build.VERSION.SDK_INT < 29) {
            try {
                Field traceTagAppField = Trace.class.getField("TRACE_TAG_APP");
                sTraceTagApp = traceTagAppField.getLong(null);

                sIsTagEnabledMethod = Trace.class.getMethod("isTagEnabled", long.class);
                sAsyncTraceBeginMethod = Trace.class.getMethod("asyncTraceBegin", long.class,
                        String.class, int.class);
                sAsyncTraceEndMethod = Trace.class.getMethod("asyncTraceEnd", long.class,
                        String.class, int.class);
                sTraceCounterMethod = Trace.class.getMethod("traceCounter", long.class,
                        String.class, int.class);
            } catch (Exception e) {
                Log.i(TAG, "Unable to initialize via reflection.", e);
            }
        }
    }

    /**
     * Checks whether or not tracing is currently enabled. This is useful to avoid intermediate
     * string creation for trace sections that require formatting. It is not necessary
     * to guard all Trace method calls as they internally already check this. However it is
     * recommended to use this to prevent creating any temporary objects that would then be
     * passed to those methods to reduce runtime cost when tracing isn't enabled.
     *
     * @return true if tracing is currently enabled, false otherwise
     */
    @SuppressWarnings("ConstantConditions")
    public static boolean isEnabled() {
        if (Build.VERSION.SDK_INT >= 29) {
            return Api29Impl.isEnabled();
        } else if (Build.VERSION.SDK_INT >= 18) {
            try {
                return (boolean) sIsTagEnabledMethod.invoke(null, sTraceTagApp);
            } catch (Exception e) {
                Log.v(TAG, "Unable to invoke isTagEnabled() via reflection.");
            }
        }

        // Never enabled on < API 18
        return false;
    }

    /**
     * Writes a trace message to indicate that a given section of code has begun. This call must
     * be followed by a corresponding call to {@link #endSection()} on the same thread.
     *
     * <p class="note"> At this time the vertical bar character '|', newline character '\n', and
     * null character '\0' are used internally by the tracing mechanism.  If sectionName contains
     * these characters they will be replaced with a space character in the trace.
     *
     * @param sectionName The name of the code section to appear in the trace.  This may be at
     * most 127 Unicode code units long.
     */
    public static void beginSection(@NonNull String sectionName) {
        if (Build.VERSION.SDK_INT >= 18) {
            Api18Impl.beginSection(sectionName);
        }
    }

    /**
     * Writes a trace message to indicate that a given section of code has ended. This call must
     * be preceeded by a corresponding call to {@link #beginSection(String)}. Calling this method
     * will mark the end of the most recently begun section of code, so care must be taken to
     * ensure that beginSection / endSection pairs are properly nested and called from the same
     * thread.
     */
    public static void endSection() {
        if (Build.VERSION.SDK_INT >= 18) {
            Api18Impl.endSection();
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
    public static void beginAsyncSection(@NonNull String methodName, int cookie) {
        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.beginAsyncSection(methodName, cookie);
        } else if (Build.VERSION.SDK_INT >= 18) {
            try {
                sAsyncTraceBeginMethod.invoke(null, sTraceTagApp, methodName, cookie);
            } catch (Exception e) {
                Log.v(TAG, "Unable to invoke asyncTraceBegin() via reflection.");
            }
        }
    }

    /**
     * Writes a trace message to indicate that the current method has ended.
     * Must be called exactly once for each call to {@link #beginAsyncSection(String, int)}
     * using the same name and cookie.
     *
     * @param methodName The method name to appear in the trace.
     * @param cookie     Unique identifier for distinguishing simultaneous events
     */
    public static void endAsyncSection(@NonNull String methodName, int cookie) {
        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.endAsyncSection(methodName, cookie);
        } else if (Build.VERSION.SDK_INT >= 18) {
            try {
                sAsyncTraceEndMethod.invoke(null, sTraceTagApp, methodName, cookie);
            } catch (Exception e) {
                Log.v(TAG, "Unable to invoke endAsyncSection() via reflection.");
            }
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
            Api29Impl.setCounter(counterName, counterValue);
        } else if (Build.VERSION.SDK_INT >= 18) {
            try {
                sTraceCounterMethod.invoke(null, sTraceTagApp, counterName, counterValue);
            } catch (Exception e) {
                Log.v(TAG, "Unable to invoke traceCounter() via reflection.");
            }
        }
    }

    private TraceCompat() {
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean isEnabled() {
            return Trace.isEnabled();
        }

        @DoNotInline
        static void endAsyncSection(String methodName, int cookie) {
            Trace.endAsyncSection(methodName, cookie);
        }

        @DoNotInline
        static void beginAsyncSection(String methodName, int cookie) {
            Trace.beginAsyncSection(methodName, cookie);
        }

        @DoNotInline
        static void setCounter(String counterName, long counterValue) {
            Trace.setCounter(counterName, counterValue);
        }
    }

    @RequiresApi(18)
    static class Api18Impl {
        private Api18Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void beginSection(String sectionName) {
            Trace.beginSection(sectionName);
        }

        @DoNotInline
        static void endSection() {
            Trace.endSection();
        }
    }
}
