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

package androidx.camera.integration.camera2.pipe.dataholders

import java.util.LinkedList
import java.util.concurrent.ConcurrentSkipListSet

/** Defines operations on graph data */
class GraphDataSortedRingBuffer {
    /**
     * Keeps track of data points in order of their frame number (same as in order of timestamp).
     * Allows O(log n) insertion complexity with an ordered sequence of elements. This allows O(1)
     * removal of the earliest point and fast search for finding window of points)
     */
    private var dataPoints: ConcurrentSkipListSet<GraphDataPoint> = ConcurrentSkipListSet()

    fun size() = dataPoints.size

    fun toList(): List<GraphDataPoint> = dataPoints.toList()

    /** Adds data point to list while maintaining sorted order and staying under capacity */
    fun addPoint(dataPoint: GraphDataPoint) {
        /** Since we can't store infinite data points, when at capacity earliest point is deleted */
        if (dataPoints.size == CAPACITY) dataPoints.pollFirst()
        dataPoints.add(dataPoint)
    }

    /** Fetches data points in time window with the given end and of the given length */
    fun getPointsInTimeWindow(
        timeWindowLengthNanos: Long,
        timeWindowEndNanos: Long
    ): List<GraphDataPoint> {
        if (timeWindowLengthNanos <= 0)
            throw IllegalArgumentException("Time window's length must be greater than 0")

        if (dataPoints.isEmpty()) return listOf()

        if (timeWindowEndNanos <= dataPoints.first().timestampNanos)
            throw IllegalArgumentException(
                "Time window's end must be after the first point's " +
                    "timestamp"
            )

        val timeWindowStartNanos = timeWindowEndNanos - timeWindowLengthNanos

        val dataPointsInTimeWindow: LinkedList<GraphDataPoint> = LinkedList()
        dataPoints.descendingSet().forEach {
            val timestamp = it.timestampNanos
            if (timestamp < timeWindowStartNanos) return@forEach
            if (timestamp <= timeWindowEndNanos) dataPointsInTimeWindow.addFirst(it)
        }

        return dataPointsInTimeWindow
    }

    companion object {
        /** 2000 is roughly the number of points added after 1 min if points are added at 30 FPS. */
        const val CAPACITY = 2000
    }
}
