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
import android.os.Build
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
class AudioEncoderInfoImplTest {

    companion object {
        private const val MIME_TYPE = "audio/mp4a-latm"
        private const val ENCODER_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC
        private const val BIT_RATE = 64000
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_COUNT = 1
        private val TIMEBASE = Timebase.UPTIME
    }

    private lateinit var encoderConfig: AudioEncoderConfig

    @Before
    fun setup() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )

        encoderConfig =
            AudioEncoderConfig.builder()
                .setMimeType(MIME_TYPE)
                .setInputTimebase(TIMEBASE)
                .setProfile(ENCODER_PROFILE)
                .setBitrate(BIT_RATE)
                .setSampleRate(SAMPLE_RATE)
                .setChannelCount(CHANNEL_COUNT)
                .build()
    }

    @Test
    fun canCreateEncoderInfoFromConfig() {
        // No exception is thrown
        AudioEncoderInfoImpl.from(encoderConfig)
    }
}
