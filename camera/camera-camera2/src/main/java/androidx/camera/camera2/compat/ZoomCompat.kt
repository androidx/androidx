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

package androidx.camera.camera2.compat

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Range
import androidx.annotation.RequiresApi
import androidx.camera.camera2.compat.workaround.getActiveArraySizeSafely
import androidx.camera.camera2.compat.workaround.getControlZoomRatioRangeSafely
import androidx.camera.camera2.impl.Camera2Logger
import androidx.camera.camera2.impl.CameraProperties
import androidx.camera.camera2.impl.UseCaseCameraRequestControl
import androidx.camera.camera2.internal.ZoomMath.nearZero
import androidx.camera.camera2.pipe.CameraMetadata.Companion.supportsZoomOverride
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

public interface ZoomCompat {
    public val minZoomRatio: Float
    public val maxZoomRatio: Float

    /** Applies the zoom ratio settings to the request control. */
    public fun applyAsync(
        zoomRatio: Float,
        requestControl: UseCaseCameraRequestControl,
    ): Deferred<Unit>

    /**
     * Removes the zoom ratio settings from the request control.
     *
     * This call only clears the settings, which makes the camera to remain in the original state
     * rather than sets it to the default.
     */
    public fun resetAsync(requestControl: UseCaseCameraRequestControl): Deferred<Unit>

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
            public fun provideZoomCompat(cameraProperties: CameraProperties): ZoomCompat {
                if ("robolectric" == Build.FINGERPRINT) {
                    val isMissingCharacteristics =
                        NoOpZoomCompat.requiredCharacteristics.any {
                            Camera2Logger.warn { "Failed to read $it for zoom features." }
                            cameraProperties.metadata[it] == null
                        }
                    if (isMissingCharacteristics) {
                        // In a Robolectric environment with missing characteristics, use a no-op
                        // implementation.
                        return NoOpZoomCompat(cameraProperties)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cameraProperties.metadata.getControlZoomRatioRangeSafely()?.let { range ->
                        // Return Android R implementation if zoom ratio range is valid.
                        return AndroidRZoomCompat(cameraProperties, range)
                    }
                }
                return CropRegionZoomCompat(cameraProperties)
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
                    minZoomRatio,
                )
            if (nearZero(ratio)) {
                Camera2Logger.warn {
                    "Invalid max zoom ratio of $ratio detected, defaulting to 1.0f"
                }
                return 1.0f
            }
            return ratio
        }

    private var currentCropRect: Rect? = null
    private val sensorRect =
        cameraProperties.metadata[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!

    override fun applyAsync(
        zoomRatio: Float,
        requestControl: UseCaseCameraRequestControl,
    ): Deferred<Unit> {
        currentCropRect = computeCropRect(sensorRect, zoomRatio)
        return requestControl.setParametersAsync(
            values = mapOf(CaptureRequest.SCALER_CROP_REGION to (currentCropRect as Any))
        )
    }

    override fun resetAsync(requestControl: UseCaseCameraRequestControl): Deferred<Unit> {
        return requestControl.removeParametersAsync(
            keys = listOf(CaptureRequest.SCALER_CROP_REGION)
        )
    }

    override fun getCropSensorRegion(): Rect = currentCropRect ?: sensorRect

    private fun computeCropRect(sensorRect: Rect, zoomRatio: Float): Rect {
        var ratio = zoomRatio
        if (nearZero(zoomRatio)) {
            Camera2Logger.warn {
                "ZoomCompat: Invalid zoom ratio of 0.0f passed in, defaulting to 1.0f"
            }
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
            (top + cropHeight).toInt(),
        )
    }
}

@RequiresApi(Build.VERSION_CODES.R)
public class AndroidRZoomCompat(
    private val cameraProperties: CameraProperties,
    private val range: Range<Float>,
) : ZoomCompat {
    override val minZoomRatio: Float
        get() = range.lower

    override val maxZoomRatio: Float
        get() = range.upper

    override fun applyAsync(
        zoomRatio: Float,
        requestControl: UseCaseCameraRequestControl,
    ): Deferred<Unit> {
        require(zoomRatio in minZoomRatio..maxZoomRatio)
        val parameters: MutableMap<CaptureRequest.Key<*>, Any> =
            mutableMapOf(CaptureRequest.CONTROL_ZOOM_RATIO to zoomRatio)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                cameraProperties.metadata.supportsZoomOverride
        ) {
            Api34Compat.setSettingsOverrideZoom(parameters)
        }
        return requestControl.setParametersAsync(values = parameters)
    }

    override fun resetAsync(requestControl: UseCaseCameraRequestControl): Deferred<Unit> {
        val keys: MutableList<CaptureRequest.Key<*>> =
            mutableListOf(CaptureRequest.CONTROL_ZOOM_RATIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            keys.add(CaptureRequest.CONTROL_SETTINGS_OVERRIDE)
        }
        return requestControl.removeParametersAsync(keys = keys)
    }

    override fun getCropSensorRegion(): Rect =
        cameraProperties.metadata[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!
}

/**
 * A [ZoomCompat] implementation that is no-op. This is used for testing environments like
 * Robolectric where camera characteristics might not be fully supported.
 */
internal class NoOpZoomCompat(private val cameraProperties: CameraProperties) : ZoomCompat {
    override val minZoomRatio: Float = 1.0f
    override val maxZoomRatio: Float = 1.0f

    override fun applyAsync(
        zoomRatio: Float,
        requestControl: UseCaseCameraRequestControl,
    ): Deferred<Unit> = CompletableDeferred(Unit)

    override fun resetAsync(requestControl: UseCaseCameraRequestControl): Deferred<Unit> =
        CompletableDeferred(Unit)

    override fun getCropSensorRegion(): Rect = cameraProperties.metadata.getActiveArraySizeSafely()

    internal companion object {
        val requiredCharacteristics = listOf(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
    }
}
