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
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import androidx.ui.unit.round
import androidx.ui.unit.toPx

/**
 * [LayoutNodeWrapper] with default implementations for methods.
 */
internal open class DelegatingLayoutNodeWrapper<T : Modifier.Element>(
    override val wrapped: LayoutNodeWrapper,
    val modifier: T
) : LayoutNodeWrapper(wrapped.layoutNode) {
    override val providedAlignmentLines: Set<AlignmentLine>
        get() = wrapped.providedAlignmentLines

    private var _isAttached = true
    override val isAttached: Boolean
        get() = _isAttached && layoutNode.isAttached()

    override val measureScope: MeasureScope get() = wrapped.measureScope

    init {
        wrapped.wrappedBy = this
    }

    override fun draw(canvas: Canvas) {
        withPositionTranslation(canvas) {
            wrapped.draw(canvas)
        }
    }

    override fun hitTest(
        pointerPositionRelativeToScreen: PxPosition,
        hitPointerInputFilters: MutableList<PointerInputFilter>
    ): Boolean {
        if (isGlobalPointerInBounds(pointerPositionRelativeToScreen)) {
            return wrapped.hitTest(pointerPositionRelativeToScreen, hitPointerInputFilters)
        } else {
            // Anything out of bounds of ourselves can't be hit.
            return false
        }
    }

    override fun get(line: AlignmentLine): IntPx? {
        val value = wrapped[line] ?: return null
        val px = value.toPx()
        val pos = wrapped.toParentPosition(PxPosition(px, px))
        return if (line is HorizontalAlignmentLine) pos.y.round() else pos.y.round()
    }

    override fun place(position: IntPxPosition) {
        this.position = position
        with(InnerPlacementScope) {
            this.parentLayoutDirection = measureScope.layoutDirection
            val previousParentWidth = parentWidth
            parentWidth = measuredSize.width
            measureResult.placeChildren(measureScope.layoutDirection)
            parentWidth = previousParentWidth
        }
    }

    override fun performMeasure(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): Placeable {
        val placeable = wrapped.measure(constraints, layoutDirection)
        measureResult = object : MeasureScope.MeasureResult {
            override val width: IntPx = wrapped.measureResult.width
            override val height: IntPx = wrapped.measureResult.height
            override val alignmentLines: Map<AlignmentLine, IntPx> = emptyMap()
            override fun placeChildren(layoutDirection: LayoutDirection) {
                with(InnerPlacementScope) {
                    placeable.placeAbsolute(-apparentToRealOffset)
                }
            }
        }
        return this
    }

    override fun findFocusWrapperWrappingThisWrapper() =
        wrappedBy?.findFocusWrapperWrappingThisWrapper()

    override fun findFocusWrapperWrappedByThisWrapper() =
        wrapped.findFocusWrapperWrappedByThisWrapper()

    override fun findLastFocusWrapper(): ModifiedFocusNode? {
        var lastFocusWrapper: ModifiedFocusNode? = null

        // Find last focus wrapper for the current layout node.
        var next: ModifiedFocusNode? = findFocusWrapperWrappedByThisWrapper()
        while (next != null) {
            lastFocusWrapper = next
            next = next.wrapped.findFocusWrapperWrappedByThisWrapper()
        }
        return lastFocusWrapper
    }

    override fun minIntrinsicWidth(height: IntPx) = wrapped.minIntrinsicWidth(height)
    override fun maxIntrinsicWidth(height: IntPx) = wrapped.maxIntrinsicWidth(height)
    override fun minIntrinsicHeight(width: IntPx) = wrapped.minIntrinsicHeight(width)
    override fun maxIntrinsicHeight(width: IntPx) = wrapped.maxIntrinsicHeight(width)
    override val parentData: Any? get() = wrapped.parentData

    override fun detach() {
        _isAttached = false
        wrapped.detach()
    }
}

internal object InnerPlacementScope : Placeable.PlacementScope() {
    override var parentWidth = 0.ipx
    override var parentLayoutDirection = LayoutDirection.Ltr
}
