/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.text.selection

import androidx.ui.core.gesture.LongPressDragObserver
import androidx.ui.geometry.Offset
import androidx.ui.input.OffsetMap
import androidx.ui.input.TextFieldValue
import androidx.ui.text.TextFieldState
import kotlin.math.max

/**
 * A bridge class between user interaction to the text field selection.
 */
internal class TextFieldSelectionManager() {

    /**
     * The current [OffsetMap] for text field.
     */
    internal var offsetMap: OffsetMap = OffsetMap.identityOffsetMap

    /**
     * Called when the input service updates the values in [TextFieldValue].
     */
    internal var onValueChange: (TextFieldValue) -> Unit = {}

    /**
     * The current [TextFieldState].
     */
    internal var state: TextFieldState? = null

    /**
     * The current [TextFieldValue].
     */
    internal var value: TextFieldValue = TextFieldValue()

    /**
     * The beginning position of the drag gesture. Every time a new drag gesture starts, it wil be
     * recalculated.
     */
    private var dragBeginPosition = Offset.Zero

    /**
     * The total distance being dragged of the drag gesture. Every time a new drag gesture starts,
     * it will be zeroed out.
     */
    private var dragTotalDistance = Offset.Zero

    /**
     * [LongPressDragObserver] for long press and drag to select in TextField.
     */
    internal val longPressDragObserver = object : LongPressDragObserver {
        override fun onLongPress(pxPosition: Offset) {
            // selection never started
            if (value.text == "") return
            state?.selectionIsOn = true
            state?.layoutResult?.let { layoutResult ->
                val offset = offsetMap.transformedToOriginal(
                    layoutResult.getOffsetForPosition(pxPosition)
                )
                updateSelection(
                    value = value,
                    startOffset = offset,
                    endOffset = offset,
                    isStartHandle = true,
                    wordBasedSelection = true
                )
            }
            dragBeginPosition = pxPosition
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(dragDistance: Offset): Offset {
            // selection never started, did not consume any drag
            if (value.text == "") return Offset.Zero

            dragTotalDistance += dragDistance
            state?.layoutResult?.let { layoutResult ->
                val startOffset = layoutResult.getOffsetForPosition(dragBeginPosition)
                val endOffset =
                    layoutResult.getOffsetForPosition(dragBeginPosition + dragTotalDistance)
                updateSelection(
                    value = value,
                    startOffset = startOffset,
                    endOffset = endOffset,
                    isStartHandle = true,
                    wordBasedSelection = true
                )
            }
            return dragDistance
        }
    }

    private fun updateSelection(
        value: TextFieldValue,
        startOffset: Int,
        endOffset: Int,
        isStartHandle: Boolean,
        wordBasedSelection: Boolean
    ) {
        val range = getTextFieldSelection(
            textLayoutResult = state?.layoutResult,
            rawStartOffset = startOffset,
            rawEndOffset = endOffset,
            previousSelection = if (value.selection.collapsed) null else value.selection,
            previousHandlesCrossed = value.selection.reversed,
            isStartHandle = isStartHandle,
            wordBasedSelection = wordBasedSelection
        )

        if (range == value.selection) return

        val newValue = TextFieldValue(
            text = value.text,
            selection = range
        )
        onValueChange(newValue)
        state?.let {
            it.selectionStartDirection = it.layoutResult!!.getBidiRunDirection(range.start)
            it.selectionEndDirection = it.layoutResult!!.getBidiRunDirection(max(range.end - 1, 0))
        }
    }
}
