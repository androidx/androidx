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

package androidx.benchmark.junit4

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import androidx.testutils.verifyWithPolling
import androidx.tracing.Trace
import androidx.tracing.trace
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest // recording is expensive
@RunWith(AndroidJUnit4::class)
class PerfettoRuleTest {
    @get:Rule
    val perfettoRule = PerfettoRule()

    @Ignore("b/217256936")
    @Test
    fun tracingEnabled() {
        trace("PerfettoCaptureTest") {
            val traceShouldBeEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            verifyTraceEnable(traceShouldBeEnabled)
        }

        // NOTE: ideally, we'd validate the output file, but it's difficult to assert the
        // behavior of the rule, since we can't really assert the result of a rule, which
        // occurs after both @Test and @After
    }
}

@SmallTest // not recording is cheap
@RunWith(AndroidJUnit4::class)
class PerfettoRuleControlTest {
    @Test
    fun tracingNotEnabled() {
        verifyTraceEnable(false)
    }
}

private fun verifyTraceEnable(enabled: Boolean) {
    // We poll here, since we may need to wait for enable flags to propagate to apps
    verifyWithPolling(
        "Timeout waiting for Trace.isEnabled == $enabled",
        periodMs = 50,
        timeoutMs = 5000
    ) {
        Trace.isEnabled() == enabled
    }
}