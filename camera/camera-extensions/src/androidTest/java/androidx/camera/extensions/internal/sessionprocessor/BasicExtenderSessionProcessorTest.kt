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

package androidx.camera.extensions.internal.sessionprocessor

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageWriter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Pair
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageReaderProxys
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.OutputSurface
import androidx.camera.core.impl.RequestProcessor
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.utils.Exif
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.impl.CaptureProcessorImpl
import androidx.camera.extensions.impl.CaptureStageImpl
import androidx.camera.extensions.impl.ExtenderStateListener
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl
import androidx.camera.extensions.impl.PreviewExtenderImpl
import androidx.camera.extensions.impl.PreviewExtenderImpl.ProcessorType
import androidx.camera.extensions.impl.PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR
import androidx.camera.extensions.impl.PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_NONE
import androidx.camera.extensions.impl.PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_REQUEST_UPDATE_ONLY
import androidx.camera.extensions.impl.PreviewImageProcessorImpl
import androidx.camera.extensions.impl.ProcessResultImpl
import androidx.camera.extensions.impl.RequestUpdateProcessorImpl
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.concurrent.futures.await
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@SdkSuppress(minSdkVersion = 29) // Extensions supported on API 29+
@RunWith(Parameterized::class)
class BasicExtenderSessionProcessorTest(
    private val hasCaptureProcessor: Boolean,
    private val previewProcessorType: ProcessorType
) {
    companion object {
        @Parameterized.Parameters(name = "hasCaptureProcessor = {0}, previewProcessorType = {1}")
        @JvmStatic
        fun parameters() = listOf(
            arrayOf(false /* No CaptureProcessor */, PROCESSOR_TYPE_NONE),
            arrayOf(true /* Has CaptureProcessor */, PROCESSOR_TYPE_NONE),
            arrayOf(false /* No CaptureProcessor */, PROCESSOR_TYPE_REQUEST_UPDATE_ONLY),
            arrayOf(true /* Has CaptureProcessor */, PROCESSOR_TYPE_REQUEST_UPDATE_ONLY),
            arrayOf(false /* No CaptureProcessor */, PROCESSOR_TYPE_IMAGE_PROCESSOR),
            arrayOf(true /* Has CaptureProcessor */, PROCESSOR_TYPE_IMAGE_PROCESSOR)
        )

        private fun createCaptureStage(
            id: Int = 0,
            parameters: List<Pair<CaptureRequest.Key<*>, Any>> = mutableListOf()
        ): CaptureStageImpl {
            return object : CaptureStageImpl {
                override fun getId() = id
                override fun getParameters() = parameters
            }
        }
    }

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var fakePreviewExtenderImpl: FakePreviewExtenderImpl
    private lateinit var fakeCaptureExtenderImpl: FakeImageCaptureExtenderImpl
    private lateinit var basicExtenderSessionProcessor: BasicExtenderSessionProcessor

    @Before
    fun setUp() = runBlocking {
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
        }

        if (hasCaptureProcessor || previewProcessorType == PROCESSOR_TYPE_IMAGE_PROCESSOR) {
            assumeTrue(areYuvYuvYuvAndYuvYuvPrivateSupported())
        }

        fakePreviewExtenderImpl = FakePreviewExtenderImpl(previewProcessorType)
        fakeCaptureExtenderImpl = FakeImageCaptureExtenderImpl(hasCaptureProcessor)
        basicExtenderSessionProcessor = BasicExtenderSessionProcessor(
            fakePreviewExtenderImpl, fakeCaptureExtenderImpl, context
        )
    }

    private suspend fun areYuvYuvYuvAndYuvYuvPrivateSupported(): Boolean {
        if (Build.BRAND.equals("SAMSUNG", ignoreCase = true)) {
            return true
        }
        val camera = withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector)
        }
        val hardwareLevel = Camera2CameraInfo.from(camera.cameraInfo).getCameraCharacteristic(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
        )

        return hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 ||
            hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
    }

    @After
    fun tearDown() = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.shutdown()[10, TimeUnit.SECONDS]
            }
        }
    }

    @Test
    fun canOutputCorrectly(): Unit = runBlocking {
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        val previewSemaphore = Semaphore(0)
        val analysisSemaphore = Semaphore(0)
        verifyUseCasesOutput(
            preview,
            imageCapture,
            imageAnalysis,
            previewSemaphore,
            analysisSemaphore
        )
    }

    @Test
    fun imageCaptureError(): Unit = runBlocking {
        assumeTrue(hasCaptureProcessor)
        fakeCaptureExtenderImpl = FakeImageCaptureExtenderImpl(
            hasCaptureProcessor, throwErrorOnProcess = true
        )
        basicExtenderSessionProcessor = BasicExtenderSessionProcessor(
            fakePreviewExtenderImpl, fakeCaptureExtenderImpl, context
        )
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        assertThrows<ImageCaptureException> {
            verifyUseCasesOutput(preview, imageCapture, imageAnalysis)
        }
    }

    @Test
    fun canOutputCorrectly_withoutAnalysis(): Unit = runBlocking {
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val previewSemaphore = Semaphore(0)
        verifyUseCasesOutput(
            preview = preview,
            imageCapture = imageCapture,
            previewFrameSemaphore = previewSemaphore
        )
    }

    suspend fun getSensorRotationDegrees(rotation: Int): Int {
        return withContext(Dispatchers.Main) {
            val camera = cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector)
            camera.cameraInfo.getSensorRotationDegrees(rotation)
        }
    }

    @Test
    fun canOutputCorrectly_setTargetRotation(): Unit = runBlocking {
        assumeTrue(hasCaptureProcessor)
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .build()
        val previewSemaphore = Semaphore(0)
        verifyUseCasesOutput(
            preview = preview,
            imageCapture = imageCapture,
            previewFrameSemaphore = previewSemaphore,
            expectedExifRotation = getSensorRotationDegrees(Surface.ROTATION_0)
        )
    }

    @Test
    fun canOutputCorrectlyAfterStopStart(): Unit = runBlocking {
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        val previewSemaphore = Semaphore(0)
        val analysisSemaphore = Semaphore(0)

        verifyUseCasesOutput(
            preview,
            imageCapture,
            imageAnalysis,
            previewSemaphore,
            analysisSemaphore
        )

        fakeLifecycleOwner.pauseAndStop()

        delay(1000)
        previewSemaphore.drainPermits()
        analysisSemaphore.drainPermits()
        fakeLifecycleOwner.startAndResume()

        assertThat(previewSemaphore.tryAcquire(3, TimeUnit.SECONDS)).isTrue()

        imageAnalysis.let {
            assertThat(analysisSemaphore.tryAcquire(3, TimeUnit.SECONDS)).isTrue()
        }

        verifyStillCapture(imageCapture)
    }

    @Test
    fun canInvokeEventsInOrder(): Unit = runBlocking {
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        val previewSemaphore = Semaphore(0)
        val analysisSemaphore = Semaphore(0)
        val camera = verifyUseCasesOutput(
            preview,
            imageCapture,
            imageAnalysis,
            previewSemaphore,
            analysisSemaphore
        )

        val cameraClosedLatch = CountDownLatch(1)
        withContext(Dispatchers.Main) {
            camera.cameraInfo.cameraState.observeForever(object : Observer<CameraState?> {
                override fun onChanged(value: CameraState?) {
                    if (value?.type == CameraState.Type.CLOSED) {
                        cameraClosedLatch.countDown()
                        camera.cameraInfo.cameraState.removeObserver(this)
                    }
                }
            })
        }

        fakeLifecycleOwner.pauseAndStop()
        assertThat(cameraClosedLatch.await(1, TimeUnit.SECONDS)).isTrue()

        fakeCaptureExtenderImpl.assertInvokeOrder(listOf(
            "onInit",
            "onPresetSession",
            "onEnableSession",
            "onDisableSession",
            "onDeInit",
        ))

        fakePreviewExtenderImpl.assertInvokeOrder(listOf(
            "onInit",
            "onPresetSession",
            "onEnableSession",
            "onDisableSession",
            "onDeInit",
        ))
    }

    class ResultMonitor {
        private var latch: CountDownLatch? = null
        private var keyToCheck: CaptureRequest.Key<*>? = null
        private var valueToCheck: Any? = null

        fun onCaptureRequestReceived(captureRequest: CaptureRequest) {
            if (latch != null) {
                keyToCheck?.let {
                    if (captureRequest.get(keyToCheck) == valueToCheck) {
                        latch!!.countDown()
                    }
                }
            }
        }
        fun assertCaptureKey(key: CaptureRequest.Key<*>, value: Any) {
            keyToCheck = key
            valueToCheck = value
            latch = CountDownLatch(1)
            assertThat(latch!!.await(3, TimeUnit.SECONDS)).isTrue()
        }
    }

    @Test
    fun repeatingRequest_containsPreviewCaptureStagesParameters(): Unit = runBlocking {
        val previewBuilder = Preview.Builder()
        val resultMonitor = ResultMonitor()
        Camera2Interop.Extender(previewBuilder)
            .setSessionCaptureCallback(object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    resultMonitor.onCaptureRequestReceived(request)
                }
            })
        val preview = previewBuilder.build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        val previewSemaphore = Semaphore(0)
        val analysisSemaphore = Semaphore(0)
        fakePreviewExtenderImpl.captureStage = createCaptureStage(
            parameters = listOf(Pair(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF
            ))
        )

        verifyUseCasesOutput(
            preview,
            imageCapture,
            imageAnalysis,
            previewSemaphore,
            analysisSemaphore
        )

        resultMonitor.assertCaptureKey(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_OFF
        )
    }

    @Test
    fun processorRequestUpdateOnly_canUpdateRepeating(): Unit = runBlocking {
        assumeTrue(previewProcessorType == PROCESSOR_TYPE_REQUEST_UPDATE_ONLY)
        val previewBuilder = Preview.Builder()
        val resultMonitor = ResultMonitor()
        Camera2Interop.Extender(previewBuilder)
            .setSessionCaptureCallback(object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    resultMonitor.onCaptureRequestReceived(request)
                }
            })
        val preview = previewBuilder.build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        val previewSemaphore = Semaphore(0)
        val analysisSemaphore = Semaphore(0)
        verifyUseCasesOutput(
            preview,
            imageCapture,
            imageAnalysis,
            previewSemaphore,
            analysisSemaphore
        )

        // Trigger RequestUpdateProcessor to update repeating request to have new parameters.
        fakePreviewExtenderImpl.fakeRequestUpdateProcessor?.captureStage =
            createCaptureStage(
                parameters = listOf(Pair(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF)
                )
            )

        resultMonitor.assertCaptureKey(
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_OFF
        )
    }

    @Test
    fun imageCapture_captureRequestParametersAreCorrect(): Unit = runBlocking {
        initBasicExtenderSessionProcessor().use {
            fakeCaptureExtenderImpl.captureStages = listOf(
                createCaptureStage(
                    0, listOf(
                        Pair(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
                    )
                ),
                createCaptureStage(
                    1, listOf(
                        Pair(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                    )
                ),
            )
            fakePreviewExtenderImpl.captureStage = createCaptureStage(
                0, listOf(
                    Pair(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                )
            )

            val fakeRequestProcessor = FakeRequestProcessor()
            basicExtenderSessionProcessor.onCaptureSessionStart(fakeRequestProcessor)

            basicExtenderSessionProcessor.startRepeating(object :
                SessionProcessor.CaptureCallback {})
            basicExtenderSessionProcessor.startCapture(object : SessionProcessor.CaptureCallback {})

            val submittedRequests = withTimeout(2000) {
                fakeRequestProcessor.awaitRequestSubmitted()
            }
            assertThat(submittedRequests.size).isEqualTo(2)
            val submittedRequestParameter0 = submittedRequests[0].toParametersList()
            val submittedRequestParameter1 = submittedRequests[1].toParametersList()

            // Capture request parameters should include both Image capture capture stage and
            // preview capture stage.
            assertThat(submittedRequestParameter0).containsExactly(
                // Set by image capture CaptureStageImpl "0"
                Pair(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF),
                // Set by preview getCaptureStage()
                Pair(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            )

            assertThat(submittedRequestParameter1).containsExactly(
                // Set by image capture CaptureStageImpl "1"
                Pair(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF),
                // Set by preview getCaptureStage()
                Pair(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            )
        }
    }

    @Test
    fun onEnableDisableRequestsAreSent(): Unit = runBlocking {
        initBasicExtenderSessionProcessor().use {
            // Verify onEnableSession
            fakePreviewExtenderImpl.onEnableSessionCaptureStage = createCaptureStage(
                0, listOf(
                    Pair(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                )
            )

            fakeCaptureExtenderImpl.onEnableSessionCaptureStage = createCaptureStage(
                0, listOf(
                    Pair(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                )
            )

            val fakeRequestProcessor = FakeRequestProcessor()
            basicExtenderSessionProcessor.onCaptureSessionStart(fakeRequestProcessor)

            val onEnableSessionRequest = withTimeout(2000) {
                fakeRequestProcessor.awaitRequestSubmitted()
            }
            assertThat(onEnableSessionRequest.size).isEqualTo(2)
            assertThat(onEnableSessionRequest[0].toParametersList()).containsExactly(
                Pair(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF),
            )
            assertThat(onEnableSessionRequest[1].toParametersList()).containsExactly(
                Pair(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF),
            )

            // Verify onDisableSession
            fakePreviewExtenderImpl.onDisableSessionCaptureStage = createCaptureStage(
                0, listOf(
                    Pair(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
                )
            )
            fakeCaptureExtenderImpl.onDisableSessionCaptureStage = createCaptureStage(
                0, listOf(
                    Pair(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                )
            )
            basicExtenderSessionProcessor.onCaptureSessionEnd()
            val onDisableSessionRequest = withTimeout(2000) {
                fakeRequestProcessor.awaitRequestSubmitted()
            }
            assertThat(onDisableSessionRequest.size).isEqualTo(2)
            assertThat(onDisableSessionRequest[0].toParametersList()).containsExactly(
                Pair(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF),
            )
            assertThat(onDisableSessionRequest[1].toParametersList()).containsExactly(
                Pair(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF),
            )
        }
    }

    private suspend fun initBasicExtenderSessionProcessor(): AutoCloseable {
        val width = 640
        val height = 480
        val maxImages = 2
        val cameraInfo = cameraProvider.availableCameraInfos[0]
        val handlerThread = HandlerThread("CameraX-AutoDrainThread")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        val surfaceTextureHolder = SurfaceTextureProvider.createAutoDrainingSurfaceTextureAsync(
            CameraXExecutors.newHandlerExecutor(handler), width, height, null
        ) { handlerThread.quitSafely() }.await()
        val previewOutputSurface = OutputSurface.create(
            Surface(surfaceTextureHolder.surfaceTexture),
            Size(width, height),
            ImageFormat.PRIVATE
        )
        val jpegImageReader =
            ImageReaderProxys.createIsolatedReader(width, height, ImageFormat.JPEG, maxImages)
        val captureOutputSurface = OutputSurface.create(
            jpegImageReader.surface!!,
            Size(width, height),
            ImageFormat.JPEG
        )

        basicExtenderSessionProcessor.initSession(
            cameraInfo,
            previewOutputSurface,
            captureOutputSurface,
            null
        )

        return AutoCloseable {
            jpegImageReader.close()
            surfaceTextureHolder.close()
        }
    }

    private fun RequestProcessor.Request.toParametersList():
        List<Pair<CaptureRequest.Key<Any>?, Any?>> {
        val submittedRequestParameter1 = parameters.listOptions().map {
            @Suppress("UNCHECKED_CAST")
            val key = it.token as CaptureRequest.Key<Any>?
            val value = parameters.retrieveOption(it)
            Pair(key, value)
        }
        return submittedRequestParameter1
    }

    /**
     * Verify if the given use cases have expected output.
     * 1) Preview frame is received
     * 2) imageCapture gets a captured JPEG image
     * 3) imageAnalysis gets a Image in Analyzer.
     */
    private suspend fun verifyUseCasesOutput(
        preview: Preview,
        imageCapture: ImageCapture,
        imageAnalysis: ImageAnalysis? = null,
        previewFrameSemaphore: Semaphore? = null,
        analysisSemaphore: Semaphore? = null,
        expectedExifRotation: Int = 0,
    ): Camera {
        val camera =
            withContext(Dispatchers.Main) {
                preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                        if (previewFrameSemaphore?.availablePermits() == 0) {
                            previewFrameSemaphore.release()
                        }
                    }
                )
                imageAnalysis?.setAnalyzer(CameraXExecutors.mainThreadExecutor()) {
                    it.close()
                    if (analysisSemaphore?.availablePermits() == 0) {
                        analysisSemaphore.release()
                    }
                }
                val cameraSelector =
                    getCameraSelectorWithSessionProcessor(
                        cameraSelector,
                        basicExtenderSessionProcessor
                    )

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

        previewFrameSemaphore?.let {
            assertThat(it.tryAcquire(3, TimeUnit.SECONDS)).isTrue()
        }

        analysisSemaphore?.let {
            assertThat(analysisSemaphore.tryAcquire(3, TimeUnit.SECONDS)).isTrue()
        }

        verifyStillCapture(imageCapture, expectedExifRotation)
        return camera
    }

    private suspend fun verifyStillCapture(
        imageCapture: ImageCapture,
        expectExifRotation: Int = 0
    ) {
        val deferCapturedImage = CompletableDeferred<ImageProxy>()
        imageCapture.takePicture(
            CameraXExecutors.mainThreadExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    deferCapturedImage.complete(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    deferCapturedImage.completeExceptionally(exception)
                }
            })
        withTimeout(6000) {
            deferCapturedImage.await().use {
                assertThat(it.format).isEqualTo(ImageFormat.JPEG)
                if (expectExifRotation != 0) {
                    val exif = Exif.createFromImageProxy(it)
                    assertThat(exif.rotation).isEqualTo(expectExifRotation)
                }
            }
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
                ): SessionProcessor {
                    return sessionProcessor
                }

                override fun getSessionProcessor(): SessionProcessor {
                    return sessionProcessor
                }
            }
        }
        val builder = CameraSelector.Builder.fromSelector(cameraSelector)
        builder.addCameraFilter(object : CameraFilter {
            override fun filter(cameraInfos: MutableList<CameraInfo>): MutableList<CameraInfo> {
                val newCameraInfos = mutableListOf<CameraInfo>()
                newCameraInfos.addAll(cameraInfos)
                return newCameraInfos
            }

            override fun getIdentifier(): Identifier {
                return identifier
            }
        })
        return builder.build()
    }

    open class FakeExtenderStateListener : ExtenderStateListener {
        private val invokeList = mutableListOf<String>()
        fun recordInvoking(action: String) {
            invokeList.add(action)
        }

        fun assertInvokeOrder(expectList: List<String>) {
            assertThat(expectList).containsExactlyElementsIn(invokeList).inOrder()
        }
        override fun onInit(
            cameraId: String,
            cameraCharacteristics: CameraCharacteristics,
            context: Context
        ) {
            recordInvoking("onInit")
        }

        override fun onDeInit() {
            recordInvoking("onDeInit")
        }
        override fun onPresetSession(): CaptureStageImpl? {
            recordInvoking("onPresetSession")
            return null
        }
        override fun onEnableSession(): CaptureStageImpl? {
            recordInvoking("onEnableSession")
            return onEnableSessionCaptureStage
        }
        override fun onDisableSession(): CaptureStageImpl? {
            recordInvoking("onDisableSession")
            return onDisableSessionCaptureStage
        }

        var onEnableSessionCaptureStage: CaptureStageImpl? = null
        var onDisableSessionCaptureStage: CaptureStageImpl? = null
    }

    private class FakePreviewExtenderImpl(
        private var processorType: ProcessorType = PROCESSOR_TYPE_NONE
    ) : PreviewExtenderImpl, FakeExtenderStateListener() {
        var fakePreviewImageProcessorImpl: FakePreviewImageProcessorImpl? = null
        var fakeRequestUpdateProcessor: FakeRequestUpdateProcessor? = null

        init {
            when (processorType) {
                PROCESSOR_TYPE_REQUEST_UPDATE_ONLY ->
                    fakeRequestUpdateProcessor = FakeRequestUpdateProcessor()
                PROCESSOR_TYPE_IMAGE_PROCESSOR ->
                    fakePreviewImageProcessorImpl = FakePreviewImageProcessorImpl()
                PROCESSOR_TYPE_NONE -> {}
            }
        }
        override fun isExtensionAvailable(
            cameraId: String,
            cameraCharacteristics: CameraCharacteristics
        ): Boolean {
            return true
        }
        override fun init(cameraId: String, cameraCharacteristics: CameraCharacteristics) {
            recordInvoking("init")
        }

        private var _captureStage: CaptureStageImpl? = null
        override fun getCaptureStage(): CaptureStageImpl {
            // Return CaptureStage if it is set already.
            if (_captureStage != null) {
                return _captureStage!!
            }

            // For PROCESSOR_TYPE_REQUEST_UPDATE_ONLY, getCaptureStage() should be in sync with
            // RequestUpdateProcessor.
            if (processorType == PROCESSOR_TYPE_REQUEST_UPDATE_ONLY) {
                return fakeRequestUpdateProcessor!!.captureStage
            }
            return createCaptureStage()
        }

        fun setCaptureStage(captureStage: CaptureStageImpl) {
            _captureStage = captureStage
        }
        override fun getProcessorType() = processorType
        override fun getProcessor() =
            when (processorType) {
                PROCESSOR_TYPE_NONE -> null
                PROCESSOR_TYPE_REQUEST_UPDATE_ONLY -> fakeRequestUpdateProcessor
                PROCESSOR_TYPE_IMAGE_PROCESSOR -> fakePreviewImageProcessorImpl
            }

        override fun getSupportedResolutions() = null
        override fun onDeInit() {
            recordInvoking("onDeInit")
            fakePreviewImageProcessorImpl?.close()
        }
    }

    private class FakeImageCaptureExtenderImpl(
        private val hasCaptureProcessor: Boolean = false,
        private val throwErrorOnProcess: Boolean = false
    ) : ImageCaptureExtenderImpl, FakeExtenderStateListener() {
        val fakeCaptureProcessorImpl: FakeCaptureProcessorImpl? by lazy {
            if (hasCaptureProcessor) {
                FakeCaptureProcessorImpl(throwErrorOnProcess)
            } else {
                null
            }
        }

        override fun isExtensionAvailable(
            cameraId: String,
            cameraCharacteristics: CameraCharacteristics
        ): Boolean {
            return true
        }

        override fun init(cameraId: String, cameraCharacteristics: CameraCharacteristics) {
            recordInvoking("init")
        }
        override fun getCaptureProcessor() = fakeCaptureProcessorImpl

        override fun getCaptureStages() = _captureStages
        private var _captureStages: List<CaptureStageImpl> = listOf(createCaptureStage())
        fun setCaptureStages(captureStages: List<CaptureStageImpl>) {
            _captureStages = captureStages
        }

        override fun getMaxCaptureStage(): Int {
            return 2
        }

        override fun getSupportedResolutions() = null
        override fun getEstimatedCaptureLatencyRange(size: Size?) = null
        override fun getAvailableCaptureRequestKeys(): MutableList<CaptureRequest.Key<Any>> {
            return mutableListOf()
        }

        override fun getAvailableCaptureResultKeys(): MutableList<CaptureResult.Key<Any>> {
            return mutableListOf()
        }

        override fun onDeInit() {
            fakeCaptureProcessorImpl?.close()
            recordInvoking("onDeInit")
        }
    }

    private class FakeCaptureProcessorImpl(
        val throwErrorOnProcess: Boolean = false
    ) : CaptureProcessorImpl {
        private var imageWriter: ImageWriter? = null
        override fun process(results: MutableMap<Int, Pair<Image, TotalCaptureResult>>?) {
            if (throwErrorOnProcess) {
                throw RuntimeException("Process failed")
            }
            val image = imageWriter!!.dequeueInputImage()
            imageWriter!!.queueInputImage(image)
        }

        override fun process(
            results: MutableMap<Int, Pair<Image, TotalCaptureResult>>?,
            resultCallback: ProcessResultImpl?,
            executor: Executor?
        ) {
            process(results)
        }

        override fun onOutputSurface(surface: Surface, imageFormat: Int) {
            imageWriter = ImageWriter.newInstance(surface, 2)
        }

        override fun onResolutionUpdate(size: Size) {}
        override fun onImageFormatUpdate(imageFormat: Int) {}
        fun close() {
            imageWriter?.close()
            imageWriter = null
        }
    }

    private class FakePreviewImageProcessorImpl : PreviewImageProcessorImpl {
        private var imageWriter: ImageWriter? = null
        override fun process(image: Image?, result: TotalCaptureResult?) {
            val emptyImage = imageWriter!!.dequeueInputImage()
            imageWriter!!.queueInputImage(emptyImage)
        }

        override fun process(
            image: Image?,
            result: TotalCaptureResult?,
            resultCallback: ProcessResultImpl?,
            executor: Executor?
        ) {
            process(image, result)
        }

        override fun onOutputSurface(surface: Surface, imageFormat: Int) {
            imageWriter = ImageWriter.newInstance(surface, 2)
        }

        override fun onResolutionUpdate(size: Size) {}
        override fun onImageFormatUpdate(imageFormat: Int) {}
        fun close() {
            imageWriter?.close()
            imageWriter = null
        }
    }

    private class FakeRequestUpdateProcessor : RequestUpdateProcessorImpl {
        override fun onOutputSurface(surface: Surface, imageFormat: Int) {
            throw RuntimeException("Should not invoke this")
        }

        override fun onResolutionUpdate(size: Size) {
            throw RuntimeException("Should not invoke this")
        }

        override fun onImageFormatUpdate(imageFormat: Int) {
            throw RuntimeException("Should not invoke this")
        }

        override fun process(result: TotalCaptureResult?): CaptureStageImpl? {
            return if (hasCaptureStageChange) {
                hasCaptureStageChange = false
                captureStage // return non-null result to trigger a repeating request update.
            } else {
                null
            }
        }

        var hasCaptureStageChange = false
        var captureStage: CaptureStageImpl = createCaptureStage()
            set(value) {
                hasCaptureStageChange = true
                field = value
            }
    }

    class FakeRequestProcessor : RequestProcessor {
        private var deferredSubmit = CompletableDeferred<List<RequestProcessor.Request>>()

        suspend fun awaitRequestSubmitted(): List<RequestProcessor.Request> {
            return deferredSubmit.await().also {
                // renew another deferred
                deferredSubmit = CompletableDeferred<List<RequestProcessor.Request>>()
            }
        }
        override fun submit(
            request: RequestProcessor.Request,
            callback: RequestProcessor.Callback
        ): Int {
            return submit(mutableListOf(request), callback)
        }

        override fun submit(
            requests: MutableList<RequestProcessor.Request>,
            callback: RequestProcessor.Callback
        ): Int {
            deferredSubmit.complete(requests)
            return 0
        }

        override fun setRepeating(
            request: RequestProcessor.Request,
            callback: RequestProcessor.Callback
        ): Int {
            return 0
        }

        override fun abortCaptures() {
        }

        override fun stopRepeating() {
        }
    }
}