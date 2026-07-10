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

import androidx.benchmark.DeviceInfo
import androidx.benchmark.InProcessTracingMode
import androidx.benchmark.inMemoryTrace
import androidx.benchmark.junit4.PerfettoTraceRule
import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.runSingleSessionServer
import androidx.benchmark.traceprocessor.PerfettoTrace
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.tracing.Tracer
import androidx.tracing.trace
import kotlin.test.assertContains
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
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
    @Before
    fun checkDeviceSupport() {
        assumeTrue(DeviceInfo.expectedToSupportTracingInTests)
    }

    companion object {
        const val UNIQUE_SLICE_NAME = "PerfettoRuleTestUnique"
        const val UNIQUE_IN_PROCESS_SLICE_NAME = "PerfettoRuleTestUniqueInProcess"
    }

    var trace: PerfettoTrace? = null

    // wrap the perfetto rule with another which consumes + validates the trace
    @get:Rule
    val rule: RuleChain =
        RuleChain.outerRule { base, _ ->
                object : Statement() {
                    override fun evaluate() {
                        base.evaluate()
                        if (PerfettoHelper.isAbiSupported()) {
                            assertNotNull(trace)
                            val sliceNameInstances =
                                TraceProcessor.runSingleSessionServer(trace!!.path) {
                                    querySlices(
                                            UNIQUE_SLICE_NAME,
                                            UNIQUE_IN_PROCESS_SLICE_NAME,
                                            packageName = null,
                                        )
                                        .map { slice -> slice.name }
                                }

                            assertContains(sliceNameInstances, UNIQUE_SLICE_NAME)
                            assertContains(sliceNameInstances, UNIQUE_IN_PROCESS_SLICE_NAME)
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
                PerfettoTraceRule(
                    inProcessTracingMode = InProcessTracingMode.UseIfAvailable,
                    labelProvider = { description -> "CUSTOM_LABEL" },
                ) {
                    trace = it
                }
            )

    @Test
    fun simple() {
        trace(UNIQUE_SLICE_NAME) {}
        Tracer.global.trace(category = "category", name = UNIQUE_IN_PROCESS_SLICE_NAME) {}
    }

    @Test
    fun inMemoryTrace() {
        // in memory tracing support is temporary, see b/409397427
        inMemoryTrace(UNIQUE_SLICE_NAME) {}
        Tracer.global.trace(category = "category", name = UNIQUE_IN_PROCESS_SLICE_NAME) {}
    }

    @Test(expected = IllegalStateException::class)
    fun exception() {
        // trace works even if test throws
        trace(UNIQUE_SLICE_NAME) {}
        Tracer.global.trace(category = "category", name = UNIQUE_IN_PROCESS_SLICE_NAME) {}
        throw IllegalStateException()
    }
}
