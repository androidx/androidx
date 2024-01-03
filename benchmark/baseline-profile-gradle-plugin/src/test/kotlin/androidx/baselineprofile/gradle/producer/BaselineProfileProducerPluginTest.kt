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
import androidx.baselineprofile.gradle.utils.TEST_AGP_VERSION_8_0_0
import androidx.baselineprofile.gradle.utils.TEST_AGP_VERSION_8_1_0
import androidx.baselineprofile.gradle.utils.TEST_AGP_VERSION_8_2_0
import androidx.baselineprofile.gradle.utils.TEST_AGP_VERSION_ALL
import androidx.baselineprofile.gradle.utils.VariantProfile
import androidx.baselineprofile.gradle.utils.build
import androidx.baselineprofile.gradle.utils.buildAndAssertThatOutput
import androidx.baselineprofile.gradle.utils.buildAndFailAndAssertThatOutput
import androidx.baselineprofile.gradle.utils.require
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.Parameterized

@RunWith(JUnit4::class)
class BaselineProfileProducerPluginTestWithAgp80 {

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(
        forceAgpVersion = TEST_AGP_VERSION_8_0_0
    )

    private val emptyReleaseVariantProfile = VariantProfile(
        flavor = null,
        buildType = "release",
        profileFileLines = mapOf()
    )

    @Test
    fun verifyTasksWithAndroidTestPlugin() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget
        )

        projectSetup.producer.gradleRunner.build("tasks") {
            val notFound = it.lines().require(
                "connectedNonMinifiedReleaseAndroidTest - ",
                "collectNonMinifiedReleaseBaselineProfile - "
            )
            assertThat(notFound).isEmpty()
        }
    }
}

@RunWith(JUnit4::class)
class BaselineProfileProducerPluginTestWithAgp81 {

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(
        forceAgpVersion = TEST_AGP_VERSION_8_1_0
    )

    private val emptyReleaseVariantProfile = VariantProfile(
        flavor = null,
        buildType = "release",
        profileFileLines = mapOf()
    )

    @Test
    fun verifyTasksWithAndroidTestPlugin() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget
        )

        projectSetup.producer.gradleRunner.build("tasks") {
            val notFound = it.lines().require(
                "connectedNonMinifiedReleaseAndroidTest - ",
                "connectedBenchmarkReleaseAndroidTest - ",
                "collectNonMinifiedReleaseBaselineProfile - "
            )
            assertThat(notFound).isEmpty()
        }
    }
}

@RunWith(JUnit4::class)
class BaselineProfileProducerPluginTestWithAgp82 {

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(
        forceAgpVersion = TEST_AGP_VERSION_8_2_0
    )

    private val emptyReleaseVariantProfile = VariantProfile(
        flavor = null,
        buildType = "release",
        profileFileLines = mapOf()
    )

    @Test
    fun verifyInstrumentationRunnerArgumentsAreSet() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget,
            additionalGradleCodeBlock = """
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
            """.trimIndent()
        )

        projectSetup
            .producer
            .gradleRunner
            .buildAndAssertThatOutput("benchmarkReleaseArguments") {
                contains("androidx.benchmark.enabledRules=macrobenchmark")
            }
        projectSetup
            .producer
            .gradleRunner
            .buildAndAssertThatOutput("nonMinifiedReleaseArguments") {
                contains("androidx.benchmark.enabledRules=baselineprofile")
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
        projectSetup
            .producer
            .gradleRunner
            .build(
                "connectedBenchmarkReleaseAndroidTest",
                "$enabledRuleProp=Macrobenchmark"
            ) {
                // This should not fail.
            }

        projectSetup
            .producer
            .gradleRunner
            .build(
                "connectedNonMinifiedReleaseAndroidTest",
                "$enabledRuleProp=BaselineProfile"
            ) {
                // This should not fail.
            }
    }
}

@RunWith(Parameterized::class)
class BaselineProfileProducerPluginTest(agpVersion: String?) {

    companion object {
        @Parameterized.Parameters(name = "agpVersion={0}")
        @JvmStatic
        fun parameters() = TEST_AGP_VERSION_ALL
    }

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule(forceAgpVersion = agpVersion)

    private val emptyReleaseVariantProfile = VariantProfile(
        flavor = null,
        buildType = "release",
        profileFileLines = mapOf()
    )

    @Test
    fun nonExistingManagedDeviceShouldThrowError() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget,
            managedDevices = listOf(),
            baselineProfileBlock = """
                managedDevices = ["nonExisting"]
            """.trimIndent()
        )

        projectSetup.producer.gradleRunner.buildAndFailAndAssertThatOutput("tasks") {
            contains("It wasn't possible to determine the test task for managed device")
        }
    }

    @Test
    fun existingManagedDeviceShouldCreateCollectTaskDependingOnManagedDeviceTask() {
        projectSetup.appTarget.setup()
        projectSetup.producer.setup(
            variantProfiles = listOf(emptyReleaseVariantProfile),
            targetProject = projectSetup.appTarget,
            managedDevices = listOf("somePixelDevice"),
            baselineProfileBlock = """
                managedDevices = ["somePixelDevice"]
            """.trimIndent()
        )

        projectSetup
            .producer
            .gradleRunner
            .build(
                "collectNonMinifiedReleaseBaselineProfile",
                "--dry-run"
            ) {
                val appTargetName = projectSetup.appTarget.name
                val producerName = projectSetup.producer.name
                val notFound = it.lines().require(
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
            baselineProfileBlock = """
                managedDevices = ["somePixelDevice"]
            """.trimIndent(),
            additionalGradleCodeBlock = """
                afterEvaluate {
                    for (String taskName : [
                            "somePixelDeviceNonMinifiedReleaseAndroidTest",
                            "collectNonMinifiedReleaseBaselineProfile"]) {
                        def task = tasks.getByName(taskName)
                        println(taskName + "=" + task.enabled)
                    }
                }
            """.trimIndent()
        )

        // Execute any task and check the expected output.
        // Note that executing `somePixelDeviceSetup` will fail for `LicenseNotAcceptedException`.
        projectSetup
            .producer
            .gradleRunner
            .build(
                "tasks",
                "-Pandroidx.baselineprofile.skipgeneration"
            ) {
                val notFound = it.lines().require(
                    "somePixelDeviceNonMinifiedReleaseAndroidTest=false",
                    "collectNonMinifiedReleaseBaselineProfile=false"
                )
                assertThat(notFound).isEmpty()
            }
    }
}
