/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.rendering.android.canvas.internal

import android.graphics.Matrix
import android.graphics.Picture
import android.graphics.RenderNode
import android.os.Build
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.color.Color
import androidx.ink.brush.color.toArgb
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.testing.buildStrokeInputBatchFromPoints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator-based logic test of [CanvasMeshRenderer].
 *
 * TODO(b/293163827) Move this to [CanvasMeshRendererRobolectricTest] once a shadow exists for
 *   [android.graphics.MeshSpecification].
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class CanvasMeshRendererTest {

    private val brush =
        Brush.createWithColorIntArgb(
            family = StockBrushes.markerLatest,
            colorIntArgb = Color.Black.toArgb(),
            size = 10F,
            epsilon = 0.1F,
        )

    private val stroke =
        Stroke(
            brush = brush,
            inputs =
                MutableStrokeInputBatch()
                    .add(InputToolType.UNKNOWN, x = 10F, y = 10F, elapsedTimeMillis = 100)
                    .asImmutable(),
        )

    private val clock = FakeClock()

    private val meshRenderer = CanvasMeshRenderer(getDurationTimeMillis = clock::currentTimeMillis)

    private val falseNegativeAffineMatrix =
        Matrix().apply {
            setValues(
                floatArrayOf(
                    1.2887144F,
                    0.33863622F,
                    -776.0461F, // first row looks affine
                    -0.33863622F,
                    1.2887144F,
                    -297.80093F, // second row looks affine
                    0F,
                    0F,
                    0.99999994F, // third row is nearly affine, except for floating point precision
                )
            )
            // Inverting this matrix yields a transform that is actually affine, but Matrix.isAffine
            // incorrectly does not consider it to be.
            invert(this)
            check(!isAffine) {
                "Trying to test the case where Matrix.isAffine is false but the bottom row is " +
                    "[0, 0, 1], but Matrix.isAffine is actually true."
            }
            val values = FloatArray(9).also { getValues(it) }
            check(
                values[Matrix.MPERSP_0] == 0F &&
                    values[Matrix.MPERSP_1] == 0F &&
                    values[Matrix.MPERSP_2] == 1F
            ) {
                "Trying to test the case where Matrix.isAffine is false but the bottom row is " +
                    "[0, 0, 1], but the Matrix is actually $this."
            }
        }

    @Test
    fun obtainShaderMetadata_whenCalledTwiceWithSamePackedInstance_returnsCachedValue() {
        assertThat(stroke.shape.getRenderGroupCount()).isEqualTo(1)
        val meshFormat = stroke.shape.renderGroupFormat(0)

        assertThat(meshRenderer.obtainShaderMetadata(meshFormat, isPacked = true))
            .isSameInstanceAs(meshRenderer.obtainShaderMetadata(meshFormat, isPacked = true))
    }

    @Test
    fun obtainShaderMetadata_whenCalledTwiceWithEquivalentPackedFormat_returnsCachedValue() {
        val anotherStroke =
            Stroke(
                brush = brush,
                inputs =
                    MutableStrokeInputBatch()
                        .add(InputToolType.UNKNOWN, x = 99F, y = 99F, elapsedTimeMillis = 100)
                        .asImmutable(),
            )

        assertThat(stroke.shape.getRenderGroupCount()).isEqualTo(1)
        val strokeFormat = stroke.shape.renderGroupFormat(0)
        assertThat(anotherStroke.shape.getRenderGroupCount()).isEqualTo(1)
        val anotherStrokeFormat = anotherStroke.shape.renderGroupFormat(0)

        assertThat(meshRenderer.obtainShaderMetadata(anotherStrokeFormat, isPacked = true))
            .isSameInstanceAs(meshRenderer.obtainShaderMetadata(strokeFormat, isPacked = true))
    }

    @Test
    fun createAndroidMesh_fromInProgressStroke_returnsMesh() {
        val inProgressStroke =
            InProgressStroke().apply {
                start(
                    Brush.createWithColorIntArgb(StockBrushes.markerLatest, 0x44112233, 10f, 0.25f)
                )
                enqueueInputs(
                    buildStrokeInputBatchFromPoints(
                        floatArrayOf(10f, 20f, 100f, 120f),
                        startTime = 0L,
                    ),
                    MutableStrokeInputBatch(),
                )
                updateShape(3L)
            }
        assertThat(meshRenderer.createAndroidMesh(inProgressStroke, coatIndex = 0, meshIndex = 0))
            .isNotNull()
    }

    @Test
    fun obtainShaderMetadata_whenCalledTwiceWithSameUnpackedInstance_returnsCachedValue() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(brush)
        assertThat(inProgressStroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(inProgressStroke.getMeshPartitionCount(0)).isEqualTo(1)
        val meshFormat = inProgressStroke.getMeshFormat(0, 0)

        assertThat(meshRenderer.obtainShaderMetadata(meshFormat, isPacked = false))
            .isSameInstanceAs(meshRenderer.obtainShaderMetadata(meshFormat, isPacked = false))
    }

    @Test
    fun obtainShaderMetadata_whenCalledTwiceWithEquivalentUnpackedFormat_returnsCachedValue() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(brush)
        assertThat(inProgressStroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(inProgressStroke.getMeshPartitionCount(0)).isEqualTo(1)

        val anotherInProgressStroke = InProgressStroke()
        anotherInProgressStroke.start(brush)
        assertThat(anotherInProgressStroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(anotherInProgressStroke.getMeshPartitionCount(0)).isEqualTo(1)

        assertThat(
                meshRenderer.obtainShaderMetadata(
                    inProgressStroke.getMeshFormat(0, 0),
                    isPacked = false,
                )
            )
            .isSameInstanceAs(
                meshRenderer.obtainShaderMetadata(
                    anotherInProgressStroke.getMeshFormat(0, 0),
                    isPacked = false,
                )
            )
    }

    @Test
    fun drawStroke_withNonAffineTransform_shouldThrow() {
        val canvas = Picture().beginRecording(100, 100)

        assertFailsWith<IllegalArgumentException> {
            meshRenderer.draw(
                canvas,
                stroke,
                Matrix().apply {
                    setValues(
                        floatArrayOf(
                            1F,
                            0F,
                            0F, // first row looks affine
                            0F,
                            1F,
                            0F, // second row looks affine
                            4F,
                            0F,
                            1F, // third row should be [0, 0, 1] to be affine
                        )
                    )
                },
            )
        }

        assertFailsWith<IllegalArgumentException> {
            meshRenderer.draw(
                canvas,
                stroke,
                Matrix().apply {
                    setValues(
                        floatArrayOf(
                            1F,
                            0F,
                            0F, // first row looks affine
                            0F,
                            1F,
                            0F, // second row looks affine
                            0F,
                            3F,
                            1F, // third row should be [0, 0, 1] to be affine
                        )
                    )
                },
            )
        }

        assertFailsWith<IllegalArgumentException> {
            meshRenderer.draw(
                canvas,
                stroke,
                Matrix().apply {
                    setValues(
                        floatArrayOf(
                            1F,
                            0F,
                            0F, // first row looks affine
                            0F,
                            1F,
                            0F, // second row looks affine
                            0F,
                            0F,
                            2F, // third row should be [0, 0, 1] to be affine
                        )
                    )
                },
            )
        }
    }

    @Test
    fun drawInProgressStroke_withNonAffineTransform_shouldThrow() {
        val canvas = Picture().beginRecording(100, 100)
        val ips = InProgressStroke().also { it.start(brush) }

        assertFailsWith<IllegalArgumentException> {
            meshRenderer.draw(
                canvas,
                ips,
                Matrix().apply {
                    setValues(
                        floatArrayOf(
                            1F,
                            0F,
                            0F, // first row looks affine
                            0F,
                            1F,
                            0F, // second row looks affine
                            4F,
                            0F,
                            1F, // third row should be [0, 0, 1] to be affine
                        )
                    )
                },
            )
        }

        assertFailsWith<IllegalArgumentException> {
            meshRenderer.draw(
                canvas,
                ips,
                Matrix().apply {
                    setValues(
                        floatArrayOf(
                            1F,
                            0F,
                            0F, // first row looks affine
                            0F,
                            1F,
                            0F, // second row looks affine
                            0F,
                            3F,
                            1F, // third row should be [0, 0, 1] to be affine
                        )
                    )
                },
            )
        }

        assertFailsWith<IllegalArgumentException> {
            meshRenderer.draw(
                canvas,
                ips,
                Matrix().apply {
                    setValues(
                        floatArrayOf(
                            1F,
                            0F,
                            0F, // first row looks affine
                            0F,
                            1F,
                            0F, // second row looks affine
                            0F,
                            0F,
                            2F, // third row should be [0, 0, 1] to be affine
                        )
                    )
                },
            )
        }
    }

    @Test
    fun drawStroke_withAffineTransform_shouldNotThrow() {
        val canvas = Picture().beginRecording(100, 100)

        // The simplest affine transform - the identity matrix.
        meshRenderer.draw(canvas, stroke, Matrix())

        // Test for an edge case where the input Matrix is actually affine if inspected directly,
        // but
        // where Android Matrix.isAffine returns false. See b/418261442 for more details.
        meshRenderer.draw(canvas, stroke, falseNegativeAffineMatrix)
    }

    @Test
    fun drawInProgressStroke_withAffineTransform_shouldNotThrow() {
        val canvas = Picture().beginRecording(100, 100)
        val ips = InProgressStroke().also { it.start(brush) }

        // The simplest affine transform - the identity matrix.
        meshRenderer.draw(canvas, ips, Matrix())

        // Test for an edge case where the input Matrix is actually affine if inspected directly,
        // but
        // where Android Matrix.isAffine returns false. See b/418261442 for more details.
        meshRenderer.draw(canvas, ips, falseNegativeAffineMatrix)
    }

    @Test
    @SdkSuppress(
        minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        maxSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    )
    fun drawStroke_whenAndroidU_shouldSaveRecentlyDrawnMesh() {
        val renderNode = RenderNode("test")
        val canvas = renderNode.beginRecording()
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        meshRenderer.draw(canvas, stroke, Matrix())
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(1)

        // New uniform value for transform scale, new mesh is created and drawn.
        meshRenderer.draw(canvas, stroke, Matrix().apply { setScale(3F, 4F) })
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(2)

        // Same uniform value for transform scale, same mesh is drawn again.
        meshRenderer.draw(canvas, stroke, Matrix().apply { setScale(3F, 4F) })
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(2)

        // Transform is the same but color is different, new mesh is created and drawn.
        val strokeNewColor =
            stroke.copy(stroke.brush.copyWithColorIntArgb(colorIntArgb = Color.White.toArgb()))
        meshRenderer.draw(canvas, strokeNewColor, Matrix().apply { setScale(3F, 4F) })
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(3)

        // Move forward just a little bit of time, the same meshes should be saved.
        clock.currentTimeMillis += 3500
        meshRenderer.draw(canvas, strokeNewColor, Matrix().apply { setScale(3F, 4F) })
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(3)

        // Entirely different Ink mesh, so a new Android mesh is created and drawn.
        val strokeNewMesh = stroke.copy(brush = stroke.brush.copy(size = 33F))
        meshRenderer.draw(canvas, strokeNewMesh, Matrix())
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(4)

        // Move forward enough time that older meshes would be cleaned up, but not enough time to
        // actually trigger a cleanup. This confirms that cleanup isn't attempted on every draw
        // call,
        // which would significantly degrade performance.
        clock.currentTimeMillis += 1999
        meshRenderer.draw(canvas, strokeNewMesh, Matrix())
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(4)

        // The next draw after enough time has passed should clean up the (no longer) recently drawn
        // meshes.
        clock.currentTimeMillis += 1
        meshRenderer.draw(canvas, strokeNewColor, Matrix().apply { setScale(3F, 4F) })
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(2)
    }

    /**
     * Same set of steps as [drawStroke_whenAndroidU_shouldSaveRecentlyDrawnMesh], but there should
     * never be any saved meshes.
     */
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun drawStroke_whenAndroidVPlus_shouldNotSaveRecentlyDrawnMeshes() {
        val renderNode = RenderNode("test")
        val canvas = renderNode.beginRecording()
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        meshRenderer.draw(canvas, stroke, Matrix())
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        meshRenderer.draw(canvas, stroke, Matrix().apply { setScale(3F, 4F) })
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        meshRenderer.draw(canvas, stroke, Matrix().apply { setScale(3F, 4F) })
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        val strokeNewColor =
            stroke.copy(stroke.brush.copyWithColorIntArgb(colorIntArgb = Color.White.toArgb()))
        meshRenderer.draw(canvas, strokeNewColor, Matrix().apply { setScale(3F, 4F) })
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        clock.currentTimeMillis += 2500
        meshRenderer.draw(canvas, strokeNewColor, Matrix().apply { setScale(3F, 4F) })
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        val strokeNewMesh = stroke.copy(brush = stroke.brush.copy(size = 33F))
        meshRenderer.draw(canvas, strokeNewMesh, Matrix())
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        clock.currentTimeMillis += 3000
        meshRenderer.draw(canvas, strokeNewColor, Matrix().apply { setScale(3F, 4F) })
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)
    }

    @Test
    @SdkSuppress(
        minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        maxSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    )
    fun drawStroke_whenAndroidU_withTextureAnimation_shouldSaveRecentlyDrawnMesh() {
        // Create a stroke with a texture animation.
        val texture =
            BrushPaint.TextureLayer(
                clientTextureId = "test",
                sizeX = 10f,
                sizeY = 10f,
                animationFrames = 8,
                animationRows = 3,
                animationColumns = 3,
            )
        val family = BrushFamily(paint = BrushPaint(listOf(texture)))
        val brush = Brush(family = family, size = 10f, epsilon = 0.1f)
        val stroke =
            Stroke(
                brush = brush,
                inputs =
                    MutableStrokeInputBatch()
                        .add(InputToolType.UNKNOWN, x = 10F, y = 10F, elapsedTimeMillis = 100)
                        .asImmutable(),
            )

        val renderNode = RenderNode("test")
        val canvas = renderNode.beginRecording()
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        // Draw the stroke at texture progress = 10%.
        meshRenderer.draw(canvas, stroke, Matrix(), 0.1f)
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(1)

        // Draw again, this time at 20% progress. Should use a new mesh.
        meshRenderer.draw(canvas, stroke, Matrix(), 0.2f)
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(2)

        // Draw at 20% progress again. The mesh should be reused.
        meshRenderer.draw(canvas, stroke, Matrix(), 0.2f)
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(2)

        // Draw at 30% progress. Should use a new mesh.
        meshRenderer.draw(canvas, stroke, Matrix(), 0.3f)
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(3)
    }

    /**
     * Same set of steps as
     * [drawStroke_whenAndroidU_withTextureAnimation_shouldSaveRecentlyDrawnMesh], but without a
     * texture animation, so changing animation progress should not cause a new mesh to be created.
     */
    @Test
    @SdkSuppress(
        minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        maxSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    )
    fun drawStroke_whenAndroidU_withoutTextureAnimation_shouldIgnoreTextureProgressForMeshReuse() {
        // Create a stroke without a texture animation.
        val texture =
            BrushPaint.TextureLayer(
                clientTextureId = "test",
                sizeX = 10f,
                sizeY = 10f,
                animationFrames = 1,
            )
        val family = BrushFamily(paint = BrushPaint(listOf(texture)))
        val brush = Brush(family = family, size = 10f, epsilon = 0.1f)
        val stroke =
            Stroke(
                brush = brush,
                inputs =
                    MutableStrokeInputBatch()
                        .add(InputToolType.UNKNOWN, x = 10F, y = 10F, elapsedTimeMillis = 100)
                        .asImmutable(),
            )

        val renderNode = RenderNode("test")
        val canvas = renderNode.beginRecording()
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        // Draw the stroke at texture progress = 10%.
        meshRenderer.draw(canvas, stroke, Matrix(), 0.1f)
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(1)

        // Draw again, this time at 20% progress. Since the stroke has no texture animation, the
        // mesh
        // should be reused.
        meshRenderer.draw(canvas, stroke, Matrix(), 0.2f)
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(1)

        // Draw at 20% progress again. Should still reuse the same mesh.
        meshRenderer.draw(canvas, stroke, Matrix(), 0.2f)
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(1)

        // Draw at 30% progress. Should still reuse the same mesh.
        meshRenderer.draw(canvas, stroke, Matrix(), 0.3f)
        assertThat(meshRenderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(1)
    }

    private class FakeClock(var currentTimeMillis: Long = 1000L)
}
