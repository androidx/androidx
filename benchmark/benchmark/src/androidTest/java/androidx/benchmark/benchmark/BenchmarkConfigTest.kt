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

package androidx.benchmark.benchmark

import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * These tests validate build time properties of benchmarks.
 *
 * These tests are enforced in presubmit, even if dryRunMode=true is passed. Standard
 * microbenchmarks will not perform these checks when dryRunMode=false.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class BenchmarkConfigTest {
    private val arguments = InstrumentationRegistry.getArguments()
    private val contextTest = InstrumentationRegistry.getInstrumentation().context
    private val contextTarget = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun coverageDisabled() {
        assertNotEquals(
            illegal = "true",
            actual = arguments.getString("coverage"),
            message = "Coverage must not be enabled in microbench instrumentation args"
        )
    }

    @Test
    fun selfInstrumenting() {
        assertEquals(
            expected = contextTest.packageName,
            actual = contextTarget.packageName,
            message =
                "Microbenchmark must be self-instrumenting," +
                    " test pkg=${contextTest.packageName}," +
                    " target pkg=${contextTarget.packageName}"
        )
    }

    @Test
    fun debuggableFalse() {
        val debuggable = contextTest.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        assertFalse(debuggable, "Microbenchmark must not be debuggable")
    }
}
