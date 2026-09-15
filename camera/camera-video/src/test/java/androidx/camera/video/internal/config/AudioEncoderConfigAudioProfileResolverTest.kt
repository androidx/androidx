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
import androidx.camera.core.impl.Timebase
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeAudioProfileProxy
import androidx.camera.video.AudioSpec
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class AudioEncoderConfigAudioProfileResolverTest {

    companion object {
        private const val ENCODER_PROFILE = AACObjectLC
        private val TIMEBASE = Timebase.UPTIME
        private val DEFAULT_AUDIO_SPEC = AudioSpec.builder().build()
    }

    @Test
    fun defaultAudioProfile_producesValidSettings() {
        val audioProfile = createFakeAudioProfileProxy(profile = ENCODER_PROFILE)
        val audioSettings = AudioConfigUtil.resolveAudioSettings(DEFAULT_AUDIO_SPEC, audioProfile)

        val config =
            AudioEncoderConfigAudioProfileResolver(
                    audioProfile.mediaType,
                    audioProfile.profile,
                    TIMEBASE,
                    DEFAULT_AUDIO_SPEC,
                    audioSettings,
                    audioProfile,
                )
                .get()

        assertThat(config.mimeType).isEqualTo(audioProfile.mediaType)
        assertThat(config.bitrate).isEqualTo(audioProfile.bitrate)
        assertThat(config.captureSampleRate).isEqualTo(audioProfile.sampleRate)
        assertThat(config.encodeSampleRate).isEqualTo(audioProfile.sampleRate)
        assertThat(config.channelCount).isEqualTo(audioProfile.channels)
    }

    @Test
    fun increasedChannelCountIncreasesBitrate() {
        val profile = createFakeAudioProfileProxy(profile = ENCODER_PROFILE)
        val defaultAudioSettings = AudioConfigUtil.resolveAudioSettings(DEFAULT_AUDIO_SPEC, profile)
        val defaultConfig =
            AudioEncoderConfigAudioProfileResolver(
                    profile.mediaType,
                    profile.profile,
                    TIMEBASE,
                    DEFAULT_AUDIO_SPEC,
                    defaultAudioSettings,
                    profile,
                )
                .get()
        val defaultChannelCount = defaultConfig.channelCount

        val higherChannelCountAudioSettings =
            defaultAudioSettings.toBuilder().setChannelCount(defaultChannelCount * 2).build()

        val higherChannelCountConfig =
            AudioEncoderConfigAudioProfileResolver(
                    profile.mediaType,
                    profile.profile,
                    TIMEBASE,
                    DEFAULT_AUDIO_SPEC,
                    higherChannelCountAudioSettings,
                    profile,
                )
                .get()

        assertThat(higherChannelCountConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }

    @Test
    fun increasedSampleRateIncreasesBitrate() {
        val profile = createFakeAudioProfileProxy(profile = ENCODER_PROFILE)
        val defaultAudioSettings = AudioConfigUtil.resolveAudioSettings(DEFAULT_AUDIO_SPEC, profile)
        val defaultConfig =
            AudioEncoderConfigAudioProfileResolver(
                    profile.mediaType,
                    profile.profile,
                    TIMEBASE,
                    DEFAULT_AUDIO_SPEC,
                    defaultAudioSettings,
                    profile,
                )
                .get()
        val defaultSampleRate = defaultConfig.captureSampleRate

        val higherSampleRateAudioSettings =
            defaultAudioSettings
                .toBuilder()
                .setCaptureSampleRate(defaultSampleRate * 2)
                .setEncodeSampleRate(defaultSampleRate * 2)
                .build()

        val higherSampleRateConfig =
            AudioEncoderConfigAudioProfileResolver(
                    profile.mediaType,
                    profile.profile,
                    TIMEBASE,
                    DEFAULT_AUDIO_SPEC,
                    higherSampleRateAudioSettings,
                    profile,
                )
                .get()

        assertThat(higherSampleRateConfig.bitrate).isGreaterThan(defaultConfig.bitrate)
    }
}
