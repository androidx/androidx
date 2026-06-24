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

package androidx.xr.runtime

import android.util.Log
import androidx.annotation.RestrictTo

/** Logging utilities for JetpackXR messages and Logcat compile-time print controls. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object XrLog {

    /** Describes the priority of a log message. */
    public class Level private constructor(internal val value: Int) : Comparable<Level> {
        public companion object {
            /**
             * Verbose messages contain extra noisy information, often used for detailed tracing.
             */
            @JvmField public val VERBOSE: Level = Level(Log.VERBOSE)
            /** Debug messages log noisy information related to the internals of the system. */
            @JvmField public val DEBUG: Level = Level(Log.DEBUG)
            /** Info messages log standard, useful information about the state of the system. */
            @JvmField public val INFO: Level = Level(Log.INFO)
            /** Logs warning messages for unexpected events that may lead to subsequent failures. */
            @JvmField public val WARN: Level = Level(Log.WARN)
            /** Logs errors for unexpected events that cause failures or data loss. */
            @JvmField public val ERROR: Level = Level(Log.ERROR)
        }

        override fun compareTo(other: Level): Int {
            return value.compareTo(other.value)
        }
    }

    internal const val TAG: String = "JetpackXR"

    /** Whether or not messages meeting the current [level] will be printed to Android Logcat. */
    public var isEnabled: Boolean = false

    /** The minimum log level that will be printed if [isEnabled]. */
    public var level: Level = Level.DEBUG

    /**
     * Logs errors for unexpected events that cause failures or data loss.
     *
     * @param throwable an optional exception for Android to log in addition to your message
     * @param message the message you want logged
     */
    public fun error(throwable: Throwable? = null, message: () -> String) {
        if (isEnabled && isLoggable(Level.ERROR)) {
            Log.e(TAG, message(), throwable)
        }
    }

    /**
     * Logs errors for unexpected events that cause failures or data loss.
     *
     * @param message the message you want logged
     */
    public fun error(message: String): Unit = error { message }

    /**
     * Logs warning messages for unexpected events that may lead to subsequent failures.
     *
     * @param throwable an optional exception for Android to log in addition to your message
     * @param message the message you want logged
     */
    public fun warn(throwable: Throwable? = null, message: () -> String) {
        if (isEnabled && isLoggable(Level.WARN)) {
            Log.w(TAG, message(), throwable)
        }
    }

    /**
     * Logs warning messages for unexpected events that may lead to subsequent failures.
     *
     * @param message the message you want logged
     */
    public fun warn(message: String): Unit = warn { message }

    /**
     * Info messages log standard, useful information about the state of the system.
     *
     * @param throwable an optional exception for Android to log in addition to your message
     * @param message the message you want logged
     */
    public fun info(throwable: Throwable? = null, message: () -> String) {
        if (isEnabled && isLoggable(Level.INFO)) {
            Log.i(TAG, message(), throwable)
        }
    }

    /**
     * Info messages log standard, useful information about the state of the system.
     *
     * @param message the message you want logged
     */
    public fun info(message: String): Unit = info { message }

    /**
     * Debug functions log noisy information related to the internals of the system.
     *
     * @param throwable an optional exception for Android to log in addition to your message
     * @param message the message you want logged
     */
    public fun debug(throwable: Throwable? = null, message: () -> String) {
        if (isEnabled && isLoggable(Level.DEBUG)) {
            Log.d(TAG, message(), throwable)
        }
    }

    /**
     * Debug functions log noisy information related to the internals of the system.
     *
     * @param message the message you want logged
     */
    public fun debug(message: String): Unit = debug { message }

    /**
     * Verbose messages contain extra noisy information, often used for detailed tracing.
     *
     * @param throwable an optional exception for Android to log in addition to your message
     * @param message the message you want logged
     */
    public fun verbose(throwable: Throwable? = null, message: () -> String) {
        if (isEnabled && isLoggable(Level.VERBOSE)) {
            Log.v(TAG, message(), throwable)
        }
    }

    /**
     * Verbose messages contain extra noisy information, often used for detailed tracing.
     *
     * @param message the message you want logged
     */
    public fun verbose(message: String): Unit = verbose { message }

    private fun isLoggable(logLevel: Level): Boolean = logLevel >= level
}
