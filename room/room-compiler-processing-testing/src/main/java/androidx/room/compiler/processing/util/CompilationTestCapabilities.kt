/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.util

import org.junit.AssumptionViolatedException
import java.util.Properties

/**
 * Provides the information about compilation test capabilities.
 * see: b/178725084
 */
object CompilationTestCapabilities {
    /**
     * `true` if we can run KSP tests.
     */
    val canTestWithKsp: Boolean

    init {
        val config = Config.load()
        canTestWithKsp = config.canEnableKsp()
    }

    /**
     * Checks if KSP tests can be run and if not, throws an [AssumptionViolatedException] to skip
     * the test.
     */
    fun assumeKspIsEnabled() {
        if (!canTestWithKsp) {
            throw AssumptionViolatedException("KSP tests are not enabled")
        }
    }

    internal data class Config(
        val kotlinVersion: String,
        val kspVersion: String
    ) {
        fun canEnableKsp(): Boolean {
            val reducedKotlin = reduceVersions(kotlinVersion)
            val reducedKsp = reduceVersions(kspVersion)
            return reducedKotlin.contentEquals(reducedKsp)
        }

        /**
         * Reduces the version to some approximation by taking major and minor versions and the
         * first character of the patch. We use this to check if ksp and kotlin are compatible,
         * feel free to change it if it does not work as it is only an approximation
         * e.g. 1.4.20 becomes 1.4.2, 1.40.210-foobar becomes 1.40.2
         */
        private fun reduceVersions(version: String): Array<String?> {
            val sections = version.split('.')
            return arrayOf(
                sections.getOrNull(0),
                sections.getOrNull(1),
                sections.getOrNull(2)?.trim()?.first()?.toString(),
            )
        }

        companion object {
            /**
             * Load the test configuration from resources.
             */
            fun load(): Config {
                val props = Properties()
                val resourceName = "/${Config::class.java.canonicalName}.properties"
                CompilationTestCapabilities::class.java
                    .getResource(resourceName)
                    .openStream()
                    .use {
                        props.load(it)
                    }
                return Config(
                    kotlinVersion = props.getProperty("kotlinVersion") as String,
                    kspVersion = props.getProperty("kspVersion") as String
                )
            }
        }
    }
}