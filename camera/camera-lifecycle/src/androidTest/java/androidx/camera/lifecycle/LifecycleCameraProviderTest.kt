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

package androidx.camera.lifecycle

import android.content.Context
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.StreamSpecsCalculatorImpl
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class LifecycleCameraProviderTest(
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
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
        }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val lifecycleOwner1 = FakeLifecycleOwner()
    private val lifecycleOwner2 = FakeLifecycleOwner()
    private val preview =
        Preview.Builder().build().apply {
            instrumentation.runOnMainSync {
                surfaceProvider =
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                        frameAvailableSemaphore.release()
                    }
            }
        }
    private val imageAnalysis =
        ImageAnalysis.Builder().build().apply {
            setAnalyzer(Dispatchers.Default.asExecutor()) { it.close() }
        }
    private var frameAvailableSemaphore = Semaphore(0)
    private lateinit var cameraSelector: CameraSelector

    private lateinit var provider1: LifecycleCameraProvider
    private lateinit var provider2: LifecycleCameraProvider

    @Before
    fun setUp() {
        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()
        runBlocking(MainScope().coroutineContext) {
            if (implName == Camera2Config::class.simpleName) {
                provider1 =
                    LifecycleCameraProvider.createInstance(context, Camera2Config.defaultConfig())
                provider2 =
                    LifecycleCameraProvider.createInstance(context, Camera2Config.defaultConfig())
            } else if (implName == CameraPipeConfig::class.simpleName) {
                provider1 =
                    LifecycleCameraProvider.createInstance(
                        context,
                        CameraPipeConfig.defaultConfig(),
                    )
                provider2 =
                    LifecycleCameraProvider.createInstance(
                        context,
                        CameraPipeConfig.defaultConfig(),
                    )
            }
        }
        lifecycleOwner1.startAndResume()
        lifecycleOwner2.startAndResume()
    }

    @After
    fun tearDown() {
        if (::provider1.isInitialized) {
            runBlocking(MainScope().coroutineContext) {
                (provider1 as LifecycleCameraProviderImpl).shutdownAsync()[10, TimeUnit.SECONDS]
            }
        }
        if (::provider2.isInitialized) {
            runBlocking(MainScope().coroutineContext) {
                (provider2 as LifecycleCameraProviderImpl).shutdownAsync()[10, TimeUnit.SECONDS]
            }
        }
    }

    @Test
    fun bindUseCasesWithDifferentInstance_checkCameraActiveness() {
        // Arrange.
        lateinit var camera1: LifecycleCamera
        lateinit var camera2: LifecycleCamera

        instrumentation.runOnMainSync {
            // Act.
            camera1 =
                provider1.bindToLifecycle(lifecycleOwner1, cameraSelector, preview)
                    as LifecycleCamera
            camera2 =
                provider2.bindToLifecycle(lifecycleOwner2, cameraSelector, imageAnalysis)
                    as LifecycleCamera
        }
        instrumentation.waitForIdleSync()

        // Assert: The first camera should be inactive while the second camera being active.
        assertThat(camera1.isActive).isFalse()
        assertThat(camera2.isActive).isTrue()

        instrumentation.runOnMainSync {
            // Act: Bind to the first camera provider again.
            camera1 =
                provider1.bindToLifecycle(lifecycleOwner1, cameraSelector, preview)
                    as LifecycleCamera
        }

        // Assert: The first camera should become active again while the second camera being
        // inactive.
        assertThat(camera1.isActive).isTrue()
        assertThat(camera2.isActive).isFalse()
    }

    @Test
    fun bindUseCasesWithDifferentInstance() {
        instrumentation.runOnMainSync {
            // Arrange.
            provider1.bindToLifecycle(lifecycleOwner1, cameraSelector, imageAnalysis)
            // Act.
            provider2.bindToLifecycle(lifecycleOwner2, cameraSelector, preview)
        }
        instrumentation.waitForIdleSync()

        // Assert: Can receive frames after bind to the second camera provider.
        assertThat(frameAvailableSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun bindUseCasesWithDifferentInstance_withShutdown() {
        instrumentation.runOnMainSync {
            // Arrange.
            provider1.bindToLifecycle(lifecycleOwner1, cameraSelector, imageAnalysis)
            provider2.bindToLifecycle(lifecycleOwner2, cameraSelector, preview)

            // Act: Shutting down the first provider, which shouldn't affect the second provider.
            (provider1 as LifecycleCameraProviderImpl).shutdownAsync()
        }
        instrumentation.waitForIdleSync()
        frameAvailableSemaphore.drainPermits()

        // Assert: Can receive frames after the first provider is shut down.
        assertThat(frameAvailableSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun bindUseCasesWithDifferentInstance_cameraControlInactive() {
        // Arrange.
        lateinit var camera1: Camera

        // Act.
        instrumentation.runOnMainSync {
            camera1 = provider1.bindToLifecycle(lifecycleOwner1, cameraSelector, preview)
            provider2.bindToLifecycle(lifecycleOwner2, cameraSelector, imageAnalysis)
        }
        instrumentation.waitForIdleSync()

        // Assert: The use of the first camera control should return an exception because it's
        // inactive.
        assertThrows(ExecutionException::class.java) {
            camera1.cameraControl.enableTorch(true).get()
        }
    }

    @Test
    fun bindUseCasesWithDifferentInstance_cameraInfoActive() {
        // Arrange.
        lateinit var camera1: Camera

        // Act.
        instrumentation.runOnMainSync {
            camera1 = provider1.bindToLifecycle(lifecycleOwner1, cameraSelector, preview)
            provider2.bindToLifecycle(lifecycleOwner2, cameraSelector, imageAnalysis)
        }
        instrumentation.waitForIdleSync()

        // Assert: The CameraInfo of the first camera should continue providing the information.
        assertThat(camera1.cameraInfo.lensFacing).isEqualTo(cameraSelector.lensFacing)
    }

    @Test
    fun shutdown_onlyRemoveNecessaryCamerasFromRepository() {
        // Arrange.
        val repository = LifecycleCameraRepository.getInstance()
        val fakeCamera =
            repository.createLifecycleCamera(
                FakeLifecycleOwner(),
                CameraUseCaseAdapter(
                    FakeCamera("2"),
                    FakeCameraCoordinator(),
                    StreamSpecsCalculatorImpl(
                        FakeUseCaseConfigFactory(),
                        FakeCameraDeviceSurfaceManager(),
                    ),
                    FakeUseCaseConfigFactory(),
                ),
            )

        // Act: Bind to a provider then shut it down.
        instrumentation.runOnMainSync {
            provider1.bindToLifecycle(lifecycleOwner1, cameraSelector, preview)
            (provider1 as LifecycleCameraProviderImpl).shutdownAsync()
        }
        instrumentation.waitForIdleSync()

        // Assert: The camera not bound by the provider should not be removed.
        assertThat(repository.lifecycleCameras).containsExactly(fakeCamera)
    }

    @Test
    fun bindWithoutUseCases_returnCameraCorrectly() =
        runBlocking(Dispatchers.Main) {
            val cameraInfo = provider1.getCameraInfo(cameraSelector)
            val camera = provider1.bindToLifecycle(lifecycleOwner1, cameraSelector)
            assertThat(camera.cameraInfo).isEqualTo(cameraInfo)
            assertThat(camera.cameraInfo.lensFacing).isEqualTo(cameraSelector.lensFacing)
        }
}
