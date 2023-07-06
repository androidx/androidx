/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.services.client.data

import androidx.health.services.client.data.DataType.Companion.ABSOLUTE_ELEVATION_STATS
import androidx.health.services.client.data.DataType.Companion.CALORIES_TOTAL
import androidx.health.services.client.data.DataType.Companion.FLOORS
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM
import androidx.health.services.client.data.DataType.Companion.STEPS
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataPointContainerTest {

    private fun Int.duration() = Duration.ofSeconds(this.toLong())
    private fun Int.instant() = Instant.ofEpochMilli(this.toLong())

    private val elevationStatsDataPoint = DataPoints.absoluteElevationStats(
        minAbsoluteElevationMeters = 10.0,
        maxAbsoluteElevationMeters = 20.0,
        averageAbsoluteElevationMeters = 15.0,
        startTime = 10.instant(),
        endTime = 20.instant()
    )

    @Test
    fun getDataReturnsCorrectNumberSimpleInput() {
        val step1 = DataPoints.steps(5, 1.duration(), 2.duration())
        val step2 = DataPoints.steps(10, 2.duration(), 10.duration())
        val container = DataPointContainer(listOf(step1, step2))

        val list: List<IntervalDataPoint<Long>> = container.getData(STEPS)

        assertThat(list).hasSize(2)
        assertThat(list).containsExactly(step1, step2)
    }

    @Test
    fun getDataReturnsCorrectNumberMultipleDataTypes() {
        val container = DataPointContainer(
            listOf(
                DataPoints.steps(5, 1.duration(), 2.duration()),
                DataPoints.heartRate(130.0, 1.duration()),
                DataPoints.floors(5.0, 1.duration(), 10.duration()),
                DataPoints.caloriesTotal(130.0, 10.instant(), 20.instant()),
                elevationStatsDataPoint,
                DataPoints.steps(10, 2.duration(), 10.duration()),
            )
        )

        val steps: List<IntervalDataPoint<Long>> = container.getData(STEPS)
        val floors: List<IntervalDataPoint<Double>> = container.getData(FLOORS)
        val elevationStats: StatisticalDataPoint<Double>? =
            container.getData(ABSOLUTE_ELEVATION_STATS)
        val caloriesTotal: CumulativeDataPoint<Double>? =
            container.getData(CALORIES_TOTAL)

        assertThat(steps).hasSize(2)
        assertThat(floors).hasSize(1)
        assertThat(elevationStats).isNotNull()
        assertThat(caloriesTotal).isNotNull()
    }

    @Test
    fun customDataType() {
        val customDataType1: DeltaDataType<ByteArray, IntervalDataPoint<ByteArray>> =
            DeltaDataType(
                "health_services.device_private.65537",
                DataType.TimeType.INTERVAL,
                ByteArray::class.java
            )
        val customDataType2: DeltaDataType<ByteArray, IntervalDataPoint<ByteArray>> =
            DeltaDataType(
                "health_services.device_private.65537",
                DataType.TimeType.INTERVAL,
                ByteArray::class.java
            )
        val byteArray = ByteArray(1)
        byteArray[0] = 0x42

        val container = DataPointContainer(
            listOf(
                DataPoints.steps(5, 1.duration(), 2.duration()),
                IntervalDataPoint<ByteArray>(
                    dataType = customDataType1,
                    value = byteArray,
                    startDurationFromBoot = 2.duration(),
                    endDurationFromBoot = 10.duration(),
                )
            )
        )

        assertThat(container.getData(customDataType2).first().value[0]).isEqualTo(0x42)
    }

    @Test
    fun getSampleDataPointsReturnsTheCorrectNumber() {
        val container = DataPointContainer(
            listOf(
                DataPoints.steps(5, 1.duration(), 2.duration()),
                DataPoints.heartRate(130.0, 1.duration()),
                DataPoints.floors(5.0, 1.duration(), 10.duration()),
                DataPoints.caloriesTotal(130.0, 10.instant(), 20.instant()),
                elevationStatsDataPoint,
                DataPoints.steps(10, 2.duration(), 10.duration()),
            )
        )

        val dataPoints = container.sampleDataPoints

        assertThat(dataPoints).hasSize(1)
    }

    @Test
    fun getIntervalDataPointsReturnsTheCorrectNumber() {
        val container = DataPointContainer(
            listOf(
                DataPoints.steps(5, 1.duration(), 2.duration()),
                DataPoints.heartRate(130.0, 1.duration()),
                DataPoints.floors(5.0, 1.duration(), 10.duration()),
                DataPoints.caloriesTotal(130.0, 10.instant(), 20.instant()),
                elevationStatsDataPoint,
                DataPoints.steps(10, 2.duration(), 10.duration()),
            )
        )

        val dataPoints = container.intervalDataPoints

        assertThat(dataPoints).hasSize(3)
    }

    @Test
    fun getCumulativeDataPointsReturnsTheCorrectNumber() {
        val container = DataPointContainer(
            listOf(
                DataPoints.steps(5, 1.duration(), 2.duration()),
                DataPoints.heartRate(130.0, 1.duration()),
                DataPoints.floors(5.0, 1.duration(), 10.duration()),
                DataPoints.caloriesTotal(130.0, 10.instant(), 20.instant()),
                elevationStatsDataPoint,
                DataPoints.steps(10, 2.duration(), 10.duration()),
            )
        )

        val dataPoints = container.cumulativeDataPoints

        assertThat(dataPoints).hasSize(1)
    }

    @Test
    fun getStatisticalDataPointsReturnsTheCorrectNumber() {
        val container = DataPointContainer(
            listOf(
                DataPoints.steps(5, 1.duration(), 2.duration()),
                DataPoints.heartRate(130.0, 1.duration()),
                DataPoints.floors(5.0, 1.duration(), 10.duration()),
                DataPoints.caloriesTotal(130.0, 10.instant(), 20.instant()),
                elevationStatsDataPoint,
                DataPoints.steps(10, 2.duration(), 10.duration()),
            )
        )

        val dataPoints = container.statisticalDataPoints

        assertThat(dataPoints).hasSize(1)
    }

    @Test
    fun dataTypePropertyContainsCorrectDataTypes() {
        val container = DataPointContainer(
            listOf(
                DataPoints.steps(5, 1.duration(), 2.duration()),
                DataPoints.heartRate(130.0, 1.duration()),
                DataPoints.floors(5.0, 1.duration(), 10.duration()),
                DataPoints.caloriesTotal(130.0, 10.instant(), 20.instant()),
                elevationStatsDataPoint,
                DataPoints.steps(10, 2.duration(), 10.duration()),
            )
        )

        assertThat(container.dataTypes).containsExactly(
            STEPS,
            HEART_RATE_BPM,
            FLOORS,
            CALORIES_TOTAL,
            ABSOLUTE_ELEVATION_STATS
        )
    }
}
