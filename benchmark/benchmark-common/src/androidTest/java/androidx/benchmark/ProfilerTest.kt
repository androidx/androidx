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
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@MediumTest
@RunWith(AndroidJUnit4::class)
public class ProfilerTest {
    @Test
    public fun getByName() {
        assertSame(MethodSampling, Profiler.getByName("MethodSampling"))
        assertSame(MethodTracing, Profiler.getByName("MethodTracing"))
        assertSame(ConnectedAllocation, Profiler.getByName("ConnectedAllocation"))
        assertSame(ConnectedSampling, Profiler.getByName("ConnectedSampling"))
        assertSame(MethodSamplingSimpleperf, Profiler.getByName("MethodSamplingSimpleperf"))

        // Compat names
        assertSame(MethodTracing, Profiler.getByName("Method"))
        assertSame(MethodSampling, Profiler.getByName("Sampled"))
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
        val deletedSuccessfully: Boolean
        try {
            file.delete() // clean up, if previous run left this behind
            assertFalse(file.exists())

            profiler.start("test")
            profiler.stop()
            assertTrue(file.exists(), "Profiler should create: ${file.absolutePath}")
        } finally {
            deletedSuccessfully = file.delete()
        }
        assertTrue(deletedSuccessfully, "File should exist, and be deleted")
    }

    @Test
    public fun methodSampling(): Unit = verifyProfiler(
        profiler = MethodSampling,
        file = Outputs.testOutputFile("test-methodSampling.trace")
    )

    @Test
    public fun methodTracing(): Unit = verifyProfiler(
        profiler = MethodTracing,
        file = Outputs.testOutputFile("test-methodTracing.trace")
    )

    @Ignore(
        "b/158303822 - Temporarily disabled in CI, since this currently requires " +
            "external script setup"
    )
    @SdkSuppress(minSdkVersion = 28)
    @Test
    public fun methodSamplingSimpleperf(): Unit = verifyProfiler(
        profiler = MethodSamplingSimpleperf,
        file = File("/data/data/androidx.benchmark.test/simpleperf_data/test.data")
    )
}
