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

package androidx.compose.ui.text.input

import android.view.inputmethod.CursorAnchorInfo
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.input.pointer.PositionCalculator
import androidx.compose.ui.text.TextLayoutResult

@Deprecated(
    "Only exists to support the legacy TextInputService APIs. It is not used by any Compose " +
        "code. A copy of this class in foundation is used by the legacy BasicTextField."
)
internal class CursorAnchorInfoController(
    private val rootPositionCalculator: PositionCalculator,
    @Suppress("DEPRECATION")
    private val inputMethodManager: InputMethodManager
) {
    private var monitorEnabled = false
    private var hasPendingImmediateRequest = false

    private var includeInsertionMarker = false
    private var includeCharacterBounds = false
    private var includeEditorBounds = false
    private var includeLineBounds = false

    private var textFieldValue: TextFieldValue? = null
    private var textLayoutResult: TextLayoutResult? = null
    private var offsetMapping: OffsetMapping? = null
    private var textFieldToRootTransform: (Matrix) -> Unit = { }
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
    ) {
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
     * @param textFieldToRootTransform function that modifies a matrix to be a transformation matrix
     *   from local coordinates to the root composable coordinates
     * @param innerTextFieldBounds visible bounds of the text field in local coordinates, or an
     *   empty rectangle if the text field is not visible
     * @param decorationBoxBounds visible bounds of the decoration box in local coordinates, or an
     *   empty rectangle if the decoration box is not visible
     */
    fun updateTextLayoutResult(
        textFieldValue: TextFieldValue,
        offsetMapping: OffsetMapping,
        textLayoutResult: TextLayoutResult,
        textFieldToRootTransform: (Matrix) -> Unit,
        innerTextFieldBounds: Rect,
        decorationBoxBounds: Rect
    ) {
        this.textFieldValue = textFieldValue
        this.offsetMapping = offsetMapping
        this.textLayoutResult = textLayoutResult
        this.textFieldToRootTransform = textFieldToRootTransform
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
    fun invalidate() {
        textFieldValue = null
        offsetMapping = null
        textLayoutResult = null
        textFieldToRootTransform = { }
        innerTextFieldBounds = null
        decorationBoxBounds = null
    }

    private fun updateCursorAnchorInfo() {
        if (!inputMethodManager.isActive()) return

        // Sets matrix to transform text field local coordinates to the root composable coordinates.
        textFieldToRootTransform(matrix)
        // Updates matrix to transform text field local coordinates to screen coordinates.
        rootPositionCalculator.localToScreen(matrix)
        androidMatrix.setFrom(matrix)

        @Suppress("DEPRECATION")
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
