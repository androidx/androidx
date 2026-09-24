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

package androidx.camera.video.internal.config

import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC
import android.media.MediaFormat
import androidx.camera.core.impl.Timebase
import androidx.camera.video.AudioSpec
import androidx.camera.video.internal.audio.AudioSettings
import androidx.camera.video.internal.encoder.EncoderConfig.CODEC_PROFILE_NONE
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class AudioEncoderConfigDefaultResolverTest {

    companion object {
        const val MIME_TYPE = "audio/mp4a-latm"
        const val ENCODER_PROFILE = AACObjectLC
        val TIMEBASE = Timebase.UPTIME
    }

    private val defaultAudioSpec = AudioSpec.builder().build()
    private val defaultAudioSettings = AudioConfigUtil.resolveAudioSettings(defaultAudioSpec)

    @Test
    fun defaultAudioSpecProducesValidSettings() {
        val resolvedAudioConfig =
            AudioEncoderConfigDefaultResolver(
                    MIME_TYPE,
                    ENCODER_PROFILE,
                    TIMEBASE,
                    defaultAudioSpec,
                    defaultAudioSettings,
                )
                .get()

        assertThat(resolvedAudioConfig.mimeType).isEqualTo(MIME_TYPE)
        assertThat(resolvedAudioConfig.profile).isEqualTo(ENCODER_PROFILE)
        assertThat(resolvedAudioConfig.channelCount).isEqualTo(defaultAudioSettings.channelCount)
        assertThat(resolvedAudioConfig.captureSampleRate)
            .isEqualTo(defaultAudioSettings.captureSampleRate)
        assertThat(resolvedAudioConfig.encodeSampleRate)
            .isEqualTo(defaultAudioSettings.encodeSampleRate)
        assertThat(resolvedAudioConfig.bitrate).isGreaterThan(0)
    }

    @Test
    fun increasedChannelCountIncreasesBitrate() {
        val defaultConfig =
            AudioEncoderConfigDefaultResolver(
                    MIME_TYPE,
                    ENCODER_PROFILE,
                    TIMEBASE,
                    defaultAudioSpec,
                    defaultAudioSettings,
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
                    higherChannelCountAudioSettings,
                )
                .get()

        assertThat(higherChannelCountConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }

    @Test
    fun increasedSampleRateIncreasesBitrate() {
        val defaultConfig =
            AudioEncoderConfigDefaultResolver(
                    MIME_TYPE,
                    ENCODER_PROFILE,
                    TIMEBASE,
                    defaultAudioSpec,
                    defaultAudioSettings,
                )
                .get()
        val defaultSampleRate = defaultConfig.captureSampleRate

        val higherSampleRate = defaultSampleRate * 2
        val higherSampleRateAudioSettings =
            defaultAudioSettings
                .toBuilder()
                .setCaptureSampleRate(higherSampleRate)
                .setEncodeSampleRate(higherSampleRate)
                .build()

        val higherSampleRateConfig =
            AudioEncoderConfigDefaultResolver(
                    MIME_TYPE,
                    ENCODER_PROFILE,
                    TIMEBASE,
                    defaultAudioSpec,
                    higherSampleRateAudioSettings,
                )
                .get()

        assertThat(higherSampleRateConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }

    @Test
    fun fallbackBitrate_amrNb_resolvesTo12200() {
        val audioSpec = AudioSpec.builder().build()
        val audioSettings =
            AudioSettings.builder()
                .setAudioSource(AudioConfigUtil.AUDIO_SOURCE_DEFAULT)
                .setAudioFormat(AudioConfigUtil.AUDIO_SOURCE_FORMAT_DEFAULT)
                .setChannelCount(1)
                .setCaptureSampleRate(8000)
                .setEncodeSampleRate(8000)
                .build()

        val config =
            AudioEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_AUDIO_AMR_NB,
                    audioProfile = CODEC_PROFILE_NONE,
                    inputTimeBase = TIMEBASE,
                    audioSpec = audioSpec,
                    audioSettings = audioSettings,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(12_200)
    }

    @Test
    fun fallbackBitrate_amrWb_resolvesTo23850() {
        val audioSpec = AudioSpec.builder().build()
        val audioSettings =
            AudioSettings.builder()
                .setAudioSource(AudioConfigUtil.AUDIO_SOURCE_DEFAULT)
                .setAudioFormat(AudioConfigUtil.AUDIO_SOURCE_FORMAT_DEFAULT)
                .setChannelCount(1)
                .setCaptureSampleRate(16000)
                .setEncodeSampleRate(16000)
                .build()

        val config =
            AudioEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_AUDIO_AMR_WB,
                    audioProfile = CODEC_PROFILE_NONE,
                    inputTimeBase = TIMEBASE,
                    audioSpec = audioSpec,
                    audioSettings = audioSettings,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(23_850)
    }

    @Test
    fun fallbackBitrate_aac_scalesBasedOnBase() {
        val audioSpec = AudioSpec.builder().build()
        // Stereo, 48 kHz
        val audioSettings =
            AudioSettings.builder()
                .setAudioSource(AudioConfigUtil.AUDIO_SOURCE_DEFAULT)
                .setAudioFormat(AudioConfigUtil.AUDIO_SOURCE_FORMAT_DEFAULT)
                .setChannelCount(2)
                .setCaptureSampleRate(48000)
                .setEncodeSampleRate(48000)
                .build()

        val config =
            AudioEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_AUDIO_AAC,
                    audioProfile = AACObjectLC,
                    inputTimeBase = TIMEBASE,
                    audioSpec = audioSpec,
                    audioSettings = audioSettings,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(156_000)
    }

    @Test
    fun explicitBitrate_isRespected() {
        val audioSpec = AudioSpec.builder().setBitrate(64000).build()
        val audioSettings =
            AudioSettings.builder()
                .setAudioSource(AudioConfigUtil.AUDIO_SOURCE_DEFAULT)
                .setAudioFormat(AudioConfigUtil.AUDIO_SOURCE_FORMAT_DEFAULT)
                .setChannelCount(1)
                .setCaptureSampleRate(8000)
                .setEncodeSampleRate(8000)
                .build()

        val config =
            AudioEncoderConfigDefaultResolver(
                    mimeType = MediaFormat.MIMETYPE_AUDIO_AMR_NB,
                    audioProfile = CODEC_PROFILE_NONE,
                    inputTimeBase = TIMEBASE,
                    audioSpec = audioSpec,
                    audioSettings = audioSettings,
                )
                .get()

        assertThat(config.bitrate).isEqualTo(64_000)
    }
}
