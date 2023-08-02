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

package androidx.health.services.client.impl.request

import android.os.Parcel
import androidx.health.services.client.data.GolfExerciseTypeConfig
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdateExerciseTypeConfigRequestTest {
    @Test
    fun parcelableRoundTrip() {
        val request =
            UpdateExerciseTypeConfigRequest(
                "package",
                GolfExerciseTypeConfig(
                    GolfExerciseTypeConfig
                        .GolfShotTrackingPlaceInfo.GOLF_SHOT_TRACKING_PLACE_INFO_TEE_BOX
                )
            )
        val parcel = Parcel.obtain()

        request.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val fromParcel = UpdateExerciseTypeConfigRequest.CREATOR.createFromParcel(parcel)

        Truth.assertThat(request).isEqualTo(fromParcel)
    }
}
