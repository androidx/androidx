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

import androidx.ui.geometry.Rect
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.unit.Density
import androidx.ui.unit.DensityScope
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.toPxSize

internal val Unmeasured = IntPxSize(IntPx.Zero, IntPx.Zero)

/**
 * Measurable and Placeable type that has a position.
 */
internal sealed class LayoutNodeWrapper(
    internal val layoutNode: LayoutNode
) : Placeable(), Measurable {
    protected open val wrapped: LayoutNodeWrapper? = null
    var position = IntPxPosition.Origin

    private var dirtySize: Boolean = false
    fun hasDirtySize(): Boolean = dirtySize || (wrapped?.hasDirtySize() ?: false)
    override var size: IntPxSize = Unmeasured
        protected set(value) {
            if (field != value) dirtySize = true
            field = value
        }

    /**
     * Calculate and set the content position based on the given offset and any internal
     * positioning.
     */
    abstract fun calculateContentPosition(offset: IntPxPosition)

    /**
     * Assigns a layout size to this [LayoutNodeWrapper] given the assigned innermost size
     * from the call to [MeasureScope.layout]. Assigns and returns the modified size.
     */
    abstract fun layoutSize(innermostSize: IntPxSize): IntPxSize

    /**
     * Places the modified child.
     */
    abstract fun place(position: IntPxPosition)

    /**
     * Places the modified child.
     */
    final override fun performPlace(position: IntPxPosition) {
        place(position)
        dirtySize = false
    }

    /**
     * Draws the content of the LayoutNode
     */
    abstract fun draw(canvas: Canvas, density: Density)
}

/**
 * [LayoutNodeWrapper] with default implementations for methods.
 */
internal sealed class DelegatingLayoutNodeWrapper(
    override val wrapped: LayoutNodeWrapper
) : LayoutNodeWrapper(wrapped.layoutNode) {
    override fun calculateContentPosition(offset: IntPxPosition) =
        wrapped.calculateContentPosition(position + offset)

    override fun layoutSize(innermostSize: IntPxSize) = wrapped.layoutSize(innermostSize)
    override fun draw(canvas: Canvas, density: Density) = wrapped.draw(canvas, density)
    override fun get(line: AlignmentLine): IntPx? = wrapped[line]
    override fun place(position: IntPxPosition) = wrapped.place(position)
    override fun measure(constraints: Constraints): Placeable {
        wrapped.measure(constraints)
        return this
    }

    override fun minIntrinsicWidth(height: IntPx) = wrapped.minIntrinsicWidth(height)
    override fun maxIntrinsicWidth(height: IntPx) = wrapped.maxIntrinsicWidth(height)
    override fun minIntrinsicHeight(width: IntPx) = wrapped.minIntrinsicHeight(width)
    override fun maxIntrinsicHeight(width: IntPx) = wrapped.maxIntrinsicHeight(width)
    override val parentData: Any? get() = wrapped.parentData
}

internal class InnerPlaceable(
    layoutNode: LayoutNode
) : LayoutNodeWrapper(layoutNode), DensityScope {
    override fun measure(constraints: Constraints): Placeable {
        val layoutResult = layoutNode.measureBlocks.measure(
                layoutNode.measureScope,
                layoutNode.layoutChildren,
                constraints
            )
        layoutNode.handleLayoutResult(layoutResult)
        return this
    }

    override val parentData: Any?
        get() = layoutNode.parentDataNode?.value

    override fun minIntrinsicWidth(height: IntPx): IntPx =
        layoutNode.measureBlocks.minIntrinsicWidth(
            layoutNode.measureScope,
            layoutNode.layoutChildren,
            height
        )

    override fun minIntrinsicHeight(width: IntPx): IntPx =
        layoutNode.measureBlocks.minIntrinsicHeight(
            layoutNode.measureScope,
            layoutNode.layoutChildren,
            width
        )

    override fun maxIntrinsicWidth(height: IntPx): IntPx =
        layoutNode.measureBlocks.maxIntrinsicWidth(
            layoutNode.measureScope,
            layoutNode.layoutChildren,
            height
        )

    override fun maxIntrinsicHeight(width: IntPx): IntPx =
        layoutNode.measureBlocks.maxIntrinsicHeight(
            layoutNode.measureScope,
            layoutNode.layoutChildren,
            width
        )

    override fun place(position: IntPxPosition) {
        layoutNode.isPlaced = true
        this.position = position
        val oldContentPosition = layoutNode.contentPosition
        layoutNode.layoutNodeWrapper.calculateContentPosition(IntPxPosition.Origin)
        if (oldContentPosition != layoutNode.contentPosition) {
            layoutNode.owner?.onPositionChange(layoutNode)
        }
        layoutNode.layout()
    }

    override val density: Density get() = layoutNode.measureScope.density

    override fun layoutSize(innermostSize: IntPxSize): IntPxSize {
        size = innermostSize
        return innermostSize
    }

    override operator fun get(line: AlignmentLine): IntPx? {
        return layoutNode.calculateAlignmentLines()[line]
    }

    override fun calculateContentPosition(offset: IntPxPosition) {
        layoutNode.contentPosition = position + offset
    }

    override fun draw(canvas: Canvas, density: Density) {
        val x = position.x.value.toFloat()
        val y = position.y.value.toFloat()
        canvas.translate(x, y)
        val owner = layoutNode.requireOwner()
        val sizePx = size.toPxSize()
        layoutNode.children.forEach { child -> owner.callDraw(canvas, child, sizePx) }
        if (owner.showLayoutBounds) {
            val rect = Rect(
                left = 0.5f,
                top = 0.5f,
                right = size.width.value.toFloat() - 0.5f,
                bottom = size.height.value.toFloat() - 0.5f
            )
            canvas.drawRect(rect, innerBoundsPaint)
        }
        canvas.translate(-x, -y)
    }

    internal companion object {
        val innerBoundsPaint = Paint().also { paint ->
            paint.color = Color.Red
            paint.strokeWidth = 1f
            paint.style = PaintingStyle.stroke
        }
    }
}

internal class ModifiedParentDataNode(
    wrapped: LayoutNodeWrapper,
    val parentDataModifier: ParentDataModifier
) : DelegatingLayoutNodeWrapper(wrapped) {
    override val parentData: Any?
        get() = with(parentDataModifier) {
            /**
             * ParentData provided through the parentData node will override the data provided
             * through a modifier
             */
            layoutNode.parentDataNode?.value
                ?: layoutNode.measureScope.modifyParentData(wrapped.parentData)
        }

    override var size: IntPxSize
        get() = if (super.size == Unmeasured) wrapped.size else super.size
        set(value) {
            super.size = value
        }
}

internal class ModifiedLayoutNode(
    wrapped: LayoutNodeWrapper,
    val layoutModifier: LayoutModifier
) : DelegatingLayoutNodeWrapper(wrapped) {
    /**
     * The [Placeable] returned by measuring [wrapped] in [measure].
     * Used to avoid creating more wrapper objects than necessary since [ModifiedLayoutNode]
     * also
     */
    private var measuredPlaceable: Placeable? = null

    /**
     * The [Constraints] used in the current measurement of this modified node wrapper.
     * See [withMeasuredConstraints]
     */
    private var measuredConstraints: Constraints? = null

    /**
     * Sets [measuredConstraints] for the duration of [block].
     */
    private inline fun <R> withMeasuredConstraints(
        constraints: Constraints,
        block: () -> R
    ): R = try {
        measuredConstraints = constraints
        block()
    } finally {
        measuredConstraints = null
    }

    override fun measure(constraints: Constraints): Placeable = with(layoutModifier) {
        val measureResult = withMeasuredConstraints(constraints) {
            wrapped.measure(layoutNode.measureScope.modifyConstraints(constraints))
        }
        measuredPlaceable = measureResult
        this@ModifiedLayoutNode
    }

    override fun minIntrinsicWidth(height: IntPx): IntPx = with(layoutModifier) {
        layoutNode.measureScope.minIntrinsicWidthOf(wrapped, height)
    }

    override fun maxIntrinsicWidth(height: IntPx): IntPx = with(layoutModifier) {
        layoutNode.measureScope.maxIntrinsicWidthOf(wrapped, height)
    }

    override fun minIntrinsicHeight(width: IntPx): IntPx = with(layoutModifier) {
        layoutNode.measureScope.minIntrinsicHeightOf(wrapped, width)
    }

    override fun maxIntrinsicHeight(width: IntPx): IntPx = with(layoutModifier) {
        layoutNode.measureScope.maxIntrinsicHeightOf(wrapped, width)
    }

    override fun place(position: IntPxPosition) {
        this.position = position
        val placeable = measuredPlaceable ?: error("Placeable not measured")
        val relativePosition = with(layoutModifier) {
            layoutNode.measureScope.modifyPosition(placeable.size, size)
        }
        placeable.place(relativePosition)
    }

    override operator fun get(line: AlignmentLine): IntPx? = with(layoutModifier) {
        var lineValue = layoutNode.measureScope.modifyAlignmentLine(line, wrapped[line])
        if (lineValue != null) {
            lineValue += if (line.horizontal) wrapped.position.y else wrapped.position.x
        }
        lineValue
    }

    override fun layoutSize(innermostSize: IntPxSize): IntPxSize = with(layoutModifier) {
        val constraints = measuredConstraints ?: error("must be called during measurement")
        layoutNode.measureScope.modifySize(constraints, wrapped.layoutSize(innermostSize)).also {
            size = it
        }
    }

    override fun calculateContentPosition(offset: IntPxPosition) {
        wrapped.calculateContentPosition(position + offset)
    }

    override fun draw(canvas: Canvas, density: Density) {
        val x = position.x.value.toFloat()
        val y = position.y.value.toFloat()
        canvas.translate(x, y)
        wrapped.draw(canvas, density)
        if (layoutNode.requireOwner().showLayoutBounds) {
            val rect = Rect(
                left = 0.5f,
                top = 0.5f,
                right = size.width.value.toFloat() - 0.5f,
                bottom = size.height.value.toFloat() - 0.5f
            )
            canvas.drawRect(rect, modifierBoundsPaint)
        }
        canvas.translate(-x, -y)
    }

    internal companion object {
        val modifierBoundsPaint = Paint().also { paint ->
            paint.color = Color.Blue
            paint.strokeWidth = 1f
            paint.style = PaintingStyle.stroke
        }
    }
}

internal class ModifiedDrawNode(
    wrapped: LayoutNodeWrapper,
    val drawModifier: DrawModifier
) : DelegatingLayoutNodeWrapper(wrapped), () -> Unit {
    private var density: Density? = null
    private var canvas: Canvas? = null

    override var size: IntPxSize
        get() = wrapped.size
        set(_) = error("Cannot set the size of a draw modifier")

    override fun place(position: IntPxPosition) {
        this.position = position
        wrapped.place(IntPxPosition.Origin)
    }

    override fun draw(canvas: Canvas, density: Density) {
        val x = position.x.value.toFloat()
        val y = position.y.value.toFloat()
        canvas.translate(x, y)
        this.density = density
        this.canvas = canvas
        val pxSize = size.toPxSize()
        drawModifier.draw(density, this, canvas, pxSize)
        this.density = null
        this.canvas = null
        canvas.translate(-x, -y)
    }

    // This is the implementation of drawContent()
    override fun invoke() {
        wrapped.draw(canvas!!, density!!)
    }
}
