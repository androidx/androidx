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

package androidx.benchmark.macro

import android.os.Build.VERSION.SDK_INT
import androidx.benchmark.DeviceInfo.isEmulator
import androidx.benchmark.inMemoryTrace
import androidx.benchmark.junit4.PerfettoTraceRule
import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.traceprocessor.PerfettoTrace
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.tracing.trace
import kotlin.test.assertContains
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * NOTE: This test is in `benchmark-macro` since validating trace content requires
 * `benchmark-macro`, which has a minAPI of 23, and benchmark-junit4 can't work with that.
 *
 * This should be moved to `benchmark-junit` once trace validation can be done in its tests.
 */
@LargeTest // recording is expensive
@OptIn(ExperimentalPerfettoCaptureApi::class)
@RunWith(AndroidJUnit4::class)
class PerfettoTraceRuleTest {
    companion object {
        const val UNIQUE_SLICE_NAME = "PerfettoRuleTestUnique"
    }

    var trace: PerfettoTrace? = null

    // wrap the perfetto rule with another which consumes + validates the trace
    @get:Rule
    val rule: RuleChain =
        RuleChain.outerRule { base, _ ->
                object : Statement() {
                    override fun evaluate() {
                        base.evaluate()
                        // Our API 23 emulators seem to be misconfigured b/438214932
                        if (PerfettoHelper.isAbiSupported() && (!isEmulator || SDK_INT != 23)) {
                            assertNotNull(trace)
                            val sliceNameInstances =
                                TraceProcessor.runSingleSessionServer(trace!!.path) {
                                    querySlices(UNIQUE_SLICE_NAME, packageName = null).map { slice
                                        ->
                                        slice.name
                                    }
                                }
                            assertEquals(listOf(UNIQUE_SLICE_NAME), sliceNameInstances)

                            assertContains(
                                trace!!.path,
                                "/CUSTOM_LABEL_",
                                message = "expected ${trace!!.path} to contain custom label",
                            )
                        }
                    }
                }
            }
            .around(
                PerfettoTraceRule(labelProvider = { description -> "CUSTOM_LABEL" }) { trace = it }
            )

    @Test
    fun simple() {
        trace(UNIQUE_SLICE_NAME) {}
    }

    @Test
    fun inMemoryTrace() {
        // in memory tracing support is temporary, see b/409397427
        inMemoryTrace(UNIQUE_SLICE_NAME) {}
    }

    @Test(expected = IllegalStateException::class)
    fun exception() {
        // trace works even if test throws
        trace(UNIQUE_SLICE_NAME) {}
        throw IllegalStateException()
    }
}
