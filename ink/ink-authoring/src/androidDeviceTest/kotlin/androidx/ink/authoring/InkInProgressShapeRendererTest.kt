/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.authoring

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalInkCustomBrushApi::class, ExperimentalCustomShapeWorkflowApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(maxSdkVersion = Build.VERSION_CODES.TIRAMISU)
class InkInProgressShapeRendererTest {

    private val canvas = mock<Canvas> {}
    private val canvasStrokeRenderer = mock<CanvasStrokeRenderer> {}
    private val shapeRenderer = InkInProgressShapeRenderer(canvasStrokeRenderer)

    private val identityTransform = Matrix()

    @Test
    fun draw_callsCanvasStrokeRenderer() {
        val shape =
            InkInProgressShape().apply {
                start(
                    Brush.createWithColorIntArgb(
                        family =
                            BrushFamily(
                                paint =
                                    BrushPaint(
                                        textureLayers =
                                            listOf(
                                                BrushPaint.TextureLayer(
                                                    clientTextureId = "fake",
                                                    sizeX = 128F,
                                                    sizeY = 128F,
                                                    animationFrames = 2,
                                                    animationRows = 1,
                                                    animationColumns = 2,
                                                    animationDurationMillis = 240,
                                                )
                                            )
                                    )
                            ),
                        colorIntArgb = Color.BLUE,
                        size = 10F,
                        epsilon = 0.1F,
                    ),
                    systemElapsedTimeMillis = 100L,
                )
            }

        // 100ms + 140ms = 240ms, which loops back to progress 0 of the 240ms animation duration
        shape.update(shapeDurationMillis = 140)
        shapeRenderer.draw(canvas, shape, identityTransform)
        verify(canvasStrokeRenderer)
            .draw(canvas, shape.inProgressStroke, identityTransform, textureAnimationProgress = 0F)

        // 100ms + 260ms = 360ms - half of the looped 240ms animation duration
        shape.update(shapeDurationMillis = 260)
        shapeRenderer.draw(canvas, shape, identityTransform)
        verify(canvasStrokeRenderer)
            .draw(
                canvas,
                shape.inProgressStroke,
                identityTransform,
                textureAnimationProgress = 0.5F,
            )

        // 100ms + 320ms = 420ms - 3/4 of the looped 240ms animation duration
        // 180 more than initial update value - 3/4 of 240ms animation duration
        shape.update(shapeDurationMillis = 320)
        shapeRenderer.draw(canvas, shape, identityTransform)
        verify(canvasStrokeRenderer)
            .draw(
                canvas,
                shape.inProgressStroke,
                identityTransform,
                textureAnimationProgress = 0.75F,
            )
    }
}
