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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel for keeping track of application state across resizes and configuration changes.
 * This includes:
 */
class CamViewModel : ViewModel() {
    private var cameraParams: HashMap<String, CameraParams> = HashMap<String, CameraParams>()
    private val currentAPI = MutableLiveData<CameraAPI>().apply { value = CameraAPI.CAMERA2 }
    private val currentCamera = MutableLiveData<Int>().apply { value = 0 }
    private val currentFocusMode = MutableLiveData<FocusMode>().apply { value = FocusMode.AUTO }
    private val currentImageCaptureSize = MutableLiveData<ImageCaptureSize>().apply {
        value = ImageCaptureSize.MAX
    }
    private val shouldOutputLog = MutableLiveData<Boolean>().apply { value = false }
    private val humanReadableReport = MutableLiveData<String>().apply {
        value = "Android Camera Performance Tool"
    }

    /** Camera API of the current test */
    fun getCurrentAPI(): MutableLiveData<CameraAPI> {
        return currentAPI
    }

    /** Camera ID of the current test */
    fun getCurrentCamera(): MutableLiveData<Int> {
        return currentCamera
    }

    /** Focus mode of the current test */
    fun getCurrentFocusMode(): MutableLiveData<FocusMode> {
        return currentFocusMode
    }

    /** Requested image capture size of the current test */
    fun getCurrentImageCaptureSize(): MutableLiveData<ImageCaptureSize> {
        return currentImageCaptureSize
    }

    /** Hashmap of the CameraParams associated with all the cameras on the device */
    fun getCameraParams(): HashMap<String, CameraParams> {
        return cameraParams
    }

    /** If the user has asked to output the debugging log */
    fun getShouldOutputLog(): MutableLiveData<Boolean> {
        return shouldOutputLog
    }

    /** Current value of the main output window on screen */
    fun getHumanReadableReport(): MutableLiveData<String> {
        return humanReadableReport
    }
}