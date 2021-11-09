/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.antelope

import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Build
import android.util.SparseIntArray
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.integration.antelope.MainActivity.Companion.FIXED_FOCUS_DISTANCE
import androidx.camera.integration.antelope.MainActivity.Companion.cameraParams
import androidx.camera.integration.antelope.MainActivity.Companion.logd
import androidx.camera.integration.antelope.cameracontrollers.Camera2CaptureSessionCallback
import androidx.camera.integration.antelope.cameracontrollers.Camera2DeviceStateCallback
import java.util.Arrays
import java.util.Collections

/** The camera API to use */
enum class CameraAPI(private val api: String) {
    /** Camera 1 API */
    CAMERA1("Camera1"),
    /** Camera 2 API */
    CAMERA2("Camera2"),
    /** Camera X API */
    CAMERAX("CameraX")
}

/** The output capture size to request for captures */
enum class ImageCaptureSize(private val size: String) {
    /** Request captures to be the maximum supported size for this camera sensor */
    MAX("Max"),
    /** Request captures to be the minimum supported size for this camera sensor */
    MIN("Min"),
}

/** The focus mechanism to request for captures */
enum class FocusMode(private val mode: String) {
    /** Auto-focus */
    AUTO("Auto"),
    /** Continuous auto-focus */
    CONTINUOUS("Continuous"),
    /** For fixed-focus lenses */
    FIXED("Fixed")
}

/**
 * For all the cameras associated with the device, populate the CameraParams values and add them to
 * the companion object for the activity.
 */
fun initializeCameras(activity: MainActivity) {
    val manager = activity.getSystemService(AppCompatActivity.CAMERA_SERVICE) as CameraManager
    try {
        val numCameras = manager.cameraIdList.size
        for (cameraId in manager.cameraIdList) {
            var cameraParamsValid = true

            val tempCameraParams = CameraParams().apply {

                val cameraChars = manager.getCameraCharacteristics(cameraId)
                val cameraCapabilities =
                    cameraChars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                        ?: IntArray(0)

                // Check supported format.
                val map = cameraChars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                if (map == null || map.isOutputSupportedFor(ImageFormat.JPEG) == false) {
                    cameraParamsValid = false
                    logd(
                        "Null streamConfigurationMap or not supporting JPEG output format " +
                            "in cameraId:" + cameraId
                    )
                    return@apply
                }

                // Multi-camera
                for (capability in cameraCapabilities) {
                    if (capability ==
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
                    ) {
                        hasMulti = true
                    } else if (capability ==
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
                    ) {
                        hasManualControl = true
                    }
                }

                logd("Camera " + cameraId + " of " + numCameras)

                id = cameraId
                isOpen = false
                hasFlash = cameraChars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                isFront = CameraCharacteristics.LENS_FACING_FRONT ==
                    cameraChars.get(CameraCharacteristics.LENS_FACING)

                isExternal = (
                    Build.VERSION.SDK_INT >= 23 &&
                        CameraCharacteristics.LENS_FACING_EXTERNAL ==
                        cameraChars.get(CameraCharacteristics.LENS_FACING)
                    )

                characteristics = cameraChars
                focalLengths =
                    cameraChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?: FloatArray(0)
                smallestFocalLength = smallestFocalLength(focalLengths)
                minDeltaFromNormal = focalLengthMinDeltaFromNormal(focalLengths)

                apertures = cameraChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                    ?: FloatArray(0)
                largestAperture = largestAperture(apertures)
                minFocusDistance =
                    cameraChars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
                    ?: MainActivity.FIXED_FOCUS_DISTANCE

                for (focalLength in focalLengths) {
                    logd("In " + id + " found focalLength: " + focalLength)
                }
                logd("Smallest smallestFocalLength: " + smallestFocalLength)
                logd("minFocusDistance: " + minFocusDistance)

                for (aperture in apertures) {
                    logd("In " + id + " found aperture: " + aperture)
                }
                logd("Largest aperture: " + largestAperture)

                if (hasManualControl) {
                    logd("Has Manual, minFocusDistance: " + minFocusDistance)
                }

                // Autofocus
                hasAF = minFocusDistance != FIXED_FOCUS_DISTANCE // If camera is fixed focus, no AF

                effects = cameraChars.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS)
                    ?: IntArray(0)
                hasSepia = effects.contains(CameraMetadata.CONTROL_EFFECT_MODE_SEPIA)
                hasMono = effects.contains(CameraMetadata.CONTROL_EFFECT_MODE_MONO)

                if (hasSepia)
                    logd("WE HAVE Sepia!")
                if (hasMono)
                    logd("WE HAVE Mono!")

                isLegacy = cameraChars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ==
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY

                val activeSensorRect: Rect = cameraChars.get(SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
                megapixels = (activeSensorRect.width() * activeSensorRect.height()) / 1000000

                camera2DeviceStateCallback =
                    Camera2DeviceStateCallback(this, activity, TestConfig())
                camera2CaptureSessionCallback =
                    Camera2CaptureSessionCallback(activity, this, TestConfig())

                previewSurfaceView = activity.binding.surfacePreview
                cameraXPreviewTexture = activity.binding.texturePreview

                cameraXPreviewBuilder = Preview.Builder()

                cameraXCaptureBuilder = ImageCapture.Builder()

                imageAvailableListener =
                    ImageAvailableListener(activity, this, TestConfig())

                if (Build.VERSION.SDK_INT >= 28) {
                    physicalCameras = cameraChars.physicalCameraIds
                }

                // Get Camera2 and CameraX image capture sizes.
                cam2MaxSize = Collections.max(
                    Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea()
                )

                cam2MinSize = Collections.min(
                    Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea()
                )

                // Use minimum image size for preview
                previewSurfaceView?.holder?.setFixedSize(cam2MinSize.width, cam2MinSize.height)

                setupImageReader(activity, this, TestConfig())
            }

            if (cameraParamsValid == false) {
                logd("Don't put Camera " + cameraId + "of " + numCameras)
                continue
            } else {
                cameraParams.put(cameraId, tempCameraParams)
            }
        } // For all camera devices
    } catch (accessError: CameraAccessException) {
        accessError.printStackTrace()
    }
}

/**
 * Convenience method to configure the ImageReaders required for Camera1 and Camera2 APIs.
 *
 * Uses JPEG image format, checks the current test configuration to determine the needed size.
 */
fun setupImageReader(activity: MainActivity, params: CameraParams, testConfig: TestConfig) {

    // Only use ImageReader for Camera1 and Camera2
    if (CameraAPI.CAMERAX != testConfig.api) {
        params.imageAvailableListener = ImageAvailableListener(activity, params, testConfig)

        val useLargest = testConfig.imageCaptureSize == ImageCaptureSize.MAX

        val size = if (useLargest)
            params.cam2MaxSize
        else
            params.cam2MinSize

        params.imageReader?.close()
        params.imageReader = ImageReader.newInstance(
            size.width, size.height,
            ImageFormat.JPEG, 5
        )
        params.imageReader?.setOnImageAvailableListener(
            params.imageAvailableListener, params.backgroundHandler
        )
    }
}

/** Finds the smallest focal length in the given array, useful for finding the widest angle lens */
fun smallestFocalLength(focalLengths: FloatArray): Float = focalLengths.minOrNull()
    ?: MainActivity.INVALID_FOCAL_LENGTH

/** Finds the largest aperture in the array of focal lengths */
fun largestAperture(apertures: FloatArray): Float = apertures.maxOrNull()
    ?: MainActivity.NO_APERTURE

/** Finds the most "normal" focal length in the array of focal lengths */
fun focalLengthMinDeltaFromNormal(focalLengths: FloatArray): Float =
    focalLengths.minByOrNull { Math.abs(it - MainActivity.NORMAL_FOCAL_LENGTH) }
        ?: Float.MAX_VALUE

/** Adds automatic flash to the given CaptureRequest.Builder */
fun setAutoFlash(params: CameraParams, requestBuilder: CaptureRequest.Builder?) {
    try {
        if (params.hasFlash) {
            requestBuilder?.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )

            // Force flash always on
//            requestBuilder?.set(CaptureRequest.CONTROL_AE_MODE,
//                    CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
        }
    } catch (e: Exception) {
        // Do nothing
    }
}

/**
 * We have to take sensor orientation into account and rotate JPEG properly.
 */
fun getOrientation(params: CameraParams, rotation: Int): Int {
    val orientations = SparseIntArray()
    orientations.append(Surface.ROTATION_0, 90)
    orientations.append(Surface.ROTATION_90, 0)
    orientations.append(Surface.ROTATION_180, 270)
    orientations.append(Surface.ROTATION_270, 180)

    logd(
        "Orientation: sensor: " +
            params.characteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION) +
            " and current rotation: " + orientations.get(rotation)
    )
    val sensorRotation: Int =
        params.characteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
    return (orientations.get(rotation) + sensorRotation + 270) % 360
}
