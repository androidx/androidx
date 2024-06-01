/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.text.Handle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.takeOrElse
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.PopupPositionProvider

internal val HandleWidth = 25.dp
internal val HandleHeight = 25.dp

/**
 * [SelectionHandleInfo]s for the nodes representing selection handles. These nodes are in popup
 * windows, and will respond to drag gestures.
 */
internal val SelectionHandleInfoKey =
    SemanticsPropertyKey<SelectionHandleInfo>("SelectionHandleInfo")

/**
 * Information about a single selection handle popup.
 *
 * @param handle Which selection [Handle] this is about.
 * @param position The position that the handle is anchored to relative to the selectable content.
 *   This position is not necessarily the position of the popup itself, it's the position that the
 *   handle "points" to (so e.g. top-middle for [Handle.Cursor]).
 * @param anchor How the selection handle is anchored to its position
 * @param visible Whether the icon of the handle is actually shown
 */
internal data class SelectionHandleInfo(
    val handle: Handle,
    val position: Offset,
    val anchor: SelectionHandleAnchor,
    val visible: Boolean,
)

/**
 * How the selection handle is anchored to its position
 *
 * In a regular text selection, selection start is anchored to left. Only cursor handle is always
 * anchored at the middle. In a regular text selection, selection end is anchored to right.
 */
internal enum class SelectionHandleAnchor {
    Left,
    Middle,
    Right
}

@Composable
internal expect fun SelectionHandle(
    offsetProvider: OffsetProvider,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    minTouchTargetSize: DpSize = DpSize.Unspecified,
    modifier: Modifier,
)

/** Avoids boxing of [Offset] which is an inline value class. */
internal fun interface OffsetProvider {
    fun provide(): Offset
}

/**
 * Adjust coordinates for given text offset.
 *
 * Currently [android.text.Layout.getLineBottom] returns y coordinates of the next line's top
 * offset, which is not included in current line's hit area. To be able to hit current line, move up
 * this y coordinates by 1 pixel.
 */
internal fun getAdjustedCoordinates(position: Offset): Offset {
    return Offset(position.x, position.y - 1f)
}

/**
 * This [PopupPositionProvider] for a selection handle. It will position the selection handle to the
 * result of [positionProvider] in its anchor layout.
 */
internal class HandlePositionProvider(
    private val handleReferencePoint: Alignment,
    private val positionProvider: OffsetProvider,
) : PopupPositionProvider {

    /**
     * When Handle disappears, it starts reporting its position as [Offset.Unspecified]. Normally,
     * Popup is dismissed immediately when its position becomes unspecified, but for one frame a
     * position update might be requested by soon-to-be-destroyed Popup. In this case, report the
     * last known position as there are no more updates. If the first ever position is provided as
     * unspecified, start with [Offset.Zero] default.
     */
    private var prevPosition: Offset = Offset.Zero

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val position = positionProvider.provide().takeOrElse { prevPosition }
        prevPosition = position

        val adjustment = handleReferencePoint.align(popupContentSize, IntSize.Zero, layoutDirection)
        return anchorBounds.topLeft + position.round() + adjustment
    }
}

/** Computes whether the handle's appearance should be left-pointing or right-pointing. */
internal fun isLeftSelectionHandle(
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean
): Boolean {
    return if (isStartHandle) {
        isHandleLtrDirection(direction, handlesCrossed)
    } else {
        !isHandleLtrDirection(direction, handlesCrossed)
    }
}

/**
 * This method is to check if the selection handles should use the natural Ltr pointing direction.
 * If the context is Ltr and the handles are not crossed, or if the context is Rtl and the handles
 * are crossed, return true.
 *
 * In Ltr context, the start handle should point to the left, and the end handle should point to the
 * right. However, in Rtl context or when handles are crossed, the start handle should point to the
 * right, and the end handle should point to left.
 */
/*@VisibleForTesting*/
internal fun isHandleLtrDirection(
    direction: ResolvedTextDirection,
    areHandlesCrossed: Boolean
): Boolean {
    return direction == ResolvedTextDirection.Ltr && !areHandlesCrossed ||
        direction == ResolvedTextDirection.Rtl && areHandlesCrossed
}
