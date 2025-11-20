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

package androidx.camera.camera2.pipe.integration.internal

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import android.util.SizeF
import androidx.annotation.IntRange
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.utils.TransformUtils
import androidx.core.util.Preconditions
import dagger.Binds
import dagger.Module
import javax.inject.Inject
import kotlin.math.atan

public interface IntrinsicZoomCalculator {
    /**
     * Calculates the intrinsic zoom ratio of a camera.
     *
     * The intrinsic zoom ratio is the zoom ratio of the current camera with respect to the default
     * camera on the device. The default camera is the one selected by
     * [androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA] or
     * [androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA].
     *
     * This ratio can be used to know the zoom level of a camera relative to the main camera. For
     * example, an ultra-wide camera will have an intrinsic zoom ratio < 1.0, and a telephoto camera
     * will have an intrinsic zoom ratio > 1.0.
     *
     * @param cameraMetadata The [CameraMetadata] for which to calculate the intrinsic zoom ratio.
     * @return The intrinsic zoom ratio, or `null` if it cannot be calculated.
     */
    public fun calculateIntrinsicZoomRatio(cameraMetadata: CameraMetadata): Float?

    @Module
    public abstract class Bindings {
        @Binds
        public abstract fun bindIntrinsicZoomCalculatorImpl(
            impl: IntrinsicZoomCalculatorImpl
        ): IntrinsicZoomCalculator
    }

    public companion object {
        /**
         * A no-op [IntrinsicZoomCalculator] that does no calculation and just returns
         * [CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN] always.
         */
        public val NO_OP_INTRINSIC_ZOOM_CALCULATOR: IntrinsicZoomCalculator =
            object : IntrinsicZoomCalculator {
                override fun calculateIntrinsicZoomRatio(cameraMetadata: CameraMetadata): Float =
                    CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN
            }
    }
}

@CameraScope
public class IntrinsicZoomCalculatorImpl
@Inject
constructor(private val cameraDevices: CameraDevices) : IntrinsicZoomCalculator {
    override fun calculateIntrinsicZoomRatio(cameraMetadata: CameraMetadata): Float? {
        return try {
            cameraMetadata.getDefaultCameraDefaultViewAngleDegrees().toFloat() /
                cameraMetadata.getDefaultViewAngleDegrees().toFloat()
        } catch (e: Exception) {
            Log.error(e) { "Failed to get the intrinsic zoom ratio" }
            null
        }
    }

    /**
     * Gets the default focal length of this [CameraMetadata].
     *
     * If the camera is a logical camera that consists of multiple physical cameras, the default
     * focal length is the focal length of the physical camera that produces image at zoom ratio
     * `1.0`.
     *
     * @throws NullPointerException If any of the required [CameraCharacteristics] is not available.
     * @throws IllegalStateException If [CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS] is
     *   empty.
     */
    private fun CameraMetadata.getDefaultFocalLength(): Float {
        val focalLengths: FloatArray =
            Preconditions.checkNotNull(
                this[CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS],
                "The focal lengths can not be empty.",
            )

        Preconditions.checkState(focalLengths.isNotEmpty(), "The focal lengths can not be empty.")

        // Assume the first focal length is the default focal length. This will not be true if the
        // camera is a logical camera consist of multiple physical cameras and reports multiple
        // focal lengths. However for this kind of cameras, it's suggested to use zoom ratio to
        // do optical zoom.
        return focalLengths[0]
    }

    /**
     * Gets the length of the horizontal side of the sensor for this [CameraMetadata].
     *
     * The horizontal side is the width of the sensor size after rotated by the sensor orientation.
     *
     * @throws NullPointerException If any of the required [CameraCharacteristics] is not available.
     */
    private fun CameraMetadata.getSensorHorizontalLength(): Float {
        var sensorSize: SizeF =
            Preconditions.checkNotNull(
                this[CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE],
                "The sensor size can't be null.",
            )

        val activeArrayRect: Rect =
            Preconditions.checkNotNull(
                this[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE],
                "The sensor orientation can't be null.",
            )

        var pixelArraySize: Size =
            Preconditions.checkNotNull(
                this[CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE],
                "The active array size can't be null.",
            )

        val sensorOrientation: Int =
            Preconditions.checkNotNull(
                this[CameraCharacteristics.SENSOR_ORIENTATION],
                "The pixel array size can't be null.",
            )

        var activeArraySize = TransformUtils.rectToSize(activeArrayRect)
        if (TransformUtils.is90or270(sensorOrientation)) {
            sensorSize = TransformUtils.reverseSizeF(sensorSize)
            activeArraySize = TransformUtils.reverseSize(activeArraySize)
            pixelArraySize = TransformUtils.reverseSize(pixelArraySize)
        }
        return sensorSize.width * activeArraySize.width / pixelArraySize.width
    }

    /**
     * Calculates view angle by focal length and sensor length.
     *
     * The returned view angle is inexact and might not be hundred percent accurate comparing to the
     * output image.
     *
     * The returned view angle should between 0 and 360.
     *
     * @throws IllegalArgumentException If the provided focal length or sensor length is not
     *   positive, or results in an invalid view angle.
     */
    @IntRange(from = 0, to = 360)
    private fun focalLengthToViewAngleDegrees(focalLength: Float, sensorLength: Float): Int {
        Preconditions.checkArgument(focalLength > 0, "Focal length should be positive.")
        Preconditions.checkArgument(sensorLength > 0, "Sensor length should be positive.")
        val viewAngleDegrees =
            Math.toDegrees(2 * atan((sensorLength / (2 * focalLength)).toDouble())).toInt()
        Preconditions.checkArgumentInRange(
            viewAngleDegrees,
            0,
            360,
            "The provided focal length and sensor length result in an invalid view" +
                " angle degrees.",
        )
        return viewAngleDegrees
    }

    /**
     * Gets the view angle of this [CameraMetadata].
     *
     * @throws IllegalStateException If a valid view angle could not be found.
     */
    @Throws(IllegalStateException::class)
    private fun CameraMetadata.getDefaultViewAngleDegrees(): Int {
        try {
            return focalLengthToViewAngleDegrees(
                getDefaultFocalLength(),
                getSensorHorizontalLength(),
            )
        } catch (e: Exception) {
            throw IllegalStateException("Failed to get a valid view angle", e)
        }
    }

    /**
     * Gets the angle of view for the default camera that shares the same lens facing as this
     * [CameraMetadata].
     *
     * The "default camera" is assumed to be the first camera returned by the system for a given
     * lens facing (e.g., [CameraCharacteristics.LENS_FACING_BACK] or
     * [CameraCharacteristics.LENS_FACING_FRONT]). This function uses the lens facing from the
     * receiver `CameraMetadata` to find the corresponding default camera and then calculates its
     * angle of view.
     *
     * @return The angle of view in degrees.
     * @receiver The [CameraMetadata] of the camera for which a corresponding default camera needs
     *   to be found.
     * @throws IllegalStateException If a default camera with the matching lens facing cannot be
     *   found or if a valid view angle cannot be calculated.
     */
    @Throws(IllegalStateException::class)
    private fun CameraMetadata.getDefaultCameraDefaultViewAngleDegrees(): Int {
        try {
            val cameraIds =
                Preconditions.checkNotNull(
                    cameraDevices.awaitCameraIds(),
                    "Failed to get available camera IDs",
                )

            cameraIds.forEach { cameraId ->
                val cameraMetadata =
                    Preconditions.checkNotNull(
                        cameraDevices.awaitCameraMetadata(cameraId),
                        "Failed to get CameraMetadata for $cameraId",
                    )
                val cameraLensFacing =
                    Preconditions.checkNotNull(
                        cameraMetadata[CameraCharacteristics.LENS_FACING],
                        "Failed to get CameraCharacteristics.LENS_FACING for $cameraId",
                    )
                val targetLensFacing =
                    Preconditions.checkNotNull(
                        this[CameraCharacteristics.LENS_FACING],
                        "Failed to get the required LENS_FACING for $camera",
                    )
                if (cameraLensFacing == targetLensFacing) {
                    return focalLengthToViewAngleDegrees(
                        cameraMetadata.getDefaultFocalLength(),
                        cameraMetadata.getSensorHorizontalLength(),
                    )
                }
            }

            throw IllegalStateException("Could not find the default camera for $camera")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to get a valid view angle", e)
        }
    }
}
