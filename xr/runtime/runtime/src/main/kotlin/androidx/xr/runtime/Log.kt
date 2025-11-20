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

import android.util.Log as AndroidLog
import androidx.annotation.RestrictTo

/**
 * Provides a set of common functions for logging JetpackXR messages, as well as controlling at
 * compile time which messages will actually be printed to Android Logcat.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public object Log {

    /** Describes the priority of a log message. */
    public class Level private constructor(internal val value: Int) : Comparable<Level> {
        public companion object {
            /**
             * Verbose messages contain extra noisy information, often used for detailed tracing.
             */
            @JvmField public val VERBOSE: Level = Level(AndroidLog.VERBOSE)
            /** Debug messages log noisy information related to the internals of the system. */
            @JvmField public val DEBUG: Level = Level(AndroidLog.DEBUG)
            /** Info messages log standard, useful information about the state of the system. */
            @JvmField public val INFO: Level = Level(AndroidLog.INFO)
            /**
             * Warning logs are used when something unexpected may lead to a crash or fatal
             * exception later on as a result of the unusual circumstances.
             */
            @JvmField public val WARN: Level = Level(AndroidLog.WARN)
            /**
             * Error logs are reserved for something unexpected that will lead to a crash or data
             * loss.
             */
            @JvmField public val ERROR: Level = Level(AndroidLog.ERROR)
        }

        override fun compareTo(other: Level): Int {
            return value.compareTo(other.value)
        }
    }

    internal const val TAG: String = "JetpackXR"

    /** Whether or not messages meeting the current [level] will be printed to Android Logcat. */
    public var enabled: Boolean = false

    /** The minimum log level that will be printed if [enabled]. */
    public var level: Level = Level.DEBUG

    /**
     * Error logs are reserved for something unexpected that will lead to a crash or data loss.
     *
     * @param throwable An optional exception for Android to log in addition to your message.
     * @param message The message you want logged.
     */
    public fun error(throwable: Throwable? = null, message: () -> String) {
        if (enabled && isLoggable(Level.ERROR)) {
            AndroidLog.e(TAG, message(), throwable)
        }
    }

    /**
     * Error logs are reserved for something unexpected that will lead to a crash or data loss.
     *
     * @param message The message you want logged.
     */
    public fun error(message: String): Unit = error { message }

    /**
     * Warning logs are used when something unexpected may lead to a crash or fatal exception later
     * on as a result of the unusual circumstances.
     *
     * @param throwable An optional exception for Android to log in addition to your message.
     * @param message The message you want logged.
     */
    public fun warn(throwable: Throwable? = null, message: () -> String) {
        if (enabled && isLoggable(Level.WARN)) {
            AndroidLog.w(TAG, message(), throwable)
        }
    }

    /**
     * Warning logs are used when something unexpected may lead to a crash or fatal exception later
     * on as a result of the unusual circumstances.
     *
     * @param message The message you want logged.
     */
    public fun warn(message: String): Unit = warn { message }

    /**
     * Info messages log standard, useful information about the state of the system.
     *
     * @param throwable An optional exception for Android to log in addition to your message.
     * @param message The message you want logged.
     */
    public fun info(throwable: Throwable? = null, message: () -> String) {
        if (enabled && isLoggable(Level.INFO)) {
            AndroidLog.i(TAG, message(), throwable)
        }
    }

    /**
     * Info messages log standard, useful information about the state of the system.
     *
     * @param message The message you want logged.
     */
    public fun info(message: String): Unit = info { message }

    /**
     * Debug functions log noisy information related to the internals of the system.
     *
     * @param throwable An optional exception for Android to log in addition to your message.
     * @param message The message you want logged.
     */
    public fun debug(throwable: Throwable? = null, message: () -> String) {
        if (enabled && isLoggable(Level.DEBUG)) {
            AndroidLog.d(TAG, message(), throwable)
        }
    }

    /**
     * Debug functions log noisy information related to the internals of the system.
     *
     * @param message The message you want logged.
     */
    public fun debug(message: String): Unit = debug { message }

    /**
     * Verbose messages contain extra noisy information, often used for detailed tracing.
     *
     * @param throwable An optional exception for Android to log in addition to your message.
     * @param message The message you want logged.
     */
    public fun verbose(throwable: Throwable? = null, message: () -> String) {
        if (enabled && isLoggable(Level.VERBOSE)) {
            AndroidLog.v(TAG, message(), throwable)
        }
    }

    /**
     * Verbose messages contain extra noisy information, often used for detailed tracing.
     *
     * @param message The message you want logged.
     */
    public fun verbose(message: String): Unit = verbose { message }

    private fun isLoggable(level: Level): Boolean = level >= Log.level
}
