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
}