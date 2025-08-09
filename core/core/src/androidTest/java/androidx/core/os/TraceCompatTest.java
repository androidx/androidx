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

package androidx.core.os;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.app.UiAutomation;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@LargeTest
@SuppressWarnings("deprecation") // TraceCompat is now deprecated
public final class TraceCompatTest {

    private static final int TRACE_BUFFER_SIZE = 8192;

    private static final boolean TRACE_AVAILABLE;

    private static String sTracedPreviousState = null;

    static {
        // Check if tracing is available via debugfs or tracefs
        TRACE_AVAILABLE = new File("/sys/kernel/debug/tracing/trace_marker").exists()
                || new File("/sys/kernel/tracing/trace_marker").exists();
    }

    private ByteArrayOutputStream mByteArrayOutputStream;

    @BeforeClass
    public static void setUpClass() throws IOException {
        if (TRACE_AVAILABLE && Build.VERSION.SDK_INT >= 30 && Build.VERSION.SDK_INT < 32) {
            // On API 30 and 31, iorapd frequently uses perfetto, which competes for the trace
            // buffer (see b/291108969, b/156260391, b/145554890, b/149790059 for more context).
            // iorap was removed in API 32.
            // Ensure perfetto's traced is disabled on affected API levels for the duration
            // of the test so we're not competing for the trace buffer.
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            executeCommand("getprop persist.traced.enable", outputStream);
            sTracedPreviousState = new String(outputStream.toByteArray(), UTF_8).trim();
            if ("1".equals(sTracedPreviousState)) {
                executeCommand("setprop persist.traced.enable 0");
            }
        }
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        // Re-enable traced if needed
        if (TRACE_AVAILABLE && "1".equals(sTracedPreviousState)) {
            executeCommand("setprop persist.traced.enable 1");
        }
    }

    @Before
    public void setUp() {
        assumeTrue("Tracing is not available via debugfs or tracefs.", TRACE_AVAILABLE);
        mByteArrayOutputStream = new ByteArrayOutputStream();
    }

    @After
    public void stopAtrace() throws IOException {
        if (TRACE_AVAILABLE) {
            executeCommand("atrace --async_stop");
        }
    }

    @Test
    @SdkSuppress(excludedSdks = { 30, 33 }) // Excluded due to flakes (b/328063273)
    public void beginAndEndSection() throws IOException {
        startTrace();
        TraceCompat.beginSection("beginAndEndSection");
        TraceCompat.endSection();
        dumpTrace();

        assertTraceContains("tracing_mark_write:\\ B\\|.*\\|beginAndEndSection");
        assertTraceContains("tracing_mark_write:\\ E");
    }

    @Test
    @SdkSuppress(excludedSdks = { 30, 33 }) // Excluded due to flakes (b/295944187)
    public void beginAndEndSectionAsync() throws IOException {
        startTrace();
        TraceCompat.beginAsyncSection("beginAndEndSectionAsync", /*cookie=*/5099);
        TraceCompat.endAsyncSection("beginAndEndSectionAsync", /*cookie=*/5099);
        dumpTrace();

        assertTraceContains("tracing_mark_write:\\ S\\|.*\\|beginAndEndSectionAsync\\|5099");
        assertTraceContains("tracing_mark_write:\\ F\\|.*\\|beginAndEndSectionAsync\\|5099");
    }

    @Test
    @SdkSuppress(excludedSdks = { 30, 33 }) // Excluded due to flakes (b/329119528)
    public void setCounter() throws IOException {
        startTrace();
        TraceCompat.setCounter("counterName", 42);
        TraceCompat.setCounter("counterName", 47);
        TraceCompat.setCounter("counterName", 9787);
        dumpTrace();

        assertTraceContains("tracing_mark_write:\\ C\\|.*\\|counterName\\|42");
        assertTraceContains("tracing_mark_write:\\ C\\|.*\\|counterName\\|47");
        assertTraceContains("tracing_mark_write:\\ C\\|.*\\|counterName\\|9787");
    }

    @Test
    public void isEnabledDuringTrace() throws IOException {
        startTrace();
        boolean enabled = TraceCompat.isEnabled();
        dumpTrace();

        assertThat(enabled).isTrue();
    }

    @SmallTest
    @Test
    @SdkSuppress(excludedSdks = { 30, 33 }) // Excluded due to flakes (b/308151557)
    public void isNotEnabledWhenNotTracing() {
        assertThat(TraceCompat.isEnabled()).isFalse();
    }

    private void startTrace() throws IOException {
        String processName =
                ApplicationProvider.getApplicationContext().getApplicationInfo().processName;

        // Write the "async_start" status to the byte array to ensure atrace has fully started
        // before issuing any trace commands. This will also capture any errors that occur during
        // start so they can be added to the assertion error's message.
        executeCommand(
                String.format("atrace --async_start -b %d -a %s", TRACE_BUFFER_SIZE, processName));
    }

    private void dumpTrace() throws IOException {
        // On older versions of atrace, the -b option is required when dumping the trace so the
        // trace buffer doesn't get cleared before being dumped.
        executeCommand(
                String.format("atrace --async_dump -b %d", TRACE_BUFFER_SIZE),
                mByteArrayOutputStream);
    }

    private static void executeCommand(@NonNull String command) throws IOException {
        executeCommand(command, null);
    }

    private static void executeCommand(@NonNull String command,
            @Nullable ByteArrayOutputStream outputStream) throws IOException {
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();

        try (ParcelFileDescriptor pfDescriptor = automation.executeShellCommand(command);
             ParcelFileDescriptor.AutoCloseInputStream inputStream =
                     new ParcelFileDescriptor.AutoCloseInputStream(
                             pfDescriptor)) {
            byte[] buffer = new byte[1024];

            int length;
            while ((length = inputStream.read(buffer)) >= 0) {
                if (outputStream != null) {
                    outputStream.write(buffer, 0, length);
                }
            }
        }
    }

    private void assertTraceContains(@NonNull String contentRegex) {
        String traceString = new String(mByteArrayOutputStream.toByteArray(), UTF_8);

        Pattern pattern = Pattern.compile(contentRegex);
        Matcher matcher = pattern.matcher(traceString);

        if (!matcher.find()) {
            throw new AssertionError(
                    String.format("Trace does not contain requested regex: %s\n%s", contentRegex,
                            traceString));
        }
    }

}

