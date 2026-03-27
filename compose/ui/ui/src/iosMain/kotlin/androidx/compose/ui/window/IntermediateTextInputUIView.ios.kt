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

import androidx.compose.ui.platform.DpInsets
import androidx.compose.ui.platform.EmptyInputTraits
import androidx.compose.ui.platform.IOSSkikoInput
import androidx.compose.ui.platform.SkikoUITextInputTraits
import androidx.compose.ui.platform.TextSelectionRect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.uikit.utils.CMPEditMenuCustomAction
import androidx.compose.ui.uikit.utils.CMPEditMenuView
import androidx.compose.ui.uikit.utils.CMPGestureRecognizer
import androidx.compose.ui.uikit.utils.CMPTextInputStringTokenizer
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.skia.BreakIterator
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointEqualToPoint
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectContainsPoint
import platform.CoreGraphics.CGRectEqualToRect
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.CoreGraphics.CGRectInset
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectNull
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeEqualToSize
import platform.Foundation.NSComparisonResult
import platform.Foundation.NSDictionary
import platform.Foundation.NSOrderedAscending
import platform.Foundation.NSOrderedDescending
import platform.Foundation.NSOrderedSame
import platform.Foundation.NSRange
import platform.Foundation.NSStringFromSelector
import platform.Foundation.dictionary
import platform.UIKit.NSWritingDirection
import platform.UIKit.NSWritingDirectionNatural
import platform.UIKit.UIAction
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsetsEqualToEdgeInsets
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIEvent
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIKeyInputProtocol
import platform.UIKit.UIKeyboardAppearance
import platform.UIKit.UIKeyboardType
import platform.UIKit.UIMenu
import platform.UIKit.UIMenuElement
import platform.UIKit.UIPress
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIResponder
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UIScrollView
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextContentType
import platform.UIKit.UITextDirection
import platform.UIKit.UITextGranularity
import platform.UIKit.UITextInputDelegateProtocol
import platform.UIKit.UITextInputProtocol
import platform.UIKit.UITextInputTokenizerProtocol
import platform.UIKit.UITextInteraction
import platform.UIKit.UITextInteractionMode
import platform.UIKit.UITextLayoutDirection
import platform.UIKit.UITextLayoutDirectionDown
import platform.UIKit.UITextLayoutDirectionLeft
import platform.UIKit.UITextLayoutDirectionRight
import platform.UIKit.UITextLayoutDirectionUp
import platform.UIKit.UITextPosition
import platform.UIKit.UITextRange
import platform.UIKit.UITextSelectionRect
import platform.UIKit.UITextStorageDirection
import platform.UIKit.UITextStorageDirectionForward
import platform.UIKit.UITextWritingDirection
import platform.UIKit.UITouch
import platform.UIKit.UIView
import platform.UIKit.UIWritingToolsBehavior
import platform.UIKit.addInteraction
import platform.UIKit.systemBlueColor
import platform.darwin.NSInteger

private val NoOpOnKeyboardPresses: (Set<*>) -> Unit = {}
/**
 * Hidden UIView to interact with iOS Keyboard and TextInput system.
 */
internal class IntermediateTextInputUIView(
    private val doubleTapTimeoutMillis: Long,
    private val usingNativeTextInput: Boolean,
    private val coroutineScope: CoroutineScope
) : CMPEditMenuView(frame = CGRectZero.readValue()),
    UIKeyInputProtocol, UITextInputProtocol {
    private var _inputDelegate: UITextInputDelegateProtocol? = null
    var input: IOSSkikoInput? = null
        set(value) {
            field = value
            if (value == null) {
                hideTextMenu()
            }
        }

    private val touchesTrackerGestureRecognizer = TouchTrackingGestureRecognizer().also {
        if (usingNativeTextInput) {
            addGestureRecognizer(it)
        }
    }

    /**
     * Callback to handle keyboard presses. The parameter is a [Set] of [UIPress] objects.
     * Erasure happens due to K/N not supporting Obj-C lightweight generics.
     */
    var onKeyboardPresses: (Set<*>) -> Unit = NoOpOnKeyboardPresses

    var inputTraits: SkikoUITextInputTraits = EmptyInputTraits

    override fun inputView(): UIView? = inputTraits.inputView()
    override fun inputAccessoryView(): UIView? = inputTraits.inputAccessoryView()

    override fun canBecomeFirstResponder() = true

    private val selectionInteraction =
        UITextInteraction.textInteractionForMode(UITextInteractionMode.UITextInteractionModeEditable)
            .also {
                it.setTextInput(this)
            }

    private var selectionInteractionAttached: Boolean = false

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        if (usingNativeTextInput) {
            if (window != null && !selectionInteractionAttached) {
                // Ensure UIKit text interaction is attached early so that cursor and selection
                // handles can be created and shown when needed.
                this.addInteraction(selectionInteraction)
                selectionInteractionAttached = true
            }
        }
    }

    override fun becomeFirstResponder(): Boolean {
        val isFirstResponder = this.isFirstResponder()
        val result = super.becomeFirstResponder()

        if (usingNativeTextInput) {
            if (!isFirstResponder && this.isFirstResponder()) {
                this.addInteraction(selectionInteraction)
                this.activateTextInputInteractionIfNeeded()
            }
        }

        return result
    }

    override fun setTintColor(tintColor: UIColor?) {
        val colorToSet = tintColor ?: UIColor.systemBlueColor
        if (super.tintColor != colorToSet) {
            if (this.isFirstResponder()) {
                this.deactivateTextInputInteractionIfNeeded()
                this.activateTextInputInteractionIfNeeded()
            }
            super.setTintColor(colorToSet)
        }
    }

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

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        onKeyboardPresses(presses)
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        onKeyboardPresses(presses)
        super.pressesEnded(presses, withEvent)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        if (usingNativeTextInput) {
            return if (input == null) {
                null
            } else {
                super.hitTest(point, withEvent)
            }
        } else {
            return super.hitTest(point, withEvent)
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
        input?.withBatch {
            input?.replaceRange(textRange, withText)
        }
    }

    override fun setSelectedTextRange(selectedTextRange: UITextRange?) {
        val range = selectedTextRange?.toTextRange()
        if (usingNativeTextInput) {
            if (input?.getSelectedTextRange() != range) {
                // iOS <= 16 does not update selection handles when selection changes from the keyboard
                // Posting an extra notification solves this issue
                val notifySelectionChanges = !available(OS.Ios to OSVersion(major = 17)) &&
                    !touchesTrackerGestureRecognizer.isTrackingTouches
                if (notifySelectionChanges) {
                    selectionWillChange()
                }
                input?.withBatch {
                    input?.setSelectedTextRange(range)
                }
                if (notifySelectionChanges) {
                    selectionDidChange()
                }
            }
        } else {
            input?.withBatch {
                input?.setSelectedTextRange(range)
            }
        }
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
        val relativeTextRange = TextRange(locationRelative, locationRelative + lengthRelative)

        // Due to iOS specifics, [setMarkedText] can be called several times in a row. Batching
        // helps to avoid text input problems, when Composables use parameters set during
        // recomposition instead of the current ones. Example:
        // 1. State "1" -> TextField(text = "1")
        // 2. setMarkedText "12" -> Not equal to TextField(text = "1") -> State "12"
        // 3. setMarkedText "1" -> Equal to TextField(text = "1") -> State remains "12"
        // scene.render() - Recomposes TextField
        // 4. State "12" -> TextField(text = "12") - Invalid state. Should be TextField(text = "1")
        input?.withBatch {
            input?.setMarkedText(markedText, relativeTextRange)
        }
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
        val from = (fromPosition as? IntermediateTextPosition)?.position ?: return null
        val to = (toPosition as? IntermediateTextPosition)?.position ?: return null
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
        val input = input ?: return null
        return input.positionFromPosition(position = p, offset = offset.toInt())?.let {
            IntermediateTextPosition(it)
        }
    }

    private fun positionFromPositionVertical(
        position: UITextPosition,
        offset: NSInteger
    ): UITextPosition? {
        val p = (position as? IntermediateTextPosition)?.position ?: return null
        val input = input ?: return null
        return input.verticalPositionFromPosition(position = p, verticalOffset = offset.toInt())
            ?.let { IntermediateTextPosition(it) }
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
        val from = (position as? IntermediateTextPosition)?.position ?: return NSOrderedSame
        val to = (toPosition as? IntermediateTextPosition)?.position ?: return NSOrderedSame
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
        if (from !is IntermediateTextPosition || toPosition !is IntermediateTextPosition) {
            return 0
        }
        return (toPosition.position - from.position).toLong()
    }

    override fun characterOffsetOfPosition(
        position: UITextPosition,
        withinRange: UITextRange
    ): NSInteger {
        return if (usingNativeTextInput) {
            val withinTextRange = withinRange.toTextRange() ?: return 0L
            val intermediatePosition = (position as? IntermediateTextPosition)?.position ?: 0
            (intermediatePosition - withinTextRange.start).toLong()
        } else 0L
    }

    override fun positionWithinRangeAtCharacterOffset(
        range: UITextRange,
        atCharacterOffset: NSInteger
    ): UITextPosition = if (usingNativeTextInput) {
        val fallback = IntermediateTextPosition(0)
        val textRange = range.toTextRange() ?: return fallback
        (textRange.start + atCharacterOffset.toInt()).takeIf { range.isValid() && it in textRange }
            ?.let { IntermediateTextPosition(it) } ?: fallback
    } else {
        IntermediateTextPosition(0)
    }

    override fun positionWithinRangeFarthestInDirection(
        range: UITextRange,
        farthestInDirection: UITextLayoutDirection
    ): UITextPosition = if (usingNativeTextInput) {
        val fallback = IntermediateTextPosition(0)
        val textRange = range.toTextRange() ?: return fallback
        PlatformTextLayoutDirection(farthestInDirection)?.let { direction ->
            input?.positionWithinRange(textRange, direction)?.let {
                IntermediateTextPosition(it)
            }
        } ?: fallback
    } else {
        IntermediateTextPosition(0)
    }

    override fun characterRangeByExtendingPosition(
        position: UITextPosition,
        inDirection: UITextLayoutDirection
    ): UITextRange? {
        val oldPosition = position as? IntermediateTextPosition ?: return null
        val newPosition = positionFromPosition(oldPosition, inDirection = inDirection, offset = 1)
            as? IntermediateTextPosition ?: return null
        return if (newPosition.position < oldPosition.position) {
            IntermediateTextRange(newPosition, oldPosition)
        } else {
            IntermediateTextRange(oldPosition, newPosition)
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

    override fun firstRectForRange(range: UITextRange): CValue<CGRect> {
        if (usingNativeTextInput) {
            val fallback = CGRectZero.readValue()
            val textRange = range.toTextRange() ?: return fallback
            return input?.firstSelectionRectForRange(textRange)?.asCGRect()
                ?: fallback
        } else {
            return CGRectNull.readValue()
        }

    }

    override fun caretRectForPosition(position: UITextPosition): CValue<CGRect> {
        val fallbackRect = CGRectMake(x = 1.0, y = 1.0, width = 0.0, height = 1.0)
        if (usingNativeTextInput) {
            val position = (position as? IntermediateTextPosition)?.position ?: return fallbackRect
            val caretDpRect = input?.caretDpRectForPosition(position)
            return caretDpRect?.asCGRect() ?: fallbackRect
        } else {
            return fallbackRect
        }
    }

    override fun selectionRectsForRange(range: UITextRange): List<*> {
        if (usingNativeTextInput) {
            val fallbackList = listOf<UITextSelectionRect>()
            val textRange = TextRange(
                start = (range.start as? IntermediateTextPosition)?.position ?: return fallbackList,
                end = (range.end as? IntermediateTextPosition)?.position ?: return fallbackList
            )
            val rects = input?.selectionDpRectsForRange(textRange) ?: return fallbackList

            // HACK: On iOS 17+, selection changes are not submitted during selection interaction.
            if (available(OS.Ios to OSVersion(major = 17)) &&
                touchesTrackerGestureRecognizer.isTrackingTouches
            ) {
                shouldPerformSelectionNotifications = false
                if (input?.getSelectedTextRange() != textRange) {
                    input?.setSelectedTextRange(textRange)
                }
                shouldPerformSelectionNotifications = true
            }

            return rects.fastMap { IntermediateTextSelectionRect(it) }
        } else {
            return listOf<UITextSelectionRect>()
        }
    }

    override fun closestPositionToPoint(point: CValue<CGPoint>): UITextPosition? {
        if (usingNativeTextInput) {
            val closestPosition =
                input?.closestPositionToPoint(point.useContents { DpOffset(x.dp, y.dp) }) ?: return null
            return IntermediateTextPosition(closestPosition)
        } else {
            return null
        }
    }

    override fun closestPositionToPoint(
        point: CValue<CGPoint>,
        withinRange: UITextRange
    ): UITextPosition? {
        if (usingNativeTextInput) {
            val textRange = (withinRange as? IntermediateTextRange)?.toTextRange() ?: return null
            val closestPosition = input?.closestPositionToPoint(
                point.useContents { DpOffset(x.dp, y.dp) },
                textRange
            ) ?: return null
            return IntermediateTextPosition(closestPosition)
        } else {
            return null
        }
    }

    override fun characterRangeAtPoint(point: CValue<CGPoint>): UITextRange? {
        if (usingNativeTextInput) {
            val characterRange =
                input?.characterRangeAtPoint(point.useContents { DpOffset(x.dp, y.dp) }) ?: return null
            return IntermediateTextRange(characterRange.start, characterRange.end)
        } else {
            return null
        }
    }

    override fun textStylingAtPosition(
        position: UITextPosition,
        inDirection: UITextStorageDirection
    ): Map<Any?, *> {
        return NSDictionary.dictionary()
        //TODO: Need to implement
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

    private var shouldPerformSelectionNotifications: Boolean = usingNativeTextInput

    /**
     * Call when something changes in text data
     */
    fun selectionWillChange() {
        if (shouldPerformSelectionNotifications) {
            _inputDelegate?.selectionWillChange(this)
        }
    }

    /**
     * Call when something changes in text data
     */
    fun selectionDidChange() {
        if (shouldPerformSelectionNotifications) {
            _inputDelegate?.selectionDidChange(this)
        }
    }

    override fun isUserInteractionEnabled(): Boolean = usingNativeTextInput

    override fun editMenuDelay(): Double =
        doubleTapTimeoutMillis.milliseconds.toDouble(DurationUnit.SECONDS)

    fun hideTextMenu() = this.hideEditMenu()

    fun isTextMenuShown() = isEditMenuShown

    private var onCopy: (() -> Unit)? = null
    private var onPaste: (() -> Unit)? = null
    private var onCut: (() -> Unit)? = null
    private var onSelectAll: (() -> Unit)? = null
    private var customActions: List<CMPEditMenuCustomAction> = emptyList()

    override fun copy(sender: Any?) {
        if (usingNativeTextInput) {
            onCopy?.invoke()
        } else {
            super.copy(sender)
        }
    }

    override fun paste(sender: Any?) {
        if (usingNativeTextInput) {
            onPaste?.invoke()
        } else {
            super.paste(sender)
        }
    }

    override fun cut(sender: Any?) {
        if (usingNativeTextInput) {
            onCut?.invoke()
        } else {
            super.cut(sender)
        }
    }

    override fun selectAll(sender: Any?) {
        if (usingNativeTextInput) {
            onSelectAll?.invoke()
        } else {
            super.selectAll(sender)
        }
    }

    override fun canPerformAction(action: COpaquePointer?, withSender: Any?): Boolean {
        if (usingNativeTextInput) {
            val selectorName = NSStringFromSelector(action)

            return when (selectorName) {
                "copy:" -> onCopy != null
                "paste:" -> onPaste != null
                "cut:" -> onCut != null
                "selectAll:" -> onSelectAll != null
                else -> super.canPerformAction(action, withSender)
            }
        } else {
            return super.canPerformAction(action, withSender)
        }
    }

    fun updateMenuActions(
        copy: (() -> Unit)?,
        paste: (() -> Unit)?,
        cut: (() -> Unit)?,
        selectAll: (() -> Unit)?,
        customActions: List<CMPEditMenuCustomAction>
    ) {
        onCopy = copy
        onPaste = paste
        onCut = cut
        onSelectAll = selectAll
        this.customActions = customActions
    }


    override fun editMenuForTextRange(textRange: UITextRange, suggestedActions: List<*>): UIMenu? {
        if (usingNativeTextInput) {
            val customMenuElements = makeCustomMenuElements()
            if (customMenuElements.isEmpty()) return null // The default menu would be returned

            @Suppress("UNCHECKED_CAST")
            val suggestedActionsElements = suggestedActions as List<UIMenuElement>

            return UIMenu.menuWithTitle("", children = customMenuElements + suggestedActionsElements)
        } else {
            return null
        }
    }

    private fun makeCustomMenuElements(): List<UIMenuElement> {
        if (customActions.isEmpty()) return emptyList()
        return customActions.mapNotNull {
            val title = it.title ?: return@mapNotNull null
            val block = it.actionBlock ?: return@mapNotNull null
            UIAction.actionWithTitle(
                title = title,
                image = null,
                identifier = null,
                handler = { block() },
            )
        }
    }

    private val _tokenizer = IntermediateTextTokenizer(textInput = this) {
        input?.let { it.textInRange(TextRange(0, it.endOfDocument())) }
    }
    override fun tokenizer(): UITextInputTokenizerProtocol = _tokenizer

    fun resetOnKeyboardPressesCallback() {
        onKeyboardPresses = NoOpOnKeyboardPresses
    }

    private fun IOSSkikoInput.withBatch(update: () -> Unit) {
        beginEditBatch()
        update()
        coroutineScope.launch {
            endEditBatch()
        }
    }

    private fun UITextRange.isValid(): Boolean {
        val range = this.toTextRange() ?: return false
        val textEndPos = input?.endOfDocument() ?: 0
        return range.start in 0..range.end && range.end <= textEndPos
    }
}

private class IntermediateTextPosition(val position: Int = 0) : UITextPosition() {
    override fun description(): String {
        return "IntermediateTextPosition($position)"
    }

    init {
        assert(position >= 0) { "position should be >= 0" }
    }
}

private class IntermediateTextSelectionRect(
    private var _rect: CValue<CGRect>,
    private val _writingDirection: UITextWritingDirection,
    private val _containsStart: Boolean,
    private val _containsEnd: Boolean,
    private val _isVertical: Boolean

) : UITextSelectionRect() {
    constructor(textSelectionRect: TextSelectionRect) : this(
        textSelectionRect.dpRect.asCGRect(),
        NSWritingDirectionNatural,
        textSelectionRect.containsStart,
        textSelectionRect.containsEnd,
        textSelectionRect.isVertical
    )

    override fun rect(): CValue<CGRect> = _rect
    override fun writingDirection(): NSWritingDirection = _writingDirection
    override fun containsStart(): Boolean = _containsStart
    override fun containsEnd(): Boolean = _containsEnd
    override fun isVertical(): Boolean = _isVertical
}

private fun IntermediateTextRange(start: Int, end: Int) =
    IntermediateTextRange(
        _start = IntermediateTextPosition(start),
        _end = IntermediateTextPosition(end)
    )

private class IntermediateTextRange(
    val _start: IntermediateTextPosition,
    val _end: IntermediateTextPosition
) : UITextRange() {
    override fun isEmpty() = (_end.position - _start.position) <= 0
    override fun start(): UITextPosition = _start
    override fun end(): UITextPosition = _end

    override fun description(): String {
        return "IntermediateTextRange(start=$_start, end=$_end)"
    }
}

// Despite UITextRange being declared as non-null, iOS can still pass null to methods that take a UITextRange parameter.
private fun UITextRange.toTextRange(): TextRange? {
    val start = (start() as? IntermediateTextPosition)?.position ?: return null
    val end = (end() as? IntermediateTextPosition)?.position ?: return null
    return TextRange(start, end)
}

private fun TextRange.toUITextRange(): UITextRange =
    IntermediateTextRange(start = start, end = end)

internal class IntermediateTextTokenizer(
    textInput: UIResponder,
    val getString: () -> String?
): CMPTextInputStringTokenizer(textInput) {
    private val newLineCharacters = setOf('\n', '\r', '\u2029')

    override fun positionFromPosition(
        position: UITextPosition,
        toBoundary: UITextGranularity,
        inDirection: UITextDirection
    ): UITextPosition? {
        val textPosition = position as? IntermediateTextPosition ?: return null
        val isForward = inDirection == UITextStorageDirectionForward ||
            inDirection == UITextLayoutDirectionRight ||
            inDirection == UITextLayoutDirectionDown

        val iterator = when (toBoundary) {
            UITextGranularity.UITextGranularityCharacter -> BreakIterator.makeCharacterInstance()
            UITextGranularity.UITextGranularityWord -> BreakIterator.makeWordInstance()
            UITextGranularity.UITextGranularitySentence -> BreakIterator.makeSentenceInstance()
            UITextGranularity.UITextGranularityLine -> BreakIterator.makeLineInstance()
            UITextGranularity.UITextGranularityParagraph ->
                return positionFromPositionToParagraphBoundary(position, isForward)

            else -> return super.positionFromPosition(position, toBoundary, inDirection)
        }

        val string = getString() ?: ""
        iterator.setText(string)

        val iteratorResult = if (isForward) {
            if (textPosition.position >= string.length - 1) {
                string.length
            } else {
                iterator.following(textPosition.position)
            }
        } else {
            if (textPosition.position <= 0) {
                0
            } else {
                iterator.preceding(textPosition.position)
            }
        }

        return IntermediateTextPosition(iteratorResult)
    }

    override fun isPositionAtBoundary(
        position: UITextPosition,
        atBoundary: UITextGranularity,
        inDirection: UITextDirection
    ): Boolean {
        val textPosition = position as? IntermediateTextPosition ?: return false

        val iterator = when (atBoundary) {
            UITextGranularity.UITextGranularityCharacter -> BreakIterator.makeCharacterInstance()
            UITextGranularity.UITextGranularityWord -> BreakIterator.makeWordInstance()
            UITextGranularity.UITextGranularitySentence -> BreakIterator.makeSentenceInstance()
            UITextGranularity.UITextGranularityLine -> BreakIterator.makeLineInstance()
            UITextGranularity.UITextGranularityParagraph -> {
                return isAtParagraphBoundary(getString() ?: "", textPosition.position)
            }
            else -> return super.isPositionAtBoundary(position, atBoundary, inDirection)
        }

        iterator.setText(getString() ?: "")
        return iterator.isBoundary(textPosition.position)
    }

    private fun positionFromPositionToParagraphBoundary(
        position: UITextPosition,
        isForward: Boolean
    ): UITextPosition? {
        val textPosition = position as? IntermediateTextPosition ?: return null

        val string = getString() ?: ""
        var location = textPosition.position
        while (isForward && location < string.length || !isForward && location > 0) {
            if (isForward) {
                if (string[location] in newLineCharacters) {
                    break
                }
                location++
            } else {
                if (string[location] in newLineCharacters) {
                    location++
                    break
                }
                location--
            }
        }
        return IntermediateTextPosition(location)
    }

    private fun isAtParagraphBoundary(text: String, position: Int): Boolean {
        if (position == 0 || position == text.length) return true
        return text[position] in newLineCharacters || text[position - 1] in newLineCharacters
    }
}

/**
 * A [UIScrollView] that hosts the [IntermediateTextInputUIView] when native input handling (Native Text Input)
 * is enabled.
 *
 * This container is necessary because:
 * 1. It provides a native coordinate system that matches the "full" (unclipped) text layout,
 *    allowing native iOS overlays (like the magnifier, floating cursor, and selection handles)
 *    to align correctly with the Compose-rendered text.
 * 2. It synchronizes its [contentOffset] and [contentInset] with the Compose text field's
 *    internal scroll state, ensuring that the UITextInput-conforming overlay view
 *    ([IntermediateTextInputUIView]) stays in sync with the visual rendering.
 * 3. It provides a larger hit-testing area for native interactive elements (like selection handles)
 *    that may overflow the immediate visual bounds of the text field.
 *
 * This view does not handle user-driven scrolling; scrolling is still managed by Compose,
 * which then updates this scroll view's state.
 */
internal class IntermediateTextScrollView(): UIScrollView(frame = CGRectZero.readValue()) {
    init {
        setScrollEnabled(false)
        setShowsVerticalScrollIndicator(false)
        setShowsHorizontalScrollIndicator(false)
        setCanCancelContentTouches(false)
        setDelaysContentTouches(false)
        setClipsToBounds(true)
    }

    var textView: IntermediateTextInputUIView? = null
        set(value) {
            if (field != value) {
                field?.removeFromSuperview()
                field = value
                value?.let {
                    addSubview(value)
                }
            }
        }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        val textView = textView ?: return null
        val hitTestResult = super.hitTest(point, withEvent)

        return if (available(OS.Ios to OSVersion(major = 17))) {
            (hitTestResult ?: hitTestTextInteractiveViews(
                point = point,
                excludeItemsWithBounds = textView.bounds
            ))?.let {
                // The text input view always returns self as a hit test result, regardless of whether
                // the pointer hits interactive elements within the view, such as selection handles.
                textView
            }
        } else {
            // On iOS <= 16 actual text interaction view is zero size.
            // Find it using hit testing over subviews.
            hitTestResult ?: textView.subviews.firstNotNullOfOrNull { subview ->
                subview as UIView
                val subviewPoint = this.convertPoint(point, toView = subview)
                subview.hitTest(subviewPoint, withEvent = withEvent)
            }
        }
    }

    /**
     * Updates frames and insets of the scroll view and underlying text view.
     *
     * Note on units:
     * - `dpNewFrame` and `dpTextBounds` are in Compose `Dp` (mapped 1:1 to iOS points via `asCGRect()`).
     * - `dpInsets` is in Compose `Dp` and can be passed to UIKit as POINTS directly.
     */
    fun setFrame(dpNewFrame: DpRect, dpTextBounds: DpRect, dpInsets: DpInsets) {
        val newFrame = dpNewFrame.asCGRect()
        val textBounds = dpTextBounds.asCGRect()

        val textViewFrame = CGRectMake(
            x = 0.0,
            y = 0.0,
            width = CGRectGetWidth(textBounds),
            height = CGRectGetHeight(textBounds)
        )

        val insets = UIEdgeInsetsMake(
            top = dpInsets.top.value.toDouble(),
            left = dpInsets.left.value.toDouble(),
            bottom = dpInsets.bottom.value.toDouble(),
            right = dpInsets.right.value.toDouble()
        )

        val scrollContentSize = textBounds.useContents { size.readValue() }
        val scrollContentInset = textBounds.useContents { origin.readValue() }

        val textFrameChanged =
            textView?.let { !CGRectEqualToRect(it.frame, textViewFrame) } ?: false
        val frameChanged = !CGRectEqualToRect(frame, newFrame)
        val contentInsetChanged = !UIEdgeInsetsEqualToEdgeInsets(contentInset, insets)
        val contentSizeChanged = !CGSizeEqualToSize(contentSize, scrollContentSize)
        val contentOffsetChanged = !CGPointEqualToPoint(contentOffset, scrollContentInset)

        val hasChanges = textFrameChanged ||
            frameChanged ||
            contentInsetChanged ||
            contentSizeChanged ||
            contentOffsetChanged

        if (hasChanges) {
            textView?.selectionWillChange()

            textView?.setFrame(textViewFrame)
            setFrame(newFrame)
            setContentInset(insets)
            setContentSize(scrollContentSize)
            setContentOffset(scrollContentInset)

            textView?.selectionDidChange()
        }
    }

    fun interactionModeAt(point: CValue<CGPoint>): UIKitInteropInteractionMode? {
        val selectionHandleOrCursor = hitTestTextInteractiveViews(
            point = point,
            excludeItemsWithBounds = textView?.bounds ?: CGRectZero.readValue()
        )

        return if (selectionHandleOrCursor != null && selectionHandleOrCursor != this) {
            UIKitInteropInteractionMode.NonCooperative
        } else if (CGRectContainsPoint(bounds, point)) {
            UIKitInteropInteractionMode.Cooperative(1000)
        } else {
            null
        }
    }
}

/**
 * The method traverses the text input view hierarchy, looking for interactive elements such as
 * selection handles or the cursor: usually these are only views that have different boundaries
 * from the text input view.
 * The method used to test interactive text editing elements when they are outside the boundaries
 * of the text input view.
 */
private fun UIView.hitTestTextInteractiveViews(
    point: CValue<CGPoint>,
    excludeItemsWithBounds: CValue<CGRect>,
    level: Int = 0
): UIView? {
    subviews.reversed().forEach { subview ->
        subview as UIView
        val subviewPoint = this.convertPoint(point, toView = subview)
        subview.hitTestTextInteractiveViews(subviewPoint, excludeItemsWithBounds, level + 1)?.let {
            return it
        }
    }
    return this.takeIf {
        !CGRectEqualToRect(bounds, excludeItemsWithBounds) &&
            CGRectContainsPoint(CGRectInset(bounds, -4.0, -4.0), point)
    }
}

private class TouchTrackingGestureRecognizer : CMPGestureRecognizer(target = null, action = null) {
    private val trackedTouches = mutableSetOf<UITouch>()

    val isTrackingTouches: Boolean get() = trackedTouches.isNotEmpty()

    init {
        cancelsTouchesInView = true
        delaysTouchesBegan = false
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent) {
        super.touchesBegan(touches, withEvent)

        touches.forEach {
            it as UITouch
            trackedTouches.add(it)
        }
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent) {
        super.touchesEnded(touches, withEvent)

        touches.forEach {
            it as UITouch
            trackedTouches.remove(it)
        }
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent) {
        super.touchesCancelled(touches, withEvent)

        touches.forEach {
            it as UITouch
            trackedTouches.remove(it)
        }
    }

    override fun canBePreventedByGestureRecognizer(
        preventingGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        return false
    }

    override fun canPreventGestureRecognizer(
        preventedGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        // Prevent other gesture recognizers so this one handles touches exclusively
        return true
    }
}

// Kotlin wrapper for UITextLayoutDirection
internal enum class PlatformTextLayoutDirection(val platform: UITextLayoutDirection) {
    Left(UITextLayoutDirectionLeft),
    Right(UITextLayoutDirectionRight),
    Up(UITextLayoutDirectionUp),
    Down(UITextLayoutDirectionDown);

    companion object {
        operator fun invoke(platform: UITextLayoutDirection): PlatformTextLayoutDirection? {
            return entries.find { it.platform == platform }
        }
    }
}