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
@file:Suppress("DEPRECATION_ERROR")
package androidx.ui.text

import androidx.compose.Composable
import androidx.compose.emptyContent
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.ClipboardManagerAmbient
import androidx.ui.core.DensityAmbient
import androidx.ui.core.FontLoaderAmbient
import androidx.ui.core.HapticFeedBackAmbient
import androidx.ui.core.Layout
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.TextInputServiceAmbient
import androidx.ui.core.TextToolbarAmbient
import androidx.ui.core.drawBehind
import androidx.ui.core.onPositioned
import androidx.ui.core.focus.FocusModifier
import androidx.ui.core.focus.FocusState
import androidx.ui.core.focus.focusState
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.LongPressDragObserver
import androidx.ui.core.gesture.dragGestureFilter
import androidx.ui.core.gesture.longPressDragGestureFilter
import androidx.ui.core.gesture.pressIndicatorGestureFilter
import androidx.ui.core.selection.SelectionLayout
import androidx.ui.core.semantics.semantics
import androidx.ui.geometry.Offset
import androidx.ui.graphics.drawscope.drawCanvas
import androidx.ui.input.EditProcessor
import androidx.ui.input.TextFieldValue
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.NO_SESSION
import androidx.ui.input.VisualTransformation
import androidx.ui.semantics.onClick
import androidx.ui.text.selection.SelectionHandle
import androidx.ui.text.selection.TextFieldSelectionManager
import kotlin.math.max
import kotlin.math.roundToInt

@Suppress("DEPRECATION")
@Composable
@Deprecated("Use the Composable with androidx.ui.input.TextFieldValue instead.")
@OptIn(InternalTextApi::class)
fun CoreTextField(
    value: androidx.ui.input.EditorValue,
    modifier: Modifier,
    onValueChange: (androidx.ui.input.EditorValue) -> Unit,
    textStyle: TextStyle = TextStyle.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onFocusChange: (Boolean) -> Unit = {},
    onImeActionPerformed: (ImeAction) -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {}
) {
    val fullModel = state { TextFieldValue() }
    if (fullModel.value.text != value.text ||
        fullModel.value.selection != value.selection ||
        fullModel.value.composition != value.composition) {
        @OptIn(InternalTextApi::class)
        fullModel.value = TextFieldValue(
            text = value.text,
            selection = value.selection.constrain(0, value.text.length),
            composition = value.composition?.constrain(0, value.text.length)
        )
    }

    val onValueChangeWrapper: (TextFieldValue) -> Unit = {
        fullModel.value = it
        onValueChange(
            androidx.ui.input.EditorValue(
                it.text,
                it.selection,
                it.composition
            )
        )
    }

    CoreTextField(
        value = fullModel.value,
        modifier = modifier,
        onValueChange = onValueChangeWrapper,
        textStyle = textStyle,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onFocusChange = onFocusChange,
        onImeActionPerformed = onImeActionPerformed,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        onTextInputStarted = onTextInputStarted
    )
}

/**
 * Base composable that enables users to edit text via hardware or software keyboard.
 *
 * This composable provides basic text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder.
 *
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state
 * represented by [TextFieldValue]. [TextFieldValue] contains the text entered by user, as well
 * as selection, cursor and text composition information. Please check [TextFieldValue] for the
 * description of its contents.
 *
 * It is crucial that the value provided in the [onValueChange] is fed back into [CoreTextField] in
 * order to have the final state of the text being displayed. Example usage:
 * @sample androidx.ui.text.samples.CoreTextFieldSample
 *
 * Please keep in mind that [onValueChange] is useful to be informed about the latest state of the
 * text input by users, however it is generally not recommended to modify the values in the
 * [TextFieldValue] that you get via [onValueChange] callback. Any change to the values in
 * [TextFieldValue] may result in a context reset and end up with input session restart. Such
 * a scenario would cause glitches in the UI or text input experience for users.
 *
 * @param value The [androidx.ui.input.TextFieldValue] to be shown in the [CoreTextField].
 * @param onValueChange Called when the input service updates the values in [TextFieldValue].
 * @param modifier optional [Modifier] for this text field.
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param keyboardType The keyboard type to be used in this text field. Note that this input type
 * is honored by IME and shows corresponding keyboard but this is not guaranteed. For example,
 * some IME may send non-ASCII character even if you set [KeyboardType.Ascii].
 * @param imeAction The IME action. This IME action is honored by IME and may show specific icons
 * on the keyboard. For example, search icon may be shown if [ImeAction.Search] is specified.
 * Then, when user tap that key, the [onImeActionPerformed] callback is called with specified
 * ImeAction.
 * @param onFocusChange Called with true value when the input field gains focus and with false
 * value when the input field loses focus. Use [FocusModifier.requestFocus] to obtain text input
 * focus to this TextField.
 * @param onImeActionPerformed Called when the input service requested an IME action. When the
 * input service emitted an IME action, this callback is called with the emitted IME action. Note
 * that this IME action may be different from what you specified in [imeAction].
 * @param visualTransformation The visual transformation filter for changing the visual
 * representation of the input. By default no visual transformation is applied.
 * @param onTextLayout Callback that is executed when a new text layout is calculated.
 * @param onTextInputStarted Callback that is executed when the initialization has done for
 * communicating with platform text input service, e.g. software keyboard on Android. Called with
 * [SoftwareKeyboardController] instance which can be used for requesting input show/hide software
 * keyboard.
 */
@Composable
@OptIn(InternalTextApi::class)
fun CoreTextField(
    value: TextFieldValue,
    modifier: Modifier = Modifier,
    onValueChange: (TextFieldValue) -> Unit,
    textStyle: TextStyle = TextStyle.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onFocusChange: (Boolean) -> Unit = {},
    onImeActionPerformed: (ImeAction) -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {}
) {
    // If developer doesn't pass new value to TextField, recompose won't happen but internal state
    // and IME may think it is updated. To fix this inconsistent state, enforce recompose by
    // incrementing generation counter when we callback to the developer and reset the state with
    // the latest state.
    val generation = state { 0 }
    val Wrapper: @Composable (Int, @Composable () -> Unit) -> Unit = { _, child -> child() }
    val onValueChangeWrapper: (TextFieldValue) -> Unit = { onValueChange(it); generation.value++ }

    Wrapper(generation.value) {
        // Ambients
        val textInputService = TextInputServiceAmbient.current
        val density = DensityAmbient.current
        val resourceLoader = FontLoaderAmbient.current

        // State
        val (visualText, offsetMap) = remember(value, visualTransformation) {
            val transformed = visualTransformation.filter(AnnotatedString(value.text))
            value.composition?.let {
                TextFieldDelegate.applyCompositionDecoration(it, transformed)
            } ?: transformed
        }
        val state = remember {
            TextFieldState(
                TextDelegate(
                    text = visualText,
                    style = textStyle,
                    density = density,
                    resourceLoader = resourceLoader
                )
            )
        }
        state.textDelegate = updateTextDelegate(
            current = state.textDelegate,
            text = visualText,
            style = textStyle,
            density = density,
            resourceLoader = resourceLoader,
            placeholders = emptyList()
        )

        // TODO: Stop lookup FocusModifier from modifier chain. (b/155434146)
        val focusModifier = chainedFocusModifier(modifier) ?: FocusModifier()

        state.processor.onNewState(value, textInputService, state.inputSession)

        val manager = remember { TextFieldSelectionManager() }
        manager.offsetMap = offsetMap
        manager.onValueChange = onValueChangeWrapper
        manager.state = state
        manager.value = value
        manager.clipboardManager = ClipboardManagerAmbient.current
        manager.textToolbar = TextToolbarAmbient.current
        manager.hapticFeedBack = HapticFeedBackAmbient.current

        val observer = textInputEventObserver(
            focusModifier = focusModifier,
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
                    onImeActionPerformed
                )
                if (state.inputSession != NO_SESSION && textInputService != null) {
                    onTextInputStarted(
                        SoftwareKeyboardController(
                            textInputService,
                            state.inputSession
                        )
                    )
                }
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
                onFocusChange(true)
            },
            onBlur = { hasNextClient ->
                state.hasFocus = false
                TextFieldDelegate.onBlur(
                    textInputService,
                    state.inputSession,
                    state.processor,
                    hasNextClient,
                    onValueChangeWrapper
                )
                onFocusChange(false)
            },
            onRelease = {
                if (state.selectionIsOn == false) {
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
            },
            state = state,
            longPressDragObserver = manager.longPressDragObserver,
            imeAction = imeAction
        )

        val drawModifier = Modifier.drawBehind {
            state.layoutResult?.let { layoutResult ->
                drawCanvas { canvas, _ ->
                    TextFieldDelegate.draw(
                        canvas,
                        value,
                        offsetMap,
                        layoutResult,
                        DefaultSelectionColor
                    )
                }
            }
        }

        val onPositionedModifier = Modifier.onPositioned {
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

        SelectionLayout(
            modifier
                .plus(observer)
                .plus(focusModifier)
                .plus(drawModifier)
                .plus(onPositionedModifier)
        ) {
            Layout(
                emptyContent(),
                Modifier
            ) { _, constraints ->
                TextFieldDelegate.layout(
                    state.textDelegate,
                    constraints,
                    layoutDirection,
                    state.layoutResult
                ).let { (width, height, result) ->
                    if (state.layoutResult != result) {
                        state.layoutResult = result
                        onTextLayout(result)
                    }
                    layout(
                        width,
                        height,
                        mapOf(
                            FirstBaseline to result.firstBaseline.roundToInt(),
                            LastBaseline to result.lastBaseline.roundToInt()
                        )
                    ) {}
                }
            }
            if (!value.selection.collapsed) {
                manager.state?.layoutResult?.let {
                    val startDirection = it.getBidiRunDirection(value.selection.start)
                    val endDirection = it.getBidiRunDirection(max(value.selection.end - 1, 0))
                    val directions = Pair(startDirection, endDirection)
                    SelectionHandle(
                        isStartHandle = true,
                        directions = directions,
                        manager = manager
                    )
                    SelectionHandle(
                        isStartHandle = false,
                        directions = directions,
                        manager = manager
                    )
                    manager.showSelectionToolbar()
                }
            }
        }
    }
}

@OptIn(InternalTextApi::class)
internal class TextFieldState(
    var textDelegate: TextDelegate
) {
    val processor = EditProcessor()
    var inputSession = NO_SESSION
    /**
     * This should be a state as every time we update the value we need to redraw it.
     * state observation during onDraw callback will make it work.
     */
    var hasFocus by mutableStateOf(false)
    /** The last layout coordinates for the Text's layout, used by selection */
    var layoutCoordinates: LayoutCoordinates? = null
    /** The latest TextLayoutResult calculated in the measure block */
    var layoutResult: TextLayoutResult? = null
    /**
     * The gesture detector status, to indicate whether current status is selection or editing.
     *
     * In the editing mode, there is no selection shown, only cursor is shown. To enter the editing
     * mode from selection mode, just tap on the screen.
     *
     * In the selection mode, there is no cursor shown, only selection is shown. To enter
     * the selection mode, just long press on the screen. In this mode, finger movement on the
     * screen changes selection instead of moving the cursor.
     */
    var selectionIsOn: Boolean = false
    /**
     * A flag to check if the selection start or end handle is being dragged.
     * If this value is true, then onPress will not select any text.
     * This value will be set to true when either handle is being dragged, and be reset to false
     * when the dragging is stopped.
     */
    var draggingHandle = false
}

private fun chainedFocusModifier(modifier: Modifier): FocusModifier? {
    var focusModifier: FocusModifier? = null
    modifier.foldIn(Unit) { _, element ->
        if (element is FocusModifier) {
            focusModifier = element
            return@foldIn
        }
    }
    return focusModifier
}

/**
 * Helper composable for observing all text input related events.
 */
@Composable
private fun textInputEventObserver(
    onPress: (Offset) -> Unit,
    onRelease: (Offset) -> Unit,
    onFocus: () -> Unit,
    state: TextFieldState,
    longPressDragObserver: LongPressDragObserver,
    onBlur: (hasNextClient: Boolean) -> Unit,
    focusModifier: FocusModifier,
    imeAction: ImeAction
): Modifier {
    val prevState = state { FocusState.NotFocused }
    if (focusModifier.focusState == FocusState.Focused &&
        prevState.value == FocusState.NotFocused
    ) {
        onFocus()
    }

    if (focusModifier.focusState == FocusState.NotFocused &&
        prevState.value == FocusState.Focused
    ) {
        onBlur(false) // TODO: Need to know if there is next focus element
    }

    prevState.value = focusModifier.focusState

    val doFocusIn = {
        if (focusModifier.focusState == FocusState.NotFocused) {
            focusModifier.requestFocus()
        }
    }

    onDispose {
        onBlur(false)
    }

    val semantics = Modifier.semantics {
        this.imeAction = imeAction
        this.supportsInputMethods()
        onClick(action = { doFocusIn(); return@onClick true })
    }
    val drag = Modifier.dragPositionGestureFilter(
        onPress = {
            state.selectionIsOn = false
            if (focusModifier.focusState == FocusState.Focused) {
                onPress(it)
            } else {
                doFocusIn()
            }
        },
        onRelease = onRelease,
        longPressDragObserver = longPressDragObserver
    )

    return semantics.plus(drag)
}

/**
 * Helper class for tracking dragging event.
 */
internal class DragEventTracker {
    private var origin = Offset.Zero
    private var distance = Offset.Zero

    /**
     * Restart the tracking from given origin.
     *
     * @param origin The origin of the drag gesture.
     */
    fun init(origin: Offset) {
        this.origin = origin
    }

    /**
     * Pass distance parameter called by DragGestureDetector$onDrag callback
     *
     * @param distance The distance from the origin of the drag origin.
     */
    fun onDrag(distance: Offset) {
        this.distance = distance
    }

    /**
     * Returns the current position.
     *
     * @return The position of the current drag point.
     */
    fun getPosition(): Offset {
        return origin + distance
    }
}

/**
 * Helper composable for tracking drag position.
 */
@Composable
private fun Modifier.dragPositionGestureFilter(
    onPress: (Offset) -> Unit,
    onRelease: (Offset) -> Unit,
    longPressDragObserver: LongPressDragObserver
): Modifier {
    val tracker = state { DragEventTracker() }
    // TODO(shepshapard): PressIndicator doesn't seem to be the right thing to use here.  It
    //  actually may be functionally correct, but might mostly suggest that it should not
    //  actually be called PressIndicator, but instead something else.

    return this
        .pressIndicatorGestureFilter(
            onStart = {
                tracker.value.init(it)
                onPress(it)
            }, onStop = {
                onRelease(tracker.value.getPosition())
            })
        .dragGestureFilter(dragObserver = object :
            DragObserver {
            override fun onDrag(dragDistance: Offset): Offset {
                tracker.value.onDrag(dragDistance)
                return Offset.Zero
            }
        })
        .longPressDragGestureFilter(longPressDragObserver)
}
