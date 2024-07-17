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

package androidx.camera.extensions.internal

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraExtensionCharacteristics.EXTENSION_FACE_RETOUCH
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.media.ImageWriter
import android.os.Build
import android.util.Pair
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.OutputSurface
import androidx.camera.core.impl.OutputSurfaceConfiguration
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.impl.advanced.AdvancedExtenderImpl
import androidx.camera.extensions.impl.advanced.Camera2OutputConfigImpl
import androidx.camera.extensions.impl.advanced.Camera2OutputConfigImplBuilder
import androidx.camera.extensions.impl.advanced.Camera2SessionConfigImpl
import androidx.camera.extensions.impl.advanced.OutputSurfaceConfigurationImpl
import androidx.camera.extensions.impl.advanced.OutputSurfaceImpl
import androidx.camera.extensions.impl.advanced.RequestProcessorImpl
import androidx.camera.extensions.impl.advanced.SessionProcessorImpl
import androidx.camera.extensions.internal.sessionprocessor.AdvancedSessionProcessor
import androidx.camera.extensions.util.Camera2SessionConfigImplBuilder
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.SurfaceTextureProvider.SurfaceTextureCallback
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@SdkSuppress(minSdkVersion = 26)
@RunWith(AndroidJUnit4::class)
class AdvancedSessionProcessorTest {
    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    @Before
    fun setUp() = runBlocking {
        // Pixel lacks some Extensions-Interface methods / classes which cause the test failure.
        assumeFalse(Build.MODEL.uppercase().startsWith("PIXEL"))
        ExtensionVersion.injectInstance(null)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
        }
    }

    @After
    fun tearDown() = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) { cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS] }
        }
    }

    private fun getCameraSelectorWithSessionProcessor(
        cameraSelector: CameraSelector,
        sessionProcessor: SessionProcessor
    ): CameraSelector {
        val identifier = Identifier.create("idStr")
        ExtendedCameraConfigProviderStore.addConfig(identifier) { _, _ ->
            object : CameraConfig {
                override fun getConfig(): Config {
                    return MutableOptionsBundle.create()
                }

                override fun getCompatibilityId(): Identifier {
                    return Identifier.create(0)
                }

                override fun getSessionProcessor(
                    valueIfMissing: SessionProcessor?
                ): SessionProcessor? {
                    return sessionProcessor
                }

                override fun getSessionProcessor(): SessionProcessor {
                    return sessionProcessor
                }
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

    @Test
    fun useCasesCanWork_directlyUseOutputSurface() = runBlocking {
        val fakeSessionProcessImpl =
            FakeSessionProcessImpl(
                // Directly use output surface
                previewConfigBlock = { outputSurfaceImpl ->
                    Camera2OutputConfigImplBuilder.newSurfaceConfig(outputSurfaceImpl.surface!!)
                        .build()
                },
                // Directly use output surface
                captureConfigBlock = { outputSurfaceImpl ->
                    Camera2OutputConfigImplBuilder.newSurfaceConfig(outputSurfaceImpl.surface!!)
                        .build()
                },
                // Directly use output surface
                analysisConfigBlock = { outputSurfaceImpl ->
                    Camera2OutputConfigImplBuilder.newSurfaceConfig(outputSurfaceImpl.surface!!)
                        .build()
                }
            )

        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        verifyUseCasesOutput(fakeSessionProcessImpl, preview, imageCapture, imageAnalysis)
    }

    @Test
    fun canInvokeStartTrigger() = runBlocking {
        assumeTrue(ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_3))
        val fakeSessionProcessImpl = FakeSessionProcessImpl()
        val advancedSessionProcessor =
            AdvancedSessionProcessor(
                fakeSessionProcessImpl,
                emptyList(),
                object : VendorExtender {},
                context
            )

        val parametersMap: MutableMap<CaptureRequest.Key<*>, Any> =
            mutableMapOf(
                CaptureRequest.CONTROL_AF_MODE to CaptureRequest.CONTROL_AF_MODE_AUTO,
                CaptureRequest.JPEG_QUALITY to 0
            )

        val config =
            RequestOptionConfig.Builder()
                .also {
                    for (key in parametersMap.keys) {
                        @Suppress("UNCHECKED_CAST") val anyKey = key as CaptureRequest.Key<Any>
                        it.setCaptureRequestOption(anyKey, parametersMap[anyKey] as Any)
                    }
                }
                .build()
        advancedSessionProcessor.startTrigger(config, object : SessionProcessor.CaptureCallback {})

        fakeSessionProcessImpl.assertStartTriggerIsCalledWithParameters(parametersMap)
    }

    @Test
    fun getRealtimeLatencyEstimate_advancedSessionProcessorInvokesSessionProcessorImpl() =
        runBlocking {
            assumeTrue(ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4))
            ClientVersion.setCurrentVersion(ClientVersion("1.4.0"))

            val fakeSessionProcessImpl =
                object : SessionProcessorImpl by FakeSessionProcessImpl() {
                    override fun getRealtimeCaptureLatency(): Pair<Long, Long> = Pair(1000L, 10L)
                }
            val advancedSessionProcessor =
                AdvancedSessionProcessor(
                    fakeSessionProcessImpl,
                    emptyList(),
                    object : VendorExtender {},
                    context
                )

            val realtimeCaptureLatencyEstimate = advancedSessionProcessor.realtimeCaptureLatency

            assertThat(realtimeCaptureLatencyEstimate?.first).isEqualTo(1000L)
            assertThat(realtimeCaptureLatencyEstimate?.second).isEqualTo(10L)
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun isCurrentExtensionTypeAvailableReturnsCorrectFalseValue() = runBlocking {
        assumeTrue(ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4))
        ClientVersion.setCurrentVersion(ClientVersion("1.4.0"))

        val advancedVendorExtender = AdvancedVendorExtender(FakeAdvancedExtenderImpl())

        val advancedSessionProcessor =
            AdvancedSessionProcessor(
                FakeSessionProcessImpl(),
                emptyList(),
                advancedVendorExtender,
                context
            )

        assertThat(advancedSessionProcessor.isCurrentExtensionModeAvailable).isFalse()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Suppress("UNCHECKED_CAST")
    @Test
    fun isCurrentExtensionTypeAvailableReturnsCorrectTrueValue() = runBlocking {
        assumeTrue(ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4))
        ClientVersion.setCurrentVersion(ClientVersion("1.4.0"))

        val advancedVendorExtender =
            AdvancedVendorExtender(
                FakeAdvancedExtenderImpl(
                    testCaptureResultKeys =
                        mutableListOf(
                            CaptureResult.EXTENSION_CURRENT_TYPE as CaptureResult.Key<Any>
                        )
                )
            )

        val advancedSessionProcessor =
            AdvancedSessionProcessor(
                FakeSessionProcessImpl(),
                emptyList(),
                advancedVendorExtender,
                context
            )

        assertThat(advancedSessionProcessor.isCurrentExtensionModeAvailable).isTrue()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun isExtensionStrengthAvailableReturnsCorrectFalseValue() = runBlocking {
        assumeTrue(ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4))
        ClientVersion.setCurrentVersion(ClientVersion("1.4.0"))

        val advancedVendorExtender = AdvancedVendorExtender(FakeAdvancedExtenderImpl())

        val advancedSessionProcessor =
            AdvancedSessionProcessor(
                FakeSessionProcessImpl(),
                emptyList(),
                advancedVendorExtender,
                context
            )

        assertThat(advancedSessionProcessor.isExtensionStrengthAvailable).isFalse()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Suppress("UNCHECKED_CAST")
    @Test
    fun isExtensionStrengthAvailableReturnsCorrectTrueValue() = runBlocking {
        assumeTrue(ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4))
        ClientVersion.setCurrentVersion(ClientVersion("1.4.0"))

        val advancedVendorExtender =
            AdvancedVendorExtender(
                FakeAdvancedExtenderImpl(
                    testCaptureRequestKeys =
                        mutableListOf(CaptureRequest.EXTENSION_STRENGTH as CaptureRequest.Key<Any>)
                )
            )

        val advancedSessionProcessor =
            AdvancedSessionProcessor(
                FakeSessionProcessImpl(),
                emptyList(),
                advancedVendorExtender,
                context
            )

        assertThat(advancedSessionProcessor.isExtensionStrengthAvailable).isTrue()
    }

    private class FakeAdvancedExtenderImpl(
        val testCaptureRequestKeys: MutableList<CaptureRequest.Key<Any>> = mutableListOf(),
        val testCaptureResultKeys: MutableList<CaptureResult.Key<Any>> = mutableListOf(),
    ) : AdvancedExtenderImpl {
        override fun isExtensionAvailable(
            cameraId: String,
            characteristicsMap: MutableMap<String, CameraCharacteristics>
        ): Boolean = true

        override fun init(
            cameraId: String,
            characteristicsMap: MutableMap<String, CameraCharacteristics>
        ) {}

        override fun getEstimatedCaptureLatencyRange(
            cameraId: String,
            captureOutputSize: Size?,
            imageFormat: Int
        ): Range<Long>? = null

        override fun getSupportedPreviewOutputResolutions(
            cameraId: String
        ): MutableMap<Int, MutableList<Size>> = mutableMapOf()

        override fun getSupportedCaptureOutputResolutions(
            cameraId: String
        ): MutableMap<Int, MutableList<Size>> = mutableMapOf()

        override fun getSupportedPostviewResolutions(
            captureSize: Size
        ): MutableMap<Int, MutableList<Size>> = mutableMapOf()

        override fun getSupportedYuvAnalysisResolutions(cameraId: String): MutableList<Size>? = null

        override fun createSessionProcessor(): SessionProcessorImpl = FakeSessionProcessImpl()

        override fun getAvailableCaptureRequestKeys(): MutableList<CaptureRequest.Key<Any>> =
            testCaptureRequestKeys

        override fun getAvailableCaptureResultKeys(): MutableList<CaptureResult.Key<Any>> =
            testCaptureResultKeys

        override fun isCaptureProcessProgressAvailable(): Boolean = false

        override fun isPostviewAvailable(): Boolean = false
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun getCurrentExtensionType_advancedSessionProcessorMonitorSessionProcessorImplResults(): Unit =
        runBlocking {
            assumeTrue(ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4))
            ClientVersion.setCurrentVersion(ClientVersion("1.4.0"))

            val fakeSessionProcessImpl =
                object : SessionProcessorImpl by FakeSessionProcessImpl() {
                    private var repeatingCallback: SessionProcessorImpl.CaptureCallback? = null

                    override fun startRepeating(
                        callback: SessionProcessorImpl.CaptureCallback
                    ): Int {
                        repeatingCallback = callback
                        return 0
                    }

                    fun updateCurrentExtensionType(extensionType: Int) {
                        repeatingCallback!!.onCaptureCompleted(
                            0,
                            0,
                            mapOf(CaptureResult.EXTENSION_CURRENT_TYPE to extensionType)
                        )
                    }
                }
            val advancedSessionProcessor =
                AdvancedSessionProcessor(
                    fakeSessionProcessImpl,
                    emptyList(),
                    object : VendorExtender {
                        override fun isCurrentExtensionModeAvailable(): Boolean {
                            return true
                        }
                    },
                    context,
                    ExtensionMode.AUTO
                )

            // Starts repeating first to let fakeSessionProcessImpl obtain the
            // AdvancedSessionProcessor's SessionProcessorImplCaptureCallbackAdapter instance
            advancedSessionProcessor.startRepeating(object : SessionProcessor.CaptureCallback {})
            val receivedTypeList = mutableListOf<Int>()
            // Sets the count as 2 for receiving the initial extension type and the updated
            // FACE_RETOUCH type
            val countDownLatch = CountDownLatch(2)
            withContext(Dispatchers.Main) {
                advancedSessionProcessor.currentExtensionMode.observeForever {
                    receivedTypeList.add(it)
                    countDownLatch.countDown()
                }
            }
            // Updates the current extension type capture result with Camera2 FACE_RETOUCH mode
            fakeSessionProcessImpl.updateCurrentExtensionType(EXTENSION_FACE_RETOUCH)
            // Verifies the new type value is updated to the type LiveData
            assertThat(countDownLatch.await(200, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(receivedTypeList)
                .containsExactlyElementsIn(listOf(ExtensionMode.AUTO, ExtensionMode.FACE_RETOUCH))
        }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun setExtensionStrength_advancedSessionProcessorInvokesSessionProcessorImpl() = runBlocking {
        assumeTrue(ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4))
        ClientVersion.setCurrentVersion(ClientVersion("1.4.0"))
        val setParametersLatch = CountDownLatch(1)
        val startRepeatingLatch = CountDownLatch(1)

        val fakeSessionProcessImpl =
            object : SessionProcessorImpl by FakeSessionProcessImpl() {
                private val UNKNOWN_STRENGTH = -1
                private var strength = UNKNOWN_STRENGTH

                override fun setParameters(parameters: MutableMap<CaptureRequest.Key<*>, Any>) {
                    if (parameters.containsKey(CaptureRequest.EXTENSION_STRENGTH)) {
                        strength = parameters[CaptureRequest.EXTENSION_STRENGTH] as Int
                        setParametersLatch.countDown()
                    }
                }

                override fun startRepeating(callback: SessionProcessorImpl.CaptureCallback): Int {
                    if (strength != UNKNOWN_STRENGTH) {
                        // Updates the new strength result value to onCaptureCompleted
                        callback.onCaptureCompleted(
                            0,
                            0,
                            mapOf(CaptureResult.EXTENSION_STRENGTH to strength)
                        )
                    }
                    startRepeatingLatch.countDown()
                    return 0
                }
            }
        val advancedSessionProcessor =
            AdvancedSessionProcessor(
                fakeSessionProcessImpl,
                emptyList(),
                object : VendorExtender {
                    override fun isExtensionStrengthAvailable(): Boolean {
                        return true
                    }
                },
                context
            )

        // Starts repeating first to verify that setExtensionStrength function will directly
        // invoke the SessionProcessImpl#startRepeating function.
        advancedSessionProcessor.startRepeating(object : SessionProcessor.CaptureCallback {})
        val newExtensionStrength = 50
        advancedSessionProcessor.setExtensionStrength(newExtensionStrength)
        // Verifies that setExtensionStrength will invoke the SessionProcessImpl#setParameters
        // function.
        assertThat(setParametersLatch.await(200, TimeUnit.MILLISECONDS)).isTrue()
        // Verifies that SessionProcessImpl#startRepeating function is invoked to apply the new
        // strength value.
        assertThat(startRepeatingLatch.await(200, TimeUnit.MILLISECONDS)).isTrue()
        // Verifies the new strength value is updated to the strength LiveData
        val expectedStrengthLatch = CountDownLatch(1)
        withContext(Dispatchers.Main) {
            advancedSessionProcessor.extensionStrength.observeForever {
                if (it == newExtensionStrength) {
                    expectedStrengthLatch.countDown()
                }
            }
        }
        assertThat(expectedStrengthLatch.await(200, TimeUnit.MILLISECONDS)).isTrue()
    }

    @RequiresApi(28)
    private suspend fun assumeAllowsSharedSurface() =
        withContext(Dispatchers.Main) {
            val imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2)
            val outputConfiguration = OutputConfiguration(imageReader.surface)
            val maxSharedSurfaceCount = outputConfiguration.maxSharedSurfaceCount
            imageReader.close()

            val camera = cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector)
            val cameraCharacteristics =
                (camera.cameraInfo as CameraInfoInternal).cameraCharacteristics
                    as CameraCharacteristics

            val hardwareLevel =
                cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            assumeTrue(
                maxSharedSurfaceCount > 1 &&
                    hardwareLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
            )
        }

    // Surface sharing of YUV format is supported after API 28.
    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun useCasesCanWork_hasSharedSurfaceOutput() = runBlocking {
        assumeAllowsSharedSurface()
        var sharedConfigId = -1
        val latchSharedSurfaceOutput = CountDownLatch(1)
        val fakeSessionProcessImpl =
            FakeSessionProcessImpl(
                // Directly use output surface
                previewConfigBlock = { outputSurfaceImpl ->
                    Camera2OutputConfigImplBuilder.newSurfaceConfig(outputSurfaceImpl.surface!!)
                        .build()
                },
                // Directly use output surface
                captureConfigBlock = { outputSurfaceImpl ->
                    Camera2OutputConfigImplBuilder.newSurfaceConfig(outputSurfaceImpl.surface!!)
                        .build()
                },
                // Directly use output surface with shared ImageReader surface.
                analysisConfigBlock = { outputSurfaceImpl ->
                    val sharedConfig =
                        Camera2OutputConfigImplBuilder.newImageReaderConfig(
                                outputSurfaceImpl.size,
                                outputSurfaceImpl.imageFormat,
                                2
                            )
                            .build()
                    sharedConfigId = sharedConfig.id

                    Camera2OutputConfigImplBuilder.newSurfaceConfig(outputSurfaceImpl.surface!!)
                        .addSurfaceSharingOutputConfig(sharedConfig)
                        .build()
                },
                onCaptureSessionStarted = {
                    it.setImageProcessor(sharedConfigId) { _, _, imageReference, _ ->
                        imageReference.decrement()
                        latchSharedSurfaceOutput.countDown()
                    }
                }
            )
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()

        verifyUseCasesOutput(fakeSessionProcessImpl, preview, imageCapture, imageAnalysis)
        assertThat(latchSharedSurfaceOutput.await(3, TimeUnit.SECONDS)).isTrue()
    }

    private fun getPhysicalCameraId(cameraSelector: CameraSelector): List<String> {
        val cameraInfos = cameraSelector.filter(cameraProvider.availableCameraInfos)
        if (cameraInfos.isEmpty()) {
            return emptyList()
        }
        return CameraUtil.getPhysicalCameraIds((cameraInfos.get(0) as CameraInfoInternal).cameraId)
    }

    // Test if physicalCameraId is set and returned in the image received in the image processor.
    @SdkSuppress(minSdkVersion = 28) // physical camera id is supported in API28+
    @Test
    fun useCasesCanWork_setPhysicalCameraId() = runBlocking {
        assumeAllowsSharedSurface()
        // Physical CameraId doesn't work on OnePlus
        assumeFalse(Build.BRAND.uppercase().equals("ONEPLUS"))
        val physicalCameraIdList = getPhysicalCameraId(cameraSelector)
        assumeTrue(physicalCameraIdList.isNotEmpty())

        val physicalCameraId = physicalCameraIdList[0]
        var analysisOutputSurface: Surface? = null
        var sharedConfigId = -1
        var intermediaConfigId = -1
        val deferredImagePhysicalCameraId = CompletableDeferred<String?>()
        val deferredSharedImagePhysicalCameraId = CompletableDeferred<String?>()

        val fakeSessionProcessImpl =
            FakeSessionProcessImpl(
                // Directly use output surface
                previewConfigBlock = { outputSurfaceImpl ->
                    Camera2OutputConfigImplBuilder.newSurfaceConfig(outputSurfaceImpl.surface!!)
                        .setPhysicalCameraId(physicalCameraId)
                        .build()
                },
                // Directly use output surface
                captureConfigBlock = {
                    Camera2OutputConfigImplBuilder.newSurfaceConfig(it.surface!!).build()
                },
                // Has intermediate image reader to process YUV
                analysisConfigBlock = { outputSurfaceImpl ->
                    analysisOutputSurface = outputSurfaceImpl.surface
                    val sharedConfig =
                        Camera2OutputConfigImplBuilder.newImageReaderConfig(
                                outputSurfaceImpl.size,
                                outputSurfaceImpl.imageFormat,
                                2
                            )
                            .setPhysicalCameraId(physicalCameraId)
                            .build()
                            .also { sharedConfigId = it.id }

                    Camera2OutputConfigImplBuilder.newImageReaderConfig(
                            outputSurfaceImpl.size,
                            ImageFormat.YUV_420_888,
                            2
                        )
                        .setPhysicalCameraId(physicalCameraId)
                        .addSurfaceSharingOutputConfig(sharedConfig)
                        .build()
                        .also { intermediaConfigId = it.id }
                },
                onCaptureSessionStarted = { requestProcessor ->
                    val imageWriter = ImageWriter.newInstance(analysisOutputSurface!!, 2)
                    requestProcessor.setImageProcessor(intermediaConfigId) {
                        _,
                        _,
                        image,
                        physicalCameraIdOfImage ->
                        deferredImagePhysicalCameraId.complete(physicalCameraIdOfImage)
                        val inputImage = imageWriter.dequeueInputImage()
                        imageWriter.queueInputImage(inputImage)
                        image.decrement()
                    }
                    requestProcessor.setImageProcessor(sharedConfigId) {
                        _,
                        _,
                        imageReference,
                        physicalCameraIdOfImage ->
                        imageReference.decrement()
                        deferredSharedImagePhysicalCameraId.complete(physicalCameraIdOfImage)
                    }
                }
            )

        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        verifyUseCasesOutput(fakeSessionProcessImpl, preview, imageCapture, imageAnalysis)
        assertThat(deferredImagePhysicalCameraId.awaitWithTimeout(2000)).isEqualTo(physicalCameraId)
        assertThat(deferredSharedImagePhysicalCameraId.awaitWithTimeout(2000))
            .isEqualTo(physicalCameraId)
    }

    private fun createOutputSurface(width: Int, height: Int, format: Int): OutputSurface {
        val captureImageReader = ImageReader.newInstance(width, height, format, 1)
        return OutputSurface.create(captureImageReader.surface, Size(width, height), format)
    }

    @Test
    fun canSetSessionTypeFromOemImpl() {
        assumeTrue(
            ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4) &&
                ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
        )
        // 1. Arrange.
        val sessionTypeToVerify = 4
        val fakeSessionProcessImpl = FakeSessionProcessImpl()
        fakeSessionProcessImpl.sessionType = sessionTypeToVerify
        val advancedSessionProcessor =
            AdvancedSessionProcessor(
                fakeSessionProcessImpl,
                emptyList(),
                object : VendorExtender {},
                context
            )
        val fakeCameraInfo = FakeCameraInfoInternal("0", context)
        val previewOutputSurface = createOutputSurface(640, 480, ImageFormat.YUV_420_888)
        val imageCaptureSurface = createOutputSurface(640, 480, ImageFormat.JPEG)

        // 2. Act.
        val sessionConfig =
            advancedSessionProcessor.initSession(
                fakeCameraInfo,
                OutputSurfaceConfiguration.create(
                    previewOutputSurface,
                    imageCaptureSurface,
                    null,
                    null
                )
            )

        // 3. Assert.
        assertThat(sessionConfig.sessionType).isEqualTo(sessionTypeToVerify)
    }

    @Test
    fun defaultSessionType() {
        // 1. Arrange.
        val fakeSessionProcessImpl = FakeSessionProcessImpl()
        fakeSessionProcessImpl.sessionType = -1
        val advancedSessionProcessor =
            AdvancedSessionProcessor(
                fakeSessionProcessImpl,
                emptyList(),
                object : VendorExtender {},
                context
            )
        val fakeCameraInfo = FakeCameraInfoInternal("0", context)
        val previewOutputSurface = createOutputSurface(640, 480, ImageFormat.YUV_420_888)
        val imageCaptureSurface = createOutputSurface(640, 480, ImageFormat.JPEG)

        // 2. Act.
        val sessionConfig =
            advancedSessionProcessor.initSession(
                fakeCameraInfo,
                OutputSurfaceConfiguration.create(
                    previewOutputSurface,
                    imageCaptureSurface,
                    null,
                    null
                )
            )

        // 3. Assert.
        assertThat(sessionConfig.sessionType).isEqualTo(SessionConfiguration.SESSION_REGULAR)
    }

    @Test
    fun getSupportedPostviewSizeIsCorrect() {
        // 1. Arrange
        val postviewSizes =
            mutableMapOf(ImageFormat.JPEG to listOf(Size(1920, 1080), Size(640, 480)))
        val vendorExtender =
            object : VendorExtender {
                override fun getSupportedPostviewResolutions(captureSize: Size) = postviewSizes
            }
        val advancedSessionProcessor =
            AdvancedSessionProcessor(FakeSessionProcessImpl(), emptyList(), vendorExtender, context)

        // 2. Act and Assert
        assertThat(
                advancedSessionProcessor
                    .getSupportedPostviewSize(Size(1920, 1080))
                    .get(ImageFormat.JPEG)
            )
            .containsExactly(Size(1920, 1080), Size(640, 480))
    }

    /**
     * Verify if the given use cases have expected output.
     * 1) Preview frame is received
     * 2) imageCapture gets a captured JPEG image
     * 3) imageAnalysis gets a Image in Analyzer.
     */
    private suspend fun verifyUseCasesOutput(
        fakeSessionProcessImpl: FakeSessionProcessImpl,
        preview: Preview,
        imageCapture: ImageCapture,
        imageAnalysis: ImageAnalysis? = null
    ) {
        val advancedSessionProcessor =
            AdvancedSessionProcessor(
                fakeSessionProcessImpl,
                emptyList(),
                object : VendorExtender {},
                context
            )
        val latchPreviewFrame = CountDownLatch(1)
        val latchAnalysis = CountDownLatch(1)
        val deferCapturedImage = CompletableDeferred<ImageProxy>()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createSurfaceTextureProvider(
                    object : SurfaceTextureCallback {
                        override fun onSurfaceTextureReady(
                            surfaceTexture: SurfaceTexture,
                            resolution: Size
                        ) {
                            surfaceTexture.setOnFrameAvailableListener {
                                latchPreviewFrame.countDown()
                            }
                        }

                        override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                            surfaceTexture.release()
                        }
                    }
                )
            )
            imageAnalysis?.setAnalyzer(CameraXExecutors.mainThreadExecutor()) {
                it.close()
                latchAnalysis.countDown()
            }
            val cameraSelector =
                getCameraSelectorWithSessionProcessor(cameraSelector, advancedSessionProcessor)

            val useCaseGroupBuilder = UseCaseGroup.Builder()
            useCaseGroupBuilder.addUseCase(preview)
            useCaseGroupBuilder.addUseCase(imageCapture)
            imageAnalysis?.let { useCaseGroupBuilder.addUseCase(it) }

            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                cameraSelector,
                useCaseGroupBuilder.build()
            )
        }

        imageCapture.takePicture(
            CameraXExecutors.mainThreadExecutor(),
            object : OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    deferCapturedImage.complete(image)
                }
            }
        )

        assertThat(latchPreviewFrame.await(3, TimeUnit.SECONDS)).isTrue()
        assertThat(deferCapturedImage.awaitWithTimeout(5000).image!!.format)
            .isEqualTo(ImageFormat.JPEG)
        imageAnalysis?.let { assertThat(latchAnalysis.await(3, TimeUnit.SECONDS)).isTrue() }
    }
}

private suspend fun <T> Deferred<T>.awaitWithTimeout(timeMillis: Long): T {
    return withTimeout(timeMillis) { await() }
}

/**
 * A fake [SessionProcessorImpl] where preview/imageCapture/imageAnalysis camera2OutputConfigImpl
 * can be customized via input blocks. onCaptureSessionStarted block can also be provided in order
 * to allow tests to access the [RequestProcessorImpl] to receive the Image for
 * [ImageReaderOutputConfigImpl].
 */
class FakeSessionProcessImpl(
    var previewConfigBlock: (OutputSurfaceImpl) -> Camera2OutputConfigImpl = { outputSurfaceImpl ->
        Camera2OutputConfigImplBuilder.newSurfaceConfig(outputSurfaceImpl.surface!!).build()
    },
    var captureConfigBlock: (OutputSurfaceImpl) -> Camera2OutputConfigImpl = { outputSurfaceImpl ->
        Camera2OutputConfigImplBuilder.newSurfaceConfig(outputSurfaceImpl.surface!!).build()
    },
    var analysisConfigBlock: ((OutputSurfaceImpl) -> Camera2OutputConfigImpl)? = null,
    var onCaptureSessionStarted: ((RequestProcessorImpl) -> Unit)? = null
) : SessionProcessorImpl {
    private var requestProcessor: RequestProcessorImpl? = null
    private var nextSequenceId = 0
    private lateinit var previewOutputConfig: Camera2OutputConfigImpl
    private lateinit var captureOutputConfig: Camera2OutputConfigImpl
    private var analysisOutputConfig: Camera2OutputConfigImpl? = null
    private var sharedOutputConfigList = arrayListOf<Camera2OutputConfigImpl>()
    private var startTriggerParametersDeferred =
        CompletableDeferred<MutableMap<CaptureRequest.Key<*>, Any>>()

    var sessionType: Int = -1

    override fun initSession(
        cameraId: String,
        cameraCharacteristicsMap: MutableMap<String, CameraCharacteristics>,
        context: Context,
        previewSurfaceConfig: OutputSurfaceImpl,
        captureSurfaceConfig: OutputSurfaceImpl,
        analysisSurfaceConfig: OutputSurfaceImpl?
    ): Camera2SessionConfigImpl {
        captureOutputConfig = captureConfigBlock.invoke(captureSurfaceConfig)
        previewOutputConfig = previewConfigBlock.invoke(previewSurfaceConfig)
        analysisSurfaceConfig?.let { analysisOutputConfig = analysisConfigBlock?.invoke(it) }

        captureOutputConfig.surfaceSharingOutputConfigs?.let { sharedOutputConfigList.addAll(it) }
        previewOutputConfig.surfaceSharingOutputConfigs?.let { sharedOutputConfigList.addAll(it) }
        analysisOutputConfig?.surfaceSharingOutputConfigs?.let { sharedOutputConfigList.addAll(it) }

        val sessionBuilder =
            Camera2SessionConfigImplBuilder().apply {
                addOutputConfig(previewOutputConfig)
                addOutputConfig(captureOutputConfig)
                analysisOutputConfig?.let { addOutputConfig(it) }
            }

        if (ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4) && sessionType != -1) {
            sessionBuilder.setSessionType(sessionType)
        }
        return sessionBuilder.build()
    }

    override fun initSession(
        cameraId: String,
        cameraCharacteristicsMap: MutableMap<String, CameraCharacteristics>,
        context: Context,
        surfaceConfigs: OutputSurfaceConfigurationImpl
    ): Camera2SessionConfigImpl {
        return initSession(
            cameraId,
            cameraCharacteristicsMap,
            context,
            surfaceConfigs.previewOutputSurface,
            surfaceConfigs.imageCaptureOutputSurface,
            surfaceConfigs.imageAnalysisOutputSurface
        )
    }

    override fun deInitSession() {}

    override fun setParameters(parameters: MutableMap<CaptureRequest.Key<*>, Any>) {}

    override fun startTrigger(
        triggers: MutableMap<CaptureRequest.Key<*>, Any>,
        callback: SessionProcessorImpl.CaptureCallback
    ): Int {
        startTriggerParametersDeferred.complete(triggers)
        return 0
    }

    suspend fun assertStartTriggerIsCalledWithParameters(
        parameters: MutableMap<CaptureRequest.Key<*>, Any>
    ) {
        assertThat(startTriggerParametersDeferred.awaitWithTimeout(1000)).isEqualTo(parameters)
    }

    override fun onCaptureSessionStart(requestProcessor: RequestProcessorImpl) {
        this.requestProcessor = requestProcessor
        onCaptureSessionStarted?.invoke(requestProcessor)
    }

    override fun onCaptureSessionEnd() {}

    override fun startRepeating(callback: SessionProcessorImpl.CaptureCallback): Int {
        val idList = ArrayList<Int>()
        idList.add(previewOutputConfig.id)
        analysisOutputConfig?.let { idList.add(it.id) }
        for (sharedConfig in sharedOutputConfigList) {
            idList.add(sharedConfig.id)
        }

        val currentSequenceId = nextSequenceId++
        val request = RequestProcessorRequest(idList, mapOf(), CameraDevice.TEMPLATE_PREVIEW)
        requestProcessor!!.setRepeating(
            request,
            object : RequestProcessorImpl.Callback {
                override fun onCaptureStarted(
                    request: RequestProcessorImpl.Request,
                    frameNumber: Long,
                    timestamp: Long
                ) {
                    callback.onCaptureStarted(currentSequenceId, timestamp)
                }

                override fun onCaptureProgressed(
                    request: RequestProcessorImpl.Request,
                    partialResult: CaptureResult
                ) {}

                override fun onCaptureCompleted(
                    request: RequestProcessorImpl.Request,
                    totalCaptureResult: TotalCaptureResult
                ) {
                    callback.onCaptureProcessStarted(currentSequenceId)
                    callback.onCaptureCompleted(
                        totalCaptureResult.get(CaptureResult.SENSOR_TIMESTAMP)!!,
                        currentSequenceId,
                        mapOf()
                    )
                    callback.onCaptureSequenceCompleted(currentSequenceId)
                }

                override fun onCaptureFailed(
                    request: RequestProcessorImpl.Request,
                    captureFailure: CaptureFailure
                ) {
                    callback.onCaptureFailed(currentSequenceId)
                    callback.onCaptureSequenceAborted(currentSequenceId)
                }

                override fun onCaptureBufferLost(
                    request: RequestProcessorImpl.Request,
                    frameNumber: Long,
                    outputStreamId: Int
                ) {}

                override fun onCaptureSequenceCompleted(sequenceId: Int, frameNumber: Long) {}

                override fun onCaptureSequenceAborted(sequenceId: Int) {}
            }
        )
        return currentSequenceId
    }

    override fun stopRepeating() {
        requestProcessor?.stopRepeating()
    }

    override fun startCapture(callback: SessionProcessorImpl.CaptureCallback): Int {
        val idList = ArrayList<Int>()
        idList.add(captureOutputConfig.id)

        val currentSequenceId = nextSequenceId++
        val request = RequestProcessorRequest(idList, mapOf(), CameraDevice.TEMPLATE_STILL_CAPTURE)
        requestProcessor?.submit(
            request,
            object : RequestProcessorImpl.Callback {
                override fun onCaptureStarted(
                    request: RequestProcessorImpl.Request,
                    frameNumber: Long,
                    timestamp: Long
                ) {
                    callback.onCaptureStarted(currentSequenceId, timestamp)
                }

                override fun onCaptureProgressed(
                    request: RequestProcessorImpl.Request,
                    partialResult: CaptureResult
                ) {}

                override fun onCaptureCompleted(
                    request: RequestProcessorImpl.Request,
                    totalCaptureResult: TotalCaptureResult
                ) {
                    callback.onCaptureCompleted(
                        totalCaptureResult.get(CaptureResult.SENSOR_TIMESTAMP)!!,
                        currentSequenceId,
                        mapOf()
                    )
                }

                override fun onCaptureFailed(
                    request: RequestProcessorImpl.Request,
                    captureFailure: CaptureFailure
                ) {
                    callback.onCaptureFailed(currentSequenceId)
                }

                override fun onCaptureBufferLost(
                    request: RequestProcessorImpl.Request,
                    frameNumber: Long,
                    outputStreamId: Int
                ) {}

                override fun onCaptureSequenceCompleted(sequenceId: Int, frameNumber: Long) {
                    callback.onCaptureSequenceCompleted(currentSequenceId)
                }

                override fun onCaptureSequenceAborted(sequenceId: Int) {
                    callback.onCaptureSequenceAborted(currentSequenceId)
                }
            }
        )
        return currentSequenceId
    }

    override fun startCaptureWithPostview(callback: SessionProcessorImpl.CaptureCallback): Int {
        return startCapture(callback)
    }

    override fun getRealtimeCaptureLatency(): Pair<Long, Long>? {
        return null
    }

    override fun abortCapture(captureSequenceId: Int) {
        requestProcessor?.abortCaptures()
    }
}

class RequestProcessorRequest(
    private val targetOutputConfigIds: List<Int>,
    private val parameters: Map<CaptureRequest.Key<*>, Any>,
    private val templateId: Int
) : RequestProcessorImpl.Request {
    override fun getTargetOutputConfigIds(): List<Int> {
        return targetOutputConfigIds
    }

    override fun getParameters(): Map<CaptureRequest.Key<*>, Any> {
        return parameters
    }

    override fun getTemplateId(): Int {
        return templateId
    }
}
