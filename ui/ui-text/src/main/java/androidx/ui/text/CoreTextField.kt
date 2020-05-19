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
package androidx.ui.text

import androidx.compose.Composable
import androidx.compose.emptyContent
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.DensityAmbient
import androidx.ui.core.FontLoaderAmbient
import androidx.ui.core.Layout
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.PassThroughLayout
import androidx.ui.core.TextInputServiceAmbient
import androidx.ui.core.drawBehind
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.dragGestureFilter
import androidx.ui.core.gesture.pressIndicatorGestureFilter
import androidx.ui.core.onPositioned
import androidx.ui.core.focus.FocusModifier
import androidx.ui.core.focus.FocusState
import androidx.ui.core.focus.focusState
import androidx.ui.graphics.drawscope.drawCanvas
import androidx.ui.input.EditProcessor
import androidx.ui.input.EditorValue
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.NO_SESSION
import androidx.ui.input.VisualTransformation
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.onClick
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import kotlin.math.roundToInt

/**
 * The common TextField implementation.
 */
@Composable
fun CoreTextField(
    value: EditorValue,
    modifier: Modifier,
    onValueChange: (EditorValue) -> Unit,
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
    val onValueChangeWrapper: (EditorValue) -> Unit = { onValueChange(it); generation.value++ }

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
        var focusModifier: FocusModifier? = null
        modifier.foldIn(Unit) { _, element ->
            if (element is FocusModifier) {
                focusModifier = element
                return@foldIn
            }
        }

        val updatedModifier = if (focusModifier == null) {
            modifier + FocusModifier().also { focusModifier = it }
        } else {
            modifier
        }

        state.processor.onNewState(value, textInputService, state.inputSession)
        TextInputEventObserver(
            focusModifier = focusModifier!!,
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
            },
            imeAction = imeAction
        ) {
            Layout(
                emptyContent(),
                updatedModifier.drawBehind {
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
                }.onPositioned {
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
            ) { _, constraints, layoutDirection ->
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
                            FirstBaseline to result.firstBaseline.roundToInt().ipx,
                            LastBaseline to result.lastBaseline.roundToInt().ipx
                        )
                    ) {}
                }
            }
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
     * state observation during onDraw callback will make it work.
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
    focusModifier: FocusModifier,
    imeAction: ImeAction,
    children: @Composable () -> Unit
) {
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

    Semantics(
        container = true,
        mergeAllDescendants = true,
        properties = {
            this.imeAction = imeAction
            this.supportsInputMethods = true
            onClick(action = { doFocusIn(); return@onClick true })
        }
    ) {
        val drag = Modifier.dragPositionGestureFilter(
            onPress = {
                if (focusModifier.focusState == FocusState.Focused) {
                    onPress(it)
                } else {
                    doFocusIn()
                }
            },
            onRelease = onRelease
        )

        // TODO(b/150706555): This layout is temporary and should be removed once Semantics
        //  is implemented with modifiers.
        @Suppress("DEPRECATION")
        PassThroughLayout(drag, children)
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
private fun Modifier.dragPositionGestureFilter(
    onPress: (PxPosition) -> Unit,
    onRelease: (PxPosition) -> Unit
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
            override fun onDrag(dragDistance: PxPosition): PxPosition {
                tracker.value.onDrag(dragDistance)
                return PxPosition.Origin
            }
        })
}
