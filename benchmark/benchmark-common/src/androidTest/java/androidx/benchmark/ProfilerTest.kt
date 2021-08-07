/*
 * Copyright 2020 The Android Open Source Project
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

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assume
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@MediumTest
@RunWith(AndroidJUnit4::class)
class ProfilerTest {
    @Test
    fun getByName() {
        assertSame(
            if (Build.VERSION.SDK_INT >= 29) StackSamplingSimpleperf else StackSamplingLegacy,
            Profiler.getByName("StackSampling")
        )
        assertSame(MethodTracing, Profiler.getByName("MethodTracing"))
        assertSame(ConnectedAllocation, Profiler.getByName("ConnectedAllocation"))
        assertSame(ConnectedSampling, Profiler.getByName("ConnectedSampling"))

        // Compat names
        assertSame(StackSamplingLegacy, Profiler.getByName("MethodSampling"))
        assertSame(StackSamplingSimpleperf, Profiler.getByName("MethodSamplingSimpleperf"))
        assertSame(MethodTracing, Profiler.getByName("Method"))
        assertSame(StackSamplingLegacy, Profiler.getByName("Sampled"))
        assertSame(ConnectedSampling, Profiler.getByName("ConnectedSampled"))
    }

    private fun verifyProfiler(
        profiler: Profiler,
        file: File
    ) {
        Assume.assumeFalse(
            "Workaround native crash on API 21 in CI, see b/173662168",
            profiler == MethodTracing && Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP,
        )

        file.delete() // clean up, if previous run left this behind
        assertFalse(file.exists())

        profiler.start("test")
        profiler.stop()
        assertTrue(file.exists(), "Profiler should create: ${file.absolutePath}")

        // we don't delete the file to enable inspecting the file
        // TODO: post the trace to the studio UI via FileLinkingRule or similar
    }

    @Test
    fun methodTracing() = verifyProfiler(
        profiler = MethodTracing,
        file = Outputs.testOutputFile("test-methodTracing.trace")
    )

    @Test
    fun stackSamplingLegacy() = verifyProfiler(
        profiler = StackSamplingLegacy,
        file = Outputs.testOutputFile("test-stackSamplingLegacy.trace")
    )

    @SdkSuppress(minSdkVersion = 29) // simpleperf on system image starting API 29
    @Test
    fun stackSamplingSimpleperf() {
        // TODO: Investigate cuttlefish API 29 failure
        assumeFalse(Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29)

        verifyProfiler(
            profiler = StackSamplingSimpleperf,
            file = Outputs.testOutputFile("test-stackSampling.trace")
        )
    }
}
