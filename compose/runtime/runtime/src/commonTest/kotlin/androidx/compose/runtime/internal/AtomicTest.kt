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

package androidx.compose.runtime.internal

import androidx.compose.runtime.OneShotCancellationHandle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AtomicTest {

    @Test
    fun atomicInt_getAndSet() {
        val value = AtomicInt(0)
        assertEquals(0, value.getAndSet(42))
        assertEquals(42, value.get())
        assertEquals(42, value.getAndSet(-1))
        assertEquals(-1, value.get())
    }

    @Test
    fun atomicBoolean_getAndSet_falseToTrue() {
        val flag = AtomicBoolean(false)
        assertFalse(flag.getAndSet(true))
        assertTrue(flag.get())
    }

    @Test
    fun atomicBoolean_getAndSet_trueToFalse() {
        val flag = AtomicBoolean(true)
        assertTrue(flag.getAndSet(false))
        assertFalse(flag.get())
    }

    @Test
    fun atomicBoolean_getAndSet_sameValue() {
        val initiallyFalse = AtomicBoolean(false)
        assertFalse(initiallyFalse.getAndSet(false))
        assertFalse(initiallyFalse.get())

        val initiallyTrue = AtomicBoolean(true)
        assertTrue(initiallyTrue.getAndSet(true))
        assertTrue(initiallyTrue.get())
    }

    @Test
    fun oneShotCancellationHandle_cancelsOnce() {
        var cancelCount = 0
        val handle = OneShotCancellationHandle { cancelCount++ }

        handle.cancel()
        handle.cancel()

        assertEquals(1, cancelCount)
    }
}
