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

package androidx.benchmark

import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.perfetto.PerfettoHelper.Companion.LOWEST_BUNDLED_VERSION_SUPPORTED
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@LargeTest
@SdkSuppress(minSdkVersion = 23)
@RunWith(AndroidJUnit4::class)
class PerfettoHelperTest {
    @Before
    @After
    fun cleanup() {
        PerfettoHelper.stopAllPerfettoProcesses()
    }

    private fun validateStopAllPerfettoProcesses(unbundled: Boolean) {
        // NOTE: we use PerfettoCapture to validate PerfettoHelper.stopAllPerfettoProcesses, as
        // these will likely be merged in the future, and the PerfettoCapture API is simpler
        fun getPerfettoPids() = Shell.getPidsForProcess(if (unbundled) "tracebox" else "perfetto")

        // should be no perfetto processes running
        assertEquals(expected = listOf(), actual = getPerfettoPids())

        // start perfetto
        val capture = PerfettoCapture(unbundled)
        capture.start(PerfettoConfig.Benchmark(listOf(Packages.TEST)))

        // should be at least one perfetto process
        assertNotEquals(illegal = listOf(), actual = getPerfettoPids())
        assertTrue(capture.isRunning())

        // kill all...
        PerfettoHelper.stopAllPerfettoProcesses()

        // should be none again
        assertEquals(expected = listOf(), actual = getPerfettoPids())
        assertFalse(capture.isRunning())
    }

    @SdkSuppress(minSdkVersion = LOWEST_BUNDLED_VERSION_SUPPORTED)
    @Test
    fun stopAllPerfettoProcesses_bundled() = validateStopAllPerfettoProcesses(unbundled = false)

    @Test
    fun stopAllPerfettoProcesses_unbundled() {
        // Only check ABI support for unbundled, as bundled test doesn't use any unbundled binaries
        Assume.assumeTrue(PerfettoHelper.isAbiSupported())

        validateStopAllPerfettoProcesses(unbundled = true)
    }
}