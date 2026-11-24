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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.StreamConfigurationMapBuilder

/**
 * End-to-end Robolectric regression tests for b/590052874 verifying that binding [ImageAnalysis]
 * with its default high-priority background executor on [Camera2Config] does not leak
 * `CameraX-camerax_high_priority` after `unbindAll()` or `shutdownAsync()`.
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class HighPriorityExecutorLeakRobolectricTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var baselineThreads: Set<Thread>
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var lifecycleOwner: FakeLifecycleOwner

    @Before
    fun setUp() {
        baselineThreads = liveHighPriorityThreads()
        addBackCamera()
        ProcessCameraProvider.configureInstance(Camera2Config.defaultConfig())
        cameraProvider = awaitWithMainLooperIdle(ProcessCameraProvider.getInstance(context))
        lifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
        ShadowLooper.idleMainLooper()
    }

    @After
    fun tearDown() {
        try {
            // Unconditionally shut down the ProcessCameraProvider singleton while idling the paused
            // main looper so queued main-thread callbacks drain cleanly and a failure in setUp() or
            // a test body can never leave CameraX configured for subsequent tests in this sandbox.
            awaitWithMainLooperIdle(ProcessCameraProvider.shutdown())
            ShadowLooper.idleMainLooper()
        } finally {
            cameraProvider = null
        }
        joinAndAssertNoNewHighPriorityThreads()
    }

    @Test
    fun bindImageAnalysis_shutdownAsyncWithoutIdlingMainLooper_terminatesHighPriorityThread() {
        val provider = checkNotNull(cameraProvider)
        val imageAnalysis = ImageAnalysis.Builder().build()

        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, imageAnalysis)
        ShadowLooper.idleMainLooper()

        val highPriorityThread = newHighPriorityThreads().single()
        assertThat(highPriorityThread.isAlive).isTrue()

        // Mirrors Robolectric tests with thread-leak checks that block on shutdownAsync().get()
        // in @After without idling the main looper afterward.
        provider.shutdownAsync().get(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)

        awaitHighPriorityThreadTerminated(highPriorityThread)
    }

    @Test
    fun bindImageAnalysis_unbindAllWithoutShutdown_terminatesHighPriorityThread() {
        val provider = checkNotNull(cameraProvider)
        val analyzerExecutor = Executors.newSingleThreadExecutor()
        try {
            // Mirrors callers that pass a managed executor to setAnalyzer() without overriding
            // setBackgroundExecutor().
            val imageAnalysis =
                ImageAnalysis.Builder().build().apply {
                    setAnalyzer(analyzerExecutor) { it.close() }
                }

            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                imageAnalysis,
            )
            ShadowLooper.idleMainLooper()

            val highPriorityThread = newHighPriorityThreads().single()
            assertThat(highPriorityThread.isAlive).isTrue()

            // Mirrors shared LifecycleCameraProvider callers that only call unbindAll() in tearDown
            // without shutting down CameraX.
            provider.unbindAll()

            // Wait for CameraPipe's async UseCaseCamera/DeferrableSurface teardown to release the
            // ImageReader listener lease without pumping the main looper.
            awaitHighPriorityThreadTerminated(highPriorityThread)
        } finally {
            analyzerExecutor.shutdownNow()
        }
    }

    @Test
    fun bindPreviewAndImageAnalysis_partialUnbindImageAnalysis_terminatesHighPriorityThread() {
        val provider = checkNotNull(cameraProvider)
        val preview =
            Preview.Builder().build().apply {
                surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
            }
        val imageAnalysis = ImageAnalysis.Builder().build()

        provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis,
        )
        ShadowLooper.idleMainLooper()

        val highPriorityThread = newHighPriorityThreads().single()
        assertThat(highPriorityThread.isAlive).isTrue()

        // Partial re-bind: unbind only ImageAnalysis while Preview remains bound on the camera.
        provider.unbind(imageAnalysis)

        awaitHighPriorityThreadTerminated(highPriorityThread)
        assertThat(provider.isBound(preview)).isTrue()
    }

    private fun awaitHighPriorityThreadTerminated(highPriorityThread: Thread) {
        // Do not pump ShadowLooper.idleMainLooper() here: mirrors thread-leak checks joining
        // directly after @After unbindAll()/unbind() when the main looper remains paused.
        highPriorityThread.join(JOIN_TIMEOUT_MILLIS)
        assertThat(highPriorityThread.isAlive).isFalse()
        assertThat(newHighPriorityThreads()).isEmpty()
    }

    private fun <T> awaitWithMainLooperIdle(
        future: ListenableFuture<T>,
        timeoutMillis: Long = SHUTDOWN_TIMEOUT_MILLIS,
    ): T {
        val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (System.nanoTime() < deadlineNanos) {
            ShadowLooper.idleMainLooper()
            try {
                return future.get(10, TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                // Keep idling the paused main looper while background tasks progress.
            }
        }
        ShadowLooper.idleMainLooper()
        return future.get(0, TimeUnit.MILLISECONDS)
    }

    private fun addBackCamera() {
        val cameraManager = context.getSystemService(CameraManager::class.java)
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(characteristics).apply {
            set(CameraCharacteristics.LENS_FACING, CameraMetadata.LENS_FACING_BACK)
            set(CameraCharacteristics.SENSOR_ORIENTATION, 0)
            set(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            )
            set(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                StreamConfigurationMapBuilder.newBuilder()
                    .addOutputSize(ImageFormat.YUV_420_888, Size(1280, 720))
                    .addOutputSize(ImageFormat.YUV_420_888, Size(640, 480))
                    .addOutputSize(Size(4032, 3024))
                    .addOutputSize(Size(1920, 1080))
                    .addOutputSize(Size(1280, 720))
                    .addOutputSize(Size(640, 480))
                    .addOutputSize(Size(320, 240))
                    .build(),
            )
        }
        shadowOf(cameraManager).addCamera("0", characteristics)
    }

    private fun liveHighPriorityThreads(): Set<Thread> =
        Thread.getAllStackTraces().keys.filterTo(mutableSetOf()) {
            it.name == HIGH_PRIORITY_THREAD && it.isAlive
        }

    private fun newHighPriorityThreads(): Set<Thread> = liveHighPriorityThreads() - baselineThreads

    private fun joinAndAssertNoNewHighPriorityThreads() {
        for (thread in newHighPriorityThreads()) {
            thread.join(JOIN_TIMEOUT_MILLIS)
            assertThat(thread.isAlive).isFalse()
        }
    }

    private companion object {
        const val HIGH_PRIORITY_THREAD = "CameraX-camerax_high_priority"
        const val JOIN_TIMEOUT_MILLIS = 5_000L
        const val SHUTDOWN_TIMEOUT_MILLIS = 30_000L
    }
}
