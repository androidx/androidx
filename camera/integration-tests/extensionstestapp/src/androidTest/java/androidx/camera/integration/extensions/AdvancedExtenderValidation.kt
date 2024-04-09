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

package androidx.camera.integration.extensions

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat
import androidx.camera.core.CameraXConfig
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.impl.advanced.AdvancedExtenderImpl
import androidx.camera.extensions.impl.advanced.Camera2OutputConfigImpl
import androidx.camera.extensions.impl.advanced.Camera2SessionConfigImpl
import androidx.camera.extensions.impl.advanced.ImageReaderOutputConfigImpl
import androidx.camera.extensions.impl.advanced.MultiResolutionImageReaderOutputConfigImpl
import androidx.camera.extensions.impl.advanced.OutputSurfaceConfigurationImpl
import androidx.camera.extensions.impl.advanced.OutputSurfaceImpl
import androidx.camera.extensions.impl.advanced.SurfaceOutputConfigImpl
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.ExtensionsUtils
import androidx.camera.extensions.internal.Version
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.getImageCaptureSupportedResolutions
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assume
import org.junit.Assume.assumeTrue

@RequiresApi(28)
class AdvancedExtenderValidation(
    private val cameraXConfig: CameraXConfig,
    private val cameraId: String,
    private val extensionMode: Int
) {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraCharacteristicsMap: Map<String, CameraCharacteristics>
    private lateinit var advancedImpl: AdvancedExtenderImpl

    fun setUp(): Unit = runBlocking {
        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider =
            ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]
        assumeTrue(CameraXExtensionsTestUtil.isAdvancedExtenderImplemented())
        val baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId)
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, extensionMode))
        val extensionCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
            baseCameraSelector,
            extensionMode
        )
        val cameraInfo = withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(FakeLifecycleOwner(), extensionCameraSelector).cameraInfo
        }
        cameraCharacteristicsMap =
            ExtensionsUtils.getCameraCharacteristicsMap(cameraInfo as CameraInfoInternal)
        advancedImpl = CameraXExtensionsTestUtil
            .createAdvancedExtenderImpl(extensionMode, cameraId, cameraInfo)
    }

    private val teardownFunctions = mutableListOf<() -> Unit>()

    // Adding block to be invoked when tearing down. The last added will be invoked the first.
    private fun addTearDown(teardown: () -> Unit) {
        synchronized(teardownFunctions) {
            teardownFunctions.add(0, teardown) // added to the head
        }
    }

    fun tearDown(): Unit = runBlocking {
        synchronized(teardownFunctions) {
            for (teardownFunction in teardownFunctions) {
                teardownFunction()
            }
            teardownFunctions.clear()
        }
        withContext(Dispatchers.Main) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
            cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
        }
    }

    // Test
    fun getSupportedPreviewOutputResolutions_returnValidData() {
        val map = advancedImpl.getSupportedPreviewOutputResolutions(cameraId)

        assertThat(map[ImageFormat.PRIVATE]).isNotEmpty()
    }

    // Test
    fun getSupportedCaptureOutputResolutions_returnValidData() {
        val map = advancedImpl.getSupportedCaptureOutputResolutions(cameraId)

        assertThat(map[ImageFormat.JPEG]).isNotEmpty()
        assertThat(map[ImageFormat.YUV_420_888]).isNotEmpty()
    }

    // Test
    fun getAvailableCaptureRequestKeys_existAfter1_3() {
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_3)
        advancedImpl.getAvailableCaptureRequestKeys()
    }

    // Test
    fun getAvailableCaptureResultKeys_existAfter1_3() {
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_3)
        advancedImpl.getAvailableCaptureResultKeys()
    }

    /**
     * The following 1.4 interface methods are validated by this test.
     * <ol>
     *   <li>AdvancedExtenderImpl#isPostviewAvailable()
     *   <li>AdvancedExtenderImpl#getSupportedPostviewResolutions()
     * </ol>
     */
    // Test
    fun validatePostviewSupport_sinceVersion_1_4() {
        // Runs the test only when the vendor library implementation is 1.4 or above
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_4)

        // Runs the test only when postview is available
        assumeTrue(advancedImpl.isPostviewAvailable)

        var anyPostViewSupported = false

        getImageCaptureSupportedResolutions(
            advancedImpl,
            cameraId,
            cameraCharacteristicsMap[cameraId]!!
        )
            .forEach { captureSize ->
                anyPostViewSupported = true
                var captureSizeSupported = false
                var yuvFormatSupported = false
                advancedImpl.getSupportedPostviewResolutions(captureSize).forEach { entry ->
                    captureSizeSupported = true
                    if (entry.key == ImageFormat.YUV_420_888) {
                        yuvFormatSupported = true
                    }

                    entry.value.forEach { postviewSize ->
                        // The postview size be smaller than or equal to the provided capture size.
                        assertThat(SizeUtil.getArea(postviewSize))
                            .isAtMost(SizeUtil.getArea(captureSize))
                        // The postview size must have the same aspect ratio as the given capture
                        // size.
                        assertThat(
                            AspectRatioUtil.hasMatchingAspectRatio(
                                postviewSize,
                                Rational(captureSize.width, captureSize.height)
                            )
                        ).isTrue()
                    }
                }
                // When postview is supported for the capture size, as the javadoc description,
                // YUV_420_888 format must be supported.
                if (captureSizeSupported) {
                    assertThat(yuvFormatSupported).isTrue()
                }
            }

        // At least one postview size must be supported when isPostviewAvailable returns true.
        assertThat(anyPostViewSupported).isTrue()
    }

    // Test
    fun validateProcessProgressSupport_sinceVersion_1_4() {
        // Runs the test only when the vendor library implementation is 1.4 or above
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_4)
        // Makes sure isCaptureProcessProgressAvailable API can be called without any exception
        // occurring when the vendor library implementation is version 1.4 or above
        advancedImpl.isCaptureProcessProgressAvailable
    }

    enum class SizeCategory {
        MAXIMUM,
        MEDIAN,
        MINIMUM
    }

    private fun createPreviewOutput(
        impl: AdvancedExtenderImpl,
        sizeCategory: SizeCategory
    ): OutputSurfaceImpl {
        val previewSizeMap = impl.getSupportedPreviewOutputResolutions(cameraId)
        assertThat(previewSizeMap[ImageFormat.PRIVATE]).isNotEmpty()

        val previewSizes = previewSizeMap[ImageFormat.PRIVATE]!!
        val previewSize = getSizeByClass(previewSizes, sizeCategory)
        val surfaceTexture = SurfaceTexture(0)
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)
        addTearDown {
            surfaceTexture.release()
        }
        return OutputSurface(previewSurface, previewSize, ImageFormat.PRIVATE)
    }

    private fun createCaptureOutput(
        impl: AdvancedExtenderImpl,
        sizeCategory: SizeCategory
    ): OutputSurfaceImpl {
        val captureSizeMap = impl.getSupportedCaptureOutputResolutions(cameraId)
        assertThat(captureSizeMap[ImageFormat.JPEG]).isNotEmpty()

        val captureSizes = captureSizeMap[ImageFormat.JPEG]!!
        var captureSize = getSizeByClass(captureSizes, sizeCategory)
        val imageReader = ImageReader.newInstance(
            captureSize.width, captureSize.height, ImageFormat.JPEG, 1
        )
        addTearDown {
            imageReader.close()
        }
        return OutputSurface(imageReader.surface, captureSize, ImageFormat.JPEG)
    }

    private fun getSizeByClass(
        sizes: List<Size>,
        sizeCategory: SizeCategory
    ): Size {
        val sortedList = sizes.sortedByDescending { it.width * it.height }
        var size =
            when (sizeCategory) {
                SizeCategory.MAXIMUM -> {
                    sortedList[0]
                }

                SizeCategory.MEDIAN -> {
                    sortedList[sortedList.size / 2]
                }

                SizeCategory.MINIMUM -> {
                    sortedList[sortedList.size - 1]
                }
            }
        return size
    }

    private fun createAnalysisOutput(
        impl: AdvancedExtenderImpl,
        sizeCategory: SizeCategory
    ): OutputSurfaceImpl? {
        val analysisSizes = impl.getSupportedYuvAnalysisResolutions(cameraId) ?: return null
        assertThat(analysisSizes).isNotEmpty()

        var analysisSize = getSizeByClass(analysisSizes, sizeCategory)
        val imageReader = ImageReader.newInstance(
            analysisSize.width, analysisSize.height, ImageFormat.YUV_420_888, 1
        )
        addTearDown {
            imageReader.close()
        }
        return OutputSurface(imageReader.surface, analysisSize, ImageFormat.YUV_420_888)
    }

    private fun createPostviewOutput(
        impl: AdvancedExtenderImpl,
        captureSize: Size
    ): OutputSurfaceImpl {
        val postviewSize =
            impl.getSupportedPostviewResolutions(captureSize)[ImageFormat.YUV_420_888]!![0]

        val postviewImageReader = ImageReader.newInstance(
            postviewSize.width, postviewSize.height, ImageFormat.YUV_420_888, 1
        )
        addTearDown {
            postviewImageReader.close()
        }
        return OutputSurface(postviewImageReader.surface, captureSize, ImageFormat.YUV_420_888)
    }

    // Test
    fun initSession_maxSize_canConfigureSession() = initSessionTest(
        previewOutputSizeCategory = SizeCategory.MAXIMUM,
        captureOutputSizeCategory = SizeCategory.MAXIMUM
    )

    // Test
    fun initSession_minSize_canConfigureSession() = initSessionTest(
        previewOutputSizeCategory = SizeCategory.MINIMUM,
        captureOutputSizeCategory = SizeCategory.MINIMUM
    )

    // Test
    fun initSession_medianSize_canConfigureSession() = initSessionTest(
        previewOutputSizeCategory = SizeCategory.MEDIAN,
        captureOutputSizeCategory = SizeCategory.MEDIAN
    )

    // Test
    fun initSessionWithAnalysis_maxSize_canConfigureSession() = initSessionTest(
        previewOutputSizeCategory = SizeCategory.MAXIMUM,
        captureOutputSizeCategory = SizeCategory.MAXIMUM,
        analysisOutputSizeCategory = SizeCategory.MAXIMUM
    )

    // Test
    fun initSessionWithAnalysis_minSize_canConfigureSession() = initSessionTest(
        previewOutputSizeCategory = SizeCategory.MINIMUM,
        captureOutputSizeCategory = SizeCategory.MINIMUM,
        analysisOutputSizeCategory = SizeCategory.MINIMUM
    )

    // Test
    fun initSessionWithAnalysis_medianSize_canConfigureSession() = initSessionTest(
        previewOutputSizeCategory = SizeCategory.MEDIAN,
        captureOutputSizeCategory = SizeCategory.MEDIAN,
        analysisOutputSizeCategory = SizeCategory.MEDIAN
    )

    // Test
    fun initSessionWithOutputSurfaceConfigurationImpl_maxSize_canConfigureSession() {
        // Runs the test only when the vendor library implementation is 1.4 or above
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_4)
        initSessionTest(
            previewOutputSizeCategory = SizeCategory.MAXIMUM,
            captureOutputSizeCategory = SizeCategory.MAXIMUM,
            enablePostview = advancedImpl.isPostviewAvailable,
            useOutputSurfaceConfigurationImpl = true
        )
    }

    // Test
    fun validateSessionTypeSupport_sinceVersion_1_4() {
        // Runs the test only when the vendor library implementation is 1.4 or above
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_4)
        val camera2SessionConfigImpl = initSession(
            previewOutputSizeCategory = SizeCategory.MAXIMUM,
            captureOutputSizeCategory = SizeCategory.MAXIMUM
        )
        // getSessionType is allowed to return any OEM customized session type, therefore, we can
        // only try to invoke this method to make sure that this method correctly exists in the
        // vendor library implementation.
        camera2SessionConfigImpl.sessionType
    }

    // Test
    fun validateSessionTypeSupportWithOutputSurfaceConfigurationImpl_sinceVersion_1_4() {
        // Runs the test only when the vendor library implementation is 1.4 or above
        assumeTrue(ExtensionVersion.getRuntimeVersion()!! >= Version.VERSION_1_4)
        val camera2SessionConfigImpl = initSession(
            previewOutputSizeCategory = SizeCategory.MAXIMUM,
            captureOutputSizeCategory = SizeCategory.MAXIMUM,
            enablePostview = advancedImpl.isPostviewAvailable,
            useOutputSurfaceConfigurationImpl = true
        )
        // getSessionType is allowed to return any OEM customized session type, therefore, we can
        // only try to invoke this method to make sure that this method correctly exists in the
        // vendor library implementation.
        camera2SessionConfigImpl.sessionType
    }

    fun initSessionTest(
        previewOutputSizeCategory: SizeCategory,
        captureOutputSizeCategory: SizeCategory,
        analysisOutputSizeCategory: SizeCategory? = null,
        enablePostview: Boolean = false,
        useOutputSurfaceConfigurationImpl: Boolean = false
    ): Unit = runBlocking {
        val camera2SessionConfigImpl = initSession(
            previewOutputSizeCategory,
            captureOutputSizeCategory,
            analysisOutputSizeCategory,
            enablePostview,
            useOutputSurfaceConfigurationImpl
        )

        verifyCamera2SessionConfig(camera2SessionConfigImpl)
    }

    private fun initSession(
        previewOutputSizeCategory: SizeCategory,
        captureOutputSizeCategory: SizeCategory,
        analysisOutputSizeCategory: SizeCategory? = null,
        enablePostview: Boolean = false,
        useOutputSurfaceConfigurationImpl: Boolean = false
    ): Camera2SessionConfigImpl {
        if (analysisOutputSizeCategory != null) {
            Assume.assumeFalse(
                advancedImpl.getSupportedYuvAnalysisResolutions(cameraId).isNullOrEmpty()
            )
        }

        val sessionProcessor = advancedImpl.createSessionProcessor()
        val previewOutput = createPreviewOutput(advancedImpl, previewOutputSizeCategory)
        val captureOutput = createCaptureOutput(advancedImpl, captureOutputSizeCategory)
        val analysisOutput = analysisOutputSizeCategory?.let {
            createAnalysisOutput(advancedImpl, analysisOutputSizeCategory)
        }

        addTearDown {
            sessionProcessor.deInitSession()
        }

        return if (!useOutputSurfaceConfigurationImpl) {
            sessionProcessor.initSession(
                cameraId,
                cameraCharacteristicsMap,
                context,
                previewOutput,
                captureOutput,
                analysisOutput
            )
        } else {
            val postviewOutput = if (enablePostview) {
                createPostviewOutput(advancedImpl, captureOutput.size)
            } else {
                null
            }
            val outputSurfaceConfigurationImpl = OutputSurfaceConfigurationImplAdapter(
                previewOutput,
                captureOutput,
                analysisOutput,
                postviewOutput
            )
            sessionProcessor.initSession(
                cameraId,
                cameraCharacteristicsMap,
                context,
                outputSurfaceConfigurationImpl
            )
        }
    }

    private class OutputSurface(
        private val surface: Surface,
        private val size: Size,
        private val imageFormat: Int
    ) : OutputSurfaceImpl {
        override fun getSurface() = surface
        override fun getSize() = size
        override fun getImageFormat() = imageFormat
    }

    private fun getOutputConfiguration(
        outputConfigImpl: Camera2OutputConfigImpl
    ): OutputConfiguration {
        var outputConfiguration: OutputConfiguration
        when (outputConfigImpl) {
            is SurfaceOutputConfigImpl -> {
                val surface = outputConfigImpl.surface
                outputConfiguration = OutputConfiguration(outputConfigImpl.surfaceGroupId, surface)
            }

            is ImageReaderOutputConfigImpl -> {
                val imageReader = ImageReader.newInstance(
                    outputConfigImpl.size.width,
                    outputConfigImpl.size.height,
                    outputConfigImpl.imageFormat,
                    outputConfigImpl.maxImages
                )
                val surface = imageReader.surface
                addTearDown { imageReader.close() }
                outputConfiguration = OutputConfiguration(outputConfigImpl.surfaceGroupId, surface)
            }

            is MultiResolutionImageReaderOutputConfigImpl ->
                throw java.lang.UnsupportedOperationException(
                    "MultiResolutionImageReaderOutputConfigImpl not supported"
                )

            else -> throw java.lang.UnsupportedOperationException(
                "Output configuration type not supported"
            )
        }

        if (outputConfigImpl.physicalCameraId != null) {
            outputConfiguration.setPhysicalCameraId(outputConfigImpl.physicalCameraId)
        }

        outputConfigImpl.surfaceSharingOutputConfigs?.let {
            for (surfaceSharingOutputConfig in it) {
                val sharingOutputConfiguration = getOutputConfiguration(surfaceSharingOutputConfig)
                outputConfiguration.addSurface(sharingOutputConfiguration.surface!!)
                outputConfiguration.enableSurfaceSharing()
            }
        }

        return outputConfiguration
    }

    private suspend fun openCameraDevice(cameraId: String): CameraDevice {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val deferred = CompletableDeferred<CameraDevice>()
        cameraManager.openCamera(
            cameraId,
            CameraXExecutors.ioExecutor(),
            object : CameraDevice.StateCallback() {
                override fun onOpened(cameraDevice: CameraDevice) {
                    deferred.complete(cameraDevice)
                }

                override fun onDisconnected(cameraDevice: CameraDevice) {
                    deferred.completeExceptionally(RuntimeException("Camera Disconnected"))
                }

                override fun onError(cameraDevice: CameraDevice, error: Int) {
                    deferred.completeExceptionally(
                        RuntimeException("Camera onError(error=$cameraDevice)")
                    )
                }
            })
        return deferred.await()
    }

    private suspend fun openCaptureSession(
        cameraDevice: CameraDevice,
        camera2SessionConfig: Camera2SessionConfigImpl
    ): CameraCaptureSession {

        val outputConfigurationList = mutableListOf<OutputConfiguration>()
        for (outputConfig in camera2SessionConfig.outputConfigs) {
            val outputConfiguration = getOutputConfiguration(outputConfig)
            outputConfigurationList.add(outputConfiguration)
        }

        val sessionDeferred = CompletableDeferred<CameraCaptureSession>()
        val sessionConfiguration = SessionConfiguration(
            SessionConfigurationCompat.SESSION_REGULAR,
            outputConfigurationList,
            CameraXExecutors.ioExecutor(),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    sessionDeferred.complete(session)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    sessionDeferred.completeExceptionally(RuntimeException("onConfigureFailed"))
                }

                override fun onReady(session: CameraCaptureSession) {
                }

                override fun onActive(session: CameraCaptureSession) {
                }

                override fun onCaptureQueueEmpty(session: CameraCaptureSession) {
                }
            }
        )

        val requestBuilder = cameraDevice.createCaptureRequest(
            camera2SessionConfig.sessionTemplateId
        )

        camera2SessionConfig.sessionParameters.forEach { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            requestBuilder.set(key as CaptureRequest.Key<Any>, value)
        }
        sessionConfiguration.sessionParameters = requestBuilder.build()

        cameraDevice.createCaptureSession(sessionConfiguration)

        return sessionDeferred.await()
    }

    private suspend fun verifyCamera2SessionConfig(camera2SessionConfig: Camera2SessionConfigImpl) {
        val cameraDevice = openCameraDevice(cameraId)
        assertThat(cameraDevice).isNotNull()
        addTearDown { cameraDevice.close() }
        val captureSession = openCaptureSession(cameraDevice, camera2SessionConfig)
        assertThat(captureSession).isNotNull()
        addTearDown { captureSession.close() }
    }

    private class OutputSurfaceConfigurationImplAdapter(
        private val previewOutputSurface: OutputSurfaceImpl,
        private val captureOutputSurface: OutputSurfaceImpl,
        private val analysisOutputSurface: OutputSurfaceImpl?,
        private val postviewOutputSurface: OutputSurfaceImpl?
    ) : OutputSurfaceConfigurationImpl {
        override fun getPreviewOutputSurface(): OutputSurfaceImpl {
            return previewOutputSurface
        }

        override fun getImageCaptureOutputSurface(): OutputSurfaceImpl {
            return captureOutputSurface
        }

        override fun getImageAnalysisOutputSurface(): OutputSurfaceImpl? {
            return analysisOutputSurface
        }

        override fun getPostviewOutputSurface(): OutputSurfaceImpl? {
            return postviewOutputSurface
        }
    }
}
