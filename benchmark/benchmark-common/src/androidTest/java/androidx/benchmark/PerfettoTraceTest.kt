/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark

import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.perfetto.PerfettoTrace
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.fail
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPerfettoCaptureApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23)
class PerfettoTraceTest {
    @Test
    @Ignore("b/281077239")
    fun record_basic() {
        assumeTrue(PerfettoHelper.isAbiSupported())
        var perfettoTrace: PerfettoTrace? = null
        PerfettoTrace.record(
            fileLabel = "testTrace",
            traceCallback = { trace ->
                perfettoTrace = trace
            }
        ) {
            // noop
        }
        assertNotNull(perfettoTrace)
        assert(perfettoTrace!!.path.matches(Regex(".*/testTrace_[0-9-]+.perfetto-trace"))) {
            "$perfettoTrace didn't match!"
        }
    }
    @Test
    @Ignore("b/281077239")
    fun record_reentrant() {
        assumeTrue(PerfettoHelper.isAbiSupported())
        var perfettoTrace: PerfettoTrace? = null
        PerfettoTrace.record(
            fileLabel = "outer",
            traceCallback = { trace ->
                perfettoTrace = trace
            }
        ) {
            // tracing while tracing should fail
            assertFailsWith<IllegalStateException> {
                PerfettoTrace.record(
                    fileLabel = "inner",
                    traceCallback = { _ ->
                        fail("inner trace should not complete / record")
                    }
                ) {
                    // noop
                }
            }
        }
        assertNotNull(perfettoTrace)
    }
}
