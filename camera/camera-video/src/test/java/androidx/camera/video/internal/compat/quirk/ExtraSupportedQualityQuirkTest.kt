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

package androidx.camera.video.internal.compat.quirk

import android.media.CamcorderProfile.QUALITY_480P
import android.media.CamcorderProfile.QUALITY_CIF
import android.media.CamcorderProfile.QUALITY_HIGH
import android.media.CamcorderProfile.QUALITY_LOW
import android.media.CamcorderProfile.QUALITY_QCIF
import android.os.Build
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.EncoderProfilesUtil.DEFAULT_DURATION
import androidx.camera.testing.impl.EncoderProfilesUtil.DEFAULT_OUTPUT_FORMAT
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_480P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_CIF
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_QCIF
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeAudioProfileProxy
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeVideoProfileProxy
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import androidx.camera.video.internal.utils.EncoderProfilesUtil.getFirstVideoProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ExtraSupportedQualityQuirkTest {
    companion object {
        private const val MOTO_C_BRAND = "motorola"
        private const val MOTO_C_MODEL = "moto c"
    }

    @Test
    fun motoC_frontCamera_canGetExtraSupportedQuality() {
        // Arrange: Simulate the condition of MotoC. See b/311311853.
        ShadowBuild.setBrand(MOTO_C_BRAND)
        ShadowBuild.setModel(MOTO_C_MODEL)
        val cameraInfo = FakeCameraInfoInternal("1", CameraSelector.LENS_FACING_FRONT)
        val profilesCif = EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
            DEFAULT_DURATION,
            DEFAULT_OUTPUT_FORMAT,
            listOf(createFakeAudioProfileProxy()),
            listOf(createFakeVideoProfileProxy(
                RESOLUTION_CIF.width,
                RESOLUTION_CIF.height,
            ))
        )
        val profilesQcif = EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
            DEFAULT_DURATION,
            DEFAULT_OUTPUT_FORMAT,
            listOf(createFakeAudioProfileProxy()),
            listOf(createFakeVideoProfileProxy(
                RESOLUTION_QCIF.width,
                RESOLUTION_QCIF.height,
            ))
        )
        val encoderProfileProvider = FakeEncoderProfilesProvider.Builder()
            .add(QUALITY_HIGH, profilesCif)
            .add(QUALITY_CIF, profilesCif)
            .add(QUALITY_QCIF, profilesQcif)
            .add(QUALITY_LOW, profilesQcif)
            .build()

        // Act.
        val qualityEncoderProfilesMap = ExtraSupportedQualityQuirk()
            .getExtraEncoderProfiles(cameraInfo, encoderProfileProvider) {
                FakeVideoEncoderInfo()
            }

        // Assert: check QUALITY_480P
        assertThat(qualityEncoderProfilesMap).isNotNull()
        assertThat(qualityEncoderProfilesMap).containsKey(QUALITY_480P)
        val profiles480p = qualityEncoderProfilesMap!![QUALITY_480P]
        val videoProfile480p = getFirstVideoProfile(profiles480p)!!
        assertThat(videoProfile480p.getResolution()).isEqualTo(RESOLUTION_480P)
        // Assert: QUALITY_HIGH is the same as QUALITY_480P
        assertThat(qualityEncoderProfilesMap).containsKey(QUALITY_HIGH)
        assertThat(qualityEncoderProfilesMap[QUALITY_HIGH]).isEqualTo(profiles480p)
    }

    private fun EncoderProfilesProxy.VideoProfileProxy.getResolution() = Size(width, height)
}
