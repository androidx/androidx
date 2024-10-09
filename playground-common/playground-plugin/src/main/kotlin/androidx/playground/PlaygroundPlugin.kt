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

package androidx.playground

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class PlaygroundPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.apply(mapOf("plugin" to "playground-develocity-conventions"))
        settings.apply(mapOf("plugin" to "com.android.settings"))
        settings.extensions.create("playground", PlaygroundExtension::class.java, settings)
        validateJvm(settings)
    }

    private fun validateJvm(settings: Settings) {
        // validate JVM version to print an understandable error if it is not set to the
        // required value (21)
        val jvmVersion = System.getProperty("java.vm.specification.version")
        check(jvmVersion == "21") {
            """
                AndroidX build must be invoked with JDK 21.
                ${
                    if (settings.gradle.startParameter.projectProperties.containsKey(
                            "android.injected.invoked.from.ide"
                        )
                    ) {
                        """
                            Make sure to set the Gradle JDK to JDK 21 in the project settings.
                            File -> Settings (on Mac Android Studio -> Preferences) ->
                            Build, Execution, Deployment -> Build Tools ->
                            Gradle -> Gradle JDK"
                        """
                        } else {
                        "Make sure your JAVA_HOME environment variable points to Java 21 JDK."
                    }
                }
                Current version: $jvmVersion
                Current JAVA HOME: ${System.getProperty("java.home")}
            """.trimIndent()
        }
    }
}
