/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.concurrent

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.test.Test
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield

class CloseBarrierTest {

    @Test
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun oneBlocker() = runTest {
        val actionPerformed = atomic(false)
        val closeBarrier = CloseBarrier {
            assertThat(actionPerformed.compareAndSet(expect = false, update = true)).isTrue()
        }
        val jobLaunched = Mutex(locked = true)

        // block the barrier
        assertThat(closeBarrier.block()).isTrue()

        // launch a close action, expect it to wait since there is one blocker
        val closeJob =
            launch(newFixedThreadPoolContext(1, "CloseThread")) {
                jobLaunched.unlock()
                closeBarrier.close()
            }

        // yield for launch and verify the close action has not been performed
        yield()
        jobLaunched.withLock { assertThat(actionPerformed.value).isFalse() }

        // unblock the barrier, close job should complete
        closeBarrier.unblock()
        closeJob.join()

        // verify action was performed
        assertThat(actionPerformed.value).isTrue()

        // verify a new block is not granted since the barrier is already close
        assertThat(closeBarrier.block()).isFalse()
    }

    @Test
    fun noBlockers() = runTest {
        val actionPerformed = atomic(false)
        val closeBarrier = CloseBarrier {
            assertThat(actionPerformed.compareAndSet(expect = false, update = true)).isTrue()
        }

        // Validate close action is performed immediately if there are no blockers
        closeBarrier.close()

        assertThat(actionPerformed.value).isTrue()
    }

    @Test
    fun unbalancedBlocker() = runTest {
        val closeBarrier = CloseBarrier {}
        assertThrows<IllegalStateException> { closeBarrier.unblock() }
            .hasMessageThat()
            .isEqualTo("Unbalanced call to unblock() detected.")
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun noStarvation() = runTest {
        val actionPerformed = atomic(false)
        val closeBarrier = CloseBarrier {
            assertThat(actionPerformed.compareAndSet(expect = false, update = true)).isTrue()
        }
        val jobLaunched = Mutex(locked = true)

        // launch a heavy blocker, it should not starve the close action
        val blockerJob =
            launch(newFixedThreadPoolContext(1, "BlockerThread")) {
                jobLaunched.unlock()
                while (true) {
                    if (closeBarrier.block()) {
                        closeBarrier.unblock()
                    } else {
                        break
                    }
                }
            }

        // yield for launch and verify the close action has not been performed in an attempt to
        // get the block / unblock loop going
        yield()
        jobLaunched.withLock { assertThat(actionPerformed.value).isFalse() }

        // initiate the close action, test should not deadlock (or timeout) meaning the barrier
        // will not cause the caller to starve
        closeBarrier.close()
        blockerJob.join()
        assertThat(actionPerformed.value).isTrue()
    }
}
