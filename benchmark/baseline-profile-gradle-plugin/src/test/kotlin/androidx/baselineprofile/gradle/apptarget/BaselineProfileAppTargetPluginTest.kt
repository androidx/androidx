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
import androidx.baselineprofile.gradle.utils.TEST_AGP_VERSION_ALL
import androidx.baselineprofile.gradle.utils.build
import androidx.baselineprofile.gradle.utils.buildAndAssertThatOutput
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.Parameterized

private val buildGradle = """
    plugins {
        id("com.android.application")
        id("androidx.baselineprofile.apptarget")
    }

    android {
        namespace 'com.example.namespace'
        buildTypes {
            anotherRelease {
                initWith(release)
            }
        }
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
            tasks.register(variant.name + "JavaSources", DisplaySourceSets) { t ->
                t.srcs.set(variant.sources.java.all)
            }
            tasks.register(variant.name + "KotlinSources", DisplaySourceSets) { t ->
                t.srcs.set(variant.sources.kotlin.all)
            }
        }
    }
    """.trimIndent()

@RunWith(Parameterized::class)
class BaselineProfileAppTargetPluginTest(agpVersion: String?) {

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(forceAgpVersion = agpVersion)

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TEST_AGP_VERSION_ALL
    }

    @Test
    fun testSrcSetAreAddedToVariantsForApplications() {
        projectSetup.appTarget.setBuildGradle(buildGradle)

        data class TaskAndExpected(val taskName: String, val expectedDirs: List<String>)

        arrayOf(
            TaskAndExpected(
                taskName = "nonMinifiedAnotherReleaseJavaSources",
                expectedDirs = listOf(
                    "src/main/java",
                    "src/anotherRelease/java",
                    "src/nonMinifiedAnotherRelease/java",
                )
            ),
            TaskAndExpected(
                taskName = "nonMinifiedReleaseJavaSources",
                expectedDirs = listOf(
                    "src/main/java",
                    "src/release/java",
                    "src/nonMinifiedRelease/java",
                )
            ),
            TaskAndExpected(
                taskName = "nonMinifiedAnotherReleaseKotlinSources",
                expectedDirs = listOf(
                    "src/main/kotlin",
                    "src/anotherRelease/kotlin",
                    "src/nonMinifiedAnotherRelease/kotlin",
                )
            ),
            TaskAndExpected(
                taskName = "nonMinifiedReleaseKotlinSources",
                expectedDirs = listOf(
                    "src/main/kotlin",
                    "src/release/kotlin",
                    "src/nonMinifiedRelease/kotlin",
                )
            )
        )
            .forEach { t ->

                // Runs the task and assert
                projectSetup.appTarget.gradleRunner.buildAndAssertThatOutput(t.taskName) {
                    t.expectedDirs
                        .map { File(projectSetup.appTarget.rootDir, it) }
                        .forEach { e -> contains(e.absolutePath) }
                }
            }
    }
}

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
