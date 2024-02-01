/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro.perfetto

import android.annotation.SuppressLint
import android.os.Build
import androidx.benchmark.macro.FileLinkingRule
import androidx.benchmark.macro.Packages
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.perfetto.PerfettoHelper.Companion.MIN_BUNDLED_SDK_VERSION
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.tracing.Trace
import androidx.tracing.trace
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Trace validation tests for PerfettoCapture
 *
 * Note: this test is defined in benchmark-macro instead of benchmark-common so that it can
 * validate trace contents with TraceProcessor
 */
@SdkSuppress(minSdkVersion = MIN_BUNDLED_SDK_VERSION)
@LargeTest
@RunWith(Parameterized::class)
class PerfettoCaptureSweepTest(
    // this test is repeated several times to verify stability
    @Suppress("UNUSED_PARAMETER") iteration: Int
) {
    @get:Rule
    val linkRule = FileLinkingRule()

    @Before
    @After
    fun cleanup() {
        PerfettoHelper.stopAllPerfettoProcesses()
    }

    @Ignore("b/258216025")
    @SdkSuppress(minSdkVersion = MIN_BUNDLED_SDK_VERSION, maxSdkVersion = 33)
    @Test
    fun captureAndValidateTrace_bundled() {
        if (Build.VERSION.SDK_INT == 33 && Build.VERSION.CODENAME != "REL") {
            return // b/262909049: Do not run this test on pre-release Android U.
        }

        captureAndValidateTrace(unbundled = false)
    }

    @Ignore("b/258216025")
    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    fun captureAndValidateTrace_unbundled() {
        if (Build.VERSION.SDK_INT == 33 && Build.VERSION.CODENAME != "REL") {
            return // b/262909049: Do not run this test on pre-release Android U.
        }

        captureAndValidateTrace(unbundled = true)
    }

    @SuppressLint("BanThreadSleep")
    private fun captureAndValidateTrace(unbundled: Boolean) {
        assumeTrue(isAbiSupported())

        val traceFilePath = linkRule.createReportedTracePath(Packages.TEST)
        val perfettoCapture = PerfettoCapture(unbundled)

        perfettoCapture.start(
            PerfettoConfig.Benchmark(
                appTagPackages = listOf(Packages.TEST),
                useStackSamplingConfig = false
            )
        )

        assertTrue(
            "In-process tracing should be enabled immediately after trace capture is started",
            Trace.isEnabled()
        )

        /**
         * Trace section labels, in order
         *
         * We trace for non-trivial duration both to enable easier manual debugging, but also to
         * help clarify problems in front/back trace truncation, with indication of severity.
         *
         * We use unique, app tag names to avoid conflicting with other legitimate platform tracing.
         */
        val traceSectionLabels = List(20) {
            "PerfettoCaptureTest_$it".also { label ->
                trace(label) { Thread.sleep(50) }
            }
        }

        perfettoCapture.stop(traceFilePath)

        val matchingSlices = PerfettoTraceProcessor.runSingleSessionServer(traceFilePath) {
            querySlices("PerfettoCaptureTest_%", packageName = null)
        }

        // Note: this test avoids validating platform-triggered trace sections, to avoid flakes
        // from legitimate (and coincidental) platform use during test.
        assertEquals(
            traceSectionLabels,
            matchingSlices.sortedBy { it.ts }.map { it.name }
        )
        matchingSlices
            .forEach {
                assertTrue(
                    "Expected dur > 30ms, was ${it.dur / 1_000_000.0} ms",
                    it.dur > 30_000_000
                )
            }
    }

    companion object {
        @Parameterized.Parameters(name = "iter={0}")
        @JvmStatic
        fun parameters(): List<Array<Any>> = List(20) {
            arrayOf(it)
        }
    }
}
