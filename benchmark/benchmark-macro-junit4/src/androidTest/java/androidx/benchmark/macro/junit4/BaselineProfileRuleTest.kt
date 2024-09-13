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
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import java.io.File
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = 28)
class BaselineProfileRuleTest {
    @get:Rule val baselineRule = BaselineProfileRule()

    /** Handle to service running in separate process, does nothing unless connect is called */
    private val trivialServiceHandle = TrivialServiceHandle()

    @Before
    fun setup() {
        // Mokey devices seem to behave differently (b/319515652) and the generated profile
        // doesn't output the class symbol line. This makes the test fail. While we investigate
        // the scope of the failure, suppress the test on this device
        assumeFalse(isMokeyDevice())
    }

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
                    profileBlock = { fail("not expected") }
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
            DeviceInfo.supportsBaselineProfileCaptureError == null
        )

        // Collects the baseline profile
        baselineRule.collect(
            packageName = Arguments.getTargetPackageNameOrThrow(),
            filterPredicate = { it.contains(PROFILE_LINE_EMPTY_ACTIVITY) },
            maxIterations = 1,
            profileBlock = {
                startActivityAndWait(Intent(ACTION))
                device.waitForIdle()
            }
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
            DeviceInfo.supportsBaselineProfileCaptureError == null
        )

        // Collects the baseline profile
        baselineRule.collect(
            packageName = Arguments.getTargetPackageNameOrThrow(),
            filterPredicate = { it.contains(PROFILE_LINE_EMPTY_ACTIVITY) },
            includeInStartupProfile = true,
            maxIterations = 1,
            stableIterations = 1,
            strictStability = false,
            profileBlock = {
                startActivityAndWait(Intent(ACTION))
                device.waitForIdle()
            }
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
    fun captureRulesRemoteProcess() {
        baselineRule.collect(
            TrivialServiceHandle.TARGET,
            maxIterations = 1,
        ) {
            trivialServiceHandle.connect(TrivialServiceHandle.Action.TEST_ACTION1)
        }

        // Asserts the output of the baseline profile. Note that this name is automatically
        // generated starting from class and method name, according to the patter
        // `<class>_<method>-baseline-prof.txt`. Changes for class and method names should be
        // reflected here in order for the test to succeed.
        File(
                Outputs.outputDirectory,
                "BaselineProfileRuleTest_captureRulesRemoteProcess-baseline-prof.txt"
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

    companion object {
        private const val ACTION =
            "androidx.benchmark.integration.macrobenchmark.target.EMPTY_ACTIVITY"
        private const val PROFILE_LINE_EMPTY_ACTIVITY =
            "androidx/benchmark/integration/macrobenchmark/target/EmptyActivity;"
        private const val DOLLAR = "$"
    }

    private fun List<String>.assertInOrder(
        @Suppress("SameParameterValue") vararg toFind: String,
        predicate: (String, String) -> (Boolean) = { line, nextToFind -> line.endsWith(nextToFind) }
    ) {
        val remaining = toFind.filter { it.isNotBlank() }.toMutableList()
        for (line in this) {
            val next = remaining.firstOrNull() ?: return
            if (predicate(line, next)) remaining.removeAt(0)
        }

        if (remaining.size > 0) {
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

    private fun isMokeyDevice() = Build.DEVICE.contains("mokey")
}
