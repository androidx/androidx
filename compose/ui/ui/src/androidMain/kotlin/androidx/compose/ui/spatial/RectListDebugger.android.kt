/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.spatial

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.unit.Constraints

@Composable
internal fun RectListDebugger(modifier: Modifier = Modifier) {
    Layout(modifier.then(RectListDebuggerModifierElement), EmptyFillMeasurePolicy)
}

private object EmptyFillMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        return layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}

@SuppressLint("ModifierNodeInspectableProperties")
private object RectListDebuggerModifierElement :
    ModifierNodeElement<RectListDebuggerModifierNode>() {
    override fun create() = RectListDebuggerModifierNode()

    override fun hashCode() = 123

    override fun equals(other: Any?) = other === this

    override fun update(node: RectListDebuggerModifierNode) {}
}

private class RectListDebuggerModifierNode : DrawModifierNode, Modifier.Node() {
    private var paint =
        Paint()
            .also {
                it.color = Color.Red
                it.style = PaintingStyle.Stroke
            }
            .asFrameworkPaint()

    var token: Any? = null

    override fun onAttach() {
        token = requireOwner().rectManager.registerOnChangedCallback { invalidateDraw() }
    }

    override fun onDetach() {
        requireOwner().rectManager.unregisterOnChangedCallback(token)
    }

    override fun ContentDrawScope.draw() {
        val rectList = requireOwner().rectManager.rects
        val canvas = drawContext.canvas.nativeCanvas
        val paint = paint
        rectList.forEachRect { _, l, t, r, b ->
            canvas.drawRect(l.toFloat(), t.toFloat(), r.toFloat(), b.toFloat(), paint)
        }
    }
}
