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

package androidx.compose.ui.platform

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.window.PlatformTextLayoutDirection

internal interface IOSSkikoInput {

    fun onResignFocus()

    fun beginFloatingCursor(offset: DpOffset)

    fun updateFloatingCursor(offset: DpOffset)

    fun endFloatingCursor()

    /**
     * Delays all edit commands until [endEditBatch] is being called.
     */
    fun beginEditBatch()

    /**
     * Performs all editing commands, starting from the [beginEditBatch] call.
     */
    fun endEditBatch()

    /**
     * A Boolean value that indicates whether the text-entry object has any text.
     * https://developer.apple.com/documentation/uikit/uikeyinput/1614457-hastext
     */
    fun hasText(): Boolean

    /**
     * Inserts a character into the displayed text.
     * Add the character text to your class’s backing store at the index corresponding to the cursor and redisplay the text.
     * https://developer.apple.com/documentation/uikit/uikeyinput/1614543-inserttext
     * @param text A string object representing the character typed on the system keyboard.
     */
    fun insertText(text: String)

    /**
     * Deletes a character from the displayed text.
     * Remove the character just before the cursor from your class’s backing store and redisplay the text.
     * https://developer.apple.com/documentation/uikit/uikeyinput/1614572-deletebackward
     */
    fun deleteBackward()

    /**
     * The text position for the end of a document.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614555-endofdocument
     */
    fun endOfDocument(): Int

    /**
     * The range of selected text in a document.
     * If the text range has a length, it indicates the currently selected text.
     * If it has zero length, it indicates the caret (insertion point).
     * If the text-range object is nil, it indicates that there is no current selection.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614541-selectedtextrange
     */
    fun getSelectedTextRange(): TextRange?

    fun setSelectedTextRange(range: TextRange?)

    fun selectAll()

    /**
     * Returns the text in the specified range.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614527-text
     * @param range A range of text in a document.
     * @return A substring of a document that falls within the specified range.
     */
    fun textInRange(range: TextRange): String?

    /**
     * Replaces the text in a document that is in the specified range.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614558-replace
     * @param range A range of text in a document.
     * @param text A string to replace the text in range.
     */
    fun replaceRange(range: TextRange, text: String)

    /**
     * Inserts the provided text and marks it to indicate that it is part of an active input session.
     * Setting marked text either replaces the existing marked text or,
     * if none is present, inserts it in place of the current selection.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614465-setmarkedtext
     * @param markedText The text to be marked.
     * @param selectedRange A range within markedText that indicates the current selection.
     * This range is always relative to markedText.
     */
    fun setMarkedText(markedText: String?, selectedRange: TextRange)

    /**
     * The range of currently marked text in a document.
     * If there is no marked text, the value of the property is nil.
     * Marked text is provisionally inserted text that requires user confirmation;
     * it occurs in multistage text input.
     * The current selection, which can be a caret or an extended range, always occurs within the marked text.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614489-markedtextrange
     */
    fun markedTextRange(): TextRange?

    /**
     * Unmarks the currently marked text.
     * After this method is called, the value of markedTextRange is nil.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614512-unmarktext
     */
    fun unmarkText()

    /**
     * Returns the text position at a specified offset from another text position.
     * Returned value must be in range between 0 and length of text (inclusive).
     */
    fun positionFromPosition(position: Int, offset: Int): Int?

    /**
     * Returns the text position at a specified offset from another text position.
     * Returned value must be in range between 0 and length of the text (inclusive).
     */
    fun verticalPositionFromPosition(position: Int, verticalOffset: Int): Int?

    /**
     * Returns the caret rectangle for a given text position.
     * https://developer.apple.com/documentation/uikit/uitextinput/caretrect(for:)
     * @param position A text position within the document.
     * @return A rectangle, in dp, that encloses the caret at the specified position, or `null`
     * if the position is invalid.
     */
    fun caretDpRectForPosition(position: Int): DpRect?

    /**
     * Returns the selection rectangles that enclose a range of text.
     * https://developer.apple.com/documentation/uikit/uitextinput/selectionrects(for:)
     * @param range A range of text in the document.
     * @return A list of rectangles, in dp, that tightly bound the visual selection for the range.
     */
    fun selectionDpRectsForRange(range: TextRange): List<TextSelectionRect>

    /**
     * Returns the first rectangle that encloses a range of text.
     * https://developer.apple.com/documentation/uikit/uitextinput/firstrect(for:)
     * @param range A range of text in the document.
     * @return The first selection rectangle, in dp, or `null` if the range is invalid or empty.
     */
    fun firstSelectionRectForRange(range: TextRange): DpRect?

    /**
     * Returns the text position that is closest to the specified point.
     * https://developer.apple.com/documentation/uikit/uitextinput/closestposition(to:)
     * @param point A point, in dp, in the coordinate space of the text input.
     * @return The position closest to the point, or `null` if none can be determined.
     */
    fun closestPositionToPoint(point: DpOffset): Int?

    /**
     * Returns the text position that is closest to the specified point within range.
     * https://developer.apple.com/documentation/uikit/uitextinput/closestposition(to:within:)
     * @param point A point, in dp, in the coordinate space of the text input.
     * @param withinRange A range that limits the returned position.
     * @return The closest position within the given range, or `null` if none exists.
     */
    fun closestPositionToPoint(point: DpOffset, withinRange: TextRange): Int?

    /**
     * Returns the character range at the specified dp point.
     * https://developer.apple.com/documentation/uikit/uitextinput/characterrange(at:)
     * @param point A point, in dp, in the coordinate space of the text input.
     * @return The range of the character at the point, or `null` if none.
     */
    fun characterRangeAtPoint(point: DpOffset): TextRange?

    /**
     * Returns the position in a specified direction that is farthest within a given range.
     * https://developer.apple.com/documentation/uikit/uitextinput/position(within:farthestin:)
     * @param range The limiting range.
     * @param farthestInDirection A direction constant (left, right, up, or down).
     * @return The farthest position within the range in the given direction, or `null` if none.
     */
    fun positionWithinRange(range: TextRange, farthestInDirection: PlatformTextLayoutDirection): Int?
}