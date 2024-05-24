/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.uiwidgets.compose.ui.screen.viewfinder

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl.OperationCanceledException
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@VisibleForTesting internal const val DEFAULT_LENS_FACING = CameraSelector.LENS_FACING_FRONT

/**
 * State Holder for ViewfinderScreen
 *
 * This State Holder supports the Preview Use Case It provides the states and implementations used
 * in the ViewfinderScreen
 */
class ViewfinderScreenState(initialLensFacing: Int = DEFAULT_LENS_FACING) {
    var lensFacing by mutableIntStateOf(initialLensFacing)
        private set

    var hasFlashUnit by mutableStateOf(false)
        private set

    var isCameraReady by mutableStateOf(false)
        private set

    var linearZoom by mutableFloatStateOf(0f)
        private set

    var zoomRatio by mutableFloatStateOf(1f)
        private set

    // Use Cases
    private val preview = Preview.Builder().build()

    private var camera: Camera? = null

    private val mainScope = MainScope()

    fun setSurfaceProvider(surfaceProvider: SurfaceProvider) {
        Log.d(TAG, "Setting Surface Provider")
        preview.setSurfaceProvider(surfaceProvider)
    }

    @JvmName("setLinearZoomFunction")
    fun setLinearZoom(linearZoom: Float) {
        Log.d(TAG, "Setting Linear Zoom $linearZoom")

        if (camera == null) {
            Log.d(TAG, "Camera is not ready to set Linear Zoom")
            return
        }

        val future = camera!!.cameraControl.setLinearZoom(linearZoom)
        mainScope.launch {
            try {
                future.await()
            } catch (exc: Exception) {
                // Log errors not related to CameraControl.OperationCanceledException
                if (exc !is OperationCanceledException) {
                    Log.w(TAG, "setLinearZoom: $linearZoom failed. ${exc.message}")
                }
            }
        }
    }

    fun toggleLensFacing() {
        Log.d(TAG, "Toggling Lens")
        lensFacing =
            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
    }

    fun startTapToFocus(meteringPoint: MeteringPoint) {
        val action = FocusMeteringAction.Builder(meteringPoint).build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    fun startCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        Log.d(TAG, "Starting Camera")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()

                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                // Remove observers from the old camera instance
                removeZoomStateObservers(lifecycleOwner)

                // Reset internal State of Camera
                camera = null
                hasFlashUnit = false
                isCameraReady = false

                try {
                    cameraProvider.unbindAll()
                    val camera =
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                        )

                    // Setup components that require Camera
                    this.camera = camera
                    setupZoomStateObserver(lifecycleOwner)
                    hasFlashUnit = camera.cameraInfo.hasFlashUnit()
                    isCameraReady = true
                } catch (exc: Exception) {
                    Log.e(TAG, "Use Cases binding failed", exc)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun setupZoomStateObserver(lifecycleOwner: LifecycleOwner) {
        Log.d(TAG, "Setting up Zoom State Observer")

        if (camera == null) {
            Log.d(TAG, "Camera is not ready to set up observer")
            return
        }

        removeZoomStateObservers(lifecycleOwner)
        camera!!.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
            linearZoom = state.linearZoom
            zoomRatio = state.zoomRatio
        }
    }

    private fun removeZoomStateObservers(lifecycleOwner: LifecycleOwner) {
        Log.d(TAG, "Removing Observers")

        if (camera == null) {
            Log.d(TAG, "Camera is not present to remove observers")
            return
        }

        camera!!.cameraInfo.zoomState.removeObservers(lifecycleOwner)
    }

    companion object {
        private const val TAG = "ViewfinderScreenState"
        val saver: Saver<ViewfinderScreenState, *> =
            listSaver(
                save = { listOf(it.lensFacing) },
                restore = { ViewfinderScreenState(initialLensFacing = it[0]) }
            )
    }
}

@Composable
fun rememberViewfinderScreenState(
    initialLensFacing: Int = DEFAULT_LENS_FACING
): ViewfinderScreenState {
    return rememberSaveable(initialLensFacing, saver = ViewfinderScreenState.saver) {
        ViewfinderScreenState(
            initialLensFacing = initialLensFacing,
        )
    }
}
