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
package androidx.camera.integration.core.camera2

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.DisplayInfoManager
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.ALLOWED_RESOLUTIONS_SLOW
import androidx.camera.testing.CameraPipeConfigTestRule
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.GLUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.SurfaceTextureProvider.SurfaceTextureCallback
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class PreviewTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig
) {
    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(cameraConfig)
    )

    companion object {
        private const val ANY_THREAD_NAME = "any-thread-name"
        private val DEFAULT_RESOLUTION: Size by lazy { Size(640, 480) }
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var defaultBuilder: Preview.Builder? = null
    private var previewResolution: Size? = null
    private var surfaceFutureSemaphore: Semaphore? = null
    private var safeToReleaseSemaphore: Semaphore? = null
    private var context: Context? = null
    private var camera: CameraUseCaseAdapter? = null

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        context = ApplicationProvider.getApplicationContext()
        CameraXUtil.initialize(context!!, cameraConfig).get()

        // init CameraX before creating Preview to get preview size with CameraX's context
        defaultBuilder = Preview.Builder.fromConfig(Preview.DEFAULT_CONFIG.config).also {
            it.mutableConfig.removeOption(ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)
        }
        surfaceFutureSemaphore = Semaphore( /*permits=*/0)
        safeToReleaseSemaphore = Semaphore( /*permits=*/0)
    }

    @After
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun tearDown() {
        if (camera != null) {
            instrumentation.runOnMainSync {
                // TODO: The removeUseCases() call might be removed after clarifying the
                //  abortCaptures() issue in b/162314023.
                camera!!.removeUseCases(camera!!.useCases)
            }
        }

        // Ensure all cameras are released for the next test
        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    @Test
    fun surfaceProvider_isUsedAfterSetting() = runBlocking {
        val preview = defaultBuilder!!.build()
        val completableDeferred = CompletableDeferred<Unit>()

        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        instrumentation.runOnMainSync { preview.setSurfaceProvider { request ->
            val surfaceTexture = SurfaceTexture(0)
            surfaceTexture.setDefaultBufferSize(
                request.resolution.width,
                request.resolution.height
            )
            surfaceTexture.detachFromGLContext()
            val surface = Surface(surfaceTexture)
            request.provideSurface(surface, CameraXExecutors.directExecutor()) {
                surface.release()
                surfaceTexture.release()
            }
            completableDeferred.complete(Unit)
        } }
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)
        withTimeout(3_000) {
            completableDeferred.await()
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun previewDetached_onSafeToReleaseCalled() {
        // Arrange.
        val preview = Preview.Builder().build()

        // Act.
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                getSurfaceProvider(null)
            )
        }
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)

        // Wait until preview gets frame.
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(5, TimeUnit.SECONDS)).isTrue()

        // Remove the UseCase from the camera
        camera!!.removeUseCases(setOf<UseCase>(preview))

        // Assert.
        Truth.assertThat(safeToReleaseSemaphore!!.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @Throws(InterruptedException::class)
    fun setSurfaceProviderBeforeBind_getsFrame() {
        // Arrange.
        val preview = defaultBuilder!!.build()
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        instrumentation.runOnMainSync { preview.setSurfaceProvider(getSurfaceProvider(null)) }

        // Act.
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)

        // Assert.
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @Throws(InterruptedException::class)
    fun setSurfaceProviderBeforeAttach_providesSurfaceOnWorkerExecutorThread() {
        val threadName = AtomicReference<String>()

        // Arrange.
        val preview = defaultBuilder!!.build()
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                workExecutorWithNamedThread,
                getSurfaceProvider { newValue: String -> threadName.set(newValue) })
        }

        // Act.
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)

        // Assert.
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
        Truth.assertThat(threadName.get()).isEqualTo(ANY_THREAD_NAME)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setSurfaceProviderAfterAttach_getsFrame() {
        // Arrange.
        val preview = defaultBuilder!!.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)

        // Act.
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        instrumentation.runOnMainSync { preview.setSurfaceProvider(getSurfaceProvider(null)) }

        // Assert.
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @Throws(InterruptedException::class)
    fun setSurfaceProviderAfterBind_providesSurfaceOnWorkerExecutorThread() {
        val threadName = AtomicReference<String>()

        // Arrange.
        val preview = defaultBuilder!!.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)

        // Act.
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                workExecutorWithNamedThread,
                getSurfaceProvider { newValue: String -> threadName.set(newValue) })
        }

        // Assert.
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
        Truth.assertThat(threadName.get()).isEqualTo(ANY_THREAD_NAME)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setMultipleNonNullSurfaceProviders_getsFrame() {
        val preview = defaultBuilder!!.build()

        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        instrumentation.runOnMainSync {
            // Set a different SurfaceProvider which will provide a different surface to be used
            // for preview.
            preview.setSurfaceProvider(getSurfaceProvider(null))
        }
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()

        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        instrumentation.runOnMainSync {
            // Set a different SurfaceProvider which will provide a different surface to be used
            // for preview.
            preview.setSurfaceProvider(getSurfaceProvider(null))
        }
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @Throws(InterruptedException::class)
    fun setMultipleNullableSurfaceProviders_getsFrame() {
        val preview = defaultBuilder!!.build()

        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        instrumentation.runOnMainSync {
            // Set a different SurfaceProvider which will provide a different surface to be used
            // for preview.
            preview.setSurfaceProvider(getSurfaceProvider(null))
        }
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()

        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        instrumentation.runOnMainSync {

            // Set the SurfaceProvider to null in order to force the Preview into an inactive
            // state before setting a different SurfaceProvider for preview.
            preview.setSurfaceProvider(null)
            preview.setSurfaceProvider(getSurfaceProvider(null))
        }
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun defaultAspectRatioWillBeSet_whenTargetResolutionIsNotSet() {
        val useCase = Preview.Builder().build()
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, useCase)
        val config = useCase.currentConfig as ImageOutputConfig
        Truth.assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun defaultAspectRatioWillBeSet_whenRatioDefaultIsSet() {
        val useCase = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_DEFAULT).build()
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, useCase)
        val config = useCase.currentConfig as ImageOutputConfig
        Truth.assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
    }

    @Suppress("DEPRECATION") // legacy resolution API
    @Test
    fun defaultAspectRatioWontBeSet_whenTargetResolutionIsSet() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
        val useCase = Preview.Builder().setTargetResolution(DEFAULT_RESOLUTION).build()
        Truth.assertThat(
            useCase.currentConfig.containsOption(
                ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO
            )
        ).isFalse()
        camera = CameraUtil.createCameraAndAttachUseCase(
            context!!,
            CameraSelector.DEFAULT_BACK_CAMERA, useCase
        )
        Truth.assertThat(
            useCase.currentConfig.containsOption(
                ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO
            )
        ).isFalse()
    }

    @Test
    fun useCaseConfigCanBeReset_afterUnbind() {
        val preview = defaultBuilder!!.build()
        val initialConfig = preview.currentConfig
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)
        instrumentation.runOnMainSync { camera!!.removeUseCases(setOf<UseCase>(preview)) }
        val configAfterUnbinding = preview.currentConfig
        Truth.assertThat(initialConfig == configAfterUnbinding).isTrue()
    }

    @Test
    fun targetRotationIsRetained_whenUseCaseIsReused() {
        val useCase = defaultBuilder!!.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, useCase)

        // Generally, the device can't be rotated to Surface.ROTATION_180. Therefore,
        // use it to do the test.
        useCase.targetRotation = Surface.ROTATION_180
        instrumentation.runOnMainSync {
            // Unbind the use case.
            camera!!.removeUseCases(setOf<UseCase>(useCase))
        }

        // Check the target rotation is kept when the use case is unbound.
        Truth.assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)

        // Check the target rotation is kept when the use case is rebound to the
        // lifecycle.
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, useCase)
        Truth.assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun targetRotationReturnsDisplayRotationIfNotSet() {
        val displayRotation = DisplayInfoManager.getInstance(context!!).maxSizeDisplay.rotation
        val useCase = defaultBuilder!!.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, useCase)

        Truth.assertThat(useCase.targetRotation).isEqualTo(displayRotation)
    }

    @Test
    @Throws(InterruptedException::class)
    fun useCaseCanBeReusedInSameCamera() {
        val preview = defaultBuilder!!.build()
        instrumentation.runOnMainSync { preview.setSurfaceProvider(getSurfaceProvider(null)) }

        // This is the first time the use case bound to the lifecycle.
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)

        // Check the frame available callback is called.
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
        instrumentation.runOnMainSync {
            // Unbind and rebind the use case to the same lifecycle.
            camera!!.removeUseCases(setOf<UseCase>(preview))
        }
        Truth.assertThat(safeToReleaseSemaphore!!.tryAcquire(5, TimeUnit.SECONDS)).isTrue()

        // Recreate the semaphore to monitor the frame available callback.
        surfaceFutureSemaphore = Semaphore( /*permits=*/0)
        // Rebind the use case to the same camera.
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)

        // Check the frame available callback can be called after reusing the use case.
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    @Throws(InterruptedException::class)
    fun useCaseCanBeReusedInDifferentCamera() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))

        val preview = defaultBuilder!!.build()
        instrumentation.runOnMainSync { preview.setSurfaceProvider(getSurfaceProvider(null)) }

        // This is the first time the use case bound to the lifecycle.
        camera = CameraUtil.createCameraAndAttachUseCase(
            context!!,
            CameraSelector.DEFAULT_BACK_CAMERA, preview
        )

        // Check the frame available callback is called.
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
        instrumentation.runOnMainSync {
            // Unbind and rebind the use case to the same lifecycle.
            camera!!.removeUseCases(setOf<UseCase>(preview))
        }
        Truth.assertThat(safeToReleaseSemaphore!!.tryAcquire(5, TimeUnit.SECONDS)).isTrue()

        // Recreate the semaphore to monitor the frame available callback.
        surfaceFutureSemaphore = Semaphore( /*permits=*/0)
        // Rebind the use case to different camera.
        camera = CameraUtil.createCameraAndAttachUseCase(
            context!!,
            CameraSelector.DEFAULT_FRONT_CAMERA, preview
        )

        // Check the frame available callback can be called after reusing the use case.
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun returnValidTargetRotation_afterUseCaseIsCreated() {
        val imageCapture = ImageCapture.Builder().build()
        Truth.assertThat(imageCapture.targetRotation).isNotEqualTo(
            ImageOutputConfig.INVALID_ROTATION
        )
    }

    @Test
    fun returnCorrectTargetRotation_afterUseCaseIsAttached() {
        val preview = Preview.Builder().setTargetRotation(
            Surface.ROTATION_180
        ).build()
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)
        Truth.assertThat(preview.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setNullSurfaceProvider_shouldStopPreview() {
        // Arrange.
        val preview = Preview.Builder().build()

        // Act.
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                SurfaceTextureProvider.createSurfaceTextureProvider(
                    object : SurfaceTextureCallback {
                        private var mIsReleased = false
                        override fun onSurfaceTextureReady(
                            surfaceTexture: SurfaceTexture,
                            resolution: Size
                        ) {
                            surfaceTexture.attachToGLContext(
                                GLUtil.getTexIdFromGLContext()
                            )
                            surfaceTexture.setOnFrameAvailableListener {
                                surfaceFutureSemaphore!!.release()
                                synchronized(this) {
                                    if (!mIsReleased) {
                                        surfaceTexture.updateTexImage()
                                    }
                                }
                            }
                        }

                        override fun onSafeToRelease(
                            surfaceTexture: SurfaceTexture
                        ) {
                            synchronized(this) {
                                mIsReleased = true
                                surfaceTexture.release()
                            }
                        }
                    })
            )
        }
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)

        // Assert.
        // Wait until preview gets frame.
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()

        // Act.
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                null
            )
        }

        // Assert.
        // No frame coming for 3 seconds in 10 seconds timeout.
        Truth.assertThat(noFrameCome(3000L, 10000L)).isTrue()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Test
    fun getsFrame_withHighResolutionEnabled() {
        // TODO(b/247492645) Remove camera-pipe-integration restriction after porting
        //  ResolutionSelector logic
        assumeTrue(implName != CameraPipeConfig::class.simpleName)

        val maxHighResolutionOutputSize = CameraUtil.getMaxHighResolutionOutputSizeWithLensFacing(
            cameraSelector.lensFacing!!,
            ImageFormat.PRIVATE
        )
        // Only runs the test when the device has high resolution output sizes
        assumeTrue(maxHighResolutionOutputSize != null)

        // Arrange.
        val resolutionSelector =
            ResolutionSelector.Builder()
                .setAllowedResolutionMode(ALLOWED_RESOLUTIONS_SLOW)
                .setResolutionFilter { _, _ ->
                    listOf(maxHighResolutionOutputSize)
                }
                .build()
        val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build()

        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        instrumentation.runOnMainSync { preview.setSurfaceProvider(getSurfaceProvider(null)) }

        // Act.
        camera = CameraUtil.createCameraAndAttachUseCase(context!!, cameraSelector, preview)

        Truth.assertThat(preview.resolutionInfo!!.resolution).isEqualTo(maxHighResolutionOutputSize)

        // Assert.
        Truth.assertThat(surfaceFutureSemaphore!!.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
    }

    private val workExecutorWithNamedThread: Executor
        get() {
            val threadFactory =
                ThreadFactory { runnable: Runnable? -> Thread(runnable, ANY_THREAD_NAME) }
            return Executors.newSingleThreadExecutor(threadFactory)
        }

    private fun getSurfaceProvider(
        threadNameConsumer: Consumer<String>?
    ): Preview.SurfaceProvider {
        return SurfaceTextureProvider.createSurfaceTextureProvider(object : SurfaceTextureCallback {
            override fun onSurfaceTextureReady(
                surfaceTexture: SurfaceTexture,
                resolution: Size
            ) {
                threadNameConsumer?.accept(Thread.currentThread().name)
                previewResolution = resolution
                surfaceTexture.setOnFrameAvailableListener {
                    surfaceFutureSemaphore!!.release()
                }
            }

            override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                surfaceTexture.release()
                safeToReleaseSemaphore!!.release()
            }
        })
    }

    /*
     * Check if there is no frame callback for `noFrameIntervalMs` milliseconds, then it will
     * return true; If the total check time is over `timeoutMs` milliseconds, then it will return
     * false.
     */
    @Throws(InterruptedException::class)
    private fun noFrameCome(noFrameIntervalMs: Long, timeoutMs: Long): Boolean {
        require(!(noFrameIntervalMs <= 0 || timeoutMs <= 0)) { "Time can't be negative value." }
        require(timeoutMs >= noFrameIntervalMs) {
            "timeoutMs should be larger than noFrameIntervalMs."
        }
        val checkFrequency = 200L
        var totalCheckTime = 0L
        var zeroFrameTimer = 0L
        do {
            Thread.sleep(checkFrequency)
            if (surfaceFutureSemaphore!!.availablePermits() > 0) {
                // Has frame, reset timer and frame count.
                zeroFrameTimer = 0
                surfaceFutureSemaphore!!.drainPermits()
            } else {
                zeroFrameTimer += checkFrequency
            }
            if (zeroFrameTimer > noFrameIntervalMs) {
                return true
            }
            totalCheckTime += checkFrequency
        } while (totalCheckTime < timeoutMs)
        return false
    }
}