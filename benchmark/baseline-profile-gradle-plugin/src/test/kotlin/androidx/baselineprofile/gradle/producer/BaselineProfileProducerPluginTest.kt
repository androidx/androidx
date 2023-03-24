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
import androidx.baselineprofile.gradle.utils.VariantProfile
import androidx.baselineprofile.gradle.utils.buildAndAssertThatOutput
import androidx.baselineprofile.gradle.utils.buildAndFailAndAssertThatOutput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BaselineProfileProducerPluginTest {

    @get:Rule
    val projectSetup = BaselineProfileProjectSetupRule()

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

        projectSetup.producer.gradleRunner.buildAndAssertThatOutput("tasks") {
            contains("connectedNonMinifiedReleaseAndroidTest - ")
            contains("collectNonMinifiedReleaseBaselineProfile - ")
        }
    }

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
            .buildAndAssertThatOutput(
                "collectNonMinifiedReleaseBaselineProfile",
                "--dry-run"
            ) {
                contains(
                    ":${projectSetup.appTarget.name}:packageNonMinifiedRelease")
                contains(
                    ":${projectSetup.producer.name}:somePixelDeviceNonMinifiedReleaseAndroidTest")
                contains(
                    ":${projectSetup.producer.name}:connectedNonMinifiedReleaseAndroidTest")
                contains(
                    ":${projectSetup.producer.name}:collectNonMinifiedReleaseBaselineProfile")
            }
    }
}
