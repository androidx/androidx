/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.baselineprofile.gradle.apptarget

import androidx.baselineprofile.gradle.utils.BaselineProfileProjectSetupRule
import androidx.baselineprofile.gradle.utils.buildAndAssertThatOutput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BaselineProfileAppTargetPluginTest {

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule()

    @Test
    fun verifyBuildTypes() {
        projectSetup.appTarget.setBuildGradle(
            """
            plugins {
                id("com.android.application")
                id("androidx.baselineprofile.apptarget")
            }
            android {
                namespace 'com.example.namespace'
                buildTypes {
                    anotherRelease { initWith(release) }
                }
            }

            def registerTask(buildTypeName, taskName) {
                tasks.register(taskName, PrintTask) { t ->
                    def buildType = android.buildTypes[buildTypeName]
                    def text = "minifyEnabled=" + buildType.minifyEnabled.toString() + "\n"
                    text += "testCoverageEnabled=" + buildType.testCoverageEnabled.toString() + "\n"
                    text += "debuggable=" + buildType.debuggable.toString() + "\n"
                    text += "profileable=" + buildType.profileable.toString() + "\n"
                    t.text.set(text)
                }
            }
            registerTask("nonMinifiedRelease", "printNonMinifiedReleaseBuildType")
            registerTask("nonMinifiedAnotherRelease", "printNonMinifiedAnotherReleaseBuildType")
            """.trimIndent()
        )

        arrayOf("printNonMinifiedReleaseBuildType", "printNonMinifiedAnotherReleaseBuildType")
            .forEach { taskName ->
                projectSetup.appTarget.gradleRunner.buildAndAssertThatOutput(taskName) {
                    contains("minifyEnabled=false")
                    contains("testCoverageEnabled=false")
                    contains("debuggable=false")
                    contains("profileable=true")
                }
            }
    }
}
