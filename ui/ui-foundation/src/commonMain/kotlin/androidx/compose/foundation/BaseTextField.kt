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

package androidx.compose.foundation

import androidx.compose.animation.core.AnimatedValue
import androidx.compose.animation.core.AnimationConstants.Infinite
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.repeatable
import androidx.compose.Composable
import androidx.compose.State
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.compose.animation.animatedColor
import androidx.ui.core.ContentDrawScope
import androidx.ui.core.DrawModifier
import androidx.ui.core.Modifier
import androidx.ui.core.composed
import androidx.ui.core.focus.FocusModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.useOrElse
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.defaultMinSizeConstraints
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.SoftwareKeyboardController
import androidx.compose.foundation.text.TextFieldDelegate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.constrain
import androidx.ui.unit.dp

// TODO(b/151940543): Remove this variable when we have a solution for idling animations
/** @suppress */
@InternalFoundationApi
var blinkingCursorEnabled: Boolean = true
    set

/**
 * Composable that enables users to edit text via hardware or software keyboard.
 *
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state
 * represented by [TextFieldValue]. [TextFieldValue] contains
 * the text entered by user, as well as selection, cursor and text composition information.
 * Please check [TextFieldValue] for the description of its contents.
 *
 * It is crucial that the value provided in the [onValueChange] is fed back into [BaseTextField] in
 * order to have the final state of the text being displayed. Example usage:
 * @sample androidx.compose.foundation.samples.TextFieldSample
 *
 * Please keep in mind that [onValueChange] is useful to be informed about the latest state of the
 * text input by users, however it is generally not recommended to modify the values in the
 * [TextFieldValue] that you get via [onValueChange] callback. Any change to the values in
 * [TextFieldValue] may result in a context reset and end up with input session restart. Such
 * a scenario would cause glitches in the UI or text input experience for users.
 *
 * This composable provides basic text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder. A design system based implementation such as
 * Material `FilledTextField` is typically what is needed to cover most of the needs. This
 * composable is designed to be used when a custom implementation for different design system is
 * needed.
 *
 * For example, if you need to include a hint in your TextField you can write a composable as below:
 * @sample androidx.compose.foundation.samples.PlaceholderTextFieldSample
 *
 * @param value The [TextFieldValue] to be shown in the [BaseTextField].
 * @param onValueChange Called when the input service updates values in
 * [TextFieldValue].
 * @param modifier optional [Modifier] for this text field.
 * @param textColor [Color] to apply to the text. If [Color.Unset], and [textStyle] has no color
 * set, this will be [contentColor].
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * The default [textStyle] uses the [currentTextStyle] defined by the theme
 * @param keyboardType The keyboard type to be used in this text field. Note that this input type
 * is honored by IME and shows corresponding keyboard but this is not guaranteed. For example,
 * some IME may send non-ASCII character even if you set [KeyboardType.Ascii].
 * @param imeAction The IME action. This IME action is honored by IME and may show specific icons
 * on the keyboard. For example, search icon may be shown if [ImeAction.Search] is specified.
 * Then, when user tap that key, the [onImeActionPerformed] callback is called with specified
 * ImeAction.
 *  * @param onImeActionPerformed Called when the input service requested an IME action. When the
 * input service emitted an IME action, this callback is called with the emitted IME action. Note
 * that this IME action may be different from what you specified in [imeAction].
 * @param onFocusChanged Called with true value when the input field gains focus and with false
 * value when the input field loses focus. Use [FocusModifier.requestFocus] to obtain text input
 * focus to this TextField.
 * @param visualTransformation The visual transformation filter for changing the visual
 * representation of the input. By default no visual transformation is applied.
 * @param onTextLayout Callback that is executed when a new text layout is calculated.
 * @param onTextInputStarted Callback that is executed when the initialization has done for
 * communicating with platform text input service, e.g. software keyboard on Android. Called with
 * [SoftwareKeyboardController] instance which can be used for requesting input show/hide software
 * keyboard.
 * @param cursorColor Color of the cursor.
 *
 * @see TextFieldValue
 * @see ImeAction
 * @see KeyboardType
 * @see VisualTransformation
 */
@Composable
@ExperimentalFoundationApi
fun BaseTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unset,
    textStyle: TextStyle = currentTextStyle(),
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onImeActionPerformed: (ImeAction) -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {},
    cursorColor: Color = contentColor()
) {
    val fullModel = state { TextFieldValue() }
    if (fullModel.value != value) {
        @OptIn(InternalTextApi::class)
        fullModel.value = TextFieldValue(
            text = value.text,
            selection = value.selection.constrain(0, value.text.length),
            composition = value.composition?.constrain(0, value.text.length)
        )
    }

    val color = textColor.useOrElse { textStyle.color.useOrElse { contentColor() } }
    val mergedStyle = textStyle.merge(TextStyle(color = color))

    // cursor with blinking animation
    val cursorState: CursorState = remember { CursorState() }
    val cursorNeeded = cursorState.focused && fullModel.value.selection.collapsed
    val animColor = animatedColor(cursorColor)
    onCommit(cursorColor, cursorState.focused, fullModel.value) {
        if (cursorNeeded) {
            // TODO(b/151940543): Disable blinking in tests until we handle idling animations
            @OptIn(InternalFoundationApi::class)
            if (blinkingCursorEnabled) {
                animColor.animateTo(Color.Transparent, anim = repeatable(
                    iterations = Infinite,
                    animation = keyframes {
                        durationMillis = 1000
                        cursorColor at 0
                        cursorColor at 499
                        Color.Transparent at 500
                    }
                ))
            } else {
                animColor.snapTo(cursorColor)
            }
        } else {
            animColor.snapTo(Color.Transparent)
        }

        onDispose {
            animColor.stop()
        }
    }

    CoreTextField(
        value = fullModel.value,
        modifier = modifier
            .defaultMinSizeConstraints(minWidth = DefaultTextFieldWidth)
            .cursorModifier(animColor, cursorState, fullModel, visualTransformation),
        onValueChange = {
            fullModel.value = it
            onValueChange(it)
        },
        textStyle = mergedStyle,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onImeActionPerformed = onImeActionPerformed,
        onFocusChanged = {
            cursorState.focused = it
            onFocusChanged(it)
        },
        visualTransformation = visualTransformation,
        onTextLayout = {
            cursorState.layoutResult = it
            onTextLayout(it)
        },
        onTextInputStarted = onTextInputStarted
    )
}

private class CursorState {
    var focused by mutableStateOf(false)
    var layoutResult by mutableStateOf<TextLayoutResult?>(null)
}

private fun Modifier.cursorModifier(
    color: AnimatedValue<Color, *>,
    cursorState: CursorState,
    textFieldValue: State<TextFieldValue>,
    visualTransformation: VisualTransformation
): Modifier {
    return if (cursorState.focused && textFieldValue.value.selection.collapsed) {
        composed {
            remember(cursorState, textFieldValue, visualTransformation) {
                CursorModifier(color, cursorState, textFieldValue, visualTransformation)
            }
        }
    } else {
        this
    }
}

private data class CursorModifier(
    val color: AnimatedValue<Color, *>,
    val cursorState: CursorState,
    val textFieldValue: State<TextFieldValue>,
    val visualTransformation: VisualTransformation
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        if (color.value.alpha != 0f) {
            val transformed = visualTransformation.filter(
                AnnotatedString(textFieldValue.value.text)
            )
            @OptIn(InternalTextApi::class)
            val transformedText = textFieldValue.value.composition?.let {
                TextFieldDelegate.applyCompositionDecoration(it, transformed)
            } ?: transformed
            val cursorWidth = CursorThickness.value * density
            val cursorHeight = cursorState.layoutResult?.size?.height?.toFloat() ?: 0f

            val cursorRect = cursorState.layoutResult?.getCursorRect(
                transformedText.offsetMap.originalToTransformed(textFieldValue.value.selection.min)
            ) ?: Rect(
                0f, 0f,
                cursorWidth, cursorHeight
            )
            val cursorX = (cursorRect.left + cursorRect.right) / 2

            drawLine(
                color.value,
                Offset(cursorX, cursorRect.top),
                Offset(cursorX, cursorRect.bottom),
                strokeWidth = cursorWidth
            )
        }

        drawContent()
    }
}

private val CursorThickness = 2.dp
private val DefaultTextFieldWidth = 280.dp