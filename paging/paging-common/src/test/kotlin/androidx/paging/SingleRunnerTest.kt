/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class SingleRunnerTest {
    private val testScope = TestCoroutineScope()

    @Test
    fun cancelsPreviousRun() = runBlocking {
        val runner = SingleRunner()
        val job = launch(Dispatchers.Unconfined) {
            runner.runInIsolation {
                delay(Long.MAX_VALUE)
            }
        }

        runner.runInIsolation {
            // Immediately return.
        }

        assertFalse { job.isCancelled }
        assertTrue { job.isCompleted }
    }

    @Test
    fun previousRunCanCancelItself() = runBlocking {
        val runner = SingleRunner()
        val job = launch(Dispatchers.Unconfined) {
            runner.runInIsolation {
                throw CancellationException()
            }
        }
        assertTrue { job.isCancelled }
        assertTrue { job.isCompleted }
    }

    @Test
    fun preventsCompletionUntilBlockCompletes() = testScope.runBlockingTest {
        val runner = SingleRunner()
        val job = testScope.launch {
            runner.runInIsolation {
                delay(1000)
            }
        }

        advanceTimeBy(500)
        assertFalse { job.isCompleted }

        advanceTimeBy(500)
        assertTrue { job.isCompleted }
    }

    @Test
    fun orderedExecution() = testScope.runBlockingTest {
        val jobStartList = mutableListOf<Int>()

        val runner = SingleRunner()
        for (index in 0..9) {
            launch {
                runner.runInIsolation {
                    jobStartList.add(index)
                    delay(Long.MAX_VALUE)
                }
            }
        }

        // Cancel previous job.
        runner.runInIsolation {
            // Immediately return.
        }

        assertEquals(List(10) { it }, jobStartList)
    }

    @Test
    fun racingCoroutines() = testScope.runBlockingTest {
        val runner = SingleRunner()
        val output = mutableListOf<Char>()
        pauseDispatcher {
            launch {
                ('0' until '4').forEach {
                    runner.runInIsolation {
                        output.add(it)
                        delay(100)
                    }
                }
            }

            launch {
                ('a' until 'e').forEach {
                    runner.runInIsolation {
                        output.add(it)
                        delay(40)
                    }
                }
            }
            // don't let delays finish to ensure they are really cancelled
            advanceTimeBy(1)
        }
        // Despite launching separately, with different delays, we should see these always
        // interleave in the same order, since the delays aren't allowed to run in parallel and
        // each launch will cancel the other one's delay.
        assertThat(output.joinToString("")).isEqualTo("0a1b2c3d")
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun ensureIsolation_whenCancelationIsIgnoredByThePreviousBlock() {
        // make sure we wait for previous one if it ignores cancellation
        val singleRunner = SingleRunner()
        val output = Collections.synchronizedList(mutableListOf<Int>())
        // using a latch instead of a mutex to avoid suspension
        val firstStarted = CountDownLatch(1)
        GlobalScope.launch {
            singleRunner.runInIsolation {
                // this code uses latches and thread sleeps instead of Mutex and delay to mimic
                // a code path which ignores coroutine cancellation
                firstStarted.countDown()
                repeat(10) {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    Thread.sleep(100)
                    output.add(it)
                }
            }
        }

        val job2 = GlobalScope.launch {
            @Suppress("BlockingMethodInNonBlockingContext")
            firstStarted.await()
            singleRunner.runInIsolation {
                repeat(10) {
                    output.add(it + 10)
                }
            }
        }
        runBlocking {
            withTimeout(TimeUnit.SECONDS.toMillis(10)) {
                job2.join()
            }
        }
        assertThat(output).isEqualTo(
            // if cancellation is ignored, make sure we wait for it to finish.
            (0 until 20).toList()
        )
    }

    @Test
    fun priority() = testScope.runBlockingTest {
        val runner = SingleRunner()
        val output = mutableListOf<String>()
        launch {
            runner.runInIsolation(
                priority = 2
            ) {
                output.add("a")
                delay(10)
                output.add("b")
                delay(100)
                output.add("unexpected")
            }
        }

        // should not run
        runner.runInIsolation(
            priority = 1
        ) {
            output.add("unexpected - 2")
        }
        advanceTimeBy(20)
        runner.runInIsolation(
            priority = 3
        ) {
            output.add("c")
        }
        advanceUntilIdle()
        // now lower priority can run since higher priority is complete
        runner.runInIsolation(
            priority = 1
        ) {
            output.add("d")
        }
        assertThat(output)
            .containsExactly("a", "b", "c", "d")
    }
}
