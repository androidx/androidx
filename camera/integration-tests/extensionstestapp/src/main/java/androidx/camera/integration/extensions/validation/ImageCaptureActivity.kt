/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.integration.extensions.validation

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.R
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_CAMERA_ID
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_ERROR_CODE
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_EXTENSION_MODE
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_IMAGE_ROTATION_DEGREES
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_IMAGE_URI
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_REQUEST_CODE
import androidx.camera.integration.extensions.validation.TestResults.Companion.INVALID_EXTENSION_MODE
import androidx.camera.integration.extensions.validation.TestResults.Companion.createCameraSelectorById
import androidx.camera.integration.extensions.validation.TestResults.Companion.getExtensionModeStringFromId
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ImageCaptureActivity"

class ImageCaptureActivity : AppCompatActivity() {

    private var extensionMode = INVALID_EXTENSION_MODE
    private val result = Intent()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraId: String
    private lateinit var viewFinder: PreviewView
    private lateinit var captureButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_capture_activity)

        cameraId = intent?.getStringExtra(INTENT_EXTRA_KEY_CAMERA_ID)!!
        extensionMode = intent.getIntExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, INVALID_EXTENSION_MODE)

        result.putExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, extensionMode)
        result.putExtra(INTENT_EXTRA_KEY_ERROR_CODE, ERROR_CODE_NONE)
        val requestCode = intent.getIntExtra(INTENT_EXTRA_KEY_REQUEST_CODE, -1)
        setResult(requestCode, result)

        supportActionBar?.title = "${resources.getString(R.string.extensions_validator)}"
        supportActionBar!!.subtitle =
            "Camera $cameraId [${getExtensionModeStringFromId(extensionMode)}]"

        viewFinder = findViewById(R.id.view_finder)
        captureButton = findViewById(R.id.camera_capture_button)

        lifecycleScope.launch {
            initialize()
            bindUseCases()
        }
    }

    @Suppress("DEPRECATION")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        lifecycleScope.launch {
            bindUseCases()
        }
    }

    override fun onDestroy() {
        cameraProvider.unbindAll()
        super.onDestroy()
    }

    private suspend fun initialize() {
        cameraProvider =
            ProcessCameraProvider.getInstance(this).await()
        extensionsManager =
            ExtensionsManager.getInstanceAsync(this, cameraProvider).await()
    }

    @SuppressLint("WrongConstant")
    private fun bindUseCases() {
        val cameraSelectorById = createCameraSelectorById(cameraId)

        if (!extensionsManager.isExtensionAvailable(
                cameraSelectorById,
                extensionMode
            )
        ) {
            result.putExtra(INTENT_EXTRA_KEY_ERROR_CODE, ERROR_CODE_EXTENSION_MODE_NOT_SUPPORT)
            finish()
            return
        }

        val extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            cameraSelectorById,
            extensionMode
        )

        val imageCapture = ImageCapture.Builder().build()
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(viewFinder.surfaceProvider)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                extensionCameraSelector,
                imageCapture,
                preview
            )
        } catch (e: IllegalArgumentException) {
            result.putExtra(INTENT_EXTRA_KEY_ERROR_CODE, ERROR_CODE_BIND_FAIL)
            Log.e(
                TAG,
                "Failed to bind use cases with ${getExtensionModeStringFromId(extensionMode)}"
            )
            finish()
            return
        }

        captureButton.setOnClickListener {
            captureButton.isEnabled = false

            imageCapture.takePicture(
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val tempFile = File.createTempFile(
                            getExtensionModeStringFromId(extensionMode),
                            ".jpg",
                            codeCacheDir
                        )
                        val outputStream = FileOutputStream(tempFile)
                        val byteArray = jpegImageToJpegByteArray(image)
                        outputStream.write(byteArray)
                        outputStream.close()

                        result.putExtra(INTENT_EXTRA_KEY_IMAGE_URI, tempFile.toUri())
                        result.putExtra(
                            INTENT_EXTRA_KEY_IMAGE_ROTATION_DEGREES,
                            image.imageInfo.rotationDegrees
                        )
                        finish()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        result.putExtra(INTENT_EXTRA_KEY_ERROR_CODE, ERROR_CODE_TAKE_PICTURE_FAILED)
                        finish()
                    }
                })
        }
    }

    /**
     * Converts JPEG [ImageProxy] to JPEG byte array.
     */
    internal fun jpegImageToJpegByteArray(image: ImageProxy): ByteArray {
        require(image.format == ImageFormat.JPEG) {
            "Incorrect image format of the input image proxy: ${image.format}"
        }
        val planes = image.planes
        val buffer = planes[0].buffer
        val data = ByteArray(buffer.capacity())
        buffer.rewind()
        buffer[data]
        return data
    }

    companion object {
        const val ERROR_CODE_NONE = 0
        const val ERROR_CODE_BIND_FAIL = 1
        const val ERROR_CODE_EXTENSION_MODE_NOT_SUPPORT = 2
        const val ERROR_CODE_TAKE_PICTURE_FAILED = 3
    }
}
