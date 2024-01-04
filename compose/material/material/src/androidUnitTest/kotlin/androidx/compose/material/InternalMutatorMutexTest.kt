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

package androidx.compose.material

import androidx.compose.foundation.MutatePriority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("RemoveExplicitTypeArguments")
@RunWith(JUnit4::class)
internal class InternalMutatorMutexTest {
    interface MutateCaller {
        suspend fun <R> mutate(
            priority: MutatePriority = MutatePriority.Default,
            block: suspend () -> R
        ): R
    }

    class MutateWithoutReceiverCaller(private val mutex: InternalMutatorMutex) : MutateCaller {
        override suspend fun <R> mutate(
            priority: MutatePriority,
            block: suspend () -> R
        ): R = mutex.mutate(priority, block)
    }

    class MutateWithReceiverCaller(private val mutex: InternalMutatorMutex) : MutateCaller {
        override suspend fun <R> mutate(
            priority: MutatePriority,
            block: suspend () -> R
        ): R {
            val receiver = Any()
            return mutex.mutateWith(receiver, priority) {
                assertSame("mutateWith receiver", receiver, this)
                block()
            }
        }
    }

    @Test
    fun newMutatorCancelsOld() = runBlocking<Unit> {
        val mutex = InternalMutatorMutex()
        runNewMutatorCancelsOld(MutateWithoutReceiverCaller(mutex))
        runNewMutatorCancelsOld(MutateWithReceiverCaller(mutex))
    }

    private suspend fun runNewMutatorCancelsOld(mutex: MutateCaller) = coroutineScope<Unit> {
        val firstMutatorJob = launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.mutate {
                // Suspend forever
                suspendCancellableCoroutine<Unit> { }
            }
            fail("mutator should have thrown CancellationException")
        }

        // Cancel firstMutatorJob
        mutex.mutate { }
        assertTrue("first mutator was cancelled", firstMutatorJob.isCancelled)
    }

    @Test
    fun mutatorsCancelByPriority() = runBlocking<Unit> {
        val mutex = InternalMutatorMutex()
        runMutatorsCancelByPriority(MutateWithoutReceiverCaller(mutex))
        runMutatorsCancelByPriority(MutateWithReceiverCaller(mutex))
    }

    @Test
    fun tryMutateBlockingSuspendsSubsequentMutate() = runBlocking<Unit> {
        val mutex = InternalMutatorMutex()
        val tryMutateJob = launch(start = CoroutineStart.LAZY) {
            mutex.tryMutate {
                while (true) { /* Block forever */ }
            }
        }
        val mutateJob = launch(start = CoroutineStart.LAZY) {
            mutex.mutate {
                if (tryMutateJob.isActive) fail("Attempted to mutate before tryMutate finished")
            }
        }
        tryMutateJob.start()
        mutateJob.start()

        tryMutateJob.cancelAndJoin()
        mutateJob.cancelAndJoin()
    }

    @Test
    fun tryMutateDoesNotOverrideActiveCaller() = runBlocking<Unit> {
        val mutex = InternalMutatorMutex()
        val mutateJob = launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.mutate {
                suspendCancellableCoroutine { } // Suspend forever
            }
        }
        val tryMutateSuccessful = mutex.tryMutate { }
        Assert.assertFalse(
            "tryMutate should not run if there is an ongoing mutation",
            tryMutateSuccessful
        )
        mutateJob.cancelAndJoin()
    }

    @Test
    fun tryMutateBlockingTryMutateLocks() = runBlocking<Unit> {
        val mutex = InternalMutatorMutex()
        mutex.tryMutate {
            val tryMutateSuccessful = mutex.tryMutate { }
            Assert.assertFalse(
                "tryMutate should not run if there is an ongoing mutation",
                tryMutateSuccessful
            )
        }
    }

    private suspend fun runMutatorsCancelByPriority(mutex: MutateCaller) = coroutineScope<Unit> {
        for (firstPriority in MutatePriority.values()) {
            for (secondPriority in MutatePriority.values()) {
                val firstMutatorJob = launch(start = CoroutineStart.UNDISPATCHED) {
                    mutex.mutate(firstPriority) {
                        // Suspend forever
                        suspendCancellableCoroutine<Unit> { }
                    }
                    fail("mutator should have thrown CancellationException")
                }

                // Attempt mutation and (maybe) cause cancellation
                try {
                    mutex.mutate(secondPriority) { }
                } catch (ce: CancellationException) {
                    assertTrue(
                        "attempted second mutation was cancelled with lower priority",
                        secondPriority < firstPriority
                    )
                }
                assertEquals(
                    "first mutator of priority $firstPriority cancelled by second " +
                        "mutator of priority $secondPriority",
                    secondPriority >= firstPriority,
                    firstMutatorJob.isCancelled
                )

                // Cleanup regardless of results
                firstMutatorJob.cancel()
                firstMutatorJob.join()
            }
        }
    }
}
