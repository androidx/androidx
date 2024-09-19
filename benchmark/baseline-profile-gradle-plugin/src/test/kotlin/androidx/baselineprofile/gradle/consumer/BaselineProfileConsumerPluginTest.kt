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

import androidx.baselineprofile.gradle.utils.ANDROID_APPLICATION_PLUGIN
import androidx.baselineprofile.gradle.utils.ANDROID_LIBRARY_PLUGIN
import androidx.baselineprofile.gradle.utils.ANDROID_TEST_PLUGIN
import androidx.baselineprofile.gradle.utils.BaselineProfileProjectSetupRule
import androidx.baselineprofile.gradle.utils.EXPECTED_PROFILE_FOLDER
import androidx.baselineprofile.gradle.utils.Fixtures
import androidx.baselineprofile.gradle.utils.TestAgpVersion
import androidx.baselineprofile.gradle.utils.TestAgpVersion.TEST_AGP_VERSION_8_0_0
import androidx.baselineprofile.gradle.utils.TestAgpVersion.TEST_AGP_VERSION_8_1_0
import androidx.baselineprofile.gradle.utils.TestAgpVersion.TEST_AGP_VERSION_8_3_1
import androidx.baselineprofile.gradle.utils.TestAgpVersion.TEST_AGP_VERSION_CURRENT
import androidx.baselineprofile.gradle.utils.VariantProfile
import androidx.baselineprofile.gradle.utils.build
import androidx.baselineprofile.gradle.utils.buildAndAssertThatOutput
import androidx.baselineprofile.gradle.utils.buildAndFailAndAssertThatOutput
import androidx.baselineprofile.gradle.utils.camelCase
import androidx.baselineprofile.gradle.utils.require
import androidx.baselineprofile.gradle.utils.requireInOrder
import androidx.baselineprofile.gradle.utils.toUri
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BaselineProfileConsumerPluginTest(private val agpVersion: TestAgpVersion) {

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TestAgpVersion.all()
    }

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(forceAgpVersion = agpVersion.versionString)

    private val gradleRunner by lazy { projectSetup.consumer.gradleRunner }

    private fun baselineProfileFile(variantName: String) =
        projectSetup.baselineProfileFile(variantName)

    private fun startupProfileFile(variantName: String) =
        projectSetup.startupProfileFile(variantName)

    private fun mergedArtProfile(variantName: String) = projectSetup.mergedArtProfile(variantName)

    private fun readBaselineProfileFileContent(variantName: String) =
        projectSetup.readBaselineProfileFileContent(variantName)

    private fun readStartupProfileFileContent(variantName: String) =
        projectSetup.readStartupProfileFileContent(variantName)

    @Test
    fun testGenerateTaskWithNoFlavorsForLibrary() {
        projectSetup.consumer.setup(androidPlugin = ANDROID_LIBRARY_PLUGIN)
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines =
                listOf(
                    Fixtures.CLASS_1_METHOD_1,
                    Fixtures.CLASS_1,
                    Fixtures.CLASS_2_METHOD_1,
                    Fixtures.CLASS_2
                ),
            releaseStartupProfileLines =
                listOf(
                    Fixtures.CLASS_3_METHOD_1,
                    Fixtures.CLASS_3,
                    Fixtures.CLASS_4_METHOD_1,
                    Fixtures.CLASS_4
                )
        )

        gradleRunner.build("generateBaselineProfile") {
            val notFound =
                it.lines()
                    .requireInOrder(
                        "A baseline profile was generated for the variant `release`:",
                        "${baselineProfileFile("main").toUri()}"
                    )
            assertThat(notFound).isEmpty()
        }

        assertThat(readBaselineProfileFileContent("main"))
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
                Fixtures.CLASS_3_METHOD_1,
                Fixtures.CLASS_3,
                Fixtures.CLASS_4_METHOD_1,
                Fixtures.CLASS_4
            )

        assertThat(startupProfileFile("main").exists()).isFalse()
    }

    @Test
    fun testGenerateTaskWithNoFlavorsForApplication() {
        projectSetup.consumer.setup(androidPlugin = ANDROID_APPLICATION_PLUGIN)
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines =
                listOf(
                    Fixtures.CLASS_1_METHOD_1,
                    Fixtures.CLASS_1,
                    Fixtures.CLASS_2_METHOD_1,
                    Fixtures.CLASS_2
                ),
            releaseStartupProfileLines =
                listOf(
                    Fixtures.CLASS_3_METHOD_1,
                    Fixtures.CLASS_3,
                    Fixtures.CLASS_4_METHOD_1,
                    Fixtures.CLASS_4
                )
        )

        gradleRunner.build("generateBaselineProfile") {
            val notFound =
                it.lines()
                    .requireInOrder(
                        "A baseline profile was generated for the variant `release`:",
                        "${baselineProfileFile("release").toUri()}",
                        "A startup profile was generated for the variant `release`:",
                        "${startupProfileFile("release").toUri()}"
                    )
            assertThat(notFound).isEmpty()
        }

        assertThat(readBaselineProfileFileContent("release"))
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
                Fixtures.CLASS_3,
                Fixtures.CLASS_3_METHOD_1,
                Fixtures.CLASS_4,
                Fixtures.CLASS_4_METHOD_1,
            )

        assertThat(readStartupProfileFileContent("release"))
            .containsExactly(
                Fixtures.CLASS_3,
                Fixtures.CLASS_3_METHOD_1,
                Fixtures.CLASS_4,
                Fixtures.CLASS_4_METHOD_1,
            )
    }

    @Test
    fun testGenerateTaskWithNoFlavorsForApplicationAndNoStartupProfile() {
        projectSetup.consumer.setup(androidPlugin = ANDROID_APPLICATION_PLUGIN)
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines =
                listOf(
                    Fixtures.CLASS_1_METHOD_1,
                    Fixtures.CLASS_1,
                ),
            releaseStartupProfileLines = listOf()
        )

        gradleRunner.withArguments("generateBaselineProfile", "--stacktrace").build()

        assertThat(readBaselineProfileFileContent("release"))
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
            )

        assertThat(startupProfileFile("release").exists()).isFalse()
    }

    @Test
    fun testGenerateTaskWithFlavorsAndDefaultMerge() {
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            dependencyOnProducerProject = true
        )
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
            freeReleaseStartupProfileLines = listOf(Fixtures.CLASS_3_METHOD_1, Fixtures.CLASS_3),
            paidReleaseStartupProfileLines = listOf(Fixtures.CLASS_4_METHOD_1, Fixtures.CLASS_4),
        )

        // Asserts that all per-variant, per-flavor and per-build type tasks are being generated.
        gradleRunner.buildAndAssertThatOutput("tasks") {
            contains("generateReleaseBaselineProfile - ")
            contains("generateFreeReleaseBaselineProfile - ")
            contains("generatePaidReleaseBaselineProfile - ")
        }

        gradleRunner.build("generateReleaseBaselineProfile") {
            arrayOf("freeRelease", "paidRelease").forEach { variantName ->
                val notFound =
                    it.lines()
                        .requireInOrder(
                            "A baseline profile was generated for the variant `$variantName`:",
                            "${baselineProfileFile(variantName).toUri()}",
                            "A startup profile was generated for the variant `$variantName`:",
                            "${startupProfileFile(variantName).toUri()}"
                        )

                assertWithMessage(
                        """
                |The following lines in gradle output were not found:
                |${notFound.joinToString("\n")}
                |
                |Full gradle output:
                |$it
            """
                            .trimMargin()
                    )
                    .that(notFound)
                    .isEmpty()
            }
        }

        assertThat(readBaselineProfileFileContent("freeRelease"))
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
                Fixtures.CLASS_3,
                Fixtures.CLASS_3_METHOD_1,
            )

        assertThat(readBaselineProfileFileContent("paidRelease"))
            .containsExactly(
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
                Fixtures.CLASS_4,
                Fixtures.CLASS_4_METHOD_1,
            )
    }

    @Test
    fun testPluginAppliedToLibraryModule() {
        projectSetup.producer.setup()
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            addAppTargetPlugin = false,
            dependencyOnProducerProject = true
        )
        gradleRunner.withArguments("generateBaselineProfile", "--stacktrace").build()
        // This should not fail.
    }

    @Test
    fun testPluginAppliedToNonApplicationAndNonLibraryModule() {
        projectSetup.producer.setup()
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_TEST_PLUGIN,
            addAppTargetPlugin = false,
            dependencyOnProducerProject = true
        )

        gradleRunner.withArguments("generateReleaseBaselineProfile", "--stacktrace").buildAndFail()
    }

    @Test
    fun testSrcSetAreAddedToVariantsForApplications() {
        projectSetup.producer.setupWithFreeAndPaidFlavors()
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            additionalGradleCodeBlock =
                """
                androidComponents {
                    onVariants(selector()) { variant ->
                        tasks.register(variant.name + "Sources", DisplaySourceSets) { t ->
                            t.srcs.set(variant.sources.baselineProfiles.all)
                        }
                    }
                }
            """
                    .trimIndent()
        )

        data class VariantExpectedSrcSets(val variantName: String, val expectedDirs: List<String>)

        fun variantBaselineProfileSrcSetDir(variantName: String): Array<String> {
            return (if (agpVersion == TEST_AGP_VERSION_8_0_0) {
                    listOf("src/$variantName/resources")
                } else {
                    listOf("src/$variantName/baselineProfiles")
                })
                .toTypedArray()
        }

        arrayOf(
                VariantExpectedSrcSets(
                    variantName = "freeRelease",
                    expectedDirs =
                        listOf(
                            "src/main/baselineProfiles",
                            "src/free/baselineProfiles",
                            "src/release/baselineProfiles",
                            *variantBaselineProfileSrcSetDir("freeRelease"),
                            "src/freeRelease/generated/baselineProfiles",
                        )
                ),
                VariantExpectedSrcSets(
                    variantName = "paidRelease",
                    expectedDirs =
                        listOf(
                            "src/main/baselineProfiles",
                            "src/paid/baselineProfiles",
                            "src/release/baselineProfiles",
                            *variantBaselineProfileSrcSetDir("paidRelease"),
                            "src/paidRelease/generated/baselineProfiles",
                        )
                ),
                // Note that we don't create a benchmark build type for AGP 8.0 due to b/265438201.
                *(if (agpVersion > TEST_AGP_VERSION_8_0_0) {
                        listOf(
                            VariantExpectedSrcSets(
                                variantName = "freeBenchmarkRelease",
                                expectedDirs =
                                    listOf(
                                        "src/main/baselineProfiles",
                                        "src/free/baselineProfiles",
                                        "src/benchmarkRelease/baselineProfiles",
                                        "src/freeBenchmarkRelease/baselineProfiles",
                                        "src/freeRelease/generated/baselineProfiles",
                                    )
                            ),
                            VariantExpectedSrcSets(
                                variantName = "paidBenchmarkRelease",
                                expectedDirs =
                                    listOf(
                                        "src/main/baselineProfiles",
                                        "src/paid/baselineProfiles",
                                        "src/benchmarkRelease/baselineProfiles",
                                        "src/paidBenchmarkRelease/baselineProfiles",
                                        "src/paidRelease/generated/baselineProfiles",
                                    )
                            )
                        )
                    } else {
                        listOf()
                    })
                    .toTypedArray()
            )
            .forEach {
                val expected =
                    it.expectedDirs
                        .map { dir -> File(projectSetup.consumer.rootDir, dir) }
                        .onEach { f ->
                            // Expected src set location. Note that src sets are not added if the
                            // folder does
                            // not exist so we need to create it.
                            f.mkdirs()
                            f.deleteOnExit()
                        }

                gradleRunner.buildAndAssertThatOutput("${it.variantName}Sources") {
                    expected.forEach { e -> contains(e.absolutePath) }
                }
            }
    }

    @Test
    fun testWhenPluginIsAppliedAndNoDependencyIsSetShouldFailWithErrorMsg() {
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = false,
            dependencyOnProducerProject = false
        )
        gradleRunner.build("generateReleaseBaselineProfile", "--stacktrace") {
            assertThat(it.replace("\n", " "))
                .contains(
                    "The baseline profile consumer plugin is applied to this module but no " +
                        "dependency has been set for variant `release`"
                )
        }
    }

    @Test
    fun testExperimentalPropertiesNotSet() {
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2)
        )
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            dependencyOnProducerProject = true,
            flavors = true,
            buildTypeAnotherRelease = true
        )

        arrayOf(
                "printExperimentalPropertiesForVariantFreeRelease",
                "printExperimentalPropertiesForVariantPaidRelease",
                "printExperimentalPropertiesForVariantFreeAnotherRelease",
                "printExperimentalPropertiesForVariantPaidAnotherRelease",
            )
            .forEach {
                gradleRunner.buildAndAssertThatOutput(it) {
                    doesNotContain("android.experimental.art-profile-r8-rewriting=")
                    doesNotContain("android.experimental.r8.dex-startup-optimization=")
                }
            }
    }

    @Test
    fun testFilterAndSortAndMerge() {
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            flavors = true,
            baselineProfileBlock =
                """
                filter {
                    include("com.sample.Utils")
                }
            """
                    .trimIndent()
        )
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines =
                listOf(
                    Fixtures.CLASS_1_METHOD_1,
                    Fixtures.CLASS_1_METHOD_2,
                    Fixtures.CLASS_1,
                ),
            paidReleaseProfileLines =
                listOf(
                    Fixtures.CLASS_2_METHOD_1,
                    Fixtures.CLASS_2_METHOD_2,
                    Fixtures.CLASS_2_METHOD_3,
                    Fixtures.CLASS_2_METHOD_4,
                    Fixtures.CLASS_2_METHOD_5,
                    Fixtures.CLASS_2,
                )
        )

        gradleRunner.withArguments("generateBaselineProfile", "--stacktrace").build()

        // In the final output there should be :
        //  - one single file in src/main/generated/baselineProfiles (because this is a library).
        //  - There should be only the Utils class [CLASS_2] because of the include filter.
        //  - The method `someOtherMethod` [CLASS_2_METHOD_3] should be included only once
        //      (despite being included multiple times with different flags).
        assertThat(readBaselineProfileFileContent("main"))
            .containsExactly(
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
                Fixtures.CLASS_2_METHOD_2,
                Fixtures.CLASS_2_METHOD_3,
            )
    }

    @Test
    fun testFilterPerVariant() {
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            baselineProfileBlock =
                """
                filter {
                    include("com.sample.Activity")
                }
                variants {
                    freeRelease {
                        filter { include("com.sample.Utils") }
                    }
                    paidRelease {
                        filter { include("com.sample.Fragment") }
                    }
                }
            """
                    .trimIndent()
        )

        val commonProfile =
            listOf(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
                Fixtures.CLASS_1_METHOD_2,
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
                Fixtures.CLASS_2_METHOD_2,
                Fixtures.CLASS_2_METHOD_3,
                Fixtures.CLASS_3,
                Fixtures.CLASS_3_METHOD_1,
            )
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = commonProfile,
            paidReleaseProfileLines = commonProfile,
        )

        gradleRunner.withArguments("generateBaselineProfile", "--stacktrace").build()

        assertThat(readBaselineProfileFileContent("freeRelease"))
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
                Fixtures.CLASS_1_METHOD_2,
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
                Fixtures.CLASS_2_METHOD_2,
                Fixtures.CLASS_2_METHOD_3,
            )
        assertThat(readBaselineProfileFileContent("paidRelease"))
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
                Fixtures.CLASS_1_METHOD_2,
                Fixtures.CLASS_3,
                Fixtures.CLASS_3_METHOD_1,
            )
    }

    @Test
    fun testSaveInSrcTrueAndAutomaticGenerationDuringBuildTrue() {
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            baselineProfileBlock =
                """
                saveInSrc = true
                automaticGenerationDuringBuild = true
            """
                    .trimIndent()
        )
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
        )

        // Asserts that assembling release triggers generation of profile
        gradleRunner.build("assembleFreeRelease", "--dry-run") {
            val notFound =
                it.lines()
                    .requireInOrder(
                        ":${projectSetup.consumer.name}:mergeFreeReleaseBaselineProfile",
                        ":${projectSetup.consumer.name}:copyFreeReleaseBaselineProfileIntoSrc",
                        ":${projectSetup.consumer.name}:mergeFreeReleaseArtProfile",
                        ":${projectSetup.consumer.name}:compileFreeReleaseArtProfile",
                        ":${projectSetup.consumer.name}:assembleFreeRelease"
                    )
            assertThat(notFound).isEmpty()
        }

        // Asserts that the profile is generated in the src folder
        gradleRunner.build("generateFreeReleaseBaselineProfile") {
            assertThat(readBaselineProfileFileContent("freeRelease"))
                .containsExactly(
                    Fixtures.CLASS_1,
                    Fixtures.CLASS_1_METHOD_1,
                )
        }
    }

    @Test
    fun testSaveInSrcTrueAndAutomaticGenerationDuringBuildFalse() {
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            baselineProfileBlock =
                """
                saveInSrc = true
                automaticGenerationDuringBuild = false
            """
                    .trimIndent()
        )
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
        )

        // Asserts that assembling release does not trigger generation of profile
        gradleRunner.buildAndAssertThatOutput("assembleFreeRelease", "--dry-run") {
            arrayOf("mergeFreeReleaseBaselineProfile", "copyFreeReleaseBaselineProfileIntoSrc")
                .forEach { doesNotContain(":${projectSetup.consumer.name}:$it") }
            arrayOf(
                    "mergeFreeReleaseArtProfile",
                    "compileFreeReleaseArtProfile",
                    "assembleFreeRelease"
                )
                .forEach { contains(":${projectSetup.consumer.name}:$it") }
        }

        // Asserts that the profile is generated in the src folder
        gradleRunner.build("generateFreeReleaseBaselineProfile") {
            assertThat(readBaselineProfileFileContent("freeRelease"))
                .containsExactly(
                    Fixtures.CLASS_1,
                    Fixtures.CLASS_1_METHOD_1,
                )
        }
    }

    @Test
    fun testSaveInSrcFalseAndAutomaticGenerationDuringBuildTrue() {
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            baselineProfileBlock =
                """
                saveInSrc = false
                automaticGenerationDuringBuild = true
            """
                    .trimIndent()
        )
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
            freeReleaseStartupProfileLines = listOf(Fixtures.CLASS_3_METHOD_1, Fixtures.CLASS_3),
            paidReleaseStartupProfileLines = listOf(Fixtures.CLASS_4_METHOD_1, Fixtures.CLASS_4),
        )

        // Asserts that assembling release triggers generation of profile
        gradleRunner.build("assembleFreeRelease", "--dry-run") {

            // Assert sequence of tasks is found
            val notFound =
                it.lines()
                    .requireInOrder(
                        ":${projectSetup.consumer.name}:mergeFreeReleaseBaselineProfile",
                        ":${projectSetup.consumer.name}:mergeFreeReleaseArtProfile",
                        ":${projectSetup.consumer.name}:compileFreeReleaseArtProfile",
                        ":${projectSetup.consumer.name}:assembleFreeRelease"
                    )
            assertThat(notFound).isEmpty()

            // Asserts that the copy task is disabled, because of `saveInSrc` set to false.
            assertThat(it)
                .doesNotContain(
                    ":${projectSetup.consumer.name}:copyFreeReleaseBaselineProfileIntoSrc"
                )
        }

        // Asserts that the profile is not generated in the src folder
        gradleRunner.build("generateFreeReleaseBaselineProfile") {
            // Note that here the profiles are generated in the intermediates so the output does
            // not matter.
            val notFound =
                it.lines()
                    .requireInOrder(
                        "A baseline profile was generated for the variant `freeRelease`:",
                        "A startup profile was generated for the variant `freeRelease`:",
                    )
            assertThat(notFound).isEmpty()
        }

        assertThat(baselineProfileFile("freeRelease").exists()).isFalse()
    }

    @Test
    fun testSaveInSrcFalseAndAutomaticGenerationDuringBuildFalse() {
        projectSetup.producer.setup()
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            baselineProfileBlock =
                """
                saveInSrc = false
                automaticGenerationDuringBuild = false
            """
                    .trimIndent()
        )
        gradleRunner
            .withArguments("generateReleaseBaselineProfile", "--stacktrace")
            .buildAndFail()
            .output
            .replace(System.lineSeparator(), " ")
            .also {
                assertThat(it)
                    .contains(
                        "The current configuration of flags `saveInSrc` and " +
                            "`automaticGenerationDuringBuild` is not supported"
                    )
            }
    }

    @Test
    fun testWhenFiltersFilterOutAllTheProfileRules() {
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            baselineProfileBlock =
                """
                filter { include("nothing.**") }
            """
                    .trimIndent()
        )
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1)
        )

        gradleRunner
            .withArguments("generateBaselineProfile", "--stacktrace")
            .buildAndFail()
            .output
            .replace(System.lineSeparator(), " ")
            .also {
                assertThat(it)
                    .contains(
                        "The baseline profile consumer plugin is configured with filters that " +
                            "exclude all the profile rules"
                    )
            }
    }

    @Test
    fun testWhenProfileProducerProducesEmptyProfile() {
        projectSetup.consumer.setup(androidPlugin = ANDROID_LIBRARY_PLUGIN)
        projectSetup.producer.setupWithoutFlavors(releaseProfileLines = listOf())
        gradleRunner.buildAndAssertThatOutput("generateBaselineProfile") {
            contains("No baseline profile rules were generated")
        }
    }

    @Test
    fun testVariantConfigurationOverrideForFlavors() {
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
        )
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            flavors = true,
            baselineProfileBlock =
                """

                // Global configuration
                saveInSrc = true
                automaticGenerationDuringBuild = false
                baselineProfileOutputDir = "generated/baselineProfiles"
                mergeIntoMain = true

                // Per variant configuration overrides global configuration.
                variants {
                    free {
                        saveInSrc = false
                        automaticGenerationDuringBuild = true
                        baselineProfileOutputDir = "somefolder"
                        mergeIntoMain = false
                    }
                    paidRelease {
                        saveInSrc = false
                        automaticGenerationDuringBuild = true
                        baselineProfileOutputDir = "someOtherfolder"
                        mergeIntoMain = false
                    }
                }

            """
                    .trimIndent()
        )

        gradleRunner.buildAndAssertThatOutput(
            "printBaselineProfileExtensionForVariantFreeRelease"
        ) {
            contains("saveInSrc=`false`")
            contains("automaticGenerationDuringBuild=`true`")
            contains("baselineProfileOutputDir=`somefolder`")
            contains("mergeIntoMain=`false`")
        }

        gradleRunner.buildAndAssertThatOutput(
            "printBaselineProfileExtensionForVariantPaidRelease"
        ) {
            contains("saveInSrc=`false`")
            contains("automaticGenerationDuringBuild=`true`")
            contains("baselineProfileOutputDir=`someOtherfolder`")
            contains("mergeIntoMain=`false`")
        }
    }

    @Test
    fun testVariantConfigurationOverrideForBuildTypes() {
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
        )
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            baselineProfileBlock =
                """

                // Global configuration
                saveInSrc = true
                automaticGenerationDuringBuild = false
                baselineProfileOutputDir = "generated/baselineProfiles"
                mergeIntoMain = true

                // Per variant configuration overrides global configuration.
                variants {
                    release {
                        saveInSrc = false
                        automaticGenerationDuringBuild = true
                        baselineProfileOutputDir = "myReleaseFolder"
                        mergeIntoMain = false
                    }
                    paidRelease {
                        saveInSrc = false
                        automaticGenerationDuringBuild = true
                        baselineProfileOutputDir = "someOtherfolder"
                        mergeIntoMain = false
                    }
                }

            """
                    .trimIndent()
        )

        gradleRunner.buildAndAssertThatOutput(
            "printBaselineProfileExtensionForVariantFreeRelease"
        ) {
            contains("saveInSrc=`false`")
            contains("automaticGenerationDuringBuild=`true`")
            contains("baselineProfileOutputDir=`myReleaseFolder`")
            contains("mergeIntoMain=`false`")
        }

        gradleRunner.buildAndAssertThatOutput(
            "printBaselineProfileExtensionForVariantPaidRelease"
        ) {
            contains("saveInSrc=`false`")
            contains("automaticGenerationDuringBuild=`true`")
            contains("baselineProfileOutputDir=`someOtherfolder`")
            contains("mergeIntoMain=`false`")
        }
    }

    @Test
    fun testVariantConfigurationOverrideForFlavorsAndBuildType() {
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
        )
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            flavors = true,
            baselineProfileBlock =
                """
                variants {
                    free {
                        saveInSrc = true
                    }
                    release {
                        saveInSrc = false
                    }
                }

            """
                    .trimIndent()
        )
        gradleRunner
            .withArguments("printBaselineProfileExtensionForVariantFreeRelease", "--stacktrace")
            .buildAndFail()
            .output
            .let {
                assertThat(it)
                    .contains("The per-variant configuration for baseline profiles is ambiguous")
            }
    }

    @Test
    fun testVariantDependenciesWithFlavors() {
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
        )

        // In this setup no dependency is being added through the dependency block.
        // Instead dependencies are being added through per-variant configuration block.
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            dependencyOnProducerProject = false,
            baselineProfileBlock =
                """
                variants {
                    free {
                        from(project(":${projectSetup.producer.name}"))
                    }
                    paid {
                        from(project(":${projectSetup.producer.name}"))
                    }
                }

            """
                    .trimIndent()
        )
        gradleRunner.withArguments("generateReleaseBaselineProfile", "--stacktrace").build()

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
    fun testPartialResults() {
        projectSetup.consumer.setup(androidPlugin = ANDROID_APPLICATION_PLUGIN)

        // Function to setup the producer, run the generate profile command and assert output
        fun setupProducerGenerateAndAssert(
            partial: Boolean,
            generatedProfiles: Map<String, List<String>>,
            actualProfile: List<String>
        ) {
            projectSetup.producer.setup(
                variantProfiles =
                    listOf(
                        VariantProfile(
                            flavor = null,
                            buildType = "release",
                            profileFileLines = generatedProfiles
                        )
                    )
            )

            val args =
                listOfNotNull(
                    "generateBaselineProfile",
                    if (partial) "-Pandroid.testInstrumentationRunnerArguments.class=someClass"
                    else null
                )

            projectSetup.consumer.gradleRunner.build(*args.toTypedArray()) {}

            assertThat(readBaselineProfileFileContent("release"))
                .containsExactly(*actualProfile.toTypedArray())
        }

        // Full generation, 2 new tests.
        setupProducerGenerateAndAssert(
            partial = false,
            generatedProfiles =
                mapOf(
                    "myTest1" to listOf(Fixtures.CLASS_1, Fixtures.CLASS_1_METHOD_1),
                    "myTest2" to listOf(Fixtures.CLASS_2, Fixtures.CLASS_2_METHOD_1)
                ),
            actualProfile =
                listOf(
                    Fixtures.CLASS_1,
                    Fixtures.CLASS_1_METHOD_1,
                    Fixtures.CLASS_2,
                    Fixtures.CLASS_2_METHOD_1
                )
        )

        // Partial generation, modify 1 test.
        setupProducerGenerateAndAssert(
            partial = true,
            generatedProfiles =
                mapOf("myTest1" to listOf(Fixtures.CLASS_3, Fixtures.CLASS_3_METHOD_1)),
            actualProfile =
                listOf(
                    Fixtures.CLASS_3,
                    Fixtures.CLASS_3_METHOD_1,
                    Fixtures.CLASS_2,
                    Fixtures.CLASS_2_METHOD_1
                )
        )

        // Partial generation, add 1 test.
        setupProducerGenerateAndAssert(
            partial = true,
            generatedProfiles =
                mapOf("myTest3" to listOf(Fixtures.CLASS_4, Fixtures.CLASS_4_METHOD_1)),
            actualProfile =
                listOf(
                    Fixtures.CLASS_3,
                    Fixtures.CLASS_3_METHOD_1,
                    Fixtures.CLASS_4,
                    Fixtures.CLASS_4_METHOD_1,
                    Fixtures.CLASS_2,
                    Fixtures.CLASS_2_METHOD_1
                )
        )

        // Full generation, 2 new tests.
        setupProducerGenerateAndAssert(
            partial = false,
            generatedProfiles =
                mapOf(
                    "myTest1-new" to listOf(Fixtures.CLASS_1, Fixtures.CLASS_1_METHOD_1),
                    "myTest2-new" to listOf(Fixtures.CLASS_2, Fixtures.CLASS_2_METHOD_1)
                ),
            actualProfile =
                listOf(
                    Fixtures.CLASS_1,
                    Fixtures.CLASS_1_METHOD_1,
                    Fixtures.CLASS_2,
                    Fixtures.CLASS_2_METHOD_1
                )
        )
    }

    @Test
    fun testBaselineProfileIsInMergeArtProfileIntermediate() {
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            baselineProfileBlock =
                """
                saveInSrc = true
                automaticGenerationDuringBuild = true
            """
                    .trimIndent()
        )

        data class VariantAndProfile(val variantName: String, val profile: List<String>)

        val freeRelease =
            VariantAndProfile(
                variantName = "freeRelease",
                profile =
                    listOf(
                        Fixtures.CLASS_1,
                        Fixtures.CLASS_1_METHOD_1,
                        Fixtures.CLASS_1_METHOD_2,
                        Fixtures.CLASS_3,
                        Fixtures.CLASS_3_METHOD_1,
                    )
            )
        val paidRelease =
            VariantAndProfile(
                variantName = "paidRelease",
                profile =
                    listOf(
                        Fixtures.CLASS_1,
                        Fixtures.CLASS_1_METHOD_1,
                        Fixtures.CLASS_1_METHOD_2,
                        Fixtures.CLASS_2,
                        Fixtures.CLASS_2_METHOD_1,
                        Fixtures.CLASS_2_METHOD_2,
                        Fixtures.CLASS_2_METHOD_3,
                    )
            )
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = freeRelease.profile,
            paidReleaseProfileLines = paidRelease.profile,
        )

        val variants = arrayOf(freeRelease, paidRelease)
        val tasks = variants.map { camelCase("merge", it.variantName, "ArtProfile") }
        gradleRunner.build(*(tasks.toTypedArray())) {}

        variants.forEach {
            val notFound =
                mergedArtProfile(it.variantName).readLines().require(*(it.profile).toTypedArray())
            assertThat(notFound).isEmpty()
        }
    }

    @Test
    fun testMultidimensionalFlavorsAndMatchingFallbacks() {
        projectSetup.consumer.setupWithBlocks(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavorsBlock =
                """
                flavorDimensions = ["tier", "color"]
                free { dimension "tier" }
                red { dimension "color" }
                paid {
                    dimension "tier"
                    matchingFallbacks += "free"
                }
                blue {
                    dimension "color"
                    matchingFallbacks += "red"
                }
            """
                    .trimIndent(),
            buildTypesBlock = "",
            dependencyOnProducerProject = false,
            dependenciesBlock =
                """
                implementation(project(":${projectSetup.dependency.name}"))
            """
                    .trimIndent(),
            baselineProfileBlock =
                """
                variants {
                    free { from(project(":${projectSetup.producer.name}")) }
                    red { from(project(":${projectSetup.producer.name}")) }
                    paid { from(project(":${projectSetup.producer.name}")) }
                    // blue is already covered by the intersection of the other dimensions so no
                    // need to specify it.
                }

            """
                    .trimIndent()
        )
        projectSetup.producer.setup(
            variantProfiles =
                listOf(
                    VariantProfile(
                        flavorDimensions =
                            mapOf(
                                "tier" to "free",
                                "color" to "red",
                            ),
                        buildType = "release",
                        profileFileLines =
                            mapOf(
                                "some-test-output" to
                                    listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1)
                            ),
                        startupFileLines =
                            mapOf(
                                "some-startup-test-output" to
                                    listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1)
                            ),
                    )
                )
        )

        arrayOf("freeRedRelease", "freeBlueRelease", "paidRedRelease", "paidBlueRelease").forEach {
            variantName ->
            gradleRunner.build(camelCase("generate", variantName, "baselineProfile")) {
                assertThat(readBaselineProfileFileContent(variantName))
                    .containsExactly(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1)
            }
        }
    }

    @Test
    fun testSkipGeneration() {
        projectSetup.consumer.setup(ANDROID_APPLICATION_PLUGIN)
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1)
        )

        gradleRunner.build("generateBaselineProfile", "-Pandroidx.baselineprofile.skipgeneration") {
            assertThat(baselineProfileFile("release").exists()).isFalse()
        }
    }

    @Test
    fun testSkipGenerationWithPreviousResults() {
        projectSetup.consumer.setup(ANDROID_APPLICATION_PLUGIN)
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1)
        )

        gradleRunner.build("generateBaselineProfile") {
            assertThat(readBaselineProfileFileContent("release"))
                .containsExactly(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1)
        }

        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2)
        )

        gradleRunner.build("generateBaselineProfile", "-Pandroidx.baselineprofile.skipgeneration") {

            // Note that the baseline profile should still contain the previous profile rules
            // and not the updated ones, as running with `skipgeneration` will disable the
            // generation tasks.
            assertThat(readBaselineProfileFileContent("release"))
                .containsExactly(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1)
        }
    }

    @Test
    fun testVariantSpecificDependencies() {
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1)
        )
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            dependenciesBlock =
                """
               releaseImplementation(project(":${projectSetup.dependency.name}"))
            """
                    .trimIndent()
        )
        gradleRunner.build("generateReleaseBaselineProfile", "--stacktrace") {
            assertThat(readBaselineProfileFileContent("release"))
                .containsExactly(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1)
        }
    }

    @Test
    fun testVariantSpecificDependenciesWithFlavorsAndMultipleBuildTypes() {
        projectSetup.consumer.setupWithBlocks(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavorsBlock =
                """
                flavorDimensions = ["tier"]
                free { dimension "tier" }
                paid { dimension "tier" }
            """
                    .trimIndent(),
            buildTypesBlock =
                """
                anotherRelease { initWith(release) }
            """
                    .trimIndent(),
            dependencyOnProducerProject = true,
            dependenciesBlock =
                """
                releaseImplementation(project(":${projectSetup.dependency.name}"))
                anotherReleaseImplementation(project(":${projectSetup.dependency.name}"))
            """
                    .trimIndent(),
        )
        projectSetup.producer.setup(
            variantProfiles =
                listOf(
                    VariantProfile(
                        flavorDimensions = mapOf("tier" to "free"),
                        buildType = "release",
                        profileFileLines =
                            mapOf(
                                "test-output-baseline-free-release" to
                                    listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1)
                            ),
                        startupFileLines = mapOf()
                    ),
                    VariantProfile(
                        flavorDimensions = mapOf("tier" to "paid"),
                        buildType = "release",
                        profileFileLines =
                            mapOf(
                                "test-output-baseline-paid-release" to
                                    listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2)
                            ),
                        startupFileLines = mapOf()
                    ),
                    VariantProfile(
                        flavorDimensions = mapOf("tier" to "free"),
                        buildType = "anotherRelease",
                        profileFileLines =
                            mapOf(
                                "test-output-baseline-free-anotherRelease" to
                                    listOf(Fixtures.CLASS_3_METHOD_1, Fixtures.CLASS_3)
                            ),
                        startupFileLines = mapOf()
                    ),
                    VariantProfile(
                        flavorDimensions = mapOf("tier" to "paid"),
                        buildType = "anotherRelease",
                        profileFileLines =
                            mapOf(
                                "test-output-baseline-paid-anotherRelease" to
                                    listOf(Fixtures.CLASS_4_METHOD_1, Fixtures.CLASS_4)
                            ),
                        startupFileLines = mapOf()
                    ),
                )
        )

        data class Expected(val variantName: String, val profileLines: List<String>)
        arrayOf(
                Expected(
                    variantName = "freeRelease",
                    profileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1)
                ),
                Expected(
                    variantName = "paidRelease",
                    profileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2)
                ),
                Expected(
                    variantName = "freeAnotherRelease",
                    profileLines = listOf(Fixtures.CLASS_3_METHOD_1, Fixtures.CLASS_3)
                ),
                Expected(
                    variantName = "paidAnotherRelease",
                    profileLines = listOf(Fixtures.CLASS_4_METHOD_1, Fixtures.CLASS_4)
                ),
            )
            .forEach { expected ->
                gradleRunner.build(camelCase("generate", expected.variantName, "baselineProfile")) {
                    assertThat(readBaselineProfileFileContent(expected.variantName))
                        .containsExactlyElementsIn(expected.profileLines)
                }
            }
    }

    @Test
    fun whenBenchmarkVariantsAreDisabledShouldNotify() {
        // Note that this test doesn't works only on AGP > 8.0.0 because in previous versions
        // the benchmark variant is not created.
        assumeTrue(agpVersion != TEST_AGP_VERSION_8_0_0)

        projectSetup.consumer.setup(
            dependencyOnProducerProject = true,
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            additionalGradleCodeBlock =
                """
                androidComponents {
                    beforeVariants(selector()) { variant ->
                        variant.enable = variant.buildType != "benchmarkRelease"
                    }
                }
            """
                    .trimIndent()
        )
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines =
                listOf(
                    Fixtures.CLASS_1_METHOD_1,
                    Fixtures.CLASS_1,
                    Fixtures.CLASS_2_METHOD_1,
                    Fixtures.CLASS_2
                ),
            releaseStartupProfileLines =
                listOf(
                    Fixtures.CLASS_3_METHOD_1,
                    Fixtures.CLASS_3,
                    Fixtures.CLASS_4_METHOD_1,
                    Fixtures.CLASS_4
                )
        )

        gradleRunner.buildAndAssertThatOutput("tasks", "--info") {
            contains("Variant `benchmarkRelease` is disabled.")
        }
    }

    @Test
    fun testProfileStats() {
        projectSetup.consumer.setup(androidPlugin = ANDROID_APPLICATION_PLUGIN)

        // Test no previous execution
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines =
                listOf(
                    Fixtures.CLASS_1_METHOD_1,
                    Fixtures.CLASS_1,
                ),
            releaseStartupProfileLines =
                listOf(
                    Fixtures.CLASS_1_METHOD_1,
                    Fixtures.CLASS_1,
                )
        )
        gradleRunner.build("generateBaselineProfile") {
            val notFound =
                it.lines()
                    .requireInOrder(
                        "Comparison with previous baseline profile:",
                        "Comparison with previous startup profile:",
                    )
            assertThat(notFound.size).isEqualTo(2)
        }

        // Test unchanged
        gradleRunner.build("generateBaselineProfile", "--rerun-tasks") {
            println(it)
            val notFound =
                it.lines()
                    .requireInOrder(
                        "Comparison with previous baseline profile:",
                        "  2 Old rules",
                        "  2 New rules",
                        "  0 Added rules (0.00%)",
                        "  0 Removed rules (0.00%)",
                        "  2 Unmodified rules (100.00%)",
                        "Comparison with previous startup profile:",
                        "  2 Old rules",
                        "  2 New rules",
                        "  0 Added rules (0.00%)",
                        "  0 Removed rules (0.00%)",
                        "  2 Unmodified rules (100.00%)",
                    )
            assertThat(notFound).isEmpty()
        }

        // Test added
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines =
                listOf(
                    Fixtures.CLASS_1_METHOD_1,
                    Fixtures.CLASS_1,
                    Fixtures.CLASS_2_METHOD_2,
                    Fixtures.CLASS_2,
                ),
            releaseStartupProfileLines =
                listOf(
                    Fixtures.CLASS_1_METHOD_1,
                    Fixtures.CLASS_1,
                    Fixtures.CLASS_2_METHOD_2,
                    Fixtures.CLASS_2,
                )
        )
        gradleRunner.build("generateBaselineProfile", "--rerun-tasks") {
            println(it)
            val notFound =
                it.lines()
                    .requireInOrder(
                        "Comparison with previous baseline profile:",
                        "  2 Old rules",
                        "  4 New rules",
                        "  2 Added rules (50.00%)",
                        "  0 Removed rules (0.00%)",
                        "  2 Unmodified rules (50.00%)",
                        "Comparison with previous startup profile:",
                        "  2 Old rules",
                        "  4 New rules",
                        "  2 Added rules (50.00%)",
                        "  0 Removed rules (0.00%)",
                        "  2 Unmodified rules (50.00%)",
                    )
            assertThat(notFound).isEmpty()
        }

        // Test removed
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines =
                listOf(
                    Fixtures.CLASS_2_METHOD_2,
                    Fixtures.CLASS_2,
                ),
            releaseStartupProfileLines =
                listOf(
                    Fixtures.CLASS_2_METHOD_2,
                    Fixtures.CLASS_2,
                )
        )
        gradleRunner.build("generateBaselineProfile", "--rerun-tasks") {
            println(it)
            val notFound =
                it.lines()
                    .requireInOrder(
                        "Comparison with previous baseline profile:",
                        "  4 Old rules",
                        "  2 New rules",
                        "  0 Added rules (0.00%)",
                        "  2 Removed rules (50.00%)",
                        "  2 Unmodified rules (50.00%)",
                        "Comparison with previous startup profile:",
                        "  4 Old rules",
                        "  2 New rules",
                        "  0 Added rules (0.00%)",
                        "  2 Removed rules (50.00%)",
                        "  2 Unmodified rules (50.00%)",
                    )
            assertThat(notFound).isEmpty()
        }
    }

    @Test
    fun testSuppressWarningMaxAgpVersion() {
        val requiredLines =
            listOf(
                "This version of the Baseline Profile Gradle Plugin was tested with versions below",
                // We skip the lines in between because they may contain changing version numbers.
                "baselineProfile {",
                "    warnings {",
                "        maxAgpVersion = false",
                "    }",
                "}"
            )
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
        )

        // Setup with default warnings
        projectSetup.consumer.setup(androidPlugin = ANDROID_APPLICATION_PLUGIN)
        projectSetup.consumer.gradleRunner.build(
            "generateBaselineProfile",
            "-Pandroidx.benchmark.test.maxagpversion=1.0.0"
        ) {
            val notFound = it.lines().requireInOrder(*requiredLines.toTypedArray())
            assertThat(notFound).isEmpty()
        }

        // Setup turning off warning
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            baselineProfileBlock =
                """
                warnings {
                    maxAgpVersion = false
                }
            """
                    .trimIndent()
        )
        projectSetup.consumer.gradleRunner.build(
            "generateBaselineProfile",
            "-Pandroidx.benchmark.test.maxagpversion=1.0.0"
        ) {
            val notFound = it.lines().requireInOrder(*requiredLines.toTypedArray())
            assertThat(notFound).isEqualTo(requiredLines)
        }
    }

    @Test
    fun testSuppressWarningWithProperty() {
        val requiredLines =
            listOf(
                "This version of the Baseline Profile Gradle Plugin was tested with versions below",
                // We skip the lines in between because they may contain changing version numbers.
                "baselineProfile {",
                "    warnings {",
                "        maxAgpVersion = false",
                "    }",
                "}"
            )

        projectSetup.consumer.setup(androidPlugin = ANDROID_APPLICATION_PLUGIN)
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
        )

        val gradleCmds =
            arrayOf(
                "generateBaselineProfile",
                "-Pandroidx.benchmark.test.maxagpversion=1.0.0",
            )

        // Run with no suppress warnings property
        projectSetup.consumer.gradleRunner.build(*gradleCmds) {
            val notFound = it.lines().requireInOrder(*requiredLines.toTypedArray())
            assertThat(notFound).isEmpty()
        }

        // Run with suppress warnings property
        projectSetup.consumer.gradleRunner.build(
            *gradleCmds,
            "-Pandroidx.baselineprofile.suppresswarnings"
        ) {
            val notFound = it.lines().requireInOrder(*requiredLines.toTypedArray())
            assertThat(notFound).isEqualTo(requiredLines)
        }
    }

    @Test
    fun testMergeArtAndStartupProfilesShouldDependOnProfileGeneration() {
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
        )

        arrayOf(
                Pair(true, true),
                Pair(true, false),
                Pair(false, true),
            )
            .forEach { (saveInSrc, automaticGenerationDuringBuild) ->
                projectSetup.consumer.setup(
                    androidPlugin = ANDROID_APPLICATION_PLUGIN,
                    flavors = true,
                    baselineProfileBlock =
                        """
                saveInSrc = $saveInSrc
                automaticGenerationDuringBuild = $automaticGenerationDuringBuild
            """
                            .trimIndent()
                )
                gradleRunner.build("generateFreeReleaseBaselineProfile", "assembleFreeRelease") {}
            }
    }
}

@RunWith(JUnit4::class)
class BaselineProfileConsumerPluginTestWithAgp80 {

    @get:Rule
    val projectSetup =
        BaselineProfileProjectSetupRule(forceAgpVersion = TEST_AGP_VERSION_8_0_0.versionString)

    @Test
    fun verifyGenerateTasks() {
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
            freeAnotherReleaseProfileLines = listOf(Fixtures.CLASS_3_METHOD_1, Fixtures.CLASS_3),
            paidAnotherReleaseProfileLines = listOf(Fixtures.CLASS_4_METHOD_1, Fixtures.CLASS_4),
        )
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            dependencyOnProducerProject = true,
            flavors = true,
            buildTypeAnotherRelease = true
        )
        projectSetup.consumer.gradleRunner.build("tasks") {
            val notFound =
                it.lines()
                    .require(
                        "generateBaselineProfile - ",
                        "generateReleaseBaselineProfile - ",
                        "generateAnotherReleaseBaselineProfile - ",
                        "generateFreeReleaseBaselineProfile - ",
                        "generatePaidReleaseBaselineProfile - ",
                        "generateFreeAnotherReleaseBaselineProfile - ",
                        "generatePaidAnotherReleaseBaselineProfile - ",
                    )
            assertThat(notFound).isEmpty()

            // Note that there are no flavor tasks with AGP 8.0 because it would build across
            // multiple build types.
            assertThat(it).apply {
                doesNotContain("generateFreeBaselineProfile")
                doesNotContain("generatePaidBaselineProfile")
            }
        }

        val name = projectSetup.consumer.name

        // 'generateBaselineProfile` does the same of `generateReleaseBaselineProfile`.
        arrayOf("generateBaselineProfile", "generateReleaseBaselineProfile").forEach { cmd ->
            projectSetup.consumer.gradleRunner.build(cmd, "--dry-run") {
                val notFound =
                    it.lines()
                        .require(
                            ":$name:copyFreeReleaseBaselineProfileIntoSrc",
                            ":$name:copyPaidReleaseBaselineProfileIntoSrc",
                        )
                assertThat(notFound).isEmpty()
            }
        }

        projectSetup.consumer.gradleRunner.build(
            "generateAnotherReleaseBaselineProfile",
            "--dry-run"
        ) {
            val notFound =
                it.lines()
                    .require(
                        ":$name:copyFreeAnotherReleaseBaselineProfileIntoSrc",
                        ":$name:copyPaidAnotherReleaseBaselineProfileIntoSrc",
                    )
            assertThat(notFound).isEmpty()
        }
    }

    @Test
    fun testRulesRewriteExperimentalPropertiesSet() {
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2)
        )
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            dependencyOnProducerProject = true,
            flavors = true,
            buildTypeAnotherRelease = true,
            baselineProfileBlock =
                """
                baselineProfileRulesRewrite = true
            """
                    .trimIndent()
        )
        arrayOf(
                "printExperimentalPropertiesForVariantFreeRelease",
                "printExperimentalPropertiesForVariantPaidRelease",
                "printExperimentalPropertiesForVariantFreeAnotherRelease",
                "printExperimentalPropertiesForVariantPaidAnotherRelease",
            )
            .forEach {
                projectSetup.consumer.gradleRunner.buildAndFailAndAssertThatOutput(it) {
                    contains("Unable to set baseline profile rules rewrite property")
                }
            }
    }

    @Test
    fun testDexLayoutOptimizationExperimentalPropertiesSet() {
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2)
        )
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            dependencyOnProducerProject = true,
            flavors = true,
            buildTypeAnotherRelease = true,
            baselineProfileBlock =
                """
                dexLayoutOptimization = true
            """
                    .trimIndent()
        )
        arrayOf(
                "printExperimentalPropertiesForVariantFreeRelease",
                "printExperimentalPropertiesForVariantPaidRelease",
                "printExperimentalPropertiesForVariantFreeAnotherRelease",
                "printExperimentalPropertiesForVariantPaidAnotherRelease",
            )
            .forEach {
                projectSetup.consumer.gradleRunner.buildAndFailAndAssertThatOutput(it) {
                    contains(" Unable to set dex layout optimization property")
                }
            }
    }

    @Test
    fun testSuppressWarningMainGenerateTask() {
        val requiredLines =
            listOf(
                "The task `generateBaselineProfile` does not support generating baseline profiles for",
                "multiple build types with AGP 8.0.",
                "This warning can be disabled setting the following property:",
                "baselineProfile {",
                "    warnings {",
                "        multipleBuildTypesWithAgp80 = false",
                "    }",
                "}"
            )
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
        )

        // Setup with default warnings
        projectSetup.consumer.setup(androidPlugin = ANDROID_APPLICATION_PLUGIN)
        projectSetup.consumer.gradleRunner.build("generateBaselineProfile") {
            println(it)
            val notFound = it.lines().requireInOrder(*requiredLines.toTypedArray())
            assertThat(notFound).isEmpty()
        }

        // Setup turning off warning
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            baselineProfileBlock =
                """
                warnings {
                    multipleBuildTypesWithAgp80 = false
                }
            """
                    .trimIndent()
        )
        projectSetup.consumer.gradleRunner.build("generateBaselineProfile") {
            val notFound = it.lines().requireInOrder(*requiredLines.toTypedArray())
            assertThat(notFound).isEqualTo(requiredLines)
        }
    }
}

@RunWith(Parameterized::class)
class BaselineProfileConsumerPluginTestWithAgp81(private val agpVersion: TestAgpVersion) {

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TestAgpVersion.atLeast(TEST_AGP_VERSION_8_1_0)
    }

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(forceAgpVersion = agpVersion.versionString)

    @Test
    fun verifyGenerateTasks() {
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
            freeAnotherReleaseProfileLines = listOf(Fixtures.CLASS_3_METHOD_1, Fixtures.CLASS_3),
            paidAnotherReleaseProfileLines = listOf(Fixtures.CLASS_4_METHOD_1, Fixtures.CLASS_4),
        )
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            dependencyOnProducerProject = true,
            flavors = true,
            buildTypeAnotherRelease = true
        )
        projectSetup.consumer.gradleRunner.build("tasks") {
            val notFound =
                it.lines()
                    .require(
                        "generateBaselineProfile - ",
                        "generateReleaseBaselineProfile - ",
                        "generateAnotherReleaseBaselineProfile - ",
                        "generateFreeBaselineProfile - ",
                        "generatePaidBaselineProfile - ",
                        "generateFreeReleaseBaselineProfile - ",
                        "generatePaidReleaseBaselineProfile - ",
                        "generateFreeAnotherReleaseBaselineProfile - ",
                        "generatePaidAnotherReleaseBaselineProfile - ",
                    )
            assertThat(notFound).isEmpty()
        }

        val name = projectSetup.consumer.name

        projectSetup.consumer.gradleRunner.build("generateBaselineProfile", "--dry-run") {
            val notFound =
                it.lines()
                    .require(
                        ":$name:copyFreeReleaseBaselineProfileIntoSrc",
                        ":$name:copyPaidReleaseBaselineProfileIntoSrc",
                        ":$name:copyFreeAnotherReleaseBaselineProfileIntoSrc",
                        ":$name:copyPaidAnotherReleaseBaselineProfileIntoSrc",
                    )
            assertThat(notFound).isEmpty()
        }

        projectSetup.consumer.gradleRunner.build("generateReleaseBaselineProfile", "--dry-run") {
            val notFound =
                it.lines()
                    .require(
                        ":$name:copyFreeReleaseBaselineProfileIntoSrc",
                        ":$name:copyPaidReleaseBaselineProfileIntoSrc",
                    )
            assertThat(notFound).isEmpty()
        }

        projectSetup.consumer.gradleRunner.build(
            "generateAnotherReleaseBaselineProfile",
            "--dry-run"
        ) {
            val notFound =
                it.lines()
                    .require(
                        ":$name:copyFreeAnotherReleaseBaselineProfileIntoSrc",
                        ":$name:copyPaidAnotherReleaseBaselineProfileIntoSrc",
                    )
            assertThat(notFound).isEmpty()
        }

        projectSetup.consumer.gradleRunner.build("generateFreeBaselineProfile", "--dry-run") {
            val notFound =
                it.lines()
                    .require(
                        ":$name:copyFreeReleaseBaselineProfileIntoSrc",
                        ":$name:copyFreeAnotherReleaseBaselineProfileIntoSrc",
                    )
            assertThat(notFound).isEmpty()
        }

        projectSetup.consumer.gradleRunner.build("generatePaidBaselineProfile", "--dry-run") {
            val notFound =
                it.lines()
                    .require(
                        ":$name:copyPaidReleaseBaselineProfileIntoSrc",
                        ":$name:copyPaidAnotherReleaseBaselineProfileIntoSrc",
                    )
            assertThat(notFound).isEmpty()
        }
    }

    @Test
    fun verifyTasksWithAndroidTestPlugin() {
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            baselineProfileBlock =
                """
                saveInSrc = true
                automaticGenerationDuringBuild = true
            """
                    .trimIndent(),
            additionalGradleCodeBlock =
                """
                androidComponents {
                    onVariants(selector()) { variant ->
                        tasks.register(variant.name + "BaselineProfileSrcSet", PrintTask) { t ->
                            t.text.set(variant.sources.baselineProfiles.directories.toString())
                        }
                    }
                }
            """
                    .trimIndent()
        )
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2),
        )

        // Asserts that running connected checks on a benchmark variants also triggers
        // baseline profile generation (due to `automaticGenerationDuringBuild` true`).
        projectSetup.producer.gradleRunner.build(
            "connectedFreeBenchmarkReleaseAndroidTest",
            "--dry-run"
        ) { text ->
            val consumerName = projectSetup.consumer.name
            val producerName = projectSetup.producer.name

            val notFound =
                text
                    .lines()
                    .requireInOrder(
                        ":$consumerName:packageFreeNonMinifiedRelease",
                        ":$producerName:connectedFreeNonMinifiedReleaseAndroidTest",
                        ":$producerName:collectFreeNonMinifiedReleaseBaselineProfile",
                        ":$consumerName:mergeFreeReleaseBaselineProfile",
                        ":$consumerName:copyFreeReleaseBaselineProfileIntoSrc",
                        ":$consumerName:mergeFreeBenchmarkReleaseArtProfile",
                        ":$consumerName:compileFreeBenchmarkReleaseArtProfile",
                        ":$consumerName:packageFreeBenchmarkRelease",
                        ":$consumerName:createFreeBenchmarkReleaseApkListingFileRedirect",
                        ":$producerName:connectedFreeBenchmarkReleaseAndroidTest"
                    )

            assertThat(notFound).isEmpty()
        }
    }

    @Test
    fun automaticGenerationDuringBuildNotCompatibleWithLibraryModule() {
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            baselineProfileBlock =
                """
                saveInSrc = true
                automaticGenerationDuringBuild = true
            """
                    .trimIndent()
        )
        projectSetup.producer.setupWithoutFlavors(
            releaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
        )

        // Asserts that running connected checks on a benchmark variants also triggers
        // baseline profile generation (due to `automaticGenerationDuringBuild` true`).
        projectSetup.consumer.gradleRunner.buildAndFailAndAssertThatOutput(
            "generateBaselineProfile",
            "--dry-run"
        ) {
            contains(
                "The flag `automaticGenerationDuringBuild` is not compatible with library " +
                    "modules. Please remove the flag `automaticGenerationDuringBuild` " +
                    "in your com.android.library module"
            )
        }
    }

    @Test
    fun testExperimentalPropertiesSet() {
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2)
        )
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            dependencyOnProducerProject = true,
            flavors = true,
            buildTypeAnotherRelease = true,
            baselineProfileBlock =
                """
                baselineProfileRulesRewrite = true
                dexLayoutOptimization = true
            """
                    .trimIndent()
        )

        arrayOf(
                "printExperimentalPropertiesForVariantFreeRelease",
                "printExperimentalPropertiesForVariantPaidRelease",
                "printExperimentalPropertiesForVariantFreeAnotherRelease",
                "printExperimentalPropertiesForVariantPaidAnotherRelease",
            )
            .forEach {
                projectSetup.consumer.gradleRunner.buildAndAssertThatOutput(it) {
                    // These properties are ignored in agp 8.0
                    contains("android.experimental.art-profile-r8-rewriting=true")
                    contains("android.experimental.r8.dex-startup-optimization=true")
                }
            }
    }

    @Test
    fun testGenerateTaskWithFlavorsAndMergeAll() {
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            flavors = true,
            dependencyOnProducerProject = true,
            baselineProfileBlock =
                """
                mergeIntoMain = true
            """
                    .trimIndent()
        )
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2)
        )

        // Asserts that all per-variant, per-flavor and per-build type tasks are being generated.
        projectSetup.consumer.gradleRunner.buildAndAssertThatOutput("tasks") {
            contains("generateBaselineProfile - ")
            doesNotContain("generateReleaseBaselineProfile - ")
            doesNotContain("generateFreeReleaseBaselineProfile - ")
            doesNotContain("generatePaidReleaseBaselineProfile - ")
        }

        projectSetup.consumer.gradleRunner
            .withArguments("generateBaselineProfile", "--stacktrace")
            .build()

        val lines =
            File(
                    projectSetup.consumer.rootDir,
                    "src/main/$EXPECTED_PROFILE_FOLDER/baseline-prof.txt"
                )
                .readLines()
        assertThat(lines)
            .containsExactly(
                Fixtures.CLASS_1,
                Fixtures.CLASS_1_METHOD_1,
                Fixtures.CLASS_2,
                Fixtures.CLASS_2_METHOD_1,
            )
    }

    @Test
    fun testExperimentalPropertyHideVariantInAndroidStudio() {
        projectSetup.producer.setupWithFreeAndPaidFlavors(
            freeReleaseProfileLines = listOf(Fixtures.CLASS_1_METHOD_1, Fixtures.CLASS_1),
            paidReleaseProfileLines = listOf(Fixtures.CLASS_2_METHOD_1, Fixtures.CLASS_2)
        )

        val taskList =
            listOf(
                "printExperimentalPropertiesForVariantFreeNonMinifiedRelease",
                "printExperimentalPropertiesForVariantFreeBenchmarkRelease",
                "printExperimentalPropertiesForVariantPaidNonMinifiedRelease",
                "printExperimentalPropertiesForVariantPaidBenchmarkRelease",
                "printExperimentalPropertiesForVariantFreeNonMinifiedAnotherRelease",
                "printExperimentalPropertiesForVariantFreeBenchmarkAnotherRelease",
                "printExperimentalPropertiesForVariantPaidNonMinifiedAnotherRelease",
                "printExperimentalPropertiesForVariantPaidBenchmarkAnotherRelease",
            )

        // Setup consumer module with DEFAULT configuration
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            dependencyOnProducerProject = true,
            flavors = true,
            buildTypeAnotherRelease = true,
        )
        taskList.forEach {
            projectSetup.consumer.gradleRunner.buildAndAssertThatOutput(it) {
                contains("androidx.baselineProfile.hideInStudio=true")
            }
        }

        // Setup consumer module NOT HIDING the build types
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            dependencyOnProducerProject = true,
            flavors = true,
            buildTypeAnotherRelease = true,
            baselineProfileBlock =
                """
                hideSyntheticBuildTypesInAndroidStudio = false
            """
                    .trimIndent()
        )
        taskList.forEach {
            projectSetup.consumer.gradleRunner.buildAndAssertThatOutput(it) {
                doesNotContain("androidx.baselineProfile.hideInStudio=")
            }
        }

        // Setup consumer module HIDING the build types
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            dependencyOnProducerProject = true,
            flavors = true,
            buildTypeAnotherRelease = true,
            baselineProfileBlock =
                """
                hideSyntheticBuildTypesInAndroidStudio = true
            """
                    .trimIndent()
        )
        taskList.forEach {
            projectSetup.consumer.gradleRunner.buildAndAssertThatOutput(it) {
                contains("androidx.baselineProfile.hideInStudio=true")
            }
        }
    }
}

@RunWith(Parameterized::class)
class BaselineProfileConsumerPluginTestWithAgp83(private val agpVersion: TestAgpVersion) {

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TestAgpVersion.atLeast(TEST_AGP_VERSION_8_3_1)
    }

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(forceAgpVersion = agpVersion.versionString)

    private val gradleRunner by lazy { projectSetup.consumer.gradleRunner }

    @Test
    fun testSrcSetAreAddedToVariantsForLibraries() {
        projectSetup.producer.setupWithoutFlavors()
        projectSetup.consumer.setup(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            additionalGradleCodeBlock =
                """
                androidComponents {
                    onVariants(selector()) { variant ->
                        tasks.register(variant.name + "Sources", DisplaySourceSets) { t ->
                            t.srcs.set(variant.sources.baselineProfiles.all)
                        }
                    }
                }
            """
                    .trimIndent()
        )

        val expected =
            listOf(
                    "src/main/baselineProfiles",
                    "src/main/generated/baselineProfiles",
                    "src/release/baselineProfiles",
                )
                .map { dir -> File(projectSetup.consumer.rootDir, dir) }
                .onEach { f ->
                    // Expected src set location. Note that src sets are not added if the folder
                    // does
                    // not exist so we need to create it.
                    f.mkdirs()
                    f.deleteOnExit()
                }

        gradleRunner.buildAndAssertThatOutput("releaseSources") {
            expected.forEach { e -> contains(e.absolutePath) }
        }
    }
}

@RunWith(Parameterized::class)
class BaselineProfileConsumerPluginTestWithKmp(agpVersion: TestAgpVersion) {

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TestAgpVersion.atLeast(TEST_AGP_VERSION_8_3_1)
    }

    @get:Rule
    val projectSetup =
        BaselineProfileProjectSetupRule(
            forceAgpVersion = agpVersion.versionString,
            addKotlinGradlePluginToClasspath = true
        )

    private val gradleRunner by lazy { projectSetup.consumer.gradleRunner }

    @Test
    fun testSrcSetAreAddedToVariantsForApplicationsWithKmp() {
        projectSetup.producer.setupWithoutFlavors(releaseProfileLines = listOf())
        projectSetup.consumer.setupWithBlocks(
            androidPlugin = ANDROID_APPLICATION_PLUGIN,
            otherPluginsBlock =
                """
                id("org.jetbrains.kotlin.multiplatform")
            """
                    .trimIndent(),
            dependenciesBlock =
                """
                implementation(project(":${projectSetup.dependency.name}"))
            """
                    .trimIndent(),
            additionalGradleCodeBlock =
                """
                kotlin {
                    jvm { }
                    androidTarget { }
                    sourceSets {
                        androidMain { }
                    }
                }

                androidComponents {
                    onVariants(selector()) { variant ->
                        tasks.register(variant.name + "Sources", DisplaySourceSets) { t ->
                            t.srcs.set(variant.sources.baselineProfiles.all)
                        }
                    }
                }
            """
                    .trimIndent()
        )

        val expected =
            listOf(
                    "src/main/baselineProfiles",
                    "src/release/baselineProfiles",
                    "src/androidRelease/generated/baselineProfiles",
                )
                .map { dir -> File(projectSetup.consumer.rootDir, dir) }
                .onEach { f ->
                    // Expected src set location. Note that src sets are not added if the folder
                    // does
                    // not exist so we need to create it.
                    f.mkdirs()
                    f.deleteOnExit()
                }

        gradleRunner.buildAndAssertThatOutput("releaseSources") {
            expected.forEach { e -> contains(e.absolutePath) }
        }
    }

    @Test
    fun testSrcSetAreAddedToVariantsForLibrariesWithKmp() {
        projectSetup.producer.setupWithoutFlavors(releaseProfileLines = listOf())
        projectSetup.consumer.setupWithBlocks(
            androidPlugin = ANDROID_LIBRARY_PLUGIN,
            otherPluginsBlock =
                """
                id("org.jetbrains.kotlin.multiplatform")
            """
                    .trimIndent(),
            dependenciesBlock =
                """
                implementation(project(":${projectSetup.dependency.name}"))
            """
                    .trimIndent(),
            additionalGradleCodeBlock =
                """
                kotlin {
                    jvm { }
                    androidTarget("androidTargetCustom") { }
                }

                androidComponents {
                    onVariants(selector()) { variant ->
                        tasks.register(variant.name + "Sources", DisplaySourceSets) { t ->
                            t.srcs.set(variant.sources.baselineProfiles.all)
                        }
                    }
                }
            """
                    .trimIndent()
        )

        val expected =
            listOf(
                    "src/main/baselineProfiles",
                    "src/release/baselineProfiles",
                    "src/androidTargetCustomMain/generated/baselineProfiles",
                )
                .map { dir -> File(projectSetup.consumer.rootDir, dir) }
                .onEach { f ->
                    // Expected src set location. Note that src sets are not added if the folder
                    // does
                    // not exist so we need to create it.
                    f.mkdirs()
                    f.deleteOnExit()
                }

        gradleRunner.buildAndAssertThatOutput("releaseSources") {
            expected.forEach { e -> contains(e.absolutePath) }
        }
    }
}

@RunWith(Parameterized::class)
class BaselineProfileConsumerPluginTestWithFtl(agpVersion: TestAgpVersion) {

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TestAgpVersion.atLeast(TEST_AGP_VERSION_CURRENT)
    }

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(forceAgpVersion = agpVersion.versionString)

    @Test
    fun testGenerateBaselineProfileWithFtlArtifact() {
        projectSetup.consumer.setup(androidPlugin = ANDROID_APPLICATION_PLUGIN)

        // The difference with FTL is that artifacts are added as global artifacts instead of
        // per test result. This different setup can be specified in the `VariantProfile`.
        projectSetup.producer.setup(
            variantProfiles =
                VariantProfile.release(
                    ftlFileLines =
                        listOf(
                            Fixtures.CLASS_2_METHOD_1,
                        ),
                )
        )

        projectSetup.consumer.gradleRunner.build("generateBaselineProfile", "--info") {
            println(it)
            val notFound =
                it.lines()
                    .requireInOrder(
                        "A baseline profile was generated for the variant `release`:",
                        "${projectSetup.baselineProfileFile("release").toUri()}",
                    )
            assertThat(notFound).isEmpty()
        }

        assertThat(projectSetup.readBaselineProfileFileContent("release"))
            .containsExactly(Fixtures.CLASS_2_METHOD_1)
    }
}
