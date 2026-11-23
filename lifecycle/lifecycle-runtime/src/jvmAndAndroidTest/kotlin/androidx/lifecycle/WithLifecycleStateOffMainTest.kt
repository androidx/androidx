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

package androidx.lifecycle

import androidx.testutils.lifecycle.FakeLifecycleOwner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

/** Tests for [withStateAtLeast] and friends when they are called from outside the main thread. */
class WithLifecycleStateOffMainTest {
    @Test
    fun testOffMainInitialStarted() = runLifecycleTest {
        val owner = FakeLifecycleOwner(Lifecycle.State.STARTED)

        val result =
            withContext(Dispatchers.Default) {
                withTimeout(TIMEOUT_MILLIS) { owner.withStarted { "block ran" } }
            }

        assertEquals("block ran", result)
    }

    @Test
    fun testOffMainBlockRunsWithLifecycleStateChange() = runLifecycleTest {
        val owner = FakeLifecycleOwner(Lifecycle.State.CREATED)
        val registry = owner.lifecycle as LifecycleRegistry

        val result =
            async(Dispatchers.Default) {
                withTimeout(TIMEOUT_MILLIS) { owner.withStarted { "block ran" } }
            }
        // Wait until the observer has been added on the main thread.
        while (registry.observerCount == 0) yield()
        owner.setState(Lifecycle.State.STARTED)

        assertEquals("block ran", result.await())
    }

    @Test
    fun testOffMainBlockCancelledWhenInitiallyDestroyed() = runLifecycleTest {
        val owner = FakeLifecycleOwner(Lifecycle.State.CREATED)
        owner.setState(Lifecycle.State.DESTROYED)

        withContext(Dispatchers.Default) {
            assertFailsWith<LifecycleDestroyedException> {
                withTimeout(TIMEOUT_MILLIS) { owner.withStarted {} }
            }
        }
    }

    @Test
    fun testOffMainBlockCancelledWhenDestroyedBeforeObserverIsAdded() = runLifecycleTest {
        val owner = FakeLifecycleOwner(Lifecycle.State.CREATED)
        val callStarted = CountDownLatch(1)

        val result =
            async(Dispatchers.Default) {
                callStarted.countDown()
                runCatching { withTimeout(TIMEOUT_MILLIS) { owner.withStarted {} } }
            }
        // Block the main thread until the call has started, then destroy the lifecycle before the
        // main thread gets to run anything the call has posted to it.
        assertTrue(callStarted.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
        owner.setState(Lifecycle.State.DESTROYED)

        assertIs<LifecycleDestroyedException>(result.await().exceptionOrNull())
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
