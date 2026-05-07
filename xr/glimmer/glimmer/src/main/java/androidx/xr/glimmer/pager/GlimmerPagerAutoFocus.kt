/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.pager

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.focus.requestFocusForChildInRootBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerInputModifierNode
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.rotary.RotaryInputModifierNode
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.abs

/**
 * A modifier that automatically requests focus for the current page in a Pager, ensuring focus
 * follows the user's navigation.
 *
 * If the Pager does not currently have focus, this modifier will not request it. Additionally,
 * during multi-page scrolls (such as `scrollToPage`), this modifier waits until the animation fully
 * settles before requesting focus to prevent interrupting the scroll.
 */
internal fun Modifier.pagerAutoFocus(pagerState: GlimmerPagerState): Modifier =
    this then PagerAutoFocusNodeElement(pagerState)

private class PagerAutoFocusNodeElement(private val pagerState: GlimmerPagerState) :
    ModifierNodeElement<PagerAutoFocusNode>() {
    override fun create(): PagerAutoFocusNode = PagerAutoFocusNode(pagerState)

    override fun update(node: PagerAutoFocusNode) {
        node.update(pagerState)
    }

    override fun hashCode(): Int = pagerState.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PagerAutoFocusNodeElement) return false
        if (pagerState != other.pagerState) return false
        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pagerAutofocus"
        properties["pagerState"] = pagerState
    }
}

private class PagerAutoFocusNode(private var state: GlimmerPagerState) :
    DelegatingNode(),
    KeyInputModifierNode,
    IndirectPointerInputModifierNode,
    PointerInputModifierNode,
    RotaryInputModifierNode,
    LayoutModifierNode {

    private val focusTargetModifierNode =
        delegate(FocusTargetModifierNode(focusability = Focusability.Never))
    private var isAutofocusEnabled: Boolean = true

    fun update(state: GlimmerPagerState) {
        this.state = state
        isAutofocusEnabled = true
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
            notifyAutoFocus()
        }
    }

    private fun notifyAutoFocus() {
        if (!isAutofocusEnabled) {
            // Only request focus after indirect pointer events.
            return
        }

        if (!focusTargetModifierNode.focusState.hasFocus) {
            // Only request focus for child when Pager already has focus.
            return
        }

        if (abs(state.targetPage - state.settledPage) > 1) {
            // During multi page scrolls such as `scrollToPage`, wait until the animation fully
            // settles before attempting to request a focus to a child. If not, scroll is getting
            // interrupted by request focus action.
            return
        }

        requestPageFocus()
    }

    private fun requestPageFocus() {
        val coordinates = requireLayoutCoordinates()
        val rootTopLeft = coordinates.localToRoot(Offset.Zero)

        val layoutInfo = state.layoutInfo
        val pageSize = layoutInfo.pageSize
        val currentPageScrollOffset = state.currentPageOffsetFraction * pageSize

        val pageLeftX =
            if (requireLayoutDirection() == LayoutDirection.Rtl || layoutInfo.reverseLayout) {
                rootTopLeft.x + currentPageScrollOffset
            } else {
                rootTopLeft.x - currentPageScrollOffset
            }
        val pageRightX = pageLeftX + pageSize

        // `requestFocusForChildInRootBounds` performs a strict intersection check.
        // `intersectionTolerancePx` ensures focus requested area sits definitively inside the
        // page's bounds.
        val intersectionTolerancePx = requireDensity().density
        requestFocusForChildInRootBounds(
            left = (pageLeftX + intersectionTolerancePx).fastRoundToInt(),
            top = rootTopLeft.y.fastRoundToInt(),
            right = (pageRightX - intersectionTolerancePx).fastRoundToInt(),
            bottom = (rootTopLeft.y + coordinates.size.height).fastRoundToInt(),
        )
    }

    override fun onPreKeyEvent(event: KeyEvent): Boolean {
        isAutofocusEnabled = false
        return false
    }

    override fun onKeyEvent(event: KeyEvent): Boolean = false

    override fun onIndirectPointerEvent(event: IndirectPointerEvent, pass: PointerEventPass) {
        isAutofocusEnabled = true
    }

    override fun onCancelIndirectPointerInput() = Unit

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        isAutofocusEnabled = false
    }

    override fun onCancelPointerInput() {}

    override fun onRotaryScrollEvent(event: RotaryScrollEvent): Boolean = false

    override fun onPreRotaryScrollEvent(event: RotaryScrollEvent): Boolean {
        isAutofocusEnabled = false
        return false
    }
}
