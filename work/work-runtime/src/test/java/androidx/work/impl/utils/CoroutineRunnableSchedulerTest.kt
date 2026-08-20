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

package androidx.work.impl.utils

import androidx.work.impl.utils.taskexecutor.SerialExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock

// Opt-in to ExperimentalCoroutinesApi at the class level to use StandardTestDispatcher
// and runTest for virtual-time control of delays.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class CoroutineRunnableSchedulerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val fakeTaskExecutor =
        object : TaskExecutor {
            override fun getMainThreadExecutor(): Executor = Executor { it.run() }

            override fun getSerialTaskExecutor(): SerialExecutor = mock(SerialExecutor::class.java)

            override fun executeOnTaskThread(runnable: Runnable) {
                runnable.run()
            }

            override fun getCoroutineScope(): CoroutineScope = testScope

            override fun getTaskCoroutineDispatcher(): CoroutineDispatcher = testDispatcher
        }

    private lateinit var scheduler: CoroutineRunnableScheduler

    @Before
    fun setUp() {
        scheduler = CoroutineRunnableScheduler(fakeTaskExecutor)
    }

    @Test
    fun testScheduleWithDelay_runsAfterDelay() =
        testScope.runTest {
            var ran = false
            scheduler.scheduleWithDelay(100, Runnable { ran = true })

            runCurrent()
            assertThat(ran).isFalse()

            advanceTimeBy(100.milliseconds)
            runCurrent()
            assertThat(ran).isTrue()
        }

    @Test
    fun testCancel_preventsExecution() =
        testScope.runTest {
            var ran = false
            val runnable = Runnable { ran = true }
            scheduler.scheduleWithDelay(100, runnable)

            advanceTimeBy(50.milliseconds)
            runCurrent()
            scheduler.cancel(runnable)

            advanceTimeBy(100.milliseconds)
            runCurrent()
            assertThat(ran).isFalse()
        }

    @Test
    fun testScheduleTwice_cancelsPrevious() =
        testScope.runTest {
            var runCount = 0
            val runnable = Runnable { runCount++ }

            // Schedule first time for 100ms
            scheduler.scheduleWithDelay(100, runnable)

            advanceTimeBy(50.milliseconds)
            runCurrent()

            // Schedule again for 100ms (should run at 50 + 100 = 150ms total)
            // This should cancel the first schedule (which would have run at 100ms)
            scheduler.scheduleWithDelay(100, runnable)

            // Advance past 100ms (when first would have run). Since first is cancelled, count is 0
            advanceTimeBy(50.milliseconds)
            runCurrent()
            assertThat(runCount).isEqualTo(0)

            // Advance past 150ms (when second runs)
            advanceTimeBy(50.milliseconds)
            runCurrent()
            assertThat(runCount).isEqualTo(1)
        }
}
