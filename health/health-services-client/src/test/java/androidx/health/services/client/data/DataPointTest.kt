/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import android.os.Bundle
import com.google.common.truth.Truth
import java.time.Duration
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DataPointTest {
    fun Int.duration() = Duration.ofSeconds(toLong())
    fun Int.instant() = Instant.ofEpochMilli(toLong())

    @Test
    fun intervalDataPointProtoRoundTrip() {
        val proto = IntervalDataPoint(
            DataType.CALORIES,
            value = 130.0,
            startDurationFromBoot = 10.duration(),
            endDurationFromBoot = 20.duration(),
            Bundle().apply {
                putInt("int", 5)
                putString("string", "value")
            },
            accuracy = null // No interval DataPoints have an accuracy component
        ).proto

        val dataPoint = DataPoint.fromProto(proto) as IntervalDataPoint

        Truth.assertThat(dataPoint.dataType).isEqualTo(DataType.CALORIES)
        Truth.assertThat(dataPoint.value).isEqualTo(130.0)
        Truth.assertThat(dataPoint.startDurationFromBoot).isEqualTo(10.duration())
        Truth.assertThat(dataPoint.endDurationFromBoot).isEqualTo(20.duration())
        Truth.assertThat(dataPoint.metadata.getInt("int")).isEqualTo(5)
        Truth.assertThat(dataPoint.metadata.getString("string")).isEqualTo("value")
        Truth.assertThat(dataPoint.accuracy).isNull()
    }

    @Test
    fun sampleDataPointProtoRoundTrip() {
        val proto = SampleDataPoint(
            DataType.HEART_RATE_BPM,
            130.0,
            20.duration(),
            Bundle().apply {
                putInt("int", 5)
                putString("string", "value")
            },
            HeartRateAccuracy(HeartRateAccuracy.SensorStatus.ACCURACY_HIGH)
        ).proto

        val dataPoint = DataPoint.fromProto(proto) as SampleDataPoint

        Truth.assertThat(dataPoint.dataType).isEqualTo(DataType.HEART_RATE_BPM)
        Truth.assertThat(dataPoint.value).isEqualTo(130.0)
        Truth.assertThat(dataPoint.timeDurationFromBoot).isEqualTo(20.duration())
        Truth.assertThat(dataPoint.metadata.getInt("int")).isEqualTo(5)
        Truth.assertThat(dataPoint.metadata.getString("string")).isEqualTo("value")
        Truth.assertThat((dataPoint.accuracy as HeartRateAccuracy).sensorStatus)
            .isEqualTo(HeartRateAccuracy.SensorStatus.ACCURACY_HIGH)
    }

    @Test
    fun cumulativeDataPointProtoRoundTrip() {
        val proto = CumulativeDataPoint(
            dataType = DataType.CALORIES_TOTAL,
            total = 100.0,
            start = 10.instant(),
            end = 99.instant(),
        ).proto

        val dataPoint = DataPoint.fromProto(proto) as CumulativeDataPoint

        Truth.assertThat(dataPoint.dataType).isEqualTo(DataType.CALORIES_TOTAL)
        Truth.assertThat(dataPoint.total).isEqualTo(100.0)
        Truth.assertThat(dataPoint.start).isEqualTo(10.instant())
        Truth.assertThat(dataPoint.end).isEqualTo(99.instant())
    }

    @Test
    fun statisticalDataPointProtoRoundTrip() {
        val proto = StatisticalDataPoint(
            dataType = DataType.HEART_RATE_BPM_STATS,
            min = 100.0,
            max = 175.5,
            average = 155.0,
            start = 10.instant(),
            end = 99.instant(),
        ).proto

        val dataPoint = DataPoint.fromProto(proto) as StatisticalDataPoint

        Truth.assertThat(dataPoint.dataType).isEqualTo(DataType.HEART_RATE_BPM_STATS)
        Truth.assertThat(dataPoint.min).isEqualTo(100.0)
        Truth.assertThat(dataPoint.max).isEqualTo(175.5)
        Truth.assertThat(dataPoint.average).isEqualTo(155.0)
        Truth.assertThat(dataPoint.start).isEqualTo(10.instant())
        Truth.assertThat(dataPoint.end).isEqualTo(99.instant())
    }

    @Test
    fun rangeValidationWithSteps_success() {
        val steps = DataPoints.steps(10, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(steps).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidSteps_throwsNoException() {
        val negativeOutOfRangeSteps =
            DataPoints.steps(-1, getStartDurationFromBoot(), getEndDurationFromBoot())
        val positiveOutOfRangeSteps =
            DataPoints.steps(1000001, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(negativeOutOfRangeSteps).isNotNull()
        Truth.assertThat(positiveOutOfRangeSteps).isNotNull()
    }

    @Test
    fun rangeValidationWithStepsPerMinute_success() {
        val stepsPerMinute = DataPoints.stepsPerMinute(10, getStartDurationFromBoot())

        Truth.assertThat(stepsPerMinute).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidStepsPerMinute_throwsNoException() {
        val negativeOutOfRangeStepsPerMinutes =
            DataPoints.stepsPerMinute(-1, getStartDurationFromBoot())
        val positiveOutOfRangeStepsPerMinutes =
            DataPoints.stepsPerMinute(1000001, getEndDurationFromBoot())

        Truth.assertThat(negativeOutOfRangeStepsPerMinutes).isNotNull()
        Truth.assertThat(positiveOutOfRangeStepsPerMinutes).isNotNull()
    }

    @Test
    fun rangeValidationWithDistance_success() {
        val distance =
            DataPoints.distance(12.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(distance).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidDistance_throwsNoException() {
        val negativeOutOfRangeDistance =
            DataPoints.distance(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        val positiveOutOfRangeDistance =
            DataPoints.distance(1000000.1, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(negativeOutOfRangeDistance).isNotNull()
        Truth.assertThat(positiveOutOfRangeDistance).isNotNull()
    }

    @Test
    fun rangeValidationWithDistanceTotal_success() {
        val distanceTotal =
            DataPoints.distanceTotal(12.0, getStartInstant(), getEndInstant())

        Truth.assertThat(distanceTotal).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidDistanceTotal_throwsNoException() {
        val negativeOutOfRangeDistanceTotal =
            DataPoints.distanceTotal(-1.0, getStartInstant(), getEndInstant())
        val positiveOutOfRangeDistanceTotal =
            DataPoints.distanceTotal(1000000.1, getStartInstant(), getEndInstant())

        Truth.assertThat(negativeOutOfRangeDistanceTotal).isNotNull()
        Truth.assertThat(positiveOutOfRangeDistanceTotal).isNotNull()
    }

    @Test
    fun rangeValidationWithElevationGain_success() {
        val elevationGain =
            DataPoints.elevationGain(12.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(elevationGain).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidElevationGain_throwsNoException() {
        val negativeOutOfRangeElevationGain =
            DataPoints.elevationGain(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        val positiveOutOfRangeElevationGain = DataPoints.elevationGain(
            1000000.1,
            getStartDurationFromBoot(),
            getEndDurationFromBoot()
        )

        Truth.assertThat(negativeOutOfRangeElevationGain).isNotNull()
        Truth.assertThat(positiveOutOfRangeElevationGain).isNotNull()
    }

    @Test
    fun rangeValidationWithElevationLoss_success() {
        val elevationLoss =
            DataPoints.elevationLoss(12.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(elevationLoss).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidElevationLoss_throwsNoException() {
        val negativeOutOfRangeElevationLoss =
            DataPoints.elevationLoss(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        val positiveOutOfRangeElevationLoss = DataPoints.elevationLoss(
            1000000.1,
            getStartDurationFromBoot(),
            getEndDurationFromBoot()
        )

        Truth.assertThat(negativeOutOfRangeElevationLoss).isNotNull()
        Truth.assertThat(positiveOutOfRangeElevationLoss).isNotNull()
    }

    @Test
    fun rangeValidationWithAbsoluteElevation_success() {
        val absoluteElevation =
            DataPoints.absoluteElevation(12.0, getStartDurationFromBoot())
        val nagativeAbsoluteElevation =
            DataPoints.absoluteElevation(-12.0, getStartDurationFromBoot())

        Truth.assertThat(absoluteElevation).isNotNull()
        Truth.assertThat(nagativeAbsoluteElevation).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidAbsoluteElevation_throwsNoException() {
        val negativeOutOfRangeAbsoluteElevation =
            DataPoints.absoluteElevation(-1000000.1, getStartDurationFromBoot())
        val positiveOutOfRangeAbsoluteElevation =
            DataPoints.absoluteElevation(1000000.1, getStartDurationFromBoot())

        Truth.assertThat(negativeOutOfRangeAbsoluteElevation).isNotNull()
        Truth.assertThat(positiveOutOfRangeAbsoluteElevation).isNotNull()
    }

    @Test
    fun rangeValidationWithAbsoluteElevationStats_success() {
        val absoluteElevationStats =
            DataPoints.absoluteElevationStats(
                12.0,
                240.0,
                120.0,
                getStartInstant(),
                getEndInstant()
            )

        Truth.assertThat(absoluteElevationStats).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidAbsoluteElevationStats_throwsNoException() {
        val negativeOutOfRangeMinAbsoluteElevationStats = DataPoints.absoluteElevationStats(
            -1000000.1,
            240.0,
            120.0,
            getStartInstant(),
            getEndInstant()
        )
        val negativeOutOfRangeMaxAbsoluteElevationStats = DataPoints.absoluteElevationStats(
            12.0,
            -1000000.1,
            120.0,
            getStartInstant(),
            getEndInstant()
        )
        val negativeOutOfRangeAvgAbsoluteElevationStats = DataPoints.absoluteElevationStats(
            12.0,
            240.0,
            -1000000.1,
            getStartInstant(),
            getEndInstant()
        )
        val positiveOutOfRangeMinAbsoluteElevationStats = DataPoints.absoluteElevationStats(
            1000001.0,
            240.0,
            120.0,
            getStartInstant(),
            getEndInstant()
        )
        val positiveOutOfRangeMaxAbsoluteElevationStats = DataPoints.absoluteElevationStats(
            12.0,
            1000001.0,
            120.0,
            getStartInstant(),
            getEndInstant()
        )
        val positiveOutOfRangeAvgAbsoluteElevationStats = DataPoints.absoluteElevationStats(
            12.0,
            240.0,
            1000001.0,
            getStartInstant(),
            getEndInstant()
        )

        Truth.assertThat(negativeOutOfRangeMinAbsoluteElevationStats).isNotNull()
        Truth.assertThat(negativeOutOfRangeMaxAbsoluteElevationStats).isNotNull()
        Truth.assertThat(negativeOutOfRangeAvgAbsoluteElevationStats).isNotNull()
        Truth.assertThat(positiveOutOfRangeMinAbsoluteElevationStats).isNotNull()
        Truth.assertThat(positiveOutOfRangeMaxAbsoluteElevationStats).isNotNull()
        Truth.assertThat(positiveOutOfRangeAvgAbsoluteElevationStats).isNotNull()
    }

    @Test
    fun rangeValidationWithFloors_success() {
        val floors = DataPoints.floors(20.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(floors).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidFloors_throwsNoException() {
        val negativeOutOfRangeFloors =
            DataPoints.floors(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        val positiveOutOfRangeFloors =
            DataPoints.floors(1000001.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(negativeOutOfRangeFloors).isNotNull()
        Truth.assertThat(positiveOutOfRangeFloors).isNotNull()
    }

    @Test
    fun rangeValidationWithCalories_success() {
        val calories =
            DataPoints.calories(15.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(calories).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidCalories_throwsNoException() {
        val negativeOutOfRangeCalories =
            DataPoints.calories(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        val positiveOutOfRangeSCalories =
            DataPoints.calories(1000001.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(negativeOutOfRangeCalories).isNotNull()
        Truth.assertThat(positiveOutOfRangeSCalories).isNotNull()
    }

    @Test
    fun rangeValidationWithCaloriesTotal_success() {
        val caloriesTotal = DataPoints.caloriesTotal(100.0, getStartInstant(), getEndInstant())

        Truth.assertThat(caloriesTotal).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidCaloriesTotal_throwsNoException() {
        val negativeOutOfRangeCaloriesTotal =
            DataPoints.caloriesTotal(-1.0, getStartInstant(), getEndInstant())
        val positiveOutOfRangeCaloriesTotal =
            DataPoints.caloriesTotal(1000001.0, getStartInstant(), getEndInstant())

        Truth.assertThat(negativeOutOfRangeCaloriesTotal).isNotNull()
        Truth.assertThat(positiveOutOfRangeCaloriesTotal).isNotNull()
    }

    @Test
    fun rangeValidationWithSwimmingStrokes_success() {
        val swimmingStrokes =
            DataPoints.swimmingStrokes(500, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(swimmingStrokes).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidSwimmingStrokes_throwsNoException() {
        val negativeOutOfRangeSwimmingStrokes =
            DataPoints.swimmingStrokes(-1, getStartDurationFromBoot(), getEndDurationFromBoot())
        val positiveOutOfRangeSwimmingStrokes = DataPoints.swimmingStrokes(
            1000001,
            getStartDurationFromBoot(),
            getEndDurationFromBoot()
        )

        Truth.assertThat(negativeOutOfRangeSwimmingStrokes).isNotNull()
        Truth.assertThat(positiveOutOfRangeSwimmingStrokes).isNotNull()
    }

    @Test
    fun rangeValidationWithGolfShotCount_success() {
        val golfShotCount =
            DataPoints.golfShotCount(30, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(golfShotCount).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidGolfShotCount_throwsNoException() {
        val negativeOutOfRangeGolfShotCount =
            DataPoints.golfShotCount(-1, getStartDurationFromBoot(), getEndDurationFromBoot())
        val positiveOutOfRangeGolfShotCount =
            DataPoints.golfShotCount(1000001, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(negativeOutOfRangeGolfShotCount).isNotNull()
        Truth.assertThat(positiveOutOfRangeGolfShotCount).isNotNull()
    }

    @Test
    fun rangeValidationWithLocation_success() {
        val location = DataPoints.location(89.99, -179.99, getStartDurationFromBoot(), 25.0, 21.0)

        Truth.assertThat(location).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidLocation_throwsNoException() {
        val negativeOutOfRangeLatitude =
            DataPoints.location(-90.1, -179.99, getStartDurationFromBoot(), 25.0, 21.0)
        val positiveOutOfRangeLatitude =
            DataPoints.location(90.1, -179.99, getStartDurationFromBoot(), 25.0, 21.0)
        val negativeOutOfRangeLongitude =
            DataPoints.location(89.0, -180.1, getStartDurationFromBoot(), 25.0, 21.0)
        val positiveOutOfRangeLongitude =
            DataPoints.location(89.0, 180.1, getStartDurationFromBoot(), 25.0, 21.0)
        val negativeOutOfRangeBearing =
            DataPoints.location(89.99, -179.99, getStartDurationFromBoot(), 25.0, -1.1)
        val positiveOutOfRangeBearing =
            DataPoints.location(89.99, -179.99, getStartDurationFromBoot(), 25.0, 360.0)

        Truth.assertThat(negativeOutOfRangeLatitude).isNotNull()
        Truth.assertThat(positiveOutOfRangeLatitude).isNotNull()
        Truth.assertThat(negativeOutOfRangeLongitude).isNotNull()
        Truth.assertThat(positiveOutOfRangeLongitude).isNotNull()
        Truth.assertThat(negativeOutOfRangeBearing).isNotNull()
        Truth.assertThat(positiveOutOfRangeBearing).isNotNull()
    }

    @Test
    fun rangeValidationWithSpeed_success() {
        val speed = DataPoints.speed(10.0, getStartDurationFromBoot())

        Truth.assertThat(speed).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidSpeed_throwsNoException() {
        val negativeOutOfRangeSpeed = DataPoints.speed(-1.0, getStartDurationFromBoot())
        val positiveOutOfRangeSpeed = DataPoints.speed(1000000.1, getStartDurationFromBoot())

        Truth.assertThat(negativeOutOfRangeSpeed).isNotNull()
        Truth.assertThat(positiveOutOfRangeSpeed).isNotNull()
    }

    @Test
    fun rangeValidationWithHeartRate_success() {
        val heartRate = DataPoints.heartRate(100.0, getStartDurationFromBoot())

        Truth.assertThat(heartRate).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidHeartRate_throwsNoException() {
        val negativeOutOfRangeHeartRate = DataPoints.heartRate(-1.0, getStartDurationFromBoot())
        val positiveOutOfRangeHeartRate = DataPoints.heartRate(300.1, getStartDurationFromBoot())

        Truth.assertThat(negativeOutOfRangeHeartRate).isNotNull()
        Truth.assertThat(positiveOutOfRangeHeartRate).isNotNull()
    }

    @Test
    fun rangeValidationWithHeartRateStats_success() {
        val heartRateStats =
            DataPoints.heartRateStats(50.0, 225.0, 80.0, getStartInstant(), getEndInstant())

        Truth.assertThat(heartRateStats).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidHeartRateStats_throwsNoException() {
        val negativeOutOfRangeMinHeartRateStats =
            DataPoints.heartRateStats(-1.0, 225.0, 80.0, getStartInstant(), getEndInstant())
        val negativeOutOfRangeMaxHeartRateStats =
            DataPoints.heartRateStats(50.0, -1.0, 80.0, getStartInstant(), getEndInstant())
        val negativeOutOfRangeAvgHeartRateStats =
            DataPoints.heartRateStats(50.0, 225.0, -1.0, getStartInstant(), getEndInstant())
        val positiveOutOfRangeMinHeartRateStats =
            DataPoints.heartRateStats(300.1, 225.0, 80.0, getStartInstant(), getEndInstant())
        val positiveOutOfRangeMaxHeartRateStats =
            DataPoints.heartRateStats(50.0, 300.1, 80.0, getStartInstant(), getEndInstant())
        val positiveOutOfRangeAvgHeartRateStats =
            DataPoints.heartRateStats(50.0, 225.0, 300.1, getStartInstant(), getEndInstant())

        Truth.assertThat(negativeOutOfRangeMinHeartRateStats).isNotNull()
        Truth.assertThat(negativeOutOfRangeMaxHeartRateStats).isNotNull()
        Truth.assertThat(negativeOutOfRangeAvgHeartRateStats).isNotNull()
        Truth.assertThat(positiveOutOfRangeMinHeartRateStats).isNotNull()
        Truth.assertThat(positiveOutOfRangeMaxHeartRateStats).isNotNull()
        Truth.assertThat(positiveOutOfRangeAvgHeartRateStats).isNotNull()
    }

    @Test
    fun rangeValidationWithDailySteps_success() {
        val dailySteps =
            DataPoints.dailySteps(20000, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(dailySteps).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidDailySteps_throwsNoException() {
        val negativeOutOfRangeDailySteps =
            DataPoints.dailySteps(-1, getStartDurationFromBoot(), getEndDurationFromBoot())
        val positiveOutOfRangeDailySteps =
            DataPoints.dailySteps(1000001, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(negativeOutOfRangeDailySteps).isNotNull()
        Truth.assertThat(positiveOutOfRangeDailySteps).isNotNull()
    }

    @Test
    fun rangeValidationWithDailyFloors_success() {
        val dailyFloors =
            DataPoints.dailyFloors(50.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(dailyFloors).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidDailyFloors_throwsNoException() {
        val negativeOutOfRangeDailyFloors =
            DataPoints.dailyFloors(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        val positiveOutOfRangeDailyFloors =
            DataPoints.dailyFloors(1000000.1, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(negativeOutOfRangeDailyFloors).isNotNull()
        Truth.assertThat(positiveOutOfRangeDailyFloors).isNotNull()
    }

    fun rangeValidationWithDailyCalories_success() {
        val dailyCalories =
            DataPoints.dailyCalories(600.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(dailyCalories).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidDailyCalories_throwsNoException() {
        val negativeOutOfRangeDailyCalories =
            DataPoints.dailyCalories(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        val positiveOutOfRangeDailyCalories = DataPoints.dailyCalories(
            1000000.1,
            getStartDurationFromBoot(),
            getEndDurationFromBoot()
        )

        Truth.assertThat(negativeOutOfRangeDailyCalories).isNotNull()
        Truth.assertThat(positiveOutOfRangeDailyCalories).isNotNull()
    }

    @Test
    fun rangeValidationWithDailyDistance_success() {
        val dailyDistance =
            DataPoints.dailyDistance(5.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(dailyDistance).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidDailyDistance_throwsNoException() {
        val negativeOutOfRangeDailyDistance =
            DataPoints.dailyDistance(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        val positiveOutOfRangeDailyDistance = DataPoints.dailyDistance(
            1000000.1,
            getStartDurationFromBoot(),
            getEndDurationFromBoot()
        )

        Truth.assertThat(negativeOutOfRangeDailyDistance).isNotNull()
        Truth.assertThat(positiveOutOfRangeDailyDistance).isNotNull()
    }

    private fun getStartDurationFromBoot() = Duration.ofSeconds(1)

    private fun getEndDurationFromBoot() = Duration.ofHours(1)

    private fun getStartInstant() = Instant.now().minusSeconds(5000)

    private fun getEndInstant() = Instant.now()
}
