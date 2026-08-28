/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.tools.environment

/** Standard output mock Logger to satisfy Studio dependencies in AndroidX runtime. */
internal interface Logger {
    public fun warn(message: String, throwable: Throwable?): Unit

    public fun warn(message: String): Unit = warn(message, null)

    public fun error(message: String, throwable: Throwable?): Unit

    public fun error(message: String): Unit = error(message, null)

    public fun error(throwable: Throwable): Unit = error(throwable.message ?: "", throwable)

    public fun debug(message: String, throwable: Throwable?): Unit

    public fun debug(message: String): Unit = debug(message, null)

    public fun debug(throwable: Throwable): Unit = debug(throwable.message ?: "", throwable)

    public fun info(message: String, throwable: Throwable?): Unit

    public fun info(message: String): Unit = info(message, null)

    public fun info(throwable: Throwable): Unit = info(throwable.message ?: "", throwable)

    public val isDebugEnabled: Boolean

    public interface LoggerProvider {
        public fun createLogger(name: String): Logger

        public val priority: Int
    }

    public companion object {
        @JvmStatic
        public fun getInstance(name: String): Logger {
            return object : Logger {
                override fun warn(message: String, throwable: Throwable?) {
                    println("[WARN] $name: $message")
                    throwable?.printStackTrace()
                }

                override fun error(message: String, throwable: Throwable?) {
                    System.err.println("[ERROR] $name: $message")
                    throwable?.printStackTrace()
                }

                override fun debug(message: String, throwable: Throwable?) {
                    println("[DEBUG] $name: $message")
                    throwable?.printStackTrace()
                }

                override fun info(message: String, throwable: Throwable?) {
                    println("[INFO] $name: $message")
                    throwable?.printStackTrace()
                }

                override val isDebugEnabled: Boolean
                    get() = true
            }
        }

        @JvmStatic public fun <T> getInstance(clazz: Class<T>): Logger = getInstance(clazz.name)
    }
}
