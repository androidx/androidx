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

package androidx.camera.lifecycle

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.InitializationException
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.impl.CameraDeviceSurfaceManager
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraFactory.Provider
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeCameraFactory
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.internal.os.HandlerExecutor
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(ExperimentalCoroutinesApi::class)
class ProcessCameraProviderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val handler = Handler(Looper.getMainLooper()) // Same to the looper of TestScope
    private val handlerExecutor = HandlerExecutor(handler)
    private lateinit var shadowPackageManager: ShadowPackageManager
    private var repeatingJob: Deferred<Unit>? = null
    private lateinit var provider: ProcessCameraProvider
    private val lifecycleOwner0 = FakeLifecycleOwner()

    @Before
    fun setUp() {
        // This test asserts both the type of the exception thrown, and the type of the cause of the
        // exception thrown in many cases. The Kotlin stacktrace recovery feature is useful for
        // debugging, but it inserts exceptions into the `cause` chain and interferes with this
        // test.
        System.setProperty("kotlinx.coroutines.stacktrace.recovery", false.toString())
        shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA, true)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_FRONT, true)
    }

    @After
    fun tearDown() = runTest {
        repeatingJob?.cancel()
        try {
            ProcessCameraProvider.shutdown().await()
        } catch (_: IllegalStateException) {
            // ProcessCameraProvider may not be configured. Ignore.
        }
    }

    @Test
    fun processCameraProviderFail_retainCameraConfig() = runTest {
        // Arrange
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(
                    createCameraXConfig(
                        cameraFactory =
                            createFakeCameraFactory(frontCamera = false, backCamera = false)
                    )
                )
                .apply {
                    setCameraExecutor(handlerExecutor)
                    setSchedulerHandler(handler)
                }

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        ProcessCameraProvider.configureInstance(configBuilder.build())

        // Act
        assertThrows<InitializationException> { ProcessCameraProvider.getInstance(context).await() }

        // Assert
        // When retrying ProcessCameraProvider#getInstance, it should be able to try without calling
        // configureInstance again.
        assertThrows<InitializationException> { ProcessCameraProvider.getInstance(context).await() }
    }

    @Test
    @OptIn(ExperimentalSessionConfig::class)
    fun bindUseCasesOrSessionConfig_withNotExistedLensFacingCamera() = runTest {
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA, true)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_FRONT, false)

        val cameraFactoryProvider = Provider { _, _, _, _, _ ->
            val cameraFactory = FakeCameraFactory()
            cameraFactory.insertCamera(LENS_FACING_BACK, "0") {
                FakeCamera("0", null, FakeCameraInfoInternal("0", 0, LENS_FACING_BACK))
            }
            cameraFactory.cameraCoordinator = FakeCameraCoordinator()
            cameraFactory
        }

        val appConfigBuilder =
            CameraXConfig.Builder()
                .setCameraFactoryProvider(cameraFactoryProvider)
                .setDeviceSurfaceManagerProvider { _, _, _ -> FakeCameraDeviceSurfaceManager() }
                .setUseCaseConfigFactoryProvider { FakeUseCaseConfigFactory() }

        ProcessCameraProvider.configureInstance(appConfigBuilder.build())

        provider = ProcessCameraProvider.getInstance(context).await()

        val useCase = Preview.Builder().build()

        // The front camera is not defined, we should get the IllegalArgumentException when it
        // tries to get the camera.
        assertThrows<IllegalArgumentException> {
            provider.bindToLifecycle(lifecycleOwner0, CameraSelector.DEFAULT_FRONT_CAMERA, useCase)
        }

        val sessionConfig = SessionConfig(useCases = listOf(useCase))
        assertThrows<IllegalArgumentException> {
            provider.bindToLifecycle(
                lifecycleOwner0,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                sessionConfig,
            )
        }

        assertThat(provider.isConcurrentCameraModeOn).isFalse()
    }

    @Test
    fun getAvailableCameraInfos_usesFilteredCameras() = runBlocking {
        ProcessCameraProvider.configureInstance(
            FakeAppConfig.create(CameraSelector.DEFAULT_BACK_CAMERA)
        )
        provider = ProcessCameraProvider.getInstance(context).await()

        val cameraInfos = provider.availableCameraInfos
        assertThat(cameraInfos.size).isEqualTo(1)

        val cameraInfo = cameraInfos.first() as FakeCameraInfoInternal
        assertThat(cameraInfo.lensFacing).isEqualTo(LENS_FACING_BACK)
    }

    private fun createCameraXConfig(
        cameraFactory: CameraFactory = createFakeCameraFactory(),
        surfaceManager: CameraDeviceSurfaceManager? = FakeCameraDeviceSurfaceManager(),
        useCaseConfigFactory: UseCaseConfigFactory? = FakeUseCaseConfigFactory(),
    ): CameraXConfig {
        val cameraFactoryProvider = Provider { _, _, _, _, _ -> cameraFactory }
        return CameraXConfig.Builder()
            .setCameraFactoryProvider(cameraFactoryProvider)
            .apply {
                surfaceManager?.let {
                    setDeviceSurfaceManagerProvider { _: Context?, _: Any?, _: Set<String?>? -> it }
                }
                useCaseConfigFactory?.let { setUseCaseConfigFactoryProvider { _: Context? -> it } }
            }
            .build()
    }

    private fun createFakeCameraFactory(
        frontCamera: Boolean = false,
        backCamera: Boolean = false,
    ): CameraFactory =
        FakeCameraFactory(null).also { cameraFactory ->
            if (backCamera) {
                cameraFactory.insertCamera(CameraSelector.LENS_FACING_BACK, CAMERA_ID_0) {
                    FakeCamera(
                        CAMERA_ID_0,
                        null,
                        FakeCameraInfoInternal(CAMERA_ID_0, 0, CameraSelector.LENS_FACING_BACK),
                    )
                }
            }
            if (frontCamera) {
                cameraFactory.insertCamera(CameraSelector.LENS_FACING_FRONT, CAMERA_ID_1) {
                    FakeCamera(
                        CAMERA_ID_1,
                        null,
                        FakeCameraInfoInternal(CAMERA_ID_1, 0, CameraSelector.LENS_FACING_FRONT),
                    )
                }
            }
            cameraFactory.cameraCoordinator = FakeCameraCoordinator()
        }

    private fun TestScope.simulateSystemTimeIncrease() = async {
        val startTimeMs = SystemClock.elapsedRealtime()
        while (SystemClock.elapsedRealtime() - startTimeMs < 20000L) {
            shadowOf(handler.looper).idle()
            if (SystemClock.elapsedRealtime() < currentTime) {
                ShadowSystemClock.advanceBy(
                    currentTime - SystemClock.elapsedRealtime(),
                    TimeUnit.MILLISECONDS,
                )
            }
            delay(FAKE_INIT_PROCESS_TIME_MS)
        }
    }

    companion object {
        private const val CAMERA_ID_0 = "0"
        private const val CAMERA_ID_1 = "1"
        private const val FAKE_INIT_PROCESS_TIME_MS = 33L
    }
}
