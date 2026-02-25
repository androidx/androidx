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
import androidx.camera.core.Camera
import androidx.camera.core.CameraIdentifier
import androidx.camera.core.CameraPresenceListener
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.core.os.HandlerCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.StreamConfigurationMapBuilder

/**
 * Robolectric tests for verifying behavior when cameras are dynamically added or removed.
 *
 * This test uses Robolectric's [ShadowCameraManager] to simulate camera hot-plugging events and
 * verify that CameraX reacts correctly.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.TIRAMISU],
    shadows = [TestShadowCameraManager::class, TestShadowCameraDeviceImpl::class],
)
@DoNotInstrument
class CameraAvailabilityTest(private val testConfig: CameraTestConfig) {
    data class CameraTestConfig(val implName: String) {
        override fun toString(): String = implName
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private lateinit var cameraManager: CameraManager
    private lateinit var shadowCameraManager: ShadowCameraManager
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    private lateinit var customConfig: CameraXConfig
    private lateinit var testSchedulerThread: HandlerThread
    private lateinit var testSchedulerHandler: Handler
    private lateinit var testCameraExecutor: Executor
    private lateinit var shadowAgent: ShadowCameraAgent

    companion object {
        const val FAKE_BACK_CAMERA_ID = "0"
        const val FAKE_FRONT_CAMERA_ID = "1"
        private val TEST_CAMERA_FRAME_SIZE: Size = Size(1280, 720)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun data() = listOf(CameraTestConfig("Camera2"))
    }

    @Before
    fun setUp() {
        ShadowLog.stream = System.out
        val configBuilder =
            when (testConfig.implName) {
                "Camera2" -> CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
                else -> throw IllegalArgumentException("Unknown impl name: ${testConfig.implName}")
            }

        testSchedulerThread = HandlerThread("TestScheduler")
        testSchedulerThread.start()
        testSchedulerHandler = HandlerCompat.createAsync(testSchedulerThread.looper)
        testCameraExecutor = CameraXExecutors.newHandlerExecutor(testSchedulerHandler)
        customConfig =
            configBuilder
                .setSchedulerHandler(testSchedulerHandler)
                .setCameraExecutor(testCameraExecutor)
                .build()

        // Initialize our test layer
        shadowAgent = ShadowCameraAgent(testSchedulerHandler)
        ShadowCameraBridge.agent = shadowAgent

        fakeLifecycleOwner = FakeLifecycleOwner()
        fakeLifecycleOwner.startAndResume()
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        shadowCameraManager = shadowOf(cameraManager)
    }

    @After
    fun tearDown() {
        cameraProvider?.shutdownAsync()?.get(10, TimeUnit.SECONDS)
        testSchedulerThread.quitSafely()
        ShadowCameraBridge.agent = null
    }

    // ===================================================================================
    // ## Group 1: Tests for availability changes when the camera is NOT in use (Closed)
    // ===================================================================================

    @Test
    @LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
    fun whenCameraAdded_itIsDetectedAndUsable() {
        // Arrange: Start with one camera.
        addCamerasToShadow(hasBackCamera = true, hasFrontCamera = false)
        initializeProviderWithConfig(customConfig)

        assertThat(cameraProvider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)).isTrue()
        assertThat(cameraProvider!!.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)).isFalse()

        // Arrange: Listen for the new camera.
        val cameraAddedLatch = CountDownLatch(1)
        cameraProvider!!.addCameraPresenceListener(
            testCameraExecutor,
            object : CameraPresenceListener {
                override fun onCamerasAdded(cameraIdentifiers: Set<CameraIdentifier>) {
                    if (cameraIdentifiers.any { it.internalId == FAKE_FRONT_CAMERA_ID }) {
                        cameraAddedLatch.countDown()
                    }
                }

                override fun onCamerasRemoved(cameraIdentifiers: Set<CameraIdentifier>) {}
            },
        )

        // Act: Add a new camera and trigger the availability event.
        shadowCameraManager.addCamera(
            FAKE_FRONT_CAMERA_ID,
            createFakeCameraCharacteristics(CameraMetadata.LENS_FACING_FRONT),
        )
        shadowAgent.triggerOnCameraAvailable(FAKE_FRONT_CAMERA_ID)
        flushLoopers()

        // Assert: The new camera is detected and can be opened.
        assertThat(cameraAddedLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(cameraProvider!!.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)).isTrue()

        val openLatch = CountDownLatch(1)
        val preview = Preview.Builder().build()
        mainThreadHandler.post {
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            val camera =
                cameraProvider!!.bindToLifecycle(
                    fakeLifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                )
            camera.cameraInfo.cameraState.observe(fakeLifecycleOwner) {
                if (it.type == CameraState.Type.OPEN) openLatch.countDown()
            }
        }
        flushLoopers()
        assertThat(openLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
    fun whenClosedCameraIsRemoved_itBecomesUnavailable() {
        // Arrange: Start with two available cameras.
        addCamerasToShadow(hasBackCamera = true, hasFrontCamera = true)
        initializeProviderWithConfig(customConfig)

        assertThat(cameraProvider!!.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)).isTrue()

        // Arrange: Listen for the removal.
        val removalLatch = CountDownLatch(1)
        cameraProvider!!.addCameraPresenceListener(
            testCameraExecutor,
            object : CameraPresenceListener {
                override fun onCamerasAdded(cameraIdentifiers: Set<CameraIdentifier>) {}

                override fun onCamerasRemoved(cameraIdentifiers: Set<CameraIdentifier>) {
                    if (cameraIdentifiers.any { it.internalId == FAKE_FRONT_CAMERA_ID }) {
                        removalLatch.countDown()
                    }
                }
            },
        )

        // Act: Remove the front camera (which is not open) and trigger the event.
        shadowCameraManager.removeCamera(FAKE_FRONT_CAMERA_ID)
        shadowAgent.triggerOnCameraUnavailable(FAKE_FRONT_CAMERA_ID)
        flushLoopers()

        // Assert: The removal was detected.
        assertThat(removalLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(cameraProvider!!.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)).isFalse()
    }

    // ===============================================================================
    // ## Group 2: Tests for an active (Open) camera being unplugged
    // ===============================================================================

    @Test
    @LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
    fun openDeviceClosesAndIsRemoved_whenNotifiedOnError() {
        verifyOpenDeviceUnplugged(ShadowCameraAgent.DeviceErrorScenario.ON_ERROR)
    }

    @Test
    @LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
    fun openDeviceClosesAndIsRemoved_whenNotifiedOnDisconnected() {
        verifyOpenDeviceUnplugged(ShadowCameraAgent.DeviceErrorScenario.ON_DISCONNECTED)
    }

    /** Helper to test unplugging an open camera, which should cause a device-specific error. */
    private fun verifyOpenDeviceUnplugged(scenario: ShadowCameraAgent.DeviceErrorScenario) {
        // Arrange: Add a camera and initialize CameraX.
        addCamerasToShadow(hasBackCamera = true, hasFrontCamera = false)
        initializeProviderWithConfig(customConfig)

        // Arrange: Bind to the camera and wait for it to open.
        val openLatch = CountDownLatch(1)
        val preview = Preview.Builder().build()
        var camera: Camera? = null
        mainThreadHandler.post {
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            camera =
                cameraProvider!!.bindToLifecycle(
                    fakeLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                )
            camera.cameraInfo.cameraState.observe(fakeLifecycleOwner) {
                if (it.type == CameraState.Type.OPEN) openLatch.countDown()
            }
        }
        flushLoopers()
        assertThat(openLatch.await(5, TimeUnit.SECONDS)).isTrue()

        // Arrange: Listen for the camera to be removed and for the open device to close.
        val removalLatch = CountDownLatch(1)
        val closeLatch = CountDownLatch(1)
        mainThreadHandler.post {
            cameraProvider!!.addCameraPresenceListener(
                testCameraExecutor,
                object : CameraPresenceListener {
                    override fun onCamerasAdded(identifiers: Set<CameraIdentifier>) {}

                    override fun onCamerasRemoved(identifiers: Set<CameraIdentifier>) {
                        if (identifiers.any { it.internalId == FAKE_BACK_CAMERA_ID }) {
                            removalLatch.countDown()
                        }
                    }
                },
            )
            camera!!.cameraInfo.cameraState.observe(fakeLifecycleOwner) {
                if (it.type == CameraState.Type.CLOSED) {
                    closeLatch.countDown()
                }
            }
        }

        // Act: Simulate the open camera being unplugged.
        // This should cause a device error and its removal from the system list.
        shadowCameraManager.removeCamera(FAKE_BACK_CAMERA_ID)
        shadowAgent.notifyOpenDeviceError(FAKE_BACK_CAMERA_ID, scenario)
        flushLoopers()

        // Assert: The device closes and is removed from the provider.
        // Note: We do NOT trigger onCameraUnavailable, as the device was already
        // unavailable because it was open.
        assertThat(removalLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(closeLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(cameraProvider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)).isFalse()
    }

    private fun addCamerasToShadow(hasBackCamera: Boolean, hasFrontCamera: Boolean) {
        if (hasBackCamera) {
            shadowCameraManager.addCamera(
                FAKE_BACK_CAMERA_ID,
                createFakeCameraCharacteristics(CameraMetadata.LENS_FACING_BACK),
            )
        }
        if (hasFrontCamera) {
            shadowCameraManager.addCamera(
                FAKE_FRONT_CAMERA_ID,
                createFakeCameraCharacteristics(CameraMetadata.LENS_FACING_FRONT),
            )
        }
    }

    private fun flushLoopers() {
        shadowOf(testSchedulerThread.looper).idle()
        ShadowLooper.idleMainLooper()
    }

    private fun createFakeCameraCharacteristics(lensFacing: Int): CameraCharacteristics {
        val cameraCharacteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(cameraCharacteristics).apply {
            set(CameraCharacteristics.LENS_FACING, lensFacing)
            set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE, Rect(0, 0, 10, 10))
            set(CameraCharacteristics.SENSOR_ORIENTATION, 0)
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

    private fun initializeProviderWithConfig(config: CameraXConfig) {
        ProcessCameraProvider.configureInstance(config)
        cameraProvider = ProcessCameraProvider.getInstance(context).get(10, TimeUnit.SECONDS)
        flushLoopers()
    }
}
