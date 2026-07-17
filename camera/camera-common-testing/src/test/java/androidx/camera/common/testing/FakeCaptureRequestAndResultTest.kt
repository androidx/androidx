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

package androidx.camera.common.testing

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import androidx.camera.common.CameraFrameNumber
import androidx.camera.common.CameraId
import androidx.camera.common.CaptureRequestWrapper
import androidx.camera.common.CaptureResultWrapper
import androidx.camera.common.Metadata
import androidx.camera.common.unwrapAs
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
public class FakeCaptureRequestAndResultTest {

    private val cameraId = CameraId("0")
    private val testCustomKey = Metadata.Key<Int>("test.custom.key")
    private val testCustomKeyAbsent = Metadata.Key<Int>("test.custom.key.absent")

    @Test
    public fun fakeCaptureRequestBehavior() {
        val fake =
            FakeCaptureRequest(
                requestParameters =
                    mapOf(CaptureRequest.CONTROL_AE_MODE to CaptureRequest.CONTROL_AE_MODE_ON),
                requestMetadata = mapOf(testCustomKey to 42),
            )

        assertThat(fake[CaptureRequest.CONTROL_AE_MODE])
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
        assertThat(fake[testCustomKey]).isEqualTo(42)
        assertThat(fake[testCustomKeyAbsent]).isNull()

        // getOrDefault
        assertThat(fake.getOrDefault(CaptureRequest.CONTROL_AE_MODE, -1))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
        assertThat(fake.getOrDefault(CaptureRequest.CONTROL_AF_MODE, -1)).isEqualTo(-1)
        assertThat(fake.getOrDefault(testCustomKey, -1)).isEqualTo(42)

        // keys and metadataKeys
        assertThat(fake.keys).containsExactly(CaptureRequest.CONTROL_AE_MODE)
        assertThat(fake.metadataKeys).containsExactly(testCustomKey)

        // unwrapAs
        assertThat(fake.unwrapAs<CaptureRequestWrapper>()).isSameInstanceAs(fake)
        assertThat(fake.unwrapAs<CaptureRequest>()).isNull()
    }

    @Test
    public fun fakeCaptureResultBehavior() {
        val fakeRequest = FakeCaptureRequest()
        val fake =
            FakeCaptureResult(
                cameraId = cameraId,
                captureRequest = fakeRequest,
                frameNumber = CameraFrameNumber(42L),
                resultParameters =
                    mapOf(CaptureResult.LENS_STATE to CaptureResult.LENS_STATE_STATIONARY),
                resultMetadata = mapOf(testCustomKey to 42),
            )

        assertThat(fake.cameraId).isEqualTo(cameraId)
        assertThat(fake.frameNumber).isEqualTo(CameraFrameNumber(42L))
        assertThat(fake.captureRequest).isSameInstanceAs(fakeRequest)
        assertThat(fake[CaptureResult.LENS_STATE]).isEqualTo(CaptureResult.LENS_STATE_STATIONARY)
        assertThat(fake[testCustomKey]).isEqualTo(42)
        assertThat(fake[testCustomKeyAbsent]).isNull()

        // getOrDefault
        assertThat(fake.getOrDefault(CaptureResult.LENS_STATE, -1))
            .isEqualTo(CaptureResult.LENS_STATE_STATIONARY)
        assertThat(fake.getOrDefault(CaptureResult.CONTROL_AE_STATE, -1)).isEqualTo(-1)
        assertThat(fake.getOrDefault(testCustomKey, -1)).isEqualTo(42)

        // keys and metadataKeys
        assertThat(fake.keys).containsExactly(CaptureResult.LENS_STATE)
        assertThat(fake.metadataKeys).containsExactly(testCustomKey)

        // unwrapAs
        assertThat(fake.unwrapAs<CaptureResultWrapper>()).isSameInstanceAs(fake)
        assertThat(fake.unwrapAs<CaptureResult>()).isNull()
    }
}
