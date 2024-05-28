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

import androidx.health.services.client.proto.DataProto
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocationDataTest {

    @Test
    fun protoRoundTrip() {
        val valueProtoBuilder = DataProto.Value.newBuilder()
        LocationData(latitude = 1.4, longitude = 2.3, altitude = 3.2, bearing = 4.1)
            .addToValueProtoBuilder(valueProtoBuilder)

        val location = LocationData.fromDataProtoValue(valueProtoBuilder.build())

        assertThat(location.latitude).isEqualTo(1.4)
        assertThat(location.longitude).isEqualTo(2.3)
        assertThat(location.altitude).isEqualTo(3.2)
        assertThat(location.bearing).isEqualTo(4.1)
    }

    @Test
    fun protoRoundTripWithDefaultAltitudeAndBearing() {
        val valueProtoBuilder = DataProto.Value.newBuilder()
        LocationData(
                latitude = 1.4,
                longitude = 2.3,
            )
            .addToValueProtoBuilder(valueProtoBuilder)

        val location = LocationData.fromDataProtoValue(valueProtoBuilder.build())

        assertThat(location.latitude).isEqualTo(1.4)
        assertThat(location.longitude).isEqualTo(2.3)
        assertThat(location.altitude).isEqualTo(LocationData.ALTITUDE_UNAVAILABLE)
        assertThat(location.bearing).isEqualTo(LocationData.BEARING_UNAVAILABLE)
    }

    @Test
    fun rangeValidationWithInvalidLatitude_throwsNoException() {
        val negativeOutOfRange = LocationData(-91.0, 10.0, 1.0, 10.0)
        val positiveOutOfRange = LocationData(91.0, 10.0, 1.0, 10.0)

        assertThat(negativeOutOfRange).isNotNull()
        assertThat(positiveOutOfRange).isNotNull()
    }

    @Test
    fun rangeValidationWithLongitude_throwsNoException() {
        val negativeBoundaryLocationData = LocationData(-90.0, -180.0, 1.0, 10.0)
        val positiveBoundaryLocationData = LocationData(90.0, 180.0, 1.0, 10.0)

        assertThat(negativeBoundaryLocationData).isNotNull()
        assertThat(positiveBoundaryLocationData).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidLongitude_throwsNoException() {
        val negativeOutOfRange = LocationData(-11.0, -181.0, 1.0, 10.0)
        val positiveOutOfRange = LocationData(85.0, 181.0, 1.0, 10.0)

        assertThat(negativeOutOfRange).isNotNull()
        assertThat(positiveOutOfRange).isNotNull()
    }

    @Test
    fun rangeValidationWithInvalidBearing_throwsNoException() {
        val negativeOutOfRange = LocationData(-11.0, -172.0, 1.0, -2.0)
        val positiveOutOfRange = LocationData(85.0, 10.0, 1.0, 360.0)

        assertThat(negativeOutOfRange).isNotNull()
        assertThat(positiveOutOfRange).isNotNull()
    }

    @Test
    fun rangeValidation_throwsNoException() {
        val locationData = LocationData(-11.0, -172.0, 1.0, 10.0)

        assertNotNull(locationData)
    }
}
