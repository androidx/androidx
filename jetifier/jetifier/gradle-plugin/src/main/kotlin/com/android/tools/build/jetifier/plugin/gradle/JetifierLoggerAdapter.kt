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

package com.android.tools.build.jetifier.plugin.gradle

import com.android.tools.build.jetifier.core.utils.LogConsumer
import org.gradle.api.logging.Logger

/**
 * Logging adapter to hook jetfier logging into gradle.
 */
class JetifierLoggerAdapter(val gradleLogger: Logger) : LogConsumer {

    override fun error(message: String) {
        gradleLogger.error(message)
    }

    override fun warning(message: String) {
        gradleLogger.warn(message)
    }

    override fun info(message: String) {
        gradleLogger.info(message)
    }

    override fun verbose(message: String) {
        gradleLogger.info(message)
    }
}