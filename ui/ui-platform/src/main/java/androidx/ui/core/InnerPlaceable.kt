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

import androidx.ui.core.focus.ModifiedFocusNode
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.PxPosition
import androidx.ui.unit.toPxSize
import androidx.ui.util.fastAny
import androidx.ui.util.fastForEach

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
            layoutNode.layoutChildren,
            constraints,
            measureScope.layoutDirection
        )
        layoutNode.handleMeasureResult(measureResult)
        return this
    }

    override val parentData: Any?
        @Suppress("DEPRECATION")
        get() = if (layoutNode.handlesParentData) {
            layoutNode.parentDataNode?.value
        } else {
            layoutNode.parentDataNode?.value
                ?: layoutNode.layoutChildren
                    .firstOrNull { it.layoutNodeWrapper.parentData != null }?.parentData
        }

    override fun findFocusWrapperWrappingThisWrapper() =
        wrappedBy?.findFocusWrapperWrappingThisWrapper()

    override fun findFocusWrapperWrappedByThisWrapper() = null

    override fun findLastFocusWrapper(): ModifiedFocusNode? = findFocusWrapperWrappingThisWrapper()

    override fun minIntrinsicWidth(height: IntPx): IntPx {
        return layoutNode.measureBlocks.minIntrinsicWidth(
            layoutNode.measureScope,
            layoutNode.layoutChildren,
            height,
            measureScope.layoutDirection
        )
    }

    override fun minIntrinsicHeight(width: IntPx): IntPx {
        return layoutNode.measureBlocks.minIntrinsicHeight(
            layoutNode.measureScope,
            layoutNode.layoutChildren,
            width,
            measureScope.layoutDirection
        )
    }

    override fun maxIntrinsicWidth(height: IntPx): IntPx {
        return layoutNode.measureBlocks.maxIntrinsicWidth(
            layoutNode.measureScope,
            layoutNode.layoutChildren,
            height,
            measureScope.layoutDirection
        )
    }

    override fun maxIntrinsicHeight(width: IntPx): IntPx {
        return layoutNode.measureBlocks.maxIntrinsicHeight(
            layoutNode.measureScope,
            layoutNode.layoutChildren,
            width,
            measureScope.layoutDirection
        )
    }

    override fun place(position: IntPxPosition) {
        layoutNode.isPlaced = true
        this.position = position
        layoutNode.layout()
    }

    override operator fun get(line: AlignmentLine): IntPx? {
        return layoutNode.calculateAlignmentLines()[line]
    }

    override fun draw(canvas: Canvas) {
        withPositionTranslation(canvas) {
            val owner = layoutNode.requireOwner()
            val sizePx = measuredSize.toPxSize()
            layoutNode.zIndexSortedChildren.fastForEach { child ->
                owner.callDraw(canvas, child, sizePx)
            }
            if (owner.showLayoutBounds) {
                drawBorder(canvas, innerBoundsPaint)
            }
        }
    }

    override fun hitTest(
        pointerPositionRelativeToScreen: PxPosition,
        hitPointerInputFilters: MutableList<PointerInputFilter>
    ): Boolean {
        if (isGlobalPointerInBounds(pointerPositionRelativeToScreen)) {
            // Any because as soon as true is returned, we know we have found a hit path and we must
            //  not add PointerInputFilters on different paths so we should not even go looking.
            return layoutNode.children.reversed().fastAny { child ->
                callHitTest(child, pointerPositionRelativeToScreen, hitPointerInputFilters)
            }
        } else {
            // Anything out of bounds of ourselves can't be hit.
            return false
        }
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
            node: ComponentNode,
            globalPoint: PxPosition,
            hitPointerInputFilters: MutableList<PointerInputFilter>
        ): Boolean {
            if (node is LayoutNode) {
                return node.hitTest(globalPoint, hitPointerInputFilters)
            } else {
                // Any because as soon as true is returned, we know we have found a hit path and we must
                // not add PointerInputFilters on different paths so we should not even go looking.
                return node.children.reversed().fastAny { child ->
                    callHitTest(child, globalPoint, hitPointerInputFilters)
                }
            }
        }
    }
}