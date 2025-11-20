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
package androidx.camera.viewfinder.core.camera2

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.camera.viewfinder.core.TransformationInfo

private const val TAG = "C2TransformationInfo"

/**
 * Utilities for generating [TransformationInfo] for use with
 * [Camera2]({@docRoot]media/camera/camera2)
 */
object Camera2TransformationInfo {

    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
    @IntDef(
        value =
            [
                OutputConfiguration.MIRROR_MODE_AUTO,
                OutputConfiguration.MIRROR_MODE_NONE,
                OutputConfiguration.MIRROR_MODE_H,
                OutputConfiguration.MIRROR_MODE_V,
            ]
    )
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class OutputConfigurationMirrorMode

    /**
     * Creates [TransformationInfo] from the provided [CameraCharacteristics] and crop rect.
     *
     * When using a viewfinder with [Camera2]({@docRoot]media/camera/camera2), this will generate
     * the appropriate [TransformationInfo] to display the camera frames upright.
     *
     * The crop rect will specify a region of interest (ROI) within the original buffer to treat as
     * the buffer dimensions. When displaying the ROI in a viewfinder, depending on the scale mode,
     * the crop rect bounding box should line up with the edges of the viewfinder, but pixels
     * outside the bounding box will not necessarily be hidden if they still land within the bounds
     * of the viewfinder.
     *
     * @param cameraCharacteristics the camera characteristics of the camera being used to produce
     *   frames
     * @param cropRectLeft the optional left bound of a crop rect
     * @param cropRectTop the optional top bound of a crop rect
     * @param cropRectRight the optional right bound of a crop rect
     * @param cropRectBottom the optional bottom bound of a crop rect
     */
    @JvmStatic
    @JvmOverloads
    fun createFromCharacteristics(
        cameraCharacteristics: CameraCharacteristics,
        cropRectLeft: Float = TransformationInfo.CROP_NONE,
        cropRectTop: Float = TransformationInfo.CROP_NONE,
        cropRectRight: Float = TransformationInfo.CROP_NONE,
        cropRectBottom: Float = TransformationInfo.CROP_NONE,
    ): TransformationInfo =
        createFromCharacteristicsInternal(
            cameraCharacteristics = cameraCharacteristics,
            cropRectLeft = cropRectLeft,
            cropRectTop = cropRectTop,
            cropRectRight = cropRectRight,
            cropRectBottom = cropRectBottom,
        )

    /**
     * Creates [TransformationInfo] from the provided [CameraCharacteristics], crop rect, and mirror
     * mode.
     *
     * When using a viewfinder with [Camera2]({@docRoot]media/camera/camera2), this will generate
     * the appropriate [TransformationInfo] to display the camera frames upright.
     *
     * The crop rect will specify a region of interest (ROI) within the original buffer to treat as
     * the buffer dimensions. When displaying the ROI in a viewfinder, depending on the scale mode,
     * the crop rect bounding box should line up with the edges of the viewfinder, but pixels
     * outside the bounding box will not necessarily be hidden if they still land within the bounds
     * of the viewfinder.
     *
     * The mirror mode is one of [OutputConfiguration.MIRROR_MODE_AUTO],
     * [OutputConfiguration.MIRROR_MODE_NONE], [OutputConfiguration.MIRROR_MODE_H], or
     * [OutputConfiguration.MIRROR_MODE_NONE], and should match what was set on
     * [OutputConfiguration.setMirrorMode]. Note that before API level 33, the default behavior was
     * equivalent to [OutputConfiguration.MIRROR_MODE_AUTO].
     *
     * @param cameraCharacteristics the camera characteristics of the camera being used to produce
     *   frames
     * @param mirrorMode the mirror mode set on [OutputConfiguration.setMirrorMode]
     * @param cropRectLeft the optional left bound of a crop rect
     * @param cropRectTop the optional top bound of a crop rect
     * @param cropRectRight the optional right bound of a crop rect
     * @param cropRectBottom the optional bottom bound of a crop rect
     */
    @JvmStatic
    @JvmOverloads
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createFromCharacteristics(
        cameraCharacteristics: CameraCharacteristics,
        @OutputConfigurationMirrorMode mirrorMode: Int,
        cropRectLeft: Float = TransformationInfo.CROP_NONE,
        cropRectTop: Float = TransformationInfo.CROP_NONE,
        cropRectRight: Float = TransformationInfo.CROP_NONE,
        cropRectBottom: Float = TransformationInfo.CROP_NONE,
    ): TransformationInfo =
        createFromCharacteristicsInternal(
            cameraCharacteristics = cameraCharacteristics,
            cropRectLeft = cropRectLeft,
            cropRectTop = cropRectTop,
            cropRectRight = cropRectRight,
            cropRectBottom = cropRectBottom,
            mirrorMode = mirrorMode,
        )

    @JvmStatic
    private fun createFromCharacteristicsInternal(
        cameraCharacteristics: CameraCharacteristics,
        @OutputConfigurationMirrorMode mirrorMode: Int = OutputConfiguration.MIRROR_MODE_AUTO,
        cropRectLeft: Float = TransformationInfo.CROP_NONE,
        cropRectTop: Float = TransformationInfo.CROP_NONE,
        cropRectRight: Float = TransformationInfo.CROP_NONE,
        cropRectBottom: Float = TransformationInfo.CROP_NONE,
    ): TransformationInfo {
        var mirrorHorz = false
        var mirrorVert = false

        val sensorRotation =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                ?: run {
                    Log.e(TAG, "Unable to retrieve sensor rotation. Assuming rotation of 0")
                    0
                }

        when (mirrorMode) {
            OutputConfiguration.MIRROR_MODE_AUTO -> {
                val lensFacing =
                    cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                        ?: run {
                            Log.e(TAG, "Unable to retrieve lens facing. Assuming BACK camera.")
                            CameraCharacteristics.LENS_FACING_BACK
                        }
                if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    when (sensorRotation) {
                        90,
                        270 -> mirrorVert = true
                        else -> mirrorHorz = true
                    }
                }
            }
            OutputConfiguration.MIRROR_MODE_H -> mirrorHorz = true
            OutputConfiguration.MIRROR_MODE_V -> mirrorVert = true
        }

        return TransformationInfo(
            sourceRotation = sensorRotation,
            isSourceMirroredHorizontally = mirrorHorz,
            isSourceMirroredVertically = mirrorVert,
            cropRectLeft = cropRectLeft,
            cropRectTop = cropRectTop,
            cropRectRight = cropRectRight,
            cropRectBottom = cropRectBottom,
        )
    }
}
