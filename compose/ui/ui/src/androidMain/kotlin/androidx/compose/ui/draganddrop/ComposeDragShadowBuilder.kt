/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.draganddrop

import android.graphics.Canvas as AndroidCanvas
import android.graphics.Point
import android.view.View
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Draws a drag shadow for a [View.DragShadowBuilder] with the DrawScope lambda
 * provided by [DragAndDropInfo.onDrawDragShadow].
 */
internal class ComposeDragShadowBuilder(
    private val density: Density,
    private val dragAndDropInfo: DragAndDropInfo,
) : View.DragShadowBuilder() {

    override fun onProvideShadowMetrics(
        outShadowSize: Point,
        outShadowTouchPoint: Point
    ) = with(density) {
        outShadowSize.set(
            dragAndDropInfo.size.width.toDp().roundToPx(),
            dragAndDropInfo.size.height.toDp().roundToPx()
        )
        outShadowTouchPoint.set(
            outShadowSize.x / 2,
            outShadowSize.y / 2
        )
    }

    override fun onDrawShadow(canvas: AndroidCanvas) {
        CanvasDrawScope().draw(
            density = density,
            size = dragAndDropInfo.size,
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(canvas),
            block = dragAndDropInfo.onDrawDragShadow,
        )
    }
}
