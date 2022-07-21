/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.importMaven

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import java.util.concurrent.TimeUnit

internal fun enableInfoLogs() {
    val ctx = LogManager.getContext(false) as LoggerContext
    ctx.configuration.rootLogger.level = Level.INFO
}

internal fun enableVerboseLogs() {
    val ctx = LogManager.getContext(false) as LoggerContext
    ctx.configuration.rootLogger.level = Level.TRACE
}

internal fun flushLogs() {
    val ctx = LogManager.getContext(false) as LoggerContext
    ctx.stop(10, TimeUnit.SECONDS)
}