/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.utils

object Log {

    var currentLevel : LogLevel = LogLevel.INFO

    var logConsumer : LogConsumer = StdOutLogConsumer()

    fun setLevel(level: String?) {
        currentLevel = when (level) {
            "debug" -> LogLevel.DEBUG
            "verbose" -> LogLevel.VERBOSE
            else -> LogLevel.INFO
        }

    }

    fun e(tag: String, message: String, vararg args: Any?) {
        if (currentLevel >= LogLevel.ERROR) {
            logConsumer.error("[$tag] $message".format(*args))
        }
    }

    fun d(tag: String, message: String, vararg args: Any?) {
        if (currentLevel >= LogLevel.DEBUG) {
            logConsumer.debug("[$tag] $message".format(*args))
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