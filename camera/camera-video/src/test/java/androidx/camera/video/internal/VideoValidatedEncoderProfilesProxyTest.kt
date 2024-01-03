/*
 * Copyright 2023 The Android Open Source Project
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

@file:RequiresApi(21)

package androidx.camera.video.internal

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.testing.impl.EncoderProfilesUtil
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val DEFAULT_WIDTH = 1920
private const val DEFAULT_HEIGHT = 1080

private val DEFAULT_VIDEO_PROFILE by lazy {
    VideoProfileProxy.create(
        EncoderProfilesUtil.DEFAULT_VIDEO_CODEC,
        EncoderProfilesUtil.DEFAULT_VIDEO_MEDIA_TYPE,
        EncoderProfilesUtil.DEFAULT_VIDEO_BITRATE,
        EncoderProfilesUtil.DEFAULT_VIDEO_FRAME_RATE,
        DEFAULT_WIDTH,
        DEFAULT_HEIGHT,
        EncoderProfilesUtil.DEFAULT_VIDEO_PROFILE,
        EncoderProfilesUtil.DEFAULT_VIDEO_BIT_DEPTH,
        EncoderProfilesUtil.DEFAULT_VIDEO_CHROMA_SUBSAMPLING,
        EncoderProfilesUtil.DEFAULT_VIDEO_HDR_FORMAT
    )
}
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VideoValidatedEncoderProfilesProxyTest {

    @Test
    fun createFromEncoderProfilesProxy() {
        val profiles = EncoderProfilesUtil.PROFILES_1080P
        val validatedProfiles = VideoValidatedEncoderProfilesProxy.from(profiles)

        assertThat(validatedProfiles.recommendedFileFormat)
            .isEqualTo(profiles.recommendedFileFormat)
        assertThat(validatedProfiles.defaultDurationSeconds)
            .isEqualTo(profiles.defaultDurationSeconds)
        assertThat(validatedProfiles.audioProfiles.size).isEqualTo(profiles.audioProfiles.size)
        assertThat(validatedProfiles.videoProfiles.size).isEqualTo(profiles.videoProfiles.size)
        assertThat(validatedProfiles.defaultAudioProfile).isNotNull()
        assertThat(validatedProfiles.defaultVideoProfile).isNotNull()
        assertThat(validatedProfiles.audioProfiles[0].codec)
            .isEqualTo(profiles.audioProfiles[0].codec)
        assertThat(validatedProfiles.audioProfiles[0].mediaType)
            .isEqualTo(profiles.audioProfiles[0].mediaType)
        assertThat(validatedProfiles.audioProfiles[0].bitrate)
            .isEqualTo(profiles.audioProfiles[0].bitrate)
        assertThat(validatedProfiles.audioProfiles[0].sampleRate)
            .isEqualTo(profiles.audioProfiles[0].sampleRate)
        assertThat(validatedProfiles.audioProfiles[0].channels)
            .isEqualTo(profiles.audioProfiles[0].channels)
        assertThat(validatedProfiles.audioProfiles[0].profile)
            .isEqualTo(profiles.audioProfiles[0].profile)
        assertThat(validatedProfiles.videoProfiles[0].codec)
            .isEqualTo(profiles.videoProfiles[0].codec)
        assertThat(validatedProfiles.videoProfiles[0].mediaType)
            .isEqualTo(profiles.videoProfiles[0].mediaType)
        assertThat(validatedProfiles.videoProfiles[0].bitrate)
            .isEqualTo(profiles.videoProfiles[0].bitrate)
        assertThat(validatedProfiles.videoProfiles[0].frameRate)
            .isEqualTo(profiles.videoProfiles[0].frameRate)
        assertThat(validatedProfiles.videoProfiles[0].width)
            .isEqualTo(profiles.videoProfiles[0].width)
        assertThat(validatedProfiles.videoProfiles[0].height)
            .isEqualTo(profiles.videoProfiles[0].height)
        assertThat(validatedProfiles.videoProfiles[0].profile)
            .isEqualTo(profiles.videoProfiles[0].profile)
        assertThat(validatedProfiles.videoProfiles[0].bitDepth)
            .isEqualTo(profiles.videoProfiles[0].bitDepth)
        assertThat(validatedProfiles.videoProfiles[0].chromaSubsampling)
            .isEqualTo(profiles.videoProfiles[0].chromaSubsampling)
        assertThat(validatedProfiles.videoProfiles[0].hdrFormat)
            .isEqualTo(profiles.videoProfiles[0].hdrFormat)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_throwsException_whenVideoProfilesIsEmpty() {
        VideoValidatedEncoderProfilesProxy.create(
            EncoderProfilesUtil.DEFAULT_DURATION,
            EncoderProfilesUtil.DEFAULT_OUTPUT_FORMAT,
            emptyList(),
            emptyList()
        )
    }

    @Test
    fun create_withEmptyAudioProfiles() {
        val validatedProfiles = VideoValidatedEncoderProfilesProxy.create(
            EncoderProfilesUtil.DEFAULT_DURATION,
            EncoderProfilesUtil.DEFAULT_OUTPUT_FORMAT,
            emptyList(),
            listOf(DEFAULT_VIDEO_PROFILE)
        )
        assertThat(validatedProfiles.defaultAudioProfile).isNull()
    }
}
