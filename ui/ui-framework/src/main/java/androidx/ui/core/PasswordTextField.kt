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
import androidx.compose.remember
import androidx.ui.core.input.FocusManager
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.PasswordVisualTransformation
import androidx.ui.input.VisualTransformation
import androidx.ui.text.TextStyle

/**
 * A user interface element for entering and modifying password text.
 *
 * The PasswordTextField component renders an input with masking characters, i.e. bullet. Once
 * input service modify the text, you will get callback [onValueChange] with new text. Then, you can
 * set this new text so that this component renders up-to-date text from input service.
 *
 * Example usage:
 * @sample androidx.ui.framework.samples.PasswordTextFieldSample
 *
 * @param value The text to be shown in the [TextField]. If you want to specify cursor location or
 * selection range, use [TextField] with [TextFieldValue] instead.
 * @param onValueChange Called when the input service updates the text. When the input service
 * update the text, this callback is called with the updated text. If you want to observe the cursor
 * location or selection range, use [TextField] with [TextFieldValue] instead.
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param mask The character shown instead of plaint text.
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
 *
 * @see ImeAction
 * @see KeyboardType
 * @see TextField
 * @see VisualTransformation
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit = {},
    textStyle: TextStyle = TextStyle.Default,
    mask: Char = '\u2022',
    imeAction: ImeAction = ImeAction.Unspecified,
    onFocus: () -> Unit = {},
    onBlur: () -> Unit = {},
    focusIdentifier: String? = null,
    onImeActionPerformed: (ImeAction) -> Unit = {}
) {
    val passwordTransformation = remember(mask) { PasswordVisualTransformation(mask) }
    TextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        onFocus = onFocus,
        onBlur = onBlur,
        focusIdentifier = focusIdentifier,
        onImeActionPerformed = onImeActionPerformed,
        visualTransformation = passwordTransformation
    )
}
