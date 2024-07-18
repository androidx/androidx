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

package androidx.camera.camera2.pipe.integration.compat

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.DynamicRangeProfiles
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.internal.DOLBY_VISION_10B_UNCONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.DOLBY_VISION_10B_UNCONSTRAINED_SLOW
import androidx.camera.camera2.pipe.integration.internal.DOLBY_VISION_8B_UNCONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.HDR10_PLUS_UNCONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.HDR10_UNCONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.HLG10_CONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.HLG10_HDR10_CONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.HLG10_SDR_CONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.HLG10_UNCONSTRAINED
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.DynamicRange
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowCameraCharacteristics

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class DynamicRangeProfilesCompatTest {

    private val cameraId = CameraId.fromCamera1Id(0)

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canWrapAndUnwrapDynamicRangeProfiles() {
        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.toDynamicRangesCompat(HLG10_UNCONSTRAINED)

        Truth.assertThat(dynamicRangeProfilesCompat).isNotNull()
        Truth.assertThat(dynamicRangeProfilesCompat?.toDynamicRangeProfiles())
            .isEqualTo(HLG10_UNCONSTRAINED)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canSupportDynamicRangeFromHlg10Profile() {
        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.toDynamicRangesCompat(HLG10_UNCONSTRAINED)
        Truth.assertThat(dynamicRangeProfilesCompat?.getSupportedDynamicRanges())
            .contains(DynamicRange.HLG_10_BIT)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canSupportDynamicRangeFromHdr10Profile() {
        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.toDynamicRangesCompat(HDR10_UNCONSTRAINED)
        Truth.assertThat(dynamicRangeProfilesCompat?.getSupportedDynamicRanges())
            .contains(DynamicRange.HDR10_10_BIT)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canSupportDynamicRangeFromHdr10PlusProfile() {
        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.toDynamicRangesCompat(HDR10_PLUS_UNCONSTRAINED)
        Truth.assertThat(dynamicRangeProfilesCompat?.getSupportedDynamicRanges())
            .contains(DynamicRange.HDR10_PLUS_10_BIT)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canSupportDynamicRangeFromDolbyVision10bProfile() {
        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.toDynamicRangesCompat(DOLBY_VISION_10B_UNCONSTRAINED)
        Truth.assertThat(dynamicRangeProfilesCompat?.getSupportedDynamicRanges())
            .contains(DynamicRange.DOLBY_VISION_10_BIT)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canSupportDynamicRangeFromDolbyVision8bProfile() {
        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.toDynamicRangesCompat(DOLBY_VISION_8B_UNCONSTRAINED)
        Truth.assertThat(dynamicRangeProfilesCompat?.getSupportedDynamicRanges())
            .contains(DynamicRange.DOLBY_VISION_8_BIT)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canProduceConcurrentDynamicRangeConstraints() {
        val hlg10ConstrainedWrapped =
            DynamicRangeProfilesCompat.toDynamicRangesCompat(HLG10_CONSTRAINED)
        Truth.assertThat(
            hlg10ConstrainedWrapped?.getDynamicRangeCaptureRequestConstraints(DynamicRange.SDR)
        ).containsExactly(DynamicRange.SDR)
        Truth.assertThat(
            hlg10ConstrainedWrapped?.getDynamicRangeCaptureRequestConstraints(
                DynamicRange.HLG_10_BIT
            )
        ).containsExactly(DynamicRange.HLG_10_BIT)

        val hlg10SdrConstrainedWrapped =
            DynamicRangeProfilesCompat.toDynamicRangesCompat(HLG10_SDR_CONSTRAINED)
        Truth.assertThat(
            hlg10SdrConstrainedWrapped?.getDynamicRangeCaptureRequestConstraints(DynamicRange.SDR)
        ).containsExactly(DynamicRange.SDR, DynamicRange.HLG_10_BIT)
        Truth.assertThat(
            hlg10SdrConstrainedWrapped?.getDynamicRangeCaptureRequestConstraints(
                DynamicRange.HLG_10_BIT
            )
        ).containsExactly(DynamicRange.HLG_10_BIT, DynamicRange.SDR)

        val hlg10Hdr10ConstrainedWrapped =
            DynamicRangeProfilesCompat.toDynamicRangesCompat(HLG10_HDR10_CONSTRAINED)
        Truth.assertThat(
            hlg10Hdr10ConstrainedWrapped?.getDynamicRangeCaptureRequestConstraints(DynamicRange.SDR)
        ).containsExactly(DynamicRange.SDR)
        Truth.assertThat(
            hlg10Hdr10ConstrainedWrapped?.getDynamicRangeCaptureRequestConstraints(
                DynamicRange.HLG_10_BIT
            )
        ).containsExactly(DynamicRange.HLG_10_BIT, DynamicRange.HDR10_10_BIT)
        Truth.assertThat(
            hlg10Hdr10ConstrainedWrapped
                ?.getDynamicRangeCaptureRequestConstraints(DynamicRange.HDR10_10_BIT)
        ).containsExactly(DynamicRange.HDR10_10_BIT, DynamicRange.HLG_10_BIT)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun producesDynamicRangeWithCorrectLatency() {
        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.toDynamicRangesCompat(DOLBY_VISION_10B_UNCONSTRAINED_SLOW)
        Truth.assertThat(dynamicRangeProfilesCompat?.isExtraLatencyPresent(DynamicRange.SDR))
            .isFalse()
        Truth.assertThat(
            dynamicRangeProfilesCompat?.isExtraLatencyPresent(DynamicRange.DOLBY_VISION_10_BIT)
        ).isTrue()
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canProduceDynamicRangeWithoutConstraints() {
        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.toDynamicRangesCompat(HLG10_UNCONSTRAINED)
        Truth.assertThat(
            dynamicRangeProfilesCompat?.getDynamicRangeCaptureRequestConstraints(
                DynamicRange.HLG_10_BIT
            )
        ).isEmpty()
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun producesNullDynamicRangeProfilesFromNullCharacteristics() {
        val cameraMetadata = FakeCameraMetadata(cameraId = cameraId)

        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.fromCameraMetaData(cameraMetadata)

        Truth.assertThat(dynamicRangeProfilesCompat.toDynamicRangeProfiles()).isNull()
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canProduceDynamicRangesCompatFromCharacteristics() {
        val cameraMetadata = FakeCameraMetadata(
            cameraId = cameraId, characteristics = mutableMapOf(
                CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES to HLG10_CONSTRAINED
            )
        )

        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.fromCameraMetaData(cameraMetadata)

        Truth.assertThat(dynamicRangeProfilesCompat.toDynamicRangeProfiles())
            .isEqualTo(HLG10_CONSTRAINED)
    }

    @Test
    fun alwaysSupportsOnlySdrWithoutDynamicRangeProfilesInCharacteristics() {
        val cameraMetadata = FakeCameraMetadata(cameraId = cameraId)

        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.fromCameraMetaData(cameraMetadata)

        Truth.assertThat(dynamicRangeProfilesCompat.getSupportedDynamicRanges())
            .containsExactly(DynamicRange.SDR)
        Truth.assertThat(
            dynamicRangeProfilesCompat.getDynamicRangeCaptureRequestConstraints(DynamicRange.SDR)
        ).containsExactly(DynamicRange.SDR)
    }

    @Test
    fun unsupportedDynamicRangeAlwaysThrowsException() {
        val characteristics = mutableMapOf<CameraCharacteristics.Key<*>, Any?>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            characteristics[CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES] =
                DOLBY_VISION_8B_UNCONSTRAINED
        }
        val cameraMetadata = FakeCameraMetadata(
            cameraId = cameraId, characteristics = characteristics
        )

        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.fromCameraMetaData(cameraMetadata)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Truth.assertThat(dynamicRangeProfilesCompat.getSupportedDynamicRanges())
                .containsExactly(DynamicRange.SDR)
        } else {
            Truth.assertThat(dynamicRangeProfilesCompat.getSupportedDynamicRanges())
                .containsExactly(
                    DynamicRange.SDR, DynamicRange.DOLBY_VISION_8_BIT
                )
        }

        Assert.assertThrows(IllegalArgumentException::class.java) {
            dynamicRangeProfilesCompat
                .getDynamicRangeCaptureRequestConstraints(DynamicRange.DOLBY_VISION_10_BIT)
        }

        Assert.assertThrows(IllegalArgumentException::class.java) {
            dynamicRangeProfilesCompat.isExtraLatencyPresent(DynamicRange.DOLBY_VISION_10_BIT)
        }
    }

    @Test
    fun sdrHasNoExtraLatency() {
        val characteristics = mutableMapOf<CameraCharacteristics.Key<*>, Any?>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            characteristics[CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES] =
                HLG10_CONSTRAINED
        }
        val cameraMetadata = FakeCameraMetadata(
            cameraId = cameraId, characteristics = characteristics
        )
        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.fromCameraMetaData(cameraMetadata)

        Truth.assertThat(dynamicRangeProfilesCompat.isExtraLatencyPresent(DynamicRange.SDR))
            .isFalse()
    }

    @Test
    fun sdrHasSdrConstraint_whenConcurrentDynamicRangesNotSupported() {
        val characteristics = mutableMapOf<CameraCharacteristics.Key<*>, Any?>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            characteristics[CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES] =
                HLG10_CONSTRAINED
        }
        val cameraMetadata = FakeCameraMetadata(
            cameraId = cameraId, characteristics = characteristics
        )
        val dynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat.fromCameraMetaData(cameraMetadata)

        Truth.assertThat(
            dynamicRangeProfilesCompat.getDynamicRangeCaptureRequestConstraints(DynamicRange.SDR)
        )
            .containsExactly(DynamicRange.SDR)
    }
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
