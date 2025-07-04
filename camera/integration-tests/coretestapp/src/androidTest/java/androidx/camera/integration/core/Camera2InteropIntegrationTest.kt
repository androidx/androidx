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

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package androidx.camera.integration.core

import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Range
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ExtendableBuilder
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.ExtensionsUtil
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
import androidx.camera.testing.impl.util.Camera2InteropUtil
import androidx.camera.testing.impl.util.Camera2InteropUtil.builder
import androidx.camera.testing.impl.util.Camera2InteropUtil.from
import androidx.concurrent.futures.await
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class Camera2InteropIntegrationTest(val implName: String, val cameraConfig: CameraXConfig) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    private var processCameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraSelector: CameraSelector
    lateinit var captureCallback: Camera2InteropUtil.CaptureCallback

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()

        // Configures the test target config
        ProcessCameraProvider.configureInstance(cameraConfig)
        processCameraProvider = ProcessCameraProvider.awaitInstance(context)
        assumeTrue(processCameraProvider!!.hasCamera(cameraSelector))
        captureCallback = Camera2InteropUtil.CaptureCallback()
    }

    @After
    fun tearDown(): Unit = runBlocking { processCameraProvider?.apply { shutdownAsync().await() } }

    @Test
    fun cameraDeviceListener_receivesClose_afterUnbindAll(): Unit = runBlocking {
        val previewBuilder = Preview.Builder()
        val deviceStateFlow = previewBuilder.createDeviceStateFlow()

        withContext(Dispatchers.Main) {
            processCameraProvider!!.bindToLifecycle(
                TestLifecycleOwner(Lifecycle.State.RESUMED),
                cameraSelector,
                previewBuilder.build(),
            )
        }

        // Call unbindAll() once device state is [DeviceState.Opened]. If another state occurs
        // first, the StateFlow subscriber will unsubscribe with the last state. Once
        // [DeviceState.Opened] occurs, we will call unbindAll(), and the next state should be
        // [DeviceState.Closed].
        var unbindAllCalled = false
        val lastState =
            deviceStateFlow
                .dropWhile { state ->
                    when (state) {
                        is DeviceState.Unknown ->
                            true // Filter out this state from the downstream flow
                        is DeviceState.Opened -> {
                            withContext(Dispatchers.Main) { processCameraProvider!!.unbindAll() }
                            unbindAllCalled = true
                            true // Filter out this state from the downstream flow
                        }
                        else -> false // Forward to the downstream flow
                    }
                }
                .first()

        assertThat(unbindAllCalled).isTrue()
        assertThat(lastState).isEqualTo(DeviceState.Closed)
    }

    @Test
    fun cameraSessionListener_receivesReady_afterBindUseCase(): Unit = runBlocking {
        val imageCaptureBuilder = ImageCapture.Builder()
        val sessionStateFlow = imageCaptureBuilder.createSessionStateFlow()
        withContext(Dispatchers.Main) {
            processCameraProvider!!.bindToLifecycle(
                TestLifecycleOwner(Lifecycle.State.RESUMED),
                cameraSelector,
                imageCaptureBuilder.build(),
            )
        }

        val lastState =
            withTimeoutOrNull(10000) {
                sessionStateFlow
                    .dropWhile { state ->
                        when (state) {
                            // Filter out this state from the downstream flow
                            is SessionState.Unknown -> true
                            is SessionState.Configured -> true
                            is SessionState.Ready -> {
                                withContext(Dispatchers.Main) {
                                    processCameraProvider!!.unbindAll()
                                }
                                false
                            }
                            else -> false // Forward to the downstream flow
                        }
                    }
                    .first()
            } ?: SessionState.Unknown

        assertThat(lastState).isEqualTo(SessionState.Ready)
    }

    @Test
    fun canUseCameraSelector_fromCamera2CameraIdAndCameraFilter(): Unit = runBlocking {
        val camera2CameraManager =
            ApplicationProvider.getApplicationContext<Context>().getSystemService(CAMERA_SERVICE)
                as CameraManager
        camera2CameraManager.cameraIdList.forEach continuing@{ id ->
            if (!isBackwardCompatible(camera2CameraManager, id)) {
                return@continuing
            }

            val cameraSelector =
                CameraSelector.Builder()
                    .addCameraFilter { cameraInfoList ->
                        cameraInfoList.filter { cameraInfo ->
                            Camera2InteropUtil.getCameraId(implName, cameraInfo) == id
                        }
                    }
                    .build()

            withContext(Dispatchers.Main) {
                val camera =
                    processCameraProvider!!.bindToLifecycle(
                        TestLifecycleOwner(Lifecycle.State.CREATED),
                        cameraSelector,
                    )
                assertThat(Camera2InteropUtil.getCameraId(implName, camera.cameraInfo))
                    .isEqualTo(id)
            }
        }
    }

    @Test
    fun canUseCameraSelector_fromCamera2CameraIdAndAvailableCameraInfos(): Unit = runBlocking {
        val camera2CameraManager =
            ApplicationProvider.getApplicationContext<Context>().getSystemService(CAMERA_SERVICE)
                as CameraManager
        camera2CameraManager.cameraIdList.forEach continuing@{ id ->
            if (!isBackwardCompatible(camera2CameraManager, id)) {
                return@continuing
            }
            withContext(Dispatchers.Main) {
                val cameraSelector =
                    processCameraProvider!!
                        .availableCameraInfos
                        .find { Camera2InteropUtil.getCameraId(implName, it) == id }
                        ?.cameraSelector

                val camera =
                    processCameraProvider!!.bindToLifecycle(
                        TestLifecycleOwner(Lifecycle.State.CREATED),
                        cameraSelector!!,
                    )
                assertThat(Camera2InteropUtil.getCameraId(implName, camera.cameraInfo))
                    .isEqualTo(id)
            }
        }
    }

    @Test
    fun setCaptureRequestOptions_valueShouldExist(): Unit = runBlocking {
        // Arrange.
        val testKey = CaptureRequest.CONTROL_CAPTURE_INTENT
        val testValue = CaptureRequest.CONTROL_CAPTURE_INTENT_CUSTOM
        val testLifecycle = TestLifecycleOwner(Lifecycle.State.RESUMED)

        // Act.
        withContext(Dispatchers.Main) {
            processCameraProvider!!
                .bindAnalysis(testLifecycle)
                .setInteropOptions(mapOf(testKey to testValue))
        }

        // Assert.
        captureCallback.verifyLastCaptureRequest(mapOf(testKey to testValue))
    }

    @Test
    fun setCaptureRequestOptions_valueShouldExist_afterLifeCycleStopStart(): Unit = runBlocking {
        // Arrange.
        val testKey = CaptureRequest.CONTROL_CAPTURE_INTENT
        val testValue = CaptureRequest.CONTROL_CAPTURE_INTENT_CUSTOM
        val testLifecycle = TestLifecycleOwner(Lifecycle.State.RESUMED)
        withContext(Dispatchers.Main) {
            processCameraProvider!!
                .bindAnalysis(testLifecycle)
                .setInteropOptions(mapOf(testKey to testValue))
        }
        captureCallback.waitFor(numOfCaptures = 10)

        // Act.
        withContext(Dispatchers.Main) {
            testLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            testLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        // Assert.
        captureCallback.verifyLastCaptureRequest(mapOf(testKey to testValue))
    }

    @Test
    fun clearCaptureRequestOptions_valueShouldNotExist(): Unit = runBlocking {
        // Arrange.
        val testKey = CaptureRequest.CONTROL_CAPTURE_INTENT
        val testValue = CaptureRequest.CONTROL_CAPTURE_INTENT_CUSTOM
        val testLifecycle = TestLifecycleOwner(Lifecycle.State.RESUMED)
        lateinit var camera: Camera
        withContext(Dispatchers.Main) {
            camera = processCameraProvider!!.bindAnalysis(testLifecycle)
        }
        camera.setInteropOptions(mapOf(testKey to testValue))
        captureCallback.waitFor(numOfCaptures = 10)

        // Act.
        camera.clearInteropOptions()

        // Assert.
        captureCallback.verifyFor(numOfCaptures = 20) { captureRequests, _ ->
            captureRequests.last()[testKey] != testValue
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    @Suppress("DEPRECATION")
    fun setPhysicalCameraId_valueShouldExist(): Unit = runBlocking {
        // Arrange: Skip the test if the camera is not a logical camera.
        val physicalCameraIds: MutableSet<String>? =
            getCameraCharacteristics(cameraSelector).physicalCameraIds
        assumeTrue(!physicalCameraIds!!.isEmpty())

        val testCameraId = physicalCameraIds.iterator().next()
        val testLifecycle = TestLifecycleOwner(Lifecycle.State.RESUMED)

        // Act.
        withContext(Dispatchers.Main) {
            processCameraProvider!!.bindAnalysis(testLifecycle, testCameraId)
        }

        // Assert.
        captureCallback.verifyFor(numOfCaptures = 20) { _, captureResults ->
            val cameraIds =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    captureResults.last().physicalCameraTotalResults.keys
                } else {
                    captureResults.last().physicalCameraResults.keys
                }
            cameraIds.size == 1 && cameraIds.contains(testCameraId)
        }
    }

    @Test
    fun camera2CameraInfo_getCharacteristics(): Unit = runBlocking {
        val cameraInfo = processCameraProvider!!.getCameraInfo(cameraSelector)

        val maxZoom =
            Camera2InteropUtil.getCamera2CameraInfoCharacteristics(
                implName,
                cameraInfo,
                CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM,
            )

        assertThat(maxZoom)
            .isEqualTo(
                getCameraCharacteristics(cameraSelector)
                    .get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun camera2CameraInfo_getExtensionsSpecificChars(): Unit = runBlocking {
        val zoomRange = Range(1f, 2f)
        val cameraSelectorWithExtensions =
            ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                processCameraProvider!!,
                cameraSelector,
                FakeSessionProcessor(
                    extensionSpecificChars =
                        listOf(
                            android.util.Pair(
                                CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE,
                                zoomRange,
                            )
                        )
                ),
            )
        val cameraInfoWithExtensions =
            processCameraProvider!!.getCameraInfo(cameraSelectorWithExtensions)
        assertThat(
                Camera2InteropUtil.getCamera2CameraInfoCharacteristics(
                    implName,
                    cameraInfoWithExtensions,
                    CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE,
                )
            )
            .isEqualTo(zoomRange)

        // Ensure it doesn't impact the regular Camera2CameraInfo without Extensions.
        val cameraInfoWithoutExtensions = processCameraProvider!!.getCameraInfo(cameraSelector)
        assertThat(
                Camera2InteropUtil.getCamera2CameraInfoCharacteristics(
                    implName,
                    cameraInfoWithoutExtensions,
                    CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE,
                )
            )
            .isEqualTo(
                getCameraCharacteristics(cameraSelector)
                    .get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            )
    }

    private fun ProcessCameraProvider.bindAnalysis(
        lifecycleOwner: LifecycleOwner,
        physicalCameraId: String? = null,
    ): Camera {
        val imageAnalysis =
            ImageAnalysis.Builder()
                .also { imageAnalysisBuilder ->
                    Camera2InteropUtil.setCamera2InteropOptions(
                        implName = implName,
                        builder = imageAnalysisBuilder,
                        captureCallback = captureCallback,
                        physicalCameraId = physicalCameraId,
                    )
                }
                .build()
                .apply {
                    // set analyzer to make it active.
                    setAnalyzer(CameraXExecutors.highPriorityExecutor()) {
                        // Fake analyzer, do nothing. Close the ImageProxy immediately to prevent
                        // the
                        // closing of the CameraDevice from being stuck.
                        it.close()
                    }
                }

        unbindAll()
        return bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
    }

    private fun Camera.setInteropOptions(parameter: Map<CaptureRequest.Key<Int>, Int>) {
        Camera2InteropUtil.Camera2CameraControlWrapper.from(implName, cameraControl).apply {
            setCaptureRequestOptions(
                Camera2InteropUtil.CaptureRequestOptionsWrapper.builder(implName)
                    .apply {
                        parameter.forEach { (key, value) -> setCaptureRequestOption(key, value) }
                    }
                    .build()
            )
        }
    }

    private fun Camera.clearInteropOptions() {
        Camera2InteropUtil.Camera2CameraControlWrapper.from(implName, cameraControl)
            .clearCaptureRequestOptions()
    }

    // Sealed class for converting CameraDevice.StateCallback into a StateFlow
    sealed class DeviceState {
        object Unknown : DeviceState()

        object Opened : DeviceState()

        object Closed : DeviceState()

        object Disconnected : DeviceState()

        data class Error(val errorCode: Int) : DeviceState()
    }

    // Sealed class for converting CameraDevice.StateCallback into a StateFlow
    sealed class SessionState {
        object Unknown : SessionState()

        object Ready : SessionState()

        object Configured : SessionState()

        object ConfigureFailed : SessionState()

        object Closed : SessionState()
    }

    /**
     * Returns a [StateFlow] which will signal the states of the camera defined in [DeviceState].
     */
    private fun <T> ExtendableBuilder<T>.createDeviceStateFlow(): StateFlow<DeviceState> =
        MutableStateFlow<DeviceState>(DeviceState.Unknown)
            .apply {
                val stateCallback =
                    object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            // tryEmit should always succeed for [MutableStateFlow]
                            value = DeviceState.Opened
                        }

                        override fun onClosed(camera: CameraDevice) {
                            tryEmit(DeviceState.Closed)
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            tryEmit(DeviceState.Disconnected)
                        }

                        override fun onError(camera: CameraDevice, errorCode: Int) {
                            tryEmit(DeviceState.Error(errorCode))
                        }
                    }
                Camera2InteropUtil.setDeviceStateCallback(
                    implName,
                    this@createDeviceStateFlow,
                    stateCallback,
                )
            }
            .asStateFlow()

    /**
     * Returns a [StateFlow] which will signal the states of the camera defined in [SessionState].
     */
    private fun <T> ExtendableBuilder<T>.createSessionStateFlow(): SharedFlow<SessionState> =
        MutableSharedFlow<SessionState>(extraBufferCapacity = 10)
            .apply {
                val stateCallback =
                    object : CameraCaptureSession.StateCallback() {
                        override fun onReady(session: CameraCaptureSession) {
                            tryEmit(SessionState.Ready)
                        }

                        override fun onConfigured(session: CameraCaptureSession) {
                            tryEmit(SessionState.Configured)
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            tryEmit(SessionState.ConfigureFailed)
                        }

                        override fun onClosed(session: CameraCaptureSession) {
                            tryEmit(SessionState.Closed)
                        }
                    }
                Camera2InteropUtil.setSessionStateCallback(
                    implName,
                    this@createSessionStateFlow,
                    stateCallback,
                )
            }
            .asSharedFlow()

    private fun isBackwardCompatible(cameraManager: CameraManager, cameraId: String): Boolean {
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val capabilities =
            cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

        capabilities?.let {
            return it.contains(REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
        }

        return false
    }

    private fun getCameraCharacteristics(cameraSelector: CameraSelector): CameraCharacteristics {
        val cameraManager =
            ApplicationProvider.getApplicationContext<Context>().getSystemService(CAMERA_SERVICE)
                as CameraManager
        return cameraManager.getCameraCharacteristics(
            CameraUtil.getCameraIdWithLensFacing(cameraSelector.lensFacing!!)!!
        )
    }
}
