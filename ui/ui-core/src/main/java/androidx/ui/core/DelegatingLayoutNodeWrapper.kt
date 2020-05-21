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
import androidx.ui.core.keyinput.ModifiedKeyInputNode
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.graphics.Canvas
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx

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

    override fun get(line: AlignmentLine): IntPx? = wrapped[line]

    override fun place(position: IntPxPosition) {
        this.position = position

        // The wrapper only runs their placement block to obtain our position, which allows them
        // to calculate the offset of an alignment line we have already provided a position for.
        // No need to place our wrapped as well (we might have actually done this already in
        // get(line), to obtain the position of the alignment line the wrapper currently needs
        // our position in order ot know how to offset the value we provided).
        if (wrappedBy?.isShallowPlacing == true) return

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

    override fun findPreviousFocusWrapper() = wrappedBy?.findPreviousFocusWrapper()

    override fun findNextFocusWrapper() = wrapped.findNextFocusWrapper()

    override fun findLastFocusWrapper(): ModifiedFocusNode? {
        var lastFocusWrapper: ModifiedFocusNode? = null

        // Find last focus wrapper for the current layout node.
        var next: ModifiedFocusNode? = findNextFocusWrapper()
        while (next != null) {
            lastFocusWrapper = next
            next = next.wrapped.findNextFocusWrapper()
        }
        return lastFocusWrapper
    }

    override fun findPreviousKeyInputWrapper() = wrappedBy?.findPreviousKeyInputWrapper()

    override fun findNextKeyInputWrapper() = wrapped.findNextKeyInputWrapper()

    override fun findLastKeyInputWrapper(): ModifiedKeyInputNode? {
        val wrapper = layoutNode.innerLayoutNodeWrapper.findPreviousKeyInputWrapper()
        return if (wrapper !== this) wrapper else null
    }

    override fun minIntrinsicWidth(height: IntPx, layoutDirection: LayoutDirection) =
        wrapped.minIntrinsicWidth(height, layoutDirection)

    override fun maxIntrinsicWidth(height: IntPx, layoutDirection: LayoutDirection) =
        wrapped.maxIntrinsicWidth(height, layoutDirection)

    override fun minIntrinsicHeight(width: IntPx, layoutDirection: LayoutDirection) =
        wrapped.minIntrinsicHeight(width, layoutDirection)

    override fun maxIntrinsicHeight(width: IntPx, layoutDirection: LayoutDirection) =
        wrapped.maxIntrinsicHeight(width, layoutDirection)

    override val parentData: Any? get() = wrapped.parentData

    override fun attach() {
        wrapped.attach()
        _isAttached = true
    }

    override fun detach() {
        _isAttached = false
        wrapped.detach()
    }
}

internal object InnerPlacementScope : Placeable.PlacementScope() {
    override var parentWidth = 0.ipx
    override var parentLayoutDirection = LayoutDirection.Ltr
}
