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
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.SurfaceRequest.TransformationInfo
import androidx.camera.core.impl.utils.TransformUtils.is90or270
import androidx.camera.core.impl.utils.TransformUtils.rectToSize
import androidx.camera.core.impl.utils.TransformUtils.rotateSize
import androidx.camera.core.impl.utils.TransformUtils.sizeToRect
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.processing.SurfaceProcessorNode.OutConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeSurfaceProcessorInternal
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
        private const val ROTATION_DEGREES = 90
        private const val MIRRORING = false
        private val INPUT_SIZE = Size(640, 480)
        private val PREVIEW_CROP_RECT = Rect(0, 0, 600, 400)
        private val VIDEO_CROP_RECT = Rect(0, 0, 300, 200)
        private val VIDEO_SIZE = Size(20, 30)
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
            val videoOutputSize = rotateSize(VIDEO_SIZE, rotationDegrees - ROTATION_DEGREES)
            createInputEdge(
                previewRotationDegrees = rotationDegrees,
                videoOutputSize = videoOutputSize
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
            assertThat(previewOutput.size).isEqualTo(rectToSize(expectedCropRect))
            assertThat(previewOutput.cropRect).isEqualTo(expectedCropRect)
            assertThat(previewOutput.rotationDegrees).isEqualTo(0)
            val videoOutput = nodeOutput[videoOutConfig]!!
            assertThat(videoOutput.size).isEqualTo(videoOutputSize)
            assertThat(videoOutput.cropRect).isEqualTo(sizeToRect(videoOutputSize))
            assertThat(videoOutput.rotationDegrees).isEqualTo(0)

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
    fun transformInput_applyCropRotateAndMirroring_outputHasNoMirroring() {
        for (mirroring in arrayOf(false, true)) {
            // Arrange.
            createSurfaceProcessorNode()
            createInputEdge(mirroring = mirroring)

            // Act.
            val nodeOutput = node.transform(nodeInput)

            // Assert: the mirroring of output is always false.
            assertThat(nodeOutput[previewOutConfig]!!.mirroring).isFalse()
            assertThat(nodeOutput[videoOutConfig]!!.mirroring).isFalse()

            // Clean up.
            nodeInput.surfaceEdge.close()
            node.release()
        }
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
        assertThat(previewSurfaceOutput.rotationDegrees).isEqualTo(ROTATION_DEGREES)
        assertThat(previewSurfaceOutput.size).isEqualTo(Size(400, 600))
        assertThat(previewSurfaceOutput.inputCropRect).isEqualTo(PREVIEW_CROP_RECT)
        assertThat(previewTransformInfo.cropRect).isEqualTo(Rect(0, 0, 400, 600))
        assertThat(previewTransformInfo.rotationDegrees).isEqualTo(0)
        assertThat(previewSurfaceOutput.inputSize).isEqualTo(INPUT_SIZE)

        val videoSurfaceOutput =
            surfaceProcessorInternal.surfaceOutputs[VIDEO_CAPTURE]!! as SurfaceOutputImpl
        assertThat(videoSurfaceOutput.rotationDegrees).isEqualTo(ROTATION_DEGREES)
        assertThat(videoSurfaceOutput.size).isEqualTo(VIDEO_SIZE)
        assertThat(videoSurfaceOutput.inputCropRect).isEqualTo(VIDEO_CROP_RECT)
        assertThat(videoTransformInfo.cropRect).isEqualTo(sizeToRect(VIDEO_SIZE))
        assertThat(videoTransformInfo.rotationDegrees).isEqualTo(0)
        assertThat(videoSurfaceOutput.inputSize).isEqualTo(INPUT_SIZE)
    }

    @Test
    fun setRotationToInput_applyCropRotateAndMirroring_rotationIsPropagated() {
        // Arrange.
        createSurfaceProcessorNode()
        createInputEdge(previewRotationDegrees = 90)
        val inputSurface = nodeInput.surfaceEdge
        val nodeOutput = node.transform(nodeInput)
        provideSurfaces(nodeOutput)
        shadowOf(getMainLooper()).idle()

        // Act.
        inputSurface.rotationDegrees = 270
        shadowOf(getMainLooper()).idle()

        // Assert: surfaceOutput of SurfaceProcessor will consume the initial rotation degrees and
        // output surface will receive the remaining degrees.
        val previewSurfaceOutput =
            surfaceProcessorInternal.surfaceOutputs[PREVIEW]!! as SurfaceOutputImpl
        assertThat(previewSurfaceOutput.rotationDegrees).isEqualTo(90)
        assertThat(previewTransformInfo.rotationDegrees).isEqualTo(180)
        assertThat(previewSurfaceOutput.inputSize).isEqualTo(INPUT_SIZE)
        val videoSurfaceOutput =
            surfaceProcessorInternal.surfaceOutputs[VIDEO_CAPTURE]!! as SurfaceOutputImpl
        assertThat(videoSurfaceOutput.rotationDegrees).isEqualTo(90)
        assertThat(videoTransformInfo.rotationDegrees).isEqualTo(180)
        assertThat(videoSurfaceOutput.inputSize).isEqualTo(INPUT_SIZE)
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
        sensorToBufferTransform: android.graphics.Matrix = android.graphics.Matrix(),
        hasCameraTransform: Boolean = true,
        previewCropRect: Rect = PREVIEW_CROP_RECT,
        previewRotationDegrees: Int = ROTATION_DEGREES,
        mirroring: Boolean = MIRRORING,
        videoOutputSize: Size = VIDEO_SIZE
    ) {
        val surface = SurfaceEdge(
            previewTarget,
            previewSize,
            sensorToBufferTransform,
            hasCameraTransform,
            previewCropRect,
            previewRotationDegrees,
            mirroring
        )
        videoOutConfig = OutConfig.of(
            VIDEO_CAPTURE,
            VIDEO_CROP_RECT,
            videoOutputSize
        )
        previewOutConfig = OutConfig.of(surface)
        nodeInput = SurfaceProcessorNode.In.of(
            surface,
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
