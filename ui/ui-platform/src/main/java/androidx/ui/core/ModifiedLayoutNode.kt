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

package androidx.ui.core

import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.unit.IntPx

internal class ModifiedLayoutNode2(
    wrapped: LayoutNodeWrapper,
    val layoutModifier: LayoutModifier2
) : DelegatingLayoutNodeWrapper(wrapped) {

    override fun measure(constraints: Constraints): Placeable = with(layoutModifier) {
        updateLayoutDirection()
        measureResult =
            layoutNode.measureScope.measure(wrapped, constraints, layoutNode.layoutDirection!!)
        this@ModifiedLayoutNode2
    }

    override fun minIntrinsicWidth(height: IntPx): IntPx = with(layoutModifier) {
        updateLayoutDirection()
        layoutNode.measureScope.minIntrinsicWidth(wrapped, height, layoutNode.layoutDirection!!)
    }

    override fun maxIntrinsicWidth(height: IntPx): IntPx = with(layoutModifier) {
        updateLayoutDirection()
        layoutNode.measureScope.maxIntrinsicWidth(wrapped, height, layoutNode.layoutDirection!!)
    }

    override fun minIntrinsicHeight(width: IntPx): IntPx = with(layoutModifier) {
        updateLayoutDirection()
        layoutNode.measureScope.minIntrinsicHeight(wrapped, width, layoutNode.layoutDirection!!)
    }

    override fun maxIntrinsicHeight(width: IntPx): IntPx = with(layoutModifier) {
        updateLayoutDirection()
        layoutNode.measureScope.maxIntrinsicHeight(wrapped, width, layoutNode.layoutDirection!!)
    }

    override operator fun get(line: AlignmentLine): IntPx? =
        measureResult.alignmentLines.getOrElse(line, { wrapped[line] })

    override fun draw(canvas: Canvas) {
        withPositionTranslation(canvas) {
            wrapped.draw(canvas)
            if (layoutNode.requireOwner().showLayoutBounds) {
                drawBorder(canvas, modifierBoundsPaint)
            }
        }
    }

    internal companion object {
        val modifierBoundsPaint = Paint().also { paint ->
            paint.color = Color.Blue
            paint.strokeWidth = 1f
            paint.style = PaintingStyle.stroke
        }
    }

    private fun updateLayoutDirection() {
        // TODO(popam): add support to change layout direction in the layout DSL
    }
}

@Suppress("Deprecation")
internal class ModifiedLayoutNode(
    wrapped: LayoutNodeWrapper,
    val layoutModifier: LayoutModifier
) : DelegatingLayoutNodeWrapper(wrapped) {
    override fun measure(constraints: Constraints): Placeable = with(layoutModifier) {
        updateLayoutDirection()
        val placeable = wrapped.measure(
            layoutNode.measureScope.modifyConstraints(constraints, layoutNode.layoutDirection!!)
        )
        val size = layoutNode.measureScope.modifySize(
            constraints,
            layoutNode.layoutDirection!!,
            placeable.size
        )
        val wrappedPosition = with(layoutModifier) {
            layoutNode.measureScope.modifyPosition(
                placeable.size,
                size,
                layoutNode.layoutDirection!!
            )
        }
        measureResult = object : MeasureScope.MeasureResult {
            override val width: IntPx = size.width
            override val height: IntPx = size.height
            override val alignmentLines: Map<AlignmentLine, IntPx> = emptyMap()
            override fun placeChildren(layoutDirection: LayoutDirection) {
                placeable.placeAbsolute(wrappedPosition)
            }
        }
        this@ModifiedLayoutNode
    }

    override fun minIntrinsicWidth(height: IntPx): IntPx = with(layoutModifier) {
        updateLayoutDirection()
        layoutNode.measureScope.minIntrinsicWidthOf(wrapped, height, layoutNode.layoutDirection!!)
    }

    override fun maxIntrinsicWidth(height: IntPx): IntPx = with(layoutModifier) {
        updateLayoutDirection()
        layoutNode.measureScope.maxIntrinsicWidthOf(wrapped, height, layoutNode.layoutDirection!!)
    }

    override fun minIntrinsicHeight(width: IntPx): IntPx = with(layoutModifier) {
        updateLayoutDirection()
        layoutNode.measureScope.minIntrinsicHeightOf(wrapped, width, layoutNode.layoutDirection!!)
    }

    override fun maxIntrinsicHeight(width: IntPx): IntPx = with(layoutModifier) {
        updateLayoutDirection()
        layoutNode.measureScope.maxIntrinsicHeightOf(wrapped, width, layoutNode.layoutDirection!!)
    }

    override operator fun get(line: AlignmentLine): IntPx? = with(layoutModifier) {
        return layoutNode.measureScope.modifyAlignmentLine(
            line,
            super.get(line),
            layoutNode.layoutDirection!!
        )
    }

    override fun draw(canvas: Canvas) {
        withPositionTranslation(canvas) {
            wrapped.draw(canvas)
            if (layoutNode.requireOwner().showLayoutBounds) {
                drawBorder(canvas, modifierBoundsPaint)
            }
        }
    }

    internal companion object {
        val modifierBoundsPaint = Paint().also { paint ->
            paint.color = Color.Blue
            paint.strokeWidth = 1f
            paint.style = PaintingStyle.stroke
        }
    }

    private fun updateLayoutDirection() = with(layoutModifier) {
        val modifiedLayoutDirection =
            layoutNode.measureScope.modifyLayoutDirection(layoutNode.layoutDirection!!)
        layoutNode.layoutDirection = modifiedLayoutDirection
    }
}