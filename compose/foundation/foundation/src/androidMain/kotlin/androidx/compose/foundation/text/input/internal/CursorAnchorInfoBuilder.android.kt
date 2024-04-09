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

package androidx.compose.foundation.text.input.internal

import android.graphics.Matrix
import android.os.Build
import android.view.inputmethod.CursorAnchorInfo
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection

/**
 * Helper function to build
 * [CursorAnchorInfo](https://developer.android.com/reference/android/view/inputmethod/CursorAnchorInfo).
 *
 * @param matrix matrix that transforms local coordinates into screen coordinates
 * @param innerTextFieldBounds visible bounds of the text field in local coordinates, or an empty
 *   rectangle if the text field is not visible
 * @param decorationBoxBounds visible bounds of the decoration box in local coordinates, or an empty
 *   rectangle if the decoration box is not visible
 */
internal fun CursorAnchorInfo.Builder.build(
    text: CharSequence,
    selection: TextRange,
    composition: TextRange?,
    textLayoutResult: TextLayoutResult,
    matrix: Matrix,
    innerTextFieldBounds: Rect,
    decorationBoxBounds: Rect,
    includeInsertionMarker: Boolean = true,
    includeCharacterBounds: Boolean = true,
    includeEditorBounds: Boolean = true,
    includeLineBounds: Boolean = true
): CursorAnchorInfo {
    reset()

    setMatrix(matrix)

    val selectionStart = selection.min
    val selectionEnd = selection.max
    setSelectionRange(selectionStart, selectionEnd)

    if (includeInsertionMarker) {
        setInsertionMarker(selectionStart, textLayoutResult, innerTextFieldBounds)
    }

    if (includeCharacterBounds) {
        val compositionStart = composition?.min ?: -1
        val compositionEnd = composition?.max ?: -1

        if (compositionStart in 0 until compositionEnd) {
            setComposingText(
                compositionStart,
                text.subSequence(compositionStart, compositionEnd)
            )
            addCharacterBounds(
                compositionStart,
                compositionEnd,
                textLayoutResult,
                innerTextFieldBounds
            )
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && includeEditorBounds) {
        CursorAnchorInfoApi33Helper.setEditorBoundsInfo(this, decorationBoxBounds)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && includeLineBounds) {
        CursorAnchorInfoApi34Helper.addVisibleLineBounds(
            this,
            textLayoutResult,
            innerTextFieldBounds
        )
    }

    return build()
}

private fun CursorAnchorInfo.Builder.setInsertionMarker(
    selectionStart: Int,
    textLayoutResult: TextLayoutResult,
    innerTextFieldBounds: Rect
): CursorAnchorInfo.Builder {
    if (selectionStart < 0) return this

    val cursorRect = textLayoutResult.getCursorRect(selectionStart)
    val x = cursorRect.left.coerceIn(0f, textLayoutResult.size.width.toFloat())
    val isTopVisible = innerTextFieldBounds.containsInclusive(x, cursorRect.top)
    val isBottomVisible = innerTextFieldBounds.containsInclusive(x, cursorRect.bottom)
    val isRtl = textLayoutResult.getBidiRunDirection(selectionStart) == ResolvedTextDirection.Rtl

    var flags = 0
    if (isTopVisible || isBottomVisible) flags = flags or CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
    if (!isTopVisible || !isBottomVisible)
        flags = flags or CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION
    if (isRtl) flags = flags or CursorAnchorInfo.FLAG_IS_RTL

    // Sets the location of the text insertion point (zero width cursor) as a rectangle in local
    // coordinates.
    setInsertionMarkerLocation(x, cursorRect.top, cursorRect.bottom, cursorRect.bottom, flags)

    return this
}

private fun CursorAnchorInfo.Builder.addCharacterBounds(
    startOffset: Int,
    endOffset: Int,
    textLayoutResult: TextLayoutResult,
    innerTextFieldBounds: Rect
): CursorAnchorInfo.Builder {
    val array = FloatArray((endOffset - startOffset) * 4)
    textLayoutResult.multiParagraph.fillBoundingBoxes(
        TextRange(
            startOffset,
            endOffset
        ), array, 0
    )

    for (offset in startOffset until endOffset) {
        val arrayIndex = 4 * (offset - startOffset)
        val rect =
            Rect(
                array[arrayIndex] /* left */,
                array[arrayIndex + 1] /* top */,
                array[arrayIndex + 2] /* right */,
                array[arrayIndex + 3] /* bottom */
            )

        var flags = 0
        if (innerTextFieldBounds.overlaps(rect)) {
            flags = flags or CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
        }
        if (
            !innerTextFieldBounds.containsInclusive(rect.left, rect.top) ||
            !innerTextFieldBounds.containsInclusive(rect.right, rect.bottom)
        ) {
            flags = flags or CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION
        }
        if (textLayoutResult.getBidiRunDirection(offset) == ResolvedTextDirection.Rtl) {
            flags = flags or CursorAnchorInfo.FLAG_IS_RTL
        }

        addCharacterBounds(offset, rect.left, rect.top, rect.right, rect.bottom, flags)
    }
    return this
}
