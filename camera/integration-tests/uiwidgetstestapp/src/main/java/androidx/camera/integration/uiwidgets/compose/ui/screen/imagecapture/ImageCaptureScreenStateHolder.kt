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
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.lifecycle.ProcessCameraProvider
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Locale

private const val DEFAULT_LENS_FACING = CameraSelector.LENS_FACING_FRONT
private const val DEFAULT_FLASH_MODE = ImageCapture.FLASH_MODE_OFF

class ImageCaptureScreenStateHolder(
    initialLensFacing: Int = DEFAULT_LENS_FACING,
    initialFlashMode: Int = DEFAULT_FLASH_MODE
) {
    var lensFacing by mutableStateOf(initialLensFacing)
        private set

    var hasFlashMode by mutableStateOf(false)
        private set

    var flashMode: Int by mutableStateOf(getValidInitialFlashMode(initialFlashMode))
        private set

    var flashModeIcon: ImageVector = getFlashModeImageVector()
        private set
        get() = getFlashModeImageVector()

    private val preview = Preview.Builder().build()
    private val imageCapture = ImageCapture
        .Builder()
        .setFlashMode(flashMode)
        .build()

    fun setSurfaceProvider(surfaceProvider: SurfaceProvider) {
        Log.d(TAG, "Setting Surface Provider")
        preview.setSurfaceProvider(surfaceProvider)
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

    fun startCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        Log.d(TAG, "Starting Camera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector
                .Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                // Setup components that require Camera
                this.hasFlashMode = camera.cameraInfo.hasFlashUnit()
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

    companion object {
        private const val TAG = "ImageCaptureScreenState"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val VALID_FLASH_MODES = listOf(
            ImageCapture.FLASH_MODE_ON,
            ImageCapture.FLASH_MODE_OFF,
            ImageCapture.FLASH_MODE_AUTO
        )
        val saver: Saver<ImageCaptureScreenStateHolder, *> = listSaver(
            save = {
                listOf(it.lensFacing, it.flashMode)
            },
            restore = {
                ImageCaptureScreenStateHolder(
                    initialLensFacing = it[0],
                    initialFlashMode = it[1]
                )
            }
        )
    }
}

@Composable
fun rememberImageCaptureScreenStateHolder(
    initialLensFacing: Int = DEFAULT_LENS_FACING,
    initialFlashMode: Int = DEFAULT_FLASH_MODE
): ImageCaptureScreenStateHolder {
    return rememberSaveable(
        initialLensFacing,
        initialFlashMode,
        saver = ImageCaptureScreenStateHolder.saver
    ) {
        ImageCaptureScreenStateHolder(
            initialLensFacing = initialLensFacing,
            initialFlashMode = initialFlashMode
        )
    }
}