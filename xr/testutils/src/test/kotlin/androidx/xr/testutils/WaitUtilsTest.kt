/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.testutils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class WaitUtilsTest {

    @Test
    fun waitUntil_onMainThread_throwsIllegalStateException() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertFailsWith<IllegalStateException> { waitUntil("main thread check") { true } }
        }
    }

    @Test
    fun waitUntil_negativeDurations_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            waitUntil("negative timeout", timeout = (-1).milliseconds) { true }
        }
        assertFailsWith<IllegalArgumentException> {
            waitUntil("negative pollInterval", pollInterval = (-1).microseconds) { true }
        }
    }

    @Test
    fun waitUntil_supportsSubMillisecondPollingAndDefaultDescription() {
        waitUntil { true }

        val attempts = AtomicInteger(0)
        waitUntil(
            description = "sub-millisecond polling",
            timeout = 1.seconds,
            pollInterval = 200.microseconds,
        ) {
            attempts.incrementAndGet() >= 4
        }
        assertEquals(4, attempts.get())
    }

    @Test
    fun waitUntil_whenConditionRemainsFalse_throwsAssertionErrorWithDescription() {
        val timeoutError =
            assertFailsWith<AssertionError> {
                waitUntil(
                    description = "unsatisfied condition",
                    timeout = 5.milliseconds,
                    pollInterval = 500.microseconds,
                ) {
                    false
                }
            }
        assertTrue(timeoutError.message.orEmpty().contains("unsatisfied condition"))
    }
}
