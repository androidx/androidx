/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.integration.extensions.proguard

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.ViewStub
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.ExtensionsApplication
import androidx.camera.integration.extensions.ImplementationOption.CAMERA2_IMPLEMENTATION_OPTION
import androidx.camera.integration.extensions.ImplementationOption.CAMERA_PIPE_IMPLEMENTATION_OPTION
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_CAMERA_IMPLEMENTATION
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_CAMERA_ID
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_EXTENSION_MODE
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_RESULT_ERROR_MESSAGE
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_RUNNING_MODE_CHECK
import androidx.camera.integration.extensions.PERMISSIONS_REQUEST_CODE
import androidx.camera.integration.extensions.R
import androidx.camera.integration.extensions.RequestResultErrorCode.RESULT_ERROR_EXTENSION_MOD_NOT_SUPPORTED
import androidx.camera.integration.extensions.RequestResultErrorCode.RESULT_ERROR_FAILED_TO_RETRIEVE_EXTENSIONS_MANAGER
import androidx.camera.integration.extensions.RequestResultErrorCode.RESULT_ERROR_INCORRECT_CAMERA_IMPLEMENTATION
import androidx.camera.integration.extensions.RequestResultErrorCode.RESULT_ERROR_PERMISSION_NOT_SATISFIED
import androidx.camera.integration.extensions.RequestResultErrorCode.RESULT_ERROR_RUNNING_MODE_INCORRECT
import androidx.camera.integration.extensions.RequestResultErrorCode.RESULT_ERROR_TAKE_PICTURE_FAILED
import androidx.camera.integration.extensions.RequestResultErrorCode.RESULT_SUCCESS
import androidx.camera.integration.extensions.utils.CameraSelectorUtil.createCameraSelectorById
import androidx.camera.integration.extensions.utils.PermissionUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.ProcessCameraProvider.Companion.getInstance
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures

private const val TAG = "ReleaseTestActivity"
// Stage that is waiting for the preview stream state becoming STREAMING.
private const val STAGE_WAIT_FOR_PREVIEW_STREAMING = 0
// Stage that is waiting for a still image is captured and saved successfully.
private const val STAGE_WAIT_FOR_STILL_IMAGE_CAPTURE = 1

/**
 * An activity is specially implemented for release apk test.
 *
 * After the activity is launched, the following steps will be executed:
 * <ul>
 * <li> Checks whether the target running mode is correct if it is specified.
 * <li> Checks whether the target extension mode is supported for the target camera device.
 * <li> Wait for the preview STREAMING state after binding Preview and ImageCapture UseCases.
 * <li> Wait for still image capture result after preview becomes STREAMING state.
 * <li> Finish the activity with RESULT_ERROR_NONE if all above steps can run successfully.
 * </ul>
 */
class ReleaseTestActivity : AppCompatActivity() {
    private var permissionsGranted: Boolean = false
    private lateinit var permissionCompleter: CallbackToFutureAdapter.Completer<Boolean>

    private var cameraImplementation: String = CAMERA2_IMPLEMENTATION_OPTION
    // When the retrieved implementation is incorrect, one time retry will be allowed.
    private var hasRetried = false
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager

    private var currentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    @ExtensionMode.Mode private var currentExtensionMode = ExtensionMode.BOKEH
    private lateinit var camera: Camera

    private lateinit var preview: Preview
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture

    private var currentStreamState: PreviewView.StreamState? = null
    private val resultIntent = Intent()
    private var runningStage = STAGE_WAIT_FOR_PREVIEW_STREAMING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sets the default result code if no any problem occurs when the activity is finished
        setResult(RESULT_SUCCESS)
        // Checks whether the running mode matches the specified one
        checkRunningMode()

        setContentView(R.layout.activity_release_test)
        setTitle(R.string.camerax_extensions)

        intent.getStringExtra(INTENT_EXTRA_CAMERA_IMPLEMENTATION)?.let { cameraImplementation = it }

        intent.getStringExtra(INTENT_EXTRA_KEY_CAMERA_ID)?.let {
            currentCameraSelector = createCameraSelectorById(it)
        }

        currentExtensionMode =
            intent.getIntExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, currentExtensionMode)

        val policy = VmPolicy.Builder().detectAll().penaltyLog().build()
        StrictMode.setVmPolicy(policy)

        val viewFinderStub = findViewById<ViewStub>(R.id.viewFinderStub)
        viewFinderStub.layoutResource = R.layout.full_previewview
        previewView = viewFinderStub.inflate() as PreviewView
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE)

        val futureCompleter = PermissionUtil.setupPermissions(this)
        permissionCompleter = futureCompleter.second
        Futures.addCallback<Boolean>(
            futureCompleter.first,
            object : FutureCallback<Boolean?> {
                override fun onSuccess(result: Boolean?) {
                    permissionsGranted = Preconditions.checkNotNull(result)

                    if (!permissionsGranted) {
                        Log.d(TAG, "Required permissions are not all granted!")
                        Toast.makeText(
                                this@ReleaseTestActivity,
                                "Required permissions are not " + "all granted!",
                                Toast.LENGTH_LONG
                            )
                            .show()
                        setResultAndFinishActivity(
                            RESULT_ERROR_PERMISSION_NOT_SATISFIED,
                            "Permission requirements are not satisfied."
                        )
                        return
                    }

                    configAndRetrieveCameraProvider()
                }

                override fun onFailure(t: Throwable) {
                    setResultAndFinishActivity(
                        RESULT_ERROR_PERMISSION_NOT_SATISFIED,
                        "Permission requirements are not satisfied."
                    )
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun checkRunningMode() {
        val runningModeCheck = intent.getStringExtra(INTENT_EXTRA_RUNNING_MODE_CHECK) ?: return

        val inDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        if (
            ("release" == runningModeCheck && !inDebug) || ("debug" == runningModeCheck && inDebug)
        ) {
            return
        }

        setResultAndFinishActivity(
            RESULT_ERROR_RUNNING_MODE_INCORRECT,
            "Running mode check failed! The target running mode is $runningModeCheck"
        )
    }

    private fun configAndRetrieveCameraProvider() {
        (application as ExtensionsApplication).cameraXConfig =
            if (cameraImplementation.equals(CAMERA_PIPE_IMPLEMENTATION_OPTION)) {
                CameraPipeConfig.defaultConfig()
            } else {
                Camera2Config.defaultConfig()
            }

        val cameraProviderFuture = getInstance(this@ReleaseTestActivity)

        Futures.addCallback<ProcessCameraProvider>(
            cameraProviderFuture,
            object : FutureCallback<ProcessCameraProvider> {
                @SuppressLint("VisibleForTests")
                override fun onSuccess(result: ProcessCameraProvider) {
                    cameraProvider = result

                    if (!checkCameraImplementation()) {
                        if (hasRetried) {
                            setResultAndFinishActivity(
                                RESULT_ERROR_INCORRECT_CAMERA_IMPLEMENTATION,
                                "Can not setup implementation correctly."
                            )
                            return
                        }
                        cameraProvider
                            .shutdownAsync()
                            .addListener(
                                {
                                    if (hasRetried) {
                                        return@addListener
                                    }
                                    hasRetried = true
                                    configAndRetrieveCameraProvider()
                                },
                                ContextCompat.getMainExecutor(this@ReleaseTestActivity)
                            )
                        return
                    }

                    setupCamera()
                }

                override fun onFailure(t: Throwable) {
                    throw RuntimeException("Failed to get camera provider", t)
                }
            },
            ContextCompat.getMainExecutor(this@ReleaseTestActivity)
        )
    }

    /** Checks whether the camera implementation mode is correct. */
    private fun checkCameraImplementation(): Boolean {
        camera = cameraProvider.bindToLifecycle(this, currentCameraSelector)

        if (cameraImplementation.equals(CAMERA_PIPE_IMPLEMENTATION_OPTION)) {
            if (!isCameraPipeImplementation(camera.cameraInfo)) {
                return false
            }
        } else {
            if (!isCamera2Implementation(camera.cameraInfo)) {
                return false
            }
        }

        return true
    }

    @OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    private fun isCamera2Implementation(cameraInfo: CameraInfo): Boolean {
        try {
            androidx.camera.camera2.interop.Camera2CameraInfo.from(cameraInfo)
        } catch (e: IllegalArgumentException) {
            return false
        }
        return true
    }

    @kotlin.OptIn(ExperimentalCamera2Interop::class)
    private fun isCameraPipeImplementation(cameraInfo: CameraInfo): Boolean {
        try {
            androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo.from(cameraInfo)
        } catch (e: IllegalArgumentException) {
            return false
        }
        return true
    }

    private fun setResultAndFinishActivity(resultErrorCode: Int, errorMessage: String? = null) {
        errorMessage?.let { resultIntent.putExtra(INTENT_EXTRA_RESULT_ERROR_MESSAGE, errorMessage) }
        setResult(resultErrorCode, resultIntent)
        runOnUiThread(::finish)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != PERMISSIONS_REQUEST_CODE) {
            return
        }

        // If request is cancelled, the result arrays are empty.
        if (grantResults.isEmpty()) {
            permissionCompleter.set(false)
            return
        }

        var allPermissionGranted = true

        for (grantResult in grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                allPermissionGranted = false
                break
            }
        }

        Log.d(TAG, "All permissions granted: $allPermissionGranted")
        permissionCompleter.set(allPermissionGranted)
    }

    private fun setupCamera() {
        if (!permissionsGranted) {
            Log.d(TAG, "Permissions denied.")
            return
        }
        if (isDestroyed) {
            Log.d(TAG, "Activity is destroyed, not to create LifecycleCamera.")
            return
        }

        camera = cameraProvider.bindToLifecycle(this, currentCameraSelector)

        val extensionsManagerFuture =
            ExtensionsManager.getInstanceAsync(applicationContext, cameraProvider)

        Futures.addCallback<ExtensionsManager>(
            extensionsManagerFuture,
            object : FutureCallback<ExtensionsManager> {
                override fun onSuccess(extensionsManager: ExtensionsManager) {
                    // There might be timing issue that the activity has been destroyed when
                    // the onSuccess callback is received. Skips the afterward flow when the
                    // situation happens.
                    if (
                        this@ReleaseTestActivity.lifecycle.currentState == Lifecycle.State.DESTROYED
                    ) {
                        return
                    }
                    this@ReleaseTestActivity.extensionsManager = extensionsManager
                    bindUseCases()
                }

                override fun onFailure(throwable: Throwable) {
                    setResultAndFinishActivity(
                        RESULT_ERROR_FAILED_TO_RETRIEVE_EXTENSIONS_MANAGER,
                        "Failed to retrieve ExtensionsManager."
                    )
                }
            },
            ContextCompat.getMainExecutor(this@ReleaseTestActivity)
        )
    }

    private fun bindUseCases() {
        if (!extensionsManager.isExtensionAvailable(currentCameraSelector, currentExtensionMode)) {
            setResultAndFinishActivity(
                RESULT_ERROR_EXTENSION_MOD_NOT_SUPPORTED,
                "Mode $currentExtensionMode is not supported!"
            )
            return
        }

        cameraProvider.unbindAll()

        val cameraSelector =
            extensionsManager.getExtensionEnabledCameraSelector(
                currentCameraSelector,
                currentExtensionMode
            )

        camera = cameraProvider.bindToLifecycle(this, cameraSelector)

        imageCapture = ImageCapture.Builder().setTargetName("ImageCapture").build()

        preview = Preview.Builder().setTargetName("Preview").build()
        currentStreamState = null
        preview.surfaceProvider = previewView.surfaceProvider

        // Observes the stream state for the unit tests to know the preview status.
        previewView.previewStreamState.removeObservers(this)
        previewView.previewStreamState.observeForever { streamState: PreviewView.StreamState ->
            currentStreamState = streamState
            if (
                streamState == PreviewView.StreamState.STREAMING &&
                    runningStage == STAGE_WAIT_FOR_PREVIEW_STREAMING
            ) {
                runningStage = STAGE_WAIT_FOR_STILL_IMAGE_CAPTURE
                // Trigger photo-taking flow
                takePicture()
            }
        }

        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
    }

    private fun takePicture() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this@ReleaseTestActivity),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    Log.d(TAG, "Still image is captured successfully!")
                    image.close()
                    // Force finish if the still image can be captured successfully
                    setResultAndFinishActivity(RESULT_SUCCESS)
                }

                override fun onError(exception: ImageCaptureException) {
                    val errorMessage =
                        "Failed to capture image - ${exception.message}, ${exception.cause}"
                    Log.e(TAG, errorMessage)
                    setResultAndFinishActivity(RESULT_ERROR_TAKE_PICTURE_FAILED, errorMessage)
                }
            }
        )
    }
}
