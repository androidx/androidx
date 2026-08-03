/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.util.Size
import android.util.SizeF
import androidx.camera.common.testing.FakeCameraCharacteristics
import androidx.camera.common.testing.FakeCameraIds
import androidx.camera.common.testing.FakeCaptureResult
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class CameraLensMathTest {

    private companion object {
        const val FLOAT_TOLERANCE = 0.0001f

        // Default test sensor setup: 8.0mm x 6.0mm (4:3 aspect ratio)
        const val DEFAULT_PHYSICAL_WIDTH = 8.0f
        const val DEFAULT_PHYSICAL_HEIGHT = 6.0f
        val DEFAULT_PHYSICAL_SIZE = SizeF(DEFAULT_PHYSICAL_WIDTH, DEFAULT_PHYSICAL_HEIGHT)
        const val DEFAULT_PIXEL_WIDTH = 4000
        const val DEFAULT_PIXEL_HEIGHT = 3000
        val DEFAULT_PIXEL_SIZE = Size(DEFAULT_PIXEL_WIDTH, DEFAULT_PIXEL_HEIGHT)
        const val DEFAULT_FOCAL_LENGTH = 2.0f

        // Full-frame SLR sensor: 36.0mm x 24.0mm (3:2 aspect ratio)
        const val SLR_PHYSICAL_WIDTH = 36.0f
        const val SLR_PHYSICAL_HEIGHT = 24.0f
        val SLR_PHYSICAL_SIZE = SizeF(SLR_PHYSICAL_WIDTH, SLR_PHYSICAL_HEIGHT)
        const val SLR_PIXEL_WIDTH = 6000
        const val SLR_PIXEL_HEIGHT = 4000
        val SLR_PIXEL_SIZE = Size(SLR_PIXEL_WIDTH, SLR_PIXEL_HEIGHT)
    }

    @Test
    fun computeFov_basic() {
        // 2 * atan(2 / (2 * 1)) = 2 * atan(1) = 2 * 45 = 90 degrees
        assertThat(CameraLensMath.computeFovFromFocalLength(1f, 2f))
            .isWithin(FLOAT_TOLERANCE)
            .of(90f)
    }

    @Test
    fun computeFov_physicalOnly() {
        // Sensor: 8mm x 6mm (4:3)
        // Focal length: 2.0mm

        // Long edge: 8mm -> FoV = 2 * atan(8 / (2 * 2)) = 2 * atan(2) = 126.8699 degrees
        var fov =
            CameraLensMath.computeFov(
                focalLengthMm = 2.0f,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(126.8699f)

        // Short edge: 6mm -> FoV = 2 * atan(6 / (2 * 2)) = 2 * atan(1.5) = 112.61986 degrees
        fov =
            CameraLensMath.computeFov(
                focalLengthMm = 2.0f,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                fovMode = CameraLensMath.FOV_MODE_CROP_SHORT_EDGE,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(112.61986f)

        // Diagonal: hypot(8, 6) = 10mm -> FoV = 2 * atan(10 / (2 * 2)) = 2 * atan(2.5) =
        // 136.39719 degrees
        fov =
            CameraLensMath.computeFov(
                focalLengthMm = 2.0f,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                fovMode = CameraLensMath.FOV_MODE_CROP_DIAGONAL,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(136.39719f)
    }

    @Test
    fun computeFov_defaultMode() {
        // Default mode should be FOV_MODE_CROP_LONG_EDGE.
        // Crop: 2000 x 1500 -> Physical crop: 4mm x 3mm
        // Long edge: 4mm
        // Focal length: 2.0mm
        // FoV = 2 * atan(4 / (2 * 2.0)) = 90 degrees
        val fov =
            CameraLensMath.computeFov(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                cropRegion = Rect(0, 0, 2000, 1500),
                // fovMode omitted, should default to FOV_MODE_CROP_LONG_EDGE
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(90f)
    }

    @Test
    fun computeFov_cropShortEdge() {
        // Crop: 2000 x 1500 -> Physical crop: 4mm x 3mm
        // Short edge: 3mm
        // Focal length: 2.0mm (DEFAULT_FOCAL_LENGTH)
        // FoV = 2 * atan(3 / (2 * 2.0)) = 2 * atan(0.75) = 73.739796 degrees
        val fov =
            CameraLensMath.computeFov(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                cropRegion = Rect(0, 0, 2000, 1500),
                fovMode = CameraLensMath.FOV_MODE_CROP_SHORT_EDGE,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(73.739796f)
    }

    @Test
    fun computeFov_cropLongEdge() {
        // Crop: 2000 x 1500 -> Physical crop: 4mm x 3mm
        // Long edge: 4mm
        // Focal length: 2.0mm
        // FoV = 2 * atan(4 / (2 * 2.0)) = 90 degrees
        val fov =
            CameraLensMath.computeFov(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                cropRegion = Rect(0, 0, 2000, 1500),
                fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(90f)
    }

    @Test
    fun computeFov_cropDiagonal() {
        // Crop: 2000 x 1500 -> Physical crop: 4mm x 3mm
        // Diagonal: 5mm (3-4-5 triangle)
        // Focal length: 2.0mm (DEFAULT_FOCAL_LENGTH)
        // FoV = 2 * atan(5 / (2 * 2.0)) = 2 * atan(1.25) = 102.68038 degrees
        val fov =
            CameraLensMath.computeFov(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                cropRegion = Rect(0, 0, 2000, 1500),
                fovMode = CameraLensMath.FOV_MODE_CROP_DIAGONAL,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(102.68038f)
    }

    @Test
    fun computeFov_withZoom_defaultMode() {
        // Crop: 2000 x 1500 -> Physical crop: 4mm x 3mm
        // Zoom Ratio: 1.25
        // Default mode (FOV_MODE_CROP_LONG_EDGE)
        // Effective physical crop width: 4.0mm / 1.25 = 3.2mm
        // Focal length: 2.0mm
        // FoV = 2 * atan(3.2 / 4.0) = 77.31961 degrees
        val fov =
            CameraLensMath.computeFov(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                cropRegion = Rect(0, 0, 2000, 1500),
                zoomRatio = 1.25f,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(77.31961f)
    }

    @Test
    fun computeFov_withZoom_cropLongEdge() {
        // Width: 4mm / 1.25 = 3.2mm. FoV = 77.31961 degrees
        val fov =
            CameraLensMath.computeFov(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                cropRegion = Rect(0, 0, 2000, 1500),
                zoomRatio = 1.25f,
                fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(77.31961f)
    }

    @Test
    fun computeFov_withZoom_cropShortEdge() {
        // Height: 3mm / 1.25 = 2.4mm. FoV = 2 * atan(2.4 / 4) = 61.92751 degrees
        val fov =
            CameraLensMath.computeFov(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                cropRegion = Rect(0, 0, 2000, 1500),
                zoomRatio = 1.25f,
                fovMode = CameraLensMath.FOV_MODE_CROP_SHORT_EDGE,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(61.92751f)
    }

    @Test
    fun computeFov_withZoom_cropDiagonal() {
        // Diagonal: hypot(4, 3) = 5.0mm. With Zoom 1.25: 4.0mm. FoV = 2 * atan(4 / 4) = 90 degrees
        val fov =
            CameraLensMath.computeFov(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                cropRegion = Rect(0, 0, 2000, 1500),
                zoomRatio = 1.25f,
                fovMode = CameraLensMath.FOV_MODE_CROP_DIAGONAL,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(90f)
    }

    @Test
    fun computeFov_aspectRatioFit() {
        // Sensor: 8mm x 6mm (4:3) -> Pixel: 4000 x 3000
        // Crop region: 2000 x 1500 (4:3)
        // Stream size: 1600 x 900 (16:9)
        // Effective crop: 2000 x 1125 (16:9)
        // Long edge: 2000 pixels (4.0mm) -> FoV 90.0
        val fovLong =
            CameraLensMath.computeFov(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                cropRegion = Rect(0, 0, 2000, 1500),
                streamSize = Size(1600, 900),
                fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE,
            )
        assertThat(fovLong).isWithin(FLOAT_TOLERANCE).of(90f)

        // Short edge: 1125 pixels (2.25mm) -> FoV 58.71551 degrees
        val fovShort =
            CameraLensMath.computeFov(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                cropRegion = Rect(0, 0, 2000, 1500),
                streamSize = Size(1600, 900),
                fovMode = CameraLensMath.FOV_MODE_CROP_SHORT_EDGE,
            )
        assertThat(fovShort).isWithin(FLOAT_TOLERANCE).of(58.71551f)
    }

    @Test
    fun computeFov_sensorMode_diagonal_16x9CropOn4x3Sensor() {
        // Sensor: 8mm x 6mm (4:3), Crop: 2000 x 1125 (16:9)
        // Width zoom: 4000 / 2000 = 2.0 -> Effective 4:3 size: 4mm x 3mm -> Diagonal: 5mm
        // Focal length: 2.5mm -> FoV: 90 degrees
        val fov =
            CameraLensMath.computeFov(
                focalLengthMm = 2.5f,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                cropRegion = Rect(0, 0, 2000, 1125),
                fovMode = CameraLensMath.FOV_MODE_SENSOR_DIAGONAL,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(90f)
    }

    @Test
    fun computeFov_sensorMode_diagonal_4x3CropOn16x9Sensor() {
        // Sensor: 8mm x 4.5mm (16:9) -> Pixel: 4000 x 2250
        // Crop: 1500 x 1125 (4:3)
        // Height zoom: 2250 / 1125 = 2.0 -> Effective 16:9 size: 4mm x 2.25mm
        // Diagonal: hypot(4.0, 2.25) = 4.5825756mm
        // Focal length: 2.0mm (DEFAULT_FOCAL_LENGTH)
        // FoV = 2 * atan(4.5825756 / (2 * 2.0)) = 97.7471 degrees
        val fov =
            CameraLensMath.computeFov(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = SizeF(DEFAULT_PHYSICAL_WIDTH, 4.5f),
                sensorPixelSize = Size(DEFAULT_PIXEL_WIDTH, 2250),
                cropRegion = Rect(0, 0, 1500, 1125),
                fovMode = CameraLensMath.FOV_MODE_SENSOR_DIAGONAL,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(97.85078f)
    }

    @Test
    fun computeFov_sensorMode_withZoom() {
        // Sensor: 8mm x 6mm (4:3), Crop: 2000 x 1125 (16:9)
        // Zoom Ratio: 1.25
        // Base effective 4:3 size: 4mm x 3mm
        // With Zoom 1.25: 3.2mm x 2.4mm -> Diagonal: 4.0mm
        // Focal length: 2.0mm -> FoV: 90 degrees
        val fov =
            CameraLensMath.computeFov(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                cropRegion = Rect(0, 0, 2000, 1125),
                zoomRatio = 1.25f,
                fovMode = CameraLensMath.FOV_MODE_SENSOR_DIAGONAL,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(90f)
    }

    @Test
    fun computeFovFromZoomRatio_basic() {
        // Stream: 1920 x 1080 (16:9), Zoom: 2.0
        // Base crop at 1.0 zoom: 4000 x 2250 -> Physical: 8mm x 4.5mm
        // At zoom 2.0: 4mm x 2.25mm -> Long edge: 4mm
        // Focal length: 2.0mm -> FoV: 90 degrees
        val fov =
            CameraLensMath.computeFovFromZoomRatio(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                zoomRatio = 2.0f,
                streamSize = Size(1920, 1080),
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(90f)
    }

    @Test
    fun computeFovFromZoomRatio_withEstimatedEis() {
        // Stream: 1920 x 1080 (16:9), Zoom: 1.6, Estimated EIS: 20% (0.2)
        // Base physical: 8mm x 4.5mm
        // Effective size: (8 / 1.6) * 0.8 = 4.0mm -> Long edge: 4.0mm
        // Focal length: 2.0mm -> FoV: 90 degrees
        val fov =
            CameraLensMath.computeFovFromZoomRatio(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                zoomRatio = 1.6f,
                streamSize = Size(1920, 1080),
                estimatedEisMargin = 0.2f,
                fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE,
            )
        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(90f)
    }

    @Test
    fun computeZoomRatioFromFov_basic() {
        // targetFoV: 90, focal length: 2.0 -> Target dimension: 4.0mm
        // Base long edge (16:9 stream): 8.0mm -> Zoom: 8.0 / 4.0 = 2.0
        val zoomRatio =
            CameraLensMath.computeZoomRatioFromFov(
                fovDegrees = 90f,
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                streamSize = Size(1920, 1080),
                fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE,
            )
        assertThat(zoomRatio).isWithin(FLOAT_TOLERANCE).of(2.0f)
    }

    @Test
    fun computeZoomRatioFromFov_withEstimatedEis() {
        // targetFoV: 90, focal length: 2.0 -> Target dimension: 4.0mm
        // Base long edge (16:9 stream): 8.0mm. Estimated EIS: 20% (0.2)
        // Zoom: (8.0 * 0.8) / 4.0 = 1.6
        val zoomRatio =
            CameraLensMath.computeZoomRatioFromFov(
                fovDegrees = 90f,
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                streamSize = Size(1920, 1080),
                estimatedEisMargin = 0.2f,
                fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE,
            )
        assertThat(zoomRatio).isWithin(FLOAT_TOLERANCE).of(1.6f)
    }

    // =============================================================================================
    // Baseline Tests (Full-frame SLR: 36mm x 24mm)
    // =============================================================================================

    @Test
    fun computeFov_baseline_50mmLens_slr() {
        // Horizontal: 2 * atan(36 / 100) = 39.59775 degrees
        val horizontalFov =
            CameraLensMath.computeFov(
                focalLengthMm = 50.0f,
                sensorPhysicalSizeMm = SLR_PHYSICAL_SIZE,
                sensorPixelSize = SLR_PIXEL_SIZE,
                cropRegion = Rect(0, 0, SLR_PIXEL_WIDTH, SLR_PIXEL_HEIGHT),
                fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE,
            )
        assertThat(horizontalFov).isWithin(FLOAT_TOLERANCE).of(39.59775f)

        // Vertical: 2 * atan(24 / 100) = 26.99147 degrees
        val verticalFov =
            CameraLensMath.computeFov(
                focalLengthMm = 50.0f,
                sensorPhysicalSizeMm = SLR_PHYSICAL_SIZE,
                sensorPixelSize = SLR_PIXEL_SIZE,
                cropRegion = Rect(0, 0, SLR_PIXEL_WIDTH, SLR_PIXEL_HEIGHT),
                fovMode = CameraLensMath.FOV_MODE_CROP_SHORT_EDGE,
            )
        assertThat(verticalFov).isWithin(FLOAT_TOLERANCE).of(26.99147f)

        // Diagonal: hypot(36, 24) = 43.2666mm. 2 * atan(43.2666 / 100) = 46.79300 degrees
        val diagonalFov =
            CameraLensMath.computeFov(
                focalLengthMm = 50.0f,
                sensorPhysicalSizeMm = SLR_PHYSICAL_SIZE,
                sensorPixelSize = SLR_PIXEL_SIZE,
                cropRegion = Rect(0, 0, SLR_PIXEL_WIDTH, SLR_PIXEL_HEIGHT),
                fovMode = CameraLensMath.FOV_MODE_CROP_DIAGONAL,
            )
        assertThat(diagonalFov).isWithin(FLOAT_TOLERANCE).of(46.79300f)
    }

    @Test
    fun computeFov_baseline_300mmLens_slr() {
        // Horizontal: 2 * atan(36 / 600) = 6.86726 degrees
        val horizontalFov =
            CameraLensMath.computeFov(
                focalLengthMm = 300.0f,
                sensorPhysicalSizeMm = SLR_PHYSICAL_SIZE,
                sensorPixelSize = SLR_PIXEL_SIZE,
                cropRegion = Rect(0, 0, SLR_PIXEL_WIDTH, SLR_PIXEL_HEIGHT),
                fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE,
            )
        assertThat(horizontalFov).isWithin(FLOAT_TOLERANCE).of(6.86726f)

        // Vertical: 2 * atan(24 / 600) = 4.58122 degrees
        val verticalFov =
            CameraLensMath.computeFov(
                focalLengthMm = 300.0f,
                sensorPhysicalSizeMm = SLR_PHYSICAL_SIZE,
                sensorPixelSize = SLR_PIXEL_SIZE,
                cropRegion = Rect(0, 0, SLR_PIXEL_WIDTH, SLR_PIXEL_HEIGHT),
                fovMode = CameraLensMath.FOV_MODE_CROP_SHORT_EDGE,
            )
        assertThat(verticalFov).isWithin(FLOAT_TOLERANCE).of(4.58122f)

        // Diagonal: hypot(36, 24) = 43.2666mm. 2 * atan(43.2666 / 600) = 8.24904 degrees
        val diagonalFov =
            CameraLensMath.computeFov(
                focalLengthMm = 300.0f,
                sensorPhysicalSizeMm = SLR_PHYSICAL_SIZE,
                sensorPixelSize = SLR_PIXEL_SIZE,
                cropRegion = Rect(0, 0, SLR_PIXEL_WIDTH, SLR_PIXEL_HEIGHT),
                fovMode = CameraLensMath.FOV_MODE_CROP_DIAGONAL,
            )
        assertThat(diagonalFov).isWithin(FLOAT_TOLERANCE).of(8.24904f)
    }

    // =============================================================================================
    // Symmetry Tests
    // =============================================================================================

    @Test
    fun computeFov_symmetry_focalLength() {
        val focalLengthsToTest = listOf(1.5f, 2.0f, 4.0f, 10.0f, 50.0f, 300.0f)
        val physicalLengthsToTest = listOf(2.0f, 4.0f, 8.0f, 24.0f, 36.0f)

        for (focalLength in focalLengthsToTest) {
            for (physicalLength in physicalLengthsToTest) {
                // 1. Compute FoV
                val fov = CameraLensMath.computeFovFromFocalLength(focalLength, physicalLength)

                // 2. Re-compute focal length from FoV
                val computedFocalLength =
                    CameraLensMath.computeFocalLengthFromFov(fov, physicalLength)

                // 3. Verify they are symmetric
                assertThat(computedFocalLength).isWithin(FLOAT_TOLERANCE).of(focalLength)
            }
        }
    }

    @Test
    fun computeFov_symmetry_zoomRatio() {
        val targetFovDegrees = 90.0f
        val streamSize = Size(1920, 1080)

        // 1. Compute required Zoom Ratio to achieve target FoV (90 deg)
        val zoomRatio =
            CameraLensMath.computeZoomRatioFromFov(
                fovDegrees = targetFovDegrees,
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                streamSize = streamSize,
                fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE,
            )

        // 2. Re-compute FoV when that zoom is applied
        val recomputedFov =
            CameraLensMath.computeFovFromZoomRatio(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                zoomRatio = zoomRatio,
                streamSize = streamSize,
            )

        // 3. Verify it matches target FoV
        assertThat(recomputedFov).isWithin(FLOAT_TOLERANCE).of(targetFovDegrees)
    }

    @Test
    fun computeFov_symmetry_zoomRatio_withEstimatedEis() {
        val targetFovDegrees = 90.0f
        val streamSize = Size(1920, 1080)
        val estimatedEisMargin = 0.2f

        // 1. Compute required Zoom Ratio to achieve target FoV (90 deg) with Estimated EIS
        val zoomRatio =
            CameraLensMath.computeZoomRatioFromFov(
                fovDegrees = targetFovDegrees,
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                streamSize = streamSize,
                estimatedEisMargin = estimatedEisMargin,
                fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE,
            )

        // 2. Re-compute FoV when that zoom and Estimated EIS is applied
        val recomputedFov =
            CameraLensMath.computeFovFromZoomRatio(
                focalLengthMm = DEFAULT_FOCAL_LENGTH,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                zoomRatio = zoomRatio,
                streamSize = streamSize,
                estimatedEisMargin = estimatedEisMargin,
                fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE,
            )

        // 3. Verify it matches target FoV
        assertThat(recomputedFov).isWithin(FLOAT_TOLERANCE).of(targetFovDegrees)
    }

    @Test
    fun computeFocalLengthFromZoomRatio_basic() {
        // Stream: 1920 x 1080 (16:9), Zoom: 2.0
        // Target FoV: 90 deg -> Target crop dimension: 4.0mm
        // Base long edge (1.0 zoom): 8.0mm
        // Zoom ratio: 2.0
        // Expected focal length: (8.0) / (2 * 2.0 * tan(90 / 2)) = 8.0 / (4.0 * 1) = 2.0mm
        val focalLength =
            CameraLensMath.computeFocalLengthFromZoomRatio(
                fovDegrees = 90f,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                zoomRatio = 2.0f,
                streamSize = Size(1920, 1080),
            )
        assertThat(focalLength).isWithin(FLOAT_TOLERANCE).of(DEFAULT_FOCAL_LENGTH)
    }

    @Test
    fun computeFocalLengthFromZoomRatio_withEstimatedEis() {
        // Stream: 1920 x 1080 (16:9), Zoom: 1.6, Estimated EIS: 20% (0.2)
        // Target FoV: 90 deg
        // Base long edge (1.0 zoom): 8.0mm
        // Expected focal length: (8.0 * 0.8) / (2 * 1.6 * tan(90 / 2)) = 6.4 / (3.2 * 1) = 2.0mm
        val focalLength =
            CameraLensMath.computeFocalLengthFromZoomRatio(
                fovDegrees = 90f,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                zoomRatio = 1.6f,
                streamSize = Size(1920, 1080),
                estimatedEisMargin = 0.2f,
                fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE,
            )
        assertThat(focalLength).isWithin(FLOAT_TOLERANCE).of(DEFAULT_FOCAL_LENGTH)
    }

    @Test
    fun computeFocalLengthFromZoomRatio_symmetry() {
        val targetFovDegrees = 90.0f
        val streamSize = Size(1920, 1080)
        val zoomRatio = 2.0f

        // 1. Compute required Focal Length to achieve target FoV (90 deg) at zoom 2.0
        val focalLength =
            CameraLensMath.computeFocalLengthFromZoomRatio(
                fovDegrees = targetFovDegrees,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                zoomRatio = zoomRatio,
                streamSize = streamSize,
            )

        // 2. Re-compute FoV when that focal length and zoom is applied
        val recomputedFov =
            CameraLensMath.computeFovFromZoomRatio(
                focalLengthMm = focalLength,
                sensorPhysicalSizeMm = DEFAULT_PHYSICAL_SIZE,
                sensorPixelSize = DEFAULT_PIXEL_SIZE,
                zoomRatio = zoomRatio,
                streamSize = streamSize,
            )

        // 3. Verify it matches target FoV
        assertThat(recomputedFov).isWithin(FLOAT_TOLERANCE).of(targetFovDegrees)
    }

    @Test
    fun computeFocalLengthFromZoomRatio_classOverload() {
        val sensorPhysicalSize = SizeF(DEFAULT_PHYSICAL_WIDTH, DEFAULT_PHYSICAL_HEIGHT)
        val sensorPixelSize = Size(DEFAULT_PIXEL_WIDTH, DEFAULT_PIXEL_HEIGHT)
        val streamSize = Size(1920, 1080)

        val focalLength =
            CameraLensMath.computeFocalLengthFromZoomRatio(
                fovDegrees = 90f,
                sensorPhysicalSizeMm = sensorPhysicalSize,
                sensorPixelSize = sensorPixelSize,
                zoomRatio = 2.0f,
                streamSize = streamSize,
            )
        assertThat(focalLength).isWithin(FLOAT_TOLERANCE).of(DEFAULT_FOCAL_LENGTH)
    }

    @Test
    @Suppress("NewApi")
    fun computeFov_realCropPath() {
        val characteristics =
            FakeCameraCharacteristics(
                cameraCharacteristics =
                    mapOf(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE to
                            SizeF(DEFAULT_PHYSICAL_WIDTH, DEFAULT_PHYSICAL_HEIGHT),
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to
                            Size(DEFAULT_PIXEL_WIDTH, DEFAULT_PIXEL_HEIGHT),
                    )
            )
        val result =
            FakeCaptureResult(
                cameraId = FakeCameraIds.default,
                frameNumber = CameraFrameNumber(1L),
                resultParameters =
                    mapOf(
                        CaptureResult.LENS_FOCAL_LENGTH to DEFAULT_FOCAL_LENGTH,
                        CaptureResult.SCALER_CROP_REGION to
                            Rect(1000, 750, 3000, 2250), // 2000x1500 crop
                        CaptureResult.CONTROL_ZOOM_RATIO to 1.25f,
                    ),
            )

        val fov =
            CameraLensMath.computeFov(
                cameraCharacteristics = characteristics,
                captureResult = result,
                streamSize = Size(1920, 1080),
            )

        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(77.31961f)
    }

    @Test
    @Suppress("NewApi")
    fun computeFov_estimatedEisPath_stabilizationOn() {
        val characteristics =
            FakeCameraCharacteristics(
                cameraCharacteristics =
                    mapOf(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE to
                            SizeF(DEFAULT_PHYSICAL_WIDTH, DEFAULT_PHYSICAL_HEIGHT),
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to
                            Size(DEFAULT_PIXEL_WIDTH, DEFAULT_PIXEL_HEIGHT),
                    )
            )
        val result =
            FakeCaptureResult(
                cameraId = FakeCameraIds.default,
                frameNumber = CameraFrameNumber(1L),
                resultParameters =
                    mapOf(
                        CaptureResult.LENS_FOCAL_LENGTH to DEFAULT_FOCAL_LENGTH,
                        CaptureResult.CONTROL_ZOOM_RATIO to 1.6f,
                        CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE to
                            CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_ON,
                    ),
            )

        val fov =
            CameraLensMath.computeFov(
                cameraCharacteristics = characteristics,
                captureResult = result,
                streamSize = Size(1920, 1080),
                defaultEisMargin = 0.2f,
            )

        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(90.0f)
    }

    @Test
    @Suppress("NewApi")
    fun computeFov_estimatedEisPath_stabilizationOn_defaultMargin() {
        val characteristics =
            FakeCameraCharacteristics(
                cameraCharacteristics =
                    mapOf(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE to
                            SizeF(DEFAULT_PHYSICAL_WIDTH, DEFAULT_PHYSICAL_HEIGHT),
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to
                            Size(DEFAULT_PIXEL_WIDTH, DEFAULT_PIXEL_HEIGHT),
                    )
            )
        val result =
            FakeCaptureResult(
                cameraId = FakeCameraIds.default,
                frameNumber = CameraFrameNumber(1L),
                resultParameters =
                    mapOf(
                        CaptureResult.LENS_FOCAL_LENGTH to DEFAULT_FOCAL_LENGTH,
                        CaptureResult.CONTROL_ZOOM_RATIO to 1.8f,
                        CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE to
                            CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_ON,
                    ),
            )

        // defaultEisMargin is omitted (defaults to -1.0f), should infer 10% (0.1f) because
        // stabilization is ON
        val fov =
            CameraLensMath.computeFov(
                cameraCharacteristics = characteristics,
                captureResult = result,
                streamSize = Size(1920, 1080),
            )

        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(90.0f)
    }

    @Test
    @Suppress("NewApi")
    fun computeFov_estimatedEisPath_stabilizationOn_explicitDefaultSentinel() {
        val characteristics =
            FakeCameraCharacteristics(
                cameraCharacteristics =
                    mapOf(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE to
                            SizeF(DEFAULT_PHYSICAL_WIDTH, DEFAULT_PHYSICAL_HEIGHT),
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to
                            Size(DEFAULT_PIXEL_WIDTH, DEFAULT_PIXEL_HEIGHT),
                    )
            )
        val result =
            FakeCaptureResult(
                cameraId = FakeCameraIds.default,
                frameNumber = CameraFrameNumber(1L),
                resultParameters =
                    mapOf(
                        CaptureResult.LENS_FOCAL_LENGTH to DEFAULT_FOCAL_LENGTH,
                        CaptureResult.CONTROL_ZOOM_RATIO to 1.8f,
                        CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE to
                            CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_ON,
                    ),
            )

        // defaultEisMargin is explicitly -1.0f, should infer 10% (0.1f) because stabilization is ON
        val fov =
            CameraLensMath.computeFov(
                cameraCharacteristics = characteristics,
                captureResult = result,
                streamSize = Size(1920, 1080),
                defaultEisMargin = -1.0f,
            )

        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(90.0f)
    }

    @Test
    @Suppress("NewApi")
    fun computeFov_estimatedEisPath_stabilizationOn_explicitZeroMargin() {
        val characteristics =
            FakeCameraCharacteristics(
                cameraCharacteristics =
                    mapOf(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE to
                            SizeF(DEFAULT_PHYSICAL_WIDTH, DEFAULT_PHYSICAL_HEIGHT),
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to
                            Size(DEFAULT_PIXEL_WIDTH, DEFAULT_PIXEL_HEIGHT),
                    )
            )
        val result =
            FakeCaptureResult(
                cameraId = FakeCameraIds.default,
                frameNumber = CameraFrameNumber(1L),
                resultParameters =
                    mapOf(
                        CaptureResult.LENS_FOCAL_LENGTH to DEFAULT_FOCAL_LENGTH,
                        CaptureResult.CONTROL_ZOOM_RATIO to 2.0f,
                        CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE to
                            CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_ON,
                    ),
            )

        // defaultEisMargin is explicitly 0.0f, should NOT infer 10% even though stabilization is ON
        val fov =
            CameraLensMath.computeFov(
                cameraCharacteristics = characteristics,
                captureResult = result,
                streamSize = Size(1920, 1080),
                defaultEisMargin = 0.0f,
            )

        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(90.0f)
    }

    @Test
    @Suppress("NewApi")
    fun computeFov_estimatedEisPath_stabilizationOff() {
        val characteristics =
            FakeCameraCharacteristics(
                cameraCharacteristics =
                    mapOf(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE to
                            SizeF(DEFAULT_PHYSICAL_WIDTH, DEFAULT_PHYSICAL_HEIGHT),
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to
                            Size(DEFAULT_PIXEL_WIDTH, DEFAULT_PIXEL_HEIGHT),
                    )
            )
        val result =
            FakeCaptureResult(
                cameraId = FakeCameraIds.default,
                frameNumber = CameraFrameNumber(1L),
                resultParameters =
                    mapOf(
                        CaptureResult.LENS_FOCAL_LENGTH to DEFAULT_FOCAL_LENGTH,
                        CaptureResult.CONTROL_ZOOM_RATIO to 2.0f,
                        CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE to
                            CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_OFF,
                    ),
            )

        val fov =
            CameraLensMath.computeFov(
                cameraCharacteristics = characteristics,
                captureResult = result,
                streamSize = Size(1920, 1080),
            )

        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(90.0f)
    }

    @Test
    fun computeFov_missingFocalLength_throwsIllegalStateException() {
        val characteristics =
            FakeCameraCharacteristics(
                cameraCharacteristics =
                    mapOf(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE to
                            SizeF(DEFAULT_PHYSICAL_WIDTH, DEFAULT_PHYSICAL_HEIGHT),
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to
                            Size(DEFAULT_PIXEL_WIDTH, DEFAULT_PIXEL_HEIGHT),
                    )
            )
        val result =
            FakeCaptureResult(
                cameraId = FakeCameraIds.default,
                frameNumber = CameraFrameNumber(1L),
                resultParameters = emptyMap(),
            )

        assertThrows(IllegalStateException::class.java) {
            CameraLensMath.computeFov(
                cameraCharacteristics = characteristics,
                captureResult = result,
                streamSize = Size(1920, 1080),
            )
        }
    }

    @Test
    fun computeFov_missingSensorPhysicalSize_throwsIllegalStateException() {
        val characteristics =
            FakeCameraCharacteristics(
                cameraCharacteristics =
                    mapOf(
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to
                            Size(DEFAULT_PIXEL_WIDTH, DEFAULT_PIXEL_HEIGHT)
                    )
            )
        val result =
            FakeCaptureResult(
                cameraId = FakeCameraIds.default,
                frameNumber = CameraFrameNumber(1L),
                resultParameters = mapOf(CaptureResult.LENS_FOCAL_LENGTH to DEFAULT_FOCAL_LENGTH),
            )

        assertThrows(IllegalStateException::class.java) {
            CameraLensMath.computeFov(
                cameraCharacteristics = characteristics,
                captureResult = result,
                streamSize = Size(1920, 1080),
            )
        }
    }

    @Test
    fun computeFov_missingSensorPixelSize_throwsIllegalStateException() {
        val characteristics =
            FakeCameraCharacteristics(
                cameraCharacteristics =
                    mapOf(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE to
                            SizeF(DEFAULT_PHYSICAL_WIDTH, DEFAULT_PHYSICAL_HEIGHT)
                    )
            )
        val result =
            FakeCaptureResult(
                cameraId = FakeCameraIds.default,
                frameNumber = CameraFrameNumber(1L),
                resultParameters = mapOf(CaptureResult.LENS_FOCAL_LENGTH to DEFAULT_FOCAL_LENGTH),
            )

        assertThrows(IllegalStateException::class.java) {
            CameraLensMath.computeFov(
                cameraCharacteristics = characteristics,
                captureResult = result,
                streamSize = Size(1920, 1080),
            )
        }
    }

    @Test
    fun computeFovFromCropRegion_cropOverload() {
        val characteristics =
            FakeCameraCharacteristics(
                cameraCharacteristics =
                    mapOf(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE to
                            SizeF(DEFAULT_PHYSICAL_WIDTH, DEFAULT_PHYSICAL_HEIGHT),
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to
                            Size(DEFAULT_PIXEL_WIDTH, DEFAULT_PIXEL_HEIGHT),
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS to
                            floatArrayOf(DEFAULT_FOCAL_LENGTH),
                    )
            )
        val fov =
            CameraLensMath.computeFovFromCropRegion(
                cameraCharacteristics = characteristics,
                cropRegion = Rect(1000, 750, 3000, 2250), // 2000x1500 crop
                streamSize = Size(1920, 1080),
                zoomRatio = 1.25f,
            )

        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(77.31961f)
    }

    @Test
    fun computeFovFromZoomRatio_eisOverload() {
        val characteristics =
            FakeCameraCharacteristics(
                cameraCharacteristics =
                    mapOf(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE to
                            SizeF(DEFAULT_PHYSICAL_WIDTH, DEFAULT_PHYSICAL_HEIGHT),
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to
                            Size(DEFAULT_PIXEL_WIDTH, DEFAULT_PIXEL_HEIGHT),
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS to
                            floatArrayOf(DEFAULT_FOCAL_LENGTH),
                    )
            )
        val fov =
            CameraLensMath.computeFovFromZoomRatio(
                cameraCharacteristics = characteristics,
                streamSize = Size(1920, 1080),
                zoomRatio = 1.6f,
                estimatedEisMargin = 0.2f,
            )

        assertThat(fov).isWithin(FLOAT_TOLERANCE).of(90.0f)
    }
}
