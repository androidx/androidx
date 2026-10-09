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

package androidx.test.backup.host

import com.android.tools.environment.Logger as StudioLogger
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Logs the messages of Android Studio's backup service through `java.util.logging`, like the rest
 * of this library, with debug messages at [Level.FINE].
 */
internal class BackupServiceLogger(name: String) : StudioLogger {

    private val logger = Logger.getLogger(name)

    override fun error(message: String, throwable: Throwable?) {
        logger.log(Level.SEVERE, message, throwable)
    }

    override fun warn(message: String, throwable: Throwable?) {
        logger.log(Level.WARNING, message, throwable)
    }

    override fun info(message: String, throwable: Throwable?) {
        logger.log(Level.INFO, message, throwable)
    }

    override fun debug(message: String, throwable: Throwable?) {
        logger.log(Level.FINE, message, throwable)
    }

    /**
     * Whether debug messages are logged. The backup service checks it before it formats the output
     * of every command it runs.
     */
    override val isDebugEnabled: Boolean
        get() = logger.isLoggable(Level.FINE)
}
