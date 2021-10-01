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

package androidx.camera.extensions.internal

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Pair
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.impl.CaptureProcessorImpl
import androidx.camera.extensions.impl.CaptureStageImpl
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class ImageCaptureConfigProviderTest {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Tests in this class majorly use mock objects to run the test. No matter which extension
    // mode is use, it should not affect the test results.
    @ExtensionMode.Mode
    private val extensionMode = ExtensionMode.NONE
    private var cameraSelector = CameraSelector.Builder().build()
    private val fakeLifecycleOwner = FakeLifecycleOwner()

    private lateinit var cameraProvider: ProcessCameraProvider

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.deviceHasCamera())

        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        fakeLifecycleOwner.startAndResume()
    }

    @After
    fun cleanUp() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    @MediumTest
    fun extenderLifeCycleTest_noMoreInteractionsBeforeAndAfterInitDeInit(): Unit =
        runBlocking {
            val mockImageCaptureExtenderImpl = mock(ImageCaptureExtenderImpl::class.java)
            val captureStages = mutableListOf<CaptureStageImpl>()

            captureStages.add(FakeCaptureStage())

            Mockito.`when`(mockImageCaptureExtenderImpl.captureStages).thenReturn(captureStages)
            Mockito.`when`(mockImageCaptureExtenderImpl.captureProcessor).thenReturn(
                mock(CaptureProcessorImpl::class.java)
            )
            val mockVendorExtender = mock(BasicVendorExtender::class.java)
            Mockito.`when`(mockVendorExtender.imageCaptureExtenderImpl)
                .thenReturn(mockImageCaptureExtenderImpl)

            val imageCapture = createImageCaptureWithExtenderImpl(mockVendorExtender)

            // Binds the use case to trigger the camera pipeline operations
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, imageCapture)
            }

            // To verify the event callbacks in order, and to verification of the onEnableSession()
            // is also used to wait for the capture session created. The test for the unbind
            // would come after the capture session was created.
            verify(mockImageCaptureExtenderImpl, timeout(3000)).captureProcessor
            verify(mockImageCaptureExtenderImpl, timeout(3000)).maxCaptureStage

            verify(mockVendorExtender, timeout(3000)).supportedCaptureOutputResolutions

            val inOrder = Mockito.inOrder(mockImageCaptureExtenderImpl)
            inOrder.verify(mockImageCaptureExtenderImpl, timeout(3000)).onInit(
                any(String::class.java),
                any(CameraCharacteristics::class.java),
                any(Context::class.java)
            )
            inOrder.verify(mockImageCaptureExtenderImpl, timeout(3000).atLeastOnce()).captureStages
            inOrder.verify(mockImageCaptureExtenderImpl, timeout(3000).atLeastOnce())
                .onPresetSession()
            inOrder.verify(mockImageCaptureExtenderImpl, timeout(3000).atLeastOnce())
                .onEnableSession()

            withContext(Dispatchers.Main) {
                // Unbind the use case to test the onDisableSession and onDeInit.
                cameraProvider.unbind(imageCapture)
            }

            // To verify the onDisableSession and onDeInit.
            inOrder.verify(mockImageCaptureExtenderImpl, timeout(3000).atLeastOnce())
                .onDisableSession()
            inOrder.verify(mockImageCaptureExtenderImpl, timeout(3000)).onDeInit()

            // To verify there is no any other calls on the mock.
            verifyNoMoreInteractions(mockImageCaptureExtenderImpl)
        }

    @Test
    @MediumTest
    fun canSetSupportedResolutionsToConfigTest(): Unit = runBlocking {
        assumeTrue(CameraUtil.deviceHasCamera())

        // getSupportedResolutions supported since version 1.1
        val version = ExtensionVersion.getRuntimeVersion()
        assumeTrue(version != null && version >= Version.VERSION_1_1)

        val mockImageCaptureExtenderImpl = mock(ImageCaptureExtenderImpl::class.java)

        Mockito.`when`(mockImageCaptureExtenderImpl.isExtensionAvailable(any(), any()))
            .thenReturn(true)

        val targetFormatResolutionsPairList = generateImageCaptureSupportedResolutions()
        Mockito.`when`(mockImageCaptureExtenderImpl.supportedResolutions).thenReturn(
            targetFormatResolutionsPairList
        )

        val mockVendorExtender = mock(BasicVendorExtender::class.java)
        Mockito.`when`(mockVendorExtender.imageCaptureExtenderImpl)
            .thenReturn(mockImageCaptureExtenderImpl)

        val preview = createImageCaptureWithExtenderImpl(mockVendorExtender)

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, preview)
        }

        val resultFormatResolutionsPairList = (preview.currentConfig as ImageOutputConfig)
            .supportedResolutions

        // Checks the result and target pair lists are the same
        for (resultPair in resultFormatResolutionsPairList) {
            val firstTargetSizes = targetFormatResolutionsPairList.filter {
                it.first == resultPair.first
            }.map {
                it.second
            }.first()

            Truth.assertThat(mutableListOf(resultPair.second)).containsExactly(firstTargetSizes)
        }
    }

    private fun createImageCaptureWithExtenderImpl(basicVendorExtender: BasicVendorExtender) =
        ImageCapture.Builder().also {
            ImageCaptureConfigProvider(extensionMode, basicVendorExtender, context).apply {
                updateBuilderConfig(it, extensionMode, basicVendorExtender, context)
            }
        }.build()

    private fun generateImageCaptureSupportedResolutions(): List<Pair<Int, Array<Size>>> {
        val formatResolutionsPairList = mutableListOf<Pair<Int, Array<Size>>>()
        val cameraInfo = cameraProvider.availableCameraInfos[0]
        val characteristics = Camera2CameraInfo.extractCameraCharacteristics(cameraInfo)
        val map = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]

        // Retrieves originally supported resolutions from CameraCharacteristics for JPEG
        // format to return.
        map?.getOutputSizes(ImageFormat.JPEG)?.let {
            formatResolutionsPairList.add(Pair.create(ImageFormat.JPEG, it))
        }

        return formatResolutionsPairList
    }

    private class FakeCaptureStage : CaptureStageImpl {
        override fun getId() = 0
        override fun getParameters(): List<Pair<CaptureRequest.Key<*>, Any>> = emptyList()
    }
}
