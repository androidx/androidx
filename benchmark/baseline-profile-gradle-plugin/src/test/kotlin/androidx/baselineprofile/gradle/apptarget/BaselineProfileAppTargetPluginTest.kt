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
import androidx.baselineprofile.gradle.utils.TEST_AGP_VERSION_8_0_0
import androidx.baselineprofile.gradle.utils.TEST_AGP_VERSION_8_1_0
import androidx.baselineprofile.gradle.utils.build
import androidx.baselineprofile.gradle.utils.buildAndAssertThatOutput
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private val buildGradle = """
    plugins {
        id("com.android.application")
        id("androidx.baselineprofile.apptarget")
    }

    android {
        namespace 'com.example.namespace'
        buildTypes { anotherRelease { initWith(release) } }
    }

    androidComponents {
        onVariants(selector()) { variant ->
            tasks.register(variant.name + "BuildProperties", PrintTask) { t ->
                def buildType = android.buildTypes[variant.buildType]
                def text = "minifyEnabled=" + buildType.minifyEnabled.toString() + "\n"
                text += "testCoverageEnabled=" + buildType.testCoverageEnabled.toString() + "\n"
                text += "debuggable=" + buildType.debuggable.toString() + "\n"
                text += "profileable=" + buildType.profileable.toString() + "\n"
                t.text.set(text)
            }
        }
    }
    """.trimIndent()

@RunWith(JUnit4::class)
class BaselineProfileAppTargetPluginTestWithAgp80 {

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(
        forceAgpVersion = TEST_AGP_VERSION_8_0_0
    )

    @Test
    fun verifyBuildTypes() {
        projectSetup.appTarget.setBuildGradle(buildGradle)

        // Assert properties of the baseline profile build types
        arrayOf("nonMinifiedReleaseBuildProperties", "nonMinifiedAnotherReleaseBuildProperties")
            .forEach { taskName ->
                projectSetup.appTarget.gradleRunner.buildAndAssertThatOutput(taskName) {
                    contains("minifyEnabled=true")
                    contains("testCoverageEnabled=false")
                    contains("debuggable=false")
                    contains("profileable=true")
                }
            }

        // Note that the proguard file path does not exist till the generate keep rule task is
        // executed. For this reason we call directly the `assemble` task and check the task log.
        // Also the generate keep rules task is the same across multiple variant builds so it will
        // be executed only once.

        projectSetup.appTarget.gradleRunner.build("assemble", "--info") {
            val logLine = it.lines().firstOrNull { l ->
                l.startsWith("Generated keep rule file for baseline profiles build type in") &&
                    l.endsWith("intermediates/baselineprofiles/tmp/dontobfuscate.pro")
            }
            assertThat(logLine).isNotNull()
        }
    }
}

@RunWith(JUnit4::class)
class BaselineProfileAppTargetPluginTestWithAgp81 {

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(
        forceAgpVersion = TEST_AGP_VERSION_8_1_0
    )

    @Test
    fun verifyBuildTypes() {
        projectSetup.appTarget.setBuildGradle(buildGradle)

        // Assert properties of the benchmark build types
        arrayOf("benchmarkReleaseBuildProperties", "benchmarkAnotherReleaseBuildProperties")
            .forEach { taskName ->
                projectSetup.appTarget.gradleRunner.buildAndAssertThatOutput(taskName) {
                    contains("minifyEnabled=true")
                    contains("testCoverageEnabled=false")
                    contains("debuggable=false")
                    contains("profileable=true")
                }
            }

        // Assert properties of the baseline profile build types
        arrayOf("nonMinifiedReleaseBuildProperties", "nonMinifiedAnotherReleaseBuildProperties")
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
