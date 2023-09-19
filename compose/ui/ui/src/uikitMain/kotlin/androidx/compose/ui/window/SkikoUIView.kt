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

import androidx.compose.ui.interop.UIKitInteropAction
import androidx.compose.ui.interop.UIKitInteropTransaction
import androidx.compose.ui.platform.IOSSkikoInput
import androidx.compose.ui.platform.SkikoUITextInputTraits
import androidx.compose.ui.platform.TextActions
import kotlinx.cinterop.*
import org.jetbrains.skia.Rect
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.QuartzCore.CAMetalLayer
import platform.UIKit.*
import platform.darwin.NSInteger
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkikoInputModifiers
import org.jetbrains.skiko.SkikoKey
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoKeyboardEventKind
import org.jetbrains.skiko.SkikoPointer
import org.jetbrains.skiko.SkikoPointerDevice
import org.jetbrains.skiko.SkikoPointerEvent
import org.jetbrains.skiko.SkikoPointerEventKind


internal interface SkikoUIViewDelegate {
    fun onKeyboardEvent(event: SkikoKeyboardEvent)

    fun pointInside(point: CValue<CGPoint>, event: UIEvent?): Boolean

    fun onPointerEvent(event: SkikoPointerEvent)

    fun retrieveInteropTransaction(): UIKitInteropTransaction

    fun render(canvas: Canvas, targetTimestamp: NSTimeInterval)
}

@Suppress("CONFLICTING_OVERLOADS")
@ExportObjCClass
internal class SkikoUIView : UIView, UIKeyInputProtocol, UITextInputProtocol {
    companion object : UIViewMeta() {
        override fun layerClass() = CAMetalLayer
    }

    @Suppress("UNUSED") // required for Objective-C
    @OverrideInit
    constructor(coder: NSCoder) : super(coder) {
        throw UnsupportedOperationException("init(coder: NSCoder) is not supported for SkikoUIView")
    }

    var delegate: SkikoUIViewDelegate? = null
    var input: IOSSkikoInput? = null
    var inputTraits: SkikoUITextInputTraits = object : SkikoUITextInputTraits {}

    private val _device: MTLDeviceProtocol =
        MTLCreateSystemDefaultDevice() ?: throw IllegalStateException("Metal is not supported on this system")
    private val _metalLayer: CAMetalLayer get() = layer as CAMetalLayer
    private var _inputDelegate: UITextInputDelegateProtocol? = null
    private var _currentTextMenuActions: TextActions? = null
    private val _redrawer: MetalRedrawer = MetalRedrawer(
        _metalLayer,
        callbacks = object : MetalRedrawerCallbacks {
            override fun render(canvas: Canvas, targetTimestamp: NSTimeInterval) {
                delegate?.render(canvas, targetTimestamp)
            }

            override fun retrieveInteropTransaction(): UIKitInteropTransaction =
                delegate?.retrieveInteropTransaction() ?: UIKitInteropTransaction.empty

        }
    )

    /*
     * When there at least one tracked touch, we need notify redrawer about it. It should schedule CADisplayLink which
     * affects frequency of polling UITouch events on high frequency display and forces it to match display refresh rate.
     */
    private var _touchesCount = 0
        set(value) {
            field = value

            val needHighFrequencyPolling = value > 0

            _redrawer.needsProactiveDisplayLink = needHighFrequencyPolling
        }

    constructor() : super(frame = CGRectZero.readValue())

    init {
        multipleTouchEnabled = true

        _metalLayer.also {
            // Workaround for KN compiler bug
            // Type mismatch: inferred type is platform.Metal.MTLDeviceProtocol but objcnames.protocols.MTLDeviceProtocol? was expected
            @Suppress("USELESS_CAST")
            it.device = _device as objcnames.protocols.MTLDeviceProtocol?

            it.pixelFormat = MTLPixelFormatBGRA8Unorm
            doubleArrayOf(0.0, 0.0, 0.0, 0.0).usePinned { pinned ->
                it.backgroundColor = CGColorCreate(CGColorSpaceCreateDeviceRGB(), pinned.addressOf(0))
            }
            it.setOpaque(true)
            it.framebufferOnly = false
        }
    }

    fun needRedraw() = _redrawer.needRedraw()

    var isForcedToPresentWithTransactionEveryFrame by _redrawer::isForcedToPresentWithTransactionEveryFrame

    /**
     * Show copy/paste text menu
     * @param targetRect - rectangle of selected text area
     * @param textActions - available (not null) actions in text menu
     */
    fun showTextMenu(targetRect: Rect, textActions: TextActions) {
        _currentTextMenuActions = textActions
        val menu: UIMenuController = UIMenuController.sharedMenuController()
        val cgRect = CGRectMake(
            x = targetRect.left.toDouble(),
            y = targetRect.top.toDouble(),
            width = targetRect.width.toDouble(),
            height = targetRect.height.toDouble()
        )
        val isTargetVisible = CGRectIntersectsRect(bounds, cgRect)
        if (isTargetVisible) {
            if (menu.isMenuVisible()) {
                menu.setTargetRect(cgRect, this)
            } else {
                menu.showMenuFromView(this, cgRect)
            }
        } else {
            if (menu.isMenuVisible()) {
                menu.hideMenu()
            }
        }
    }

    fun hideTextMenu() {
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

    fun dispose() {
        _redrawer.dispose()
        removeFromSuperview()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()

        window?.screen?.let {
            contentScaleFactor = it.scale
            _redrawer.maximumFramesPerSecond = it.maximumFramesPerSecond
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()

        val scaledSize = bounds.useContents {
            CGSizeMake(size.width * contentScaleFactor, size.height * contentScaleFactor)
        }

        // If drawableSize is zero in any dimension it means that it's a first layout
        // we need to synchronously dispatch first draw and block until it's presented
        // so user doesn't have a flicker
        val needsSynchronousDraw = _metalLayer.drawableSize.useContents {
            width == 0.0 || height == 0.0
        }

        _metalLayer.drawableSize = scaledSize

        if (needsSynchronousDraw) {
            _redrawer.drawSynchronously()
        }
    }

    fun showScreenKeyboard() = becomeFirstResponder()
    fun hideScreenKeyboard() = resignFirstResponder()
    fun isScreenKeyboardOpen() = isFirstResponder

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

    override fun canBecomeFirstResponder() = true

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        if (withEvent != null) {
            for (press in withEvent.allPresses) {
                val uiPress = press as? UIPress
                if (uiPress != null) {
                    delegate?.onKeyboardEvent(
                        toSkikoKeyboardEvent(press, SkikoKeyboardEventKind.DOWN)
                    )
                }
            }
        }
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        if (withEvent != null) {
            for (press in withEvent.allPresses) {
                val uiPress = press as? UIPress
                if (uiPress != null) {
                    delegate?.onKeyboardEvent(
                        toSkikoKeyboardEvent(press, SkikoKeyboardEventKind.UP)
                    )
                }
            }
        }
        super.pressesEnded(presses, withEvent)
    }

    /**
     * https://developer.apple.com/documentation/uikit/uiview/1622533-point
     */
    override fun pointInside(point: CValue<CGPoint>, withEvent: UIEvent?): Boolean =
        delegate?.pointInside(point, withEvent) ?: super.pointInside(point, withEvent)


    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesBegan(touches, withEvent)

        _touchesCount += touches.size

        withEvent?.let {
            delegate?.onPointerEvent(it.toSkikoPointerEvent(SkikoPointerEventKind.DOWN))
        }
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesEnded(touches, withEvent)

        _touchesCount -= touches.size

        withEvent?.let {
            delegate?.onPointerEvent(it.toSkikoPointerEvent(SkikoPointerEventKind.UP))
        }
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesMoved(touches, withEvent)

        withEvent?.let {
            delegate?.onPointerEvent(it.toSkikoPointerEvent(SkikoPointerEventKind.MOVE))
        }
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesCancelled(touches, withEvent)

        _touchesCount -= touches.size

        withEvent?.let {
            delegate?.onPointerEvent(it.toSkikoPointerEvent(SkikoPointerEventKind.UP))
        }
    }

    private fun UIEvent.toSkikoPointerEvent(kind: SkikoPointerEventKind): SkikoPointerEvent {
        val pointers = touchesForView(this@SkikoUIView).orEmpty().map {
            val touch = it as UITouch
            val (x, y) = touch.locationInView(this@SkikoUIView).useContents { x to y }
            SkikoPointer(
                x = x,
                y = y,
                pressed = touch.isPressed,
                device = SkikoPointerDevice.TOUCH,
                id = touch.hashCode().toLong(),
                pressure = touch.force
            )
        }

        return SkikoPointerEvent(
            x = pointers.centroidX,
            y = pointers.centroidY,
            kind = kind,
            timestamp = (timestamp * 1_000).toLong(),
            pointers = pointers,
            platform = this
        )
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
    override fun textRangeFromPosition(fromPosition: UITextPosition, toPosition: UITextPosition): UITextRange? {
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
    override fun positionFromPosition(position: UITextPosition, offset: NSInteger): UITextPosition? {
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
    override fun comparePosition(position: UITextPosition, toPosition: UITextPosition): NSComparisonResult {
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
        val fromPosition = from as IntermediateTextPosition
        val to = toPosition as IntermediateTextPosition
        return to.position - fromPosition.position
    }

    override fun tokenizer(): UITextInputTokenizerProtocol = @Suppress("CONFLICTING_OVERLOADS") object : UITextInputStringTokenizer() {

        /**
         * Return whether a text position is at a boundary of a text unit of a specified granularity in a specified direction.
         * https://developer.apple.com/documentation/uikit/uitextinputtokenizer/1614553-isposition?language=objc
         * @param position
         * A text-position object that represents a location in a document.
         * @param granularity
         * A constant that indicates a certain granularity of text unit.
         * @param direction
         * A constant that indicates a direction relative to position. The constant can be of type UITextStorageDirection or UITextLayoutDirection.
         * @return
         * TRUE if the text position is at the given text-unit boundary in the given direction; FALSE if it is not at the boundary.
         */
        override fun isPosition(
            position: UITextPosition,
            atBoundary: UITextGranularity,
            inDirection: UITextDirection
        ): Boolean = TODO("implement isPosition")

        /**
         * Return whether a text position is within a text unit of a specified granularity in a specified direction.
         * https://developer.apple.com/documentation/uikit/uitextinputtokenizer/1614491-isposition?language=objc
         * @param position
         * A text-position object that represents a location in a document.
         * @param granularity
         * A constant that indicates a certain granularity of text unit.
         * @param direction
         * A constant that indicates a direction relative to position. The constant can be of type UITextStorageDirection or UITextLayoutDirection.
         * @return
         * TRUE if the text position is within a text unit of the specified granularity in the specified direction; otherwise, return FALSE.
         * If the text position is at a boundary, return TRUE only if the boundary is part of the text unit in the given direction.
         */
        override fun isPosition(
            position: UITextPosition,
            withinTextUnit: UITextGranularity,
            inDirection: UITextDirection
        ): Boolean = TODO("implement isPosition")

        /**
         * Return the next text position at a boundary of a text unit of the given granularity in a given direction.
         * https://developer.apple.com/documentation/uikit/uitextinputtokenizer/1614513-positionfromposition?language=objc
         * @param position
         * A text-position object that represents a location in a document.
         * @param granularity
         * A constant that indicates a certain granularity of text unit.
         * @param direction
         * A constant that indicates a direction relative to position. The constant can be of type UITextStorageDirection or UITextLayoutDirection.
         * @return
         * The next boundary position of a text unit of the given granularity in the given direction, or nil if there is no such position.
         */
        override fun positionFromPosition(
            position: UITextPosition,
            toBoundary: UITextGranularity,
            inDirection: UITextDirection
        ): UITextPosition? = null

        /**
         * Return the range for the text enclosing a text position in a text unit of a given granularity in a given direction.
         * https://developer.apple.com/documentation/uikit/uitextinputtokenizer/1614464-rangeenclosingposition?language=objc
         * @param position
         * A text-position object that represents a location in a document.
         * @param granularity
         * A constant that indicates a certain granularity of text unit.
         * @param direction
         * A constant that indicates a direction relative to position. The constant can be of type UITextStorageDirection or UITextLayoutDirection.
         * @return
         * A text-range representing a text unit of the given granularity in the given direction, or nil if there is no such enclosing unit.
         * Whether a boundary position is enclosed depends on the given direction, using the same rule as the isPosition:withinTextUnit:inDirection: method.
         */
        override fun rangeEnclosingPosition(
            position: UITextPosition,
            withGranularity: UITextGranularity,
            inDirection: UITextDirection
        ): UITextRange? {
            position as IntermediateTextPosition
            return input?.rangeEnclosingPosition(
                position = position.position.toInt(),
                withGranularity = withGranularity,
                inDirection = inDirection
            )?.toUITextRange()
        }

    }

    override fun positionWithinRange(range: UITextRange, atCharacterOffset: NSInteger): UITextPosition? =
        TODO("positionWithinRange range: $range, atCharacterOffset: $atCharacterOffset")

    override fun positionWithinRange(range: UITextRange, farthestInDirection: UITextLayoutDirection): UITextPosition? =
        TODO("positionWithinRange, farthestInDirection: ${farthestInDirection.directionToStr()}")

    override fun characterRangeByExtendingPosition(
        position: UITextPosition,
        inDirection: UITextLayoutDirection
    ): UITextRange? {
        TODO("characterRangeByExtendingPosition, inDirection: ${inDirection.directionToStr()}")
    }

    override fun baseWritingDirectionForPosition(
        position: UITextPosition,
        inDirection: UITextStorageDirection
    ): NSWritingDirection {
        return NSWritingDirectionLeftToRight // TODO support RTL text direction
    }

    override fun setBaseWritingDirection(writingDirection: NSWritingDirection, forRange: UITextRange) {
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
    }

    override fun selectionRectsForRange(range: UITextRange): List<*> = listOf<UITextSelectionRect>()
    override fun closestPositionToPoint(point: CValue<CGPoint>): UITextPosition? = null
    override fun closestPositionToPoint(point: CValue<CGPoint>, withinRange: UITextRange): UITextPosition? = null
    override fun characterRangeAtPoint(point: CValue<CGPoint>): UITextRange? = null

    override fun textStylingAtPosition(position: UITextPosition, inDirection: UITextStorageDirection): Map<Any?, *>? {
        return NSDictionary.dictionary()
    }

    override fun characterOffsetOfPosition(position: UITextPosition, withinRange: UITextRange): NSInteger {
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

    override fun canPerformAction(action: COpaquePointer?, withSender: Any?): Boolean =
        when (action) {
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

    override fun keyboardType(): UIKeyboardType = inputTraits.keyboardType()
    override fun keyboardAppearance(): UIKeyboardAppearance = inputTraits.keyboardAppearance()
    override fun returnKeyType(): UIReturnKeyType = inputTraits.returnKeyType()
    override fun textContentType(): UITextContentType? = inputTraits.textContentType()
    override fun isSecureTextEntry(): Boolean = inputTraits.isSecureTextEntry()
    override fun enablesReturnKeyAutomatically(): Boolean = inputTraits.enablesReturnKeyAutomatically()
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
        else -> "unknown direction"
    }

private val UITouch.isPressed
    get() =
        phase != UITouchPhase.UITouchPhaseEnded &&
            phase != UITouchPhase.UITouchPhaseCancelled

private val Iterable<SkikoPointer>.centroidX get() = asSequence().map { it.x }.average()
private val Iterable<SkikoPointer>.centroidY get() = asSequence().map { it.y }.average()

private fun toSkikoKeyboardEvent(
    event: UIPress,
    kind: SkikoKeyboardEventKind
): SkikoKeyboardEvent {
    val timestamp = (event.timestamp * 1_000).toLong()
    return SkikoKeyboardEvent(
        SkikoKey.valueOf(event.key!!.keyCode),
        toSkikoModifiers(event),
        kind,
        timestamp,
        event
    )
}

private fun toSkikoModifiers(event: UIPress): SkikoInputModifiers {
    var result = 0
    val modifiers = event.key!!.modifierFlags
    if (modifiers and UIKeyModifierAlternate != 0L) {
        result = result.or(SkikoInputModifiers.ALT.value)
    }
    if (modifiers and UIKeyModifierShift != 0L) {
        result = result.or(SkikoInputModifiers.SHIFT.value)
    }
    if (modifiers and UIKeyModifierControl != 0L) {
        result = result.or(SkikoInputModifiers.CONTROL.value)
    }
    if (modifiers and UIKeyModifierCommand != 0L) {
        result = result.or(SkikoInputModifiers.META.value)
    }
    return SkikoInputModifiers(result)
}