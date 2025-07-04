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

package androidx.baselineprofile.gradle.apptarget

import androidx.baselineprofile.gradle.utils.BaselineProfileProjectSetupRule
import androidx.baselineprofile.gradle.utils.TestAgpVersion
import androidx.baselineprofile.gradle.utils.TestAgpVersion.TEST_AGP_VERSION_8_1_1
import androidx.baselineprofile.gradle.utils.build
import androidx.baselineprofile.gradle.utils.buildAndAssertThatOutput
import androidx.baselineprofile.gradle.utils.containsOnly
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private fun createBuildGradle(overrideExtendedBuildTypesForRelease: Boolean = false) =
    """
    import static com.android.build.gradle.internal.ProguardFileType.EXPLICIT;

    plugins {
        id("com.android.application")
        id("androidx.baselineprofile.apptarget")
    }

    android {
        namespace 'com.example.namespace'
        buildTypes {

            ${
        if (overrideExtendedBuildTypesForRelease) """

            benchmarkRelease {
                initWith(release)
                profileable true
            }
            nonMinifiedRelease {
                initWith(release)
            }
   
            """.trimIndent() else ""
    }

            anotherRelease {
                initWith(release)
                minifyEnabled true
                postprocessing {
                    proguardFile("proguard-rules1.pro")
                }
            }
            myCustomRelease {
                initWith(release)
                minifyEnabled false
                proguardFiles "proguard-rules2.pro"
            }
            benchmarkMyCustomRelease {
                initWith(myCustomRelease)

                // These are the opposite of default ensure the plugin doesn't modify them
                minifyEnabled false
                shrinkResources false
            }
            nonMinifiedMyCustomRelease {
                initWith(myCustomRelease)

                // These are the opposite of default ensure the plugin does modify them
                debuggable true
                minifyEnabled true
                shrinkResources true
                profileable false
            }
        }
    }

    def printVariantsTaskProvider = tasks.register("printVariants", PrintTask) { t ->
        t.text.set("")
    }

    androidComponents {
        onVariants(selector()) { variant ->
            printVariantsTaskProvider.configure { t ->
                t.text.set(t.text.get() + "\n" + "print-variant:" + variant.name)
            }
            tasks.register(variant.name + "BuildProperties", PrintTask) { t ->
                def buildType = android.buildTypes[variant.buildType]
                def text = "minifyEnabled=" + buildType.minifyEnabled.toString() + "\n"
                text += "testCoverageEnabled=" + buildType.testCoverageEnabled.toString() + "\n"
                text += "debuggable=" + buildType.debuggable.toString() + "\n"
                text += "profileable=" + buildType.profileable.toString() + "\n"
                text += "proguardFiles=" + buildType.proguardFiles.toString() + "\n"
                text += "postProcessingProguardFiles=" + buildType.postprocessing.getProguardFiles(EXPLICIT) + "\n"
                t.text.set(text)
            }
            tasks.register(variant.name + "JavaSources", DisplaySourceSets) { t ->
                t.srcs.set(variant.sources.java.all)
            }
            tasks.register(variant.name + "KotlinSources", DisplaySourceSets) { t ->
                t.srcs.set(variant.sources.kotlin.all)
            }
        }
    }
    """
        .trimIndent()

@RunWith(Parameterized::class)
class BaselineProfileAppTargetPluginTestWithAgp81AndAbove(agpVersion: TestAgpVersion) {

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TestAgpVersion.atLeast(TEST_AGP_VERSION_8_1_1)
    }

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(forceAgpVersion = agpVersion.versionString)

    private val buildGradle = createBuildGradle()

    @Test
    fun additionalBuildTypesShouldNotBeCreatedForExistingNonMinifiedAndBenchmarkBuildTypes() =
        arrayOf(true, false).forEach { overrideExtendedBuildTypesForRelease ->
            projectSetup.appTarget.setBuildGradle(
                buildGradleContent =
                    createBuildGradle(
                        overrideExtendedBuildTypesForRelease = overrideExtendedBuildTypesForRelease
                    )
            )
            projectSetup.appTarget.gradleRunner.build("printVariants") {
                val variants =
                    it.lines()
                        .filter { l -> l.startsWith("print-variant:") }
                        .map { l -> l.substringAfter("print-variant:").trim() }
                        .toSet()
                        .toList()

                assertThat(
                        variants.containsOnly(
                            "debug",
                            "release",
                            "benchmarkRelease",
                            "nonMinifiedRelease",
                            "anotherRelease",
                            "nonMinifiedAnotherRelease",
                            "benchmarkAnotherRelease",
                            "myCustomRelease",
                            "nonMinifiedMyCustomRelease",
                            "benchmarkMyCustomRelease",
                        )
                    )
                    .isTrue()
            }
        }

    @Test
    fun verifyUnitTestDisabled() {
        projectSetup.appTarget.setBuildGradle(buildGradle)
        projectSetup.appTarget.gradleRunner.buildAndAssertThatOutput("test", "--dry-run") {
            contains(":testDebugUnitTest ")
            contains(":testReleaseUnitTest ")
            contains(":testAnotherReleaseUnitTest ")
            doesNotContain(":testNonMinifiedReleaseUnitTest ")
            doesNotContain(":testBenchmarkReleaseUnitTest ")
            doesNotContain(":testNonMinifiedAnotherReleaseUnitTest ")
            doesNotContain(":testBenchmarkAnotherReleaseUnitTest ")
        }
    }

    @Test
    fun verifyNewBuildTypes() {
        projectSetup.appTarget.setBuildGradle(buildGradle)

        // Assert properties of the benchmark build types
        projectSetup.appTarget.gradleRunner.buildAndAssertThatOutput(
            "benchmarkReleaseBuildProperties"
        ) {
            contains("testCoverageEnabled=false")
            contains("debuggable=false")
            contains("profileable=true")

            // This value is false for `release` so it should be copied over.
            contains("minifyEnabled=false")
        }

        projectSetup.appTarget.gradleRunner.buildAndAssertThatOutput(
            "benchmarkAnotherReleaseBuildProperties"
        ) {
            contains("testCoverageEnabled=false")
            contains("debuggable=false")
            contains("profileable=true")

            // This value is true for `release` so it should be copied over.
            contains("minifyEnabled=true")
        }

        // Assert properties of the baseline profile build types.
        arrayOf("nonMinifiedReleaseBuildProperties", "nonMinifiedAnotherReleaseBuildProperties")
            .forEach { taskName ->
                projectSetup.appTarget.gradleRunner.buildAndAssertThatOutput(taskName) {
                    contains("minifyEnabled=false")
                    contains("testCoverageEnabled=false")
                    contains("debuggable=false")
                    contains("profileable=true")
                }
            }
    }

    @Test
    fun verifyOverrideBuildTypes() {
        projectSetup.appTarget.setBuildGradle(buildGradle)

        projectSetup.appTarget.gradleRunner.buildAndAssertThatOutput(
            "benchmarkMyCustomReleaseBuildProperties"
        ) {

            // Should be overridden
            contains("testCoverageEnabled=false")
            contains("debuggable=false")
            contains("profileable=true")

            // Should not be overridden
            contains("minifyEnabled=false")
        }

        projectSetup.appTarget.gradleRunner.buildAndAssertThatOutput(
            "nonMinifiedMyCustomReleaseBuildProperties"
        ) {

            // Should all be overridden
            contains("minifyEnabled=false")
            contains("testCoverageEnabled=false")
            contains("debuggable=false")
            contains("profileable=true")
        }
    }

    @Test
    fun verifyProguardFilesAreCopiedInExtendedBuildTypes() {
        projectSetup.appTarget.setBuildGradle(buildGradle)

        data class TaskAndExpected(
            val benchmarkBuildType: String,
            val baselineProfileBuildType: String,
            val expectedProguardFile: String?,
            val expectedPostProcessingProguardFile: String?,
        )

        arrayOf(
                TaskAndExpected(
                    benchmarkBuildType = "benchmarkRelease",
                    baselineProfileBuildType = "nonMinifiedRelease",
                    expectedProguardFile = null,
                    expectedPostProcessingProguardFile = null,
                ),
                TaskAndExpected(
                    benchmarkBuildType = "benchmarkAnotherRelease",
                    baselineProfileBuildType = "nonMinifiedAnotherRelease",
                    expectedProguardFile = null,
                    expectedPostProcessingProguardFile = "proguard-rules1.pro",
                ),
                TaskAndExpected(
                    benchmarkBuildType = "benchmarkMyCustomRelease",
                    baselineProfileBuildType = "nonMinifiedMyCustomRelease",
                    expectedProguardFile = "proguard-rules2.pro",
                    expectedPostProcessingProguardFile = null,
                ),
            )
            .forEach {
                projectSetup.appTarget.gradleRunner.buildAndAssertThatOutput(
                    "${it.benchmarkBuildType}BuildProperties"
                ) {
                    if (it.expectedProguardFile != null) {
                        contains(
                            "proguardFiles=[${
                                File(
                                    projectSetup.appTarget.rootDir.canonicalFile,
                                    it.expectedProguardFile,
                                )
                            }]"
                        )
                    }
                    if (it.expectedPostProcessingProguardFile != null) {
                        containsMatch(
                            "postProcessingProguardFiles=\\[[^,]+, ${
                                File(
                                    projectSetup.appTarget.rootDir.canonicalFile,
                                    it.expectedPostProcessingProguardFile,
                                )
                            }"
                        )
                    }
                }
                projectSetup.appTarget.gradleRunner.buildAndAssertThatOutput(
                    "${it.baselineProfileBuildType}BuildProperties"
                ) {
                    if (it.expectedProguardFile != null) {
                        contains(
                            "proguardFiles=[${
                                File(
                                    projectSetup.appTarget.rootDir.canonicalFile,
                                    it.expectedProguardFile,
                                )
                            }]"
                        )
                    }
                    if (it.expectedPostProcessingProguardFile != null) {
                        containsMatch(
                            "postProcessingProguardFiles=\\[[^,]+, ${
                                File(
                                    projectSetup.appTarget.rootDir.canonicalFile,
                                    it.expectedPostProcessingProguardFile,
                                )
                            }"
                        )
                    }
                }
            }
    }
}

@RunWith(Parameterized::class)
class BaselineProfileAppTargetPluginTestWithAgp80AndAbove(agpVersion: TestAgpVersion) {

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TestAgpVersion.atLeast(TEST_AGP_VERSION_8_1_1)
    }

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(forceAgpVersion = agpVersion.versionString)

    private val buildGradle = createBuildGradle()

    @Test
    fun testSrcSetAreAddedToVariantsForApplications() {
        projectSetup.appTarget.setBuildGradle(buildGradle)

        data class TaskAndExpected(val taskName: String, val expectedDirs: List<String>)

        arrayOf(
                TaskAndExpected(
                    taskName = "nonMinifiedAnotherReleaseJavaSources",
                    expectedDirs =
                        listOf(
                            "src/main/java",
                            "src/anotherRelease/java",
                            "src/nonMinifiedAnotherRelease/java",
                        ),
                ),
                TaskAndExpected(
                    taskName = "nonMinifiedReleaseJavaSources",
                    expectedDirs =
                        listOf("src/main/java", "src/release/java", "src/nonMinifiedRelease/java"),
                ),
                TaskAndExpected(
                    taskName = "nonMinifiedAnotherReleaseKotlinSources",
                    expectedDirs =
                        listOf(
                            "src/main/kotlin",
                            "src/anotherRelease/kotlin",
                            "src/nonMinifiedAnotherRelease/kotlin",
                        ),
                ),
                TaskAndExpected(
                    taskName = "nonMinifiedReleaseKotlinSources",
                    expectedDirs =
                        listOf(
                            "src/main/kotlin",
                            "src/release/kotlin",
                            "src/nonMinifiedRelease/kotlin",
                        ),
                ),
            )
            .forEach { t ->

                // Runs the task and assert
                projectSetup.appTarget.gradleRunner.buildAndAssertThatOutput(t.taskName) {
                    t.expectedDirs
                        .map { File(projectSetup.appTarget.rootDir, it) }
                        .forEach { e -> contains(e.absolutePath) }
                }
            }
    }

    @Test
    fun additionalBuildTypesShouldNotBeCreatedForExistingNonMinifiedAndBenchmarkBuildTypes() =
        arrayOf(true, false).forEach { overrideExtendedBuildTypesForRelease ->
            projectSetup.appTarget.setBuildGradle(
                buildGradleContent =
                    createBuildGradle(
                        overrideExtendedBuildTypesForRelease = overrideExtendedBuildTypesForRelease
                    )
            )

            projectSetup.appTarget.gradleRunner.build("printVariants") {
                val variants =
                    it.lines()
                        .filter { l -> l.startsWith("print-variant:") }
                        .map { l -> l.substringAfter("print-variant:").trim() }
                        .toSet()
                        .toList()

                assertThat(
                        variants.containsOnly(
                            "debug",
                            "release",
                            "nonMinifiedRelease",
                            "anotherRelease",
                            "nonMinifiedAnotherRelease",
                            "myCustomRelease",
                            "nonMinifiedMyCustomRelease",
                        )
                    )
                    .isTrue()
            }
        }
}
