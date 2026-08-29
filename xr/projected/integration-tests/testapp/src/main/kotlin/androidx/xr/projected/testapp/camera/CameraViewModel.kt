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

package androidx.xr.projected.testapp.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** ViewModel holding UI state and camera info for Camera test. */
@OptIn(ExperimentalProjectedApi::class)
class CameraViewModel : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _cameraCount = MutableStateFlow(0)
    val cameraCount: StateFlow<Int> = _cameraCount.asStateFlow()

    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    private val _isTakingPicture = MutableStateFlow(false)
    val isTakingPicture: StateFlow<Boolean> = _isTakingPicture.asStateFlow()

    private val _lastPictureName = MutableStateFlow("")
    val lastPictureName: StateFlow<String> = _lastPictureName.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _statusMessage = MutableStateFlow("Initializing")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }

    fun setCameraInfo(count: Int, ready: Boolean) {
        _cameraCount.value = count
        _isCameraReady.value = ready
    }

    fun setTakingPicture(taking: Boolean) {
        _isTakingPicture.value = taking
    }

    fun setLastPictureName(name: String) {
        _lastPictureName.value = name
    }

    fun setCapturedBitmap(bitmap: Bitmap?) {
        _capturedBitmap.value = bitmap
    }

    fun setStatusMessage(message: String) {
        _statusMessage.value = message
    }
}
