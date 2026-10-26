/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.wear.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class WearWidgetUpdateRequestTest {

    @Test
    fun fromParcel_matchesOriginal() {
        val originalRequest = WearWidgetUpdateRequest(instanceId = WidgetInstanceId("ns", 123))

        val parcel = originalRequest.toParcel()
        val restoredRequest = WearWidgetUpdateRequest.fromParcel(parcel)

        assertThat(restoredRequest.instanceId).isEqualTo(originalRequest.instanceId)
    }
}
