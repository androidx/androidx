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
    fun buildInfoDependencies_includesAndroidXTestUiAutomator_andExcludesOtherAndroidXTestGroupsAndNonAndroidXGroups() {
        var deps: List<ModuleDependency> =
            listOf(
                DefaultExternalModuleDependency("androidx.test.uiautomator", "artifact", "version")
            )
        deps
            .asBuildInfoDependencies()
            .single()
            .check { it.groupId == "androidx.test.uiautomator" }
            .check { it.artifactId == "artifact" }
            .check { it.version == "version" }
            .check { !it.isTipOfTree }

        deps = listOf(DefaultExternalModuleDependency("androidx.test.ext", "artifact", "version"))
        assertThat(deps.asBuildInfoDependencies()).isEmpty()

        deps =
            listOf(
                DefaultExternalModuleDependency(
                    "androidx.test.integration-tests",
                    "artifact",
                    "version",
                )
            )
        assertThat(deps.asBuildInfoDependencies()).isEmpty()

        deps =
            listOf(
                DefaultExternalModuleDependency("androidx.test.screenshot", "artifact", "version")
            )
        assertThat(deps.asBuildInfoDependencies()).isEmpty()

        deps =
            listOf(DefaultExternalModuleDependency("androidx.databinding", "artifact", "version"))
        assertThat(deps.asBuildInfoDependencies()).isEmpty()

        deps = listOf(DefaultExternalModuleDependency("androidx.media3", "artifact", "version"))
        assertThat(deps.asBuildInfoDependencies()).isEmpty()

        deps = listOf(DefaultExternalModuleDependency("google.guava", "artifact", "version"))
        assertThat(deps.asBuildInfoDependencies()).isEmpty()
    }

    @Test
    fun suffix() {
        computeTaskSuffix(variantName = "cubane", isKmp = false).check { it == "" }
        computeTaskSuffix(variantName = "kotlinMultiplatform", isKmp = true).check { it == "" }
        computeTaskSuffix(variantName = "jvm", isKmp = true).check { it == "Jvm" }
        computeTaskSuffix(variantName = "jvm-linux-x64", isKmp = true).check { it == "JvmLinuxX64" }
    }

    @Test
    fun buildInfoTaskCreatesSimpleFile() {
        setupBuildInfoProject()
        gradleRunner
            .withArguments("createLibraryBuildInfoFiles", "--no-configuration-cache")
            .build()

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
        assertThat(buildInfo.kmpChildren)
            .isEqualTo(setOf("android", "jvm", "jvmstubs", "linuxx64stubs", "wasm-js"))
    }

    @Test
    fun buildInfoTaskCreatesSimpleFileWithAllDependencies() {
        setupBuildInfoProjectWithAllDependencies()
        gradleRunner
            .withArguments("createLibraryBuildInfoFiles", "--no-configuration-cache")
            .build()

        val buildInfoFile =
            distDir.root.resolve("build-info/androidx.build_info_test_test_build_info.txt")
        assertThat(buildInfoFile.exists()).isTrue()

        val buildInfo = parseBuildInfo(buildInfoFile)

        assertThat(buildInfo.allDependencies.map { "${it.groupId}:${it.artifactId}" })
            .containsExactly(
                "androidx.activity:activity",
                "androidx.annotation:annotation",
                "androidx.annotation:annotation-experimental",
                "androidx.appcompat:appcompat",
                "androidx.appcompat:appcompat-resources",
                "androidx.arch.core:core-common",
                "androidx.arch.core:core-runtime",
                "androidx.collection:collection",
                "androidx.core:core",
                "androidx.cursoradapter:cursoradapter",
                "androidx.customview:customview",
                "androidx.drawerlayout:drawerlayout",
                "androidx.fragment:fragment",
                "androidx.interpolator:interpolator",
                "androidx.lifecycle:lifecycle-common",
                "androidx.lifecycle:lifecycle-livedata",
                "androidx.lifecycle:lifecycle-livedata-core",
                "androidx.lifecycle:lifecycle-runtime",
                "androidx.lifecycle:lifecycle-viewmodel",
                "androidx.lifecycle:lifecycle-viewmodel-savedstate",
                "androidx.loader:loader",
                "androidx.savedstate:savedstate",
                "androidx.tracing:tracing",
                "androidx.vectordrawable:vectordrawable",
                "androidx.vectordrawable:vectordrawable-animated",
                "androidx.versionedparcelable:versionedparcelable",
                "androidx.viewpager:viewpager",
                "org.jetbrains:annotations",
                "org.jetbrains.kotlin:kotlin-stdlib",
            )
            .inOrder()
    }

    @Test
    fun buildInfoSelectsCorrectKmpVariant() {
        setupBuildInfoProjectWithKmpDependency()
        gradleRunner
            .withArguments("createLibraryBuildInfoFiles", "--no-configuration-cache")
            .build()

        val buildInfoFile =
            distDir.root.resolve("build-info/androidx.build_info_test_test_build_info.txt")
        assertThat(buildInfoFile.exists()).isTrue()

        val buildInfo = parseBuildInfo(buildInfoFile)

        assertThat(buildInfo.allDependencies.map { "${it.groupId}:${it.artifactId}" })
            .containsExactly(
                "androidx.annotation:annotation",
                "androidx.annotation:annotation-jvm",
                "androidx.collection:collection",
                "androidx.collection:collection-jvm",
                "org.jetbrains:annotations",
                "org.jetbrains.kotlin:kotlin-stdlib",
            )
            .inOrder()
    }

    @Test
    fun buildInfoTaskAddsTestModuleNames() {
        setupBuildInfoProject()
        gradleRunner
            .withArguments("createLibraryBuildInfoFiles", "--no-configuration-cache")
            .build()

        val buildInfoFile =
            distDir.root.resolve("build-info/androidx.build_info_test_test_build_info.txt")
        assertThat(buildInfoFile.exists()).isTrue()

        val buildInfo = parseBuildInfo(buildInfoFile)

        assertThat(buildInfo.testModuleNames).containsExactly("test.xml")
    }

    @Test
    fun buildInfoTaskWithSuffixSkipsTestModuleNames() {
        setupBuildInfoProjectForArtifactWithSuffix()
        gradleRunner
            .withArguments("createLibraryBuildInfoFiles", "--no-configuration-cache")
            .build()

        val buildInfoFile =
            distDir.root.resolve("build-info/androidx.build_info_test_test-jvm_build_info.txt")
        assertThat(buildInfoFile.exists()).isTrue()

        val buildInfo = parseBuildInfo(buildInfoFile)

        assertThat(buildInfo.testModuleNames).isEmpty()
    }

    @Test
    fun hasApplePlatform_withAtLeastOnePlatformIdentifierTargetingAnApplePlatform_returnsTrue() {
        val platforms =
            setOf(PlatformIdentifier.ANDROID, PlatformIdentifier.IOS_ARM_64, PlatformIdentifier.JVM)
        assertThat(hasApplePlatform(platforms)).isTrue()
    }

    @Test
    fun hasApplePlatform_withNoPlatformIdentifiersTargetingAnApplePlatform_returnsFalse() {
        val platforms = setOf(PlatformIdentifier.ANDROID, PlatformIdentifier.JVM)
        assertThat(hasApplePlatform(platforms)).isFalse()
    }

    private fun setupBuildInfoProject() {
        // Set the project name to be equal to artifact id, so that it is not adding a suffix to
        // the task name.
        File(projectSetup.rootDir, "settings.gradle").writeText("rootProject.name = \"test\"")
        projectSetup.writeDefaultBuildGradle(
            prefix =
                """
                import androidx.build.buildInfo.CreateLibraryBuildInfoFileTaskKt
                plugins {
                    id("com.android.library")
                    id("maven-publish")
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
                    publishing {
                        singleVariant('release') { }
                    }
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
                            // This test is set up such that putting `it.artifactId` in a provider
                            // directly means that the `MavenPublication` object no longer exists when
                            // the provider is evaluated.
                            def artifactId = it.artifactId
                            CreateLibraryBuildInfoFileTaskKt.createBuildInfoTask(
                                project,
                                it,
                                null,
                                project.provider { artifactId },
                                project.provider { "fakeSha" },
                                project.provider { false },
                                false,
                                "androidx",
                                ["android", "jvm", "jvmStubs", "linuxx64Stubs", "wasmJs"].toSet(),
                                project.provider { ["test.xml"] },
                                it.name,
                            )
                        }
                    }
                }
                """
                    .trimIndent(),
        )
    }

    private fun setupBuildInfoProjectWithAllDependencies() {
        File(projectSetup.rootDir, "settings.gradle").writeText("rootProject.name = \"test\"")
        projectSetup.writeDefaultBuildGradle(
            prefix =
                """
            import androidx.build.buildInfo.CreateLibraryBuildInfoFileTaskKt
            plugins {
                id("com.android.library")
                id("maven-publish")
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
                    implementation("androidx.appcompat:appcompat:1.3.0")
                }
                android {
                    namespace 'androidx.build_info'
                    publishing {
                        singleVariant('release') { }
                    }
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
                            def artifactId = it.artifactId
                            CreateLibraryBuildInfoFileTaskKt.createBuildInfoTask(
                                project,
                                it,
                                null,
                                project.provider { artifactId },
                                project.provider { "fakeSha" },
                                project.provider { false },
                                false,
                                "androidx",
                                ["android", "jvm", "jvmStubs", "linuxx64Stubs", "wasmJs"].toSet(),
                                project.provider { ["test.xml"] },
                                it.name
                            )
                        }
                    }
                }
                """
                    .trimIndent(),
        )
    }

    private fun setupBuildInfoProjectWithKmpDependency() {
        File(projectSetup.rootDir, "settings.gradle").writeText("rootProject.name = \"test\"")
        projectSetup.writeDefaultBuildGradle(
            prefix =
                """
            import androidx.build.buildInfo.CreateLibraryBuildInfoFileTaskKt
            plugins {
                id("com.android.library")
                id("maven-publish")
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
                    implementation("androidx.collection:collection:1.5.0")
                }
                android {
                    namespace 'androidx.build_info'
                    publishing {
                        singleVariant('release') { }
                    }
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
                            def artifactId = it.artifactId
                            CreateLibraryBuildInfoFileTaskKt.createBuildInfoTask(
                                project,
                                it,
                                null,
                                project.provider { artifactId },
                                project.provider { "fakeSha" },
                                project.provider { false },
                                false,
                                "androidx",
                                [].toSet(),
                                project.provider { [] },
                                it.name
                            )
                        }
                    }
                }
                """
                    .trimIndent(),
        )
    }

    private fun setupBuildInfoProjectForArtifactWithSuffix() {
        File(projectSetup.rootDir, "settings.gradle").writeText("rootProject.name = \"test\"")
        projectSetup.writeDefaultBuildGradle(
            prefix =
                """
                import androidx.build.buildInfo.CreateLibraryBuildInfoFileTaskKt
                plugins {
                    id("com.android.library")
                    id("maven-publish")
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
                    publishing {
                        singleVariant('release') { }
                    }
                }
                group = "androidx.build_info_test"
                afterEvaluate {
                    publishing {
                        publications {
                            maven(MavenPublication) {
                                groupId = 'androidx.build_info_test'
                                artifactId = 'test-jvm'
                                version = '0.0.1'
                                from(components.release)
                            }
                        }
                        publications.withType(MavenPublication) {
                            // This test is set up such that putting `it.artifactId` in a provider
                            // directly means that the `MavenPublication` object no longer exists when
                            // the provider is evaluated.
                            def artifactId = it.artifactId
                            CreateLibraryBuildInfoFileTaskKt.createBuildInfoTask(
                                project,
                                it,
                                null,
                                project.provider { artifactId },
                                project.provider { "fakeSha" },
                                project.provider { false }, // shouldPublishDocs
                                true, // isKmp
                                "androidx",
                                ["android", "jvm"].toSet(),
                                project.provider { ["test.xml"] },
                                it.name,
                            )
                        }
                    }
                }
                """
                    .trimIndent(),
        )
    }

    private fun parseBuildInfo(buildInfoFile: File): LibraryBuildInfoFile {
        val gson = Gson()
        val contents = buildInfoFile.readText(Charsets.UTF_8)
        return gson.fromJson(contents, LibraryBuildInfoFile::class.java)
    }
}
