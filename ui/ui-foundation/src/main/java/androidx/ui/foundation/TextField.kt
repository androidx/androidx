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

package androidx.ui.foundation

import androidx.animation.AnimatedValue
import androidx.animation.Infinite
import androidx.animation.KeyframesBuilder
import androidx.animation.RepeatableBuilder
import androidx.compose.Composable
import androidx.compose.Immutable
import androidx.compose.Stable
import androidx.compose.State
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.animation.animatedColor
import androidx.ui.core.ContentDrawScope
import androidx.ui.core.DrawModifier
import androidx.ui.core.Modifier
import androidx.ui.core.composed
import androidx.ui.core.focus.FocusModifier
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.graphics.drawscope.Stroke
import androidx.ui.graphics.useOrElse
import androidx.ui.input.EditorValue
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.VisualTransformation
import androidx.ui.layout.defaultMinSizeConstraints
import androidx.ui.savedinstancestate.Saver
import androidx.ui.savedinstancestate.listSaver
import androidx.ui.text.CoreTextField
import androidx.ui.text.AnnotatedString
import androidx.ui.text.SoftwareKeyboardController
import androidx.ui.text.TextFieldDelegate
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp

/**
 * A class holding information about the editing state.
 *
 * The input service updates text selection or cursor as well as text. You can observe and
 * control the selection, cursor and text altogether.
 *
 * @param text the text will be rendered
 * @param selection the selection range. If the selection is collapsed, it represents cursor
 * location. Do not specify outside of the text buffer.
 */
@Immutable
data class TextFieldValue(
    @Stable
    val text: String = "",
    @Stable
    val selection: TextRange = TextRange(0, 0)
) {
    companion object {
        /**
         * The default [Saver] implementation for [TextFieldValue].
         */
        val Saver = listSaver<TextFieldValue, Any>(
            save = {
                listOf(it.text, it.selection.start, it.selection.end)
            },
            restore = {
                TextFieldValue(it[0] as String, TextRange(it[1] as Int, it[2] as Int))
            }
        )
    }
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
 * @sample androidx.ui.foundation.samples.TextFieldSample
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
 * @param modifier optional [Modifier] for this text field.
 * @param textColor [Color] to apply to the text. If [Color.Unset], and [textStyle] has no color
 * set, this will be [contentColor].
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
 * @param cursorColor Color of the cursor.
 *
 * @see TextFieldValue
 * @see ImeAction
 * @see KeyboardType
 * @see VisualTransformation
 */
@Composable
fun TextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unset,
    textStyle: TextStyle = currentTextStyle(),
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onFocusChange: (Boolean) -> Unit = {},
    onImeActionPerformed: (ImeAction) -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {},
    cursorColor: Color = contentColor()
) {
    val fullModel = state { EditorValue() }
    if (fullModel.value.text != value.text || fullModel.value.selection != value.selection) {
        val newSelection = TextRange(
            value.selection.start.coerceIn(0, value.text.length),
            value.selection.end.coerceIn(0, value.text.length)
        )
        fullModel.value = EditorValue(
            text = value.text,
            selection = newSelection
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
            animColor.animateTo(Color.Transparent, anim = RepeatableBuilder<Color>().apply {
                iterations = Infinite
                animation = KeyframesBuilder<Color>().apply {
                    duration = 1000
                    cursorColor at 0
                    cursorColor at 499
                    Color.Transparent at 500
                }
            })
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
            val prevState = fullModel.value
            fullModel.value = it
            if (prevState.text != it.text || prevState.selection != it.selection) {
                onValueChange(
                    TextFieldValue(
                        it.text,
                        it.selection
                    )
                )
            }
        },
        textStyle = mergedStyle,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onFocusChange = {
            cursorState.focused = it
            onFocusChange(it)
        },
        onImeActionPerformed = onImeActionPerformed,
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
    editorValue: State<EditorValue>,
    visualTransformation: VisualTransformation
): Modifier {
    return if (cursorState.focused && editorValue.value.selection.collapsed) {
        composed {
            remember(cursorState, editorValue, visualTransformation) {
                CursorModifier(color, cursorState, editorValue, visualTransformation)
            }
        }
    } else {
        this
    }
}

private data class CursorModifier(
    val color: AnimatedValue<Color, *>,
    val cursorState: CursorState,
    val editorValue: State<EditorValue>,
    val visualTransformation: VisualTransformation
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        if (color.value.alpha != 0f) {
            val transformed = visualTransformation.filter(AnnotatedString(editorValue.value.text))
            val transformedText = editorValue.value.composition?.let {
                TextFieldDelegate.applyCompositionDecoration(it, transformed)
            } ?: transformed
            val cursorWidth = CursorThickness.value * density
            val cursorHeight = cursorState.layoutResult?.size?.height?.value?.toFloat() ?: 0f

            val cursorRect = cursorState.layoutResult?.getCursorRect(
                transformedText.offsetMap.originalToTransformed(editorValue.value.selection.min)
            ) ?: Rect(
                0f, 0f,
                cursorWidth, cursorHeight
            )
            val cursorX = (cursorRect.left + cursorRect.right) / 2

            drawLine(
                color.value,
                Offset(cursorX, cursorRect.top),
                Offset(cursorX, cursorRect.bottom),
                Stroke(cursorWidth)
            )
        }

        drawContent()
    }
}

private val CursorThickness = 2.dp
private val DefaultTextFieldWidth = 280.dp