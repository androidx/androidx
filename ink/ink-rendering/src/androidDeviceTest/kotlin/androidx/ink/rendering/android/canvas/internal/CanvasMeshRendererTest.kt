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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Picture
import android.os.Build
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushCoat
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import androidx.ink.brush.SelfOverlap
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.color.Color
import androidx.ink.brush.color.toArgb
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.testing.buildStrokeInputBatchFromPoints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator-based logic test of [CanvasMeshRenderer].
 *
 * TODO(b/293163827) Move this to a Robolectric test once a shadow exists for
 *   [android.graphics.MeshSpecification].
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class CanvasMeshRendererTest {

    private val simplePaint = BrushPaint()
    private val simpleBrush =
        Brush(family = BrushFamily(paint = simplePaint), size = 10f, epsilon = 0.1f)

    private val simpleInputs =
        MutableStrokeInputBatch()
            .add(InputToolType.UNKNOWN, x = 10F, y = 10F, elapsedTimeMillis = 100)
            .toImmutable()

    private val simpleStroke = Stroke(brush = simpleBrush, inputs = simpleInputs)

    private val clock = FakeClock()

    private val renderer = CanvasMeshRenderer(getDurationTimeMillis = clock::currentTimeMillis)

    @Test
    fun canDraw_withEmptyStroke_returnsTrue() {
        val emptyStroke = Stroke(simpleBrush, ImmutableStrokeInputBatch.EMPTY)

        assertThat(
                renderer.canDraw(
                    canvas = createCanvas(),
                    stroke = emptyStroke,
                    coatIndex = 0,
                    paintPreferenceIndex = 0,
                )
            )
            .isTrue()
    }

    @Test
    fun canDraw_withSoftwareCanvas_returnsFalse() {
        val softwareCanvas = Canvas(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))

        assertThat(
                renderer.canDraw(
                    canvas = softwareCanvas,
                    stroke = simpleStroke,
                    coatIndex = 0,
                    paintPreferenceIndex = 0,
                )
            )
            .isFalse()
    }

    @Test
    fun canDraw_withSelfOverlapDiscard_returnsFalse() {
        val selfOverlapDiscardPaint = BrushPaint(selfOverlap = SelfOverlap.DISCARD)
        val selfOverlapDiscardBrush =
            Brush(
                family =
                    BrushFamily(
                        coats =
                            listOf(BrushCoat(paintPreferences = listOf(selfOverlapDiscardPaint)))
                    ),
                size = 10F,
                epsilon = 0.1F,
            )
        val stroke = Stroke(selfOverlapDiscardBrush, simpleInputs)

        assertThat(
                renderer.canDraw(
                    canvas = createCanvas(),
                    stroke = stroke,
                    coatIndex = 0,
                    paintPreferenceIndex = 0,
                )
            )
            .isFalse()
    }

    @Test
    fun obtainShaderMetadata_whenCalledTwiceWithSamePackedInstance_returnsCachedValue() {
        assertThat(simpleStroke.shape.getRenderGroupCount()).isEqualTo(1)
        val meshFormat = simpleStroke.shape.renderGroupFormat(0)

        assertThat(renderer.obtainShaderMetadata(meshFormat, isPacked = true))
            .isSameInstanceAs(renderer.obtainShaderMetadata(meshFormat, isPacked = true))
    }

    @Test
    fun obtainShaderMetadata_whenCalledTwiceWithEquivalentPackedFormat_returnsCachedValue() {
        val anotherStroke =
            Stroke(
                brush = simpleBrush,
                inputs =
                    MutableStrokeInputBatch()
                        .add(InputToolType.UNKNOWN, x = 99F, y = 99F, elapsedTimeMillis = 100)
                        .toImmutable(),
            )

        assertThat(simpleStroke.shape.getRenderGroupCount()).isEqualTo(1)
        val strokeFormat = simpleStroke.shape.renderGroupFormat(0)
        assertThat(anotherStroke.shape.getRenderGroupCount()).isEqualTo(1)
        val anotherStrokeFormat = anotherStroke.shape.renderGroupFormat(0)

        assertThat(renderer.obtainShaderMetadata(anotherStrokeFormat, isPacked = true))
            .isSameInstanceAs(renderer.obtainShaderMetadata(strokeFormat, isPacked = true))
    }

    @Test
    fun createAndroidMesh_fromInProgressStroke_returnsMesh() {
        val inProgressStroke =
            InProgressStroke().apply {
                start(Brush.createWithColorIntArgb(StockBrushes.marker(), 0x44112233, 10f, 0.25f))
                enqueueInputs(
                    buildStrokeInputBatchFromPoints(
                        floatArrayOf(10f, 20f, 100f, 120f),
                        startTime = 0L,
                    ),
                    MutableStrokeInputBatch(),
                )
                updateShape(3L)
            }
        assertThat(renderer.createAndroidMesh(inProgressStroke, coatIndex = 0, meshIndex = 0))
            .isNotNull()
    }

    @Test
    fun obtainShaderMetadata_whenCalledTwiceWithSameUnpackedInstance_returnsCachedValue() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(simpleBrush)
        assertThat(inProgressStroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(inProgressStroke.getMeshPartitionCount(0)).isEqualTo(1)
        val meshFormat = inProgressStroke.getMeshFormat(0)

        assertThat(renderer.obtainShaderMetadata(meshFormat, isPacked = false))
            .isSameInstanceAs(renderer.obtainShaderMetadata(meshFormat, isPacked = false))
    }

    @Test
    fun obtainShaderMetadata_whenCalledTwiceWithEquivalentUnpackedFormat_returnsCachedValue() {
        val inProgressStroke = InProgressStroke()
        inProgressStroke.start(simpleBrush)
        assertThat(inProgressStroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(inProgressStroke.getMeshPartitionCount(0)).isEqualTo(1)

        val anotherInProgressStroke = InProgressStroke()
        anotherInProgressStroke.start(simpleBrush)
        assertThat(anotherInProgressStroke.getBrushCoatCount()).isEqualTo(1)
        assertThat(anotherInProgressStroke.getMeshPartitionCount(0)).isEqualTo(1)

        assertThat(
                renderer.obtainShaderMetadata(inProgressStroke.getMeshFormat(0), isPacked = false)
            )
            .isSameInstanceAs(
                renderer.obtainShaderMetadata(
                    anotherInProgressStroke.getMeshFormat(0),
                    isPacked = false,
                )
            )
    }

    @Test
    @SdkSuppress(
        minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        maxSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    )
    fun drawStroke_whenAndroidU_shouldSaveRecentlyDrawnMesh() {
        val canvas = createCanvas()
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        renderer.draw(
            canvas = canvas,
            stroke = simpleStroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix(),
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(1)

        // New uniform value for transform scale, new mesh is created and drawn.
        renderer.draw(
            canvas = canvas,
            stroke = simpleStroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix().apply { setScale(3F, 4F) },
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(2)

        // Same uniform value for transform scale, same mesh is drawn again.
        renderer.draw(
            canvas = canvas,
            stroke = simpleStroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix().apply { setScale(3F, 4F) },
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(2)

        // Transform is the same but color is different, new mesh is created and drawn.
        val strokeNewColor =
            simpleStroke.copy(
                simpleStroke.brush.copyWithColorIntArgb(colorIntArgb = Color.White.toArgb())
            )
        renderer.draw(
            canvas = canvas,
            stroke = strokeNewColor,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix().apply { setScale(3F, 4F) },
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(3)

        // Move forward just a little bit of time, the same meshes should be saved.
        clock.currentTimeMillis += 3500
        renderer.draw(
            canvas = canvas,
            stroke = strokeNewColor,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix().apply { setScale(3F, 4F) },
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(3)

        // Entirely different Ink mesh, so a new Android mesh is created and drawn.
        val strokeNewMesh = simpleStroke.copy(brush = simpleStroke.brush.copy(size = 33F))
        renderer.draw(
            canvas = canvas,
            stroke = strokeNewMesh,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix(),
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(4)

        // Move forward enough time that older meshes would be cleaned up, but not enough time to
        // actually trigger a cleanup. This confirms that cleanup isn't attempted on every draw
        // call,
        // which would significantly degrade performance.
        clock.currentTimeMillis += 1999
        renderer.draw(
            canvas = canvas,
            stroke = strokeNewMesh,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix(),
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(4)

        // The next draw after enough time has passed should clean up the (no longer) recently drawn
        // meshes.
        clock.currentTimeMillis += 1
        renderer.draw(
            canvas = canvas,
            stroke = strokeNewColor,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix().apply { setScale(3F, 4F) },
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(2)
    }

    /**
     * Same set of steps as [drawStroke_whenAndroidU_shouldSaveRecentlyDrawnMesh], but there should
     * never be any saved meshes.
     */
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun drawStroke_whenAndroidVPlus_shouldNotSaveRecentlyDrawnMeshes() {
        val canvas = createCanvas()
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        renderer.draw(
            canvas = canvas,
            stroke = simpleStroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix(),
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        renderer.draw(
            canvas = canvas,
            stroke = simpleStroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix().apply { setScale(3F, 4F) },
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        renderer.draw(
            canvas = canvas,
            stroke = simpleStroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix().apply { setScale(3F, 4F) },
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        val strokeNewColor =
            simpleStroke.copy(
                simpleStroke.brush.copyWithColorIntArgb(colorIntArgb = Color.White.toArgb())
            )
        renderer.draw(
            canvas = canvas,
            stroke = strokeNewColor,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix().apply { setScale(3F, 4F) },
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        clock.currentTimeMillis += 2500
        renderer.draw(
            canvas = canvas,
            stroke = strokeNewColor,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix().apply { setScale(3F, 4F) },
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        val strokeNewMesh = simpleStroke.copy(brush = simpleStroke.brush.copy(size = 33F))
        renderer.draw(
            canvas = canvas,
            stroke = strokeNewMesh,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix(),
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        clock.currentTimeMillis += 3000
        renderer.draw(
            canvas = canvas,
            stroke = strokeNewColor,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix().apply { setScale(3F, 4F) },
            textureAnimationProgress = 0F,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)
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
        val paint = BrushPaint(listOf(texture))
        val family = BrushFamily(paint = paint)
        val brush = Brush(family = family, size = 10f, epsilon = 0.1f)
        val stroke =
            Stroke(
                brush = brush,
                inputs =
                    MutableStrokeInputBatch()
                        .add(InputToolType.UNKNOWN, x = 10F, y = 10F, elapsedTimeMillis = 100)
                        .toImmutable(),
            )

        val canvas = createCanvas()
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        // Draw the stroke at texture progress = 10%.
        renderer.draw(
            canvas = canvas,
            stroke = stroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix(),
            textureAnimationProgress = 0.1f,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(1)

        // Draw again, this time at 20% progress. Should use a new mesh.
        renderer.draw(
            canvas = canvas,
            stroke = stroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix(),
            textureAnimationProgress = 0.2f,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(2)

        // Draw at 20% progress again. The mesh should be reused.
        renderer.draw(
            canvas = canvas,
            stroke = stroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix(),
            textureAnimationProgress = 0.2f,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(2)

        // Draw at 30% progress. Should use a new mesh.
        renderer.draw(
            canvas = canvas,
            stroke = stroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix(),
            textureAnimationProgress = 0.3f,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(3)
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
                        .toImmutable(),
            )

        val canvas = createCanvas()
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(0)

        // Draw the stroke at texture progress = 10%.
        renderer.draw(
            canvas = canvas,
            stroke = stroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix(),
            textureAnimationProgress = 0.1f,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(1)

        // Draw again, this time at 20% progress. Since the stroke has no texture animation, the
        // mesh
        // should be reused.
        renderer.draw(
            canvas = canvas,
            stroke = stroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix(),
            textureAnimationProgress = 0.2f,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(1)

        // Draw at 20% progress again. Should still reuse the same mesh.
        renderer.draw(
            canvas = canvas,
            stroke = stroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix(),
            textureAnimationProgress = 0.2f,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(1)

        // Draw at 30% progress. Should still reuse the same mesh.
        renderer.draw(
            canvas = canvas,
            stroke = stroke,
            coatIndex = 0,
            paintPreferenceIndex = 0,
            strokeToScreenTransform = Matrix(),
            textureAnimationProgress = 0.3f,
        )
        assertThat(renderer.getRecentlyDrawnAndroidMeshesCount()).isEqualTo(1)
    }

    private fun createCanvas() = Picture().beginRecording(100, 100)

    private class FakeClock(var currentTimeMillis: Long = 1000L)
}
