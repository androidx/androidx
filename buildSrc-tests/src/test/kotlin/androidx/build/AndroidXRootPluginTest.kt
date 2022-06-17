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

import java.io.File
import net.saff.checkmark.Checkmark.Companion.check
import net.saff.checkmark.Checkmark.Companion.checks
import org.junit.Assert
import org.junit.Test

class AndroidXRootPluginTest {
    @Test
    fun rootProjectConfigurationHasAndroidXTasks() = pluginTest {
        writeRootSettingsFile()
        writeRootBuildFile()
        Assert.assertTrue(privateJar.path, privateJar.exists())

        // --stacktrace gives more details on failure.
        runGradle("tasks", "--stacktrace").output.check {
            it.contains("listAndroidXProperties - Lists AndroidX-specific properties")
        }
    }

    @Test
    fun createZipForSimpleProject() = pluginTest {
        writeRootSettingsFile(":cubane:cubane")
        writeRootBuildFile()

        File(supportRoot, "libraryversions.toml").writeText(
            """|[groups]
               |[versions]
               |""".trimMargin()
        )

        val projectFolder = setup.rootDir.resolve("cubane/cubane")

        checks {
            projectFolder.mkdirs()

            // TODO(b/233089408): avoid full path for androidx.build.AndroidXImplPlugin
            File(projectFolder, "build.gradle").writeText(
                """|import androidx.build.LibraryGroup
                   |import androidx.build.Publish
                   |import androidx.build.Version
                   |
                   |plugins {
                   |  // b/233089408: would prefer to use this syntax, but it fails
                   |  // id("AndroidXPlugin")
                   |  id("java-library")
                   |  id("kotlin")
                   |}
                   |
                   |// Workaround for b/233089408
                   |apply plugin: androidx.build.AndroidXImplPlugin
                   |
                   |dependencies {
                   |  api(libs.kotlinStdlib)
                   |}
                   |
                   |androidx {
                   |  publish = Publish.SNAPSHOT_AND_RELEASE
                   |  mavenVersion = new Version("1.2.3")
                   |  mavenGroup = new LibraryGroup("cubane", null)
                   |}
                   |""".trimMargin()
            )

            val output = runGradle(
                ":cubane:cubane:createProjectZip",
                "--stacktrace",
                "-P$ALLOW_MISSING_LINT_CHECKS_PROJECT=true"
            ).output
            checks {
                output.check { it.contains("BUILD SUCCESSFUL") }
                outDir.check { it.fileList().contains("dist") }
                outDir.resolve("dist/per-project-zips").fileList()
                    .check { it.contains("cubane-cubane-all-0-1.2.3.zip") }
            }
        }
    }

    private fun File.fileList() = list()!!.toList()
}