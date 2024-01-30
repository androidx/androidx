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

/** Represents single point to be graphed - timestampNanos is used for x, value is used for y */
data class GraphDataPoint(
    val frameNumber: Long,

    /** The time this data was recorded according to the CaptureResult it came with */
    val timestampNanos: Long,

    /** The time this data was actually received - used to graph latency */
    val timeArrivedNanos: Long,

    /**
     * TODO("Make custom class to store value to eliminate the need for Number ext. functions")
     */
    val value: Number
) : Comparable<GraphDataPoint> {

    override fun compareTo(other: GraphDataPoint): Int =
        if (frameNumber != other.frameNumber) frameNumber.compareTo(other.frameNumber)
        else timestampNanos.compareTo(timestampNanos)
}
