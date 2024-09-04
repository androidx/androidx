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
import android.graphics.RenderNode
import android.os.Build
import androidx.core.os.BuildCompat
import androidx.ink.brush.Brush
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
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator-based logic test of [CanvasMeshRenderer].
 *
 * TODO(b/293163827) Move this to [CanvasMeshRendererRobolectricTest] once a shadow exists for
 *   [android.graphics.MeshSpecification].
 */
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
                    .addOrThrow(InputToolType.UNKNOWN, x = 10F, y = 10F, elapsedTimeMillis = 100)
                    .asImmutable(),
        )

    private val clock = FakeClock()

    @OptIn(ExperimentalInkCustomBrushApi::class)
    private val meshRenderer = CanvasMeshRenderer(getDurationTimeMillis = clock::currentTimeMillis)

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
                        .addOrThrow(
                            InputToolType.UNKNOWN,
                            x = 99F,
                            y = 99F,
                            elapsedTimeMillis = 100
                        )
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
                assertThat(
                        enqueueInputs(
                                buildStrokeInputBatchFromPoints(
                                    floatArrayOf(10f, 20f, 100f, 120f),
                                    startTime = 0L
                                ),
                                MutableStrokeInputBatch(),
                            )
                            .isSuccess
                    )
                    .isTrue()
                assertThat(updateShape(3L).isSuccess).isTrue()
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
                    isPacked = false
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
    fun drawStroke_whenAndroidU_shouldSaveRecentlyDrawnMesh() {
        if (BuildCompat.isAtLeastV()) {
            return
        }
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
    fun drawStroke_whenAndroidVPlus_shouldNotSaveRecentlyDrawnMeshes() {
        if (!BuildCompat.isAtLeastV()) {
            return
        }
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

    private class FakeClock(var currentTimeMillis: Long = 1000L)
}
