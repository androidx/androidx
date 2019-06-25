/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.core

import androidx.compose.composer
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.memo
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.core.input.FocusManager
import androidx.ui.graphics.Color
import androidx.ui.input.EditProcessor
import androidx.ui.input.EditorState
import androidx.ui.painting.AnnotatedString
import androidx.ui.painting.TextPainter
import androidx.ui.painting.TextStyle

/**
 * Data class holding text display attributes used for editors.
 */
data class EditorStyle(
    /** The  editor text style */
    val textStyle: TextStyle? = null,

    /**
     * The composition background color
     *
     * @see EditorState.composition
     */
    val compositionColor: Color = Color(alpha = 0xFF, red = 0xB0, green = 0xE0, blue = 0xE6),

    /**
     *  The selection background color
     *
     *  @see EditorState.selection
     */
    // TODO(nona): share with Text.DEFAULT_SELECTION_COLOR
    val selectionColor: Color = Color(alpha = 0x66, red = 0x33, green = 0xB5, blue = 0xE5)
)

/**
 * A default implementation of InputField
 *
 * To make InputField work with platoform input service, you must keep the editor state and update
 * in [onValueChagne] callback.
 *
 * Example:
 *     var state = +state { EditorState() }
 *     InputField(
 *         value = state.value,
 *         onValueChange = { state.value = it })
 */
@Composable
fun InputField(
    /** Initial editor state value */
    value: EditorState,

    /** The editor style */
    editorStyle: EditorStyle,

    /** Called when the InputMethodService update the editor state */
    onValueChange: (EditorState) -> Unit = {},

    /** Called when the InputMethod requested an editor action */
    onEditorActionPerformed: (Any) -> Unit = {}, // TODO(nona): Define argument type

    /** Called when the InputMethod forwarded a key event */
    onKeyEventForwarded: (Any) -> Unit = {} // TODO(nona): Define argument type
) {
    val style = +ambient(CurrentTextStyleAmbient)
    val mergedStyle = style.merge(editorStyle.textStyle)

    val processor = +memo { EditProcessor() }
    processor.onNewState(value)

    // TODO(nona): Add parameter for text direction, softwrap, etc.
    val delegate = InputFieldDelegate(
        TextPainter(
            text = AnnotatedString(text = value.text),
            style = mergedStyle
        )
    )

    val textInputService = +ambient(TextInputServiceAmbient)
    TextInputEventObserver(
        onPress = { delegate.onPress(it) },
        onFocus = {
            textInputService?.startInput(
                initState = value,
                onEditCommand = {
                    onValueChange(processor.onEditCommands(it))
                },
                onEditorActionPerformed = onEditorActionPerformed,
                onKeyEventForwarded = onKeyEventForwarded
            )
        },
        onBlur = {
            textInputService?.stopInput()
        },
        onDragAt = { delegate.onDragAt(it) },
        onRelease = { delegate.onRelease(it) }
    ) {
        Layout(
            children = @Composable {
                Draw { canvas, _ ->
                    delegate.draw(canvas, value, editorStyle)
                }
            },
            layoutBlock = { _, constraints ->
                delegate.layout(constraints).let {
                    layout(it.first, it.second) {}
                }
            }
        )
    }
}

/**
 * Helper composable for observing all text input related events.
 */
@Composable
private fun TextInputEventObserver(
    onPress: (PxPosition) -> Unit,
    onDragAt: (PxPosition) -> Unit,
    onRelease: (PxPosition) -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
    @Children children: @Composable() () -> Unit
) {
    val focused = +state { false }
    val focusManager = +ambient(FocusManagerAmbient)

    DragPositionGestureDetector(
        onPress = {
            if (focused.value) {
                onPress(it)
            } else {
                focusManager.requestFocus(object : FocusManager.FocusNode {
                    override fun onFocus() {
                        onFocus()
                        focused.value = true
                    }

                    override fun onBlur() {
                        onBlur()
                        focused.value = false
                    }
                })
            }
        },
        onDragAt = onDragAt,
        onRelease = onRelease
    ) {
        children()
    }
}

/**
 * Helper class for tracking dragging event.
 */
internal class DragEventTracker {
    private var origin = PxPosition.Origin
    private var distance = PxPosition.Origin

    /**
     * Restart the tracking from given origin.
     *
     * @param origin The origin of the drag gesture.
     */
    fun init(origin: PxPosition) {
        this.origin = origin
    }

    /**
     * Pass distance parameter called by DragGestureDetector$onDrag callback
     *
     * @param distance The distance from the origin of the drag origin.
     */
    fun onDrag(distance: PxPosition) {
        this.distance = distance
    }

    /**
     * Returns the current position.
     *
     * @return The position of the current drag point.
     */
    fun getPosition(): PxPosition {
        return origin + distance
    }
}

/**
 * Helper composable for tracking drag position.
 */
@Composable
private fun DragPositionGestureDetector(
    onPress: (PxPosition) -> Unit,
    onDragAt: (PxPosition) -> Unit,
    onRelease: (PxPosition) -> Unit,
    @Children children: @Composable() () -> Unit
) {
    val tracker = +state { DragEventTracker() }
    PressGestureDetector(
        onPress = {
            tracker.value.init(it)
            onPress(it)
        },
        onRelease = { onRelease(tracker.value.getPosition()) }
    ) {
        DragGestureDetector(
            dragObserver = object : DragObserver {
                override fun onDrag(dragDistance: PxPosition): PxPosition {
                    tracker.value.onDrag(dragDistance)
                    onDragAt(tracker.value.getPosition())
                    return tracker.value.getPosition()
                }
            }
        ) {
            children()
        }
    }
}