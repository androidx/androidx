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

package androidx.benchmark.macro.perfetto

import android.os.Build
import androidx.benchmark.Outputs
import androidx.benchmark.macro.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.verifyWithPolling
import androidx.tracing.Trace
import androidx.tracing.trace
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@SdkSuppress(minSdkVersion = 29) // Lower to 21 after fixing trace config.
@RunWith(Parameterized::class)
public class PerfettoCaptureTest(private val unbundled: Boolean) {
    private val traceFile = File(Outputs.dirUsableByAppAndShell, "PerfettoCaptureTest.trace")
    private val traceFilePath = traceFile.absolutePath

    @Before
    @After
    public fun cleanup() {
        PerfettoCapture(unbundled).cancel()
        traceFile.delete()
    }

    @LargeTest
    @Test
    public fun traceAndCheckFileSize() {
        // Change the check to API >=21, once we have the correct Perfetto config.
        assumeTrue(Build.VERSION.SDK_INT >= 29 && isAbiSupported())
        val perfettoCapture = PerfettoCapture(unbundled)

        verifyTraceEnable(false)

        perfettoCapture.start()

        verifyTraceEnable(true)

        trace("PerfettoCaptureTest") {
            // Tracing non-trivial duration for manual debugging/verification
            Thread.sleep(20)
        }

        perfettoCapture.stop(traceFilePath)

        val length = traceFile.length()
        assertTrue("Expect > 10KiB file, was $length bytes", length > 10 * 1024)
    }

    public companion object {
        @Parameterized.Parameters(name = "unbundled={0}")
        @JvmStatic
        public fun parameters(): Array<Any> {
            return arrayOf(true, false)
        }
    }
}

public fun verifyTraceEnable(enabled: Boolean) {
    // We poll here, since we may need to wait for enable flags to propagate to apps
    verifyWithPolling(
        "Timeout waiting for Trace.isEnabled == $enabled",
        periodMs = 50,
        timeoutMs = 5000
    ) {
        Trace.isEnabled() == enabled
    }
}
