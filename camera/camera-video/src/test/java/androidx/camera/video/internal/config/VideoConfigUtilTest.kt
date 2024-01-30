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

package androidx.camera.video.internal.config

import android.media.EncoderProfiles
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Build
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.testing.impl.EncoderProfilesUtil
import androidx.camera.video.MediaSpec
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VideoConfigUtilTest {

    @Test
    fun videoMimeInfo_resolvesFromDynamicRange_noCompatibleProfile() {
        val videoMimeInfo = VideoConfigUtil.resolveVideoMimeInfo(
            createMediaSpec(),
            DynamicRange.HLG_10_BIT,
            createFakeEncoderProfiles(listOf(VIDEO_PROFILE_DEFAULT))
        )

        assertThat(videoMimeInfo.compatibleVideoProfile).isNull()
        assertThat(videoMimeInfo.mimeType).isEqualTo(MediaFormat.MIMETYPE_VIDEO_HEVC)
    }

    @Test
    fun videoMimeInfo_resolvesFromDynamicRange_withCompatibleProfile() {
        val videoMimeInfo = VideoConfigUtil.resolveVideoMimeInfo(
            createMediaSpec(outputFormat = MediaSpec.OUTPUT_FORMAT_AUTO),
            DynamicRange.HLG_10_BIT,
            createFakeEncoderProfiles(listOf(
                VIDEO_PROFILE_DEFAULT,
                VIDEO_PROFILE_HEVC_HLG10,
                VIDEO_PROFILE_DOLBY_VISION_10_BIT
            ))
        )

        val compatibleProfile = videoMimeInfo.compatibleVideoProfile
        assertThat(videoMimeInfo.compatibleVideoProfile).isEqualTo(VIDEO_PROFILE_HEVC_HLG10)
        assertThat(videoMimeInfo.mimeType).isEqualTo(compatibleProfile!!.mediaType)
    }

    @Test
    fun videoMimeInfo_ignoresVideoProfiles_withIncompatibleOutputFormat() {
        val videoMimeInfo = VideoConfigUtil.resolveVideoMimeInfo(
            createMediaSpec(outputFormat = MediaSpec.OUTPUT_FORMAT_MPEG_4),
            DynamicRange.HLG_10_BIT,
            createFakeEncoderProfiles(listOf(
                VIDEO_PROFILE_DEFAULT,
                VIDEO_PROFILE_VP9_HLG10 // VP9 uses WebM format
            ))
        )

        assertThat(videoMimeInfo.compatibleVideoProfile).isNull()
    }

    @Test
    fun videoMimeInfo_ignoresVideoProfiles_withIncompatibleDynamicRange() {
        val videoMimeInfo = VideoConfigUtil.resolveVideoMimeInfo(
            createMediaSpec(),
            DynamicRange.DOLBY_VISION_10_BIT,
            createFakeEncoderProfiles(listOf(
                VIDEO_PROFILE_DEFAULT,
                VIDEO_PROFILE_DOLBY_VISION_8_BIT // Dolby vision 8-bit, when 10-bit is passed in
            ))
        )

        assertThat(videoMimeInfo.compatibleVideoProfile).isNull()
        assertThat(videoMimeInfo.mimeType).isEqualTo(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION)
    }

    @Test
    fun videoMimeInfo_resolvesFromMatchingMime() {
        val expectedProfileMap = mapOf(
            DynamicRange.SDR to VIDEO_PROFILE_DEFAULT,
            DynamicRange.HLG_10_BIT to VIDEO_PROFILE_HEVC_HLG10,
            DynamicRange.HDR10_10_BIT to VIDEO_PROFILE_HEVC_HDR10,
            DynamicRange.HDR10_PLUS_10_BIT to VIDEO_PROFILE_HEVC_HDR10_PLUS,
            DynamicRange.DOLBY_VISION_10_BIT to VIDEO_PROFILE_DOLBY_VISION_10_BIT,
            DynamicRange.DOLBY_VISION_8_BIT to VIDEO_PROFILE_DOLBY_VISION_8_BIT
        )
        val encoderProfiles = createFakeEncoderProfiles(expectedProfileMap.values.toList())

        for (dynamicRangeAndExpectedProfile in expectedProfileMap) {
            val dynamicRange = dynamicRangeAndExpectedProfile.key

            val videoMimeInfo = VideoConfigUtil.resolveVideoMimeInfo(
                createMediaSpec(),
                dynamicRange,
                encoderProfiles
            )

            val expectedVideoProfile = dynamicRangeAndExpectedProfile.value
            assertThat(videoMimeInfo.compatibleVideoProfile).isEqualTo(expectedVideoProfile)
            assertThat(videoMimeInfo.mimeType).isEqualTo(expectedVideoProfile.mediaType)
        }
    }

    companion object {
        fun createFakeEncoderProfiles(videoProfileProxies: List<VideoProfileProxy>) =
            VideoValidatedEncoderProfilesProxy.create(
                EncoderProfilesUtil.DEFAULT_DURATION,
                EncoderProfilesUtil.DEFAULT_OUTPUT_FORMAT,
                emptyList(),
                videoProfileProxies
            )

        fun createMediaSpec(outputFormat: Int = MediaSpec.OUTPUT_FORMAT_AUTO) =
            MediaSpec.builder().apply {
                setOutputFormat(outputFormat)
            }.build()

        private const val DEFAULT_VIDEO_WIDTH = 1920
        private const val DEFAULT_VIDEO_HEIGHT = 1080

        val VIDEO_PROFILE_DEFAULT = EncoderProfilesUtil.createFakeVideoProfileProxy(
            DEFAULT_VIDEO_WIDTH,
            DEFAULT_VIDEO_HEIGHT
        )

        val VIDEO_PROFILE_HEVC_HLG10 = EncoderProfilesUtil.createFakeVideoProfileProxy(
            DEFAULT_VIDEO_WIDTH,
            DEFAULT_VIDEO_HEIGHT,
            videoCodec = MediaRecorder.VideoEncoder.HEVC,
            videoMediaType = MediaFormat.MIMETYPE_VIDEO_HEVC,
            videoHdrFormat = EncoderProfiles.VideoProfile.HDR_HLG,
            videoBitDepth = VideoProfileProxy.BIT_DEPTH_10
        )

        val VIDEO_PROFILE_HEVC_HDR10 = EncoderProfilesUtil.createFakeVideoProfileProxy(
            DEFAULT_VIDEO_WIDTH,
            DEFAULT_VIDEO_HEIGHT,
            videoCodec = MediaRecorder.VideoEncoder.HEVC,
            videoMediaType = MediaFormat.MIMETYPE_VIDEO_HEVC,
            videoHdrFormat = EncoderProfiles.VideoProfile.HDR_HDR10,
            videoBitDepth = VideoProfileProxy.BIT_DEPTH_10
        )

        val VIDEO_PROFILE_HEVC_HDR10_PLUS = EncoderProfilesUtil.createFakeVideoProfileProxy(
            DEFAULT_VIDEO_WIDTH,
            DEFAULT_VIDEO_HEIGHT,
            videoCodec = MediaRecorder.VideoEncoder.HEVC,
            videoMediaType = MediaFormat.MIMETYPE_VIDEO_HEVC,
            videoHdrFormat = EncoderProfiles.VideoProfile.HDR_HDR10PLUS,
            videoBitDepth = VideoProfileProxy.BIT_DEPTH_10
        )

        val VIDEO_PROFILE_DOLBY_VISION_10_BIT = EncoderProfilesUtil.createFakeVideoProfileProxy(
            DEFAULT_VIDEO_WIDTH,
            DEFAULT_VIDEO_HEIGHT,
            videoCodec = MediaRecorder.VideoEncoder.DOLBY_VISION,
            videoMediaType = MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION,
            videoHdrFormat = EncoderProfiles.VideoProfile.HDR_DOLBY_VISION,
            videoBitDepth = VideoProfileProxy.BIT_DEPTH_10
        )

        val VIDEO_PROFILE_DOLBY_VISION_8_BIT = EncoderProfilesUtil.createFakeVideoProfileProxy(
            DEFAULT_VIDEO_WIDTH,
            DEFAULT_VIDEO_HEIGHT,
            videoCodec = MediaRecorder.VideoEncoder.DOLBY_VISION,
            videoMediaType = MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION,
            videoHdrFormat = EncoderProfiles.VideoProfile.HDR_DOLBY_VISION,
            videoBitDepth = VideoProfileProxy.BIT_DEPTH_8
        )

        val VIDEO_PROFILE_VP9_HLG10 = EncoderProfilesUtil.createFakeVideoProfileProxy(
            DEFAULT_VIDEO_WIDTH,
            DEFAULT_VIDEO_HEIGHT,
            videoCodec = MediaRecorder.VideoEncoder.VP9,
            videoMediaType = MediaFormat.MIMETYPE_VIDEO_VP9,
            videoHdrFormat = EncoderProfiles.VideoProfile.HDR_HLG,
            videoBitDepth = VideoProfileProxy.BIT_DEPTH_10
        )
    }
}
