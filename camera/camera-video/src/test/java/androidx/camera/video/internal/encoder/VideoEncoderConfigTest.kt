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

package androidx.camera.video.internal.encoder

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaFormat.KEY_CAPTURE_RATE
import android.media.MediaFormat.KEY_FRAME_RATE
import android.media.MediaFormat.KEY_OPERATING_RATE
import android.media.MediaFormat.KEY_PRIORITY
import android.util.Size
import androidx.camera.core.impl.Timebase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val FRAME_RATE = 30

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class VideoEncoderConfigTest {

    @Test
    fun toMediaFormat_sameCaptureEncodeFrameRates_doesNotContainCaptureRate() {
        // Act.
        val format = createVideoEncoderConfig(FRAME_RATE, FRAME_RATE).toMediaFormat()

        // Assert.
        assertThat(format.getInteger(KEY_FRAME_RATE)).isEqualTo(FRAME_RATE)
        assertThat(format.containsKey(KEY_CAPTURE_RATE)).isFalse()
        assertThat(format.containsKey(KEY_OPERATING_RATE)).isFalse()
        assertThat(format.containsKey(KEY_PRIORITY)).isFalse()
    }

    @Test
    fun toMediaFormat_differentCaptureEncodeFrameRates_containsAdditionalKeyValues() {
        // Arrange.
        val captureFrameRate = FRAME_RATE
        val encodeFrameRate = FRAME_RATE / 2

        // Act.
        val format = createVideoEncoderConfig(captureFrameRate, encodeFrameRate).toMediaFormat()

        // Assert.
        assertThat(format.getInteger(KEY_FRAME_RATE)).isEqualTo(encodeFrameRate)
        assertThat(format.getInteger(KEY_CAPTURE_RATE)).isEqualTo(captureFrameRate)
        assertThat(format.getInteger(KEY_OPERATING_RATE)).isEqualTo(captureFrameRate)
        assertThat(format.getInteger(KEY_PRIORITY)).isEqualTo(0)
    }

    private fun createVideoEncoderConfig(
        captureFrameRate: Int,
        encodeFrameRate: Int,
    ): VideoEncoderConfig =
        VideoEncoderConfig.builder()
            .setMimeType(MediaFormat.MIMETYPE_VIDEO_AVC)
            .setInputTimebase(Timebase.UPTIME)
            .setResolution(Size(1280, 720))
            .setColorFormat(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            .setBitrate(10 * 1024 * 1024)
            .setCaptureFrameRate(captureFrameRate)
            .setEncodeFrameRate(encodeFrameRate)
            .setIFrameInterval(1)
            .build()
}
