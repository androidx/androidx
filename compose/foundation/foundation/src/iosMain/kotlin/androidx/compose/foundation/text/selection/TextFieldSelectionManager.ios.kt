/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.SelectionMagnifierElement
import androidx.compose.foundation.isPlatformMagnifierSupported
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.addTextContextMenuComponents
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItemWithComposableLeadingIcon
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal actual fun Modifier.textFieldMagnifier(
    manager: TextFieldSelectionManager
): Modifier = if (isPlatformMagnifierSupported()) {
    this.then(
        SelectionMagnifierElement(
            manager = manager,
            hapticFeedback = { hapticFeedBack },
            calculateCenter = { size -> calculateSelectionMagnifierCenterIOS(manager, size) }
        )
    )
} else {
    inspectable(
        // Publish inspector info even if magnification isn't supported.
        inspectorInfo = debugInspectorInfo {
            name = "textFieldMagnifier (not supported)"
            properties["manager"] = manager
        }
    ) { this }
}

// similar to calculateSelectionMagnifierCenterAndroid, but magnifier
// 1) displays even if the text field is empty
// 2) moves among the text field (not among text)
// 3) hides when drag goes below the text field
//
// native magnifier also hides when selection goes to the next line in multiline text field
// But! Compose text selection is a bit different from iOS:
// when we select multiple lines below the selection start on iOS - we always see the caret / handle.
// Compose caret in such scenario is always covered by finger so we don't actually see what do we select.
private fun Density.calculateSelectionMagnifierCenterIOS(
    manager: TextFieldSelectionManager,
    magnifierSize: IntSize,
): Offset {

    // state read of currentDragPosition so that we always recompose on drag position changes
    val localDragPosition = manager.currentDragPosition ?: return Offset.Unspecified

    val rawTextOffset = when (manager.draggingHandle) {
        null -> return Offset.Unspecified
        Handle.Cursor,
        Handle.SelectionStart -> manager.value.selection.start

        Handle.SelectionEnd -> manager.value.selection.end
    }

    // If the text hasn't been laid out yet, don't show the magnifier.
    val textLayoutResultProxy = manager.state?.layoutResult ?: return Offset.Unspecified
    val transformedText = manager.state?.textDelegate?.text ?: return Offset.Unspecified

    val textOffset = manager.offsetMapping
        .takeIf { transformedText.isNotEmpty() }
        ?.originalToTransformed(rawTextOffset)
        ?.coerceIn(transformedText.indices)
        ?: 0

    val layoutResult = textLayoutResultProxy.value

    val innerDragPosition = textLayoutResultProxy
        .translateDecorationToInnerCoordinates(localDragPosition)

    // hide magnifier when selection goes below the text field
    if (innerDragPosition.y > layoutResult.lastBaseline + MagnifierPostTravel.toPx()) {
        return Offset.Unspecified
    }

    val innerFieldBounds = manager.state?.layoutResult
        ?.innerTextFieldCoordinates
        ?.visibleBounds()
        ?.takeIf { !it.isEmpty }
        ?: return Offset.Unspecified

    // Center vertically on the current line.
    val centerY = if (transformedText.isNotEmpty()) {
        val line = layoutResult.getLineForOffset(textOffset)
        val top = layoutResult.getLineTop(line)
        val bottom = layoutResult.getLineBottom(line)
        ((bottom - top) / 2) + top
    } else {
        // can't get line bounds for empty field
        // better alternatives?
        innerFieldBounds.center.y
    }

    // native magnifier goes a little bit farther than text field bounds
    val centerX = innerDragPosition.x.coerceIn(
        -magnifierSize.width / 4f,
        max(0f, innerFieldBounds.right) + magnifierSize.width / 4
    )

    return Offset(centerX, centerY)
}

/**
 * Bottom drag point below the last text baseline after that magnifier is dismissed
 * */
internal val MagnifierPostTravel = 36.dp

/**
 * Multiplier for height tolerance calculation.
 * iOS has different location for the knobs of selection handles (leading - top, trailing - bottom),
 * and they might be not seen in mixed BiDi text, so this tolerance is required for their visibility in this case
 * if more than 80% of selection handle is visible, it should be shown
 */
private const val HeightToleranceFactor = 0.2f

/**
 * Whether the selection handle is in the visible bound of the TextField.
 */
internal actual fun TextFieldSelectionManager.isSelectionHandleInVisibleBound(
    isStartHandle: Boolean
): Boolean {
    val visibleBounds = state?.layoutCoordinates?.visibleBounds() ?: return false

    val handlePositionInText = if (isStartHandle) value.selection.start else value.selection.end
    val line = state?.layoutResult?.value?.getLineForOffset(handlePositionInText) ?: 0
    val handleHeight =
        state?.layoutResult?.value?.multiParagraph?.getLineHeight(line) ?: 0f
    val handleOffset = getHandlePosition(isStartHandle)

    return isSelectionHandleIsVisible(isStartHandle, handleOffset, handleHeight, visibleBounds)
}

internal fun isSelectionHandleIsVisible(isStartHandle: Boolean, position: Offset, height: Float, visibleBounds: Rect): Boolean {
    val containsHorizontal = position.x in visibleBounds.left..visibleBounds.right
    val heightTolerance = height * HeightToleranceFactor
    val toleratedY =
        if (isStartHandle) position.y - height + heightTolerance else position.y - heightTolerance
    val containsVertical = toleratedY in visibleBounds.top..visibleBounds.bottom

    return containsHorizontal && containsVertical
}

internal actual fun Modifier.addBasicTextFieldTextContextMenuComponents(
    manager: TextFieldSelectionManager,
    coroutineScope: CoroutineScope,
): Modifier = addTextContextMenuComponents {
    fun TextContextMenuBuilderScope.textFieldItem(
        key: Any,
        enabled: Boolean,
        onClick: () -> Unit,
    ) {
        addComponent(
            TextContextMenuItemWithComposableLeadingIcon(
                key = key,
                label = "$key",
                enabled = enabled,
                onClick = {
                    onClick()
                    close()
                    coroutineScope.launch {
                        manager.updateClipboardEntry()
                    }
                })
        )
    }

    with(manager) {
        separator()
        textFieldItem(TextContextMenuKeys.CutKey, enabled = canShowCutMenuItem()) { cut() }
        textFieldItem(TextContextMenuKeys.CopyKey, enabled = canShowCopyMenuItem()) { copy(cancelSelection = false) }
        textFieldItem(TextContextMenuKeys.PasteKey, enabled = canShowPasteMenuItem()) { paste() }
        textFieldItem(TextContextMenuKeys.SelectAllKey, enabled = canShowSelectAllMenuItem()) { selectAll() }
        separator()
    }
}
