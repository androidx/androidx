/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.lifecycle

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Range
import android.util.Rational
import android.view.Surface
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.StreamSpecsCalculator.Companion.NO_OP_STREAM_SPECS_CALCULATOR
import androidx.camera.core.internal.utils.ImageUtil
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.ExtensionsUtil
import androidx.camera.testing.impl.GarbageCollectionUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeCameraFilter
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessor
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.concurrent.futures.await
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
@kotlin.OptIn(ExperimentalSessionConfig::class)
class ProcessCameraProviderTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName.contains(CameraPipeConfig::class.simpleName!!))

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val lifecycleOwner0 = FakeLifecycleOwner()
    private val lifecycleOwner1 = FakeLifecycleOwner()
    private lateinit var cameraSelector: CameraSelector

    private lateinit var provider: ProcessCameraProvider

    @Before
    fun setUp() {
        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()
    }

    @After
    fun tearDown(): Unit =
        runBlocking(Dispatchers.Main) {
            try {
                val provider = ProcessCameraProvider.getInstance(context).await()
                provider.shutdownAsync().await()
            } catch (e: IllegalStateException) {
                // ProcessCameraProvider may not be configured. Ignore.
            }
        }

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun canRetrieveCamera_withZeroUseCases() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val camera = provider.bindToLifecycle(lifecycleOwner0, cameraSelector)
            assertThat(camera).isNotNull()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindUseCase_isBound() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase = Preview.Builder().build()
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase)

            assertThat(provider.isBound(useCase)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindSessionConfig_isBound() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase = Preview.Builder().build()
            val sessionConfig = SessionConfig(useCases = listOf(useCase))
            assertThat(provider.isBound(sessionConfig)).isFalse()
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig)

            assertThat(provider.isBound(sessionConfig)).isTrue()
            assertThat(provider.isBound(useCase)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindSecondUseCaseToDifferentLifecycle_AllUseCasesBound() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()

            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase0)
            provider.bindToLifecycle(lifecycleOwner1, cameraSelector, useCase1)

            assertThat(useCase0.camera).isNotNull()
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(useCase1.camera).isNotNull()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindSecondSessionConfigToDifferentLifecycle_AllSessionConfigsBound() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()
            val sessionConfig0 = SessionConfig(useCases = listOf(useCase0))
            val sessionConfig1 = SessionConfig(useCases = listOf(useCase1))

            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig0)
            provider.bindToLifecycle(lifecycleOwner1, cameraSelector, sessionConfig1)

            assertThat(useCase0.camera).isNotNull()
            assertThat(provider.isBound(sessionConfig0)).isTrue()
            assertThat(useCase1.camera).isNotNull()
            assertThat(provider.isBound(sessionConfig1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun isNotBound_afterUnbindUseCase() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase = Preview.Builder().build()

            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase)

            provider.unbind(useCase)

            assertThat(provider.isBound(useCase)).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun isNotBound_afterUnbindSessionConfig() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase = Preview.Builder().build()
            val sessionConfig = SessionConfig(useCases = listOf(useCase))

            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig)

            provider.unbind(sessionConfig)

            assertThat(provider.isBound(sessionConfig)).isFalse()
            assertThat(provider.isBound(useCase)).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun canRebindSessionConfigWithDuplicatedPreview() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        provider = ProcessCameraProvider.getInstance(context).await()
        val preview = Preview.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        val previewSurfaceProvider = PreviewSurfaceProvider()
        val sessionConfig1 = SessionConfig(useCases = listOf(preview))
        val sessionConfig2 = SessionConfig(useCases = listOf(preview, imageAnalysis))
        lifecycleOwner0.startAndResume()

        withContext(Dispatchers.Main) {
            preview.surfaceProvider = previewSurfaceProvider
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig1)
        }
        previewSurfaceProvider.assertFramesReceivedAfterSurfaceRequested()

        val analyisLatch = CountDownLatch(1)
        withContext(Dispatchers.Main) {
            imageAnalysis.setAnalyzer(CameraXExecutors.directExecutor()) {
                analyisLatch.countDown()
                it.close()
            }
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig2)
        }
        previewSurfaceProvider.assertFramesReceivedAfterSurfaceRequested()
        assertThat(analyisLatch.await(5, TimeUnit.SECONDS)).isTrue()

        assertThat(provider.isBound(sessionConfig1)).isFalse()
        assertThat(provider.isBound(sessionConfig2)).isTrue()
        assertThat(provider.isBound(preview)).isTrue()
        assertThat(provider.isBound(imageAnalysis)).isTrue()
        assertThat(provider.isConcurrentCameraModeOn).isFalse()
    }

    @Test
    fun canRebindSessionConfigWithDifferentUseCases() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        provider = ProcessCameraProvider.getInstance(context).await()
        val preview1 = Preview.Builder().build()
        val imageAnalysis1 = ImageAnalysis.Builder().build()
        val previewSurfaceProvider1 = PreviewSurfaceProvider()
        val preview2 = Preview.Builder().build()
        val imageAnalysis2 = ImageAnalysis.Builder().build()
        val previewSurfaceProvider2 = PreviewSurfaceProvider()
        val sessionConfig1 = SessionConfig(useCases = listOf(preview1, imageAnalysis1))
        val sessionConfig2 = SessionConfig(useCases = listOf(preview2, imageAnalysis2))
        lifecycleOwner0.startAndResume()

        val analyisLatch1 = CountDownLatch(1)
        withContext(Dispatchers.Main) {
            preview1.surfaceProvider = previewSurfaceProvider1
            imageAnalysis1.setAnalyzer(CameraXExecutors.directExecutor()) {
                analyisLatch1.countDown()
                it.close()
            }
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig1)
        }
        previewSurfaceProvider1.assertFramesReceivedAfterSurfaceRequested()
        assertThat(analyisLatch1.await(5, TimeUnit.SECONDS)).isTrue()

        val analyisLatch2 = CountDownLatch(1)
        withContext(Dispatchers.Main) {
            preview2.surfaceProvider = previewSurfaceProvider2
            imageAnalysis2.setAnalyzer(CameraXExecutors.directExecutor()) {
                analyisLatch2.countDown()
                it.close()
            }
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig2)
        }
        previewSurfaceProvider2.assertFramesReceivedAfterSurfaceRequested()
        assertThat(analyisLatch2.await(5, TimeUnit.SECONDS)).isTrue()

        assertThat(provider.isBound(sessionConfig1)).isFalse()
        assertThat(provider.isBound(sessionConfig2)).isTrue()
        assertThat(provider.isBound(preview1)).isFalse()
        assertThat(provider.isBound(imageAnalysis1)).isFalse()
        assertThat(provider.isBound(preview2)).isTrue()
        assertThat(provider.isBound(imageAnalysis2)).isTrue()
        assertThat(provider.isConcurrentCameraModeOn).isFalse()
    }

    @Test
    fun canRebindSessionConfig_sameLifecycleOwnerWithDifferentCamera() = runBlocking {
        val cameraSelectors = CameraUtil.getAvailableCameraSelectors()
        assumeTrue("No enough cameras to test.", cameraSelectors.size >= 2)

        ProcessCameraProvider.configureInstance(cameraConfig)
        provider = ProcessCameraProvider.getInstance(context).await()
        val preview1 = Preview.Builder().build()
        val imageAnalysis1 = ImageAnalysis.Builder().build()
        val previewSurfaceProvider1 = PreviewSurfaceProvider()
        val preview2 = Preview.Builder().build()
        val imageAnalysis2 = ImageAnalysis.Builder().build()
        val previewSurfaceProvider2 = PreviewSurfaceProvider()
        val sessionConfig1 = SessionConfig(useCases = listOf(preview1, imageAnalysis1))
        val sessionConfig2 = SessionConfig(useCases = listOf(preview2, imageAnalysis2))
        lifecycleOwner0.startAndResume()

        val analyisLatch1 = CountDownLatch(1)
        withContext(Dispatchers.Main) {
            preview1.surfaceProvider = previewSurfaceProvider1
            imageAnalysis1.setAnalyzer(CameraXExecutors.directExecutor()) {
                analyisLatch1.countDown()
                it.close()
            }
            provider.bindToLifecycle(lifecycleOwner0, cameraSelectors[0], sessionConfig1)
        }
        previewSurfaceProvider1.assertFramesReceivedAfterSurfaceRequested()
        assertThat(analyisLatch1.await(5, TimeUnit.SECONDS)).isTrue()

        val analyisLatch2 = CountDownLatch(1)
        withContext(Dispatchers.Main) {
            preview2.surfaceProvider = previewSurfaceProvider2
            imageAnalysis2.setAnalyzer(CameraXExecutors.directExecutor()) {
                analyisLatch2.countDown()
                it.close()
            }
            provider.bindToLifecycle(lifecycleOwner0, cameraSelectors[1], sessionConfig2)
        }
        previewSurfaceProvider2.assertFramesReceivedAfterSurfaceRequested()
        assertThat(analyisLatch2.await(5, TimeUnit.SECONDS)).isTrue()

        assertThat(provider.isBound(sessionConfig1)).isFalse()
        assertThat(provider.isBound(sessionConfig2)).isTrue()
        assertThat(provider.isBound(preview1)).isFalse()
        assertThat(provider.isBound(imageAnalysis1)).isFalse()
        assertThat(provider.isBound(preview2)).isTrue()
        assertThat(provider.isBound(imageAnalysis2)).isTrue()
        assertThat(provider.isConcurrentCameraModeOn).isFalse()
    }

    @Test
    fun canRebindSameSessionConfig_sameLifecycleOwnerWithDifferentCamera() = runBlocking {
        val cameraSelectors = CameraUtil.getAvailableCameraSelectors()
        assumeTrue("No enough cameras to test.", cameraSelectors.size >= 2)

        ProcessCameraProvider.configureInstance(cameraConfig)
        provider = ProcessCameraProvider.getInstance(context).await()
        val preview = Preview.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        val previewSurfaceProvider = PreviewSurfaceProvider()
        val sessionConfig = SessionConfig(useCases = listOf(preview, imageAnalysis))
        lifecycleOwner0.startAndResume()

        var analyisLatch1 = CountDownLatch(1)
        withContext(Dispatchers.Main) {
            preview.surfaceProvider = previewSurfaceProvider
            imageAnalysis.setAnalyzer(CameraXExecutors.directExecutor()) {
                analyisLatch1.countDown()
                it.close()
            }
            provider.bindToLifecycle(lifecycleOwner0, cameraSelectors[0], sessionConfig)
        }
        previewSurfaceProvider.assertFramesReceivedAfterSurfaceRequested()
        assertThat(analyisLatch1.await(5, TimeUnit.SECONDS)).isTrue()

        withContext(Dispatchers.Main) {
            provider.bindToLifecycle(lifecycleOwner0, cameraSelectors[1], sessionConfig)
        }
        previewSurfaceProvider.assertFramesReceivedAfterSurfaceRequested()
        analyisLatch1 = CountDownLatch(1)
        assertThat(analyisLatch1.await(5, TimeUnit.SECONDS)).isTrue()

        assertThat(provider.isBound(sessionConfig)).isTrue()
        assertThat(provider.isBound(imageAnalysis)).isTrue()
        assertThat(provider.isBound(preview)).isTrue()
        assertThat(provider.isConcurrentCameraModeOn).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    fun canRebindSessionConfigToSameLifecycleOwner_withExtensionsEnabled() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        val previewSurfaceProvider0 = PreviewSurfaceProvider()
        val previewSurfaceProvider1 = PreviewSurfaceProvider()

        provider = ProcessCameraProvider.getInstance(context).await()

        val useCase0 = Preview.Builder().build()
        val useCase1 = Preview.Builder().build()
        val sessionConfig0 = SessionConfig(useCases = listOf(useCase0))
        val sessionConfig1 = SessionConfig(useCases = listOf(useCase1))
        lifecycleOwner0.startAndResume()
        withContext(Dispatchers.Main) {
            useCase0.surfaceProvider = previewSurfaceProvider0
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig0)
        }
        previewSurfaceProvider0.assertFramesReceivedAfterSurfaceRequested()

        withContext(Dispatchers.Main) {
            useCase1.surfaceProvider = previewSurfaceProvider1
            val sessionProcessor = FakeSessionProcessor()
            val extensionsSelector =
                ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                    provider,
                    cameraSelector,
                    sessionProcessor,
                )
            provider.bindToLifecycle(lifecycleOwner0, extensionsSelector, sessionConfig1)
        }
        previewSurfaceProvider1.assertFramesReceivedAfterSurfaceRequested()
        assertThat(provider.isBound(sessionConfig0)).isFalse()
        assertThat(provider.isBound(useCase0)).isFalse()
        assertThat(provider.isBound(sessionConfig1)).isTrue()
        assertThat(provider.isBound(useCase1)).isTrue()
        assertThat(provider.isConcurrentCameraModeOn).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    fun canRebindSameSessionConfigToSameLifecycleOwner_withExtensionsEnabled() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        val previewSurfaceProvider = PreviewSurfaceProvider()
        provider = ProcessCameraProvider.getInstance(context).await()

        val useCase = Preview.Builder().build()
        val sessionConfig = SessionConfig(useCases = listOf(useCase))
        lifecycleOwner0.startAndResume()
        withContext(Dispatchers.Main) {
            useCase.surfaceProvider = previewSurfaceProvider
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig)
        }
        previewSurfaceProvider.assertFramesReceivedAfterSurfaceRequested()

        withContext(Dispatchers.Main) {
            val sessionProcessor = FakeSessionProcessor()
            val extensionsSelector =
                ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                    provider,
                    cameraSelector,
                    sessionProcessor,
                )
            provider.bindToLifecycle(lifecycleOwner0, extensionsSelector, sessionConfig)
        }
        previewSurfaceProvider.assertFramesReceivedAfterSurfaceRequested()
        assertThat(provider.isBound(sessionConfig)).isTrue()
        assertThat(provider.isBound(useCase)).isTrue()
        assertThat(provider.isConcurrentCameraModeOn).isFalse()
    }

    class PreviewSurfaceProvider : Preview.SurfaceProvider {
        var surfaceRequestLatch = CountDownLatch(1)
        var frameLatch: CountDownLatch? = null
        val surfaceProviderImpl =
            SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                frameLatch?.countDown()
            }

        override fun onSurfaceRequested(request: SurfaceRequest) {
            surfaceProviderImpl.onSurfaceRequested(request)
            frameLatch = CountDownLatch(1)
            surfaceRequestLatch.countDown()
        }

        fun assertFramesReceivedAfterSurfaceRequested() {
            assertThat(surfaceRequestLatch?.await(5, TimeUnit.SECONDS)).isTrue()
            assertThat(frameLatch?.await(5, TimeUnit.SECONDS)).isTrue()
            frameLatch = null
            surfaceRequestLatch = CountDownLatch(1)
        }
    }

    @Test
    fun isUseCaseNotBound_afterOnDestroyed() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase = Preview.Builder().build()
            lifecycleOwner0.startAndResume()
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase)

            lifecycleOwner0.pauseAndStop()
            lifecycleOwner0.destroy()

            assertThat(provider.isBound(useCase)).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun isSessionConfigNotBound_afterOnDestroyed() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase = Preview.Builder().build()
            lifecycleOwner0.startAndResume()
            val sessionConfig = SessionConfig(useCases = listOf(useCase))
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig)

            lifecycleOwner0.pauseAndStop()
            lifecycleOwner0.destroy()

            assertThat(provider.isBound(useCase)).isFalse()
            assertThat(provider.isBound(sessionConfig)).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleOwner_dereferencedAfterDestroyed() = runBlocking {
        // Arrange.
        ProcessCameraProvider.configureInstance(cameraConfig)
        provider = ProcessCameraProvider.awaitInstance(context)
        var lifecycleOwner: FakeLifecycleOwner? = FakeLifecycleOwner()
        val referenceQueue = ReferenceQueue<LifecycleOwner>()
        val phantomReference = PhantomReference(lifecycleOwner, referenceQueue)
        val frameLatch = CountDownLatch(1)
        val preview = Preview.Builder().build()

        instrumentation.runOnMainSync {
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                    frameLatch.countDown()
                }
            lifecycleOwner!!.startAndResume()
            val sessionConfig = SessionConfig(useCases = listOf(preview))
            provider.bindToLifecycle(lifecycleOwner!!, cameraSelector, sessionConfig)
        }
        // Wait for the preview to start running.
        assertThat(frameLatch.await(5, TimeUnit.SECONDS)).isTrue()

        instrumentation.runOnMainSync {
            lifecycleOwner!!.pauseAndStop()
            // Assert: trigger onDestroy, which should release the references to the lifecycleOwner.
            lifecycleOwner!!.destroy()
        }
        // Wait for the event to be processed.
        instrumentation.waitForIdleSync()

        try {
            // Nullify the strong reference to the lifecycleOwner.
            lifecycleOwner = null
            // Trigger the garbage collection.
            GarbageCollectionUtil.runFinalization()
            // Assert: the reference should become phantom reachable.
            assertThat(referenceQueue.poll()).isNotNull()
        } finally {
            phantomReference.clear()
        }
    }

    @Test
    fun unbindFirstUseCase_secondUseCaseStillBound() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()

            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase0, useCase1)

            provider.unbind(useCase0)

            assertThat(useCase0.camera).isNull()
            assertThat(provider.isBound(useCase0)).isFalse()
            assertThat(useCase1.camera).isNotNull()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun unbindAll_unbindsAllUseCasesFromCameras() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()

            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase0)
            provider.bindToLifecycle(lifecycleOwner1, cameraSelector, useCase1)

            provider.unbindAll()

            assertThat(useCase0.camera).isNull()
            assertThat(provider.isBound(useCase0)).isFalse()
            assertThat(useCase1.camera).isNull()
            assertThat(provider.isBound(useCase1)).isFalse()

            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun unbindAll_unbindSessionConfigFromCameras() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()
            val sessionConfig0 = SessionConfig(useCases = listOf(useCase0))
            val sessionConfig1 = SessionConfig(useCases = listOf(useCase1))

            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig0)
            provider.bindToLifecycle(lifecycleOwner1, cameraSelector, sessionConfig1)

            provider.unbindAll()

            assertThat(useCase0.camera).isNull()
            assertThat(provider.isBound(sessionConfig0)).isFalse()
            assertThat(provider.isBound(useCase0)).isFalse()
            assertThat(provider.isBound(sessionConfig1)).isFalse()
            assertThat(provider.isBound(useCase1)).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun unbindNonboundUseCases_noOps() =
        runBlocking(Dispatchers.Main) {
            ProcessCameraProvider.configureInstance(cameraConfig)
            provider = ProcessCameraProvider.awaitInstance(context)
            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()
            lifecycleOwner0.startAndResume()
            val camera =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase0)
                    as LifecycleCamera
            provider.unbind(useCase1) // noops

            assertThat(useCase0.camera).isNotNull()
            assertThat(useCase1.camera).isNull()
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isFalse()
            assertThat(camera.isActive).isTrue() // the unbind doesn't impact the camera.
        }

    @Test
    fun unbindNonboundSessionConfig_noOps() =
        runBlocking(Dispatchers.Main) {
            ProcessCameraProvider.configureInstance(cameraConfig)
            provider = ProcessCameraProvider.awaitInstance(context)
            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()
            val sessionConfig0 = SessionConfig(useCases = listOf(useCase0))
            val sessionConfig1 = SessionConfig(useCases = listOf(useCase1))
            lifecycleOwner0.startAndResume()
            val camera =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig0)
                    as LifecycleCamera
            provider.unbind(sessionConfig1) // noops

            assertThat(useCase0.camera).isNotNull()
            assertThat(useCase1.camera).isNull()
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isFalse()
            assertThat(provider.isBound(sessionConfig0)).isTrue()
            assertThat(provider.isBound(sessionConfig1)).isFalse()
            assertThat(camera.isActive).isTrue() // the unbind doesn't impact the camera.
        }

    @Test
    fun bindMultipleUseCases() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()

            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase0, useCase1)

            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindUseCasesAfterBindSessionConfig_throwsExceptions() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()
            val sessionConfig = SessionConfig(useCases = listOf(useCase0))
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig)
            assertThrows<IllegalStateException> {
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase1)
            }
            assertThat(provider.isBound(sessionConfig)).isTrue()
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindSessionConfigAfterBindUseCases_throwsExceptions() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()
            val sessionConfig = SessionConfig(useCases = listOf(useCase1))
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase0)
            assertThrows<IllegalStateException> {
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig)
            }
            assertThat(provider.isBound(sessionConfig)).isFalse()
            assertThat(provider.isBound(useCase1)).isFalse()
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bind_createsDifferentLifecycleCameras_forDifferentLifecycles() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().build()
            val camera0 = provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase0)

            val useCase1 = Preview.Builder().build()
            val camera1 = provider.bindToLifecycle(lifecycleOwner1, cameraSelector, useCase1)

            assertThat(camera0).isNotEqualTo(camera1)
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindSessionConfig_createsDifferentLifecycleCameras_forDifferentLifecycles() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().build()
            val sessionConfig0 = SessionConfig(useCases = listOf(useCase0))
            val camera0 = provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig0)

            val useCase1 = Preview.Builder().build()
            val sessionConfig1 = SessionConfig(useCases = listOf(useCase1))
            val camera1 = provider.bindToLifecycle(lifecycleOwner1, cameraSelector, sessionConfig1)

            assertThat(camera0).isNotEqualTo(camera1)
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isBound(sessionConfig0)).isTrue()
            assertThat(provider.isBound(sessionConfig1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bind_returnTheSameCameraForSameSelectorAndLifecycleOwner() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()

            val camera0 = provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase0)
            val camera1 = provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase1)

            assertThat(camera0).isSameInstanceAs(camera1)
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindUseCases_withDifferentLensFacingButSameLifecycleOwner_throwExceptions() = runBlocking {
        val cameraSelectors = CameraUtil.getAvailableCameraSelectors()
        assumeTrue("No enough cameras to test.", cameraSelectors.size >= 2)

        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()

            provider.bindToLifecycle(lifecycleOwner0, cameraSelectors[0], useCase0)

            assertThrows<IllegalArgumentException> {
                provider.bindToLifecycle(lifecycleOwner0, cameraSelectors[1], useCase1)
            }
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindUseCases_withDifferentLensFacingAndLifecycle() = runBlocking {
        val cameraSelectors = CameraUtil.getAvailableCameraSelectors()
        assumeTrue("No enough cameras to test.", cameraSelectors.size >= 2)

        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()

            val camera0 = provider.bindToLifecycle(lifecycleOwner0, cameraSelectors[0], useCase0)

            val camera1 = provider.bindToLifecycle(lifecycleOwner1, cameraSelectors[1], useCase1)

            assertThat(camera0).isNotEqualTo(camera1)
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindSessionConfig_withDifferentLensFacingAndLifecycle() = runBlocking {
        val cameraSelectors = CameraUtil.getAvailableCameraSelectors()
        assumeTrue("No enough cameras to test.", cameraSelectors.size >= 2)

        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().build()
            val useCase1 = Preview.Builder().build()
            val sessionConfig0 = SessionConfig(useCases = listOf(useCase0))
            val sessionConfig1 = SessionConfig(useCases = listOf(useCase1))

            val camera0 =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelectors[0], sessionConfig0)

            val camera1 =
                provider.bindToLifecycle(lifecycleOwner1, cameraSelectors[1], sessionConfig1)

            assertThat(camera0).isNotEqualTo(camera1)
            assertThat(provider.isBound(sessionConfig0)).isTrue()
            assertThat(provider.isBound(sessionConfig1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindSameUseCase_toDifferentLifecycle_throwsExceptions(): Unit = runBlocking {
        val cameraSelectors = CameraUtil.getAvailableCameraSelectors()
        assumeTrue("No enough cameras to test.", cameraSelectors.size >= 2)

        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase = Preview.Builder().build()
            provider.bindToLifecycle(lifecycleOwner0, cameraSelectors[0], useCase)

            assertThrows<IllegalStateException> {
                provider.bindToLifecycle(lifecycleOwner1, cameraSelectors[1], useCase)
            }
        }
    }

    @Test
    fun bindSessionConfigsWithSameUseCase_toDifferentLifecycle_throwsExceptions(): Unit =
        runBlocking {
            val cameraSelectors = CameraUtil.getAvailableCameraSelectors()
            assumeTrue("No enough cameras to test.", cameraSelectors.size >= 2)

            ProcessCameraProvider.configureInstance(cameraConfig)

            withContext(Dispatchers.Main) {
                provider = ProcessCameraProvider.getInstance(context).await()

                val useCase = Preview.Builder().build()
                val sessionConfig0 = SessionConfig(useCases = listOf(useCase))
                val sessionConfig1 = SessionConfig(useCases = listOf(useCase))
                provider.bindToLifecycle(lifecycleOwner0, cameraSelectors[0], sessionConfig0)

                assertThrows<IllegalStateException> {
                    provider.bindToLifecycle(lifecycleOwner1, cameraSelectors[1], sessionConfig1)
                }
            }
        }

    @Test
    fun bindUseCaseGroupWithEffect_effectIsSetOnUseCase() = runBlocking {
        // Arrange.
        ProcessCameraProvider.configureInstance(cameraConfig)
        val surfaceProcessor = FakeSurfaceProcessor(mainThreadExecutor())
        val effect = FakeSurfaceEffect(mainThreadExecutor(), surfaceProcessor)
        val preview = Preview.Builder().build()
        val useCaseGroup = UseCaseGroup.Builder().addUseCase(preview).addEffect(effect).build()

        withContext(Dispatchers.Main) {
            // Act.
            provider = ProcessCameraProvider.getInstance(context).await()
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCaseGroup)

            // Assert.
            assertThat(preview.effect).isEqualTo(effect)
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindSessionConfigWithEffect_effectIsSetOnUseCase() = runBlocking {
        // Arrange.
        ProcessCameraProvider.configureInstance(cameraConfig)
        val surfaceProcessor = FakeSurfaceProcessor(mainThreadExecutor())
        val effect = FakeSurfaceEffect(mainThreadExecutor(), surfaceProcessor)
        val preview = Preview.Builder().build()
        val sessionConfig = SessionConfig(useCases = listOf(preview), effects = listOf(effect))

        withContext(Dispatchers.Main) {
            // Act.
            provider = ProcessCameraProvider.getInstance(context).await()
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig)

            // Assert.
            assertThat(preview.effect).isEqualTo(effect)
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindSessionConfig_withSupportedFrameRate(): Unit = runBlocking {
        // Arrange.
        ProcessCameraProvider.configureInstance(cameraConfig)
        val provider = ProcessCameraProvider.getInstance(context).await()
        val preview = Preview.Builder().build()
        val cameraInfo = provider.getCameraInfo(cameraSelector)
        val availableFrameRateRanges =
            cameraInfo.getSupportedFrameRateRanges(SessionConfig(useCases = listOf(preview)))
        val expectedFrameRateRange =
            availableFrameRateRanges.firstOrNull()
                ?: throw AssumptionViolatedException("No supported frame rate")
        val sessionConfig =
            SessionConfig(useCases = listOf(preview), frameRateRange = expectedFrameRateRange)

        withContext(Dispatchers.Main) {
            // Act.
            provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig)
        }

        assertThat(preview.attachedStreamSpec!!.expectedFrameRateRange)
            .isEqualTo(expectedFrameRateRange)
    }

    @Test
    fun bindSessionConfig_withUnsupportedFrameRate_throwException(): Unit = runBlocking {
        // Arrange.
        ProcessCameraProvider.configureInstance(cameraConfig)
        val provider = ProcessCameraProvider.getInstance(context).await()
        val preview = Preview.Builder().build()
        val sessionConfig =
            SessionConfig(useCases = listOf(preview), frameRateRange = Range(1, 100))

        withContext(Dispatchers.Main) {
            // Act & Assert.
            assertThrows(IllegalArgumentException::class.java) {
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig)
            }
        }
    }

    @Test
    fun bindUseCaseGroupWithViewPort_viewPortUpdated() =
        runBlocking(Dispatchers.Main) {
            ProcessCameraProvider.configureInstance(cameraConfig)
            provider = ProcessCameraProvider.awaitInstance(context)
            val preview = Preview.Builder().build()
            val imageCapture = ImageCapture.Builder().build()
            val imageAnalysis = ImageAnalysis.Builder().build()
            val videoCapture = VideoCapture.Builder(Recorder.Builder().build()).build()
            val aspectRatio = Rational(2, 1)
            val viewPort = ViewPort.Builder(aspectRatio, Surface.ROTATION_0).build()

            // Act.
            provider.bindToLifecycle(
                FakeLifecycleOwner(),
                cameraSelector,
                UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(imageCapture)
                    .addUseCase(imageAnalysis)
                    .addUseCase(videoCapture)
                    .build(),
            )

            // Assert: The aspect ratio of the use cases should be close to the aspect ratio of the
            // view port set to the UseCaseGroup.
            val aspectRatioThreshold = 0.01
            assertThat(preview.viewPortCropRect!!.aspectRatio().toDouble())
                .isWithin(aspectRatioThreshold)
                .of(preview.getExpectedAspectRatio(aspectRatio))
            assertThat(imageCapture.viewPortCropRect!!.aspectRatio().toDouble())
                .isWithin(aspectRatioThreshold)
                .of(imageCapture.getExpectedAspectRatio(aspectRatio))
            assertThat(imageAnalysis.viewPortCropRect!!.aspectRatio().toDouble())
                .isWithin(aspectRatioThreshold)
                .of(imageAnalysis.getExpectedAspectRatio(aspectRatio))
            assertThat(videoCapture.viewPortCropRect!!.aspectRatio().toDouble())
                .isWithin(aspectRatioThreshold)
                .of(videoCapture.getExpectedAspectRatio(aspectRatio))
        }

    @Test
    fun bindSessionConfigWithViewPort_viewPortUpdated() =
        runBlocking(Dispatchers.Main) {
            // Arrange.
            ProcessCameraProvider.configureInstance(cameraConfig)
            provider = ProcessCameraProvider.awaitInstance(context)
            val preview = Preview.Builder().build()
            val imageCapture = ImageCapture.Builder().build()
            val imageAnalysis = ImageAnalysis.Builder().build()
            val videoCapture = VideoCapture.Builder(Recorder.Builder().build()).build()
            val aspectRatio = Rational(2, 1)
            val viewPort = ViewPort.Builder(aspectRatio, Surface.ROTATION_0).build()

            // Act.
            provider.bindToLifecycle(
                FakeLifecycleOwner(),
                cameraSelector,
                SessionConfig(
                    useCases = listOf(preview, imageCapture, imageAnalysis, videoCapture),
                    viewPort = viewPort,
                ),
            )

            // Assert: The aspect ratio of the use cases should be close to the aspect ratio of the
            // view port set to the UseCaseGroup.
            val aspectRatioThreshold = 0.01
            assertThat(preview.viewPortCropRect!!.aspectRatio().toDouble())
                .isWithin(aspectRatioThreshold)
                .of(preview.getExpectedAspectRatio(aspectRatio))
            assertThat(imageCapture.viewPortCropRect!!.aspectRatio().toDouble())
                .isWithin(aspectRatioThreshold)
                .of(imageCapture.getExpectedAspectRatio(aspectRatio))
            assertThat(imageAnalysis.viewPortCropRect!!.aspectRatio().toDouble())
                .isWithin(aspectRatioThreshold)
                .of(imageAnalysis.getExpectedAspectRatio(aspectRatio))
            assertThat(videoCapture.viewPortCropRect!!.aspectRatio().toDouble())
                .isWithin(aspectRatioThreshold)
                .of(videoCapture.getExpectedAspectRatio(aspectRatio))
        }

    @Test
    fun bindUseCaseGroupAndRebindWithoutViewPortAndEffect_viewPortEffectIsReset() =
        runBlocking(Dispatchers.Main) {
            // Arrange.
            ProcessCameraProvider.configureInstance(cameraConfig)
            provider = ProcessCameraProvider.awaitInstance(context)
            val preview = Preview.Builder().build()
            val imageCapture = ImageCapture.Builder().build()
            val aspectRatio = Rational(2, 1)
            val viewPort = ViewPort.Builder(aspectRatio, Surface.ROTATION_0).build()
            val surfaceProcessor = FakeSurfaceProcessor(mainThreadExecutor())
            val effect = FakeSurfaceEffect(mainThreadExecutor(), surfaceProcessor)

            // Put only Preview / ImageCapture to avoid stream sharing
            val useCaseGroup =
                UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(imageCapture)
                    .setViewPort(viewPort)
                    .addEffect(effect)
                    .build()

            // Act.
            provider.bindToLifecycle(FakeLifecycleOwner(), cameraSelector, useCaseGroup)

            provider.unbindAll()

            provider.bindToLifecycle(FakeLifecycleOwner(), cameraSelector, preview, imageCapture)

            // Assert: The aspect ratio and the effect of the use cases should be reset to null
            assertThat(preview.viewPortCropRect).isNull()
            assertThat(imageCapture.viewPortCropRect).isNull()
        }

    @Test
    fun bindSessionConfigAndRebindWithoutViewPortAndEffect_viewPortEffectIsReset() =
        runBlocking(Dispatchers.Main) {
            // Arrange.
            ProcessCameraProvider.configureInstance(cameraConfig)
            provider = ProcessCameraProvider.awaitInstance(context)
            val preview = Preview.Builder().build()
            val imageCapture = ImageCapture.Builder().build()
            val aspectRatio = Rational(2, 1)
            val viewPort = ViewPort.Builder(aspectRatio, Surface.ROTATION_0).build()
            val surfaceProcessor = FakeSurfaceProcessor(mainThreadExecutor())
            val effect = FakeSurfaceEffect(mainThreadExecutor(), surfaceProcessor)
            val fakeLifecycleOwner = FakeLifecycleOwner().also { it.startAndResume() }
            val sessionConfig =
                SessionConfig(
                    // Put only Preview / ImageCapture to avoid stream sharing
                    useCases = listOf(preview, imageCapture),
                    viewPort = viewPort,
                    effects = listOf(effect),
                )

            // Act.
            provider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, sessionConfig)

            provider.unbind(sessionConfig)

            provider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, preview, imageCapture)

            // Assert: The aspect ratio and the effect of the use cases should be reset to null
            assertThat(preview.viewPortCropRect).isNull()
            assertThat(preview.effect).isNull()
            assertThat(imageCapture.viewPortCropRect).isNull()
            assertThat(imageCapture.effect).isNull()
        }

    private fun UseCase.getExpectedAspectRatio(aspectRatio: Rational): Double {
        val camera = this.camera!!
        val isStreamSharingOn = !camera.hasTransform
        // If stream sharing is on, the expected aspect ratio doesn't have to be adjusted with
        // sensor rotation.
        val rotation = if (isStreamSharingOn) 0 else camera.cameraInfo.sensorRotationDegrees
        return ImageUtil.getRotatedAspectRatio(rotation, aspectRatio).toDouble()
    }

    @Test
    fun lifecycleCameraIsNotActive_withZeroUseCases_bindBeforeLifecycleStarted() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector) as LifecycleCamera
            lifecycleOwner0.startAndResume()
            assertThat(camera.isActive).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsNotActive_withZeroUseCases_bindAfterLifecycleStarted() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            lifecycleOwner0.startAndResume()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector) as LifecycleCamera
            assertThat(camera.isActive).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsActive_withUseCases_bindBeforeLifecycleStarted() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase = Preview.Builder().build()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase)
                    as LifecycleCamera
            lifecycleOwner0.startAndResume()
            assertThat(camera.isActive).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsActive_withSessionConfig_bindBeforeLifecycleStarted() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val sessionConfig = SessionConfig(useCases = listOf(Preview.Builder().build()))
            val camera: LifecycleCamera =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig)
                    as LifecycleCamera
            lifecycleOwner0.startAndResume()
            assertThat(camera.isActive).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsActive_withUseCases_bindAfterLifecycleStarted() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase = Preview.Builder().build()
            lifecycleOwner0.startAndResume()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase)
                    as LifecycleCamera
            assertThat(camera.isActive).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsActive_withSessionConfig_bindAfterLifecycleStarted() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val sessionConfig = SessionConfig(useCases = listOf(Preview.Builder().build()))
            lifecycleOwner0.startAndResume()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig)
                    as LifecycleCamera
            assertThat(camera.isActive).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsNotActive_bindAfterLifecycleDestroyed() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase = Preview.Builder().build()
            lifecycleOwner0.destroy()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(
                    lifecycleOwner0,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    useCase,
                ) as LifecycleCamera
            assertThat(camera.isActive).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsNotActive_bindSessionConfigAfterLifecycleDestroyed() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val sessionConfig = SessionConfig(useCases = listOf(Preview.Builder().build()))
            lifecycleOwner0.destroy()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(
                    lifecycleOwner0,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    sessionConfig,
                ) as LifecycleCamera
            assertThat(camera.isActive).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsNotActive_unbindUseCase() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase = Preview.Builder().build()
            lifecycleOwner0.startAndResume()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase)
                    as LifecycleCamera
            assertThat(camera.isActive).isTrue()
            provider.unbind(useCase)
            assertThat(camera.isActive).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsNotActive_unbindSessionConfig() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val sessionConfig = SessionConfig(useCases = listOf(Preview.Builder().build()))
            lifecycleOwner0.startAndResume()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig)
                    as LifecycleCamera
            assertThat(camera.isActive).isTrue()
            provider.unbind(sessionConfig)
            assertThat(camera.isActive).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsNotActive_unbindAll() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase = Preview.Builder().build()
            lifecycleOwner0.startAndResume()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector, useCase)
                    as LifecycleCamera
            assertThat(camera.isActive).isTrue()
            provider.unbindAll()
            assertThat(camera.isActive).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsNotActive_unbindAllWithSessionConfig() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val sessionConfig = SessionConfig(useCases = listOf(Preview.Builder().build()))
            lifecycleOwner0.startAndResume()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelector, sessionConfig)
                    as LifecycleCamera
            assertThat(camera.isActive).isTrue()
            provider.unbindAll()
            assertThat(camera.isActive).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun getAvailableCameraInfos_usesAllCameras() = runBlocking {
        ProcessCameraProvider.configureInstance(cameraConfig)
        provider = ProcessCameraProvider.getInstance(context).await()
        val cameraCount =
            cameraConfig
                .getCameraFactoryProvider(null)!!
                .newInstance(
                    context,
                    CameraThreadConfig.create(
                        mainThreadExecutor(),
                        Handler(Looper.getMainLooper()),
                    ),
                    null,
                    -1L,
                    NO_OP_STREAM_SPECS_CALCULATOR,
                )
                .availableCameraIds
                .size

        assertThat(provider.availableCameraInfos.size).isEqualTo(cameraCount)
    }

    @Test
    fun getCameraInfo_sameCameraInfoWithBindToLifecycle_afterBinding() = runBlocking {
        // Arrange.
        ProcessCameraProvider.configureInstance(cameraConfig)

        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            // Act: getting the camera info after bindToLifecycle.
            val camera = provider.bindToLifecycle(lifecycleOwner0, cameraSelector)
            val cameraInfoInternal1: CameraInfoInternal =
                provider.getCameraInfo(cameraSelector) as CameraInfoInternal
            val cameraInfoInternal2: CameraInfoInternal = camera.cameraInfo as CameraInfoInternal

            // Assert.
            assertThat(cameraInfoInternal1).isSameInstanceAs(cameraInfoInternal2)
        }
    }

    @Test
    fun getCameraInfo_sameCameraInfoWithBindToLifecycle_beforeBinding() = runBlocking {
        // Arrange.
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()

            // Act: getting the camera info before bindToLifecycle.
            val cameraInfoInternal1: CameraInfoInternal =
                provider.getCameraInfo(cameraSelector) as CameraInfoInternal
            val camera = provider.bindToLifecycle(lifecycleOwner0, cameraSelector)
            val cameraInfoInternal2: CameraInfoInternal = camera.cameraInfo as CameraInfoInternal

            // Assert.
            assertThat(cameraInfoInternal1).isSameInstanceAs(cameraInfoInternal2)
        }
    }

    @Test
    fun getCameraInfo_containExtendedCameraConfig() = runBlocking {
        // Arrange.
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val id = Identifier.create("FakeId")
            val cameraConfig = FakeCameraConfig(postviewSupported = true)
            ExtendedCameraConfigProviderStore.addConfig(id) { _, _ -> cameraConfig }
            val cameraSelector =
                CameraSelector.Builder().addCameraFilter(FakeCameraFilter(id)).build()

            // Act.
            val adapterCameraInfo = provider.getCameraInfo(cameraSelector) as AdapterCameraInfo

            // Assert.
            assertThat(adapterCameraInfo.isPostviewSupported).isTrue()
        }
    }

    @Test
    fun getCameraInfo_exceptionWhenCameraSelectorInvalid(): Unit = runBlocking {
        // Arrange.
        ProcessCameraProvider.configureInstance(cameraConfig)
        withContext(Dispatchers.Main) {
            provider = ProcessCameraProvider.getInstance(context).await()
            // Intentionally create a camera selector that doesn't result in a camera.
            val cameraSelector =
                CameraSelector.Builder().addCameraFilter { ArrayList<CameraInfo>() }.build()

            // Act & Assert.
            assertThrows(IllegalArgumentException::class.java) {
                provider.getCameraInfo(cameraSelector)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    fun bindWithExtensions_doesNotImpactPreviousCamera(): Unit =
        runBlocking(Dispatchers.Main) {
            // 1. Arrange.
            val cameraSelectorWithExtensions =
                getCameraSelectorWithLimitedCapabilities(
                    cameraSelector,
                    emptySet(), // All capabilities are not supported.
                )
            ProcessCameraProvider.configureInstance(cameraConfig)
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase = Preview.Builder().build()

            // 2. Act: bind with and then without Extensions.
            // bind with regular cameraSelector to get the regular camera (with empty use cases)
            val camera = provider.bindToLifecycle(lifecycleOwner0, cameraSelector)
            // bind with extensions cameraSelector to get the restricted version of camera.
            val cameraWithExtensions =
                provider.bindToLifecycle(lifecycleOwner0, cameraSelectorWithExtensions, useCase)

            // 3. Assert: ensure we can different instances of Camera and one does not affect the
            // other.
            assertThat(camera).isNotSameInstanceAs(cameraWithExtensions)

            // the Extensions CameraControl does not support the zoom.
            assertThrows<IllegalStateException> {
                cameraWithExtensions.cameraControl.setZoomRatio(1.0f).await()
            }

            // only the Extensions CameraInfo does not support the zoom.
            assertThat(camera.cameraInfo.zoomState.value!!.maxZoomRatio).isGreaterThan(1.0f)
            assertThat(cameraWithExtensions.cameraInfo.zoomState.value!!.maxZoomRatio)
                .isEqualTo(1.0f)
        }

    @RequiresApi(23)
    private fun getCameraSelectorWithLimitedCapabilities(
        cameraSelector: CameraSelector,
        supportedCapabilities: Set<Int>,
    ): CameraSelector {
        val identifier = Identifier.create("idStr")
        val sessionProcessor =
            FakeSessionProcessor(supportedCameraOperations = supportedCapabilities)
        ExtendedCameraConfigProviderStore.addConfig(identifier) { _, _ ->
            object : CameraConfig {
                override fun getConfig(): Config {
                    return MutableOptionsBundle.create()
                }

                override fun getCompatibilityId(): Identifier {
                    return identifier
                }

                override fun getSessionProcessor(valueIfMissing: SessionProcessor?) =
                    sessionProcessor

                override fun getSessionProcessor() = sessionProcessor
            }
        }

        val builder = CameraSelector.Builder.fromSelector(cameraSelector)
        builder.addCameraFilter(
            object : CameraFilter {
                override fun filter(cameraInfos: MutableList<CameraInfo>): MutableList<CameraInfo> {
                    val newCameraInfos = mutableListOf<CameraInfo>()
                    newCameraInfos.addAll(cameraInfos)
                    return newCameraInfos
                }

                override fun getIdentifier(): Identifier {
                    return identifier
                }
            }
        )

        return builder.build()
    }

    private fun Rect.aspectRatio(rotationDegrees: Int = 0): Rational {
        return if (rotationDegrees % 180 != 0) Rational(height(), width())
        else Rational(width(), height())
    }
}
