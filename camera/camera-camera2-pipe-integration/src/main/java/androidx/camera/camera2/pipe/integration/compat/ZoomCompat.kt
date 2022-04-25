/*
 * Copyright 2020 The Android Open Source Project
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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.compat

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Range
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import dagger.Module
import dagger.Provides

interface ZoomCompat {
    val minZoom: Float
    val maxZoom: Float

    fun apply(
        zoomRatio: Float,
        camera: UseCaseCamera
    )

    @Module
    abstract class Bindings {
        companion object {
            @Provides
            fun provideZoomRatio(cameraProperties: CameraProperties): ZoomCompat {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val range =
                        cameraProperties.metadata[CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE]
                    if (range != null) {
                        AndroidRZoomCompat(range)
                    } else {
                        CropRegionZoomCompat(cameraProperties)
                    }
                } else {
                    CropRegionZoomCompat(cameraProperties)
                }
            }
        }
    }
}

class CropRegionZoomCompat(private val cameraProperties: CameraProperties) : ZoomCompat {
    override val minZoom: Float
        get() = 1.0f
    override val maxZoom: Float
        get() = cameraProperties.metadata.getOrDefault(
            CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM, minZoom
        )

    override fun apply(
        zoomRatio: Float,
        camera: UseCaseCamera
    ) {
        val sensorRect =
            cameraProperties.metadata[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!
        val cropRect = computeCropRect(sensorRect, zoomRatio)
        camera.setParameterAsync(CaptureRequest.SCALER_CROP_REGION, cropRect)
    }

    private fun computeCropRect(sensorRect: Rect, zoomRatio: Float): Rect {
        val cropWidth: Float = sensorRect.width() / zoomRatio
        val cropHeight: Float = sensorRect.height() / zoomRatio
        val left: Float = (sensorRect.width() - cropWidth) / 2.0f
        val top: Float = (sensorRect.height() - cropHeight) / 2.0f
        return Rect(
            left.toInt(),
            top.toInt(),
            (left + cropWidth).toInt(),
            (top + cropHeight).toInt()
        )
    }
}

@RequiresApi(Build.VERSION_CODES.R)
class AndroidRZoomCompat(private val range: Range<Float>) : ZoomCompat {
    override val minZoom: Float
        get() = range.lower
    override val maxZoom: Float
        get() = range.upper

    override fun apply(
        zoomRatio: Float,
        camera: UseCaseCamera
    ) {
        require(zoomRatio in minZoom..maxZoom)
        camera.setParameterAsync(CaptureRequest.CONTROL_ZOOM_RATIO, zoomRatio)
    }
}