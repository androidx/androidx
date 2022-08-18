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

import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceEffect.PREVIEW
import androidx.camera.core.SurfaceOutput.GlTransformOptions
import androidx.camera.core.SurfaceOutput.GlTransformOptions.APPLY_CROP_ROTATE_AND_MIRRORING
import androidx.camera.core.SurfaceOutput.GlTransformOptions.USE_SURFACE_TEXTURE_TRANSFORM
import androidx.camera.core.impl.utils.TransformUtils.is90or270
import androidx.camera.core.impl.utils.TransformUtils.rectToSize
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeSurfaceEffectInternal
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
 * Unit tests for [SurfaceEffectNode].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SurfaceEffectNodeTest {

    companion object {
        private const val TARGET = PREVIEW
        private const val FORMAT = PixelFormat.RGBA_8888
        private const val ROTATION_DEGREES = 90
        private val SIZE = Size(640, 480)
        private val CROP_RECT = Rect(0, 0, 600, 400)
    }

    private lateinit var surfaceEffectInternal: FakeSurfaceEffectInternal
    private lateinit var appSurface: Surface
    private lateinit var appSurfaceTexture: SurfaceTexture
    private lateinit var node: SurfaceEffectNode
    private lateinit var inputEdge: SurfaceEdge

    @Before
    fun setup() {
        appSurfaceTexture = SurfaceTexture(0)
        appSurface = Surface(appSurfaceTexture)
        surfaceEffectInternal = FakeSurfaceEffectInternal(mainThreadExecutor())
    }

    @After
    fun tearDown() {
        appSurfaceTexture.release()
        appSurface.release()
        surfaceEffectInternal.release()
        if (::node.isInitialized) {
            node.release()
        }
        if (::inputEdge.isInitialized) {
            inputEdge.surfaces.forEach { it.close() }
        }
        shadowOf(getMainLooper()).idle()
    }

    @Test
    fun transformInput_withoutGlTransform_outputHasTheSameProperty() {
        // Arrange.
        createSurfaceEffectNode()
        createInputEdge()
        val inputSurface = inputEdge.surfaces[0]

        // Act.
        val outputEdge = node.transform(inputEdge)

        // Assert: without transformation, the output has the same property as the input.
        assertThat(outputEdge.surfaces).hasSize(1)
        val outputSurface = outputEdge.surfaces[0]
        assertThat(outputSurface.size).isEqualTo(inputSurface.size)
        assertThat(outputSurface.format).isEqualTo(inputSurface.format)
        assertThat(outputSurface.targets).isEqualTo(inputSurface.targets)
        assertThat(outputSurface.cropRect).isEqualTo(inputSurface.cropRect)
        assertThat(outputSurface.rotationDegrees).isEqualTo(inputSurface.rotationDegrees)
        assertThat(outputSurface.mirroring).isEqualTo(inputSurface.mirroring)
        assertThat(outputSurface.hasEmbeddedTransform()).isFalse()
    }

    @Test
    fun transformInput_withGlTransformRotation_outputIsCroppedAndRotated() {
        val cropRect = Rect(200, 100, 600, 400)
        for (rotationDegrees in arrayOf(0, 90, 180, 270)) {
            // Arrange.
            createSurfaceEffectNode(APPLY_CROP_ROTATE_AND_MIRRORING)
            createInputEdge(
                size = rectToSize(cropRect),
                cropRect = cropRect,
                rotationDegrees = rotationDegrees
            )
            // The result cropRect should have zero left and top.
            val expectedCropRect = if (is90or270(rotationDegrees))
                Rect(0, 0, cropRect.height(), cropRect.width())
            else
                Rect(0, 0, cropRect.width(), cropRect.height())

            // Act.
            val outputEdge = node.transform(inputEdge)

            // Assert: with transformation, the output size is cropped/rotated and the rotation
            // degrees is reset.
            assertThat(outputEdge.surfaces).hasSize(1)
            val outputSurface = outputEdge.surfaces[0]
            assertThat(outputSurface.size).isEqualTo(rectToSize(expectedCropRect))
            assertThat(outputSurface.cropRect).isEqualTo(expectedCropRect)
            assertThat(outputSurface.rotationDegrees).isEqualTo(0)

            // Clean up.
            inputEdge.surfaces[0].close()
            node.release()
        }
    }

    @Test
    fun transformInput_withGlTransformMirroring_outputHasNoMirroring() {
        for (mirroring in arrayOf(false, true)) {
            // Arrange.
            createSurfaceEffectNode(APPLY_CROP_ROTATE_AND_MIRRORING)
            createInputEdge(mirroring = mirroring)

            // Act.
            val outputEdge = node.transform(inputEdge)

            // Assert: the mirroring of output is always false.
            assertThat(outputEdge.surfaces).hasSize(1)
            val outputSurface = outputEdge.surfaces[0]
            assertThat(outputSurface.mirroring).isFalse()

            // Clean up.
            inputEdge.surfaces[0].close()
            node.release()
        }
    }

    @Test
    fun provideSurfaceToOutput_surfaceIsPropagatedE2E() {
        // Arrange.
        createSurfaceEffectNode()
        createInputEdge()
        val inputSurface = inputEdge.surfaces[0]
        val outputEdge = node.transform(inputEdge)
        val outputSurface = outputEdge.surfaces[0]

        // Act.
        outputSurface.setProvider(Futures.immediateFuture(appSurface))
        shadowOf(getMainLooper()).idle()

        // Assert: effect receives app Surface. CameraX receives effect Surface.
        assertThat(surfaceEffectInternal.outputSurface).isEqualTo(appSurface)
        assertThat(inputSurface.surface.get()).isEqualTo(surfaceEffectInternal.inputSurface)
    }

    @Test
    fun releaseNode_effectIsReleased() {
        // Arrange.
        createSurfaceEffectNode()
        createInputEdge()
        val outputSurface = node.transform(inputEdge).surfaces[0]
        outputSurface.setProvider(Futures.immediateFuture(appSurface))
        shadowOf(getMainLooper()).idle()

        // Act: release the node.
        node.release()
        shadowOf(getMainLooper()).idle()

        // Assert: effect is released and has requested effect to close the SurfaceOutput
        assertThat(surfaceEffectInternal.isReleased).isTrue()
        assertThat(surfaceEffectInternal.isOutputSurfaceRequestedToClose).isTrue()
    }

    private fun createInputEdge(
        target: Int = TARGET,
        size: Size = SIZE,
        format: Int = FORMAT,
        sensorToBufferTransform: android.graphics.Matrix = android.graphics.Matrix(),
        hasEmbeddedTransform: Boolean = true,
        cropRect: Rect = CROP_RECT,
        rotationDegrees: Int = ROTATION_DEGREES,
        mirroring: Boolean = false
    ) {
        val surface = SettableSurface(
            target,
            size,
            format,
            sensorToBufferTransform,
            hasEmbeddedTransform,
            cropRect,
            rotationDegrees,
            mirroring
        )
        inputEdge = SurfaceEdge.create(listOf(surface))
    }

    private fun createSurfaceEffectNode(
        glTransformOptions: GlTransformOptions = USE_SURFACE_TEXTURE_TRANSFORM
    ) {
        node = SurfaceEffectNode(FakeCamera(), glTransformOptions, surfaceEffectInternal)
    }
}
