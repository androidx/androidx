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

import android.view.inputmethod.CursorAnchorInfo
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue

internal class LegacyCursorAnchorInfoController(
    private val localToScreen: (Matrix) -> Unit,
    private val inputMethodManager: InputMethodManager
) {
    private val lock = Any()

    private var monitorEnabled = false
    private var hasPendingImmediateRequest = false

    private var includeInsertionMarker = false
    private var includeCharacterBounds = false
    private var includeEditorBounds = false
    private var includeLineBounds = false

    private var textFieldValue: TextFieldValue? = null
    private var textLayoutResult: TextLayoutResult? = null
    private var offsetMapping: OffsetMapping? = null
    private var innerTextFieldBounds: Rect? = null
    private var decorationBoxBounds: Rect? = null

    private val builder = CursorAnchorInfo.Builder()
    private val matrix = Matrix()
    private val androidMatrix = android.graphics.Matrix()

    /**
     * Requests [CursorAnchorInfo] updates to be provided to the [InputMethodManager].
     *
     * Combinations of [immediate] and [monitor] are used to specify when to provide updates. If
     * these are both false, then no further updates will be provided.
     *
     * @param immediate whether to update with the current [CursorAnchorInfo] immediately, or as
     *   soon as available
     * @param monitor whether to provide [CursorAnchorInfo] updates for all future layout or
     *   position changes
     * @param includeInsertionMarker whether to include insertion marker (i.e. cursor) location
     *   information
     * @param includeCharacterBounds whether to include character bounds information for the
     *   composition range
     * @param includeEditorBounds whether to include editor bounds information
     * @param includeLineBounds whether to include line bounds information
     */
    fun requestUpdate(
        immediate: Boolean,
        monitor: Boolean,
        includeInsertionMarker: Boolean,
        includeCharacterBounds: Boolean,
        includeEditorBounds: Boolean,
        includeLineBounds: Boolean
    ) =
        synchronized(lock) {
            this.includeInsertionMarker = includeInsertionMarker
            this.includeCharacterBounds = includeCharacterBounds
            this.includeEditorBounds = includeEditorBounds
            this.includeLineBounds = includeLineBounds

            if (immediate) {
                hasPendingImmediateRequest = true
                if (textFieldValue != null) {
                    updateCursorAnchorInfo()
                }
            }
            monitorEnabled = monitor
        }

    /**
     * Notify the controller of layout and position changes.
     *
     * @param textFieldValue the text field's [TextFieldValue]
     * @param offsetMapping the offset mapping for the visual transformation
     * @param textLayoutResult the text field's [TextLayoutResult]
     * @param innerTextFieldBounds visible bounds of the text field in text layout coordinates, an
     *   empty rectangle if the text field is not visible
     * @param decorationBoxBounds visible bounds of the decoration box in text layout coordinates,
     *   or an empty rectangle if the decoration box is not visible
     */
    fun updateTextLayoutResult(
        textFieldValue: TextFieldValue,
        offsetMapping: OffsetMapping,
        textLayoutResult: TextLayoutResult,
        innerTextFieldBounds: Rect,
        decorationBoxBounds: Rect
    ) =
        synchronized(lock) {
            this.textFieldValue = textFieldValue
            this.offsetMapping = offsetMapping
            this.textLayoutResult = textLayoutResult
            this.innerTextFieldBounds = innerTextFieldBounds
            this.decorationBoxBounds = decorationBoxBounds

            if (hasPendingImmediateRequest || monitorEnabled) {
                updateCursorAnchorInfo()
            }
        }

    /**
     * Invalidate the last received layout and position data.
     *
     * This should be called when the [TextFieldValue] has changed, so the last received layout and
     * position data is no longer valid. [CursorAnchorInfo] updates will not be sent until new
     * layout and position data is received.
     */
    fun invalidate() =
        synchronized(lock) {
            textFieldValue = null
            offsetMapping = null
            textLayoutResult = null
            innerTextFieldBounds = null
            decorationBoxBounds = null
        }

    private fun updateCursorAnchorInfo() {
        if (
            !inputMethodManager.isActive() ||
                textFieldValue == null ||
                offsetMapping == null ||
                textLayoutResult == null ||
                innerTextFieldBounds == null ||
                decorationBoxBounds == null
        )
            return

        matrix.reset()
        // Updates matrix to transform decoration box coordinates to screen coordinates.
        localToScreen(matrix)
        // Updates matrix to transform text layout coordinates to screen coordinates.
        matrix.translate(-decorationBoxBounds!!.left, -decorationBoxBounds!!.top, 0f)
        androidMatrix.setFrom(matrix)

        inputMethodManager.updateCursorAnchorInfo(
            builder.build(
                textFieldValue!!,
                offsetMapping!!,
                textLayoutResult!!,
                androidMatrix,
                innerTextFieldBounds!!,
                decorationBoxBounds!!,
                includeInsertionMarker,
                includeCharacterBounds,
                includeEditorBounds,
                includeLineBounds
            )
        )

        hasPendingImmediateRequest = false
    }
}
