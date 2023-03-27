/*
 * Copyright 2021 The Android Open Source Project
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
import android.media.CamcorderProfile.QUALITY_LOW
import android.os.Build
import android.util.Size
import androidx.camera.testing.EncoderProfilesUtil
import androidx.camera.testing.EncoderProfilesUtil.PROFILES_2160P
import androidx.camera.testing.EncoderProfilesUtil.PROFILES_720P
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.fakes.FakeEncoderProfilesProvider
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy
import androidx.core.util.component1
import androidx.core.util.component2
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VideoCapabilitiesTest {

    private val cameraInfo = FakeCameraInfoInternal().apply {
        encoderProfilesProvider = FakeEncoderProfilesProvider.Builder()
            .add(QUALITY_HIGH, PROFILES_2160P) // UHD (2160p) per above definition
            .add(QUALITY_2160P, PROFILES_2160P) // UHD (2160p)
            .add(QUALITY_720P, PROFILES_720P) // HD (720p)
            .add(QUALITY_LOW, PROFILES_720P) // HD (720p) per above definition
            .build()
    }
    private val validatedProfiles2160p = VideoValidatedEncoderProfilesProxy.from(PROFILES_2160P)
    private val validatedProfiles720p = VideoValidatedEncoderProfilesProxy.from(PROFILES_720P)

    @Test
    fun isQualitySupported() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        assertThat(videoCapabilities.isQualitySupported(Quality.HIGHEST)).isTrue()
        assertThat(videoCapabilities.isQualitySupported(Quality.LOWEST)).isTrue()
        assertThat(videoCapabilities.isQualitySupported(Quality.UHD)).isTrue()
        assertThat(videoCapabilities.isQualitySupported(Quality.FHD)).isFalse()
        assertThat(videoCapabilities.isQualitySupported(Quality.HD)).isTrue()
        assertThat(videoCapabilities.isQualitySupported(Quality.SD)).isFalse()
    }

    @Test
    fun getProfile() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        assertThat(videoCapabilities.getProfiles(Quality.HIGHEST)).isEqualTo(validatedProfiles2160p)
        assertThat(videoCapabilities.getProfiles(Quality.LOWEST)).isEqualTo(validatedProfiles720p)
        assertThat(videoCapabilities.getProfiles(Quality.UHD)).isEqualTo(validatedProfiles2160p)
        assertThat(videoCapabilities.getProfiles(Quality.FHD)).isNull()
        assertThat(videoCapabilities.getProfiles(Quality.HD)).isEqualTo(validatedProfiles720p)
        assertThat(videoCapabilities.getProfiles(Quality.SD)).isNull()
    }

    @Test
    fun findHighestSupportedQuality_returnsHigherQuality() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        // Create a size between 720p and 2160p
        val (width720p, height720p) = EncoderProfilesUtil.RESOLUTION_720P
        val inBetweenSize = Size(width720p + 10, height720p)

        assertThat(videoCapabilities.findHighestSupportedQualityFor(inBetweenSize))
            .isEqualTo(Quality.UHD)
    }

    @Test
    fun findHighestSupportedQuality_returnsHighestQuality_whenAboveHighest() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        // Create a size between greater than the max quality (UHD)
        val (width2160p, height2160p) = EncoderProfilesUtil.RESOLUTION_2160P
        val aboveHighestSize = Size(width2160p + 10, height2160p)

        assertThat(videoCapabilities.findHighestSupportedQualityFor(aboveHighestSize))
            .isEqualTo(Quality.UHD)
    }

    @Test
    fun findHighestSupportedQuality_returnsLowestQuality_whenBelowLowest() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        // Create a size below the lowest quality (HD)
        val (width720p, height720p) = EncoderProfilesUtil.RESOLUTION_720P
        val belowLowestSize = Size(width720p - 10, height720p)

        assertThat(videoCapabilities.findHighestSupportedQualityFor(belowLowestSize))
            .isEqualTo(Quality.HD)
    }

    @Test
    fun findHighestSupportedQuality_returnsExactQuality_whenExactSizeGiven() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        val exactSize720p = EncoderProfilesUtil.RESOLUTION_720P

        assertThat(videoCapabilities.findHighestSupportedQualityFor(exactSize720p))
            .isEqualTo(Quality.HD)
    }

    @Test
    fun findHighestSupportedEncoderProfilesFor_returnsHigherProfile() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        // Create a size between 720p and 2160p
        val (width720p, height720p) = EncoderProfilesUtil.RESOLUTION_720P
        val inBetweenSize = Size(width720p + 10, height720p)

        assertThat(videoCapabilities.findHighestSupportedEncoderProfilesFor(inBetweenSize))
            .isEqualTo(validatedProfiles2160p)
    }

    @Test
    fun findHighestSupportedEncoderProfilesFor_returnsHighestProfile_whenAboveHighest() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        // Create a size between greater than the max quality (UHD)
        val (width2160p, height2160p) = EncoderProfilesUtil.RESOLUTION_2160P
        val aboveHighestSize = Size(width2160p + 10, height2160p)

        assertThat(videoCapabilities.findHighestSupportedEncoderProfilesFor(aboveHighestSize))
            .isEqualTo(validatedProfiles2160p)
    }

    @Test
    fun findHighestSupportedEncoderProfilesFor_returnsLowestProfile_whenBelowLowest() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        // Create a size below the lowest quality (HD)
        val (width720p, height720p) = EncoderProfilesUtil.RESOLUTION_720P
        val belowLowestSize = Size(width720p - 10, height720p)

        assertThat(videoCapabilities.findHighestSupportedEncoderProfilesFor(belowLowestSize))
            .isEqualTo(validatedProfiles720p)
    }

    @Test
    fun findHighestSupportedEncoderProfilesFor_returnsExactProfile_whenExactSizeGiven() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        val exactSize720p = EncoderProfilesUtil.RESOLUTION_720P

        assertThat(videoCapabilities.findHighestSupportedEncoderProfilesFor(exactSize720p))
            .isEqualTo(validatedProfiles720p)
    }
}
