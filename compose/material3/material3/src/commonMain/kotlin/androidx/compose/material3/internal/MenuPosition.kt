/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.internal

import androidx.collection.IntList
import androidx.collection.MutableIntList
import androidx.compose.material3.DropdownMenuPopupPositionProvider
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuHorizontalMargin
import androidx.compose.material3.MenuPositionScopeImpl
import androidx.compose.material3.MenuVerticalMargin
import androidx.compose.material3.calculateTransformOrigin
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Interfaces for positioning a menu within a window. This is the same purpose as the interface
 * [PopupPositionProvider], except [Vertical] and [Horizontal] separate out the positioning logic
 * for each direction individually.
 */
@Stable
internal object MenuPosition {
    /**
     * An interface to calculate the vertical position of a menu with respect to its anchor and
     * window. The returned y-coordinate is relative to the window.
     *
     * @see PopupPositionProvider
     */
    @Stable
    fun interface Vertical {
        fun position(anchorBounds: IntRect, windowSize: IntSize, menuHeight: Int): Int
    }

    /**
     * An interface to calculate the horizontal position of a menu with respect to its anchor,
     * window, and layout direction. The returned x-coordinate is relative to the window.
     *
     * @see PopupPositionProvider
     */
    @Stable
    fun interface Horizontal {
        fun position(
            anchorBounds: IntRect,
            windowSize: IntSize,
            menuWidth: Int,
            layoutDirection: LayoutDirection,
        ): Int
    }

    /** [MenuPosition.Horizontal] which aligns the start of the menu to the start of the anchor. */
    val startToAnchorStart: Horizontal =
        AnchorAlignmentOffsetPosition.Horizontal(
            menuAlignment = Alignment.Start,
            anchorAlignment = Alignment.Start,
        )

    /** [MenuPosition.Horizontal] which aligns the start of the menu to the end of the anchor. */
    val startToAnchorEnd: Horizontal =
        AnchorAlignmentOffsetPosition.Horizontal(
            menuAlignment = Alignment.Start,
            anchorAlignment = Alignment.End,
        )

    /** [MenuPosition.Horizontal] which aligns the end of the menu to the end of the anchor. */
    val endToAnchorEnd: Horizontal =
        AnchorAlignmentOffsetPosition.Horizontal(
            menuAlignment = Alignment.End,
            anchorAlignment = Alignment.End,
        )

    /** [MenuPosition.Horizontal] which aligns the end of the menu to the start of the anchor. */
    val endToAnchorStart: Horizontal =
        AnchorAlignmentOffsetPosition.Horizontal(
            menuAlignment = Alignment.End,
            anchorAlignment = Alignment.Start,
        )

    /** [MenuPosition.Horizontal] which aligns the left of the menu to the left of the window. */
    val leftToWindowLeft: Horizontal =
        WindowAlignmentMarginPosition.Horizontal(alignment = AbsoluteAlignment.Left)

    /** [MenuPosition.Horizontal] which aligns the right of the menu to the right of the window. */
    val rightToWindowRight: Horizontal =
        WindowAlignmentMarginPosition.Horizontal(alignment = AbsoluteAlignment.Right)

    /** [MenuPosition.Vertical] which aligns the top of the menu to the bottom of the anchor. */
    val topToAnchorBottom: Vertical =
        AnchorAlignmentOffsetPosition.Vertical(
            menuAlignment = Alignment.Top,
            anchorAlignment = Alignment.Bottom,
        )

    /** [MenuPosition.Vertical] which aligns the top of the menu to the top of the anchor. */
    val topToAnchorTop: Vertical =
        AnchorAlignmentOffsetPosition.Vertical(
            menuAlignment = Alignment.Top,
            anchorAlignment = Alignment.Top,
        )

    /** [MenuPosition.Vertical] which aligns the bottom of the menu to the top of the anchor. */
    val bottomToAnchorTop: Vertical =
        AnchorAlignmentOffsetPosition.Vertical(
            menuAlignment = Alignment.Bottom,
            anchorAlignment = Alignment.Top,
        )

    /** [MenuPosition.Vertical] which aligns the bottom of the menu to the bottom of the anchor. */
    val bottomToAnchorBottom: Vertical =
        AnchorAlignmentOffsetPosition.Vertical(
            menuAlignment = Alignment.Bottom,
            anchorAlignment = Alignment.Bottom,
        )

    /** [MenuPosition.Vertical] which aligns the center of the menu to the top of the anchor. */
    val centerToAnchorTop: Vertical =
        AnchorAlignmentOffsetPosition.Vertical(
            menuAlignment = Alignment.CenterVertically,
            anchorAlignment = Alignment.Top,
        )

    /** [MenuPosition.Vertical] which aligns the top of the menu to the top of the window. */
    val topToWindowTop: Vertical = WindowAlignmentMarginPosition.Vertical(alignment = Alignment.Top)

    /** [MenuPosition.Vertical] which aligns the bottom of the menu to the bottom of the window. */
    val bottomToWindowBottom: Vertical =
        WindowAlignmentMarginPosition.Vertical(alignment = Alignment.Bottom)

    internal fun xValuesFromCandidates(
        xCandidates: List<Horizontal>,
        anchorBounds: IntRect,
        windowSize: IntSize,
        menuWidth: Int,
        layoutDirection: LayoutDirection,
    ): IntList {
        val xCandidatesMapped = MutableIntList(xCandidates.size)
        for (i in xCandidates.indices) {
            xCandidatesMapped.add(
                xCandidates[i].position(
                    anchorBounds = anchorBounds,
                    windowSize = windowSize,
                    menuWidth = menuWidth,
                    layoutDirection = layoutDirection,
                )
            )
        }
        return xCandidatesMapped
    }

    internal fun yValuesFromCandidates(
        yCandidates: List<Vertical>,
        anchorBounds: IntRect,
        windowSize: IntSize,
        menuHeight: Int,
    ): IntList {
        val yCandidatesMapped = MutableIntList(yCandidates.size)
        for (i in yCandidates.indices) {
            yCandidatesMapped.add(
                yCandidates[i].position(
                    anchorBounds = anchorBounds,
                    windowSize = windowSize,
                    menuHeight = menuHeight,
                )
            )
        }
        return yCandidatesMapped
    }
}

@Immutable
internal object AnchorAlignmentOffsetPosition {
    /**
     * A [MenuPosition.Horizontal] which horizontally aligns the given [menuAlignment] with the
     * given [anchorAlignment].
     */
    @Immutable
    data class Horizontal(
        private val menuAlignment: Alignment.Horizontal,
        private val anchorAlignment: Alignment.Horizontal,
    ) : MenuPosition.Horizontal {
        override fun position(
            anchorBounds: IntRect,
            windowSize: IntSize,
            menuWidth: Int,
            layoutDirection: LayoutDirection,
        ): Int {
            val anchorAlignmentOffset =
                anchorAlignment.align(
                    size = 0,
                    space = anchorBounds.width,
                    layoutDirection = layoutDirection,
                )
            val menuAlignmentOffset =
                -menuAlignment.align(size = 0, space = menuWidth, layoutDirection)
            return anchorBounds.left + anchorAlignmentOffset + menuAlignmentOffset
        }
    }

    /**
     * A [MenuPosition.Vertical] which vertically aligns the given [menuAlignment] with the given
     * [anchorAlignment].
     */
    @Immutable
    data class Vertical(
        private val menuAlignment: Alignment.Vertical,
        private val anchorAlignment: Alignment.Vertical,
    ) : MenuPosition.Vertical {
        override fun position(anchorBounds: IntRect, windowSize: IntSize, menuHeight: Int): Int {
            val anchorAlignmentOffset = anchorAlignment.align(size = 0, space = anchorBounds.height)
            val menuAlignmentOffset = -menuAlignment.align(size = 0, space = menuHeight)
            return anchorBounds.top + anchorAlignmentOffset + menuAlignmentOffset
        }
    }
}

@Immutable
internal object WindowAlignmentMarginPosition {
    /**
     * A [MenuPosition.Horizontal] which horizontally aligns the menu within the window according to
     * the given [alignment].
     */
    @Immutable
    data class Horizontal(private val alignment: Alignment.Horizontal) : MenuPosition.Horizontal {
        override fun position(
            anchorBounds: IntRect,
            windowSize: IntSize,
            menuWidth: Int,
            layoutDirection: LayoutDirection,
        ): Int {
            return alignment.align(
                size = menuWidth,
                space = windowSize.width,
                layoutDirection = layoutDirection,
            )
        }
    }

    /**
     * A [MenuPosition.Vertical] which vertically aligns the menu within the window according to the
     * given [alignment].
     */
    @Immutable
    data class Vertical(private val alignment: Alignment.Vertical) : MenuPosition.Vertical {
        override fun position(anchorBounds: IntRect, windowSize: IntSize, menuHeight: Int): Int {
            return alignment.align(size = menuHeight, space = windowSize.height)
        }
    }
}

/** Calculates the position of a Material [androidx.compose.material3.DropdownMenu]. */
@Immutable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal data class DropdownMenuPositionProvider(
    val contentOffset: DpOffset,
    val density: Density,
    val dropdownMenuAnchorPosition: MenuAnchorPosition,
    val verticalMargin: Int = with(density) { MenuVerticalMargin.roundToPx() },
    val horizontalMargin: Int = with(density) { MenuHorizontalMargin.roundToPx() },
    val onPositionCalculated: (anchorBounds: IntRect, menuBounds: IntRect) -> Unit = { _, _ -> },
) : DropdownMenuPopupPositionProvider {
    override var transformOrigin by mutableStateOf(TransformOrigin.Center)
        private set

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val scope =
            MenuPositionScopeImpl(
                anchorBounds = anchorBounds,
                windowSize = windowSize,
                menuSize = popupContentSize,
                layoutDirection = layoutDirection,
            )
        val xCandidates = dropdownMenuAnchorPosition.xCandidates(scope)
        val yCandidates = dropdownMenuAnchorPosition.yCandidates(scope)

        return positioningLogic(
            xCandidates,
            yCandidates,
            anchorBounds,
            windowSize,
            popupContentSize,
            layoutDirection,
        )
    }

    private fun positioningLogic(
        xCandidates: IntList,
        yCandidates: IntList,
        anchorBounds: IntRect,
        windowSize: IntSize,
        popupContentSize: IntSize,
        layoutDirection: LayoutDirection,
    ): IntOffset {
        val contentOffsetX =
            with(density) {
                contentOffset.x.roundToPx() *
                    (if (layoutDirection == LayoutDirection.Ltr) 1 else -1)
            }
        val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

        var x = 0
        for (index in xCandidates.indices) {
            val xCandidate = xCandidates[index] + contentOffsetX
            if (
                xCandidate >= horizontalMargin &&
                    xCandidate + popupContentSize.width <= windowSize.width - horizontalMargin
            ) {
                x = xCandidate
                break
            }
            if (index == xCandidates.lastIndex) {
                x =
                    if (popupContentSize.width >= windowSize.width - 2 * horizontalMargin) {
                        Alignment.CenterHorizontally.align(
                            size = popupContentSize.width,
                            space = windowSize.width,
                            layoutDirection = layoutDirection,
                        )
                    } else {
                        xCandidate.coerceIn(
                            horizontalMargin,
                            windowSize.width - horizontalMargin - popupContentSize.width,
                        )
                    }
                break
            }
        }

        var y = 0
        for (index in yCandidates.indices) {
            val yCandidate = yCandidates[index] + contentOffsetY
            if (
                yCandidate >= verticalMargin &&
                    yCandidate + popupContentSize.height <= windowSize.height - verticalMargin
            ) {
                y = yCandidate
                break
            }
            if (index == yCandidates.lastIndex) {
                y =
                    if (popupContentSize.height >= windowSize.height - 2 * verticalMargin) {
                        Alignment.CenterVertically.align(
                            size = popupContentSize.height,
                            space = windowSize.height,
                        )
                    } else {
                        yCandidate.coerceIn(
                            verticalMargin,
                            windowSize.height - verticalMargin - popupContentSize.height,
                        )
                    }
                break
            }
        }

        val menuOffset = IntOffset(x, y)
        transformOrigin =
            calculateTransformOrigin(anchorBounds, IntRect(offset = menuOffset, popupContentSize))
        onPositionCalculated(anchorBounds, IntRect(offset = menuOffset, size = popupContentSize))
        return menuOffset
    }
}
