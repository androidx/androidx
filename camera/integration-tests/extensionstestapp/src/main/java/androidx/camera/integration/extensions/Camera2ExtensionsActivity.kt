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
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CameraExtensionSession.ExtensionCaptureCallback
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
import android.hardware.camera2.CameraMetadata.CONTROL_AF_TRIGGER_IDLE
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE
import android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE_ON
import android.hardware.camera2.CaptureRequest.CONTROL_AE_REGIONS
import android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE
import android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE_AUTO
import android.hardware.camera2.CaptureRequest.CONTROL_AF_REGIONS
import android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER
import android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER_START
import android.hardware.camera2.CaptureRequest.CONTROL_AWB_REGIONS
import android.hardware.camera2.CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.ExtensionSessionConfiguration
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewStub
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.integration.extensions.ExtensionTestType.TEST_TYPE_CAMERA2_EXTENSION_STREAM_CONFIG_LATENCY
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_CAMERA_ID
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_ERROR_CODE
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_EXTENSION_MODE
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_IMAGE_ROTATION_DEGREES
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_IMAGE_URI
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_REQUEST_CODE
import androidx.camera.integration.extensions.TapToFocusDetector.CameraInfo
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_FAILED
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_NOT_TESTED
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_PASSED
import androidx.camera.integration.extensions.ValidationErrorCode.ERROR_CODE_EXTENSION_MODE_NOT_SUPPORT
import androidx.camera.integration.extensions.ValidationErrorCode.ERROR_CODE_NONE
import androidx.camera.integration.extensions.ValidationErrorCode.ERROR_CODE_SAVE_IMAGE_FAILED
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.getCamera2ExtensionModeStringFromId
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.getLensFacingCameraId
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.isCamera2ExtensionModeSupported
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.pickPreviewResolution
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.pickStillImageResolution
import androidx.camera.integration.extensions.utils.FileUtil
import androidx.camera.integration.extensions.utils.TransformUtil.calculateRelativeImageRotationDegrees
import androidx.camera.integration.extensions.utils.TransformUtil.surfaceRotationToRotationDegrees
import androidx.camera.integration.extensions.utils.TransformUtil.transformTextureView
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity
import androidx.camera.integration.extensions.validation.TestResults
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer
import androidx.core.util.Preconditions
import androidx.test.espresso.idling.CountingIdlingResource
import com.google.common.util.concurrent.ListenableFuture
import java.text.Format
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val TAG = "Camera2ExtensionsAct~"
private const val FRAMES_UNTIL_VIEW_IS_READY = 10
private const val KEY_CAMERA2_LATENCY = "camera2"
private const val KEY_CAMERA_EXTENSION_LATENCY = "camera_extension"
private const val MAX_EXTENSION_LATENCY_MILLIS = 800

// The states of the camera/capture session open/close flow
//  - STATE_CAMERA_CLOSED -> Open camera first. Open the capture session when camera is opened.
//  - STATE_CAPTURE_SESSION_CONFIGURED -> Close the capture session and camera first. If the target
//    camera is the same, directly open the capture session with the new target extension mode.
//    Otherwise, reopen the camera and then switch to the new extension mode
//  - Others -> Only update the target camera and extension mode info. When receiving camera
//    onOpened or capture session onConfigured events, reopen to the new target camera and
//    extension mode if it is mismatched.
private const val STATE_CAMERA_CLOSED = 0
private const val STATE_CAMERA_OPENING = 1
private const val STATE_CAMERA_OPENED = 2
private const val STATE_CAPTURE_SESSION_OPENING = 3
private const val STATE_CAPTURE_SESSION_CONFIGURED = 4
private const val STATE_CAPTURE_SESSION_CLOSING = 5
private const val STATE_CAPTURE_SESSION_CLOSED = 6
private const val STATE_CAMERA_CLOSING = 7

@RequiresApi(31)
class Camera2ExtensionsActivity : AppCompatActivity() {

    // ===============================================================
    // Fields that will be accessed on the camera thread
    // ===============================================================

    private var currentState = STATE_CAMERA_CLOSED

    /** A reference to the opened [CameraDevice]. */
    private var cameraDevice: CameraDevice? = null

    /**
     * The current camera capture session. Use Any type to store it because it might be either a
     * CameraCaptureSession instance if current is in normal mode, or, it might be a
     * CameraExtensionSession instance if current is in Camera2 extension mode.
     */
    private var cameraCaptureSession: Any? = null

    private val focusMeteringControl = FocusMeteringControl(::startAfTrigger, ::cancelAfTrigger)
    private var meteringRectangles: Array<MeteringRectangle?> = EMPTY_RECTANGLES

    // ===============================================================
    // Fields that will be accessed on the camera thread
    // ===============================================================

    private lateinit var backCameraId: String
    private lateinit var frontCameraId: String

    private var activityStopped = false
    private var currentCameraId = "0"
    private var cameraSensorRotationDegrees = 0

    /** Camera extension characteristics for the current camera device. */
    private lateinit var extensionCharacteristics: CameraExtensionCharacteristics

    /** Track current extension type and index. */
    private var currentExtensionMode = EXTENSION_MODE_NONE
    private var currentExtensionIdx = 0
    private val supportedExtensionModes = mutableListOf<Int>()
    private var extensionModeEnabled = false

    private lateinit var tapToFocusDetector: TapToFocusDetector

    // ===============================================================
    // Fields that will be accessed under synchronization protection
    // ===============================================================
    private val lock = Object()
    @GuardedBy("lock")
    private var captureSessionClosedDeferred: CompletableDeferred<Unit> =
        CompletableDeferred<Unit>().apply { complete(Unit) }

    private lateinit var cameraManager: CameraManager

    /**
     * Tracks the stream configuration latency of camera extension and camera2. Each key is
     * associated with a list of durations. This allows clients to run multiple invocations to
     * measure the min, avg, and max latency.
     */
    private val streamConfigurationLatency =
        mutableMapOf<String, MutableList<Long>>(
            KEY_CAMERA2_LATENCY to mutableListOf(),
            KEY_CAMERA_EXTENSION_LATENCY to mutableListOf(),
        )

    /** Still capture image reader */
    private var stillImageReader: ImageReader? = null

    private lateinit var containerView: View

    private lateinit var textureView: TextureView
    private lateinit var videoStabilizationToggleView: Switch
    private lateinit var videoStabilizationModeView: TextView

    private var previewSurface: Surface? = null

    private val surfaceTextureListener =
        object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                with: Int,
                height: Int,
            ) {
                previewSurface = Surface(surfaceTexture)
                setupAndStartPreview()
            }

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: SurfaceTexture,
                with: Int,
                height: Int,
            ) {}

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                // Will release the surface texture after the camera is closed
                return false
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                if (
                    captureProcessStartedIdlingResource.isIdleNow &&
                        receivedPreviewFrameCount.getAndIncrement() >= FRAMES_UNTIL_VIEW_IS_READY &&
                        !previewIdlingResource.isIdleNow
                ) {
                    previewIdlingResource.decrement()
                }

                if (measureStreamConfigurationLatency && lastSurfaceTextureTimestampNanos != 0L) {
                    val duration =
                        TimeUnit.NANOSECONDS.toMillis(
                            surfaceTexture.timestamp - lastSurfaceTextureTimestampNanos
                        )
                    if (duration > 150) {
                        if (!extensionModeEnabled) {
                            streamConfigurationLatency[KEY_CAMERA2_LATENCY]?.add(duration)
                        } else {
                            streamConfigurationLatency[KEY_CAMERA_EXTENSION_LATENCY]?.add(duration)
                        }
                        measureStreamConfigurationLatency = false
                    }
                }
                lastSurfaceTextureTimestampNanos = surfaceTexture.timestamp
            }
        }

    private val captureCallbackExtensionMode =
        object : ExtensionCaptureCallback() {
            override fun onCaptureProcessStarted(
                session: CameraExtensionSession,
                request: CaptureRequest,
            ) {
                handleCaptureStartedEvent()
            }

            override fun onCaptureFailed(session: CameraExtensionSession, request: CaptureRequest) {
                Log.e(TAG, "onCaptureFailed!!")
            }
        }

    private val captureCallbackNormalMode =
        object : CaptureCallback() {
            override fun onCaptureStarted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                timestamp: Long,
                frameNumber: Long,
            ) {
                handleCaptureStartedEvent()
            }
        }

    private fun handleCaptureStartedEvent() {
        checkRunOnCameraThread()
        if (
            receivedCaptureProcessStartedCount.getAndIncrement() >= FRAMES_UNTIL_VIEW_IS_READY &&
                !captureProcessStartedIdlingResource.isIdleNow
        ) {
            captureProcessStartedIdlingResource.decrement()
        }
    }

    private val comboCaptureCallbackExtensionMode =
        ComboCaptureCallbackExtensionMode().apply {
            addCaptureCallback(captureCallbackExtensionMode)
        }

    private val comboCaptureCallbackNormalMode =
        ComboCaptureCallbackNormalMode().apply { addCaptureCallback(captureCallbackNormalMode) }

    private val cameraTaskDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private lateinit var cameraThread: Thread

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var imageSaveTerminationFuture: ListenableFuture<Any?> = Futures.immediateFuture(null)

    /**
     * Tracks the last timestamp of a surface texture rendered on to the TextureView. This is used
     * to measure the configuration latency from the last preview frame received from the previous
     * camera session until the first preview frame received of the new camera session.
     */
    private var lastSurfaceTextureTimestampNanos: Long = 0

    /**
     * A flag which represents when to measure the stream configuration latency. This is triggered
     * when the user toggles the camera extension mode.
     */
    private var measureStreamConfigurationLatency: Boolean = true

    /** Used to wait for the camera is closed. */
    private val cameraClosedIdlingResource = CountingIdlingResource("cameraClosed")

    /** Used to wait for the capture session is configured. */
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

    /** Used to trigger a picture taking action and waits for the image being saved. */
    private val imageSavedIdlingResource = CountingIdlingResource("imageSaved")

    private val receivedCaptureProcessStartedCount: AtomicLong = AtomicLong(0)
    private val receivedPreviewFrameCount: AtomicLong = AtomicLong(0)

    private lateinit var sessionImageUriSet: SessionMediaUriSet

    /** Stores the request code passed from the caller activity. */
    private var requestCode = -1

    /**
     * This will be true if the activity is called by other activity to request capturing an image.
     */
    private var isRequestMode = false

    /** The result intent that saves the image capture request results. */
    private lateinit var result: Intent

    /** A [HandlerThread] used for normal mode camera capture operations */
    private val normalModeCaptureThread = HandlerThread("CameraThread").apply { start() }

    /** [Handler] corresponding to [normalModeCaptureThread] */
    private val normalModeCaptureHandler = Handler(normalModeCaptureThread.looper)

    /** A [HandlerThread] used for saving image files */
    private val imageSaverThread = HandlerThread("ImageSaver").apply { start() }

    /** [Handler] corresponding to [normalModeCaptureThread] */
    private val imageSaverHandler = Handler(imageSaverThread.looper)

    /**
     * A toast is shown when an extension is enabled or disabled. Tracking this allows cancelling
     * the toast before showing a new one. This is specifically for scenarios where toggling an
     * extension quickly requires cancelling the last toast before showing the new one.
     */
    private var toast: Toast? = null

    private var zoomRatio: Float = 1.0f

    /**
     * Define a scale gesture detector to respond to pinch events and call setZoom on
     * Camera.Parameters.
     */
    private val scaleGestureListener =
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = hasZoomSupport()

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                // Set the zoom level
                startZoom(detector.scaleFactor)
                return true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")
        setContentView(R.layout.activity_camera_extensions)

        // Retrieves the cameraThread that will be used to check whether the code is correctly
        // executed on the camera thread.
        runBlocking {
            coroutineScope
                .launch(cameraTaskDispatcher) { cameraThread = Thread.currentThread() }
                .join()
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        backCameraId = getLensFacingCameraId(cameraManager, CameraCharacteristics.LENS_FACING_BACK)
        frontCameraId =
            getLensFacingCameraId(cameraManager, CameraCharacteristics.LENS_FACING_FRONT)

        currentCameraId =
            if (isCameraSupportExtensions(backCameraId)) {
                backCameraId
            } else if (isCameraSupportExtensions(frontCameraId)) {
                frontCameraId
            } else {
                Toast.makeText(
                        this,
                        "Can't find camera supporting Camera2 extensions.",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                switchActivity(CameraExtensionsActivity::class.java.name)
                return
            }

        sessionImageUriSet = SessionMediaUriSet(contentResolver)

        // Gets params from extra bundle
        intent.extras?.let { bundle ->
            currentCameraId = bundle.getString(INTENT_EXTRA_KEY_CAMERA_ID, currentCameraId)
            currentExtensionMode =
                bundle.getInt(INTENT_EXTRA_KEY_EXTENSION_MODE, currentExtensionMode)
            extensionModeEnabled = currentExtensionMode != EXTENSION_MODE_NONE

            requestCode = bundle.getInt(INTENT_EXTRA_KEY_REQUEST_CODE, -1)
            isRequestMode = requestCode != -1

            if (isRequestMode) {
                setupForRequestMode()
            }
        }

        updateExtensionInfo()
        setupTextureView()
        enableUiControl(false)
        setupUiControl()
        setupVideoStabilizationModeView()
        enableZoomAndTapToFocusGesture()
    }

    private fun setupForRequestMode() {
        checkRunOnMainThread()
        result = Intent()
        result.putExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, currentExtensionMode)
        result.putExtra(INTENT_EXTRA_KEY_ERROR_CODE, ERROR_CODE_NONE)
        setResult(requestCode, result)

        if (!isCamera2ExtensionModeSupported(this, currentCameraId, currentExtensionMode)) {
            result.putExtra(INTENT_EXTRA_KEY_ERROR_CODE, ERROR_CODE_EXTENSION_MODE_NOT_SUPPORT)
            finish()
            return
        }

        val lensFacing =
            cameraManager
                .getCameraCharacteristics(currentCameraId)[CameraCharacteristics.LENS_FACING]

        supportActionBar?.title = resources.getString(R.string.camera2_extensions_validator)
        supportActionBar!!.subtitle =
            "Camera $currentCameraId [${getLensFacingString(lensFacing!!)}][${
                getCamera2ExtensionModeStringFromId(currentExtensionMode)
            }]"

        findViewById<Button>(R.id.PhotoToggle).visibility = View.INVISIBLE
        findViewById<Button>(R.id.Switch).visibility = View.INVISIBLE

        setExtensionToggleButtonResource()
        findViewById<ImageButton>(R.id.ExtensionToggle).apply {
            visibility = View.VISIBLE
            setOnClickListener {
                measureStreamConfigurationLatency = true
                extensionModeEnabled = !extensionModeEnabled

                // Close current capture session to re-create the new capture session for the
                // new settings
                closeCaptureSession()
                setExtensionToggleButtonResource()

                val newToast =
                    if (extensionModeEnabled) {
                        Toast.makeText(
                            this@Camera2ExtensionsActivity,
                            "Extension is enabled!",
                            Toast.LENGTH_SHORT,
                        )
                    } else {
                        Toast.makeText(
                            this@Camera2ExtensionsActivity,
                            "Extension is disabled!",
                            Toast.LENGTH_SHORT,
                        )
                    }
                toast?.cancel()
                newToast.show()
                toast = newToast
            }
        }
    }

    @Suppress("DEPRECATION") // EXTENSION_BEAUTY
    private fun setExtensionToggleButtonResource() {
        checkRunOnMainThread()
        val extensionToggleButton: ImageButton = findViewById(R.id.ExtensionToggle)

        if (!extensionModeEnabled) {
            extensionToggleButton.setImageResource(R.drawable.outline_block)
            return
        }

        val resourceId =
            when (currentExtensionMode) {
                CameraExtensionCharacteristics.EXTENSION_HDR -> R.drawable.outline_hdr_on
                CameraExtensionCharacteristics.EXTENSION_BOKEH -> R.drawable.outline_portrait
                CameraExtensionCharacteristics.EXTENSION_NIGHT -> R.drawable.outline_bedtime
                CameraExtensionCharacteristics.EXTENSION_BEAUTY ->
                    R.drawable.outline_face_retouching_natural
                CameraExtensionCharacteristics.EXTENSION_AUTOMATIC ->
                    R.drawable.outline_auto_awesome
                else -> throw IllegalArgumentException("Invalid extension mode!")
            }

        extensionToggleButton.setImageResource(resourceId)
    }

    private fun getLensFacingString(lensFacing: Int) =
        when (lensFacing) {
            CameraMetadata.LENS_FACING_BACK -> "BACK"
            CameraMetadata.LENS_FACING_FRONT -> "FRONT"
            CameraMetadata.LENS_FACING_EXTERNAL -> "EXTERNAL"
            else -> throw IllegalArgumentException("Invalid lens facing!!")
        }

    private fun isCameraSupportExtensions(cameraId: String): Boolean {
        val characteristics = cameraManager.getCameraExtensionCharacteristics(cameraId)
        return characteristics.supportedExtensions.isNotEmpty()
    }

    private fun updateExtensionInfo() {
        checkRunOnMainThread()
        Log.d(
            TAG,
            "updateExtensionInfo() - camera Id: $currentCameraId, current extension mode: " +
                "$currentExtensionMode",
        )
        extensionCharacteristics = cameraManager.getCameraExtensionCharacteristics(currentCameraId)
        supportedExtensionModes.clear()
        supportedExtensionModes.add(EXTENSION_MODE_NONE)
        supportedExtensionModes.addAll(extensionCharacteristics.supportedExtensions)

        cameraSensorRotationDegrees =
            cameraManager
                .getCameraCharacteristics(currentCameraId)[CameraCharacteristics.SENSOR_ORIENTATION]
                ?: 0

        currentExtensionIdx = getExtensionModeIndex(currentExtensionMode)
        extensionModeEnabled = currentExtensionMode != EXTENSION_MODE_NONE
    }

    private fun getExtensionModeIndex(extensionMode: Int): Int {
        checkRunOnMainThread()
        supportedExtensionModes.forEachIndexed { index, mode ->
            if (extensionMode == mode) {
                return index
            }
        }
        // This should happen only when switching camera. The new target camera might not support
        // the original extensions mode.
        return -1
    }

    private fun setupTextureView() {
        val viewFinderStub = findViewById<ViewStub>(R.id.viewFinderStub)
        viewFinderStub.layoutResource = R.layout.full_textureview
        containerView = viewFinderStub.inflate()
        textureView = containerView.findViewById(R.id.textureView)
        textureView.surfaceTextureListener = surfaceTextureListener
    }

    private fun setupVideoStabilizationModeView() {
        videoStabilizationToggleView = findViewById(R.id.videoStabilizationToggle)
        videoStabilizationModeView = findViewById(R.id.videoStabilizationMode)

        val availableModes =
            cameraManager
                .getCameraCharacteristics(currentCameraId)
                .get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                ?: intArrayOf()

        if (
            availableModes.contains(
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
            )
        ) {
            videoStabilizationToggleView.visibility = View.VISIBLE
            videoStabilizationModeView.visibility = View.VISIBLE

            videoStabilizationToggleView.setOnCheckedChangeListener { _, isChecked ->
                val mode = if (isChecked) "Preview" else "Off"
                videoStabilizationModeView.text = "Video Stabilization Mode: $mode"

                setRepeatingRequest()
            }
        } else {
            videoStabilizationToggleView.visibility = View.GONE
            videoStabilizationModeView.visibility = View.GONE
        }
    }

    private fun enableUiControl(enabled: Boolean) {
        findViewById<Button>(R.id.PhotoToggle).isEnabled = enabled
        findViewById<Button>(R.id.Switch).isEnabled = enabled
        findViewById<Button>(R.id.Picture).isEnabled = enabled
    }

    private fun enableZoomAndTapToFocusGesture() {
        val scaleGestureDetector = ScaleGestureDetector(this, scaleGestureListener)
        textureView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            tapToFocusDetector.onTouchEvent(event)
            true
        }
    }

    private fun setupUiControl() {
        checkRunOnMainThread()
        val extensionModeToggleButton = findViewById<Button>(R.id.PhotoToggle)
        extensionModeToggleButton.text = getCamera2ExtensionModeStringFromId(currentExtensionMode)
        extensionModeToggleButton.setOnClickListener {
            enableUiControl(false)
            currentExtensionIdx = (currentExtensionIdx + 1) % supportedExtensionModes.size
            currentExtensionMode = supportedExtensionModes[currentExtensionIdx]
            extensionModeEnabled = currentExtensionMode != EXTENSION_MODE_NONE
            extensionModeToggleButton.text =
                getCamera2ExtensionModeStringFromId(currentExtensionMode)
            closeCaptureSession()
        }

        val cameraSwitchButton = findViewById<Button>(R.id.Switch)
        cameraSwitchButton.setOnClickListener { switchCamera() }

        val captureButton = findViewById<Button>(R.id.Picture)
        captureButton.setOnClickListener {
            enableUiControl(false)
            resetImageSavedIdlingResource()
            takePicture()
        }
    }

    private fun determineNextStepOnUiThread(
        state: Int,
        cameraId: String,
        extensionMode: Int? = null,
    ) {
        coroutineScope.launch(Dispatchers.Main) {
            when (state) {
                STATE_CAMERA_OPENED -> {
                    if (activityStopped || currentCameraId != cameraId) {
                        closeCamera()
                    } else {
                        updatePreviewSize()
                        openCaptureSession(currentExtensionMode, extensionModeEnabled)
                    }
                }
                STATE_CAMERA_CLOSED -> {
                    if (!activityStopped) {
                        openCamera(cameraManager, currentCameraId)
                    }
                }
                STATE_CAPTURE_SESSION_CONFIGURED -> {
                    if (
                        activityStopped ||
                            (extensionModeEnabled && currentExtensionMode != extensionMode) ||
                            (!extensionModeEnabled && extensionMode != EXTENSION_MODE_NONE)
                    ) {
                        closeCaptureSession()
                    } else {
                        setRepeatingRequest()
                        enableUiControl(true)
                        if (!captureSessionConfiguredIdlingResource.isIdleNow) {
                            captureSessionConfiguredIdlingResource.decrement()
                        }
                    }
                }
                STATE_CAPTURE_SESSION_CLOSED -> {
                    if (activityStopped || currentCameraId != cameraId) {
                        closeCamera()
                    } else {
                        updatePreviewSize()
                        openCaptureSession(currentExtensionMode, extensionModeEnabled)
                    }
                }
            }
        }
    }

    @VisibleForTesting
    fun switchCamera() {
        checkRunOnMainThread()
        val newCameraId = if (currentCameraId == backCameraId) frontCameraId else backCameraId

        if (!isCameraSupportExtensions(newCameraId)) {
            Toast.makeText(
                    this,
                    "Camera of the other lens facing doesn't support Camera2 extensions.",
                    Toast.LENGTH_SHORT,
                )
                .show()
            return
        }

        enableUiControl(false)
        currentCameraId = newCameraId
        updateExtensionInfo()

        val extensionModeToggleButton = findViewById<Button>(R.id.PhotoToggle)
        extensionModeToggleButton.text = getCamera2ExtensionModeStringFromId(currentExtensionMode)

        closeCaptureSession()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart()")
        activityStopped = false
        if (textureView.isAvailable) {
            setupAndStartPreview()
        }
    }

    override fun onStop() {
        Log.d(TAG, "onStop()++")
        super.onStop()
        activityStopped = true
        // Closes the capture session to shut down the whole pipeline.
        closeCaptureSession()
        lastSurfaceTextureTimestampNanos = 0L
        Log.d(TAG, "onStop()--")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()++")
        super.onDestroy()
        Log.d(TAG, "Waiting for capture session closed...")
        synchronized(lock) { captureSessionClosedDeferred }
            .asListenableFuture()
            .addListener(
                {
                    previewSurface?.release()
                    textureView.surfaceTexture?.release()
                    normalModeCaptureThread.quitSafely()
                    Log.d(TAG, "Surface texture released. $previewSurface")
                    imageSaveTerminationFuture.addListener(
                        {
                            stillImageReader?.close()
                            Log.d(TAG, "stillImageReader closed. ${stillImageReader?.surface}")
                            imageSaverThread.quitSafely()
                        },
                        mainExecutor,
                    )
                },
                mainExecutor,
            )

        streamConfigurationLatency[KEY_CAMERA2_LATENCY]?.also {
            val min = "${it.minOrNull() ?: "n/a"}"
            val max = "${it.maxOrNull() ?: "n/a"}"
            val avg = it.average().format(2)

            Log.d(
                TAG,
                "Camera2 Stream Configuration Latency: min=${min}ms max=${max}ms avg=${avg}ms",
            )
        }
        var testResultDetails = ""
        streamConfigurationLatency[KEY_CAMERA_EXTENSION_LATENCY]?.also {
            val min = "${it.minOrNull() ?: "n/a"}"
            val max = "${it.maxOrNull() ?: "n/a"}"
            val avg = it.average().format(2)
            testResultDetails = "min=${min}ms max=${max}ms avg=${avg}ms"

            Log.d(TAG, "Camera Extensions Stream Configuration Latency: $testResultDetails")
        }

        val durations = streamConfigurationLatency[KEY_CAMERA_EXTENSION_LATENCY] ?: emptyList()
        val testResult =
            if (durations.isNotEmpty()) {
                if (durations.average() > MAX_EXTENSION_LATENCY_MILLIS) {
                    TEST_RESULT_FAILED
                } else {
                    TEST_RESULT_PASSED
                }
            } else {
                TEST_RESULT_NOT_TESTED
            }

        val testResults = TestResults.getInstance(this@Camera2ExtensionsActivity)
        testResults.updateTestResultAndSave(
            TEST_TYPE_CAMERA2_EXTENSION_STREAM_CONFIG_LATENCY,
            currentCameraId,
            currentExtensionMode,
            testResult,
            testResultDetails,
        )

        Log.d(TAG, "onDestroy()--")
    }

    private fun closeCaptureSession() =
        coroutineScope.async(cameraTaskDispatcher) {
            Log.d(TAG, "closeCaptureSession()++")
            // Directly return here if no capture session is configured yet. If the newly created
            // capture session should be closed, handleCaptureSessionOnConfiguredEvent will invoke
            // this function again to close it when the capture session is configured.
            if (getCurrentState() != STATE_CAPTURE_SESSION_CONFIGURED) {
                return@async
            }
            setCurrentState(STATE_CAPTURE_SESSION_CLOSING)
            resetCaptureSessionConfiguredIdlingResource()

            try {
                if (cameraCaptureSession is CameraCaptureSession) {
                    (cameraCaptureSession as CameraCaptureSession).close()
                    Log.d(TAG, "closed CameraCaptureSession")
                } else {
                    (cameraCaptureSession as CameraExtensionSession).close()
                    Log.d(TAG, "closed CameraExtensionSession")
                }
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
    private fun setupAndStartPreview() {
        checkRunOnMainThread()
        if (!textureView.isAvailable) {
            Toast.makeText(this, "TextureView is invalid!!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        updatePreviewSize()
        openCamera(cameraManager, currentCameraId)
    }

    @Suppress("DEPRECATION") /* defaultDisplay */
    private fun updatePreviewSize() {
        checkRunOnMainThread()
        val previewResolution =
            pickPreviewResolution(
                cameraManager,
                currentCameraId,
                resources.displayMetrics,
                if (extensionModeEnabled) currentExtensionMode else EXTENSION_MODE_NONE,
            )

        if (previewResolution == null) {
            Toast.makeText(this, "Invalid preview extension sizes!.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "Set default buffer size to previewResolution: $previewResolution")

        textureView.surfaceTexture?.setDefaultBufferSize(
            previewResolution.width,
            previewResolution.height,
        )

        textureView.layoutParams =
            FrameLayout.LayoutParams(previewResolution.width, previewResolution.height)

        val containerViewSize = Size(containerView.width, containerView.height)

        val lensFacing =
            cameraManager
                .getCameraCharacteristics(currentCameraId)[CameraCharacteristics.LENS_FACING]

        transformTextureView(
            textureView,
            containerViewSize,
            previewResolution,
            windowManager.defaultDisplay.rotation,
            cameraSensorRotationDegrees,
            lensFacing == CameraCharacteristics.LENS_FACING_BACK,
        )

        tapToFocusDetector =
            TapToFocusDetector(this, textureView, getCameraInfo(), display!!.rotation, ::tapToFocus)
    }

    private fun getCameraInfo(): CameraInfo {
        checkRunOnMainThread()
        val lensFacing =
            cameraManager
                .getCameraCharacteristics(currentCameraId)[CameraCharacteristics.LENS_FACING]
        val sensorOrientation =
            cameraManager
                .getCameraCharacteristics(currentCameraId)[CameraCharacteristics.SENSOR_ORIENTATION]
        val activeArraySize =
            cameraManager
                .getCameraCharacteristics(currentCameraId)[
                    CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]
        return CameraInfo(lensFacing!!, sensorOrientation!!.toFloat(), activeArraySize!!)
    }

    private fun tapToFocus(meteringRectangles: Array<MeteringRectangle?>) {
        coroutineScope.launch(cameraTaskDispatcher) {
            focusMeteringControl.updateMeteringRectangles(meteringRectangles)
        }
    }

    private fun checkRunOnMainThread() {
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            val exception = IllegalStateException("Must run on the main thread!")
            Log.e(TAG, exception.toString())
            exception.printStackTrace()
            throw exception
        }
    }

    private fun checkRunOnCameraThread() {
        if (
            Thread.currentThread() != cameraThread &&
                Thread.currentThread() != normalModeCaptureThread
        ) {
            val exception = IllegalStateException("Must run on the camera thread!")
            Log.e(TAG, exception.toString())
            exception.printStackTrace()
            throw exception
        }
    }

    private fun getCurrentState(): Int {
        checkRunOnCameraThread()
        return currentState
    }

    private fun setCurrentState(state: Int) {
        checkRunOnCameraThread()
        Log.d(
            TAG,
            "Old state: ${getStateString(currentState)}, new state: ${getStateString(state)}",
        )
        currentState = state
    }

    private fun getStateString(state: Int) =
        when (state) {
            STATE_CAMERA_CLOSED -> "STATE_CAMERA_CLOSED"
            STATE_CAMERA_OPENING -> "STATE_CAMERA_OPENING"
            STATE_CAMERA_OPENED -> "STATE_CAMERA_OPENED"
            STATE_CAPTURE_SESSION_OPENING -> "STATE_CAPTURE_SESSION_OPENING"
            STATE_CAPTURE_SESSION_CONFIGURED -> "STATE_CAPTURE_SESSION_CONFIGURED"
            STATE_CAPTURE_SESSION_CLOSING -> "STATE_CAPTURE_SESSION_CLOSING"
            STATE_CAPTURE_SESSION_CLOSED -> "STATE_CAPTURE_SESSION_CLOSED"
            STATE_CAMERA_CLOSING -> "STATE_CAMERA_CLOSING"
            else -> throw IllegalArgumentException("Invalid state value!")
        }

    /** Opens and returns the camera (as the result of the suspend coroutine) */
    @SuppressLint("MissingPermission")
    fun openCamera(manager: CameraManager, cameraId: String) =
        coroutineScope.async(cameraTaskDispatcher) {
            Log.d(TAG, "openCamera()++: $cameraId")
            if (getCurrentState() != STATE_CAMERA_CLOSED) {
                return@async
            }
            setCurrentState(STATE_CAMERA_OPENING)
            resetCameraClosedIdlingResource()
            manager.openCamera(
                cameraId,
                cameraTaskDispatcher.asExecutor(),
                object : CameraDevice.StateCallback() {
                    override fun onOpened(device: CameraDevice) {
                        Log.d(TAG, "Camera ${device.id} - onOpened")
                        cameraDevice = device
                        setCurrentState(STATE_CAMERA_OPENED)
                        determineNextStepOnUiThread(STATE_CAMERA_OPENED, device.id)
                    }

                    override fun onDisconnected(device: CameraDevice) {
                        Log.d(TAG, "Camera ${device.id} - onDisconnected")
                        // Closes camera when onDisconnected event is received
                        setCurrentState(STATE_CAMERA_CLOSING)
                        closeCamera()
                    }

                    override fun onClosed(device: CameraDevice) {
                        Log.d(TAG, "Camera ${device.id} - onClosed")
                        cameraDevice = null
                        setCurrentState(STATE_CAMERA_CLOSED)
                        if (!cameraClosedIdlingResource.isIdleNow) {
                            cameraClosedIdlingResource.decrement()
                        }
                        determineNextStepOnUiThread(STATE_CAMERA_CLOSED, device.id)
                    }

                    override fun onError(device: CameraDevice, error: Int) {
                        Log.d(TAG, "Camera ${device.id} - onError, error code: $error")
                        val msg =
                            when (error) {
                                ERROR_CAMERA_DEVICE -> "Fatal (device)"
                                ERROR_CAMERA_DISABLED -> "Device policy"
                                ERROR_CAMERA_IN_USE -> "Camera in use"
                                ERROR_CAMERA_SERVICE -> "Fatal (service)"
                                ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                                else -> "Unknown"
                            }
                        val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                        Log.e(TAG, exc.message, exc)
                        // Closes camera when an error occurs
                        setCurrentState(STATE_CAMERA_CLOSING)
                        closeCamera()
                    }
                },
            )
            Log.d(TAG, "openCamera()--: $cameraId")
        }

    private fun closeCamera() =
        coroutineScope.async(cameraTaskDispatcher) {
            val cameraId = cameraDevice?.id
            Log.d(TAG, "closeCamera()++: $cameraId")
            setCurrentState(STATE_CAMERA_CLOSING)
            cameraDevice?.close()
            Log.d(TAG, "closeCamera()--: $cameraId")
        }

    /** Opens and returns the extensions session (as the result of the suspend coroutine) */
    private fun openCaptureSession(extensionMode: Int, extensionModeEnabled: Boolean) =
        coroutineScope.async(cameraTaskDispatcher) {
            Log.d(TAG, "openCaptureSession")
            // Resets the metering rectangles
            meteringRectangles = EMPTY_RECTANGLES
            setCurrentState(STATE_CAPTURE_SESSION_OPENING)

            if (stillImageReader != null) {
                val imageReaderToClose = stillImageReader!!
                imageSaveTerminationFuture.addListener({ imageReaderToClose.close() }, mainExecutor)
            }

            stillImageReader = setupImageReader(cameraDevice!!.id, extensionMode)

            val outputConfigs = arrayListOf<OutputConfiguration>()
            outputConfigs.add(OutputConfiguration(stillImageReader!!.surface))
            outputConfigs.add(OutputConfiguration(previewSurface!!))

            synchronized(lock) { captureSessionClosedDeferred = CompletableDeferred() }

            if (extensionModeEnabled) {
                createCameraExtensionSession(outputConfigs, extensionMode)
            } else {
                createCameraCaptureSession(outputConfigs)
            }
        }

    /** Creates normal mode CameraCaptureSession */
    private fun createCameraCaptureSession(outputConfigs: ArrayList<OutputConfiguration>) {
        checkRunOnCameraThread()
        Log.d(TAG, "createCameraCaptureSession++")
        val sessionConfiguration =
            SessionConfiguration(
                SESSION_REGULAR,
                outputConfigs,
                cameraTaskDispatcher.asExecutor(),
                object : CameraCaptureSession.StateCallback() {
                    override fun onClosed(session: CameraCaptureSession) {
                        Log.d(TAG, "CaptureSession - onClosed: $session")
                        handleCaptureSessionOnClosedEvent(session.device.id, EXTENSION_MODE_NONE)
                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(TAG, "CaptureSession - onConfigured: $session")
                        handleCaptureSessionOnConfiguredEvent(
                            session,
                            session.device.id,
                            EXTENSION_MODE_NONE,
                        )
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "CaptureSession - onConfigureFailed: $session")
                        handleCaptureSessionOnConfigureFailedEvent()
                    }
                },
            )
        cameraDevice!!.createCaptureSession(sessionConfiguration)
        Log.d(TAG, "createCameraCaptureSession--")
    }

    /** Creates extension mode CameraExtensionSession */
    private fun createCameraExtensionSession(
        outputConfigs: ArrayList<OutputConfiguration>,
        extensionMode: Int,
    ) {
        checkRunOnCameraThread()
        Log.d(TAG, "createCameraExtensionSession++, extensionMode=$extensionMode")
        val extensionConfiguration =
            ExtensionSessionConfiguration(
                extensionMode,
                outputConfigs,
                cameraTaskDispatcher.asExecutor(),
                object : CameraExtensionSession.StateCallback() {
                    override fun onClosed(session: CameraExtensionSession) {
                        Log.d(TAG, "Extension CaptureSession - onClosed: $session")
                        handleCaptureSessionOnClosedEvent(cameraDevice!!.id, extensionMode)
                    }

                    override fun onConfigured(session: CameraExtensionSession) {
                        Log.d(TAG, "Extension CaptureSession - onConfigured: $session")
                        handleCaptureSessionOnConfiguredEvent(
                            session,
                            cameraDevice!!.id,
                            extensionMode,
                        )
                    }

                    override fun onConfigureFailed(session: CameraExtensionSession) {
                        Log.e(TAG, "Extension CaptureSession - onConfigureFailed: $session")
                        handleCaptureSessionOnConfigureFailedEvent()
                    }
                },
            )
        Log.d(TAG, "createCameraExtensionSession########")
        cameraDevice!!.createExtensionSession(extensionConfiguration)
        Log.d(TAG, "createCameraExtensionSession--")
    }

    private fun handleCaptureSessionOnClosedEvent(cameraId: String, mode: Int) {
        checkRunOnCameraThread()
        setCurrentState(STATE_CAPTURE_SESSION_CLOSED)
        cameraCaptureSession = null
        synchronized(lock) { captureSessionClosedDeferred.complete(Unit) }
        determineNextStepOnUiThread(STATE_CAPTURE_SESSION_CLOSED, cameraId, mode)
    }

    private fun handleCaptureSessionOnConfiguredEvent(session: Any, cameraId: String, mode: Int) {
        checkRunOnCameraThread()
        setCurrentState(STATE_CAPTURE_SESSION_CONFIGURED)
        cameraCaptureSession = session
        determineNextStepOnUiThread(STATE_CAPTURE_SESSION_CONFIGURED, cameraId, mode)
    }

    private fun handleCaptureSessionOnConfigureFailedEvent() {
        checkRunOnCameraThread()
        // CLoses the camera to restart the whole pipe line
        setCurrentState(STATE_CAMERA_CLOSING)
        closeCamera()
    }

    private fun startAfTrigger(meteringRectangles: Array<MeteringRectangle?>) {
        coroutineScope.launch(cameraTaskDispatcher) {
            this@Camera2ExtensionsActivity.meteringRectangles = meteringRectangles
            addFocusMeteringCaptureCallback()

            val captureBuilder = getCaptureRequestBuilder()

            captureBuilder.set(CONTROL_AF_TRIGGER, CONTROL_AF_TRIGGER_START)

            setRepeatingRequest(captureBuilder.build())
        }
    }

    private fun cancelAfTrigger(afTriggerType: Int) {
        coroutineScope.launch(cameraTaskDispatcher) {
            if (afTriggerType == CONTROL_AF_TRIGGER_CANCEL) {
                this@Camera2ExtensionsActivity.meteringRectangles = EMPTY_RECTANGLES
            }

            removeFocusMeteringCaptureCallback()

            val captureBuilder = getCaptureRequestBuilder()

            if (afTriggerType == CONTROL_AF_TRIGGER_IDLE) {
                captureBuilder.set(CONTROL_AF_TRIGGER, CONTROL_AF_TRIGGER_IDLE)
            } else {
                captureBuilder.set(CONTROL_AF_TRIGGER, CONTROL_AF_TRIGGER_CANCEL)
            }

            setRepeatingRequest(captureBuilder.build())
        }
    }

    private fun addFocusMeteringCaptureCallback() {
        checkRunOnCameraThread()
        val captureCallback =
            focusMeteringControl.getCaptureCallback(cameraCaptureSession is CameraExtensionSession)
        if (cameraCaptureSession is CameraExtensionSession) {
            comboCaptureCallbackExtensionMode.addCaptureCallback(
                captureCallback as ExtensionCaptureCallback
            )
        } else {
            comboCaptureCallbackNormalMode.addCaptureCallback(captureCallback as CaptureCallback)
        }
    }

    private fun removeFocusMeteringCaptureCallback() {
        checkRunOnCameraThread()
        val captureCallback =
            focusMeteringControl.getCaptureCallback(cameraCaptureSession is CameraExtensionSession)
        if (cameraCaptureSession is CameraExtensionSession) {
            comboCaptureCallbackExtensionMode.removeCaptureCallback(
                captureCallback as ExtensionCaptureCallback
            )
        } else {
            comboCaptureCallbackNormalMode.removeCaptureCallback(captureCallback as CaptureCallback)
        }
    }

    private fun setRepeatingRequest(captureRequest: CaptureRequest? = null) {
        coroutineScope.launch(cameraTaskDispatcher) {
            if (cameraCaptureSession is CameraCaptureSession) {
                (cameraCaptureSession as CameraCaptureSession).setRepeatingRequest(
                    captureRequest ?: getCaptureRequestBuilder().build(),
                    comboCaptureCallbackNormalMode,
                    normalModeCaptureHandler,
                )
            } else {
                (cameraCaptureSession as CameraExtensionSession).setRepeatingRequest(
                    captureRequest ?: getCaptureRequestBuilder().build(),
                    cameraTaskDispatcher.asExecutor(),
                    comboCaptureCallbackExtensionMode,
                )
            }
        }
    }

    private fun getCaptureRequestBuilder(): CaptureRequest.Builder {
        checkRunOnCameraThread()
        val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureBuilder.addTarget(previewSurface!!)
        val videoStabilizationMode =
            if (videoStabilizationToggleView.isChecked) {
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
            } else {
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            }

        captureBuilder.set(CONTROL_VIDEO_STABILIZATION_MODE, videoStabilizationMode)

        captureBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO, zoomRatio)

        if (!meteringRectangles.contentEquals(EMPTY_RECTANGLES)) {
            captureBuilder.set(CONTROL_AF_MODE, CONTROL_AF_MODE_AUTO)
            captureBuilder.set(CONTROL_AF_REGIONS, meteringRectangles)
            captureBuilder.set(CONTROL_AE_MODE, CONTROL_AE_MODE_ON)
            captureBuilder.set(CONTROL_AE_REGIONS, meteringRectangles)
            captureBuilder.set(CONTROL_AWB_REGIONS, meteringRectangles)
        }

        return captureBuilder
    }

    private fun setupImageReader(cameraId: String, extensionMode: Int): ImageReader {
        val (size, format) =
            pickStillImageResolution(
                cameraManager.getCameraCharacteristics(cameraId),
                extensionCharacteristics,
                extensionMode,
            )

        Log.d(TAG, "Setup image reader - size: $size, format: $format")

        return ImageReader.newInstance(size.width, size.height, format, 1)
    }

    /** Takes a picture. */
    private fun takePicture() {
        checkRunOnMainThread()
        val (fileName, suffix) = generateFileName(currentCameraId, currentExtensionMode)
        val rotationDegrees: Int
        try {
            val lensFacing =
                cameraManager
                    .getCameraCharacteristics(currentCameraId)[CameraCharacteristics.LENS_FACING]

            rotationDegrees =
                calculateRelativeImageRotationDegrees(
                    (surfaceRotationToRotationDegrees(display!!.rotation)),
                    cameraSensorRotationDegrees,
                    lensFacing == CameraCharacteristics.LENS_FACING_BACK,
                )
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
            return
        }

        var takePictureCompleter: Completer<Any>? = null

        imageSaveTerminationFuture =
            CallbackToFutureAdapter.getFuture<Any> {
                takePictureCompleter = it
                "imageSaveTerminationFuture"
            }

        stillImageReader!!.setOnImageAvailableListener(
            { reader: ImageReader ->
                val imageUri = acquireImageAndSave(reader, fileName, suffix, rotationDegrees)

                imageUri?.let { sessionImageUriSet.add(it) }

                stillImageReader!!.setOnImageAvailableListener(null, null)
                takePictureCompleter?.set(null)

                if (!imageSavedIdlingResource.isIdleNow) {
                    imageSavedIdlingResource.decrement()
                }

                coroutineScope.launch(Dispatchers.Main) {
                    if (isRequestMode) {
                        if (imageUri == null) {
                            result.putExtra(
                                INTENT_EXTRA_KEY_ERROR_CODE,
                                ERROR_CODE_SAVE_IMAGE_FAILED,
                            )
                        } else {
                            result.putExtra(INTENT_EXTRA_KEY_IMAGE_URI, imageUri)
                            result.putExtra(
                                INTENT_EXTRA_KEY_IMAGE_ROTATION_DEGREES,
                                rotationDegrees,
                            )
                        }
                        finish()
                    } else {
                        enableUiControl(true)
                    }
                }
            },
            imageSaverHandler,
        )
        submitStillImageCaptureRequest(takePictureCompleter!!)
    }

    /** Acquires the latest image from the image reader and save it to the Pictures folder */
    private fun acquireImageAndSave(
        imageReader: ImageReader,
        fileName: String,
        suffix: String,
        rotationDegrees: Int,
    ): Uri? {
        var uri: Uri?

        imageReader.acquireLatestImage().let { image ->
            try {
                uri =
                    if (isRequestMode) {
                        // Saves as temp file if the activity is called by other validation activity
                        // to capture a image.
                        FileUtil.saveImageToTempFile(image, fileName, suffix, null, rotationDegrees)
                    } else {
                        FileUtil.saveImage(
                            image,
                            fileName,
                            suffix,
                            "Pictures/ExtensionsPictures",
                            contentResolver,
                            rotationDegrees,
                        )
                    }
            } finally {
                image.close()
            }

            val msg =
                if (uri != null) {
                    "Saved image to $fileName.jpg"
                } else {
                    "Failed to save image."
                }

            if (!isRequestMode) {
                coroutineScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@Camera2ExtensionsActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        return uri
    }

    private fun submitStillImageCaptureRequest(takePictureCompleter: Completer<Any>?) {
        coroutineScope.launch(cameraTaskDispatcher) {
            Preconditions.checkState(
                cameraCaptureSession != null,
                "take picture button is only enabled when session is configured successfully",
            )

            val captureBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(stillImageReader!!.surface)

            if (cameraCaptureSession is CameraCaptureSession) {
                (cameraCaptureSession as CameraCaptureSession).capture(
                    captureBuilder.build(),
                    object : CaptureCallback() {
                        override fun onCaptureFailed(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            failure: CaptureFailure,
                        ) {
                            takePictureCompleter?.set(null)
                            Log.e(TAG, "Failed to take picture.")
                        }
                    },
                    normalModeCaptureHandler,
                )
            } else {
                (cameraCaptureSession as CameraExtensionSession).capture(
                    captureBuilder.build(),
                    cameraTaskDispatcher.asExecutor(),
                    object : ExtensionCaptureCallback() {
                        override fun onCaptureFailed(
                            session: CameraExtensionSession,
                            request: CaptureRequest,
                        ) {
                            takePictureCompleter?.set(null)
                            Log.e(TAG, "Failed to take picture.")
                        }

                        override fun onCaptureSequenceCompleted(
                            session: CameraExtensionSession,
                            sequenceId: Int,
                        ) {
                            Log.v(TAG, "onCaptureProcessSequenceCompleted: $sequenceId")
                        }
                    },
                )
            }
        }
    }

    /**
     * Generate the output file name and suffix depending on whether the image is requested by the
     * validation activity.
     */
    private fun generateFileName(cameraId: String, extensionMode: Int): Pair<String, String> {
        val fileName: String
        val suffix: String

        if (isRequestMode) {
            val lensFacing =
                cameraManager
                    .getCameraCharacteristics(cameraId)[CameraCharacteristics.LENS_FACING]!!
            fileName =
                "[Camera2Extension][Camera-$cameraId][${getLensFacingStringFromInt(lensFacing)}][${
                    getCamera2ExtensionModeStringFromId(extensionMode)
                }]${if (extensionModeEnabled) "[Enabled]" else "[Disabled]"}"
            suffix = ""
        } else {
            val formatter: Format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            fileName =
                "[${formatter.format(Calendar.getInstance().time)}][Camera2]${
                    getCamera2ExtensionModeStringFromId(extensionMode)
                }"
            suffix = ".jpg"
        }

        return Pair(fileName, suffix)
    }

    private fun getLensFacingStringFromInt(lensFacing: Int): String =
        when (lensFacing) {
            CameraMetadata.LENS_FACING_BACK -> "BACK"
            CameraMetadata.LENS_FACING_FRONT -> "FRONT"
            CameraMetadata.LENS_FACING_EXTERNAL -> "EXTERNAL"
            else -> throw IllegalArgumentException("Invalid lens facing!!")
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!isRequestMode) {
            val inflater = menuInflater
            inflater.inflate(R.menu.main_menu_camera2_extensions_activity, menu)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_camerax_extensions -> {
                switchActivity(CameraExtensionsActivity::class.java.name)
                return true
            }
            R.id.menu_validation_tool -> {
                switchActivity(CameraValidationResultActivity::class.java.name)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun switchActivity(className: String) {
        // Set the activityStopped as true early because the onStop event might come late after the
        // target activity is launched. The camera might be re-opened in this activity to cause the
        // ERROR_CAMERA_IN_USE problem when the target activity tries to open the camera.
        activityStopped = true
        val intent = Intent()
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setClassName(this, className)
        startActivity(intent)
    }

    @VisibleForTesting
    fun getCameraClosedIdlingResource(): CountingIdlingResource = cameraClosedIdlingResource

    @VisibleForTesting
    fun getCaptureSessionConfiguredIdlingResource(): CountingIdlingResource =
        captureSessionConfiguredIdlingResource

    @VisibleForTesting
    fun getPreviewIdlingResource(): CountingIdlingResource = previewIdlingResource

    @VisibleForTesting
    fun getImageSavedIdlingResource(): CountingIdlingResource = imageSavedIdlingResource

    private fun resetCameraClosedIdlingResource() {
        if (cameraClosedIdlingResource.isIdleNow) {
            cameraClosedIdlingResource.increment()
        }
    }

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

    private class SessionMediaUriSet(val contentResolver: ContentResolver) {
        private val mSessionMediaUris: MutableSet<Uri> = mutableSetOf()

        fun add(uri: Uri) {
            synchronized(mSessionMediaUris) { mSessionMediaUris.add(uri) }
        }

        fun deleteAllUris() {
            synchronized(mSessionMediaUris) {
                val it = mSessionMediaUris.iterator()
                while (it.hasNext()) {
                    contentResolver.delete(it.next(), null, null)
                    it.remove()
                }
            }
        }
    }

    private fun startZoom(scaleFactor: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        zoomRatio =
            (zoomRatio * scaleFactor).coerceIn(
                ZoomUtil.minZoom(cameraManager.getCameraCharacteristics(currentCameraId)),
                ZoomUtil.maxZoom(cameraManager.getCameraCharacteristics(currentCameraId)),
            )
        Log.d(TAG, "onScale: $zoomRatio")
        setRepeatingRequest()
    }

    /** Not all cameras have zoom support. Returns true if zoom is supported otherwise false. */
    private fun hasZoomSupport(): Boolean {
        checkRunOnMainThread()
        return if (!extensionModeEnabled) {
            ZoomUtil.hasZoomSupport(currentCameraId, cameraManager)
        } else if (extensionModeEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ZoomUtilExtensions.hasZoomSupport(currentCameraId, cameraManager, currentExtensionMode)
        } else {
            false
        }
    }

    @RequiresApi(33)
    private object ZoomUtilExtensions {
        @JvmStatic
        fun hasZoomSupport(
            cameraId: String,
            cameraManager: CameraManager,
            extensionMode: Int,
        ): Boolean =
            cameraManager
                .getCameraExtensionCharacteristics(cameraId)
                .getAvailableCaptureRequestKeys(extensionMode)
                .contains(CaptureRequest.CONTROL_ZOOM_RATIO)
    }

    @RequiresApi(31)
    private object ZoomUtil {
        fun hasZoomSupport(cameraId: String, cameraManager: CameraManager): Boolean {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val availableCaptureRequestKeys = characteristics.availableCaptureRequestKeys
            return availableCaptureRequestKeys.contains(CaptureRequest.CONTROL_ZOOM_RATIO)
        }

        fun minZoom(characteristics: CameraCharacteristics): Float =
            characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)?.lower ?: 1.0f

        fun maxZoom(characteristics: CameraCharacteristics): Float =
            characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)?.upper ?: 1.0f
    }

    /**
     * A combo ExtensionCaptureCallback implementation to receive to pass the events to the
     * underlying callbacks.
     */
    private class ComboCaptureCallbackExtensionMode : ExtensionCaptureCallback() {
        private val captureCallbacks: MutableList<ExtensionCaptureCallback> = mutableListOf()

        fun addCaptureCallback(captureCallback: ExtensionCaptureCallback) {
            if (!captureCallbacks.contains(captureCallback)) {
                captureCallbacks.add(captureCallback)
            }
        }

        fun removeCaptureCallback(captureCallback: ExtensionCaptureCallback) {
            captureCallbacks.remove(captureCallback)
        }

        override fun onCaptureStarted(
            session: CameraExtensionSession,
            request: CaptureRequest,
            timestamp: Long,
        ) {
            captureCallbacks.forEach { it.onCaptureStarted(session, request, timestamp) }
        }

        override fun onCaptureProcessStarted(
            session: CameraExtensionSession,
            request: CaptureRequest,
        ) {
            captureCallbacks.forEach { it.onCaptureProcessStarted(session, request) }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onCaptureResultAvailable(
            session: CameraExtensionSession,
            request: CaptureRequest,
            result: TotalCaptureResult,
        ) {
            captureCallbacks.forEach { it.onCaptureResultAvailable(session, request, result) }
        }

        override fun onCaptureFailed(session: CameraExtensionSession, request: CaptureRequest) {
            captureCallbacks.forEach { it.onCaptureFailed(session, request) }
        }
    }

    /**
     * A combo CaptureCallback implementation to receive to pass the events to the underlying
     * callbacks.
     */
    private class ComboCaptureCallbackNormalMode : CaptureCallback() {
        private val captureCallbacks: MutableList<CaptureCallback> = mutableListOf()

        fun addCaptureCallback(captureCallback: CaptureCallback) {
            if (!captureCallbacks.contains(captureCallback)) {
                captureCallbacks.add(captureCallback)
            }
        }

        fun removeCaptureCallback(captureCallback: CaptureCallback) {
            captureCallbacks.remove(captureCallback)
        }

        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long,
        ) {
            captureCallbacks.forEach {
                it.onCaptureStarted(session, request, timestamp, frameNumber)
            }
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult,
        ) {
            captureCallbacks.forEach { it.onCaptureCompleted(session, request, result) }
        }
    }
}

fun Double.format(scale: Int): String = String.format("%.${scale}f", this)

/** Convert a job into a ListenableFuture<T>. */
@OptIn(ExperimentalCoroutinesApi::class)
private fun <T> Deferred<T>.asListenableFuture(
    tag: Any? = "Deferred.asListenableFuture"
): ListenableFuture<T> {
    val resolver: CallbackToFutureAdapter.Resolver<T> =
        CallbackToFutureAdapter.Resolver<T> { completer ->
            this.invokeOnCompletion {
                if (it != null) {
                    if (it is CancellationException) {
                        completer.setCancelled()
                    } else {
                        completer.setException(it)
                    }
                } else {
                    // Ignore exceptions - This should never throw in this situation.
                    completer.set(this.getCompleted())
                }
            }
            tag
        }
    return CallbackToFutureAdapter.getFuture(resolver)
}
