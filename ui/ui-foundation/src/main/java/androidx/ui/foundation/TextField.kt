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

import androidx.compose.Composable
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.text.CoreTextField
import androidx.ui.core.Modifier
import androidx.ui.core.input.FocusManager
import androidx.ui.core.input.FocusNode
import androidx.ui.graphics.Color
import androidx.ui.graphics.useOrElse
import androidx.ui.input.ImeAction
import androidx.ui.input.EditorValue
import androidx.ui.input.KeyboardType
import androidx.ui.input.VisualTransformation
import androidx.ui.layout.fillMaxWidth
import androidx.ui.savedinstancestate.Saver
import androidx.ui.savedinstancestate.listSaver
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle

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
data class TextFieldValue(
    val text: String = "",
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
 * By default, text field will occupy all available space granted to it. You can use
 * [androidx.ui.layout.preferredWidthIn] or [androidx.ui.layout.preferredWidth] modifiers to
 * constrain the horizontal space occupied by the text field.
 * @param focusNode The focus node to be used for this TextField. Do not share this node with
 * other TextField.
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
 * value when the input field loses focus.
 * [FocusManager.requestFocus] to this value to move focus to this TextField. This identifier
 * must be unique in your app. If you have duplicated identifiers, the behavior is undefined.
 * @param onImeActionPerformed Called when the input service requested an IME action. When the
 * input service emitted an IME action, this callback is called with the emitted IME action. Note
 * that this IME action may be different from what you specified in [imeAction].
 * @param visualTransformation Optional visual filter for changing visual output of input field.
 * @param onTextLayout Callback that is executed when a new text layout is calculated.
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
    focusNode: FocusNode? = null,
    textColor: Color = Color.Unset,
    textStyle: TextStyle = currentTextStyle(),
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onFocusChange: (Boolean) -> Unit = {},
    onImeActionPerformed: (ImeAction) -> Unit = {},
    visualTransformation: VisualTransformation? = null,
    onTextLayout: (TextLayoutResult) -> Unit = {}
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

    CoreTextField(
        value = fullModel.value,
        modifier = modifier.fillMaxWidth(),
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
        onFocusChange = onFocusChange,
        focusNode = focusNode ?: remember { FocusNode() },
        onImeActionPerformed = onImeActionPerformed,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout
    )
}
