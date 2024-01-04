/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.integration.extensions

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.utils.CameraIdExtensionModePair
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.StressTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class OpenCloseCaptureSessionStressTest(private val config: CameraIdExtensionModePair) {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var camera: Camera
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalysis: ImageAnalysis
    private var isImageAnalysisSupported = false
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private val cameraSessionMonitor = CameraSessionMonitor()

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        val (cameraId, extensionMode) = config
        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode
        )

        camera = withContext(Dispatchers.Main) {
            lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
            cameraProvider.bindToLifecycle(lifecycleOwner, extensionCameraSelector)
        }

        val previewBuilder = Preview.Builder()
        injectCameraSessionMonitor(previewBuilder, cameraSessionMonitor)
        preview = previewBuilder.build()
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
        }
        imageCapture = ImageCapture.Builder().build()
        imageAnalysis = ImageAnalysis.Builder().build()

        isImageAnalysisSupported =
            camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis)
    }

    private fun injectCameraSessionMonitor(
        previewBuilder: Preview.Builder,
        cameraMonitor: CameraSessionMonitor
    ) {
        Camera2Interop.Extender(previewBuilder)
            .setSessionStateCallback(object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraMonitor.onOpenedSession()
                }

                override fun onClosed(session: CameraCaptureSession) {
                    cameraMonitor.onClosedSession()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                }

                override fun onReady(session: CameraCaptureSession) {
                }

                override fun onActive(session: CameraCaptureSession) {
                }

                override fun onCaptureQueueEmpty(session: CameraCaptureSession) {
                }

                override fun onSurfacePrepared(session: CameraCaptureSession, surface: Surface) {
                }
            })
            .setDeviceStateCallback(object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                }
                // Some device doesn't invoke CameraCaptureSession onClosed callback thus
                // we need to invoke when camera is closed.
                override fun onClosed(device: CameraDevice) {
                    cameraMonitor.onClosedSession()
                }

                override fun onDisconnected(device: CameraDevice) {
                }

                override fun onError(device: CameraDevice, error: Int) {
                }
            })
    }
    @After
    fun cleanUp(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
            }
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    fun openCloseCaptureSessionStressTest_withPreviewImageCapture(): Unit = runBlocking {
        bindUseCase_unbindAll_toCheckCameraSession_repeatedly(preview, imageCapture)
    }

    @Test
    fun openCloseCaptureSessionStressTest_withPreviewImageCaptureImageAnalysis(): Unit =
        runBlocking {
            val imageAnalysis = ImageAnalysis.Builder().build()
            assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
            bindUseCase_unbindAll_toCheckCameraSession_repeatedly(
                preview,
                imageCapture,
                imageAnalysis
            )
        }

    /**
     * Repeatedly binds use cases, unbind all to check whether the capture session can be opened
     * and closed successfully by monitoring the camera session state.
     */
    private fun bindUseCase_unbindAll_toCheckCameraSession_repeatedly(
        vararg useCases: UseCase,
        repeatCount: Int = CameraXExtensionsTestUtil.getStressTestRepeatingCount()
    ): Unit = runBlocking {
        for (i in 1..repeatCount) {
            // Arrange: resets the camera session monitor
            cameraSessionMonitor.reset()

            withContext(Dispatchers.Main) {
                // Act: binds use cases
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    extensionCameraSelector,
                    *useCases
                )
            }

            // Assert: checks the camera session is opened.
            cameraSessionMonitor.awaitSessionOpenedAndAssert()

            // Act: unbinds all use cases
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
            }

            // Assert: checks the camera session is closed.
            cameraSessionMonitor.awaitSessionClosedAndAssert()
        }
    }

    companion object {
        @ClassRule
        @JvmField val stressTest = StressTestRule()

        @JvmStatic
        @get:Parameterized.Parameters(name = "config = {0}")
        val parameters: Collection<CameraIdExtensionModePair>
            get() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    /**
     * To monitor whether the camera is closed or opened.
     */
    private class CameraSessionMonitor {
        private var sessionEnabledLatch = CountDownLatch(1)
        private var sessionDisabledLatch = CountDownLatch(1)

        fun onOpenedSession() {
            sessionEnabledLatch.countDown()
        }

        fun onClosedSession() {
            sessionDisabledLatch.countDown()
        }

        fun reset() {
            sessionEnabledLatch = CountDownLatch(1)
            sessionDisabledLatch = CountDownLatch(1)
        }

        fun awaitSessionOpenedAndAssert() {
            assertThat(sessionEnabledLatch.await(3000, TimeUnit.MILLISECONDS)).isTrue()
        }

        fun awaitSessionClosedAndAssert() {
            assertThat(sessionDisabledLatch.await(3000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }
}
