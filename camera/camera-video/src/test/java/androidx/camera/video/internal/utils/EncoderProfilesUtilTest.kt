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

package androidx.camera.video.internal.utils

import android.media.EncoderProfiles.VideoProfile.HDR_DOLBY_VISION
import android.media.EncoderProfiles.VideoProfile.HDR_HDR10
import android.media.EncoderProfiles.VideoProfile.HDR_HDR10PLUS
import android.media.EncoderProfiles.VideoProfile.HDR_HLG
import android.media.EncoderProfiles.VideoProfile.HDR_NONE
import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaFormat
import android.media.MediaRecorder
import android.util.Range
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT
import androidx.camera.core.DynamicRange.HDR10_10_BIT
import androidx.camera.core.DynamicRange.HDR10_PLUS_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_10
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_8
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeVideoProfileProxy
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import androidx.camera.video.internal.utils.EncoderProfilesUtil.validateOrAdapt
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class EncoderProfilesUtilTest {

    private val sdrProfile: VideoProfileProxy =
        createFakeVideoProfileProxy(RESOLUTION_1080P, bitrate = 8_000_000)
    private val hardwareEncoderFinder = VideoEncoderInfo.Finder { FakeVideoEncoderInfo() }
    private val softwareEncoderFinder = VideoEncoderInfo.Finder {
        FakeVideoEncoderInfo(isHardwareAccelerated = false)
    }

    @Test
    fun deriveHdrVideoProfile_hlg() {
        verifyDerivedHdrVideoProfile(
            dynamicRange = HLG_10_BIT,
            expectedHdrFormat = HDR_HLG,
            expectedCodec = MediaRecorder.VideoEncoder.HEVC,
            expectedMediaType = MediaFormat.MIMETYPE_VIDEO_HEVC,
            expectedProfile = CodecProfileLevel.HEVCProfileMain10,
        )
    }

    @Test
    fun deriveHdrVideoProfile_hdr10() {
        verifyDerivedHdrVideoProfile(
            dynamicRange = HDR10_10_BIT,
            expectedHdrFormat = HDR_HDR10,
            expectedCodec = MediaRecorder.VideoEncoder.HEVC,
            expectedMediaType = MediaFormat.MIMETYPE_VIDEO_HEVC,
            expectedProfile = CodecProfileLevel.HEVCProfileMain10HDR10,
        )
    }

    @Test
    fun deriveHdrVideoProfile_hdr10Plus() {
        verifyDerivedHdrVideoProfile(
            dynamicRange = HDR10_PLUS_10_BIT,
            expectedHdrFormat = HDR_HDR10PLUS,
            expectedCodec = MediaRecorder.VideoEncoder.HEVC,
            expectedMediaType = MediaFormat.MIMETYPE_VIDEO_HEVC,
            expectedProfile = CodecProfileLevel.HEVCProfileMain10HDR10Plus,
        )
    }

    @Test
    fun deriveHdrVideoProfile_dolbyVision() {
        verifyDerivedHdrVideoProfile(
            dynamicRange = DOLBY_VISION_10_BIT,
            expectedHdrFormat = HDR_DOLBY_VISION,
            expectedCodec = MediaRecorder.VideoEncoder.DOLBY_VISION,
            expectedMediaType = MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION,
            expectedProfile = CodecProfileLevel.DolbyVisionProfileDvheSt,
        )
    }

    @Test
    fun validateOrAdapt_10BitProfile_returnsNullForSoftwareEncoder() {
        assertThat(validateOrAdapt(createHlg10Profile(), softwareEncoderFinder)).isNull()
    }

    @Test
    fun validateOrAdapt_10BitProfile_returnsProfileForHardwareEncoder() {
        assertThat(validateOrAdapt(createHlg10Profile(), hardwareEncoderFinder)).isNotNull()
    }

    @Test
    fun validateOrAdapt_8BitProfile_returnsProfileForSoftwareEncoder() {
        assertThat(validateOrAdapt(sdrProfile, softwareEncoderFinder)).isNotNull()
    }

    @Test
    fun validateOrAdapt_bitrateIsClamped() {
        val finder = VideoEncoderInfo.Finder {
            FakeVideoEncoderInfo(supportedBitrateRange = Range.create(0, 1000))
        }

        assertThat(validateOrAdapt(sdrProfile, finder)!!.bitrate).isEqualTo(1000)
    }

    @Test
    fun validateOrAdapt_returnsNullWhenNoEncoder() {
        assertThat(validateOrAdapt(sdrProfile) { null }).isNull()
    }

    @Test
    fun createDefaultVideoProfile_sdr() {
        val profile = EncoderProfilesUtil.createDefaultVideoProfile(RESOLUTION_1080P, SDR)

        assertThat(profile.codec).isEqualTo(MediaRecorder.VideoEncoder.H264)
        assertThat(profile.mediaType).isEqualTo(MediaFormat.MIMETYPE_VIDEO_AVC)
        assertThat(profile.profile).isEqualTo(EncoderProfilesProxy.CODEC_PROFILE_NONE)
        assertThat(profile.bitDepth).isEqualTo(BIT_DEPTH_8)
        assertThat(profile.hdrFormat).isEqualTo(HDR_NONE)
        assertThat(profile.resolution).isEqualTo(RESOLUTION_1080P)
        assertThat(profile.frameRate).isEqualTo(30)
        // 4 Mbps at 720p scaled to 1080p.
        assertThat(profile.bitrate).isEqualTo(9_000_000)
    }

    @Test
    fun createDefaultVideoProfile_hlg10() {
        val profile = EncoderProfilesUtil.createDefaultVideoProfile(RESOLUTION_1080P, HLG_10_BIT)

        assertThat(profile.codec).isEqualTo(MediaRecorder.VideoEncoder.HEVC)
        assertThat(profile.mediaType).isEqualTo(MediaFormat.MIMETYPE_VIDEO_HEVC)
        assertThat(profile.profile).isEqualTo(CodecProfileLevel.HEVCProfileMain10)
        assertThat(profile.bitDepth).isEqualTo(BIT_DEPTH_10)
        assertThat(profile.hdrFormat).isEqualTo(HDR_HLG)
        // 4 Mbps at 720p scaled to 1080p and 10-bit.
        assertThat(profile.bitrate).isEqualTo(11_250_000)
    }

    @Test
    fun createDefaultVideoProfile_dolbyVision() {
        val profile =
            EncoderProfilesUtil.createDefaultVideoProfile(RESOLUTION_1080P, DOLBY_VISION_10_BIT)

        assertThat(profile.codec).isEqualTo(MediaRecorder.VideoEncoder.DOLBY_VISION)
        assertThat(profile.mediaType).isEqualTo(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION)
        assertThat(profile.profile).isEqualTo(CodecProfileLevel.DolbyVisionProfileDvheSt)
        assertThat(profile.hdrFormat).isEqualTo(HDR_DOLBY_VISION)
    }

    @Test
    fun createDefaultEncoderProfiles() {
        val profiles = EncoderProfilesUtil.createDefaultEncoderProfiles(listOf(sdrProfile))

        assertThat(profiles.defaultDurationSeconds).isEqualTo(60)
        assertThat(profiles.recommendedFileFormat).isEqualTo(MediaRecorder.OutputFormat.MPEG_4)
        assertThat(profiles.videoProfiles).containsExactly(sdrProfile)
        val audioProfile = profiles.audioProfiles.single()
        assertThat(audioProfile.mediaType).isEqualTo(MediaFormat.MIMETYPE_AUDIO_AAC)
        assertThat(audioProfile.bitrate).isEqualTo(96000)
        assertThat(audioProfile.sampleRate).isEqualTo(44100)
        assertThat(audioProfile.channels).isEqualTo(1)
    }

    private fun createHlg10Profile(): VideoProfileProxy =
        EncoderProfilesUtil.deriveHdrVideoProfile(sdrProfile, HLG_10_BIT)

    private fun verifyDerivedHdrVideoProfile(
        dynamicRange: DynamicRange,
        expectedHdrFormat: Int,
        expectedCodec: Int,
        expectedMediaType: String,
        expectedProfile: Int,
    ) {
        val profile = EncoderProfilesUtil.deriveHdrVideoProfile(sdrProfile, dynamicRange)

        assertThat(profile.codec).isEqualTo(expectedCodec)
        assertThat(profile.mediaType).isEqualTo(expectedMediaType)
        assertThat(profile.profile).isEqualTo(expectedProfile)
        assertThat(profile.hdrFormat).isEqualTo(expectedHdrFormat)
        assertThat(profile.bitDepth).isEqualTo(BIT_DEPTH_10)
        // Bitrate is scaled by the bit depth ratio (10 / 8).
        assertThat(profile.bitrate).isEqualTo(10_000_000)
        assertThat(profile.width).isEqualTo(sdrProfile.width)
        assertThat(profile.height).isEqualTo(sdrProfile.height)
        assertThat(profile.frameRate).isEqualTo(sdrProfile.frameRate)
    }
}
