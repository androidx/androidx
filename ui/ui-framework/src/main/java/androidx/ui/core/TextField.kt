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
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.memo
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.gesture.TouchSlopDragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.core.input.FocusManager
import androidx.ui.input.EditProcessor
import androidx.ui.input.EditorModel
import androidx.ui.input.EditorStyle
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.VisualTransformation
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.onClick
import androidx.ui.text.TextDelegate

/**
 * A default implementation of TextField
 *
 * To make TextField work with platoform input service, you must keep the editor state and update
 * in [onValueChange] callback.
 *
 * Example:
 *     var state = +state { EditorModel() }
 *     TextField(
 *         value = state.value,
 *         onValueChange = { state.value = it })
 */
@Composable
fun TextField(
    /** Initial editor state value */
    value: EditorModel,

    /** The editor style */
    editorStyle: EditorStyle? = null,

    /**
     * The keyboard type to be used in this text field.
     *
     * Note that this input type is honored by IME and shows corresponding keyboard but this is not
     * guaranteed. For example, some IME may send non-ASCII character even if you set
     * [KeyboardType.Ascii]
     */
    keyboardType: KeyboardType = KeyboardType.Text,

    /**
     * The IME action
     *
     * This IME action is honored by IME and may show specific icons on the keyboard. For example,
     * search icon may be shown if [ImeAction.Search] is specified. Then, when user tap that key,
     * the [onImeActionPerformed] callback is called with specified ImeAction.
     */
    imeAction: ImeAction = ImeAction.Unspecified,

    /** Called when the InputMethodService update the editor state */
    onValueChange: (EditorModel) -> Unit = {},

    /** Called when the input field gains focus. */
    onFocus: () -> Unit = {},

    /** Called when the input field loses focus. */
    onBlur: () -> Unit = {},

    /** Called when the InputMethod requested an IME action */
    onImeActionPerformed: (ImeAction) -> Unit = {},

    /**
     * Optional visual filter for changing visual output of input field.
     */
    visualTransformation: VisualTransformation? = null
) {
    // Ambients
    val style = +ambient(CurrentTextStyleAmbient)
    val textInputService = +ambient(TextInputServiceAmbient)
    val density = +ambient(DensityAmbient)
    val resourceLoader = +ambient(FontLoaderAmbient)
    val layoutDirection = +ambient(LayoutDirectionAmbient)

    // Memos
    val processor = +memo { EditProcessor() }
    val mergedStyle = style.merge(editorStyle?.textStyle)
    val (visualText, offsetMap) = +memo(value, visualTransformation) {
        TextFieldDelegate.applyVisualFilter(value, visualTransformation)
    }
    val textDelegate = +memo(visualText, mergedStyle, density, resourceLoader) {
        // TODO(nona): Add parameter softwrap, etc.
        TextDelegate(
            text = visualText,
            style = mergedStyle,
            density = density,
            layoutDirection = layoutDirection,
            resourceLoader = resourceLoader
        )
    }

    // States
    val hasFocus = +state { false }
    val coords = +state<LayoutCoordinates?> { null }

    processor.onNewState(value, textInputService)
    TextInputEventObserver(
        onPress = { },
        onFocus = {
            hasFocus.value = true
            TextFieldDelegate.onFocus(
                textInputService,
                value,
                processor,
                keyboardType,
                imeAction,
                onValueChange,
                onImeActionPerformed)
            coords.value?.let { coords ->
                textInputService?.let { textInputService ->
                    TextFieldDelegate.notifyFocusedRect(
                        value,
                        textDelegate,
                        coords,
                        textInputService,
                        hasFocus.value,
                        offsetMap
                    )
                }
            }
            onFocus()
        },
        onBlur = {
            hasFocus.value = false
            TextFieldDelegate.onBlur(
                textInputService,
                processor,
                onValueChange)
            onBlur()
        },
        onDragAt = { TextFieldDelegate.onDragAt(it) },
        onRelease = {
            TextFieldDelegate.onRelease(
                it,
                textDelegate,
                processor,
                offsetMap,
                onValueChange,
                textInputService,
                hasFocus.value)
        }
    ) {
        Layout(
            children = @Composable {
                OnPositioned {
                    if (textInputService != null) {
                        // TODO(nona): notify focused rect in onPreDraw equivalent callback for
                        //             supporting multiline text.
                        coords.value = it
                        TextFieldDelegate.notifyFocusedRect(
                            value,
                            textDelegate,
                            it,
                            textInputService,
                            hasFocus.value,
                            offsetMap
                        )
                    }
                }
                Draw { canvas, _ -> TextFieldDelegate.draw(
                    canvas,
                    value,
                    offsetMap,
                    textDelegate,
                    hasFocus.value,
                    editorStyle?.selectionColor) }
            },
            measureBlock = { _, constraints ->
                TextFieldDelegate.layout(textDelegate, constraints).let {
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
    children: @Composable() () -> Unit
) {
    val focused = +state { false }
    val focusManager = +ambient(FocusManagerAmbient)

    val doFocusIn = {
        if (!focused.value) {
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
    }

    Semantics(
        properties = {
            onClick(action = doFocusIn)
        }
    ) {
        DragPositionGestureDetector(
            onPress = {
                if (focused.value) {
                    onPress(it)
                } else {
                    doFocusIn()
                }
            },
            onDragAt = onDragAt,
            onRelease = onRelease
        ) {
            children()
        }
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
    children: @Composable() () -> Unit
) {
    val tracker = +state { DragEventTracker() }
    PressGestureDetector(
        onPress = {
            tracker.value.init(it)
            onPress(it)
        },
        onRelease = { onRelease(tracker.value.getPosition()) }
    ) {
        TouchSlopDragGestureDetector(
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
