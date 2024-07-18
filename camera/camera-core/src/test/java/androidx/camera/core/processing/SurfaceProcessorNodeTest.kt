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
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.SurfaceRequest.TransformationInfo
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.impl.ImageOutputConfig.ROTATION_NOT_SPECIFIED
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.utils.TransformUtils
import androidx.camera.core.impl.utils.TransformUtils.getRectToRect
import androidx.camera.core.impl.utils.TransformUtils.is90or270
import androidx.camera.core.impl.utils.TransformUtils.rectToSize
import androidx.camera.core.impl.utils.TransformUtils.sizeToRect
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.processing.SurfaceProcessorNode.OutConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.fakes.FakeImageReaderProxy
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessorInternal
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [SurfaceProcessorNode].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SurfaceProcessorNodeTest {

    companion object {
        private const val INPUT_ROTATION_DEGREES = 90
        private const val VIDEO_ROTATION_DEGREES = 0
        private const val MIRRORING = false
        private val INPUT_SIZE = Size(640, 480)
        private val PREVIEW_CROP_RECT = Rect(0, 0, 600, 400)
        private val VIDEO_CROP_RECT = Rect(0, 0, 300, 200)
        private val VIDEO_SIZE = Size(30, 20)
    }

    private lateinit var surfaceProcessorInternal: FakeSurfaceProcessorInternal
    private lateinit var previewSurface: Surface
    private lateinit var previewTexture: SurfaceTexture
    private lateinit var previewOutConfig: OutConfig
    private lateinit var videoSurface: Surface
    private lateinit var videoTexture: SurfaceTexture
    private lateinit var videoOutConfig: OutConfig
    private lateinit var node: SurfaceProcessorNode
    private lateinit var nodeInput: SurfaceProcessorNode.In
    private lateinit var previewSurfaceRequest: SurfaceRequest
    private lateinit var videoSurfaceRequest: SurfaceRequest
    private lateinit var previewTransformInfo: TransformationInfo
    private lateinit var videoTransformInfo: TransformationInfo

    @Before
    fun setup() {
        previewTexture = SurfaceTexture(0)
        previewSurface = Surface(previewTexture)
        videoTexture = SurfaceTexture(0)
        videoSurface = Surface(videoTexture)
        surfaceProcessorInternal = FakeSurfaceProcessorInternal(mainThreadExecutor())
    }

    @After
    fun tearDown() {
        previewTexture.release()
        previewSurface.release()
        videoTexture.release()
        videoSurface.release()
        surfaceProcessorInternal.release()
        if (::node.isInitialized) {
            node.release()
        }
        if (::nodeInput.isInitialized) {
            nodeInput.surfaceEdge.close()
        }
        if (::previewSurfaceRequest.isInitialized) {
            previewSurfaceRequest.deferrableSurface.close()
        }
        if (::videoSurfaceRequest.isInitialized) {
            videoSurfaceRequest.deferrableSurface.close()
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    fun transformInput_getCorrectSensorToBufferMatrix() {
        // Arrange.
        createSurfaceProcessorNode()
        val inputTransform = getRectToRect(
            RectF(0F, 0F, 1400F, 1000F),
            RectF(0F, 0F, 700F, 500F),
            /*rotationDegrees=*/0
        )
        val inputEdge = SurfaceEdge(
            PREVIEW,
            INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            StreamSpec.builder(Size(700, 500)).build(),
            inputTransform,
            false,
            Rect(60, 20, 700, 500), // 640 x 480 crop rect
            180,
            ROTATION_NOT_SPECIFIED,
            true
        )
        nodeInput = SurfaceProcessorNode.In.of(inputEdge, listOf(OutConfig.of(inputEdge)))

        // Act.
        val outputTransform =
            node.transform(nodeInput).entries.single().value.sensorToBufferTransform

        // Assert.
        val sensorCoordinates = floatArrayOf(1400F, 1000F)
        outputTransform.mapPoints(sensorCoordinates)
        // Sensor to input surface: 1400, 1000 -> 700, 500
        // Input surface cropped: 700, 500 -> 640, 480
        // Input surface rotated 180 degrees: 640, 480 -> 0, 0
        // Input surface mirrored: 0, 0 -> 640, 0
        assertThat(sensorCoordinates).usingTolerance(1E-3).containsExactly(640F, 0F)
    }

    @Test
    fun inputHasNoCameraTransform_surfaceOutputReceivesNullCamera() {
        // Arrange: configure node to produce JPEG output.
        createSurfaceProcessorNode()
        createInputEdge(
            inputEdge = SurfaceEdge(
                PREVIEW,
                INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                StreamSpec.builder(INPUT_SIZE).build(),
                Matrix(),
                false,
                PREVIEW_CROP_RECT,
                0,
                ROTATION_NOT_SPECIFIED,
                false
            )
        )

        // Act.
        val nodeOutput = node.transform(nodeInput)
        provideSurfaces(nodeOutput)
        shadowOf(getMainLooper()).idle()

        val previewSurfaceOutput =
            surfaceProcessorInternal.surfaceOutputs[PREVIEW]!! as SurfaceOutputImpl
        assertThat(previewSurfaceOutput.camera).isNull()
        val videoSurfaceOutput =
            surfaceProcessorInternal.surfaceOutputs[VIDEO_CAPTURE]!! as SurfaceOutputImpl
        assertThat(videoSurfaceOutput.camera).isNull()
    }

    @Test
    fun configureJpegOutput_returnsJpegFormat() {
        // Arrange: configure node to produce JPEG output.
        createSurfaceProcessorNode()
        val inputEdge = SurfaceEdge(
            PREVIEW,
            INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            StreamSpec.builder(INPUT_SIZE).build(),
            Matrix(),
            true,
            PREVIEW_CROP_RECT,
            0,
            ROTATION_NOT_SPECIFIED,
            false
        )
        val outConfig = OutConfig.of(
            IMAGE_CAPTURE,
            ImageFormat.JPEG,
            inputEdge.cropRect,
            TransformUtils.getRotatedSize(inputEdge.cropRect, inputEdge.rotationDegrees),
            inputEdge.rotationDegrees,
            inputEdge.mirroring
        )
        nodeInput = SurfaceProcessorNode.In.of(inputEdge, listOf(outConfig))
        // Act.
        val out = node.transform(nodeInput)
        shadowOf(getMainLooper()).idle()
        // Assert: the output is JPEG format.
        val outEdge = out[outConfig]!!
        assertThat(outEdge.format).isEqualTo(ImageFormat.JPEG)
        // Act: provides a JPEG Surface.
        val imageReader = FakeImageReaderProxy.newInstance(
            INPUT_SIZE.width,
            INPUT_SIZE.height,
            ImageFormat.JPEG,
            1,
            0
        )
        val outputDeferrableSurface = ImmediateSurface(
            imageReader.surface!!,
            Size(PREVIEW_CROP_RECT.width(), PREVIEW_CROP_RECT.height()),
            ImageFormat.JPEG
        )
        outEdge.setProvider(outputDeferrableSurface)
        shadowOf(getMainLooper()).idle()
        // Assert: SurfaceProcessor receives a JPEG SurfaceOutput.
        val imageCaptureOutput = surfaceProcessorInternal.surfaceOutputs[IMAGE_CAPTURE]!!
        assertThat(imageCaptureOutput.format).isEqualTo(ImageFormat.JPEG)
        imageReader.close()
    }

    @Test
    fun identicalOutConfigs_returnDifferentEdges() {
        // Arrange: create 2 OutConfig with identical values
        createSurfaceProcessorNode()
        val inputEdge = SurfaceEdge(
            PREVIEW,
            INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            StreamSpec.builder(INPUT_SIZE).build(),
            Matrix(),
            true,
            PREVIEW_CROP_RECT,
            0,
            ROTATION_NOT_SPECIFIED,
            false
        )
        val outConfig1 = OutConfig.of(inputEdge)
        val outConfig2 = OutConfig.of(inputEdge)
        nodeInput = SurfaceProcessorNode.In.of(inputEdge, listOf(outConfig1, outConfig2))
        // Act.
        val output = node.transform(nodeInput)
        // Assert: there are two outputs
        assertThat(output).hasSize(2)
        // Cleanup
        inputEdge.close()
    }

    @Test
    fun transformInput_receivesSurfaceRequest() {
        // Arrange.
        createSurfaceProcessorNode()
        createInputEdge()
        // Act.
        node.transform(nodeInput)
        shadowOf(getMainLooper()).idle()
        // Assert.
        val surfaceRequest = surfaceProcessorInternal.surfaceRequest
        assertThat(surfaceRequest!!.resolution).isEqualTo(INPUT_SIZE)
    }

    @Test
    fun transformInput_applyCropRotateAndMirroring_outputIsCroppedAndRotated() {
        for (rotationDegrees in arrayOf(0, 90, 180, 270)) {
            // Arrange.
            createSurfaceProcessorNode()
            createInputEdge(
                inputRotationDegrees = rotationDegrees,
            )
            // The result cropRect should have zero left and top.
            val expectedCropRect = if (is90or270(rotationDegrees))
                Rect(0, 0, PREVIEW_CROP_RECT.height(), PREVIEW_CROP_RECT.width())
            else
                Rect(0, 0, PREVIEW_CROP_RECT.width(), PREVIEW_CROP_RECT.height())

            // Act.
            val nodeOutput = node.transform(nodeInput)

            // Assert: with transformation, the output size is cropped/rotated and the rotation
            // degrees is reset.
            val previewOutput = nodeOutput[previewOutConfig]!!
            assertThat(previewOutput.streamSpec.resolution).isEqualTo(rectToSize(expectedCropRect))
            assertThat(previewOutput.cropRect).isEqualTo(expectedCropRect)
            assertThat(previewOutput.rotationDegrees).isEqualTo(0)
            assertThat(previewOutput.mirroring).isFalse()
            val videoOutput = nodeOutput[videoOutConfig]!!
            assertThat(videoOutput.streamSpec.resolution).isEqualTo(VIDEO_SIZE)
            assertThat(videoOutput.cropRect).isEqualTo(sizeToRect(VIDEO_SIZE))
            assertThat(videoOutput.rotationDegrees).isEqualTo(rotationDegrees)
            assertThat(videoOutput.mirroring).isTrue()

            // Clean up.
            nodeInput.surfaceEdge.close()
            node.release()
            shadowOf(getMainLooper()).idle()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun cropSizeMismatchesOutputSize_throwsException() {
        createSurfaceProcessorNode()
        createInputEdge(
            videoOutputSize = Size(VIDEO_SIZE.width - 2, VIDEO_SIZE.height + 2)
        )
        node.transform(nodeInput)
    }

    @Test
    fun transformInputWithFrameRate_propagatesToChildren() {
        // Arrange: create input edge with frame rate.
        val frameRateRange = Range.create(30, 30)
        createSurfaceProcessorNode()
        createInputEdge(
            frameRateRange = frameRateRange
        )
        // Act.
        val nodeOutput = node.transform(nodeInput)
        // Assert: all outputs have the same frame rate.
        assertThat(nodeOutput[previewOutConfig]!!.streamSpec.expectedFrameRateRange)
            .isEqualTo(frameRateRange)
        assertThat(nodeOutput[videoOutConfig]!!.streamSpec.expectedFrameRateRange)
            .isEqualTo(frameRateRange)
    }

    @Test
    fun transformInput_applyCropRotateAndMirroring_initialTransformInfoIsPropagated() {
        // Arrange.
        createSurfaceProcessorNode()
        createInputEdge()

        // Act.
        val nodeOutput = node.transform(nodeInput)
        provideSurfaces(nodeOutput)
        shadowOf(getMainLooper()).idle()

        // Assert: surfaceOutput of SurfaceProcessor will consume the initial rotation degrees and
        // output surface will receive 0 degrees.
        val previewSurfaceOutput =
            surfaceProcessorInternal.surfaceOutputs[PREVIEW]!! as SurfaceOutputImpl
        assertThat(previewSurfaceOutput.rotationDegrees).isEqualTo(INPUT_ROTATION_DEGREES)
        assertThat(previewSurfaceOutput.size).isEqualTo(Size(400, 600))
        assertThat(previewSurfaceOutput.inputCropRect).isEqualTo(PREVIEW_CROP_RECT)
        assertThat(previewTransformInfo.cropRect).isEqualTo(Rect(0, 0, 400, 600))
        assertThat(previewTransformInfo.rotationDegrees).isEqualTo(0)
        assertThat(previewSurfaceOutput.inputSize).isEqualTo(INPUT_SIZE)
        assertThat(previewSurfaceOutput.mirroring).isFalse()
        assertThat(previewSurfaceOutput.camera).isNotNull()

        val videoSurfaceOutput =
            surfaceProcessorInternal.surfaceOutputs[VIDEO_CAPTURE]!! as SurfaceOutputImpl
        assertThat(videoSurfaceOutput.rotationDegrees).isEqualTo(VIDEO_ROTATION_DEGREES)
        assertThat(videoSurfaceOutput.size).isEqualTo(VIDEO_SIZE)
        assertThat(videoSurfaceOutput.inputCropRect).isEqualTo(VIDEO_CROP_RECT)
        assertThat(videoTransformInfo.cropRect).isEqualTo(sizeToRect(VIDEO_SIZE))
        assertThat(videoTransformInfo.rotationDegrees).isEqualTo(270)
        assertThat(videoSurfaceOutput.inputSize).isEqualTo(INPUT_SIZE)
        assertThat(videoSurfaceOutput.mirroring).isTrue()
        assertThat(videoSurfaceOutput.camera).isNotNull()
    }

    @Test
    fun setRotationToInput_applyCropRotateAndMirroring_rotationIsPropagated() {
        // Arrange.
        createSurfaceProcessorNode()
        createInputEdge(inputRotationDegrees = 90)
        val inputSurface = nodeInput.surfaceEdge
        val nodeOutput = node.transform(nodeInput)
        provideSurfaces(nodeOutput)
        shadowOf(getMainLooper()).idle()

        // Act: update rotation degrees
        inputSurface.updateTransformation(270)
        shadowOf(getMainLooper()).idle()

        // Assert: surfaceOutput of SurfaceProcessor will consume the initial rotation degrees and
        // output surface will receive the remaining degrees.
        val previewSurfaceOutput =
            surfaceProcessorInternal.surfaceOutputs[PREVIEW]!! as SurfaceOutputImpl
        assertThat(previewSurfaceOutput.rotationDegrees).isEqualTo(INPUT_ROTATION_DEGREES)
        assertThat(previewTransformInfo.rotationDegrees).isEqualTo(180)
        assertThat(previewSurfaceOutput.inputSize).isEqualTo(INPUT_SIZE)
        assertThat(previewSurfaceOutput.mirroring).isFalse()
        val videoSurfaceOutput =
            surfaceProcessorInternal.surfaceOutputs[VIDEO_CAPTURE]!! as SurfaceOutputImpl
        assertThat(videoSurfaceOutput.rotationDegrees).isEqualTo(VIDEO_ROTATION_DEGREES)
        assertThat(videoTransformInfo.rotationDegrees).isEqualTo(90)
        assertThat(videoSurfaceOutput.inputSize).isEqualTo(INPUT_SIZE)
        assertThat(videoSurfaceOutput.mirroring).isTrue()

        // Act: update rotation degrees
        inputSurface.updateTransformation(180)
        shadowOf(getMainLooper()).idle()
        // Assert: video rotation degrees is opposite of preview because it's not mirrored.
        assertThat(previewTransformInfo.rotationDegrees).isEqualTo(90)
        assertThat(videoTransformInfo.rotationDegrees).isEqualTo(180)
    }

    @Test
    fun setRotationAndMirroringToInput_applyCropRotateAndMirroring_rotationIsPropagated() {
        // Arrange.
        createSurfaceProcessorNode()
        createInputEdge(inputRotationDegrees = 90, mirroring = true)
        val inputSurface = nodeInput.surfaceEdge
        val nodeOutput = node.transform(nodeInput)
        provideSurfaces(nodeOutput)
        shadowOf(getMainLooper()).idle()

        // Act: update rotation degrees
        inputSurface.updateTransformation(270)
        shadowOf(getMainLooper()).idle()

        // Assert: surfaceOutput of SurfaceProcessor will consume the initial rotation degrees and
        // output surface will receive the remaining degrees.
        val previewSurfaceOutput =
            surfaceProcessorInternal.surfaceOutputs[PREVIEW]!! as SurfaceOutputImpl
        assertThat(previewSurfaceOutput.rotationDegrees).isEqualTo(INPUT_ROTATION_DEGREES)
        assertThat(previewTransformInfo.rotationDegrees).isEqualTo(180)
        assertThat(previewSurfaceOutput.inputSize).isEqualTo(INPUT_SIZE)
        assertThat(previewSurfaceOutput.mirroring).isTrue()
        val videoSurfaceOutput =
            surfaceProcessorInternal.surfaceOutputs[VIDEO_CAPTURE]!! as SurfaceOutputImpl
        assertThat(videoSurfaceOutput.rotationDegrees).isEqualTo(VIDEO_ROTATION_DEGREES)
        assertThat(videoTransformInfo.rotationDegrees).isEqualTo(90)
        assertThat(videoSurfaceOutput.inputSize).isEqualTo(INPUT_SIZE)
        assertThat(videoSurfaceOutput.mirroring).isTrue()

        // Act: update rotation degrees
        inputSurface.updateTransformation(180)
        shadowOf(getMainLooper()).idle()
        // Assert: video rotation is the same as preview and are compensated by mirroring.
        assertThat(previewTransformInfo.rotationDegrees).isEqualTo(270)
        assertThat(videoTransformInfo.rotationDegrees).isEqualTo(180)
    }

    @Test
    fun provideSurfaceToOutput_surfaceIsPropagatedE2E() {
        // Arrange.
        createSurfaceProcessorNode()
        createInputEdge()
        val inputSurface = nodeInput.surfaceEdge
        val nodeOutput = node.transform(nodeInput)

        // Act.
        provideSurfaces(nodeOutput)
        shadowOf(getMainLooper()).idle()

        // Assert: processor receives app Surface. CameraX receives processor Surface.
        assertThat(surfaceProcessorInternal.outputSurfaces[PREVIEW]).isEqualTo(previewSurface)
        assertThat(surfaceProcessorInternal.outputSurfaces[VIDEO_CAPTURE]).isEqualTo(videoSurface)
        assertThat(inputSurface.deferrableSurface.surface.get())
            .isEqualTo(surfaceProcessorInternal.inputSurface)
    }

    @Test
    fun releaseNode_processorIsReleased() {
        // Arrange.
        createSurfaceProcessorNode()
        createInputEdge()
        val nodeOutput = node.transform(nodeInput)
        provideSurfaces(nodeOutput)
        shadowOf(getMainLooper()).idle()

        // Act: release the node.
        node.release()
        shadowOf(getMainLooper()).idle()

        // Assert: processor is released and has requested processor to close the SurfaceOutput
        assertThat(surfaceProcessorInternal.isReleased).isTrue()
        assertThat(surfaceProcessorInternal.isOutputSurfaceRequestedToClose[PREVIEW]).isTrue()
        assertThat(surfaceProcessorInternal.isOutputSurfaceRequestedToClose[VIDEO_CAPTURE]).isTrue()
    }

    private fun createInputEdge(
        previewTarget: Int = PREVIEW,
        previewSize: Size = INPUT_SIZE,
        sensorToBufferTransform: Matrix = Matrix(),
        hasCameraTransform: Boolean = true,
        previewCropRect: Rect = PREVIEW_CROP_RECT,
        inputRotationDegrees: Int = INPUT_ROTATION_DEGREES,
        mirroring: Boolean = MIRRORING,
        videoOutputSize: Size = VIDEO_SIZE,
        frameRateRange: Range<Int> = StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED,
        inputEdge: SurfaceEdge = SurfaceEdge(
            previewTarget,
            INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            StreamSpec.builder(previewSize).setExpectedFrameRateRange(frameRateRange).build(),
            sensorToBufferTransform,
            hasCameraTransform,
            previewCropRect,
            inputRotationDegrees,
            ROTATION_NOT_SPECIFIED,
            mirroring,
        ),
    ) {
        videoOutConfig = OutConfig.of(
            VIDEO_CAPTURE,
            INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            VIDEO_CROP_RECT,
            videoOutputSize,
            VIDEO_ROTATION_DEGREES,
            true
        )
        previewOutConfig = OutConfig.of(inputEdge)
        nodeInput = SurfaceProcessorNode.In.of(
            inputEdge,
            listOf(previewOutConfig, videoOutConfig)
        )
    }

    private fun createSurfaceProcessorNode() {
        node = SurfaceProcessorNode(
            FakeCamera(),
            surfaceProcessorInternal
        )
    }

    private fun provideSurfaces(nodeOutput: SurfaceProcessorNode.Out) {
        previewSurfaceRequest =
            nodeOutput[previewOutConfig]!!.createSurfaceRequest(FakeCamera()).apply {
                setTransformationInfoListener(mainThreadExecutor()) {
                    previewTransformInfo = it
                }
                provideSurface(previewSurface, mainThreadExecutor()) { previewSurface.release() }
            }
        videoSurfaceRequest =
            nodeOutput[videoOutConfig]!!.createSurfaceRequest(FakeCamera()).apply {
                setTransformationInfoListener(mainThreadExecutor()) {
                    videoTransformInfo = it
                }
                provideSurface(videoSurface, mainThreadExecutor()) { videoSurface.release() }
            }
    }
}
