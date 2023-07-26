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
import android.hardware.camera2.TotalCaptureResult
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
import androidx.camera.integration.core.util.CameraPipeUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.concurrent.futures.await
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class Camera2InteropIntegrationTest(
    val implName: String,
    val cameraConfig: CameraXConfig,
) {

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(cameraConfig)
    )

    private var processCameraProvider: ProcessCameraProvider? = null

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Configures the test target config
        ProcessCameraProvider.configureInstance(cameraConfig)
        processCameraProvider = ProcessCameraProvider.getInstance(context).await()

        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
    }

    @After
    fun tearDown(): Unit = runBlocking {
        processCameraProvider?.apply {
            shutdown().await()
        }
    }

    @Test
    fun cameraDeviceListener_receivesClose_afterUnbindAll(): Unit = runBlocking {
        val previewBuilder = Preview.Builder()
        val deviceStateFlow = previewBuilder.createDeviceStateFlow()

        withContext(Dispatchers.Main) {
            processCameraProvider!!.bindToLifecycle(
                TestLifecycleOwner(Lifecycle.State.RESUMED),
                CameraSelector.DEFAULT_BACK_CAMERA, previewBuilder.build()
            )
        }

        // Call unbindAll() once device state is [DeviceState.Opened]. If another state occurs
        // first, the StateFlow subscriber will unsubscribe with the last state. Once
        // [DeviceState.Opened] occurs, we will call unbindAll(), and the next state should be
        // [DeviceState.Closed].
        var unbindAllCalled = false
        val lastState = deviceStateFlow.dropWhile { state ->
            when (state) {
                is DeviceState.Unknown -> true // Filter out this state from the downstream flow
                is DeviceState.Opened -> {
                    withContext(Dispatchers.Main) { processCameraProvider!!.unbindAll() }
                    unbindAllCalled = true
                    true // Filter out this state from the downstream flow
                }

                else -> false // Forward to the downstream flow
            }
        }.first()

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
                CameraSelector.DEFAULT_BACK_CAMERA,
                imageCaptureBuilder.build()
            )
        }

        val lastState = withTimeoutOrNull(10000) {
            sessionStateFlow.dropWhile { state ->
                when (state) {
                    // Filter out this state from the downstream flow
                    is SessionState.Unknown -> true
                    is SessionState.Configured -> true
                    is SessionState.Ready -> {
                        withContext(Dispatchers.Main) { processCameraProvider!!.unbindAll() }
                        false
                    }

                    else -> false // Forward to the downstream flow
                }
            }.first()
        } ?: SessionState.Unknown

        assertThat(lastState).isEqualTo(SessionState.Ready)
    }

    @Test
    fun canUseCameraSelector_fromCamera2CameraIdAndCameraFilter(): Unit = runBlocking {
        val camera2CameraManager = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(CAMERA_SERVICE) as CameraManager
        camera2CameraManager.cameraIdList.forEach continuing@{ id ->
            if (!isBackwardCompatible(camera2CameraManager, id)) {
                return@continuing
            }

            val cameraSelector = CameraSelector.Builder()
                .addCameraFilter { cameraInfoList ->
                    cameraInfoList.filter { cameraInfo ->
                        CameraPipeUtil.getCameraId(
                            implName,
                            cameraInfo
                        ) == id
                    }
                }.build()

            withContext(Dispatchers.Main) {
                val camera = processCameraProvider!!.bindToLifecycle(
                    TestLifecycleOwner(Lifecycle.State.CREATED),
                    cameraSelector
                )
                assertThat(
                    CameraPipeUtil.getCameraId(implName, camera.cameraInfo)
                ).isEqualTo(id)
            }
        }
    }

    @Test
    fun canUseCameraSelector_fromCamera2CameraIdAndAvailableCameraInfos(): Unit = runBlocking {
        val camera2CameraManager = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(CAMERA_SERVICE) as CameraManager
        camera2CameraManager.cameraIdList.forEach continuing@{ id ->
            if (!isBackwardCompatible(camera2CameraManager, id)) {
                return@continuing
            }
            withContext(Dispatchers.Main) {
                val cameraSelector =
                    processCameraProvider!!.availableCameraInfos.find {
                        CameraPipeUtil.getCameraId(implName, it) == id
                    }?.cameraSelector

                val camera = processCameraProvider!!.bindToLifecycle(
                    TestLifecycleOwner(Lifecycle.State.CREATED),
                    cameraSelector!!
                )
                assertThat(
                    CameraPipeUtil.getCameraId(implName, camera.cameraInfo)
                ).isEqualTo(id)
            }
        }
    }

    @Test
    fun requestOptionsShouldExist_afterLifeCycleStopStart(): Unit = runBlocking {
        // Arrange.
        val testKey = CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
        val testValue = 1
        val testLifecycle = TestLifecycleOwner(Lifecycle.State.RESUMED)
        withContext(Dispatchers.Main) {
            processCameraProvider!!.bindAnalysis(testLifecycle).setInteropOptions(
                mapOf(testKey to testValue)
            )
        }
        captureCallback.waitFor(numOfCaptures = 10) {}

        // Act.
        withContext(Dispatchers.Main) {
            testLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            testLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        // Assert.
        captureCallback.waitFor(numOfCaptures = 20) {
            assertThat(it.last().get(testKey)).isEqualTo(testValue)
        }
    }

    private fun ProcessCameraProvider.bindAnalysis(lifecycleOwner: LifecycleOwner): Camera {
        val imageAnalysis = ImageAnalysis.Builder().also { imageAnalysisBuilder ->
            CameraPipeUtil.setCameraCaptureSessionCallback(
                implName,
                imageAnalysisBuilder,
                captureCallback
            )
        }.build().apply {
            // set analyzer to make it active.
            setAnalyzer(
                CameraXExecutors.highPriorityExecutor()
            ) {
                // Fake analyzer, do nothing. Close the ImageProxy immediately to prevent the
                // closing of the CameraDevice from being stuck.
                it.close()
            }
        }

        unbindAll()
        return bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            imageAnalysis
        )
    }

    private fun Camera.setInteropOptions(parameter: Map<CaptureRequest.Key<Int>, Int>) {
        CameraPipeUtil.setRequestOptions(implName, cameraControl, parameter)
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        val waitingList = mutableListOf<CaptureContainer>()

        fun waitFor(
            timeout: Long = TimeUnit.SECONDS.toMillis(5),
            numOfCaptures: Int = 1,
            verifyResults: (captureRequests: List<CaptureRequest>) -> Unit
        ) {
            val resultContainer = CaptureContainer(CountDownLatch(numOfCaptures))
            waitingList.add(resultContainer)
            assertTrue(resultContainer.countDownLatch.await(timeout, TimeUnit.MILLISECONDS))
            verifyResults(resultContainer.captureRequests)
            waitingList.remove(resultContainer)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            waitingList.toList().forEach {
                it.captureRequests.add(request)
                it.countDownLatch.countDown()
            }
        }
    }

    data class CaptureContainer(
        val countDownLatch: CountDownLatch,
        val captureRequests: MutableList<CaptureRequest> = mutableListOf()
    )

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
        MutableStateFlow<DeviceState>(DeviceState.Unknown).apply {
            val stateCallback = object : CameraDevice.StateCallback() {
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
            CameraPipeUtil.setDeviceStateCallback(
                implName,
                this@createDeviceStateFlow,
                stateCallback
            )
        }.asStateFlow()

    /**
     * Returns a [StateFlow] which will signal the states of the camera defined in [SessionState].
     */
    private fun <T> ExtendableBuilder<T>.createSessionStateFlow(): StateFlow<SessionState> =
        MutableStateFlow<SessionState>(SessionState.Unknown).apply {
            val stateCallback = object : CameraCaptureSession.StateCallback() {
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
            CameraPipeUtil.setSessionStateCallback(
                implName,
                this@createSessionStateFlow,
                stateCallback
            )
        }.asStateFlow()

    private fun isBackwardCompatible(cameraManager: CameraManager, cameraId: String): Boolean {
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val capabilities =
            cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

        capabilities?.let {
            return it.contains(REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
        }

        return false
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }
}
