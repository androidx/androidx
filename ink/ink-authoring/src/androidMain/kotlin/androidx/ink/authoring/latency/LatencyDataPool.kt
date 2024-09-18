/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.latency

import android.os.Build
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.UiThread
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.InProgressStrokeId
import kotlin.collections.ArrayDeque
import kotlin.collections.MutableCollection

/**
 * A pool of preallocated [LatencyData]s to be recycled.
 *
 * The purpose of this class is to preallocate all [LatencyData] instances that the caller will ever
 * need, in order to avoid new allocations during latency-sensitive interactive use of the client
 * app.
 */
@ExperimentalLatencyDataApi
@UiThread
internal class LatencyDataPool(numPreAllocatedInstances: Int = 100) {
    private val pool = ArrayDeque<LatencyData>(numPreAllocatedInstances)

    init {
        repeat(numPreAllocatedInstances) { recycle(LatencyData()) }
    }

    /**
     * Gets a [LatencyData] from the pool, or a newly allocated one if the pool is empty.
     *
     * Allocations in response to `obtain()` are undesirable since they may trigger garbage
     * collection, causing latency or jank.
     */
    fun obtain(): LatencyData {
        return pool.removeFirstOrNull()
            ?: LatencyData().also {
                Log.w(
                    this::class.simpleName,
                    "Pool is empty; allocating a LatencyData. You should have preallocated more instances.",
                )
            }
    }

    /** Puts a [LatencyData] into the pool for later reuse. */
    fun recycle(latencyData: LatencyData) {
        latencyData.reset()
        pool.addLast(latencyData)
    }

    /**
     * [obtain]s a [LatencyData] and sets its [LatencyData.EventAction] and
     * [LatencyData.osDetectsEvent] fields from the given event. Ignores "historical" events that
     * might be batched with the primary (most recent) event.
     */
    fun obtainLatencyDataForSingleEvent(
        event: MotionEvent?,
        inProgressStrokeAction: LatencyData.StrokeAction,
        inProgressStrokeId: InProgressStrokeId?,
        strokesViewGetsActionTimeNanos: Long,
        predicted: Boolean = false,
    ): LatencyData {
        return obtain().apply {
            if (inProgressStrokeId != null) strokeId = inProgressStrokeId
            strokeAction = inProgressStrokeAction
            if (event != null) {
                eventAction = LatencyData.EventAction.fromMotionEvent(event, predicted)
                batchSize = event.historySize + 1
                batchIndex = batchSize - 1
                osDetectsEvent = event.getPrimaryEventTimeNanos()
            }
            strokesViewGetsAction = strokesViewGetsActionTimeNanos
        }
    }

    /**
     * [obtain]s a [LatencyData] for the primary (most recent) event in a [MotionEvent] and also for
     * each historical event that was batched in with it. (See
     * [MotionEvent.getHistoricalEventTimeNanos].) Also initializes the [LatencyData.EventAction]
     * and [LatencyData.osDetectsEvent] fields in each such [LatencyData].
     *
     * @param event the [MotionEvent] from which to pull primary and historical event times.
     * @param predicted whether this event came from a prediction engine.
     * @param datas output argument: a queue to be populated with the [LatencyData]s produced.
     */
    fun obtainLatencyDataForPrimaryAndHistoricalEvents(
        event: MotionEvent,
        inProgressStrokeAction: LatencyData.StrokeAction,
        inProgressStrokeId: InProgressStrokeId,
        strokesViewGetsActionTimeNanos: Long,
        predicted: Boolean,
        datas: MutableCollection<LatencyData>,
    ) {
        datas.clear()
        val action = LatencyData.EventAction.fromMotionEvent(event, predicted)
        val historySize = event.historySize
        // Note that this loop is inclusive of historySize, which signals the primary event.
        for (historyIndex in 0..historySize) {
            datas.add(
                obtain().apply {
                    strokeId = inProgressStrokeId
                    strokeAction = inProgressStrokeAction
                    eventAction = action
                    batchSize = historySize + 1
                    batchIndex = historyIndex
                    osDetectsEvent = event.getMaybeHistoricalEventTimeNanos(historyIndex)
                    strokesViewGetsAction = strokesViewGetsActionTimeNanos
                }
            )
        }
    }

    /**
     * Gets the time in nanoseconds of an event batched into this [MotionEvent]. If [historyIndex]
     * is less than [MotionEvent.getHistorySize], it refers to a "historical" point that was
     * previously unreported but was batched into this event. If it is equal, then it refers to the
     * primary (most recent) event.
     *
     * See [MotionEvent.getHistoricalEventTimeNanos].
     */
    private fun MotionEvent.getMaybeHistoricalEventTimeNanos(historyIndex: Int): Long {
        return if (historyIndex == historySize) {
            getPrimaryEventTimeNanos()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                getHistoricalEventTimeNanos(historyIndex)
            } else {
                getHistoricalEventTime(historyIndex) * 1_000_000L
            }
        }
    }

    private fun MotionEvent.getPrimaryEventTimeNanos(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            eventTimeNanos
        } else {
            eventTime * 1_000_000L
        }
    }
}
