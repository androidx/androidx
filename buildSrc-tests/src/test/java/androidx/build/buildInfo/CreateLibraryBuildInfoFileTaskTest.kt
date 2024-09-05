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

package androidx.build.buildInfo

import androidx.build.PlatformIdentifier
import androidx.build.buildInfo.CreateLibraryBuildInfoFileTask.Companion.asBuildInfoDependencies
import androidx.build.jetpad.LibraryBuildInfoFile
import androidx.testutils.gradle.ProjectSetupRule
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import java.io.File
import net.saff.checkmark.Checkmark.Companion.check
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CreateLibraryBuildInfoFileTaskTest {
    @get:Rule val distDir = TemporaryFolder()

    @get:Rule val projectSetup = ProjectSetupRule()
    private lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        gradleRunner =
            GradleRunner.create()
                .withProjectDir(projectSetup.rootDir)
                .withPluginClasspath()
                .withEnvironment(mapOf("DIST_DIR" to distDir.root.absolutePath))
    }

    @Test
    fun buildInfoDependencies() {
        val deps: List<ModuleDependency> =
            listOf(DefaultExternalModuleDependency("androidx.group", "artifact", "version"))
        deps
            .asBuildInfoDependencies()
            .single()
            .check { it.groupId == "androidx.group" }
            .check { it.artifactId == "artifact" }
            .check { it.version == "version" }
            .check { !it.isTipOfTree }
    }

    @Test
    fun suffix() {
        computeTaskSuffix(projectName = "cubane", artifactId = "cubane").check { it == "" }
        computeTaskSuffix(projectName = "cubane", artifactId = "cubane-jvm").check { it == "Jvm" }
        computeTaskSuffix(projectName = "cubane", artifactId = "cubane-jvm-linux-x64").check {
            it == "JvmLinuxX64"
        }
    }

    @Test
    fun buildInfoTaskCreatesSimpleFile() {
        setupBuildInfoProject()
        gradleRunner.withArguments("createLibraryBuildInfoFiles").build()

        val buildInfoFile =
            distDir.root.resolve("build-info/androidx.build_info_test_test_build_info.txt")
        assertThat(buildInfoFile.exists()).isTrue()

        val buildInfo = parseBuildInfo(buildInfoFile)

        assertThat(buildInfo.groupId).isEqualTo("androidx.build_info_test")
        assertThat(buildInfo.artifactId).isEqualTo("test")
        assertThat(buildInfo.version).isEqualTo("0.0.1")
        assertThat(buildInfo.kotlinVersion).isEqualTo(projectSetup.props.kgpVersion)
        assertThat(buildInfo.groupIdRequiresSameVersion).isFalse()
        assertThat(buildInfo.dependencies).hasSize(1)
        assertThat(buildInfo.dependencies.single().groupId).isEqualTo("androidx.core")
        assertThat(buildInfo.dependencies.single().artifactId).isEqualTo("core")
        assertThat(buildInfo.dependencyConstraints).hasSize(1)
        assertThat(buildInfo.dependencyConstraints.single().groupId).isEqualTo("androidx.core")
        assertThat(buildInfo.dependencyConstraints.single().artifactId).isEqualTo("core-ktx")
        assertThat(buildInfo.shouldPublishDocs).isFalse()
        assertThat(buildInfo.isKmp).isFalse()
        assertThat(buildInfo.target).isEqualTo("androidx")
    }

    @Test
    fun hasApplePlatform_withAtLeastOnePlatformIdentifierTargetingAnApplePlatform_returnsTrue() {
        val platforms =
            setOf(
                PlatformIdentifier.ANDROID,
                PlatformIdentifier.IOS_ARM_64,
                PlatformIdentifier.JVM,
            )
        assertThat(hasApplePlatform(platforms)).isTrue()
    }

    @Test
    fun hasApplePlatform_withNoPlatformIdentifiersTargetingAnApplePlatform_returnsFalse() {
        val platforms =
            setOf(
                PlatformIdentifier.ANDROID,
                PlatformIdentifier.JVM,
            )
        assertThat(hasApplePlatform(platforms)).isFalse()
    }

    private fun setupBuildInfoProject() {
        projectSetup.writeDefaultBuildGradle(
            prefix =
                """
                import androidx.build.buildInfo.CreateLibraryBuildInfoFileTaskKt
                plugins {
                    id("com.android.library")
                    id("maven-publish")
                    id("kotlin-android")
                }
                ext {
                    supportRootFolder = new File("${projectSetup.rootDir}")
                }
            """
                    .trimIndent(),
            suffix =
                """
            version = "0.0.1"
            dependencies {
                constraints {
                    implementation("androidx.core:core-ktx:1.1.0")
                }
                implementation("androidx.core:core:1.1.0")
            }
            android {
                 namespace 'androidx.build_info'
            }
            group = "androidx.build_info_test"
            afterEvaluate {
                publishing {
                    publications {
                        maven(MavenPublication) {
                            groupId = 'androidx.build_info_test'
                            artifactId = 'test'
                            version = '0.0.1'
                            from(components.release)
                        }
                    }
                    publications.withType(MavenPublication) {
                        CreateLibraryBuildInfoFileTaskKt.createBuildInfoTask(
                            project,
                            it,
                            null,
                            it.artifactId,
                            project.provider { "fakeSha" },
                            false,
                            false,
                            "androidx"
                        )
                    }
                }
            }
            """
                    .trimIndent()
        )
    }

    fun parseBuildInfo(buildInfoFile: File): LibraryBuildInfoFile {
        val gson = Gson()
        val contents = buildInfoFile.readText(Charsets.UTF_8)
        return gson.fromJson(contents, LibraryBuildInfoFile::class.java)
    }
}
