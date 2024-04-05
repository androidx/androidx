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

import androidx.compose.foundation.PlatformMagnifierFactory
import androidx.compose.foundation.isPlatformMagnifierSupported
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

internal actual fun Modifier.textFieldMagnifier(manager: TextFieldSelectionManager): Modifier {
    if (!isPlatformMagnifierSupported()) {
        return this
    }

    return composed {
        val density = LocalDensity.current
        var magnifierSize by remember { mutableStateOf(IntSize.Zero) }

        val color = LocalTextSelectionColors.current

        magnifier(
            sourceCenter = {
                // Don't animate position as it is automatically animated by the framework
                calculateSelectionMagnifierCenterIOS(
                    manager = manager,
                    magnifierSize = magnifierSize,
                    density = density.density
                )
            },
            onSizeChanged = { size ->
                magnifierSize = with(density) {
                    IntSize(size.width.roundToPx(), size.height.roundToPx())
                }
            },
            color = color.handleColor, // align magnifier border color with selection handleColor
            platformMagnifierFactory = PlatformMagnifierFactory.getForCurrentPlatform()
        )
    }
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
@OptIn(InternalFoundationTextApi::class)
private fun calculateSelectionMagnifierCenterIOS(
    manager: TextFieldSelectionManager,
    magnifierSize: IntSize,
    density : Float,
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
    if (innerDragPosition.y > layoutResult.lastBaseline + HideThresholdDp * density) {
        return Offset.Unspecified
    }

    val innerFieldBounds = manager.state?.layoutResult
        ?.innerTextFieldCoordinates?.visibleBounds()
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
        innerFieldBounds.right + magnifierSize.width / 4
    )

    return Offset(centerX, centerY)
}

private const val HideThresholdDp = 36
