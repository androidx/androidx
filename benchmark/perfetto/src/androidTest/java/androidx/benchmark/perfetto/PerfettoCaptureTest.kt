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

package androidx.benchmark.perfetto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.Trace
import androidx.tracing.trace
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class PerfettoCaptureTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val traceFile = File(context.getExternalFilesDir(null), "PerfettoCaptureTest.trace")
    private val traceFilePath = traceFile.absolutePath

    @Before
    @After
    fun cleanup() {
        PerfettoCapture().cancel()
        traceFile.delete()
    }

    @LargeTest
    @Test
    fun traceAndCheckFileSize() {
        ignorePerfettoTestIfUnsupportedCuttlefish()

        val perfettoCapture = PerfettoCapture()

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
}

@Suppress("SameParameterValue")
private fun verifyWithPoll(
    message: String,
    periodMs: Long,
    timeoutMs: Long,
    tryBlock: () -> Boolean
) {
    var totalDurationMs = 0L
    while (!tryBlock()) {
        Thread.sleep(periodMs)

        totalDurationMs += periodMs
        if (totalDurationMs > timeoutMs) {
            fail(message)
        }
    }
}

fun verifyTraceEnable(enabled: Boolean) {
    // We poll here, since we may need to wait for enable flags to propagate to apps
    verifyWithPoll(
        "Timeout waiting for Trace.isEnabled == $enabled",
        periodMs = 50,
        timeoutMs = 500
    ) {
        Trace.isEnabled() == enabled
    }
}