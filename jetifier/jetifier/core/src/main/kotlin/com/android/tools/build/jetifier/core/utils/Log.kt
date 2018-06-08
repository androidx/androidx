/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.core.utils

object Log {

    var currentLevel: LogLevel = LogLevel.ERROR

    var logConsumer: LogConsumer = StdOutLogConsumer()

    fun setLevel(level: String?) {
        currentLevel = when (level) {
            "info" -> LogLevel.INFO
            "error" -> LogLevel.ERROR
            "warning" -> LogLevel.WARNING
            "verbose" -> LogLevel.VERBOSE
            else -> LogLevel.WARNING
        }
    }

    fun e(tag: String, message: String, vararg args: Any?) {
        if (currentLevel >= LogLevel.ERROR) {
            logConsumer.error("[$tag] $message".format(*args))
        }
    }

    fun w(tag: String, message: String, vararg args: Any?) {
        if (currentLevel >= LogLevel.WARNING) {
            logConsumer.warning("[$tag] $message".format(*args))
        }
    }

    fun i(tag: String, message: String, vararg args: Any?) {
        if (currentLevel >= LogLevel.INFO) {
            logConsumer.info("[$tag] $message".format(*args))
        }
    }

    fun v(tag: String, message: String, vararg args: Any?) {
        if (currentLevel >= LogLevel.VERBOSE) {
            logConsumer.verbose("[$tag] $message".format(*args))
        }
    }
}