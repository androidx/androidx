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

package androidx.benchmark.integration.macrobenchmark

import android.content.Intent
import android.os.Build
import androidx.benchmark.Outputs
import androidx.benchmark.Shell
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlin.test.assertFailsWith
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@LargeTest
@SdkSuppress(minSdkVersion = 29)
class BaselineProfileRuleTest {

    @get:Rule
    val baselineRule = BaselineProfileRule()

    private val filterRegex = "^.*L${PACKAGE_NAME.replace(".", "/")}".toRegex()

    @Test
    @Ignore("b/294123161")
    fun appNotInstalled() {
        val error = assertFailsWith<AssertionError> {
            baselineRule.collect(
                packageName = "fake.package.not.installed",
                profileBlock = {
                    fail("not expected")
                }
            )
        }
        println(error.message)
        assertTrue(error.message!!.contains("Unable to find target package"))
    }

    @Test
    @Ignore("b/294123161")
    fun filter() {
        // TODO: share this 'is supported' check with the one inside BaselineProfileRule, once this
        //  test class is moved out of integration-tests, into benchmark-macro-junit4
        assumeTrue(Build.VERSION.SDK_INT >= 33 || Shell.isSessionRooted())

        // Collects the baseline profile
        baselineRule.collect(
            packageName = PACKAGE_NAME,
            filterPredicate = { it.contains(filterRegex) },
            profileBlock = {
                startActivityAndWait(Intent(ACTION))
                device.waitForIdle()
            }
        )

        // Note: this name is automatically generated starting from class and method name,
        // according to the patter `<class>_<method>-baseline-prof.txt`. Changes for class and
        // method names should be reflected here in order for the test to succeed.
        val baselineProfileOutputFileName = "BaselineProfileRuleTest_filter-baseline-prof.txt"

        // Asserts the output of the baseline profile
        val lines = File(Outputs.outputDirectory, baselineProfileOutputFileName).readLines()
        assertThat(lines).containsExactly(
            "Landroidx/benchmark/integration/macrobenchmark/target/EmptyActivity;",
            "HSPLandroidx/benchmark/integration/macrobenchmark/target/EmptyActivity;-><init>()V",
            "PLandroidx/benchmark/integration/macrobenchmark/target/EmptyActivity;-><init>()V",
            "HSPLandroidx/benchmark/integration/macrobenchmark/target/EmptyActivity;" +
                "->onCreate(Landroid/os/Bundle;)V",
            "PLandroidx/benchmark/integration/macrobenchmark/target/EmptyActivity;" +
                "->onCreate(Landroid/os/Bundle;)V",
        )
    }

    @Test
    @Ignore("b/294123161")
    fun profileType() {
        assumeTrue(Build.VERSION.SDK_INT >= 33 || Shell.isSessionRooted())

        data class TestConfig(val includeInStartupProfile: Boolean, val outputFileName: String)

        arrayOf(
            TestConfig(true, "BaselineProfileRuleTest_profileType-startup-prof.txt"),
            TestConfig(false, "BaselineProfileRuleTest_profileType-baseline-prof.txt"),
        ).forEach { (includeInStartupProfile, outputFilename) ->

            // Collects the baseline profile
            baselineRule.collect(
                packageName = PACKAGE_NAME,
                filterPredicate = { it.contains(filterRegex) },
                includeInStartupProfile = includeInStartupProfile,
                profileBlock = {
                    startActivityAndWait(Intent(ACTION))
                    device.waitForIdle()
                }
            )

            // Asserts the output of the baseline profile
            val lines = File(Outputs.outputDirectory, outputFilename).readLines()
            assertThat(lines).containsExactly(
                "Landroidx/benchmark/integration/macrobenchmark/target/EmptyActivity;",
                "HSPLandroidx/benchmark/integration/macrobenchmark/target/EmptyActivity;" +
                    "-><init>()V",
                "PLandroidx/benchmark/integration/macrobenchmark/target/EmptyActivity;-><init>()V",
                "HSPLandroidx/benchmark/integration/macrobenchmark/target/EmptyActivity;" +
                    "->onCreate(Landroid/os/Bundle;)V",
                "PLandroidx/benchmark/integration/macrobenchmark/target/EmptyActivity;" +
                    "->onCreate(Landroid/os/Bundle;)V",
            )
        }
    }

    companion object {
        private const val PACKAGE_NAME =
            "androidx.benchmark.integration.macrobenchmark.target"
        private const val ACTION =
            "androidx.benchmark.integration.macrobenchmark.target.EMPTY_ACTIVITY"
    }
}
