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

package androidx.compose.ui.text.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.DpInsets
import androidx.compose.ui.platform.NativeTextEditingDelegate
import androidx.compose.ui.platform.PlatformTextLayoutDirection
import androidx.compose.ui.platform.TextInputSelectionRect
import androidx.compose.ui.platform.UIKitNativeTextInputContextMenuCustomAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.platform.toUIColor
import androidx.compose.ui.scene.ComposeSceneFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.uikit.density
import androidx.compose.ui.uikit.utils.CMPEditMenuCustomAction
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.FocusedViewsList
import androidx.compose.ui.window.NativeTextInputScrollView
import androidx.compose.ui.window.NativeTextInputView
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView

internal class NativeTextInputConnection(
    updateView: () -> Unit,
    view: UIView,
    coroutineScope: CoroutineScope,
    focusedViewsList: FocusedViewsList?,
    focusManager: () -> ComposeSceneFocusManager?
) : TextInputConnection(
    updateView,
    view,
    coroutineScope,
    focusedViewsList,
    focusManager
), NativeTextEditingDelegate {
    private val scrollView by lazy { NativeTextInputScrollView() }

    override val textInputView = NativeTextInputView()

    override fun attachInputToView() {
        view.addSubview(scrollView)
        scrollView.textView = textInputView

        textInputView.input = this

        textInputView.resignFirstResponder()
        textInputView.becomeFirstResponder()
        setupTintColor()
        onViewGeometryUpdated()
    }

    override fun detachView() {
        // Out-of-bounds non-empty frame is required to hide text keyboard focus frame
        val outOfBoundsFrame = CGRectMake(-100000.0, 0.0, 1.0, 1.0)

        textInputView.input = null

        textInputView.let { textView ->
            textView.setFrame(outOfBoundsFrame)
            coroutineScope.launch {
                delay(CLEAR_FOCUS_DELAY)
                scrollView.textView = null
                textView.removeFromSuperview()
            }
        }
        scrollView.removeFromSuperview()
    }

    override fun stateWillChange(textChanged: Boolean, selectionChanged: Boolean) {
        if (textChanged) {
            textInputView.textWillChange()
        }
        if (selectionChanged) {
            textInputView.selectionWillChange()
        }
    }

    override fun stateDidChange(textChanged: Boolean, selectionChanged: Boolean) {
        if (textChanged) {
            textInputView.textDidChange()
        }
        if (selectionChanged) {
            textInputView.selectionDidChange()
        }
    }

    override fun onTextFieldValueUpdated(newValue: TextFieldValue) {
        super.onTextFieldValueUpdated(newValue)

        // textLayoutResult might be updated after the text fiend value change
        onViewGeometryUpdated()
    }

    private var currentContentBounds: Rect? = null
    private var currentContentInsets: DpInsets? = null

    override fun onViewGeometryUpdated() {
        val rect = textFieldRectInRoot ?: return
        val offset = unclippedTextOffsetInRoot ?: return
        // Since Compose content is rendered on a MetalView and the UITextInput-implementing
        // view is overlayed on top of it, we need to synchronize the Compose text
        // field with the IntermediateTextScrollView (which contains IntermediateUITextView)
        // to ensure native iOS text input controls
        // align correctly with the rendered text.
        val layoutResult = textLayoutResult ?: return

        val contentBounds = calculateContentBounds(layoutResult, rect, offset)
        currentContentBounds = contentBounds
        val contentInsets = calculateContentInsets(rect, contentBounds)
        currentContentInsets = contentInsets
        scrollView.setFrame(
            rect.toDpRect(view.density),
            contentBounds.toDpRect(view.density),
            contentInsets
        )
    }

    private fun calculateContentBounds(textLayoutResult: TextLayoutResult, textFieldFrame: Rect, unclippedTextPosition: Offset): Rect {
        val textSize = textLayoutResult.size.toSize()
        val contentBounds = Rect(
            offset = Offset(x = textFieldFrame.left - unclippedTextPosition.x, y = textFieldFrame.top - unclippedTextPosition.y),
            size = textSize
        )
        return contentBounds
    }

    private fun calculateContentInsets(textFieldFrame: Rect, contentBounds: Rect): DpInsets = with(view.density) {
        return DpInsets(
            left = max(0f, -contentBounds.left).toDp(),
            top = max(0f, -contentBounds.top).toDp(),
            right = max(0f, textFieldFrame.width - contentBounds.width + contentBounds.left).toDp(),
            bottom = max(
                0f,
                textFieldFrame.height - contentBounds.height + contentBounds.top
            ).toDp()
        )
    }

    override fun caretDpRectForPosition(position: Int): DpRect? {
        val text = currentTextFieldValue?.text ?: return null
        if (position < 0 || position > text.length) {
            return null
        }
        val currentTextLayoutResult = textLayoutResult ?: return null
        if (position > currentTextLayoutResult.multiParagraph.intrinsics.annotatedString.length) {
            return null
        }
        val rect = currentTextLayoutResult.getCursorRect(position)
        return rect.toDpRect(view.density).let {
            val halfWidth = cursorThickness / 2
            val center = (it.left + it.right) / 2
            it.copy(left = center - halfWidth, right = center + halfWidth)
        }
    }

    override fun selectionDpRectsForRange(range: TextRange): List<TextInputSelectionRect> {
        // Native selection rects are required for correct work of the text editing menu
        // Without them, it will be impossible to call the text editing menu by tapping on the selected area
        if (range.collapsed || isIncorrect(range)) {
            return emptyList()
        }
        val currentTextLayoutResult = textLayoutResult ?: return emptyList()

        // Layout in native text input mode may be outdated, so not checking this may cause OOB error
        // This workaround should be deleted after https://youtrack.jetbrains.com/issue/CMP-9767/
        if (range.end > currentTextLayoutResult.multiParagraph.intrinsics.annotatedString.length) return emptyList()

        val startSelectionHandleRect = currentTextLayoutResult.getCursorRect(range.start)
        val endSelectionHandleRect = currentTextLayoutResult.getCursorRect(range.end)

        val firstLineNumber = currentTextLayoutResult.getLineForOffset(range.start)
        val lastLineNumber = currentTextLayoutResult.getLineForOffset(range.end)

        return if (firstLineNumber == lastLineNumber) {
            listOf(
                TextInputSelectionRect(
                    dpRect = Rect(
                        topLeft = startSelectionHandleRect.topLeft,
                        bottomRight = endSelectionHandleRect.bottomRight
                    ).toDpRect(view.density),
                    writingDirection = TextDirection.Content,
                    containsStart = true,
                    containsEnd = true,
                    isVertical = false
                )
            )
        } else {
            // TODO Consider RTL Layout
            // We require separate rects for start line, end line and everything in between them
            val contentInsets = currentContentInsets ?: return emptyList()
            val contentRect = currentContentBounds?.let {
                with(view.density) {
                    Rect(
                        top = it.top + contentInsets.top.toPx(),
                        left = it.left + contentInsets.left.toPx(),
                        right = it.right + contentInsets.right.toPx(),
                        bottom = it.bottom + contentInsets.bottom.toPx()
                    )
                }
            } ?: return emptyList()

            val firstLineSelectionRect = TextInputSelectionRect(
                dpRect = Rect(
                    top = startSelectionHandleRect.top,
                    left = startSelectionHandleRect.left,
                    right = contentRect.right,
                    bottom = startSelectionHandleRect.bottom
                ).toDpRect(view.density),
                writingDirection = TextDirection.Content,
                containsStart = true,
                containsEnd = false,
                isVertical = false
            )

            val middleAreaSelectionRect = TextInputSelectionRect(
                dpRect = Rect(
                    top = startSelectionHandleRect.bottom,
                    left = contentRect.left,
                    right = contentRect.right,
                    bottom = endSelectionHandleRect.top
                ).toDpRect(view.density),
                writingDirection = TextDirection.Content,
                containsStart = false,
                containsEnd = false,
                isVertical = false
            )

            val lastLineStartRect = currentTextLayoutResult.getCursorRect(
                currentTextLayoutResult.getLineStart(lastLineNumber)
            )
            val lastLineRect = TextInputSelectionRect(
                dpRect = Rect(
                    topLeft = lastLineStartRect.topLeft,
                    bottomRight = endSelectionHandleRect.bottomRight
                ).toDpRect(view.density),
                writingDirection = TextDirection.Content,
                containsStart = false,
                containsEnd = true,
                isVertical = false
            )

            listOf(
                firstLineSelectionRect,
                middleAreaSelectionRect,
                lastLineRect
            )
        }
    }

    override fun firstSelectionRectForRange(range: TextRange): DpRect? {
        if (range.collapsed || isIncorrect(range)) {
            return null
        }
        val currentTextLayoutResult = textLayoutResult ?: return null

        // Layout in native text input mode may be outdated, so not checking this may cause OOB error
        // This workaround should be deleted after https://youtrack.jetbrains.com/issue/CMP-9767/
        if (range.end > currentTextLayoutResult.multiParagraph.intrinsics.annotatedString.length) return null

        val startHandleLineNumber = currentTextLayoutResult.getLineForOffset(range.start)
        val endHandleLineNumber = currentTextLayoutResult.getLineForOffset(range.end)

        val startHandleRect = currentTextLayoutResult.getCursorRect(range.start)

        return if (startHandleLineNumber == endHandleLineNumber) {
            Rect(
                topLeft = startHandleRect.topLeft,
                bottomRight = currentTextLayoutResult.getCursorRect(range.end).bottomRight
            ).toDpRect(view.density)
        } else {
            val startLineNumber = currentTextLayoutResult.getLineForOffset(range.start)
            val startLineRight = currentTextLayoutResult.getLineRight(startLineNumber)
            Rect(
                startHandleRect.left,
                startHandleRect.top,
                startLineRight,
                startHandleRect.bottom
            ).toDpRect(view.density)
        }
    }

    override fun closestPositionToPoint(point: DpOffset): Int? {
        return textLayoutResult?.getOffsetForPosition(point.toOffset(view.density))
    }

    override fun closestPositionToPoint(point: DpOffset, withinRange: TextRange): Int? {
        val pointOffset =
            textLayoutResult?.getOffsetForPosition(point.toOffset(view.density))
                ?: return null
        return pointOffset.coerceIn(withinRange.start, withinRange.end)
    }

    override fun characterRangeAtPoint(point: DpOffset): TextRange? {
        val pointOffset =
            textLayoutResult?.getOffsetForPosition(point.toOffset(view.density))
                ?: return null
        return textLayoutResult?.getWordBoundary(pointOffset)
    }

    override fun positionWithinRange(
        range: TextRange,
        farthestInDirection: PlatformTextLayoutDirection
    ): Int? {
        if (isIncorrect(range)) return null
        return when (farthestInDirection) {
            PlatformTextLayoutDirection.Up -> range.start
            PlatformTextLayoutDirection.Down -> range.end
            else -> {
                val layout = textLayoutResult ?: return null
                val startLine = layout.getLineForOffset(range.start)
                val endLine = layout.getLineForOffset(range.end)

                val candidateOffsets = buildSet {
                    add(range.start)
                    add(range.end)

                    for (line in startLine..endLine) {
                        add(max(range.start, layout.getLineStart(line)))
                        add(min(range.end, layout.getLineEnd(line)))
                    }
                }

                when (farthestInDirection) {
                    PlatformTextLayoutDirection.Left ->
                        candidateOffsets.minByOrNull { layout.getHorizontalPosition(it, true) }
                    PlatformTextLayoutDirection.Right ->
                        candidateOffsets.maxByOrNull { layout.getHorizontalPosition(it, true) }
                    else -> null
                }
            }
        }
    }

    // If not specified, iOS would use the default system tint color
    private var selectionTintColor: Color? = null
    private fun setupTintColor() {
        textInputView.let {
            val uiColor = selectionTintColor?.toUIColor()
            it.setTintColor(uiColor)
        }
    }

    fun updateNativeTextInputEditMenuState(
        copy: (() -> Unit)?,
        paste: (() -> Unit)?,
        cut: (() -> Unit)?,
        selectAll: (() -> Unit)?,
        customActions: List<UIKitNativeTextInputContextMenuCustomAction>?
    ) {
        textInputView.updateMenuActions(
            copy,
            paste,
            cut,
            selectAll,
            customActions?.map { action ->
                CMPEditMenuCustomAction(action.title, action.action)
            } ?: emptyList()
        )
    }

    fun updateNativeTextInputTintColor(color: Color?) {
        selectionTintColor = color
        setupTintColor()
    }

    /**
     * Matches DefaultCursorThickness
     *
     * Must be at least 1.dp to make caret interactable
     */
    private val cursorThickness = 2.dp
}