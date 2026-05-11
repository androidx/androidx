/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.ui.platform.TextInputPosition
import androidx.compose.ui.platform.TextInputRange
import androidx.compose.ui.platform.TextInputStringTokenizer
import androidx.compose.ui.platform.PlatformTextLayoutDirection
import androidx.compose.ui.platform.NativeTextEditingDelegate
import androidx.compose.ui.platform.SkikoUITextInputTraits
import androidx.compose.ui.platform.toTextRange
import androidx.compose.ui.platform.toUITextRange
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.uikit.utils.CMPEditMenuCustomAction
import androidx.compose.ui.uikit.utils.CMPTextInputView
import androidx.compose.ui.uikit.utils.CMPGestureRecognizer
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.toCGRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
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
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UIScrollView
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextContentType
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
import platform.UIKit.UITouch
import platform.UIKit.UIView
import platform.UIKit.UIWritingToolsBehavior
import platform.UIKit.addInteraction
import platform.UIKit.systemBlueColor
import platform.darwin.NSInteger

internal class NativeTextInputView
    : CMPTextInputView(frame = CGRectZero.readValue()), UIKeyInputProtocol, UITextInputProtocol {

    var input: NativeTextEditingDelegate? = null

    private val inputTraits: SkikoUITextInputTraits
        get() = input?.inputTraits ?: EmptyInputTraits

    private var _inputDelegate: UITextInputDelegateProtocol? = null

    private val touchesTrackerGestureRecognizer = TouchTrackingGestureRecognizer().also {
        addGestureRecognizer(it)
    }

    init {
        clipsToBounds = false
    }

    override fun canBecomeFirstResponder() = true

    private val selectionInteraction =
        UITextInteraction.textInteractionForMode(UITextInteractionMode.UITextInteractionModeEditable)
            .also {
                it.setTextInput(this)
            }
    private var selectionInteractionAttached: Boolean = false

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        if (window != null && !selectionInteractionAttached) {
            // Ensure UIKit text interaction is attached early so that cursor and selection
            // handles can be created and shown when needed.
            this.addInteraction(selectionInteraction)
            selectionInteractionAttached = true
        }
    }

    override fun becomeFirstResponder(): Boolean {
        val isFirstResponder = this.isFirstResponder()
        val result = super.becomeFirstResponder()

        if (!isFirstResponder && this.isFirstResponder()) {
            this.addInteraction(selectionInteraction)
            this.activateTextInputInteractionIfNeeded()
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
        input?.onKeyboardPresses(presses)
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        input?.onKeyboardPresses(presses)
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
        if (input?.getSelectedTextRange() == range) { return }

        // iOS <= 16 does not update selection handles when selection changes from the keyboard
        // Posting an extra notification solves this issue
        val notifySelectionChanges = !available(OS.Ios to OSVersion(major = 17)) &&
            !touchesTrackerGestureRecognizer.isTrackingTouches
        if (notifySelectionChanges) {
            selectionWillChange()
        }
        input?.setSelectedTextRange(range)
        if (notifySelectionChanges) {
            selectionDidChange()
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
    ): NSInteger {
        val withinTextRange = withinRange.toTextRange() ?: return 0L
        val intermediatePosition = (position as? TextInputPosition)?.position ?: 0
        return (intermediatePosition - withinTextRange.start).toLong()
    }

    override fun positionWithinRangeAtCharacterOffset(
        range: UITextRange,
        atCharacterOffset: NSInteger
    ): UITextPosition {
        val fallback = TextInputPosition(0)
        val textRange = range.toTextRange() ?: return fallback
        return (textRange.start + atCharacterOffset.toInt()).takeIf { range.isValid() && it in textRange }
            ?.let { TextInputPosition(it) } ?: fallback
    }

    override fun positionWithinRangeFarthestInDirection(
        range: UITextRange,
        farthestInDirection: UITextLayoutDirection
    ): UITextPosition {
        val fallback = TextInputPosition(0)
        val textRange = range.toTextRange() ?: return fallback
        return PlatformTextLayoutDirection(farthestInDirection)?.let { direction ->
            input?.positionWithinRange(textRange, direction)?.let {
                TextInputPosition(it)
            }
        } ?: fallback
    }

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

    override fun firstRectForRange(range: UITextRange): CValue<CGRect> {
        val fallback = CGRectZero.readValue()
        val textRange = range.toTextRange() ?: return fallback
        return input?.firstSelectionRectForRange(textRange)?.toCGRect()
            ?: fallback
    }

    override fun caretRectForPosition(position: UITextPosition): CValue<CGRect> {
        val fallbackRect = CGRectMake(x = 1.0, y = 1.0, width = 0.0, height = 1.0)
        val position = (position as? TextInputPosition)?.position ?: return fallbackRect
        val caretDpRect = input?.caretDpRectForPosition(position)
        return caretDpRect?.toCGRect() ?: fallbackRect
    }

    override fun selectionRectsForRange(range: UITextRange): List<*> {
        val fallbackList = listOf<UITextSelectionRect>()
        val textRange = TextRange(
            start = (range.start as? TextInputPosition)?.position ?: return fallbackList,
            end = (range.end as? TextInputPosition)?.position ?: return fallbackList
        )
        return input?.selectionDpRectsForRange(textRange) ?: fallbackList
    }

    override fun closestPositionToPoint(point: CValue<CGPoint>): UITextPosition? {
        val closestPosition =
            input?.closestPositionToPoint(point.useContents { DpOffset(x.dp, y.dp) }) ?: return null
        return TextInputPosition(closestPosition)
    }

    override fun closestPositionToPoint(
        point: CValue<CGPoint>,
        withinRange: UITextRange
    ): UITextPosition? {
        val textRange = (withinRange as? TextInputRange)?.toTextRange() ?: return null
        val closestPosition = input?.closestPositionToPoint(
            point.useContents { DpOffset(x.dp, y.dp) },
            textRange
        ) ?: return null
        return TextInputPosition(closestPosition)
    }

    override fun characterRangeAtPoint(point: CValue<CGPoint>): UITextRange? {
        val characterRange =
            input?.characterRangeAtPoint(point.useContents { DpOffset(x.dp, y.dp) }) ?: return null
        return TextInputRange(characterRange.start, characterRange.end)
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

    private var onCopy: (() -> Unit)? = null
    private var onPaste: (() -> Unit)? = null
    private var onCut: (() -> Unit)? = null
    private var onSelectAll: (() -> Unit)? = null
    private var customActions: List<CMPEditMenuCustomAction> = emptyList()

    override fun copy(sender: Any?) {
        onCopy?.invoke()
    }

    override fun paste(sender: Any?) {
        onPaste?.invoke()
    }

    override fun cut(sender: Any?) {
        onCut?.invoke()
    }

    override fun selectAll(sender: Any?) {
        onSelectAll?.invoke()
    }

    override fun canPerformAction(action: COpaquePointer?, withSender: Any?): Boolean =
        when (NSStringFromSelector(action)) {
            "copy:" -> onCopy != null
            "paste:" -> onPaste != null
            "cut:" -> onCut != null
            "selectAll:" -> onSelectAll != null
            else -> super.canPerformAction(action, withSender)
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
        val customMenuElements = makeCustomMenuElements()
        if (customMenuElements.isEmpty()) return null // The default menu would be returned

        @Suppress("UNCHECKED_CAST")
        val suggestedActionsElements = suggestedActions as List<UIMenuElement>

        return UIMenu.menuWithTitle("", children = customMenuElements + suggestedActionsElements)
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

    private val _tokenizer = TextInputStringTokenizer(textInput = this) {
        input?.let { it.textInRange(TextRange(0, it.endOfDocument())) }
    }
    override fun tokenizer(): UITextInputTokenizerProtocol = _tokenizer

    private fun UITextRange.isValid(): Boolean {
        val range = this.toTextRange() ?: return false
        val textEndPos = input?.endOfDocument() ?: 0
        return range.start in 0..range.end && range.end <= textEndPos
    }
}

/**
 * A [UIScrollView] that hosts the [NativeTextInputView] when native input handling (Native Text Input)
 * is enabled.
 *
 * This container is necessary because:
 * 1. It provides a native coordinate system that matches the "full" (unclipped) text layout,
 *    allowing native iOS overlays (like the magnifier, floating cursor, and selection handles)
 *    to align correctly with the Compose-rendered text.
 * 2. It synchronizes its [contentOffset] and [contentInset] with the Compose text field's
 *    internal scroll state, ensuring that the UITextInput-conforming overlay view
 *    ([NativeTextInputView]) stays in sync with the visual rendering.
 * 3. It provides a larger hit-testing area for native interactive elements (like selection handles)
 *    that may overflow the immediate visual bounds of the text field.
 *
 * This view does not handle user-driven scrolling; scrolling is still managed by Compose,
 * which then updates this scroll view's state.
 */
internal class NativeTextInputScrollView: UIScrollView(frame = CGRectZero.readValue()) {
    init {
        setScrollEnabled(false)
        setShowsVerticalScrollIndicator(false)
        setShowsHorizontalScrollIndicator(false)
        setCanCancelContentTouches(false)
        setDelaysContentTouches(false)
        setClipsToBounds(true)
    }

    var textView: NativeTextInputView? = null
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
        val newFrame = dpNewFrame.toCGRect()
        val textBounds = dpTextBounds.toCGRect()

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