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

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.media.Image
import android.media.ImageReader
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.CameraUtil
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import androidx.testutils.fail
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 21)
class OpenGlRendererTest {

    companion object {
        private const val WIDTH = 640
        private const val HEIGHT = 480
        private val IDENTITY_MATRIX = createGlIdentityMatrix()

        private fun createGlIdentityMatrix() = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }

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

    private lateinit var glThread: HandlerThread
    private lateinit var glHandler: Handler
    private lateinit var glDispatcher: CoroutineDispatcher
    private lateinit var glRenderer: OpenGlRenderer
    private lateinit var cameraDeviceHolder: CameraUtil.CameraDeviceHolder

    @Before
    fun setUp() {
        glThread = HandlerThread("GL Thread").apply { start() }
        glHandler = Handler(glThread.looper)
        glDispatcher = glHandler.asCoroutineDispatcher()
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraDeviceHolder.isInitialized) {
            CameraUtil.releaseCameraDevice(cameraDeviceHolder)
        }
        if (::glRenderer.isInitialized) {
            withContext(glDispatcher) {
                glRenderer.release()
            }
        }
        if (::glThread.isInitialized) {
            glThread.quitSafely()
        }
    }

    @Test
    fun getTextureNameWithoutInit_throwException(): Unit = runBlocking(glDispatcher) {
        createOpenGlRenderer()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.textureName
        }
    }

    @Test
    fun setOutputSurfaceWithoutInit_throwException(): Unit = runBlocking(glDispatcher) {
        createOpenGlRenderer()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.setOutputSurface(mock(Surface::class.java))
        }
    }

    @Test
    fun renderWithoutInit_throwException(): Unit = runBlocking(glDispatcher) {
        createOpenGlRenderer()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.render(123L, IDENTITY_MATRIX)
        }
    }

    @Test
    fun releaseWithoutInit_noOp(): Unit = runBlocking(glDispatcher) {
        createOpenGlRenderer()
        glRenderer.release()
    }

    @Test
    fun getTextureNameOnNonGlThread_throwException(): Unit = runBlocking {
        createOpenGlRendererAndInit()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.textureName
        }
    }

    @Test
    fun setOutputSurfaceOnNonGlThread_throwException(): Unit = runBlocking {
        createOpenGlRendererAndInit()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.setOutputSurface(mock(Surface::class.java))
        }
    }

    @Test
    fun renderOnNonGlThread_throwException(): Unit = runBlocking {
        createOpenGlRendererAndInit()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.render(123L, IDENTITY_MATRIX)
        }
    }

    @Test
    fun releaseOnNonGlThread_throwException(): Unit = runBlocking {
        createOpenGlRendererAndInit()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.release()
        }
    }

    @Test
    fun reInit(): Unit = runBlocking(glDispatcher) {
        createOpenGlRendererAndInit()
        glRenderer.release()
        glRenderer.init()
        assertThat(glRenderer.textureName).isNotEqualTo(0L)
    }

    @Test
    fun renderFromCameraToImageReader(): Unit = runBlocking(glDispatcher) {
        // Arrange.
        createOpenGlRendererAndInit()

        // Prepare input
        val surfaceTexture = SurfaceTexture(glRenderer.textureName).apply {
            setDefaultBufferSize(WIDTH, HEIGHT)
        }
        val inputSurface = Surface(surfaceTexture)
        openCameraAndSetRepeating(inputSurface)
        cameraDeviceHolder.closedFuture.addListener({
            inputSurface.release()
            surfaceTexture.release()
        }, CameraXExecutors.directExecutor())

        // Prepare output
        val imageReader =
            ImageReader.newInstance(WIDTH, HEIGHT, ImageFormat.PRIVATE, 2)
        val imageFlow = callbackFlow<Image> {
            val listener = ImageReader.OnImageAvailableListener {
                trySend(it.acquireLatestImage())
            }
            imageReader.setOnImageAvailableListener(listener, glHandler)
            awaitClose { imageReader.close() }
        }

        // Bridge input to output
        surfaceTexture.setOnFrameAvailableListener({
            it.updateTexImage()
            glRenderer.setOutputSurface(imageReader.surface)
            glRenderer.render(0L, IDENTITY_MATRIX)
        }, glHandler)

        val scope = CoroutineScope(glDispatcher)
        val imageCollectJob = scope.launch {
            imageFlow.collectIndexed { index, image ->
                image.close()
                if (index >= 4) {
                    scope.cancel()
                }
            }
        }

        // Assert.
        withTimeoutOrNull(10_000L) {
            imageCollectJob.join()
            true
        } ?: fail("Timed out to receive images")
    }

    private suspend fun createOpenGlRendererAndInit() {
        createOpenGlRenderer()

        if (currentCoroutineContext()[ContinuationInterceptor] == glDispatcher) {
            // same dispatcher, init directly
            glRenderer.init()
        } else {
            runBlocking(glDispatcher) {
                glRenderer.init()
            }
        }
    }

    private fun createOpenGlRenderer() {
        glRenderer = OpenGlRenderer()
    }

    private fun openCameraAndSetRepeating(surface: Surface) {
        cameraDeviceHolder = CameraUtil.getCameraDevice(null)
        val captureSessionHolder = cameraDeviceHolder.createCaptureSession(listOf(surface))
        captureSessionHolder.startRepeating(
            CameraDevice.TEMPLATE_PREVIEW,
            listOf(surface),
            null,
            null
        )
    }
}
