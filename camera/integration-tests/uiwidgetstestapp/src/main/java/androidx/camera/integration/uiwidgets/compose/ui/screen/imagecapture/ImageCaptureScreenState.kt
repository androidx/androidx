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

package androidx.camera.integration.uiwidgets.compose.ui.screen.imagecapture

import android.content.ContentValues
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl.OperationCanceledException
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.transform.OutputTransform
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.FlashAuto
import androidx.compose.material.icons.sharp.FlashOff
import androidx.compose.material.icons.sharp.FlashOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch

@VisibleForTesting
internal const val DEFAULT_LENS_FACING = CameraSelector.LENS_FACING_FRONT
private const val DEFAULT_FLASH_MODE = ImageCapture.FLASH_MODE_OFF

// State Holder for ImageCaptureScreen
// This State Holder supports Preview + ImageCapture + ImageAnalysis Use Cases
// It provides the states and implementations used in the ImageCaptureScreen
class ImageCaptureScreenState(
    initialLensFacing: Int = DEFAULT_LENS_FACING,
    initialFlashMode: Int = DEFAULT_FLASH_MODE
) {
    var lensFacing by mutableStateOf(initialLensFacing)
        private set

    var hasFlashUnit by mutableStateOf(false)
        private set

    var isCameraReady by mutableStateOf(false)
        private set

    var flashMode: Int by mutableStateOf(getValidInitialFlashMode(initialFlashMode))
        private set

    var flashModeIcon: ImageVector = getFlashModeImageVector()
        private set
        get() = getFlashModeImageVector()

    var linearZoom by mutableStateOf(0f)
        private set

    var zoomRatio by mutableStateOf(1f)
        private set

    var qrCodeBoundingBox by mutableStateOf<Rect?>(null)
        private set

    // Configure QR Code Scanner
    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    // Uses COORDINATE_SYSTEM_VIEW_REFERENCED to transform bounding box
    // to PreviewView's coordinates
    // We need to acquire OutputTransform from PreviewView for this to work
    private val mlKitAnalyzer = MlKitAnalyzer(
        listOf(barcodeScanner),
        COORDINATE_SYSTEM_VIEW_REFERENCED,
        Dispatchers.Main.asExecutor()
    ) { result ->
        val barcodes = result.getValue(barcodeScanner)
        qrCodeBoundingBox = if (barcodes != null && barcodes.size > 0) {
            barcodes[0].boundingBox
        } else {
            null
        }
    }

    // Use Cases
    private val preview = Preview.Builder().build()
    private val imageCapture = ImageCapture
        .Builder()
        .setFlashMode(flashMode)
        .build()
    private val imageAnalysis = ImageAnalysis
        .Builder()
        .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(
                Dispatchers.Main.asExecutor(),
                mlKitAnalyzer
            )
        }

    private var camera: Camera? = null

    private val mainScope = MainScope()

    fun setSurfaceProvider(surfaceProvider: SurfaceProvider) {
        Log.d(TAG, "Setting Surface Provider")
        preview.setSurfaceProvider(surfaceProvider)
    }

    // To update MlKitAnalyzer's OutputTransform when PreviewView is attached to Screen
    // It uses OutputTransform hence we need to @SuppressWarnings to get past the linter
    @SuppressWarnings("UnsafeOptInUsageError")
    fun setOutputTransform(outputTransform: OutputTransform) {
        mlKitAnalyzer.updateTransform(outputTransform.matrix)
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
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    fun toggleFlashMode() {
        Log.d(TAG, "Toggling Flash Mode")
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> {
                ImageCapture.FLASH_MODE_AUTO
            }
            ImageCapture.FLASH_MODE_AUTO -> {
                ImageCapture.FLASH_MODE_ON
            }
            ImageCapture.FLASH_MODE_ON -> {
                ImageCapture.FLASH_MODE_OFF
            }
            else -> {
                throw IllegalStateException("Flash Mode: $flashMode is invalid!")
            }
        }
        imageCapture.flashMode = flashMode
    }

    fun startTapToFocus(meteringPoint: MeteringPoint) {
        val action = FocusMeteringAction.Builder(meteringPoint).build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    fun startCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        Log.d(TAG, "Starting Camera")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector
                .Builder()
                .requireLensFacing(lensFacing)
                .build()

            // Remove observers from the old camera instance
            removeZoomStateObservers(lifecycleOwner)

            // Reset internal State of Camera
            camera = null
            hasFlashUnit = false
            isCameraReady = false

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )

                // Setup components that require Camera
                this.camera = camera
                setupZoomStateObserver(lifecycleOwner)
                hasFlashUnit = camera.cameraInfo.hasFlashUnit()
                isCameraReady = true
            } catch (exc: Exception) {
                Log.e(TAG, "Use Cases binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto(context: Context) {
        Log.d(TAG, "Taking Photo")
        val outputFileOptions = getOutputFileOptions(context)

        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${outputFileResults.savedUri}"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    private fun getOutputFileOptions(context: Context): ImageCapture.OutputFileOptions {
        val contentResolver = context.contentResolver
        val displayName = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        return ImageCapture
            .OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()
    }

    // Method to release resources when the Screen is removed from the Composition
    // We will release the ImageAnalyzer from ImageAnalysis use case and close the analyzer
    fun releaseResources() {
        imageAnalysis.clearAnalyzer()
        barcodeScanner.close()
    }

    private fun getValidInitialFlashMode(flashMode: Int): Int {
        return if (flashMode in VALID_FLASH_MODES) {
            flashMode
        } else {
            DEFAULT_FLASH_MODE
        }
    }

    private fun getFlashModeImageVector(): ImageVector {
        return when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> {
                Icons.Sharp.FlashOff
            }
            ImageCapture.FLASH_MODE_ON -> {
                Icons.Sharp.FlashOn
            }
            ImageCapture.FLASH_MODE_AUTO -> {
                Icons.Sharp.FlashAuto
            }
            else -> {
                throw IllegalStateException("Flash Mode: $flashMode is invalid!")
            }
        }
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
        private const val TAG = "ImageCaptureScreenState"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val VALID_FLASH_MODES = listOf(
            ImageCapture.FLASH_MODE_ON,
            ImageCapture.FLASH_MODE_OFF,
            ImageCapture.FLASH_MODE_AUTO
        )
        val saver: Saver<ImageCaptureScreenState, *> = listSaver(
            save = {
                listOf(it.lensFacing, it.flashMode)
            },
            restore = {
                ImageCaptureScreenState(
                    initialLensFacing = it[0],
                    initialFlashMode = it[1]
                )
            }
        )
    }
}

@Composable
fun rememberImageCaptureScreenState(
    initialLensFacing: Int = DEFAULT_LENS_FACING,
    initialFlashMode: Int = DEFAULT_FLASH_MODE
): ImageCaptureScreenState {
    return rememberSaveable(
        initialLensFacing,
        initialFlashMode,
        saver = ImageCaptureScreenState.saver
    ) {
        ImageCaptureScreenState(
            initialLensFacing = initialLensFacing,
            initialFlashMode = initialFlashMode
        )
    }
}
