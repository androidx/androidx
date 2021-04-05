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
import androidx.camera.testing.CamcorderProfileUtil
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_2160P
import androidx.camera.testing.CamcorderProfileUtil.PROFILE_720P
import androidx.camera.testing.fakes.FakeCamcorderProfileProvider
import androidx.camera.testing.fakes.FakeCameraInfoInternal
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
            .addProfile(PROFILE_HIGH)
            .addProfile(PROFILE_2160P)
            .addProfile(PROFILE_720P)
            .addProfile(PROFILE_LOW)
            .build()
    }

    @Test
    fun isQualitySupported() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        assertThat(videoCapabilities.isQualitySupported(QualitySelector.QUALITY_NONE)).isFalse()
        assertThat(videoCapabilities.isQualitySupported(QualitySelector.QUALITY_HIGHEST)).isTrue()
        assertThat(videoCapabilities.isQualitySupported(QualitySelector.QUALITY_LOWEST)).isTrue()
        assertThat(videoCapabilities.isQualitySupported(QualitySelector.QUALITY_UHD)).isTrue()
        assertThat(videoCapabilities.isQualitySupported(QualitySelector.QUALITY_FHD)).isFalse()
        assertThat(videoCapabilities.isQualitySupported(QualitySelector.QUALITY_HD)).isTrue()
        assertThat(videoCapabilities.isQualitySupported(QualitySelector.QUALITY_SD)).isFalse()
    }

    @Test
    fun getProfile() {
        val videoCapabilities = VideoCapabilities.from(cameraInfo)
        assertThat(videoCapabilities.getProfile(QualitySelector.QUALITY_NONE)).isNull()
        assertThat(videoCapabilities.getProfile(QualitySelector.QUALITY_HIGHEST))
            .isEqualTo(PROFILE_2160P)
        assertThat(videoCapabilities.getProfile(QualitySelector.QUALITY_LOWEST))
            .isEqualTo(PROFILE_720P)
        assertThat(videoCapabilities.getProfile(QualitySelector.QUALITY_UHD))
            .isEqualTo(PROFILE_2160P)
        assertThat(videoCapabilities.getProfile(QualitySelector.QUALITY_FHD)).isNull()
        assertThat(videoCapabilities.getProfile(QualitySelector.QUALITY_HD))
            .isEqualTo(PROFILE_720P)
        assertThat(videoCapabilities.getProfile(QualitySelector.QUALITY_SD)).isNull()
    }
}
