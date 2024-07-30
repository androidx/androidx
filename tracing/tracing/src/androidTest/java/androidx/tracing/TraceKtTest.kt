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

package androidx.tracing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class TraceKtTest {
    @Test
    fun traceTest() {
        val x = trace("Test") { 10 }
        assertEquals(10, x)
    }

    @Test
    fun testNoCrossInline() {
        trace("Test") {
            return
        }
    }

    @Test
    fun traceLazyTest() {
        assertFalse(
            "This test expects to be run without tracing enabled in this process",
            Trace.isEnabled()
        )

        val x =
            trace(
                lazyLabel = {
                    throw IllegalStateException("tracing should be disabled, with message not used")
                }
            ) {
                10
            }
        assertEquals(10, x)
    }

    @Test
    fun asyncTraceTest() {
        runBlocking {
            val x =
                traceAsync("test", 0) {
                    delay(1)
                    10
                }
            assertEquals(10, x)
        }
    }

    @Test
    fun asyncTraceLazyTest() {
        assertFalse(
            "This test expects to be run without tracing enabled in this process",
            Trace.isEnabled()
        )
        runBlocking {
            val x =
                traceAsync(
                    lazyMethodName = {
                        throw IllegalStateException(
                            "tracing should be disabled, with message not used"
                        )
                    },
                    lazyCookie = {
                        throw IllegalStateException(
                            "tracing should be disabled, with message not used"
                        )
                    }
                ) {
                    delay(1)
                    10
                }
            assertEquals(10, x)
        }
    }
}
