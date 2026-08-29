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

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Controller managing CameraX initialization and image capture for the Camera test. */
@OptIn(ExperimentalProjectedApi::class)
class CameraController(
    private val context: Context,
    private val viewModel: CameraViewModel,
    private val scope: CoroutineScope,
) : AutoCloseable {

    private lateinit var imageCapture: ImageCapture
    private var lastPictureName = ""
    private var nextPictureName = ""
    private var currentLifecycleOwner: LifecycleOwner? = null
    private val photoExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        updateConnectedStateAndInitializeCamera()
    }

    fun updateConnectedStateAndInitializeCamera() {
        val connectedFlow: Flow<Boolean> =
            ProjectedContext.isProjectedDeviceConnected(context, Dispatchers.Default)
        scope.launch(Dispatchers.Default) {
            connectedFlow.collect { connected ->
                viewModel.setConnected(connected)
                if (connected) {
                    val projectedContext = createProjectedContext()
                    projectedContext?.let {
                        if (
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.setStatusMessage("Camera permission is required.")
                            return@collect
                        }
                        currentLifecycleOwner?.let { owner -> initCamera(it, owner) }
                    }
                    Log.i(TAG, "Projected device is connected")
                } else {
                    viewModel.setStatusMessage("Projected device is not connected.")
                    Log.w(TAG, "Projected device is not connected")
                    viewModel.setCameraInfo(0, false)
                }
            }
        }
    }

    fun startCamera(lifecycleOwner: LifecycleOwner) {
        currentLifecycleOwner = lifecycleOwner
        val projectedContext = createProjectedContext() ?: return
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.setStatusMessage("Camera permission is required.")
            return
        }
        initCamera(projectedContext, lifecycleOwner)
    }

    private fun createProjectedContext(): Context? {
        try {
            return ProjectedContext.createProjectedDeviceContext(context)
        } catch (e: IllegalStateException) {
            viewModel.setStatusMessage("Failed to create Projected Context.")
            Log.w(TAG, "Error creating projected context: $e")
            return null
        }
    }

    private fun initCamera(projectedContext: Context, lifecycleOwner: LifecycleOwner) {
        ProcessCameraProvider.getInstance(projectedContext).apply {
            addListener(
                {
                    val cameraProvider = get()
                    val availableCameras: List<CameraInfo> = cameraProvider.availableCameraInfos
                    if (availableCameras.isEmpty()) {
                        Log.w(TAG, "No Cameras are available on the projected context.")
                        viewModel.setStatusMessage(
                            "No Cameras are available on the projected context."
                        )
                        viewModel.setCameraInfo(0, false)
                        return@addListener
                    }
                    Log.i(TAG, "Available Camera count : ${availableCameras.size}")

                    val virtualCamera: CameraInfo = availableCameras[0]

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
                    val cameraSelector = virtualCamera.cameraSelector

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner = lifecycleOwner,
                        cameraSelector,
                        imageCapture,
                    )
                    viewModel.setCameraInfo(availableCameras.size, true)
                    viewModel.setStatusMessage("Camera ready (${availableCameras.size} available).")
                },
                ContextCompat.getMainExecutor(context),
            )
        }
    }

    fun takePicture() {
        if (!::imageCapture.isInitialized) {
            viewModel.setStatusMessage("Camera is not initialized.")
            return
        }
        viewModel.setTakingPicture(true)
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
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues,
                )
                .build()

        imageCapture.takePicture(
            outputOptions,
            photoExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    viewModel.setTakingPicture(false)
                    viewModel.setStatusMessage("Photo capture failed: ${exc.message}")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.i(TAG, "Photo capture succeeded for: $lastPictureName")
                    lastPictureName = nextPictureName
                    viewModel.setLastPictureName(lastPictureName)
                    viewModel.setStatusMessage("Photo capture succeeded for: $lastPictureName")
                    viewModel.setTakingPicture(false)

                    val uri = output.savedUri
                    if (uri != null) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val bitmap = decodeAndOrientBitmap(context, uri)
                                viewModel.setCapturedBitmap(bitmap)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error decoding captured photo", e)
                            }
                        }
                    }
                }
            },
        )
    }

    private fun decodeAndOrientBitmap(context: Context, uri: Uri): Bitmap? {
        val bitmap =
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

        val rotationDegrees =
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    when (
                        exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL,
                        )
                    ) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read EXIF orientation", e)
                0f
            }

        return if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    override fun close() {
        photoExecutor.shutdown()
    }

    companion object {
        const val PHOTO_RESOLUTION_WIDTH = 720
        const val PHOTO_RESOLUTION_HEIGHT = 1280
        const val FILENAME_FORMAT = "YYYY-MM-dd,HH_mm_ss"
        private const val TAG = "CameraController"
    }
}
