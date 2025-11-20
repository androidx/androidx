/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.tracing

import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tracing.Trace.MAX_TRACE_LABEL_LENGTH
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

@LargeTest
class TraceTest {
    private var byteArrayOutputStream = ByteArrayOutputStream()

    @After
    fun stopAtrace() {
        // Since API 23, 'async_stop' will work. On lower API levels it was broken (see aosp/157142)
        if (Build.VERSION.SDK_INT >= 23) {
            executeCommand("atrace --async_stop")
        } else {
            // Ensure tracing is not currently running by performing a short synchronous trace.
            executeCommand("atrace -t 0")
        }
    }

    @Test
    @Ignore("b/280041271")
    fun beginAndEndSection() {
        startTrace()
        Trace.beginSection("beginAndEndSection")
        Trace.endSection()
        dumpTrace()

        assertTraceContains("tracing_mark_write:\\ B\\|.*\\|beginAndEndSection")
        assertTraceContains("tracing_mark_write:\\ E")
    }

    @Test
    @Ignore("b/280041271")
    fun beginAndEndTraceSectionLongLabel() {
        val builder = StringBuilder()
        for (i in 0..19) {
            builder.append("longLabel")
        }
        startTrace()
        Trace.beginSection(label = builder.toString())
        Trace.endSection()
        dumpTrace()
        assertTraceContains(
            "tracing_mark_write:\\ B\\|.*\\|" + builder.substring(0, MAX_TRACE_LABEL_LENGTH)
        )
        assertTraceContains("tracing_mark_write:\\ E")
    }

    @Test
    @SdkSuppress(minSdkVersion = 29) // SELinux
    fun beginAndEndSectionAsync() {
        startTrace()
        Trace.beginAsyncSection(methodName = "beginAndEndSectionAsync", cookie = 5099)
        Trace.endAsyncSection(methodName = "beginAndEndSectionAsync", cookie = 5099)
        dumpTrace()

        assertTraceContains("tracing_mark_write:\\ S\\|.*\\|beginAndEndSectionAsync\\|5099")
        assertTraceContains("tracing_mark_write:\\ F\\|.*\\|beginAndEndSectionAsync\\|5099")
    }

    @Test
    @SdkSuppress(minSdkVersion = 29) // SELinux
    fun setCounter() {
        startTrace()
        assertTrue("Checking that tracing is enabled", Trace.isEnabled())
        Trace.beginSection(label = "setting counters")
        Trace.setCounter(counterName = "counterName", counterValue = 42)
        Trace.setCounter(counterName = "counterName", counterValue = 47)
        Trace.setCounter(counterName = "counterName", counterValue = 9787)
        Trace.endSection()
        assertTrue("Checking that tracing is enabled", Trace.isEnabled())
        dumpTrace()
        assertTraceContains("setting counters")
        assertTraceContains("tracing_mark_write:\\ C\\|.*\\|counterName\\|42")
        assertTraceContains("tracing_mark_write:\\ C\\|.*\\|counterName\\|47")
        assertTraceContains("tracing_mark_write:\\ C\\|.*\\|counterName\\|9787")
    }

    @Ignore("b/280041271")
    @Test
    fun isEnabledDuringTrace() {
        startTrace()
        val enabled = Trace.isEnabled()
        dumpTrace()
        assertTrue(enabled)
    }

    @Test
    @SmallTest
    fun isNotEnabledWhenNotTracing() {
        assertFalse(Trace.isEnabled())
    }

    private fun startTrace() {
        val processName = getApplicationContext<Context>().applicationInfo.processName

        // Write the "async_start" status to the byte array to ensure atrace has fully started
        // before issuing any trace commands. This will also capture any errors that occur during
        // start so they can be added to the assertion error's message.
        executeCommand("atrace --async_start -b $TRACE_BUFFER_SIZE -a $processName")
    }

    private fun dumpTrace() {
        // On older versions of atrace, the -b option is required when dumping the trace so the
        // trace buffer doesn't get cleared before being dumped.
        executeCommand("atrace --async_dump -b $TRACE_BUFFER_SIZE", byteArrayOutputStream)
    }

    private fun assertTraceContains(contentRegex: String) {
        val traceString = byteArrayOutputStream.toByteArray().toString(StandardCharsets.UTF_8)

        if (!contentRegex.toRegex().containsMatchIn(traceString)) {
            throw AssertionError(
                "Trace does not contain requested regex: $contentRegex\n$traceString"
            )
        }
    }
}

private const val TRACE_BUFFER_SIZE = 8192

private fun executeCommand(command: String, outputStream: ByteArrayOutputStream? = null) {
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation

    automation.executeShellCommand(command).use { pfDescriptor ->
        ParcelFileDescriptor.AutoCloseInputStream(pfDescriptor).use { inputStream ->
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } >= 0) {
                outputStream?.write(buffer, 0, length)
            }
        }
    }
}
