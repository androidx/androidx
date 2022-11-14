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

import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.HandlerUtil
import androidx.camera.testing.fakes.FakeCamera
import androidx.concurrent.futures.await
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 21)
class DefaultSurfaceProcessorTest {

    companion object {
        private const val WIDTH = 640
        private const val HEIGHT = 480
        private const val CUSTOM_SHADER_FORMAT = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        uniform samplerExternalOES %s;
        varying vec2 %s;
        void main() {
          vec4 sampleColor = texture2D(%s, %s);
          gl_FragColor = vec4(sampleColor.r * 0.493 + sampleColor. g * 0.769 +
             sampleColor.b * 0.289, sampleColor.r * 0.449 + sampleColor.g * 0.686 +
             sampleColor.b * 0.268, sampleColor.r * 0.272 + sampleColor.g * 0.534 +
             sampleColor.b * 0.131, 1.0);
        }
        """

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

    private lateinit var surfaceProcessor: DefaultSurfaceProcessor
    private lateinit var cameraDeviceHolder: CameraUtil.CameraDeviceHolder
    private lateinit var renderOutput: RenderOutput<*>
    private val inputSurfaceRequestsToClose = mutableListOf<SurfaceRequest>()
    private val surfacesToRelease = mutableListOf<Surface>()
    private val surfaceTexturesToRelease = mutableListOf<SurfaceTexture>()
    private val fakeCamera = FakeCamera()

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraDeviceHolder.isInitialized) {
            CameraUtil.releaseCameraDevice(cameraDeviceHolder)
        }
        if (::renderOutput.isInitialized) {
            renderOutput.release()
        }
        for (surface in surfacesToRelease) {
            surface.release()
        }
        for (surfaceTexture in surfaceTexturesToRelease) {
            surfaceTexture.release()
        }
        if (::surfaceProcessor.isInitialized) {
            surfaceProcessor.release()
            surfaceProcessor.awaitReleased(10_000L)
        }
        for (surfaceRequest in inputSurfaceRequestsToClose) {
            surfaceRequest.deferrableSurface.close()
        }
    }

    @Test
    fun release_closeAllSurfaceOutputs(): Unit = runBlocking {
        // Arrange.
        createSurfaceProcessor()
        val surfaceOutput1 = createSurfaceOutput()
        surfaceProcessor.onOutputSurface(surfaceOutput1)

        val surfaceOutput2 = createSurfaceOutput()
        surfaceProcessor.onOutputSurface(surfaceOutput2)

        surfaceProcessor.idle()

        // Act.
        surfaceProcessor.release()
        surfaceProcessor.awaitReleased(5000)

        // Assert.
        assertThat(surfaceOutput1.isClosed).isTrue()
        assertThat(surfaceOutput2.isClosed).isTrue()
    }

    @Test
    fun callOnInputSurfaceAfterReleased_willNotProvideSurface() {
        // Arrange.
        createSurfaceProcessor()
        val surfaceRequest = createInputSurfaceRequest()

        // Act.
        surfaceProcessor.release()
        surfaceProcessor.onInputSurface(surfaceRequest)

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
        createSurfaceProcessor()
        val surfaceOutput = createSurfaceOutput()

        // Act.
        surfaceProcessor.release()
        surfaceProcessor.awaitReleased()
        surfaceProcessor.onOutputSurface(surfaceOutput)

        // Assert.
        assertThat(surfaceOutput.isClosed).isTrue()
    }

    @Test
    fun requestCloseAfterOnOutputSurface_closeSurfaceOutput() {
        // Arrange.
        createSurfaceProcessor()
        val surfaceOutput = createSurfaceOutput()

        // Act.
        surfaceProcessor.onOutputSurface(surfaceOutput)
        surfaceProcessor.idle()
        surfaceOutput.requestClose()
        surfaceProcessor.idle()

        // Assert.
        assertThat(surfaceOutput.isClosed).isTrue()
    }

    @Test
    fun requestCloseBeforeOnOutputSurface_closeSurfaceOutput() {
        // Arrange.
        createSurfaceProcessor()
        val surfaceOutput = createSurfaceOutput()

        // Act.
        surfaceOutput.requestClose()
        surfaceProcessor.onOutputSurface(surfaceOutput)
        surfaceProcessor.idle()

        // Assert.
        assertThat(surfaceOutput.isClosed).isTrue()
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun render(): Unit = runBlocking {
        testRender(OutputType.IMAGE_READER)
    }

    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = 22)
    @Test
    fun renderBelowApi23(): Unit = runBlocking {
        testRender(OutputType.SURFACE_TEXTURE)
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun renderByCustomShader(): Unit = runBlocking {
        testRender(OutputType.IMAGE_READER, createCustomShaderProvider())
    }

    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = 22)
    @Test
    fun renderByCustomShaderBelowApi23(): Unit = runBlocking {
        testRender(OutputType.SURFACE_TEXTURE, createCustomShaderProvider())
    }

    @Test
    fun createByInvalidShaderString_throwException() {
        val shaderProvider = createCustomShaderProvider(shaderString = "Invalid shader")
        assertThrows(IllegalArgumentException::class.java) {
            createSurfaceProcessor(shaderProvider)
        }
    }

    @Test
    fun createByFailedShaderProvider_throwException() {
        val shaderProvider =
            createCustomShaderProvider(exceptionToThrow = RuntimeException("Failed Shader"))
        assertThrows(IllegalArgumentException::class.java) {
            createSurfaceProcessor(shaderProvider)
        }
    }

    @Test
    fun createByIncorrectSamplerName_throwException() {
        val shaderProvider = createCustomShaderProvider(samplerVarName = "_mySampler_")
        assertThrows(IllegalArgumentException::class.java) {
            createSurfaceProcessor(shaderProvider)
        }
    }

    @Test
    fun createByIncorrectFragCoordsName_throwException() {
        val shaderProvider = createCustomShaderProvider(fragCoordsVarName = "_myFragCoords_")
        assertThrows(IllegalArgumentException::class.java) {
            createSurfaceProcessor(shaderProvider)
        }
    }

    private suspend fun testRender(
        outputType: OutputType,
        shaderProvider: ShaderProvider = ShaderProvider.DEFAULT
    ) {
        createSurfaceProcessor(shaderProvider)
        // Prepare input
        val inputSurfaceRequest = createInputSurfaceRequest()
        surfaceProcessor.onInputSurface(inputSurfaceRequest)
        val inputDeferrableSurface = inputSurfaceRequest.deferrableSurface
        val inputSurface = inputDeferrableSurface.surface.await()
        openCameraAndSetRepeating(inputSurface)
        cameraDeviceHolder.closedFuture.addListener({
            inputDeferrableSurface.close()
        }, CameraXExecutors.directExecutor())

        // Prepare output
        renderOutput = RenderOutput.createRenderOutput(outputType)
        val surfaceOutput = createSurfaceOutput(renderOutput.surface)
        surfaceProcessor.onOutputSurface(surfaceOutput)

        // Assert.
        assertThat(renderOutput.await(/*imageCount=*/5, /*timeoutInMs=*/10_000L)).isTrue()
    }

    private fun openCameraAndSetRepeating(surface: Surface) {
        cameraDeviceHolder = CameraUtil.getCameraDevice(null)
        val captureSessionHolder = cameraDeviceHolder.createCaptureSession(listOf(surface))
        captureSessionHolder.startRepeating(
            TEMPLATE_PREVIEW,
            listOf(surface),
            null,
            null
        )
    }

    private fun createSurfaceProcessor(shaderProvider: ShaderProvider = ShaderProvider.DEFAULT) {
        surfaceProcessor = DefaultSurfaceProcessor(
            shaderProvider
        )
    }

    private fun createInputSurfaceRequest(): SurfaceRequest {
        return SurfaceRequest(Size(WIDTH, HEIGHT), fakeCamera, false) {}.apply {
            inputSurfaceRequestsToClose.add(this)
        }
    }

    private fun createSurfaceOutput(surface: Surface = createAutoReleaseSurface()) =
        SurfaceOutputImpl(
            surface,
            CameraEffect.PREVIEW,
            ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            Size(WIDTH, HEIGHT),
            Size(WIDTH, HEIGHT),
            Rect(0, 0, WIDTH, HEIGHT),
            /*rotationDegrees=*/0,
            /*mirroring=*/false
        )

    private fun createCustomShaderProvider(
        samplerVarName: String? = null,
        fragCoordsVarName: String? = null,
        shaderString: String? = null,
        exceptionToThrow: Exception? = null,
    ) = object : ShaderProvider {
        override fun createFragmentShader(
            correctSamplerVarName: String,
            correctFragCoordsVarName: String
        ): String {
            exceptionToThrow?.let { throw it }
            return shaderString ?: String.format(
                Locale.US,
                CUSTOM_SHADER_FORMAT,
                samplerVarName ?: correctSamplerVarName,
                fragCoordsVarName ?: correctFragCoordsVarName,
                samplerVarName ?: correctSamplerVarName,
                fragCoordsVarName ?: correctFragCoordsVarName
            )
        }
    }

    private fun createAutoReleaseSurface(): Surface {
        val surfaceTexture = SurfaceTexture(0)
        surfaceTexture.setDefaultBufferSize(WIDTH, HEIGHT)
        surfaceTexturesToRelease.add(surfaceTexture)
        val surface = Surface(surfaceTexture)
        surfacesToRelease.add(surface)

        return surface
    }

    private fun DefaultSurfaceProcessor.idle() {
        HandlerUtil.waitForLooperToIdle(mGlHandler)
    }

    private suspend fun DefaultSurfaceProcessor.awaitReleased(timeoutMs: Long = 5000L) {
        withTimeoutOrNull(timeoutMs) {
            while (true) {
                delay(500L)
                if (!mGlThread.isAlive) break
            }
            true
        } ?: fail("Timed out $timeoutMs milli-second to wait released")
    }
}
