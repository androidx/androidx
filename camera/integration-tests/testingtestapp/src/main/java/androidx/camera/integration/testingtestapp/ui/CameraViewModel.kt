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
package androidx.camera.integration.testingtestapp.ui

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture.OnImageSavedCallback
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageCaptureException
import androidx.camera.integration.testingtestapp.camerax.OutputOptionsProvider
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class CameraViewModel
@Inject
constructor(
    private val outputOptionsProvider: OutputOptionsProvider,
    private val cameraController: LifecycleCameraController,
    @Named("MainExecutor") private val mainExecutor: Executor,
) : ViewModel() {

    val errorState = MutableStateFlow("")

    private val _isUsingBackLens = MutableStateFlow(false)

    // Source of truth for whether we're using the back camera
    val isUsingBackLens: StateFlow<Boolean>
        get() = _isUsingBackLens

    fun toggleCamera() {
        _isUsingBackLens.value = !_isUsingBackLens.value
        val newCamera =
            if (isUsingBackLens.value) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }
        if (cameraController.hasCamera(newCamera)) {
            cameraController.cameraSelector = newCamera
        }
    }

    fun takePhoto() {
        try {
            cameraController.takePicture(
                outputOptionsProvider.getOutputOptions("myPhoto"),
                mainExecutor,
                object : OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: OutputFileResults) {
                        Log.d(LOG_TAG, "Saved in ${outputFileResults.savedUri}")
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.d(LOG_TAG, "Errors! ${exception.message}")
                    }
                }
            )
        } catch (e: IllegalStateException) {
            errorState.value = "Camera not ready"
        }
    }

    fun initCameraController(lifecycleOwner: LifecycleOwner): LifecycleCameraController {
        with(cameraController) {
            bindToLifecycle(lifecycleOwner)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            initializationFuture.addListener(
                {
                    if (isUsingBackLens.value && hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    } else if (
                        !isUsingBackLens.value && hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                    ) {
                        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        Log.d(LOG_TAG, "Error, no camera supported")
                        errorState.value = "No camera supported"
                    }
                },
                mainExecutor
            )
        }
        return cameraController
    }

    fun disposeCameraController() {
        cameraController.unbind()
    }

    fun setViewError(e: IllegalArgumentException) {
        errorState.value = e.message ?: "Error binding to view"
    }
}

private val LOG_TAG = "CameraViewModel"
