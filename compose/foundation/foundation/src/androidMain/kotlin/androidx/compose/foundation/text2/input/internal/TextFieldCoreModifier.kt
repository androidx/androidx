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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modifier element for the core functionality of [BasicTextField2] that is passed as inner
 * TextField to the decoration box. This is only half the actual modifiers for the field, the other
 * half are only attached to the decorated text field.
 *
 * This modifier mostly handles layout and draw.
 */
@OptIn(ExperimentalFoundationApi::class)
internal data class TextFieldCoreModifier(
    private val isFocused: Boolean,
    private val textLayoutState: TextLayoutState,
    private val textFieldState: TextFieldState,
    private val cursorBrush: Brush,
    private val writeable: Boolean,
    private val scrollState: ScrollState,
    private val orientation: Orientation,
) : ModifierNodeElement<TextFieldCoreModifierNode>() {

    override fun create(): TextFieldCoreModifierNode = TextFieldCoreModifierNode(
        isFocused = isFocused,
        textLayoutState = textLayoutState,
        textFieldState = textFieldState,
        cursorBrush = cursorBrush,
        writable = writeable,
        scrollState = scrollState,
        orientation = orientation,
    )

    override fun update(node: TextFieldCoreModifierNode) {
        node.updateNode(
            isFocused = isFocused,
            textLayoutState = textLayoutState,
            textFieldState = textFieldState,
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
    private var isFocused: Boolean,
    private var textLayoutState: TextLayoutState,
    private var textFieldState: TextFieldState,
    private var cursorBrush: Brush,
    private var writable: Boolean,
    private var scrollState: ScrollState,
    private var orientation: Orientation,
) : Modifier.Node(),
    LayoutModifierNode,
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    /**
     * Animatable object for cursor's alpha value. It becomes 1f for half a second and 0f for
     * another half a second when TextField is focused and editable. Initial value should be 0f
     * so that when cursor needs to be drawn for the first time, change to 1f invalidates draw.
     */
    private val cursorAlpha = Animatable(0f)

    /**
     * Whether to show cursor at all when TextField has focus. This depends on enabled, read only,
     * and brush at a given time.
     */
    private val showCursor: Boolean
        get() = writable && isFocused && cursorBrush.isSpecified

    /**
     * Observes the [textFieldState] for any changes to content or selection. If a change happens,
     * cursor blink animation gets reset.
     */
    private var changeObserverJob: Job? = null

    // TODO: kdoc
    private var previousSelection: TextRange = TextRange.Zero
    private var previousCursorRect: Rect = Rect.Zero

    /**
     * Updates all the related properties and invalidates internal state based on the changes.
     */
    fun updateNode(
        isFocused: Boolean,
        textLayoutState: TextLayoutState,
        textFieldState: TextFieldState,
        cursorBrush: Brush,
        writeable: Boolean,
        scrollState: ScrollState,
        orientation: Orientation,
    ) {
        val wasFocused = this.isFocused
        val previousTextFieldState = this.textFieldState

        this.isFocused = isFocused
        this.textLayoutState = textLayoutState
        this.textFieldState = textFieldState
        this.cursorBrush = cursorBrush
        this.writable = writeable
        this.scrollState = scrollState
        this.orientation = orientation

        if (!showCursor) {
            changeObserverJob?.cancel()
            changeObserverJob = null
            coroutineScope.launch { cursorAlpha.snapTo(0f) }
        } else if (!wasFocused || previousTextFieldState != textFieldState) {
            // this node is writeable, focused and gained that focus just now.
            // start the state value observation
            changeObserverJob = coroutineScope.launch {
                // Animate the cursor even when animations are disabled by the system.
                withContext(FixedMotionDurationScale) {
                    snapshotFlow { textFieldState.text }
                        .collectLatest {
                            // ensure that the value is always 1f _this_ frame by calling snapTo
                            cursorAlpha.snapTo(1f)
                            // then start the cursor blinking on animation clock (500ms on to start)
                            cursorAlpha.animateTo(0f, cursorAnimationSpec)
                        }
                }
            }
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
        val value = textFieldState.text
        val textLayoutResult = textLayoutState.layoutResult ?: return

        if (value.selectionInChars.collapsed) {
            drawText(textLayoutResult)
            drawCursor(value.selectionInChars, textLayoutResult)
        } else {
            drawSelection(value.selectionInChars, textLayoutResult)
            drawText(textLayoutResult)
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        textLayoutState.proxy?.innerTextFieldCoordinates = coordinates
    }

    private fun MeasureScope.measureVerticalScroll(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val currSelection = textFieldState.text.selectionInChars
        val offsetToFollow = when {
            currSelection.start != previousSelection.start -> currSelection.start
            currSelection.end != previousSelection.end -> currSelection.end
            else -> currSelection.min
        }
        previousSelection = currSelection

        // remove any height constraints for TextField since it'll be able to scroll vertically.
        val childConstraints = constraints.copy(maxHeight = Constraints.Infinity)
        val placeable = measurable.measure(childConstraints)
        // final height is the minimum of calculated or constrained maximum height.
        val height = min(placeable.height, constraints.maxHeight)

        return layout(placeable.width, height) {
            // we may need to update the scroll state to bring the cursor back into view after
            // layout is completed
            updateScrollState(
                cursorRect = getCursorRectInScroller(
                    cursorOffset = offsetToFollow,
                    textLayoutResult = textLayoutState.layoutResult,
                    rtl = layoutDirection == LayoutDirection.Rtl,
                    textFieldWidth = placeable.width
                ),
                containerSize = height,
                textFieldSize = placeable.height
            )

            placeable.placeRelative(0, -scrollState.value)
        }
    }

    private fun MeasureScope.measureHorizontalScroll(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val value = textFieldState.text
        val offsetToFollow = when {
            value.selectionInChars.start != previousSelection.start -> value.selectionInChars.start
            value.selectionInChars.end != previousSelection.end -> value.selectionInChars.end
            else -> value.selectionInChars.min
        }
        previousSelection = value.selectionInChars

        // If the maxIntrinsicWidth of the children is already smaller than the constraint, pass
        // the original constraints so that the children has more information to determine its
        // size.
        val maxIntrinsicWidth = measurable.maxIntrinsicWidth(constraints.maxHeight)

        // remove any width constraints for TextField since it'll be able to scroll horizontally.
        val childConstraints = if (maxIntrinsicWidth < constraints.maxWidth) {
            constraints
        } else {
            constraints.copy(maxWidth = Constraints.Infinity)
        }
        val placeable = measurable.measure(childConstraints)
        val width = min(placeable.width, constraints.maxWidth)

        return layout(width, placeable.height) {
            // we may need to update the scroll state to bring the cursor back into view before
            // layout is updated.
            updateScrollState(
                cursorRect = getCursorRectInScroller(
                    cursorOffset = offsetToFollow,
                    textLayoutResult = textLayoutState.layoutResult,
                    rtl = layoutDirection == LayoutDirection.Rtl,
                    textFieldWidth = placeable.width
                ),
                containerSize = width,
                textFieldSize = placeable.width
            )

            placeable.placeRelative(-scrollState.value, 0)
        }
    }

    /**
     * Updates the scroll state to make sure cursor is visible after text content, selection, or
     * layout changes. Only scroll changes won't trigger this.
     *
     * @param cursorRect Rectangle area to bring into view
     * @param containerSize Either height or width of scrollable host, depending on scroll
     * orientation
     * @param textFieldSize Either height or width of scrollable text field content, depending on
     * scroll orientation
     */
    private fun updateScrollState(
        cursorRect: Rect,
        containerSize: Int,
        textFieldSize: Int,
    ) {
        // update the maximum scroll value
        val difference = textFieldSize - containerSize
        scrollState.maxValue = difference

        // if cursor is not showing, we don't have to update the state for cursor
        if (!showCursor) return

        // Check if cursor has actually changed its location
        if (cursorRect.left != previousCursorRect.left ||
            cursorRect.top != previousCursorRect.top) {
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
            previousCursorRect = cursorRect
            coroutineScope.launch {
                // this call will respect the earlier set maxValue
                // no need to coerce again.
                scrollState.scrollBy(offsetDifference)
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
    private fun DrawScope.drawCursor(
        selection: TextRange,
        textLayoutResult: TextLayoutResult
    ) {
        // Only draw cursor if it can be shown and its alpha is higher than 0f
        // Alpha is checked before showCursor purposefully to make sure that we read
        // cursorAlpha.value in draw phase. So, when the alpha value changes, draw phase
        // invalidates.
        if (cursorAlpha.value <= 0f || !showCursor) return

        val cursorAlphaValue = cursorAlpha.value.coerceIn(0f, 1f)
        if (cursorAlphaValue == 0f) return

        val cursorRect = textLayoutResult.getCursorRect(selection.start)
        val cursorWidth = DefaultCursorThickness.toPx()
        val cursorX = (cursorRect.left + cursorWidth / 2)
            .coerceAtMost(size.width - cursorWidth / 2)

        drawLine(
            cursorBrush,
            Offset(cursorX, cursorRect.top),
            Offset(cursorX, cursorRect.bottom),
            alpha = cursorAlphaValue,
            strokeWidth = cursorWidth
        )
    }
}

private val cursorAnimationSpec: AnimationSpec<Float> = infiniteRepeatable(
    animation = keyframes {
        durationMillis = 1000
        1f at 0
        1f at 499
        0f at 500
        0f at 999
    }
)

private val DefaultCursorThickness = 2.dp

/**
 * If brush has a specified color. It's possible that [SolidColor] contains [Color.Unspecified].
 */
private val Brush.isSpecified: Boolean
    get() = !(this is SolidColor && this.value.isUnspecified)

private object FixedMotionDurationScale : MotionDurationScale {
    override val scaleFactor: Float
        get() = 1f
}

/**
 * Finds the rectangle area that corresponds to the visible cursor.
 *
 * @param cursorOffset Index of where cursor is at
 * @param textLayoutResult Current text layout to look for cursor rect.
 * @param rtl True if layout direction is RightToLeft
 * @param textFieldWidth Total width of TextField composable
 */
private fun Density.getCursorRectInScroller(
    cursorOffset: Int,
    textLayoutResult: TextLayoutResult?,
    rtl: Boolean,
    textFieldWidth: Int
): Rect {
    val cursorRect = textLayoutResult?.getCursorRect(cursorOffset) ?: Rect.Zero
    val thickness = DefaultCursorThickness.roundToPx()

    val cursorLeft = if (rtl) {
        textFieldWidth - cursorRect.left - thickness
    } else {
        cursorRect.left
    }

    val cursorRight = if (rtl) {
        textFieldWidth - cursorRect.left
    } else {
        cursorRect.left + thickness
    }
    return cursorRect.copy(left = cursorLeft, right = cursorRight)
}
