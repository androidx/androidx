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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val TAG = "CameraController"

/**
 * Controller managing CameraX initialization and image capture for the Camera test.
 *
 * The camera is only bound once both of these conditions are met:
 * 1. The projected device is connected.
 * 2. The CAMERA permission is already granted on the projected device. The Camera test does not
 *    request it; grant it beforehand (e.g. through the Permissions test).
 *
 * Call flow:
 * ```
 * start()
 *  └─ observe projected device connection
 *      ├─ connected    -> onProjectedDeviceConnected()
 *      │                   ├─ permission granted -> bindCamera()
 *      │                   └─ permission missing -> show "permission required" status
 *      └─ disconnected -> onProjectedDeviceDisconnected() -> unbindCamera()
 * ```
 *
 * All methods must be called on the main thread. [scope] is expected to dispatch on the main thread
 * (e.g. `lifecycleScope`), so connection updates are also handled on the main thread.
 *
 * @param context The host Activity context.
 * @param lifecycleOwner The lifecycle the camera use cases are bound to.
 * @param viewModel Holds the UI state for the Camera test.
 * @param scope The scope used to observe the projected device connection.
 */
@OptIn(ExperimentalProjectedApi::class)
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: CameraViewModel,
    private val scope: CoroutineScope,
) : AutoCloseable {

    /** Context of the connected projected device, or null while disconnected. */
    private var projectedContext: Context? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var connectionJob: Job? = null
    private var nextPictureName = ""
    private val photoExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /** Starts observing the projected device connection. Subsequent calls are no-ops. */
    fun start() {
        if (connectionJob != null) return
        connectionJob = scope.launch {
            ProjectedContext.isProjectedDeviceConnected(context, Dispatchers.Default)
                .distinctUntilChanged()
                .collect { connected ->
                    viewModel.setConnected(connected)
                    if (connected) {
                        onProjectedDeviceConnected()
                    } else {
                        onProjectedDeviceDisconnected()
                    }
                }
        }
    }

    private fun onProjectedDeviceConnected() {
        Log.i(TAG, "Projected device is connected")
        val projectedContext = createProjectedContext() ?: return
        this.projectedContext = projectedContext

        if (hasCameraPermission(projectedContext)) {
            bindCamera(projectedContext)
        } else {
            Log.w(TAG, "CAMERA permission is not granted on the projected device")
            viewModel.setStatusMessage(
                "Camera permission is not granted on the projected device. Grant it in the " +
                    "Permissions test and relaunch the Camera test."
            )
        }
    }

    private fun onProjectedDeviceDisconnected() {
        Log.w(TAG, "Projected device is not connected")
        unbindCamera()
        projectedContext = null
        viewModel.setStatusMessage("Projected device is not connected.")
    }

    private fun createProjectedContext(): Context? =
        try {
            ProjectedContext.createProjectedDeviceContext(context)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Error creating projected context: $e")
            viewModel.setStatusMessage("Failed to create Projected Context.")
            null
        }

    private fun bindCamera(projectedContext: Context) {
        val providerFuture = ProcessCameraProvider.getInstance(projectedContext)
        providerFuture.addListener(
            {
                if (this.projectedContext !== projectedContext) {
                    Log.w(
                        TAG,
                        "Projected context changed or disconnected before CameraProvider was ready.",
                    )
                    return@addListener
                }

                val provider = providerFuture.get()
                val availableCameras = provider.availableCameraInfos
                if (availableCameras.isEmpty()) {
                    Log.w(TAG, "No Cameras are available on the projected context.")
                    viewModel.setStatusMessage("No Cameras are available on the projected context.")
                    viewModel.setCameraInfo(0, false)
                    return@addListener
                }
                Log.i(TAG, "Available Camera count : ${availableCameras.size}")

                val capture = createImageCapture()
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    availableCameras.first().cameraSelector,
                    capture,
                )
                cameraProvider = provider
                imageCapture = capture
                viewModel.setCameraInfo(availableCameras.size, true)
                viewModel.setStatusMessage("Camera ready (${availableCameras.size} available).")
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun unbindCamera() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
        viewModel.setCameraInfo(0, false)
    }

    fun takePicture() {
        val capture = imageCapture
        if (capture == null) {
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

        capture.takePicture(
            outputOptions,
            photoExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    viewModel.setTakingPicture(false)
                    viewModel.setStatusMessage("Photo capture failed: ${exc.message}")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val pictureName = nextPictureName
                    Log.i(TAG, "Photo capture succeeded for: $pictureName")
                    viewModel.setLastPictureName(pictureName)
                    viewModel.setStatusMessage("Photo capture succeeded for: $pictureName")
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

    override fun close() {
        connectionJob?.cancel()
        connectionJob = null
        unbindCamera()
        projectedContext = null
        photoExecutor.shutdown()
    }

    companion object {
        const val PHOTO_RESOLUTION_WIDTH = 720
        const val PHOTO_RESOLUTION_HEIGHT = 1280
        const val FILENAME_FORMAT = "yyyy-MM-dd_HH_mm_ss_SSS"
    }
}

private fun hasCameraPermission(projectedContext: Context): Boolean =
    ContextCompat.checkSelfPermission(projectedContext, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

private fun createImageCapture(): ImageCapture =
    ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(
                            CameraController.PHOTO_RESOLUTION_WIDTH,
                            CameraController.PHOTO_RESOLUTION_HEIGHT,
                        ),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                    )
                )
                .build()
        )
        .build()

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
