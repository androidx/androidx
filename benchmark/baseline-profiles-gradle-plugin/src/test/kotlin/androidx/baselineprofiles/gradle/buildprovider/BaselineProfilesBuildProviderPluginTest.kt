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
import java.io.File
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
                tasks.register("printNonObfuscatedReleaseBuildType") {
                    android.buildTypes.nonObfuscatedRelease.properties.each {k,v->println(k+"="+v)}
                }
            """.trimIndent(),
            suffix = ""
        )

        val buildTypeProperties = gradleRunner
            .withArguments("printNonObfuscatedReleaseBuildType", "--stacktrace")
            .build()
            .output
            .lines()

        assertThat(buildTypeProperties).contains("shrinkResources=true")
        assertThat(buildTypeProperties).contains("minifyEnabled=true")
        assertThat(buildTypeProperties).contains("testCoverageEnabled=false")
        assertThat(buildTypeProperties).contains("debuggable=false")
    }

    @Test
    fun generateBaselineProfilesKeepRuleFile() {
        projectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofiles.buildprovider")
                }
                android {
                    namespace 'com.example.namespace'
                }
            """.trimIndent(),
            suffix = ""
        )

        val outputLines = gradleRunner
            .withArguments("generateBaselineProfilesKeepRules", "--stacktrace", "--info")
            .build()
            .output
            .lines()

        val find = "Generated keep rule file for baseline profiles build in"
        val proguardFilePath = outputLines
            .first { it.startsWith(find) }
            .split(find)[1]
            .trim()
        val proguardContent = File(proguardFilePath)
            .readText()
            .lines()
            .filter { !it.startsWith("#") && it.isNotBlank() }
        assertThat(proguardContent).containsExactly("-dontobfuscate")
    }
}
