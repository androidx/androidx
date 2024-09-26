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

package androidx.camera.integration.core

import android.app.Instrumentation
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.CompositionSettings
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.SurfaceTextureProvider.SurfaceTextureCallback
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ConcurrentCameraTest(private val implName: String, private val cameraConfig: CameraXConfig) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName == CameraPipeConfig::class.simpleName,
        )

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    @get:Rule val labTest: LabTestRule = LabTestRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
            )
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val packageManager = context.packageManager
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private var surfaceFutureSemaphoreFront: Semaphore? = null
    private var safeToReleaseSemaphoreFront: Semaphore? = null
    private var surfaceFutureSemaphoreBack: Semaphore? = null
    private var safeToReleaseSemaphoreBack: Semaphore? = null

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT))

        ProcessCameraProvider.configureInstance(cameraConfig)

        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        handlerThread = HandlerThread("AnalysisThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        surfaceFutureSemaphoreFront = Semaphore(/* permits= */ 0)
        safeToReleaseSemaphoreFront = Semaphore(/* permits= */ 0)
        surfaceFutureSemaphoreBack = Semaphore(/* permits= */ 0)
        safeToReleaseSemaphoreBack = Semaphore(/* permits= */ 0)

        instrumentation.runOnMainSync {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
        }
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            instrumentation.runOnMainSync { cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS] }
        }

        if (::handler.isInitialized) {
            handlerThread.quitSafely()
        }

        // Ensure all cameras are released for the next test
        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    @Test
    fun testFrontAndBackCameraPreview_doNotCrash() = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Arrange.
        val previewFront = Preview.Builder().build()
        instrumentation.runOnMainSync {
            previewFront.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                getSurfaceProvider(surfaceFutureSemaphoreFront, safeToReleaseSemaphoreFront)
            )
        }
        val primary =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                UseCaseGroup.Builder().addUseCase(previewFront).build(),
                fakeLifecycleOwner
            )
        val previewBack = Preview.Builder().build()
        instrumentation.runOnMainSync {
            previewBack.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                getSurfaceProvider(surfaceFutureSemaphoreBack, safeToReleaseSemaphoreBack)
            )
        }
        val secondary =
            SingleCameraConfig(
                CameraSelector.DEFAULT_BACK_CAMERA,
                UseCaseGroup.Builder().addUseCase(previewBack).build(),
                fakeLifecycleOwner
            )

        // Act.
        var concurrentCamera: ConcurrentCamera? = null
        instrumentation.runOnMainSync {
            concurrentCamera = cameraProvider.bindToLifecycle(listOf(primary, secondary))
        }

        // Assert.
        assertThat(concurrentCamera).isNotNull()
        assertThat(concurrentCamera!!.cameras.size).isEqualTo(2)
        assertThat(surfaceFutureSemaphoreFront!!.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
        assertThat(surfaceFutureSemaphoreBack!!.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testOnlyOneCamera_throwException(): Unit = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))

        // Arrange.
        val previewFront = Preview.Builder().build()
        val primary =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                UseCaseGroup.Builder().addUseCase(previewFront).build(),
                fakeLifecycleOwner
            )

        // Act & Assert.
        instrumentation.runOnMainSync { cameraProvider.bindToLifecycle(listOf(primary)) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMoreThanTwoCameras_throwException(): Unit = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Arrange.
        val previewFront = Preview.Builder().build()
        val primary =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                UseCaseGroup.Builder().addUseCase(previewFront).build(),
                fakeLifecycleOwner
            )
        val previewBack = Preview.Builder().build()
        val secondary =
            SingleCameraConfig(
                CameraSelector.DEFAULT_BACK_CAMERA,
                UseCaseGroup.Builder().addUseCase(previewBack).build(),
                fakeLifecycleOwner
            )

        val tertiary =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                UseCaseGroup.Builder().addUseCase(previewBack).build(),
                fakeLifecycleOwner
            )

        // Act & Assert.
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(listOf(primary, secondary, tertiary))
        }
    }

    @Test
    fun testFrontAndBackCameraRecording_doNotCrash() = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Arrange.
        val preview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                getSurfaceProvider(surfaceFutureSemaphoreFront, safeToReleaseSemaphoreFront)
            )
        }
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val useCaseGroup =
            UseCaseGroup.Builder().addUseCase(preview).addUseCase(videoCapture).build()

        val primary =
            SingleCameraConfig(
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCaseGroup,
                CompositionSettings.Builder().setOffset(0.0f, 0.0f).setScale(1.0f, 1.0f).build(),
                fakeLifecycleOwner
            )
        val secondary =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                useCaseGroup,
                CompositionSettings.Builder().setOffset(-0.3f, -0.4f).setScale(0.3f, 0.3f).build(),
                fakeLifecycleOwner
            )

        // Act.
        var concurrentCamera: ConcurrentCamera? = null
        instrumentation.runOnMainSync {
            concurrentCamera = cameraProvider.bindToLifecycle(listOf(primary, secondary))
        }

        // Assert.
        assertThat(concurrentCamera).isNotNull()
        assertThat(concurrentCamera!!.cameras.size).isEqualTo(1)
        assertThat(surfaceFutureSemaphoreFront!!.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMoreThanTwoCamerasRecording_throwException(): Unit = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Arrange.
        val preview = Preview.Builder().build()
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val useCaseGroup =
            UseCaseGroup.Builder().addUseCase(preview).addUseCase(videoCapture).build()

        val primary =
            SingleCameraConfig(
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCaseGroup,
                CompositionSettings.Builder().setOffset(0.0f, 0.0f).setScale(1.0f, 1.0f).build(),
                fakeLifecycleOwner
            )
        val secondary =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                useCaseGroup,
                CompositionSettings.Builder().setOffset(-0.3f, -0.4f).setScale(0.3f, 0.3f).build(),
                fakeLifecycleOwner
            )
        val tertiary =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                useCaseGroup,
                CompositionSettings.Builder().setOffset(-0.3f, -0.4f).setScale(0.3f, 0.3f).build(),
                fakeLifecycleOwner
            )

        // Act & Assert.
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(listOf(primary, secondary, tertiary))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testOnlyOneCameraRecording_throwException(): Unit = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))

        // Arrange.
        val preview = Preview.Builder().build()
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val useCaseGroup =
            UseCaseGroup.Builder().addUseCase(preview).addUseCase(videoCapture).build()

        val primary =
            SingleCameraConfig(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                useCaseGroup,
                CompositionSettings.Builder().setOffset(0.0f, 0.0f).setScale(1.0f, 1.0f).build(),
                fakeLifecycleOwner
            )

        // Act & Assert.
        instrumentation.runOnMainSync { cameraProvider.bindToLifecycle(listOf(primary)) }
    }

    private fun getSurfaceProvider(
        surfaceFutureSemaphore: Semaphore?,
        safeToReleaseSemaphore: Semaphore?
    ): Preview.SurfaceProvider {
        return SurfaceTextureProvider.createSurfaceTextureProvider(
            object : SurfaceTextureCallback {
                override fun onSurfaceTextureReady(
                    surfaceTexture: SurfaceTexture,
                    resolution: Size
                ) {
                    surfaceTexture.setOnFrameAvailableListener {
                        surfaceFutureSemaphore!!.release()
                    }
                }

                override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                    surfaceTexture.release()
                    safeToReleaseSemaphore!!.release()
                }
            }
        )
    }
}
