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

package androidx.build

import net.saff.checkmark.Checkmark.Companion.check
import org.gradle.testkit.runner.internal.DefaultBuildResult
import org.junit.Test

class AndroidXPluginTestContextTest {
    @Test
    fun useBuildAction() = pluginTest {
        runGradle("") { buildResult("fake output!") }.check {
            it.output == "fake output!"
        }
    }

    @Test
    fun betterDebuggingForClasspathIssues() = pluginTest {
        thrown {
            runGradle("") {
                val className = "androidx.build.gradle.ExtensionsKt"
                buildResult("java.lang.ClassNotFoundException: $className")
            }
        }!!.check {
            // Since we're faking this error, we expect that the class is actually there in the jar
            it.message!!.contains("androidx/build/gradle/ExtensionsKt.class")
        }
    }

    private fun buildResult(output: String) = DefaultBuildResult(output, listOf())

    private fun thrown(action: () -> Unit): Throwable? {
        try {
            action()
        } catch (e: Throwable) {
            return e
        }
        return null
    }
}