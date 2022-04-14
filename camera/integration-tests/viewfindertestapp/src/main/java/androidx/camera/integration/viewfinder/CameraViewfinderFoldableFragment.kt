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

package androidx.camera.integration.viewfinder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.viewfinder.CameraViewfinder
import androidx.camera.viewfinder.ExperimentalViewfinder
import androidx.camera.viewfinder.ViewfinderSurfaceRequest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.google.common.base.Objects
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

/**
 * Fold aware fragment for {@link CameraViewfinder}.
 */
class CameraViewfinderFoldableFragment : Fragment(), View.OnClickListener,
    ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * ID of the current [CameraDevice].
     */
    private lateinit var cameraId: String

    /**
     * An instance of {@link CameraManager}.
     */
    private lateinit var cameraManager: CameraManager

    /**
     * An {@link CameraViewfinder} for camera viewfinder.
     */
    private lateinit var cameraViewfinder: CameraViewfinder

    /**
     * A [CameraCaptureSession] for camera preview.
     */
    private var captureSession: CameraCaptureSession? = null

    /**
     * A reference to the opened [CameraDevice].
     */
    private var cameraDevice: CameraDevice? = null

    /**
     * The {@link android.util.Size} of camera viewfinder.
     */
    private var viewfinderSize: Size? = null

    /**
     * The request for viewfinder surface.
     */
    private var viewfinderSurfaceRequest: ViewfinderSurfaceRequest? = null

    /**
     * An instance of {@link WindowInfoTracker}.
     *
     * See https://developer.android.com/guide/topics/large-screens/make-apps-fold-aware
     */
    private lateinit var windowInfoTracker: WindowInfoTracker

    private var activeWindowLayoutInfo: WindowLayoutInfo? = null

    private var isViewfinderInLeftTop = true

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its state.
     */
    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@CameraViewfinderFoldableFragment.cameraDevice = cameraDevice
            createCameraViewfinderSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@CameraViewfinderFoldableFragment.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            onDisconnected(cameraDevice)
            this@CameraViewfinderFoldableFragment.activity?.finish()
        }
    }

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var backgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var backgroundHandler: Handler? = null

    /**
     * An [ImageReader] that handles still image capture.
     */
    private var imageReader: ImageReader? = null

    /**
     * This is the output file for our picture.
     */
    private lateinit var file: File

    /**
     * This a callback object for the [ImageReader]. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        backgroundHandler?.post(ImageSaver(it.acquireNextImage(), file))
    }

    /**
     * [CaptureRequest.Builder] for the camera preview
     */
    private lateinit var viewfinderRequestBuilder: CaptureRequest.Builder

    /**
     * [CaptureRequest] generated by [.previewRequestBuilder]
     */
    private lateinit var viewfinderRequest: CaptureRequest

    /**
     * The current state of camera state for taking pictures.
     *
     * @see .captureCallback
     */
    private var state = STATE_PREVIEW

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val cameraOpenCloseLock = Semaphore(1)

    /**
     * Whether the current camera device supports Flash or not.
     */
    private var flashSupported = false

    /**
     * Orientation of the camera sensor
     */
    private var sensorOrientation = 0

    /**
     * A [CameraCaptureSession.CaptureCallback] that handles events related to JPEG capture.
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        private fun process(result: CaptureResult) {
            when (state) {
                STATE_PREVIEW -> Unit // Do nothing when the camera preview is working normally.
                STATE_WAITING_LOCK -> capturePicture(result)
                STATE_WAITING_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        state = STATE_WAITING_NON_PRECAPTURE
                    }
                }
                STATE_WAITING_NON_PRECAPTURE -> {
                    // CONTROL_AE_STATE can be null on some devices
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        state = STATE_PICTURE_TAKEN
                        captureStillPicture()
                    }
                }
            }
        }

        private fun capturePicture(result: CaptureResult) {
            val afState = result.get(CaptureResult.CONTROL_AF_STATE)
            if (afState == null) {
                captureStillPicture()
            } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                // CONTROL_AE_STATE can be null on some devices
                val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    state = STATE_PICTURE_TAKEN
                    captureStillPicture()
                } else {
                    runPrecaptureSequence()
                }
            }
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            process(partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result)
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onPrepareOptionsMenu(menu: Menu) {
        @OptIn(markerClass = [ExperimentalViewfinder::class])
        val title = "Current impl: ${cameraViewfinder.implementationMode}"
        menu.findItem(R.id.implementationMode)?.title = title
        super.onPrepareOptionsMenu(menu)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        @OptIn(markerClass = [ExperimentalViewfinder::class])
        when (item.itemId) {
            R.id.implementationMode -> {
                cameraViewfinder.implementationMode =
                    when (cameraViewfinder.implementationMode) {
                        CameraViewfinder.ImplementationMode.PERFORMANCE ->
                            CameraViewfinder.ImplementationMode.COMPATIBLE
                        else -> CameraViewfinder.ImplementationMode.PERFORMANCE
                    }
                closeCamera()
                cameraViewfinder.post {
                    openCamera(cameraViewfinder.width, cameraViewfinder.height, false)
                }
            }
            R.id.fitCenter -> cameraViewfinder.scaleType = CameraViewfinder.ScaleType.FIT_CENTER
            R.id.fillCenter -> cameraViewfinder.scaleType = CameraViewfinder.ScaleType.FILL_CENTER
            R.id.fitStart -> cameraViewfinder.scaleType = CameraViewfinder.ScaleType.FIT_START
            R.id.fitEnd -> cameraViewfinder.scaleType = CameraViewfinder.ScaleType.FIT_END
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_camera_view_finder_foldable, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.picture).setOnClickListener(this)
        view.findViewById<View>(R.id.toggle).setOnClickListener(this)
        view.findViewById<View>(R.id.bitmap).setOnClickListener(this)
        view.findViewById<View>(R.id.switch_area).setOnClickListener(this)
        cameraViewfinder = view.findViewById(R.id.view_finder)
        cameraManager = activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        windowInfoTracker = WindowInfoTracker.getOrCreate(requireContext())
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        file = File(requireActivity().getExternalFilesDir(null), "pic.jpg")
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        // CameraViewfinder API needs to be called on UI thread, for simplicity, we posted on view.
        cameraViewfinder.post {
            openCamera(
                cameraViewfinder.width, cameraViewfinder.height, false
            )
        }

        lifecycleScope.launch {
            windowInfoTracker.windowLayoutInfo(requireActivity())
                .collect { newLayoutInfo ->
                    Log.d(TAG, "newLayoutInfo: $newLayoutInfo")
                    activeWindowLayoutInfo = newLayoutInfo
                    adjustPreviewByFoldingState()
                }
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun adjustPreviewByFoldingState() {
        val btnSwitchArea = requireView().findViewById<View>(R.id.switch_area)
        activeWindowLayoutInfo?.displayFeatures?.firstOrNull { it is FoldingFeature }
            ?.let {
                val rect = getFeaturePositionInViewRect(
                    it,
                    cameraViewfinder.parent as View
                ) ?: return@let
                val foldingFeature = it as FoldingFeature
                if (foldingFeature.state == FoldingFeature.State.HALF_OPENED) {
                    btnSwitchArea.visibility = View.VISIBLE
                    when (foldingFeature.orientation) {
                        FoldingFeature.Orientation.VERTICAL -> {
                            if (isViewfinderInLeftTop) {
                                cameraViewfinder.moveToLeftOf(rect)
                                val blankAreaWidth =
                                    (btnSwitchArea.parent as View).width - rect.right
                                btnSwitchArea.x = rect.right +
                                    (blankAreaWidth - btnSwitchArea.width) / 2f
                                btnSwitchArea.y =
                                    (cameraViewfinder.height - btnSwitchArea.height) / 2f
                            } else {
                                cameraViewfinder.moveToRightOf(rect)
                                btnSwitchArea.x =
                                    (rect.left - btnSwitchArea.width) / 2f
                                btnSwitchArea.y =
                                    (cameraViewfinder.height - btnSwitchArea.height) / 2f
                            }
                        }
                        FoldingFeature.Orientation.HORIZONTAL -> {
                            if (isViewfinderInLeftTop) {
                                cameraViewfinder.moveToTopOf(rect)
                                val blankAreaHeight =
                                    (btnSwitchArea.parent as View).height - rect.bottom
                                btnSwitchArea.x =
                                    (cameraViewfinder.width - btnSwitchArea.width) / 2f
                                btnSwitchArea.y = rect.bottom +
                                    (blankAreaHeight - btnSwitchArea.height) / 2f
                            } else {
                                cameraViewfinder.moveToBottomOf(rect)
                                btnSwitchArea.x =
                                    (cameraViewfinder.width - btnSwitchArea.width) / 2f
                                btnSwitchArea.y =
                                    (rect.top - btnSwitchArea.height) / 2f
                            }
                        }
                    }
                } else {
                    cameraViewfinder.restore()
                    btnSwitchArea.x = 0f
                    btnSwitchArea.y = 0f
                    btnSwitchArea.visibility = View.INVISIBLE
                }
            }
    }

    private fun View.moveToLeftOf(foldingFeatureRect: Rect) {
        x = 0f
        layoutParams = layoutParams.apply {
            width = foldingFeatureRect.left
        }
    }

    private fun View.moveToRightOf(foldingFeatureRect: Rect) {
        x = foldingFeatureRect.left.toFloat()
        layoutParams = layoutParams.apply {
            width = (parent as View).width - foldingFeatureRect.left
        }
    }

    private fun View.moveToTopOf(foldingFeatureRect: Rect) {
        y = 0f
        layoutParams = layoutParams.apply {
            height = foldingFeatureRect.top
        }
    }

    private fun View.moveToBottomOf(foldingFeatureRect: Rect) {
        y = foldingFeatureRect.top.toFloat()
        layoutParams = layoutParams.apply {
            height = (parent as View).height - foldingFeatureRect.top
        }
    }

    private fun View.restore() {
        // Restore to full view
        layoutParams = layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        y = 0f
        x = 0f
    }

    private fun getFeaturePositionInViewRect(
        displayFeature: DisplayFeature,
        view: View,
        includePadding: Boolean = true
    ): Rect? {
        // The location of the view in window to be in the same coordinate space as the feature.
        val viewLocationInWindow = IntArray(2)
        view.getLocationInWindow(viewLocationInWindow)

        // Intersect the feature rectangle in window with view rectangle to clip the bounds.
        val viewRect = Rect(
            viewLocationInWindow[0], viewLocationInWindow[1],
            viewLocationInWindow[0] + view.width, viewLocationInWindow[1] + view.height
        )

        // Include padding if needed
        if (includePadding) {
            viewRect.left += view.paddingLeft
            viewRect.top += view.paddingTop
            viewRect.right -= view.paddingRight
            viewRect.bottom -= view.paddingBottom
        }

        val featureRectInView = Rect(displayFeature.bounds)
        val intersects = featureRectInView.intersect(viewRect)
        if ((featureRectInView.width() == 0 && featureRectInView.height() == 0) ||
            !intersects
        ) {
            return null
        }

        // Offset the feature coordinates to view coordinate space start point
        featureRectInView.offset(-viewLocationInWindow[0], -viewLocationInWindow[1])

        return featureRectInView
    }

    @Suppress("DEPRECATION")
    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            ConfirmationDialog().show(childFragmentManager, FRAGMENT_DIALOG)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                    .show(childFragmentManager, FRAGMENT_DIALOG)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @Suppress("DEPRECATION")
    private fun setUpCameraOutputs(width: Int, height: Int, toggleCamera: Boolean) {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                // Toggle the front and back camera
                if (toggleCamera) {
                    val currentFacing: Int? = cameraManager.getCameraCharacteristics(this.cameraId)
                        .get<Int>(CameraCharacteristics.LENS_FACING)
                    if (Objects.equal(currentFacing, facing)) {
                        continue
                    }
                }

                val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

                // For still image captures, we use the largest available size.
                val largest = Collections.max(
                    /* coll = */ Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                    /* comp = */ CompareSizesByArea())
                imageReader = ImageReader.newInstance(largest.width, largest.height,
                    ImageFormat.JPEG, /*maxImages*/ 2).apply {
                    setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
                }

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                val displayRotation = requireActivity().windowManager.defaultDisplay.rotation

                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
                val swappedDimensions = areDimensionsSwapped(displayRotation)

                val displaySize = Point()
                requireActivity().windowManager.defaultDisplay.getSize(displaySize)
                val rotatedPreviewWidth = if (swappedDimensions) height else width
                val rotatedPreviewHeight = if (swappedDimensions) width else height
                var maxPreviewWidth = if (swappedDimensions) displaySize.y else displaySize.x
                var maxPreviewHeight = if (swappedDimensions) displaySize.x else displaySize.y

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                viewfinderSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
                    rotatedPreviewWidth, rotatedPreviewHeight,
                    maxPreviewWidth, maxPreviewHeight,
                    largest)

                // Check if the flash is supported.
                flashSupported =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                this.cameraId = cameraId

                @OptIn(markerClass = [ExperimentalViewfinder::class])
                val request =
                    ViewfinderSurfaceRequest(
                        viewfinderSize!!, characteristics
                    )
                viewfinderSurfaceRequest = request
                return
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * Determines if the dimensions are swapped given the phone's current rotation.
     *
     * @param displayRotation The current rotation of the display
     *
     * @return true if the dimensions are swapped, false otherwise.
     */
    private fun areDimensionsSwapped(displayRotation: Int): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                Log.e(TAG, "Display rotation is invalid: $displayRotation")
            }
        }
        return swappedDimensions
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(width: Int, height: Int, toggleCamera: Boolean) {
        val permission = activity?.let {
            ContextCompat.checkSelfPermission(it, Manifest.permission.CAMERA)
        }
        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
            return
        }
        setUpCameraOutputs(width, height, toggleCamera)
        try {
            // Wait for camera to open - 2.5 seconds is sufficient
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            cameraManager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    /**
     * Closes the current [CameraDevice].
     */
    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = backgroundThread?.looper?.let { Handler(it) }
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * Creates a new [CameraCaptureSession] for camera viewfinder.
     */
    private fun createCameraViewfinderSession() {
        @OptIn(markerClass = [ExperimentalViewfinder::class])
        val surfaceListenableFuture: ListenableFuture<Surface> =
            cameraViewfinder.requestSurfaceAsync(viewfinderSurfaceRequest!!)

        Futures.addCallback(surfaceListenableFuture, object : FutureCallback<Surface?> {
            override fun onSuccess(surface: Surface?) {
                Log.d(TAG, "request onSurfaceAvailable surface = $surface")
                surface?.let {
                    createCaptureSession(it)
                }
            }

            override fun onFailure(t: Throwable) {
                Log.e(TAG, "request onSurfaceClosed")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @Suppress("DEPRECATION")
    private fun createCaptureSession(surface: Surface) {
        try {
            viewfinderRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            viewfinderRequestBuilder.addTarget(surface)
            cameraDevice!!.createCaptureSession(
                mutableListOf(surface, imageReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(
                        cameraCaptureSession: CameraCaptureSession
                    ) {
                        // The camera is already closed
                        if (null == cameraDevice) {
                            return
                        }

                        // When the session is ready, we start displaying the viewfinder.
                        captureSession = cameraCaptureSession
                        try {
                            // Auto focus should be continuous for camera viewfinder.
                            viewfinderRequestBuilder.set<Int>(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            // Flash is automatically enabled when necessary.
                            setAutoFlash(viewfinderRequestBuilder)

                            // Finally, we start displaying the camera viewfinder.
                            viewfinderRequest = viewfinderRequestBuilder.build()
                            captureSession!!.setRepeatingRequest(
                                viewfinderRequest,
                                captureCallback,
                                null
                            )
                        } catch (e: CameraAccessException) {
                            Log.e(
                                TAG,
                                "onConfigured CameraAccessException message = " + e.message
                            )
                        }
                    }

                    override fun onConfigureFailed(
                        cameraCaptureSession: CameraCaptureSession
                    ) {
                        showToast("Failed")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            Log.e(
                TAG,
                "createCaptureSession CameraAccessException message = " + e.message
            )
        }
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private fun lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            viewfinderRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START)
            // Tell #captureCallback to wait for the lock.
            state = STATE_WAITING_LOCK
            captureSession?.capture(viewfinderRequestBuilder.build(), captureCallback,
                backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in [.captureCallback] from [.lockFocus].
     */
    private fun runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            viewfinderRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
            // Tell #captureCallback to wait for the precapture sequence to be set.
            state = STATE_WAITING_PRECAPTURE
            captureSession?.capture(viewfinderRequestBuilder.build(), captureCallback,
                backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * [.captureCallback] from both [.lockFocus].
     */
    @Suppress("DEPRECATION")
    private fun captureStillPicture() {
        try {
            if (activity == null || cameraDevice == null) return
            val rotation = requireActivity().windowManager.defaultDisplay.rotation

            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder = cameraDevice?.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE)?.apply {
                imageReader?.surface?.let { addTarget(it) }

                // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
                // We have to take that into account and rotate JPEG properly.
                // For devices with orientation of 90, we return our mapping from ORIENTATIONS.
                // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
                set(CaptureRequest.JPEG_ORIENTATION,
                    (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360)

                // Use the same AE and AF modes as the preview.
                set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }?.also { setAutoFlash(it) }

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    showToast("Saved: $file")
                    Log.d(TAG, file.toString())
                    unlockFocus()
                }
            }

            captureSession?.apply {
                stopRepeating()
                abortCaptures()
                captureBuilder?.build()?.let { capture(it, captureCallback, null) }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private fun unlockFocus() {
        try {
            // Reset the auto-focus trigger
            viewfinderRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            setAutoFlash(viewfinderRequestBuilder)
            captureSession?.capture(viewfinderRequestBuilder.build(), captureCallback,
                backgroundHandler)
            // After this, the camera will go back to the normal state of preview.
            state = STATE_PREVIEW
            captureSession?.setRepeatingRequest(viewfinderRequest, captureCallback,
                backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.picture -> lockFocus()
            R.id.toggle -> toggleCamera()
            R.id.bitmap -> saveBitmap()
            R.id.switch_area -> {
                isViewfinderInLeftTop = !isViewfinderInLeftTop
                adjustPreviewByFoldingState()
            }
        }
    }

    private fun toggleCamera() {
        closeCamera()
        openCamera(cameraViewfinder.width, cameraViewfinder.height, true)
    }

    private fun saveBitmap() {
        @OptIn(markerClass = [ExperimentalViewfinder::class])
        val bitmap: Bitmap? = cameraViewfinder.bitmap
        bitmap?.let { saveBitmapAsFile(it) }
    }

    private fun saveBitmapAsFile(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
            val displayName = dateFormat.format(Date()) + "_ViewfinderBitmap.png"
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            val resolver = requireContext().contentResolver
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uri = resolver.insert(contentUri, values)
            try {
                val fos = resolver.openOutputStream(uri!!)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos!!.close()
                showToast("Saved: $displayName")
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "saveBitmapAsFile IOException message = " + e.message
                )
            }
        } else {
            try {
                val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
                val file = File(
                    getBatchDirectoryName(),
                    dateFormat.format(Date()) + "_ViewfinderBitmap.png"
                )
                val fos = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
                showToast("Saved: ViewfinderBitmap.png")
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "saveBitmapAsFile IOException message = " + e.message
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getBatchDirectoryName(): String {
        val appFolderPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
        val dir = File(appFolderPath)
        return if (!dir.exists() && !dir.mkdirs()) {
            ""
        } else appFolderPath
    }

    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
        if (flashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        }
    }

    private fun showToast(text: String) {
        val activity: Activity? = activity
        activity?.runOnUiThread {
            Toast.makeText(
                activity,
                text,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Saves a JPEG [Image] into the specified [File].
     */
    private class ImageSaver(
        /**
         * The JPEG image
         */
        private val image: Image,
        /**
         * The file we save the image into.
         */
        private val file: File
    ) :
        Runnable {
        override fun run() {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer[bytes]
            try {
                image.use {
                    FileOutputStream(file).use { output ->
                        output.write(
                            bytes
                        )
                    }
                }
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "ImageSaver CameraAccessException message = " + e.message
                )
            }
        }
    }

    /**
     * Shows an error message dialog.
     */
    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ -> requireActivity().finish() }
                .create()

        companion object {
            @JvmStatic private val ARG_MESSAGE = "message"
            @JvmStatic fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    class ConfirmationDialog : DialogFragment() {
        @Suppress("DEPRECATION")
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(R.string.request_permission)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    requireParentFragment().requestPermissions(arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CAMERA_PERMISSION)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    requireParentFragment().activity?.finish()
                }
                .create()
    }

    companion object {
        /**
         * Conversion from screen rotation to JPEG orientation.
         */
        private const val REQUEST_CAMERA_PERMISSION: Int = 1
        private const val FRAGMENT_DIALOG = "dialog"

        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        /**
         * Tag for the [Log].
         */
        private const val TAG = "Camera2BasicFragment"

        /**
         * Camera state: Showing camera preview.
         */
        private const val STATE_PREVIEW = 0

        /**
         * Camera state: Waiting for the focus to be locked.
         */
        private const val STATE_WAITING_LOCK = 1

        /**
         * Camera state: Waiting for the exposure to be precapture state.
         */
        private const val STATE_WAITING_PRECAPTURE = 2

        /**
         * Camera state: Waiting for the exposure state to be something other than precapture.
         */
        private const val STATE_WAITING_NON_PRECAPTURE = 3

        /**
         * Camera state: Picture was taken.
         */
        private const val STATE_PICTURE_TAKEN = 4

        /**
         * Max preview width that is guaranteed by Camera2 API
         */
        private const val MAX_PREVIEW_WIDTH = 1920

        /**
         * Max preview height that is guaranteed by Camera2 API
         */
        private const val MAX_PREVIEW_HEIGHT = 1080

        /**
         * Given `choices` of `Size`s supported by a camera, choose the smallest one that
         * is at least as large as the respective view size, and that is at most as large as
         * the respective max size, and whose aspect ratio matches with the specified value. If such
         * size doesn't exist, choose the largest one that is at most as large as the respective max
         * size, and whose aspect ratio matches with the specified value.
         *
         * @param choices           The list of sizes that the camera supports for the intended
         *                          output class
         * @param viewWidth  The width of the view relative to sensor coordinate
         * @param viewHeight The height of the view relative to sensor coordinate
         * @param maxWidth          The maximum width that can be chosen
         * @param maxHeight         The maximum height that can be chosen
         * @param aspectRatio       The aspect ratio
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
        @JvmStatic private fun chooseOptimalSize(
            choices: Array<Size>,
            viewWidth: Int,
            viewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size
        ): Size {
            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w) {
                    if (option.width >= viewWidth && option.height >= viewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            return if (bigEnough.size > 0) {
                Collections.min(bigEnough, CompareSizesByArea())
            } else if (notBigEnough.size > 0) {
                Collections.max(notBigEnough, CompareSizesByArea())
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size")
                choices[0]
            }
        }
    }
}