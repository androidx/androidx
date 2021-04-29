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

import androidx.benchmark.macro.device
import androidx.benchmark.macro.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@SmallTest
@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
public class PerfettoTraceProcessorTest {
    @Test
    public fun shellPath() {
        assumeTrue(isAbiSupported())
        val shellPath = PerfettoTraceProcessor.shellPath
        val device = InstrumentationRegistry.getInstrumentation().device()
        val out = device.executeShellCommand("$shellPath --version")
        assertTrue(
            "expect to get Perfetto version string, saw: $out",
            out.contains("Perfetto v")
        )
    }

    @Test
    public fun getJsonMetrics_tracePathWithSpaces() {
        assumeTrue(isAbiSupported())
        assertFailsWith<IllegalArgumentException> {
            PerfettoTraceProcessor.getJsonMetrics("/a b", "ignored")
        }
    }

    @Test
    public fun getJsonMetrics_metricWithSpaces() {
        assumeTrue(isAbiSupported())
        assertFailsWith<IllegalArgumentException> {
            PerfettoTraceProcessor.getJsonMetrics("/ignored", "a b")
        }
    }

    @Test
    public fun validateAbiNotSupportedBehavior() {
        assumeFalse(isAbiSupported())
        assertFailsWith<IllegalStateException> {
            PerfettoTraceProcessor.shellPath
        }

        assertFailsWith<IllegalStateException> {
            PerfettoTraceProcessor.getJsonMetrics("ignored_path", "ignored_metric")
        }
    }

    @Test
    public fun validateTraceProcessorBinariesExist() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffixes = listOf("aarch64")
        val entries = suffixes.map { "trace_processor_shell_$it" }.toSet()
        val assets = context.assets.list("") ?: emptyArray()
        assertTrue(
            "Expected to find $entries",
            assets.toSet().containsAll(entries)
        )
    }
}
