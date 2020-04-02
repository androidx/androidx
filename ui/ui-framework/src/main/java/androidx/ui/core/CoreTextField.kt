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
import androidx.compose.emptyContent
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.dragGestureFilter
import androidx.ui.core.gesture.pressIndicatorGestureFilter
import androidx.ui.core.input.FocusNode
import androidx.ui.input.EditProcessor
import androidx.ui.input.ImeAction
import androidx.ui.input.EditorValue
import androidx.ui.input.KeyboardType
import androidx.ui.input.NO_SESSION
import androidx.ui.input.VisualTransformation
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.onClick
import androidx.ui.text.TextDelegate
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextStyle
import androidx.ui.unit.PxPosition
import androidx.ui.unit.round

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
    onFocus: () -> Unit = {},
    onBlur: () -> Unit = {},
    focusIdentifier: String? = null,
    onImeActionPerformed: (ImeAction) -> Unit = {},
    visualTransformation: VisualTransformation? = null,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    // If developer doesn't pass new value to TextField, recompose won't happen but internal state
    // and IME may think it is updated. To fix this inconsistent state, enforce recompose by
    // incrementing generation counter when we callback to the developer and reset the state with
    // the latest state.
    val generation = state { 0 }
    val Wrapper: @Composable() (Int, @Composable() () -> Unit) -> Unit = { _, child -> child() }
    val onValueChangeWrapper: (EditorValue) -> Unit = { onValueChange(it); generation.value++ }

    Wrapper(generation.value) {
        // Ambients
        val textInputService = TextInputServiceAmbient.current
        val density = DensityAmbient.current
        val resourceLoader = FontLoaderAmbient.current

        // State
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
                    style = textStyle,
                    density = density,
                    resourceLoader = resourceLoader
                )
            )
        }
        state.textDelegate = updateTextDelegate(
            current = state.textDelegate,
            text = visualText,
            // TODO(143536715): TextField should use currentTextStyle() here, so we need a higher
            // level TextField
            style = textStyle,
            density = density,
            resourceLoader = resourceLoader
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
                    onImeActionPerformed
                )
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
                    onValueChangeWrapper
                )
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
            Layout(
                emptyContent(),
                modifier.drawBehind {
                    state.layoutResult?.let { layoutResult ->
                        TextFieldDelegate.draw(
                            this,
                            value,
                            offsetMap,
                            layoutResult,
                            state.hasFocus,
                            DefaultSelectionColor
                        )
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
                            FirstBaseline to result.firstBaseline.round(),
                            LastBaseline to result.lastBaseline.round()
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
        FocusNode().also {
            focusManager.registerObserver(it) { fromNode, toNode ->
                if (fromNode == it) { // Focus lost
                    onBlur(toNode != null)
                    focused.value = false
                } else { // Focus gain
                    onFocus()
                    focused.value = true
                }
            }
            if (focusIdentifier != null)
                focusManager.registerFocusNode(focusIdentifier, it)
        }
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
        val drag = Modifier.dragPositionGestureFilter(
            onPress = {
                if (focused.value) {
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
                return tracker.value.getPosition()
            }
        })
}
