/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.integration.core

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.StreamConfigurationMapBuilder

/**
 * An integration-level Robolectric test to verify that CameraState transitions correctly when a
 * camera device fails to open for various reasons.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(
    minSdk = Build.VERSION_CODES.M,
    shadows = [TestShadowCameraManager::class, TestShadowCameraDeviceImpl::class],
)
class CameraStateRobolectricTest(private val config: TestConfig) {

    data class TestConfig(
        val implName: String,
        val openError: ShadowCameraAgent.DeviceOpenError,
        val expectedCameraStateTypes: Set<CameraState.Type>,
        val expectedErrorCode: Int,
    ) {
        override fun toString(): String = "$implName - ${openError.name}"
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private lateinit var cameraManager: CameraManager
    private lateinit var shadowCameraManager: ShadowCameraManager
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var testSchedulerThread: HandlerThread
    private lateinit var testSchedulerHandler: Handler
    private lateinit var testCameraExecutor: Executor
    private lateinit var shadowAgent: ShadowCameraAgent

    companion object {
        const val FAKE_CAMERA_ID = "0"
        private val TEST_CAMERA_FRAME_SIZE: Size = Size(1280, 720)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun data(): Collection<TestConfig> {
            val impls = listOf("Camera2")
            val errorScenarios =
                listOf(
                    // Recoverable errors should transition to OPENING or PENDING_OPEN
                    Triple(
                        ShadowCameraAgent.DeviceOpenError.CAMERA_IN_USE,
                        setOf(CameraState.Type.OPENING, CameraState.Type.PENDING_OPEN),
                        CameraState.ERROR_CAMERA_IN_USE,
                    ),
                    Triple(
                        ShadowCameraAgent.DeviceOpenError.MAX_CAMERAS_IN_USE,
                        setOf(CameraState.Type.OPENING, CameraState.Type.PENDING_OPEN),
                        CameraState.ERROR_MAX_CAMERAS_IN_USE,
                    ),
                    Triple(
                        ShadowCameraAgent.DeviceOpenError.CAMERA_DEVICE_ERROR,
                        setOf(CameraState.Type.OPENING, CameraState.Type.PENDING_OPEN),
                        CameraState.ERROR_OTHER_RECOVERABLE_ERROR,
                    ),
                    // Fatal errors should transition to CLOSING (and then CLOSED)
                    Triple(
                        ShadowCameraAgent.DeviceOpenError.CAMERA_DISABLED,
                        setOf(CameraState.Type.CLOSING),
                        CameraState.ERROR_CAMERA_DISABLED,
                    ),
                    Triple(
                        ShadowCameraAgent.DeviceOpenError.CAMERA_SERVICE_ERROR,
                        setOf(CameraState.Type.CLOSING),
                        CameraState.ERROR_CAMERA_FATAL_ERROR,
                    ),
                )

            return impls.flatMap { implName ->
                errorScenarios.map { (error, state, code) ->
                    TestConfig(implName, error, state, code)
                }
            }
        }
    }

    @Before
    fun setUp() {
        ShadowLog.stream = System.out
        val configBuilder =
            when (config.implName) {
                "Camera2" -> CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
                else -> throw IllegalArgumentException("Unknown impl name: ${config.implName}")
            }

        testSchedulerThread = HandlerThread("CameraStateTestScheduler")
        testSchedulerThread.start()
        testSchedulerHandler = Handler(testSchedulerThread.looper)
        testCameraExecutor = Executors.newFixedThreadPool(2)

        val cameraXConfig =
            configBuilder
                .setSchedulerHandler(testSchedulerHandler)
                .setCameraExecutor(testCameraExecutor)
                .build()

        shadowAgent = ShadowCameraAgent(testSchedulerHandler)
        ShadowCameraBridge.agent = shadowAgent

        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        shadowCameraManager = shadowOf(cameraManager)
        addFakeCamera(FAKE_CAMERA_ID)

        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context).get(10, TimeUnit.SECONDS)

        fakeLifecycleOwner = FakeLifecycleOwner()
        fakeLifecycleOwner.startAndResume()
        flushLoopers()
    }

    @After
    fun tearDown() {
        cameraProvider?.shutdownAsync()?.get(10, TimeUnit.SECONDS)
        testSchedulerThread.quitSafely()
        ShadowCameraBridge.agent = null
    }

    @Ignore // b/446723558
    @Test
    @LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
    fun cameraStateTransitionsToCorrectError_whenOpenFails() {
        // Arrange: Configure the agent to fail the next camera open call with a specific error.
        shadowAgent.failNextOpenCameraWith(config.openError)

        val cameraErrorLatch = CountDownLatch(1)
        var capturedState: CameraState? = null
        var isErrorCaptured = false

        // Act: Bind a use case, which will trigger the camera open call that we've hijacked.
        mainThreadHandler.post {
            val preview = Preview.Builder().build()
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            val camera =
                cameraProvider!!.bindToLifecycle(
                    fakeLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                )

            camera.cameraInfo.cameraState.observe(fakeLifecycleOwner) { state ->
                // We are interested in the state where an error is first reported.
                if (state.error != null && !isErrorCaptured) {
                    capturedState = state
                    isErrorCaptured = true
                    cameraErrorLatch.countDown()
                }
            }
        }
        flushLoopers()

        // Assert: Wait for the error state and verify it matches expectations.
        assertThat(cameraErrorLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(capturedState).isNotNull()
        assertThat(capturedState!!.type).isIn(config.expectedCameraStateTypes)
        assertThat(capturedState.error?.code).isEqualTo(config.expectedErrorCode)
    }

    private fun addFakeCamera(cameraId: String) {
        val characteristics = createFakeCameraCharacteristics(CameraMetadata.LENS_FACING_BACK)
        shadowCameraManager.addCamera(cameraId, characteristics)
    }

    private fun createFakeCameraCharacteristics(lensFacing: Int): CameraCharacteristics {
        val cameraCharacteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(cameraCharacteristics).apply {
            set(CameraCharacteristics.LENS_FACING, lensFacing)
            set(CameraCharacteristics.SENSOR_ORIENTATION, 0)
            set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE, Rect(0, 0, 10, 10))
            set(CameraCharacteristics.FLASH_INFO_AVAILABLE, false)
            set(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            )
            set(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                StreamConfigurationMapBuilder.newBuilder()
                    .addOutputSize(ImageFormat.YUV_444_888, TEST_CAMERA_FRAME_SIZE)
                    .addOutputSize(ImageFormat.YUV_420_888, TEST_CAMERA_FRAME_SIZE)
                    .addOutputSize(TEST_CAMERA_FRAME_SIZE)
                    .addOutputSize(Size(1920, 1080))
                    .addOutputSize(Size(4032, 3024))
                    .addOutputSize(Size(3840, 2160))
                    .addOutputSize(Size(640, 480))
                    .addOutputSize(Size(320, 240))
                    .addOutputSize(/* format= */ 0x21, TEST_CAMERA_FRAME_SIZE)
                    .build(),
            )
        }
        return cameraCharacteristics
    }

    private fun flushLoopers() {
        val testLooper = testSchedulerThread.looper
        if (testLooper != null && testLooper.thread.isAlive) {
            shadowOf(testLooper).idle()
        }
        ShadowLooper.idleMainLooper()
    }
}
