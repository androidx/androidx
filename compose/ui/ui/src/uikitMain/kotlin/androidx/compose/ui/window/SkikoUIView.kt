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

import androidx.compose.ui.platform.SkikoUITextInputTraits
import androidx.compose.ui.platform.TextActions
import kotlinx.cinterop.*
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.QuartzCore.CAMetalLayer
import platform.UIKit.*
import platform.darwin.NSInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.native.ref.WeakReference
import org.jetbrains.skiko.SkikoInputModifiers
import org.jetbrains.skiko.SkikoKey
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoKeyboardEventKind
import org.jetbrains.skiko.SkikoPointer
import org.jetbrains.skiko.SkikoPointerDevice
import org.jetbrains.skiko.SkikoPointerEvent
import org.jetbrains.skiko.SkikoPointerEventKind

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

    /**
     * Indicates that renderer should wait until the issued command buffer is scheduled for execution, so it can
     * present the drawable inside the CATransaction to sync it with UIKit changes
     *
     * See [relevant doc](https://developer.apple.com/documentation/quartzcore/cametallayer/1478157-presentswithtransaction?language=objc)
     */
    @Suppress("UNUSED") // Part of public API
    var presentsWithTransaction: Boolean
        get() = _metalLayer.presentsWithTransaction
        set(value) {
            _metalLayer.presentsWithTransaction = value
        }

    private val _device: MTLDeviceProtocol =
        MTLCreateSystemDefaultDevice() ?: throw IllegalStateException("Metal is not supported on this system")
    private val _metalLayer: CAMetalLayer get() = layer as CAMetalLayer
    private var _skiaLayer: IOSSkiaLayer? = null
    private var _pointInside: (Point, UIEvent?) -> Boolean = { _, _ -> true }
    private var _skikoUITextInputTrains: SkikoUITextInputTraits = object : SkikoUITextInputTraits {}
    private var _inputDelegate: UITextInputDelegateProtocol? = null
    private var _currentTextMenuActions: TextActions? = null
    private lateinit var _redrawer: MetalRedrawer

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

    init {
        multipleTouchEnabled = true
        opaque = false // For UIKit interop through a "Hole"

        _metalLayer.also {
            // Workaround for KN compiler bug
            // Type mismatch: inferred type is platform.Metal.MTLDeviceProtocol but objcnames.protocols.MTLDeviceProtocol? was expected
            @Suppress("USELESS_CAST")
            it.device = _device as objcnames.protocols.MTLDeviceProtocol?

            it.pixelFormat = MTLPixelFormatBGRA8Unorm
            doubleArrayOf(0.0, 0.0, 0.0, 0.0).usePinned { pinned ->
                it.backgroundColor = CGColorCreate(CGColorSpaceCreateDeviceRGB(), pinned.addressOf(0))
            }
            it.framebufferOnly = false
        }
    }

    // merging two constructors might cause a binary incompatibility, which will result in a unclear linking error,
    // if a project using newer compose depends on an older compose transitively
    // https://youtrack.jetbrains.com/issue/KT-60399
    @Suppress("UNUSED") // public API
    constructor(
        skiaLayer: IOSSkiaLayer,
        frame: CValue<CGRect> = CGRectNull.readValue(),
        pointInside: (Point, UIEvent?) -> Boolean = { _, _ -> true }
    ) : this(skiaLayer, frame, pointInside, skikoUITextInputTrains = object :
        SkikoUITextInputTraits {})

    constructor(
        skiaLayer: IOSSkiaLayer,
        frame: CValue<CGRect> = CGRectNull.readValue(),
        pointInside: (Point, UIEvent?) -> Boolean = { _, _ -> true },
        skikoUITextInputTrains: SkikoUITextInputTraits,
        drawCompletionCallback: () -> Unit = { }
    ) : super(frame) {
        _skiaLayer = skiaLayer
        _pointInside = pointInside
        _skikoUITextInputTrains = skikoUITextInputTrains


        // TODO: remove weak wrapper if we are not going to implement lifecycle-bound behavior
        val weakSkiaLayer = WeakReference(skiaLayer)

        _redrawer = MetalRedrawer(
            _metalLayer,
            drawCallback = { surface ->
                weakSkiaLayer.get()?.draw(surface)
            },
        )

        skiaLayer.needRedrawCallback = _redrawer::needRedraw
        skiaLayer.view = this
    }

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

    internal fun detach() {
        _redrawer.dispose()
    }

    fun load(): SkikoUIView {
        // TODO: redundant, remove in next refactor pass
        return this
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

        _metalLayer.drawableSize = scaledSize
    }

    fun showScreenKeyboard() = becomeFirstResponder()
    fun hideScreenKeyboard() = resignFirstResponder()
    fun isScreenKeyboardOpen() = isFirstResponder

    /**
     * A Boolean value that indicates whether the text-entry object has any text.
     * https://developer.apple.com/documentation/uikit/uikeyinput/1614457-hastext
     */
    override fun hasText(): Boolean {
        return _skiaLayer?.skikoView?.input?.hasText() ?: false
    }

    /**
     * Inserts a character into the displayed text.
     * Add the character text to your class’s backing store at the index corresponding to the cursor and redisplay the text.
     * https://developer.apple.com/documentation/uikit/uikeyinput/1614543-inserttext
     * @param text A string object representing the character typed on the system keyboard.
     */
    override fun insertText(text: String) {
        _skiaLayer?.skikoView?.input?.insertText(text)
    }

    /**
     * Deletes a character from the displayed text.
     * Remove the character just before the cursor from your class’s backing store and redisplay the text.
     * https://developer.apple.com/documentation/uikit/uikeyinput/1614572-deletebackward
     */
    override fun deleteBackward() {
        _skiaLayer?.skikoView?.input?.deleteBackward()
    }

    override fun canBecomeFirstResponder() = true

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        if (withEvent != null) {
            for (press in withEvent.allPresses) {
                val uiPress = press as? UIPress
                if (uiPress != null) {
                    _skiaLayer?.skikoView?.onKeyboardEvent(
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
                    _skiaLayer?.skikoView?.onKeyboardEvent(
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
    override fun pointInside(point: CValue<CGPoint>, withEvent: UIEvent?): Boolean {
        val skiaPoint: Point = point.useContents { Point(x.toFloat(), y.toFloat()) }
        return _pointInside(skiaPoint, withEvent)
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesBegan(touches, withEvent)

        _touchesCount += touches.size

        sendTouchEventToSkikoView(withEvent!!, SkikoPointerEventKind.DOWN)
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesEnded(touches, withEvent)

        _touchesCount -= touches.size

        sendTouchEventToSkikoView(withEvent!!, SkikoPointerEventKind.UP)
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesMoved(touches, withEvent)
        sendTouchEventToSkikoView(withEvent!!, SkikoPointerEventKind.MOVE)
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesCancelled(touches, withEvent)

        _touchesCount -= touches.size

        sendTouchEventToSkikoView(withEvent!!, SkikoPointerEventKind.UP)
    }

    private fun sendTouchEventToSkikoView(event: UIEvent, kind: SkikoPointerEventKind) {
        val pointers = event.touchesForView(this).orEmpty().map {
            val touch = it as UITouch
            val (x, y) = touch.locationInView(this).useContents { x to y }
            SkikoPointer(
                x = x,
                y = y,
                pressed = touch.isPressed,
                device = SkikoPointerDevice.TOUCH,
                id = touch.hashCode().toLong(),
                pressure = touch.force
            )
        }

        _skiaLayer?.skikoView?.onPointerEvent(
            SkikoPointerEvent(
                x = pointers.centroidX,
                y = pointers.centroidY,
                kind = kind,
                timestamp = (event.timestamp * 1_000).toLong(),
                pointers = pointers,
                platform = event
            )
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
        return _skiaLayer?.skikoView?.input?.textInRange(range.toIntRange())
    }

    /**
     * Replaces the text in a document that is in the specified range.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614558-replace
     * @param range A range of text in a document.
     * @param withText A string to replace the text in range.
     */
    override fun replaceRange(range: UITextRange, withText: String) {
        _skiaLayer?.skikoView?.input?.replaceRange(range.toIntRange(), withText)
    }

    override fun setSelectedTextRange(selectedTextRange: UITextRange?) {
        _skiaLayer?.skikoView?.input?.setSelectedTextRange(selectedTextRange?.toIntRange())
    }

    /**
     * The range of selected text in a document.
     * If the text range has a length, it indicates the currently selected text.
     * If it has zero length, it indicates the caret (insertion point).
     * If the text-range object is nil, it indicates that there is no current selection.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614541-selectedtextrange
     */
    override fun selectedTextRange(): UITextRange? {
        return _skiaLayer?.skikoView?.input?.getSelectedTextRange()?.toUITextRange()
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
        return _skiaLayer?.skikoView?.input?.markedTextRange()?.toUITextRange()
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
        _skiaLayer?.skikoView?.input?.setMarkedText(markedText, relativeTextRange)
    }

    /**
     * Unmarks the currently marked text.
     * After this method is called, the value of markedTextRange is nil.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614512-unmarktext
     */
    override fun unmarkText() {
        _skiaLayer?.skikoView?.input?.unmarkText()
    }

    override fun beginningOfDocument(): UITextPosition {
        return IntermediateTextPosition(0)
    }

    /**
     * The text position for the end of a document.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614555-endofdocument
     */
    override fun endOfDocument(): UITextPosition {
        return IntermediateTextPosition(_skiaLayer?.skikoView?.input?.endOfDocument() ?: 0)
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
     */
    override fun positionFromPosition(position: UITextPosition, offset: NSInteger): UITextPosition? {
        val p = (position as? IntermediateTextPosition)?.position ?: return null
        val endOfDocument = _skiaLayer?.skikoView?.input?.endOfDocument()
        return if (endOfDocument != null) {
            IntermediateTextPosition(max(min(p + offset, endOfDocument), 0))
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

    override fun tokenizer(): UITextInputTokenizerProtocol {
        return UITextInputStringTokenizer()
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
    override fun caretRectForPosition(position: UITextPosition): CValue<CGRect> = CGRectNull.readValue()
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

    override fun keyboardType(): UIKeyboardType = _skikoUITextInputTrains.keyboardType()
    override fun keyboardAppearance(): UIKeyboardAppearance = _skikoUITextInputTrains.keyboardAppearance()
    override fun returnKeyType(): UIReturnKeyType = _skikoUITextInputTrains.returnKeyType()
    override fun textContentType(): UITextContentType? = _skikoUITextInputTrains.textContentType()
    override fun isSecureTextEntry(): Boolean = _skikoUITextInputTrains.isSecureTextEntry()
    override fun enablesReturnKeyAutomatically(): Boolean = _skikoUITextInputTrains.enablesReturnKeyAutomatically()
    override fun autocapitalizationType(): UITextAutocapitalizationType =
        _skikoUITextInputTrains.autocapitalizationType()

    override fun autocorrectionType(): UITextAutocorrectionType = _skikoUITextInputTrains.autocorrectionType()

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