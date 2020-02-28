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

import androidx.compose.Composable
import androidx.compose.mutableStateOf
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.PressGestureDetector
import androidx.ui.core.gesture.TouchSlopDragGestureDetector
import androidx.ui.core.input.FocusManager
import androidx.ui.input.EditProcessor
import androidx.ui.input.ImeAction
import androidx.ui.input.InputState
import androidx.ui.input.KeyboardType
import androidx.ui.input.NO_SESSION
import androidx.ui.input.VisualTransformation
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.onClick
import androidx.ui.text.TextDelegate
import androidx.ui.text.TextFieldValue
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.unit.PxPosition

/**
 * A user interface element for entering and modifying text.
 *
 * The TextField component renders an input and additional decorations set by input service
 * which is software keyboard in Android. Once input service modify the text, you will get callback
 * [onValueChange] with new text. Then, you can set this new text so that this component renders
 * up-to-date text from input service.
 *
 * Example usage:
 * @sample androidx.ui.framework.samples.StringTextFieldSample
 *
 * This is the most simple TextField that observes only text update and have control only for the
 * text. If you want to change/observe the selection/cursor location, you can use TextField with
 * [TextFieldValue] object.
 *
 * Note: Please be careful if you setting text other than the one passed to [onValueChange]
 * callback. Especially, it is not recommended to modify the text passed to [onValueChange]
 * callback. The text change may be translated to full context reset by input service and end up
 * with input session restart. This will be visible to users, for example, any ongoing composition
 * text will be cleared or committed, then software keyboard may go back to the default one.
 *
 * @param value The text to be shown in the [TextField]. If you want to specify cursor location or
 * selection range, use [TextField] with [TextFieldValue] instead.
 * @param onValueChange Called when the input service updates the text. When the input service
 * update the text, this callback is called with the updated text. If you want to observe the cursor
 * location or selection range, use [TextField] with [TextFieldValue] instead.
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param keyboardType The keyboard type to be used in this text field. Note that this input type
 * is honored by IME and shows corresponding keyboard but this is not guaranteed. For example,
 * some IME may send non-ASCII character even if you set [KeyboardType.Ascii].
 * @param imeAction The IME action. This IME action is honored by IME and may show specific icons
 * on the keyboard. For example, search icon may be shown if [ImeAction.Search] is specified.
 * Then, when user tap that key, the [onImeActionPerformed] callback is called with specified
 * ImeAction.
 * @param onFocus Called when the input field gains focus.
 * @param onBlur Called when the input field loses focus.
 * @param focusIdentifier Optional value to identify focus identifier. You can pass
 * [FocusManager.requestFocus] to this value to move focus to this TextField. This identifier
 * must be unique in your app. If you have duplicated identifiers, the behavior is undefined.
 * @param onImeActionPerformed Called when the input service requested an IME action. When the
 * input service emitted an IME action, this callback is called with the emitted IME action. Note
 * that this IME action may be different from what you specified in [imeAction].
 * @param visualTransformation Optional visual filter for changing visual output of input field.
 *
 * @see PasswordTextField
 * @see TextFieldValue
 * @see ImeAction
 * @see KeyboardType
 * @see VisualTransformation
 */
@Composable
fun TextField(
    value: String,
    modifier: Modifier = Modifier.None,
    onValueChange: (String) -> Unit = {},
    textStyle: TextStyle = TextStyle.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onFocus: () -> Unit = {},
    onBlur: () -> Unit = {},
    focusIdentifier: String? = null,
    onImeActionPerformed: (ImeAction) -> Unit = {},
    visualTransformation: VisualTransformation? = null
) {
    val fullModel = state { InputState() }
    if (fullModel.value.text != value) {
        val newSelection = TextRange(
            fullModel.value.selection.start.coerceIn(0, value.length),
            fullModel.value.selection.end.coerceIn(0, value.length)
        )
        fullModel.value = InputState(
            text = value,
            selection = newSelection
        )
    }

    BaseTextField(
        value = fullModel.value,
        modifier = modifier,
        onValueChange = {
            val prevValue = fullModel.value.text
            fullModel.value = it
            if (prevValue != it.text) {
                onValueChange(it.text)
            }
        },
        textStyle = textStyle,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onFocus = onFocus,
        onBlur = onBlur,
        focusIdentifier = focusIdentifier,
        onImeActionPerformed = onImeActionPerformed,
        visualTransformation = visualTransformation
    )
}

/**
 * A user interface element for entering and modifying text.
 *
 * The TextField component renders an input and additional decorations set by input service
 * which is software keyboard in Android. Once input service modify the text, you will get callback
 * [onValueChange] with new text. Then, you can set this new text so that this component renders
 * up-to-date text from input service.
 *
 * Example usage:
 * @sample androidx.ui.framework.samples.EditorModelTextFieldSample
 *
 * Note: Please be careful if you setting model other than the one passed to [onValueChange]
 * callback including selection or cursor. Especially, it is not recommended to modify the model
 * passed to [onValueChange] callback. Any change to text, selection or cursor may be translated to
 * full context reset by input service and end up with input session restart. This will be visible
 * to users, for example, any ongoing composition text will be cleared or committed, then software
 * keyboard may go back to the default one.
 *
 * @param value The [TextFieldValue] to be shown in the [TextField].
 * @param onValueChange Called when the input service updates the text, selection or cursor. When
 * the input service update the text, selection or cursor, this callback is called with the updated
 * [TextFieldValue]. If you want to observe the composition text, use [TextField] with
 * compositionRange instead.
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param keyboardType The keyboard type to be used in this text field. Note that this input type
 * is honored by IME and shows corresponding keyboard but this is not guaranteed. For example,
 * some IME may send non-ASCII character even if you set [KeyboardType.Ascii].
 * @param imeAction The IME action. This IME action is honored by IME and may show specific icons
 * on the keyboard. For example, search icon may be shown if [ImeAction.Search] is specified.
 * Then, when user tap that key, the [onImeActionPerformed] callback is called with specified
 * ImeAction.
 * @param onFocus Called when the input field gains focus.
 * @param onBlur Called when the input field loses focus.
 * @param focusIdentifier Optional value to identify focus identifier. You can pass
 * [FocusManager.requestFocus] to this value to move focus to this TextField. This identifier
 * must be unique in your app. If you have duplicated identifiers, the behavior is undefined.
 * @param onImeActionPerformed Called when the input service requested an IME action. When the
 * input service emitted an IME action, this callback is called with the emitted IME action. Note
 * that this IME action may be different from what you specified in [imeAction].
 * @param visualTransformation Optional visual filter for changing visual output of input field.
 *
 * @see TextFieldValue
 * @see ImeAction
 * @see KeyboardType
 * @see VisualTransformation
 */
@Composable
fun TextField(
    value: TextFieldValue,
    modifier: Modifier = Modifier.None,
    onValueChange: (TextFieldValue) -> Unit = {},
    textStyle: TextStyle = TextStyle.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onFocus: () -> Unit = {},
    onBlur: () -> Unit = {},
    focusIdentifier: String? = null,
    onImeActionPerformed: (ImeAction) -> Unit = {},
    visualTransformation: VisualTransformation? = null
) {
    val fullModel = state { InputState() }
    if (fullModel.value.text != value.text || fullModel.value.selection != value.selection) {
        val newSelection = TextRange(
            value.selection.start.coerceIn(0, value.text.length),
            value.selection.end.coerceIn(0, value.text.length)
        )
        fullModel.value = InputState(
            text = value.text,
            selection = newSelection
        )
    }

    BaseTextField(
        value = fullModel.value,
        modifier = modifier,
        onValueChange = {
            val prevState = fullModel.value
            fullModel.value = it
            if (prevState.text != it.text || prevState.selection != it.selection) {
                onValueChange(TextFieldValue(it.text, it.selection))
            }
        },
        textStyle = textStyle,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onFocus = onFocus,
        onBlur = onBlur,
        focusIdentifier = focusIdentifier,
        onImeActionPerformed = onImeActionPerformed,
        visualTransformation = visualTransformation
    )
}

/**
 * A user interface element for entering and modifying text.
 *
 * The TextField component renders an input and additional decorations set by input service
 * which is software keyboard in Android. Once input service modify the text, you will get callback
 * [onValueChange] with new text. Then, you can set this new text so that this component renders
 * up-to-date text from input service.
 *
 * Example usage:
 * @sample androidx.ui.framework.samples.CompositionEditorModelTextFieldSample
 *
 * It is not recommended to use this component unless you are interested in composition region.
 * The composition text is set by input service and you don't have control of it. If you modify
 * composition, the input service may confuse and restart new input session. Also please do not
 * expect no composition range at the beginning of input session. The input service may convert
 * existing text to composition text at the beginning of the input session.
 *
 * Note: Please be careful if you setting model other than the one passed to [onValueChange]
 * callback including selection or cursor. Especially, it is not recommended to modify the model
 * passed to [onValueChange] callback. Any change to text, selection or cursor may be translated to
 * full context reset by input service and end up with input session restart. This will be visible
 * to users, for example, any ongoing composition text will be cleared or committed, then software
 * keyboard may go back to the default one.
 *
 * @param model The [TextFieldValue] to be shown in the [TextField].
 * @param onValueChange Called when the input service updates the text, selection or cursor. When
 * the input service update the text, selection or cursor, this callback is called with the updated
 * [TextFieldValue].
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param keyboardType The keyboard type to be used in this text field. Note that this input type
 * is honored by IME and shows corresponding keyboard but this is not guaranteed. For example,
 * some IME may send non-ASCII character even if you set [KeyboardType.Ascii].
 * @param imeAction The IME action. This IME action is honored by IME and may show specific icons
 * on the keyboard. For example, search icon may be shown if [ImeAction.Search] is specified.
 * Then, when user tap that key, the [onImeActionPerformed] callback is called with specified
 * ImeAction.
 * @param onFocus Called when the input field gains focus.
 * @param onBlur Called when the input field loses focus.
 * @param focusIdentifier Optional value to identify focus identifier. You can pass
 * [FocusManager.requestFocus] to this value to move focus to this TextField. This identifier
 * must be unique in your app. If you have duplicated identifiers, the behavior is undefined.
 * @param onImeActionPerformed Called when the input service requested an IME action. When the
 * input service emitted an IME action, this callback is called with the emitted IME action. Note
 * that this IME action may be different from what you specified in [imeAction].
 * @param visualTransformation Optional visual filter for changing visual output of input field.
 *
 * @see TextFieldValue
 * @see ImeAction
 * @see KeyboardType
 * @see VisualTransformation
 */
@Composable
fun TextField(
    model: TextFieldValue,
    compositionRange: TextRange?,
    modifier: Modifier = Modifier.None,
    onValueChange: (TextFieldValue, TextRange?) -> Unit = { _, _ -> },
    textStyle: TextStyle = TextStyle.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onFocus: () -> Unit = {},
    onBlur: () -> Unit = {},
    focusIdentifier: String? = null,
    onImeActionPerformed: (ImeAction) -> Unit = {},
    visualTransformation: VisualTransformation? = null
) {
    BaseTextField(
        value = InputState(model.text, model.selection, compositionRange),
        modifier = modifier,
        onValueChange = { onValueChange(TextFieldValue(it.text, it.selection), it.composition) },
        textStyle = textStyle,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onFocus = onFocus,
        onBlur = onBlur,
        focusIdentifier = focusIdentifier,
        onImeActionPerformed = onImeActionPerformed,
        visualTransformation = visualTransformation
    )
}

/**
 * The common TextField implementation.
 */
@Composable
internal fun BaseTextField(
    value: InputState,
    modifier: Modifier,
    onValueChange: (InputState) -> Unit = { },
    textStyle: TextStyle = TextStyle.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onFocus: () -> Unit = {},
    onBlur: () -> Unit = {},
    focusIdentifier: String? = null,
    onImeActionPerformed: (ImeAction) -> Unit = {},
    visualTransformation: VisualTransformation? = null
) {
    // If developer doesn't pass new value to TextField, recompose won't happen but internal state
    // and IME may think it is updated. To fix this inconsistent state, enforce recompose by
    // incrementing generation counter when we callback to the developer and reset the state with
    // the latest state.
    val generation = state { 0 }
    val Wrapper: @Composable() (Int, @Composable() () -> Unit) -> Unit = { _, child -> child() }
    val onValueChangeWrapper: (InputState) -> Unit = { onValueChange(it); generation.value++ }

    Wrapper(generation.value) {
        // Ambients
        val style = CurrentTextStyleAmbient.current
        val textInputService = TextInputServiceAmbient.current
        val density = DensityAmbient.current
        val resourceLoader = FontLoaderAmbient.current
        val layoutDirection = LayoutDirectionAmbient.current

        // State
        val mergedStyle = style.merge(textStyle)
        val (visualText, offsetMap) = remember(value, visualTransformation) {
            val transformed = TextFieldDelegate.applyVisualFilter(value, visualTransformation)
            value.composition?.let {
                TextFieldDelegate.applyCompositionDecoration(it, transformed)
            } ?: transformed
        }
        val state = remember {
            TextFieldState(
                TextDelegate(
                    text = visualText,
                    style = mergedStyle,
                    density = density,
                    resourceLoader = resourceLoader,
                    layoutDirection = layoutDirection
                )
            )
        }
        state.textDelegate = updateTextDelegate(
            current = state.textDelegate,
            text = visualText,
            style = mergedStyle,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = layoutDirection
        )

        state.processor.onNewState(value, textInputService, state.inputSession)
        TextInputEventObserver(
            focusIdentifier = focusIdentifier,
            onPress = { },
            onFocus = {
                state.hasFocus = true
                state.inputSession = TextFieldDelegate.onFocus(
                    textInputService,
                    value,
                    state.processor,
                    keyboardType,
                    imeAction,
                    onValueChangeWrapper,
                    onImeActionPerformed)
                state.layoutCoordinates?.let { coords ->
                    textInputService?.let { textInputService ->
                        state.layoutResult?.let { layoutResult ->
                            TextFieldDelegate.notifyFocusedRect(
                                value,
                                state.textDelegate,
                                layoutResult,
                                coords,
                                textInputService,
                                state.inputSession,
                                state.hasFocus,
                                offsetMap
                            )
                        }
                    }
                }
                onFocus()
            },
            onBlur = { hasNextClient ->
                state.hasFocus = false
                TextFieldDelegate.onBlur(
                    textInputService,
                    state.inputSession,
                    state.processor,
                    hasNextClient,
                    onValueChangeWrapper)
                onBlur()
            },
            onRelease = {
                state.layoutResult?.let { layoutResult ->
                    TextFieldDelegate.onRelease(
                        it,
                        layoutResult,
                        state.processor,
                        offsetMap,
                        onValueChangeWrapper,
                        textInputService,
                        state.inputSession,
                        state.hasFocus
                    )
                }
            }
        ) {
            val textDrawModifier = draw { canvas, _ ->
                state.layoutResult?.let { layoutResult ->
                    TextFieldDelegate.draw(
                        canvas,
                        value,
                        offsetMap,
                        layoutResult,
                        state.hasFocus,
                        DefaultSelectionColor
                    )
                }
            }
            Layout(
                modifier = modifier + textDrawModifier,
                children = @Composable {
                    OnPositioned {
                        if (textInputService != null) {
                            state.layoutCoordinates = it
                            state.layoutResult?.let { layoutResult ->
                                TextFieldDelegate.notifyFocusedRect(
                                    value,
                                    state.textDelegate,
                                    layoutResult,
                                    it,
                                    textInputService,
                                    state.inputSession,
                                    state.hasFocus,
                                    offsetMap
                                )
                            }
                        }
                    }
                },
                measureBlock = { _, constraints, _ ->
                    TextFieldDelegate.layout(
                        state.textDelegate,
                        constraints,
                        state.layoutResult
                    ).let { (width, height, result) ->
                        state.layoutResult = result
                        layout(width, height) {}
                    }
                }
            )
        }
    }
}

private class TextFieldState(
    var textDelegate: TextDelegate
) {
    val processor = EditProcessor()
    var inputSession = NO_SESSION
    /**
     * This should be a state as every time we update the value we need to redraw it.
     * @Model observation during onDraw callback will make it work.
     */
    var hasFocus by mutableStateOf(false)
    /** The last layout coordinates for the Text's layout, used by selection */
    var layoutCoordinates: LayoutCoordinates? = null
    /** The latest TextLayoutResult calculated in the measure block */
    var layoutResult: TextLayoutResult? = null
}

/**
 * Helper composable for observing all text input related events.
 */
@Composable
private fun TextInputEventObserver(
    onPress: (PxPosition) -> Unit,
    onRelease: (PxPosition) -> Unit,
    onFocus: () -> Unit,
    onBlur: (hasNextClient: Boolean) -> Unit,
    focusIdentifier: String?,
    children: @Composable() () -> Unit
) {
    val focused = state { false }
    val focusManager = FocusManagerAmbient.current

    val focusNode = remember {
        val node = object : FocusManager.FocusNode {
            override fun onFocus() {
                onFocus()
                focused.value = true
            }

            override fun onBlur(hasNextClient: Boolean) {
                onBlur(hasNextClient)
                focused.value = false
            }
        }

        if (focusIdentifier != null)
            focusManager.registerFocusNode(focusIdentifier, node)

        node
    }

    onDispose {
        if (focused.value) {
            focusManager.blur(focusNode)
        }
        if (focusIdentifier != null)
            focusManager.unregisterFocusNode(focusIdentifier)
    }

    val doFocusIn = {
        if (!focused.value) {
            focusManager.requestFocus(focusNode)
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
            onRelease = onRelease,
            children = children
        )
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
    onRelease: (PxPosition) -> Unit,
    children: @Composable() () -> Unit
) {
    val tracker = state { DragEventTracker() }
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
                    return tracker.value.getPosition()
                }
            }, children = children)
    }
}
