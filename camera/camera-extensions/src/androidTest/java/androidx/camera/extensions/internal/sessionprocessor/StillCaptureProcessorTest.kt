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
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.media.ImageWriter
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXThreads
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageReaderProxys
import androidx.camera.core.impl.ImageReaderProxy
import androidx.camera.core.impl.OutputSurface
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.extensions.impl.CaptureProcessorImpl
import androidx.camera.extensions.impl.ProcessResultImpl
import androidx.camera.extensions.internal.ClientVersion
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.Version
import androidx.camera.extensions.internal.sessionprocessor.StillCaptureProcessor.OnCaptureResultCallback
import androidx.camera.extensions.util.Api21Impl
import androidx.camera.extensions.util.Api21Impl.toCameraDeviceWrapper
import androidx.camera.testing.impl.Camera2Util
import androidx.camera.testing.impl.CameraUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@SdkSuppress(minSdkVersion = 29) // Extensions supported on API 29+
@RunWith(AndroidJUnit4::class)
class StillCaptureProcessorTest {
    private lateinit var fakeCaptureProcessorImpl: FakeCaptureProcessorImpl
    private var stillCaptureProcessor: StillCaptureProcessor? = null
    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private var imageReaderOutputYuv: ImageReaderProxy? = null
    private var cameraDevice: Api21Impl.CameraDeviceWrapper? = null
    private var cameraYuvImageReader: ImageReader? = null

    companion object {
        const val CAMERA_ID = "0"
        const val WIDTH = 640
        const val HEIGHT = 480
    }

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        backgroundThread = HandlerThread(CameraXThreads.TAG + "still_capture_processor_test")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
        fakeCaptureProcessorImpl = FakeCaptureProcessorImpl()
    }

    private fun initStillCaptureProcessor(
        postviewOutputSurface: OutputSurface? = null,
        overrideTimestamp: Boolean = false
    ) {
        imageReaderOutputYuv =
            ImageReaderProxys.createIsolatedReader(WIDTH, HEIGHT, ImageFormat.YUV_420_888, 2)
        stillCaptureProcessor =
            StillCaptureProcessor(
                fakeCaptureProcessorImpl,
                imageReaderOutputYuv!!.surface!!,
                Size(WIDTH, HEIGHT),
                postviewOutputSurface,
                overrideTimestamp,
            )
    }

    @After
    fun tearDown() {
        stillCaptureProcessor?.close()

        if (::backgroundThread.isInitialized) {
            backgroundThread.quitSafely()
        }
        imageReaderOutputYuv?.close()

        if (::fakeCaptureProcessorImpl.isInitialized) {
            fakeCaptureProcessorImpl.close()
        }
        cameraDevice?.close()
        cameraYuvImageReader?.close()
    }

    @Test
    fun canOutputYuv_3CaptureStages(): Unit = runBlocking {
        initStillCaptureProcessor()
        withTimeout(10000) { openCameraAndCaptureImageAwait(listOf(1, 2, 3)) }
            .use { assertThat(it.format).isEqualTo(ImageFormat.YUV_420_888) }
    }

    @Test
    fun canOutputYuv_1CaptureStage(): Unit = runBlocking {
        initStillCaptureProcessor()
        withTimeout(10000) { openCameraAndCaptureImageAwait(listOf(1)) }
            .use { assertThat(it.format).isEqualTo(ImageFormat.YUV_420_888) }
    }

    @Test
    fun canOutputYuv_withOverrideTimestamp(): Unit = runBlocking {
        initStillCaptureProcessor(overrideTimestamp = true)
        withTimeout(10000) { openCameraAndCaptureImageAwait(listOf(1)) }
            .use { assertThat(it.format).isEqualTo(ImageFormat.YUV_420_888) }
    }

    @Test
    fun onErrorInvoked_oemProcessingFailed(): Unit = runBlocking {
        initStillCaptureProcessor()
        fakeCaptureProcessorImpl.enableThrowExceptionDuringProcess()
        assertThrows<Exception> {
            withTimeout(3000) { openCameraAndCaptureImageAwait(listOf(1)).close() }
        }
    }

    private suspend fun captureImage(
        cameraDevice: CameraDevice,
        cameraCaptureSession: CameraCaptureSession,
        cameraYuvImageReader: ImageReader,
        captureStageIdList: List<Int>,
        enablePostview: Boolean = false,
    ): ImageProxy {
        cameraYuvImageReader.setOnImageAvailableListener(
            {
                val image = it.acquireNextImage()
                stillCaptureProcessor!!.notifyImage(createImageReference(image))
            },
            backgroundHandler
        )
        val deferredCaptureCompleted = CompletableDeferred<Unit>()
        stillCaptureProcessor!!.startCapture(
            enablePostview,
            captureStageIdList,
            object : OnCaptureResultCallback {
                override fun onProcessCompleted() {
                    deferredCaptureCompleted.complete(Unit)
                }

                override fun onError(e: Exception) {
                    deferredCaptureCompleted.completeExceptionally(e)
                }

                override fun onCaptureCompleted(
                    shutterTimestamp: Long,
                    result: MutableList<android.util.Pair<CaptureResult.Key<Any>, Any>>
                ) {}

                override fun onCaptureProcessProgressed(progress: Int) {}
            }
        )

        val outputYuvDeferred = CompletableDeferred<ImageProxy>()
        imageReaderOutputYuv!!.setOnImageAvailableListener(
            {
                val image = it.acquireNextImage()
                outputYuvDeferred.complete(image!!)
            },
            CameraXExecutors.newHandlerExecutor(backgroundHandler)
        )

        captureStageIdList.forEach { captureStageId ->
            val captureResult =
                Camera2Util.submitSingleRequest(
                    cameraDevice,
                    cameraCaptureSession,
                    listOf(cameraYuvImageReader.surface),
                    backgroundHandler
                )
            stillCaptureProcessor!!.notifyCaptureResult(captureResult, captureStageId)
        }
        deferredCaptureCompleted.await()
        return outputYuvDeferred.await()
    }

    @Test
    fun canStartCaptureMultipleTimes(): Unit = runBlocking {
        initStillCaptureProcessor()
        val captureStageIdList = listOf(0, 1, 2)
        cameraDevice =
            Camera2Util.openCameraDevice(cameraManager, CAMERA_ID, backgroundHandler)
                .toCameraDeviceWrapper()

        cameraYuvImageReader =
            ImageReader.newInstance(
                WIDTH,
                HEIGHT,
                ImageFormat.YUV_420_888,
                captureStageIdList.size /* maxImages */
            )
        val captureSession =
            Camera2Util.openCaptureSession(
                cameraDevice!!.unwrap(),
                listOf(cameraYuvImageReader!!.surface),
                backgroundHandler
            )

        withTimeout(30000) {
            repeat(3) {
                captureImage(
                        cameraDevice!!.unwrap(),
                        captureSession,
                        cameraYuvImageReader!!,
                        captureStageIdList
                    )
                    .use { assertThat(it).isNotNull() }
            }
        }
    }

    @Test
    fun canStartCaptureWithPostview(): Unit = runBlocking {
        Assume.assumeTrue(
            ClientVersion.isMinimumCompatibleVersion(Version.VERSION_1_4) &&
                ExtensionVersion.isMinimumCompatibleVersion(Version.VERSION_1_4)
        )
        val postviewImageReader =
            ImageReaderProxys.createIsolatedReader(WIDTH, HEIGHT, ImageFormat.YUV_420_888, 2)
        val postviewOutputSurface =
            OutputSurface.create(
                postviewImageReader.surface!!,
                Size(WIDTH, HEIGHT),
                ImageFormat.YUV_420_888
            )
        initStillCaptureProcessor(postviewOutputSurface)

        val captureStageIdList = listOf(0, 1, 2)
        cameraDevice =
            Camera2Util.openCameraDevice(cameraManager, CAMERA_ID, backgroundHandler)
                .toCameraDeviceWrapper()

        cameraYuvImageReader =
            ImageReader.newInstance(
                WIDTH,
                HEIGHT,
                ImageFormat.YUV_420_888,
                captureStageIdList.size /* maxImages */
            )

        val captureSession =
            Camera2Util.openCaptureSession(
                cameraDevice!!.unwrap(),
                listOf(cameraYuvImageReader!!.surface),
                backgroundHandler
            )

        val postviewDeferred = CompletableDeferred<ImageProxy>()
        postviewImageReader.setOnImageAvailableListener(
            {
                val postviewImage = it.acquireNextImage()
                postviewDeferred.complete(postviewImage!!)
            },
            CameraXExecutors.mainThreadExecutor()
        )

        withTimeout(10000) {
            captureImage(
                    cameraDevice!!.unwrap(),
                    captureSession,
                    cameraYuvImageReader!!,
                    captureStageIdList,
                    enablePostview = true
                )
                .use { assertThat(it).isNotNull() }

            val postviewImage = postviewDeferred.await()
            assertThat(postviewImage.format).isEqualTo(ImageFormat.YUV_420_888)
        }

        postviewImageReader.close()
    }

    private suspend fun openCameraAndCaptureImageAwait(
        captureStageIdList: List<Int>,
        onBeforeInputYuvReady: suspend () -> Unit = {},
        onProcessCompleted: suspend () -> Unit = {},
    ): ImageProxy {
        val (deferredCapture, deferredYuv) =
            openCameraAndCaptureImage(captureStageIdList, onBeforeInputYuvReady, onProcessCompleted)
        deferredCapture.await()
        return deferredYuv.await()
    }

    private suspend fun openCameraAndCaptureImage(
        captureStageIdList: List<Int>,
        onBeforeInputYuvReady: suspend () -> Unit = {},
        onProcessCompleted: suspend () -> Unit = {},
    ): Pair<Deferred<Unit>, Deferred<ImageProxy>> {
        cameraDevice =
            Camera2Util.openCameraDevice(cameraManager, CAMERA_ID, backgroundHandler)
                .toCameraDeviceWrapper()
        cameraYuvImageReader =
            ImageReader.newInstance(
                WIDTH,
                HEIGHT,
                ImageFormat.YUV_420_888,
                captureStageIdList.size /* maxImages */
            )
        val captureSession =
            Camera2Util.openCaptureSession(
                cameraDevice!!.unwrap(),
                listOf(cameraYuvImageReader!!.surface),
                backgroundHandler
            )

        val deferredCapture = CompletableDeferred<Unit>()
        stillCaptureProcessor!!.startCapture(
            false,
            captureStageIdList,
            object : OnCaptureResultCallback {
                override fun onProcessCompleted() {
                    deferredCapture.complete(Unit)
                }

                override fun onError(e: java.lang.Exception) {
                    deferredCapture.completeExceptionally(e)
                }

                override fun onCaptureCompleted(
                    shutterTimestamp: Long,
                    result: MutableList<android.util.Pair<CaptureResult.Key<Any>, Any>>
                ) {}

                override fun onCaptureProcessProgressed(progress: Int) {}
            }
        )

        val deferredOutputYuv = CompletableDeferred<ImageProxy>()
        imageReaderOutputYuv!!.setOnImageAvailableListener(
            {
                val image = it.acquireNextImage()
                deferredOutputYuv.complete(image!!)
            },
            CameraXExecutors.newHandlerExecutor(backgroundHandler)
        )

        cameraYuvImageReader!!.setOnImageAvailableListener(
            {
                val image = it.acquireNextImage()
                stillCaptureProcessor!!.notifyImage(createImageReference(image))
            },
            backgroundHandler
        )

        onBeforeInputYuvReady.invoke()

        for (id in captureStageIdList) {
            val captureResult =
                Camera2Util.submitSingleRequest(
                    cameraDevice!!.unwrap(),
                    captureSession,
                    listOf(cameraYuvImageReader!!.surface),
                    backgroundHandler
                )
            stillCaptureProcessor!!.notifyCaptureResult(captureResult, id)
        }

        onProcessCompleted.invoke()

        return Pair(deferredCapture, deferredOutputYuv)
    }

    @Test
    fun canCloseBeforeProcessing(): Unit = runBlocking {
        initStillCaptureProcessor()
        withTimeout(3000) {
            openCameraAndCaptureImage(
                listOf(0, 1),
                onBeforeInputYuvReady = {
                    // Close the StillCaptureProcessor before it starts the processing.
                    stillCaptureProcessor?.close()
                    // Close output yuv image reader to see if processing failed.
                    imageReaderOutputYuv?.close()
                },
                onProcessCompleted = {
                    // Delay a little while to see if close causes any issue
                    delay(1000)
                }
            )
        }
    }

    private fun createImageReference(image: Image): ImageReference {
        return object : ImageReference {
            private var refCount = 1

            override fun increment(): Boolean {
                if (refCount <= 0) return false
                refCount++
                return true
            }

            override fun decrement(): Boolean {
                if (refCount <= 0) return false
                refCount--
                if (refCount <= 0) {
                    image.close()
                }
                return true
            }

            override fun get(): Image {
                return image
            }
        }
    }

    // A fake CaptureProcessorImpl that simply output a blank Image.
    class FakeCaptureProcessorImpl : CaptureProcessorImpl {
        private var imageWriter: ImageWriter? = null
        private var imageWriterPostview: ImageWriter? = null

        private var throwExceptionDuringProcess = false

        fun enableThrowExceptionDuringProcess() {
            throwExceptionDuringProcess = true
        }

        override fun process(
            results: MutableMap<Int, android.util.Pair<Image, TotalCaptureResult>>
        ) {
            processInternal()
        }

        override fun process(
            results: MutableMap<Int, android.util.Pair<Image, TotalCaptureResult>>,
            resultCallback: ProcessResultImpl,
            executor: Executor?
        ) {
            processInternal()
        }

        private fun processInternal(enablePostview: Boolean = false) {
            if (throwExceptionDuringProcess) {
                throw RuntimeException("Process failed")
            }
            val image = imageWriter!!.dequeueInputImage()
            imageWriter!!.queueInputImage(image)

            if (enablePostview) {
                val imagePostview = imageWriterPostview!!.dequeueInputImage()
                imageWriterPostview!!.queueInputImage(imagePostview)
            }
        }

        override fun onOutputSurface(surface: Surface, imageFormat: Int) {
            imageWriter = ImageWriter.newInstance(surface, 2, imageFormat)
        }

        override fun onResolutionUpdate(size: Size) {}

        override fun onImageFormatUpdate(imageFormat: Int) {}

        override fun onPostviewOutputSurface(surface: Surface) {
            imageWriterPostview = ImageWriter.newInstance(surface, 2)
        }

        override fun onResolutionUpdate(size: Size, postviewSize: Size) {}

        override fun processWithPostview(
            results: MutableMap<Int, android.util.Pair<Image, TotalCaptureResult>>,
            resultCallback: ProcessResultImpl,
            executor: Executor?
        ) {
            processInternal(enablePostview = true)
        }

        fun close() {
            imageWriter?.close()
        }
    }
}
