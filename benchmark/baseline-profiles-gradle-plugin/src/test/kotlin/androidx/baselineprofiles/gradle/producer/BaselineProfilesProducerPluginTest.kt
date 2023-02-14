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

package androidx.baselineprofiles.gradle.producer

import androidx.testutils.gradle.ProjectSetupRule
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BaselineProfilesProducerPluginTest {

    // Unit test will be minimal because the producer plugin is applied to an android test module,
    // that requires a working target application. Testing will be covered only by integration tests.

    private val rootFolder = TemporaryFolder().also { it.create() }

    @get:Rule
    val producerProjectSetup = ProjectSetupRule(rootFolder.root)

    @get:Rule
    val buildProviderProjectSetup = ProjectSetupRule(rootFolder.root)

    private lateinit var producerModuleName: String
    private lateinit var buildProviderModuleName: String
    private lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        producerModuleName = producerProjectSetup.rootDir.relativeTo(rootFolder.root).name
        buildProviderModuleName = buildProviderProjectSetup.rootDir.relativeTo(rootFolder.root).name

        rootFolder.newFile("settings.gradle").writeText(
            """
            include '$producerModuleName'
            include '$buildProviderModuleName'
        """.trimIndent()
        )
        gradleRunner = GradleRunner.create()
            .withProjectDir(producerProjectSetup.rootDir)
            .withPluginClasspath()
    }

    @Test
    fun verifyTasksWithAndroidTestPlugin() {
        buildProviderProjectSetup.writeDefaultBuildGradle(
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
        producerProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.test")
                    id("androidx.baselineprofiles.producer")
                }
                android {
                    targetProjectPath = ":$buildProviderModuleName"
                    namespace 'com.example.namespace.test'
                }
                tasks.register("mergeNonObfuscatedReleaseTestResultProtos") { println("Stub") }
            """.trimIndent(),
            suffix = ""
        )

        val output = gradleRunner.withArguments("tasks", "--stacktrace").build().output
        assertTrue { output.contains("collectNonObfuscatedReleaseBaselineProfiles - ") }
    }
}
