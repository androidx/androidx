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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.camera.common

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.Size
import android.util.SizeF
import kotlin.math.roundToInt

/**
 * Utility functions for doing common transformations of camera and sensor coordinate spaces.
 *
 * When doing camera coordinate transformations, there are three main "coordinate spaces" that a
 * developer may need to understand and work with:
 * - **Sensor-relative** coordinates: These are integer-pixel coordinates that correspond to the
 *   physical active pixel array of a camera device. Most operations in Camera2 (like 3A metering
 *   regions or crop regions) define coordinates relative to the physical sensor (with (0,0) being
 *   the top-left active pixel).
 * - **Stream-relative** coordinates: When configuring camera output streams, they are configured
 *   with a specific resolution. The stream coordinate space is axially aligned and centered within
 *   the crop region of the sensor, but may be further cropped either horizontally or vertically to
 *   maintain square pixels and its own aspect ratio (see Android's camera cropping specification at
 *   [Output streams, cropping, and zoom](https://source.android.com/docs/core/camera/camera3_crop_reprocess)).
 *   Stream coordinates are relative to the `streamWidth` and `streamHeight` boundaries.
 * - **UI-relative** coordinates: These represent the display area of a stream on a UI surface (like
 *   a viewfinder). UI coordinates are scaled, potentially mirrored, and rotated relative to the
 *   coordinate system of the screen or window the viewfinder is currently displayed on. *Note*: The
 *   `uiWidth` and `uiHeight` used in calculations represent the UI-relative size of the viewfinder,
 *   without accounting for any viewport clipping or padding. Callers must normalize UI coordinates
 *   to account for clipping or padding before calling these functions.
 */
@Suppress("ValueClassUsageWithoutJvmName")
public object CameraMath {
    /**
     * When converting to sensor coordinates, this is the minimum width/height for rectangles that
     * will be sent to the sensor. This helps prevent scenarios where rectangles need to be clamped
     * to the sensor, but where setting a small, or zero, size can lead to unexpected or bad
     * behavior.
     */
    public const val DEFAULT_MIN_SENSOR_PIXELS: Int = 32

    /**
     * It's common to need to convert a point into a sensor-relative rectangle for things like
     * touch-to-focus, which often starts from a touch point that then needs to be converted into a
     * sensor-relative rectangle.
     *
     * This ratio is used to compute a rectangle based on the short dimension of the current stream.
     */
    public const val DEFAULT_POINT_TO_RECT_RATIO: Float = 0.18375f

    // =============================================================================================
    // Rotations
    // =============================================================================================

    /**
     * Computes the rotation required to rotate coordinates from sensor space to the display
     * coordinate system.
     *
     * @param displayRotation The rotation of the display in degrees. Must be one of 0, 90, 180,
     *     270. (Note: do not pass Surface.ROTATION_X constants directly as they represent index
     *          values rather than degrees).
     *
     * @param sensorOrientation The physical orientation of the sensor in degrees (typically
     *   SENSOR_ORIENTATION from CameraCharacteristics). Must be one of 0, 90, 180, 270.
     * @param sensorIsFacingScreen Whether the camera sensor is facing the screen (front-facing).
     */
    @JvmStatic
    @JvmName("computeSensorRotationToDisplayOrientation")
    public fun computeSensorRotationToDisplayOrientation(
        displayRotation: Int,
        sensorOrientation: Int,
        sensorIsFacingScreen: Boolean,
    ): DiscreteRotation =
        if (sensorIsFacingScreen) {
            DiscreteRotation.from(sensorOrientation) + DiscreteRotation.from(displayRotation)
        } else {
            DiscreteRotation.from(sensorOrientation) - DiscreteRotation.from(displayRotation)
        }

    /**
     * Computes the rotation required to rotate coordinates from sensor space to the display
     * coordinate system.
     *
     * @param displayRotation The rotation of the display.
     * @param sensorOrientation The physical orientation of the sensor.
     * @param sensorIsFacingScreen Whether the camera sensor is facing the screen (front-facing).
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun computeSensorRotationToDisplayOrientation(
        displayRotation: DiscreteRotation,
        sensorOrientation: DiscreteRotation,
        sensorIsFacingScreen: Boolean,
    ): DiscreteRotation =
        if (sensorIsFacingScreen) {
            sensorOrientation + displayRotation
        } else {
            sensorOrientation - displayRotation
        }

    /**
     * Computes the clockwise rotation required to be applied to the captured image to be viewed
     * upright.
     *
     * @param deviceOrientation The orientation of the device in degrees.
     * @param sensorOrientation The physical orientation of the sensor (typically SENSOR_ORIENTATION
     *   from CameraCharacteristics).
     * @param sensorIsFacingScreen Whether the camera sensor is facing the screen (front-facing).
     */
    @JvmStatic
    @JvmName("computeSensorRotationToJpegOrientation")
    public fun computeSensorRotationToJpegOrientation(
        deviceOrientation: Int,
        sensorOrientation: Int,
        sensorIsFacingScreen: Boolean,
    ): DiscreteRotation =
        if (sensorIsFacingScreen) {
            DiscreteRotation.from(sensorOrientation) - DiscreteRotation.from(deviceOrientation)
        } else {
            DiscreteRotation.from(sensorOrientation) + DiscreteRotation.from(deviceOrientation)
        }

    /**
     * Computes the clockwise rotation required to be applied to the captured image to be viewed
     * upright.
     *
     * @param deviceOrientation The orientation of the device.
     * @param sensorOrientation The physical orientation of the sensor.
     * @param sensorIsFacingScreen Whether the camera sensor is facing the screen (front-facing).
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun computeSensorRotationToJpegOrientation(
        deviceOrientation: DiscreteRotation,
        sensorOrientation: DiscreteRotation,
        sensorIsFacingScreen: Boolean,
    ): DiscreteRotation =
        if (sensorIsFacingScreen) {
            sensorOrientation - deviceOrientation
        } else {
            sensorOrientation + deviceOrientation
        }

    // =============================================================================================
    // Coordinate Mapping Methods
    // =============================================================================================

    // --- Mapping from UI to STREAM ---

    /**
     * Converts a point relative to a UI element to an equivalent point relative to a specific
     * camera stream.
     *
     * @param uiPoint The point in the UI. UI coordinates are relative to the uiSize, and does not
     *   account for the positioning of the UI. Callers must normalize coordinates to account for
     *   clipping or padding before calling this function.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     */
    @JvmStatic
    public fun mapUiPointToStreamPointF(
        uiPoint: Point,
        uiSize: Size,
        streamSize: Size,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: Int,
    ): PointF =
        mapUiPointToStreamPoint(
            uiX = uiPoint.x.toFloat(),
            uiY = uiPoint.y.toFloat(),
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
        ) { streamX, streamY ->
            PointF(streamX, streamY)
        }

    /**
     * Converts a point relative to a UI element to an equivalent point relative to a specific
     * camera stream.
     *
     * @param uiX The x coordinate in the UI. UI coordinates are relative to the uiWidth, and does
     *   not account for the positioning of the UI. Callers must normalize coordinates to account
     *   for clipping or padding before calling this function.
     * @param uiY The y coordinate in the UI. UI coordinates are relative to the uiHeight, and does
     *   not account for the positioning of the UI. Callers must normalize coordinates to account
     *   for clipping or padding before calling this function.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamHeight The height of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param toStreamPoint A callback to construct the final return object from the computed stream
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapUiPointToStreamPoint(
        uiX: Float,
        uiY: Float,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        crossinline toStreamPoint: (streamX: Float, streamY: Float) -> T,
    ): T {
        val uiWidthFloat = uiWidth.toFloat()
        val uiHeightFloat = uiHeight.toFloat()

        val uiNormalizedX: Float
        if (!streamMirroring) {
            uiNormalizedX = uiX / uiWidthFloat
        } else {
            uiNormalizedX = (uiWidthFloat - uiX) / uiWidthFloat
        }
        val uiNormalizedY = uiY / uiHeightFloat

        val streamNormalizedX: Float
        val streamNormalizedY: Float
        when (sensorRotationToDisplayOrientation) {
            DiscreteRotation.ROTATION_0 -> {
                streamNormalizedX = uiNormalizedX
                streamNormalizedY = uiNormalizedY
            }
            DiscreteRotation.ROTATION_90 -> {
                streamNormalizedX = uiNormalizedY
                streamNormalizedY = 1 - uiNormalizedX
            }
            DiscreteRotation.ROTATION_180 -> {
                streamNormalizedX = 1 - uiNormalizedX
                streamNormalizedY = 1 - uiNormalizedY
            }
            DiscreteRotation.ROTATION_270 -> {
                streamNormalizedX = 1 - uiNormalizedY
                streamNormalizedY = uiNormalizedX
            }
            else ->
                throw IllegalArgumentException(
                    "Unexpected degrees: $sensorRotationToDisplayOrientation"
                )
        }

        return toStreamPoint(
            streamNormalizedX * streamWidth.toFloat(),
            streamNormalizedY * streamHeight.toFloat(),
        )
    }

    /**
     * Converts a rectangle relative to a UI element to an equivalent rectangle relative to a
     * specific camera stream.
     *
     * @param uiRect The rectangle in the UI. UI coordinates are relative to the uiSize, and does
     *   not account for the positioning of the UI. Callers must normalize coordinates to account
     *   for clipping or padding before calling this function.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     */
    @JvmStatic
    public fun mapUiRectToStreamRectF(
        uiRect: RectF,
        uiSize: Size,
        streamSize: Size,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: Int,
    ): RectF =
        mapUiRectToStreamRect(
            uiLeft = uiRect.left,
            uiTop = uiRect.top,
            uiRight = uiRect.right,
            uiBottom = uiRect.bottom,
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
        ) { streamLeft, streamTop, streamRight, streamBottom ->
            RectF(streamLeft, streamTop, streamRight, streamBottom)
        }

    /**
     * Converts a rectangle relative to a UI element to an equivalent rectangle relative to a
     * specific camera stream.
     *
     * @param uiLeft The left coordinate of the rectangle in the UI. UI coordinates are relative to
     *   the uiWidth, and does not account for the positioning of the UI. Callers must normalize
     *   coordinates to account for clipping or padding before calling this function.
     * @param uiTop The top coordinate of the rectangle in the UI. UI coordinates are relative to
     *   the uiHeight, and does not account for the positioning of the UI. Callers must normalize
     *   coordinates to account for clipping or padding before calling this function.
     * @param uiRight The right coordinate of the rectangle in the UI. UI coordinates are relative
     *   to the uiWidth, and does not account for the positioning of the UI. Callers must normalize
     *   coordinates to account for clipping or padding before calling this function.
     * @param uiBottom The bottom coordinate of the rectangle in the UI. UI coordinates are relative
     *   to the uiHeight, and does not account for the positioning of the UI. Callers must normalize
     *   coordinates to account for clipping or padding before calling this function.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamHeight The height of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param toStreamRect A callback to construct the final return object from the computed stream
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapUiRectToStreamRect(
        uiLeft: Float,
        uiTop: Float,
        uiRight: Float,
        uiBottom: Float,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        crossinline toStreamRect:
            (streamLeft: Float, streamTop: Float, streamRight: Float, streamBottom: Float) -> T,
    ): T {
        val uiWidthFloat = uiWidth.toFloat()
        val uiHeightFloat = uiHeight.toFloat()

        val uiNormalizedLeft: Float
        val uiNormalizedRight: Float
        if (!streamMirroring) {
            uiNormalizedLeft = uiLeft / uiWidthFloat
            uiNormalizedRight = uiRight / uiWidthFloat
        } else {
            uiNormalizedLeft = (uiWidthFloat - uiRight) / uiWidthFloat
            uiNormalizedRight = (uiWidthFloat - uiLeft) / uiWidthFloat
        }
        val uiNormalizedTop = uiTop / uiHeightFloat
        val uiNormalizedBottom = uiBottom / uiHeightFloat

        val streamNormalizedLeft: Float
        val streamNormalizedTop: Float
        val streamNormalizedRight: Float
        val streamNormalizedBottom: Float

        when (sensorRotationToDisplayOrientation) {
            DiscreteRotation.ROTATION_0 -> {
                streamNormalizedLeft = uiNormalizedLeft
                streamNormalizedTop = uiNormalizedTop
                streamNormalizedRight = uiNormalizedRight
                streamNormalizedBottom = uiNormalizedBottom
            }
            DiscreteRotation.ROTATION_90 -> {
                streamNormalizedLeft = uiNormalizedTop
                streamNormalizedTop = 1 - uiNormalizedRight
                streamNormalizedRight = uiNormalizedBottom
                streamNormalizedBottom = 1 - uiNormalizedLeft
            }
            DiscreteRotation.ROTATION_180 -> {
                streamNormalizedLeft = 1 - uiNormalizedRight
                streamNormalizedTop = 1 - uiNormalizedBottom
                streamNormalizedRight = 1 - uiNormalizedLeft
                streamNormalizedBottom = 1 - uiNormalizedTop
            }
            DiscreteRotation.ROTATION_270 -> {
                streamNormalizedLeft = 1 - uiNormalizedBottom
                streamNormalizedTop = uiNormalizedLeft
                streamNormalizedRight = 1 - uiNormalizedTop
                streamNormalizedBottom = uiNormalizedRight
            }
            else ->
                throw IllegalArgumentException(
                    "Unexpected degrees: $sensorRotationToDisplayOrientation"
                )
        }

        return toStreamRect(
            streamNormalizedLeft * streamWidth.toFloat(),
            streamNormalizedTop * streamHeight.toFloat(),
            streamNormalizedRight * streamWidth.toFloat(),
            streamNormalizedBottom * streamHeight.toFloat(),
        )
    }

    /**
     * Converts a point relative to a UI element to an equivalent rectangle relative to a specific
     * camera stream.
     *
     * @param uiPoint The point in the UI. UI coordinates are relative to the uiSize, and does not
     *   account for the positioning of the UI. Callers must normalize coordinates to account for
     *   clipping or padding before calling this function.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param touchToFocusRatio The ratio to use when computing a rectangle based on the short
     *   dimension of the stream.
     */
    @JvmStatic
    @JvmOverloads
    public fun mapUiPointToStreamRectF(
        uiPoint: Point,
        uiSize: Size,
        streamSize: Size,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: Int,
        touchToFocusRatio: Float = DEFAULT_POINT_TO_RECT_RATIO,
    ): RectF =
        mapUiPointToStreamRect(
            uiX = uiPoint.x.toFloat(),
            uiY = uiPoint.y.toFloat(),
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
            ratio = touchToFocusRatio,
        ) { streamLeft, streamTop, streamRight, streamBottom ->
            RectF(streamLeft, streamTop, streamRight, streamBottom)
        }

    /**
     * Converts a point relative to a UI element to an equivalent rectangle relative to a specific
     * camera stream.
     *
     * @param uiX The x coordinate in the UI. UI coordinates are relative to the uiWidth, and does
     *   not account for the positioning of the UI. Callers must normalize coordinates to account
     *   for clipping or padding before calling this function.
     * @param uiY The y coordinate in the UI. UI coordinates are relative to the uiHeight, and does
     *   not account for the positioning of the UI. Callers must normalize coordinates to account
     *   for clipping or padding before calling this function.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamHeight The height of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param ratio The ratio to use when computing a rectangle based on the short dimension of the
     *   stream.
     * @param toStreamRect A callback to construct the final return object from the computed stream
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapUiPointToStreamRect(
        uiX: Float,
        uiY: Float,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        ratio: Float = DEFAULT_POINT_TO_RECT_RATIO,
        crossinline toStreamRect:
            (streamLeft: Float, streamTop: Float, streamRight: Float, streamBottom: Float) -> T,
    ): T =
        mapUiPointToStreamPoint(
            uiX = uiX,
            uiY = uiY,
            uiWidth = uiWidth,
            uiHeight = uiHeight,
            streamWidth = streamWidth,
            streamHeight = streamHeight,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation = sensorRotationToDisplayOrientation,
        ) { streamX, streamY ->
            val shortEdge = minOf(streamWidth.toFloat(), streamHeight.toFloat())
            val rectSize = shortEdge * ratio
            val halfRectSize = rectSize / 2.0f

            val streamLeft = streamX - halfRectSize
            val streamRight = streamX + (rectSize - halfRectSize)
            val streamTop = streamY - halfRectSize
            val streamBottom = streamY + (rectSize - halfRectSize)

            toStreamRect(streamLeft, streamTop, streamRight, streamBottom)
        }

    /**
     * Converts a size relative to a UI element to an equivalent size relative to a specific camera
     * stream.
     *
     * @param size The size in the UI. UI coordinates are relative to the uiSize, and does not
     *   account for the positioning of the UI. Callers must normalize coordinates to account for
     *   clipping or padding before calling this function.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     */
    @JvmStatic
    @JvmName("mapUiSizeToStreamSizeF")
    public fun mapUiSizeToStreamSizeF(
        size: Size,
        uiSize: Size,
        streamSize: Size,
        sensorRotationToDisplayOrientation: Int,
    ): SizeF =
        mapUiSizeToStreamSize(
            uiSizeWidth = size.width.toFloat(),
            uiSizeHeight = size.height.toFloat(),
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
        ) { streamWidthFloat, streamHeightFloat ->
            SizeF(streamWidthFloat, streamHeightFloat)
        }

    /**
     * Converts a size relative to a UI element to an equivalent size relative to a specific camera
     * stream.
     *
     * @param uiSizeWidth The width of the size in the UI. UI coordinates are relative to the
     *   uiWidth, and does not account for the positioning of the UI. Callers must normalize
     *   coordinates to account for clipping or padding before calling this function.
     * @param uiSizeHeight The height of the size in the UI. UI coordinates are relative to the
     *   uiHeight, and does not account for the positioning of the UI. Callers must normalize
     *   coordinates to account for clipping or padding before calling this function.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream.
     * @param streamHeight The height of the camera stream.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param toStreamSize A callback to construct the final return object from the computed stream
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapUiSizeToStreamSize(
        uiSizeWidth: Float,
        uiSizeHeight: Float,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        crossinline toStreamSize: (streamWidth: Float, streamHeight: Float) -> T,
    ): T {
        val normalizedWidth = uiSizeWidth / uiWidth.toFloat()
        val normalizedHeight = uiSizeHeight / uiHeight.toFloat()

        val normalizedStreamWidth: Float
        val normalizedStreamHeight: Float

        if (
            sensorRotationToDisplayOrientation == DiscreteRotation.ROTATION_0 ||
                sensorRotationToDisplayOrientation == DiscreteRotation.ROTATION_180
        ) {
            normalizedStreamWidth = normalizedWidth
            normalizedStreamHeight = normalizedHeight
        } else {
            normalizedStreamWidth = normalizedHeight
            normalizedStreamHeight = normalizedWidth
        }

        return toStreamSize(
            normalizedStreamWidth * streamWidth.toFloat(),
            normalizedStreamHeight * streamHeight.toFloat(),
        )
    }

    // --- Mapping from UI to SENSOR ---

    /**
     * Converts a point relative to a UI element to an equivalent point relative to the camera
     * sensor.
     *
     * @param uiPoint The point in the UI. UI coordinates are relative to the uiSize, and does not
     *   account for the positioning of the UI. Callers must normalize coordinates to account for
     *   clipping or padding before calling this function.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCrop The crop region of the sensor (typically CameraMetadata.SCALER_CROP_REGION,
     *   or CameraMetadata.SENSOR_INFO_ACTIVE_ARRAY_SIZE if no crop is applied). Sensor coordinates
     *   are relative to the sensor 0,0.
     * @param coerceToCropRegion Whether to coerce the output point into the crop region.
     */
    @JvmStatic
    @JvmOverloads
    public fun mapUiPointToSensorPoint(
        uiPoint: Point,
        uiSize: Size,
        streamSize: Size,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: Int,
        sensorCrop: Rect,
        coerceToCropRegion: Boolean = true,
    ): Point =
        mapUiPointToSensorPoint(
            uiX = uiPoint.x.toFloat(),
            uiY = uiPoint.y.toFloat(),
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
            sensorCropX = sensorCrop.left,
            sensorCropY = sensorCrop.top,
            sensorCropWidth = sensorCrop.width(),
            sensorCropHeight = sensorCrop.height(),
            coerceToCropRegion = coerceToCropRegion,
        ) { sensorX, sensorY ->
            Point(sensorX, sensorY)
        }

    /**
     * Converts a point relative to a UI element to an equivalent point relative to the camera
     * sensor.
     *
     * @param uiX The x coordinate in the UI. UI coordinates are relative to the uiWidth, and does
     *   not account for the positioning of the UI. Callers must normalize coordinates to account
     *   for clipping or padding before calling this function.
     * @param uiY The y coordinate in the UI. UI coordinates are relative to the uiHeight, and does
     *   not account for the positioning of the UI. Callers must normalize coordinates to account
     *   for clipping or padding before calling this function.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamHeight The height of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCropX The x coordinate of the crop region of the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorCropY The y coordinate of the crop region of the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorCropWidth The width of the crop region of the sensor.
     * @param sensorCropHeight The height of the crop region of the sensor.
     * @param coerceToCropRegion Whether to coerce the output point into the crop region.
     * @param toSensorPoint A callback to construct the final return object from the computed sensor
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapUiPointToSensorPoint(
        uiX: Float,
        uiY: Float,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        sensorCropX: Int,
        sensorCropY: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        coerceToCropRegion: Boolean = true,
        crossinline toSensorPoint: (sensorX: Int, sensorY: Int) -> T,
    ): T =
        mapUiPointToStreamPoint(
            uiX = uiX,
            uiY = uiY,
            uiWidth = uiWidth,
            uiHeight = uiHeight,
            streamWidth = streamWidth,
            streamHeight = streamHeight,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation = sensorRotationToDisplayOrientation,
        ) { streamX, streamY ->
            mapStreamPointToSensorPoint(
                streamX = streamX,
                streamY = streamY,
                streamWidth = streamWidth,
                streamHeight = streamHeight,
                sensorCropX = sensorCropX,
                sensorCropY = sensorCropY,
                sensorCropWidth = sensorCropWidth,
                sensorCropHeight = sensorCropHeight,
                coerceToCropRegion = coerceToCropRegion,
            ) { sensorX, sensorY ->
                toSensorPoint(sensorX, sensorY)
            }
        }

    /**
     * Converts a rectangle relative to a UI element to an equivalent rectangle relative to the
     * camera sensor.
     *
     * @param uiRect The rectangle in the UI. UI coordinates are relative to the uiSize, and does
     *   not account for the positioning of the UI. Callers must normalize coordinates to account
     *   for clipping or padding before calling this function.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCrop The crop region of the sensor (typically CameraMetadata.SCALER_CROP_REGION,
     *   or CameraMetadata.SENSOR_INFO_ACTIVE_ARRAY_SIZE if no crop is applied). Sensor coordinates
     *   are relative to the sensor 0,0.
     * @param minimumSensorSize The minimum allowed width and height for the output rectangle in
     *   sensor pixels. If the calculated rectangle is smaller than this value in either dimension,
     *   it will be expanded symmetrically from its center to reach the minimum. Defaults to
     *   [DEFAULT_MIN_SENSOR_PIXELS]. Set to 0 or less to disable this behavior.
     * @param coerceToCropRegion Whether to coerce the output rectangle into the crop region.
     */
    @JvmStatic
    @JvmOverloads
    public fun mapUiRectToSensorRect(
        uiRect: RectF,
        uiSize: Size,
        streamSize: Size,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: Int,
        sensorCrop: Rect,
        minimumSensorSize: Int = DEFAULT_MIN_SENSOR_PIXELS,
        coerceToCropRegion: Boolean = true,
    ): Rect =
        mapUiRectToSensorRect(
            uiLeft = uiRect.left,
            uiTop = uiRect.top,
            uiRight = uiRect.right,
            uiBottom = uiRect.bottom,
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
            sensorCropX = sensorCrop.left,
            sensorCropY = sensorCrop.top,
            sensorCropWidth = sensorCrop.width(),
            sensorCropHeight = sensorCrop.height(),
            minimumSensorSize = minimumSensorSize,
            coerceToCropRegion = coerceToCropRegion,
        ) { sensorLeft, sensorTop, sensorRight, sensorBottom ->
            Rect(sensorLeft, sensorTop, sensorRight, sensorBottom)
        }

    /**
     * Converts a rectangle relative to a UI element to an equivalent rectangle relative to the
     * camera sensor.
     *
     * @param uiLeft The left coordinate of the rectangle in the UI. UI coordinates are relative to
     *   the uiWidth, and does not account for the positioning of the UI. Callers must normalize
     *   coordinates to account for clipping or padding before calling this function.
     * @param uiTop The top coordinate of the rectangle in the UI. UI coordinates are relative to
     *   the uiHeight, and does not account for the positioning of the UI. Callers must normalize
     *   coordinates to account for clipping or padding before calling this function.
     * @param uiRight The right coordinate of the rectangle in the UI. UI coordinates are relative
     *   to the uiWidth, and does not account for the positioning of the UI. Callers must normalize
     *   coordinates to account for clipping or padding before calling this function.
     * @param uiBottom The bottom coordinate of the rectangle in the UI. UI coordinates are relative
     *   to the uiHeight, and does not account for the positioning of the UI. Callers must normalize
     *   coordinates to account for clipping or padding before calling this function.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamHeight The height of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCropX The x coordinate of the crop region of the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorCropY The y coordinate of the crop region of the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorCropWidth The width of the crop region of the sensor.
     * @param sensorCropHeight The height of the crop region of the sensor.
     * @param minimumSensorSize The minimum allowed width and height for the output rectangle in
     *   sensor pixels. If the calculated rectangle is smaller than this value in either dimension,
     *   it will be expanded symmetrically from its center to reach the minimum. Defaults to
     *   [DEFAULT_MIN_SENSOR_PIXELS]. Set to 0 or less to disable this behavior.
     * @param coerceToCropRegion Whether to coerce the output rectangle into the crop region.
     * @param toSensorRect A callback to construct the final return object from the computed sensor
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapUiRectToSensorRect(
        uiLeft: Float,
        uiTop: Float,
        uiRight: Float,
        uiBottom: Float,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        sensorCropX: Int,
        sensorCropY: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        minimumSensorSize: Int = DEFAULT_MIN_SENSOR_PIXELS,
        coerceToCropRegion: Boolean = true,
        crossinline toSensorRect:
            (sensorLeft: Int, sensorTop: Int, sensorRight: Int, sensorBottom: Int) -> T,
    ): T =
        mapUiRectToStreamRect(
            uiLeft = uiLeft,
            uiTop = uiTop,
            uiRight = uiRight,
            uiBottom = uiBottom,
            uiWidth = uiWidth,
            uiHeight = uiHeight,
            streamWidth = streamWidth,
            streamHeight = streamHeight,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation = sensorRotationToDisplayOrientation,
        ) { streamLeft, streamTop, streamRight, streamBottom ->
            mapStreamRectToSensorRect(
                streamLeft = streamLeft,
                streamTop = streamTop,
                streamRight = streamRight,
                streamBottom = streamBottom,
                streamWidth = streamWidth,
                streamHeight = streamHeight,
                sensorCropX = sensorCropX,
                sensorCropY = sensorCropY,
                sensorCropWidth = sensorCropWidth,
                sensorCropHeight = sensorCropHeight,
                minimumSensorSize = minimumSensorSize,
                coerceToCropRegion = coerceToCropRegion,
                toSensorRect = toSensorRect,
            )
        }

    /**
     * Converts a point relative to a UI element to an equivalent rectangle relative to the camera
     * sensor.
     *
     * The output rectangle will have a size determined by the [touchToFocusRatio] and will be
     * centered on the mapped point. If the calculated rectangle is smaller than [minimumSensorSize]
     * in either dimension, it will be expanded symmetrically from its center to reach the minimum.
     *
     * @param uiPoint The point in the UI. UI coordinates are relative to the uiSize, and does not
     *   account for the positioning of the UI. Callers must normalize coordinates to account for
     *   clipping or padding before calling this function.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCrop The crop region of the sensor (typically CameraMetadata.SCALER_CROP_REGION,
     *   or CameraMetadata.SENSOR_INFO_ACTIVE_ARRAY_SIZE if no crop is applied). Sensor coordinates
     *   are relative to the sensor 0,0.
     * @param minimumSensorSize The minimum allowed width and height for the output rectangle in
     *   sensor pixels. If the calculated rectangle is smaller than this value in either dimension,
     *   it will be expanded symmetrically from its center to reach the minimum. Defaults to
     *   [DEFAULT_MIN_SENSOR_PIXELS]. Set to 0 or less to disable this behavior.
     * @param coerceToCropRegion Whether to coerce the output rectangle into the crop region.
     * @param touchToFocusRatio The ratio to use when computing a rectangle based on the short
     *   dimension of the stream.
     */
    @JvmStatic
    @JvmOverloads
    public fun mapUiPointToSensorRect(
        uiPoint: Point,
        uiSize: Size,
        streamSize: Size,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: Int,
        sensorCrop: Rect,
        minimumSensorSize: Int = DEFAULT_MIN_SENSOR_PIXELS,
        coerceToCropRegion: Boolean = true,
        touchToFocusRatio: Float = DEFAULT_POINT_TO_RECT_RATIO,
    ): Rect =
        mapUiPointToSensorRect(
            uiX = uiPoint.x.toFloat(),
            uiY = uiPoint.y.toFloat(),
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
            sensorCropX = sensorCrop.left,
            sensorCropY = sensorCrop.top,
            sensorCropWidth = sensorCrop.width(),
            sensorCropHeight = sensorCrop.height(),
            minimumSensorSize = minimumSensorSize,
            coerceToCropRegion = coerceToCropRegion,
            ratio = touchToFocusRatio,
        ) { sensorLeft, sensorTop, sensorRight, sensorBottom ->
            Rect(sensorLeft, sensorTop, sensorRight, sensorBottom)
        }

    /**
     * Converts a point relative to a UI element to an equivalent rectangle relative to the camera
     * sensor.
     *
     * @param uiX The x coordinate in the UI. UI coordinates are relative to the uiWidth, and does
     *   not account for the positioning of the UI. Callers must normalize coordinates to account
     *   for clipping or padding before calling this function.
     * @param uiY The y coordinate in the UI. UI coordinates are relative to the uiHeight, and does
     *   not account for the positioning of the UI. Callers must normalize coordinates to account
     *   for clipping or padding before calling this function.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamHeight The height of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCropX The x coordinate of the crop region of the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorCropY The y coordinate of the crop region of the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorCropWidth The width of the crop region of the sensor.
     * @param sensorCropHeight The height of the crop region of the sensor.
     * @param minimumSensorSize The minimum allowed width and height for the output rectangle in
     *   sensor pixels. If the calculated rectangle is smaller than this value in either dimension,
     *   it will be expanded symmetrically from its center to reach the minimum. Defaults to
     *   [DEFAULT_MIN_SENSOR_PIXELS]. Set to 0 or less to disable this behavior.
     * @param coerceToCropRegion Whether to coerce the output rectangle into the crop region.
     * @param ratio The ratio to use when computing a rectangle based on the short dimension of the
     *   stream.
     * @param toSensorRect A callback to construct the final return object from the computed sensor
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapUiPointToSensorRect(
        uiX: Float,
        uiY: Float,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        sensorCropX: Int,
        sensorCropY: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        minimumSensorSize: Int = DEFAULT_MIN_SENSOR_PIXELS,
        coerceToCropRegion: Boolean = true,
        ratio: Float = DEFAULT_POINT_TO_RECT_RATIO,
        crossinline toSensorRect:
            (sensorLeft: Int, sensorTop: Int, sensorRight: Int, sensorBottom: Int) -> T,
    ): T =
        mapUiPointToStreamPoint(
            uiX = uiX,
            uiY = uiY,
            uiWidth = uiWidth,
            uiHeight = uiHeight,
            streamWidth = streamWidth,
            streamHeight = streamHeight,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation = sensorRotationToDisplayOrientation,
        ) { streamX, streamY ->
            mapStreamPointToSensorRect(
                streamX = streamX,
                streamY = streamY,
                streamWidth = streamWidth,
                streamHeight = streamHeight,
                sensorCropX = sensorCropX,
                sensorCropY = sensorCropY,
                sensorCropWidth = sensorCropWidth,
                sensorCropHeight = sensorCropHeight,
                minimumSensorSize = minimumSensorSize,
                coerceToCropRegion = coerceToCropRegion,
                ratio = ratio,
                toSensorRect = toSensorRect,
            )
        }

    /**
     * Converts a size relative to a UI element to an equivalent size relative to the camera sensor.
     *
     * @param size The size in the UI. UI coordinates are relative to the uiSize, and does not
     *   account for the positioning of the UI. Callers must normalize coordinates to account for
     *   clipping or padding before calling this function.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCropWidth The width of the sensor crop region.
     * @param sensorCropHeight The height of the sensor crop region.
     */
    @JvmStatic
    public fun mapUiSizeToSensorSize(
        size: Size,
        uiSize: Size,
        streamSize: Size,
        sensorRotationToDisplayOrientation: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
    ): Size =
        mapUiSizeToSensorSize(
            uiSizeWidth = size.width.toFloat(),
            uiSizeHeight = size.height.toFloat(),
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
            sensorCropWidth = sensorCropWidth,
            sensorCropHeight = sensorCropHeight,
        ) { sensorWidth, sensorHeight ->
            Size(sensorWidth, sensorHeight)
        }

    /**
     * Converts a size relative to a UI element to an equivalent size relative to the camera sensor.
     *
     * @param uiSizeWidth The width of the size in the UI. UI coordinates are relative to the
     *   uiWidth, and does not account for the positioning of the UI. Callers must normalize
     *   coordinates to account for clipping or padding before calling this function.
     * @param uiSizeHeight The height of the size in the UI. UI coordinates are relative to the
     *   uiHeight, and does not account for the positioning of the UI. Callers must normalize
     *   coordinates to account for clipping or padding before calling this function.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamHeight The height of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCropWidth The width of the sensor crop region.
     * @param sensorCropHeight The height of the sensor crop region.
     * @param toSensorSize A callback to construct the final return object from the computed sensor
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapUiSizeToSensorSize(
        uiSizeWidth: Float,
        uiSizeHeight: Float,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        crossinline toSensorSize: (sensorWidth: Int, sensorHeight: Int) -> T,
    ): T =
        mapUiSizeToStreamSize(
            uiSizeWidth = uiSizeWidth,
            uiSizeHeight = uiSizeHeight,
            uiWidth = uiWidth,
            uiHeight = uiHeight,
            streamWidth = streamWidth,
            streamHeight = streamHeight,
            sensorRotationToDisplayOrientation = sensorRotationToDisplayOrientation,
        ) { streamWidthFloat, streamHeightFloat ->
            mapStreamSizeToSensorSize(
                streamSizeWidth = streamWidthFloat,
                streamSizeHeight = streamHeightFloat,
                streamWidth = streamWidth,
                streamHeight = streamHeight,
                sensorCropWidth = sensorCropWidth,
                sensorCropHeight = sensorCropHeight,
                toSensorSize = toSensorSize,
            )
        }

    // --- Mapping from STREAM to UI ---

    /**
     * Converts a point relative to a specific camera stream to an equivalent point relative to a UI
     * element.
     *
     * @param streamPoint The point in the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     */
    @JvmStatic
    public fun mapStreamPointToUiPointF(
        streamPoint: PointF,
        uiSize: Size,
        streamSize: Size,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: Int,
    ): PointF =
        mapStreamPointToUiPoint(
            streamX = streamPoint.x,
            streamY = streamPoint.y,
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
        ) { uiX, uiY ->
            PointF(uiX, uiY)
        }

    /**
     * Converts a point relative to a specific camera stream to an equivalent point relative to a UI
     * element.
     *
     * @param streamX The x coordinate in the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamY The y coordinate in the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream.
     * @param streamHeight The height of the camera stream.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param toUiPoint A callback to construct the final return object from the computed UI
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapStreamPointToUiPoint(
        streamX: Float,
        streamY: Float,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        crossinline toUiPoint: (uiX: Float, uiY: Float) -> T,
    ): T {
        val streamWidthFloat = streamWidth.toFloat()
        val streamHeightFloat = streamHeight.toFloat()
        val streamNormalizedX = streamX / streamWidthFloat
        val streamNormalizedY = streamY / streamHeightFloat

        val uiNormalizedX: Float
        val uiNormalizedY: Float

        when (sensorRotationToDisplayOrientation) {
            DiscreteRotation.ROTATION_0 -> {
                uiNormalizedX = streamNormalizedX
                uiNormalizedY = streamNormalizedY
            }
            DiscreteRotation.ROTATION_90 -> {
                uiNormalizedX = 1 - streamNormalizedY
                uiNormalizedY = streamNormalizedX
            }
            DiscreteRotation.ROTATION_180 -> {
                uiNormalizedX = 1 - streamNormalizedX
                uiNormalizedY = 1 - streamNormalizedY
            }
            DiscreteRotation.ROTATION_270 -> {
                uiNormalizedX = streamNormalizedY
                uiNormalizedY = 1 - streamNormalizedX
            }
            else ->
                throw IllegalArgumentException(
                    "Unexpected degrees: $sensorRotationToDisplayOrientation"
                )
        }

        val uiX: Float =
            if (!streamMirroring) {
                uiNormalizedX * uiWidth.toFloat()
            } else {
                (1 - uiNormalizedX) * uiWidth.toFloat()
            }
        val uiY = uiNormalizedY * uiHeight.toFloat()

        return toUiPoint(uiX, uiY)
    }

    /**
     * Converts a rectangle relative to a specific camera stream to an equivalent rectangle relative
     * to a UI element.
     *
     * @param streamRect The rectangle in the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     */
    @JvmStatic
    public fun mapStreamRectToUiRectF(
        streamRect: RectF,
        uiSize: Size,
        streamSize: Size,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: Int,
    ): RectF =
        mapStreamRectToUiRect(
            streamLeft = streamRect.left,
            streamTop = streamRect.top,
            streamRight = streamRect.right,
            streamBottom = streamRect.bottom,
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
        ) { uiLeft, uiTop, uiRight, uiBottom ->
            RectF(uiLeft, uiTop, uiRight, uiBottom)
        }

    /**
     * Converts a rectangle relative to a specific camera stream to an equivalent rectangle relative
     * to a UI element.
     *
     * @param streamLeft The left coordinate of the rectangle in the camera stream. Stream
     *   coordinates are relative to the streamWidth/streamHeight.
     * @param streamTop The top coordinate of the rectangle in the camera stream. Stream coordinates
     *   are relative to the streamWidth/streamHeight.
     * @param streamRight The right coordinate of the rectangle in the camera stream. Stream
     *   coordinates are relative to the streamWidth/streamHeight.
     * @param streamBottom The bottom coordinate of the rectangle in the camera stream. Stream
     *   coordinates are relative to the streamWidth/streamHeight.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream.
     * @param streamHeight The height of the camera stream.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param toUiRect A callback to construct the final return object from the computed UI
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapStreamRectToUiRect(
        streamLeft: Float,
        streamTop: Float,
        streamRight: Float,
        streamBottom: Float,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        crossinline toUiRect: (uiLeft: Float, uiTop: Float, uiRight: Float, uiBottom: Float) -> T,
    ): T {
        val streamWidthFloat = streamWidth.toFloat()
        val streamHeightFloat = streamHeight.toFloat()
        val streamNormalizedLeft = streamLeft / streamWidthFloat
        val streamNormalizedTop = streamTop / streamHeightFloat
        val streamNormalizedRight = streamRight / streamWidthFloat
        val streamNormalizedBottom = streamBottom / streamHeightFloat

        val uiNormalizedLeft: Float
        val uiNormalizedTop: Float
        val uiNormalizedRight: Float
        val uiNormalizedBottom: Float

        when (sensorRotationToDisplayOrientation) {
            DiscreteRotation.ROTATION_0 -> {
                uiNormalizedLeft = streamNormalizedLeft
                uiNormalizedTop = streamNormalizedTop
                uiNormalizedRight = streamNormalizedRight
                uiNormalizedBottom = streamNormalizedBottom
            }
            DiscreteRotation.ROTATION_90 -> {
                uiNormalizedLeft = 1 - streamNormalizedBottom
                uiNormalizedTop = streamNormalizedLeft
                uiNormalizedRight = 1 - streamNormalizedTop
                uiNormalizedBottom = streamNormalizedRight
            }
            DiscreteRotation.ROTATION_180 -> {
                uiNormalizedLeft = 1 - streamNormalizedRight
                uiNormalizedTop = 1 - streamNormalizedBottom
                uiNormalizedRight = 1 - streamNormalizedLeft
                uiNormalizedBottom = 1 - streamNormalizedTop
            }
            DiscreteRotation.ROTATION_270 -> {
                uiNormalizedLeft = streamNormalizedTop
                uiNormalizedTop = 1 - streamNormalizedRight
                uiNormalizedRight = streamNormalizedBottom
                uiNormalizedBottom = 1 - streamNormalizedLeft
            }
            else ->
                throw IllegalArgumentException(
                    "Unexpected degrees: $sensorRotationToDisplayOrientation"
                )
        }

        val uiLeft: Float
        val uiRight: Float
        if (!streamMirroring) {
            uiLeft = uiNormalizedLeft * uiWidth.toFloat()
            uiRight = uiNormalizedRight * uiWidth.toFloat()
        } else {
            uiLeft = (1 - uiNormalizedRight) * uiWidth.toFloat()
            uiRight = (1 - uiNormalizedLeft) * uiWidth.toFloat()
        }
        val uiTop = uiNormalizedTop * uiHeight.toFloat()
        val uiBottom = uiNormalizedBottom * uiHeight.toFloat()

        return toUiRect(uiLeft, uiTop, uiRight, uiBottom)
    }

    /**
     * Converts a size relative to a specific camera stream to an equivalent size relative to a UI
     * element.
     *
     * @param size The size in the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     */
    @JvmStatic
    @JvmName("mapStreamSizeToUiSizeF")
    public fun mapStreamSizeToUiSizeF(
        size: Size,
        uiSize: Size,
        streamSize: Size,
        sensorRotationToDisplayOrientation: Int,
    ): SizeF =
        mapStreamSizeToUiSize(
            streamSizeWidth = size.width.toFloat(),
            streamSizeHeight = size.height.toFloat(),
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
        ) { widthFloat, heightFloat ->
            SizeF(widthFloat, heightFloat)
        }

    /**
     * Converts a size relative to a specific camera stream to an equivalent size relative to a UI
     * element.
     *
     * @param streamSizeWidth The width of the size in the camera stream. Stream coordinates are
     *   relative to the streamWidth/streamHeight.
     * @param streamSizeHeight The height of the size in the camera stream. Stream coordinates are
     *   relative to the streamWidth/streamHeight.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream.
     * @param streamHeight The height of the camera stream.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param toUiSize A callback to construct the final return object from the computed UI
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapStreamSizeToUiSize(
        streamSizeWidth: Float,
        streamSizeHeight: Float,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        crossinline toUiSize: (width: Float, height: Float) -> T,
    ): T {
        val normalizedWidth = streamSizeWidth / streamWidth.toFloat()
        val normalizedHeight = streamSizeHeight / streamHeight.toFloat()

        val normalizedUiWidth: Float
        val normalizedUiHeight: Float

        if (
            sensorRotationToDisplayOrientation == DiscreteRotation.ROTATION_0 ||
                sensorRotationToDisplayOrientation == DiscreteRotation.ROTATION_180
        ) {
            normalizedUiWidth = normalizedWidth
            normalizedUiHeight = normalizedHeight
        } else {
            normalizedUiWidth = normalizedHeight
            normalizedUiHeight = normalizedWidth
        }

        return toUiSize(
            normalizedUiWidth * uiWidth.toFloat(),
            normalizedUiHeight * uiHeight.toFloat(),
        )
    }

    // --- Mapping from STREAM to SENSOR ---

    /**
     * Converts a point relative to a specific camera stream to an equivalent point relative to the
     * camera sensor.
     *
     * @param streamPoint The point in the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param streamSize The size of the camera stream.
     * @param sensorCrop The crop region of the sensor (typically CameraMetadata.SCALER_CROP_REGION,
     *   or CameraMetadata.SENSOR_INFO_ACTIVE_ARRAY_SIZE if no crop is applied). Sensor coordinates
     *   are relative to the sensor 0,0.
     * @param coerceToCropRegion Whether to coerce the output point into the crop region.
     */
    @JvmStatic
    @JvmOverloads
    public fun mapStreamPointToSensorPoint(
        streamPoint: PointF,
        streamSize: Size,
        sensorCrop: Rect,
        coerceToCropRegion: Boolean = true,
    ): Point =
        mapStreamPointToSensorPoint(
            streamX = streamPoint.x,
            streamY = streamPoint.y,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            sensorCropX = sensorCrop.left,
            sensorCropY = sensorCrop.top,
            sensorCropWidth = sensorCrop.width(),
            sensorCropHeight = sensorCrop.height(),
            coerceToCropRegion = coerceToCropRegion,
        ) { sensorX, sensorY ->
            Point(sensorX, sensorY)
        }

    /**
     * Converts a point relative to a specific camera stream to an equivalent point relative to the
     * camera sensor.
     *
     * @param streamX The x coordinate in the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamY The y coordinate in the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamWidth The width of the camera stream.
     * @param streamHeight The height of the camera stream.
     * @param sensorCropX The x coordinate of the crop region of the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorCropY The y coordinate of the crop region of the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorCropWidth The width of the crop region of the sensor.
     * @param sensorCropHeight The height of the crop region of the sensor.
     * @param coerceToCropRegion Whether to coerce the output point into the crop region.
     * @param toSensorPoint A callback to construct the final return object from the computed sensor
     *   coordinates.
     */
    @JvmSynthetic
    public inline fun <T> mapStreamPointToSensorPoint(
        streamX: Float,
        streamY: Float,
        streamWidth: Int,
        streamHeight: Int,
        sensorCropX: Int,
        sensorCropY: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        coerceToCropRegion: Boolean = true,
        crossinline toSensorPoint: (sensorX: Int, sensorY: Int) -> T,
    ): T {
        return streamToSensorTransform(
            streamWidth = streamWidth,
            streamHeight = streamHeight,
            sensorCropX = sensorCropX,
            sensorCropY = sensorCropY,
            sensorCropWidth = sensorCropWidth,
            sensorCropHeight = sensorCropHeight,
        ) { scalar, sensorOffsetX, sensorOffsetY ->
            var sensorX = (streamX * scalar).roundToInt() + sensorOffsetX
            var sensorY = (streamY * scalar).roundToInt() + sensorOffsetY

            if (coerceToCropRegion) {
                sensorX = sensorX.coerceIn(sensorCropX, sensorCropX + sensorCropWidth)
                sensorY = sensorY.coerceIn(sensorCropY, sensorCropY + sensorCropHeight)
            }

            toSensorPoint(sensorX, sensorY)
        }
    }

    /**
     * Converts a rectangle relative to a specific camera stream to an equivalent rectangle relative
     * to the camera sensor.
     *
     * @param streamRect The rectangle in the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param streamSize The size of the camera stream.
     * @param sensorCrop The crop region of the sensor (typically CameraMetadata.SCALER_CROP_REGION,
     *   or CameraMetadata.SENSOR_INFO_ACTIVE_ARRAY_SIZE if no crop is applied). Sensor coordinates
     *   are relative to the sensor 0,0.
     * @param minimumSensorSize The minimum allowed width and height for the output rectangle in
     *   sensor pixels. If the calculated rectangle is smaller than this value in either dimension,
     *   it will be expanded symmetrically from its center to reach the minimum. Defaults to
     *   [DEFAULT_MIN_SENSOR_PIXELS]. Set to 0 or less to disable this behavior.
     * @param coerceToCropRegion Whether to coerce the output rectangle into the crop region.
     */
    @JvmStatic
    @JvmOverloads
    public fun mapStreamRectToSensorRect(
        streamRect: RectF,
        streamSize: Size,
        sensorCrop: Rect,
        minimumSensorSize: Int = DEFAULT_MIN_SENSOR_PIXELS,
        coerceToCropRegion: Boolean = true,
    ): Rect =
        mapStreamRectToSensorRect(
            streamLeft = streamRect.left,
            streamTop = streamRect.top,
            streamRight = streamRect.right,
            streamBottom = streamRect.bottom,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            sensorCropX = sensorCrop.left,
            sensorCropY = sensorCrop.top,
            sensorCropWidth = sensorCrop.width(),
            sensorCropHeight = sensorCrop.height(),
            minimumSensorSize = minimumSensorSize,
            coerceToCropRegion = coerceToCropRegion,
        ) { sensorLeft, sensorTop, sensorRight, sensorBottom ->
            Rect(sensorLeft, sensorTop, sensorRight, sensorBottom)
        }

    /**
     * Converts a rectangle relative to a specific camera stream to an equivalent rectangle relative
     * to the camera sensor.
     *
     * @param streamLeft The left coordinate of the rectangle in the camera stream. Stream
     *   coordinates are relative to the streamWidth/streamHeight.
     * @param streamTop The top coordinate of the rectangle in the camera stream. Stream coordinates
     *   are relative to the streamWidth/streamHeight.
     * @param streamRight The right coordinate of the rectangle in the camera stream. Stream
     *   coordinates are relative to the streamWidth/streamHeight.
     * @param streamBottom The bottom coordinate of the rectangle in the camera stream. Stream
     *   coordinates are relative to the streamWidth/streamHeight.
     * @param streamWidth The width of the camera stream.
     * @param streamHeight The height of the camera stream.
     * @param sensorCropX The x coordinate of the crop region of the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorCropY The y coordinate of the crop region of the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorCropWidth The width of the crop region of the sensor.
     * @param sensorCropHeight The height of the crop region of the sensor.
     * @param minimumSensorSize The minimum allowed width and height for the output rectangle in
     *   sensor pixels. If the calculated rectangle is smaller than this value in either dimension,
     *   it will be expanded symmetrically from its center to reach the minimum. Defaults to
     *   [DEFAULT_MIN_SENSOR_PIXELS]. Set to 0 or less to disable this behavior.
     * @param coerceToCropRegion Whether to coerce the output rectangle into the crop region.
     * @param toSensorRect A callback to construct the final return object from the computed sensor
     *   coordinates.
     */
    @JvmSynthetic
    public inline fun <T> mapStreamRectToSensorRect(
        streamLeft: Float,
        streamTop: Float,
        streamRight: Float,
        streamBottom: Float,
        streamWidth: Int,
        streamHeight: Int,
        sensorCropX: Int,
        sensorCropY: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        minimumSensorSize: Int = DEFAULT_MIN_SENSOR_PIXELS,
        coerceToCropRegion: Boolean = true,
        crossinline toSensorRect:
            (sensorLeft: Int, sensorTop: Int, sensorRight: Int, sensorBottom: Int) -> T,
    ): T {
        return streamToSensorTransform(
            streamWidth = streamWidth,
            streamHeight = streamHeight,
            sensorCropX = sensorCropX,
            sensorCropY = sensorCropY,
            sensorCropWidth = sensorCropWidth,
            sensorCropHeight = sensorCropHeight,
        ) { scalar, sensorOffsetX, sensorOffsetY ->
            var sensorLeft = (streamLeft * scalar).roundToInt() + sensorOffsetX
            var sensorRight = (streamRight * scalar).roundToInt() + sensorOffsetX
            var sensorTop = (streamTop * scalar).roundToInt() + sensorOffsetY
            var sensorBottom = (streamBottom * scalar).roundToInt() + sensorOffsetY

            if (minimumSensorSize > 0) {
                val width = sensorRight - sensorLeft
                val height = sensorBottom - sensorTop
                if (width < minimumSensorSize) {
                    val delta = minimumSensorSize - width
                    val shiftLeft = delta / 2
                    sensorLeft -= shiftLeft
                    sensorRight += (delta - shiftLeft)
                }

                if (height < minimumSensorSize) {
                    val delta = minimumSensorSize - height
                    val shiftUp = delta / 2
                    sensorTop -= shiftUp
                    sensorBottom += (delta - shiftUp)
                }
            }

            if (coerceToCropRegion) {
                val sensorWidth = sensorRight - sensorLeft
                val sensorHeight = sensorBottom - sensorTop

                var deltaX = 0
                var deltaY = 0

                if (sensorWidth > sensorCropWidth) {
                    val delta = (sensorCropWidth - sensorWidth) / 2
                    deltaX = delta
                } else {
                    if (sensorLeft < sensorCropX) {
                        deltaX = sensorCropX - sensorLeft
                    } else if (sensorRight > sensorCropX + sensorCropWidth) {
                        deltaX = sensorCropX + sensorCropWidth - sensorRight
                    }
                }

                if (sensorHeight > sensorCropHeight) {
                    val delta = (sensorCropHeight - sensorHeight) / 2
                    deltaY = delta
                } else {
                    if (sensorTop < sensorCropY) {
                        deltaY = sensorCropY - sensorTop
                    } else if (sensorBottom > sensorCropY + sensorCropHeight) {
                        deltaY = sensorCropY + sensorCropHeight - sensorBottom
                    }
                }

                sensorLeft += deltaX
                sensorRight += deltaX
                sensorTop += deltaY
                sensorBottom += deltaY
            }

            toSensorRect(sensorLeft, sensorTop, sensorRight, sensorBottom)
        }
    }

    /**
     * Converts a point relative to a specific camera stream to an equivalent rectangle relative to
     * the camera sensor.
     *
     * The output rectangle will have a size determined by the [touchToFocusRatio] and will be
     * centered on the mapped point. If the calculated rectangle is smaller than [minimumSensorSize]
     * in either dimension, it will be expanded symmetrically from its center to reach the minimum.
     *
     * @param streamPoint The point in the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param streamSize The size of the camera stream.
     * @param sensorCrop The crop region of the sensor (typically CameraMetadata.SCALER_CROP_REGION,
     *   or CameraMetadata.SENSOR_INFO_ACTIVE_ARRAY_SIZE if no crop is applied).
     * @param minimumSensorSize The minimum allowed width and height for the output rectangle in
     *   sensor pixels. If the calculated rectangle is smaller than this value in either dimension,
     *   it will be expanded symmetrically from its center to reach the minimum. Defaults to
     *   [DEFAULT_MIN_SENSOR_PIXELS]. Set to 0 or less to disable this behavior.
     * @param coerceToCropRegion Whether to coerce the output rectangle into the crop region.
     * @param touchToFocusRatio The ratio to use when computing a rectangle based on the short
     *   dimension of the stream.
     */
    @JvmStatic
    @JvmOverloads
    public fun mapStreamPointToSensorRect(
        streamPoint: PointF,
        streamSize: Size,
        sensorCrop: Rect,
        minimumSensorSize: Int = DEFAULT_MIN_SENSOR_PIXELS,
        coerceToCropRegion: Boolean = true,
        touchToFocusRatio: Float = DEFAULT_POINT_TO_RECT_RATIO,
    ): Rect =
        mapStreamPointToSensorRect(
            streamX = streamPoint.x,
            streamY = streamPoint.y,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            sensorCropX = sensorCrop.left,
            sensorCropY = sensorCrop.top,
            sensorCropWidth = sensorCrop.width(),
            sensorCropHeight = sensorCrop.height(),
            minimumSensorSize = minimumSensorSize,
            coerceToCropRegion = coerceToCropRegion,
            ratio = touchToFocusRatio,
        ) { sensorLeft, sensorTop, sensorRight, sensorBottom ->
            Rect(sensorLeft, sensorTop, sensorRight, sensorBottom)
        }

    /**
     * Converts a point relative to a specific camera stream to an equivalent rectangle relative to
     * the camera sensor.
     *
     * @param streamX The x coordinate in the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamY The y coordinate in the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamWidth The width of the camera stream.
     * @param streamHeight The height of the camera stream.
     * @param sensorCropX The x coordinate of the crop region of the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorCropY The y coordinate of the crop region of the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorCropWidth The width of the crop region of the sensor.
     * @param sensorCropHeight The height of the crop region of the sensor.
     * @param minimumSensorSize The minimum allowed width and height for the output rectangle in
     *   sensor pixels. If the calculated rectangle is smaller than this value in either dimension,
     *   it will be expanded symmetrically from its center to reach the minimum. Defaults to
     *   [DEFAULT_MIN_SENSOR_PIXELS]. Set to 0 or less to disable this behavior.
     * @param coerceToCropRegion Whether to coerce the output rectangle into the crop region.
     * @param ratio The ratio to use when computing a rectangle based on the short dimension of the
     *   stream.
     * @param toSensorRect A callback to construct the final return object from the computed sensor
     *   coordinates.
     */
    @JvmSynthetic
    public inline fun <T> mapStreamPointToSensorRect(
        streamX: Float,
        streamY: Float,
        streamWidth: Int,
        streamHeight: Int,
        sensorCropX: Int,
        sensorCropY: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        minimumSensorSize: Int = DEFAULT_MIN_SENSOR_PIXELS,
        coerceToCropRegion: Boolean = true,
        ratio: Float = DEFAULT_POINT_TO_RECT_RATIO,
        crossinline toSensorRect:
            (sensorLeft: Int, sensorTop: Int, sensorRight: Int, sensorBottom: Int) -> T,
    ): T {
        val shortEdge = minOf(streamWidth.toFloat(), streamHeight.toFloat())
        val rectSize = shortEdge * ratio
        val halfRectSize = rectSize / 2.0f

        val streamLeft = streamX - halfRectSize
        val streamRight = streamX + (rectSize - halfRectSize)
        val streamTop = streamY - halfRectSize
        val streamBottom = streamY + (rectSize - halfRectSize)

        return mapStreamRectToSensorRect(
            streamLeft = streamLeft,
            streamTop = streamTop,
            streamRight = streamRight,
            streamBottom = streamBottom,
            streamWidth = streamWidth,
            streamHeight = streamHeight,
            sensorCropX = sensorCropX,
            sensorCropY = sensorCropY,
            sensorCropWidth = sensorCropWidth,
            sensorCropHeight = sensorCropHeight,
            minimumSensorSize = minimumSensorSize,
            coerceToCropRegion = coerceToCropRegion,
            toSensorRect = toSensorRect,
        )
    }

    /**
     * Converts a size relative to a specific camera stream to an equivalent size relative to the
     * camera sensor.
     *
     * @param size The size in the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param streamSize The size of the camera stream.
     * @param sensorCropWidth The width of the sensor crop region.
     * @param sensorCropHeight The height of the sensor crop region.
     */
    @JvmStatic
    public fun mapStreamSizeToSensorSize(
        size: Size,
        streamSize: Size,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
    ): Size =
        mapStreamSizeToSensorSize(
            streamSizeWidth = size.width.toFloat(),
            streamSizeHeight = size.height.toFloat(),
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            sensorCropWidth = sensorCropWidth,
            sensorCropHeight = sensorCropHeight,
        ) { sensorWidth, sensorHeight ->
            Size(sensorWidth, sensorHeight)
        }

    /**
     * Converts a size relative to a specific camera stream to an equivalent size relative to the
     * camera sensor.
     *
     * @param streamSizeWidth The width of the size in the camera stream. Stream coordinates are
     *   relative to the streamWidth/streamHeight.
     * @param streamSizeHeight The height of the size in the camera stream. Stream coordinates are
     *   relative to the streamWidth/streamHeight.
     * @param streamWidth The width of the camera stream.
     * @param streamHeight The height of the camera stream.
     * @param sensorCropWidth The width of the sensor crop region.
     * @param sensorCropHeight The height of the sensor crop region.
     * @param toSensorSize A callback to construct the final return object from the computed sensor
     *   coordinates.
     */
    @JvmSynthetic
    public inline fun <T> mapStreamSizeToSensorSize(
        streamSizeWidth: Float,
        streamSizeHeight: Float,
        streamWidth: Int,
        streamHeight: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        crossinline toSensorSize: (sensorWidth: Int, sensorHeight: Int) -> T,
    ): T {
        val scalar = computeScaleToFit(streamWidth, streamHeight, sensorCropWidth, sensorCropHeight)
        return toSensorSize(
            (streamSizeWidth * scalar).roundToInt(),
            (streamSizeHeight * scalar).roundToInt(),
        )
    }

    // --- Mapping from SENSOR to UI ---

    /**
     * Converts a point relative to the camera sensor to an equivalent point relative to a UI
     * element.
     *
     * @param sensorPoint The point on the sensor. Sensor coordinates are relative to the sensor
     *   0,0.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCrop The crop region of the sensor (typically CameraMetadata.SCALER_CROP_REGION,
     *   or CameraMetadata.SENSOR_INFO_ACTIVE_ARRAY_SIZE if no crop is applied).
     */
    @JvmStatic
    public fun mapSensorPointToUiPointF(
        sensorPoint: Point,
        uiSize: Size,
        streamSize: Size,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: Int,
        sensorCrop: Rect,
    ): PointF =
        mapSensorPointToUiPoint(
            sensorX = sensorPoint.x,
            sensorY = sensorPoint.y,
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
            sensorCropX = sensorCrop.left,
            sensorCropY = sensorCrop.top,
            sensorCropWidth = sensorCrop.width(),
            sensorCropHeight = sensorCrop.height(),
        ) { uiX, uiY ->
            PointF(uiX, uiY)
        }

    /**
     * Converts a point relative to the camera sensor to an equivalent point relative to a UI
     * element.
     *
     * @param sensorX The x coordinate on the sensor. Sensor coordinates are relative to the sensor
     *   0,0.
     * @param sensorY The y coordinate on the sensor. Sensor coordinates are relative to the sensor
     *   0,0.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamHeight The height of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCropX The x coordinate of the crop region of the sensor.
     * @param sensorCropY The y coordinate of the crop region of the sensor.
     * @param sensorCropWidth The width of the crop region of the sensor.
     * @param sensorCropHeight The height of the crop region of the sensor.
     * @param toUiPoint A callback to construct the final return object from the computed UI
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapSensorPointToUiPoint(
        sensorX: Int,
        sensorY: Int,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        sensorCropX: Int,
        sensorCropY: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        crossinline toUiPoint: (uiX: Float, uiY: Float) -> T,
    ): T =
        mapSensorPointToStreamPoint(
            sensorX = sensorX,
            sensorY = sensorY,
            streamWidth = streamWidth,
            streamHeight = streamHeight,
            sensorCropX = sensorCropX,
            sensorCropY = sensorCropY,
            sensorCropWidth = sensorCropWidth,
            sensorCropHeight = sensorCropHeight,
        ) { streamX, streamY ->
            mapStreamPointToUiPoint(
                streamX = streamX,
                streamY = streamY,
                uiWidth = uiWidth,
                uiHeight = uiHeight,
                streamWidth = streamWidth,
                streamHeight = streamHeight,
                streamMirroring = streamMirroring,
                sensorRotationToDisplayOrientation = sensorRotationToDisplayOrientation,
                toUiPoint = toUiPoint,
            )
        }

    /**
     * Converts a rectangle relative to the camera sensor to an equivalent rectangle relative to a
     * UI element.
     *
     * @param sensorRect The rectangle on the sensor. Sensor coordinates are relative to the sensor
     *   0,0.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCrop The crop region of the sensor (typically CameraMetadata.SCALER_CROP_REGION,
     *   or CameraMetadata.SENSOR_INFO_ACTIVE_ARRAY_SIZE if no crop is applied).
     */
    @JvmStatic
    public fun mapSensorRectToUiRectF(
        sensorRect: Rect,
        uiSize: Size,
        streamSize: Size,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: Int,
        sensorCrop: Rect,
    ): RectF =
        mapSensorRectToUiRect(
            sensorLeft = sensorRect.left,
            sensorTop = sensorRect.top,
            sensorRight = sensorRect.right,
            sensorBottom = sensorRect.bottom,
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            streamMirroring = streamMirroring,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
            sensorCropX = sensorCrop.left,
            sensorCropY = sensorCrop.top,
            sensorCropWidth = sensorCrop.width(),
            sensorCropHeight = sensorCrop.height(),
        ) { uiLeft, uiTop, uiRight, uiBottom ->
            RectF(uiLeft, uiTop, uiRight, uiBottom)
        }

    /**
     * Converts a rectangle relative to the camera sensor to an equivalent rectangle relative to a
     * UI element.
     *
     * @param sensorLeft The left coordinate of the rectangle on the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorTop The top coordinate of the rectangle on the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorRight The right coordinate of the rectangle on the sensor. Sensor coordinates
     *   are relative to the sensor 0,0.
     * @param sensorBottom The bottom coordinate of the rectangle on the sensor. Sensor coordinates
     *   are relative to the sensor 0,0.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamHeight The height of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamMirroring Whether the camera stream is mirrored.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCropX The x coordinate of the crop region of the sensor.
     * @param sensorCropY The y coordinate of the crop region of the sensor.
     * @param sensorCropWidth The width of the crop region of the sensor.
     * @param sensorCropHeight The height of the crop region of the sensor.
     * @param toUiRect A callback to construct the final return object from the computed UI
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapSensorRectToUiRect(
        sensorLeft: Int,
        sensorTop: Int,
        sensorRight: Int,
        sensorBottom: Int,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        streamMirroring: Boolean,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        sensorCropX: Int,
        sensorCropY: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        crossinline toUiRect: (uiLeft: Float, uiTop: Float, uiRight: Float, uiBottom: Float) -> T,
    ): T =
        mapSensorRectToStreamRect(
            sensorLeft = sensorLeft,
            sensorTop = sensorTop,
            sensorRight = sensorRight,
            sensorBottom = sensorBottom,
            streamWidth = streamWidth,
            streamHeight = streamHeight,
            sensorCropX = sensorCropX,
            sensorCropY = sensorCropY,
            sensorCropWidth = sensorCropWidth,
            sensorCropHeight = sensorCropHeight,
        ) { streamLeft, streamTop, streamRight, streamBottom ->
            mapStreamRectToUiRect(
                streamLeft = streamLeft,
                streamTop = streamTop,
                streamRight = streamRight,
                streamBottom = streamBottom,
                uiWidth = uiWidth,
                uiHeight = uiHeight,
                streamWidth = streamWidth,
                streamHeight = streamHeight,
                streamMirroring = streamMirroring,
                sensorRotationToDisplayOrientation = sensorRotationToDisplayOrientation,
                toUiRect = toUiRect,
            )
        }

    /**
     * Converts a size relative to the camera sensor to an equivalent size relative to a UI element.
     *
     * @param size The size on the sensor. Sensor coordinates are relative to the sensor 0,0.
     * @param uiSize The size of the UI (specifically the UI-relative size of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCropWidth The width of the sensor crop region.
     * @param sensorCropHeight The height of the sensor crop region.
     */
    @JvmStatic
    @JvmName("mapSensorSizeToUiSizeF")
    public fun mapSensorSizeToUiSizeF(
        size: Size,
        uiSize: Size,
        streamSize: Size,
        sensorRotationToDisplayOrientation: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
    ): SizeF =
        mapSensorSizeToUiSize(
            sensorSizeWidth = size.width,
            sensorSizeHeight = size.height,
            uiWidth = uiSize.width,
            uiHeight = uiSize.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            sensorRotationToDisplayOrientation =
                DiscreteRotation.from(sensorRotationToDisplayOrientation),
            sensorCropWidth = sensorCropWidth,
            sensorCropHeight = sensorCropHeight,
        ) { uiSizeWidth, uiSizeHeight ->
            SizeF(uiSizeWidth, uiSizeHeight)
        }

    /**
     * Converts a size relative to the camera sensor to an equivalent size relative to a UI element.
     *
     * @param sensorSizeWidth The width of the size on the sensor. Sensor coordinates are relative
     *   to the sensor 0,0.
     * @param sensorSizeHeight The height of the size on the sensor. Sensor coordinates are relative
     *   to the sensor 0,0.
     * @param uiWidth The width of the UI (specifically the UI-relative width of the viewfinder,
     *   without accounting for clipping or padding).
     * @param uiHeight The height of the UI (specifically the UI-relative height of the viewfinder,
     *   without accounting for clipping or padding).
     * @param streamWidth The width of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamHeight The height of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param sensorRotationToDisplayOrientation The orientation from sensor to display.
     * @param sensorCropWidth The width of the sensor crop region.
     * @param sensorCropHeight The height of the sensor crop region.
     * @param toUiSize A callback to construct the final return object from the computed UI
     *   coordinates.
     */
    @Suppress("ValueClassUsageWithoutJvmName")
    @JvmSynthetic
    public inline fun <T> mapSensorSizeToUiSize(
        sensorSizeWidth: Int,
        sensorSizeHeight: Int,
        uiWidth: Int,
        uiHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        sensorRotationToDisplayOrientation: DiscreteRotation,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        crossinline toUiSize: (uiSizeWidth: Float, uiSizeHeight: Float) -> T,
    ): T =
        mapSensorSizeToStreamSize(
            sensorSizeWidth = sensorSizeWidth,
            sensorSizeHeight = sensorSizeHeight,
            streamWidth = streamWidth,
            streamHeight = streamHeight,
            sensorCropWidth = sensorCropWidth,
            sensorCropHeight = sensorCropHeight,
        ) { streamSizeWidth, streamSizeHeight ->
            mapStreamSizeToUiSize(
                streamSizeWidth = streamSizeWidth,
                streamSizeHeight = streamSizeHeight,
                uiWidth = uiWidth,
                uiHeight = uiHeight,
                streamWidth = streamWidth,
                streamHeight = streamHeight,
                sensorRotationToDisplayOrientation = sensorRotationToDisplayOrientation,
                toUiSize = toUiSize,
            )
        }

    // --- Mapping from SENSOR to STREAM ---

    /**
     * Converts a point relative to the camera sensor to an equivalent point relative to a specific
     * camera stream.
     *
     * @param sensorPoint The point on the sensor. Sensor coordinates are relative to the sensor
     *   0,0.
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param sensorCrop The crop region of the sensor (typically CameraMetadata.SCALER_CROP_REGION,
     *   or CameraMetadata.SENSOR_INFO_ACTIVE_ARRAY_SIZE if no crop is applied).
     */
    @JvmStatic
    public fun mapSensorPointToStreamPointF(
        sensorPoint: Point,
        streamSize: Size,
        sensorCrop: Rect,
    ): PointF =
        mapSensorPointToStreamPoint(
            sensorX = sensorPoint.x,
            sensorY = sensorPoint.y,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            sensorCropX = sensorCrop.left,
            sensorCropY = sensorCrop.top,
            sensorCropWidth = sensorCrop.width(),
            sensorCropHeight = sensorCrop.height(),
        ) { streamX, streamY ->
            PointF(streamX, streamY)
        }

    /**
     * Converts a point relative to the camera sensor to an equivalent point relative to a specific
     * camera stream.
     *
     * @param sensorX The x coordinate on the sensor. Sensor coordinates are relative to the sensor
     *   0,0.
     * @param sensorY The y coordinate on the sensor. Sensor coordinates are relative to the sensor
     *   0,0.
     * @param streamWidth The width of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamHeight The height of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param sensorCropX The x coordinate of the crop region of the sensor.
     * @param sensorCropY The y coordinate of the crop region of the sensor.
     * @param sensorCropWidth The width of the crop region of the sensor.
     * @param sensorCropHeight The height of the crop region of the sensor.
     * @param toStreamPoint A callback to construct the final return object from the computed stream
     *   coordinates.
     */
    @JvmSynthetic
    public inline fun <T> mapSensorPointToStreamPoint(
        sensorX: Int,
        sensorY: Int,
        streamWidth: Int,
        streamHeight: Int,
        sensorCropX: Int,
        sensorCropY: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        crossinline toStreamPoint: (streamX: Float, streamY: Float) -> T,
    ): T {
        return streamToSensorTransform(
            streamWidth = streamWidth,
            streamHeight = streamHeight,
            sensorCropX = sensorCropX,
            sensorCropY = sensorCropY,
            sensorCropWidth = sensorCropWidth,
            sensorCropHeight = sensorCropHeight,
        ) { scalar, sensorOffsetX, sensorOffsetY ->
            toStreamPoint(
                (sensorX.toFloat() - sensorOffsetX) / scalar,
                (sensorY.toFloat() - sensorOffsetY) / scalar,
            )
        }
    }

    /**
     * Converts a rectangle relative to the camera sensor to an equivalent rectangle relative to a
     * specific camera stream.
     *
     * @param sensorRect The rectangle on the sensor. Sensor coordinates are relative to the sensor
     *   0,0.
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param sensorCrop The crop region of the sensor (typically CameraMetadata.SCALER_CROP_REGION,
     *   or CameraMetadata.SENSOR_INFO_ACTIVE_ARRAY_SIZE if no crop is applied).
     */
    @JvmStatic
    public fun mapSensorRectToStreamRectF(
        sensorRect: Rect,
        streamSize: Size,
        sensorCrop: Rect,
    ): RectF =
        mapSensorRectToStreamRect(
            sensorLeft = sensorRect.left,
            sensorTop = sensorRect.top,
            sensorRight = sensorRect.right,
            sensorBottom = sensorRect.bottom,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            sensorCropX = sensorCrop.left,
            sensorCropY = sensorCrop.top,
            sensorCropWidth = sensorCrop.width(),
            sensorCropHeight = sensorCrop.height(),
        ) { streamLeft, streamTop, streamRight, streamBottom ->
            RectF(streamLeft, streamTop, streamRight, streamBottom)
        }

    /**
     * Converts a rectangle relative to the camera sensor to an equivalent rectangle relative to a
     * specific camera stream.
     *
     * @param sensorLeft The left coordinate of the rectangle on the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorTop The top coordinate of the rectangle on the sensor. Sensor coordinates are
     *   relative to the sensor 0,0.
     * @param sensorRight The right coordinate of the rectangle on the sensor. Sensor coordinates
     *   are relative to the sensor 0,0.
     * @param sensorBottom The bottom coordinate of the rectangle on the sensor. Sensor coordinates
     *   are relative to the sensor 0,0.
     * @param streamWidth The width of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamHeight The height of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param sensorCropX The x coordinate of the crop region of the sensor.
     * @param sensorCropY The y coordinate of the crop region of the sensor.
     * @param sensorCropWidth The width of the crop region of the sensor.
     * @param sensorCropHeight The height of the crop region of the sensor.
     * @param toStreamRect A callback to construct the final return object from the computed stream
     *   coordinates.
     */
    @JvmSynthetic
    public inline fun <T> mapSensorRectToStreamRect(
        sensorLeft: Int,
        sensorTop: Int,
        sensorRight: Int,
        sensorBottom: Int,
        streamWidth: Int,
        streamHeight: Int,
        sensorCropX: Int,
        sensorCropY: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        crossinline toStreamRect:
            (streamLeft: Float, streamTop: Float, streamRight: Float, streamBottom: Float) -> T,
    ): T {
        return streamToSensorTransform(
            streamWidth = streamWidth,
            streamHeight = streamHeight,
            sensorCropX = sensorCropX,
            sensorCropY = sensorCropY,
            sensorCropWidth = sensorCropWidth,
            sensorCropHeight = sensorCropHeight,
        ) { scalar, sensorOffsetX, sensorOffsetY ->
            toStreamRect(
                (sensorLeft.toFloat() - sensorOffsetX) / scalar,
                (sensorTop.toFloat() - sensorOffsetY) / scalar,
                (sensorRight.toFloat() - sensorOffsetX) / scalar,
                (sensorBottom.toFloat() - sensorOffsetY) / scalar,
            )
        }
    }

    /**
     * Converts a size relative to the camera sensor to an equivalent size relative to a specific
     * camera stream.
     *
     * @param size The size on the sensor. Sensor coordinates are relative to the sensor 0,0.
     * @param streamSize The size of the camera stream. Stream coordinates are relative to the
     *   streamWidth/Height.
     * @param sensorCropWidth The width of the sensor crop region.
     * @param sensorCropHeight The height of the sensor crop region.
     */
    @JvmStatic
    @JvmName("mapSensorSizeToStreamSizeF")
    public fun mapSensorSizeToStreamSizeF(
        size: Size,
        streamSize: Size,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
    ): SizeF =
        mapSensorSizeToStreamSize(
            sensorSizeWidth = size.width,
            sensorSizeHeight = size.height,
            streamWidth = streamSize.width,
            streamHeight = streamSize.height,
            sensorCropWidth = sensorCropWidth,
            sensorCropHeight = sensorCropHeight,
        ) { streamSizeWidth, streamSizeHeight ->
            SizeF(streamSizeWidth, streamSizeHeight)
        }

    /**
     * Converts a size relative to the camera sensor to an equivalent size relative to a specific
     * camera stream.
     *
     * @param sensorSizeWidth The width of the size on the sensor. Sensor coordinates are relative
     *   to the sensor 0,0.
     * @param sensorSizeHeight The height of the size on the sensor. Sensor coordinates are relative
     *   to the sensor 0,0.
     * @param streamWidth The width of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param streamHeight The height of the camera stream. Stream coordinates are relative to the
     *   streamWidth/streamHeight.
     * @param sensorCropWidth The width of the sensor crop region.
     * @param sensorCropHeight The height of the sensor crop region.
     * @param toStreamSize A callback to construct the final return object from the computed stream
     *   coordinates.
     */
    @JvmSynthetic
    public inline fun <T> mapSensorSizeToStreamSize(
        sensorSizeWidth: Int,
        sensorSizeHeight: Int,
        streamWidth: Int,
        streamHeight: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        crossinline toStreamSize: (streamWidth: Float, streamHeight: Float) -> T,
    ): T {
        val scalar = computeScaleToFit(streamWidth, streamHeight, sensorCropWidth, sensorCropHeight)
        return toStreamSize(sensorSizeWidth.toFloat() / scalar, sensorSizeHeight.toFloat() / scalar)
    }

    // =============================================================================================
    // 5. Coordinate Space Transform Helpers
    // =============================================================================================

    @PublishedApi
    internal inline fun <T> streamToSensorTransform(
        streamWidth: Int,
        streamHeight: Int,
        sensorCropX: Int,
        sensorCropY: Int,
        sensorCropWidth: Int,
        sensorCropHeight: Int,
        crossinline transform:
            (streamToSensorScalar: Float, sensorOffsetX: Int, sensorOffsetY: Int) -> T,
    ): T =
        computeScaleAndOffsetWithinBounds(
            width = streamWidth,
            height = streamHeight,
            boundsX = sensorCropX,
            boundsY = sensorCropY,
            boundsWidth = sensorCropWidth,
            boundsHeight = sensorCropHeight,
        ) { scalar, offsetX, offsetY ->
            transform(scalar, offsetX, offsetY)
        }

    @PublishedApi
    internal inline fun <T> computeScaleAndOffsetWithinBounds(
        width: Int,
        height: Int,
        boundsX: Int,
        boundsY: Int,
        boundsWidth: Int,
        boundsHeight: Int,
        crossinline transform: (scalar: Float, offsetX: Int, offsetY: Int) -> T,
    ): T {
        val scaleToFit = computeScaleToFit(width, height, boundsWidth, boundsHeight)

        val left = ((boundsWidth.toFloat() - width.toFloat() * scaleToFit) / 2.0f).toInt()
        val top = ((boundsHeight.toFloat() - height.toFloat() * scaleToFit) / 2.0f).toInt()

        return transform(scaleToFit, boundsX + left, boundsY + top)
    }

    @PublishedApi
    internal inline fun computeScaleToFit(
        width: Int,
        height: Int,
        widthToFit: Int,
        heightToFit: Int,
    ): Float {
        val widthScalar: Float = widthToFit.toFloat() / width.toFloat()
        val heightScalar: Float = heightToFit.toFloat() / height.toFloat()

        return minOf(widthScalar, heightScalar)
    }
}
