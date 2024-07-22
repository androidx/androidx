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
package androidx.tracing

import android.os.Build
import android.os.Trace
import android.util.Log
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Writes trace events to the system trace buffer.
 *
 * These trace events can be collected and visualized using the Android Studio System Trace,
 * Perfetto, and Systrace tools.
 *
 * Tracing should generally be performed in a non-debuggable app for more accurate measurements,
 * representative of real user experience. In a non-debuggable app, tracing is [enabled][.isEnabled]
 * if a trace is currently being captured, as well as one of the following:
 * * Android 12 (API 31) or greater: On by default, unless
 * <pre>&lt;profileable enabled=false/&gt;</pre>
 *
 * or <pre>&lt;profileable shell=false/&gt;</pre> is set in the manifest.
 * * Android 10 or 11 (API 29 or 30): <pre>&lt;profileable shell=true/&gt;</pre> is set in the
 *   manifest, or [.forceEnableAppTracing] has been called
 * * JellyBean through Android 11 (API 18 through API 28): [.forceEnableAppTracing] has been called
 *
 * This tracing mechanism is independent of the method tracing mechanism offered by
 * [android.os.Debug.startMethodTracing]. In particular, it enables tracing of events that occur
 * across multiple processes.
 *
 * For information see [Overview of system tracing]({@docRoot}studio/profile/systrace/).
 */
object Trace {
    private const val TAG: String = "Trace"
    internal const val MAX_TRACE_LABEL_LENGTH: Int = 127

    private var traceTagApp = 0L
    private var isTagEnabledMethod: Method? = null
    private var asyncTraceBeginMethod: Method? = null
    private var asyncTraceEndMethod: Method? = null
    private var traceCounterMethod: Method? = null
    private var hasAppTracingEnabled = false

    /**
     * Checks whether or not tracing is currently enabled.
     *
     * This is useful to avoid intermediate string creation for trace sections that require
     * formatting. It is not necessary to guard all Trace method calls as they internally already
     * check this. However it is recommended to use this to prevent creating any temporary objects
     * that would then be passed to those methods to reduce runtime cost when tracing isn't enabled.
     *
     * @return `true` if tracing is currently enabled, `false` otherwise.
     */
    @JvmStatic // A function (not a property) for source compatibility with Kotlin callers
    fun isEnabled(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            TraceApi29Impl.isEnabled
        } else {
            isEnabledFallback
        }

    /**
     * Enables the app tracing tag in a non-debuggable process.
     *
     * Beginning in Android 12 (API 31), app tracing - custom tracing performed by app code via this
     * class or android.os.Trace - is always enabled in all apps. Prior to this, app tracing was
     * only enabled in debuggable apps (as well as profileable apps, on API 29/30).
     *
     * Calling this method enables the app to record custom trace content without debuggable=true on
     * any platform version that supports tracing. Tracing of non-debuggable apps is highly
     * recommended, to ensure accurate performance measurements.
     *
     * As app tracing is always enabled on Android 12 (API 31) and above, this does nothing after
     * API 31.
     */
    @JvmStatic
    fun forceEnableAppTracing() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            try {
                if (!hasAppTracingEnabled) {
                    hasAppTracingEnabled = true // only attempt once
                    val setAppTracingAllowed =
                        Trace::class
                            .java
                            .getMethod(
                                "setAppTracingAllowed",
                                Boolean::class.javaPrimitiveType,
                            )
                    setAppTracingAllowed.invoke(null, true)
                }
            } catch (exception: Exception) {
                handleException("setAppTracingAllowed", exception)
            }
        }
    }

    /**
     * Writes a trace message to indicate that a given section of code has begun.
     *
     * This call must be followed by a corresponding call to [endSection] on the same thread.
     *
     * At this time the vertical bar character '|', newline character '\n', and null character '\0'
     * are used internally by the tracing mechanism. If sectionName contains these characters they
     * will be replaced with a space character in the trace.
     *
     * @param label The name of the code section to appear in the trace.
     */
    @JvmStatic
    fun beginSection(label: String) {
        Trace.beginSection(label.truncatedTraceSectionLabel())
    }

    /**
     * Writes a trace message to indicate that a given section of code has ended.
     *
     * This call must be preceded by a corresponding call to [beginSection]. Calling this method
     * will mark the end of the most recently begun section of code, so care must be taken to ensure
     * that beginSection / endSection pairs are properly nested and called from the same thread.
     */
    @JvmStatic
    fun endSection() {
        Trace.endSection()
    }

    /**
     * Writes a trace message to indicate that a given section of code has begun.
     *
     * Must be followed by a call to [endAsyncSection] with the same methodName and cookie. Unlike
     * [beginSection] and [endSection], asynchronous events do not need to be nested. The name and
     * cookie used to begin an event must be used to end it.
     *
     * The cookie must be unique to any overlapping events. If events don't overlap, you can simply
     * always pass the same integer (e.g. `0`). If they do overlap, the cookie is used to
     * disambiguate between overlapping events, like the following scenario:
     * ```
     * [==========================]
     *           [=====================================]
     *                                      [====]
     * ```
     *
     * Without unique cookies, these start/stop timestamps could be misinterpreted by the trace
     * display like the following, to show very different ranges:
     * ```
     * [=========================================]
     *           [================]
     *                                      [==========]
     * ```
     *
     * @param methodName The method name to appear in the trace.
     * @param cookie Unique identifier for distinguishing simultaneous events with the same
     *   methodName.
     * @see endAsyncSection
     */
    @JvmStatic
    fun beginAsyncSection(methodName: String, cookie: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            TraceApi29Impl.beginAsyncSection(methodName.truncatedTraceSectionLabel(), cookie)
        } else {
            beginAsyncSectionFallback(methodName.truncatedTraceSectionLabel(), cookie)
        }
    }

    /**
     * Writes a trace message to indicate that the current method has ended.
     *
     * Must be called exactly once for each call to [beginAsyncSection] using the same name and
     * cookie.
     *
     * @param methodName The method name to appear in the trace.
     * @param cookie Unique identifier for distinguishing simultaneous events with the same
     *   methodName.
     * @see beginAsyncSection
     */
    @JvmStatic
    fun endAsyncSection(methodName: String, cookie: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            TraceApi29Impl.endAsyncSection(methodName.truncatedTraceSectionLabel(), cookie)
        } else {
            endAsyncSectionFallback(methodName.truncatedTraceSectionLabel(), cookie)
        }
    }

    /**
     * Writes trace message to indicate the value of a given counter.
     *
     * @param counterName The counter name to appear in the trace.
     * @param counterValue The counter value.
     */
    @JvmStatic
    fun setCounter(counterName: String, counterValue: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            TraceApi29Impl.setCounter(counterName.truncatedTraceSectionLabel(), counterValue)
        } else {
            setCounterFallback(counterName.truncatedTraceSectionLabel(), counterValue)
        }
    }

    @Suppress("JavaReflectionMemberAccess", "BanUncheckedReflection")
    private val isEnabledFallback: Boolean
        get() {
            try {
                if (isTagEnabledMethod == null) {
                    val traceTagAppField = Trace::class.java.getField("TRACE_TAG_APP")
                    traceTagApp = traceTagAppField.getLong(null)
                    isTagEnabledMethod =
                        Trace::class.java.getMethod("isTagEnabled", Long::class.javaPrimitiveType)
                }
                return requireNotNull(isTagEnabledMethod).invoke(null, traceTagApp) as Boolean
            } catch (exception: Exception) {
                handleException("isTagEnabled", exception)
            }
            // Never enabled on < API 18
            return false
        }

    @Suppress("JavaReflectionMemberAccess", "BanUncheckedReflection")
    private fun beginAsyncSectionFallback(methodName: String, cookie: Int) {
        try {
            if (asyncTraceBeginMethod == null) {
                asyncTraceBeginMethod =
                    Trace::class
                        .java
                        .getMethod(
                            "asyncTraceBegin",
                            Long::class.javaPrimitiveType,
                            String::class.java,
                            Int::class.javaPrimitiveType,
                        )
            }
            requireNotNull(asyncTraceBeginMethod).invoke(null, traceTagApp, methodName, cookie)
        } catch (exception: Exception) {
            handleException("asyncTraceBegin", exception)
        }
    }

    @Suppress("JavaReflectionMemberAccess", "BanUncheckedReflection")
    private fun endAsyncSectionFallback(methodName: String, cookie: Int) {
        try {
            if (asyncTraceEndMethod == null) {
                asyncTraceEndMethod =
                    Trace::class
                        .java
                        .getMethod(
                            "asyncTraceEnd",
                            Long::class.javaPrimitiveType,
                            String::class.java,
                            Int::class.javaPrimitiveType,
                        )
            }
            requireNotNull(asyncTraceEndMethod).invoke(null, traceTagApp, methodName, cookie)
        } catch (exception: Exception) {
            handleException("asyncTraceEnd", exception)
        }
    }

    @Suppress("JavaReflectionMemberAccess", "BanUncheckedReflection")
    private fun setCounterFallback(counterName: String, counterValue: Int) {
        try {
            if (traceCounterMethod == null) {
                traceCounterMethod =
                    Trace::class
                        .java
                        .getMethod(
                            "traceCounter",
                            Long::class.javaPrimitiveType,
                            String::class.java,
                            Int::class.javaPrimitiveType,
                        )
            }
            requireNotNull(traceCounterMethod).invoke(null, traceTagApp, counterName, counterValue)
        } catch (exception: Exception) {
            handleException("traceCounter", exception)
        }
    }

    private fun handleException(methodName: String, exception: Exception) {
        if (exception is InvocationTargetException) {
            val cause = exception.cause
            if (cause is RuntimeException) {
                throw cause
            } else {
                throw RuntimeException(cause)
            }
        }
        Log.v(TAG, "Unable to call $methodName via reflection", exception)
    }

    private fun String.truncatedTraceSectionLabel(): String =
        takeIf { it.length <= MAX_TRACE_LABEL_LENGTH } ?: substring(0, MAX_TRACE_LABEL_LENGTH)
}
