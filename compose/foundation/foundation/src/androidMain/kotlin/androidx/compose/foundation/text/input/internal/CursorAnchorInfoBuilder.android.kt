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
import android.view.inputmethod.EditorBoundsInfo
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.ResolvedTextDirection

/**
 * Helper function to build
 * [CursorAnchorInfo](https://developer.android.com/reference/android/view/inputmethod/CursorAnchorInfo).
 *
 * @param textFieldValue the text field's [TextFieldValue]
 * @param offsetMapping the offset mapping for the text field's visual transformation
 * @param textLayoutResult the text field's [TextLayoutResult]
 * @param matrix matrix that transforms local coordinates into screen coordinates
 * @param innerTextFieldBounds visible bounds of the text field in local coordinates, or an empty
 *   rectangle if the text field is not visible
 * @param decorationBoxBounds visible bounds of the decoration box in local coordinates, or an empty
 *   rectangle if the decoration box is not visible
 * @param includeInsertionMarker whether to include insertion marker info in the CursorAnchorInfo
 * @param includeCharacterBounds whether to include character bounds info in the CursorAnchorInfo
 * @param includeEditorBounds whether to include editor bounds info in the CursorAnchorInfo
 * @param includeLineBounds whether to include line bounds info in the CursorAnchorInfo
 */
internal fun CursorAnchorInfo.Builder.build(
    textFieldValue: TextFieldValue,
    offsetMapping: OffsetMapping,
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

    val selectionStart = textFieldValue.selection.min
    val selectionEnd = textFieldValue.selection.max
    setSelectionRange(selectionStart, selectionEnd)

    if (includeInsertionMarker) {
        setInsertionMarker(selectionStart, offsetMapping, textLayoutResult, innerTextFieldBounds)
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
                offsetMapping,
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
    offsetMapping: OffsetMapping,
    textLayoutResult: TextLayoutResult,
    innerTextFieldBounds: Rect
): CursorAnchorInfo.Builder {
    if (selectionStart < 0) return this

    val selectionStartTransformed = offsetMapping.originalToTransformed(selectionStart)
    val cursorRect = textLayoutResult.getCursorRect(selectionStartTransformed)
    val x = cursorRect.left.coerceIn(0f, textLayoutResult.size.width.toFloat())
    val isTopVisible = innerTextFieldBounds.containsInclusive(x, cursorRect.top)
    val isBottomVisible = innerTextFieldBounds.containsInclusive(x, cursorRect.bottom)
    val isRtl =
        textLayoutResult.getBidiRunDirection(selectionStartTransformed) == ResolvedTextDirection.Rtl

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
    offsetMapping: OffsetMapping,
    textLayoutResult: TextLayoutResult,
    innerTextFieldBounds: Rect
): CursorAnchorInfo.Builder {
    val startOffsetTransformed = offsetMapping.originalToTransformed(startOffset)
    val endOffsetTransformed = offsetMapping.originalToTransformed(endOffset)
    val array = FloatArray((endOffsetTransformed - startOffsetTransformed) * 4)
    textLayoutResult.multiParagraph.fillBoundingBoxes(
        TextRange(
            startOffsetTransformed,
            endOffsetTransformed
        ), array, 0
    )

    for (offset in startOffset until endOffset) {
        // It's possible for a visual transformation to hide some characters. If the character at
        // the offset is hidden, then offsetTransformed points to the last preceding character that
        // is not hidden. Since the CursorAnchorInfo API doesn't define what to return in this case,
        // and visual transformations hiding characters should be rare, returning the bounds for the
        // last preceding character is the simplest behavior.
        val offsetTransformed = offsetMapping.originalToTransformed(offset)
        val arrayIndex = 4 * (offsetTransformed - startOffsetTransformed)
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
        if (textLayoutResult.getBidiRunDirection(offsetTransformed) == ResolvedTextDirection.Rtl) {
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

@RequiresApi(34)
private object CursorAnchorInfoApi34Helper {
    @JvmStatic
    @DoNotInline
    fun addVisibleLineBounds(
        builder: CursorAnchorInfo.Builder,
        textLayoutResult: TextLayoutResult,
        innerTextFieldBounds: Rect
    ): CursorAnchorInfo.Builder {
        if (!innerTextFieldBounds.isEmpty) {
            val firstLine = textLayoutResult.getLineForVerticalPosition(innerTextFieldBounds.top)
            val lastLine = textLayoutResult.getLineForVerticalPosition(innerTextFieldBounds.bottom)
            for (index in firstLine..lastLine) {
                builder.addVisibleLineBounds(
                    textLayoutResult.getLineLeft(index),
                    textLayoutResult.getLineTop(index),
                    textLayoutResult.getLineRight(index),
                    textLayoutResult.getLineBottom(index)
                )
            }
        }
        return builder
    }
}

/**
 * Whether the point specified by the given offset lies inside or on an edge of this rectangle.
 *
 * Note this differs from [Rect.contains] which returns false for points on the bottom or right
 * edges.
 */
private fun Rect.containsInclusive(x: Float, y: Float): Boolean {
    return x in left..right && y in top..bottom
}
