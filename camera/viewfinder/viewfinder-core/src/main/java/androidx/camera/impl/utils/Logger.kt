/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.impl.utils

import android.os.Build
import android.util.Log

/**
 * Handles logging requests inside CameraX. Log messages are output only if:
 * - The minimum logging level allows for it. The minimum logging level is set via
 *   [.setMinLogLevel], which should typically be called during the process of configuring CameraX.
 * - The log tag is [loggable][Log.isLoggable]. This can be configured by setting the system
 *   property `setprop log.tag.TAG LEVEL`, where TAG is the log tag, and LEVEL is [Log.DEBUG],
 *   [Log.INFO], [Log.WARN] or [Log.ERROR].
 *
 * A typical usage of the Logger looks as follows:
 * <pre>
 * try {
 * int quotient = dividend / divisor;
 * } catch (ArithmeticException exception) {
 * Logger.e(TAG, "Divide operation error", exception);
 * }
 * </pre> *
 *
 * If an action has to be performed alongside logging, or if building the log message is costly,
 * perform a log level check before attempting to log.
 * <pre>
 * try {
 * int quotient = dividend / divisor;
 * } catch (ArithmeticException exception) {
 * if (Logger.isErrorEnabled(TAG)) {
 * Logger.e(TAG, "Divide operation error", exception);
 * doSomething();
 * }
 * }
 * </pre> *
 */
object Logger {
    /** On API levels strictly below 24, the log tag's length must not exceed 23 characters. */
    private const val MAX_TAG_LENGTH = 23
    private const val DEFAULT_MIN_LOG_LEVEL = Log.DEBUG

    /** Returns current minimum logging level. */
    /**
     * Sets the minimum logging level to use in [Logger]. After calling this method, only logs at
     * the level `logLevel` and above are output.
     */
    private var minLogLevel = DEFAULT_MIN_LOG_LEVEL

    /**
     * Returns `true` if logging with the truncated tag `truncatedTag` is enabled at the `logLevel`
     * level.
     */
    private fun isLogLevelEnabled(truncatedTag: String, logLevel: Int): Boolean {
        return minLogLevel <= logLevel || Log.isLoggable(truncatedTag, logLevel)
    }

    /**
     * Resets the minimum logging level to use in [Logger] to the default minimum logging level.
     * After calling this method, only logs at the default level and above are output.
     */
    fun resetMinLogLevel() {
        minLogLevel = DEFAULT_MIN_LOG_LEVEL
    }

    /**
     * Returns `true` if logging with the tag `tag` is enabled at the [Log.DEBUG] level. This is
     * true when the minimum logging level is less than or equal to [Log.DEBUG], or if the log level
     * of `tag` was explicitly set to [Log.DEBUG] at least.
     */
    fun isDebugEnabled(tag: String): Boolean {
        return isLogLevelEnabled(truncateTag(tag), Log.DEBUG)
    }

    /**
     * Returns `true` if logging with the tag `tag` is enabled at the [Log.INFO] level. This is true
     * when the minimum logging level is less than or equal to [Log.INFO], or if the log level of
     * `tag` was explicitly set to [Log.INFO] at least.
     */
    fun isInfoEnabled(tag: String): Boolean {
        return isLogLevelEnabled(truncateTag(tag), Log.INFO)
    }

    /**
     * Returns `true` if logging with the tag `tag` is enabled at the [Log.WARN] level. This is true
     * when the minimum logging level is less than or equal to [Log.WARN], or if the log level of
     * `tag` was explicitly set to [Log.WARN] at least.
     */
    fun isWarnEnabled(tag: String): Boolean {
        return isLogLevelEnabled(truncateTag(tag), Log.WARN)
    }

    /**
     * Returns `true` if logging with the tag `tag` is enabled at the [Log.ERROR] level. This is
     * true when the minimum logging level is less than or equal to [Log.ERROR], or if the log level
     * of `tag` was explicitly set to [Log.ERROR] at least.
     */
    fun isErrorEnabled(tag: String): Boolean {
        return isLogLevelEnabled(truncateTag(tag), Log.ERROR)
    }

    /** Logs the given [Log.DEBUG] message if the tag is [loggable][.isDebugEnabled]. */
    fun d(tag: String, message: String) {
        val truncatedTag = truncateTag(tag)
        if (isLogLevelEnabled(truncatedTag, Log.DEBUG)) {
            Log.d(truncatedTag, message)
        }
    }

    /**
     * Logs the given [Log.DEBUG] message and the exception's stacktrace if the tag is
     * [loggable][.isDebugEnabled].
     */
    fun d(tag: String, message: String, throwable: Throwable) {
        val truncatedTag = truncateTag(tag)
        if (isLogLevelEnabled(truncatedTag, Log.DEBUG)) {
            Log.d(truncatedTag, message, throwable)
        }
    }

    /** Logs the given [Log.INFO] message if the tag is [loggable][.isInfoEnabled]. */
    fun i(tag: String, message: String) {
        val truncatedTag = truncateTag(tag)
        if (isLogLevelEnabled(truncatedTag, Log.INFO)) {
            Log.i(truncatedTag, message)
        }
    }

    /**
     * Logs the given [Log.INFO] message and the exception's stacktrace if the tag is
     * [loggable][.isInfoEnabled].
     */
    fun i(tag: String, message: String, throwable: Throwable) {
        val truncatedTag = truncateTag(tag)
        if (isLogLevelEnabled(truncatedTag, Log.INFO)) {
            Log.i(truncatedTag, message, throwable)
        }
    }

    /** Logs the given [Log.WARN] message if the tag is [loggable][.isWarnEnabled]. */
    fun w(tag: String, message: String) {
        val truncatedTag = truncateTag(tag)
        if (isLogLevelEnabled(truncatedTag, Log.WARN)) {
            Log.w(truncatedTag, message)
        }
    }

    /**
     * Logs the given [Log.WARN] message and the exception's stacktrace if the tag is
     * [loggable][.isWarnEnabled].
     */
    fun w(tag: String, message: String, throwable: Throwable) {
        val truncatedTag = truncateTag(tag)
        if (isLogLevelEnabled(truncatedTag, Log.WARN)) {
            Log.w(truncatedTag, message, throwable)
        }
    }

    /** Logs the given [Log.ERROR] message if the tag is [loggable][.isErrorEnabled]. */
    fun e(tag: String, message: String) {
        val truncatedTag = truncateTag(tag)
        if (isLogLevelEnabled(truncatedTag, Log.ERROR)) {
            Log.e(truncatedTag, message)
        }
    }

    /**
     * Logs the given [Log.ERROR] message and the exception's stacktrace if the tag is
     * [loggable][.isErrorEnabled].
     */
    fun e(tag: String, message: String, throwable: Throwable) {
        val truncatedTag = truncateTag(tag)
        if (isLogLevelEnabled(truncatedTag, Log.ERROR)) {
            Log.e(truncatedTag, message, throwable)
        }
    }

    /**
     * Truncates the tag so it can be used to log.
     *
     * On API 24, the tag length limit of 23 characters was removed.
     */
    private fun truncateTag(tag: String): String {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && MAX_TAG_LENGTH < tag.length) {
            tag.substring(0, MAX_TAG_LENGTH)
        } else tag
    }
}
