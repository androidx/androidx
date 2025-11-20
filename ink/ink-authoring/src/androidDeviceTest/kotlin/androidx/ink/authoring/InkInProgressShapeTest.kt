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

import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalInkCustomBrushApi::class, ExperimentalCustomShapeWorkflowApi::class)
@RunWith(AndroidJUnit4::class)
class InkInProgressShapeTest {

    @Test
    fun getUpdatedRegion_withTextureAnimationThatProgressed_shouldIncludeEntireStroke() {
        val shape = InkInProgressShape()

        // Create a brush with a texture animation.
        val brushSize = 10f
        val texture =
            BrushPaint.TextureLayer(
                clientTextureId = "test",
                sizeX = 10f,
                sizeY = 10f,
                animationFrames = 8,
                animationRows = 3,
                animationColumns = 3,
                animationDurationMillis = 1000,
            )
        val family = BrushFamily(paint = BrushPaint(listOf(texture)))
        val brush = Brush(family = family, size = brushSize, epsilon = 0.1f)

        shape.start(brush, systemElapsedTimeMillis = 0)

        // Start a stroke with the texture-animated brush and an initial animation progress value.
        shape.enqueueInputs(
            realInputs =
                MutableStrokeInputBatch()
                    .add(type = InputToolType.STYLUS, x = 14f, y = 23f, elapsedTimeMillis = 321),
            predictedInputs = ImmutableStrokeInputBatch.EMPTY,
        )
        shape.update(shapeDurationMillis = 300)

        // Extend the stroke with an updated animation progress value.
        shape.enqueueInputs(
            realInputs =
                MutableStrokeInputBatch()
                    .add(type = InputToolType.STYLUS, x = 34f, y = 63f, elapsedTimeMillis = 338),
            predictedInputs = ImmutableStrokeInputBatch.EMPTY,
        )
        shape.update(shapeDurationMillis = 500)

        // Make the below assertions easier by just considering this last update.
        shape.resetUpdatedRegion()

        // Extend the stroke again with another updated animation progress value.
        shape.enqueueInputs(
            realInputs =
                MutableStrokeInputBatch()
                    .add(type = InputToolType.STYLUS, x = 54f, y = 93f, elapsedTimeMillis = 355),
            predictedInputs = ImmutableStrokeInputBatch.EMPTY,
        )
        shape.update(shapeDurationMillis = 700)

        // Since the animation progress changed since the last draw, the modified region should
        // cover
        // the entire stroke, not just the new part.
        val updatedRegion = assertNotNull(shape.getUpdatedRegion())
        assertThat(updatedRegion.width).isWithin(0.5f).of((54f - 14f) + brushSize)
        assertThat(updatedRegion.height).isWithin(0.5f).of((93f - 23f) + brushSize)
    }

    @Test
    fun getUpdatedRegion_withTextureAnimationThatDidNotProgress_shouldIncludeOnlyNewPartOfStroke() {
        // TODO: b/394129093 - Once the redraw bug is fixed, duplicate the above test without a
        // progress
        // update on the second add. Since the animation is still on the same progress, the modified
        // region should cover just the new part.
    }
}
