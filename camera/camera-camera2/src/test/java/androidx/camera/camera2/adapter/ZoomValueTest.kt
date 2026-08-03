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

package androidx.camera.camera2.adapter

import androidx.camera.core.CameraInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class ZoomValueTest {

    @Test
    fun activeIntrinsicZoomRatio_isUnknownByDefault() {
        val zoomValue = ZoomValue(1.0f, 1.0f, 5.0f)

        assertThat(zoomValue.activeIntrinsicZoomRatio)
            .isEqualTo(CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN)
    }

    @Test
    fun primaryConstructor_setsActiveIntrinsicZoomRatio() {
        val zoomValue = ZoomValue(1.0f, 1.0f, 5.0f, 0.5f)

        assertThat(zoomValue.activeIntrinsicZoomRatio).isEqualTo(0.5f)
    }

    @Test
    fun secondaryConstructor_setsActiveIntrinsicZoomRatio() {
        val zoomValue = ZoomValue(ZoomValue.LinearZoom(0.5f), 1.0f, 5.0f, 2.0f)

        assertThat(zoomValue.activeIntrinsicZoomRatio).isEqualTo(2.0f)
    }
}
