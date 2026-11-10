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

package androidx.benchmark.macro.junit4

import android.content.Intent
import android.os.Build
import androidx.benchmark.Arguments
import androidx.benchmark.DeviceInfo
import androidx.benchmark.Outputs
import androidx.benchmark.macro.BaselineProfileConfig
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import java.io.File
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = 28)
class BaselineProfileRuleTest {
    @get:Rule val baselineRule = BaselineProfileRule()

    /** Handle to service running in separate process, does nothing unless connect is called */
    private val trivialServiceHandle = TrivialServiceHandle()

    @After
    fun teardown() {
        trivialServiceHandle.disconnect()
    }

    @Test
    fun appNotInstalled() {
        val error =
            assertFailsWith<AssertionError> {
                baselineRule.collect(
                    packageName = "fake.package.not.installed",
                    profileBlock = { fail("not expected") },
                )
            }
        println(error.message)
        assertTrue(error.message!!.contains("Unable to find target package"))
    }

    @Test
    fun filter() {
        // skip if device doesn't support profile capture
        assumeTrue(
            DeviceInfo.supportsBaselineProfileCaptureError,
            DeviceInfo.supportsBaselineProfileCaptureError == null,
        )

        // Collects the baseline profile
        val result =
            baselineRule.collectWithResults(
                packageName = Arguments.getTargetPackageNameOrThrow(),
                filterPredicate = { it.contains(PROFILE_LINE_EMPTY_ACTIVITY) },
                maxIterations = 1,
                profileBlock = {
                    startActivityAndWait(Intent(ACTION))
                    device.waitForIdle()
                },
            )

        assertTrue(
            /* message = */ "Baseline profiles should exist",
            /* condition = */ result.baselineProfiles.isNotEmpty(),
        )

        // Asserts the output of the baseline profile. Note that this name is automatically
        // generated starting from class and method name, according to the patter
        // `<class>_<method>-baseline-prof.txt`. Changes for class and method names should be
        // reflected here in order for the test to succeed.
        File(Outputs.outputDirectory, "BaselineProfileRuleTest_filter-baseline-prof.txt")
            .readLines()
            .assertInOrder(
                PROFILE_LINE_EMPTY_ACTIVITY,
                "$PROFILE_LINE_EMPTY_ACTIVITY-><init>()V",
                "$PROFILE_LINE_EMPTY_ACTIVITY->onCreate(Landroid/os/Bundle;)V",
            )
    }

    @Test
    fun startupProfile() {
        // skip if device doesn't support profile capture
        assumeTrue(
            DeviceInfo.supportsBaselineProfileCaptureError,
            DeviceInfo.supportsBaselineProfileCaptureError == null,
        )

        // Collects the baseline profile
        val result =
            baselineRule.collectWithResults(
                packageName = Arguments.getTargetPackageNameOrThrow(),
                filterPredicate = { it.contains(PROFILE_LINE_EMPTY_ACTIVITY) },
                includeInStartupProfile = true,
                maxIterations = 1,
                stableIterations = 1,
                strictStability = false,
                profileBlock = {
                    startActivityAndWait(Intent(ACTION))
                    device.waitForIdle()
                },
            )

        assertTrue(
            /* message = */ "Startup profiles should exist",
            /* condition = */ result.startupProfiles.isNotEmpty(),
        )

        File(Outputs.outputDirectory, "BaselineProfileRuleTest_startupProfile-startup-prof.txt")
            .readLines()
            .assertInOrder(
                PROFILE_LINE_EMPTY_ACTIVITY,
                "$PROFILE_LINE_EMPTY_ACTIVITY-><init>()V",
                "$PROFILE_LINE_EMPTY_ACTIVITY->onCreate(Landroid/os/Bundle;)V",
            )
    }

    @Test
    fun config() {
        // skip if device doesn't support profile capture
        assumeTrue(
            DeviceInfo.supportsBaselineProfileCaptureError,
            DeviceInfo.supportsBaselineProfileCaptureError == null,
        )

        val config =
            BaselineProfileConfig.Builder(
                    packageName = Arguments.getTargetPackageNameOrThrow(),
                    profileBlock = {
                        startActivityAndWait(Intent(ACTION))
                        device.waitForIdle()
                    },
                )
                .setFilterPredicate { it.contains(PROFILE_LINE_EMPTY_ACTIVITY) }
                .setMaxIterations(1)
                .build()

        // Collects the baseline profile
        val result = baselineRule.collectWithResults(config = config)

        assertTrue(
            /* message = */ "Baseline profiles should exist",
            /* condition = */ result.baselineProfiles.isNotEmpty(),
        )

        // Asserts the output of the baseline profile. Note that this name is automatically
        // generated starting from class and method name, according to the patter
        // `<class>_<method>-baseline-prof.txt`. Changes for class and method names should be
        // reflected here in order for the test to succeed.
        File(Outputs.outputDirectory, "BaselineProfileRuleTest_config-baseline-prof.txt")
            .readLines()
            .assertInOrder(
                PROFILE_LINE_EMPTY_ACTIVITY,
                "$PROFILE_LINE_EMPTY_ACTIVITY-><init>()V",
                "$PROFILE_LINE_EMPTY_ACTIVITY->onCreate(Landroid/os/Bundle;)V",
            )
    }

    @Test
    @Ignore(
        "ART is making a change to the way profiles work. Classes loaded in the background are no longer added to the profile. The goal is reduce the size of the app image (helps with memory). [b/502168386]"
    )
    fun captureRulesRemoteProcess() {
        val result =
            baselineRule.collectWithResults(TrivialServiceHandle.TARGET, maxIterations = 1) {
                trivialServiceHandle.connect(TrivialServiceHandle.Action.TEST_ACTION1)
            }

        assertTrue(
            /* message = */ "Baseline profiles should exist",
            /* condition = */ result.baselineProfiles.isNotEmpty(),
        )

        // Asserts the output of the baseline profile. Note that this name is automatically
        // generated starting from class and method name, according to the patter
        // `<class>_<method>-baseline-prof.txt`. Changes for class and method names should be
        // reflected here in order for the test to succeed.
        File(
                Outputs.outputDirectory,
                "BaselineProfileRuleTest_captureRulesRemoteProcess-baseline-prof.txt",
            )
            .readLines()
            .assertInOrder(
                "androidx/benchmark/integration/macrobenchmark/target/TrivialService;",
                "androidx/benchmark/integration/macrobenchmark/target/TrivialService;-><init>()V",
                "androidx/benchmark/integration/macrobenchmark/target/TrivialService;->onBind(Landroid/content/Intent;)Landroid/os/IBinder;",
                "androidx/benchmark/integration/macrobenchmark/target/TrivialService${DOLLAR}InnerClass;",
                "androidx/benchmark/integration/macrobenchmark/target/TrivialService${DOLLAR}InnerClass;-><init>()V",
                "androidx/benchmark/integration/macrobenchmark/target/TrivialService${DOLLAR}InnerClass;->function1()V",
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun captureLoadOrder() {
        // Don't run tests on Cuttlefish.
        // Hardware perf_events in simpleperf fail in Cuttlefish because Cuttlefish runs as a
        // virtual machine where the hypervisor (KVM / crosvm) does not virtualize or pass through
        // the host CPU's hardware Performance Monitoring Unit (PMU).
        assumeFalse(Build.MODEL.contains("Cuttlefish"))
        val config =
            BaselineProfileConfig.Builder(
                    packageName = Arguments.getTargetPackageNameOrThrow(),
                    profileBlock = {
                        startActivityAndWait(Intent(ACTION))
                        device.waitForIdle()
                    },
                )
                .setMaxIterations(1)
                .setIncludeInStartupProfile(true)
                .setMaxLoadOrderIterations(1)
                .setRestrictLoadOrderToDataDirectory(false)
                .build()

        val result = baselineRule.collectWithResults(config = config)
        assertTrue(
            /* message = */ "Load order files should exist",
            /* condition = */ result.loadOrderFiles.isNotEmpty(),
        )
        assertTrue(
            /* message = */ "Load order files should exist on disk",
            /* condition = */ result.loadOrderFiles.all { File(it).exists() },
        )
    }

    companion object {
        private const val ACTION =
            "androidx.benchmark.integration.macrobenchmark.target.EMPTY_ACTIVITY"
        private const val PROFILE_LINE_EMPTY_ACTIVITY =
            "androidx/benchmark/integration/macrobenchmark/target/EmptyActivity;"
        private const val DOLLAR = "$"
    }

    private fun List<String>.assertInOrder(
        @Suppress("SameParameterValue") vararg toFind: String,
        predicate: (String, String) -> (Boolean) = { line, nextToFind ->
            line.endsWith(nextToFind)
        },
    ) {
        val remaining = toFind.filter { it.isNotBlank() }.toMutableList()
        for (line in this) {
            val next = remaining.firstOrNull() ?: return
            if (predicate(line, next)) remaining.removeAt(0)
        }

        if (remaining.isNotEmpty()) {
            fail(
                """
                The following lines were not found in order:
                ${remaining.joinToString(System.lineSeparator())}

                List content was:
                ${this.joinToString(System.lineSeparator())}
            """
                    .trimIndent()
            )
        }
    }
}
