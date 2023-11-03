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
    fun rangeValidationForInvalidLocationAccuracy_throwsNoException() {
        val invalidHorizontalPositionErrorMeters = LocationAccuracy(-1.0, 1.0,)
        val invalidVerticalPositionErrorMeters = LocationAccuracy(1.0, -1.0,)

        Truth.assertThat(invalidHorizontalPositionErrorMeters).isNotNull()
        Truth.assertThat(invalidVerticalPositionErrorMeters).isNotNull()
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
