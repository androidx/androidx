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

import androidx.ui.core.LayoutNode.LayoutState.Ready
import androidx.ui.core.focus.ExperimentalFocus
import androidx.ui.core.focus.FocusState2
import androidx.ui.core.focus.ModifiedFocusNode
import androidx.ui.core.focus.ModifiedFocusNode2
import androidx.ui.core.keyinput.ModifiedKeyInputNode
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.ui.unit.Density
import androidx.ui.unit.IntOffset
import androidx.ui.util.fastAny
import androidx.ui.util.fastForEach

@OptIn(ExperimentalLayoutNodeApi::class)
internal class InnerPlaceable(
    layoutNode: LayoutNode
) : LayoutNodeWrapper(layoutNode), Density by layoutNode.measureScope {

    override val providedAlignmentLines: Set<AlignmentLine>
        get() = layoutNode.providedAlignmentLines.keys
    override val isAttached: Boolean
        get() = layoutNode.isAttached()

    override val measureScope get() = layoutNode.measureScope

    override fun performMeasure(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): Placeable {
        layoutNode.layoutDirection = layoutDirection
        val measureResult = layoutNode.measureBlocks.measure(
            layoutNode.measureScope,
            layoutNode.children,
            constraints,
            measureScope.layoutDirection
        )
        layoutNode.handleMeasureResult(measureResult)
        return this
    }

    override val parentData: Any?
        get() = null

    override fun findPreviousFocusWrapper() = wrappedBy?.findPreviousFocusWrapper()

    override fun findPreviousFocusWrapper2() = wrappedBy?.findPreviousFocusWrapper2()

    override fun findNextFocusWrapper() = null

    override fun findNextFocusWrapper2() = null

    override fun findLastFocusWrapper(): ModifiedFocusNode? = findPreviousFocusWrapper()

    override fun findLastFocusWrapper2(): ModifiedFocusNode2? = findPreviousFocusWrapper2()

    @OptIn(ExperimentalFocus::class)
    override fun propagateFocusStateChange(focusState: FocusState2) {
        // TODO(b/160922058): Propagate focus controllers across layout nodes.
        // wrappedBy?.propagateFocusStateChange(focusState)
    }

    override fun findPreviousKeyInputWrapper() = wrappedBy?.findPreviousKeyInputWrapper()

    override fun findNextKeyInputWrapper() = null

    override fun findLastKeyInputWrapper(): ModifiedKeyInputNode? = findPreviousKeyInputWrapper()

    override fun minIntrinsicWidth(height: Int, layoutDirection: LayoutDirection): Int {
        return layoutNode.measureBlocks.minIntrinsicWidth(
            measureScope,
            layoutNode.children,
            height,
            layoutDirection
        )
    }

    override fun minIntrinsicHeight(width: Int, layoutDirection: LayoutDirection): Int {
        return layoutNode.measureBlocks.minIntrinsicHeight(
            measureScope,
            layoutNode.children,
            width,
            layoutDirection
        )
    }

    override fun maxIntrinsicWidth(height: Int, layoutDirection: LayoutDirection): Int {
        return layoutNode.measureBlocks.maxIntrinsicWidth(
            measureScope,
            layoutNode.children,
            height,
            layoutDirection
        )
    }

    override fun maxIntrinsicHeight(width: Int, layoutDirection: LayoutDirection): Int {
        return layoutNode.measureBlocks.maxIntrinsicHeight(
            measureScope,
            layoutNode.children,
            width,
            layoutDirection
        )
    }

    override fun place(position: IntOffset) {
        this.position = position

        // The wrapper only runs their placement block to obtain our position, which allows them
        // to calculate the offset of an alignment line we have already provided a position for.
        // No need to place our wrapped as well (we might have actually done this already in
        // get(line), to obtain the position of the alignment line the wrapper currently needs
        // our position in order ot know how to offset the value we provided).
        if (wrappedBy?.isShallowPlacing == true) return

        layoutNode.isPlaced = true
        layoutNode.layoutChildren()
    }

    override operator fun get(line: AlignmentLine): Int {
        return layoutNode.calculateAlignmentLines()[line] ?: AlignmentLine.Unspecified
    }

    override fun draw(canvas: Canvas) {
        withPositionTranslation(canvas) {
            val owner = layoutNode.requireOwner()
            layoutNode.zIndexSortedChildren.fastForEach { child ->
                if (child.isPlaced) {
                    require(child.layoutState == Ready) {
                        "$child is not ready. layoutState is ${child.layoutState}"
                    }
                    child.draw(canvas)
                }
            }
            if (owner.showLayoutBounds) {
                drawBorder(canvas, innerBoundsPaint)
            }
        }
    }

    override fun hitTest(
        pointerPositionRelativeToScreen: Offset,
        hitPointerInputFilters: MutableList<PointerInputFilter>
    ) {
        // Any because as soon as true is returned, we know we have found a hit path and we must
        // not add PointerInputFilters on different paths so we should not even go looking.
        val originalSize = hitPointerInputFilters.size
        layoutNode.zIndexSortedChildren.reversed().fastAny { child ->
            callHitTest(child, pointerPositionRelativeToScreen, hitPointerInputFilters)
            hitPointerInputFilters.size > originalSize
        }
    }

    override fun attach() {
        // Do nothing. InnerPlaceable only is attached when the LayoutNode is attached.
    }

    override fun detach() {
        // Do nothing. InnerPlaceable only is detached when the LayoutNode is detached.
    }

    internal companion object {
        val innerBoundsPaint = Paint().also { paint ->
            paint.color = Color.Red
            paint.strokeWidth = 1f
            paint.style = PaintingStyle.stroke
        }

        private fun callHitTest(
            node: LayoutNode,
            globalPoint: Offset,
            hitPointerInputFilters: MutableList<PointerInputFilter>
        ) {
            node.hitTest(globalPoint, hitPointerInputFilters)
        }
    }
}
