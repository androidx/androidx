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

package androidx.camera.camera2.internal.compat.params

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.DynamicRangeProfiles
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.internal.DOLBY_VISION_10B_UNCONSTRAINED
import androidx.camera.camera2.internal.DOLBY_VISION_10B_UNCONSTRAINED_SLOW
import androidx.camera.camera2.internal.DOLBY_VISION_8B_UNCONSTRAINED
import androidx.camera.camera2.internal.HDR10_PLUS_UNCONSTRAINED
import androidx.camera.camera2.internal.HDR10_UNCONSTRAINED
import androidx.camera.camera2.internal.HLG10_CONSTRAINED
import androidx.camera.camera2.internal.HLG10_HDR10_CONSTRAINED
import androidx.camera.camera2.internal.HLG10_SDR_CONSTRAINED
import androidx.camera.camera2.internal.HLG10_UNCONSTRAINED
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT
import androidx.camera.core.DynamicRange.DOLBY_VISION_8_BIT
import androidx.camera.core.DynamicRange.HDR10_10_BIT
import androidx.camera.core.DynamicRange.HDR10_PLUS_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class DynamicRangesCompatTest {
    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canWrapAndUnwrapDynamicRangeProfiles() {
        val dynamicRangesCompat = DynamicRangesCompat.toDynamicRangesCompat(HLG10_UNCONSTRAINED)

        assertThat(dynamicRangesCompat).isNotNull()
        assertThat(dynamicRangesCompat?.toDynamicRangeProfiles()).isEqualTo(HLG10_UNCONSTRAINED)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canSupportDynamicRangeFromHlg10Profile() {
        val dynamicRangesCompat = DynamicRangesCompat.toDynamicRangesCompat(HLG10_UNCONSTRAINED)
        assertThat(dynamicRangesCompat?.supportedDynamicRanges).contains(HLG_10_BIT)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canSupportDynamicRangeFromHdr10Profile() {
        val dynamicRangesCompat = DynamicRangesCompat.toDynamicRangesCompat(HDR10_UNCONSTRAINED)
        assertThat(dynamicRangesCompat?.supportedDynamicRanges).contains(HDR10_10_BIT)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canSupportDynamicRangeFromHdr10PlusProfile() {
        val dynamicRangesCompat =
            DynamicRangesCompat.toDynamicRangesCompat(HDR10_PLUS_UNCONSTRAINED)
        assertThat(dynamicRangesCompat?.supportedDynamicRanges).contains(HDR10_PLUS_10_BIT)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canSupportDynamicRangeFromDolbyVision10bProfile() {
        val dynamicRangesCompat =
            DynamicRangesCompat.toDynamicRangesCompat(DOLBY_VISION_10B_UNCONSTRAINED)
        assertThat(dynamicRangesCompat?.supportedDynamicRanges).contains(DOLBY_VISION_10_BIT)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canSupportDynamicRangeFromDolbyVision8bProfile() {
        val dynamicRangesCompat =
            DynamicRangesCompat.toDynamicRangesCompat(DOLBY_VISION_8B_UNCONSTRAINED)
        assertThat(dynamicRangesCompat?.supportedDynamicRanges).contains(DOLBY_VISION_8_BIT)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canProduceConcurrentDynamicRangeConstraints() {
        val hlg10ConstrainedWrapped = DynamicRangesCompat.toDynamicRangesCompat(HLG10_CONSTRAINED)
        assertThat(
            hlg10ConstrainedWrapped?.getDynamicRangeCaptureRequestConstraints(SDR)
        ).containsExactly(SDR)
        assertThat(
            hlg10ConstrainedWrapped?.getDynamicRangeCaptureRequestConstraints(
                HLG_10_BIT
            )
        ).containsExactly(HLG_10_BIT)

        val hlg10SdrConstrainedWrapped =
            DynamicRangesCompat.toDynamicRangesCompat(HLG10_SDR_CONSTRAINED)
        assertThat(
            hlg10SdrConstrainedWrapped?.getDynamicRangeCaptureRequestConstraints(SDR)
        ).containsExactly(SDR, HLG_10_BIT)
        assertThat(
            hlg10SdrConstrainedWrapped?.getDynamicRangeCaptureRequestConstraints(
                HLG_10_BIT
            )
        ).containsExactly(HLG_10_BIT, SDR)

        val hlg10Hdr10ConstrainedWrapped =
            DynamicRangesCompat.toDynamicRangesCompat(HLG10_HDR10_CONSTRAINED)
        assertThat(
            hlg10Hdr10ConstrainedWrapped?.getDynamicRangeCaptureRequestConstraints(SDR)
        ).containsExactly(SDR)
        assertThat(
            hlg10Hdr10ConstrainedWrapped?.getDynamicRangeCaptureRequestConstraints(
                HLG_10_BIT
            )
        ).containsExactly(HLG_10_BIT, HDR10_10_BIT)
        assertThat(
            hlg10Hdr10ConstrainedWrapped?.getDynamicRangeCaptureRequestConstraints(HDR10_10_BIT)
        ).containsExactly(HDR10_10_BIT, HLG_10_BIT)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun producesDynamicRangeWithCorrectLatency() {
        val dynamicRangesCompat =
            DynamicRangesCompat.toDynamicRangesCompat(DOLBY_VISION_10B_UNCONSTRAINED_SLOW)
        assertThat(dynamicRangesCompat?.isExtraLatencyPresent(SDR)).isFalse()
        assertThat(
            dynamicRangesCompat?.isExtraLatencyPresent(DOLBY_VISION_10_BIT)
        ).isTrue()
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canProduceDynamicRangeWithoutConstraints() {
        val dynamicRangesCompat = DynamicRangesCompat.toDynamicRangesCompat(HLG10_UNCONSTRAINED)
        assertThat(
            dynamicRangesCompat?.getDynamicRangeCaptureRequestConstraints(HLG_10_BIT)
        ).isEmpty()
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun producesNullDynamicRangeProfilesFromNullCharacteristics() {
        val characteristics = newCameraCharacteristicsCompat()

        val dynamicRangesCompat = DynamicRangesCompat.fromCameraCharacteristics(characteristics)

        assertThat(dynamicRangesCompat.toDynamicRangeProfiles()).isNull()
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canProduceDynamicRangesCompatFromCharacteristics() {
        val characteristics = newCameraCharacteristicsCompat()
        Shadow.extract<ShadowCameraCharacteristics>(
            characteristics.toCameraCharacteristics()
        ).addDynamicRangeProfiles(HLG10_CONSTRAINED)

        val dynamicRangesCompat = DynamicRangesCompat.fromCameraCharacteristics(characteristics)

        assertThat(dynamicRangesCompat.toDynamicRangeProfiles()).isEqualTo(HLG10_CONSTRAINED)
    }

    @Test
    fun alwaysSupportsOnlySdrWithoutDynamicRangeProfilesInCharacteristics() {
        val characteristics = newCameraCharacteristicsCompat()

        val dynamicRangesCompat = DynamicRangesCompat.fromCameraCharacteristics(characteristics)

        assertThat(dynamicRangesCompat.supportedDynamicRanges).containsExactly(SDR)
        assertThat(
            dynamicRangesCompat.getDynamicRangeCaptureRequestConstraints(SDR)
        ).containsExactly(SDR)
    }

    @Test
    fun unsupportedDynamicRangeAlwaysThrowsException() {
        val characteristics = newCameraCharacteristicsCompat()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Shadow.extract<ShadowCameraCharacteristics>(
                characteristics.toCameraCharacteristics()
            ).addDynamicRangeProfiles(DOLBY_VISION_8B_UNCONSTRAINED)
        }

        val dynamicRangesCompat = DynamicRangesCompat.fromCameraCharacteristics(characteristics)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            assertThat(dynamicRangesCompat.supportedDynamicRanges).containsExactly(SDR)
        } else {
            assertThat(dynamicRangesCompat.supportedDynamicRanges).containsExactly(
                SDR, DOLBY_VISION_8_BIT
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            dynamicRangesCompat.getDynamicRangeCaptureRequestConstraints(DOLBY_VISION_10_BIT)
        }

        assertThrows(IllegalArgumentException::class.java) {
            dynamicRangesCompat.isExtraLatencyPresent(DOLBY_VISION_10_BIT)
        }
    }

    @Test
    fun sdrHasNoExtraLatency() {
        val characteristics = newCameraCharacteristicsCompat()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Shadow.extract<ShadowCameraCharacteristics>(
                characteristics.toCameraCharacteristics()
            ).addDynamicRangeProfiles(HLG10_CONSTRAINED)
        }

        val dynamicRangesCompat = DynamicRangesCompat.fromCameraCharacteristics(characteristics)

        assertThat(dynamicRangesCompat.isExtraLatencyPresent(SDR)).isFalse()
    }

    @Test
    fun sdrHasSdrConstraint_whenConcurrentDynamicRangesNotSupported() {
        val characteristics = newCameraCharacteristicsCompat()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Shadow.extract<ShadowCameraCharacteristics>(
                characteristics.toCameraCharacteristics()
            ).addDynamicRangeProfiles(HLG10_CONSTRAINED)
        }

        val dynamicRangesCompat = DynamicRangesCompat.fromCameraCharacteristics(characteristics)

        assertThat(dynamicRangesCompat.getDynamicRangeCaptureRequestConstraints(SDR))
            .containsExactly(SDR)
    }
}

private const val CAMERA_ID = "0"

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun newCameraCharacteristicsCompat(): CameraCharacteristicsCompat {
    return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
        ShadowCameraCharacteristics.newCameraCharacteristics(),
        CAMERA_ID
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun ShadowCameraCharacteristics.addDynamicRangeProfiles(
    dynamicRangeProfiles: DynamicRangeProfiles
) {
    set(
        CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES,
        dynamicRangeProfiles
    )
}
