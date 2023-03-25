/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.text2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
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
internal data class TextFieldCoreModifierElement(
    private val isFocused: Boolean,
    private val textLayoutState: TextLayoutState,
    private val textFieldState: TextFieldState,
    private val cursorBrush: Brush,
    private val writeable: Boolean
) : ModifierNodeElement<TextFieldCoreModifierNode>() {

    override fun create(): TextFieldCoreModifierNode = TextFieldCoreModifierNode(
        isFocused = isFocused,
        textLayoutState = textLayoutState,
        textFieldState = textFieldState,
        cursorBrush = cursorBrush,
        writable = writeable
    )

    override fun update(node: TextFieldCoreModifierNode): TextFieldCoreModifierNode {
        node.updateNode(
            isFocused = isFocused,
            textLayoutState = textLayoutState,
            textFieldState = textFieldState,
            cursorBrush = cursorBrush,
            writeable = writeable
        )
        return node
    }

    override fun InspectorInfo.inspectableProperties() {
        // no inspector info
    }
}

/** Modifier node for [TextFieldCoreModifierElement]. */
@OptIn(ExperimentalFoundationApi::class)
internal class TextFieldCoreModifierNode(
    private var isFocused: Boolean,
    private var textLayoutState: TextLayoutState,
    private var textFieldState: TextFieldState,
    private var cursorBrush: Brush,
    private var writable: Boolean
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {

    /**
     * Animatable object for cursor's alpha value. Cursor is always drawn, only its alpha gets
     * animated.
     */
    private val cursorAlpha = Animatable(1f)

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

    /**
     * Updates all the related properties and invalidates internal state based on the changes.
     */
    fun updateNode(
        isFocused: Boolean,
        textLayoutState: TextLayoutState,
        textFieldState: TextFieldState,
        cursorBrush: Brush,
        writeable: Boolean
    ) {
        val wasFocused = this.isFocused
        val previousTextFieldState = this.textFieldState

        this.isFocused = isFocused
        this.textLayoutState = textLayoutState
        this.textFieldState = textFieldState
        this.cursorBrush = cursorBrush
        this.writable = writeable

        if (!showCursor) {
            changeObserverJob?.cancel()
            changeObserverJob = null
        } else if (!wasFocused || previousTextFieldState != textFieldState) {
            // this node is writeable, focused and gained that focus just now.
            // start the state value observation
            changeObserverJob = coroutineScope.launch {
                // Animate the cursor even when animations are disabled by the system.
                withContext(FixedMotionDurationScale) {
                    snapshotFlow { textFieldState.value }
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

    override fun ContentDrawScope.draw() {
        drawContent()
        val value = textFieldState.value
        val textLayoutResult = textLayoutState.layoutResult ?: return

        if (value.selection.collapsed) {
            drawText(textLayoutResult)
            drawCursor(value.selection, textLayoutResult)
        } else {
            drawSelection(value.selection, textLayoutResult)
            drawText(textLayoutResult)
        }
    }

    /**
     * Draws the selection highlight.
     */
    private fun ContentDrawScope.drawSelection(
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
    private fun ContentDrawScope.drawText(textLayoutResult: TextLayoutResult) {
        drawIntoCanvas { canvas ->
            TextPainter.paint(canvas, textLayoutResult)
        }
    }

    /**
     * Draws the cursor indicator. Do not confuse it with cursor handle which is a popup that
     * carries the cursor movement gestures.
     */
    private fun ContentDrawScope.drawCursor(
        selection: TextRange,
        textLayoutResult: TextLayoutResult
    ) {
        if (!showCursor) return

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
