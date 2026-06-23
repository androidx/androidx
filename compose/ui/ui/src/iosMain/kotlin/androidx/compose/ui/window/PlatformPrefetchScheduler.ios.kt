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

import androidx.compose.ui.platform.PlatformPrefetchRequest
import androidx.compose.ui.platform.PlatformPrefetchRequestScope
import androidx.compose.ui.platform.PlatformPrefetchScheduler
import androidx.compose.ui.uikit.toNanoSeconds
import androidx.compose.ui.util.trace
import androidx.compose.ui.util.traceValue
import platform.Foundation.NSThread
import platform.Foundation.NSTimeInterval
import platform.QuartzCore.CACurrentMediaTime

internal class PlatformPrefetchSchedulerImpl(
    private val currentTime: () -> NSTimeInterval = { CACurrentMediaTime() },
    private var onHasWorkScheduled: (Boolean) -> Unit,
) : PlatformPrefetchScheduler {
    private val scheduledPrefetchRequests = ScheduledPrefetchRequests()
    private val scope = PrefetchRequestScopeImpl()
    private val hasWorkScheduled: Boolean get() = scheduledPrefetchRequests.hasWorkScheduled

    /**
     * Marks the start of the display-link interval where drawing happened.
     */
    private var lastDrawTimestamp: NSTimeInterval = currentTime()

    /**
     * Timestamp after which the draw loop is considered idle enough for prefetch work to ignore the
     * normal per-frame deadline.
     *
     * This threshold is frozen after the first no-draw callback following a draw, so later refresh-rate
     * changes do not move the meaning of "two intervals after the draw".
     */
    private var drawIdleThresholdTimestamp: NSTimeInterval = lastDrawTimestamp

    /**
     * Display-link interval from the callback that invoked `draw()`, used to calculate
     * [drawIdleThresholdTimestamp] together with the first following no-draw interval.
     *
     * It starts as `0.0` so the first observed no-draw interval is used as a fallback before the
     * first real draw interval is known.
     */
    private var drawFrameIntervalForIdleThreshold: NSTimeInterval = 0.0

    /**
     * Whether [drawIdleThresholdTimestamp] still needs the first valid no-draw interval after a draw.
     */
    private var isDrawIdleThresholdPending: Boolean = true
    private var isDisposed = false

    override fun scheduleHighPriorityPrefetch(request: PlatformPrefetchRequest) {
        check(NSThread.isMainThread) {
            "PlatformPrefetchSchedulerImpl.scheduleHighPriorityPrefetch() must be called on main thread"
        }

        if (isDisposed) {
            return
        }

        scheduledPrefetchRequests.addHighPriority(request)
        onHasWorkScheduled(hasWorkScheduled)
    }

    override fun scheduleLowPriorityPrefetch(request: PlatformPrefetchRequest) {
        check(NSThread.isMainThread) {
            "PlatformPrefetchSchedulerImpl.scheduleLowPriorityPrefetch() must be called on main thread"
        }

        if (isDisposed) {
            return
        }

        scheduledPrefetchRequests.addLowPriority(request)
        onHasWorkScheduled(hasWorkScheduled)
    }

    /**
     * Executes scheduler prefetch requests during a display-link callback.
     *
     * @param lastFrameTimestamp Timestamp of the last displayed frame. Used as the start of the
     * current display-link interval.
     * @param targetTimestamp Deadline for prefetch work that runs before the next frame.
     * @param didDraw `true` when `draw()` was invoked during this display-link callback.
     */
    fun execute(
        lastFrameTimestamp: NSTimeInterval,
        targetTimestamp: NSTimeInterval,
        didDraw: Boolean,
    ) {
        check(NSThread.isMainThread) {
            "PlatformPrefetchSchedulerImpl.execute() must be called on main thread"
        }
        if (isDisposed) {
            onHasWorkScheduled(false)
            return
        }

        val frameInterval = targetTimestamp - lastFrameTimestamp

        if (didDraw) {
            recordDraw(lastFrameTimestamp, frameInterval)
        } else {
            updateDrawIdleThresholdIfNeeded(frameInterval)
        }

        if (!hasWorkScheduled) {
            onHasWorkScheduled(false)
            return
        }

        val isPastDrawIdleThreshold = !isDrawIdleThresholdPending && currentTime() > drawIdleThresholdTimestamp
        scope.isDrawIdle = !didDraw && isPastDrawIdleThreshold
        scope.nextFrameTimestamp = targetTimestamp

        var continueInNextFrame = false
        while (hasWorkScheduled && !continueInNextFrame) {
            continueInNextFrame =
                if (scope.isDrawIdle) {
                    trace("compose:lazy:prefetch:idle_frame") { executeRequest() }
                } else {
                    executeRequest()
                }
        }

        onHasWorkScheduled(hasWorkScheduled)
        traceValue("compose:lazy:prefetch:available_time_nanos", 0L)
    }

    private fun recordDraw(
        lastFrameTimestamp: NSTimeInterval,
        frameInterval: NSTimeInterval,
    ) {
        lastDrawTimestamp = lastFrameTimestamp
        drawFrameIntervalForIdleThreshold = maxOf(0.0, frameInterval)
        isDrawIdleThresholdPending = true
    }

    private fun updateDrawIdleThresholdIfNeeded(frameInterval: NSTimeInterval) {
        if (!isDrawIdleThresholdPending || frameInterval <= 0.0) {
            return
        }

        val drawFrameInterval = drawFrameIntervalForIdleThreshold.takeIf { it > 0.0 } ?: frameInterval
        drawIdleThresholdTimestamp = lastDrawTimestamp + drawFrameInterval + frameInterval
        isDrawIdleThresholdPending = false
    }

    fun dispose() {
        check(NSThread.isMainThread) {
            "PlatformPrefetchSchedulerImpl.dispose() must be called on main thread"
        }
        isDisposed = true
        scheduledPrefetchRequests.clear()
        onHasWorkScheduled(false)
        onHasWorkScheduled = {}
    }

    private fun executeRequest(): Boolean {
        val availableTimeNanos = scope.availableTimeNanos()
        traceValue("compose:lazy:prefetch:available_time_nanos", availableTimeNanos)

        return if (availableTimeNanos > 0) {
            val hasMoreWorkToDo = with(scheduledPrefetchRequests) { scope.executeNext() }
            scope.isDrawIdle = false
            hasMoreWorkToDo
        } else {
            true
        }
    }

    private inner class PrefetchRequestScopeImpl : PlatformPrefetchRequestScope {
        var isDrawIdle: Boolean = false
        var nextFrameTimestamp: NSTimeInterval = 0.0

        override fun availableTimeNanos(): Long =
            if (isDrawIdle) {
                Long.MAX_VALUE
            } else {
                val availableTime = nextFrameTimestamp - currentTime()
                maxOf(0.0, availableTime).toNanoSeconds()
            }
    }
}

private class ScheduledPrefetchRequests {
    private val highPriorityPrefetchRequests = ArrayDeque<PlatformPrefetchRequest>()
    private val lowPriorityPrefetchRequests = ArrayDeque<PlatformPrefetchRequest>()

    val hasWorkScheduled: Boolean
        get() =
            highPriorityPrefetchRequests.isNotEmpty() ||
                lowPriorityPrefetchRequests.isNotEmpty()

    fun addHighPriority(request: PlatformPrefetchRequest) {
        highPriorityPrefetchRequests.addLast(request)
    }

    fun addLowPriority(request: PlatformPrefetchRequest) {
        lowPriorityPrefetchRequests.addLast(request)
    }

    fun clear() {
        highPriorityPrefetchRequests.clear()
        lowPriorityPrefetchRequests.clear()
    }

    fun PlatformPrefetchRequestScope.executeNext(): Boolean {
        val requestQueue = when {
            highPriorityPrefetchRequests.isNotEmpty() -> highPriorityPrefetchRequests
            lowPriorityPrefetchRequests.isNotEmpty() -> lowPriorityPrefetchRequests
            else -> return false
        }
        val hasMoreWorkToDo = with(requestQueue.first()) {
            execute()
        }
        if (!hasMoreWorkToDo) {
            requestQueue.removeFirst()
        }
        return hasMoreWorkToDo
    }
}