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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.StreamConfigurationMapBuilder

/**
 * An integration-level Robolectric test to verify that Camera2Logger respects the minimum log level
 * set in CameraXConfig.
 *
 * This test specifically checks for a DEBUG log message from UseCaseManager ("Attaching [...]") to
 * ensure the integration-layer logger is working, not just a log from the core camera-pipe library.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    minSdk = Build.VERSION_CODES.M,
    shadows = [TestShadowCameraManager::class, TestShadowCameraDeviceImpl::class],
)
class Camera2LoggerIntegrationTest {

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
        private const val CXCP_TAG = "CXCP" // The tag used by Camera2Logger

        // The specific log message we are looking for from the integration layer
        private const val INTEGRATION_LOG_MARKER = "Attaching ["
    }

    @Before
    fun setUp() {
        ShadowLog.stream = System.out

        testSchedulerThread = HandlerThread("CameraStateTestScheduler")
        testSchedulerThread.start()
        testSchedulerHandler = Handler(testSchedulerThread.looper)
        testCameraExecutor = Executors.newFixedThreadPool(2)

        shadowAgent = ShadowCameraAgent(testSchedulerHandler)
        ShadowCameraBridge.agent = shadowAgent

        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        shadowCameraManager = Shadows.shadowOf(cameraManager)
        addFakeCamera(FAKE_CAMERA_ID)

        fakeLifecycleOwner = FakeLifecycleOwner()
        fakeLifecycleOwner.startAndResume()
    }

    @After
    fun tearDown() {
        // Shut down the provider to allow the next test to call configureInstance()
        cameraProvider?.shutdownAsync()?.get(10, TimeUnit.SECONDS)
        testSchedulerThread.quitSafely()
        ShadowCameraBridge.agent = null
        ShadowLog.clear()
    }

    @Test
    @LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
    fun showsLogs_whenLevelIsDebug() {
        // Arrange: Configure CameraX with DEBUG level
        initializeCameraProvider(Log.DEBUG)
        ShadowLog.clear() // Clear init logs

        // Act: Bind a use case
        bindPreviewUseCase()

        // Assert: Check that the specific integration-layer log is PRESENT
        val allLogs = ShadowLog.getLogs()
        val attachLogFound =
            allLogs.any {
                it.tag == CXCP_TAG &&
                    it.type == Log.DEBUG &&
                    it.msg.contains(INTEGRATION_LOG_MARKER)
            }

        assertWithMessage("Expected integration-layer '$INTEGRATION_LOG_MARKER' log to be visible")
            .that(attachLogFound)
            .isTrue()
    }

    @Test
    @LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
    fun hidesLogs_whenLevelIsWarn() {
        // Arrange: Configure CameraX with WARN level
        initializeCameraProvider(Log.WARN)
        ShadowLog.clear() // Clear init logs

        // Act: Bind a use case
        bindPreviewUseCase()

        // Assert: Check that the specific integration-layer log is ABSENT
        val allLogs = ShadowLog.getLogs()
        val attachLogFound =
            allLogs.any {
                it.tag == CXCP_TAG &&
                    it.type == Log.DEBUG &&
                    it.msg.contains(INTEGRATION_LOG_MARKER)
            }

        assertWithMessage("Expected integration-layer '$INTEGRATION_LOG_MARKER' log to be hidden")
            .that(attachLogFound)
            .isFalse()
    }

    @Test
    @LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
    fun showsLogs_byDefault() {
        // Arrange: Configure CameraX with the *default* log level (no explicit setting)
        initializeCameraProvider(null) // Pass null to use default
        ShadowLog.clear() // Clear init logs

        // Act: Bind a use case
        bindPreviewUseCase()

        // Assert: Check that the specific integration-layer log is PRESENT
        // The default level is DEBUG, so DEBUG logs should be visible.
        val allLogs = ShadowLog.getLogs()
        val attachLogFound =
            allLogs.any {
                it.tag == CXCP_TAG &&
                    it.type == Log.DEBUG &&
                    it.msg.contains(INTEGRATION_LOG_MARKER)
            }

        assertWithMessage(
                "Expected integration-layer '$INTEGRATION_LOG_MARKER' DEBUG log to be visible by default"
            )
            .that(attachLogFound)
            .isTrue()
    }

    /** Initializes ProcessCameraProvider with a specific log level, or default if null. */
    private fun initializeCameraProvider(logLevel: Int?) {
        val configBuilder =
            CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
                .setSchedulerHandler(testSchedulerHandler)
                .setCameraExecutor(testCameraExecutor)

        // Only set the log level if one is provided
        if (logLevel != null) {
            configBuilder.setMinimumLoggingLevel(logLevel)
        }

        ProcessCameraProvider.configureInstance(configBuilder.build())
        cameraProvider = ProcessCameraProvider.getInstance(context).get(10, TimeUnit.SECONDS)
        flushLoopers()
    }

    /** Binds a simple Preview use case to trigger camera open logic. */
    private fun bindPreviewUseCase() {
        mainThreadHandler.post {
            val preview = Preview.Builder().build()
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            cameraProvider!!.bindToLifecycle(
                fakeLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
            )
        }
        flushLoopers()
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
            Shadows.shadowOf(testLooper).idle()
        }
        ShadowLooper.idleMainLooper()
    }
}
