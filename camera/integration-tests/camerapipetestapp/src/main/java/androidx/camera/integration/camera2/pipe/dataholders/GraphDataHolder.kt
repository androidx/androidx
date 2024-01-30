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

/**
 * Data source for 1D graph visualizations. Implemented for both graphing state over time and
 * graphing value over time
 */
interface GraphDataHolder {

    /**
     * Keeps track of a set number of data points in order. When capacity is reached, the oldest
     * data point is dropped when a new data point is added.
     */
    var graphData: GraphDataSortedRingBuffer

    /** Adds data point to list while maintaining sorted order and staying under capacity */
    fun addPoint(dataPoint: GraphDataPoint) = graphData.addPoint(dataPoint)

    /** Fetches data points in time window with the given end and of the given length */
    fun getPointsInTimeWindow(timeWindowLengthNanos: Long, timeWindowEndNanos: Long):
        List<GraphDataPoint> = graphData.getPointsInTimeWindow(
            timeWindowLengthNanos,
            timeWindowEndNanos
        )
}
