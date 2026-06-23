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

package androidx.compose.ui.window

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.PlatformPrefetchRequest
import androidx.compose.ui.platform.PlatformPrefetchRequestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import platform.Foundation.NSTimeInterval

@OptIn(InternalComposeUiApi::class)
class PlatformPrefetchSchedulerTest {
    @Test
    fun testExecutesHighPriorityRequestsBeforeLowPriorityRequests() {
        val scheduler = scheduler()
        val executedRequests = mutableListOf<String>()

        scheduler.scheduleLowPriorityPrefetch(request("low-0", executedRequests))
        scheduler.scheduleHighPriorityPrefetch(request("high-0", executedRequests))
        scheduler.scheduleLowPriorityPrefetch(request("low-1", executedRequests))
        scheduler.scheduleHighPriorityPrefetch(request("high-1", executedRequests))

        scheduler.execute(lastFrameTimestamp = 0.0, targetTimestamp = 1.0, didDraw = false)

        assertEquals(listOf("high-0", "high-1", "low-0", "low-1"), executedRequests)
    }

    @Test
    fun testKeepsRequestScheduledWhenItHasMoreWorkToDo() {
        val scheduler = scheduler()
        val executedRequests = mutableListOf<String>()

        scheduler.scheduleLowPriorityPrefetch(
            request("request-0", executedRequests, executeResults = listOf(true, false))
        )
        scheduler.scheduleLowPriorityPrefetch(request("request-1", executedRequests))

        scheduler.execute(lastFrameTimestamp = 0.0, targetTimestamp = 1.0, didDraw = false)

        assertEquals(listOf("request-0"), executedRequests)

        scheduler.execute(lastFrameTimestamp = 1.0, targetTimestamp = 2.0, didDraw = false)

        assertEquals(listOf("request-0", "request-0", "request-1"), executedRequests)
    }

    @Test
    fun testKeepsExecutingStartedHighPriorityRequestBeforeNewerHighPriorityRequests() {
        val scheduler = scheduler()
        val executedRequests = mutableListOf<String>()

        scheduler.scheduleHighPriorityPrefetch(
            request("request-0", executedRequests, executeResults = listOf(true, false))
        )

        scheduler.execute(lastFrameTimestamp = 0.0, targetTimestamp = 1.0, didDraw = false)

        assertEquals(listOf("request-0"), executedRequests)

        scheduler.scheduleHighPriorityPrefetch(request("request-1", executedRequests))
        scheduler.execute(lastFrameTimestamp = 1.0, targetTimestamp = 2.0, didDraw = false)

        assertEquals(listOf("request-0", "request-0", "request-1"), executedRequests)
    }

    @Test
    fun testDoesNotExecuteRequestWhenNextFrameDeadlinePassed() {
        val scheduler = scheduler(currentTime = { 1.01 })
        val executedRequests = mutableListOf<String>()

        scheduler.scheduleLowPriorityPrefetch(request("request", executedRequests))
        scheduler.execute(lastFrameTimestamp = 1.0, targetTimestamp = 1.01, didDraw = false)

        assertTrue(executedRequests.isEmpty())
    }

    @Test
    fun testReportsNoScheduledWorkWhenNoRequestsAreScheduledAndDidNotDraw() {
        val hasWorkEvents = mutableListOf<Boolean>()
        val scheduler = scheduler(onHasWorkScheduled = hasWorkEvents::add)

        scheduler.execute(lastFrameTimestamp = 0.0, targetTimestamp = 1.0, didDraw = false)

        assertEquals(listOf(false), hasWorkEvents)
    }

    @Test
    fun testReportsNoScheduledWorkWhenNoRequestsAreScheduledAndDidDraw() {
        val hasWorkEvents = mutableListOf<Boolean>()
        val scheduler = scheduler(onHasWorkScheduled = hasWorkEvents::add)

        scheduler.execute(lastFrameTimestamp = 0.0, targetTimestamp = 1.0, didDraw = true)

        assertEquals(listOf(false), hasWorkEvents)
    }

    @Test
    fun testReportsNoScheduledWorkWhenQueueDrains() {
        val hasWorkEvents = mutableListOf<Boolean>()
        val scheduler = scheduler(onHasWorkScheduled = hasWorkEvents::add)

        scheduler.scheduleLowPriorityPrefetch(request("request"))
        scheduler.execute(lastFrameTimestamp = 0.0, targetTimestamp = 1.0, didDraw = false)

        assertEquals(listOf(true, false), hasWorkEvents)
    }

    @Test
    fun testRequestReceivesAvailableTimeBeforeNextFrameWhenNotIdle() {
        var currentTime = 1.0
        val scheduler = scheduler(currentTime = { currentTime })
        val availableTimes = mutableListOf<Long>()

        scheduler.scheduleLowPriorityPrefetch(
            request("request", availableTimes = availableTimes)
        )

        scheduler.execute(lastFrameTimestamp = 1.0, targetTimestamp = 1.01, didDraw = false)

        assertEquals(listOf(10_000_000L), availableTimes)
    }

    @Test
    fun testRequestReceivesUnboundedAvailableTimeWhenDrawIsIdle() {
        var currentTime = 1.0
        val scheduler = scheduler(currentTime = { currentTime })
        val availableTimes = mutableListOf<Long>()

        scheduler.execute(lastFrameTimestamp = 1.0, targetTimestamp = 1.01, didDraw = true)
        scheduler.execute(lastFrameTimestamp = 1.01, targetTimestamp = 1.03, didDraw = false)

        scheduler.scheduleLowPriorityPrefetch(
            request("request", availableTimes = availableTimes)
        )
        currentTime = 1.031

        scheduler.execute(lastFrameTimestamp = 1.03, targetTimestamp = 1.05, didDraw = false)

        assertEquals(listOf(Long.MAX_VALUE), availableTimes)
    }

    @Test
    fun testDoesNotExecuteRequestsWhenDisposed() {
        val scheduler = scheduler()
        val executedRequests = mutableListOf<String>()

        scheduler.scheduleLowPriorityPrefetch(request("request-0", executedRequests))
        scheduler.dispose()
        scheduler.scheduleHighPriorityPrefetch(request("request-1", executedRequests))
        scheduler.execute(lastFrameTimestamp = 0.0, targetTimestamp = 1.0, didDraw = false)

        assertTrue(executedRequests.isEmpty())
    }

    private fun scheduler(
        onHasWorkScheduled: (Boolean) -> Unit = {},
        currentTime: () -> NSTimeInterval = { 0.0 },
    ) = PlatformPrefetchSchedulerImpl(
        onHasWorkScheduled = onHasWorkScheduled,
        currentTime = currentTime,
    )

    private fun request(
        name: String,
        executedRequests: MutableList<String> = mutableListOf(),
        availableTimes: MutableList<Long> = mutableListOf(),
        executeResults: List<Boolean> = listOf(false),
    ) = object : PlatformPrefetchRequest {
        private var executionCount = 0

        override fun PlatformPrefetchRequestScope.execute(): Boolean {
            executedRequests.add(name)
            availableTimes.add(availableTimeNanos())

            val result = executeResults.getOrElse(executionCount) { false }
            executionCount += 1
            return result
        }
    }
}
