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

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package androidx.paging

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

        assertTrue { job.isCancelled }
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
}