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

import androidx.build.AndroidXPluginTestContext.Companion.fileList
import androidx.build.AndroidXSelfTestProject.Companion.cubaneBuildGradleText
import androidx.build.AndroidXSelfTestProject.Companion.cubaneKmpNoJavaProject
import androidx.build.AndroidXSelfTestProject.Companion.cubaneProject
import net.saff.checkmark.Checkmark
import net.saff.checkmark.Checkmark.Companion.check
import org.junit.Test

class AndroidXPluginTest {
    @Test
    fun createZipForSimpleProject() = pluginTest {
        writeBuildFiles(cubaneProject)

        val output = runGradle(":cubane:cubane:createProjectZip", "--stacktrace").output
        Checkmark.checks {
            output.check { it.contains("BUILD SUCCESSFUL") }
            outDir.check { it.fileList().contains("dist") }
            outDir.resolve("dist/per-project-zips").fileList()
                .check { it.contains("cubane-cubane-all-0-1.2.3.zip") }
        }
    }

    @Test
    fun variants() = pluginTest {
        writeBuildFiles(cubaneProject)
        runGradle(":cubane:cubane:outgoingVariants", "--stacktrace").output.check {
            it.contains("Variant sourcesElements")
        }
    }

    @Test
    fun publishWithNoJavaWithMultiplatformFlag() = pluginTest {
        // Make sure we generate a sourceJar without invoking the java plugin
        writeBuildFiles(cubaneKmpNoJavaProject)
        cubaneKmpNoJavaProject.publishMavenLocal(
            "-Pandroidx.compose.multiplatformEnabled=true"
        )
    }

    @Test
    fun androidLibraryAndKotlinAndroid() = pluginTest {
        val project = cubaneProject.copy(
            buildGradleText = cubaneBuildGradleText(
                listOf("com.android.library", "kotlin-android")
            )
        )
        writeBuildFiles(project)
        project.checkConfigurationSucceeds()
    }

    @Test
    fun kotlinAndroidAndAndroidLibrary() = pluginTest {
        val project = cubaneProject.copy(
            buildGradleText = cubaneBuildGradleText(
                listOf("kotlin-android", "com.android.library")
            )
        )
        writeBuildFiles(project)
        project.checkConfigurationSucceeds()
    }

    @Test
    fun androidSampleApplicationWithoutVersion() = pluginTest {
        val project = cubaneProject.copy(
            buildGradleText = cubaneBuildGradleText(
                pluginsBeforeAndroidX = listOf(
                    "com.android.application",
                    "org.jetbrains.kotlin.android"
                ),
                version = null
            )
        )
        writeBuildFiles(project)
        project.checkConfigurationSucceeds()
    }
}