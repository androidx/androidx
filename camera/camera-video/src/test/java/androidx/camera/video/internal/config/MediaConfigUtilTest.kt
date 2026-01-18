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

import android.media.EncoderProfiles.VideoProfile.HDR_HLG
import android.media.EncoderProfiles.VideoProfile.HDR_NONE
import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import android.media.MediaFormat.MIMETYPE_AUDIO_AMR_NB
import android.media.MediaFormat.MIMETYPE_AUDIO_VORBIS
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.media.MediaFormat.MIMETYPE_VIDEO_HEVC
import android.media.MediaFormat.MIMETYPE_VIDEO_MPEG4
import android.media.MediaFormat.MIMETYPE_VIDEO_VP8
import android.media.MediaRecorder.VideoEncoder.H264
import android.media.MediaRecorder.VideoEncoder.HEVC
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_10
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_8
import androidx.camera.testing.impl.EncoderProfilesUtil.DEFAULT_DURATION
import androidx.camera.testing.impl.EncoderProfilesUtil.DEFAULT_OUTPUT_FORMAT
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeAudioProfileProxy
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeVideoProfileProxy
import androidx.camera.video.AudioSpec
import androidx.camera.video.MediaSpec
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_UNSPECIFIED
import androidx.camera.video.VideoSpec
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class MediaConfigUtilTest {

    companion object {
        private const val VIDEO_AVC = MIMETYPE_VIDEO_AVC
        private const val VIDEO_MPEG4 = MIMETYPE_VIDEO_MPEG4
        private const val VIDEO_HEVC = MIMETYPE_VIDEO_HEVC
        private const val VIDEO_VP8 = MIMETYPE_VIDEO_VP8
        private const val AUDIO_AAC = MIMETYPE_AUDIO_AAC
        private const val AUDIO_AMR_NB = MIMETYPE_AUDIO_AMR_NB
        private const val AUDIO_VORBIS = MIMETYPE_AUDIO_VORBIS

        private val VIDEO_PROFILE_SDR_AVC =
            createFakeVideoProfileProxy(
                RESOLUTION_1080P,
                videoCodec = H264,
                videoMediaType = VIDEO_AVC,
                videoHdrFormat = HDR_NONE,
                videoBitDepth = BIT_DEPTH_8,
            )

        private val VIDEO_PROFILE_HLG_HEVC =
            createFakeVideoProfileProxy(
                RESOLUTION_1080P,
                videoCodec = HEVC,
                videoMediaType = VIDEO_HEVC,
                videoHdrFormat = HDR_HLG,
                videoBitDepth = BIT_DEPTH_10,
            )
    }

    @After
    fun tearDown() {
        // Reset overrides to ensure a clean slate
        MediaConfigUtil.setSupportedEncoderMimeTypes(videoMimes = null, audioMimes = null)
    }

    @Test
    fun resolveMediaInfo_fullyCompatibleEncoderProfiles_returnsImmediately() {
        // Arrange
        val mediaSpec = createMediaSpec()
        val encoderProfiles = createFakeEncoderProfiles(listOf(VIDEO_PROFILE_SDR_AVC))

        // Act
        val mediaInfo = MediaConfigUtil.resolveMediaInfo(mediaSpec, SDR, encoderProfiles)

        // Assert: It should resolve using the provided profiles without needing fallback
        assertThat(mediaInfo).isNotNull()
        assertThat(mediaInfo!!.videoMimeInfo.mimeType).isEqualTo(VIDEO_AVC)
        assertThat(mediaInfo.videoMimeInfo.compatibleVideoProfile).isEqualTo(VIDEO_PROFILE_SDR_AVC)
    }

    @Test
    fun resolveMediaInfo_nullProfiles_fallsBackToRegistry() {
        // Arrange
        MediaConfigUtil.setSupportedEncoderMimeTypes(
            videoMimes = listOf(VIDEO_AVC),
            audioMimes = listOf(AUDIO_AAC),
        )
        val mediaSpec = createMediaSpec()

        // Act
        val mediaInfo = MediaConfigUtil.resolveMediaInfo(mediaSpec, SDR, null)

        // Assert: Resolved via Registry. Compatible profiles will be null.
        assertThat(mediaInfo).isNotNull()
        assertThat(mediaInfo!!.videoMimeInfo.mimeType).isEqualTo(VIDEO_AVC)
        assertThat(mediaInfo.audioMimeInfo!!.mimeType).isEqualTo(AUDIO_AAC)
        assertThat(mediaInfo.videoMimeInfo.compatibleVideoProfile).isNull()
        assertThat(mediaInfo.audioMimeInfo.compatibleAudioProfile).isNull()
    }

    @Test
    @Config(minSdk = 24) // Required for HEVC
    fun resolveMediaInfo_incompatibleProfiles_fallsBackToRegistry() {
        // Arrange: Device supports HEVC and AVC
        MediaConfigUtil.setSupportedEncoderMimeTypes(
            videoMimes = listOf(VIDEO_HEVC, VIDEO_AVC),
            audioMimes = listOf(AUDIO_AAC),
        )

        // EncoderProfile is HLG/HEVC, but we request SDR
        val hlgProfiles = createFakeEncoderProfiles(listOf(VIDEO_PROFILE_HLG_HEVC))
        val mediaSpec = createMediaSpec()

        // Act
        val mediaInfo = MediaConfigUtil.resolveMediaInfo(mediaSpec, SDR, hlgProfiles)

        // Assert: Since HLG HEVC profile isn't SDR compatible, it falls back to Registry (SDR ->
        // AVC)
        assertThat(mediaInfo).isNotNull()
        assertThat(mediaInfo!!.videoMimeInfo.mimeType).isEqualTo(VIDEO_AVC)
        assertThat(mediaInfo.videoMimeInfo.compatibleVideoProfile).isNull()
    }

    @Test
    @Config(minSdk = 24) // Required for HEVC
    fun resolveMediaInfo_hdrRequest_resolvesCorrectHdrFallback() {
        // Arrange: Device supports HEVC
        MediaConfigUtil.setSupportedEncoderMimeTypes(
            videoMimes = listOf(VIDEO_HEVC),
            audioMimes = listOf(AUDIO_AAC),
        )
        val mediaSpec = createMediaSpec()

        // Act: Request HLG with no profiles
        val mediaInfo = MediaConfigUtil.resolveMediaInfo(mediaSpec, HLG_10_BIT, null)

        // Assert: Should resolve to HEVC as it's the standard for HLG
        assertThat(mediaInfo).isNotNull()
        assertThat(mediaInfo!!.videoMimeInfo.mimeType).isEqualTo(VIDEO_HEVC)
    }

    @Test
    fun resolveMediaInfo_respectsExplicitMimeTypeInSpec() {
        // Arrange
        val explicitMime = VIDEO_MPEG4
        MediaConfigUtil.setSupportedEncoderMimeTypes(
            videoMimes = listOf(explicitMime),
            audioMimes = listOf(AUDIO_AAC),
        )
        val mediaSpec = createMediaSpec(videoMime = explicitMime)

        // Act
        val mediaInfo = MediaConfigUtil.resolveMediaInfo(mediaSpec, SDR, null)

        // Assert
        assertThat(mediaInfo).isNotNull()
        assertThat(mediaInfo!!.videoMimeInfo.mimeType).isEqualTo(explicitMime)
    }

    @Test
    fun resolveMediaInfo_returnsNull_whenDeviceDoesNotSupport() {
        // Arrange: Spec wants HEVC, but device only has AVC
        MediaConfigUtil.setSupportedEncoderMimeTypes(
            videoMimes = listOf(VIDEO_AVC),
            audioMimes = listOf(AUDIO_AAC),
        )
        val mediaSpec = createMediaSpec(videoMime = VIDEO_MPEG4)

        // Act
        val mediaInfo = MediaConfigUtil.resolveMediaInfo(mediaSpec, SDR, null)

        // Assert
        assertThat(mediaInfo).isNull()
    }

    @Test
    fun resolveMediaInfo_partialProfileMatch_resolvesCompatibleProfileForFallback() {
        // Arrange: Device supports AVC, AAC and AMR_NB
        MediaConfigUtil.setSupportedEncoderMimeTypes(
            videoMimes = listOf(VIDEO_AVC),
            audioMimes = listOf(AUDIO_AAC, AUDIO_AMR_NB),
        )

        // Spec is AUDIO_AMR_NB, but Profiles is AUDIO_AAC
        val profiles = createFakeEncoderProfiles(listOf(VIDEO_PROFILE_SDR_AVC))
        val mediaSpec = createMediaSpec(audioMime = AUDIO_AMR_NB)

        // Act
        val mediaInfo = MediaConfigUtil.resolveMediaInfo(mediaSpec, SDR, profiles)

        // Assert: Not "fully compatible" because audio mime doesn't match, but it should find and
        // attach the AVC profile to the fallback.
        assertThat(mediaInfo).isNotNull()
        assertThat(mediaInfo!!.videoMimeInfo.mimeType).isEqualTo(VIDEO_AVC)
        assertThat(mediaInfo.audioMimeInfo!!.mimeType).isEqualTo(AUDIO_AMR_NB)
        assertThat(mediaInfo.videoMimeInfo.compatibleVideoProfile).isEqualTo(VIDEO_PROFILE_SDR_AVC)
        assertThat(mediaInfo.audioMimeInfo.compatibleAudioProfile).isNull()
    }

    @Test
    fun resolveMediaInfo_videoOnlyFallback_returnsNullAudioInfo() {
        // Arrange: Device supports VP8, but VORBIS/OPUS are not supported
        MediaConfigUtil.setSupportedEncoderMimeTypes(
            videoMimes = listOf(VIDEO_VP8),
            audioMimes = listOf(AUDIO_AAC),
        )
        val mediaSpec = createMediaSpec(videoMime = VIDEO_VP8)

        // Act
        val mediaInfo = MediaConfigUtil.resolveMediaInfo(mediaSpec, SDR, null)

        // Assert: Should resolve to VP8 with no audio
        assertThat(mediaInfo).isNotNull()
        assertThat(mediaInfo!!.videoMimeInfo.mimeType).isEqualTo(VIDEO_VP8)
        assertThat(mediaInfo.audioMimeInfo).isNull()
    }

    @Test
    fun resolveMediaInfo_containerMismatch_ignoresProfiles() {
        // Arrange
        MediaConfigUtil.setSupportedEncoderMimeTypes(
            videoMimes = listOf(VIDEO_AVC, VIDEO_VP8),
            audioMimes = listOf(AUDIO_AAC, AUDIO_VORBIS),
        )
        // Profiles are for MP4 (default), but user requests WebM
        val mp4Profiles = createFakeEncoderProfiles(listOf(VIDEO_PROFILE_SDR_AVC))
        val webmSpec = createMediaSpec(outputFormat = MediaSpec.OUTPUT_FORMAT_WEBM)

        // Act
        val mediaInfo = MediaConfigUtil.resolveMediaInfo(webmSpec, SDR, mp4Profiles)

        // Assert: Should ignore the MP4 profiles and fallback to SDR WebM registry
        assertThat(mediaInfo).isNotNull()
        assertThat(mediaInfo!!.containerInfo.outputFormat).isEqualTo(MediaSpec.OUTPUT_FORMAT_WEBM)
        assertThat(mediaInfo.containerInfo.compatibleEncoderProfiles).isNull()
    }

    private fun createFakeEncoderProfiles(videoProfiles: List<VideoProfileProxy>) =
        VideoValidatedEncoderProfilesProxy.create(
            DEFAULT_DURATION,
            DEFAULT_OUTPUT_FORMAT,
            listOf(createFakeAudioProfileProxy()),
            videoProfiles,
        )

    private fun createMediaSpec(
        outputFormat: Int = OUTPUT_FORMAT_UNSPECIFIED,
        videoMime: String = VideoSpec.MIME_TYPE_UNSPECIFIED,
        audioMime: String = AudioSpec.MIME_TYPE_UNSPECIFIED,
    ) =
        MediaSpec.builder()
            .setOutputFormat(outputFormat)
            .configureVideo { it.setMimeType(videoMime) }
            .configureAudio { it.setMimeType(audioMime) }
            .build()
}
