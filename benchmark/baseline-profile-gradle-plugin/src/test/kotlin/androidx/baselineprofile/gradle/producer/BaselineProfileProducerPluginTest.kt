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

package androidx.baselineprofile.gradle.producer

import androidx.baselineprofile.gradle.utils.BaselineProfileProjectSetupRule
import androidx.baselineprofile.gradle.utils.TestAgpVersion
import androidx.baselineprofile.gradle.utils.TestAgpVersion.TEST_AGP_VERSION_8_1_0
import androidx.baselineprofile.gradle.utils.TestAgpVersion.TEST_AGP_VERSION_8_2_0
import androidx.baselineprofile.gradle.utils.TestAgpVersion.TEST_AGP_VERSION_8_3_1
import androidx.baselineprofile.gradle.utils.TestAgpVersion.TEST_AGP_VERSION_8_4_2
import androidx.baselineprofile.gradle.utils.VariantProfile
import androidx.baselineprofile.gradle.utils.build
import androidx.baselineprofile.gradle.utils.buildAndAssertThatOutput
import androidx.baselineprofile.gradle.utils.buildAndFailAndAssertThatOutput
import androidx.baselineprofile.gradle.utils.require
import com.google.common.truth.StringSubject
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private val GRADLE_PRINT_ARGS_TASK =
    """
abstract class PrintArgsTask extends DefaultTask {
    @Input abstract MapProperty<String, String> getProperties()
    @TaskAction void exec() {
        for (Map.Entry<String, String> e : getProperties().get().entrySet()) {
            println(e.key + "=" + e.value)
        }
    }
}
androidComponents {
    onVariants(selector()) { variant ->
        tasks.register(variant.name + "Arguments", PrintArgsTask) { t ->
            t.properties.set(variant.instrumentationRunnerArguments)
        }
    }
}
"""
        .trimIndent()

@RunWith(Parameterized::class)
class BaselineProfileProducerPluginTest(agpVersion: TestAgpVersion) {

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TestAgpVersion.values()
    }

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(forceAgpVersion = agpVersion.versionString)

    private val emptyReleaseVariantProfile =
        VariantProfile(flavor = null, buildType = "release", profileFileLines = mapOf())

    @Test
    fun verifyTasksWithAndroidTestPlugin() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget
        )

        projectSetup.producer.gradleRunner.build("tasks") {
            val notFound =
                it.lines()
                    .require(
                        "connectedNonMinifiedReleaseAndroidTest - ",
                        "collectNonMinifiedReleaseBaselineProfile - "
                    )
            assertThat(notFound).isEmpty()
        }
    }

    @Test
    fun nonExistingManagedDeviceShouldThrowError() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget,
            managedDevices = listOf(),
            baselineProfileBlock =
                """
                managedDevices = ["nonExisting"]
            """
                    .trimIndent()
        )

        projectSetup.producer.gradleRunner.buildAndFailAndAssertThatOutput("tasks") {
            contains("No managed device named `nonExisting` was found.")
        }
    }

    @Test
    fun existingManagedDeviceShouldCreateCollectTaskDependingOnManagedDeviceTask() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget,
            managedDevices = listOf("somePixelDevice"),
            baselineProfileBlock =
                """
                managedDevices = ["somePixelDevice"]
            """
                    .trimIndent()
        )

        projectSetup.producer.gradleRunner.build(
            "collectNonMinifiedReleaseBaselineProfile",
            "--dry-run"
        ) {
            val appTargetName = projectSetup.appTarget.name
            val producerName = projectSetup.producer.name
            val notFound =
                it.lines()
                    .require(
                        ":$appTargetName:packageNonMinifiedRelease",
                        ":$producerName:somePixelDeviceNonMinifiedReleaseAndroidTest",
                        ":$producerName:connectedNonMinifiedReleaseAndroidTest",
                        ":$producerName:collectNonMinifiedReleaseBaselineProfile"
                    )
            assertThat(notFound).isEmpty()
        }
    }

    @Test
    fun skipGenerationPropertyShouldDisableTestTasks() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget,
            managedDevices = listOf("somePixelDevice"),
            baselineProfileBlock =
                """
                managedDevices = ["somePixelDevice"]
            """
                    .trimIndent(),
            additionalGradleCodeBlock =
                """
                afterEvaluate {
                    for (String taskName : [
                            "somePixelDeviceNonMinifiedReleaseAndroidTest",
                            "collectNonMinifiedReleaseBaselineProfile"]) {
                        def task = tasks.getByName(taskName)
                        println(taskName + "=" + task.enabled)
                    }
                }
            """
                    .trimIndent()
        )

        // Execute any task and check the expected output.
        // Note that executing `somePixelDeviceSetup` will fail for `LicenseNotAcceptedException`.
        projectSetup.producer.gradleRunner.build(
            "tasks",
            "-Pandroidx.baselineprofile.skipgeneration"
        ) {
            val notFound =
                it.lines()
                    .require(
                        "somePixelDeviceNonMinifiedReleaseAndroidTest=false",
                        "collectNonMinifiedReleaseBaselineProfile=false"
                    )
            assertThat(notFound).isEmpty()
        }
    }

    @Test
    fun whenUseOnlyConnectedDevicesShouldOverrideDsl() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget,
            managedDevices = listOf("somePixelDevice"),
            baselineProfileBlock =
                """
                managedDevices = ["somePixelDevice"]
                useConnectedDevices = false
            """
                    .trimIndent()
        )

        // Execute any task and check the expected output.
        // Note that executing `somePixelDeviceSetup` will fail for `LicenseNotAcceptedException`.
        projectSetup.producer.gradleRunner.buildAndAssertThatOutput(
            "collectNonMinifiedReleaseBaselineProfile",
            "--dry-run",
            "-Pandroidx.baselineprofile.forceonlyconnecteddevices"
        ) {
            contains("connectedNonMinifiedReleaseAndroidTest")
            doesNotContain("somePixelDeviceNonMinifiedReleaseAndroidTest")
        }
    }

    @Test
    fun whenNotUseOnlyConnectedDevicesShouldOverrideDsl() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget,
            managedDevices = listOf("somePixelDevice"),
            baselineProfileBlock =
                """
                managedDevices = ["somePixelDevice"]
                useConnectedDevices = false
            """
                    .trimIndent()
        )

        // Execute any task and check the expected output.
        // Note that executing `somePixelDeviceSetup` will fail for `LicenseNotAcceptedException`.
        projectSetup.producer.gradleRunner.buildAndAssertThatOutput(
            "collectNonMinifiedReleaseBaselineProfile",
            "--dry-run",
        ) {
            doesNotContain("connectedNonMinifiedReleaseAndroidTest")
            contains("somePixelDeviceNonMinifiedReleaseAndroidTest")
        }
    }
}

@RunWith(Parameterized::class)
class BaselineProfileProducerPluginTestWithAgp81AndAbove(agpVersion: TestAgpVersion) {

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TestAgpVersion.atLeast(TEST_AGP_VERSION_8_1_0)
    }

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(forceAgpVersion = agpVersion.versionString)

    private val emptyReleaseVariantProfile =
        VariantProfile(flavor = null, buildType = "release", profileFileLines = mapOf())

    @Test
    fun verifyTasksWithAndroidTestPlugin() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget
        )

        projectSetup.producer.gradleRunner.build("tasks") {
            val notFound =
                it.lines()
                    .require(
                        "connectedNonMinifiedReleaseAndroidTest - ",
                        "connectedBenchmarkReleaseAndroidTest - ",
                        "collectNonMinifiedReleaseBaselineProfile - "
                    )
            assertThat(notFound).isEmpty()
        }
    }
}

@RunWith(Parameterized::class)
class BaselineProfileProducerPluginTestWithAgp82AndAbove(agpVersion: TestAgpVersion) {

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TestAgpVersion.atLeast(TEST_AGP_VERSION_8_2_0)
    }

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(forceAgpVersion = agpVersion.versionString)

    private val emptyReleaseVariantProfile =
        VariantProfile(flavor = null, buildType = "release", profileFileLines = mapOf())

    @Test
    fun verifyInstrumentationRunnerArgumentsAreSet() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget,
            additionalGradleCodeBlock = GRADLE_PRINT_ARGS_TASK
        )

        data class AssertData(
            val taskName: String,
            val applyProp: Boolean,
            val assertBlock: StringSubject.() -> (Unit)
        )

        arrayOf(
                AssertData("benchmarkReleaseArguments", false) {
                    contains("androidx.benchmark.enabledRules=macrobenchmark")
                    contains("androidx.benchmark.skipOnEmulator=true")
                },
                AssertData("nonMinifiedReleaseArguments", false) {
                    contains("androidx.benchmark.enabledRules=baselineprofile")
                },
                AssertData("benchmarkReleaseArguments", true) {
                    doesNotContain("androidx.benchmark.enabledRules=macrobenchmark")
                    contains("androidx.benchmark.skipOnEmulator=true")
                },
                AssertData("nonMinifiedReleaseArguments", true) {
                    doesNotContain("androidx.benchmark.enabledRules=baselineprofile")
                },
            )
            .forEach {
                projectSetup.producer.gradleRunner.buildAndAssertThatOutput(
                    arguments =
                        listOfNotNull(
                                it.taskName,
                                if (it.applyProp) "-Pandroidx.baselineprofile.dontdisablerules"
                                else null
                            )
                            .toTypedArray(),
                    assertBlock = it.assertBlock
                )
            }
    }

    @Test
    fun runWhenInstrumentationRunnerArgumentsAreSetManually() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget
        )

        val enabledRuleProp =
            "-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules"
        projectSetup.producer.gradleRunner.build(
            "connectedBenchmarkReleaseAndroidTest",
            "$enabledRuleProp=Macrobenchmark"
        ) {
            // This should not fail.
        }

        projectSetup.producer.gradleRunner.build(
            "connectedNonMinifiedReleaseAndroidTest",
            "$enabledRuleProp=BaselineProfile"
        ) {
            // This should not fail.
        }
    }
}

@RunWith(Parameterized::class)
class BaselineProfileProducerPluginTestWithAgp83AndAbove(agpVersion: TestAgpVersion) {

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TestAgpVersion.atLeast(TEST_AGP_VERSION_8_3_1)
    }

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(forceAgpVersion = agpVersion.versionString)

    private val emptyReleaseVariantProfile =
        VariantProfile(flavor = null, buildType = "release", profileFileLines = mapOf())

    @Test
    fun verifyTargetPackageNamePassedAsInstrumentationRunnerArgumentWithOverride() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget,
            additionalGradleCodeBlock = GRADLE_PRINT_ARGS_TASK
        )

        val prop =
            "-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.targetPackageName"
        arrayOf(
                arrayOf(
                    "benchmarkReleaseArguments",
                    "$prop=com.someotherpackage1",
                    "androidx.benchmark.targetPackageName=com.someotherpackage1"
                ),
                arrayOf(
                    "nonMinifiedReleaseArguments",
                    "$prop=com.someotherpackage2",
                    "androidx.benchmark.targetPackageName=com.someotherpackage2"
                ),
            )
            .forEach {
                projectSetup.producer.gradleRunner.buildAndAssertThatOutput(it[0], it[1]) {
                    // Note that if the targetPackageName argument is overridden from CLI
                    // then it shouldn't be in the runner arguments map at this stage, as it's
                    // added later by the test plugin.
                    doesNotContain(it[2])
                }
            }
    }
}

@RunWith(Parameterized::class)
class BaselineProfileProducerPluginTestWithAgp84AndAbove(agpVersion: TestAgpVersion) {

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TestAgpVersion.atLeast(TEST_AGP_VERSION_8_4_2)
    }

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(forceAgpVersion = agpVersion.versionString)

    private val emptyReleaseVariantProfile =
        VariantProfile(flavor = null, buildType = "release", profileFileLines = mapOf())

    @Test
    fun verifyTargetPackageNamePassedAsInstrumentationRunnerArgument() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget,
            additionalGradleCodeBlock = GRADLE_PRINT_ARGS_TASK
        )
        arrayOf(
                Pair(
                    "benchmarkReleaseArguments",
                    "androidx.benchmark.targetPackageName=com.example.namespace"
                ),
                Pair(
                    "nonMinifiedReleaseArguments",
                    "androidx.benchmark.targetPackageName=com.example.namespace"
                ),
            )
            .forEach {
                projectSetup.producer.gradleRunner.buildAndAssertThatOutput(it.first) {
                    contains(it.second)
                }
            }
    }
}
