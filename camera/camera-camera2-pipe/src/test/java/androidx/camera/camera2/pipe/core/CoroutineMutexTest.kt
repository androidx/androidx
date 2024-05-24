/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.core

import android.os.Build
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CancellationException
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.robolectric.annotation.Config

@RunWith(JUnit4::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CoroutineMutexTest {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Test
    fun testMultipleSequencesAreIndependent() = runBlocking {
        val sequence1 = CoroutineMutex()
        val sequence2 = CoroutineMutex()

        val internalResult =
            sequence1.withLockAsync(scope) { sequence2.withLockAsync(scope) { 42 }.await() }.await()

        assertThat(internalResult).isEqualTo(42)
    }

    @Test
    fun testSequenceOrder() = runBlocking {
        var counter = 0
        val sequence = CoroutineMutex()

        val first = sequence.withLockAsync(scope) { ++counter }
        val second = sequence.withLockAsync(scope) { ++counter }
        val third = sequence.withLockAsync(scope) { ++counter }

        assertThat(listOf(second, first, third).awaitAll()).containsExactly(2, 1, 3).inOrder()
    }

    @Test
    fun testLaunchOnOuterScopeWithinExecuteAsyncDoesNotDeadlock() = runBlocking {
        var output: Int = -1
        val sequence = CoroutineMutex()

        coroutineScope {
            val sharedMutex = Mutex(locked = true)

            sequence
                .withLockAsync(this) {
                    // Note: The receiver is `this@coroutineScope`. The `sequenceAsync {}` block
                    // will return
                    // racing the launched block.
                    this@coroutineScope.launch {
                        sharedMutex.lock()
                        // Doesn't throw as no block reentered `sequenceAsync {}`.
                        sequence.withLockAsync(this) { output = 42 }.await()
                    }
                }
                .await()

            sharedMutex.unlock()
        }
        assertThat(output).isEqualTo(42)
    }

    @Test
    fun testLaunchOrderIndependentOfScope() {
        var counter = 0
        val sequence = CoroutineMutex()
        val scope1 = CoroutineScope(Job() + Dispatchers.Default)
        val scope2 = CoroutineScope(Job() + Dispatchers.Default)

        val output = mutableListOf<Deferred<Int>>()

        for (i in 0..5000) {
            if (i % 2 > 0) {
                output.add(sequence.withLockAsync(scope1) { counter++ })
            } else {
                output.add(sequence.withLockAsync(scope2) { counter++ })
            }
        }

        runBlocking {
            assertThat(output.awaitAll()).containsExactlyElementsIn(0.rangeTo(5000)).inOrder()
        }
    }

    @Test
    fun testCancelledCoroutineDropsFromSequence() {
        var counter = 0
        val sequence = CoroutineMutex()
        val mutex = Mutex(locked = true)

        val first = sequence.withLockAsync(scope) { mutex.withLock { ++counter } }
        val second = sequence.withLockAsync(scope) { mutex.withLock { ++counter } }
        val third = sequence.withLockAsync(scope) { mutex.withLock { ++counter } }

        second.cancel()

        mutex.unlock()
        runBlocking {
            assertFailsWith(CancellationException::class) { second.await() }
            assertThat(first.await()).isEqualTo(1)
            assertThat(third.await()).isEqualTo(2)
        }
    }

    @Test
    fun testFailedCoroutineDropsFromSequenceWithoutAffectingLaterBlocks() {
        class ExpectedException : Exception()

        var counter = 0
        val sequence = CoroutineMutex()

        val first = sequence.withLockAsync(scope) { ++counter }
        val second = sequence.withLockAsync(scope) { throw ExpectedException() }
        val third = sequence.withLockAsync(scope) { ++counter }

        runBlocking {
            assertThat(first.await()).isEqualTo(1)
            assertFailsWith(ExpectedException::class) { second.await() }
            assertThat(third.await()).isEqualTo(2)
        }
    }
}
