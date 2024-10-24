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

package androidx.camera.video.internal.config

import android.media.MediaCodecInfo
import android.os.Build
import android.util.Range
import androidx.camera.core.impl.Timebase
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.video.AudioSpec
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 21)
class AudioEncoderConfigDefaultResolverTest {

    companion object {
        const val MIME_TYPE = "audio/mp4a-latm"
        const val ENCODER_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC
        val TIMEBASE = Timebase.UPTIME
    }

    private val defaultAudioSpec = AudioSpec.builder().build()
    private val defaultAudioSettings = AudioSettingsDefaultResolver(defaultAudioSpec).get()

    @Test
    fun defaultAudioSpecProducesValidSettings() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        val resolvedAudioConfig =
            AudioEncoderConfigDefaultResolver(
                    MIME_TYPE,
                    ENCODER_PROFILE,
                    TIMEBASE,
                    defaultAudioSpec,
                    defaultAudioSettings
                )
                .get()

        assertThat(resolvedAudioConfig.mimeType).isEqualTo(MIME_TYPE)
        assertThat(resolvedAudioConfig.profile).isEqualTo(ENCODER_PROFILE)
        assertThat(resolvedAudioConfig.channelCount).isEqualTo(defaultAudioSettings.channelCount)
        assertThat(resolvedAudioConfig.sampleRate).isEqualTo(defaultAudioSettings.sampleRate)
        assertThat(resolvedAudioConfig.bitrate).isGreaterThan(0)
    }

    @Test
    fun increasedChannelCountIncreasesBitrate() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        // Get default channel count
        val defaultConfig =
            AudioEncoderConfigDefaultResolver(
                    MIME_TYPE,
                    ENCODER_PROFILE,
                    TIMEBASE,
                    defaultAudioSpec,
                    defaultAudioSettings
                )
                .get()
        val defaultChannelCount = defaultConfig.channelCount

        val higherChannelCountAudioSettings =
            defaultAudioSettings.toBuilder().setChannelCount(defaultChannelCount * 2).build()

        val higherChannelCountConfig =
            AudioEncoderConfigDefaultResolver(
                    MIME_TYPE,
                    ENCODER_PROFILE,
                    TIMEBASE,
                    defaultAudioSpec,
                    higherChannelCountAudioSettings
                )
                .get()

        assertThat(higherChannelCountConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }

    @Test
    fun increasedSampleRateIncreasesBitrate() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        // Get default sample rate
        val defaultConfig =
            AudioEncoderConfigDefaultResolver(
                    MIME_TYPE,
                    ENCODER_PROFILE,
                    TIMEBASE,
                    defaultAudioSpec,
                    defaultAudioSettings
                )
                .get()
        val defaultSampleRate = defaultConfig.sampleRate

        val higherSampleRateAudioSettings =
            defaultAudioSettings.toBuilder().setSampleRate(defaultSampleRate * 2).build()

        val higherSampleRateConfig =
            AudioEncoderConfigDefaultResolver(
                    MIME_TYPE,
                    ENCODER_PROFILE,
                    TIMEBASE,
                    defaultAudioSpec,
                    higherSampleRateAudioSettings
                )
                .get()

        assertThat(higherSampleRateConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }

    @Test
    fun bitrateRangeInVideoSpecClampsBitrate() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )
        val defaultConfig =
            AudioEncoderConfigDefaultResolver(
                    MIME_TYPE,
                    ENCODER_PROFILE,
                    TIMEBASE,
                    defaultAudioSpec,
                    defaultAudioSettings
                )
                .get()
        val defaultBitrate = defaultConfig.bitrate

        // Create audio spec with limit 20% higher than default.
        val higherBitrate = (defaultBitrate * 1.2).toInt()
        val higherAudioSpec =
            AudioSpec.builder().setBitrate(Range(higherBitrate, Int.MAX_VALUE)).build()

        // Create audio spec with limit 20% lower than default.
        val lowerBitrate = (defaultBitrate * 0.8).toInt()
        val lowerAudioSpec = AudioSpec.builder().setBitrate(Range(0, lowerBitrate)).build()

        assertThat(
                AudioEncoderConfigDefaultResolver(
                        MIME_TYPE,
                        ENCODER_PROFILE,
                        TIMEBASE,
                        higherAudioSpec,
                        defaultAudioSettings
                    )
                    .get()
                    .bitrate
            )
            .isEqualTo(higherBitrate)

        assertThat(
                AudioEncoderConfigDefaultResolver(
                        MIME_TYPE,
                        ENCODER_PROFILE,
                        TIMEBASE,
                        lowerAudioSpec,
                        defaultAudioSettings
                    )
                    .get()
                    .bitrate
            )
            .isEqualTo(lowerBitrate)
    }
}
