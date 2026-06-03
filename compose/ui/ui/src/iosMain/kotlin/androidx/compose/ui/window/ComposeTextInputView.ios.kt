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

package androidx.compose.ui.window

import androidx.compose.ui.platform.EmptyInputTraits
import androidx.compose.ui.platform.TextInputPosition
import androidx.compose.ui.platform.TextInputRange
import androidx.compose.ui.platform.TextInputStringTokenizer
import androidx.compose.ui.platform.SkikoUITextInputTraits
import androidx.compose.ui.platform.TextEditingDelegate
import androidx.compose.ui.platform.selectTextNearCursor
import androidx.compose.ui.platform.toTextRange
import androidx.compose.ui.platform.toUITextRange
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.uikit.utils.CMPEditMenuView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectNull
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSComparisonResult
import platform.Foundation.NSDictionary
import platform.Foundation.NSOrderedAscending
import platform.Foundation.NSOrderedDescending
import platform.Foundation.NSOrderedSame
import platform.Foundation.NSRange
import platform.Foundation.dictionary
import platform.UIKit.NSWritingDirection
import platform.UIKit.NSWritingDirectionNatural
import platform.UIKit.UIKeyInputProtocol
import platform.UIKit.UIKeyboardAppearance
import platform.UIKit.UIKeyboardType
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextContentType
import platform.UIKit.UITextInputDelegateProtocol
import platform.UIKit.UITextInputProtocol
import platform.UIKit.UITextInputTokenizerProtocol
import platform.UIKit.UITextLayoutDirection
import platform.UIKit.UITextLayoutDirectionDown
import platform.UIKit.UITextLayoutDirectionLeft
import platform.UIKit.UITextLayoutDirectionRight
import platform.UIKit.UITextLayoutDirectionUp
import platform.UIKit.UITextPosition
import platform.UIKit.UITextRange
import platform.UIKit.UITextSelectionRect
import platform.UIKit.UITextStorageDirection
import platform.UIKit.UIView
import platform.UIKit.UIWritingToolsBehavior
import platform.darwin.NSInteger

/**
 * Hidden UIView to interact with iOS Keyboard and TextInput system.
 */
internal class ComposeTextInputView(
    private val doubleTapTimeoutMillis: Long,
) : CMPEditMenuView(frame = CGRectZero.readValue()),
    UIKeyInputProtocol, UITextInputProtocol {
    private var _inputDelegate: UITextInputDelegateProtocol? = null
    var input: TextEditingDelegate? = null
        set(value) {
            field = value
            if (value == null) {
                hideTextMenu()
            }
        }

    private val inputTraits: SkikoUITextInputTraits
        get() = input?.inputTraits ?: EmptyInputTraits

    override fun inputView(): UIView? = inputTraits.inputView()
    override fun inputAccessoryView(): UIView? = inputTraits.inputAccessoryView()

    override fun canBecomeFirstResponder() = true

    override fun resignFirstResponder(): Boolean {
        input?.onResignFocus()
        hideTextMenu()
        return super.resignFirstResponder()
    }

    override fun beginFloatingCursorAtPoint(point: CValue<CGPoint>) {
        input?.beginFloatingCursor(point.useContents { DpOffset(x.dp, y.dp) })
    }

    override fun updateFloatingCursorAtPoint(point: CValue<CGPoint>) {
        input?.updateFloatingCursor(point.useContents { DpOffset(x.dp, y.dp) })
    }

    override fun endFloatingCursor() {
        input?.endFloatingCursor()
    }

    override fun showEditMenuAtRect(
        targetRect: CValue<CGRect>,
        copy: (() -> Unit)?,
        cut: (() -> Unit)?,
        paste: (() -> Unit)?,
        select: (() -> Unit)?,
        selectAll: (() -> Unit)?,
        customActions: List<*>?
    ) {
        val patchedSelect = select ?: {
            this.select()
        }.takeIf { selectAll != null && showSelectMenu }

        super.showEditMenuAtRect(
            targetRect = targetRect,
            copy = copy,
            cut = cut,
            paste = paste,
            select = patchedSelect,
            selectAll = selectAll,
            customActions = customActions,
        )
    }

    private val showSelectMenu: Boolean
        get() = input?.getSelectedTextRange()?.length == 0 && input?.hasText() == true

    private fun select() {
        selectionWillChange()
        input?.selectTextNearCursor()
        selectionDidChange()
    }

    /**
     * A Boolean value that indicates whether the text-entry object has any text.
     * https://developer.apple.com/documentation/uikit/uikeyinput/1614457-hastext
     */
    override fun hasText(): Boolean {
        return input?.hasText() ?: false
    }

    /**
     * Inserts a character into the displayed text.
     * Add the character text to your class’s backing store at the index corresponding to the cursor and redisplay the text.
     * https://developer.apple.com/documentation/uikit/uikeyinput/1614543-inserttext
     * @param text A string object representing the character typed on the system keyboard.
     */
    override fun insertText(text: String) {
        input?.insertText(text)
    }

    /**
     * Deletes a character from the displayed text.
     * Remove the character just before the cursor from your class’s backing store and redisplay the text.
     * https://developer.apple.com/documentation/uikit/uikeyinput/1614572-deletebackward
     */
    override fun deleteBackward() {
        input?.deleteBackward()
    }

    override fun inputDelegate(): UITextInputDelegateProtocol? {
        return _inputDelegate
    }

    override fun setInputDelegate(inputDelegate: UITextInputDelegateProtocol?) {
        _inputDelegate = inputDelegate
    }

    /**
     * Returns the text in the specified range.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614527-text
     * @param range A range of text in a document.
     * @return A substring of a document that falls within the specified range.
     */
    override fun textInRange(range: UITextRange): String? {
        val textRange = range.toTextRange() ?: return null
        return input?.textInRange(textRange)
    }

    /**
     * Replaces the text in a document that is in the specified range.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614558-replace
     * @param range A range of text in a document.
     * @param withText A string to replace the text in range.
     */
    override fun replaceRange(range: UITextRange, withText: String) {
        val textRange = range.toTextRange() ?: return
        input?.replaceRange(textRange, withText)
    }

    override fun setSelectedTextRange(selectedTextRange: UITextRange?) {
        val range = selectedTextRange?.toTextRange()
        input?.setSelectedTextRange(range)
    }

    /**
     * The range of selected text in a document.
     * If the text range has a length, it indicates the currently selected text.
     * If it has zero length, it indicates the caret (insertion point).
     * If the text-range object is nil, it indicates that there is no current selection.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614541-selectedtextrange
     */
    override fun selectedTextRange(): UITextRange? {
        return input?.getSelectedTextRange()?.toUITextRange()
    }

    /**
     * The range of currently marked text in a document.
     * If there is no marked text, the value of the property is nil.
     * Marked text is provisionally inserted text that requires user confirmation;
     * it occurs in multistage text input.
     * The current selection, which can be a caret or an extended range, always occurs within the marked text.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614489-markedtextrange
     */
    override fun markedTextRange(): UITextRange? {
        return input?.markedTextRange()?.toUITextRange()
    }

    override fun setMarkedTextStyle(markedTextStyle: Map<Any?, *>?) {
        // do nothing
    }

    override fun markedTextStyle(): Map<Any?, *>? {
        return null
    }

    /**
     * Inserts the provided text and marks it to indicate that it is part of an active input session.
     * Setting marked text either replaces the existing marked text or,
     * if none is present, inserts it in place of the current selection.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614465-setmarkedtext
     * @param markedText The text to be marked.
     * @param selectedRange A range within markedText that indicates the current selection.
     * This range is always relative to markedText.
     */
    override fun setMarkedText(markedText: String?, selectedRange: CValue<NSRange>) {
        val relativeTextRange = selectedRange.useContents {
            val loc = location.toInt()
            TextRange(loc, loc + length.toInt())
        }

        input?.setMarkedText(markedText, relativeTextRange)
    }

    /**
     * Unmarks the currently marked text.
     * After this method is called, the value of markedTextRange is nil.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614512-unmarktext
     */
    override fun unmarkText() {
        input?.unmarkText()
    }

    override fun beginningOfDocument(): UITextPosition {
        return TextInputPosition(0)
    }

    /**
     * The text position for the end of a document.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614555-endofdocument
     */
    override fun endOfDocument(): UITextPosition {
        return TextInputPosition(input?.endOfDocument() ?: 0)
    }

    /**
     * Attention! fromPosition and toPosition may be null
     */
    override fun textRangeFromPosition(
        fromPosition: UITextPosition,
        toPosition: UITextPosition
    ): UITextRange? {
        val from = (fromPosition as? TextInputPosition)?.position ?: return null
        val to = (toPosition as? TextInputPosition)?.position ?: return null
        return TextInputRange(
            TextInputPosition(minOf(from, to)),
            TextInputPosition(maxOf(from, to))
        )
    }

    /**
     * Attention! position may be null
     * @param position a custom UITextPosition object that represents a location in a document.
     * @param offset a character offset from position. It can be a positive or negative value.
     * Offset should be considered as a number of Unicode characters. One Unicode character can contain several bytes.
     */
    override fun positionFromPosition(
        position: UITextPosition,
        offset: NSInteger
    ): UITextPosition? {
        val p = (position as? TextInputPosition)?.position ?: return null
        val input = input ?: return null
        return input.positionFromPosition(position = p, offset = offset.toInt())?.let {
            TextInputPosition(it)
        }
    }

    private fun positionFromPositionVertical(
        position: UITextPosition,
        offset: NSInteger
    ): UITextPosition? {
        val p = (position as? TextInputPosition)?.position ?: return null
        val input = input ?: return null
        return input.verticalPositionFromPosition(position = p, verticalOffset = offset.toInt())
            ?.let { TextInputPosition(it) }
    }

    override fun positionFromPosition(
        position: UITextPosition,
        inDirection: UITextLayoutDirection,
        offset: NSInteger
    ): UITextPosition? {
        return when (inDirection) {
            UITextLayoutDirectionLeft -> positionFromPosition(position, -offset)
            UITextLayoutDirectionRight -> positionFromPosition(position, offset)
            UITextLayoutDirectionDown -> positionFromPositionVertical(position, offset)
            UITextLayoutDirectionUp -> positionFromPositionVertical(position, -offset)
            else -> null
        }
    }

    /**
     * Attention! position and toPosition may be null
     */
    override fun comparePosition(
        position: UITextPosition,
        toPosition: UITextPosition
    ): NSComparisonResult {
        val from = (position as? TextInputPosition)?.position ?: return NSOrderedSame
        val to = (toPosition as? TextInputPosition)?.position ?: return NSOrderedSame
        val result = if (from < to) {
            NSOrderedAscending
        } else if (from > to) {
            NSOrderedDescending
        } else {
            NSOrderedSame
        }
        return result
    }

    override fun offsetFromPosition(from: UITextPosition, toPosition: UITextPosition): NSInteger {
        if (from !is TextInputPosition || toPosition !is TextInputPosition) {
            return 0
        }
        return (toPosition.position - from.position).toLong()
    }

    override fun characterOffsetOfPosition(
        position: UITextPosition,
        withinRange: UITextRange
    ): NSInteger = 0L

    override fun positionWithinRange(
        range: UITextRange,
        farthestInDirection: UITextLayoutDirection
    ): UITextPosition = TextInputPosition(0)

    override fun characterRangeByExtendingPosition(
        position: UITextPosition,
        inDirection: UITextLayoutDirection
    ): UITextRange? {
        val oldPosition = position as? TextInputPosition ?: return null
        val newPosition = positionFromPosition(oldPosition, inDirection = inDirection, offset = 1)
            as? TextInputPosition ?: return null
        return if (newPosition.position < oldPosition.position) {
            TextInputRange(newPosition, oldPosition)
        } else {
            TextInputRange(oldPosition, newPosition)
        }
    }

    override fun baseWritingDirectionForPosition(
        position: UITextPosition,
        inDirection: UITextStorageDirection
    ): NSWritingDirection {
        return NSWritingDirectionNatural
    }

    override fun setBaseWritingDirection(
        writingDirection: NSWritingDirection,
        forRange: UITextRange
    ) {}

    override fun firstRectForRange(range: UITextRange): CValue<CGRect> =
        CGRectNull.readValue()

    override fun caretRectForPosition(position: UITextPosition): CValue<CGRect> =
        CGRectMake(x = 1.0, y = 1.0, width = 0.0, height = 1.0)

    override fun selectionRectsForRange(range: UITextRange): List<*> =
        listOf<UITextSelectionRect>()

    override fun closestPositionToPoint(point: CValue<CGPoint>): UITextPosition? = null

    override fun closestPositionToPoint(
        point: CValue<CGPoint>,
        withinRange: UITextRange
    ): UITextPosition? = null

    override fun characterRangeAtPoint(point: CValue<CGPoint>): UITextRange? = null

    override fun textStylingAtPosition(
        position: UITextPosition,
        inDirection: UITextStorageDirection
    ): Map<Any?, *> {
        return NSDictionary.dictionary()
    }

    override fun shouldChangeTextInRange(range: UITextRange, replacementText: String): Boolean {
        // Here we should decide to replace text in range or not.
        // By default, this method returns true.
        return true
    }

    override fun textInputView(): UIView {
        return this
    }

    override fun keyboardType(): UIKeyboardType = inputTraits.keyboardType()
    override fun keyboardAppearance(): UIKeyboardAppearance = inputTraits.keyboardAppearance()
    override fun returnKeyType(): UIReturnKeyType = inputTraits.returnKeyType()
    override fun textContentType(): UITextContentType = inputTraits.textContentType()
    override fun isSecureTextEntry(): Boolean = inputTraits.isSecureTextEntry()
    override fun enablesReturnKeyAutomatically(): Boolean = inputTraits.enablesReturnKeyAutomatically()
    override fun autocapitalizationType(): UITextAutocapitalizationType = inputTraits.autocapitalizationType()
    override fun autocorrectionType(): UITextAutocorrectionType = inputTraits.autocorrectionType()
    override fun writingToolsBehavior(): UIWritingToolsBehavior = inputTraits.writingToolsBehavior()

    /**
     * Call when something changes in text data
     */
    fun textWillChange() {
        _inputDelegate?.textWillChange(this)
    }

    /**
     * Call when something changes in text data
     */
    fun textDidChange() {
        _inputDelegate?.textDidChange(this)
    }

    /**
     * Call when something changes in text data
     */
    fun selectionWillChange() {
        _inputDelegate?.selectionWillChange(this)
    }

    /**
     * Call when something changes in text data
     */
    fun selectionDidChange() {
        _inputDelegate?.selectionDidChange(this)
    }

    override fun isUserInteractionEnabled(): Boolean = false

    override fun editMenuDelay(): Double =
        doubleTapTimeoutMillis.milliseconds.toDouble(DurationUnit.SECONDS)

    fun hideTextMenu() = this.hideEditMenu()

    fun isTextMenuShown() = isEditMenuShown

    private val _tokenizer = TextInputStringTokenizer(textInput = this) {
        input?.let { it.textInRange(TextRange(0, it.endOfDocument())) }
    }
    override fun tokenizer(): UITextInputTokenizerProtocol = _tokenizer
}
