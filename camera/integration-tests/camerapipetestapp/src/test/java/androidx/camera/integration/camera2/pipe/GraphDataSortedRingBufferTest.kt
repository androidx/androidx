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

package androidx.camera.integration.camera2.pipe

import androidx.camera.integration.camera2.pipe.dataholders.GraphDataPoint
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataSortedRingBuffer
import androidx.camera.integration.camera2.pipe.dataholders.GraphDataSortedRingBuffer.Companion.CAPACITY
import com.google.common.truth.Truth
import org.junit.Test

class GraphDataSortedRingBufferTest {
    @Test(expected = IllegalArgumentException::class)
    fun getPoints_timeWindowLengthZero() {
        val graphData =
            GraphDataSortedRingBuffer()

        graphData.getPointsInTimeWindow(0, 90)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getPoints_windowEndBeforeFirstPoint() {

        val graphData =
            GraphDataSortedRingBuffer()
        graphData.addPoint(
            GraphDataPoint(
                1,
                9,
                0,
                2
            )
        )
        graphData.addPoint(
            GraphDataPoint(
                2,
                13,
                0,
                4
            )
        )

        graphData.getPointsInTimeWindow(2, 5)
    }

    @Test
    fun getPoints_emptyList() {
        val graphData =
            GraphDataSortedRingBuffer()

        Truth.assertThat(
            graphData
                .getPointsInTimeWindow(10, 90)
                .size
        ).isEqualTo(0)
    }

    @Test
    fun getPoints_windowStartBeforeFirstPoint() {

        val graphData =
            GraphDataSortedRingBuffer()
        graphData.addPoint(
            GraphDataPoint(
                1,
                1,
                0,
                2
            )
        )
        graphData.addPoint(
            GraphDataPoint(
                2,
                13,
                0,
                4
            )
        )

        Truth.assertThat(
            graphData
                .getPointsInTimeWindow(66, 13)
                .size
        ).isEqualTo(2)
    }

    @Test
    fun getPoints_windowStartAtFirstPoint() {

        val graphData =
            GraphDataSortedRingBuffer()
        graphData.addPoint(
            GraphDataPoint(
                1,
                1,
                0,
                2
            )
        )
        graphData.addPoint(
            GraphDataPoint(
                2,
                3,
                0,
                4
            )
        )
        graphData.addPoint(
            GraphDataPoint(
                3,
                5,
                0,
                6
            )
        )
        graphData.addPoint(
            GraphDataPoint(
                4,
                7,
                0,
                8
            )
        )

        Truth.assertThat(
            graphData
                .getPointsInTimeWindow(9, 10)
                .size
        ).isEqualTo(4)
    }

    @Test
    fun getPoints_windowStartInMiddle() {

        val graphData =
            GraphDataSortedRingBuffer()
        graphData.addPoint(
            GraphDataPoint(
                1,
                1,
                0,
                2
            )
        )
        graphData.addPoint(
            GraphDataPoint(
                2,
                13,
                0,
                4
            )
        )
        graphData.addPoint(
            GraphDataPoint(
                3,
                25,
                0,
                6
            )
        )
        graphData.addPoint(
            GraphDataPoint(
                4,
                77,
                0,
                8
            )
        )

        Truth.assertThat(
            graphData
                .getPointsInTimeWindow(66, 90)
                .size
        ).isEqualTo(2)
    }

    @Test
    fun getPoints_windowStartAtLastPoint() {
        val graphData =
            GraphDataSortedRingBuffer()
        graphData.addPoint(
            GraphDataPoint(
                1,
                1,
                0,
                2
            )
        )
        graphData.addPoint(
            GraphDataPoint(
                2,
                13,
                0,
                4
            )
        )

        Truth.assertThat(
            graphData
                .getPointsInTimeWindow(31, 44)
                .size
        ).isEqualTo(1)
    }

    @Test
    fun getPoints_windowStartAfterLastPoint() {
        val graphData =
            GraphDataSortedRingBuffer()
        graphData.addPoint(
            GraphDataPoint(
                1,
                1,
                0,
                2
            )
        )
        graphData.addPoint(
            GraphDataPoint(
                2,
                13,
                0,
                4
            )
        )

        Truth.assertThat(
            graphData
                .getPointsInTimeWindow(2, 44)
                .size
        ).isEqualTo(0)
    }

    @Test
    fun add_toEmptyList() {

        val graphData =
            GraphDataSortedRingBuffer()
        graphData.addPoint(
            GraphDataPoint(
                0,
                1,
                0,
                2
            )
        )

        Truth.assertThat(
            graphData.size()
        ).isEqualTo(1)
    }

    @Test
    fun add_toAtCapacityList() {
        var graphData =
            GraphDataSortedRingBuffer()
        graphData.addPoint(
            GraphDataPoint(
                1,
                0,
                0,
                0
            )
        )
        (2..CAPACITY).forEach {
            val point =
                GraphDataPoint(
                    it.toLong(),
                    1,
                    0,
                    2
                )
            graphData.addPoint(point)
        }
        Truth.assertThat(graphData.size()).isEqualTo(CAPACITY)

        graphData.addPoint(
            GraphDataPoint(
                (CAPACITY + 1).toLong(),
                10,
                10,
                10
            )
        )
        Truth.assertThat(graphData.size()).isEqualTo(2000)
        Truth.assertThat(graphData.toList().first().frameNumber).isEqualTo(2)
        Truth.assertThat(graphData.toList().last().frameNumber).isEqualTo(2001)
    }

    @Test
    fun add_outOfOrder() {
        var graphData =
            GraphDataSortedRingBuffer()

        val p1 =
            GraphDataPoint(
                1,
                48,
                0,
                1
            )
        val p2 =
            GraphDataPoint(
                2,
                49,
                0,
                2
            )
        val p3 =
            GraphDataPoint(
                3,
                50,
                0,
                2
            )
        val p4 =
            GraphDataPoint(
                4,
                55,
                0,
                2
            )
        val p5 =
            GraphDataPoint(
                5,
                60,
                0,
                5
            )

        graphData.addPoint(p5)
        graphData.addPoint(p1)
        graphData.addPoint(p4)
        graphData.addPoint(p3)
        graphData.addPoint(p2)

        Truth.assertThat(graphData.toList()).isEqualTo(listOf(p1, p2, p3, p4, p5))
    }

    @Test
    fun addOutOfOrderAndGetPoints_slidingTimeWindow() {
        var graphData =
            GraphDataSortedRingBuffer()
        val p1 =
            GraphDataPoint(
                1,
                43,
                0,
                2
            )
        val p2 =
            GraphDataPoint(
                2,
                44,
                0,
                2
            )
        val p3 =
            GraphDataPoint(
                3,
                45,
                0,
                1
            )
        val p4 =
            GraphDataPoint(
                4,
                47,
                0,
                2
            )
        val p5 =
            GraphDataPoint(
                5,
                48,
                0,
                5
            )

        val timeWindowlengthNanos = 10L
        graphData.addPoint(p5)
        graphData.addPoint(p3)

        var pointList = graphData.getPointsInTimeWindow(timeWindowlengthNanos, 50)
        Truth.assertThat(pointList).isEqualTo(listOf(p3, p5))
        Truth.assertThat(pointList.size).isEqualTo(2)

        graphData.addPoint(p2)
        graphData.addPoint(p1)
        graphData.addPoint(p4)

        pointList = graphData.getPointsInTimeWindow(timeWindowlengthNanos, 50)
        Truth.assertThat(pointList).isEqualTo(listOf(p1, p2, p3, p4, p5))
        Truth.assertThat(pointList.size).isEqualTo(5)
    }
}
