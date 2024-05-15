/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextHighlightType
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.input.internal.selection.textFieldMagnifierNode
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.truncate
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Modifier element for the core functionality of [BasicTextField] that is passed as inner
 * TextField to the decoration box. This is only half the actual modifiers for the field, the other
 * half are only attached to the decorated text field.
 *
 * This modifier mostly handles layout and draw.
 */
internal data class TextFieldCoreModifier(
    private val isFocused: Boolean, /* true iff component is focused and the window in focus */
    private val isDragHovered: Boolean,
    private val textLayoutState: TextLayoutState,
    private val textFieldState: TransformedTextFieldState,
    private val textFieldSelectionState: TextFieldSelectionState,
    private val cursorBrush: Brush,
    private val writeable: Boolean,
    private val scrollState: ScrollState,
    private val orientation: Orientation,
) : ModifierNodeElement<TextFieldCoreModifierNode>() {

    override fun create(): TextFieldCoreModifierNode = TextFieldCoreModifierNode(
        isFocused = isFocused,
        isDragHovered = isDragHovered,
        textLayoutState = textLayoutState,
        textFieldState = textFieldState,
        textFieldSelectionState = textFieldSelectionState,
        cursorBrush = cursorBrush,
        writeable = writeable,
        scrollState = scrollState,
        orientation = orientation,
    )

    override fun update(node: TextFieldCoreModifierNode) {
        node.updateNode(
            isFocused = isFocused,
            isDragHovered = isDragHovered,
            textLayoutState = textLayoutState,
            textFieldState = textFieldState,
            textFieldSelectionState = textFieldSelectionState,
            cursorBrush = cursorBrush,
            writeable = writeable,
            scrollState = scrollState,
            orientation = orientation,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        // no inspector info
    }
}

/** Modifier node for [TextFieldCoreModifier]. */
@OptIn(ExperimentalFoundationApi::class)
internal class TextFieldCoreModifierNode(
    // true iff this component is focused and the window is focused
    private var isFocused: Boolean,
    private var isDragHovered: Boolean,
    private var textLayoutState: TextLayoutState,
    private var textFieldState: TransformedTextFieldState,
    private var textFieldSelectionState: TextFieldSelectionState,
    private var cursorBrush: Brush,
    private var writeable: Boolean,
    private var scrollState: ScrollState,
    private var orientation: Orientation,
) : DelegatingNode(),
    LayoutModifierNode,
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode,
    SemanticsModifierNode {

    /**
     * Animatable object for cursor's alpha value. It becomes 1f for half a second and 0f for
     * another half a second when TextField is focused and editable. Initial value should be 0f
     * so that when cursor needs to be drawn for the first time, change to 1f invalidates draw.
     */
    private val cursorAnimation = CursorAnimationState()

    /**
     * Whether to show cursor at all when TextField has focus. This depends on enabled, read only,
     * and brush at a given time.
     */
    private val showCursor: Boolean
        get() = writeable && (isFocused || isDragHovered) && cursorBrush.isSpecified

    /**
     * Observes the [textFieldState] for any changes to content or selection. If a change happens,
     * cursor blink animation gets reset.
     */
    private var changeObserverJob: Job? = null

    /**
     * When selection/cursor changes its position, it may go out of the visible area. When that
     * happens, ideally we would want to scroll the TextField to keep the changing handle in the
     * visible area. The following member variables keep track of the latest selection and cursor
     * positions that we have adjusted for. When we detect a change to both of them during the
     * layout phase, ScrollState gets adjusted.
     * The same is also true when text layout size changes. For example when a new line is entered
     * that makes the decoration box bigger, this first triggers a cursor change without updating
     * the layout values. In this case we cannot scroll to the new line because we don't know the
     * new size. Immediately after, the new layout size is reported but as far as we know, we
     * already reacted to the cursor change so we shouldn't scroll. Thus, it also makes sense to
     * check whether layout size is changed between calls to bring cursor into view.
     */
    private var previousSelection: TextRange? = null
    private var previousCursorRect: Rect = Rect(-1f, -1f, -1f, -1f)
    private var previousTextLayoutSize: Int = 0

    private val textFieldMagnifierNode = delegate(
        textFieldMagnifierNode(
            textFieldState = textFieldState,
            textFieldSelectionState = textFieldSelectionState,
            textLayoutState = textLayoutState,
            visible = isFocused || isDragHovered
        )
    )

    override fun onAttach() {
        // if the attributes are right during onAttach, start the cursor job immediately.
        // This is possible when BasicTextField2 decorator toggles innerTextField in-and-out of
        // composition.
        if (isFocused && showCursor) {
            startCursorJob()
        }
    }

    /**
     * Updates all the related properties and invalidates internal state based on the changes.
     */
    fun updateNode(
        isFocused: Boolean,
        isDragHovered: Boolean,
        textLayoutState: TextLayoutState,
        textFieldState: TransformedTextFieldState,
        textFieldSelectionState: TextFieldSelectionState,
        cursorBrush: Brush,
        writeable: Boolean,
        scrollState: ScrollState,
        orientation: Orientation,
    ) {
        val previousShowCursor = this.showCursor
        val wasFocused = this.isFocused
        val previousTextFieldState = this.textFieldState
        val previousTextLayoutState = this.textLayoutState
        val previousTextFieldSelectionState = this.textFieldSelectionState
        val previousScrollState = this.scrollState

        this.isFocused = isFocused
        this.isDragHovered = isDragHovered
        this.textLayoutState = textLayoutState
        this.textFieldState = textFieldState
        this.textFieldSelectionState = textFieldSelectionState
        this.cursorBrush = cursorBrush
        this.writeable = writeable
        this.scrollState = scrollState
        this.orientation = orientation

        textFieldMagnifierNode.update(
            textFieldState = textFieldState,
            textFieldSelectionState = textFieldSelectionState,
            textLayoutState = textLayoutState,
            visible = isFocused || isDragHovered
        )

        if (!showCursor) {
            changeObserverJob?.cancel()
            changeObserverJob = null
            cursorAnimation.cancelAndHide()
        } else if (!wasFocused ||
            previousTextFieldState != textFieldState ||
            !previousShowCursor
        ) {
            // this node is writeable, focused and gained that focus just now.
            // start the state value observation
            startCursorJob()
        }

        if (previousTextFieldState != textFieldState ||
            previousTextLayoutState != textLayoutState ||
            previousTextFieldSelectionState != textFieldSelectionState ||
            previousScrollState != scrollState) {
            invalidateMeasurement()
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ) = if (orientation == Orientation.Vertical) {
        measureVerticalScroll(measurable, constraints)
    } else {
        measureHorizontalScroll(measurable, constraints)
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val value = textFieldState.visualText
        val textLayoutResult = textLayoutState.layoutResult ?: return

        value.highlight?.let { drawHighlight(it, textLayoutResult) }
        if (value.selection.collapsed) {
            drawText(textLayoutResult)
            if (value.shouldShowSelection()) {
                drawCursor()
            }
        } else {
            if (value.shouldShowSelection()) {
                drawSelection(value.selection, textLayoutResult)
            }
            drawText(textLayoutResult)
        }

        with(textFieldMagnifierNode) { draw() }
    }

    private fun MeasureScope.measureVerticalScroll(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // remove any height constraints for TextField since it'll be able to scroll vertically.
        val childConstraints = constraints.copy(maxHeight = Constraints.Infinity)
        val placeable = measurable.measure(childConstraints)
        // final height is the minimum of calculated or constrained maximum height.
        val height = min(placeable.height, constraints.maxHeight)

        return layout(placeable.width, height) {
            // we may need to update the scroll state to bring the cursor back into view after
            // layout is completed.
            updateScrollState(
                containerSize = height,
                textLayoutSize = placeable.height,
                currSelection = textFieldState.visualText.selection,
                layoutDirection = layoutDirection
            )

            placeable.placeRelative(0, -scrollState.value)
        }
    }

    private fun MeasureScope.measureHorizontalScroll(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // remove any width constraints for TextField since it'll be able to scroll horizontally.
        val placeable = measurable.measure(constraints.copy(maxWidth = Constraints.Infinity))
        val width = min(placeable.width, constraints.maxWidth)

        return layout(width, placeable.height) {
            // we may need to update the scroll state to bring the cursor back into view before
            // layout is updated.
            updateScrollState(
                containerSize = width,
                textLayoutSize = placeable.width,
                currSelection = textFieldState.visualText.selection,
                layoutDirection = layoutDirection
            )

            placeable.placeRelative(-scrollState.value, 0)
        }
    }

    /**
     * Returns which offset to follow to bring into view.
     */
    private fun calculateOffsetToFollow(
        currSelection: TextRange,
        currTextLayoutSize: Int
    ): Int {
        return when {
            currSelection.end != previousSelection?.end -> currSelection.end
            currSelection.start != previousSelection?.start -> currSelection.start
            currTextLayoutSize != previousTextLayoutSize -> currSelection.start
            else -> -1
        }
    }

    /**
     * Updates the scroll state to make sure cursor is visible after text content, selection, or
     * layout changes. Only scroll changes won't trigger this.
     *
     * @param containerSize Either height or width of scrollable host, depending on scroll
     * orientation.
     * @param textLayoutSize Either height or width of scrollable text field content, depending on
     * scroll orientation.
     * @param currSelection The current selection to cache if this function ends up scrolling to
     * bring the cursor or selection into view.
     */
    private fun Density.updateScrollState(
        containerSize: Int,
        textLayoutSize: Int,
        currSelection: TextRange,
        layoutDirection: LayoutDirection
    ) {
        // update the maximum scroll value
        val difference = textLayoutSize - containerSize
        scrollState.maxValue = difference

        // figure out if and which offset is going to be scrolled into view
        val offsetToFollow = calculateOffsetToFollow(currSelection, textLayoutSize)

        // if the cursor is not showing or there's no offset to be followed, we can return early.
        if (offsetToFollow < 0 || !showCursor) return

        val layoutResult = textLayoutState.layoutResult ?: return

        val rawCursorRect = layoutResult.getCursorRect(
            offsetToFollow.coerceIn(0..layoutResult.layoutInput.text.length)
        )
        val cursorRect = getCursorRectInScroller(
            cursorRect = rawCursorRect,
            rtl = layoutDirection == LayoutDirection.Rtl,
            textLayoutSize = textLayoutSize
        )

        // Check if cursor's location or text layout size was changed compared to the previous run.
        if (cursorRect.left != previousCursorRect.left ||
            cursorRect.top != previousCursorRect.top ||
            textLayoutSize != previousTextLayoutSize) {
            val vertical = orientation == Orientation.Vertical
            val cursorStart = if (vertical) cursorRect.top else cursorRect.left
            val cursorEnd = if (vertical) cursorRect.bottom else cursorRect.right

            val startVisibleBound = scrollState.value
            val endVisibleBound = startVisibleBound + containerSize
            val offsetDifference = when {
                // make bottom/end of the cursor visible
                //
                // text box
                // +----------------------+
                // |                      |
                // |                      |
                // |          cursor      |
                // |             |        |
                // +-------------|--------+
                //               |
                //
                cursorEnd > endVisibleBound -> cursorEnd - endVisibleBound

                // in rare cases when there's not enough space to fit the whole cursor, prioritise
                // the bottom/end of the cursor
                //
                //             cursor
                // text box      |
                // +-------------|--------+
                // |             |        |
                // +-------------|--------+
                //               |
                //
                cursorStart < startVisibleBound && cursorEnd - cursorStart > containerSize ->
                    cursorEnd - endVisibleBound

                // make top/start of the cursor visible if there's enough space to fit the whole
                // cursor
                //
                //               cursor
                // text box       |
                // +--------------|-------+
                // |              |       |
                // |                      |
                // |                      |
                // |                      |
                // +----------------------+
                //
                cursorStart < startVisibleBound && cursorEnd - cursorStart <= containerSize ->
                    cursorStart - startVisibleBound

                // otherwise keep current offset
                else -> 0f
            }

            previousSelection = currSelection
            previousCursorRect = cursorRect
            previousTextLayoutSize = textLayoutSize

            // this call will respect the earlier set [scrollState.maxValue]
            // no need to coerce again.
            // prefer to use immediate dispatch instead of suspending scroll calls
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                scrollState.scrollBy(offsetDifference.roundToNext())
                // make sure to use the cursor rect from text layout since bringIntoView does its
                // own checks for RTL layouts.
                textLayoutState.bringIntoViewRequester.bringIntoView(rawCursorRect)
            }
        }
    }

    /**
     * Draws the selection highlight.
     */
    private fun DrawScope.drawSelection(
        selection: TextRange,
        textLayoutResult: TextLayoutResult
    ) {
        val start = selection.min
        val end = selection.max
        if (start != end) {
            val selectionBackgroundColor = currentValueOf(LocalTextSelectionColors)
                .backgroundColor
            val selectionPath = textLayoutResult.getPathForRange(start, end)
            drawPath(selectionPath, color = selectionBackgroundColor)
        }
    }

    private fun DrawScope.drawHighlight(
        highlight: Pair<TextHighlightType, TextRange>,
        textLayoutResult: TextLayoutResult
    ) {
        val (type, range) = highlight

        if (range.collapsed) return

        val highlightPath = textLayoutResult.getPathForRange(range.min, range.max)

        if (type == TextHighlightType.HandwritingDeletePreview) {
            // The handwriting delete gesture preview highlight should be the same color as the
            // text at 20% opacity.
            val brush = textLayoutResult.layoutInput.style.brush
            if (brush != null) {
                drawPath(highlightPath, brush = brush, alpha = 0.2f)
            } else {
                val textColor =
                    textLayoutResult.layoutInput.style.color.takeOrElse { Color.Black }
                val highlightBackgroundColor = textColor.copy(alpha = textColor.alpha * 0.2f)
                drawPath(highlightPath, color = highlightBackgroundColor)
            }
        } else {
            // The handwriting select gesture preview highlight should be the same color as the
            // regular select highlight.
            val highlightBackgroundColor =
                currentValueOf(LocalTextSelectionColors).backgroundColor
            drawPath(highlightPath, color = highlightBackgroundColor)
        }
    }

    /**
     * Draws the text content.
     */
    private fun DrawScope.drawText(textLayoutResult: TextLayoutResult) {
        drawIntoCanvas { canvas ->
            TextPainter.paint(canvas, textLayoutResult)
        }
    }

    /**
     * Draws the cursor indicator. Do not confuse it with cursor handle which is a popup that
     * carries the cursor movement gestures.
     */
    private fun DrawScope.drawCursor() {
        // Only draw cursor if it can be shown and its alpha is higher than 0f
        // Alpha is checked before showCursor purposefully to make sure that we read
        // cursorAlpha in draw phase. So, when the alpha value changes, draw phase invalidates.
        val cursorAlphaValue = cursorAnimation.cursorAlpha
        if (cursorAlphaValue == 0f || !showCursor) return

        val cursorRect = textFieldSelectionState.getCursorRect()

        drawLine(
            cursorBrush,
            cursorRect.topCenter,
            cursorRect.bottomCenter,
            alpha = cursorAlphaValue,
            strokeWidth = cursorRect.width
        )
    }

    /**
     * Starts a job in this node's [coroutineScope] that infinitely toggles cursor's visibility
     * as long as the window is focused. The job also restarts whenever the text changes so that
     * cursor visibility snaps back to "visible".
     */
    private fun startCursorJob() {
        changeObserverJob = coroutineScope.launch {
            // A flag to oscillate the reported isWindowFocused value in snapshotFlow.
            // Repeatedly returning true/false everytime snapshotFlow is re-evaluated breaks
            // the assumption that each re-evaluation would also trigger the collector. However,
            // snapshotFlow carries an implicit `distinctUntilChanged` logic that prevents
            // the propagation of update events. Instead we introduce a sign that changes each
            // time snapshotFlow is re-entered. true/false becomes 1/2 or -1/-2.
            // true = 1 = -1
            // false = 2 = -2
            // sign is either 1 or -1
            var sign = 1
            snapshotFlow {
                // Read the text state, so the animation restarts when the text or cursor
                // position change.
                textFieldState.visualText
                // Only animate the cursor when its window is actually focused. This also
                // disables the cursor animation when the screen is off.
                // TODO: b/335668644, snapshotFlow is invoking this block even after the coroutine
                // has been cancelled, and currentCoroutineContext().isActive is false
                val isWindowFocused = isAttached && currentValueOf(LocalWindowInfo).isWindowFocused

                ((if (isWindowFocused) 1 else 2) * sign).also { sign *= -1 }
            }.collectLatest { isWindowFocused ->
                if (isWindowFocused.absoluteValue == 1) {
                    cursorAnimation.snapToVisibleAndAnimate()
                }
            }
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.textLayoutState.coreNodeCoordinates = coordinates
        textFieldMagnifierNode.onGloballyPositioned(coordinates)
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        with(textFieldMagnifierNode) { applySemantics() }
    }
}

private val DefaultCursorThickness = 2.dp

/**
 * If brush has a specified color. It's possible that [SolidColor] contains [Color.Unspecified].
 */
private val Brush.isSpecified: Boolean
    get() = !(this is SolidColor && this.value.isUnspecified)

/**
 * Converts cursorRect in text layout coordinates to scroller coordinates by adding the default
 * cursor thickness and calculating the relative positioning caused by the layout direction.
 *
 * @param cursorRect Reported cursor rect by the text layout.
 * @param rtl True if layout direction is RightToLeft
 * @param textLayoutSize Total width of TextField composable
 */
private fun Density.getCursorRectInScroller(
    cursorRect: Rect,
    rtl: Boolean,
    textLayoutSize: Int
): Rect {
    val thickness = DefaultCursorThickness.roundToPx()

    val cursorLeft = if (rtl) {
        textLayoutSize - cursorRect.right
    } else {
        cursorRect.left
    }

    val cursorRight = if (rtl) {
        textLayoutSize - cursorRect.right + thickness
    } else {
        cursorRect.left + thickness
    }
    return cursorRect.copy(left = cursorLeft, right = cursorRight)
}

/**
 * Rounds a negative number to floor, and a positive number to ceil. This is essentially the
 * opposite of [truncate].
 */
private fun Float.roundToNext(): Float = when {
    this.isNaN() || this.isInfinite() -> this
    this > 0 -> ceil(this)
    else -> floor(this)
}
