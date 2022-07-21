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
import android.content.ContentResolver
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
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewStub
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.getExtensionModeStringFromId
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.getLensFacingCameraId
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.pickPreviewResolution
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.pickStillImageResolution
import androidx.camera.integration.extensions.utils.FileUtil
import androidx.camera.integration.extensions.utils.TransformUtil.calculateRelativeImageRotationDegrees
import androidx.camera.integration.extensions.utils.TransformUtil.surfaceRotationToRotationDegrees
import androidx.camera.integration.extensions.utils.TransformUtil.transformTextureView
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer
import androidx.core.util.Preconditions
import androidx.lifecycle.lifecycleScope
import androidx.test.espresso.idling.CountingIdlingResource
import com.google.common.util.concurrent.ListenableFuture
import java.text.Format
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
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
private const val FRAMES_UNTIL_VIEW_IS_READY = 10

// Launch the activity with the specified camera id.
@VisibleForTesting
const val INTENT_EXTRA_CAMERA_ID = "camera_id"

// Launch the activity with the specified extension mode.
@VisibleForTesting
const val INTENT_EXTRA_EXTENSION_MODE = "extension_mode"

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

    private lateinit var containerView: View

    private lateinit var textureView: TextureView

    private lateinit var previewSurface: Surface

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(
            surfaceTexture: SurfaceTexture,
            with: Int,
            height: Int
        ) {
            previewSurface = Surface(surfaceTexture)
            setupAndStartPreview(currentCameraId, currentExtensionMode)
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
            if (captureProcessStartedIdlingResource.isIdleNow &&
                receivedPreviewFrameCount.getAndIncrement() >= FRAMES_UNTIL_VIEW_IS_READY &&
                !previewIdlingResource.isIdleNow
            ) {
                previewIdlingResource.decrement()
            }
        }
    }

    private val captureCallbacks = object : CameraExtensionSession.ExtensionCaptureCallback() {
        override fun onCaptureProcessStarted(
            session: CameraExtensionSession,
            request: CaptureRequest
        ) {
            if (receivedCaptureProcessStartedCount.getAndIncrement() >=
                FRAMES_UNTIL_VIEW_IS_READY && !captureProcessStartedIdlingResource.isIdleNow
            ) {
                captureProcessStartedIdlingResource.decrement()
            }
        }

        override fun onCaptureFailed(session: CameraExtensionSession, request: CaptureRequest) {
            Log.e(TAG, "onCaptureFailed!!")
        }
    }

    private var restartOnStart = false

    private var activityStopped = false

    private val cameraTaskDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private var imageSaveTerminationFuture: ListenableFuture<Any?> = Futures.immediateFuture(null)

    /**
     * Used to wait for the capture session is configured.
     */
    private val captureSessionConfiguredIdlingResource =
        CountingIdlingResource("captureSessionConfigured").apply { increment() }
    /**
     * Used to wait for the ExtensionCaptureCallback#onCaptureProcessStarted is called which means
     * an image is captured and extension processing is triggered.
     */
    private val captureProcessStartedIdlingResource =
        CountingIdlingResource("captureProcessStarted").apply { increment() }

    /**
     * Used to wait for the preview is ready. This will become idle after
     * captureProcessStartedIdlingResource becomes idle and
     * [SurfaceTextureListener#onSurfaceTextureUpdated()] is also called. It means that there has
     * been images captured to trigger the extension processing and the preview's SurfaceTexture is
     * also updated by [SurfaceTexture#updateTexImage()] calls.
     */
    private val previewIdlingResource = CountingIdlingResource("preview").apply { increment() }

    /**
     * Used to trigger a picture taking action and waits for the image being saved.
     */
    private val imageSavedIdlingResource = CountingIdlingResource("imageSaved")

    private val receivedCaptureProcessStartedCount: AtomicLong = AtomicLong(0)
    private val receivedPreviewFrameCount: AtomicLong = AtomicLong(0)

    private lateinit var sessionImageUriSet: SessionMediaUriSet

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

        sessionImageUriSet = SessionMediaUriSet(contentResolver)

        // Gets params from extra bundle
        intent.extras?.let { bundle ->
            currentCameraId = bundle.getString(INTENT_EXTRA_CAMERA_ID, currentCameraId)
            currentExtensionMode = bundle.getInt(INTENT_EXTRA_EXTENSION_MODE, currentExtensionMode)
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
        containerView = viewFinderStub.inflate()
        textureView = containerView.findViewById(R.id.textureView)
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

            closeCaptureSessionAsync()
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

            closeCameraAsync()
        }

        val captureButton = findViewById<Button>(R.id.Picture)
        captureButton.setOnClickListener {
            enableUiControl(false)
            resetImageSavedIdlingResource()
            takePicture()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart()")
        activityStopped = false
        if (restartOnStart) {
            restartOnStart = false
            setupAndStartPreview(currentCameraId, currentExtensionMode)
        }
    }

    override fun onStop() {
        Log.d(TAG, "onStop()++")
        super.onStop()
        // Needs to close the camera first. Otherwise, the next activity might be failed to open
        // the camera and configure the capture session.
        runBlocking {
            closeCaptureSessionAsync().await()
            closeCameraAsync().await()
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

    private fun closeCameraAsync(): Deferred<Unit> = lifecycleScope.async(cameraTaskDispatcher) {
        Log.d(TAG, "closeCamera()++")
        cameraDevice?.close()
        cameraDevice = null
        Log.d(TAG, "closeCamera()--")
    }

    private fun closeCaptureSessionAsync(): Deferred<Unit> =
        lifecycleScope.async(cameraTaskDispatcher) {
            Log.d(TAG, "closeCaptureSession()++")
            resetCaptureSessionConfiguredIdlingResource()
            try {
                cameraExtensionSession?.close()
                cameraExtensionSession = null
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
            Log.d(TAG, "closeCaptureSession()--")
        }

    /**
     * Sets up the UI layout settings for the specified camera and extension mode. And then,
     * triggers to open the camera and capture session to start the preview with the extension mode
     * enabled.
     */
    @Suppress("DEPRECATION") /* defaultDisplay */
    private fun setupAndStartPreview(cameraId: String, extensionMode: Int) {
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
            cameraId,
            resources.displayMetrics,
            extensionMode
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

        Log.d(TAG, "Set default buffer size to previewResolution: $previewResolution")

        textureView.surfaceTexture?.setDefaultBufferSize(
            previewResolution.width,
            previewResolution.height
        )

        textureView.layoutParams =
            FrameLayout.LayoutParams(previewResolution.width, previewResolution.height)

        val containerViewSize = Size(containerView.width, containerView.height)

        val lensFacing =
            cameraManager.getCameraCharacteristics(cameraId)[CameraCharacteristics.LENS_FACING]

        transformTextureView(
            textureView,
            containerViewSize,
            previewResolution,
            windowManager.defaultDisplay.rotation,
            cameraSensorRotationDegrees,
            lensFacing == CameraCharacteristics.LENS_FACING_BACK
        )

        startPreview(cameraId, extensionMode)
    }

    /**
     * Opens the camera and capture session to start the preview with the extension mode enabled.
     */
    private fun startPreview(cameraId: String, extensionMode: Int) =
        lifecycleScope.launch(cameraTaskDispatcher) {
            Log.d(TAG, "openCameraWithExtensionMode()++ cameraId: $cameraId")
            if (cameraDevice == null || cameraDevice!!.id != cameraId) {
                cameraDevice = openCamera(cameraManager, cameraId)
            }
            cameraExtensionSession = openCaptureSession(extensionMode)

            lifecycleScope.launch(Dispatchers.Main) {
                if (activityStopped) {
                    closeCaptureSessionAsync()
                    closeCameraAsync()
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
                            setupAndStartPreview(currentCameraId, currentExtensionMode)
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
    private suspend fun openCaptureSession(extensionMode: Int): CameraExtensionSession =
        suspendCancellableCoroutine { cont ->
            Log.d(TAG, "openCaptureSession")

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
                extensionMode, outputConfig,
                cameraTaskDispatcher.asExecutor(), object : CameraExtensionSession.StateCallback() {
                    override fun onClosed(session: CameraExtensionSession) {
                        Log.d(TAG, "CaptureSession - onClosed: $session")

                        lifecycleScope.launch(Dispatchers.Main) {
                            if (restartPreview) {
                                restartPreview = false

                                val newExtensionMode = currentExtensionMode

                                lifecycleScope.launch(cameraTaskDispatcher) {
                                    cameraExtensionSession =
                                        openCaptureSession(newExtensionMode)
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
                            runOnUiThread {
                                enableUiControl(true)
                                if (!captureSessionConfiguredIdlingResource.isIdleNow) {
                                    captureSessionConfiguredIdlingResource.decrement()
                                }
                            }
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

    private fun setupImageReader(): ImageReader {
        val (size, format) = pickStillImageResolution(
            extensionCharacteristics,
            currentExtensionMode
        )

        Log.d(TAG, "Setup image reader - size: $size, format: $format")

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
                    acquireImageAndSave(reader)?.let { sessionImageUriSet.add(it) }

                    stillImageReader!!.setOnImageAvailableListener(null, null)
                    takePictureCompleter?.set(null)

                    if (!imageSavedIdlingResource.isIdleNow) {
                        imageSavedIdlingResource.decrement()
                    }

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
    private fun acquireImageAndSave(imageReader: ImageReader): Uri? {
        var uri: Uri? = null
        try {
            val formatter: Format =
                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            val fileName =
                "[${formatter.format(Calendar.getInstance().time)}][Camera2]${
                    getExtensionModeStringFromId(currentExtensionMode)
                }"

            val rotationDegrees = calculateRelativeImageRotationDegrees(
                (surfaceRotationToRotationDegrees(display!!.rotation)),
                cameraSensorRotationDegrees,
                currentCameraId == backCameraId
            )

            imageReader.acquireLatestImage().let { image ->
                uri = FileUtil.saveImage(
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

        return uri
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
            closeCaptureSessionAsync().await()
            closeCameraAsync().await()
        }

        val intent = Intent()
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setClassName(this, className)
        startActivity(intent)
    }

    @VisibleForTesting
    fun getCaptureSessionConfiguredIdlingResource(): CountingIdlingResource =
        captureSessionConfiguredIdlingResource

    @VisibleForTesting
    fun getPreviewIdlingResource(): CountingIdlingResource = previewIdlingResource

    @VisibleForTesting
    fun getImageSavedIdlingResource(): CountingIdlingResource = imageSavedIdlingResource

    private fun resetCaptureSessionConfiguredIdlingResource() {
        if (captureSessionConfiguredIdlingResource.isIdleNow) {
            captureSessionConfiguredIdlingResource.increment()
        }
    }

    @VisibleForTesting
    fun resetPreviewIdlingResource() {
        receivedCaptureProcessStartedCount.set(0)
        receivedPreviewFrameCount.set(0)

        if (captureProcessStartedIdlingResource.isIdleNow) {
            captureProcessStartedIdlingResource.increment()
        }

        if (previewIdlingResource.isIdleNow) {
            previewIdlingResource.increment()
        }
    }

    private fun resetImageSavedIdlingResource() {
        if (imageSavedIdlingResource.isIdleNow) {
            imageSavedIdlingResource.increment()
        }
    }

    @VisibleForTesting
    fun deleteSessionImages() {
        sessionImageUriSet.deleteAllUris()
    }

    private class SessionMediaUriSet constructor(val contentResolver: ContentResolver) {
        private val mSessionMediaUris: MutableSet<Uri> = mutableSetOf()

        fun add(uri: Uri) {
            synchronized(mSessionMediaUris) {
                mSessionMediaUris.add(uri)
            }
        }

        fun deleteAllUris() {
            synchronized(mSessionMediaUris) {
                val it =
                    mSessionMediaUris.iterator()
                while (it.hasNext()) {
                    contentResolver.delete(it.next(), null, null)
                    it.remove()
                }
            }
        }
    }
}