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
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.Range
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.DisplayInfoManager
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.ExtraCroppingQuirk
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.SurfaceRequest.TransformationInfo
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.ImageOutputConfig.OPTION_RESOLUTION_SELECTOR
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.Threads.runOnMainSync
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionFilter
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.integration.core.util.CameraInfoUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.ExtensionsUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.concurrent.futures.await
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.fail
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class PreviewTest(private val implName: String, private val cameraConfig: CameraXConfig) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    // Launch activity when testing in Vivo devices to prevent testing process from being killed.
    @get:Rule
    val wakelockEmptyActivityRule = WakelockEmptyActivityRule(brandsToEnable = listOf("vivo"))

    companion object {
        private const val ANY_THREAD_NAME = "any-thread-name"
        private val DEFAULT_RESOLUTION: Size by lazy { Size(640, 480) }
        private val DEFAULT_RESOLUTION_PORTRAIT: Size by lazy { Size(480, 640) }
        private const val FRAMES_TO_VERIFY = 10
        private const val RESULT_TIMEOUT = 5000L
        private const val TOLERANCE = 0.1f

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private var previewResolution: Size? = null
    private var frameSemaphore: Semaphore? = null
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val lifecycleOwner = FakeLifecycleOwner()

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setUp() = runBlocking {
        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()

        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context).await()
        frameSemaphore = Semaphore(/* permits= */ 0)
        lifecycleOwner.startAndResume()
    }

    @After
    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
        }
    }

    // ======================================================
    //   Section 1: SurfaceProvider behavior testing
    // ======================================================
    @Test
    fun surfaceProvider_isUsedAfterSetting() = runBlocking {
        val preview = Preview.Builder().build()
        val completableDeferred = CompletableDeferred<Unit>()

        instrumentation.runOnMainSync {
            preview.setSurfaceProvider { request ->
                val surfaceTexture = SurfaceTexture(0)
                surfaceTexture.setDefaultBufferSize(
                    request.resolution.width,
                    request.resolution.height,
                )
                surfaceTexture.detachFromGLContext()
                val surface = Surface(surfaceTexture)
                request.provideSurface(surface, CameraXExecutors.directExecutor()) {
                    surface.release()
                    surfaceTexture.release()
                }
                completableDeferred.complete(Unit)
            }

            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }
        withTimeout(3_000) { completableDeferred.await() }
    }

    private fun <T> CompletableDeferred<T>.completeOnceOnly(value: T) {
        if (!this.complete(value)) {
            throw IllegalStateException("Result Listener being invoked twice")
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun previewUnbound_RESULT_SURFACE_USED_SUCCESSFULLY_isCalled() = runBlocking {
        // Arrange.
        val preview = Preview.Builder().build()
        val resultDeferred = CompletableDeferred<Int>()
        // Act.
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                getSurfaceProvider(
                    frameAvailableListener = { frameSemaphore!!.release() },
                    resultListener = { result ->
                        resultDeferred.completeOnceOnly(result.resultCode)
                    },
                ),
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Wait until preview gets frame.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 5)

        // Ensure resultListener is not invoked before unbind.
        assertThat(withTimeoutOrNull(500) { resultDeferred.await() }).isNull()

        // Remove the UseCase from the camera
        instrumentation.runOnMainSync { cameraProvider.unbind(preview) }

        // Assert.
        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setSurfaceProviderBeforeBind_getsFrame() {
        // Arrange.
        val preview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            )
            // Act.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Assert.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setSurfaceProviderBeforeBind_providesSurfaceOnWorkerExecutorThread() {
        val threadName = AtomicReference<String>()

        // Arrange.
        val preview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                workExecutorWithNamedThread,
                getSurfaceProvider(
                    threadNameConsumer = { newValue: String -> threadName.set(newValue) },
                    frameAvailableListener = { frameSemaphore!!.release() },
                ),
            )

            // Act.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Assert.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
        assertThat(threadName.get()).isEqualTo(ANY_THREAD_NAME)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setSurfaceProviderAfterBind_getsFrame() {
        // Arrange.
        val preview = Preview.Builder().build()

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)

            // Act.
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            )
        }

        // Assert.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setSurfaceProviderAfterBind_providesSurfaceOnWorkerExecutorThread() {
        val threadName = AtomicReference<String>()

        // Arrange.
        val preview = Preview.Builder().build()

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            // Act.
            preview.setSurfaceProvider(
                workExecutorWithNamedThread,
                getSurfaceProvider(
                    threadNameConsumer = { newValue: String -> threadName.set(newValue) },
                    frameAvailableListener = { frameSemaphore!!.release() },
                ),
            )
        }

        // Assert.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
        assertThat(threadName.get()).isEqualTo(ANY_THREAD_NAME)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setMultipleNonNullSurfaceProviders_getsFrame() {
        val preview = Preview.Builder().build()

        instrumentation.runOnMainSync {
            // Set a different SurfaceProvider which will provide a different surface to be used
            // for preview.
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)

        // Use another sSemaphore to monitor the frames from the 2nd surfaceProvider.
        val frameSemaphore2 = Semaphore(/* permits= */ 0)

        instrumentation.runOnMainSync {
            // Set a different SurfaceProvider which will provide a different surface to be used
            // for preview.
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore2.release() })
            )
        }
        frameSemaphore2.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setMultipleNullableSurfaceProviders_getsFrame() {
        val preview = Preview.Builder().build()

        instrumentation.runOnMainSync {
            // Set a different SurfaceProvider which will provide a different surface to be used
            // for preview.
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)

        // Recreate the semaphore to monitor the frame available callback.
        val frameSemaphore2 = Semaphore(/* permits= */ 0)

        instrumentation.runOnMainSync {
            // Set the SurfaceProvider to null in order to force the Preview into an inactive
            // state before setting a different SurfaceProvider for preview.
            preview.setSurfaceProvider(null)
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore2.release() })
            )
        }
        frameSemaphore2.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    fun willNotProvideSurface_resultCode_WILL_NOT_PROVIDE_SURFACE(): Unit = runBlocking {
        val preview = Preview.Builder().build()

        val result: Int =
            withContext(Dispatchers.Main) {
                val surfaceRequestDeferred = CompletableDeferred<SurfaceRequest>()
                preview.setSurfaceProvider { request ->
                    if (!surfaceRequestDeferred.complete(request)) {
                        // Ignore any new results. Could also call preview.setSurfaceProvider(null)
                        // on successful completion to ensure no further requests are sent.
                        request.willNotProvideSurface()
                    }
                }

                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)

                withTimeoutOrNull(RESULT_TIMEOUT) { surfaceRequestDeferred.await() }
                    ?.let { request ->
                        val resultDeferred = CompletableDeferred<Int>()
                        request.willNotProvideSurface()
                        val surface = Surface(SurfaceTexture(0))
                        // can't provideSurface successfully after willNotProvideSurface.
                        // RESULT_WILL_NOT_PROVIDE_SURFACE will be notified.
                        request.provideSurface(surface, CameraXExecutors.directExecutor()) { result
                            ->
                            resultDeferred.completeOnceOnly(result.resultCode)
                        }

                        withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() }
                            ?: fail("Timed out while waiting for surface result.")
                    } ?: fail("Timed out while waiting for surface request.")
            }

        assertThat(result).isEqualTo(SurfaceRequest.Result.RESULT_WILL_NOT_PROVIDE_SURFACE)
    }

    @Test
    fun provideSurfaceTwice_resultCode_SURFACE_ALREADY_PROVIDED(): Unit = runBlocking {
        val preview = Preview.Builder().build()

        val resultDeferred1 = CompletableDeferred<Int>()
        val resultDeferred2 = CompletableDeferred<Int>()
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider { surfaceRequest ->
                runBlocking {
                    val surfaceTextureHolder =
                        SurfaceTextureProvider.createAutoDrainingSurfaceTextureAsync(
                                surfaceRequest.resolution.width,
                                surfaceRequest.resolution.height,
                                { frameSemaphore!!.release() },
                            )
                            .await()
                    val surface = Surface(surfaceTextureHolder!!.surfaceTexture)
                    surfaceRequest.provideSurface(
                        surface,
                        CameraXExecutors.directExecutor(),
                        { result ->
                            surfaceTextureHolder.close()
                            surface.release()
                            resultDeferred1.completeOnceOnly(result.resultCode)
                        },
                    )

                    // Invoking provideSurface twice is a no-op and the result will be
                    // RESULT_SURFACE_ALREADY_PROVIDED
                    surfaceRequest.provideSurface(
                        Surface(SurfaceTexture(1)),
                        CameraXExecutors.directExecutor(),
                    ) { result ->
                        resultDeferred2.completeOnceOnly(result.resultCode)
                    }
                }
            }
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Wait until preview gets frame.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)

        instrumentation.runOnMainSync { cameraProvider.unbind(preview) }

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred2.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_ALREADY_PROVIDED)

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred1.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)
    }

    @Test
    fun surfaceRequestCancelled_resultCode_REQUEST_CANCELLED() = runBlocking {
        val preview = Preview.Builder().build()

        val resultDeferred = CompletableDeferred<Int>()
        val surfaceRequestDeferred = CompletableDeferred<SurfaceRequest>()
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider { surfaceRequest ->
                surfaceRequestDeferred.complete(surfaceRequest)
            }
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            // unbind the use case to cancel the surface request.
            cameraProvider.unbind(preview)
        }

        // Small delay to allow the SurfaceRequest to be cancelled.
        delay(500)

        val surfaceRequest = surfaceRequestDeferred.await()
        instrumentation.runOnMainSync {
            surfaceRequest.provideSurface(
                Surface(SurfaceTexture(0)),
                CameraXExecutors.directExecutor(),
            ) { result ->
                resultDeferred.completeOnceOnly(result.resultCode)
            }
        }

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_REQUEST_CANCELLED)
    }

    @Test
    fun newSurfaceProviderAfterSurfaceProvided_resultCode_SURFACE_USED_SUCCESSFULLY() =
        runBlocking {
            val preview = Preview.Builder().build()

            val resultDeferred1 = CompletableDeferred<Int>()
            val resultDeferred2 = CompletableDeferred<Int>()
            instrumentation.runOnMainSync {
                preview.setSurfaceProvider(CameraXExecutors.mainThreadExecutor()) { surfaceRequest
                    ->
                    val surface = Surface(SurfaceTexture(0))
                    surfaceRequest.provideSurface(surface, CameraXExecutors.directExecutor()) {
                        result ->
                        resultDeferred1.completeOnceOnly(result.resultCode)
                    }

                    // After the surface is provided, if there is a new request (here we trigger by
                    // setting another surfaceProvider), the previous surfaceRequest will receive
                    // RESULT_SURFACE_USED_SUCCESSFULLY.
                    preview.setSurfaceProvider(
                        getSurfaceProvider(
                            frameAvailableListener = { frameSemaphore!!.release() },
                            resultListener = { resultDeferred2.completeOnceOnly(it.resultCode) },
                        )
                    )
                }
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }

            assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred1.await() })
                .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)

            // Wait until preview gets frame.
            frameSemaphore!!.verifyFramesReceived(
                frameCount = FRAMES_TO_VERIFY,
                timeoutInSeconds = 5,
            )

            instrumentation.runOnMainSync { cameraProvider.unbindAll() }

            assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred2.await() })
                .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)
        }

    @Test
    fun newSurfaceRequestAfterSurfaceProvided_resultCode_SURFACE_USED_SUCCESSFULLY() = runBlocking {
        val preview = Preview.Builder().build()

        val resultDeferred1 = CompletableDeferred<Int>()
        val resultDeferred2 = CompletableDeferred<Int>()

        var surfaceRequestCount = 0
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(CameraXExecutors.mainThreadExecutor()) { surfaceRequest ->
                // the surface will be requested twice on the same SurfaceProvider instance.
                if (surfaceRequestCount == 0) {
                    val surfaceTexture = SurfaceTexture(0)
                    val surface = Surface(surfaceTexture)
                    surfaceRequest.provideSurface(surface, CameraXExecutors.directExecutor()) {
                        result ->
                        surfaceTexture.release()
                        surface.release()
                        resultDeferred1.completeOnceOnly(result.resultCode)
                    }

                    // After the surface is provided, if there is a new request (here we trigger by
                    // unbind and rebind), the previous surfaceRequest will receive
                    // RESULT_SURFACE_USED_SUCCESSFULLY.
                    cameraProvider.unbind(preview)
                    surfaceRequestCount++
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } else {
                    runBlocking {
                        val surfaceTextureHolder =
                            SurfaceTextureProvider.createAutoDrainingSurfaceTextureAsync(
                                    surfaceRequest.resolution.width,
                                    surfaceRequest.resolution.height,
                                    { frameSemaphore!!.release() },
                                )
                                .await()
                        val surface = Surface(surfaceTextureHolder.surfaceTexture)
                        surfaceRequest.provideSurface(surface, CameraXExecutors.directExecutor()) {
                            result ->
                            surfaceTextureHolder.close()
                            surface.release()
                            resultDeferred2.completeOnceOnly(result.resultCode)
                        }
                    }
                }
            }
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred1.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)

        // Wait until preview gets frame.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 5)

        instrumentation.runOnMainSync { cameraProvider.unbindAll() }

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred2.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)
    }

    @Test
    @Throws(InterruptedException::class)
    fun setNullSurfaceProvider_shouldStopPreview() {
        // Arrange.
        val preview = Preview.Builder().build()

        // Act.
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                    frameSemaphore!!.release()
                },
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Assert.
        // Wait until preview gets frame.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)

        // Act.
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(CameraXExecutors.mainThreadExecutor(), null)
        }

        // Assert.
        // No frame coming for 3 seconds in 10 seconds timeout.
        assertThat(noFrameCome(3000L, 10000L)).isTrue()
    }

    @Test
    fun surfaceClosed_resultCode_INVALID_SURFACE() = runBlocking {
        // Arrange.
        val preview = Preview.Builder().build()
        val resultDeferred1 = CompletableDeferred<Int>()

        // Act.
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                CameraXExecutors.mainThreadExecutor(),
                { request ->
                    request.provideSurface(
                        Surface(SurfaceTexture(0)).also { it.release() }, // invalid surface
                        CameraXExecutors.directExecutor(),
                    ) { result ->
                        resultDeferred1.completeOnceOnly(result.resultCode)
                    }
                },
            )

            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred1.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_INVALID_SURFACE)
    }

    // ======================================================
    // Section 2: targetResolution / targetRotation / targetAspectRatio
    // ======================================================

    @Test
    fun defaultAspectRatioWillBeSet_whenTargetResolutionIsNotSet() =
        runBlocking(Dispatchers.Main) {
            val useCase = Preview.Builder().build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)
            val config = useCase.currentConfig as ImageOutputConfig
            assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
        }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun defaultAspectRatioWillBeSet_whenRatioDefaultIsSet() =
        runBlocking(Dispatchers.Main) {
            assumeTrue(
                !hasExtraCroppingQuirk() &&
                    isAspectRatioResolutionSupported(4.0f / 3.0f, isLegacyApi = true)
            )
            val useCase = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_DEFAULT).build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)
            val config = useCase.currentConfig as ImageOutputConfig
            assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
            val resolution = useCase.resolutionInfo!!.resolution
            assertThat(resolution.width.toFloat() / resolution.height)
                .isWithin(TOLERANCE)
                .of(4.0f / 3.0f)
        }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun aspectRatio4_3_resolutionIsSet() =
        runBlocking(Dispatchers.Main) {
            assumeTrue(
                !hasExtraCroppingQuirk() &&
                    isAspectRatioResolutionSupported(4.0f / 3.0f, isLegacyApi = true)
            )

            val useCase = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)
            val config = useCase.currentConfig as ImageOutputConfig
            assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
            val resolution = useCase.resolutionInfo!!.resolution
            assertThat(resolution.width.toFloat() / resolution.height)
                .isWithin(TOLERANCE)
                .of(4.0f / 3.0f)
        }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun aspectRatio16_9_resolutionIsSet() =
        runBlocking(Dispatchers.Main) {
            assumeTrue(
                !hasAspectRatioLegacyApi21Quirk() &&
                    isAspectRatioResolutionSupported(16.0f / 9.0f, isLegacyApi = true)
            )
            val useCase = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9).build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)
            val config = useCase.currentConfig as ImageOutputConfig
            assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_16_9)
            val resolution = useCase.resolutionInfo!!.resolution
            assertThat(resolution.width.toFloat() / resolution.height)
                .isWithin(TOLERANCE)
                .of(16.0f / 9.0f)
        }

    private fun isAspectRatioResolutionSupported(
        targetAspectRatioValue: Float,
        isLegacyApi: Boolean = false,
    ): Boolean {
        val cameraCharacteristics =
            (cameraProvider.getCameraInfo(cameraSelector) as CameraInfoInternal)
                .cameraCharacteristics as CameraCharacteristics
        val map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSizes = map!!.getOutputSizes(SurfaceTexture::class.java)
        return previewSizes.find { size ->
            val aspectRatioVal = size.width.toFloat() / size.height.toFloat()
            abs(targetAspectRatioValue - aspectRatioVal) <= TOLERANCE &&
                isValidPreviewSize(size, isLegacyApi)
        } != null
    }

    private fun isValidPreviewSize(size: Size, isLegacyApi: Boolean): Boolean {
        val previewDefinitionSize = getPreviewDefinitionSize()
        val sizeArea = SizeUtil.getArea(size)
        val previewSizeArea = SizeUtil.getArea(previewDefinitionSize)

        // When using ResolutionSelector API, all sizes equal to or smaller than PREVIEW size can
        // be selected.
        if (!isLegacyApi) {
            return sizeArea <= previewSizeArea
        }

        // When using Legacy API, if the PREVIEW size is smaller than 640x480, all output sizes
        // smaller than it can be selected. The reason is, for some devices, there might be 16:9
        // output sizes smaller than 480P. But it might make the preview have bad image quality
        // when the display size is larger than 480P. Therefore, those sizes can be selected to use
        // only when the device display size is smaller than 480P or apps explicitly set a target
        // resolution smaller than 480P.
        if (SizeUtil.isSmallerByArea(previewDefinitionSize, SizeUtil.RESOLUTION_480P)) {
            return SizeUtil.isSmallerByArea(size, previewDefinitionSize)
        }

        // Otherwise, only sizes between PREVIEW size and 640x480 can be selected
        val vgaSizeArea = SizeUtil.getArea(SizeUtil.RESOLUTION_VGA)

        return sizeArea in vgaSizeArea..previewSizeArea
    }

    @Suppress("DEPRECATION") // getRealSize
    private fun getPreviewDefinitionSize(): Size {
        val point = Point()
        DisplayInfoManager.getInstance(context).getMaxSizeDisplay(false).also {
            it.getRealSize(point)
        }
        val previewSize =
            if (point.x > point.y) {
                Size(point.x, point.y)
            } else {
                Size(point.y, point.x)
            }
        return if (SizeUtil.isSmallerByArea(previewSize, SizeUtil.RESOLUTION_1080P)) {
            previewSize
        } else {
            SizeUtil.RESOLUTION_1080P
        }
    }

    private fun hasExtraCroppingQuirk(): Boolean {
        return (implName.contains(CameraPipeConfig::class.simpleName!!) &&
            DeviceQuirks[ExtraCroppingQuirk::class.java] != null) ||
            androidx.camera.camera2.internal.compat.quirk.DeviceQuirks.get(
                androidx.camera.camera2.internal.compat.quirk.ExtraCroppingQuirk::class.java
            ) != null
    }

    // Checks whether it is the device for AspectRatioLegacyApi21Quirk
    private fun hasAspectRatioLegacyApi21Quirk(): Boolean {
        val quirks =
            (cameraProvider.getCameraInfo(cameraSelector) as CameraInfoInternal).cameraQuirks
        return if (implName == CameraPipeConfig::class.simpleName) {
            quirks.contains(
                androidx.camera.camera2.pipe.integration.compat.quirk
                        .AspectRatioLegacyApi21Quirk::class
                    .java
            )
        } else {
            quirks.contains(
                androidx.camera.camera2.internal.compat.quirk.AspectRatioLegacyApi21Quirk::class
                    .java
            )
        }
    }

    @Suppress("DEPRECATION") // legacy resolution API
    @Test
    fun defaultAspectRatioWontBeSet_andResolutionIsSet_whenTargetResolutionIsSet() =
        runBlocking(Dispatchers.Main) {
            assumeTrue(
                CameraUtil.isCameraSensorPortraitInNativeOrientation(cameraSelector.lensFacing!!) &&
                    !hasExtraCroppingQuirk()
            )
            val useCase =
                Preview.Builder()
                    .setTargetResolution(DEFAULT_RESOLUTION_PORTRAIT)
                    .setTargetRotation(Surface.ROTATION_0) // enforce native orientation.
                    .build()
            assertThat(
                    useCase.currentConfig.containsOption(
                        ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO
                    )
                )
                .isFalse()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCase,
            )

            assertThat(
                    useCase.currentConfig.containsOption(
                        ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO
                    )
                )
                .isFalse()
            assertThat(useCase.resolutionInfo!!.resolution).isEqualTo(DEFAULT_RESOLUTION)
        }

    @Test
    fun targetRotationIsRetained_whenUseCaseIsReused() =
        runBlocking(Dispatchers.Main) {
            val useCase = Preview.Builder().build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)

            // Generally, the device can't be rotated to Surface.ROTATION_180. Therefore,
            // use it to do the test.
            useCase.targetRotation = Surface.ROTATION_180
            cameraProvider.unbind(useCase)

            // Check the target rotation is kept when the use case is unbound.
            assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)

            // Check the target rotation is kept when the use case is rebound to the
            // lifecycle.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)
            assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)
        }

    @Test
    fun targetRotationReturnsDisplayRotationIfNotSet() =
        runBlocking(Dispatchers.Main) {
            val displayRotation =
                DisplayInfoManager.getInstance(context).getMaxSizeDisplay(true).rotation
            val useCase = Preview.Builder().build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)

            assertThat(useCase.targetRotation).isEqualTo(displayRotation)
        }

    @Test
    fun returnValidTargetRotation_afterUseCaseIsCreated() {
        val preview = Preview.Builder().build()
        assertThat(preview.targetRotation).isNotEqualTo(ImageOutputConfig.INVALID_ROTATION)
    }

    @Test
    fun returnCorrectTargetRotation_afterUseCaseIsBound() =
        runBlocking(Dispatchers.Main) {
            val preview = Preview.Builder().setTargetRotation(Surface.ROTATION_180).build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            assertThat(preview.targetRotation).isEqualTo(Surface.ROTATION_180)
        }

    @Suppress("DEPRECATION") // legacy resolution API
    @Test
    fun setTargetRotationOnBuilder_ResolutionIsSetCorrectly() =
        runBlocking(Dispatchers.Main) {
            assumeTrue(
                CameraUtil.isCameraSensorPortraitInNativeOrientation(cameraSelector.lensFacing!!) &&
                    !hasExtraCroppingQuirk()
            )
            val preview =
                Preview.Builder()
                    .setTargetRotation(Surface.ROTATION_90)
                    .setTargetResolution(DEFAULT_RESOLUTION)
                    .build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            assertThat(preview.resolutionInfo!!.resolution).isEqualTo(DEFAULT_RESOLUTION)
            assertThat(preview.resolutionInfo!!.rotationDegrees).isEqualTo(0)
        }

    @Test
    fun setTargetRotationAfterBind_transformationInfoIsUpdated() =
        runBlocking(Dispatchers.Main) {
            assumeTrue(
                CameraUtil.isCameraSensorPortraitInNativeOrientation(cameraSelector.lensFacing!!)
            )

            var transformationInfoDeferred = CompletableDeferred<TransformationInfo>()
            val surfaceProvidedDeferred = CompletableDeferred<SurfaceRequest>()

            val preview = Preview.Builder().setTargetRotation(Surface.ROTATION_0).build()
            preview.surfaceProvider =
                Preview.SurfaceProvider { request ->
                    request.setTransformationInfoListener(CameraXExecutors.directExecutor()) {
                        transformationInfoDeferred.complete(it)
                    }
                    request.provideSurface(
                        Surface(SurfaceTexture(0)),
                        CameraXExecutors.directExecutor(),
                    ) {}
                    surfaceProvidedDeferred.complete(request)
                }

            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            surfaceProvidedDeferred.await()
            var transformationInfo = withTimeoutOrNull(5000) { transformationInfoDeferred.await() }
            assertThat(transformationInfo).isNotNull()
            assertThat(transformationInfo!!.rotationDegrees).isEqualTo(90)

            transformationInfoDeferred = CompletableDeferred()
            preview.targetRotation = Surface.ROTATION_90

            transformationInfo = withTimeoutOrNull(5000) { transformationInfoDeferred.await() }
            assertThat(transformationInfo).isNotNull()
            assertThat(transformationInfo!!.rotationDegrees).isEqualTo(0)
        }

    @Test
    fun viewPort_OverwriteTransformation() = runBlocking {
        // Arrange.
        val rotation =
            if (CameraUtil.getSensorOrientation(cameraSelector.lensFacing!!)!! % 180 != 0)
                Surface.ROTATION_90
            else Surface.ROTATION_0
        val transformationInfoDeferred = CompletableDeferred<TransformationInfo>()
        val preview = Preview.Builder().setTargetRotation(rotation).build()
        val viewPort = ViewPort.Builder(Rational(2, 1), preview.targetRotation).build()

        // Act.
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider { request ->
                request.setTransformationInfoListener(CameraXExecutors.directExecutor()) {
                    transformationInfoDeferred.complete(it)
                }
            }
            val useCaseGroup =
                UseCaseGroup.Builder().setViewPort(viewPort).addUseCase(preview).build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
        }
        val transformationInfo = withTimeoutOrNull(5000) { transformationInfoDeferred.await() }

        // Assert.
        assertThat(
                Rational(
                        transformationInfo!!.cropRect.width(),
                        transformationInfo.cropRect.height(),
                    )
                    .toFloat()
            )
            .isWithin(TOLERANCE)
            .of(viewPort.aspectRatio.toFloat())
    }

    // ======================================================
    // Section 3: UseCase Reusability Test
    // ======================================================

    @Test
    fun useCaseConfigCanBeReset_afterUnbind() =
        runBlocking(Dispatchers.Main) {
            val preview = Preview.Builder().build()
            val initialConfig = preview.currentConfig
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            cameraProvider.unbind(preview)
            val configAfterUnbinding = preview.currentConfig
            assertThat(initialConfig == configAfterUnbinding).isTrue()
        }

    @Test
    @Throws(InterruptedException::class)
    fun useCaseCanBeReusedInSameCamera() = runBlocking {
        val preview = Preview.Builder().build()
        var resultDeferred = CompletableDeferred<Int>()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                getSurfaceProvider(
                    frameAvailableListener = { frameSemaphore!!.release() },
                    resultListener = { result ->
                        resultDeferred.completeOnceOnly(result.resultCode)
                    },
                )
            )
            // This is the first time the use case bound to the lifecycle.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Check the frame available callback is called.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
        withContext(Dispatchers.Main) { cameraProvider.unbind(preview) }

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)

        // Recreate the semaphore / deferred to monitor the frame available callback.
        frameSemaphore = Semaphore(/* permits= */ 0)
        // Recreate the resultDeferred to monitor the result listener again.
        resultDeferred = CompletableDeferred()

        withContext(Dispatchers.Main) {
            // Rebind the use case to the same camera.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Check the frame available callback can be called after reusing the use case.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
        withContext(Dispatchers.Main) { cameraProvider.unbind(preview) }
        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)
    }

    @Test
    @Throws(InterruptedException::class)
    fun useCaseCanBeReusedInDifferentCamera() = runBlocking {
        val cameraSelectors = CameraUtil.getAvailableCameraSelectors()
        assumeTrue("No enough cameras to test.", cameraSelectors.size >= 2)

        val preview = Preview.Builder().build()
        var resultDeferred = CompletableDeferred<Int>()
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider(
                getSurfaceProvider(
                    frameAvailableListener = { frameSemaphore!!.release() },
                    resultListener = { result ->
                        resultDeferred.completeOnceOnly(result.resultCode)
                    },
                )
            )
            // This is the first time the use case bound to the lifecycle.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelectors[0], preview)
        }

        // Check the frame available callback is called.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
        // Unbind and rebind the use case to the same lifecycle.
        instrumentation.runOnMainSync { cameraProvider.unbind(preview) }

        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)

        // Recreate the semaphore to monitor the frame available callback.
        frameSemaphore = Semaphore(/* permits= */ 0)
        // Recreate the resultDeferred to monitor the result listener again.
        resultDeferred = CompletableDeferred()

        instrumentation.runOnMainSync {
            // Rebind the use case to different camera.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelectors[1], preview)
        }

        // Check the frame available callback can be called after reusing the use case.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
        withContext(Dispatchers.Main) { cameraProvider.unbind(preview) }
        assertThat(withTimeoutOrNull(RESULT_TIMEOUT) { resultDeferred.await() })
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)
    }

    // ======================================================
    // Section 4: ResolutionSelector
    // ======================================================

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Test
    fun verifyHighResolutionIsDisabledForPreview() = runBlocking {
        val highResolutionOutputSizes =
            CameraInfoUtil.getHighResolutionOutputSizes(
                cameraProvider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA),
                ImageFormat.PRIVATE,
            )
        // Only runs the test when the device has high resolution output sizes
        assumeTrue(highResolutionOutputSizes.isNotEmpty())

        // Arrange.
        // Sets the mode to allow high resolution support and uses a ResolutionFilter to verify the
        // high resolution output sizes are not included in the provided sizes list
        val resolutionSelector =
            ResolutionSelector.Builder()
                .setAllowedResolutionMode(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .setResolutionFilter { outputSizes, _ ->
                    assertThat(outputSizes).containsNoneIn(highResolutionOutputSizes)
                    outputSizes
                }
                .build()
        val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            )
            // Act.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Assert.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    fun getsFrame_withHighestAvailableResolutionStrategy() = runBlocking {
        // Arrange.
        val resolutionSelector =
            ResolutionSelector.Builder()
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .build()
        val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            )
            // Act.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Assert.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    fun getsFrame_withAspectRatio_4_3_strategy() = runBlocking {
        assumeTrue(!hasExtraCroppingQuirk() && isAspectRatioResolutionSupported(4.0f / 3.0f))
        // Arrange.
        val resolutionSelector =
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            )
            // Act.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Assert.
        val resolution = preview.resolutionInfo!!.resolution
        assertThat(resolution.width.toFloat() / resolution.height)
            .isWithin(TOLERANCE)
            .of(4.0f / 3.0f)
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    fun getsFrame_withAspectRatio_16_9_strategy() = runBlocking {
        assumeTrue(
            !hasAspectRatioLegacyApi21Quirk() && isAspectRatioResolutionSupported(16.0f / 9.0f)
        )

        // Arrange.
        val resolutionSelector =
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            )
            // Act.
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // Assert.
        val resolution = preview.resolutionInfo!!.resolution
        assertThat(resolution.width.toFloat() / resolution.height)
            .isWithin(TOLERANCE)
            .of(16.0f / 9.0f)
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    fun defaultMaxResolutionCanBeKept_whenResolutionStrategyIsNotSet() =
        runBlocking(Dispatchers.Main) {
            val useCase = Preview.Builder().build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)

            assertThat(
                    useCase.currentConfig.containsOption(ImageOutputConfig.OPTION_MAX_RESOLUTION)
                )
                .isTrue()
        }

    @Test
    fun defaultMaxResolutionCanBeRemoved_whenResolutionStrategyIsSet() =
        runBlocking(Dispatchers.Main) {
            val useCase =
                Preview.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                            .build()
                    )
                    .build()

            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)
            assertThat(
                    useCase.currentConfig.containsOption(ImageOutputConfig.OPTION_MAX_RESOLUTION)
                )
                .isFalse()
        }

    @Test
    fun resolutionSelectorConfigCorrectlyMerged_afterBindToLifecycle() =
        runBlocking(Dispatchers.Main) {
            val resolutionFilter = ResolutionFilter { supportedSizes, _ -> supportedSizes }
            val useCase =
                Preview.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionFilter(resolutionFilter)
                            .setAllowedResolutionMode(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                            .build()
                    )
                    .build()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCase)

            val resolutionSelector =
                useCase.currentConfig.retrieveOption(OPTION_RESOLUTION_SELECTOR)
            // The default 4:3 AspectRatioStrategy is kept
            assertThat(resolutionSelector!!.aspectRatioStrategy)
                .isEqualTo(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            // The default highest available ResolutionStrategy is kept
            assertThat(resolutionSelector.resolutionStrategy)
                .isEqualTo(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            // The set resolutionFilter is kept
            assertThat(resolutionSelector.resolutionFilter).isEqualTo(resolutionFilter)
            // The set allowedResolutionMode is kept
            assertThat(resolutionSelector.allowedResolutionMode)
                .isEqualTo(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
        }

    // ======================================================
    // Section 5: Session error handling
    // ======================================================

    @Test
    fun sessionErrorListenerReceivesError_getsFrame(): Unit = runBlocking {
        val cameraSelectors = CameraUtil.getAvailableCameraSelectors()
        assumeTrue("No enough cameras to test.", cameraSelectors.size >= 2)

        // Arrange.
        val preview = Preview.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelectors[0], preview)
            // Act.
            preview.surfaceProvider =
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
        }

        // Retrieves the initial session config
        val initialSessionConfig = preview.sessionConfig

        // Checks that image can be received successfully when onError is received.
        triggerOnErrorAndVerifyNewImageReceived(initialSessionConfig)

        // Rebinds to different camera
        withContext(Dispatchers.Main) {
            cameraProvider.unbind(preview)
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelectors[1], preview)
        }

        // Checks that image can be received successfully when onError is received by the old
        // error listener.
        triggerOnErrorAndVerifyNewImageReceived(initialSessionConfig)

        val sessionConfigBeforeValidErrorNotification = preview.sessionConfig
        // Checks that image can be received successfully when onError is received by the new
        // error listener.
        triggerOnErrorAndVerifyNewImageReceived(preview.sessionConfig)
        // Checks that triggering onError to valid listener has recreated the pipeline
        assertThat(preview.sessionConfig).isNotEqualTo(sessionConfigBeforeValidErrorNotification)
    }

    private fun triggerOnErrorAndVerifyNewImageReceived(sessionConfig: SessionConfig) {
        frameSemaphore = Semaphore(0)
        // Forces invoke the onError callback
        runOnMainSync {
            sessionConfig.errorListener!!.onError(
                sessionConfig,
                SessionConfig.SessionError.SESSION_ERROR_UNKNOWN,
            )
        }
        // Assert.
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    // ======================================================
    // Section 6: Preview Stabilization, Dynamic Range, Frame rate
    // ======================================================
    @Test
    fun previewStabilizationIsSetCorrectly(): Unit = runBlocking {
        val preview = Preview.Builder().setPreviewStabilizationEnabled(true).build()
        assertThat(preview.isPreviewStabilizationEnabled).isTrue()
    }

    private fun isPreviewStabilizationModeSupported(cameraSelector: CameraSelector): Boolean {
        val cameraInfoInternal = cameraProvider.getCameraInfo(cameraSelector) as CameraInfoInternal
        val cameraCharacteristics =
            cameraInfoInternal.cameraCharacteristics as CameraCharacteristics
        val stabilizationModes =
            cameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES
            ) as IntArray
        return stabilizationModes.contains(
            CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
        )
    }

    @Test
    fun getPreviewCapabilitiesStabilizationSupportIsCorrect() {
        val capabilities =
            Preview.getPreviewCapabilities(cameraProvider.getCameraInfo(cameraSelector))

        assertThat(capabilities.isStabilizationSupported())
            .isEqualTo(isPreviewStabilizationModeSupported(cameraSelector))
    }

    @Test
    fun previewStabilizationOn_videoStabilizationModeIsPreviewStabilization(): Unit = runBlocking {
        val previewCapabilities =
            Preview.getPreviewCapabilities(cameraProvider.getCameraInfo(cameraSelector))
        assumeTrue(previewCapabilities.isStabilizationSupported)

        val previewBuilder = Preview.Builder().setPreviewStabilizationEnabled(true)
        verifyVideoStabilizationModeInResultAndFramesAvailable(
            cameraSelector = cameraSelector,
            previewBuilder = previewBuilder,
            expectedMode = CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION,
        )
    }

    @Test
    fun previewStabilizationOnAndVideoOff_videoStabilizationModeIsOff(): Unit = runBlocking {
        val previewCapabilities =
            Preview.getPreviewCapabilities(cameraProvider.getCameraInfo(cameraSelector))
        val videoCaptureCapabilities =
            Recorder.getVideoCapabilities(cameraProvider.getCameraInfo(cameraSelector))
        assumeTrue(
            previewCapabilities.isStabilizationSupported &&
                videoCaptureCapabilities.isStabilizationSupported
        )

        val previewBuilder = Preview.Builder().setPreviewStabilizationEnabled(true)
        val videoCapture =
            VideoCapture.Builder(Recorder.Builder().build())
                .setVideoStabilizationEnabled(false)
                .build()

        verifyVideoStabilizationModeInResultAndFramesAvailable(
            cameraSelector = cameraSelector,
            previewBuilder = previewBuilder,
            videoCapture = videoCapture,
            expectedMode = CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF,
        )
    }

    @Test
    fun previewStabilizationOnAndVideoOn_videoStabilizationModeIsPreviewStabilization(): Unit =
        runBlocking {
            val previewCapabilities =
                Preview.getPreviewCapabilities(cameraProvider.getCameraInfo(cameraSelector))
            val videoCaptureCapabilities =
                Recorder.getVideoCapabilities(cameraProvider.getCameraInfo(cameraSelector))
            assumeTrue(
                previewCapabilities.isStabilizationSupported &&
                    videoCaptureCapabilities.isStabilizationSupported
            )

            val previewBuilder = Preview.Builder().setPreviewStabilizationEnabled(true)
            val videoCapture =
                VideoCapture.Builder(Recorder.Builder().build())
                    .setVideoStabilizationEnabled(true)
                    .build()

            verifyVideoStabilizationModeInResultAndFramesAvailable(
                cameraSelector = cameraSelector,
                previewBuilder = previewBuilder,
                videoCapture = videoCapture,
                expectedMode = CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION,
            )
        }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    fun getPreviewCapabilitiesStabilizationSupportIsCorrect_whenNotSupportedInExtensions() {
        assumeTrue(isPreviewStabilizationModeSupported(CameraSelector.DEFAULT_BACK_CAMERA))
        val sessionProcessor =
            FakeSessionProcessor(
                extensionSpecificChars =
                    listOf(
                        android.util.Pair(
                            CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES,
                            intArrayOf(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF),
                        )
                    )
            )
        val cameraSelector =
            ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                cameraProvider,
                CameraSelector.DEFAULT_BACK_CAMERA,
                sessionProcessor,
            )
        val capabilities =
            Preview.getPreviewCapabilities(cameraProvider.getCameraInfo(cameraSelector))

        assertThat(capabilities.isStabilizationSupported()).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    fun getPreviewCapabilitiesStabilizationSupportIsCorrect_whenSupportedInExtensions() {
        assumeFalse(isPreviewStabilizationModeSupported(CameraSelector.DEFAULT_BACK_CAMERA))
        val sessionProcessor =
            FakeSessionProcessor(
                extensionSpecificChars =
                    listOf(
                        android.util.Pair(
                            CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES,
                            intArrayOf(
                                CameraCharacteristics
                                    .CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
                            ),
                        )
                    )
            )
        val cameraSelector =
            ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                cameraProvider,
                CameraSelector.DEFAULT_BACK_CAMERA,
                sessionProcessor,
            )
        val capabilities =
            Preview.getPreviewCapabilities(cameraProvider.getCameraInfo(cameraSelector))

        assertThat(capabilities.isStabilizationSupported()).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    fun previewStabilizationCanBeSet_whenSupportedInExtensions() = runBlocking {
        assumeTrue(isPreviewStabilizationModeSupported(CameraSelector.DEFAULT_BACK_CAMERA))
        val sessionProcessor =
            FakeSessionProcessor(
                extensionSpecificChars =
                    listOf(
                        android.util.Pair(
                            CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES,
                            intArrayOf(
                                CameraCharacteristics
                                    .CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
                            ),
                        )
                    )
            )
        val cameraSelectorWithExtensions =
            ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                cameraProvider,
                CameraSelector.DEFAULT_BACK_CAMERA,
                sessionProcessor,
            )

        val previewBuilder = Preview.Builder().setPreviewStabilizationEnabled(true)
        verifyVideoStabilizationModeInResultAndFramesAvailable(
            cameraSelector = cameraSelectorWithExtensions,
            previewBuilder = previewBuilder,
            expectedMode = CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = 32)
    fun setMirrorModeIsNoOp_priorToAPI33() = runBlocking {
        // Skip for b/404348154
        assumeFalse("Skip test for API 26.", Build.VERSION.SDK_INT == 26)
        verifyMirrorMode(
            CameraSelector.DEFAULT_BACK_CAMERA,
            mirrorMode = MirrorMode.MIRROR_MODE_ON,
            expectedMirrorMode = MirrorMode.MIRROR_MODE_ON_FRONT_ONLY,
            expectedIsMirroringInTransformationInfo = false,
        )

        verifyMirrorMode(
            CameraSelector.DEFAULT_FRONT_CAMERA,
            mirrorMode = MirrorMode.MIRROR_MODE_ON,
            expectedMirrorMode = MirrorMode.MIRROR_MODE_ON_FRONT_ONLY,
            expectedIsMirroringInTransformationInfo = true,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun defaultMirrorMode() = runBlocking {
        verifyMirrorMode(
            CameraSelector.DEFAULT_BACK_CAMERA,
            mirrorMode = null, // don't set the mirror mode
            expectedMirrorMode = MirrorMode.MIRROR_MODE_ON_FRONT_ONLY,
            expectedIsMirroringInTransformationInfo = false,
        )

        verifyMirrorMode(
            CameraSelector.DEFAULT_FRONT_CAMERA,
            mirrorMode = null, // don't set the mirror mode
            expectedMirrorMode = MirrorMode.MIRROR_MODE_ON_FRONT_ONLY,
            expectedIsMirroringInTransformationInfo = true,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun mirrorModeOn() = runBlocking {
        verifyMirrorMode(
            CameraSelector.DEFAULT_BACK_CAMERA,
            mirrorMode = MirrorMode.MIRROR_MODE_ON,
            expectedMirrorMode = MirrorMode.MIRROR_MODE_ON,
            expectedIsMirroringInTransformationInfo = true,
        )

        verifyMirrorMode(
            CameraSelector.DEFAULT_FRONT_CAMERA,
            mirrorMode = MirrorMode.MIRROR_MODE_ON,
            expectedMirrorMode = MirrorMode.MIRROR_MODE_ON,
            expectedIsMirroringInTransformationInfo = true,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun mirrorModeOff() = runBlocking {
        verifyMirrorMode(
            CameraSelector.DEFAULT_BACK_CAMERA,
            mirrorMode = MirrorMode.MIRROR_MODE_OFF,
            expectedMirrorMode = MirrorMode.MIRROR_MODE_OFF,
            expectedIsMirroringInTransformationInfo = false,
        )

        verifyMirrorMode(
            CameraSelector.DEFAULT_FRONT_CAMERA,
            mirrorMode = MirrorMode.MIRROR_MODE_OFF,
            expectedMirrorMode = MirrorMode.MIRROR_MODE_OFF,
            expectedIsMirroringInTransformationInfo = false,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun mirrorModeOnFrontOnly() = runBlocking {
        verifyMirrorMode(
            CameraSelector.DEFAULT_BACK_CAMERA,
            mirrorMode = MirrorMode.MIRROR_MODE_ON_FRONT_ONLY,
            expectedMirrorMode = MirrorMode.MIRROR_MODE_ON_FRONT_ONLY,
            expectedIsMirroringInTransformationInfo = false,
        )

        verifyMirrorMode(
            CameraSelector.DEFAULT_FRONT_CAMERA,
            MirrorMode.MIRROR_MODE_ON_FRONT_ONLY,
            expectedMirrorMode = MirrorMode.MIRROR_MODE_ON_FRONT_ONLY,
            expectedIsMirroringInTransformationInfo = true,
        )
    }

    private suspend fun verifyMirrorMode(
        cameraSelector: CameraSelector,
        mirrorMode: Int? = null,
        expectedMirrorMode: Int,
        expectedIsMirroringInTransformationInfo: Boolean,
    ) {
        val preview =
            Preview.Builder()
                .also { builder -> mirrorMode?.let { builder.setMirrorMode(it) } }
                .build()

        val transformationInfoDeferred = CompletableDeferred<TransformationInfo>()
        withContext(Dispatchers.Main) {
            cameraProvider.unbindAll()
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider(
                    null,
                    { surfaceRequest ->
                        surfaceRequest.setTransformationInfoListener(
                            CameraXExecutors.directExecutor(),
                            { transformationInfo ->
                                transformationInfoDeferred.complete(transformationInfo)
                            },
                        )
                    },
                    {},
                )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        // TODO: better check the Camera2 OutputConfiguration but currently there is no way do that.
        assertThat(preview.sessionConfig.outputConfigs.get(0).mirrorMode)
            .isEqualTo(expectedMirrorMode)
        assertThat(withTimeoutOrNull(1000) { transformationInfoDeferred.await() }!!.isMirroring)
            .isEqualTo(expectedIsMirroringInTransformationInfo)
    }

    private suspend fun verifyVideoStabilizationModeInResultAndFramesAvailable(
        cameraSelector: CameraSelector,
        previewBuilder: Preview.Builder,
        videoCapture: VideoCapture<Recorder>? = null,
        expectedMode: Int,
    ) {
        val captureResultDeferred = CompletableDeferred<TotalCaptureResult>()
        Camera2Interop.Extender(previewBuilder)
            .setSessionCaptureCallback(
                object : CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult,
                    ) {
                        captureResultDeferred.complete(result)
                    }
                }
            )
        val useCaseGroupBuilder = UseCaseGroup.Builder()
        val preview = previewBuilder.build()
        useCaseGroupBuilder.addUseCase(preview)
        videoCapture?.let { useCaseGroupBuilder.addUseCase(it) }

        withContext(Dispatchers.Main) {
            preview.surfaceProvider =
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                useCaseGroupBuilder.build(),
            )
        }

        assertThat(
                withTimeoutOrNull(5000) { captureResultDeferred.await() }!!
                    .request
                    .get(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE)
            )
            .isEqualTo(expectedMode)
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    @Test
    fun dynamicRange_HLG10BIT(): Unit = runBlocking {
        assumeTrue(
            cameraProvider
                .getCameraInfo(cameraSelector)
                .querySupportedDynamicRanges(setOf(DynamicRange.HLG_10_BIT))
                .contains(DynamicRange.HLG_10_BIT)
        )

        val preview = Preview.Builder().setDynamicRange(DynamicRange.HLG_10_BIT).build()
        withContext(Dispatchers.Main) {
            preview.surfaceProvider =
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        assertThat(preview.dynamicRange).isEqualTo(DynamicRange.HLG_10_BIT)
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 5)
    }

    @Test
    fun dynamicRangeIsSetInSurfaceRequest(): Unit = runBlocking {
        assumeTrue(
            cameraProvider
                .getCameraInfo(cameraSelector)
                .querySupportedDynamicRanges(setOf(DynamicRange.HLG_10_BIT))
                .contains(DynamicRange.HLG_10_BIT)
        )

        val surfaceRequestDeferred = CompletableDeferred<SurfaceRequest>()
        val preview = Preview.Builder().setDynamicRange(DynamicRange.HLG_10_BIT).build()
        withContext(Dispatchers.Main) {
            preview.surfaceProvider =
                Preview.SurfaceProvider { surfaceRequest ->
                    surfaceRequestDeferred.complete(surfaceRequest)
                }
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        assertThat(withTimeoutOrNull(3000) { surfaceRequestDeferred.await() }!!.dynamicRange)
            .isEqualTo(DynamicRange.HLG_10_BIT)
    }

    @Test
    fun dynamicRangeIsNotSet_SDRInSurfaceRequest(): Unit = runBlocking {
        val surfaceRequestDeferred = CompletableDeferred<SurfaceRequest>()
        val preview = Preview.Builder().build()
        withContext(Dispatchers.Main) {
            preview.surfaceProvider =
                Preview.SurfaceProvider { surfaceRequest ->
                    surfaceRequestDeferred.complete(surfaceRequest)
                }
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        assertThat(withTimeoutOrNull(3000) { surfaceRequestDeferred.await() }!!.dynamicRange)
            .isEqualTo(DynamicRange.SDR)
    }

    @Test
    fun canSetFrameRate30_30(): Unit = runBlocking {
        val fpsToVerify = Range(30, 30)
        assumeTrue(
            cameraProvider
                .getCameraInfo(cameraSelector)
                .supportedFrameRateRanges
                .contains(fpsToVerify)
        )
        val previewBuilder = Preview.Builder().setTargetFrameRate(fpsToVerify)
        verifyFrameRateRangeInResultAndFramesAvailable(
            previewBuilder = previewBuilder,
            expectedFpsRange = fpsToVerify,
        )
    }

    @Test
    fun frameRateIsSetInSurfaceRequest(): Unit = runBlocking {
        val fpsToVerify = Range(30, 30)
        assumeTrue(
            cameraProvider
                .getCameraInfo(cameraSelector)
                .supportedFrameRateRanges
                .contains(fpsToVerify)
        )
        val previewBuilder = Preview.Builder().setTargetFrameRate(fpsToVerify)

        val preview = previewBuilder.build()
        val surfaceRequestDeferred = CompletableDeferred<SurfaceRequest>()

        withContext(Dispatchers.Main) {
            preview.surfaceProvider =
                Preview.SurfaceProvider { surfaceRequest ->
                    surfaceRequestDeferred.complete(surfaceRequest)
                }
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }
        assertThat(withTimeoutOrNull(3000) { surfaceRequestDeferred.await() }!!.expectedFrameRate)
            .isEqualTo(fpsToVerify)
    }

    @Test
    fun canSetFrameRate60_60(): Unit = runBlocking {
        val fpsToVerify = Range(60, 60)
        assumeTrue(
            cameraProvider
                .getCameraInfo(cameraSelector)
                .supportedFrameRateRanges
                .contains(fpsToVerify)
        )
        val previewBuilder = Preview.Builder().setTargetFrameRate(fpsToVerify)

        verifyFrameRateRangeInResultAndFramesAvailable(
            previewBuilder = previewBuilder,
            expectedFpsRange = fpsToVerify,
        )
    }

    private suspend fun verifyFrameRateRangeInResultAndFramesAvailable(
        previewBuilder: Preview.Builder,
        expectedFpsRange: Range<Int>,
    ) {
        val captureResultDeferred = CompletableDeferred<TotalCaptureResult>()
        Camera2Interop.Extender(previewBuilder)
            .setSessionCaptureCallback(
                object : CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult,
                    ) {
                        captureResultDeferred.complete(result)
                    }
                }
            )
        val preview = previewBuilder.build()
        assertThat(preview.targetFrameRate).isEqualTo(expectedFpsRange)

        withContext(Dispatchers.Main) {
            preview.surfaceProvider =
                getSurfaceProvider(frameAvailableListener = { frameSemaphore!!.release() })
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }

        val captureResult = withTimeoutOrNull(5000) { captureResultDeferred.await() }
        assertThat(captureResult).isNotNull()
        val fpsRangeInResult =
            captureResult!!.request.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE)
        // ignore the case CONTROL_AE_TARGET_FPS_RANGE is null
        assumeTrue(fpsRangeInResult != null)
        assertThat(fpsRangeInResult).isEqualTo(expectedFpsRange)
        frameSemaphore!!.verifyFramesReceived(frameCount = FRAMES_TO_VERIFY, timeoutInSeconds = 10)
    }

    private val workExecutorWithNamedThread: Executor
        get() {
            val threadFactory = ThreadFactory { runnable: Runnable? ->
                Thread(runnable, ANY_THREAD_NAME)
            }
            return Executors.newSingleThreadExecutor(threadFactory)
        }

    private fun getSurfaceProvider(
        threadNameConsumer: Consumer<String>? = null,
        resultListener: Consumer<SurfaceRequest.Result>? = null,
        frameAvailableListener: SurfaceTexture.OnFrameAvailableListener? = null,
    ): Preview.SurfaceProvider {
        return SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider(
            frameAvailableListener,
            { surfaceRequest ->
                previewResolution = surfaceRequest.resolution
                threadNameConsumer?.accept(Thread.currentThread().name)
            },
            resultListener,
        )
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
            if (frameSemaphore!!.availablePermits() > 0) {
                // Has frame, reset timer and frame count.
                zeroFrameTimer = 0
                frameSemaphore!!.drainPermits()
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

    private fun Semaphore.verifyFramesReceived(frameCount: Int, timeoutInSeconds: Long = 10) {
        assertThat(this.tryAcquire(frameCount, timeoutInSeconds, TimeUnit.SECONDS)).isTrue()
    }
}
