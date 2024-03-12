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
import androidx.compose.ui.platform.IOSSkikoInput
import androidx.compose.ui.platform.SkikoUITextInputTraits
import androidx.compose.ui.platform.TextActions
import androidx.compose.ui.platform.ViewConfiguration
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectIntersectsRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectNull
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSComparisonResult
import platform.Foundation.NSDictionary
import platform.Foundation.NSOrderedAscending
import platform.Foundation.NSOrderedDescending
import platform.Foundation.NSOrderedSame
import platform.Foundation.NSRange
import platform.Foundation.NSSelectorFromString
import platform.Foundation.dictionary
import platform.UIKit.NSWritingDirection
import platform.UIKit.NSWritingDirectionLeftToRight
import platform.UIKit.UIEvent
import platform.UIKit.UIKeyInputProtocol
import platform.UIKit.UIKeyboardAppearance
import platform.UIKit.UIKeyboardType
import platform.UIKit.UIMenuController
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIResponderStandardEditActionsProtocol
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextContentType
import platform.UIKit.UITextInputDelegateProtocol
import platform.UIKit.UITextInputProtocol
import platform.UIKit.UITextInputStringTokenizer
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
import platform.darwin.NSInteger

/**
 * Hidden UIView to interact with iOS Keyboard and TextInput system.
 * TODO maybe need to call reloadInputViews() to update UIKit text features?
 */
@Suppress("CONFLICTING_OVERLOADS")
internal class IntermediateTextInputUIView(
    private val keyboardEventHandler: KeyboardEventHandler,
    private val viewConfiguration: ViewConfiguration
) : UIView(frame = CGRectZero.readValue()),
    UIKeyInputProtocol, UITextInputProtocol {
    private var menuMonitoringJob = Job()
    private var _inputDelegate: UITextInputDelegateProtocol? = null
    var input: IOSSkikoInput? = null
        set(value) {
            field = value
            if (value == null) {
                cancelContextMenuUpdate()
            }
        }

    private var _currentTextMenuActions: TextActions? = null
    var inputTraits: SkikoUITextInputTraits = EmptyInputTraits

    override fun canBecomeFirstResponder() = true

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        handleUIViewPressesBegan(keyboardEventHandler, presses, withEvent)
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        handleUIViewPressesEnded(keyboardEventHandler, presses, withEvent)
        super.pressesEnded(presses, withEvent)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return if (input == null) {
            null
        } else {
            super.hitTest(point, withEvent)
        }
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
        return input?.textInRange(range.toIntRange())
    }

    /**
     * Replaces the text in a document that is in the specified range.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614558-replace
     * @param range A range of text in a document.
     * @param withText A string to replace the text in range.
     */
    override fun replaceRange(range: UITextRange, withText: String) {
        input?.replaceRange(range.toIntRange(), withText)
    }

    override fun setSelectedTextRange(selectedTextRange: UITextRange?) {
        input?.setSelectedTextRange(selectedTextRange?.toIntRange())
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
        val (locationRelative, lengthRelative) = selectedRange.useContents {
            location.toInt() to length.toInt()
        }
        val relativeTextRange = locationRelative until locationRelative + lengthRelative
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
        return IntermediateTextPosition(0)
    }

    /**
     * The text position for the end of a document.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614555-endofdocument
     */
    override fun endOfDocument(): UITextPosition {
        return IntermediateTextPosition(input?.endOfDocument() ?: 0)
    }

    /**
     * Attention! fromPosition and toPosition may be null
     */
    override fun textRangeFromPosition(
        fromPosition: UITextPosition,
        toPosition: UITextPosition
    ): UITextRange? {
        val from = (fromPosition as? IntermediateTextPosition)?.position ?: 0
        val to = (toPosition as? IntermediateTextPosition)?.position ?: 0
        return IntermediateTextRange(
            IntermediateTextPosition(minOf(from, to)),
            IntermediateTextPosition(maxOf(from, to))
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
        val p = (position as? IntermediateTextPosition)?.position ?: return null
        val endOfDocument = input?.endOfDocument()
        return if (endOfDocument != null) {
            val result = input?.positionFromPosition(position = p, offset = offset)
            IntermediateTextPosition(result ?: (p + offset).coerceIn(0, endOfDocument))
        } else {
            null
        }
    }

    override fun positionFromPosition(
        position: UITextPosition,
        inDirection: UITextLayoutDirection,
        offset: NSInteger
    ): UITextPosition? {
        return when (inDirection) {
            UITextLayoutDirectionLeft, UITextLayoutDirectionUp -> {
                positionFromPosition(position, -offset)
            }

            else -> positionFromPosition(position, offset)
        }
    }

    /**
     * Attention! position and toPosition may be null
     */
    override fun comparePosition(
        position: UITextPosition,
        toPosition: UITextPosition
    ): NSComparisonResult {
        val from = (position as? IntermediateTextPosition)?.position ?: 0
        val to = (toPosition as? IntermediateTextPosition)?.position ?: 0
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
        if (from !is IntermediateTextPosition) {
            error("from !is IntermediateTextPosition")
        }
        if (toPosition !is IntermediateTextPosition) {
            error("toPosition !is IntermediateTextPosition")
        }
        return toPosition.position - from.position
    }

    override fun positionWithinRange(
        range: UITextRange,
        atCharacterOffset: NSInteger
    ): UITextPosition? =
        TODO("positionWithinRange range: $range, atCharacterOffset: $atCharacterOffset")

    override fun positionWithinRange(
        range: UITextRange,
        farthestInDirection: UITextLayoutDirection
    ): UITextPosition? =
        TODO("positionWithinRange, farthestInDirection: ${farthestInDirection.directionToStr()}")

    override fun characterRangeByExtendingPosition(
        position: UITextPosition,
        inDirection: UITextLayoutDirection
    ): UITextRange? {
        if (position !is IntermediateTextPosition) {
            error("position !is IntermediateTextPosition")
        }
        TODO("characterRangeByExtendingPosition, inDirection: ${inDirection.directionToStr()}")
    }

    override fun baseWritingDirectionForPosition(
        position: UITextPosition,
        inDirection: UITextStorageDirection
    ): NSWritingDirection {
        return NSWritingDirectionLeftToRight // TODO support RTL text direction
        if (position !is IntermediateTextPosition) {
            error("position !is IntermediateTextPosition")
        }
    }

    override fun setBaseWritingDirection(
        writingDirection: NSWritingDirection,
        forRange: UITextRange
    ) {
        // TODO support RTL text direction
    }

    //Working with Geometry and Hit-Testing. All methods return stubs for now.
    override fun firstRectForRange(range: UITextRange): CValue<CGRect> = CGRectNull.readValue()
    override fun caretRectForPosition(position: UITextPosition): CValue<CGRect> {
        /* TODO: https://youtrack.jetbrains.com/issue/COMPOSE-332/
            CGRectNull here led to crash with Speech-to-text on iOS 16.0
            Set all fields to 1.0 to avoid potential dividing by zero
            Ideally, here should be correct rect for caret from Compose.
         */
        return CGRectMake(x = 1.0, y = 1.0, width = 1.0, height = 1.0)
        if (position !is IntermediateTextPosition) {
            error("position !is IntermediateTextPosition")
        }
    }

    override fun selectionRectsForRange(range: UITextRange): List<*> = listOf<UITextSelectionRect>()
    override fun closestPositionToPoint(point: CValue<CGPoint>): UITextPosition? = null
    override fun closestPositionToPoint(
        point: CValue<CGPoint>,
        withinRange: UITextRange
    ): UITextPosition? = null

    override fun characterRangeAtPoint(point: CValue<CGPoint>): UITextRange? = null

    override fun textStylingAtPosition(
        position: UITextPosition,
        inDirection: UITextStorageDirection
    ): Map<Any?, *>? {
        return NSDictionary.dictionary()
        //TODO: Need to implement
        if (position !is IntermediateTextPosition) {
            error("position !is IntermediateTextPosition")
        }
    }

    override fun characterOffsetOfPosition(
        position: UITextPosition,
        withinRange: UITextRange
    ): NSInteger {
        if (position !is IntermediateTextPosition) {
            error("position !is IntermediateTextPosition")
        }
        TODO("characterOffsetOfPosition")
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
    override fun enablesReturnKeyAutomatically(): Boolean =
        inputTraits.enablesReturnKeyAutomatically()

    override fun autocapitalizationType(): UITextAutocapitalizationType =
        inputTraits.autocapitalizationType()

    override fun autocorrectionType(): UITextAutocorrectionType = inputTraits.autocorrectionType()

    override fun dictationRecognitionFailed() {
        //todo may be useful
    }

    override fun dictationRecordingDidEnd() {
        //todo may be useful
    }

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

    override fun isUserInteractionEnabled(): Boolean = false // disable clicks

    override fun canPerformAction(action: COpaquePointer?, withSender: Any?): Boolean {
        return when (action) {
            NSSelectorFromString(UIResponderStandardEditActionsProtocol::copy.name + ":") ->
                _currentTextMenuActions?.copy != null

            NSSelectorFromString(UIResponderStandardEditActionsProtocol::cut.name + ":") ->
                _currentTextMenuActions?.cut != null

            NSSelectorFromString(UIResponderStandardEditActionsProtocol::paste.name + ":") ->
                _currentTextMenuActions?.paste != null

            NSSelectorFromString(UIResponderStandardEditActionsProtocol::selectAll.name + ":") ->
                _currentTextMenuActions?.selectAll != null

            else -> false
        }
    }

    private fun shouldReloadContextMenuItems(actions: TextActions): Boolean {
        return (_currentTextMenuActions?.copy == null) != (actions.copy == null) ||
            (_currentTextMenuActions?.paste == null) != (actions.paste == null) ||
            (_currentTextMenuActions?.cut == null) != (actions.cut == null) ||
            (_currentTextMenuActions?.selectAll == null) != (actions.selectAll == null)
    }

    private fun cancelContextMenuUpdate() {
        menuMonitoringJob.cancel()
        menuMonitoringJob = Job()
    }

    /**
     * Show copy/paste text menu
     * @param targetRect - rectangle of selected text area
     * @param textActions - available (not null) actions in text menu
     */
    fun showTextMenu(targetRect: org.jetbrains.skia.Rect, textActions: TextActions) {
        val cgRect = CGRectMake(
            x = targetRect.left.toDouble(),
            y = targetRect.top.toDouble(),
            width = targetRect.width.toDouble(),
            height = targetRect.height.toDouble()
        )
        val isTargetVisible = CGRectIntersectsRect(bounds, cgRect)

        if (isTargetVisible) {
            // TODO: UIMenuController is deprecated since iOS 17 and not available on iOS 12
            val menu: UIMenuController = UIMenuController.sharedMenuController()
            if (shouldReloadContextMenuItems(textActions)) {
                menu.hideMenu()
            }
            cancelContextMenuUpdate()
            CoroutineScope(Dispatchers.Main + menuMonitoringJob).launch {
                delay(viewConfiguration.doubleTapTimeoutMillis)
                menu.showMenuFromView(targetView = this@IntermediateTextInputUIView, cgRect)
            }
            _currentTextMenuActions = textActions
        } else {
            hideTextMenu()
        }
    }

    fun hideTextMenu() {
        cancelContextMenuUpdate()

        _currentTextMenuActions = null
        val menu: UIMenuController = UIMenuController.sharedMenuController()
        menu.hideMenu()
    }

    fun isTextMenuShown(): Boolean {
        return _currentTextMenuActions != null
    }

    override fun copy(sender: Any?) {
        _currentTextMenuActions?.copy?.invoke()
    }

    override fun paste(sender: Any?) {
        _currentTextMenuActions?.paste?.invoke()
    }

    override fun cut(sender: Any?) {
        _currentTextMenuActions?.cut?.invoke()
    }

    override fun selectAll(sender: Any?) {
        _currentTextMenuActions?.selectAll?.invoke()
    }

    override fun tokenizer(): UITextInputTokenizerProtocol =
        UITextInputStringTokenizer(textInput = this)
}

private class IntermediateTextPosition(val position: Long = 0) : UITextPosition()

private fun IntermediateTextRange(start: Int, end: Int) =
    IntermediateTextRange(
        _start = IntermediateTextPosition(start.toLong()),
        _end = IntermediateTextPosition(end.toLong())
    )

private class IntermediateTextRange(
    private val _start: IntermediateTextPosition,
    private val _end: IntermediateTextPosition
) : UITextRange() {
    override fun isEmpty() = (_end.position - _start.position) <= 0
    override fun start(): UITextPosition = _start
    override fun end(): UITextPosition = _end
}

private fun UITextRange.toIntRange(): IntRange {
    val start = (start() as IntermediateTextPosition).position.toInt()
    val end = (end() as IntermediateTextPosition).position.toInt()
    return start until end
}

private fun IntRange.toUITextRange(): UITextRange =
    IntermediateTextRange(start = start, end = endInclusive + 1)

private fun NSWritingDirection.directionToStr() =
    when (this) {
        UITextLayoutDirectionLeft -> "Left"
        UITextLayoutDirectionRight -> "Right"
        UITextLayoutDirectionUp -> "Up"
        UITextLayoutDirectionDown -> "Down"
        else -> "Unknown"
    }
