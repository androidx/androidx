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

package androidx.xr.projected.testapp.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** The CameraActivity takes a picture on a projected device. */
@OptIn(ExperimentalProjectedApi::class)
class CameraActivity : ComponentActivity() {
    private lateinit var imageCapture: ImageCapture
    private lateinit var connectedFlow: Flow<Boolean>
    private var cameraInitialized = mutableStateOf(false)
    private var statusMessage = mutableStateOf("Initializing")
    private var takingPicture = mutableStateOf(false)
    private var lastPictureName = ""
    private var nextPictureName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "Creating CameraActivity")
        super.onCreate(savedInstanceState)
        updateConnectedStateAndInitializeCamera()
        setContent { CreateCameraUi() }
    }

    @Composable
    private fun CreateCameraUi() {
        val displayCameraUi = remember { cameraInitialized }
        val cameraStatus = remember { statusMessage }
        val disableButton = remember { takingPicture }
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            if (!displayCameraUi.value) {
                Text(cameraStatus.value)
                return
            }
            if (lastPictureName.isNotEmpty()) {
                Text("Last Picture Name: $lastPictureName")
            }
            Button(onClick = { takePicture() }, enabled = !disableButton.value) {
                Text("Take Picture")
            }
        }
    }

    fun updateConnectedStateAndInitializeCamera() {
        connectedFlow = ProjectedContext.isProjectedDeviceConnected(this, Dispatchers.Default)
        CoroutineScope(Dispatchers.Default).launch {
            connectedFlow.collect { connected ->
                if (connected) {
                    val projectedContext = createProjectedContext()
                    projectedContext?.let {
                        if (
                            ActivityCompat.checkSelfPermission(it, Manifest.permission.CAMERA) !=
                                PackageManager.PERMISSION_GRANTED
                        ) {
                            statusMessage.value = "Camera permission is required."
                            return@collect
                        }
                        // Once we are connected initialize the camera
                        initCamera(it)
                    }
                    Log.i(TAG, "Projected device is connected")
                } else {
                    statusMessage.value = "Projected device is not connected."
                    Log.w(TAG, "Projected device is not connected")
                    cameraInitialized.value = false
                }
            }
        }
    }

    private fun createProjectedContext(): Context? {
        try {
            return ProjectedContext.createProjectedDeviceContext(this)
        } catch (e: IllegalStateException) {
            statusMessage.value = "Failed to create Projected Context."
            Log.w(TAG, "Error creating projected context: $e")
            return null
        }
    }

    // Initialize the Camera.
    private fun initCamera(context: Context) {
        ProcessCameraProvider.getInstance(context).apply {
            addListener(
                {
                    val cameraProvider = get()
                    val availableCameras: List<CameraInfo> = cameraProvider.availableCameraInfos
                    if (availableCameras.isEmpty()) {
                        Log.w(TAG, "No Cameras are available on the projected context.")
                        statusMessage.value = "No Cameras are available on the projected context."
                        return@addListener
                    }
                    Log.i(TAG, "Available Camera count : ${availableCameras.size}")

                    val virtualCamera: CameraInfo = availableCameras[0]

                    // No previous image capture to reuse, so we bind a new one
                    imageCapture =
                        ImageCapture.Builder()
                            .setResolutionSelector(
                                ResolutionSelector.Builder()
                                    .setResolutionStrategy(
                                        ResolutionStrategy(
                                            Size(PHOTO_RESOLUTION_WIDTH, PHOTO_RESOLUTION_HEIGHT),
                                            ResolutionStrategy
                                                .FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                                        )
                                    )
                                    .build()
                            )
                            .build()
                    val cameraSelector = virtualCamera.getCameraSelector()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner = this@CameraActivity,
                        cameraSelector,
                        imageCapture,
                    )
                    cameraInitialized.value = true
                },
                ContextCompat.getMainExecutor(this@CameraActivity),
            )
        }
    }

    private fun takePicture() {
        takingPicture.value = true
        Log.i(TAG, "Taking a Picture")
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            }
        nextPictureName = "$name.jpg"
        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(
                    contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues,
                )
                .build()

        imageCapture.takePicture(
            outputOptions,
            PHOTO_EXECUTOR,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    takingPicture.value = false
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.i(TAG, "Photo capture succeeded for: $lastPictureName")
                    lastPictureName = nextPictureName
                    takingPicture.value = false
                }
            },
        )
    }

    private companion object {
        const val TAG = "CameraActivity"
        const val PHOTO_RESOLUTION_WIDTH = 720
        const val PHOTO_RESOLUTION_HEIGHT = 1280
        const val FILENAME_FORMAT = "YYYY-MM-dd,HH_mm_ss"
        private val PHOTO_EXECUTOR: ExecutorService = Executors.newSingleThreadExecutor()
    }
}
