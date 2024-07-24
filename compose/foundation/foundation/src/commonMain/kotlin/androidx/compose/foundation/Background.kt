/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.annotation.FloatRange
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.LayoutDirection

/**
 * Draws [shape] with a solid [color] behind the content.
 *
 * @sample androidx.compose.foundation.samples.DrawBackgroundColor
 * @param color color to paint background with
 * @param shape desired shape of the background
 */
@Stable
fun Modifier.background(color: Color, shape: Shape = RectangleShape): Modifier {
    val alpha = 1.0f // for solid colors
    return this.then(
        BackgroundElement(
            color = color,
            shape = shape,
            alpha = alpha,
            inspectorInfo =
                debugInspectorInfo {
                    name = "background"
                    value = color
                    properties["color"] = color
                    properties["shape"] = shape
                }
        )
    )
}

/**
 * Draws [shape] with [brush] behind the content.
 *
 * @sample androidx.compose.foundation.samples.DrawBackgroundShapedBrush
 * @param brush brush to paint background with
 * @param shape desired shape of the background
 * @param alpha Opacity to be applied to the [brush], with `0` being completely transparent and `1`
 *   being completely opaque. The value must be between `0` and `1`.
 */
@Stable
fun Modifier.background(
    brush: Brush,
    shape: Shape = RectangleShape,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f
) =
    this.then(
        BackgroundElement(
            brush = brush,
            alpha = alpha,
            shape = shape,
            inspectorInfo =
                debugInspectorInfo {
                    name = "background"
                    properties["alpha"] = alpha
                    properties["brush"] = brush
                    properties["shape"] = shape
                }
        )
    )

private class BackgroundElement(
    private val color: Color = Color.Unspecified,
    private val brush: Brush? = null,
    private val alpha: Float,
    private val shape: Shape,
    private val inspectorInfo: InspectorInfo.() -> Unit
) : ModifierNodeElement<BackgroundNode>() {
    override fun create(): BackgroundNode {
        return BackgroundNode(color, brush, alpha, shape)
    }

    override fun update(node: BackgroundNode) {
        node.color = color
        node.brush = brush
        node.alpha = alpha
        node.shape = shape
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + (brush?.hashCode() ?: 0)
        result = 31 * result + alpha.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? BackgroundElement ?: return false
        return color == otherModifier.color &&
            brush == otherModifier.brush &&
            alpha == otherModifier.alpha &&
            shape == otherModifier.shape
    }
}

private class BackgroundNode(
    var color: Color,
    var brush: Brush?,
    var alpha: Float,
    var shape: Shape,
) : DrawModifierNode, Modifier.Node(), ObserverModifierNode {

    // Naively cache outline calculation if input parameters are the same, we manually observe
    // reads inside shape#createOutline separately
    private var lastSize: Size = Size.Unspecified
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastOutline: Outline? = null
    private var lastShape: Shape? = null
    private var tmpOutline: Outline? = null

    override fun ContentDrawScope.draw() {
        if (shape === RectangleShape) {
            // shortcut to avoid Outline calculation and allocation
            drawRect()
        } else {
            drawOutline()
        }
        drawContent()
    }

    override fun onObservedReadsChanged() {
        // Reset cached properties
        lastSize = Size.Unspecified
        lastLayoutDirection = null
        lastOutline = null
        lastShape = null
        // Invalidate draw so we build the cache again - this is needed because observeReads within
        // the draw scope obscures the state reads from the draw scope's observer
        invalidateDraw()
    }

    private fun ContentDrawScope.drawRect() {
        if (color != Color.Unspecified) drawRect(color = color)
        brush?.let { drawRect(brush = it, alpha = alpha) }
    }

    private fun ContentDrawScope.drawOutline() {
        val outline = getOutline()
        if (color != Color.Unspecified) drawOutline(outline, color = color)
        brush?.let { drawOutline(outline, brush = it, alpha = alpha) }
    }

    private fun ContentDrawScope.getOutline(): Outline {
        val outline: Outline?
        if (size == lastSize && layoutDirection == lastLayoutDirection && lastShape == shape) {
            outline = lastOutline!!
        } else {
            // Manually observe reads so we can directly invalidate the outline when it changes
            // Use tmpOutline to avoid creating an object reference to local var outline
            observeReads { tmpOutline = shape.createOutline(size, layoutDirection, this) }
            outline = tmpOutline
            tmpOutline = null
        }
        lastOutline = outline
        lastSize = size
        lastLayoutDirection = layoutDirection
        lastShape = shape
        return outline!!
    }
}
