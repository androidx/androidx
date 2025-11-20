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

package androidx.compose.ui.test

import kotlin.math.ceil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class AbstractMainTestClock(
    /**
     * The underlying scheduler which this clock controls. Only advance the time or run current
     * tasks from the scheduler on the UI thread, as any task could be a UI thread only task.
     */
    private val scheduler: TestCoroutineScheduler,
    private val frameDelayMillis: Long,
    private val isStandardTestDispatcherSupportEnabled: Boolean,
    private val runOnUiThread: (action: () -> Unit) -> Unit,
) : MainTestClock {

    override val currentTime: Long
        get() = scheduler.currentTime

    override var autoAdvance: Boolean = true

    override fun advanceTimeByFrame() {
        advanceScheduler(frameDelayMillis)
    }

    override fun advanceTimeBy(milliseconds: Long, ignoreFrameDuration: Boolean) {
        val actualDelay =
            if (ignoreFrameDuration) {
                milliseconds
            } else {
                ceil(milliseconds.toDouble() / frameDelayMillis).toLong() * frameDelayMillis
            }
        advanceScheduler(actualDelay)
    }

    override fun advanceTimeUntil(timeoutMillis: Long, condition: () -> Boolean) {
        val startTime = currentTime
        runOnUiThread {
            // With a StandardTestDispatcher, it could be that tasks are due which can satisfy the
            // condition, so run all pending tasks before checking the condition.
            if (isStandardTestDispatcherSupportEnabled) {
                scheduler.runCurrent()
            }

            while (!condition()) {
                advanceScheduler(frameDelayMillis)
                if (currentTime - startTime > timeoutMillis) {
                    throw ComposeTimeoutException(
                        "Condition still not satisfied after $timeoutMillis ms"
                    )
                }
            }
        }
    }

    /**
     * Executes all tasks that are currently due for immediate execution on this clock.
     *
     * Due tasks are operations scheduled to run on this clock without any delay. This method should
     * only be necessary when the test is running on a **confined**
     * [TestDispatcher][kotlinx.coroutines.test.TestDispatcher] (like
     * [StandardTestDispatcher][kotlinx.coroutines.test.StandardTestDispatcher]), where tasks are
     * queued but not executed automatically. On an
     * [UnconfinedTestDispatcher][kotlinx.coroutines.test.UnconfinedTestDispatcher], all tasks that
     * are scheduled without delay will be executed immediately, so they will never end up as a due
     * task.
     *
     * This function is a fundamental building block for more advanced synchronization APIs. For
     * example, it is used internally by APIs like `waitForIdle()`, `advanceTimeBy()`, etc. to
     * process pending work before or after manipulating the clock.
     *
     * In almost all testing scenarios, you should prefer using higher-level APIs to ensure proper
     * synchronization.
     *
     * Only call this function directly if you have a specific need to run only the immediately
     * available tasks without advancing time or waiting for a complete idle state.
     */
    fun runCurrent() {
        runOnUiThread { scheduler.runCurrent() }
    }

    private fun advanceScheduler(millis: Long) {
        runOnUiThread {
            // advanceTimeBy() runs all tasks up to, but not including, the new time
            scheduler.advanceTimeBy(millis)
            // So finish with a call to runCurrent() to run all tasks at the new time
            scheduler.runCurrent()
        }
    }
}
