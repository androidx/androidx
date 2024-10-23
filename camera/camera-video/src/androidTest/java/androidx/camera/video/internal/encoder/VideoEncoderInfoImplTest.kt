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

package androidx.camera.video.internal.encoder

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.util.Size
import androidx.camera.core.impl.Timebase
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class VideoEncoderInfoImplTest {

    companion object {
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val BIT_RATE = 10 * 1024 * 1024 // 10M
        private const val FRAME_RATE = 30
        private const val I_FRAME_INTERVAL = 1
        private const val WIDTH = 640
        private const val HEIGHT = 480
        private val TIMEBASE = Timebase.UPTIME
    }

    private lateinit var encoderConfig: VideoEncoderConfig

    @Before
    fun setup() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )

        encoderConfig =
            VideoEncoderConfig.builder()
                .setBitrate(BIT_RATE)
                .setColorFormat(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                .setFrameRate(FRAME_RATE)
                .setIFrameInterval(I_FRAME_INTERVAL)
                .setMimeType(MIME_TYPE)
                .setResolution(Size(WIDTH, HEIGHT))
                .setInputTimebase(TIMEBASE)
                .build()
    }

    @Test
    fun canCreateEncoderInfoFromConfig() {
        // No exception is thrown
        VideoEncoderInfoImpl.from(encoderConfig)
    }
}
