/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.compose.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
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
 * Draws a background in a given [shape] and with a [color] or [alpha] that can be animated.
 *
 * @param color color to paint background with
 * @param alpha alpha of the background
 * @param shape desired shape of the background
 */
fun Modifier.animatedBackground(
    color: () -> Color,
    alpha: () -> Float = DefaultAlpha,
    shape: Shape = RectangleShape,
) =
    this.then(
        BackgroundElement(
            color = color,
            alpha = alpha,
            shape = shape,
            inspectorInfo =
                debugInspectorInfo {
                    name = "background"
                    value = color
                    properties["color"] = color
                    properties["alpha"] = alpha
                    properties["shape"] = shape
                },
        )
    )

private val DefaultAlpha = { 1f }

private class BackgroundElement(
    private val color: () -> Color,
    private val alpha: () -> Float,
    private val shape: Shape,
    private val inspectorInfo: InspectorInfo.() -> Unit,
) : ModifierNodeElement<BackgroundNode>() {
    override fun create(): BackgroundNode {
        return BackgroundNode(color, alpha, shape)
    }

    override fun update(node: BackgroundNode) {
        node.color = color
        node.alpha = alpha
        node.shape = shape
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? BackgroundElement ?: return false
        return color == otherModifier.color &&
            alpha == otherModifier.alpha &&
            shape == otherModifier.shape
    }
}

private class BackgroundNode(var color: () -> Color, var alpha: () -> Float, var shape: Shape) :
    DrawModifierNode, Modifier.Node(), ObserverModifierNode {

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
        drawRect(color = color(), alpha = alpha())
    }

    private fun ContentDrawScope.drawOutline() {
        val outline = getOutline()
        drawOutline(outline, color = color(), alpha = alpha())
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
