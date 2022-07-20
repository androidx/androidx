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
import androidx.build.AndroidXSelfTestProject.Companion.cubaneKmpProject
import androidx.build.AndroidXSelfTestProject.Companion.cubaneProject
import net.saff.checkmark.Checkmark.Companion.check
import org.junit.Assert
import org.junit.Test

class AndroidXRootPluginTest {
    @Test
    fun rootProjectConfigurationHasAndroidXTasks() = pluginTest {
        writeBuildFiles()

        // Check that our classpath jars exist
        Assert.assertTrue(buildJars.privateJar.path, buildJars.privateJar.exists())

        // --stacktrace gives more details on failure.
        runGradle(
            "tasks",
            "--stacktrace",
            "-P$ALLOW_MISSING_LINT_CHECKS_PROJECT=true"
        ).output.check {
            it.contains("listAndroidXProperties - Lists AndroidX-specific properties")
        }
    }

    @Test
    fun unzipSourcesWithNoProjects() = pluginTest {
        setupDocsProjects()

        runGradle(":docs-public:unzipSourcesForDackka", "--stacktrace").output.check {
            it.contains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun unzipSourcesWithCubane() = pluginTest {
        setupDocsProjects(cubaneProject)
        cubaneProject.publishMavenLocal()
        runGradle(":docs-public:unzipSourcesForDackka", "--stacktrace").output.check {
            it.contains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun unzipSourcesWithCubaneKmp() = pluginTest {
        setupDocsProjects(cubaneKmpProject)

        runGradle(":cubane:cubanekmp:tasks", "--all", "--stacktrace").output.check {
            it.contains("publishJvmPublicationToMavenLocal")
        }

        cubaneKmpProject.publishMavenLocal()

        // Make sure we are using metadata, not pom (unfortunately, if we use pom, and don't check,
        // this test passes without testing the things we actually want to know)
        // (See b/230396269)
        cubaneKmpProject.readPublishedFile("cubanekmp-1.2.3.pom")
            .check { it.contains("do_not_remove: published-with-gradle-metadata") }

        cubaneKmpProject.readPublishedFile("cubanekmp-1.2.3.module")
            .check { it.contains("\"org.gradle.docstype\": \"sources\"") }

        runGradle(":docs-public:unzipSourcesForDackka", "--stacktrace").output.check {
            it.contains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun testSaveMavenFoldersForDebugging() = pluginTest {
        setupDocsProjects(cubaneProject)
        cubaneProject.publishMavenLocal()

        val where = tmpFolder.newFolder()
        saveMavenFoldersForDebugging(where.path)
        where.fileList().check { it.contains("cubane") }
    }

    @Test
    fun testPublishNoDuplicates() = pluginTest {
        setupDocsProjects(cubaneKmpProject)

        runGradle(":cubane:cubanekmp:publish", "--stacktrace").output.check {
            // If we have overlapping publications, it will print an error message like
            // """
            // Multiple publications with coordinates
            // 'androidx.collection:collection:1.3.0-alpha01' are published to repository 'maven'.
            // The publications 'kotlinMultiplatform' in project ':collection:collection' and
            // 'sourcesMaven' in project ':collection:collection' will overwrite each other!
            // """
            !it.contains("will overwrite each other")
        }
    }

    private fun AndroidXPluginTestContext.setupDocsProjects(
        vararg projects: AndroidXSelfTestProject
    ) {
        val sourceDependencies = projects.map { it.sourceCoordinate }
        val docsPublicBuildGradle =
            """|plugins {
               |  id("com.android.library")
               |}
               |
               |// b/233089408: would prefer to use plugins { id } syntax, but work around.
               |apply plugin: androidx.build.docs.AndroidXDocsImplPlugin
               |
               |repositories {
               |  mavenLocal()
               |}
               |
               |dependencies {
               |  ${sourceDependencies.joinToString("\n") { "docs(\"$it\")" }}
               |}
               |""".trimMargin()

        val docsPublicProject = AndroidXSelfTestProject(
            groupId = "docs-public",
            artifactId = null,
            version = null,
            buildGradleText = docsPublicBuildGradle
        )
        val fakeAnnotations = AndroidXSelfTestProject(
            groupId = "fakeannotations",
            artifactId = null,
            version = null,
            buildGradleText = ""
        )
        writeBuildFiles(projects.toList() + listOf(docsPublicProject, fakeAnnotations))
    }
}