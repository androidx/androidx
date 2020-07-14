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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
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
            advanceUntilIdle()
        }
        // Despite launching separately, with different delays, we should see these always
        // interleave in the same order, since the delays aren't allowed to run in parallel and
        // each launch will cancel the other one's delay.
        assertThat(output.joinToString("")).isEqualTo("0a1b2c3d")
    }
}