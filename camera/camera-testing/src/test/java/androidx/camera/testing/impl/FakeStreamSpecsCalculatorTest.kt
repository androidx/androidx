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

package androidx.camera.testing.impl

import android.graphics.ImageFormat
import android.os.Build
import android.util.Range
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraMode
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class FakeStreamSpecsCalculatorTest {
    private val fakeStreamSpecsCalculator = FakeStreamSpecsCalculator()
    private val fakeCameraInfo = FakeCameraInfoInternal()

    @Test
    fun calculateSuggestedStreamSpecs_noSupportedSpecs_throwsException() {
        val useCase = Preview.Builder().build()

        assertThrows(IllegalArgumentException::class.java) {
            fakeStreamSpecsCalculator.calculateSuggestedStreamSpecs(
                cameraMode = CameraMode.DEFAULT,
                cameraInfoInternal = fakeCameraInfo,
                newUseCases = listOf(useCase),
            )
        }
    }

    @Test
    fun calculateSuggestedStreamSpecs_matchingSpecExists_returnsCorrectStreamSpec() {
        val dynamicRange = DynamicRange.HLG_10_BIT
        val frameRateRange = Range(24, 30)
        val stabilizationMode = StabilizationMode.ON

        val useCase =
            Preview.Builder()
                .setDynamicRange(dynamicRange)
                .setTargetFrameRate(frameRateRange)
                .setPreviewStabilizationEnabled(true)
                .build()

        val expectedStreamSpec =
            FakeStreamSpecsCalculator.ExtendedStreamSpec(
                dynamicRange = dynamicRange,
                expectedFrameRateRange = frameRateRange,
                imageFormat = INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                previewStabilizationMode = stabilizationMode,
            )

        fakeStreamSpecsCalculator.addSupportedStreamSpecs(expectedStreamSpec)

        val result =
            fakeStreamSpecsCalculator.calculateSuggestedStreamSpecs(
                cameraMode = CameraMode.DEFAULT,
                cameraInfoInternal = fakeCameraInfo,
                newUseCases = listOf(useCase),
            )

        assertThat(result.streamSpecs[useCase]).isEqualTo(expectedStreamSpec)
    }

    @Test
    fun calculateSuggestedStreamSpecs_multipleSpecsExist_returnsMatchingStreamSpec() {
        val previewDynamicRange = DynamicRange.HLG_10_BIT
        val previewFpsRange = Range(24, 30)
        val previewStabilizationMode = StabilizationMode.ON

        val preview =
            Preview.Builder()
                .setDynamicRange(previewDynamicRange)
                .setTargetFrameRate(previewFpsRange)
                .setPreviewStabilizationEnabled(true)
                .build()

        val imageCapture =
            ImageCapture.Builder()
                .setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
                .build()

        val expectedPreviewStreamSpec =
            FakeStreamSpecsCalculator.ExtendedStreamSpec(
                dynamicRange = previewDynamicRange,
                expectedFrameRateRange = previewFpsRange,
                imageFormat = INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                previewStabilizationMode = previewStabilizationMode,
            )

        val expectedImageCaptureStreamSpec =
            FakeStreamSpecsCalculator.ExtendedStreamSpec(
                dynamicRange = DynamicRange.UNSPECIFIED,
                expectedFrameRateRange = StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED,
                imageFormat = ImageFormat.JPEG_R,
                previewStabilizationMode = StabilizationMode.UNSPECIFIED,
            )

        fakeStreamSpecsCalculator.addSupportedStreamSpecs(
            expectedPreviewStreamSpec,
            expectedImageCaptureStreamSpec,
        )

        val result =
            fakeStreamSpecsCalculator.calculateSuggestedStreamSpecs(
                cameraMode = CameraMode.DEFAULT,
                cameraInfoInternal = fakeCameraInfo,
                newUseCases = listOf(preview, imageCapture),
            )

        assertThat(result.streamSpecs[preview]).isEqualTo(expectedPreviewStreamSpec)
        assertThat(result.streamSpecs[imageCapture]).isEqualTo(expectedImageCaptureStreamSpec)
    }
}
