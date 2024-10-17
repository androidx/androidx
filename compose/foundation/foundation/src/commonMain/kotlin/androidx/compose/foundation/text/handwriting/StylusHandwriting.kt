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

package androidx.compose.foundation.text.handwriting

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.isDeepPress
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusRequesterModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.TouchBoundsExpansion
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirstOrNull

/**
 * A modifier that detects stylus movements and calls the [onHandwritingSlopExceeded] when it
 * detects that stylus movement has exceeds the handwriting slop. If [onHandwritingSlopExceeded]
 * returns true, it will consume the events and consider that the handwriting has successfully
 * started. Otherwise, it'll stop monitoring the current gesture.
 *
 * @param enabled whether this modifier is enabled, it's used for the case where the editor is
 *   readOnly or disabled.
 * @param onHandwritingSlopExceeded the callback that's invoked when it detects stylus handwriting.
 *   And this modifier will consume the pointer events.
 */
internal fun Modifier.stylusHandwriting(
    enabled: Boolean,
    onHandwritingSlopExceeded: () -> Unit
): Modifier =
    if (enabled && isStylusHandwritingSupported) {
        this.then(StylusHandwritingElement(onHandwritingSlopExceeded))
    } else {
        this
    }

private data class StylusHandwritingElement(val onHandwritingSlopExceeded: () -> Unit) :
    ModifierNodeElement<StylusHandwritingNode>() {
    override fun create(): StylusHandwritingNode {
        return StylusHandwritingNode(onHandwritingSlopExceeded)
    }

    override fun update(node: StylusHandwritingNode) {
        node.onHandwritingSlopExceeded = onHandwritingSlopExceeded
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "stylusHandwriting"
        properties["onHandwritingSlopExceeded"] = onHandwritingSlopExceeded
    }
}

internal open class StylusHandwritingNode(var onHandwritingSlopExceeded: () -> Unit) :
    DelegatingNode(), PointerInputModifierNode, FocusEventModifierNode, FocusRequesterModifierNode {

    private var focused = false

    override fun onFocusEvent(focusState: FocusState) {
        focused = focusState.isFocused
    }

    override val touchBoundsExpansion: TouchBoundsExpansion
        get() {
            val result =
                with(requireDensity()) {
                    TouchBoundsExpansion(
                        start = HandwritingBoundsHorizontalOffset.roundToPx(),
                        top = HandwritingBoundsVerticalOffset.roundToPx(),
                        end = HandwritingBoundsHorizontalOffset.roundToPx(),
                        bottom = HandwritingBoundsVerticalOffset.roundToPx()
                    )
                }
            return result
        }

    private val suspendingPointerInputModifierNode =
        delegate(
            SuspendingPointerInputModifierNode {
                awaitEachGesture {
                    val firstDown =
                        awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Initial)

                    val isStylus =
                        firstDown.type == PointerType.Stylus || firstDown.type == PointerType.Eraser
                    if (!isStylus) {
                        return@awaitEachGesture
                    }

                    val isInBounds =
                        firstDown.position.x >= 0 &&
                            firstDown.position.x < size.width &&
                            firstDown.position.y >= 0 &&
                            firstDown.position.y < size.height

                    // If the editor is focused or the first down is within the editor's bounds, we
                    // await the initial pass. This prioritize the focused editor over unfocused
                    // editor.
                    val pass =
                        if (focused || isInBounds) {
                            PointerEventPass.Initial
                        } else {
                            PointerEventPass.Main
                        }

                    // Await the touch slop before long press timeout.
                    var exceedsTouchSlop: PointerInputChange? = null
                    // The stylus move must exceeds touch slop before long press timeout.
                    while (true) {
                        val pointerEvent = awaitPointerEvent(pass)
                        // The tracked pointer is consumed or lifted, stop tracking.
                        val change =
                            pointerEvent.changes.fastFirstOrNull {
                                !it.isConsumed && it.id == firstDown.id && it.pressed
                            }
                        if (change == null) {
                            break
                        }

                        val time = change.uptimeMillis - firstDown.uptimeMillis
                        if (time >= viewConfiguration.longPressTimeoutMillis) {
                            break
                        }
                        if (pointerEvent.isDeepPress) {
                            break
                        }

                        val offset = change.position - firstDown.position
                        if (offset.getDistance() > viewConfiguration.handwritingSlop) {
                            exceedsTouchSlop = change
                            break
                        }
                    }

                    if (exceedsTouchSlop == null) {
                        return@awaitEachGesture
                    }

                    if (!focused) {
                        requestFocus()
                    }

                    onHandwritingSlopExceeded.invoke()
                    exceedsTouchSlop.consume()

                    // Consume the remaining changes of this pointer.
                    while (true) {
                        val pointerEvent = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val pointerChange =
                            pointerEvent.changes.fastFirstOrNull {
                                !it.isConsumed && it.id == firstDown.id && it.pressed
                            } ?: return@awaitEachGesture
                        pointerChange.consume()
                    }
                }
            }
        )

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        suspendingPointerInputModifierNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        suspendingPointerInputModifierNode.onCancelPointerInput()
    }

    fun resetPointerInputHandler() {
        suspendingPointerInputModifierNode.resetPointerInputHandler()
    }
}

/**
 * Whether the platform supports the stylus handwriting or not. This is for platform level support
 * and NOT for checking whether the IME supports handwriting.
 */
internal expect val isStylusHandwritingSupported: Boolean

/** The amount of the padding added to the handwriting bounds of an editor. */
internal val HandwritingBoundsVerticalOffset = 40.dp
internal val HandwritingBoundsHorizontalOffset = 10.dp
