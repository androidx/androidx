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

package androidx.baselineprofile.gradle.consumer

import androidx.baselineprofile.gradle.utils.CONFIGURATION_NAME_BASELINE_PROFILES
import androidx.baselineprofile.gradle.utils.GRADLE_CODE_PRINT_TASK
import androidx.baselineprofile.gradle.utils.buildAndAssertThatOutput
import androidx.baselineprofile.gradle.utils.camelCase
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
class BaselineProfileConsumerPluginTest {

    // To test the consumer plugin we need a module that exposes a baselineprofile configuration
    // to be consumed. This is why we'll be using 2 projects. The producer project build gradle
    // is generated ad hoc in the tests that require it in order to supply mock profiles.

    companion object {
        private const val expectedBaselineProfileOutputFolder = "generated/baselineProfiles"
    }

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

    private fun writeDefaultProducerProject() {
        producerProjectSetup.writeDefaultBuildGradle(
            prefix = MockProducerBuildGrade()
                .withConfiguration(flavor = "", buildType = "release")
                .withProducedBaselineProfile(
                    lines = listOf(
                        Fixtures.CLASS_1_METHOD_1,
                        Fixtures.CLASS_2_METHOD_2,
                        Fixtures.CLASS_2,
                        Fixtures.CLASS_1,
                    ),
                    flavor = "",
                    buildType = "release"
                )
                .build(),
            suffix = ""
        )
    }

    private fun readBaselineProfileFileContent(variantName: String) =
        File(
            consumerProjectSetup.rootDir,
            "src/$variantName/$expectedBaselineProfileOutputFolder/baseline-prof.txt"
        ).readLines()

    @Test
    fun testGenerateTaskWithNoFlavors() {
        consumerProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.library")
                    id("androidx.baselineprofile.consumer")
                }
                android {
                    namespace 'com.example.namespace'
                }
                dependencies {
                    baselineProfile(project(":$producerModuleName"))
                }
            """.trimIndent(),
            suffix = ""
        )
        producerProjectSetup.writeDefaultBuildGradle(
            prefix = MockProducerBuildGrade()
                .withConfiguration(flavor = "", buildType = "release")
                .withProducedBaselineProfile(
                    lines = listOf(
                        Fixtures.CLASS_1_METHOD_1,
                        Fixtures.CLASS_1,
                    ),
                    flavor = "",
                    buildType = "release"
                )
                .withProducedBaselineProfile(
                    lines = listOf(
                        Fixtures.CLASS_2_METHOD_1,
                        Fixtures.CLASS_2,
                    ),
                    flavor = "",
                    buildType = "release"
                )
                .build(),
            suffix = ""
        )

        gradleRunner
            .withArguments("generateBaselineProfile", "--stacktrace")
            .build()

        assertThat(readBaselineProfileFileContent("main"))
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
            )
    }

    @Test
    fun testGenerateTaskWithFlavorsAndDefaultMerge() {
        consumerProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofile.consumer")
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
                    baselineProfile(project(":$producerModuleName"))
                }
            """.trimIndent(),
            suffix = ""
        )
        producerProjectSetup.writeDefaultBuildGradle(
            prefix = MockProducerBuildGrade()
                .withConfiguration(flavor = "free", buildType = "release")
                .withConfiguration(flavor = "paid", buildType = "release")
                .withProducedBaselineProfile(
                    lines = listOf(
                        Fixtures.CLASS_1_METHOD_1,
                        Fixtures.CLASS_1,
                    ),
                    flavor = "free",
                    buildType = "release"
                )
                .withProducedBaselineProfile(
                    lines = listOf(
                        Fixtures.CLASS_2_METHOD_1,
                        Fixtures.CLASS_2,
                    ),
                    flavor = "paid",
                    buildType = "release"
                )
                .build(),
            suffix = ""
        )

        // Asserts that all per-variant, per-flavor and per-build type tasks are being generated.
        gradleRunner
            .withArguments("tasks", "--stacktrace")
            .build()
            .output
            .also {
                assertThat(it).contains("generateReleaseBaselineProfile - ")
                assertThat(it).contains("generateFreeReleaseBaselineProfile - ")
                assertThat(it).contains("generatePaidReleaseBaselineProfile - ")
            }

        gradleRunner
            .withArguments("generateReleaseBaselineProfile", "--stacktrace")
            .build()

        assertThat(readBaselineProfileFileContent("freeRelease"))
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
            )

        assertThat(readBaselineProfileFileContent("paidRelease"))
            .containsExactly(
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
            )
    }

    @Test
    fun testGenerateTaskWithFlavorsAndMergeAll() {
        consumerProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofile.consumer")
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
                    baselineProfile(project(":$producerModuleName"))
                }
                baselineProfile {
                    mergeIntoMain = true
                }
            """.trimIndent(),
            suffix = ""
        )
        producerProjectSetup.writeDefaultBuildGradle(
            prefix = MockProducerBuildGrade()
                .withConfiguration(flavor = "free", buildType = "release")
                .withConfiguration(flavor = "paid", buildType = "release")
                .withProducedBaselineProfile(
                    lines = listOf(
                        Fixtures.CLASS_1_METHOD_1,
                        Fixtures.CLASS_1,
                    ),
                    flavor = "free",
                    buildType = "release"
                )
                .withProducedBaselineProfile(
                    lines = listOf(
                        Fixtures.CLASS_2_METHOD_1,
                        Fixtures.CLASS_2,
                    ),
                    flavor = "paid",
                    buildType = "release"
                )
                .build(),
            suffix = ""
        )

        // Asserts that all per-variant, per-flavor and per-build type tasks are being generated.
        gradleRunner
            .withArguments("tasks", "--stacktrace")
            .build()
            .output
            .also {
                assertThat(it).contains("generateBaselineProfile - ")
                assertThat(it).contains("generateReleaseBaselineProfile - ")
                assertThat(it).doesNotContain("generateFreeReleaseBaselineProfile - ")
                assertThat(it).doesNotContain("generatePaidReleaseBaselineProfile - ")
            }

        gradleRunner
            .withArguments("generateBaselineProfile", "--stacktrace")
            .build()

        assertThat(readBaselineProfileFileContent("main"))
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
            )
    }

    @Test
    fun testPluginAppliedToApplicationModule() {

        // For this test the producer is not important
        writeDefaultProducerProject()

        consumerProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofile.consumer")
                }
                android {
                    namespace 'com.example.namespace'
                }
                dependencies {
                    baselineProfile(project(":$producerModuleName"))
                }
            """.trimIndent(),
            suffix = ""
        )

        gradleRunner
            .withArguments("generateReleaseBaselineProfile", "--stacktrace")
            .build()

        // This should not fail.
    }

    @Test
    fun testPluginAppliedToLibraryModule() {
        // For this test the producer is not important
        writeDefaultProducerProject()

        consumerProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.library")
                    id("androidx.baselineprofile.consumer")
                }
                android {
                    namespace 'com.example.namespace'
                }
                dependencies {
                    baselineProfile(project(":$producerModuleName"))
                }
            """.trimIndent(),
            suffix = ""
        )

        gradleRunner
            .withArguments("generateBaselineProfile", "--stacktrace")
            .build()

        // This should not fail.
    }

    @Test
    fun testPluginAppliedToNonApplicationAndNonLibraryModule() {
        // For this test the producer is not important
        writeDefaultProducerProject()

        consumerProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.test")
                    id("androidx.baselineprofile.consumer")
                }
                android {
                    namespace 'com.example.namespace'
                }
                dependencies {
                    baselineProfile(project(":$producerModuleName"))
                }
            """.trimIndent(),
            suffix = ""
        )

        gradleRunner
            .withArguments("generateReleaseBaselineProfile", "--stacktrace")
            .buildAndFail()
    }

    @Test
    fun testSrcSetAreAddedToVariants() {
        consumerProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.application")
                    id("androidx.baselineprofile.apptarget")
                    id("androidx.baselineprofile.consumer")
                }
                android {
                    namespace 'com.example.namespace'
                    productFlavors {
                        flavorDimensions = ["version"]
                        free { dimension "version" }
                        paid { dimension "version" }
                    }
                }
                baselineProfile {
                    enableR8BaselineProfileRewrite = false
                }

                $GRADLE_CODE_PRINT_TASK

                androidComponents {
                    onVariants(selector()) { variant ->
                        tasks.register(variant.name + "Print", PrintTask) { t ->
                            t.text.set(variant.sources.baselineProfiles?.all?.get().toString())
                        }
                    }
                }
            """.trimIndent(),
            suffix = ""
        )

        arrayOf("freeRelease", "paidRelease")
            .forEach {

                // Expected src set location. Note that src sets are not added if the folder does
                // not exist so we need to create it.
                val expected =
                    File(
                    consumerProjectSetup.rootDir,
                    "src/$it/$expectedBaselineProfileOutputFolder"
                )
                    .apply {
                        mkdirs()
                        deleteOnExit()
                    }

                gradleRunner.buildAndAssertThatOutput("${it}Print") {
                    contains(expected.absolutePath)
                }
            }
    }

    @Test
    fun testR8RewriteBaselineProfilePropertySet() {
        consumerProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.library")
                    id("androidx.baselineprofile.consumer")
                }
                android {
                    namespace 'com.example.namespace'
                    productFlavors {
                        flavorDimensions = ["version"]
                        free { dimension "version" }
                        paid { dimension "version" }
                    }
                    buildTypes {
                        anotherRelease { initWith(release) }
                    }
                }

                $GRADLE_CODE_PRINT_TASK

                androidComponents {
                    onVariants(selector()) { variant ->
                        println(variant.name)
                        tasks.register("print" + variant.name, PrintTask) { t ->
                            def prop = "android.experimental.art-profile-r8-rewriting"
                            if (prop in variant.experimentalProperties) {
                                def value = variant.experimentalProperties[prop].get().toString()
                                t.text.set( "r8-rw=" + value)
                            } else {
                                t.text.set( "r8-rw=false")
                            }
                        }
                    }
                }
            """.trimIndent(),
            suffix = ""
        )

        arrayOf(
            "printFreeRelease",
            "printPaidRelease",
            "printFreeAnotherRelease",
            "printPaidAnotherRelease",
        ).forEach { gradleRunner.buildAndAssertThatOutput(it) { contains("r8-rw=false") } }
    }

    @Test
    fun testFilterAndSortAndMerge() {
        consumerProjectSetup.writeDefaultBuildGradle(
            prefix = """
                plugins {
                    id("com.android.library")
                    id("androidx.baselineprofile.consumer")
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
                    baselineProfile(project(":$producerModuleName"))
                }
                baselineProfile {
                    filter { include("com.sample.Utils") }
                }
            """.trimIndent(),
            suffix = ""
        )
        producerProjectSetup.writeDefaultBuildGradle(
            prefix = MockProducerBuildGrade()
                .withConfiguration(
                    flavor = "free",
                    buildType = "release"
                )
                .withConfiguration(
                    flavor = "paid",
                    buildType = "release"
                )
                .withProducedBaselineProfile(
                    lines = listOf(
                        Fixtures.CLASS_1_METHOD_1,
                        Fixtures.CLASS_1_METHOD_2,
                        Fixtures.CLASS_1,
                    ),
                    flavor = "free",
                    buildType = "release"
                )
                .withProducedBaselineProfile(
                    lines = listOf(
                        Fixtures.CLASS_2_METHOD_1,
                        Fixtures.CLASS_2_METHOD_2,
                        Fixtures.CLASS_2_METHOD_3,
                        Fixtures.CLASS_2_METHOD_4,
                        Fixtures.CLASS_2_METHOD_5,
                        Fixtures.CLASS_2,
                    ),
                    flavor = "paid",
                    buildType = "release"
                )
                .build(),
            suffix = ""
        )

        gradleRunner
            .withArguments("generateBaselineProfile", "--stacktrace")
            .build()

        // In the final output there should be :
        //  - one single file in src/main/generatedBaselineProfiles because merge = `all`.
        //  - There should be only the Utils class [CLASS_2] because of the include filter.
        //  - The method `someOtherMethod` [CLASS_2_METHOD_3] should be included only once.
        assertThat(readBaselineProfileFileContent("main"))
            .containsExactly(
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
                Fixtures.CLASS_2_METHOD_2,
                Fixtures.CLASS_2_METHOD_3,
            )
    }
}

private class MockProducerBuildGrade {

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
                    attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, "baselineProfile"))
                    attribute(Attribute.of("androidx.baselineprofile.gradle.attributes.BuildType", String), "$buildType")
                    attribute(Attribute.of("androidx.baselineprofile.gradle.attributes.Flavor", String), "$flavor")
                }
            }
        }

        """.trimIndent()
        return this
    }

    fun withProducedBaselineProfile(
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

object Fixtures {
    const val CLASS_1 = "Lcom/sample/Activity;"
    const val CLASS_1_METHOD_1 = "HSPLcom/sample/Activity;-><init>()V"
    const val CLASS_1_METHOD_2 = "HSPLcom/sample/Activity;->onCreate(Landroid/os/Bundle;)V"
    const val CLASS_2 = "Lcom/sample/Utils;"
    const val CLASS_2_METHOD_1 = "HSLcom/sample/Utils;-><init>()V"
    const val CLASS_2_METHOD_2 = "HLcom/sample/Utils;->someMethod()V"
    const val CLASS_2_METHOD_3 = "HLcom/sample/Utils;->someOtherMethod()V"
    const val CLASS_2_METHOD_4 = "HSLcom/sample/Utils;->someOtherMethod()V"
    const val CLASS_2_METHOD_5 = "HSPLcom/sample/Utils;->someOtherMethod()V"
}