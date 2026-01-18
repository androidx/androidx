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

package androidx.camera.video.internal

import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_720P
import android.media.MediaFormat
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_720P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeAudioProfileProxy
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeEncoderProfilesProxy
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeVideoProfileProxy
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class MimeMatchedEncoderProfilesProviderTest {

    companion object {
        private const val MIME_VIDEO_AVC = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val MIME_VIDEO_HEVC = MediaFormat.MIMETYPE_VIDEO_HEVC
        private const val MIME_AUDIO_AAC = MediaFormat.MIMETYPE_AUDIO_AAC
        private const val MIME_AUDIO_OPUS = MediaFormat.MIMETYPE_AUDIO_OPUS
    }

    @Test
    fun hasProfile_returnsTrueWhenQualityExistsInBase() {
        val baseProvider =
            FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
        val provider = MimeMatchedEncoderProfilesProvider(baseProvider)

        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
    }

    @Test
    fun getAll_filtersBothVideoAndAudioProfilesByMime() {
        // Arrange: Create a proxy with multiple video and audio codecs
        val mixedVideo =
            listOf(
                createFakeVideoProfileProxy(RESOLUTION_1080P, videoMediaType = MIME_VIDEO_AVC),
                createFakeVideoProfileProxy(RESOLUTION_1080P, videoMediaType = MIME_VIDEO_HEVC),
            )
        val mixedAudio =
            listOf(
                createFakeAudioProfileProxy(audioMediaType = MIME_AUDIO_AAC),
                createFakeAudioProfileProxy(audioMediaType = MIME_AUDIO_OPUS),
            )
        val mixedProfilesProxy =
            createFakeEncoderProfilesProxy(videoProfiles = mixedVideo, audioProfiles = mixedAudio)
        val baseProvider =
            FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, mixedProfilesProxy).build()

        // Act: Filter for HEVC and OPUS
        val provider =
            MimeMatchedEncoderProfilesProvider(
                baseProvider,
                videoMime = MIME_VIDEO_HEVC,
                audioMime = MIME_AUDIO_OPUS,
            )
        val result = provider.getAll(QUALITY_1080P)

        // Assert
        assertThat(result).isNotNull()
        assertThat(result!!.videoProfiles).hasSize(1)
        assertThat(result.videoProfiles[0].mediaType).isEqualTo(MIME_VIDEO_HEVC)
        assertThat(result.audioProfiles).hasSize(1)
        assertThat(result.audioProfiles[0].mediaType).isEqualTo(MIME_AUDIO_OPUS)
    }

    @Test
    fun getAll_returnsNullProfilesIfNoMimesMatch() {
        // Arrange: Base provider only has AVC and AAC
        val baseProvider =
            FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()

        // Act: Filter for HEVC and OPUS (which don't exist in PROFILES_1080P)
        val provider =
            MimeMatchedEncoderProfilesProvider(
                baseProvider,
                videoMime = MIME_VIDEO_HEVC,
                audioMime = MIME_AUDIO_OPUS,
            )
        val result = provider.getAll(QUALITY_1080P)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun getAll_returnsNullWhenBaseReturnsNull() {
        val baseProvider = FakeEncoderProfilesProvider.Builder().build()
        val provider = MimeMatchedEncoderProfilesProvider(baseProvider)

        assertThat(provider.getAll(QUALITY_720P)).isNull()
    }

    @Test
    fun resultsAreCachedInternally() {
        val baseProvider =
            FakeEncoderProfilesProvider.Builder().add(QUALITY_720P, PROFILES_720P).build()
        val provider = MimeMatchedEncoderProfilesProvider(baseProvider)

        val firstResult = provider.getAll(QUALITY_720P)
        val secondResult = provider.getAll(QUALITY_720P)

        // Assert referential equality
        assertThat(firstResult).isSameInstanceAs(secondResult)
    }
}
