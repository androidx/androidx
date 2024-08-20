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

package androidx.camera.camera2.pipe.integration.compat

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Range
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.compat.workaround.getControlZoomRatioRangeSafely
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import androidx.camera.camera2.pipe.integration.internal.ZoomMath.nearZero
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Deferred

public interface ZoomCompat {
    public val minZoomRatio: Float
    public val maxZoomRatio: Float

    public fun applyAsync(
        zoomRatio: Float,
        requestControl: UseCaseCameraRequestControl
    ): Deferred<Unit>

    /**
     * Returns the current crop sensor region which should be used for converting
     * [androidx.camera.core.MeteringPoint] to sensor coordinates. Returns the sensor rect if there
     * is no crop region being set.
     */
    public fun getCropSensorRegion(): Rect

    @Module
    public abstract class Bindings {
        public companion object {
            @Provides
            public fun provideZoomRatio(cameraProperties: CameraProperties): ZoomCompat {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val range = cameraProperties.metadata.getControlZoomRatioRangeSafely()
                    if (range != null) {
                        AndroidRZoomCompat(cameraProperties, range)
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

public class CropRegionZoomCompat(private val cameraProperties: CameraProperties) : ZoomCompat {
    override val minZoomRatio: Float
        get() = 1.0f

    override val maxZoomRatio: Float
        get() {
            val ratio =
                cameraProperties.metadata.getOrDefault(
                    CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM,
                    minZoomRatio
                )
            if (nearZero(ratio)) {
                Log.warn { "Invalid max zoom ratio of $ratio detected, defaulting to 1.0f" }
                return 1.0f
            }
            return ratio
        }

    private var currentCropRect: Rect? = null

    override fun applyAsync(
        zoomRatio: Float,
        requestControl: UseCaseCameraRequestControl
    ): Deferred<Unit> {
        val sensorRect =
            cameraProperties.metadata[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!
        currentCropRect = computeCropRect(sensorRect, zoomRatio)
        return requestControl.setParametersAsync(
            values = mapOf(CaptureRequest.SCALER_CROP_REGION to (currentCropRect as Any))
        )
    }

    override fun getCropSensorRegion(): Rect =
        currentCropRect
            ?: cameraProperties.metadata[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!

    private fun computeCropRect(sensorRect: Rect, zoomRatio: Float): Rect {
        var ratio = zoomRatio
        if (nearZero(zoomRatio)) {
            Log.warn { "ZoomCompat: Invalid zoom ratio of 0.0f passed in, defaulting to 1.0f" }
            ratio = 1.0f
        }
        val cropWidth: Float = sensorRect.width() / ratio
        val cropHeight: Float = sensorRect.height() / ratio
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
public class AndroidRZoomCompat(
    private val cameraProperties: CameraProperties,
    private val range: Range<Float>,
) : ZoomCompat {
    private val shouldOverrideZoom =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            Api34Compat.isZoomOverrideAvailable(cameraProperties)

    override val minZoomRatio: Float
        get() = range.lower

    override val maxZoomRatio: Float
        get() = range.upper

    override fun applyAsync(
        zoomRatio: Float,
        requestControl: UseCaseCameraRequestControl
    ): Deferred<Unit> {
        require(zoomRatio in minZoomRatio..maxZoomRatio)
        val parameters: MutableMap<CaptureRequest.Key<*>, Any> =
            mutableMapOf(CaptureRequest.CONTROL_ZOOM_RATIO to zoomRatio)
        if (shouldOverrideZoom && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Api34Compat.setSettingsOverrideZoom(parameters)
        }
        return requestControl.setParametersAsync(values = parameters)
    }

    override fun getCropSensorRegion(): Rect =
        cameraProperties.metadata[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!
}
