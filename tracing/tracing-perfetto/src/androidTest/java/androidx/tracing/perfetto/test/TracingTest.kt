/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.tracing.perfetto.test

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.tracing.perfetto.PerfettoSdkTrace
import androidx.tracing.perfetto.TracingReceiver
import androidx.tracing.perfetto.internal.handshake.protocol.RequestKeys.RECEIVER_CLASS_NAME
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@RequiresApi(Build.VERSION_CODES.R) // TODO(234351579): Support API < 30
class TracingTest {
    @Test
    fun test_endToEnd_binaryDependenciesPresent() {
        assertThat(PerfettoSdkTrace.isEnabled).isEqualTo(false)

        // Note: no path to binary dependencies provided, so we are testing the case where the app
        // directly depends on :tracing:tracing-perfetto-binary
        PerfettoSdkTrace.enable()
        assertThat(PerfettoSdkTrace.isEnabled).isEqualTo(true)

        PerfettoSdkTrace.enable()
        assertThat(PerfettoSdkTrace.isEnabled).isEqualTo(true)

        PerfettoSdkTrace.beginSection("foo")
        PerfettoSdkTrace.beginSection("bar")
        PerfettoSdkTrace.endSection()
        PerfettoSdkTrace.endSection()

        // Note: content of the trace is verified by another test: TrivialTracingBenchmark
    }

    @Test
    fun tracing_receiver_class_name() {
        /** Verifies that [RECEIVER_CLASS_NAME] is matching [TracingReceiver] class name. */
        assertThat(TracingReceiver::class.java.name).isEqualTo(RECEIVER_CLASS_NAME)
    }
}
