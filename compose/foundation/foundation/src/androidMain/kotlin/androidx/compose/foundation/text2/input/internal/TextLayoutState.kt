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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.foundation.text.updateTextDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

/**
 * Manages text layout for TextField including layout coordinates of decoration box and inner text
 * field.
 */
@OptIn(InternalFoundationTextApi::class)
internal class TextLayoutState {
    /**
     * Set of parameters and an internal cache to compute text layout.
     */
    var textDelegate: TextDelegate? = null
        private set

    /**
     * Text Layout State.
     */
    var layoutResult: TextLayoutResult? by mutableStateOf(null)
        private set

    /** Measured bounds of the decoration box and inner text field. Together used to
     * calculate the relative touch offset. Because touches are applied on the decoration box, we
     * need to translate it to the inner text field coordinates.
     *
     * [LayoutCoordinates] object returned from onGloballyPositioned callback is usually the same
     * instance unless a node is detached and re-attached to the tree. To react to layout and
     * positional changes even though the object never changes, we employ a neverEqualPolicy.
     */
    var innerTextFieldCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())
    var decorationBoxCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())

    fun MeasureScope.layout(
        text: AnnotatedString,
        textStyle: TextStyle,
        softWrap: Boolean,
        density: Density,
        fontFamilyResolver: FontFamily.Resolver,
        constraints: Constraints,
        onTextLayout: Density.(TextLayoutResult) -> Unit
    ): TextLayoutResult {
        val prevResult = Snapshot.withoutReadObservation { layoutResult }

        val currTextDelegate = textDelegate

        val newTextDelegate = if (currTextDelegate != null) {
            updateTextDelegate(
                current = currTextDelegate,
                text = text,
                style = textStyle,
                softWrap = softWrap,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                placeholders = emptyList(),
            )
        } else {
            TextDelegate(
                text = text,
                style = textStyle,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                softWrap = true,
                placeholders = emptyList()
            )
        }

        return newTextDelegate.layout(
            layoutDirection = layoutDirection,
            constraints = constraints,
            prevResult = prevResult
        ).also {
            textDelegate = newTextDelegate
            if (prevResult != it) {
                onTextLayout(it)
            }
            layoutResult = it
        }
    }

    /**
     * Translates the position of the touch on the screen to the position in text. Because touch
     * is relative to the decoration box, we need to translate it to the inner text field's
     * coordinates first before calculating position of the symbol in text.
     *
     * @param position original position of the gesture relative to the decoration box
     * @param coerceInVisibleBounds if true and original [position] is outside visible bounds
     * of the inner text field, the [position] will be shifted to the closest edge of the inner
     * text field's visible bounds. This is useful when you have a decoration box
     * bigger than the inner text field, so when user touches to the decoration box area, the cursor
     * goes to the beginning or the end of the visible inner text field; otherwise if we put the
     * cursor under the touch in the invisible part of the inner text field, it would scroll to
     * make the cursor visible. This behavior is not needed, and therefore
     * [coerceInVisibleBounds] should be set to false, when the user drags outside visible bounds
     * to make a selection.
     * @return The offset that corresponds to the [position]. Returns -1 if text layout has not
     * been measured yet.
     */
    fun getOffsetForPosition(position: Offset, coerceInVisibleBounds: Boolean = true): Int {
        val layoutResult = layoutResult ?: return -1
        val relativePosition = position
            .let { if (coerceInVisibleBounds) it.coercedInVisibleBoundsOfInputText() else it }
            .relativeToInputText()
        return layoutResult.getOffsetForPosition(relativePosition)
    }

    /**
     * Translates the click happened on the decoration box to the position in the inner text
     * field coordinates. This relative position is then used to determine symbol position in
     * text using TextLayoutResult object.
     */
    private fun Offset.relativeToInputText(): Offset {
        // Translates touch to the inner text field coordinates
        return innerTextFieldCoordinates?.let { innerTextFieldCoordinates ->
            decorationBoxCoordinates?.let { decorationBoxCoordinates ->
                if (innerTextFieldCoordinates.isAttached && decorationBoxCoordinates.isAttached) {
                    innerTextFieldCoordinates.localPositionOf(decorationBoxCoordinates, this)
                } else {
                    this
                }
            }
        } ?: this
    }

    /**
     * If click on the decoration box happens outside visible inner text field, coerce the click
     * position to the visible edges of the inner text field.
     */
    private fun Offset.coercedInVisibleBoundsOfInputText(): Offset {
        // If offset is outside visible bounds of the inner text field, use visible bounds edges
        val visibleInnerTextFieldRect =
            innerTextFieldCoordinates?.let { innerTextFieldCoordinates ->
                if (innerTextFieldCoordinates.isAttached) {
                    decorationBoxCoordinates?.localBoundingBoxOf(innerTextFieldCoordinates)
                } else {
                    Rect.Zero
                }
            } ?: Rect.Zero
        return this.coerceIn(visibleInnerTextFieldRect)
    }
}

private fun Offset.coerceIn(rect: Rect): Offset {
    val xOffset = when {
        x < rect.left -> rect.left
        x > rect.right -> rect.right
        else -> x
    }
    val yOffset = when {
        y < rect.top -> rect.top
        y > rect.bottom -> rect.bottom
        else -> y
    }
    return Offset(xOffset, yOffset)
}