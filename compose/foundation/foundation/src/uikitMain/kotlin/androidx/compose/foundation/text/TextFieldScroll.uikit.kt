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

package androidx.compose.foundation.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min
import kotlin.math.roundToInt

@ExperimentalFoundationApi
@Composable
internal actual fun rememberTextFieldOverscrollEffect(): OverscrollEffect? =
    rememberOverscrollEffect()

internal actual fun Modifier.textFieldScroll(
    scrollerPosition: TextFieldScrollerPosition,
    textFieldValue: TextFieldValue,
    visualTransformation: VisualTransformation,
    textLayoutResultProvider: () -> TextLayoutResultProxy?
): Modifier {
    val orientation = scrollerPosition.orientation
    val cursorOffset = scrollerPosition.getOffsetToFollow(textFieldValue.selection)
    scrollerPosition.previousSelection = textFieldValue.selection

    val transformedText = visualTransformation.filterWithValidation(textFieldValue.annotatedString)

    val layout = when (orientation) {
        Orientation.Vertical ->
            IOSVerticalScrollLayoutModifier(
                scrollerPosition,
                cursorOffset,
                transformedText,
                textLayoutResultProvider
            )

        Orientation.Horizontal ->
            IOSHorizontalScrollLayoutModifier(
                scrollerPosition,
                cursorOffset,
                transformedText,
                textLayoutResultProvider
            )
    }
    return this.clipToBounds().then(layout)
}

private data class IOSVerticalScrollLayoutModifier(
    val scrollerPosition: TextFieldScrollerPosition,
    val cursorOffset: Int,
    val transformedText: TransformedText,
    val textLayoutResultProvider: () -> TextLayoutResultProxy?
) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val childConstraints = constraints.copy(maxHeight = Constraints.Infinity)
        val placeable = measurable.measure(childConstraints)
        val height = min(placeable.height, constraints.maxHeight)

        return layout(placeable.width, height) {
            val textLayoutResult = textLayoutResultProvider()?.value
            val cursorRect = getCursorRectInScroller(
                cursorOffset = cursorOffset,
                transformedText = transformedText,
                textLayoutResult = textLayoutResult,
                rtl = false,
                textFieldWidth = placeable.width
            )

            val additionalDeltaY = if (textLayoutResult != null) {
                // On iOS we will scroll to one line before the cursor:
                val cursorLine = textLayoutResult.getLineForOffset(cursorOffset)
                val scrollToLine = (cursorLine - 1).coerceAtLeast(0)
                textLayoutResult.multiParagraph.getLineHeight(scrollToLine)
            } else {
                0f
            }

            scrollerPosition.update(
                orientation = Orientation.Vertical,
                cursorRect = cursorRect.copy(top = cursorRect.top - additionalDeltaY),
                containerSize = height,
                textFieldSize = placeable.height
            )

            val offset = -scrollerPosition.offset
            placeable.placeRelative(0, offset.roundToInt())
        }
    }
}

/**
 * Copied from commonMain source set.
 */
private data class IOSHorizontalScrollLayoutModifier(
    val scrollerPosition: TextFieldScrollerPosition,
    val cursorOffset: Int,
    val transformedText: TransformedText,
    val textLayoutResultProvider: () -> TextLayoutResultProxy?
) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // If the maxIntrinsicWidth of the children is already smaller than the constraint, pass
        // the original constraints so that the children has more information to  determine its
        // size.
        val maxIntrinsicWidth = measurable.maxIntrinsicWidth(constraints.maxHeight)
        val childConstraints = if (maxIntrinsicWidth < constraints.maxWidth) {
            constraints
        } else {
            constraints.copy(maxWidth = Constraints.Infinity)
        }
        val placeable = measurable.measure(childConstraints)
        val width = min(placeable.width, constraints.maxWidth)

        return layout(width, placeable.height) {
            val cursorRect = getCursorRectInScroller(
                cursorOffset = cursorOffset,
                transformedText = transformedText,
                textLayoutResult = textLayoutResultProvider()?.value,
                rtl = layoutDirection == LayoutDirection.Rtl,
                textFieldWidth = placeable.width
            )

            scrollerPosition.update(
                orientation = Orientation.Horizontal,
                cursorRect = cursorRect,
                containerSize = width,
                textFieldSize = placeable.width
            )

            val offset = -scrollerPosition.offset
            placeable.placeRelative(offset.roundToInt(), 0)
        }
    }
}

/**
 * Copied from commonMain source set.
 */
private fun Density.getCursorRectInScroller(
    cursorOffset: Int,
    transformedText: TransformedText,
    textLayoutResult: TextLayoutResult?,
    rtl: Boolean,
    textFieldWidth: Int
): Rect {
    val cursorRect = textLayoutResult?.getCursorRect(
        transformedText.offsetMapping.originalToTransformed(cursorOffset)
    ) ?: Rect.Zero
    val thickness = DefaultCursorThickness.roundToPx()

    val cursorLeft = if (rtl) {
        textFieldWidth - cursorRect.left - thickness
    } else {
        cursorRect.left
    }

    val cursorRight = if (rtl) {
        textFieldWidth - cursorRect.left
    } else {
        cursorRect.left + thickness
    }
    return cursorRect.copy(left = cursorLeft, right = cursorRight)
}
