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

import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File
import java.util.Properties

/**
 * Helper class to laod some environment related configuration.
 */
object EnvironmentConfig {
    private val config: Properties by lazy {
        val config = Properties()
        val resourceName = "/importMavenConfig.properties"
        EnvironmentConfig::class.java.getResource(resourceName).let {
            checkNotNull(it) {
                "Cannot find properties file: $resourceName"
            }
        }.openStream()
            .use {
                config.load(it)
            }
        config
    }

    /**
     * The path for the support root folder (frameworks/support)
     */
    val supportRoot: Path by lazy {
        checkNotNull(config["supportRoot"]) {
            "missing supportRoot property"
        }.let {
            File(it.toString()).toOkioPath()
        }
    }
}