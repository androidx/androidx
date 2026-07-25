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

package androidx.camera.video

import android.util.Range
import android.view.Surface
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.ImageOutputConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class VideoCaptureDslTest {

    @Test
    fun testVideoCaptureDsl() {
        val videoOutput =
            object : VideoOutput {
                override fun onSurfaceRequested(request: SurfaceRequest) {
                    request.willNotProvideSurface()
                }
            }
        val videoCapture =
            videoCapture(videoOutput) {
                targetName = "test_video"
                targetRotation = Surface.ROTATION_90
                isVideoStabilizationEnabled = true
                targetFrameRate = Range(30, 60)
            }

        val config = videoCapture.currentConfig as ImageOutputConfig
        assertThat(videoCapture.name).isEqualTo("test_video")
        assertThat(config.getTargetRotation(Surface.ROTATION_0)).isEqualTo(Surface.ROTATION_90)
        assertThat(videoCapture.isVideoStabilizationEnabled).isTrue()
        assertThat(videoCapture.targetFrameRate).isEqualTo(Range(30, 60))
    }
}
