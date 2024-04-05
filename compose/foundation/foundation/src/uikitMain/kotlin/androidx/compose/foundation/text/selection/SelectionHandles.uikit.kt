/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.HandleReferencePoint.BottomMiddle
import androidx.compose.foundation.text.selection.HandleReferencePoint.TopLeft
import androidx.compose.foundation.text.selection.HandleReferencePoint.TopMiddle
import androidx.compose.foundation.text.selection.HandleReferencePoint.TopRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * Clickable padding of handler
 */
private val PADDING = 5.dp

/**
 * Radius of handle circle
 */
private val RADIUS = 6.dp

/**
 * Thickness of handlers vertical line
 */
private val THICKNESS = 2.dp

@Composable
internal actual fun SelectionHandle(
    offsetProvider: OffsetProvider,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    lineHeight: Float,
    modifier: Modifier,
) {
    val isLeft = isLeft(isStartHandle, direction, handlesCrossed)
    // The left selection handle's top right is placed at the given position, and vice versa.
    val handleReferencePoint = if (isLeft) BottomMiddle else TopMiddle
    val offset = if (isLeft) Offset.Zero else Offset(0f, -lineHeight)

    HandlePopup(positionProvider = offsetProvider, handleReferencePoint = handleReferencePoint, offset = offset) {
        SelectionHandleIcon(
            modifier = modifier.semantics {
                val position = offsetProvider.provide()
                this[SelectionHandleInfoKey] = SelectionHandleInfo(
                    handle = if (isStartHandle) Handle.SelectionStart else Handle.SelectionEnd,
                    position = position,
                    anchor = if (isLeft) SelectionHandleAnchor.Left else SelectionHandleAnchor.Right,
                    visible = position.isSpecified,
                )
            },
            iconVisible = { offsetProvider.provide().isSpecified },
            lineHeight = lineHeight,
            isLeft = isLeft,
        )
    }
}

@Composable
/*@VisibleForTesting*/
internal fun SelectionHandleIcon(
    modifier: Modifier,
    iconVisible: () -> Boolean,
    lineHeight: Float,
    isLeft: Boolean,
) {
    val density = LocalDensity.current
    val lineHeightDp = with(density) { lineHeight.toDp() }
    Spacer(
        modifier
            .size(
                width = (PADDING + RADIUS) * 2,
                height = RADIUS * 2 + PADDING + lineHeightDp
            )
            .drawSelectionHandle(iconVisible, lineHeight, isLeft)
    )
}

internal fun Modifier.drawSelectionHandle(
    iconVisible: () -> Boolean,
    lineHeight: Float,
    isLeft: Boolean
): Modifier = composed {
    val density = LocalDensity.current
    val paddingPx = with(density) { PADDING.toPx() }
    val radiusPx = with(density) { RADIUS.toPx() }
    val thicknessPx = with(density) { THICKNESS.toPx() }
    val handleColor = LocalTextSelectionColors.current.handleColor
    this.drawWithCache {
        onDrawWithContent {
            drawContent()
            if (!iconVisible()) return@onDrawWithContent

            // vertical line
            drawRect(
                color = handleColor,
                topLeft = Offset(
                    x = paddingPx + radiusPx - thicknessPx / 2,
                    y = if (isLeft) paddingPx + radiusPx else 0f
                ),
                size = Size(thicknessPx, lineHeight + radiusPx)
            )
            // handle circle
            drawCircle(
                color = handleColor,
                radius = radiusPx,
                center = center.copy(
                    y = if (isLeft) paddingPx + radiusPx else lineHeight + radiusPx
                )
            )
        }
    }
}

@Composable
internal fun HandlePopup(
    offset: Offset,
    positionProvider: OffsetProvider,
    handleReferencePoint: HandleReferencePoint,
    content: @Composable () -> Unit
) {
    val popupPositionProvider = remember(handleReferencePoint, positionProvider, offset) {
        HandlePositionProvider(handleReferencePoint, positionProvider, offset)
    }
    Popup(
        popupPositionProvider = popupPositionProvider,
        properties = PopupProperties(clippingEnabled = false),
        content = content,
    )
}

// TODO: Move everything below into commonMain source set (remove copy-paste from AndroidSelectionHandles.android.kt)

/**
 * The enum that specifies how a selection/cursor handle is placed to its given position.
 * When this value is [TopLeft], the top left corner of the handle will be placed at the
 * given position.
 * When this value is [TopRight], the top right corner of the handle will be placed at the
 * given position.
 * When this value is [TopMiddle], the handle top edge's middle point will be placed at the given
 * position.
 */
internal enum class HandleReferencePoint {
    TopLeft,
    TopRight,
    TopMiddle,
    BottomMiddle,
}

/**
 * This [PopupPositionProvider] for [HandlePopup]. It will position the selection handle
 * to the result of [positionProvider] in its anchor layout.
 *
 * @see HandleReferencePoint
 */
internal class HandlePositionProvider(
    private val handleReferencePoint: HandleReferencePoint,
    private val positionProvider: OffsetProvider,
    private val offset: Offset,
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
        val position = positionProvider.provide().takeOrElse { prevPosition } + offset
        prevPosition = position

        // We want the cursor to point to the position,
        // so adjust the x-axis based on where the handle is pointing.
        val xAdjustment = when (handleReferencePoint) {
            TopLeft -> 0
            TopMiddle, BottomMiddle -> popupContentSize.width / 2
            TopRight -> popupContentSize.width
        }
        val yAdjustment = when (handleReferencePoint) {
            TopLeft, TopMiddle, TopRight -> 0
            BottomMiddle -> popupContentSize.height
        }

        val offset = position.round()
        val x = anchorBounds.left + offset.x - xAdjustment
        val y = anchorBounds.top + offset.y - yAdjustment
        return IntOffset(x, y)
    }
}

/**
 * Computes whether the handle's appearance should be left-pointing or right-pointing.
 */
private fun isLeft(
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
 * This method is to check if the selection handles should use the natural Ltr pointing
 * direction.
 * If the context is Ltr and the handles are not crossed, or if the context is Rtl and the handles
 * are crossed, return true.
 *
 * In Ltr context, the start handle should point to the left, and the end handle should point to
 * the right. However, in Rtl context or when handles are crossed, the start handle should point to
 * the right, and the end handle should point to left.
 */
/*@VisibleForTesting*/
private fun isHandleLtrDirection(
    direction: ResolvedTextDirection,
    areHandlesCrossed: Boolean
): Boolean {
    return direction == ResolvedTextDirection.Ltr && !areHandlesCrossed ||
        direction == ResolvedTextDirection.Rtl && areHandlesCrossed
}
