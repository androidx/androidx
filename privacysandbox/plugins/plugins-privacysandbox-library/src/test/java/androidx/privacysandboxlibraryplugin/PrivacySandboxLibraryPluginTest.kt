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

package androidx.privacysandboxlibraryplugin

import androidx.testutils.gradle.ProjectSetupRule
import java.io.File
import java.nio.file.Files
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PrivacySandboxLibraryPluginTest {

    @get:Rule
    val projectSetup = ProjectSetupRule()

    lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        File(projectSetup.rootDir, "settings.gradle")
            .writeText("rootProject.name = \"test-privacysandbox-library\"")
        projectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("kotlin-android")
                    id("androidx.privacysandbox.library")
                }
            """.trimIndent(),
            suffix = """
                    android {
                         namespace "test.privacysandboxlibrary"
                         compileOptions {
                            sourceCompatibility JavaVersion.VERSION_17
                            targetCompatibility JavaVersion.VERSION_17
                         }
                        kotlinOptions {
                            jvmTarget=17
                        }
                    }
            """
        )

        val myServiceSource = File(
            projectSetup.rootDir,
            "src/main/java/test/privacysandboxlibraryplugintest"
        ).also { it.mkdirs() }

        myServiceSource.resolve("MyService.kt").also {
            Files.createFile(it.toPath())
            it.writeText(
                """package test.privacysandboxlibrary

                    import androidx.privacysandbox.tools.PrivacySandboxService

                    @PrivacySandboxService
                    interface MyService {
                        suspend fun doStuff(x: Int, y: Int): String
                    }
                """
            )
        }

        gradleRunner = GradleRunner.create()
            .withProjectDir(projectSetup.rootDir)
            .withPluginClasspath()
    }

    /* Test plugin applies successfully and produces KSP generated directory. The output of KSP
    * is unit tested in :tools:apicompiler and integration tested in Android Gradle Plugin tests.
    */
    @Test
    fun applyPlugin() {
        val output = gradleRunner.withArguments("build", "--stacktrace").build()
        assertEquals(output.task(":build")!!.outcome, TaskOutcome.SUCCESS)
        assertEquals(output.task(":kspDebugKotlin")!!.outcome, TaskOutcome.SUCCESS)
        val build = File(projectSetup.rootDir, "build")
        assertTrue(File(build, "generated/ksp/debug").exists())
    }
}
