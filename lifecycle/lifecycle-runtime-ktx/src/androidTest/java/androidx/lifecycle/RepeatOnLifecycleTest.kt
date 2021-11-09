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

package androidx.lifecycle

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Test

@SmallTest
class RepeatOnLifecycleTest {

    private val expectations = Expectations()
    private val owner = FakeLifecycleOwner()

    @Test
    fun testBlockRunsWhenCreatedStateIsReached() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.CREATED)
        expectations.expect(1)

        val repeatingWorkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                expectations.expect(2)
            }
        }

        expectations.expect(3)
        owner.setState(Lifecycle.State.DESTROYED)
        assertThat(repeatingWorkJob.isCompleted).isTrue()
    }

    @Test
    fun testBlockRunsWhenStartedStateIsReached() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.CREATED)
        expectations.expect(1)

        val repeatingWorkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                expectations.expect(2)
            }
        }

        owner.setState(Lifecycle.State.STARTED)
        expectations.expect(3)
        owner.setState(Lifecycle.State.DESTROYED)
        assertThat(repeatingWorkJob.isCompleted).isTrue()
    }

    @Test
    fun testBlockRunsWhenResumedStateIsReached() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.CREATED)
        expectations.expect(1)

        val repeatingWorkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                expectations.expect(3)
            }
        }

        owner.setState(Lifecycle.State.STARTED)
        expectations.expect(2)
        owner.setState(Lifecycle.State.RESUMED)
        expectations.expect(4)
        owner.setState(Lifecycle.State.DESTROYED)
        assertThat(repeatingWorkJob.isCompleted).isTrue()
    }

    @Test
    fun testBlocksRepeatsExecution() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.CREATED)
        var restarted = false
        expectations.expect(1)

        val repeatingWorkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (!restarted) {
                    expectations.expect(2)
                } else {
                    expectations.expect(5)
                }
            }
        }

        owner.setState(Lifecycle.State.RESUMED)
        expectations.expect(3)
        owner.setState(Lifecycle.State.STARTED)
        expectations.expect(4)

        restarted = true
        owner.setState(Lifecycle.State.RESUMED)
        expectations.expect(6)
        owner.setState(Lifecycle.State.DESTROYED)
        assertThat(repeatingWorkJob.isCompleted).isTrue()
    }

    @Test
    fun testBlocksRepeatsExecutionSerially() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.CREATED)
        var restarted = false
        expectations.expect(1)

        val repeatingWorkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (!restarted) {
                    expectations.expect(2)
                    try {
                        awaitCancellation()
                    } finally {
                        withContext(NonCancellable) {
                            expectations.expect(4)
                            yield()
                            expectations.expect(6)
                            yield()
                            expectations.expect(8)
                        }
                    }
                } else {
                    expectations.expect(9)
                }
            }
        }

        owner.setState(Lifecycle.State.RESUMED)
        expectations.expect(3)
        owner.setState(Lifecycle.State.STARTED)
        expectations.expect(5)

        restarted = true
        owner.setState(Lifecycle.State.RESUMED)
        yield()
        expectations.expect(7)
        yield()
        owner.setState(Lifecycle.State.DESTROYED)
        assertThat(repeatingWorkJob.isCompleted).isTrue()
    }

    @Test
    fun testBlockIsCancelledWhenLifecycleIsDestroyed() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.RESUMED)
        expectations.expect(1)

        val repeatingWorkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    expectations.expect(2)
                    awaitCancellation()
                } catch (e: CancellationException) {
                    expectations.expect(4)
                }
            }
        }

        expectations.expect(3)
        owner.setState(Lifecycle.State.DESTROYED)

        yield() // let the cancellation code run before asserting it happened
        expectations.expect(5)
        assertThat(repeatingWorkJob.isCompleted).isTrue()
    }

    @Test
    fun testBlockRunsOnSubsequentLifecycleState() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.RESUMED)
        expectations.expect(1)

        val repeatingWorkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                expectations.expect(2)
            }
        }

        expectations.expect(3)
        owner.setState(Lifecycle.State.DESTROYED)
        assertThat(repeatingWorkJob.isCompleted).isTrue()
    }

    @Test
    fun testBlockDoesNotStartIfLifecycleIsDestroyed() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.DESTROYED)
        expectations.expect(1)

        val repeatingWorkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                expectations.expectUnreached()
            }
        }

        expectations.expect(2)
        assertThat(repeatingWorkJob.isCompleted).isTrue()
    }

    @Test
    fun testCancellingTheReturnedJobCancelsTheBlock() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.RESUMED)
        expectations.expect(1)

        val repeatingWorkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    expectations.expect(2)
                    awaitCancellation()
                } catch (e: CancellationException) {
                    expectations.expect(4)
                }
            }
        }

        expectations.expect(3)
        repeatingWorkJob.cancel()
        yield() // let the cancellation code run before asserting it happened

        expectations.expect(5)
        assertThat(repeatingWorkJob.isCancelled).isTrue()
        assertThat(repeatingWorkJob.isCompleted).isTrue()
    }

    @Test
    fun testCancellingACustomJobCanBeHandled() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.RESUMED)
        expectations.expect(1)

        val customJob = Job()
        val repeatingWorkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                withContext(customJob) {
                    try {
                        expectations.expect(2)
                        awaitCancellation()
                    } catch (e: CancellationException) {
                        expectations.expect(4)
                    }
                }
            }
        }

        expectations.expect(3)
        customJob.cancel()
        yield() // let the cancellation code run before asserting it happened

        expectations.expect(5)
        assertThat(customJob.isCancelled).isTrue()
        assertThat(customJob.isCompleted).isTrue()
        owner.setState(Lifecycle.State.DESTROYED)
        assertThat(repeatingWorkJob.isCompleted).isTrue()
    }

    @Test
    fun testCancellingACustomJobDoesNotReRunThatBlock() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.CREATED)
        var restarted = false
        expectations.expect(1)

        val customJob = Job()
        val repeatingWorkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (!restarted) {
                    expectations.expect(2)
                } else {
                    expectations.expect(6)
                }
                withContext(customJob) {
                    if (!restarted) {
                        expectations.expect(3)
                    } else {
                        expectations.expectUnreached()
                    }
                }
            }
        }

        owner.setState(Lifecycle.State.RESUMED)
        expectations.expect(4)
        owner.setState(Lifecycle.State.STARTED)
        expectations.expect(5)

        customJob.cancel()
        restarted = true
        owner.setState(Lifecycle.State.RESUMED)
        expectations.expect(7)
        owner.setState(Lifecycle.State.DESTROYED)
        assertThat(repeatingWorkJob.isCompleted).isTrue()
    }

    @Test
    fun testCancellingTheJobDoesNotRestartTheBlockOnNewStates() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.RESUMED)
        expectations.expect(1)

        var restarted = false
        val repeatingWorkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (!restarted) {
                    expectations.expect(2)
                } else {
                    expectations.expectUnreached()
                }
            }
        }

        expectations.expect(3)
        repeatingWorkJob.cancel()
        assertThat(repeatingWorkJob.isCancelled).isTrue()
        assertThat(repeatingWorkJob.isCompleted).isTrue()
        owner.setState(Lifecycle.State.STARTED)

        restarted = true
        expectations.expect(4)
        owner.setState(Lifecycle.State.RESUMED)
        yield() // Block shouldn't restart

        expectations.expect(5)
        owner.setState(Lifecycle.State.DESTROYED)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testBlockRunsWhenLogicUsesWithContext() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.CREATED)
        expectations.expect(1)

        val testDispatcher = TestCoroutineDispatcher().apply {
            runBlockingTest {
                owner.lifecycleScope.launch {
                    owner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                        withContext(this@apply) {
                            expectations.expect(2)
                        }
                    }
                }
            }
        }

        expectations.expect(3)
        owner.setState(Lifecycle.State.DESTROYED)
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun testBlockDoesNotStartWithDestroyedState() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.STARTED)
        expectations.expect(1)

        val repeatingWorkJob = owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.DESTROYED) {
                expectations.expectUnreached()
            }
        }

        expectations.expect(2)
        owner.setState(Lifecycle.State.DESTROYED)
        assertThat(repeatingWorkJob.isCompleted).isTrue()
    }

    @Test
    fun testExceptionWithInitializedState() = runBlocking(Dispatchers.Main) {
        val exceptions: MutableList<Throwable> = mutableListOf()
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            exceptions.add(exception)
        }

        owner.lifecycleScope.launch(coroutineExceptionHandler) {
            owner.repeatOnLifecycle(Lifecycle.State.INITIALIZED) {
                // IllegalArgumentException expected
            }
        }

        assertThat(exceptions[0]).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exceptions).hasSize(1)
    }
}
