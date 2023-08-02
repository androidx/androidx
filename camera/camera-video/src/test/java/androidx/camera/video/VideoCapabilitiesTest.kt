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

import android.os.Build
import android.util.Size
import androidx.camera.testing.CamcorderProfileUtil
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_2160P
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_720P
import androidx.camera.testing.fakes.FakeCamcorderProfileProvider
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.core.util.component1
import androidx.core.util.component2
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private val PROFILE_HIGH = CamcorderProfileUtil.asHighQuality(PROFILE_2160P)
private val PROFILE_LOW = CamcorderProfileUtil.asLowQuality(PROFILE_720P)

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VideoCapabilitiesTest {

    private val cameraInfo = FakeCameraInfoInternal().apply {
        camcorderProfileProvider = FakeCamcorderProfileProvider.Builder()
            .addProfile(PROFILE_HIGH) // UHD (2160p) per above definition
            .addProfile(PROFILE_2160P) // UHD (2160p)
            .addProfile(PROFILE_720P) // HD (720p)
            .addProfile(PROFILE_LOW) // HD (720p) per above definition
            .build()
    }

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
        assertThat(videoCapabilities.getProfile(Quality.HIGHEST))
            .isEqualTo(PROFILE_2160P)
        assertThat(videoCapabilities.getProfile(Quality.LOWEST))
            .isEqualTo(PROFILE_720P)
        assertThat(videoCapabilities.getProfile(Quality.UHD))
            .isEqualTo(PROFILE_2160P)
        assertThat(videoCapabilities.getProfile(Quality.FHD)).isNull()
        assertThat(videoCapabilities.getProfile(Quality.HD))
            .isEqualTo(PROFILE_720P)
        assertThat(videoCapabilities.getProfile(Quality.SD)).isNull()
    }

    @Test
    fun findHighestSupportedQuality_returnsHigherQuality() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        // Create a size between 720p and 2160p
        val (width720p, height720p) = CamcorderProfileUtil.RESOLUTION_720P
        val inBetweenSize = Size(width720p + 10, height720p)

        assertThat(videoCapabilities.findHighestSupportedQualityFor(inBetweenSize))
            .isEqualTo(Quality.UHD)
    }

    @Test
    fun findHighestSupportedQuality_returnsHighestQuality_whenAboveHighest() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        // Create a size between greater than the max quality (UHD)
        val (width2160p, height2160p) = CamcorderProfileUtil.RESOLUTION_2160P
        val aboveHighestSize = Size(width2160p + 10, height2160p)

        assertThat(videoCapabilities.findHighestSupportedQualityFor(aboveHighestSize))
            .isEqualTo(Quality.UHD)
    }

    @Test
    fun findHighestSupportedQuality_returnsLowestQuality_whenBelowLowest() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        // Create a size below the lowest quality (HD)
        val (width720p, height720p) = CamcorderProfileUtil.RESOLUTION_720P
        val belowLowestSize = Size(width720p - 10, height720p)

        assertThat(videoCapabilities.findHighestSupportedQualityFor(belowLowestSize))
            .isEqualTo(Quality.HD)
    }

    @Test
    fun findHighestSupportedQuality_returnsExactQuality_whenExactSizeGiven() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        val exactSize720p = CamcorderProfileUtil.RESOLUTION_720P

        assertThat(videoCapabilities.findHighestSupportedQualityFor(exactSize720p))
            .isEqualTo(Quality.HD)
    }
}
