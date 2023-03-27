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

package androidx.stableaidl

import androidx.testutils.gradle.ProjectSetupRule
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StableAidlPluginTest {
    @get:Rule
    val projectSetup = ProjectSetupRule()
    private lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        gradleRunner = GradleRunner.create()
            .withProjectDir(projectSetup.rootDir)
            .withPluginClasspath()
    }

    @Test
    fun applyPluginAppProject() {
        projectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id('com.android.application')
                    id('androidx.stableaidl')
                }
            """.trimIndent(),
            suffix = """
            android {
                namespace 'androidx.stableaidl.testapp'
                buildFeatures {
                  aidl = true
                }
                buildTypes.all {
                  stableAidl {
                    version 1
                  }
                }
            }
            """.trimIndent()
        )

        // Tasks should contain those defined in StableAidlTasks.
        val output = gradleRunner.withArguments("tasks", "--stacktrace").build()
        assertTrue { output.output.contains("compileDebugAidlApi - ") }
        assertTrue { output.output.contains("checkDebugAidlApiRelease - ") }
    }

    @Test
    fun applyPluginAndroidLibProject() {
        projectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id('com.android.library')
                    id('androidx.stableaidl')
                }
            """.trimIndent(),
            suffix = """
            android {
                namespace 'androidx.stableaidl.testapp'
                buildFeatures {
                  aidl = true
                }
                buildTypes.all {
                  stableAidl {
                    version 1
                  }
                }
            }
            """.trimIndent()
        )

        // Tasks should contain those defined in StableAidlTasks.
        val output = gradleRunner.withArguments("tasks", "--stacktrace").build()
        assertTrue { output.output.contains("compileDebugAidlApi - ") }
        assertTrue { output.output.contains("checkDebugAidlApiRelease - ") }
    }

    @Test
    fun applyPluginNonAndroidProject() {
        projectSetup.buildFile.writeText(
            """
            plugins {
                id('java')
                id('androidx.stableaidl')
            }

            repositories {
                ${projectSetup.defaultRepoLines}
            }
            """.trimIndent()
        )

        assertFailsWith(UnexpectedBuildFailure::class) {
            gradleRunner.withArguments("jar").build()
        }
    }
}
