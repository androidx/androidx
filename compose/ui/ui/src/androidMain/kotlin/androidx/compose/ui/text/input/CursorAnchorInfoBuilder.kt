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

package androidx.compose.ui.text.input

import android.graphics.Matrix
import android.os.Build
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorBoundsInfo
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection

/**
 * Helper function to build
 * [CursorAnchorInfo](https://developer.android.com/reference/android/view/inputmethod/CursorAnchorInfo).
 *
 * @param textFieldValue the text field's [TextFieldValue]
 * @param textLayoutResult the text field's [TextLayoutResult]
 * @param matrix matrix that transforms local coordinates into screen coordinates
 * @param innerTextFieldBounds visible bounds of the text field in local coordinates, or an empty
 *   rectangle if the text field is not visible
 * @param decorationBoxBounds visible bounds of the decoration box in local coordinates, or an empty
 *   rectangle if the decoration box is not visible
 * @param includeInsertionMarker whether to include insertion marker info in the CursorAnchorInfo
 * @param includeCharacterBounds whether to include character bounds info in the CursorAnchorInfo
 * @param includeEditorBounds whether to include editor bounds info in the CursorAnchorInfo
 */
internal fun CursorAnchorInfo.Builder.build(
    textFieldValue: TextFieldValue,
    textLayoutResult: TextLayoutResult,
    matrix: Matrix,
    innerTextFieldBounds: Rect,
    decorationBoxBounds: Rect,
    includeInsertionMarker: Boolean = true,
    includeCharacterBounds: Boolean = true,
    includeEditorBounds: Boolean = true
): CursorAnchorInfo {
    reset()

    setMatrix(matrix)

    val selectionStart = textFieldValue.selection.min
    val selectionEnd = textFieldValue.selection.max
    setSelectionRange(selectionStart, selectionEnd)

    if (includeInsertionMarker) {
        setInsertionMarker(selectionStart, textLayoutResult, innerTextFieldBounds)
    }

    if (includeCharacterBounds) {
        val compositionStart = textFieldValue.composition?.min ?: -1
        val compositionEnd = textFieldValue.composition?.max ?: -1

        if (compositionStart in 0 until compositionEnd) {
            setComposingText(
                compositionStart,
                textFieldValue.text.subSequence(compositionStart, compositionEnd)
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

    return build()
}

private fun CursorAnchorInfo.Builder.setInsertionMarker(
    selectionStart: Int,
    textLayoutResult: TextLayoutResult,
    innerTextFieldBounds: Rect
): CursorAnchorInfo.Builder {
    if (selectionStart < 0) return this

    val cursorRect = textLayoutResult.getCursorRect(selectionStart)
    val isTopVisible = innerTextFieldBounds.containsInclusive(cursorRect.topLeft)
    val isBottomVisible = innerTextFieldBounds.containsInclusive(cursorRect.bottomLeft)
    val isRtl = textLayoutResult.getBidiRunDirection(selectionStart) == ResolvedTextDirection.Rtl

    var flags = 0
    if (isTopVisible || isBottomVisible) flags = flags or CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
    if (!isTopVisible || !isBottomVisible)
        flags = flags or CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION
    if (isRtl) flags = flags or CursorAnchorInfo.FLAG_IS_RTL

    // Sets the location of the text insertion point (zero width cursor) as a rectangle in local
    // coordinates.
    setInsertionMarkerLocation(
        cursorRect.left,
        cursorRect.top,
        cursorRect.bottom,
        cursorRect.bottom,
        flags
    )

    return this
}

private fun CursorAnchorInfo.Builder.addCharacterBounds(
    startOffset: Int,
    endOffset: Int,
    textLayoutResult: TextLayoutResult,
    innerTextFieldBounds: Rect
): CursorAnchorInfo.Builder {
    val array = FloatArray((endOffset - startOffset) * 4)
    textLayoutResult.multiParagraph.fillBoundingBoxes(TextRange(startOffset, endOffset), array, 0)

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
            !innerTextFieldBounds.containsInclusive(rect.topLeft) ||
                !innerTextFieldBounds.containsInclusive(rect.bottomRight)
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

@RequiresApi(33)
private object CursorAnchorInfoApi33Helper {
    @JvmStatic
    @DoNotInline
    fun setEditorBoundsInfo(
        builder: CursorAnchorInfo.Builder,
        decorationBoxBounds: Rect
    ): CursorAnchorInfo.Builder =
        builder.setEditorBoundsInfo(
            EditorBoundsInfo.Builder()
                .setEditorBounds(decorationBoxBounds.toAndroidRectF())
                .setHandwritingBounds(decorationBoxBounds.toAndroidRectF())
                .build()
        )
}

/**
 * Whether the point specified by the given offset lies inside or on an edge of this rectangle.
 *
 * Note this differs from [Rect.contains] which returns false for points on the bottom or right
 * edges.
 */
private fun Rect.containsInclusive(offset: Offset): Boolean {
    return offset.x in left..right && offset.y in top..bottom
}
