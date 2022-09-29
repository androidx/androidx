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

import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocationAccuracyTest {

    @Test
    fun rangeValidationForLocationAccuracy_throwsException() {
        val invalidHorizontalPositionErrorMeters =
            Assert.assertThrows(IllegalArgumentException::class.java) {
                LocationAccuracy(
                    /* negative horizontalPositionErrorMeters*/
                    -1.0,
                    /* positive verticalPositionErrorMeters*/
                    1.0,
                )
            }
        val invalidVerticalPositionErrorMeters =
            Assert.assertThrows(IllegalArgumentException::class.java) {
                LocationAccuracy(
                    /* positive horizontalPositionErrorMeters*/
                    1.0,
                    /* negative verticalPositionErrorMeters*/
                    -1.0,
                )
            }

        Truth.assertThat(invalidHorizontalPositionErrorMeters).hasMessageThat()
            .contains("horizontalPositionErrorMeters value -1.0 is out of range")
        Truth.assertThat(invalidVerticalPositionErrorMeters).hasMessageThat()
            .contains("verticalPositionErrorMeters value -1.0 is out of range")
    }

    @Test
    fun rangeValidationForLocationAccuracy_throwsNoException() {
        val validLocationAccuracy = LocationAccuracy(
            1.0,
            1.0
        )

        Assert.assertNotNull(validLocationAccuracy)
    }
}