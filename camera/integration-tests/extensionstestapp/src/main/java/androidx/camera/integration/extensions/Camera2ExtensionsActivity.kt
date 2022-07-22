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

package androidx.camera.integration.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.ExtensionSessionConfiguration
import android.hardware.camera2.params.OutputConfiguration
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import android.view.TextureView
import android.view.ViewStub
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.calculateRelativeImageRotationDegrees
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.createExtensionCaptureCallback
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.getDisplayRotationDegrees
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.getExtensionModeStringFromId
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.getLensFacingCameraId
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.pickPreviewResolution
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.pickStillImageResolution
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.transformPreview
import androidx.camera.integration.extensions.utils.FileUtil
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer
import androidx.core.util.Preconditions
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import java.text.Format
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "Camera2ExtensionsAct~"
private const val EXTENSION_MODE_INVALID = -1

@RequiresApi(31)
class Camera2ExtensionsActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager

    /**
     * A reference to the opened [CameraDevice].
     */
    private var cameraDevice: CameraDevice? = null

    /**
     * The current camera extension session.
     */
    private var cameraExtensionSession: CameraExtensionSession? = null

    private var currentCameraId = "0"

    private lateinit var backCameraId: String
    private lateinit var frontCameraId: String

    private var cameraSensorRotationDegrees = 0

    /**
     * Still capture image reader
     */
    private var stillImageReader: ImageReader? = null

    /**
     * Camera extension characteristics for the current camera device.
     */
    private lateinit var extensionCharacteristics: CameraExtensionCharacteristics

    /**
     * Flag whether we should restart preview after an extension switch.
     */
    private var restartPreview = false

    /**
     * Flag whether we should restart after an camera switch.
     */
    private var restartCamera = false

    /**
     * Track current extension type and index.
     */
    private var currentExtensionMode = EXTENSION_MODE_INVALID
    private var currentExtensionIdx = -1
    private val supportedExtensionModes = mutableListOf<Int>()

    private lateinit var textureView: TextureView

    private lateinit var previewSurface: Surface

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(
            surfaceTexture: SurfaceTexture,
            with: Int,
            height: Int
        ) {
            previewSurface = Surface(surfaceTexture)
            openCameraWithExtensionMode(currentCameraId)
        }

        override fun onSurfaceTextureSizeChanged(
            surfaceTexture: SurfaceTexture,
            with: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        }
    }

    private val captureCallbacks = createExtensionCaptureCallback()

    private var restartOnStart = false

    private var activityStopped = false

    private val cameraTaskDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private var imageSaveTerminationFuture: ListenableFuture<Any?> = Futures.immediateFuture(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")
        setContentView(R.layout.activity_camera_extensions)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        backCameraId = getLensFacingCameraId(cameraManager, CameraCharacteristics.LENS_FACING_BACK)
        frontCameraId =
            getLensFacingCameraId(cameraManager, CameraCharacteristics.LENS_FACING_FRONT)

        currentCameraId = if (isCameraSupportExtensions(backCameraId)) {
            backCameraId
        } else if (isCameraSupportExtensions(frontCameraId)) {
            frontCameraId
        } else {
            Toast.makeText(
                this,
                "Can't find camera supporting Camera2 extensions.",
                Toast.LENGTH_SHORT
            ).show()
            closeCameraAndStartActivity(CameraExtensionsActivity::class.java.name)
            return
        }

        updateExtensionInfo()

        setupTextureView()
        enableUiControl(false)
        setupUiControl()
    }

    private fun isCameraSupportExtensions(cameraId: String): Boolean {
        val characteristics = cameraManager.getCameraExtensionCharacteristics(cameraId)
        return characteristics.supportedExtensions.isNotEmpty()
    }

    private fun updateExtensionInfo() {
        Log.d(
            TAG,
            "updateExtensionInfo() - camera Id: $currentCameraId, ${
                getExtensionModeStringFromId(currentExtensionMode)
            }"
        )
        extensionCharacteristics = cameraManager.getCameraExtensionCharacteristics(currentCameraId)
        supportedExtensionModes.clear()
        supportedExtensionModes.addAll(extensionCharacteristics.supportedExtensions)

        cameraSensorRotationDegrees = cameraManager.getCameraCharacteristics(
            currentCameraId)[CameraCharacteristics.SENSOR_ORIENTATION] ?: 0

        currentExtensionIdx = -1

        // Checks whether the original selected extension mode is supported by the new target camera
        if (currentExtensionMode != EXTENSION_MODE_INVALID) {
            for (i in 0..supportedExtensionModes.size) {
                if (supportedExtensionModes[i] == currentExtensionMode) {
                    currentExtensionIdx = i
                    break
                }
            }
        }

        // Switches to the first supported extension mode if the original selected mode is not
        // supported
        if (currentExtensionIdx == -1) {
            currentExtensionIdx = 0
            currentExtensionMode = supportedExtensionModes[0]
        }
    }

    private fun setupTextureView() {
        val viewFinderStub = findViewById<ViewStub>(R.id.viewFinderStub)
        viewFinderStub.layoutResource = R.layout.full_textureview
        textureView = viewFinderStub.inflate() as TextureView
        textureView.surfaceTextureListener = surfaceTextureListener
    }

    private fun enableUiControl(enabled: Boolean) {
        findViewById<Button>(R.id.PhotoToggle).isEnabled = enabled
        findViewById<Button>(R.id.Switch).isEnabled = enabled
        findViewById<Button>(R.id.Picture).isEnabled = enabled
    }

    private fun setupUiControl() {
        val extensionModeToggleButton = findViewById<Button>(R.id.PhotoToggle)
        extensionModeToggleButton.text = getExtensionModeStringFromId(currentExtensionMode)
        extensionModeToggleButton.setOnClickListener {
            enableUiControl(false)
            currentExtensionIdx = (currentExtensionIdx + 1) % supportedExtensionModes.size
            currentExtensionMode = supportedExtensionModes[currentExtensionIdx]
            restartPreview = true
            extensionModeToggleButton.text = getExtensionModeStringFromId(currentExtensionMode)

            closeCaptureSession()
        }

        val cameraSwitchButton = findViewById<Button>(R.id.Switch)
        cameraSwitchButton.setOnClickListener {
            val newCameraId = if (currentCameraId == backCameraId) frontCameraId else backCameraId

            if (!isCameraSupportExtensions(newCameraId)) {
                Toast.makeText(
                    this,
                    "Camera of the other lens facing doesn't support Camera2 extensions.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            enableUiControl(false)
            currentCameraId = newCameraId
            restartCamera = true

            closeCamera()
        }

        val captureButton = findViewById<Button>(R.id.Picture)
        captureButton.setOnClickListener {
            enableUiControl(false)
            takePicture()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart()")
        activityStopped = false
        if (restartOnStart) {
            restartOnStart = false
            openCameraWithExtensionMode(currentCameraId)
        }
    }

    override fun onStop() {
        Log.d(TAG, "onStop()++")
        super.onStop()
        // Needs to close the camera first. Otherwise, the next activity might be failed to open
        // the camera and configure the capture session.
        runBlocking {
            closeCaptureSession().await()
            closeCamera().await()
        }
        restartOnStart = true
        activityStopped = true
        Log.d(TAG, "onStop()--")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()++")
        super.onDestroy()
        previewSurface.release()

        imageSaveTerminationFuture.addListener({ stillImageReader?.close() }, mainExecutor)
        Log.d(TAG, "onDestroy()--")
    }

    private fun closeCamera(): Deferred<Unit> = lifecycleScope.async(cameraTaskDispatcher) {
        Log.d(TAG, "closeCamera()++")
        cameraDevice?.close()
        cameraDevice = null
        Log.d(TAG, "closeCamera()--")
    }

    private fun closeCaptureSession(): Deferred<Unit> = lifecycleScope.async(cameraTaskDispatcher) {
        Log.d(TAG, "closeCaptureSession()++")
        try {
            cameraExtensionSession?.close()
            cameraExtensionSession = null
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
        Log.d(TAG, "closeCaptureSession()--")
    }

    private fun openCameraWithExtensionMode(cameraId: String) =
        lifecycleScope.launch(cameraTaskDispatcher) {
            Log.d(TAG, "openCameraWithExtensionMode()++ cameraId: $cameraId")
            cameraDevice = openCamera(cameraManager, cameraId)
            cameraExtensionSession = openCaptureSession()

            lifecycleScope.launch(Dispatchers.Main) {
                if (activityStopped) {
                    closeCaptureSession()
                    closeCamera()
                }
            }
            Log.d(TAG, "openCameraWithExtensionMode()--")
        }

    /**
     * Opens and returns the camera (as the result of the suspend coroutine)
     */
    @SuppressLint("MissingPermission")
    suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        Log.d(TAG, "openCamera(): $cameraId")
        manager.openCamera(
            cameraId,
            cameraTaskDispatcher.asExecutor(),
            object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) = cont.resume(device)

                override fun onDisconnected(device: CameraDevice) {
                    Log.w(TAG, "Camera $cameraId has been disconnected")
                    finish()
                }

                override fun onClosed(camera: CameraDevice) {
                    Log.d(TAG, "Camera - onClosed: $cameraId")
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (restartCamera) {
                            restartCamera = false
                            updateExtensionInfo()
                            openCameraWithExtensionMode(currentCameraId)
                        }
                    }
                }

                override fun onError(device: CameraDevice, error: Int) {
                    Log.d(TAG, "Camera - onError: $cameraId")
                    val msg = when (error) {
                        ERROR_CAMERA_DEVICE -> "Fatal (device)"
                        ERROR_CAMERA_DISABLED -> "Device policy"
                        ERROR_CAMERA_IN_USE -> "Camera in use"
                        ERROR_CAMERA_SERVICE -> "Fatal (service)"
                        ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                        else -> "Unknown"
                    }
                    val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                    Log.e(TAG, exc.message, exc)
                    cont.resumeWithException(exc)
                }
            })
    }

    /**
     * Opens and returns the extensions session (as the result of the suspend coroutine)
     */
    private suspend fun openCaptureSession(): CameraExtensionSession =
        suspendCancellableCoroutine { cont ->
            Log.d(TAG, "openCaptureSession")
            setupPreview()

            if (stillImageReader != null) {
                val imageReaderToClose = stillImageReader!!
                imageSaveTerminationFuture.addListener(
                    { imageReaderToClose.close() },
                    mainExecutor
                )
            }

            stillImageReader = setupImageReader()

            val outputConfig = ArrayList<OutputConfiguration>()
            outputConfig.add(OutputConfiguration(stillImageReader!!.surface))
            outputConfig.add(OutputConfiguration(previewSurface))
            val extensionConfiguration = ExtensionSessionConfiguration(
                currentExtensionMode, outputConfig,
                cameraTaskDispatcher.asExecutor(), object : CameraExtensionSession.StateCallback() {
                    override fun onClosed(session: CameraExtensionSession) {
                        Log.d(TAG, "CaptureSession - onClosed: $session")

                        lifecycleScope.launch(Dispatchers.Main) {
                            if (restartPreview) {
                                restartPreview = false

                                lifecycleScope.launch(cameraTaskDispatcher) {
                                    cameraExtensionSession = openCaptureSession()
                                }
                            }
                        }
                    }

                    override fun onConfigured(session: CameraExtensionSession) {
                        Log.d(TAG, "CaptureSession - onConfigured: $session")
                        try {
                            val captureBuilder =
                                session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                            captureBuilder.addTarget(previewSurface)
                            session.setRepeatingRequest(
                                captureBuilder.build(),
                                cameraTaskDispatcher.asExecutor(), captureCallbacks
                            )
                            cont.resume(session)
                            runOnUiThread { enableUiControl(true) }
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, e.toString())
                            cont.resumeWithException(
                                RuntimeException("Failed to create capture session.")
                            )
                        }
                    }

                    override fun onConfigureFailed(session: CameraExtensionSession) {
                        Log.e(TAG, "CaptureSession - onConfigureFailed: $session")
                        cont.resumeWithException(
                            RuntimeException("Configure failed when creating capture session.")
                        )
                    }
                }
            )
            try {
                cameraDevice!!.createExtensionSession(extensionConfiguration)
            } catch (e: CameraAccessException) {
                Log.e(TAG, e.toString())
                cont.resumeWithException(RuntimeException("Failed to create capture session."))
            }
        }

    @Suppress("DEPRECATION") /* defaultDisplay */
    private fun setupPreview() {
        if (!textureView.isAvailable) {
            Toast.makeText(
                this, "TextureView is invalid!!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        val previewResolution = pickPreviewResolution(
            cameraManager,
            currentCameraId,
            resources.displayMetrics,
            currentExtensionMode
        )

        if (previewResolution == null) {
            Toast.makeText(
                this,
                "Invalid preview extension sizes!.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        textureView.surfaceTexture?.setDefaultBufferSize(
            previewResolution.width,
            previewResolution.height
        )
        transformPreview(textureView, previewResolution, windowManager.defaultDisplay.rotation)
    }

    private fun setupImageReader(): ImageReader {
        val (size, format) = pickStillImageResolution(
            extensionCharacteristics,
            currentExtensionMode
        )

        return ImageReader.newInstance(size.width, size.height, format, 1)
    }

    /**
     * Takes a picture.
     */
    private fun takePicture() = lifecycleScope.launch(cameraTaskDispatcher) {
        Preconditions.checkState(
            cameraExtensionSession != null,
            "take picture button is only enabled when session is configured successfully"
        )
        val session = cameraExtensionSession!!

        var takePictureCompleter: Completer<Any?>? = null

        imageSaveTerminationFuture = CallbackToFutureAdapter.getFuture<Any?> {
            takePictureCompleter = it
            "imageSaveTerminationFuture"
        }

        stillImageReader!!.setOnImageAvailableListener(
            { reader: ImageReader ->
                lifecycleScope.launch(cameraTaskDispatcher) {
                    acquireImageAndSave(reader)
                    stillImageReader!!.setOnImageAvailableListener(null, null)
                    takePictureCompleter?.set(null)
                    lifecycleScope.launch(Dispatchers.Main) {
                        enableUiControl(true)
                    }
                }
            }, Handler(Looper.getMainLooper())
        )

        val captureBuilder = session.device.createCaptureRequest(
            CameraDevice.TEMPLATE_STILL_CAPTURE
        )
        captureBuilder.addTarget(stillImageReader!!.surface)

        session.capture(
            captureBuilder.build(),
            cameraTaskDispatcher.asExecutor(),
            object : CameraExtensionSession.ExtensionCaptureCallback() {
                override fun onCaptureFailed(
                    session: CameraExtensionSession,
                    request: CaptureRequest
                ) {
                    takePictureCompleter?.set(null)
                    Log.e(TAG, "Failed to take picture.")
                }

                override fun onCaptureSequenceCompleted(
                    session: CameraExtensionSession,
                    sequenceId: Int
                ) {
                    Log.v(TAG, "onCaptureProcessSequenceCompleted: $sequenceId")
                }
            }
        )
    }

    /**
     * Acquires the latest image from the image reader and save it to the Pictures folder
     */
    private fun acquireImageAndSave(imageReader: ImageReader) {
        try {
            val formatter: Format =
                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            val fileName =
                "[${formatter.format(Calendar.getInstance().time)}][Camera2]${
                    getExtensionModeStringFromId(currentExtensionMode)
                }"

            val rotationDegrees = calculateRelativeImageRotationDegrees(
                (getDisplayRotationDegrees(display!!.rotation)),
                cameraSensorRotationDegrees,
                currentCameraId == backCameraId
            )

            imageReader.acquireLatestImage().let { image ->
                val uri = FileUtil.saveImage(
                    image,
                    fileName,
                    ".jpg",
                    "Pictures/ExtensionsPictures",
                    contentResolver,
                    rotationDegrees
                )

                image.close()

                val msg = if (uri != null) {
                    "Saved image to $fileName.jpg"
                } else {
                    "Failed to save image."
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@Camera2ExtensionsActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu_camera2_extensions_activity, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_camerax_extensions -> {
                closeCameraAndStartActivity(CameraExtensionsActivity::class.java.name)
                return true
            }
            R.id.menu_validation_tool -> {
                closeCameraAndStartActivity(CameraValidationResultActivity::class.java.name)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun closeCameraAndStartActivity(className: String) {
        // Needs to close the camera first. Otherwise, the next activity might be failed to open
        // the camera and configure the capture session.
        runBlocking {
            closeCaptureSession().await()
            closeCamera().await()
        }

        val intent = Intent()
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setClassName(this, className)
        startActivity(intent)
    }
}