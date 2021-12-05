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
import android.util.Range
import androidx.camera.video.AudioSpec
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 21)
class AudioEncoderConfigDefaultResolverTest {

    companion object {
        const val MIME_TYPE = "audio/mp4a-latm"
        const val ENCODER_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC
    }

    private val defaultAudioSpec = AudioSpec.builder().build()
    private val defaultAudioSourceSettings =
        AudioSourceSettingsDefaultResolver(defaultAudioSpec).get()

    @Test
    fun defaultAudioSpecProducesValidSettings() {
        val resolvedAudioConfig = AudioEncoderConfigDefaultResolver(
            MIME_TYPE,
            ENCODER_PROFILE,
            defaultAudioSpec,
            defaultAudioSourceSettings
        ).get()

        assertThat(resolvedAudioConfig.mimeType).isEqualTo(MIME_TYPE)
        assertThat(resolvedAudioConfig.profile).isEqualTo(ENCODER_PROFILE)
        assertThat(resolvedAudioConfig.channelCount)
            .isEqualTo(defaultAudioSourceSettings.channelCount)
        assertThat(resolvedAudioConfig.sampleRate).isEqualTo(defaultAudioSourceSettings.sampleRate)
        assertThat(resolvedAudioConfig.bitrate).isGreaterThan(0)
    }

    @Test
    fun increasedChannelCountIncreasesBitrate() {
        // Get default channel count
        val defaultConfig =
            AudioEncoderConfigDefaultResolver(
                MIME_TYPE,
                ENCODER_PROFILE,
                defaultAudioSpec,
                defaultAudioSourceSettings
            ).get()
        val defaultChannelCount = defaultConfig.channelCount

        val higherChannelCountSourceSettings =
            defaultAudioSourceSettings.toBuilder().setChannelCount(defaultChannelCount * 2).build()

        val higherChannelCountConfig = AudioEncoderConfigDefaultResolver(
            MIME_TYPE,
            ENCODER_PROFILE,
            defaultAudioSpec,
            higherChannelCountSourceSettings
        ).get()

        assertThat(higherChannelCountConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }

    @Test
    fun increasedSampleRateIncreasesBitrate() {
        // Get default sample rate
        val defaultConfig =
            AudioEncoderConfigDefaultResolver(
                MIME_TYPE,
                ENCODER_PROFILE,
                defaultAudioSpec,
                defaultAudioSourceSettings
            ).get()
        val defaultSampleRate = defaultConfig.sampleRate

        val higherSampleRateSourceSettings =
            defaultAudioSourceSettings.toBuilder().setSampleRate(defaultSampleRate * 2).build()

        val higherSampleRateConfig = AudioEncoderConfigDefaultResolver(
            MIME_TYPE,
            ENCODER_PROFILE,
            defaultAudioSpec,
            higherSampleRateSourceSettings
        ).get()

        assertThat(higherSampleRateConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }

    @Test
    fun bitrateRangeInVideoSpecClampsBitrate() {
        val defaultConfig =
            AudioEncoderConfigDefaultResolver(
                MIME_TYPE,
                ENCODER_PROFILE,
                defaultAudioSpec,
                defaultAudioSourceSettings
            ).get()
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
                higherAudioSpec,
                defaultAudioSourceSettings
            ).get().bitrate
        ).isEqualTo(higherBitrate)

        assertThat(
            AudioEncoderConfigDefaultResolver(
                MIME_TYPE,
                ENCODER_PROFILE,
                lowerAudioSpec,
                defaultAudioSourceSettings
            ).get().bitrate
        ).isEqualTo(lowerBitrate)
    }
}