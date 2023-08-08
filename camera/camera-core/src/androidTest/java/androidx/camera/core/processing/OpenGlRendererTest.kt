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

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.params.OutputConfiguration
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.params.DynamicRangeConversions
import androidx.camera.camera2.internal.compat.params.DynamicRangesCompat
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.TestImageUtil.createBitmap
import androidx.camera.testing.impl.TestImageUtil.getAverageDiff
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 21)
class OpenGlRendererTest {

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
    private lateinit var cameraId: String
    private lateinit var cameraCharacteristicsCompat: CameraCharacteristicsCompat
    private lateinit var cameraDeviceHolder: CameraUtil.CameraDeviceHolder
    private lateinit var renderOutput: RenderOutput<*>
    private var lensFacing = LENS_FACING_BACK
    private val surfacesToRelease = mutableListOf<Surface>()
    private val surfaceTexturesToRelease = mutableListOf<SurfaceTexture>()

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
        if (::renderOutput.isInitialized) {
            renderOutput.release()
        }
        for (surface in surfacesToRelease) {
            surface.release()
        }
        for (surfaceTexture in surfaceTexturesToRelease) {
            surfaceTexture.release()
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

    @RequiresApi(Build.VERSION_CODES.M)
    @Test
    fun drawInputSurface_snapshotReturnsTheSame(): Unit = runBlocking(glDispatcher) {
        // Arrange: set up renderer and Surface.
        createOpenGlRendererAndInit()
        val surfaceTexture = SurfaceTexture(glRenderer.textureName).apply {
            setDefaultBufferSize(WIDTH, HEIGHT)
            surfaceTexturesToRelease.add(this)
        }
        val inputSurface = Surface(surfaceTexture).apply {
            surfacesToRelease.add(this)
        }
        // Listen for OnFrameAvailable updates before drawing.
        val deferredOnFrameAvailable = CompletableDeferred<Unit>()
        surfaceTexture.setOnFrameAvailableListener({
            deferredOnFrameAvailable.complete(Unit)
        }, Handler(Looper.getMainLooper()))

        // Draw bitmap to inputSurface.
        val inputImage = createBitmap(WIDTH, HEIGHT)
        val canvas = inputSurface.lockHardwareCanvas()
        canvas.drawBitmap(inputImage, 0f, 0f, null)
        inputSurface.unlockCanvasAndPost(canvas)

        // Wait for frame available and update texture.
        withTimeoutOrNull(5_000) {
            deferredOnFrameAvailable.await()
        } ?: fail("Timed out waiting for SurfaceTexture frame available.")
        surfaceTexture.updateTexImage()

        // Act: take a snapshot.
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix, 0)
        val result = glRenderer.snapshot(Size(WIDTH, HEIGHT), matrix)

        // Assert: the result is identical to the input.
        assertThat(getAverageDiff(result, inputImage)).isEqualTo(0)
    }

    @Test
    fun getTextureNameWithoutInit_throwException(): Unit = runBlocking(glDispatcher) {
        createOpenGlRenderer()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.textureName
        }
    }

    @Test
    fun registerOutputSurfaceWithoutInit_throwException(): Unit = runBlocking(glDispatcher) {
        createOpenGlRenderer()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.registerOutputSurface(createAutoReleaseSurface())
        }
    }

    @Test
    fun renderWithoutInit_throwException(): Unit = runBlocking(glDispatcher) {
        createOpenGlRenderer()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.render(123L, IDENTITY_MATRIX, createAutoReleaseSurface())
        }
    }

    @Test
    fun unregisterOutputSurfaceWithoutInit_throwException(): Unit = runBlocking(glDispatcher) {
        createOpenGlRenderer()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.unregisterOutputSurface(createAutoReleaseSurface())
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
    fun registerOnNonGlThread_throwException(): Unit = runBlocking {
        createOpenGlRendererAndInit()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.registerOutputSurface(createAutoReleaseSurface())
        }
    }

    @Test
    fun renderOnNonGlThread_throwException(): Unit = runBlocking {
        createOpenGlRendererAndInit()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.render(123L, IDENTITY_MATRIX, createAutoReleaseSurface())
        }
    }

    @Test
    fun unregisterOnNonGlThread_throwException(): Unit = runBlocking {
        createOpenGlRendererAndInit()
        assertThrows(IllegalStateException::class.java) {
            glRenderer.unregisterOutputSurface(createAutoReleaseSurface())
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
    fun initByInvalidShaderString_throwException() {
        val shaderProvider = createCustomShaderProvider(shaderString = "Invalid shader")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking(glDispatcher) {
                createOpenGlRendererAndInit(shaderProvider = shaderProvider)
            }
        }
    }

    @Test
    fun initByFailedShaderProvider_throwException() {
        val shaderProvider =
            createCustomShaderProvider(exceptionToThrow = RuntimeException("Failed Shader"))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking(glDispatcher) {
                createOpenGlRendererAndInit(shaderProvider = shaderProvider)
            }
        }
    }

    @Test
    fun initByIncorrectSamplerName_throwException() {
        val shaderProvider = createCustomShaderProvider(samplerVarName = "_mySampler_")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking(glDispatcher) {
                createOpenGlRendererAndInit(shaderProvider = shaderProvider)
            }
        }
    }

    @Test
    fun initByIncorrectFragCoordsName_throwException() {
        val shaderProvider = createCustomShaderProvider(fragCoordsVarName = "_myFragCoords_")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking(glDispatcher) {
                createOpenGlRendererAndInit(shaderProvider = shaderProvider)
            }
        }
    }

    @Test
    fun reInit(): Unit = runBlocking(glDispatcher) {
        createOpenGlRendererAndInit()
        glRenderer.release()
        glRenderer.init(DynamicRange.SDR, ShaderProvider.DEFAULT)
        assertThat(glRenderer.textureName).isNotEqualTo(0L)
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun render(): Unit = runBlocking(glDispatcher) {
        testRender(OutputType.IMAGE_READER)
    }

    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = 22)
    @Test
    fun renderBelowApi23(): Unit = runBlocking(glDispatcher) {
        testRender(OutputType.SURFACE_TEXTURE)
    }

    @SdkSuppress(minSdkVersion = 33) // HDR is supported from API 33.
    @Test
    fun renderByHlg(): Unit = runBlocking(glDispatcher) {
        testRender(OutputType.SURFACE_TEXTURE, dynamicRange = DynamicRange.HLG_10_BIT)
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun renderByCustomShader(): Unit = runBlocking(glDispatcher) {
        testRender(OutputType.IMAGE_READER, shaderProvider = createCustomShaderProvider())
    }

    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = 22)
    @Test
    fun renderByCustomShaderBelowApi23(): Unit = runBlocking(glDispatcher) {
        testRender(OutputType.SURFACE_TEXTURE, shaderProvider = createCustomShaderProvider())
    }

    @Test
    fun unregisterOutputSurface(): Unit = runBlocking(glDispatcher) {
        // Arrange.
        createOpenGlRendererAndInit()

        // Prepare output
        val outputSurface = createAutoReleaseSurface()

        // Assert.
        glRenderer.registerOutputSurface(outputSurface)
        assertThat(glRenderer.mOutputSurfaceMap[outputSurface]).isNotNull()
        glRenderer.unregisterOutputSurface(outputSurface)
        assertThat(glRenderer.mOutputSurfaceMap[outputSurface]).isNull()
    }

    private suspend fun testRender(
        outputType: OutputType,
        dynamicRange: DynamicRange = DynamicRange.SDR,
        shaderProvider: ShaderProvider = ShaderProvider.DEFAULT
    ) {
        // Arrange.
        prepareCamera()
        assumeDynamicRange(dynamicRange)
        createOpenGlRendererAndInit(dynamicRange = dynamicRange, shaderProvider = shaderProvider)

        // Prepare input
        val surfaceTexture = SurfaceTexture(glRenderer.textureName).apply {
            setDefaultBufferSize(WIDTH, HEIGHT)
        }
        val inputSurface = Surface(surfaceTexture)
        openCameraAndSetRepeating(inputSurface, dynamicRange)
        cameraDeviceHolder.closedFuture.addListener({
            inputSurface.release()
            surfaceTexture.release()
        }, CameraXExecutors.directExecutor())

        // Prepare output
        renderOutput = RenderOutput.createRenderOutput(outputType)
        val outputSurface = renderOutput.surface

        // Bridge input to output
        glRenderer.registerOutputSurface(outputSurface)
        surfaceTexture.setOnFrameAvailableListener({
            it.updateTexImage()
            glRenderer.render(0L, IDENTITY_MATRIX, outputSurface)
        }, glHandler)

        // Assert.
        assertThat(renderOutput.await(/*imageCount=*/5, /*timeoutInMs=*/10_000L)).isTrue()
    }

    private suspend fun createOpenGlRendererAndInit(
        dynamicRange: DynamicRange = DynamicRange.SDR,
        shaderProvider: ShaderProvider = ShaderProvider.DEFAULT
    ) {
        createOpenGlRenderer()

        if (currentCoroutineContext()[ContinuationInterceptor] == glDispatcher) {
            // same dispatcher, init directly
            glRenderer.init(dynamicRange, shaderProvider)
        } else {
            runBlocking(glDispatcher) {
                glRenderer.init(dynamicRange, shaderProvider)
            }
        }
    }

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

    private fun createOpenGlRenderer() {
        glRenderer = OpenGlRenderer()
    }

    private fun createAutoReleaseSurface(): Surface {
        val surfaceTexture = SurfaceTexture(0)
        surfaceTexture.setDefaultBufferSize(WIDTH, HEIGHT)
        surfaceTexturesToRelease.add(surfaceTexture)
        val surface = Surface(surfaceTexture)
        surfacesToRelease.add(surface)

        return surface
    }

    private fun prepareCamera() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing))
        cameraId = CameraUtil.getCameraIdWithLensFacing(lensFacing)!!
        val cameraCharacteristics = CameraUtil.getCameraCharacteristics(lensFacing)!!
        cameraCharacteristicsCompat = CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
            cameraCharacteristics,
            cameraId
        )
    }

    private fun assumeDynamicRange(dynamicRange: DynamicRange) {
        if (dynamicRange == DynamicRange.SDR) {
            // SDR is always supported.
            return
        }
        val supportedDynamicRange =
            DynamicRangesCompat.fromCameraCharacteristics(cameraCharacteristicsCompat)
                .supportedDynamicRanges
        assumeTrue(
            "$dynamicRange is not in supported set $supportedDynamicRange",
            supportedDynamicRange.contains(dynamicRange)
        )
    }

    private fun openCameraAndSetRepeating(surface: Surface, dynamicRange: DynamicRange) {
        // Open camera
        cameraDeviceHolder = CameraUtil.getCameraDevice(cameraId, null)

        // Create capture session
        val captureSessionHolder = if (dynamicRange == DynamicRange.SDR) {
            cameraDeviceHolder.createCaptureSession(listOf(surface))
        } else {
            if (Build.VERSION.SDK_INT >= 33) {
                val outputConfiguration = OutputConfiguration(surface).apply {
                    dynamicRangeProfile = dynamicRange.toDynamicRangeProfile()
                }
                cameraDeviceHolder.createCaptureSessionByOutputConfigurations(
                    listOf(outputConfiguration)
                )
            } else {
                throw AssertionError("HDR is supported from API 33")
            }
        }

        // Set repeating
        captureSessionHolder.startRepeating(
            CameraDevice.TEMPLATE_PREVIEW,
            listOf(surface),
            null,
            null
        )
    }

    @RequiresApi(33)
    private fun DynamicRange.toDynamicRangeProfile(): Long {
        val dynamicRangeProfiles =
            DynamicRangesCompat.fromCameraCharacteristics(cameraCharacteristicsCompat)
                .toDynamicRangeProfiles()!!
        return DynamicRangeConversions.dynamicRangeToFirstSupportedProfile(
            this,
            dynamicRangeProfiles
        )!!
    }
}
