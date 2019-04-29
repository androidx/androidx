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

package androidx.camera.integration.antelope.cameracontrollers

import androidx.camera.integration.antelope.CameraAPI
import androidx.camera.integration.antelope.CameraParams
import androidx.camera.integration.antelope.MainActivity
import androidx.camera.integration.antelope.TestConfig

/**
 * Cross-API function to close the currently active preview stream and camera device
 */
fun closePreviewAndCamera(activity: MainActivity, params: CameraParams?, testConfig: TestConfig) {
    if (null == params)
        return

    when (testConfig.api) {
        CameraAPI.CAMERA1 -> camera1CloseCamera(activity, params, testConfig)
        CameraAPI.CAMERA2 -> camera2CloseCamera(activity, params, testConfig)
        CameraAPI.CAMERAX -> closeCameraX(activity, params, testConfig)
    }
}

/**
 * Convenience method to close all cameras on a device.
 */
fun closeAllCameras(activity: MainActivity, testConfig: TestConfig) {
    MainActivity.logd("Closing all cameras.")
    for (tempCameraParams: CameraParams in MainActivity.cameraParams.values) {
        closePreviewAndCamera(activity, tempCameraParams, testConfig)
    }
}

/**
 * Close the first open camera on the device. This can be used if a ERROR_MAX_CAMERAS_IN_USE is
 * occurring.
 */
fun closeACamera(activity: MainActivity, testConfig: TestConfig) {
    var closedACamera = false
    MainActivity.logd("In closeACamera, looking for open camera.")
    for (tempCameraParams: CameraParams in MainActivity.cameraParams.values) {
        if (tempCameraParams.isOpen) {
            MainActivity.logd("In closeACamera, found open camera, closing: " + tempCameraParams.id)
            closedACamera = true
            closePreviewAndCamera(activity, tempCameraParams, testConfig)
            break
        }
    }

    // We couldn't find an open camera, let's close everything
    if (!closedACamera) {
        closeAllCameras(activity, testConfig)
    }
}