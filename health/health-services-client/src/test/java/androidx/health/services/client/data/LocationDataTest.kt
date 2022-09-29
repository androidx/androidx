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
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocationDataTest {

    @Test
    fun protoRoundTrip() {
        val valueProtoBuilder = DataProto.Value.newBuilder()
        LocationData(
            latitude = 1.4,
            longitude = 2.3,
            altitude = 3.2,
            bearing = 4.1
        ).addToValueProtoBuilder(valueProtoBuilder)

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
        ).addToValueProtoBuilder(valueProtoBuilder)

        val location = LocationData.fromDataProtoValue(valueProtoBuilder.build())

        assertThat(location.latitude).isEqualTo(1.4)
        assertThat(location.longitude).isEqualTo(2.3)
        assertThat(location.altitude).isEqualTo(LocationData.ALTITUDE_UNAVAILABLE)
        assertThat(location.bearing).isEqualTo(LocationData.BEARING_UNAVAILABLE)
    }

    @Test
    fun rangeValidationWithLatitude_throwsException() {
        val negativeOutOfRange = assertThrows(IllegalArgumentException::class.java) {
            LocationData(/* Out of range latitude */-91.0,
                /* In range longitude */ 10.0,
                /* In range altitude */1.0,
                /* In range bearing */10.0
            )
        }
        val positiveOutOfRange = assertThrows(IllegalArgumentException::class.java) {
            LocationData(/* Out of range latitude */91.0,
                /* In range longitude */ 10.0,
                /* In range altitude */1.0,
                /* In range bearing */10.0
            )
        }

        assertThat(negativeOutOfRange).isNotNull()
        assertThat(positiveOutOfRange).isNotNull()
    }

    @Test
    fun rangeValidationWithLongitude_throwsException() {
        val negativeBoundaryLocationData = LocationData(-90.0, -180.0, 1.0, 10.0)
        val positiveBoundaryLocationData = LocationData(90.0, 180.0, 1.0, 10.0)

        assertThat(negativeBoundaryLocationData).isNotNull()
        assertThat(positiveBoundaryLocationData).isNotNull()
        val negativeOutOfRange = assertThrows(IllegalArgumentException::class.java) {
            LocationData(/* In range latitude */-11.0,
                /* Out of range longitude */ -181.0,
                /* In range altitude */1.0,
                /* In range bearing */10.0
            )
        }
        val positiveOutOfRange = assertThrows(IllegalArgumentException::class.java) {
            LocationData(/* In range latitude */85.0,
                /* Out of range longitude */ 181.0,
                /* In range altitude */1.0,
                /* In range bearing */10.0
            )
        }
        assertThat(negativeOutOfRange).isNotNull()
        assertThat(positiveOutOfRange).isNotNull()
    }

    @Test
    fun rangeValidationWithBearing_throwsException() {
        val negativeOutOfRange = assertThrows(IllegalArgumentException::class.java) {
            LocationData(/* In range latitude */-11.0,
                /* In range longitude */ -172.0,
                /* In range altitude */1.0,
                /* Out of range bearing */-2.0
            )
        }
        val positiveOutOfRange = assertThrows(IllegalArgumentException::class.java) {
            LocationData(/* In range latitude */85.0,
                /* In range longitude */ 10.0,
                /* In range altitude */1.0,
                /* Out of range bearing */360.0
            )
        }
        assertThat(negativeOutOfRange).isNotNull()
        assertThat(positiveOutOfRange).isNotNull()
    }

    @Test
    fun rangeValidation_throwsNoException() {
        val locationData = LocationData(/* In range latitude */-11.0,
            /* In range longitude */ -172.0,
            /* In range altitude */1.0,
            /* In range bearing */10.0
        )

        assertNotNull(locationData)
    }
}