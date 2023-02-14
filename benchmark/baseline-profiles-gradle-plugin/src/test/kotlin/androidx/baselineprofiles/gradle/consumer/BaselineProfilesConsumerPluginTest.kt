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

package androidx.baselineprofiles.gradle.consumer

import androidx.baselineprofiles.gradle.utils.CONFIGURATION_NAME_BASELINE_PROFILES
import androidx.baselineprofiles.gradle.utils.camelCase
import androidx.testutils.gradle.ProjectSetupRule
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BaselineProfilesConsumerPluginTest {

    // To test the consumer plugin we need a module that exposes a baselineprofiles configuration
    // to be consumed. This is why we'll be using 2 projects. The producer project build gradle
    // is generated ad hoc in the tests that require it in order to supply mock profiles.

    private val rootFolder = TemporaryFolder().also { it.create() }

    @get:Rule
    val consumerProjectSetup = ProjectSetupRule(rootFolder.root)

    @get:Rule
    val producerProjectSetup = ProjectSetupRule(rootFolder.root)

    private lateinit var consumerModuleName: String
    private lateinit var producerModuleName: String
    private lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        consumerModuleName = consumerProjectSetup.rootDir.relativeTo(rootFolder.root).name
        producerModuleName = producerProjectSetup.rootDir.relativeTo(rootFolder.root).name

        rootFolder.newFile("settings.gradle").writeText(
            """
            include '$consumerModuleName'
            include '$producerModuleName'
        """.trimIndent()
        )
        gradleRunner = GradleRunner.create()
            .withProjectDir(consumerProjectSetup.rootDir)
            .withPluginClasspath()
    }

    @Test
    fun testGenerateBaselineProfilesTaskWithNoFlavors() {
        consumerProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.library")
                    id("androidx.baselineprofiles.consumer")
                }
                android {
                    namespace 'com.example.namespace'
                }
                dependencies {
                    baselineprofiles(project(":$producerModuleName"))
                }
            """.trimIndent(),
            suffix = ""
        )
        producerProjectSetup.writeDefaultBuildGradle(
            prefix = MockProducerBuildGrade()
                .withConfiguration(flavor = "", buildType = "release")
                .withProducedBaselineProfiles(listOf("3", "2"), flavor = "", buildType = "release")
                .withProducedBaselineProfiles(listOf("4", "1"), flavor = "", buildType = "release")
                .build(),
            suffix = ""
        )

        gradleRunner
            .withArguments("generateBaselineProfiles", "--stacktrace")
            .build()

        // The expected output should have each line sorted descending
        assertThat(
            File(consumerProjectSetup.rootDir, "src/main/baseline-prof.txt").readLines()
        )
            .containsExactly("4", "3", "2", "1")
    }

    @Test
    fun testGenerateBaselineProfilesTaskWithFlavors() {
        consumerProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofiles.consumer")
                }
                android {
                    namespace 'com.example.namespace'
                    productFlavors {
                        flavorDimensions = ["version"]
                        free {
                            dimension "version"
                        }
                        paid {
                            dimension "version"
                        }
                    }
                }
                dependencies {
                    baselineprofiles(project(":$producerModuleName"))
                }
            """.trimIndent(),
            suffix = ""
        )
        producerProjectSetup.writeDefaultBuildGradle(
            prefix = MockProducerBuildGrade()
                .withConfiguration(flavor = "free", buildType = "release")
                .withConfiguration(flavor = "paid", buildType = "release")
                .withProducedBaselineProfiles(
                    listOf("3", "2"),
                    flavor = "free",
                    buildType = "release"
                )
                .withProducedBaselineProfiles(
                    listOf("4", "1"),
                    flavor = "paid",
                    buildType = "release"
                )
                .build(),
            suffix = ""
        )

        gradleRunner
            .withArguments("generateBaselineProfiles", "--stacktrace")
            .build()

        // The expected output should have each line sorted ascending
        val baselineProf =
            File(consumerProjectSetup.rootDir, "src/main/baseline-prof.txt").readLines()
        assertThat(baselineProf).containsExactly("1", "2", "3", "4")
    }
}

private class MockProducerBuildGrade() {

    private var profileIndex = 0
    private var content = """
        plugins { id("com.android.library") }
        android { namespace 'com.example.namespace' }

        // This task produces a file with a fixed output
        abstract class TestProfileTask extends DefaultTask {
            @Input abstract Property<String> getFileContent()
            @OutputFile abstract RegularFileProperty getOutputFile()
            @TaskAction void exec() { getOutputFile().get().asFile.write(getFileContent().get()) }
        }

    """.trimIndent()

    fun withConfiguration(flavor: String, buildType: String): MockProducerBuildGrade {

        content += """

        configurations {
            ${configurationName(flavor, buildType)} {
                canBeConsumed = true
                canBeResolved = false
                attributes {
                    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, "baselineprofile"))
                    attribute(Attribute.of("androidx.baselineprofiles.gradle.attributes.BuildType", String), "$buildType")
                    attribute(Attribute.of("androidx.baselineprofiles.gradle.attributes.Flavor", String), "$flavor")
                }
            }
        }

        """.trimIndent()
        return this
    }

    fun withProducedBaselineProfiles(
        lines: List<String>,
        flavor: String = "",
        buildType: String
    ): MockProducerBuildGrade {
        profileIndex++
        content += """

        def task$profileIndex = tasks.register('testProfile$profileIndex', TestProfileTask)
        task$profileIndex.configure {
            it.outputFile.set(project.layout.buildDirectory.file("test$profileIndex"))
            it.fileContent.set(${"\"\"\"${lines.joinToString("\n")}\"\"\""})
        }
        artifacts {
            add("${configurationName(flavor, buildType)}", task$profileIndex.map { it.outputFile })
        }

        """.trimIndent()
        return this
    }

    fun build() = content
}

private fun configurationName(flavor: String, buildType: String): String =
    camelCase(flavor, buildType, CONFIGURATION_NAME_BASELINE_PROFILES)
