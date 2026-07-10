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

package androidx.camera.video

import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_HIGH
import android.media.EncoderProfiles.VideoProfile
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_2160P
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import androidx.camera.video.Quality.QUALITY_SOURCE_HIGH_SPEED
import androidx.camera.video.Quality.QUALITY_SOURCE_REGULAR
import androidx.camera.video.Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE
import androidx.camera.video.Recorder.VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class EncoderProfilesProviderResolverTest {

    private val videoEncoderInfoFinder = VideoEncoderInfo.Finder { FakeVideoEncoderInfo() }

    @Test
    fun resolve_highSpeed_returnsProviderIfSupported() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                isHighSpeedSupported = true
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder()
                        .add(QUALITY_HIGH_SPEED_HIGH, PROFILES_1080P)
                        .build()
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_HIGH_SPEED,
                videoEncoderInfoFinder,
            )

        assertThat(provider).isSameInstanceAs(cameraInfo.encoderProfilesProvider)
    }

    @Test
    fun resolve_highSpeed_returnsEmptyIfNotSupported() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                isHighSpeedSupported = false
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder()
                        .add(QUALITY_HIGH_SPEED_HIGH, PROFILES_1080P)
                        .build()
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_HIGH_SPEED,
                videoEncoderInfoFinder,
            )

        assertThat(provider).isSameInstanceAs(EncoderProfilesProvider.EMPTY)
    }

    @Test
    fun resolve_noSupportedQuality_usesDefaultProvider() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider = EncoderProfilesProvider.EMPTY
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_1080P),
                )
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        // Assert: 1080p is added.
        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
    }

    @Test
    fun resolve_codecCapabilitiesSource_usesQualityExploredProvider() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                setSupportedResolutions(
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    listOf(RESOLUTION_1080P, RESOLUTION_2160P),
                )
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        // Assert: 2160P is explored.
        assertThat(provider.hasProfile(QUALITY_2160P)).isTrue()
    }

    @Test
    fun resolve_hlg10Supported_usesBackupHdrProvider() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                encoderProfilesProvider =
                    FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()
                supportedDynamicRanges = setOf(DynamicRange.HLG_10_BIT)
            }

        val provider =
            EncoderProfilesProviderResolver.resolve(
                cameraInfo,
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                QUALITY_SOURCE_REGULAR,
                videoEncoderInfoFinder,
            )

        assertThat(
                provider.getAll(QUALITY_1080P)!!.videoProfiles.any {
                    it.hdrFormat == VideoProfile.HDR_HLG
                }
            )
            .isTrue()
    }
}
