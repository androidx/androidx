/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.utils.CameraIdExtensionModePair
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * ImageAnalysisTest is to verify if CameraX ImageAnalysis can work properly. The tests will be
 * ignored if OEM doesn't support ImageAnalysis.
 *
 * For advanced extender implementation, ImageAnalysis is supported if
 * AdvancedExtenderImpl#getSupportedYuvAnalysisResolutions returns an non-empty list.). For
 * basic extender implementation, ImageAnalysis is supported if the hardware level of the camera
 * supports the stream configuration determined by the capture processor and the preview processor.
 * For example, if both CaptureProcessor is enabled and the preview's processorType is
 * PROCESSOR_TYPE_IMAGE_PROCESSOR, then the stream configuration is YUV+YUV+YUV which requires
 * hardware level FULL or above to support it.
 */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class ImageAnalysisTest(private val config: CameraIdExtensionModePair) {
    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "config = {0}")
        val parameters: Collection<CameraIdExtensionModePair>
            get() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector
    private lateinit var lifecycleOwner: FakeLifecycleOwner

    @Before
    fun setUp(): Unit = runBlocking(Dispatchers.Main) {
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, SECONDS]
        lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()
        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(config.cameraId)
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]
        Assume.assumeTrue(
            extensionsManager.isExtensionAvailable(
                baseCameraSelector,
                config.extensionMode
            )
        )
        Assume.assumeTrue(
            extensionsManager.isImageAnalysisSupported(
                baseCameraSelector, config.extensionMode
            )
        )
    }

    @After
    fun tearDown() = runBlocking(Dispatchers.Main) {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, SECONDS]
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10, SECONDS]
        }
    }

    @Test
    fun imageAnalysisPreviewImageCaptureCanProduceOutput() = runBlocking {
        extensionCameraSelector = extensionsManager
            .getExtensionEnabledCameraSelector(baseCameraSelector, config.extensionMode)

        val previewFrameDeferred = CompletableDeferred<Boolean>()
        val captureDeferred = CompletableDeferred<Boolean>()
        val imageAnalysisDeferred = CompletableDeferred<Boolean>()

        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        imageAnalysis.setAnalyzer(CameraXExecutors.ioExecutor()) {
            it.close()
            imageAnalysisDeferred.complete(true)
        }

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                    previewFrameDeferred.complete(true)
                }
            )
            cameraProvider.bindToLifecycle(
                lifecycleOwner, extensionCameraSelector,
                preview, imageCapture, imageAnalysis
            )
        }

        Truth.assertThat(
            withTimeoutOrNull(SECONDS.toMillis(3L)) { previewFrameDeferred.await() } ?: false)
            .isTrue()

        Truth.assertThat(
            withTimeoutOrNull(SECONDS.toMillis(3L)) { imageAnalysisDeferred.await() } ?: false)
            .isTrue()

        imageCapture.takePicture(
            CameraXExecutors.ioExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    captureDeferred.complete(true)
                }

                override fun onError(exception: ImageCaptureException) {
                    captureDeferred.completeExceptionally(exception)
                }
            })
        Truth.assertThat(
            withTimeoutOrNull(SECONDS.toMillis(10L)) { captureDeferred.await() } ?: false)
            .isTrue()
    }
}
