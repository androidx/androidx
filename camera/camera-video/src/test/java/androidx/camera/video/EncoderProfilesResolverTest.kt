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

package androidx.camera.video

import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_720P
import android.media.CamcorderProfile.QUALITY_HIGH
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_2160P
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_720P
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_HIGH
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_LOW
import android.media.CamcorderProfile.QUALITY_LOW
import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.BIT_DEPTH_10_BIT
import androidx.camera.core.DynamicRange.BIT_DEPTH_8_BIT
import androidx.camera.core.DynamicRange.BIT_DEPTH_UNSPECIFIED
import androidx.camera.core.DynamicRange.ENCODING_DOLBY_VISION
import androidx.camera.core.DynamicRange.ENCODING_HDR_UNSPECIFIED
import androidx.camera.core.DynamicRange.ENCODING_UNSPECIFIED
import androidx.camera.core.DynamicRange.HDR10_10_BIT
import androidx.camera.core.DynamicRange.HDR_UNSPECIFIED_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.DynamicRange.UNSPECIFIED
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_2160P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_720P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_2160P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_480P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_720P
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeEncoderProfilesProxy
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeHighSpeedEncoderProfilesProxy
import androidx.camera.testing.impl.FrameRateUtil.FPS_120_120
import androidx.camera.testing.impl.FrameRateUtil.FPS_240
import androidx.camera.testing.impl.FrameRateUtil.FPS_240_240
import androidx.camera.testing.impl.FrameRateUtil.FPS_30_120
import androidx.camera.testing.impl.FrameRateUtil.FPS_30_240
import androidx.camera.testing.impl.FrameRateUtil.FPS_30_480
import androidx.camera.testing.impl.FrameRateUtil.FPS_480
import androidx.camera.testing.impl.FrameRateUtil.FPS_480_480
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.video.Quality.FHD
import androidx.camera.video.Quality.HD
import androidx.camera.video.Quality.HIGHEST
import androidx.camera.video.Quality.LOWEST
import androidx.camera.video.Quality.QUALITY_SOURCE_HIGH_SPEED
import androidx.camera.video.Quality.QUALITY_SOURCE_REGULAR
import androidx.camera.video.Quality.SD
import androidx.camera.video.Quality.UHD
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy
import androidx.core.util.component1
import androidx.core.util.component2
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private val UNSPECIFIED_8_BIT = DynamicRange(ENCODING_UNSPECIFIED, BIT_DEPTH_8_BIT)
private val UNSPECIFIED_10_BIT = DynamicRange(ENCODING_UNSPECIFIED, BIT_DEPTH_10_BIT)
private val HDR_UNSPECIFIED = DynamicRange(ENCODING_HDR_UNSPECIFIED, BIT_DEPTH_UNSPECIFIED)
private val DOLBY_VISION_UNSPECIFIED = DynamicRange(ENCODING_DOLBY_VISION, BIT_DEPTH_UNSPECIFIED)

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class EncoderProfilesResolverTest(private val qualitySource: Int) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "qualitySource={0}")
        fun data() = listOf(arrayOf(QUALITY_SOURCE_REGULAR), arrayOf(QUALITY_SOURCE_HIGH_SPEED))
    }

    private val isHighSpeed = qualitySource == QUALITY_SOURCE_HIGH_SPEED

    private val defaultProfilesProvider =
        FakeEncoderProfilesProvider.Builder()
            .apply {
                if (isHighSpeed) {
                        val profile2160p240fpsSdrHlg =
                            createFakeHighSpeedEncoderProfilesProxy(
                                RESOLUTION_2160P,
                                videoFrameRate = FPS_240,
                                dynamicRanges = setOf(SDR, HLG_10_BIT),
                            )
                        val profile720p480fpsSdrHlg =
                            createFakeHighSpeedEncoderProfilesProxy(
                                RESOLUTION_720P,
                                videoFrameRate = FPS_480,
                                dynamicRanges = setOf(SDR, HLG_10_BIT),
                            )
                        // Add the same profiles to support parameterized test.
                        add(QUALITY_HIGH_SPEED_HIGH, profile2160p240fpsSdrHlg)
                        add(QUALITY_HIGH_SPEED_2160P, profile2160p240fpsSdrHlg) // UHD (2160p)
                        add(QUALITY_HIGH_SPEED_720P, profile720p480fpsSdrHlg) // HD (720p)
                        add(QUALITY_HIGH_SPEED_LOW, profile720p480fpsSdrHlg)
                    } else {
                        val profile2160pSdrHlg =
                            createFakeEncoderProfilesProxy(
                                videoResolution = RESOLUTION_2160P,
                                dynamicRanges = setOf(SDR, HLG_10_BIT),
                            )
                        val profile720pSdrHlg =
                            createFakeEncoderProfilesProxy(
                                videoResolution = RESOLUTION_720P,
                                dynamicRanges = setOf(SDR, HLG_10_BIT),
                            )
                        add(QUALITY_HIGH, profile2160pSdrHlg) // UHD (2160p) per above definition
                        add(QUALITY_2160P, profile2160pSdrHlg) // UHD (2160p)
                        add(QUALITY_720P, profile720pSdrHlg) // HD (720p)
                        add(QUALITY_LOW, profile720pSdrHlg) // HD (720p) per above definition
                    }
                    .build()
            }
            .build()
    private val defaultDynamicRanges = setOf(SDR, HLG_10_BIT)
    private val cameraInfo =
        FakeCameraInfoInternal().apply {
            isHighSpeedSupported = isHighSpeed
            encoderProfilesProvider = defaultProfilesProvider
            supportedDynamicRanges = defaultDynamicRanges
            setSupportedResolutions(
                INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                listOf(RESOLUTION_2160P, RESOLUTION_1080P, RESOLUTION_720P, RESOLUTION_480P),
            )
            // 120 FPS -  2160P, 1080P, 720P, 480P
            setSupportedHighSpeedResolutions(
                FPS_30_120,
                listOf(RESOLUTION_2160P, RESOLUTION_1080P, RESOLUTION_720P, RESOLUTION_480P),
            )
            setSupportedHighSpeedResolutions(
                FPS_120_120,
                listOf(RESOLUTION_2160P, RESOLUTION_1080P, RESOLUTION_720P, RESOLUTION_480P),
            )
            // 240 FPS - 2160P, 1080P, 720P, 480P
            setSupportedHighSpeedResolutions(
                FPS_30_240,
                listOf(RESOLUTION_2160P, RESOLUTION_1080P, RESOLUTION_720P, RESOLUTION_480P),
            )
            setSupportedHighSpeedResolutions(
                FPS_240_240,
                listOf(RESOLUTION_2160P, RESOLUTION_1080P, RESOLUTION_720P, RESOLUTION_480P),
            )
            // 480 FPS - 720P, 480P
            setSupportedHighSpeedResolutions(FPS_30_480, listOf(RESOLUTION_720P, RESOLUTION_480P))
            setSupportedHighSpeedResolutions(FPS_480_480, listOf(RESOLUTION_720P, RESOLUTION_480P))
        }
    // Note: validated profiles only contain SDR profiles.
    private val validatedProfiles2160p =
        VideoValidatedEncoderProfilesProxy.from(
            if (isHighSpeed)
                createFakeHighSpeedEncoderProfilesProxy(
                    RESOLUTION_2160P,
                    videoFrameRate = FPS_240,
                    dynamicRanges = setOf(SDR),
                )
            else PROFILES_2160P
        )
    private val validatedProfiles720p =
        VideoValidatedEncoderProfilesProxy.from(
            if (isHighSpeed)
                createFakeHighSpeedEncoderProfilesProxy(
                    RESOLUTION_720P,
                    videoFrameRate = FPS_480,
                    dynamicRanges = setOf(SDR),
                )
            else PROFILES_720P
        )
    private val encoderProfilesResolver =
        EncoderProfilesResolver(
            cameraInfo.encoderProfilesProvider,
            qualitySource,
            cameraInfo.supportedDynamicRanges,
        )

    @Test
    fun canGetSupportedDynamicRanges() {
        assertThat(encoderProfilesResolver.supportedDynamicRanges).containsExactly(SDR, HLG_10_BIT)
    }

    @Test
    fun hasSupportedQualities_sdr() {
        assertThat(encoderProfilesResolver.getSupportedQualities(SDR)).containsExactly(HD, UHD)
    }

    @Test
    fun hasSupportedQualities_hlg10() {
        assertThat(encoderProfilesResolver.getSupportedQualities(HLG_10_BIT))
            .containsExactly(HD, UHD)
    }

    @Test
    fun hasSupportedQualities_hdr10() {
        assertThat(encoderProfilesResolver.getSupportedQualities(HDR10_10_BIT)).isEmpty()
    }

    @Test
    fun hasSupportedQualities_unspecified() {
        assertThat(encoderProfilesResolver.getSupportedQualities(UNSPECIFIED))
            .containsExactly(HD, UHD)
    }

    @Test
    fun hasSupportedQualities_hdrUnspecified() {
        assertThat(encoderProfilesResolver.getSupportedQualities(HDR_UNSPECIFIED))
            .containsExactly(HD, UHD)
    }

    @Test
    fun hasSupportedQualities_hdrUnspecified10Bit() {
        assertThat(encoderProfilesResolver.getSupportedQualities(HDR_UNSPECIFIED_10_BIT))
            .containsExactly(HD, UHD)
    }

    @Test
    fun hasSupportedQualities_unspecified8Bit() {
        assertThat(encoderProfilesResolver.getSupportedQualities(UNSPECIFIED_8_BIT))
            .containsExactly(HD, UHD)
    }

    @Test
    fun hasSupportedQualities_unspecified10Bit() {
        assertThat(encoderProfilesResolver.getSupportedQualities(UNSPECIFIED_10_BIT))
            .containsExactly(HD, UHD)
    }

    @Test
    fun hasSupportedQualities_dolbyVisionUnspecified() {
        assertThat(encoderProfilesResolver.getSupportedQualities(DOLBY_VISION_UNSPECIFIED))
            .isEmpty()
    }

    @Test
    fun isQualitySupported_sdr() {
        assertThat(encoderProfilesResolver.isQualitySupported(HIGHEST, SDR)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(LOWEST, SDR)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(UHD, SDR)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(FHD, SDR)).isFalse()
        assertThat(encoderProfilesResolver.isQualitySupported(HD, SDR)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(SD, SDR)).isFalse()
    }

    @Test
    fun isQualitySupported_unspecified() {
        assertThat(encoderProfilesResolver.isQualitySupported(HIGHEST, UNSPECIFIED)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(LOWEST, UNSPECIFIED)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(UHD, UNSPECIFIED)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(FHD, UNSPECIFIED)).isFalse()
        assertThat(encoderProfilesResolver.isQualitySupported(HD, UNSPECIFIED)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(SD, UNSPECIFIED)).isFalse()
    }

    @Test
    fun isQualitySupported_hlg10() {
        assertThat(encoderProfilesResolver.isQualitySupported(HIGHEST, HLG_10_BIT)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(LOWEST, HLG_10_BIT)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(UHD, HLG_10_BIT)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(FHD, HLG_10_BIT)).isFalse()
        assertThat(encoderProfilesResolver.isQualitySupported(HD, HLG_10_BIT)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(SD, HLG_10_BIT)).isFalse()
    }

    @Test
    fun isQualitySupported_hdrUnspecified10Bit() {
        assertThat(encoderProfilesResolver.isQualitySupported(HIGHEST, HDR_UNSPECIFIED_10_BIT))
            .isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(LOWEST, HDR_UNSPECIFIED_10_BIT))
            .isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(UHD, HDR_UNSPECIFIED_10_BIT)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(FHD, HDR_UNSPECIFIED_10_BIT))
            .isFalse()
        assertThat(encoderProfilesResolver.isQualitySupported(HD, HDR_UNSPECIFIED_10_BIT)).isTrue()
        assertThat(encoderProfilesResolver.isQualitySupported(SD, HDR_UNSPECIFIED_10_BIT)).isFalse()
    }

    @Test
    fun canGetSameSdrProfile() {
        assertThat(encoderProfilesResolver.getProfiles(HIGHEST, SDR))
            .isEqualTo(validatedProfiles2160p)
        assertThat(encoderProfilesResolver.getProfiles(LOWEST, SDR))
            .isEqualTo(validatedProfiles720p)
        assertThat(encoderProfilesResolver.getProfiles(UHD, SDR)).isEqualTo(validatedProfiles2160p)
        assertThat(encoderProfilesResolver.getProfiles(FHD, SDR)).isNull()
        assertThat(encoderProfilesResolver.getProfiles(HD, SDR)).isEqualTo(validatedProfiles720p)
        assertThat(encoderProfilesResolver.getProfiles(SD, SDR)).isNull()
    }

    @Test
    fun canGetNonNullHdrUnspecified() {
        assertThat(encoderProfilesResolver.getProfiles(HIGHEST, HDR_UNSPECIFIED_10_BIT)).isNotNull()
        assertThat(encoderProfilesResolver.getProfiles(LOWEST, HDR_UNSPECIFIED_10_BIT)).isNotNull()
        assertThat(encoderProfilesResolver.getProfiles(UHD, HDR_UNSPECIFIED_10_BIT)).isNotNull()
        assertThat(encoderProfilesResolver.getProfiles(FHD, HDR_UNSPECIFIED_10_BIT)).isNull()
        assertThat(encoderProfilesResolver.getProfiles(HD, HDR_UNSPECIFIED_10_BIT)).isNotNull()
        assertThat(encoderProfilesResolver.getProfiles(SD, HDR_UNSPECIFIED_10_BIT)).isNull()
    }

    @Test
    fun canGetNonNullHlg10() {
        assertThat(encoderProfilesResolver.getProfiles(HIGHEST, HLG_10_BIT)).isNotNull()
        assertThat(encoderProfilesResolver.getProfiles(LOWEST, HLG_10_BIT)).isNotNull()
        assertThat(encoderProfilesResolver.getProfiles(UHD, HLG_10_BIT)).isNotNull()
        assertThat(encoderProfilesResolver.getProfiles(FHD, HLG_10_BIT)).isNull()
        assertThat(encoderProfilesResolver.getProfiles(HD, HLG_10_BIT)).isNotNull()
        assertThat(encoderProfilesResolver.getProfiles(SD, HLG_10_BIT)).isNull()
    }

    @Test
    fun findNearestHigherSupportedQuality_returnsHigherQuality() {
        // Create a size between 720p and 2160p
        val (width720p, height720p) = RESOLUTION_720P
        val inBetweenSize = Size(width720p + 10, height720p)

        assertThat(encoderProfilesResolver.findNearestHigherSupportedQualityFor(inBetweenSize, SDR))
            .isEqualTo(UHD)
    }

    @Test
    fun findNearestHigherSupportedQuality_returnsHighestQuality_whenAboveHighest() {
        // Create a size between greater than the max quality (UHD)
        val (width2160p, height2160p) = RESOLUTION_2160P
        val aboveHighestSize = Size(width2160p + 10, height2160p)

        assertThat(
                encoderProfilesResolver.findNearestHigherSupportedQualityFor(aboveHighestSize, SDR)
            )
            .isEqualTo(UHD)
    }

    @Test
    fun findNearestHigherSupportedQuality_returnsLowestQuality_whenBelowLowest() {
        // Create a size below the lowest quality (HD)
        val (width720p, height720p) = RESOLUTION_720P
        val belowLowestSize = Size(width720p - 10, height720p)

        assertThat(
                encoderProfilesResolver.findNearestHigherSupportedQualityFor(belowLowestSize, SDR)
            )
            .isEqualTo(HD)
    }

    @Test
    fun findNearestHigherSupportedQuality_returnsExactQuality_whenExactSizeGiven() {
        val exactSize720p = RESOLUTION_720P

        assertThat(encoderProfilesResolver.findNearestHigherSupportedQualityFor(exactSize720p, SDR))
            .isEqualTo(HD)
    }

    @Test
    fun findNearestHigherSupportedEncoderProfilesFor_returnsHigherProfile() {
        // Create a size between 720p and 2160p
        val (width720p, height720p) = RESOLUTION_720P
        val inBetweenSize = Size(width720p + 10, height720p)

        assertThat(
                encoderProfilesResolver.findNearestHigherSupportedEncoderProfilesFor(
                    inBetweenSize,
                    SDR,
                )
            )
            .isEqualTo(validatedProfiles2160p)
    }

    @Test
    fun findNearestHigherSupportedEncoderProfilesFor_returnsHighestProfile_whenAboveHighest() {
        // Create a size between greater than the max quality (UHD)
        val (width2160p, height2160p) = RESOLUTION_2160P
        val aboveHighestSize = Size(width2160p + 10, height2160p)

        assertThat(
                encoderProfilesResolver.findNearestHigherSupportedEncoderProfilesFor(
                    aboveHighestSize,
                    SDR,
                )
            )
            .isEqualTo(validatedProfiles2160p)
    }

    @Test
    fun findNearestHigherSupportedEncoderProfilesFor_returnsLowestProfile_whenBelowLowest() {
        // Create a size below the lowest quality (HD)
        val (width720p, height720p) = RESOLUTION_720P
        val belowLowestSize = Size(width720p - 10, height720p)

        assertThat(
                encoderProfilesResolver.findNearestHigherSupportedEncoderProfilesFor(
                    belowLowestSize,
                    SDR,
                )
            )
            .isEqualTo(validatedProfiles720p)
    }

    @Test
    fun findNearestHigherSupportedEncoderProfilesFor_returnsExactProfile_whenExactSizeGiven() {
        val exactSize720p = RESOLUTION_720P

        assertThat(
                encoderProfilesResolver.findNearestHigherSupportedEncoderProfilesFor(
                    exactSize720p,
                    SDR,
                )
            )
            .isEqualTo(validatedProfiles720p)
    }
}
