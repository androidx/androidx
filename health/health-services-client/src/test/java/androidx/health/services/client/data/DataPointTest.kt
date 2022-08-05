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
import org.junit.Assert.assertThrows
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
    fun rangeValidationWithSteps_exception() {
        val negativeOutOfRangeSteps = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.steps(-1, getStartDurationFromBoot(), getEndDurationFromBoot())
        }
        val positiveOutOfRangeSteps = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.steps(1000001, getStartDurationFromBoot(), getEndDurationFromBoot())
        }

        Truth.assertThat(negativeOutOfRangeSteps).hasMessageThat()
            .contains("steps value -1 is out of range")
        Truth.assertThat(positiveOutOfRangeSteps).hasMessageThat()
            .contains("steps value 1000001 is out of range")
    }

    @Test
    fun rangeValidationWithStepsPerMinute_success() {
        val stepsPerMinute = DataPoints.stepsPerMinute(10, getStartDurationFromBoot())

        Truth.assertThat(stepsPerMinute).isNotNull()
    }

    @Test
    fun rangeValidationWithStepsPerMinute_exception() {
        val negativeOutOfRangeStepsPerMinutes = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.stepsPerMinute(-1, getStartDurationFromBoot())
        }
        val positiveOutOfRangeStepsPerMinutes = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.stepsPerMinute(1000001, getEndDurationFromBoot())
        }

        Truth.assertThat(negativeOutOfRangeStepsPerMinutes).hasMessageThat()
            .contains("stepsPerMinute value -1 is out of range")
        Truth.assertThat(positiveOutOfRangeStepsPerMinutes).hasMessageThat()
            .contains("stepsPerMinute value 1000001 is out of range")
    }

    @Test
    fun rangeValidationWithDistance_success() {
        val distance =
            DataPoints.distance(12.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(distance).isNotNull()
    }

    @Test
    fun rangeValidationWithDistance_exception() {
        val negativeOutOfRangeDistance = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.distance(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        }
        val positiveOutOfRangeDistance = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.distance(1000000.1, getStartDurationFromBoot(), getEndDurationFromBoot())
        }

        Truth.assertThat(negativeOutOfRangeDistance).hasMessageThat()
            .contains("meters value -1.0 is out of range")

        Truth.assertThat(positiveOutOfRangeDistance).hasMessageThat()
            .contains("meters value 1000000.1 is out of range")
    }

    @Test
    fun rangeValidationWithDistanceTotal_success() {
        val distanceTotal =
            DataPoints.distanceTotal(12.0, getStartInstant(), getEndInstant())

        Truth.assertThat(distanceTotal).isNotNull()
    }

    @Test
    fun rangeValidationWithDistanceTotal_exception() {
        val negativeOutOfRangeDistanceTotal = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.distanceTotal(-1.0, getStartInstant(), getEndInstant())
        }
        val positiveOutOfRangeDistanceTotal = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.distanceTotal(1000000.1, getStartInstant(), getEndInstant())
        }

        Truth.assertThat(negativeOutOfRangeDistanceTotal).hasMessageThat()
            .contains("meters value -1.0 is out of range")

        Truth.assertThat(positiveOutOfRangeDistanceTotal).hasMessageThat()
            .contains("meters value 1000000.1 is out of range")
    }

    @Test
    fun rangeValidationWithElevationGain_success() {
        val elevationGain =
            DataPoints.elevationGain(12.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(elevationGain).isNotNull()
    }

    @Test
    fun rangeValidationWithElevationGain_exception() {
        val negativeOutOfRangeElevationGain = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.elevationGain(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        }
        val positiveOutOfRangeElevationGain = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.elevationGain(
                1000000.1,
                getStartDurationFromBoot(),
                getEndDurationFromBoot()
            )
        }

        Truth.assertThat(negativeOutOfRangeElevationGain).hasMessageThat()
            .contains("meters value -1.0 is out of range")
        Truth.assertThat(positiveOutOfRangeElevationGain).hasMessageThat()
            .contains("meters value 1000000.1 is out of range")
    }

    @Test
    fun rangeValidationWithElevationLoss_success() {
        val elevationLoss =
            DataPoints.elevationLoss(12.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(elevationLoss).isNotNull()
    }

    @Test
    fun rangeValidationWithElevationLoss_exception() {
        val negativeOutOfRangeElevationLoss = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.elevationLoss(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        }
        val positiveOutOfRangeElevationLoss = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.elevationLoss(
                1000000.1,
                getStartDurationFromBoot(),
                getEndDurationFromBoot()
            )
        }

        Truth.assertThat(negativeOutOfRangeElevationLoss).hasMessageThat()
            .contains("meters value -1.0 is out of range")
        Truth.assertThat(positiveOutOfRangeElevationLoss).hasMessageThat()
            .contains("meters value 1000000.1 is out of range")
    }

    @Test
    fun rangeValidationWithAbsoluteElevation_success() {
        val absoluteElevation =
            DataPoints.absoluteElevation(12.0, getStartDurationFromBoot())

        Truth.assertThat(absoluteElevation).isNotNull()
    }

    @Test
    fun rangeValidationWithAbsoluteElevation_exception() {
        val negativeOutOfRangeAbsoluteElevation =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.absoluteElevation(-1.0, getStartDurationFromBoot())
            }
        val positiveOutOfRangeAbsoluteElevation =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.absoluteElevation(1000001.0, getStartDurationFromBoot())
            }

        Truth.assertThat(negativeOutOfRangeAbsoluteElevation).hasMessageThat()
            .contains("meters value -1.0 is out of range")
        Truth.assertThat(positiveOutOfRangeAbsoluteElevation).hasMessageThat()
            .contains("meters value 1000001.0 is out of range")
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
    fun rangeValidationWithAbsoluteElevationStats_exception() {
        val negativeOutOfRangeMinAbsoluteElevationStats =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.absoluteElevationStats(
                    -1.0,
                    240.0,
                    120.0,
                    getStartInstant(),
                    getEndInstant()
                )
            }
        val negativeOutOfRangeMaxAbsoluteElevationStats =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.absoluteElevationStats(
                    12.0,
                    -1.0,
                    120.0,
                    getStartInstant(),
                    getEndInstant()
                )
            }
        val negativeOutOfRangeAvgAbsoluteElevationStats =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.absoluteElevationStats(
                    12.0,
                    240.0,
                    -1.0,
                    getStartInstant(),
                    getEndInstant()
                )
            }
        val positiveOutOfRangeMinAbsoluteElevationStats =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.absoluteElevationStats(
                    1000001.0,
                    240.0,
                    120.0,
                    getStartInstant(),
                    getEndInstant()
                )
            }
        val positiveOutOfRangeMaxAbsoluteElevationStats =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.absoluteElevationStats(
                    12.0,
                    1000001.0,
                    120.0,
                    getStartInstant(),
                    getEndInstant()
                )
            }
        val positiveOutOfRangeAvgAbsoluteElevationStats =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.absoluteElevationStats(
                    12.0,
                    240.0,
                    1000001.0,
                    getStartInstant(),
                    getEndInstant()
                )
            }

        Truth.assertThat(negativeOutOfRangeMinAbsoluteElevationStats).hasMessageThat()
            .contains("minAbsoluteElevationMeters value -1.0 is out of range")
        Truth.assertThat(negativeOutOfRangeMaxAbsoluteElevationStats).hasMessageThat()
            .contains("maxAbsoluteElevationMeters value -1.0 is out of range")
        Truth.assertThat(negativeOutOfRangeAvgAbsoluteElevationStats).hasMessageThat()
            .contains("averageAbsoluteElevationMeters value -1.0 is out of range")
        Truth.assertThat(positiveOutOfRangeMinAbsoluteElevationStats).hasMessageThat()
            .contains("minAbsoluteElevationMeters value 1000001.0 is out of range")
        Truth.assertThat(positiveOutOfRangeMaxAbsoluteElevationStats).hasMessageThat()
            .contains("maxAbsoluteElevationMeters value 1000001.0 is out of range")
        Truth.assertThat(positiveOutOfRangeAvgAbsoluteElevationStats).hasMessageThat()
            .contains("averageAbsoluteElevationMeters value 1000001.0 is out of range")
    }

    @Test
    fun rangeValidationWithFloors_success() {
        val floors = DataPoints.floors(20.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(floors).isNotNull()
    }

    @Test
    fun rangeValidationWithFloors_exception() {
        val negativeOutOfRangeFloors = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.floors(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        }
        val positiveOutOfRangeFloors = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.floors(1000001.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        }

        Truth.assertThat(negativeOutOfRangeFloors).hasMessageThat()
            .contains("floors value -1.0 is out of range")
        Truth.assertThat(positiveOutOfRangeFloors).hasMessageThat()
            .contains("floors value 1000001.0 is out of range")
    }

    @Test
    fun rangeValidationWithCalories_success() {
        val calories =
            DataPoints.calories(15.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(calories).isNotNull()
    }

    @Test
    fun rangeValidationWithCalories_exception() {
        val negativeOutOfRangeCalories = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.calories(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        }
        val positiveOutOfRangeSCalories = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.calories(1000001.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        }

        Truth.assertThat(negativeOutOfRangeCalories).hasMessageThat()
            .contains("kilocalories value -1.0 is out of range")
        Truth.assertThat(positiveOutOfRangeSCalories).hasMessageThat()
            .contains("kilocalories value 1000001.0 is out of range")
    }

    @Test
    fun rangeValidationWithCaloriesTotal_success() {
        val caloriesTotal = DataPoints.caloriesTotal(100.0, getStartInstant(), getEndInstant())

        Truth.assertThat(caloriesTotal).isNotNull()
    }

    @Test
    fun rangeValidationWithCaloriesTotal_exception() {
        val negativeOutOfRangeCaloriesTotal = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.caloriesTotal(-1.0, getStartInstant(), getEndInstant())
        }
        val positiveOutOfRangeCaloriesTotal = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.caloriesTotal(1000001.0, getStartInstant(), getEndInstant())
        }

        Truth.assertThat(negativeOutOfRangeCaloriesTotal).hasMessageThat()
            .contains("kilocalories value -1.0 is out of range")
        Truth.assertThat(positiveOutOfRangeCaloriesTotal).hasMessageThat()
            .contains("kilocalories value 1000001.0 is out of range")
    }

    @Test
    fun rangeValidationWithSwimmingStrokes_success() {
        val swimmingStrokes =
            DataPoints.swimmingStrokes(500, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(swimmingStrokes).isNotNull()
    }

    @Test
    fun rangeValidationWithSwimmingStrokes_exception() {
        val negativeOutOfRangeSwimmingStrokes = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.swimmingStrokes(-1, getStartDurationFromBoot(), getEndDurationFromBoot())
        }
        val positiveOutOfRangeSwimmingStrokes = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.swimmingStrokes(
                1000001,
                getStartDurationFromBoot(),
                getEndDurationFromBoot()
            )
        }

        Truth.assertThat(negativeOutOfRangeSwimmingStrokes).hasMessageThat()
            .contains("strokes value -1 is out of range")
        Truth.assertThat(positiveOutOfRangeSwimmingStrokes).hasMessageThat()
            .contains("strokes value 1000001 is out of range")
    }

    @Test
    fun rangeValidationWithGolfShotCount_success() {
        val golfShotCount =
            DataPoints.golfShotCount(30, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(golfShotCount).isNotNull()
    }

    @Test
    fun rangeValidationWithGolfShotCount_exception() {
        val negativeOutOfRangeGolfShotCount = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.golfShotCount(-1, getStartDurationFromBoot(), getEndDurationFromBoot())
        }
        val positiveOutOfRangeGolfShotCount = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.golfShotCount(1000001, getStartDurationFromBoot(), getEndDurationFromBoot())
        }

        Truth.assertThat(negativeOutOfRangeGolfShotCount).hasMessageThat()
            .contains("shots value -1 is out of range")
        Truth.assertThat(positiveOutOfRangeGolfShotCount).hasMessageThat()
            .contains("shots value 1000001 is out of range")
    }

    @Test
    fun rangeValidationWithLocation_success() {
        val location = DataPoints.location(89.99, -179.99, getStartDurationFromBoot(), 25.0, 21.0)

        Truth.assertThat(location).isNotNull()
    }

    @Test
    fun rangeValidationWithLocation_exception() {
        val negativeOutOfRangeLatitude = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.location(-90.1, -179.99, getStartDurationFromBoot(), 25.0, 21.0)
        }
        val positiveOutOfRangeLatitude = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.location(90.1, -179.99, getStartDurationFromBoot(), 25.0, 21.0)
        }
        val negativeOutOfRangeLongitude = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.location(89.0, -180.1, getStartDurationFromBoot(), 25.0, 21.0)
        }
        val positiveOutOfRangeLongitude = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.location(89.0, 180.1, getStartDurationFromBoot(), 25.0, 21.0)
        }
        val negativeOutOfRangeBearing = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.location(89.99, -179.99, getStartDurationFromBoot(), 25.0, -1.1)
        }
        val positiveOutOfRangeBearing = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.location(89.99, -179.99, getStartDurationFromBoot(), 25.0, 360.0)
        }

        Truth.assertThat(negativeOutOfRangeLatitude).hasMessageThat()
            .contains("latitude value -90.1 is out of range")
        Truth.assertThat(positiveOutOfRangeLatitude).hasMessageThat()
            .contains("latitude value 90.1 is out of range")
        Truth.assertThat(negativeOutOfRangeLongitude).hasMessageThat()
            .contains("longitude value -180.1 is out of range")
        Truth.assertThat(positiveOutOfRangeLongitude).hasMessageThat()
            .contains("longitude value 180.1 is out of range")
        Truth.assertThat(negativeOutOfRangeBearing).hasMessageThat()
            .contains("bearing value -1.1 is out of range")
        Truth.assertThat(positiveOutOfRangeBearing).hasMessageThat()
            .contains("bearing value 360.0 is out of range")
    }

    @Test
    fun rangeValidationWithSpeed_success() {
        val speed = DataPoints.speed(10.0, getStartDurationFromBoot())

        Truth.assertThat(speed).isNotNull()
    }

    @Test
    fun rangeValidationWithSpeed_exception() {
        val negativeOutOfRangeSpeed = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.speed(-1.0, getStartDurationFromBoot())
        }
        val positiveOutOfRangeSpeed = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.speed(1000000.1, getStartDurationFromBoot())
        }

        Truth.assertThat(negativeOutOfRangeSpeed).hasMessageThat()
            .contains("metersPerSecond value -1.0 is out of range")
        Truth.assertThat(positiveOutOfRangeSpeed).hasMessageThat()
            .contains("metersPerSecond value 1000000.1 is out of range")
    }

    @Test
    fun rangeValidationWithHeartRate_success() {
        val heartRate = DataPoints.heartRate(100.0, getStartDurationFromBoot())

        Truth.assertThat(heartRate).isNotNull()
    }

    @Test
    fun rangeValidationWithHeartRate_exception() {
        val negativeOutOfRangeHeartRate = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.heartRate(-1.0, getStartDurationFromBoot())
        }
        val positiveOutOfRangeHeartRate = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.heartRate(300.1, getStartDurationFromBoot())
        }

        Truth.assertThat(negativeOutOfRangeHeartRate).hasMessageThat()
            .contains("bpm value -1.0 is out of range")
        Truth.assertThat(positiveOutOfRangeHeartRate).hasMessageThat()
            .contains("bpm value 300.1 is out of range")
    }

    @Test
    fun rangeValidationWithHeartRateStats_success() {
        val heartRateStats =
            DataPoints.heartRateStats(50.0, 225.0, 80.0, getStartInstant(), getEndInstant())

        Truth.assertThat(heartRateStats).isNotNull()
    }

    @Test
    fun rangeValidationWithHeartRateStats_exception() {
        val negativeOutOfRangeMinHeartRateStats =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.heartRateStats(-1.0, 225.0, 80.0, getStartInstant(), getEndInstant())
            }
        val negativeOutOfRangeMaxHeartRateStats =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.heartRateStats(50.0, -1.0, 80.0, getStartInstant(), getEndInstant())
            }
        val negativeOutOfRangeAvgHeartRateStats =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.heartRateStats(50.0, 225.0, -1.0, getStartInstant(), getEndInstant())
            }
        val positiveOutOfRangeMinHeartRateStats =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.heartRateStats(300.1, 225.0, 80.0, getStartInstant(), getEndInstant())
            }
        val positiveOutOfRangeMaxHeartRateStats =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.heartRateStats(50.0, 300.1, 80.0, getStartInstant(), getEndInstant())
            }
        val positiveOutOfRangeAvgHeartRateStats =
            assertThrows(IllegalArgumentException::class.java) {
                DataPoints.heartRateStats(50.0, 225.0, 300.1, getStartInstant(), getEndInstant())
            }

        Truth.assertThat(negativeOutOfRangeMinHeartRateStats).hasMessageThat()
            .contains("minBpm value -1.0 is out of range")
        Truth.assertThat(negativeOutOfRangeMaxHeartRateStats).hasMessageThat()
            .contains("maxBpm value -1.0 is out of range")
        Truth.assertThat(negativeOutOfRangeAvgHeartRateStats).hasMessageThat()
            .contains("averageBpm value -1.0 is out of range")
        Truth.assertThat(positiveOutOfRangeMinHeartRateStats).hasMessageThat()
            .contains("minBpm value 300.1 is out of range")
        Truth.assertThat(positiveOutOfRangeMaxHeartRateStats).hasMessageThat()
            .contains("maxBpm value 300.1 is out of range")
        Truth.assertThat(positiveOutOfRangeAvgHeartRateStats).hasMessageThat()
            .contains("averageBpm value 300.1 is out of range")
    }

    @Test
    fun rangeValidationWithDailySteps_success() {
        val dailySteps =
            DataPoints.dailySteps(20000, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(dailySteps).isNotNull()
    }

    @Test
    fun rangeValidationWithDailySteps_exception() {
        val negativeOutOfRangeDailySteps = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.dailySteps(-1, getStartDurationFromBoot(), getEndDurationFromBoot())
        }
        val positiveOutOfRangeDailySteps = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.dailySteps(1000001, getStartDurationFromBoot(), getEndDurationFromBoot())
        }

        Truth.assertThat(negativeOutOfRangeDailySteps).hasMessageThat()
            .contains("dailySteps value -1 is out of range")
        Truth.assertThat(positiveOutOfRangeDailySteps).hasMessageThat()
            .contains("dailySteps value 1000001 is out of range")
    }

    @Test
    fun rangeValidationWithDailyFloors_success() {
        val dailyFloors =
            DataPoints.dailyFloors(50.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(dailyFloors).isNotNull()
    }

    @Test
    fun rangeValidationWithDailyFloors_exception() {
        val negativeOutOfRangeDailyFloors = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.dailyFloors(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        }
        val positiveOutOfRangeDailyFloors = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.dailyFloors(1000000.1, getStartDurationFromBoot(), getEndDurationFromBoot())
        }

        Truth.assertThat(negativeOutOfRangeDailyFloors).hasMessageThat()
            .contains("floors value -1.0 is out of range")
        Truth.assertThat(positiveOutOfRangeDailyFloors).hasMessageThat()
            .contains("floors value 1000000.1 is out of range")
    }

    fun rangeValidationWithDailyCalories_success() {
        val dailyCalories =
            DataPoints.dailyCalories(600.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(dailyCalories).isNotNull()
    }

    @Test
    fun rangeValidationWithDailyCalories_exception() {
        val negativeOutOfRangeDailyCalories = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.dailyCalories(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        }
        val positiveOutOfRangeDailyCalories = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.dailyCalories(
                1000000.1,
                getStartDurationFromBoot(),
                getEndDurationFromBoot()
            )
        }

        Truth.assertThat(negativeOutOfRangeDailyCalories).hasMessageThat()
            .contains("calories value -1.0 is out of range")
        Truth.assertThat(positiveOutOfRangeDailyCalories).hasMessageThat()
            .contains("calories value 1000000.1 is out of range")
    }

    @Test
    fun rangeValidationWithDailyDistance_success() {
        val dailyDistance =
            DataPoints.dailyDistance(5.0, getStartDurationFromBoot(), getEndDurationFromBoot())

        Truth.assertThat(dailyDistance).isNotNull()
    }

    @Test
    fun rangeValidationWithDailyDistance_exception() {
        val negativeOutOfRangeDailyDistance = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.dailyDistance(-1.0, getStartDurationFromBoot(), getEndDurationFromBoot())
        }
        val positiveOutOfRangeDailyDistance = assertThrows(IllegalArgumentException::class.java) {
            DataPoints.dailyDistance(
                1000000.1,
                getStartDurationFromBoot(),
                getEndDurationFromBoot()
            )
        }

        Truth.assertThat(negativeOutOfRangeDailyDistance).hasMessageThat()
            .contains("meters value -1.0 is out of range")
        Truth.assertThat(positiveOutOfRangeDailyDistance).hasMessageThat()
            .contains("meters value 1000000.1 is out of range")
    }

    private fun getStartDurationFromBoot() = Duration.ofSeconds(1)

    private fun getEndDurationFromBoot() = Duration.ofHours(1)

    private fun getStartInstant() = Instant.now().minusSeconds(5000)

    private fun getEndInstant() = Instant.now()
}