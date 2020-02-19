/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.core.draw
import androidx.ui.graphics.Canvas
import androidx.ui.layout.ColumnScope
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Spacer
import androidx.ui.unit.Density
import androidx.ui.unit.PxSize

/**
 * Interface that provides everything you need to draw on [Canvas]. This includes
 * [androidx.ui.graphics.Canvas] scoping, [Density] and size of the [Canvas], already converted
 * to pixels
 */
interface CanvasScope : Canvas, Density {
    val size: PxSize
}

private class CanvasScopeImpl(
    canvas: Canvas,
    density: Density,
    override val size: PxSize
) : CanvasScope, Canvas by canvas, Density by density

/**
 * Component that allow you to specify an area on the screen and perform canvas drawing on this
 * area. You MUST specify size with modifier, whether with exact sizes via [LayoutSize] modifier,
 * or relative to parent, via [LayoutSize.Fill], [ColumnScope.LayoutFlexible], etc. If parent
 * wraps this child, only exact sizes must be specified.
 *
 * @sample androidx.ui.foundation.samples.CanvasSample
 *
 * @param modifier mandatory modifier to specify size strategy for this composable
 * @param onCanvas lambda that will be called to perform drawing. Note that this lambda will be
 * called during draw stage, you have no access to composition scope, meaning that [Composable]
 * function invocation inside it will result to runtime exception
 */
@Composable
fun Canvas(modifier: Modifier, onCanvas: CanvasScope.() -> Unit) {
    Spacer(
        modifier + draw(onDraw = { canvas, size ->
            CanvasScopeImpl(canvas, this, size).onCanvas()
        })
    )
}