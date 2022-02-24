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
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.DisplayOrientedMeteringPointFactory
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.R
import androidx.camera.integration.extensions.utils.CameraSelectorUtil.createCameraSelectorById
import androidx.camera.integration.extensions.utils.ExtensionModeUtil.getExtensionModeStringFromId
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_CAMERA_ID
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_ERROR_CODE
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_EXTENSION_MODE
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_IMAGE_ROTATION_DEGREES
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_IMAGE_URI
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_LENS_FACING
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_REQUEST_CODE
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INVALID_LENS_FACING
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.getLensFacingStringFromInt
import androidx.camera.integration.extensions.validation.TestResults.Companion.INVALID_EXTENSION_MODE
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ImageCaptureActivity"

class ImageCaptureActivity : AppCompatActivity() {

    private var extensionMode = INVALID_EXTENSION_MODE
    private var extensionEnabled = true
    private val result = Intent()
    private var lensFacing = INVALID_LENS_FACING
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraId: String
    private lateinit var viewFinder: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var camera: Camera
    private var flashMode = FLASH_MODE_OFF
    private var evToast: Toast? = null

    private val evFutureCallback: FutureCallback<Int?> = object : FutureCallback<Int?> {
        override fun onSuccess(result: Int?) {
            val ev = result!! * camera.cameraInfo.exposureState.exposureCompensationStep.toFloat()
            Log.d(TAG, "success new EV: $ev")
            showEVToast(String.format("EV: %.2f", ev))
        }

        override fun onFailure(t: Throwable) {
            Log.d(TAG, "failed $t")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_capture_activity)

        cameraId = intent?.getStringExtra(INTENT_EXTRA_KEY_CAMERA_ID)!!
        lensFacing = intent.getIntExtra(INTENT_EXTRA_KEY_LENS_FACING, INVALID_LENS_FACING)
        extensionMode = intent.getIntExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, INVALID_EXTENSION_MODE)

        result.putExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, extensionMode)
        result.putExtra(INTENT_EXTRA_KEY_ERROR_CODE, ERROR_CODE_NONE)
        val requestCode = intent.getIntExtra(INTENT_EXTRA_KEY_REQUEST_CODE, -1)
        setResult(requestCode, result)

        supportActionBar?.title = "${resources.getString(R.string.extensions_validator)}"
        supportActionBar!!.subtitle =
            "Camera $cameraId [${getLensFacingStringFromInt(lensFacing)}]" +
                "[${getExtensionModeStringFromId(extensionMode)}]"

        viewFinder = findViewById(R.id.view_finder)

        lifecycleScope.launch {
            initialize()
            bindUseCases()
            setupUiControls()
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

        imageCapture = ImageCapture.Builder().setFlashMode(flashMode).build()
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(viewFinder.surfaceProvider)

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this,
                if (extensionEnabled) extensionCameraSelector else cameraSelectorById,
                imageCapture,
                preview
            )

            Log.d(TAG, "Extension mode is $extensionMode (enabled: $extensionEnabled)")
        } catch (e: IllegalArgumentException) {
            result.putExtra(INTENT_EXTRA_KEY_ERROR_CODE, ERROR_CODE_BIND_FAIL)
            Log.e(
                TAG,
                "Failed to bind use cases with ${getExtensionModeStringFromId(extensionMode)}"
            )
            finish()
            return
        }
    }

    private fun setupUiControls() {
        // Sets up the flash toggle button
        setUpFlashButton()

        // Sets up the EV +/- buttons
        setUpEvButtons()

        // Sets up the tap-to-focus functions and zoom in/out by GestureDetector
        setupViewFinderGestureControls()

        // Sets up the extension mode enabled/disabled toggle button
        setUpExtensionToggleButton()

        // Sets up the capture button
        val captureButton: ImageButton = findViewById(R.id.camera_capture_button)

        captureButton.setOnClickListener {
            captureButton.isEnabled = false

            imageCapture.takePicture(
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val filenamePrefix =
                            "[Camera-$cameraId][${getLensFacingStringFromInt(lensFacing)}]" +
                                "[${getExtensionModeStringFromId(extensionMode)}]"
                        val filename = if (extensionEnabled) {
                            "$filenamePrefix[Enabled]"
                        } else {
                            "$filenamePrefix[Disabled]"
                        }
                        val tempFile = File.createTempFile(
                            filename,
                            "",
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

    private fun setUpFlashButton() {
        setFlashButtonResource()

        val flashToggleButton: ImageButton = findViewById(R.id.flash_toggle)

        flashToggleButton.setOnClickListener {
            flashMode = when (flashMode) {
                FLASH_MODE_ON -> FLASH_MODE_OFF
                FLASH_MODE_OFF -> FLASH_MODE_AUTO
                FLASH_MODE_AUTO -> FLASH_MODE_ON
                else -> throw IllegalArgumentException("Invalid flash mode!")
            }

            imageCapture.flashMode = flashMode
            setFlashButtonResource()
        }
    }

    private fun setFlashButtonResource() {
        val flashToggleButton: ImageButton = findViewById(R.id.flash_toggle)

        flashToggleButton.setImageResource(
            when (flashMode) {
                FLASH_MODE_ON -> R.drawable.ic_flash_on
                FLASH_MODE_OFF -> R.drawable.ic_flash_off
                FLASH_MODE_AUTO -> R.drawable.ic_flash_auto
                else -> throw IllegalArgumentException("Invalid flash mode!")
            }
        )
    }

    private fun setUpEvButtons() {
        val plusEvButton: Button = findViewById(R.id.plus_ev_button)
        plusEvButton.setOnClickListener {
            plusEv()
        }

        val decEvButton: Button = findViewById(R.id.dec_ev_button)
        decEvButton.setOnClickListener {
            decEv()
        }
    }

    private fun plusEv() {
        val range = camera.cameraInfo.exposureState.exposureCompensationRange
        val ec = camera.cameraInfo.exposureState.exposureCompensationIndex

        if (range.contains(ec + 1)) {
            val future: ListenableFuture<Int> =
                camera.cameraControl.setExposureCompensationIndex(ec + 1)
            Futures.addCallback(
                future, evFutureCallback,
                CameraXExecutors.mainThreadExecutor()
            )
        } else {
            showEVToast(
                String.format(
                    "EV: %.2f",
                    range.upper * camera.cameraInfo.exposureState.exposureCompensationStep.toFloat()
                )
            )
        }
    }

    private fun decEv() {
        val range = camera.cameraInfo.exposureState.exposureCompensationRange
        val ec = camera.cameraInfo.exposureState.exposureCompensationIndex

        if (range.contains(ec - 1)) {
            val future: ListenableFuture<Int> =
                camera.cameraControl.setExposureCompensationIndex(ec - 1)
            Futures.addCallback(
                future, evFutureCallback,
                CameraXExecutors.mainThreadExecutor()
            )
        } else {
            showEVToast(
                String.format(
                    "EV: %.2f",
                    range.lower * camera.cameraInfo.exposureState.exposureCompensationStep.toFloat()
                )
            )
        }
    }

    internal fun showEVToast(message: String?) {
        evToast?.cancel()
        evToast = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT)
        evToast?.show()
    }

    private fun setupViewFinderGestureControls() {
        val onTapGestureListener: GestureDetector.OnGestureListener =
            object : SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    val factory: MeteringPointFactory = DisplayOrientedMeteringPointFactory(
                        viewFinder.getDisplay(),
                        camera.getCameraInfo(),
                        viewFinder.getWidth().toFloat(),
                        viewFinder.getHeight().toFloat()
                    )
                    val action = FocusMeteringAction.Builder(
                        factory.createPoint(e.x, e.y)
                    ).build()
                    Futures.addCallback(
                        camera.getCameraControl().startFocusAndMetering(action),
                        object : FutureCallback<FocusMeteringResult?> {
                            override fun onSuccess(result: FocusMeteringResult?) {
                                Log.d(TAG, "Focus and metering succeeded.")
                            }

                            override fun onFailure(t: Throwable) {
                                Log.e(TAG, "Focus and metering failed.", t)
                            }
                        },
                        CameraXExecutors.mainThreadExecutor()
                    )
                    return true
                }
            }

        val scaleGestureListener: SimpleOnScaleGestureListener =
            object : SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val cameraInfo: CameraInfo = camera.getCameraInfo()
                    val newZoom = (cameraInfo.zoomState.value!!.zoomRatio
                        * detector.scaleFactor)
                    setZoomRatio(newZoom)
                    return true
                }
            }

        val tapGestureDetector = GestureDetector(this, onTapGestureListener)
        val scaleDetector = ScaleGestureDetector(this, scaleGestureListener)
        viewFinder.setOnTouchListener { _, e: MotionEvent ->
            val tapEventProcessed = tapGestureDetector.onTouchEvent(e)
            val scaleEventProcessed = scaleDetector.onTouchEvent(e)
            tapEventProcessed || scaleEventProcessed
        }
    }

    internal fun setZoomRatio(newZoom: Float) {
        val cameraInfo: CameraInfo = camera.getCameraInfo()
        val cameraControl: CameraControl = camera.getCameraControl()
        val clampedNewZoom = MathUtils.clamp(
            newZoom,
            cameraInfo.zoomState.value!!.minZoomRatio,
            cameraInfo.zoomState.value!!.maxZoomRatio
        )
        Log.d(TAG, "setZoomRatio ratio: $clampedNewZoom")
        val listenableFuture = cameraControl.setZoomRatio(
            clampedNewZoom
        )
        Futures.addCallback(listenableFuture, object : FutureCallback<Void?> {
            override fun onSuccess(result: Void?) {
                Log.d(TAG, "setZoomRatio onSuccess: $clampedNewZoom")
            }

            override fun onFailure(t: Throwable) {
                Log.d(TAG, "setZoomRatio failed, $t")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setUpExtensionToggleButton() {
        val extensionToggleButton: ImageButton = findViewById(R.id.extension_toggle)
        setExtensionToggleButtonResource()

        extensionToggleButton.setOnClickListener {
            extensionEnabled = !extensionEnabled
            setExtensionToggleButtonResource()
            bindUseCases()
            setupUiControls()

            if (extensionEnabled) {
                Toast.makeText(this, "Effect is enabled!", Toast.LENGTH_SHORT).show()
            } else {

                Toast.makeText(this, "Effect is disabled!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setExtensionToggleButtonResource() {
        val extensionToggleButton: ImageButton = findViewById(R.id.extension_toggle)

        if (!extensionEnabled) {
            extensionToggleButton.setImageResource(R.drawable.outline_block)
            return
        }

        val resourceId = when (extensionMode) {
            ExtensionMode.HDR -> R.drawable.outline_hdr_on
            ExtensionMode.BOKEH -> R.drawable.outline_portrait
            ExtensionMode.NIGHT -> R.drawable.outline_bedtime
            ExtensionMode.FACE_RETOUCH -> R.drawable.outline_face_retouching_natural
            ExtensionMode.AUTO -> R.drawable.outline_auto_awesome
            else -> throw IllegalArgumentException("Invalid extension mode!")
        }

        extensionToggleButton.setImageResource(resourceId)
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
