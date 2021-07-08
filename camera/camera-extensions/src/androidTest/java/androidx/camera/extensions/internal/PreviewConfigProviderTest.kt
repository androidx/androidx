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
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.util.Pair
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.impl.CaptureStageImpl
import androidx.camera.extensions.impl.PreviewExtenderImpl
import androidx.camera.extensions.impl.PreviewImageProcessorImpl
import androidx.camera.extensions.impl.RequestUpdateProcessorImpl
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.SurfaceTextureProvider.SurfaceTextureCallback
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class PreviewConfigProviderTest {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val surfaceTextureCallback: SurfaceTextureCallback =
        object : SurfaceTextureCallback {
            override fun onSurfaceTextureReady(
                surfaceTexture: SurfaceTexture,
                resolution: Size
            ) {
                // No-op.
            }

            override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                // No-op.
            }
        }

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
    fun extenderLifeCycleTest_noMoreInvokeBeforeAndAfterInitDeInit(): Unit = runBlocking {
        val mockPreviewExtenderImpl = mock(PreviewExtenderImpl::class.java)

        Mockito.`when`(mockPreviewExtenderImpl.processorType).thenReturn(
            PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR
        )
        Mockito.`when`(mockPreviewExtenderImpl.processor)
            .thenReturn(mock(PreviewImageProcessorImpl::class.java))
        Mockito.`when`(
            mockPreviewExtenderImpl.isExtensionAvailable(
                any(String::class.java),
                any(CameraCharacteristics::class.java)
            )
        ).thenReturn(true)

        val preview = createPreviewWithExtenderImpl(mockPreviewExtenderImpl)

        withContext(Dispatchers.Main) {
            // To set the update listener and Preview will change to active state.
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createSurfaceTextureProvider(surfaceTextureCallback)
            )

            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, preview)
        }

        // To verify the call in order after bind to life cycle, and to verification of the
        // getCaptureStages() is also used to wait for the capture session created. The test for
        // the unbind would come after the capture session was created. Ignore any of the calls
        // unrelated to the ExtenderStateListener.
        verify(mockPreviewExtenderImpl, timeout(3000)).processorType
        verify(mockPreviewExtenderImpl, timeout(3000)).processor

        // getSupportedResolutions supported since version 1.1
        val version = ExtensionVersion.getRuntimeVersion()
        if (version != null && version >= Version.VERSION_1_1) {
            verify(mockPreviewExtenderImpl, timeout(3000)).supportedResolutions
        }

        val inOrder = Mockito.inOrder(*Mockito.ignoreStubs(mockPreviewExtenderImpl))
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onInit(
            any(String::class.java),
            any(CameraCharacteristics::class.java),
            any(Context::class.java)
        )

        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onPresetSession()
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onEnableSession()
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).captureStage

        withContext(Dispatchers.Main) {
            // Unbind the use case to test the onDisableSession and onDeInit.
            cameraProvider.unbind(preview)
        }

        // To verify the onDisableSession and onDeInit.
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onDisableSession()
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onDeInit()

        // To verify there is no any other calls on the mock.
        verifyNoMoreInteractions(mockPreviewExtenderImpl)
    }

    @Test
    fun getCaptureStagesTest_shouldSetToRepeatingRequest(): Unit = runBlocking {
        // Set up a result for getCaptureStages() testing.
        val fakeCaptureStageImpl: CaptureStageImpl = FakeCaptureStageImpl()
        val mockPreviewExtenderImpl = mock(PreviewExtenderImpl::class.java)
        val mockRequestUpdateProcessorImpl = mock(RequestUpdateProcessorImpl::class.java)

        // The mock an RequestUpdateProcessorImpl to capture the returned TotalCaptureResult
        Mockito.`when`(mockPreviewExtenderImpl.processorType).thenReturn(
            PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_REQUEST_UPDATE_ONLY
        )
        Mockito.`when`(mockPreviewExtenderImpl.processor).thenReturn(mockRequestUpdateProcessorImpl)
        Mockito.`when`(
            mockPreviewExtenderImpl.isExtensionAvailable(
                any(String::class.java),
                any(CameraCharacteristics::class.java)
            )
        ).thenReturn(true)
        Mockito.`when`(mockPreviewExtenderImpl.captureStage).thenReturn(fakeCaptureStageImpl)

        val preview = createPreviewWithExtenderImpl(mockPreviewExtenderImpl)

        withContext(Dispatchers.Main) {
            // To set the update listener and Preview will change to active state.
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createSurfaceTextureProvider(surfaceTextureCallback)
            )

            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, preview)
        }

        val captureResultArgumentCaptor = ArgumentCaptor.forClass(
            TotalCaptureResult::class.java
        )
        verify(mockRequestUpdateProcessorImpl, timeout(3000).atLeastOnce()).process(
            captureResultArgumentCaptor.capture()
        )

        // TotalCaptureResult might be captured multiple times. Only care to get one instance of
        // it, since they should all have the same value for the tested key
        val totalCaptureResult = captureResultArgumentCaptor.value

        // To verify the capture result should include the parameter of the getCaptureStages().
        val parameters = fakeCaptureStageImpl.parameters
        for (parameter: Pair<CaptureRequest.Key<*>?, Any> in parameters) {
            assertThat(totalCaptureResult.request[parameter.first] == parameter.second)
        }
    }

    @Test
    fun processShouldBeInvoked_typeImageProcessor(): Unit = runBlocking {
        // The type image processor will invoke PreviewImageProcessor.process()
        val mockPreviewImageProcessorImpl = mock(PreviewImageProcessorImpl::class.java)
        val mockPreviewExtenderImpl = mock(PreviewExtenderImpl::class.java)

        Mockito.`when`(mockPreviewExtenderImpl.processor).thenReturn(mockPreviewImageProcessorImpl)
        Mockito.`when`(mockPreviewExtenderImpl.processorType)
            .thenReturn(PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR)
        Mockito.`when`(
            mockPreviewExtenderImpl.isExtensionAvailable(
                any(String::class.java),
                any(CameraCharacteristics::class.java)
            )
        ).thenReturn(true)

        val preview = createPreviewWithExtenderImpl(mockPreviewExtenderImpl)

        withContext(Dispatchers.Main) {
            // To set the update listener and Preview will change to active state.
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createSurfaceTextureProvider(surfaceTextureCallback)
            )

            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, preview)
        }

        // To verify the process() method was invoked with non-null TotalCaptureResult input.
        verify(mockPreviewImageProcessorImpl, Mockito.timeout(3000).atLeastOnce())
            .process(any(Image::class.java), ArgumentMatchers.any(TotalCaptureResult::class.java))
    }

    @Test
    @MediumTest
    fun canSetSupportedResolutionsToConfigTest(): Unit = runBlocking {
        assumeTrue(CameraUtil.deviceHasCamera())

        // getSupportedResolutions supported since version 1.1
        val version = ExtensionVersion.getRuntimeVersion()
        assumeTrue(version != null && version >= Version.VERSION_1_1)

        val mockPreviewExtenderImpl = mock(PreviewExtenderImpl::class.java)

        Mockito.`when`(mockPreviewExtenderImpl.isExtensionAvailable(any(), any())).thenReturn(true)
        Mockito.`when`(mockPreviewExtenderImpl.processorType).thenReturn(
            PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_NONE
        )

        val targetFormatResolutionsPairList = generatePreviewSupportedResolutions()
        Mockito.`when`(mockPreviewExtenderImpl.supportedResolutions).thenReturn(
            targetFormatResolutionsPairList
        )

        val preview = createPreviewWithExtenderImpl(mockPreviewExtenderImpl)

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

            assertThat(mutableListOf(resultPair.second)).containsExactly(firstTargetSizes)
        }
    }

    private fun createPreviewWithExtenderImpl(impl: PreviewExtenderImpl) =
        Preview.Builder().also {
            val cameraInfo = cameraSelector.filter(cameraProvider.availableCameraInfos)[0]
            PreviewConfigProvider(extensionMode, cameraInfo, context).apply {
                updateBuilderConfig(it, extensionMode, impl, context)
            }
        }.build()

    private fun generatePreviewSupportedResolutions(): List<Pair<Int, Array<Size>>> {
        val formatResolutionsPairList = mutableListOf<Pair<Int, Array<Size>>>()
        val cameraInfo = cameraProvider.availableCameraInfos[0]
        val characteristics = Camera2CameraInfo.extractCameraCharacteristics(cameraInfo)
        val map = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]

        // Retrieves originally supported resolutions from CameraCharacteristics for PRIVATE
        // format to return.
        map?.getOutputSizes(ImageFormat.PRIVATE)?.let {
            formatResolutionsPairList.add(Pair.create(ImageFormat.PRIVATE, it))
        }

        return formatResolutionsPairList
    }

    private class FakeCaptureStageImpl : CaptureStageImpl {
        override fun getId() = 0
        override fun getParameters(): List<Pair<CaptureRequest.Key<*>, Any>> = mutableListOf(
            Pair.create(
                CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA
            )
        )
    }
}
