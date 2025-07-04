/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.core.internal

import android.graphics.ImageFormat
import android.media.MediaCodec
import android.util.Size
import androidx.camera.core.Preview
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.SessionConfig.OutputConfig
import androidx.camera.testing.impl.FrameRateUtil.FPS_120_120
import androidx.camera.testing.impl.FrameRateUtil.FPS_30_120
import androidx.camera.testing.impl.fakes.FakeDeferrableSurface
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class HighSpeedFpsModifierTest {

    private companion object {
        private val ANY_SIZE = Size(640, 480)
    }

    private val videoSurface =
        FakeDeferrableSurface(ANY_SIZE, ImageFormat.PRIVATE).apply {
            setContainerClass(MediaCodec::class.java)
        }
    private val previewSurface =
        FakeDeferrableSurface(ANY_SIZE, ImageFormat.PRIVATE).apply {
            setContainerClass(Preview::class.java)
        }
    private val highSpeedFpsModifier = HighSpeedFpsModifier()

    @Test
    fun modifyFpsForPreviewOnlyRepeating_twoOutputConfigsWithoutVideoSurface_fpsRangeModified() {
        // Arrange
        val outputConfigs =
            listOf(
                OutputConfig.builder(previewSurface).build(),
                OutputConfig.builder(videoSurface).build(),
            )
        val repeatingConfigBuilder =
            CaptureConfig.Builder().apply {
                addSurface(previewSurface)
                setExpectedFrameRateRange(FPS_120_120)
            }

        // Act
        highSpeedFpsModifier.modifyFpsForPreviewOnlyRepeating(outputConfigs, repeatingConfigBuilder)

        // Assert
        assertThat(repeatingConfigBuilder.expectedFrameRateRange).isEqualTo(FPS_30_120)
    }

    @Test
    fun modifyFpsForPreviewOnlyRepeating_twoOutputConfigsWithVideoSurface_fpsRangeNotModified() {
        // Arrange
        val outputConfigs =
            listOf(
                OutputConfig.builder(previewSurface).build(),
                OutputConfig.builder(videoSurface).build(),
            )
        val repeatingConfigBuilder =
            CaptureConfig.Builder().apply {
                addSurface(previewSurface)
                addSurface(videoSurface)
                setExpectedFrameRateRange(FPS_120_120)
            }

        // Act
        highSpeedFpsModifier.modifyFpsForPreviewOnlyRepeating(outputConfigs, repeatingConfigBuilder)

        // Assert
        assertThat(repeatingConfigBuilder.expectedFrameRateRange).isEqualTo(FPS_120_120)
    }
}
