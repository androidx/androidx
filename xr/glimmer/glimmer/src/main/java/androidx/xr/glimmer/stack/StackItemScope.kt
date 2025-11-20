/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.stack

import androidx.collection.MutableScatterMap
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.xr.glimmer.DepthNode
import androidx.xr.glimmer.GlimmerTheme.Companion.LocalGlimmerTheme

/** Receiver scope used by item content in [VerticalStack]. */
@Stable
public sealed interface StackItemScope {

    /**
     * Adds a decoration shape for this item, which is used in transition animations (to clip items
     * behind) and depth effect. Each item must have its decoration shape set, as otherwise the
     * clipping and depth will not be applied.
     *
     * @param shape The shape of this stack item.
     */
    public fun Modifier.itemDecoration(shape: Shape): Modifier =
        this then ItemDecorationElement(this@StackItemScope as StackItemScopeImpl, shape)
}

internal class ItemDecorationElement
internal constructor(private val stackItemScope: StackItemScopeImpl, private val shape: Shape) :
    ModifierNodeElement<ItemDecorationNode>() {

    override fun create(): ItemDecorationNode = ItemDecorationNode(stackItemScope, shape)

    override fun update(node: ItemDecorationNode) {
        node.update(stackItemScope, shape)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ItemDecorationElement) return false

        if (stackItemScope != other.stackItemScope) return false
        if (shape != other.shape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stackItemScope.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "stackItemDecoration"
        properties["shape"] = shape
    }
}

// TODO(b/413429531): add support for shape bounds.
internal class StackItemDecoration(internal var shape: Shape, internal var size: Size)

internal class StackItemScopeImpl(internal val state: StackState) : StackItemScope {

    internal var index = -1
    internal val decorations = MutableScatterMap<Any, StackItemDecoration>()

    // TODO(b/413429531): remove the first decoration getter once multiple shapes are supported.
    internal fun firstDecoration(): StackItemDecoration? {
        var decoration: StackItemDecoration? = null
        decorations.forEachValue {
            decoration = it
            return@forEachValue
        }
        return decoration
    }
}

internal class ItemDecorationNode(
    private var stackItemScope: StackItemScopeImpl,
    private var shape: Shape,
) :
    DelegatingNode(),
    LayoutAwareModifierNode,
    CompositionLocalConsumerModifierNode,
    DrawModifierNode {

    private var depthNode: DepthNode? = null
    private var size = Size.Zero
    private var itemOutline: Outline? = null

    override fun onAttach() {
        depthNode = delegate(DepthNode(currentValueOfDepth(), shape))
        if (size != Size.Zero) {
            // If this node is reused, we need to update the shape in case there is no remeasure.
            updateShapeAndOutline()
        }
    }

    override fun onRemeasured(size: IntSize) {
        // TODO(b/413429531): add support for shape bounds.
        this.size = size.toSize()
        updateShapeAndOutline()
    }

    override fun onDetach() {
        stackItemScope.decorations.remove(this)
        depthNode?.let { undelegate(it) }
    }

    override fun ContentDrawScope.draw() {
        val index = stackItemScope.index
        if (index == -1) return
        val state = stackItemScope.state
        val topItem = state.topItem
        val offsetFraction = state.topItemOffsetFraction

        val contentAlpha =
            calculateContentAlpha(index = index, topItem = topItem, offsetFraction = offsetFraction)

        // Apply alpha to the depth separately from the content so that the shadows are not clipped.
        depthNode?.apply { drawDepth(alpha = contentAlpha) }

        // Draw item content with a scrim based on the current alpha.
        drawContent()

        val scrimAlpha =
            calculateScrimAlpha(index = index, topItem = topItem, offsetFraction = offsetFraction)
        val scrimColor = getScrimColor(index = index, topItem = topItem)
        val outline = getItemOutline()

        if (scrimColor == null) {
            // If there is no scrim color, apply alpha to the item content.
            drawOutline(
                outline = outline,
                blendMode = BlendMode.DstOut,
                color = Color.Black,
                alpha = 1f - contentAlpha,
            )
        } else {
            // If there is a scrim color, apply it on top of the item instead of content alpha.
            drawOutline(outline = outline, color = scrimColor, alpha = scrimAlpha)
        }
    }

    fun update(stackItemScope: StackItemScopeImpl, shape: Shape) {
        depthNode?.update(currentValueOfDepth(), shape)
        if (this.stackItemScope != stackItemScope || this.shape != shape) {
            this.stackItemScope = stackItemScope
            this.shape = shape
            updateShapeAndOutline()
        }
    }

    private fun ContentDrawScope.getItemOutline(): Outline {
        itemOutline?.let {
            return it
        }
        val outline = shape.createOutline(size, layoutDirection, this)
        itemOutline = outline
        return outline
    }

    private fun updateShapeAndOutline() {
        val decoration = stackItemScope.decorations[this]
        if (decoration != null) {
            decoration.shape = shape
            decoration.size = size
            // TODO(b/413429531): make sure a shape change invalidates layout and updates clipping.
        } else {
            stackItemScope.decorations.put(this, StackItemDecoration(shape, size))
        }
        itemOutline = null
    }

    private fun calculateContentAlpha(index: Int, topItem: Int, offsetFraction: Float): Float =
        when {
            index.isTopItem(topItem = topItem) -> 1f - offsetFraction
            index.isNextItem(topItem = topItem) -> 1f
            index.isNextNextItem(topItem = topItem) -> offsetFraction
            else -> 0f
        }

    private fun calculateScrimAlpha(index: Int, topItem: Int, offsetFraction: Float): Float =
        when {
            index.isNextItem(topItem = topItem) -> (1f - offsetFraction) * MaxItemScrimAlpha
            index.isNextNextItem(topItem = topItem) -> MaxItemScrimAlpha
            else -> 0f
        }

    private fun getScrimColor(index: Int, topItem: Int): Color? =
        when {
            index.isNextItem(topItem = topItem) -> SurfaceLow
            index.isNextNextItem(topItem = topItem) -> SurfaceLow
            else -> null
        }

    private fun currentValueOfDepth() = currentValueOf(LocalGlimmerTheme).depthLevels.level2
}

/** Returns whether the index is of the top of the stack item. */
internal fun Int.isTopItem(topItem: Int) = this == topItem

/** Returns whether the index is of the next item that follows the top of the stack item. */
internal fun Int.isNextItem(topItem: Int) = this == topItem + 1

/** Returns whether the index is of the next-next item after the top of the stack item. */
internal fun Int.isNextNextItem(topItem: Int) = this == topItem + 2

private val SurfaceLow = Color(0xFF4F4F4F)
private val MaxItemScrimAlpha = 0.5f
