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

import android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC
import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import android.media.MediaFormat.MIMETYPE_AUDIO_VORBIS
import android.util.Rational
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeAudioProfileProxy
import androidx.camera.video.AudioSpec.Companion.CHANNEL_COUNT_MONO
import androidx.camera.video.AudioSpec.Companion.SOURCE_FORMAT_PCM_16BIT
import androidx.camera.video.MediaConstants.MIME_TYPE_UNSPECIFIED
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
class AudioConfigUtilTest {

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

        assertThat(result.captureRate).isEqualTo(24000)
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

        assertThat(result.captureRate).isEqualTo(48000)
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
}
