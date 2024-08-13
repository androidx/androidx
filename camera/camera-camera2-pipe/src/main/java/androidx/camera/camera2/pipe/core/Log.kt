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

package androidx.camera.camera2.pipe.core

import android.util.Log

/**
 * This object provides a set of common log functions that are optimized for CameraPipe with options
 * to control which log levels are available to log at compile time via const val's.
 *
 * Log functions have been designed so that printing variables and doing string concatenation will
 * not occur if the log level is disabled, which leads to slightly unusual syntax:
 *
 * Log.debug { "This is a log message with a $value" }
 */
public object Log {
    public const val TAG: String = "CXCP"

    private const val LOG_LEVEL_DEBUG = 1
    private const val LOG_LEVEL_INFO = 2
    private const val LOG_LEVEL_WARN = 3
    private const val LOG_LEVEL_ERROR = 4

    // This indicates the lowest log level that will always log.
    private const val LOG_LEVEL = LOG_LEVEL_DEBUG

    public val DEBUG_LOGGABLE: Boolean =
        LOG_LEVEL <= LOG_LEVEL_DEBUG || Log.isLoggable(TAG, Log.DEBUG)
    public val INFO_LOGGABLE: Boolean = LOG_LEVEL <= LOG_LEVEL_INFO || Log.isLoggable(TAG, Log.INFO)
    public val WARN_LOGGABLE: Boolean = LOG_LEVEL <= LOG_LEVEL_WARN || Log.isLoggable(TAG, Log.WARN)
    public val ERROR_LOGGABLE: Boolean =
        LOG_LEVEL <= LOG_LEVEL_ERROR || Log.isLoggable(TAG, Log.ERROR)

    /** Debug functions log noisy information related to the internals of the system. */
    public inline fun debug(crossinline msg: () -> String) {
        if (Debug.ENABLE_LOGGING && DEBUG_LOGGABLE) Log.d(TAG, msg())
    }

    public inline fun debug(throwable: Throwable, crossinline msg: () -> String) {
        if (Debug.ENABLE_LOGGING && DEBUG_LOGGABLE) Log.d(TAG, msg(), throwable)
    }

    /** Info functions log standard, useful information about the state of the system. */
    public inline fun info(crossinline msg: () -> String) {
        if (Debug.ENABLE_LOGGING && INFO_LOGGABLE) Log.i(TAG, msg())
    }

    public inline fun info(throwable: Throwable, crossinline msg: () -> String) {
        if (Debug.ENABLE_LOGGING && INFO_LOGGABLE) Log.i(TAG, msg(), throwable)
    }

    /**
     * Warning functions are used when something unexpected may lead to a crash or fatal exception
     * later on as a result if the unusual circumstances
     */
    public inline fun warn(crossinline msg: () -> String) {
        if (Debug.ENABLE_LOGGING && WARN_LOGGABLE) Log.w(TAG, msg())
    }

    public inline fun warn(throwable: Throwable, crossinline msg: () -> String) {
        if (Debug.ENABLE_LOGGING && WARN_LOGGABLE) Log.w(TAG, msg(), throwable)
    }

    /**
     * Error functions are reserved for something unexpected that will lead to a crash or data loss.
     */
    public inline fun error(crossinline msg: () -> String) {
        if (Debug.ENABLE_LOGGING && ERROR_LOGGABLE) Log.e(TAG, msg())
    }

    public inline fun error(throwable: Throwable, crossinline msg: () -> String) {
        if (Debug.ENABLE_LOGGING && ERROR_LOGGABLE) Log.e(TAG, msg(), throwable)
    }

    /**
     * Try-catch [block] and rethrow caught exception after logging with [msg].
     *
     * @param msg The message to log with the exception.
     * @param block Function to be wrapped in try-catch.
     * @return Original returned value of `block` in case of no exception.
     * @throws Exception that is caught while executing `block`.
     */
    public inline fun <T> rethrowExceptionAfterLogging(msg: String, crossinline block: () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            error(e) { msg }
            throw e
        }

    /** Read the stack trace of a calling method and join it to a formatted string. */
    public fun readStackTrace(limit: Int = 4): String {
        val elements = Thread.currentThread().stackTrace
        // Ignore the first 3 elements, which ignores:
        // VMStack.getThreadStackTrace
        // Thread.currentThread().getStackTrace()
        // dumpStackTrace()
        return elements
            .drop(3)
            .joinToString(
                prefix = "\n\t",
                separator = "\t",
                limit = limit,
            )
    }

    /**
     * Note that the message constants here may be used to parse test data, so these constant values
     * should be changed with caution. See b/356108571 for details.
     */
    public object MonitoredLogMessages {
        public const val REPEATING_REQUEST_STARTED_TIMEOUT: String =
            "awaitStarted on last repeating request timed out"
    }
}
