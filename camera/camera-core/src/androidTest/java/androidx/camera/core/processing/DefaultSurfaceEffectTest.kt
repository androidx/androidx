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

package androidx.camera.core.processing

import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceEffect
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.HandlerUtil
import androidx.camera.testing.fakes.FakeCamera
import androidx.concurrent.futures.await
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 21)
class DefaultSurfaceEffectTest {

    companion object {
        private const val WIDTH = 640
        private const val HEIGHT = 480
        private val IDENTITY_MATRIX = createGlIdentityMatrix()

        private fun createGlIdentityMatrix() =
            FloatArray(16).apply { android.opengl.Matrix.setIdentityM(this, 0) }

        @JvmStatic
        lateinit var testCameraRule: CameraUtil.PreTestCamera

        @JvmStatic
        lateinit var testCameraIdListRule: CameraUtil.PreTestCameraIdList

        @BeforeClass
        @JvmStatic
        fun classSetup() {
            // In order to test camera just once, keep the test rules.
            testCameraRule = CameraUtil.PreTestCamera()
            testCameraIdListRule = CameraUtil.PreTestCameraIdList()
        }
    }

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(testCameraRule, testCameraIdListRule)

    private lateinit var surfaceEffect: DefaultSurfaceEffect
    private lateinit var cameraDeviceHolder: CameraUtil.CameraDeviceHolder
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler
    private lateinit var dispatcher: CoroutineDispatcher
    private val inputSurfaceRequestsToClose = mutableListOf<SurfaceRequest>()
    private val settableSurfacesToClose = mutableMapOf<SettableSurface, DeferrableSurface>()
    private val fakeCamera = FakeCamera()

    @Before
    fun setUp() {
        surfaceEffect = DefaultSurfaceEffect()
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraDeviceHolder.isInitialized) {
            CameraUtil.releaseCameraDevice(cameraDeviceHolder)
        }
        if (::surfaceEffect.isInitialized) {
            surfaceEffect.release()
            surfaceEffect.awaitReleased(10_000L)
        }
        if (::handlerThread.isInitialized) {
            handlerThread.quitSafely()
        }
        for (surfaceRequest in inputSurfaceRequestsToClose) {
            surfaceRequest.deferrableSurface.close()
        }
        for ((settableSurface, sourceSurface) in settableSurfacesToClose) {
            settableSurface.close()
            sourceSurface.close()
        }
    }

    @Test
    fun release_closeAllSurfaceOutputs(): Unit = runBlocking {
        // Arrange.
        val surfaceOutput1 = createSurfaceOutput()
        surfaceEffect.onOutputSurface(surfaceOutput1)

        val surfaceOutput2 = createSurfaceOutput()
        surfaceEffect.onOutputSurface(surfaceOutput2)

        surfaceEffect.idle()

        // Act.
        surfaceEffect.release()
        surfaceEffect.awaitReleased(5000)

        // Assert.
        assertThat(surfaceOutput1.isClosed).isTrue()
        assertThat(surfaceOutput2.isClosed).isTrue()
    }

    @Test
    fun callOnInputSurfaceAfterReleased_willNotProvideSurface() {
        // Arrange.
        val surfaceRequest = createInputSurfaceRequest()

        // Act.
        surfaceEffect.release()
        surfaceEffect.onInputSurface(surfaceRequest)

        // Assert.
        try {
            surfaceRequest.deferrableSurface.surface.get()
        } catch (e: ExecutionException) {
            assertThat(e.cause)
                .isInstanceOf(DeferrableSurface.SurfaceUnavailableException::class.java)
        }
    }

    @Test
    fun callOnOutputSurfaceAfterReleased_closeSurfaceOutput(): Unit = runBlocking {
        // Arrange.
        val surfaceOutput = createSurfaceOutput()

        // Act.
        surfaceEffect.release()
        surfaceEffect.awaitReleased()
        surfaceEffect.onOutputSurface(surfaceOutput)

        // Assert.
        assertThat(surfaceOutput.isClosed).isTrue()
    }

    @Test
    fun requestCloseAfterOnOutputSurface_closeSurfaceOutput() {
        // Arrange.
        val sourceSurface = ImmediateSurface(mock(Surface::class.java))
        val settableSurface = createSettableSurface(sourceSurface)
        val surfaceOutput = createSurfaceOutput(settableSurface)

        // Act.
        surfaceEffect.onOutputSurface(surfaceOutput)
        surfaceEffect.idle()
        surfaceOutput.requestClose()
        surfaceEffect.idle()

        // Assert.
        assertThat(surfaceOutput.isClosed).isTrue()

        // Clean-up.
        settableSurface.close()
        sourceSurface.close()
    }

    @Test
    fun requestCloseBeforeOnOutputSurface_closeSurfaceOutput() {
        // Arrange.
        val surfaceOutput = createSurfaceOutput()

        // Act.
        surfaceOutput.requestClose()
        surfaceEffect.onOutputSurface(surfaceOutput)
        surfaceEffect.idle()

        // Assert.
        assertThat(surfaceOutput.isClosed).isTrue()
    }

    @Test
    fun inputFromCameraAndOutputToImageReader(): Unit = runBlocking {
        prepareHandlerThread()
        // Prepare input
        val inputSurfaceRequest = createInputSurfaceRequest()
        surfaceEffect.onInputSurface(inputSurfaceRequest)
        val inputDeferrableSurface = inputSurfaceRequest.deferrableSurface
        val inputSurface = inputDeferrableSurface.surface.await()
        openCameraAndSetRepeating(inputSurface)
        cameraDeviceHolder.closedFuture.addListener({
            inputDeferrableSurface.close()
        }, CameraXExecutors.directExecutor())

        // Prepare output
        val imageReader = ImageReader.newInstance(
            WIDTH,
            HEIGHT,
            ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            2
        )
        val scope = CoroutineScope(dispatcher)
        val imageCollectJob = scope.launch {
            callbackFlow {
                val listener = ImageReader.OnImageAvailableListener {
                    trySend(it.acquireLatestImage())
                }
                imageReader.setOnImageAvailableListener(listener, handler)
                awaitClose { imageReader.close() }
            }.collectIndexed { index, image ->
                image.close()
                if (index >= 4) {
                    // stop the collect job
                    scope.cancel()
                }
            }
        }
        val surfaceOutput = createSurfaceOutput(
            settableSurface = createSettableSurface(ImmediateSurface(imageReader.surface))
        )
        surfaceEffect.onOutputSurface(surfaceOutput)

        // Assert.
        withTimeoutOrNull(10_000L) {
            imageCollectJob.join()
            true
        } ?: fail("Timed out to receive images")
    }

    private fun prepareHandlerThread() {
        handlerThread = HandlerThread("Worker").apply { start() }
        handler = Handler(handlerThread.looper)
        dispatcher = handler.asCoroutineDispatcher()
    }

    private fun openCameraAndSetRepeating(surface: Surface) {
        cameraDeviceHolder = CameraUtil.getCameraDevice(null)
        val captureSessionHolder = cameraDeviceHolder.createCaptureSession(listOf(surface))
        captureSessionHolder.startRepeating(
            TEMPLATE_PREVIEW,
            listOf(surface),
            null,
            null)
    }

    private fun createInputSurfaceRequest(): SurfaceRequest {
        return SurfaceRequest(Size(WIDTH, HEIGHT), fakeCamera, false).apply {
            inputSurfaceRequestsToClose.add(this)
        }
    }

    private fun createSurfaceOutput(settableSurface: SettableSurface = createSettableSurface()) =
        SurfaceOutputImpl(settableSurface, IDENTITY_MATRIX)

    private fun createSettableSurface(
        source: DeferrableSurface = ImmediateSurface(mock(Surface::class.java))
    ) = SettableSurface(
        SurfaceEffect.PREVIEW,
        Size(WIDTH, HEIGHT),
        ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
        Matrix(),
        false,
        Rect(0, 0, WIDTH, HEIGHT),
        0,
        false
    ).apply {
        settableSurfacesToClose[this] = source
        setSource(source)
    }

    private fun DefaultSurfaceEffect.idle() {
        HandlerUtil.waitForLooperToIdle(mGlHandler)
    }

    private suspend fun DefaultSurfaceEffect.awaitReleased(timeoutMs: Long = 5000L) {
        withTimeoutOrNull(timeoutMs) {
            while (true) {
                delay(500L)
                if (!mGlThread.isAlive) break
            }
            true
        } ?: fail("Timed out $timeoutMs milli-second to wait released")
    }
}
