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

package androidx.camera.video.internal.config

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC
import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import android.media.MediaFormat.MIMETYPE_AUDIO_VORBIS
import android.util.Rational
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeAudioProfileProxy
import androidx.camera.video.AudioSpec
import androidx.camera.video.AudioSpec.Companion.CHANNEL_COUNT_MONO
import androidx.camera.video.AudioSpec.Companion.SOURCE_FORMAT_PCM_16BIT
import androidx.camera.video.MediaConstants.MIME_TYPE_UNSPECIFIED
import androidx.camera.video.internal.encoder.EncoderConfig.CODEC_PROFILE_NONE
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS], shadows = [AudioConfigUtilTest.ShadowAudioRecord::class])
class AudioConfigUtilTest {

    @Before
    fun setUp() {
        ShadowAudioRecord.reset()
    }

    @Test
    fun resolveAudioSettings_unsupportedSampleRate_fallsBack() {
        // Arrange
        val audioSpec = AudioSpec.builder().setSampleRate(44100).build()

        // Only support 48000
        ShadowAudioRecord.setSupportedSettings(listOf(ShadowAudioRecord.Setting(48000, 1)))

        // Act
        val settings = AudioConfigUtil.resolveAudioSettings(audioSpec)

        // Assert
        assertThat(settings.captureSampleRate).isEqualTo(48000)
    }

    @Test
    fun resolveAudioSettings_unsupportedChannelCount_fallsBack() {
        // Arrange
        val audioSpec = AudioSpec.builder().setChannelCount(2).build()

        // Only support Mono
        ShadowAudioRecord.setSupportedSettings(listOf(ShadowAudioRecord.Setting(48000, 1)))

        // Act
        val settings = AudioConfigUtil.resolveAudioSettings(audioSpec)

        // Assert
        assertThat(settings.channelCount).isEqualTo(1)
    }

    @Test
    fun resolveAudioSettings_unsupportedSampleRateAndChannelCount_fallsBack() {
        // Arrange
        // Initial request: Stereo, 44100Hz
        val audioSpec = AudioSpec.builder().setChannelCount(2).setSampleRate(44100).build()

        // ONLY support:
        // 1. Mono (1 channel) with 48000Hz
        // 2. Mono (1 channel) with 44100Hz
        // (Stereo is not supported at all)
        ShadowAudioRecord.setSupportedSettings(
            listOf(ShadowAudioRecord.Setting(48000, 1), ShadowAudioRecord.Setting(44100, 1))
        )

        // Act
        val settings = AudioConfigUtil.resolveAudioSettings(audioSpec)

        // Assert:
        // 1. First it tries Stereo + 44100Hz -> Fails
        // 2. Then it tries Stereo + COMMON_SAMPLE_RATES -> All fail
        // 3. Then it falls back to Mono (default) + 44100Hz -> Succeeds
        assertThat(settings.channelCount).isEqualTo(1)
        assertThat(settings.captureSampleRate).isEqualTo(44100)
    }

    @Test
    fun resolveSampleRates_noRatio() {
        val targetEncodeSampleRate = 24000
        val captureToEncodeRatio: Rational? = null

        val result =
            AudioConfigUtil.resolveSampleRates(
                targetEncodeSampleRate,
                CHANNEL_COUNT_MONO,
                SOURCE_FORMAT_PCM_16BIT,
                captureToEncodeRatio,
            )

        assertThat(result).isNotNull()
        assertThat(result!!.captureRate).isEqualTo(24000)
        assertThat(result.encodeRate).isEqualTo(24000)
    }

    @Test
    fun resolveSampleRates_withRatio() {
        val targetEncodeSampleRate = 24000
        val captureToEncodeRatio = Rational(2, 1)

        val result =
            AudioConfigUtil.resolveSampleRates(
                targetEncodeSampleRate,
                CHANNEL_COUNT_MONO,
                SOURCE_FORMAT_PCM_16BIT,
                captureToEncodeRatio,
            )

        assertThat(result).isNotNull()
        assertThat(result!!.captureRate).isEqualTo(48000)
        assertThat(result.encodeRate).isEqualTo(24000)
    }

    @Test
    fun resolveCompatibleAudioProfile_matchesSpecificMimeAndProfile_returnsProfile() {
        // Arrange: Prepare profiles including one matching AAC
        val audioMime = MIMETYPE_AUDIO_AAC
        val matchingProfile =
            createFakeAudioProfileProxy(audioMediaType = audioMime, profile = AACObjectLC)
        val profiles =
            listOf(
                createFakeAudioProfileProxy(audioMediaType = MIMETYPE_AUDIO_VORBIS),
                matchingProfile,
            )

        // Act
        val result = AudioConfigUtil.resolveCompatibleAudioProfile(audioMime, profiles)

        // Assert
        assertThat(result).isEqualTo(matchingProfile)
    }

    @Test
    fun resolveCompatibleAudioProfile_matchesMimeButMismatchesProfile_returnsNull() {
        // Arrange: Create a profile that has the right MIME but the WRONG profile integer
        val audioMime = MIMETYPE_AUDIO_AAC
        val mismatchingProfile =
            createFakeAudioProfileProxy(audioMediaType = audioMime, profile = CODEC_PROFILE_NONE)
        val profiles = listOf(mismatchingProfile)

        // Act
        val result = AudioConfigUtil.resolveCompatibleAudioProfile(audioMime, profiles)

        // Assert: Even though MIME matches, the profile check should fail it
        assertThat(result).isNull()
    }

    @Test
    fun resolveCompatibleAudioProfile_noMatchReturnsNull() {
        // Arrange: Request a MIME type not present in the list
        val audioMime = MIMETYPE_AUDIO_VORBIS
        val profiles =
            listOf(
                createFakeAudioProfileProxy(
                    audioMediaType = MIMETYPE_AUDIO_AAC,
                    profile = AACObjectLC,
                )
            )

        // Act
        val result = AudioConfigUtil.resolveCompatibleAudioProfile(audioMime, profiles)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun resolveCompatibleAudioProfile_unspecifiedMimeReturnsFirstProfile() {
        // Arrange: Provide a list of profiles
        val audioMime = MIME_TYPE_UNSPECIFIED
        val profiles =
            listOf(
                createFakeAudioProfileProxy(audioMediaType = MIMETYPE_AUDIO_VORBIS),
                createFakeAudioProfileProxy(
                    audioMediaType = MIMETYPE_AUDIO_AAC,
                    profile = AACObjectLC,
                ),
            )

        // Act
        val result = AudioConfigUtil.resolveCompatibleAudioProfile(audioMime, profiles)

        // Assert: It should return the first available profile
        assertThat(result).isEqualTo(profiles.first())
    }

    @Implements(AudioRecord::class)
    class ShadowAudioRecord {
        data class Setting(val sampleRate: Int, val channelCount: Int)

        companion object {
            // null means "support everything" (default state)
            private var supportedConfigs: Set<Setting>? = null

            fun setSupportedSettings(configs: Collection<Setting>?) {
                supportedConfigs = configs?.toSet()
            }

            fun reset() {
                supportedConfigs = null
            }

            @Suppress("unused")
            @Implementation
            @JvmStatic
            fun getMinBufferSize(sampleRateInHz: Int, channelConfig: Int, audioFormat: Int): Int {
                val configs = supportedConfigs ?: return 1024

                val channelCount =
                    when (channelConfig) {
                        AudioFormat.CHANNEL_IN_MONO -> 1
                        AudioFormat.CHANNEL_IN_STEREO -> 2
                        else -> 0
                    }

                return if (configs.contains(Setting(sampleRateInHz, channelCount))) {
                    1024
                } else {
                    // AudioRecord returns ERROR_BAD_VALUE (-2) for unsupported settings
                    -1
                }
            }
        }
    }
}
