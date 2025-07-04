/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.video.internal.workaround

import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_2160P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_480P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_720P
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import androidx.camera.video.Quality
import androidx.camera.video.Quality.FHD
import androidx.camera.video.Quality.HD
import androidx.camera.video.Quality.QUALITY_SOURCE_REGULAR
import androidx.camera.video.Quality.SD
import androidx.camera.video.Quality.UHD
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_AUDIO_BITRATE
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_AUDIO_CHANNELS
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_AUDIO_CODEC
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_AUDIO_PROFILE
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_AUDIO_SAMPLE_RATE
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_VIDEO_BITRATE_FHD
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_VIDEO_BITRATE_HD
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_VIDEO_BITRATE_SD
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_VIDEO_BITRATE_UHD
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_VIDEO_BIT_DEPTH
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_VIDEO_CHROMA_SUBSAMPLING
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_VIDEO_CODEC
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_VIDEO_FRAME_RATE
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_VIDEO_HDR_FORMAT
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider.Companion.DEFAULT_VIDEO_PROFILE
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class DefaultEncoderProfilesProviderTest {

    private val targetQualities = listOf(UHD, FHD, HD, SD)
    private val fakeCameraInfo = FakeCameraInfoInternal()
    private lateinit var defaultEncoderProfilesProvider: DefaultEncoderProfilesProvider

    @Test
    fun getUhdProfile_verifyResult() {
        // Arrange.
        fakeCameraInfo.setSupportedResolutions(
            RESOLUTION_2160P,
            RESOLUTION_1080P,
            RESOLUTION_720P,
            RESOLUTION_480P,
        )
        defaultEncoderProfilesProvider =
            DefaultEncoderProfilesProvider(
                fakeCameraInfo,
                targetQualities,
                FakeVideoEncoderInfoFinder(),
            )

        // Act.
        val encoderProfiles = defaultEncoderProfilesProvider.getAll(UHD.value)

        // Assert.
        verifyVideoProfile(
            encoderProfiles,
            RESOLUTION_2160P.width,
            RESOLUTION_2160P.height,
            DEFAULT_VIDEO_BITRATE_UHD,
        )
        verifyAudioProfile(encoderProfiles)
    }

    @Test
    fun getUhdProfileWithUnsupportedBitrate_verifyClampedBitrate() {
        // Arrange.
        fakeCameraInfo.setSupportedResolutions(
            RESOLUTION_2160P,
            RESOLUTION_1080P,
            RESOLUTION_720P,
            RESOLUTION_480P,
        )
        defaultEncoderProfilesProvider =
            DefaultEncoderProfilesProvider(
                fakeCameraInfo,
                targetQualities,
                FakeVideoEncoderInfoFinder(DEFAULT_VIDEO_BITRATE_FHD),
            )

        // Act.
        val encoderProfiles = defaultEncoderProfilesProvider.getAll(UHD.value)

        // Assert.
        verifyVideoProfile(
            encoderProfiles,
            RESOLUTION_2160P.width,
            RESOLUTION_2160P.height,
            DEFAULT_VIDEO_BITRATE_FHD,
        )
        verifyAudioProfile(encoderProfiles)
    }

    @Test
    fun getUhdProfileWithUnsupportedSize_returnNull() {
        // Arrange: 2160P is not supported.
        fakeCameraInfo.setSupportedResolutions(RESOLUTION_1080P, RESOLUTION_720P, RESOLUTION_480P)
        defaultEncoderProfilesProvider =
            DefaultEncoderProfilesProvider(
                fakeCameraInfo,
                targetQualities,
                FakeVideoEncoderInfoFinder(),
            )

        // Act.
        val encoderProfiles = defaultEncoderProfilesProvider.getAll(UHD.value)

        // Assert.
        assertThat(encoderProfiles).isNull()
    }

    @Test
    fun getFhdProfile_verifyResult() {
        // Arrange.
        fakeCameraInfo.setSupportedResolutions(RESOLUTION_1080P, RESOLUTION_720P, RESOLUTION_480P)
        defaultEncoderProfilesProvider =
            DefaultEncoderProfilesProvider(
                fakeCameraInfo,
                targetQualities,
                FakeVideoEncoderInfoFinder(),
            )

        // Act.
        val encoderProfiles = defaultEncoderProfilesProvider.getAll(FHD.value)

        // Assert.
        verifyVideoProfile(
            encoderProfiles,
            RESOLUTION_1080P.width,
            RESOLUTION_1080P.height,
            DEFAULT_VIDEO_BITRATE_FHD,
        )
        verifyAudioProfile(encoderProfiles)
    }

    @Test
    fun getFhdProfileWithUnsupportedBitrate_verifyClampedBitrate() {
        // Arrange.
        fakeCameraInfo.setSupportedResolutions(RESOLUTION_1080P, RESOLUTION_720P, RESOLUTION_480P)
        defaultEncoderProfilesProvider =
            DefaultEncoderProfilesProvider(
                fakeCameraInfo,
                targetQualities,
                FakeVideoEncoderInfoFinder(DEFAULT_VIDEO_BITRATE_HD),
            )

        // Act.
        val encoderProfiles = defaultEncoderProfilesProvider.getAll(FHD.value)

        // Assert.
        verifyVideoProfile(
            encoderProfiles,
            RESOLUTION_1080P.width,
            RESOLUTION_1080P.height,
            DEFAULT_VIDEO_BITRATE_HD,
        )
        verifyAudioProfile(encoderProfiles)
    }

    @Test
    fun getFhdProfileWithUnsupportedSize_returnNull() {
        // Arrange: 1080P is not supported.
        fakeCameraInfo.setSupportedResolutions(RESOLUTION_720P, RESOLUTION_480P)
        defaultEncoderProfilesProvider =
            DefaultEncoderProfilesProvider(
                fakeCameraInfo,
                targetQualities,
                FakeVideoEncoderInfoFinder(),
            )

        // Act.
        val encoderProfiles = defaultEncoderProfilesProvider.getAll(FHD.value)

        // Arrange.
        assertThat(encoderProfiles).isNull()
    }

    @Test
    fun getHdProfile_verifyResult() {
        // Arrange.
        fakeCameraInfo.setSupportedResolutions(RESOLUTION_720P, RESOLUTION_480P)
        defaultEncoderProfilesProvider =
            DefaultEncoderProfilesProvider(
                fakeCameraInfo,
                targetQualities,
                FakeVideoEncoderInfoFinder(),
            )

        // Act.
        val encoderProfiles = defaultEncoderProfilesProvider.getAll(HD.value)

        // Arrange.
        verifyVideoProfile(
            encoderProfiles,
            RESOLUTION_720P.width,
            RESOLUTION_720P.height,
            DEFAULT_VIDEO_BITRATE_HD,
        )
        verifyAudioProfile(encoderProfiles)
    }

    @Test
    fun getHdProfileWithUnsupportedBitrate_verifyClampedBitrate() {
        // Arrange.
        fakeCameraInfo.setSupportedResolutions(RESOLUTION_720P, RESOLUTION_480P)
        defaultEncoderProfilesProvider =
            DefaultEncoderProfilesProvider(
                fakeCameraInfo,
                targetQualities,
                FakeVideoEncoderInfoFinder(DEFAULT_VIDEO_BITRATE_SD),
            )

        // Act.
        val encoderProfiles = defaultEncoderProfilesProvider.getAll(HD.value)

        // Assert.
        verifyVideoProfile(
            encoderProfiles,
            RESOLUTION_720P.width,
            RESOLUTION_720P.height,
            DEFAULT_VIDEO_BITRATE_SD,
        )
        verifyAudioProfile(encoderProfiles)
    }

    @Test
    fun getHdProfileWithUnsupportedSize_returnNull() {
        // Arrange: 720P is not supported.
        fakeCameraInfo.setSupportedResolutions(RESOLUTION_480P)
        defaultEncoderProfilesProvider =
            DefaultEncoderProfilesProvider(
                fakeCameraInfo,
                targetQualities,
                FakeVideoEncoderInfoFinder(),
            )

        // Act.
        val encoderProfiles = defaultEncoderProfilesProvider.getAll(HD.value)

        // Assert.
        assertThat(encoderProfiles).isNull()
    }

    @Test
    fun getSdProfile_verifyResult() {
        // Arrange.
        fakeCameraInfo.setSupportedResolutions(RESOLUTION_480P)
        defaultEncoderProfilesProvider =
            DefaultEncoderProfilesProvider(
                fakeCameraInfo,
                targetQualities,
                FakeVideoEncoderInfoFinder(),
            )

        // Act.
        val encoderProfiles = defaultEncoderProfilesProvider.getAll(SD.value)

        // Assert.
        verifyVideoProfile(
            encoderProfiles,
            RESOLUTION_480P.width,
            RESOLUTION_480P.height,
            DEFAULT_VIDEO_BITRATE_SD,
        )
        verifyAudioProfile(encoderProfiles)
    }

    @Test
    fun getSdProfileWithUnsupportedBitrate_verifyClampedBitrate() {
        // Arrange.
        fakeCameraInfo.setSupportedResolutions(RESOLUTION_480P)
        val maxSupportedBitrate = DEFAULT_VIDEO_BITRATE_SD / 2
        defaultEncoderProfilesProvider =
            DefaultEncoderProfilesProvider(
                fakeCameraInfo,
                targetQualities,
                FakeVideoEncoderInfoFinder(maxSupportedBitrate),
            )

        // Act.
        val encoderProfiles = defaultEncoderProfilesProvider.getAll(SD.value)

        // Assert.
        verifyVideoProfile(
            encoderProfiles,
            RESOLUTION_480P.width,
            RESOLUTION_480P.height,
            maxSupportedBitrate,
        )
        verifyAudioProfile(encoderProfiles)
    }

    @Test
    fun getSdProfileWithUnsupportedSize_returnNull() {
        // Arrange: 480P is not supported.
        fakeCameraInfo.setSupportedResolutions()
        defaultEncoderProfilesProvider =
            DefaultEncoderProfilesProvider(
                fakeCameraInfo,
                targetQualities,
                FakeVideoEncoderInfoFinder(),
            )

        // Act.
        val encoderProfiles = defaultEncoderProfilesProvider.getAll(SD.value)

        // Assert.
        assertThat(encoderProfiles).isNull()
    }

    private fun verifyVideoProfile(
        encoderProfiles: EncoderProfilesProxy?,
        width: Int,
        height: Int,
        bitrate: Int,
    ) {
        assertThat(encoderProfiles!!.videoProfiles).hasSize(1)
        val videoProfile = encoderProfiles.videoProfiles[0]
        assertThat(videoProfile.codec).isEqualTo(DEFAULT_VIDEO_CODEC)
        assertThat(videoProfile.bitrate).isEqualTo(bitrate)
        assertThat(videoProfile.width).isEqualTo(width)
        assertThat(videoProfile.height).isEqualTo(height)
        assertThat(videoProfile.frameRate).isEqualTo(DEFAULT_VIDEO_FRAME_RATE)
        assertThat(videoProfile.profile).isEqualTo(DEFAULT_VIDEO_PROFILE)
        assertThat(videoProfile.bitDepth).isEqualTo(DEFAULT_VIDEO_BIT_DEPTH)
        assertThat(videoProfile.chromaSubsampling).isEqualTo(DEFAULT_VIDEO_CHROMA_SUBSAMPLING)
        assertThat(videoProfile.hdrFormat).isEqualTo(DEFAULT_VIDEO_HDR_FORMAT)
    }

    private fun verifyAudioProfile(encoderProfiles: EncoderProfilesProxy?) {
        assertThat(encoderProfiles!!.audioProfiles).hasSize(1)
        val audioProfile = encoderProfiles.audioProfiles[0]
        assertThat(audioProfile.codec).isEqualTo(DEFAULT_AUDIO_CODEC)
        assertThat(audioProfile.bitrate).isEqualTo(DEFAULT_AUDIO_BITRATE)
        assertThat(audioProfile.sampleRate).isEqualTo(DEFAULT_AUDIO_SAMPLE_RATE)
        assertThat(audioProfile.channels).isEqualTo(DEFAULT_AUDIO_CHANNELS)
        assertThat(audioProfile.profile).isEqualTo(DEFAULT_AUDIO_PROFILE)
    }

    private fun FakeCameraInfoInternal.setSupportedResolutions(vararg supportedSizes: Size) {
        setSupportedResolutions(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE, supportedSizes.toList())
    }

    private val Quality.value: Int
        get() = (this as Quality.ConstantQuality).getQualityValue(QUALITY_SOURCE_REGULAR)

    private class FakeVideoEncoderInfoFinder(
        private val maxSupportedBitrate: Int = Int.MAX_VALUE,
        private val minSupportedBitrate: Int = 0,
    ) : VideoEncoderInfo.Finder {
        override fun find(mimeType: String): VideoEncoderInfo =
            FakeVideoEncoderInfo(
                supportedBitrateRange = Range.create(minSupportedBitrate, maxSupportedBitrate)
            )
    }
}
