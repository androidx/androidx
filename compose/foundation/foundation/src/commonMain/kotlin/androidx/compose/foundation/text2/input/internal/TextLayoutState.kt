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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Manages text layout for TextField including layout coordinates of decoration box and inner text
 * field.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class TextLayoutState {
    private var layoutCache = TextFieldLayoutStateCache()

    var onTextLayout: (Density.(() -> TextLayoutResult?) -> Unit)? = null

    val layoutResult: TextLayoutResult? by layoutCache

    /**
     * Measured layout coordinates of the decoration box, core text field, and text layout node.
     *
     * DecoratorNode
     * -------------------
     * |  CoreNode       |--> Outer Decoration Box with padding
     * |  -------------  |
     * |  |           |  |
     * |  |           |--|--> Visible inner text field
     * |  -------------  |    (Below the dashed line is not visible)
     * |  |           |  |
     * |  |           |  |
     * -------------------
     *    |           |
     *    |           |---> Scrollable part (TextLayoutNode)
     *    -------------
     *
     * These coordinates are used to calculate the relative positioning between multiple layers
     * of a BasicTextField. For example, touches are processed by the decoration box but these
     * should be converted to text layout positions to find out which character is pressed.
     *
     * [LayoutCoordinates] object returned from onGloballyPositioned callback is usually the same
     * instance unless a node is detached and re-attached to the tree. To react to layout and
     * positional changes even though the object never changes, we employ a neverEqualPolicy.
     */
    var textLayoutNodeCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())
    var coreNodeCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())
    var decoratorNodeCoordinates: LayoutCoordinates? by mutableStateOf(null, neverEqualPolicy())

    /**
     * Set to a non-zero value for single line TextFields in order to prevent text cuts.
     */
    var minHeightForSingleLineField by mutableStateOf(0.dp)

    /**
     * A [BringIntoViewRequester] that can be used to request a specific region of text be brought
     * into view (via [TextLayoutState.bringCursorIntoView]).
     *
     * This requester should only be applied to the core text field node, _inside_ the internal
     * scroll container.
     */
    val bringIntoViewRequester = BringIntoViewRequester()

    /**
     * Updates the [TextFieldLayoutStateCache] with inputs that don't come from the measure phase.
     * This method will initialize the cache the first time it's called.
     * If the new inputs require re-calculating text layout, any readers of [layoutResult] called
     * from a snapshot observer will be invalidated.
     *
     * @see layoutWithNewMeasureInputs
     */
    fun updateNonMeasureInputs(
        textFieldState: TransformedTextFieldState,
        textStyle: TextStyle,
        singleLine: Boolean,
        softWrap: Boolean,
    ) {
        layoutCache.updateNonMeasureInputs(
            textFieldState = textFieldState,
            textStyle = textStyle,
            singleLine = singleLine,
            softWrap = softWrap,
        )
    }

    /**
     * Updates the [TextFieldLayoutStateCache] with inputs that come from the measure phase and returns the
     * latest [TextLayoutResult]. If the measure inputs haven't changed significantly since the
     * last call, this will be the cached result. If the new inputs require re-calculating text
     * layout, any readers of [layoutResult] called from a snapshot observer will be invalidated.
     *
     * [updateNonMeasureInputs] must be called before this method to initialize the cache.
     */
    fun layoutWithNewMeasureInputs(
        density: Density,
        layoutDirection: LayoutDirection,
        fontFamilyResolver: FontFamily.Resolver,
        constraints: Constraints,
    ): TextLayoutResult {
        val layoutResult = layoutCache.layoutWithNewMeasureInputs(
            density = density,
            layoutDirection = layoutDirection,
            fontFamilyResolver = fontFamilyResolver,
            constraints = constraints,
        )

        onTextLayout?.let { onTextLayout ->
            val textLayoutProvider = { layoutCache.value }
            onTextLayout(density, textLayoutProvider)
        }

        return layoutResult
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
        val coercedPosition = if (coerceInVisibleBounds) {
            position.coercedInVisibleBoundsOfInputText()
        } else {
            position
        }
        val relativePosition = fromDecorationToTextLayout(coercedPosition)
        return layoutResult.getOffsetForPosition(relativePosition)
    }

    /**
     * Returns true if the screen coordinates position (x,y) corresponds to a character displayed
     * in the view. Returns false when the position is in the empty space of left/right of text.
     * This function may return true even when [offset] is below or above the text layout.
     */
    fun isPositionOnText(offset: Offset): Boolean {
        val layoutResult = layoutResult ?: return false
        val relativeOffset = fromDecorationToTextLayout(offset.coercedInVisibleBoundsOfInputText())
        val line = layoutResult.getLineForVerticalPosition(relativeOffset.y)
        return relativeOffset.x >= layoutResult.getLineLeft(line) &&
            relativeOffset.x <= layoutResult.getLineRight(line)
    }

    /**
     * If click on the decoration box happens outside visible inner text field, coerce the click
     * position to the visible edges of the inner text field.
     */
    private fun Offset.coercedInVisibleBoundsOfInputText(): Offset {
        // If offset is outside visible bounds of the inner text field, use visible bounds edges
        val visibleTextLayoutNodeRect =
            textLayoutNodeCoordinates?.let { textLayoutNodeCoordinates ->
                if (textLayoutNodeCoordinates.isAttached) {
                    decoratorNodeCoordinates?.localBoundingBoxOf(textLayoutNodeCoordinates)
                } else {
                    Rect.Zero
                }
            } ?: Rect.Zero
        return this.coerceIn(visibleTextLayoutNodeRect)
    }
}

internal fun Offset.coerceIn(rect: Rect): Offset {
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

/**
 * Translates a position from text layout node coordinates to core node coordinates.
 */
internal fun TextLayoutState.fromTextLayoutToCore(offset: Offset): Offset {
    return textLayoutNodeCoordinates?.takeIf { it.isAttached }?.let { textLayoutNodeCoordinates ->
        coreNodeCoordinates?.takeIf { it.isAttached }?.let { coreNodeCoordinates ->
            coreNodeCoordinates.localPositionOf(textLayoutNodeCoordinates, offset)
        }
    } ?: offset
}

/**
 * Translates the click happened on the decorator node to the position in the text layout node
 * coordinates. This relative position is then used to determine symbol position in text using
 * TextLayoutResult object.
 */
internal fun TextLayoutState.fromDecorationToTextLayout(offset: Offset): Offset {
    return textLayoutNodeCoordinates?.let { textLayoutNodeCoordinates ->
        decoratorNodeCoordinates?.let { decoratorNodeCoordinates ->
            if (textLayoutNodeCoordinates.isAttached && decoratorNodeCoordinates.isAttached) {
                textLayoutNodeCoordinates.localPositionOf(decoratorNodeCoordinates, offset)
            } else {
                offset
            }
        }
    } ?: offset
}

internal fun TextLayoutState.fromWindowToDecoration(offset: Offset): Offset {
    return decoratorNodeCoordinates?.let { decoratorNodeCoordinates ->
        if (decoratorNodeCoordinates.isAttached) {
            decoratorNodeCoordinates.windowToLocal(offset)
        } else {
            offset
        }
    } ?: offset
}

/**
 * Asks [TextLayoutState.bringIntoViewRequester] to bring the bounds of the cursor at [cursorIndex]
 * into view.
 */
@OptIn(ExperimentalFoundationApi::class)
internal suspend fun TextLayoutState.bringCursorIntoView(cursorIndex: Int) {
    val layoutResult = layoutResult ?: return
    val rect = layoutResult.getCursorRect(cursorIndex)
    bringIntoViewRequester.bringIntoView(rect)
}
