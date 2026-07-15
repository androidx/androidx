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
import android.util.Size
import android.util.SizeF
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import kotlin.math.atan
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

/**
 * Utility functions for converting between Field of View (FoV), Focal Length, and Zoom Ratio.
 *
 * ### Core Concepts
 * * **Field of View (FoV)**: The angular extent of a given scene that is imaged by a camera sensor,
 *   measured in degrees. It is determined by the active physical dimension of the sensor (or a
 *   cropped portion of it) and the focal length of the lens.
 * * **Focal Length**: The distance from the lens to the sensor when focused at infinity, measured
 *   in millimeters. Shorter focal lengths produce wider fields of view, and vice versa.
 * * **Zoom Ratio**: The magnification factor applied to the captured image. A zoom ratio of 1.0
 *   represents no zoom (the base Field of View), and values greater than 1.0 represent digital
 *   zoom.
 * * **Crop Region**: The subset of the sensor's pixel array that is read out to produce the image
 *   frame. Cropping reduces the active sensor size, effectively narrowing the Field of View.
 *
 * ### Multi-Step Conversion Workflows
 * This class provides basic conversions, but you can chain these functions to perform complex,
 * multi-step conversions depending on your needs.
 *
 * #### 1. Equivalent Focal Length (Full-Frame/35mm Equivalent)
 * To calculate the "35mm equivalent" focal length (a common metric for comparing field of view
 * across different sensor sizes):
 * 1. Compute the target FoV of your camera setup using [computeFov].
 * 2. Compute the equivalent focal length by passing the target FoV and the physical size of a
 *    standard 35mm film frame (36.0mm x 24.0mm) to [computeFocalLengthFromFov]:
 * ```kotlin
 * val activeFov = CameraLensMath.computeFov(
 *     focalLengthMm = 4.3f,
 *     sensorPhysicalSizeMm = SizeF(6.4f, 4.8f),
 *     sensorPixelSize = Size(4000, 3000),
 *     cropRegion = Rect(500, 375, 3500, 2625),
 *     streamSize = Size(1920, 1080),
 *     fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE
 * )
 *
 * // Standard 35mm film frame size: 36.0mm x 24.0mm
 * val equivalentFocalLength = CameraLensMath.computeFocalLengthFromFov(
 *     fovDegrees = activeFov,
 *     physicalLengthMm = 36.0f // Using horizontal FoV (long edge)
 * )
 * ```
 *
 * #### 2. Equivalent Zoom between Different Camera Sensors
 * To find the equivalent zoom factor on a secondary camera (e.g. Ultra-wide) to match the field of
 * view of the primary camera at a specific zoom:
 * 1. Compute the active FoV of the primary camera at its current zoom using [computeFov].
 * 2. Compute the required zoom ratio on the secondary camera to match that target FoV using
 *    [computeZoomRatioFromFov]:
 * ```kotlin
 * val primaryFov = CameraLensMath.computeFov(
 *     focalLengthMm = 5.43f,
 *     sensorPhysicalSizeMm = SizeF(7.6f, 5.7f),
 *     sensorPixelSize = Size(4000, 3000),
 *     cropRegion = Rect(0, 0, 4000, 3000),
 *     streamSize = Size(1920, 1080),
 *     zoomRatio = 2.0f, // Primary is zoomed to 2x
 *     fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE
 * )
 *
 * val equivalentZoom = CameraLensMath.computeZoomRatioFromFov(
 *     focalLengthMm = 1.85f, // Secondary (Ultra-wide) focal length
 *     sensorPhysicalSizeMm = SizeF(6.2f, 4.65f), // Secondary sensor size
 *     sensorPixelSize = Size(3264, 2448),
 *     streamSize = Size(1920, 1080),
 *     fovDegrees = primaryFov,
 *     fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE
 * )
 * ```
 *
 * The resulting `equivalentZoom` tells you the digital zoom factor to apply to the camera (e.g.,
 * via [android.hardware.camera2.CaptureRequest.CONTROL_ZOOM_RATIO]) to capture the same horizontal
 * Field of View.
 *
 * For more information, see:
 * - [Angle of View on Wikipedia](https://en.wikipedia.org/wiki/Angle_of_view)
 * - [Android Camera3 Cropping
 *   Specification](https://source.android.com/docs/core/camera/camera3_crop_reprocess)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object CameraLensMath {

    /** Compute FoV based on the short edge of the crop region. */
    public const val FOV_MODE_CROP_SHORT_EDGE: Int = 1

    /** Compute FoV based on the long edge of the crop region. */
    public const val FOV_MODE_CROP_LONG_EDGE: Int = 2

    /** Compute FoV based on the diagonal of the crop region. */
    public const val FOV_MODE_CROP_DIAGONAL: Int = 3

    /** Compute FoV based on the long edge of the sensor, scaled to the crop region. */
    public const val FOV_MODE_SENSOR_LONG_EDGE: Int = 4

    /** Compute FoV based on the short edge of the sensor, scaled to the crop region. */
    public const val FOV_MODE_SENSOR_SHORT_EDGE: Int = 5

    /** Compute FoV based on the diagonal of the sensor, scaled to the crop region. */
    public const val FOV_MODE_SENSOR_DIAGONAL: Int = 6

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        FOV_MODE_CROP_SHORT_EDGE,
        FOV_MODE_CROP_LONG_EDGE,
        FOV_MODE_CROP_DIAGONAL,
        FOV_MODE_SENSOR_LONG_EDGE,
        FOV_MODE_SENSOR_SHORT_EDGE,
        FOV_MODE_SENSOR_DIAGONAL,
    )
    public annotation class CameraFovMode

    internal const val RADIANS_TO_DEGREES_X2: Float = 114.591559026f // (180 / PI) * 2

    /**
     * Computes the Field of View (FoV) in degrees for a given focal length and physical length.
     *
     * ### Example
     *
     * ```kotlin
     * val fov = CameraLensMath.computeFovFromFocalLength(
     *     focalLengthMm = 4.3f,
     *     physicalLengthMm = 6.4f
     * )
     * ```
     *
     * @param focalLengthMm The focal length of the lens in millimeters. Can be retrieved from
     *   [android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS] or
     *   [android.hardware.camera2.CaptureResult.LENS_FOCAL_LENGTH]. Must be positive (> 0.0).
     * @param physicalLengthMm The physical length in millimeters of the active sensor area used for
     *   the FoV calculation (representing a physical line segment on the sensor surface). While
     *   this is derived from the sensor's physical size
     *   [android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE], the value must
     *   represent the projected physical dimension of the cropped, zoomed, or aspect-ratio fitted
     *   region rather than the raw physical sensor size. Must be positive (> 0.0).
     * @return The Field of View in degrees.
     * @throws IllegalArgumentException if [focalLengthMm] or [physicalLengthMm] is non-positive.
     */
    @JvmStatic
    public fun computeFovFromFocalLength(
        @FloatRange(from = 0.0, fromInclusive = false) focalLengthMm: Float,
        @FloatRange(from = 0.0, fromInclusive = false) physicalLengthMm: Float,
    ): Float {
        require(focalLengthMm > 0f) { "Focal length must be positive: $focalLengthMm" }
        require(physicalLengthMm > 0f) { "Physical length must be positive: $physicalLengthMm" }
        return atan(physicalLengthMm / (2f * focalLengthMm)) * RADIANS_TO_DEGREES_X2
    }

    /**
     * Computes the focal length required to achieve a target Field of View (FoV) for a given
     * physical length.
     *
     * ### Example
     *
     * ```kotlin
     * val focalLength = CameraLensMath.computeFocalLengthFromFov(
     *     fovDegrees = 75.0f,
     *     physicalLengthMm = 6.4f
     * )
     * ```
     *
     * @param fovDegrees The target Field of View in degrees. Must be in the range (0.0, 180.0)
     *   exclusive.
     * @param physicalLengthMm The physical length in millimeters of the active sensor area
     *   (representing a physical line segment on the sensor surface). For cropped, zoomed, or
     *   aspect-ratio fitted regions, this represents the projected physical dimension on the sensor
     *   rather than the raw physical sensor size
     *   [android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]. Must be
     *   positive (> 0.0).
     * @return The required focal length in millimeters.
     * @throws IllegalArgumentException if [fovDegrees] is not in (0, 180) or [physicalLengthMm] is
     *   non-positive.
     */
    @JvmStatic
    public fun computeFocalLengthFromFov(
        @FloatRange(from = 0.0, to = 180.0, fromInclusive = false, toInclusive = false)
        fovDegrees: Float,
        @FloatRange(from = 0.0, fromInclusive = false) physicalLengthMm: Float,
    ): Float {
        require(fovDegrees > 0f && fovDegrees < 180f) {
            "FoV must be in range (0, 180): $fovDegrees"
        }
        require(physicalLengthMm > 0f) { "Physical length must be positive: $physicalLengthMm" }
        return physicalLengthMm / (2f * tan(fovDegrees / RADIANS_TO_DEGREES_X2))
    }

    /**
     * Computes the Field of View (FoV) in degrees for a sensor, based on the physical size and the
     * specified [CameraFovMode], using Android graphics classes.
     *
     * This is a convenience overload for cases where there is no cropping or zoom applied.
     *
     * ### Example
     *
     * ```kotlin
     * val sensorSize = SizeF(6.4f, 4.8f)
     * val fov = CameraLensMath.computeFov(
     *     focalLengthMm = 4.3f,
     *     sensorPhysicalSizeMm = sensorSize,
     *     fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE
     * )
     * ```
     *
     * @param focalLengthMm The focal length of the lens in millimeters. Can be retrieved from
     *   [android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS] or
     *   [android.hardware.camera2.CaptureResult.LENS_FOCAL_LENGTH]. Must be positive (> 0.0).
     * @param sensorPhysicalSizeMm The physical size of the sensor in millimeters. Can be retrieved
     *   from [android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]. Width and
     *   height must be positive (> 0.0).
     * @param fovMode The mode defining how to compute the FoV from the sensor dimensions. Must be
     *   one of [CameraFovMode]. Defaults to [FOV_MODE_CROP_LONG_EDGE].
     * @return The Field of View in degrees.
     * @throws IllegalArgumentException if any of the input dimensions are non-positive.
     */
    @JvmStatic
    @JvmOverloads
    public fun computeFov(
        @FloatRange(from = 0.0, fromInclusive = false) focalLengthMm: Float,
        sensorPhysicalSizeMm: SizeF,
        @CameraFovMode fovMode: Int = FOV_MODE_CROP_LONG_EDGE,
    ): Float {
        require(focalLengthMm > 0f) { "Focal length must be positive: $focalLengthMm" }
        require(sensorPhysicalSizeMm.width > 0f) {
            "Sensor physical width must be positive: ${sensorPhysicalSizeMm.width}"
        }
        require(sensorPhysicalSizeMm.height > 0f) {
            "Sensor physical height must be positive: ${sensorPhysicalSizeMm.height}"
        }
        val physicalLengthMm =
            when (fovMode) {
                FOV_MODE_CROP_LONG_EDGE,
                FOV_MODE_SENSOR_LONG_EDGE ->
                    max(sensorPhysicalSizeMm.width, sensorPhysicalSizeMm.height)
                FOV_MODE_CROP_SHORT_EDGE,
                FOV_MODE_SENSOR_SHORT_EDGE ->
                    min(sensorPhysicalSizeMm.width, sensorPhysicalSizeMm.height)
                FOV_MODE_CROP_DIAGONAL,
                FOV_MODE_SENSOR_DIAGONAL ->
                    hypot(sensorPhysicalSizeMm.width, sensorPhysicalSizeMm.height)
                else -> throw IllegalArgumentException("Invalid FoV mode: $fovMode")
            }
        return computeFovFromFocalLength(focalLengthMm, physicalLengthMm)
    }

    /**
     * Computes the Field of View (FoV) in degrees for a cropped region of the sensor, using Android
     * graphics and utility classes.
     *
     * ### Example
     *
     * ```kotlin
     * val fov = CameraLensMath.computeFov(
     *     focalLengthMm = 4.3f,
     *     sensorPhysicalSizeMm = SizeF(6.4f, 4.8f),
     *     sensorPixelSize = Size(4000, 3000),
     *     cropRegion = Rect(500, 375, 3500, 2625),
     *     streamSize = Size(1920, 1080),
     *     zoomRatio = 1.0f,
     *     fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE
     * )
     * ```
     *
     * @param focalLengthMm The focal length of the lens in millimeters. Can be retrieved from
     *   [android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS] or
     *   [android.hardware.camera2.CaptureResult.LENS_FOCAL_LENGTH]. Must be positive (> 0.0).
     * @param sensorPhysicalSizeMm The physical size of the sensor in millimeters. Can be retrieved
     *   from [android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]. Width and
     *   height must be positive (> 0.0).
     * @param sensorPixelSize The full pixel array size of the sensor in pixels. Can be retrieved
     *   from [android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE]. Width
     *   and height must be positive (>= 1).
     * @param cropRegion The crop region in pixels. Can be retrieved from
     *   [android.hardware.camera2.CaptureResult.SCALER_CROP_REGION]. Width and height must be
     *   positive (>= 1).
     * @param zoomRatio The digital zoom ratio (e.g., 1.0 for no zoom, 2.0 for 2x zoom). Must be
     *   positive (> 0.0). Defaults to 1.0f. Links to
     *   [android.hardware.camera2.CaptureRequest.CONTROL_ZOOM_RATIO].
     * @param streamSize The size of the output stream in pixels. Used to determine the aspect
     *   ratio. Width and height must be positive (>= 1). Defaults to the crop region size.
     * @param fovMode The mode defining how to compute the FoV from the crop and sensor dimensions.
     *   Must be one of [CameraFovMode]. Defaults to [FOV_MODE_CROP_LONG_EDGE].
     * @return The Field of View in degrees.
     * @throws IllegalArgumentException if any of the input dimensions are non-positive, or if
     *   [zoomRatio] is non-positive.
     */
    @JvmStatic
    @JvmOverloads
    public fun computeFov(
        @FloatRange(from = 0.0, fromInclusive = false) focalLengthMm: Float,
        sensorPhysicalSizeMm: SizeF,
        sensorPixelSize: Size,
        cropRegion: Rect,
        streamSize: Size = Size(cropRegion.width(), cropRegion.height()),
        @FloatRange(from = 0.0, fromInclusive = false) zoomRatio: Float = 1.0f,
        @CameraFovMode fovMode: Int = FOV_MODE_CROP_LONG_EDGE,
    ): Float {
        require(focalLengthMm > 0f) { "Focal length must be positive: $focalLengthMm" }
        val lengthMm =
            computePhysicalLengthMm(
                sensorPhysicalSizeMm = sensorPhysicalSizeMm,
                sensorPixelSize = sensorPixelSize,
                cropSize = Size(cropRegion.width(), cropRegion.height()),
                streamSize = streamSize,
                zoomRatio = zoomRatio,
                fovMode = fovMode,
            )
        return computeFovFromFocalLength(focalLengthMm, lengthMm)
    }

    /**
     * Computes the Field of View (FoV) in degrees from a given zoom ratio, using Android graphics
     * and utility classes.
     *
     * This function calculates the effective FoV when a specific [zoomRatio] is applied. It assumes
     * the crop region is centered and matches the aspect ratio of the [streamSize].
     *
     * ### Example
     *
     * ```kotlin
     * val fov = CameraLensMath.computeFovFromZoomRatio(
     *     focalLengthMm = 4.3f,
     *     sensorPhysicalSizeMm = SizeF(6.4f, 4.8f),
     *     sensorPixelSize = Size(4000, 3000),
     *     streamSize = Size(1920, 1080),
     *     zoomRatio = 2.0f,
     *     estimatedEisMargin = 0.1f,
     *     fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE
     * )
     * ```
     *
     * @param focalLengthMm The focal length of the lens in millimeters. Can be retrieved from
     *   [android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS] or
     *   [android.hardware.camera2.CaptureResult.LENS_FOCAL_LENGTH]. Must be positive (> 0.0).
     * @param sensorPhysicalSizeMm The physical size of the sensor in millimeters. Can be retrieved
     *   from [android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]. Width and
     *   height must be positive (> 0.0).
     * @param sensorPixelSize The full pixel array size of the sensor in pixels. Can be retrieved
     *   from [android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE]. Width
     *   and height must be positive (>= 1).
     * @param zoomRatio The digital zoom ratio (e.g., 1.0 for no zoom, 2.0 for 2x zoom). Links to
     *   [android.hardware.camera2.CaptureResult.CONTROL_ZOOM_RATIO]. Must be positive (> 0.0).
     * @param streamSize The size of the output stream in pixels. Used to determine the aspect
     *   ratio. Width and height must be positive (>= 1).
     * @param estimatedEisMargin The estimated EIS margin as a fraction of the sensor area (e.g.,
     *   0.1 for 10% crop). Must be in the range [0.0, 1.0) exclusive. Defaults to 0f.
     * @param fovMode The mode defining how to compute the FoV. Must be one of [CameraFovMode].
     *   Defaults to [FOV_MODE_CROP_LONG_EDGE].
     * @return The Field of View in degrees.
     * @throws IllegalArgumentException if any of the input dimensions or [zoomRatio] are
     *   non-positive, or if [estimatedEisMargin] is not in the range [0.0, 1.0).
     */
    @JvmStatic
    @JvmOverloads
    public fun computeFovFromZoomRatio(
        @FloatRange(from = 0.0, fromInclusive = false) focalLengthMm: Float,
        sensorPhysicalSizeMm: SizeF,
        sensorPixelSize: Size,
        streamSize: Size,
        @FloatRange(from = 0.0, fromInclusive = false) zoomRatio: Float,
        @FloatRange(from = 0.0, to = 1.0, toInclusive = false) estimatedEisMargin: Float = 0f,
        @CameraFovMode fovMode: Int = FOV_MODE_CROP_LONG_EDGE,
    ): Float {
        require(focalLengthMm > 0f) { "Focal length must be positive: $focalLengthMm" }
        require(zoomRatio > 0f) { "Zoom ratio must be positive: $zoomRatio" }
        require(estimatedEisMargin >= 0f && estimatedEisMargin < 1f) {
            "Estimated EIS margin must be in range [0, 1): $estimatedEisMargin"
        }
        val cropWidth = (sensorPixelSize.width * (1f - estimatedEisMargin)).toInt()
        val cropHeight = (sensorPixelSize.height * (1f - estimatedEisMargin)).toInt()
        val cropSize = Size(cropWidth, cropHeight)

        val lengthMm =
            computePhysicalLengthMm(
                sensorPhysicalSizeMm = sensorPhysicalSizeMm,
                sensorPixelSize = sensorPixelSize,
                cropSize = cropSize,
                streamSize = streamSize,
                zoomRatio = zoomRatio,
                fovMode = fovMode,
            )
        return computeFovFromFocalLength(focalLengthMm, lengthMm)
    }

    /**
     * Computes the Zoom Ratio required to achieve a target Field of View (FoV), using Android
     * graphics and utility classes.
     *
     * ### Example
     *
     * ```kotlin
     * val zoomRatio = CameraLensMath.computeZoomRatioFromFov(
     *     focalLengthMm = 4.3f,
     *     sensorPhysicalSizeMm = SizeF(6.4f, 4.8f),
     *     sensorPixelSize = Size(4000, 3000),
     *     streamSize = Size(1920, 1080),
     *     fovDegrees = 60.0f,
     *     estimatedEisMargin = 0.1f,
     *     fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE
     * )
     * ```
     *
     * @param focalLengthMm The focal length of the lens in millimeters. Can be retrieved from
     *   [android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS] or
     *   [android.hardware.camera2.CaptureResult.LENS_FOCAL_LENGTH]. Must be positive (> 0.0).
     * @param sensorPhysicalSizeMm The physical size of the sensor in millimeters. Can be retrieved
     *   from [android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]. Width and
     *   height must be positive (> 0.0).
     * @param sensorPixelSize The full pixel array size of the sensor in pixels. Can be retrieved
     *   from [android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE]. Width
     *   and height must be positive (>= 1).
     * @param streamSize The size of the output stream in pixels. Used to determine the aspect
     *   ratio. Width and height must be positive (>= 1).
     * @param fovDegrees The target Field of View in degrees. Must be in the range (0.0, 180.0)
     *   exclusive.
     * @param estimatedEisMargin The estimated EIS margin as a fraction of the sensor area (e.g.,
     *   0.1 for 10% crop). Must be in the range [0.0, 1.0) exclusive. Defaults to 0f.
     * @param fovMode The mode defining how the target FoV is measured. Must be one of
     *   [CameraFovMode]. Defaults to [FOV_MODE_CROP_LONG_EDGE].
     * @return The required zoom ratio.
     * @throws IllegalArgumentException if [fovDegrees] is not in (0, 180), [focalLengthMm] is
     *   non-positive, any of the sensor or stream dimensions are non-positive, or if
     *   [estimatedEisMargin] is not in the range [0.0, 1.0).
     */
    @JvmStatic
    @JvmOverloads
    public fun computeZoomRatioFromFov(
        @FloatRange(from = 0.0, fromInclusive = false) focalLengthMm: Float,
        sensorPhysicalSizeMm: SizeF,
        sensorPixelSize: Size,
        streamSize: Size,
        @FloatRange(from = 0.0, to = 180.0, fromInclusive = false, toInclusive = false)
        fovDegrees: Float,
        @FloatRange(from = 0.0, to = 1.0, toInclusive = false) estimatedEisMargin: Float = 0f,
        @CameraFovMode fovMode: Int = FOV_MODE_CROP_LONG_EDGE,
    ): Float {
        require(fovDegrees > 0f && fovDegrees < 180f) {
            "Target FoV must be in range (0, 180): $fovDegrees"
        }
        require(focalLengthMm > 0f) { "Focal length must be positive: $focalLengthMm" }
        require(estimatedEisMargin >= 0f && estimatedEisMargin < 1f) {
            "Estimated EIS margin must be in range [0, 1): $estimatedEisMargin"
        }

        val baseLengthMm =
            computePhysicalLengthMm(
                sensorPhysicalSizeMm = sensorPhysicalSizeMm,
                sensorPixelSize = sensorPixelSize,
                cropSize = sensorPixelSize,
                streamSize = streamSize,
                zoomRatio = 1.0f,
                fovMode = fovMode,
            )

        // Compute target physical length from target FoV
        val targetLengthMm = 2f * focalLengthMm * tan(fovDegrees / RADIANS_TO_DEGREES_X2)

        return (baseLengthMm * (1f - estimatedEisMargin)) / targetLengthMm
    }

    /**
     * Computes the Focal Length required to achieve a target Field of View (FoV) at a given zoom
     * ratio, using Android graphics and utility classes.
     *
     * ### Example
     *
     * ```kotlin
     * val focalLength = CameraLensMath.computeFocalLengthFromZoomRatio(
     *     sensorPhysicalSizeMm = SizeF(6.4f, 4.8f),
     *     sensorPixelSize = Size(4000, 3000),
     *     streamSize = Size(1920, 1080),
     *     fovDegrees = 60.0f,
     *     zoomRatio = 2.0f,
     *     estimatedEisMargin = 0.1f,
     *     fovMode = CameraLensMath.FOV_MODE_CROP_LONG_EDGE
     * )
     * ```
     *
     * @param sensorPhysicalSizeMm The physical size of the sensor in millimeters. Can be retrieved
     *   from [android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]. Width and
     *   height must be positive (> 0.0).
     * @param sensorPixelSize The full pixel array size of the sensor in pixels. Can be retrieved
     *   from [android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE]. Width
     *   and height must be positive (>= 1).
     * @param streamSize The size of the output stream in pixels. Used to determine the aspect
     *   ratio. Width and height must be positive (>= 1).
     * @param fovDegrees The target Field of View in degrees. Must be in the range (0.0, 180.0)
     *   exclusive.
     * @param zoomRatio The digital zoom ratio (e.g., 1.0 for no zoom, 2.0 for 2x zoom). Links to
     *   [android.hardware.camera2.CaptureResult.CONTROL_ZOOM_RATIO]. Must be positive (> 0.0).
     * @param estimatedEisMargin The estimated EIS margin as a fraction of the sensor area (e.g.,
     *   0.1 for 10% crop). Must be in the range [0.0, 1.0) exclusive. Defaults to 0f.
     * @param fovMode The mode defining how the target FoV is measured. Must be one of
     *   [CameraFovMode]. Defaults to [FOV_MODE_CROP_LONG_EDGE].
     * @return The required focal length in millimeters.
     * @throws IllegalArgumentException if [fovDegrees] is not in (0, 180), [zoomRatio] is
     *   non-positive, any of the sensor or stream dimensions are non-positive, or if
     *   [estimatedEisMargin] is not in the range [0.0, 1.0).
     */
    @JvmStatic
    @JvmOverloads
    public fun computeFocalLengthFromZoomRatio(
        sensorPhysicalSizeMm: SizeF,
        sensorPixelSize: Size,
        streamSize: Size,
        @FloatRange(from = 0.0, to = 180.0, fromInclusive = false, toInclusive = false)
        fovDegrees: Float,
        @FloatRange(from = 0.0, fromInclusive = false) zoomRatio: Float,
        @FloatRange(from = 0.0, to = 1.0, toInclusive = false) estimatedEisMargin: Float = 0f,
        @CameraFovMode fovMode: Int = FOV_MODE_CROP_LONG_EDGE,
    ): Float {
        require(fovDegrees > 0f && fovDegrees < 180f) {
            "Target FoV must be in range (0, 180): $fovDegrees"
        }
        require(zoomRatio > 0f) { "Zoom ratio must be positive: $zoomRatio" }
        require(estimatedEisMargin >= 0f && estimatedEisMargin < 1f) {
            "Estimated EIS margin must be in range [0, 1): $estimatedEisMargin"
        }

        val baseLengthMm =
            computePhysicalLengthMm(
                sensorPhysicalSizeMm = sensorPhysicalSizeMm,
                sensorPixelSize = sensorPixelSize,
                cropSize = sensorPixelSize,
                streamSize = streamSize,
                zoomRatio = 1.0f,
                fovMode = fovMode,
            )

        return (baseLengthMm * (1f - estimatedEisMargin)) /
            (2f * zoomRatio * tan(fovDegrees / RADIANS_TO_DEGREES_X2))
    }

    /**
     * Resolves the effective physical crop dimension in millimeters based on sensor/crop mode,
     * stream aspect ratio, and zoom.
     */
    internal fun computePhysicalLengthMm(
        sensorPhysicalSizeMm: SizeF,
        sensorPixelSize: Size,
        cropSize: Size,
        streamSize: Size,
        @FloatRange(from = 0.0, fromInclusive = false) zoomRatio: Float,
        @CameraFovMode fovMode: Int,
    ): Float {
        require(sensorPhysicalSizeMm.width > 0f) {
            "Sensor physical width must be positive: ${sensorPhysicalSizeMm.width}"
        }
        require(sensorPhysicalSizeMm.height > 0f) {
            "Sensor physical height must be positive: ${sensorPhysicalSizeMm.height}"
        }
        require(sensorPixelSize.width > 0) {
            "Sensor pixel array width must be positive: ${sensorPixelSize.width}"
        }
        require(sensorPixelSize.height > 0) {
            "Sensor pixel array height must be positive: ${sensorPixelSize.height}"
        }
        require(cropSize.width > 0) { "Crop size width must be positive: ${cropSize.width}" }
        require(cropSize.height > 0) { "Crop size height must be positive: ${cropSize.height}" }
        require(streamSize.width > 0) { "Stream width must be positive: ${streamSize.width}" }
        require(streamSize.height > 0) { "Stream height must be positive: ${streamSize.height}" }
        require(zoomRatio > 0f) { "Zoom ratio must be positive: $zoomRatio" }

        // Fit stream size inside crop size
        val scaleToFit =
            CameraMath.computeScaleToFit(
                width = streamSize.width,
                height = streamSize.height,
                widthToFit = cropSize.width,
                heightToFit = cropSize.height,
            )
        val baseCropWidthPixels = streamSize.width * scaleToFit
        val baseCropHeightPixels = streamSize.height * scaleToFit

        // Convert base crop to physical mm (multiplying first minimizes precision loss)
        val basePhysicalWidthMm =
            (baseCropWidthPixels * sensorPhysicalSizeMm.width) / sensorPixelSize.width
        val basePhysicalHeightMm =
            (baseCropHeightPixels * sensorPhysicalSizeMm.height) / sensorPixelSize.height

        // Apply zoom ratio to get effective physical size
        val scale = 1f / zoomRatio
        val effectivePhysicalWidthMm = basePhysicalWidthMm * scale
        val effectivePhysicalHeightMm = basePhysicalHeightMm * scale

        val isSensorMode =
            fovMode == FOV_MODE_SENSOR_LONG_EDGE ||
                fovMode == FOV_MODE_SENSOR_SHORT_EDGE ||
                fovMode == FOV_MODE_SENSOR_DIAGONAL

        return if (isSensorMode) {
            val sensorAspectRatio = sensorPixelSize.width.toFloat() / sensorPixelSize.height
            val effectiveSensorWidthMm =
                max(effectivePhysicalWidthMm, effectivePhysicalHeightMm * sensorAspectRatio)
            val effectiveSensorHeightMm =
                max(effectivePhysicalHeightMm, effectivePhysicalWidthMm / sensorAspectRatio)

            when (fovMode) {
                FOV_MODE_SENSOR_LONG_EDGE -> max(effectiveSensorWidthMm, effectiveSensorHeightMm)
                FOV_MODE_SENSOR_SHORT_EDGE -> min(effectiveSensorWidthMm, effectiveSensorHeightMm)
                FOV_MODE_SENSOR_DIAGONAL -> hypot(effectiveSensorWidthMm, effectiveSensorHeightMm)
                else -> throw IllegalArgumentException("Invalid FoV mode: $fovMode")
            }
        } else {
            when (fovMode) {
                FOV_MODE_CROP_SHORT_EDGE -> min(effectivePhysicalWidthMm, effectivePhysicalHeightMm)
                FOV_MODE_CROP_LONG_EDGE -> max(effectivePhysicalWidthMm, effectivePhysicalHeightMm)
                FOV_MODE_CROP_DIAGONAL -> hypot(effectivePhysicalWidthMm, effectivePhysicalHeightMm)
                else -> throw IllegalArgumentException("Invalid FoV mode: $fovMode")
            }
        }
    }
}
