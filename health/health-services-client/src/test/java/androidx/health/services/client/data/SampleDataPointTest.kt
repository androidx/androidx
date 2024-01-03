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
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM
import androidx.health.services.client.data.DataType.Companion.LOCATION
import androidx.health.services.client.data.HeartRateAccuracy.SensorStatus.Companion.ACCURACY_HIGH
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SampleDataPointTest {
    fun Int.duration() = Duration.ofSeconds(toLong())

    @Test
    fun protoRoundTrip() {
        val proto = SampleDataPoint(
            HEART_RATE_BPM,
            130.0,
            20.duration(),
            Bundle().apply {
                putInt("int", 5)
                putString("string", "value")
            },
            HeartRateAccuracy(ACCURACY_HIGH)
        ).proto

        val dataPoint = SampleDataPoint.fromProto(proto)

        assertThat(dataPoint.dataType).isEqualTo(HEART_RATE_BPM)
        assertThat(dataPoint.value).isEqualTo(130.0)
        assertThat(dataPoint.timeDurationFromBoot).isEqualTo(20.duration())
        assertThat(dataPoint.metadata.getInt("int")).isEqualTo(5)
        assertThat(dataPoint.metadata.getString("string")).isEqualTo("value")
        assertThat((dataPoint.accuracy as HeartRateAccuracy).sensorStatus).isEqualTo(ACCURACY_HIGH)
    }

    @Test
    fun protoRoundTrip_emptyBundleAndAccuracy() {
        val proto = SampleDataPoint(
            HEART_RATE_BPM,
            130.0,
            20.duration(),
            accuracy = null,
            metadata = Bundle()
        ).proto

        val dataPoint = SampleDataPoint.fromProto(proto)

        assertThat(dataPoint.dataType).isEqualTo(HEART_RATE_BPM)
        assertThat(dataPoint.value).isEqualTo(130.0)
        assertThat(dataPoint.timeDurationFromBoot).isEqualTo(20.duration())
        assertThat(dataPoint.metadata.keySet()).isEmpty()
        assertThat(dataPoint.accuracy).isNull()
    }

    @Test
    fun protoRoundTripLocation() {
        val proto = SampleDataPoint(
            dataType = LOCATION,
            value = LocationData(
                latitude = 41.2,
                longitude = 82.3,
                altitude = 93.4,
                bearing = 274.5
            ),
            timeDurationFromBoot = 20.duration(),
            accuracy = LocationAccuracy(
                horizontalPositionErrorMeters = 3.5,
                verticalPositionErrorMeters = 4.7
            )
        ).proto

        val dataPoint = SampleDataPoint.fromProto(proto)

        assertThat(dataPoint.dataType).isEqualTo(LOCATION)
        val data = dataPoint.value as LocationData
        assertThat(data.latitude).isEqualTo(41.2)
        assertThat(data.longitude).isEqualTo(82.3)
        assertThat(data.altitude).isEqualTo(93.4)
        assertThat(data.bearing).isEqualTo(274.5)
        assertThat(dataPoint.timeDurationFromBoot).isEqualTo(20.duration())
        val accuracy = dataPoint.accuracy as LocationAccuracy
        assertThat(accuracy.horizontalPositionErrorMeters).isEqualTo(3.5)
        assertThat(accuracy.verticalPositionErrorMeters).isEqualTo(4.7)
    }

    @Test
    fun protoRoundTripLocation_defaultAltitudeAndBearing() {
        val proto = SampleDataPoint(
            dataType = LOCATION,
            value = LocationData(
                latitude = 41.2,
                longitude = 82.3,
            ),
            timeDurationFromBoot = 20.duration(),
            accuracy = LocationAccuracy(
                horizontalPositionErrorMeters = 3.5,
            )
        ).proto

        val dataPoint = SampleDataPoint.fromProto(proto)

        assertThat(dataPoint.dataType).isEqualTo(LOCATION)
        val data = dataPoint.value as LocationData
        assertThat(data.latitude).isEqualTo(41.2)
        assertThat(data.longitude).isEqualTo(82.3)
        assertThat(data.altitude).isEqualTo(LocationData.ALTITUDE_UNAVAILABLE)
        assertThat(data.bearing).isEqualTo(LocationData.BEARING_UNAVAILABLE)
        assertThat(dataPoint.timeDurationFromBoot).isEqualTo(20.duration())
        val accuracy = dataPoint.accuracy as LocationAccuracy
        assertThat(accuracy.horizontalPositionErrorMeters).isEqualTo(3.5)
        assertThat(accuracy.verticalPositionErrorMeters).isEqualTo(Double.MAX_VALUE)
    }
}
