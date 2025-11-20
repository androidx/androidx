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

package androidx.compose.runtime

import androidx.compose.runtime.internal.AtomicInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.test.IgnoreJsTarget
import kotlinx.test.IgnoreWasmTarget

@ExperimentalCoroutinesApi
class BroadcastFrameClockTest {
    @Test
    fun sendAndReceiveFrames() =
        runTest(UnconfinedTestDispatcher()) {
            val clock = BroadcastFrameClock()

            val frameAwaiter = async { clock.withFrameNanos { it } }

            clock.sendFrame(1)
            assertEquals(1, frameAwaiter.await(), "awaiter frame time")
        }

    private suspend fun assertAwaiterCancelled(name: String, awaiter: Deferred<*>) {
        assertTrue(
            runCatching { awaiter.await() }.exceptionOrNull() is CancellationException,
            "$name threw CancellationException",
        )
    }

    @Test
    fun cancelClock() =
        runTest(UnconfinedTestDispatcher()) {
            val clock = BroadcastFrameClock()
            val frameAwaiter = async { clock.withFrameNanos { it } }

            clock.cancel()

            assertAwaiterCancelled("awaiter", frameAwaiter)

            assertTrue(
                runCatching { clock.withFrameNanos { it } }.exceptionOrNull()
                    is CancellationException,
                "late awaiter threw CancellationException",
            )
        }

    @Test
    fun failClockWhenNewAwaitersNotified() =
        runTest(UnconfinedTestDispatcher()) {
            val clock = BroadcastFrameClock { throw CancellationException("failed frame clock") }

            val failingAwaiter = async { clock.withFrameNanos { it } }
            assertAwaiterCancelled("failingAwaiter", failingAwaiter)

            val lateAwaiter = async { clock.withFrameNanos { it } }
            assertAwaiterCancelled("lateAwaiter", lateAwaiter)
        }

    // This thread is intentionally ignored on JS because it tests a scenario using and that can
    // only happen when multiple threads are mutating the BroadcastFrameClock.
    //
    // Remove this annotation if JS adds proper multithreading support, then flee the area quickly.
    @IgnoreJsTarget
    @IgnoreWasmTarget
    @OptIn(InternalCoroutinesApi::class)
    @Test
    fun locklessCancellation() =
        runTest(timeout = 5.seconds) {
            val clock = BroadcastFrameClock()
            val cancellationGate = AtomicInt(1)

            var spin = true
            async(start = UNDISPATCHED) {
                clock.withFrameNanos {
                    cancellationGate.add(-1)
                    while (spin && isActive) {
                        // No-op spin loop
                    }
                }
            }

            val cancellingJob = async(start = UNDISPATCHED) { clock.withFrameNanos {} }

            launch(Dispatchers.Default) { clock.sendFrame(1) }

            // Wait for the spinlock to start
            while (cancellationGate.get() != 0) yield()

            // Assert that this line doesn't deadlock.
            cancellingJob.cancelAndJoin()

            // Make sure that we can queue up new jobs for subsequent frames
            spin = false
            assertFalse(clock.hasAwaiters)
            async(start = UNDISPATCHED) { clock.withFrameNanos {} }
            assertTrue(clock.hasAwaiters)

            clock.cancel()
        }
}
