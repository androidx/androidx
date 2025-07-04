/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.integration.extensions

import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.MeteringRectangle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import androidx.core.math.MathUtils.clamp

private const val TAG = "TapToFocusDetector"
private const val METERING_RECTANGLE_SIZE = 0.15f

/**
 * A class helps to detect the tap-to-focus event and also normalize the point to mapping to the
 * camera sensor coordinate.
 */
class TapToFocusDetector(
    context: Context,
    private val textureView: TextureView,
    private val cameraInfo: CameraInfo,
    private val displayRotation: Int,
    private val tapToFocusImpl: (Array<MeteringRectangle?>) -> Unit,
) {
    private val mTapToFocusListener: GestureDetector.SimpleOnGestureListener =
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
                return tapToFocus(motionEvent)
            }
        }

    private val tapToFocusGestureDetector = GestureDetector(context, mTapToFocusListener)

    fun onTouchEvent(event: MotionEvent) {
        tapToFocusGestureDetector.onTouchEvent(event)
    }

    private fun tapToFocus(motionEvent: MotionEvent): Boolean {
        val normalizedPoint = calculateCameraSensorMappingPoint(motionEvent)
        val meteringRectangle = calculateMeteringRectangle(normalizedPoint)
        tapToFocusImpl.invoke(arrayOf(meteringRectangle))
        return true
    }

    /**
     * Calculates the point which will be mapped to a point in the camera sensor coordinate
     * dimension.
     */
    private fun calculateCameraSensorMappingPoint(motionEvent: MotionEvent): FloatArray {
        // Gets the dimension info to calculate the normalized point info in the camera sensor
        // coordinate dimension first.
        val activeArraySize = cameraInfo.activeArraySize
        val relativeRotationDegrees = calculateRelativeRotationDegrees()
        val dimension =
            if (relativeRotationDegrees % 180 == 0) {
                activeArraySize
            } else {
                Rect(0, 0, activeArraySize.height(), activeArraySize.width())
            }

        // Calculates what should the full dimension be because the preview might be cropped from
        // the full FOV of camera sensor.
        val scaledFullDimension =
            if (
                dimension.width() / dimension.height().toFloat() >
                    textureView.width / textureView.height.toFloat()
            ) {
                Rect(
                    0,
                    0,
                    dimension.width() * textureView.height / dimension.height(),
                    textureView.height,
                )
            } else {
                Rect(
                    0,
                    0,
                    textureView.width,
                    dimension.height() * textureView.width / dimension.width(),
                )
            }

        // Calculates the shift values for calibration.
        val shiftX = (scaledFullDimension.width() - textureView.width) / 2
        val shiftY = (scaledFullDimension.height() - textureView.height) / 2

        // Calculates the normalized point which will be the point between [0, 0] to [1, 1].
        val normalizedPoint =
            floatArrayOf(
                (motionEvent.x + shiftX) / scaledFullDimension.width(),
                (motionEvent.y + shiftY) / scaledFullDimension.height(),
            )

        // Transforms the normalizedPoint to the camera sensor coordinate.
        val matrix = Matrix()
        // Rotates the normalized point to the camera sensor orientation
        matrix.postRotate(-relativeRotationDegrees.toFloat(), 0.5f, 0.5f)
        // Flips if current working camera is front camera
        if (cameraInfo.lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            matrix.postScale(1.0f, -1.0f, 0.5f, 0.5f)
        }
        // Scales the point to the camera sensor coordinate dimension.
        matrix.postScale(activeArraySize.width().toFloat(), activeArraySize.height().toFloat())
        matrix.mapPoints(normalizedPoint)

        Log.e(TAG, "Tap-to-focus point: ${normalizedPoint.toList()}")

        return normalizedPoint
    }

    private fun calculateRelativeRotationDegrees(): Int {
        val rotationDegrees =
            when (displayRotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else ->
                    throw IllegalArgumentException("Unsupported surface rotation: $displayRotation")
            }
        return if (cameraInfo.lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
            (cameraInfo.sensorOrientation.toInt() - rotationDegrees + 360) % 360
        } else {
            (cameraInfo.sensorOrientation.toInt() + rotationDegrees) % 360
        }
    }

    /**
     * Calculates the metering rectangle according to the camera sensor coordinate dimension mapping
     * point.
     */
    private fun calculateMeteringRectangle(point: FloatArray): MeteringRectangle {
        val activeArraySize = cameraInfo.activeArraySize
        val halfMeteringRectWidth: Float = (METERING_RECTANGLE_SIZE * activeArraySize.width()) / 2
        val halfMeteringRectHeight: Float = (METERING_RECTANGLE_SIZE * activeArraySize.height()) / 2

        val meteringRegion =
            Rect(
                clamp((point[0] - halfMeteringRectWidth).toInt(), 0, activeArraySize.width()),
                clamp((point[1] - halfMeteringRectHeight).toInt(), 0, activeArraySize.height()),
                clamp((point[0] + halfMeteringRectWidth).toInt(), 0, activeArraySize.width()),
                clamp((point[1] + halfMeteringRectHeight).toInt(), 0, activeArraySize.height()),
            )

        return MeteringRectangle(meteringRegion, MeteringRectangle.METERING_WEIGHT_MAX)
    }

    data class CameraInfo(
        val lensFacing: Int,
        val sensorOrientation: Float,
        val activeArraySize: Rect,
    )
}
