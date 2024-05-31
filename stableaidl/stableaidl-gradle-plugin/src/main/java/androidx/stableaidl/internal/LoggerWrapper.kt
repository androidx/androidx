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
package androidx.stableaidl.internal

import com.android.ide.common.resources.MergingException
import com.android.utils.ILogger
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Implementation of Android's [ILogger] over Gradle's [Logger].
 *
 * Note that this maps info to the default user-visible lifecycle.
 *
 * Cloned from `com.android.build.gradle.internal.LoggerWrapper`.
 */
class LoggerWrapper(private val logger: Logger) : ILogger {
    override fun error(throwable: Throwable?, sOrig: String?, vararg objects: Any) {
        if (throwable is MergingException) {
            // MergingExceptions have a known cause: they aren't internal errors, they
            // are errors in the user's code, so a full exception is not helpful (and
            // these exceptions should include a pointer to the user's error right in
            // the message).
            //
            // Furthermore, these exceptions are already caught by the MergeResources
            // and MergeAsset tasks, so don't duplicate the output
            return
        }
        if (!logger.isEnabled(ILOGGER_ERROR)) {
            return
        }
        val s =
            if (sOrig == null) {
                "[no message defined]"
            } else if (objects.isNotEmpty()) {
                String.format(sOrig, *objects)
            } else {
                sOrig
            }
        if (throwable == null) {
            logger.log(ILOGGER_ERROR, s)
        } else {
            logger.log(ILOGGER_ERROR, s, throwable)
        }
    }

    override fun warning(s: String, vararg objects: Any) {
        log(ILOGGER_WARNING, s, objects)
    }

    override fun quiet(s: String, vararg objects: Any) {
        log(ILOGGER_QUIET, s, objects)
    }

    override fun lifecycle(s: String, vararg objects: Any) {
        log(ILOGGER_LIFECYCLE, s, objects)
    }

    override fun info(s: String, vararg objects: Any) {
        log(ILOGGER_INFO, s, objects)
    }

    override fun verbose(s: String, vararg objects: Any) {
        log(ILOGGER_VERBOSE, s, objects)
    }

    private fun log(logLevel: LogLevel, s: String, objects: Array<out Any>) {
        if (!logger.isEnabled(logLevel)) {
            return
        }
        if (objects.isEmpty()) {
            logger.log(logLevel, s)
        } else {
            logger.log(logLevel, String.format(s, *objects))
        }
    }

    companion object {
        // Mapping from ILogger method call to gradle log level.
        private val ILOGGER_ERROR = LogLevel.ERROR
        private val ILOGGER_WARNING = LogLevel.WARN
        private val ILOGGER_QUIET = LogLevel.QUIET
        private val ILOGGER_LIFECYCLE = LogLevel.LIFECYCLE
        private val ILOGGER_INFO = LogLevel.INFO
        private val ILOGGER_VERBOSE = LogLevel.INFO

        fun getLogger(klass: Class<*>): LoggerWrapper {
            return LoggerWrapper(Logging.getLogger(klass))
        }
    }
}
