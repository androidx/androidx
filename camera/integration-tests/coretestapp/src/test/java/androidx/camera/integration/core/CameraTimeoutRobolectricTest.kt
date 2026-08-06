/*
 * Copyright 2026 The Android Open Source Project
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
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 24, shadows = [TestShadowCameraManager::class, TestShadowCameraDeviceImpl::class])
class CameraTimeoutRobolectricTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private lateinit var cameraManager: CameraManager
    private lateinit var shadowCameraManager: ShadowCameraManager
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    private lateinit var shadowAgent: ShadowCameraAgent

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler
    private lateinit var cameraExecutor: Executor

    private lateinit var lightweightThread: HandlerThread
    private lateinit var lightweightHandler: Handler
    private lateinit var lightweightExecutor: Executor

    private lateinit var blockingThread: HandlerThread
    private lateinit var blockingHandler: Handler
    private lateinit var blockingExecutor: Executor

    private lateinit var customCameraPipe: CameraPipe

    companion object {
        const val FAKE_CAMERA_ID = "0"
        private val TEST_CAMERA_FRAME_SIZE: Size = Size(1280, 720)
        private const val CUSTOM_TIMEOUT_MS = 1000L
        private const val TAG = "CameraTimeoutTest"
    }

    @Before
    fun setUp() {
        ArchTaskExecutor.getInstance()
            .setDelegate(
                object : TaskExecutor() {
                    override fun executeOnDiskIO(runnable: Runnable) {
                        runnable.run()
                    }

                    override fun postToMainThread(runnable: Runnable) {
                        mainThreadHandler.post(runnable)
                    }

                    override fun isMainThread(): Boolean {
                        return Looper.myLooper() == Looper.getMainLooper()
                    }
                }
            )
        ShadowLog.stream = System.out

        cameraThread = HandlerThread("CameraThread")
        cameraThread.start()
        cameraHandler = Handler(cameraThread.looper)
        cameraExecutor = Executor { cameraHandler.post(it) }

        lightweightThread = HandlerThread("LightweightThread")
        lightweightThread.start()
        lightweightHandler = Handler(lightweightThread.looper)
        lightweightExecutor = Executor { lightweightHandler.post(it) }

        blockingThread = HandlerThread("BlockingThread")
        blockingThread.start()
        blockingHandler = Handler(blockingThread.looper)
        blockingExecutor = Executor { blockingHandler.post(it) }

        val cameraPipeConfig =
            CameraPipe.Config(
                appContext = context,
                threadConfig =
                    CameraPipe.ThreadConfig(
                        defaultLightweightExecutor = lightweightExecutor,
                        defaultBackgroundExecutor = blockingExecutor,
                        defaultBlockingExecutor = blockingExecutor,
                        defaultCameraExecutor = cameraExecutor,
                        defaultCameraHandler = cameraHandler,
                    ),
                cameraInteropConfig =
                    CameraPipe.CameraInteropConfig(
                        cameraOpenRetryMaxTimeout = CUSTOM_TIMEOUT_MS.milliseconds
                    ),
            )
        customCameraPipe = CameraPipe(cameraPipeConfig)

        val baseConfig =
            Camera2Config.from(
                sharedCameraPipe = customCameraPipe,
                sharedAppContext = context,
                sharedThreadConfig =
                    CameraThreadConfig.create(lightweightExecutor, lightweightHandler),
            )
        val cameraXConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .apply { setCameraOpenRetryMaxTimeoutInMillisWhileResuming(CUSTOM_TIMEOUT_MS) }
                .build()

        Log.d(
            TAG,
            "Configured timeout: ${cameraXConfig.getCameraOpenRetryMaxTimeoutInMillisWhileResuming()}",
        )

        shadowAgent = ShadowCameraAgent(cameraHandler)
        ShadowCameraBridge.agent = shadowAgent

        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        shadowCameraManager = shadowOf(cameraManager)
        addFakeCamera(FAKE_CAMERA_ID)

        ProcessCameraProvider.configureInstance(cameraXConfig)
        val future = ProcessCameraProvider.getInstance(context)
        while (!future.isDone) {
            flushLoopers()
        }
        cameraProvider = future.get()

        fakeLifecycleOwner = FakeLifecycleOwner()
        fakeLifecycleOwner.startAndResume()
        flushLoopers()
    }

    @After
    fun tearDown() {
        val shutdownFuture = cameraProvider?.shutdownAsync()
        if (shutdownFuture != null) {
            while (!shutdownFuture.isDone) {
                flushLoopers()
                runBlocking { delay(10) }
            }
            shutdownFuture.get()
        }
        shadowAgent.closeAllOpenDevices()
        flushLoopers()
        cameraThread.quitSafely()
        lightweightThread.quitSafely()
        blockingThread.quitSafely()
        ShadowCameraBridge.agent = null
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    private fun flushLoopers() {
        flushLooper(cameraThread.looper)
        flushLooper(lightweightThread.looper)
        flushLooper(blockingThread.looper)
        ShadowLooper.idleMainLooper()
    }

    private fun flushLooper(looper: Looper?) {
        if (looper != null && looper.thread.isAlive) {
            shadowOf(looper).idle()
        }
    }

    @Test
    fun cameraOpenRetriesStop_afterTimeout() {
        // Arrange: Configure the agent to fail all camera open calls with CAMERA_DEVICE_ERROR.
        shadowAgent.failAllOpenCallsWith(ShadowCameraAgent.DeviceOpenError.CAMERA_DEVICE_ERROR)

        // Act: Bind a use case, which will trigger the camera open call.
        mainThreadHandler.post {
            val preview = Preview.Builder().build()
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
            cameraProvider!!.bindToLifecycle(
                fakeLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
            )
        }
        ShadowLooper.idleMainLooper()

        // Wait for some time to allow retries to start and exceed the timeout (1s).
        // We wait 2s of real and virtual time.
        waitRealAndVirtualTime(2000)

        val attemptsAfterTimeout = shadowAgent.openAttempts

        // It should have tried at least 2 times (initial + 1 retry).
        assertThat(attemptsAfterTimeout).isAtLeast(2)

        // Wait another 2s to verify it doesn't retry anymore.
        waitRealAndVirtualTime(2000)

        val attemptsLater = shadowAgent.openAttempts

        // The number of attempts should not have increased.
        assertThat(attemptsLater).isEqualTo(attemptsAfterTimeout)
    }

    /**
     * Helper to wait for a specified duration in both real time and virtual time. This is necessary
     * because CameraPipe runs on real background threads, but uses Robolectric's virtualized
     * SystemClock for timeout calculations.
     */
    private fun waitRealAndVirtualTime(ms: Long) {
        val steps = ms / 10
        for (i in 1..steps) {
            ShadowLooper.idleMainLooper(10, TimeUnit.MILLISECONDS)
            flushLooper(cameraThread.looper)
            flushLooper(lightweightThread.looper)
            flushLooper(blockingThread.looper)
            runBlocking { delay(10) }
        }
    }

    private fun addFakeCamera(cameraId: String) {
        if (cameraManager.cameraIdList.contains(cameraId)) {
            shadowCameraManager.removeCamera(cameraId)
        }

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
}
