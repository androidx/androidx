/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.inspection.gradle

import androidx.testutils.gradle.ProjectSetupRule
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.apache.tools.zip.ZipFile
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InspectionPluginTest {
    @get:Rule
    val projectSetup = ProjectSetupRule()

    lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        File("src/test/test-data").copyRecursively(projectSetup.rootDir)
        gradleRunner = GradleRunner.create()
            .withProjectDir(projectSetup.rootDir)
            .withPluginClasspath()
    }

    @Test
    fun testInspectorJar() {
        with(projectSetup) {
            val prefix =
                "import static androidx.inspection.gradle.InspectionPluginKt.packageInspector\n\n" +
                "plugins { id(\"com.android.library\") }\n"
            File(rootDir, "lib/build.gradle")
                .writeText("$prefix\n\n" +
                    "$repositories\n\n$androidProject\n${namespace("foox.lib")}\n" +
                    "packageInspector(project, \":lib-inspector\")"
                )
            val inspectorPlugins = """
                plugins {
                    id("com.android.library")
                    id("androidx.inspection")
                }
            """.trimIndent()
            val suffix = """
                dependencies {
                    implementation("androidx.inspection:inspection:1.0.0")
                }
                android {
                    defaultConfig {
                        targetSdkVersion 30
                    }
                }
            """.trimIndent()

            File(rootDir, "lib-inspector/build.gradle")
                .writeText(
                    "$inspectorPlugins\n$repositories\n\n$androidProject\n" +
                        "${namespace("foox.lib.inspector")}\n$suffix"
                )
        }

        val task = ":lib:assembleRelease"
        val output = gradleRunner.withArguments(task).build()
        assertEquals(output.task(task)!!.outcome, TaskOutcome.SUCCESS)
        val artifact = File(projectSetup.rootDir, "lib/build/outputs/aar/lib-release.aar")
        assertTrue { artifact.exists() }
        val inspectorJar = ZipFile(artifact).use { aarFile ->
            aarFile.entries.toList().find {
                it.name == "inspector.jar"
            }
        }
        assertNotNull(inspectorJar)
    }
}

private fun namespace(name: String) = """
    android {
         namespace "$name"
    }
""".trimIndent()
