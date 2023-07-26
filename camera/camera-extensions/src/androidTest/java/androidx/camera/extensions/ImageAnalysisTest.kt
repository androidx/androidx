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

package androidx.camera.extensions

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.util.Pair
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.internal.VendorExtender
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class ImageAnalysisTest(
    @ExtensionMode.Mode private val extensionMode: Int,
    @CameraSelector.LensFacing private val lensFacing: Int
) {
    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "extension = {0}, facing = {1}")
        val parameters: Collection<Array<Any>>
            get() = ExtensionsTestUtil.getAllExtensionsLensFacingCombinations()
    }

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionsCameraSelector: CameraSelector
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var camera: Camera

    @Before
    fun setUp(): Unit = runBlocking {
        Assume.assumeTrue(
            ExtensionsTestUtil.isTargetDeviceAvailableForExtensions(
                lensFacing,
                extensionMode
            )
        )

        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        baseCameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        Assume.assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))

        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
            camera = withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(fakeLifecycleOwner, baseCameraSelector)
            }
        }
    }

    @After
    fun teardown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    fun canBindImageAnalysis_ifIsImageAnalysisSupportedReturnsTrue(): Unit = runBlocking {
        // 1. Arrange
        extensionsCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode
        )
        Assume.assumeTrue(extensionsManager
            .isImageAnalysisSupported(extensionsCameraSelector, extensionMode))

        val analysisLatch = CountDownLatch(2)
        withContext(Dispatchers.Main) {
            val preview = Preview.Builder().build()
            val imageCapture = ImageCapture.Builder().build()
            val imageAnalysis = ImageAnalysis.Builder().build()

            preview.setSurfaceProvider(
                SurfaceTextureProvider.createSurfaceTextureProvider()
            )

            imageAnalysis.setAnalyzer(CameraXExecutors.ioExecutor()) {
                analysisLatch.countDown()
                it.close()
            }

            // 2. Act
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                extensionsCameraSelector,
                preview, imageCapture, imageAnalysis
            )
        }

        // 3. Assert
        assertThat(analysisLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue()
    }

    private fun getOutputSizes(imageFormat: Int): Array<Size> {
        val map = Camera2CameraInfo.from(camera.cameraInfo)
            .getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        return map.getOutputSizes(imageFormat)
    }

    @Test
    fun imageAnalysisResolutionIsFromVendorExtender(): Unit = runBlocking {
        // 1. Arrange
        val injectAnalysisSize = getOutputSizes(ImageFormat.YUV_420_888)
            .minBy { it.width * it.height }
        // Inject a fake VendorExtender that reports empty supported size for imageAnalysis.
        extensionsManager.setVendorExtenderFactory {
            object : VendorExtender {
                override fun isExtensionAvailable(
                    cameraId: String,
                    characteristicsMap: MutableMap<String, CameraCharacteristics>
                ) = true

                override fun getSupportedYuvAnalysisResolutions(): Array<Size> {
                    return arrayOf(injectAnalysisSize)
                }

                override fun getSupportedPreviewOutputResolutions(): List<Pair<Int, Array<Size>>> {
                    return listOf(
                        Pair(ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                            getOutputSizes(
                                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE))
                    )
                }

                override fun getSupportedCaptureOutputResolutions(): List<Pair<Int, Array<Size>>> {
                    return listOf(
                        Pair(ImageFormat.JPEG,
                            getOutputSizes(ImageFormat.JPEG))
                    )
                }
            }
        }

        extensionsCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode
        )
        assertThat(extensionsManager
            .isImageAnalysisSupported(baseCameraSelector, extensionMode)).isTrue()
        withContext(Dispatchers.Main) {
            val preview = Preview.Builder().build()
            val imageCapture = ImageCapture.Builder().build()
            val imageAnalysis = ImageAnalysis.Builder().build()

            // 2. Act
            cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    extensionsCameraSelector,
                    preview, imageCapture, imageAnalysis
            )

            // 3. Assert
            assertThat(imageAnalysis.resolutionInfo!!.resolution).isEqualTo(injectAnalysisSize)
        }
    }

    @Test
    fun bindImageAnalysisThrowException_ifIsImageAnalysisSupportedReturnsFalse():
        Unit = runBlocking {
        // 1. Arrange
        // Inject a fake VendorExtender that reports empty supported size for imageAnalysis.
        extensionsManager.setVendorExtenderFactory {
            object : VendorExtender {
                override fun isExtensionAvailable(
                    cameraId: String,
                    characteristicsMap: MutableMap<String, CameraCharacteristics>
                ) = true

                override fun getSupportedYuvAnalysisResolutions(): Array<Size> {
                    return emptyArray()
                }

                override fun getSupportedPreviewOutputResolutions(): List<Pair<Int, Array<Size>>> {
                    return listOf(
                        Pair(ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                            getOutputSizes(
                                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE))
                    )
                }

                override fun getSupportedCaptureOutputResolutions(): List<Pair<Int, Array<Size>>> {
                    return listOf(
                        Pair(ImageFormat.JPEG,
                            getOutputSizes(ImageFormat.JPEG))
                    )
                }
            }
        }

        extensionsCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode
        )
        assertThat(extensionsManager
            .isImageAnalysisSupported(baseCameraSelector, extensionMode)).isFalse()
        withContext(Dispatchers.Main) {
            val preview = Preview.Builder().build()
            val imageCapture = ImageCapture.Builder().build()
            val imageAnalysis = ImageAnalysis.Builder().build()

            // 3. Act && Assert
            assertThrows<IllegalArgumentException> {
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    extensionsCameraSelector,
                    preview, imageCapture, imageAnalysis
                )
            }
        }
    }
}
