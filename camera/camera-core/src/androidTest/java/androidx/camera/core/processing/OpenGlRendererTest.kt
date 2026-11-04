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

import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.DataSpace
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.OutputConfiguration
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.processing.HdrYuvInputTestUtil.Y_LIMITED_BLACK
import androidx.camera.core.processing.HdrYuvInputTestUtil.Y_LIMITED_WHITE
import androidx.camera.core.processing.HdrYuvInputTestUtil.assertNeutralGray
import androidx.camera.core.processing.HdrYuvInputTestUtil.assertWithinTolerance
import androidx.camera.core.processing.HdrYuvInputTestUtil.createP010ImageWriter
import androidx.camera.core.processing.HdrYuvInputTestUtil.fullRange8BitLevel
import androidx.camera.core.processing.HdrYuvInputTestUtil.isHdrYuvSamplingSupported
import androidx.camera.core.processing.HdrYuvInputTestUtil.queueP010Frame
import androidx.camera.core.processing.util.GLUtils.InputFormat
import androidx.camera.core.processing.util.GraphicDeviceInfo
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
class OpenGlRendererTest {

    companion object {
        private const val WIDTH = 640
        private const val HEIGHT = 480
        private const val CUSTOM_SHADER_FORMAT =
            """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        uniform samplerExternalOES %s;
        uniform float uAlphaScale;
        varying vec2 %s;
        void main() {
          vec4 sampleColor = texture2D(%s, %s);
          vec4 src = vec4(sampleColor.r * 0.493 + sampleColor. g * 0.769 +
             sampleColor.b * 0.289, sampleColor.r * 0.449 + sampleColor.g * 0.686 +
             sampleColor.b * 0.268, sampleColor.r * 0.272 + sampleColor.g * 0.534 +
             sampleColor.b * 0.131, 1.0);
          gl_FragColor = vec4(src.rgb, src.a * uAlphaScale);
        }
        """

        private val IDENTITY_MATRIX = createGlIdentityMatrix()

        // For the input color range tests.
        private const val COLOR_RANGE_TEST_SIZE = 64
        private const val COLOR_RANGE_TEST_FRAME_TIMEOUT_MS = 5_000L

        private fun createGlIdentityMatrix() = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }

        @JvmStatic lateinit var testCameraRule: CameraUtil.PreTestCamera

        @JvmStatic lateinit var testCameraIdListRule: CameraUtil.PreTestCameraIdList

        @BeforeClass
        @JvmStatic
        fun classSetup() {
            // In order to test camera just once, keep the test rules.
            testCameraRule = CameraUtil.PreTestCamera()
            testCameraIdListRule = CameraUtil.PreTestCameraIdList()
        }
    }

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(testCameraRule, testCameraIdListRule)

    private lateinit var glThread: HandlerThread
    private lateinit var glHandler: Handler
    private lateinit var glDispatcher: CoroutineDispatcher
    private lateinit var glRenderer: OpenGlRenderer
    private lateinit var cameraId: String
    private lateinit var cameraCharacteristics: CameraCharacteristics
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
            withContext(glDispatcher) { glRenderer.release() }
        }
        if (::glThread.isInitialized) {
            glThread.quitSafely()
        }
    }

    @Test
    fun drawInputSurface_snapshotReturnsTheSame(): Unit =
        runBlocking(glDispatcher) {
            // Arrange: set up renderer and Surface.
            createOpenGlRendererAndInit()
            val surfaceTexture =
                SurfaceTexture(glRenderer.textureName).apply {
                    setDefaultBufferSize(WIDTH, HEIGHT)
                    surfaceTexturesToRelease.add(this)
                }
            val inputSurface = Surface(surfaceTexture).apply { surfacesToRelease.add(this) }
            // Listen for OnFrameAvailable updates before drawing.
            val deferredOnFrameAvailable = CompletableDeferred<Unit>()
            surfaceTexture.setOnFrameAvailableListener(
                { deferredOnFrameAvailable.complete(Unit) },
                Handler(Looper.getMainLooper()),
            )

            // Draw bitmap to inputSurface.
            val inputImage = createBitmap(WIDTH, HEIGHT)
            val canvas = inputSurface.lockHardwareCanvas()
            canvas.drawBitmap(inputImage, 0f, 0f, null)
            inputSurface.unlockCanvasAndPost(canvas)

            // Wait for frame available and update texture.
            withTimeoutOrNull(5_000) { deferredOnFrameAvailable.await() }
                ?: fail("Timed out waiting for SurfaceTexture frame available.")
            surfaceTexture.updateTexImage()

            // Act: take a snapshot.
            val matrix = FloatArray(16)
            Matrix.setIdentityM(matrix, 0)
            val result = glRenderer.snapshot(Size(WIDTH, HEIGHT), matrix)

            // Assert: the result is identical to the input.
            assertThat(getAverageDiff(result, inputImage)).isEqualTo(0)
        }

    @Test
    fun getTextureNameWithoutInit_throwException(): Unit =
        runBlocking(glDispatcher) {
            createOpenGlRenderer()
            assertThrows(IllegalStateException::class.java) { glRenderer.textureName }
        }

    @Test
    fun registerOutputSurfaceWithoutInit_throwException(): Unit =
        runBlocking(glDispatcher) {
            createOpenGlRenderer()
            assertThrows(IllegalStateException::class.java) {
                glRenderer.registerOutputSurface(createAutoReleaseSurface())
            }
        }

    @Test
    fun renderWithoutInit_throwException(): Unit =
        runBlocking(glDispatcher) {
            createOpenGlRenderer()
            assertThrows(IllegalStateException::class.java) {
                glRenderer.render(123L, IDENTITY_MATRIX, createAutoReleaseSurface())
            }
        }

    @Test
    fun unregisterOutputSurfaceWithoutInit_throwException(): Unit =
        runBlocking(glDispatcher) {
            createOpenGlRenderer()
            assertThrows(IllegalStateException::class.java) {
                glRenderer.unregisterOutputSurface(createAutoReleaseSurface())
            }
        }

    @Test
    fun releaseWithoutInit_noOp(): Unit =
        runBlocking(glDispatcher) {
            createOpenGlRenderer()
            glRenderer.release()
        }

    @Test
    fun getTextureNameOnNonGlThread_throwException(): Unit = runBlocking {
        createOpenGlRendererAndInit()
        assertThrows(IllegalStateException::class.java) { glRenderer.textureName }
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
        assertThrows(IllegalStateException::class.java) { glRenderer.release() }
    }

    @Test
    fun initByInvalidShaderString_throwException() {
        val shaderProvider = createCustomShaderProvider(shaderString = "Invalid shader")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking(glDispatcher) {
                createOpenGlRendererAndInit(
                    shaderProviderOverrides = mapOf(InputFormat.DEFAULT to shaderProvider)
                )
            }
        }
    }

    @Test
    fun initByFailedShaderProvider_throwException() {
        val shaderProvider =
            createCustomShaderProvider(exceptionToThrow = RuntimeException("Failed Shader"))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking(glDispatcher) {
                createOpenGlRendererAndInit(
                    shaderProviderOverrides = mapOf(InputFormat.DEFAULT to shaderProvider)
                )
            }
        }
    }

    @Test
    fun initByIncorrectSamplerName_throwException() {
        val shaderProvider = createCustomShaderProvider(samplerVarName = "_mySampler_")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking(glDispatcher) {
                createOpenGlRendererAndInit(
                    shaderProviderOverrides = mapOf(InputFormat.DEFAULT to shaderProvider)
                )
            }
        }
    }

    @Test
    fun initByIncorrectFragCoordsName_throwException() {
        val shaderProvider = createCustomShaderProvider(fragCoordsVarName = "_myFragCoords_")
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking(glDispatcher) {
                createOpenGlRendererAndInit(
                    shaderProviderOverrides = mapOf(InputFormat.DEFAULT to shaderProvider)
                )
            }
        }
    }

    @Test
    fun init_returnsFormattedVersionStrings() =
        runBlocking(glDispatcher) {
            val graphicsDeviceInfo = initRender()
            val versionRegex = "([0-9]+)\\.([0-9]+)"
            assertThat(graphicsDeviceInfo.glVersion).matches(versionRegex)
            assertThat(graphicsDeviceInfo.eglVersion).matches(versionRegex)
        }

    @Test
    fun reInit(): Unit =
        runBlocking(glDispatcher) {
            createOpenGlRendererAndInit()
            glRenderer.release()
            glRenderer.init(DynamicRange.SDR)
            assertThat(glRenderer.textureName).isNotEqualTo(0L)
        }

    @Test fun render(): Unit = runBlocking(glDispatcher) { testRender(OutputType.IMAGE_READER) }

    @SdkSuppress(minSdkVersion = 33) // HDR is supported from API 33.
    @Test
    fun renderByHlg(): Unit =
        runBlocking(glDispatcher) {
            val dynamicRange = DynamicRange.HLG_10_BIT
            val graphicsDeviceInfo = initRender(dynamicRange = dynamicRange)
            // Without the EGL_EXT_gl_colorspace_bt2020_hlg extension, we can't set the dataspace on
            // the output surface, so we shouldn't expect a specified standard and transfer.
            // As a fallback in the future, we can tonemap to PQ and make use of the
            // EGL_EXT_gl_colorspace_bt2020_pq, should it exist on the device.
            val (expectedStandard, expectedTransfer) =
                if (graphicsDeviceInfo.eglExtensions.contains("EGL_EXT_gl_colorspace_bt2020_hlg")) {
                    Pair(DataSpace.STANDARD_BT2020, DataSpace.TRANSFER_HLG)
                } else {
                    Pair(null, null)
                }

            testRender(
                OutputType.SURFACE_TEXTURE,
                dynamicRange = dynamicRange,
                shouldInit = false, /* already initialized */
                expectedStandard = expectedStandard,
                expectedTransfer = expectedTransfer,
            )
        }

    @SdkSuppress(minSdkVersion = 33) // HDR and ImageWriter data spaces are supported from API 33.
    @Test
    fun renderHlgYuv_limitedRangeInput_limitedBlackAndWhiteMapToFullScale(): Unit = runBlocking {
        initHdrYuvRendererWithoutCamera()
        val limitedRange = HdrYuvInputTestUtil.bt2020HlgDataSpace(DataSpace.RANGE_LIMITED)

        // Limited range black (64) and white (940) are stretched to 0 and 255.
        assertWithinTolerance(renderP010Gray(limitedRange, Y_LIMITED_BLACK), expected = 0)
        assertWithinTolerance(renderP010Gray(limitedRange, Y_LIMITED_WHITE), expected = 255)
    }

    @SdkSuppress(minSdkVersion = 33) // HDR and ImageWriter data spaces are supported from API 33.
    @Test
    fun renderHlgYuv_fullRangeInput_codeValuesAreNotStretched(): Unit = runBlocking {
        initHdrYuvRendererWithoutCamera()
        val fullRange = HdrYuvInputTestUtil.bt2020HlgDataSpace(DataSpace.RANGE_FULL)

        // In full range, 64 and 940 are not black and white, but 64/1023 and 940/1023. Without
        // considering the input range they would be wrongly mapped to 0 and 255.
        assertWithinTolerance(
            renderP010Gray(fullRange, Y_LIMITED_BLACK),
            expected = fullRange8BitLevel(Y_LIMITED_BLACK),
        )
        assertWithinTolerance(
            renderP010Gray(fullRange, Y_LIMITED_WHITE),
            expected = fullRange8BitLevel(Y_LIMITED_WHITE),
        )
    }

    @Test
    fun renderByCustomShader(): Unit =
        runBlocking(glDispatcher) {
            val shaderProviderOverride = createCustomShaderProvider()
            testRender(
                OutputType.IMAGE_READER,
                shaderProviderOverrides = mapOf(InputFormat.DEFAULT to shaderProviderOverride),
            )
        }

    @Test
    fun unregisterOutputSurface(): Unit =
        runBlocking(glDispatcher) {
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

    private suspend fun initRender(
        dynamicRange: DynamicRange = DynamicRange.SDR,
        shaderProviderOverrides: Map<InputFormat, ShaderProvider> = emptyMap(),
    ): GraphicDeviceInfo {
        prepareCamera()
        assumeDynamicRange(dynamicRange)
        return createOpenGlRendererAndInit(
            dynamicRange = dynamicRange,
            shaderProviderOverrides = shaderProviderOverrides,
        )
    }

    private suspend fun testRender(
        outputType: OutputType,
        dynamicRange: DynamicRange = DynamicRange.SDR,
        shaderProviderOverrides: Map<InputFormat, ShaderProvider> = emptyMap(),
        shouldInit: Boolean = true,
        expectedStandard: Int? = null,
        expectedTransfer: Int? = null,
    ) {
        // Arrange.
        if (shouldInit) {
            initRender(dynamicRange, shaderProviderOverrides)
        }

        // Prepare input
        val surfaceTexture =
            SurfaceTexture(glRenderer.textureName).apply { setDefaultBufferSize(WIDTH, HEIGHT) }
        val inputSurface = Surface(surfaceTexture)
        openCameraAndSetRepeating(inputSurface, dynamicRange)
        cameraDeviceHolder.closedFuture.addListener(
            {
                inputSurface.release()
                surfaceTexture.release()
            },
            CameraXExecutors.directExecutor(),
        )

        // Prepare output
        renderOutput = RenderOutput.createRenderOutput(outputType)
        val outputSurface = renderOutput.surface

        // Bridge input to output
        glRenderer.registerOutputSurface(outputSurface)
        surfaceTexture.setOnFrameAvailableListener(
            {
                it.updateTexImage()
                glRenderer.render(0L, IDENTITY_MATRIX, outputSurface)
            },
            glHandler,
        )

        // Assert.
        assertThat(renderOutput.await(/* imageCount= */ 5, /* timeoutInMs= */ 10_000L)).isTrue()
        expectedStandard?.let {
            if (Build.VERSION.SDK_INT >= 33) {
                assertThat(DataSpace.getStandard(renderOutput.dataSpace)).isEqualTo(it)
            } else {
                fail("Color standard only supported on API 33 and above.")
            }
        }
        expectedTransfer?.let {
            if (Build.VERSION.SDK_INT >= 33) {
                assertThat(DataSpace.getTransfer(renderOutput.dataSpace)).isEqualTo(it)
            } else {
                fail("Transfer function only supported on API 33 and above.")
            }
        }
    }

    private suspend fun createOpenGlRendererAndInit(
        dynamicRange: DynamicRange = DynamicRange.SDR,
        shaderProviderOverrides: Map<InputFormat, ShaderProvider> = emptyMap(),
    ): GraphicDeviceInfo {
        createOpenGlRenderer()

        return if (currentCoroutineContext()[ContinuationInterceptor] == glDispatcher) {
            // same dispatcher, init directly
            glRenderer.init(dynamicRange, shaderProviderOverrides)
        } else {
            runBlocking(glDispatcher) {
                return@runBlocking glRenderer.init(dynamicRange, shaderProviderOverrides)
            }
        }
    }

    private fun createCustomShaderProvider(
        samplerVarName: String? = null,
        fragCoordsVarName: String? = null,
        shaderString: String? = null,
        exceptionToThrow: Exception? = null,
    ) =
        object : ShaderProvider {
            override fun createFragmentShader(
                correctSamplerVarName: String,
                correctFragCoordsVarName: String,
            ): String {
                exceptionToThrow?.let { throw it }
                return shaderString
                    ?: String.format(
                        Locale.US,
                        CUSTOM_SHADER_FORMAT,
                        samplerVarName ?: correctSamplerVarName,
                        fragCoordsVarName ?: correctFragCoordsVarName,
                        samplerVarName ?: correctSamplerVarName,
                        fragCoordsVarName ?: correctFragCoordsVarName,
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
        cameraCharacteristics = CameraUtil.getCameraCharacteristics(lensFacing)!!
    }

    private fun assumeDynamicRange(dynamicRange: DynamicRange) {
        if (dynamicRange == DynamicRange.SDR) {
            // SDR is always supported.
            return
        }
        val supportedDynamicRange = getSupportedDynamicRangeProfiles()
        assumeTrue(
            "$dynamicRange is not in supported set $supportedDynamicRange",
            supportedDynamicRange.contains(dynamicRange.toDynamicRangeProfile()),
        )
    }

    private fun openCameraAndSetRepeating(surface: Surface, dynamicRange: DynamicRange) {
        // Open camera
        cameraDeviceHolder = CameraUtil.getCameraDevice(cameraId, null)

        // Create capture session
        val captureSessionHolder =
            if (dynamicRange == DynamicRange.SDR) {
                cameraDeviceHolder.createCaptureSession(listOf(surface))
            } else {
                if (Build.VERSION.SDK_INT >= 33) {
                    val outputConfiguration =
                        OutputConfiguration(surface).apply {
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
            null,
        )
    }

    private fun DynamicRange.toDynamicRangeProfile(): Long {
        return when (this) {
            DynamicRange.SDR -> DynamicRangeProfiles.STANDARD
            DynamicRange.HLG_10_BIT -> DynamicRangeProfiles.HLG10
            else -> throw IllegalArgumentException("Not support in this test")
        }
    }

    private fun getSupportedDynamicRangeProfiles(): Set<Long> {
        return if (Build.VERSION.SDK_INT >= 33) {
            // Dynamic range API is from API 33.
            cameraCharacteristics
                .get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)
                ?.supportedProfiles ?: setOf(DynamicRangeProfiles.STANDARD)
        } else {
            setOf(DynamicRangeProfiles.STANDARD)
        }
    }

    /**
     * Initializes [glRenderer] for HDR with YUV input, like the HDR camera path, but without
     * opening a camera. The input frames are produced by [renderP010Gray] instead.
     */
    @Suppress("NewApi") // Only called by tests suppressed below API 33.
    private suspend fun initHdrYuvRendererWithoutCamera() {
        assumeTrue("HDR YUV sampling is not supported.", isHdrYuvSamplingSupported())
        createOpenGlRendererAndInit(DynamicRange.HLG_10_BIT)
        withContext(glDispatcher) { glRenderer.setInputFormat(InputFormat.YUV) }
    }

    /**
     * Renders a P010 frame filled with [yValue] and neutral chroma in [dataSpace] with
     * [glRenderer], and returns the gray level (0-255) of the output.
     */
    @Suppress("NewApi") // Only called by tests suppressed below API 33.
    private suspend fun renderP010Gray(dataSpace: Int, yValue: Int): Int {
        val size = COLOR_RANGE_TEST_SIZE
        val surfaceTexture =
            withContext(glDispatcher) {
                SurfaceTexture(glRenderer.textureName).apply { setDefaultBufferSize(size, size) }
            }
        val surface = Surface(surfaceTexture)
        val imageWriter = createP010ImageWriter(surface, size, size, dataSpace)
        try {
            assumeTrue("P010 ImageWriter is not supported.", imageWriter != null)
            val frameAvailable = CompletableDeferred<Unit>()
            surfaceTexture.setOnFrameAvailableListener({ frameAvailable.complete(Unit) }, glHandler)
            assumeTrue(
                "Failed to write a P010 frame by CPU.",
                queueP010Frame(checkNotNull(imageWriter), size, size, yValue),
            )

            assertThat(
                    withTimeoutOrNull(COLOR_RANGE_TEST_FRAME_TIMEOUT_MS) { frameAvailable.await() }
                )
                .isNotNull()

            val pixel =
                withContext(glDispatcher) {
                    val isUpdated =
                        try {
                            surfaceTexture.updateTexImage()
                            true
                        } catch (e: RuntimeException) {
                            false
                        }
                    // Some GPUs can't sample P010 buffers written by CPU.
                    assumeTrue("Failed to sample the P010 buffer by GPU.", isUpdated)
                    assumeTrue(
                        "The data space is not propagated to SurfaceTexture.",
                        surfaceTexture.dataSpace == dataSpace,
                    )
                    val textureMatrix = FloatArray(16)
                    surfaceTexture.getTransformMatrix(textureMatrix)
                    // Same as what DefaultSurfaceProcessor does for each frame.
                    glRenderer.setInputFullRange(
                        DefaultSurfaceProcessor.isFullRangeDataSpace(surfaceTexture.dataSpace)
                    )
                    glRenderer
                        .snapshot(Size(size, size), textureMatrix)
                        .getPixel(size / 2, size / 2)
                }
            assertNeutralGray(pixel)
            // R, G and B are equal (within tolerance), so any channel can be returned as the gray
            // level. This is the return value, not a check.
            return Color.green(pixel)
        } finally {
            imageWriter?.close()
            surface.release()
            withContext(glDispatcher) { surfaceTexture.release() }
        }
    }
}
