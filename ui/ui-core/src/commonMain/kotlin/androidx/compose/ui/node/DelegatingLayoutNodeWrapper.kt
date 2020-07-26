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

package androidx.compose.ui.node

import androidx.compose.ui.AlignmentLine
import androidx.compose.ui.MeasureScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Placeable
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusState2
import androidx.compose.ui.input.key.ModifiedKeyInputNode
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.nativeClass

/**
 * [LayoutNodeWrapper] with default implementations for methods.
 */
@OptIn(ExperimentalLayoutNodeApi::class)
internal open class DelegatingLayoutNodeWrapper<T : Modifier.Element>(
    override var wrapped: LayoutNodeWrapper,
    open var modifier: T
) : LayoutNodeWrapper(wrapped.layoutNode) {
    override val providedAlignmentLines: Set<AlignmentLine>
        get() = wrapped.providedAlignmentLines

    private var _isAttached = true
    override val isAttached: Boolean
        get() = _isAttached && layoutNode.isAttached()

    override val measureScope: MeasureScope get() = wrapped.measureScope

    /**
     * Indicates that this modifier is used in [wrappedBy] also.
     */
    var isChained = false

    init {
        wrapped.wrappedBy = this
    }

    /**
     * Sets the modifier instance to the new modifier. [modifier] must be the
     * same type as the current modifier.
     */
    fun setModifierTo(modifier: Modifier.Element) {
        if (modifier !== this.modifier) {
            require(modifier.nativeClass() == this.modifier.nativeClass())
            @Suppress("UNCHECKED_CAST")
            this.modifier = modifier as T
        }
    }

    override fun draw(canvas: Canvas) {
        withPositionTranslation(canvas) {
            wrapped.draw(canvas)
        }
    }

    override fun hitTest(
        pointerPositionRelativeToScreen: Offset,
        hitPointerInputFilters: MutableList<PointerInputFilter>
    ) {
        wrapped.hitTest(pointerPositionRelativeToScreen, hitPointerInputFilters)
    }

    override fun get(line: AlignmentLine): Int = wrapped[line]

    override fun place(position: IntOffset) {
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
            override val width: Int = wrapped.measureResult.width
            override val height: Int = wrapped.measureResult.height
            override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()
            override fun placeChildren(layoutDirection: LayoutDirection) {
                with(InnerPlacementScope) {
                    placeable.placeAbsolute(-apparentToRealOffset)
                }
            }
        }
        return this
    }

    override fun findPreviousFocusWrapper() = wrappedBy?.findPreviousFocusWrapper()

    override fun findPreviousFocusWrapper2() = wrappedBy?.findPreviousFocusWrapper2()

    override fun findNextFocusWrapper() = wrapped.findNextFocusWrapper()

    override fun findNextFocusWrapper2() = wrapped.findNextFocusWrapper2()

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

    override fun findLastFocusWrapper2(): ModifiedFocusNode2? {
        var lastFocusWrapper: ModifiedFocusNode2? = null

        // Find last focus wrapper for the current layout node.
        var next: ModifiedFocusNode2? = findNextFocusWrapper2()
        while (next != null) {
            lastFocusWrapper = next
            next = next.wrapped.findNextFocusWrapper2()
        }
        return lastFocusWrapper
    }

    @OptIn(ExperimentalFocus::class)
    override fun propagateFocusStateChange(focusState: FocusState2) {
        wrappedBy?.propagateFocusStateChange(focusState)
    }

    override fun findPreviousKeyInputWrapper() = wrappedBy?.findPreviousKeyInputWrapper()

    override fun findNextKeyInputWrapper() = wrapped.findNextKeyInputWrapper()

    override fun findLastKeyInputWrapper(): ModifiedKeyInputNode? {
        val wrapper = layoutNode.innerLayoutNodeWrapper.findPreviousKeyInputWrapper()
        return if (wrapper !== this) wrapper else null
    }

    override fun minIntrinsicWidth(height: Int, layoutDirection: LayoutDirection) =
        wrapped.minIntrinsicWidth(height, layoutDirection)

    override fun maxIntrinsicWidth(height: Int, layoutDirection: LayoutDirection) =
        wrapped.maxIntrinsicWidth(height, layoutDirection)

    override fun minIntrinsicHeight(width: Int, layoutDirection: LayoutDirection) =
        wrapped.minIntrinsicHeight(width, layoutDirection)

    override fun maxIntrinsicHeight(width: Int, layoutDirection: LayoutDirection) =
        wrapped.maxIntrinsicHeight(width, layoutDirection)

    override val parentData: Any? get() = wrapped.parentData

    override fun attach() {
        _isAttached = true
    }

    override fun detach() {
        _isAttached = false
    }
}

internal object InnerPlacementScope : Placeable.PlacementScope() {
    override var parentWidth = 0
    override var parentLayoutDirection = LayoutDirection.Ltr
}
