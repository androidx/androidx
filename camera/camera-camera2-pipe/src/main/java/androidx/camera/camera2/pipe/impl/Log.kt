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

package androidx.camera.camera2.pipe.impl

import android.util.Log

/**
 * This object provides a set of common log functions that are optimized for CameraPipe with
 * options to control which log levels are available to log at compile time via const val's.
 *
 * Log functions have been designed so that printing variables and doing string concatenation
 * will not occur if the log level is disabled, which leads to slightly unusual syntax:
 *
 * Log.debug { "This is a log message with a $value" }
 */
object Log {
    const val TAG = "CameraPipe"

    private const val LOG_LEVEL_DEBUG = 1
    private const val LOG_LEVEL_INFO = 2
    private const val LOG_LEVEL_WARN = 3
    private const val LOG_LEVEL_ERROR = 4

    // This indicates the lowest log level that can be enabled.
    private const val LOG_LEVEL =
        LOG_LEVEL_ERROR

    const val DEBUG_ENABLED = Debug.ENABLE_LOGGING && LOG_LEVEL <= LOG_LEVEL_DEBUG
    const val INFO_ENABLED = Debug.ENABLE_LOGGING && LOG_LEVEL <= LOG_LEVEL_INFO
    const val WARN_ENABLED = Debug.ENABLE_LOGGING && LOG_LEVEL <= LOG_LEVEL_WARN
    const val ERROR_ENABLED = Debug.ENABLE_LOGGING && LOG_LEVEL <= LOG_LEVEL_ERROR

    val DEBUG_LOGGABLE = Log.isLoggable(TAG, Log.DEBUG)
    val INFO_LOGGABLE = Log.isLoggable(TAG, Log.INFO)
    val WARN_LOGGABLE = Log.isLoggable(TAG, Log.WARN)
    val ERROR_LOGGABLE = Log.isLoggable(TAG, Log.ERROR)

    /**
     * Debug functions log noisy information related to the internals of the system.
     */
    inline fun debug(crossinline msg: () -> String) {
        if (DEBUG_ENABLED && DEBUG_LOGGABLE) Log.d(TAG, msg())
    }

    inline fun debug(throwable: Throwable, crossinline msg: () -> String) {
        if (DEBUG_ENABLED && DEBUG_LOGGABLE) Log.d(TAG, msg(), throwable)
    }

    /**
     * Info functions log standard, useful information about the state of the system.
     */
    inline fun info(crossinline msg: () -> String) {
        if (INFO_ENABLED && INFO_LOGGABLE) Log.i(TAG, msg())
    }

    inline fun info(throwable: Throwable, crossinline msg: () -> String) {
        if (INFO_ENABLED && INFO_LOGGABLE) Log.i(TAG, msg(), throwable)
    }

    /**
     * Warning functions are used when something unexpected may lead to a crash or fatal exception
     * later on as a result if the unusual circumstances
     */
    inline fun warn(crossinline msg: () -> String) {
        if (WARN_ENABLED && WARN_LOGGABLE) Log.w(TAG, msg())
    }

    inline fun warn(throwable: Throwable, crossinline msg: () -> String) {
        if (WARN_ENABLED && WARN_LOGGABLE) Log.w(TAG, msg(), throwable)
    }

    /**
     * Error functions are reserved for something unexpected that will lead to a crash or data loss.
     */
    inline fun error(crossinline msg: () -> String) {
        if (ERROR_ENABLED && ERROR_LOGGABLE) Log.e(TAG, msg())
    }

    inline fun error(throwable: Throwable, crossinline msg: () -> String) {
        if (ERROR_ENABLED && ERROR_LOGGABLE) Log.e(TAG, msg(), throwable)
    }
}