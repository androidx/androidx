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

package androidx.compose.foundation.text2.input.internal.selection

import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.DefaultSelectionHandle
import androidx.compose.foundation.text.selection.HandleReferencePoint
import androidx.compose.foundation.text.selection.SelectionHandleAnchor
import androidx.compose.foundation.text.selection.SelectionHandleInfo
import androidx.compose.foundation.text.selection.SelectionHandleInfoKey
import androidx.compose.foundation.text.selection.isLeft
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.takeOrElse
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

@Composable
internal actual fun TextFieldSelectionHandle2(
    positionProvider: OffsetProvider,
    isStartHandle: Boolean,
    direction: ResolvedTextDirection,
    handlesCrossed: Boolean,
    modifier: Modifier
) {
    val isLeft = isLeft(isStartHandle, direction, handlesCrossed)
    // The left selection handle's top right is placed at the given position, and vice versa.
    val handleReferencePoint = if (isLeft) {
        HandleReferencePoint.TopRight
    } else {
        HandleReferencePoint.TopLeft
    }

    HandlePopup2(
        positionProvider = positionProvider,
        handleReferencePoint = handleReferencePoint
    ) {
        DefaultSelectionHandle(
            modifier = modifier
                .semantics {
                    this[SelectionHandleInfoKey] = SelectionHandleInfo(
                        handle = if (isStartHandle) {
                            Handle.SelectionStart
                        } else {
                            Handle.SelectionEnd
                        },
                        position = positionProvider.provide(),
                        anchor = if (isLeft) {
                            SelectionHandleAnchor.Left
                        } else {
                            SelectionHandleAnchor.Right
                        }
                    )
                },
            isStartHandle = isStartHandle,
            direction = direction,
            handlesCrossed = handlesCrossed
        )
    }
}

/**
 * An alternative HandlePopup API that uses dynamic positioning. This enables us to update the
 * handle position when onGloballyPositioned is called.
 */
@Composable
internal fun HandlePopup2(
    positionProvider: OffsetProvider,
    handleReferencePoint: HandleReferencePoint,
    content: @Composable () -> Unit
) {
    val popupPositioner = remember(handleReferencePoint, positionProvider) {
        HandlePositionProvider2(handleReferencePoint, positionProvider)
    }

    Popup(
        popupPositionProvider = popupPositioner,
        properties = PopupProperties(
            excludeFromSystemGesture = true,
            clippingEnabled = false
        ),
        content = content
    )
}

internal class HandlePositionProvider2(
    private val handleReferencePoint: HandleReferencePoint,
    private val positionProvider: OffsetProvider
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
        val intOffset = position.round()

        return when (handleReferencePoint) {
            HandleReferencePoint.TopLeft ->
                IntOffset(
                    x = anchorBounds.left + intOffset.x,
                    y = anchorBounds.top + intOffset.y
                )
            HandleReferencePoint.TopRight ->
                IntOffset(
                    x = anchorBounds.left + intOffset.x - popupContentSize.width,
                    y = anchorBounds.top + intOffset.y
                )
            HandleReferencePoint.TopMiddle ->
                IntOffset(
                    x = anchorBounds.left + intOffset.x - popupContentSize.width / 2,
                    y = anchorBounds.top + intOffset.y
                )
        }
    }
}
