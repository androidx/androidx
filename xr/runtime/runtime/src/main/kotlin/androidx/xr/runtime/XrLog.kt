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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Deprecated("Use XrLog instead.", ReplaceWith("XrLog"))
public object Log {
    public class Level private constructor(internal val value: Int) : Comparable<Level> {
        public companion object {
            @JvmField public val VERBOSE: Level = Level(XrLog.Level.VERBOSE.value)
            @JvmField public val DEBUG: Level = Level(XrLog.Level.DEBUG.value)
            @JvmField public val INFO: Level = Level(XrLog.Level.INFO.value)
            @JvmField public val WARN: Level = Level(XrLog.Level.WARN.value)
            @JvmField public val ERROR: Level = Level(XrLog.Level.ERROR.value)
        }

        override fun compareTo(other: Level): Int {
            return value.compareTo(other.value)
        }
    }

    public var enabled: Boolean
        get() = XrLog.isEnabled
        set(value) {
            XrLog.isEnabled = value
        }

    public var level: Level = Level.DEBUG
        get() =
            when (XrLog.level) {
                XrLog.Level.VERBOSE -> Level.VERBOSE
                XrLog.Level.DEBUG -> Level.DEBUG
                XrLog.Level.INFO -> Level.INFO
                XrLog.Level.WARN -> Level.WARN
                XrLog.Level.ERROR -> Level.ERROR
                else -> throw IllegalStateException()
            }
        set(value) {
            field = value
            XrLog.level =
                when (value) {
                    Level.VERBOSE -> XrLog.Level.VERBOSE
                    Level.DEBUG -> XrLog.Level.DEBUG
                    Level.INFO -> XrLog.Level.INFO
                    Level.WARN -> XrLog.Level.WARN
                    Level.ERROR -> XrLog.Level.ERROR
                    else -> throw IllegalArgumentException()
                }
        }

    public fun error(throwable: Throwable? = null, message: () -> String): Unit =
        XrLog.error(throwable, message)

    public fun error(message: String): Unit = XrLog.error(message)

    public fun warn(throwable: Throwable? = null, message: () -> String): Unit =
        XrLog.warn(throwable, message)

    public fun warn(message: String): Unit = XrLog.warn(message)

    public fun info(throwable: Throwable? = null, message: () -> String): Unit =
        XrLog.info(throwable, message)

    public fun info(message: String): Unit = XrLog.info(message)

    public fun debug(throwable: Throwable? = null, message: () -> String): Unit =
        XrLog.debug(throwable, message)

    public fun debug(message: String): Unit = XrLog.debug(message)

    public fun verbose(throwable: Throwable? = null, message: () -> String): Unit =
        XrLog.verbose(throwable, message)

    public fun verbose(message: String): Unit = XrLog.verbose(message)
}

/**
 * Provides a set of common functions for logging JetpackXR messages, as well as controlling at
 * compile time which messages will actually be printed to Android Logcat.
 */
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
            /**
             * Warning logs are used when something unexpected may lead to a crash or fatal
             * exception later on as a result of the unusual circumstances.
             */
            @JvmField public val WARN: Level = Level(Log.WARN)
            /**
             * Error logs are reserved for something unexpected that will lead to a crash or data
             * loss.
             */
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
     * Error logs are reserved for something unexpected that will lead to a crash or data loss.
     *
     * @param throwable An optional exception for Android to log in addition to your message.
     * @param message The message you want logged.
     */
    @JvmOverloads
    public fun error(throwable: Throwable? = null, message: () -> String) {
        if (isEnabled && isLoggable(Level.ERROR)) {
            Log.e(TAG, message(), throwable)
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
    @JvmOverloads
    public fun warn(throwable: Throwable? = null, message: () -> String) {
        if (isEnabled && isLoggable(Level.WARN)) {
            Log.w(TAG, message(), throwable)
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
    @JvmOverloads
    public fun info(throwable: Throwable? = null, message: () -> String) {
        if (isEnabled && isLoggable(Level.INFO)) {
            Log.i(TAG, message(), throwable)
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
    @JvmOverloads
    public fun debug(throwable: Throwable? = null, message: () -> String) {
        if (isEnabled && isLoggable(Level.DEBUG)) {
            Log.d(TAG, message(), throwable)
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
    @JvmOverloads
    public fun verbose(throwable: Throwable? = null, message: () -> String) {
        if (isEnabled && isLoggable(Level.VERBOSE)) {
            Log.v(TAG, message(), throwable)
        }
    }

    /**
     * Verbose messages contain extra noisy information, often used for detailed tracing.
     *
     * @param message The message you want logged.
     */
    public fun verbose(message: String): Unit = verbose { message }

    private fun isLoggable(level: Level): Boolean = level >= XrLog.level
}
