/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.graph

import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.camera2.pipe.compat.CorrectedFrameMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class CorrectedFrameMetadataTest {

    @Test
    fun canOverrideFrameMetadata() {
        val metadata = FakeFrameMetadata(
            mapOf(
                CaptureResult.CONTROL_AE_MODE to CaptureResult.CONTROL_AE_MODE_ON,
                CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            ),
            mapOf(FakeMetadata.TEST_KEY to 42)
        )

        val fixed = CorrectedFrameMetadata(
            metadata,
            mapOf(
                CaptureResult.CONTROL_AE_MODE to CaptureResult.CONTROL_AE_MODE_OFF,
                CaptureResult.LENS_STATE to CaptureResult.LENS_STATE_STATIONARY
            )
        )

        assertThat(fixed[CaptureResult.CONTROL_AE_MODE])
            .isEqualTo(CaptureResult.CONTROL_AE_MODE_OFF)
        assertThat(fixed[CaptureResult.CONTROL_AF_MODE])
            .isEqualTo(CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        assertThat(fixed[CaptureResult.LENS_STATE]).isEqualTo(CaptureResult.LENS_STATE_STATIONARY)
        assertThat(fixed[FakeMetadata.TEST_KEY]).isEqualTo(42)
        assertThat(fixed[FakeMetadata.TEST_KEY_ABSENT]).isNull()
    }
}