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

package androidx.baselineprofiles.gradle.buildprovider

import androidx.testutils.gradle.ProjectSetupRule
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BaselineProfilesBuildProviderPluginTest {

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
    fun verifyBuildType() {
        projectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofiles.buildprovider")
                }
                android {
                    namespace 'com.example.namespace'
                }
                tasks.register("printBuildType") {
                    println(android.buildTypes.nonMinifiedRelease)
                }
            """.trimIndent(),
            suffix = ""
        )

        gradleRunner
            .withArguments("printBuildType", "--stacktrace")
            .build()
            .output
            .also {
                assertThat(it).contains("minifyEnabled=false")
                assertThat(it).contains("testCoverageEnabled=false")
                assertThat(it).contains("debuggable=false")
            }
    }
}
