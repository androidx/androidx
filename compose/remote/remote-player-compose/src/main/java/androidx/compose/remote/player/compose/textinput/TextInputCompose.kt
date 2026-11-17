/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose.textinput

import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.remote.core.operations.layout.managers.TextInputLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.flow.drop

/** Maps the layout's keyboard type / IME action / flags to Compose [KeyboardOptions]. */
internal fun TextInputLayout.keyboardOptions(): KeyboardOptions {
    val keyboardType =
        when (keyboardType) {
            TextInputLayout.KEYBOARD_ASCII -> KeyboardType.Ascii
            TextInputLayout.KEYBOARD_NUMBER -> KeyboardType.Number
            TextInputLayout.KEYBOARD_PHONE -> KeyboardType.Phone
            TextInputLayout.KEYBOARD_URI -> KeyboardType.Uri
            TextInputLayout.KEYBOARD_EMAIL -> KeyboardType.Email
            TextInputLayout.KEYBOARD_PASSWORD -> KeyboardType.Password
            TextInputLayout.KEYBOARD_NUMBER_PASSWORD -> KeyboardType.NumberPassword
            TextInputLayout.KEYBOARD_DECIMAL -> KeyboardType.Decimal
            else -> KeyboardType.Text
        }
    val imeAction =
        when (imeAction) {
            TextInputLayout.IME_ACTION_NONE -> ImeAction.None
            TextInputLayout.IME_ACTION_GO -> ImeAction.Go
            TextInputLayout.IME_ACTION_SEARCH -> ImeAction.Search
            TextInputLayout.IME_ACTION_SEND -> ImeAction.Send
            TextInputLayout.IME_ACTION_PREVIOUS -> ImeAction.Previous
            TextInputLayout.IME_ACTION_NEXT -> ImeAction.Next
            TextInputLayout.IME_ACTION_DONE -> ImeAction.Done
            else ->
                if (hasInputFlag(TextInputLayout.FLAG_SINGLE_LINE)) ImeAction.Done
                else ImeAction.Default
        }
    val capitalization =
        when {
            hasInputFlag(TextInputLayout.FLAG_CAPITALIZE_CHARACTERS) ->
                KeyboardCapitalization.Characters
            hasInputFlag(TextInputLayout.FLAG_CAPITALIZE_WORDS) -> KeyboardCapitalization.Words
            hasInputFlag(TextInputLayout.FLAG_CAPITALIZE_SENTENCES) ->
                KeyboardCapitalization.Sentences
            else -> KeyboardCapitalization.None
        }
    return KeyboardOptions(
        capitalization = capitalization,
        autoCorrectEnabled = hasInputFlag(TextInputLayout.FLAG_AUTO_CORRECT),
        keyboardType = keyboardType,
        imeAction = imeAction,
    )
}

/** Applies the layout's character filter and maximum length to a candidate value. */
internal fun TextInputLayout.filterInput(text: String): String {
    val allowed: ((Char) -> Boolean)? =
        when (filter) {
            TextInputLayout.FILTER_DIGITS -> { c ->
                c in '0'..'9'
            }
            TextInputLayout.FILTER_DECIMAL -> { c ->
                c in '0'..'9' || c in ".,-"
            }
            TextInputLayout.FILTER_HEXADECIMAL -> { c ->
                c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
            }
            TextInputLayout.FILTER_ALPHANUMERIC -> { c ->
                c.isLetterOrDigit()
            }
            else -> null
        }
    var result = if (allowed != null) text.filter(allowed) else text
    if (maxLength > 0 && result.length > maxLength) {
        result = result.substring(0, maxLength)
    }
    return result
}

/**
 * A [BasicTextField] bound to a document string declared editable by [op].
 *
 * The field owns a [TextFieldState] (text, cursor, selection) and pushes the text to
 * [onValueChange] on every keystroke, or only through [onDone] when [op] has
 * [TextInputLayout.FLAG_COMMIT_ON_DONE]. [onDone] is called on the IME action and when focus is
 * lost.
 *
 * The field takes focus when tapped like any text field. Hosts that show the field in response to a
 * tap that happened elsewhere (the overlay editor opened by a click on the document) add a
 * `focusRequester` to [modifier] and request focus once the field is composed.
 *
 * @param text the current value of the document string
 * @param onValueChange writes a new value into the document string
 * @param onDone editing is over (IME action or focus loss)
 */
@Composable
internal fun RcTextInputField(
    text: String,
    op: TextInputLayout,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    hint: String? = null,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit = {},
) {
    val commitOnDone = op.hasInputFlag(TextInputLayout.FLAG_COMMIT_ON_DONE)
    val singleLine = op.hasInputFlag(TextInputLayout.FLAG_SINGLE_LINE)
    val state = rememberTextFieldState(text, TextRange(text.length))
    var focused by remember { mutableStateOf(false) }
    // Follow external changes to the string while the user is not typing.
    if (!focused && state.text.toString() != text) {
        state.setTextAndPlaceCursorAtEnd(text)
    }
    // Per keystroke commit: publish every change of the state's text.
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    if (!commitOnDone) {
        LaunchedEffect(state) {
            snapshotFlow { state.text.toString() }.drop(1).collect { latestOnValueChange(it) }
        }
    }

    fun commit() {
        val value = state.text.toString()
        if (value != text) onValueChange(value)
    }

    val secure =
        op.keyboardType == TextInputLayout.KEYBOARD_PASSWORD ||
            op.keyboardType == TextInputLayout.KEYBOARD_NUMBER_PASSWORD
    // A secure field is single line by construction.
    val inputTransformation =
        remember(op, singleLine, secure) { RcInputTransformation(op, singleLine || secure) }
    val fieldModifier =
        modifier.contentTypeSemantics(op).onFocusChanged { focusState ->
            val wasFocused = focused
            focused = focusState.isFocused
            if (wasFocused && !focusState.isFocused) {
                if (commitOnDone) commit()
                onDone()
            }
        }
    val onKeyboardAction = KeyboardActionHandler {
        if (commitOnDone) commit()
        onDone()
    }
    val decorator = TextFieldDecorator { innerTextField ->
        if (state.text.isEmpty() && !hint.isNullOrEmpty()) {
            BasicText(
                text = hint,
                style = textStyle.copy(color = textStyle.color.copy(alpha = 0.4f)),
                maxLines = 1,
            )
        }
        innerTextField()
    }
    if (secure) {
        // Obfuscates the text, disables the clipboard and keyboard suggestions.
        BasicSecureTextField(
            state = state,
            modifier = fieldModifier,
            inputTransformation = inputTransformation,
            textStyle = textStyle,
            keyboardOptions = op.keyboardOptions(),
            onKeyboardAction = onKeyboardAction,
            cursorBrush = SolidColor(textStyle.color),
            decorator = decorator,
        )
    } else {
        BasicTextField(
            state = state,
            modifier = fieldModifier,
            inputTransformation = inputTransformation,
            textStyle = textStyle,
            keyboardOptions = op.keyboardOptions(),
            onKeyboardAction = onKeyboardAction,
            lineLimits =
                if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(),
            cursorBrush = SolidColor(textStyle.color),
            decorator = decorator,
        )
    }
}

/**
 * Applies the layout's character filter and maximum length, and keeps single line fields on one
 * line (the IME only configures the keyboard; newlines can still arrive by paste).
 */
private class RcInputTransformation(
    private val op: TextInputLayout,
    private val singleLine: Boolean,
) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val current = asCharSequence().toString()
        val oneLine = if (singleLine) current.replace("\n", "").replace("\r", "") else current
        val filtered = op.filterInput(oneLine)
        if (filtered != current) {
            replace(0, length, filtered)
            placeCursorAtEnd()
        }
    }
}

/** Declares the layout's content type as autofill semantics, when it has one. */
private fun Modifier.contentTypeSemantics(op: TextInputLayout): Modifier {
    val type =
        when (op.contentType) {
            TextInputLayout.CONTENT_TYPE_PERSON_NAME -> ContentType.PersonFullName
            TextInputLayout.CONTENT_TYPE_PERSON_FIRST_NAME -> ContentType.PersonFirstName
            TextInputLayout.CONTENT_TYPE_PERSON_LAST_NAME -> ContentType.PersonLastName
            TextInputLayout.CONTENT_TYPE_EMAIL_ADDRESS -> ContentType.EmailAddress
            TextInputLayout.CONTENT_TYPE_PHONE_NUMBER -> ContentType.PhoneNumber
            TextInputLayout.CONTENT_TYPE_USERNAME -> ContentType.Username
            TextInputLayout.CONTENT_TYPE_PASSWORD -> ContentType.Password
            TextInputLayout.CONTENT_TYPE_NEW_PASSWORD -> ContentType.NewPassword
            TextInputLayout.CONTENT_TYPE_ONE_TIME_CODE -> ContentType.SmsOtpCode
            TextInputLayout.CONTENT_TYPE_POSTAL_ADDRESS -> ContentType.PostalAddress
            TextInputLayout.CONTENT_TYPE_POSTAL_CODE -> ContentType.PostalCode
            TextInputLayout.CONTENT_TYPE_CREDIT_CARD_NUMBER -> ContentType.CreditCardNumber
            TextInputLayout.CONTENT_TYPE_CREDIT_CARD_EXPIRATION_DATE ->
                ContentType.CreditCardExpirationDate
            TextInputLayout.CONTENT_TYPE_CREDIT_CARD_SECURITY_CODE ->
                ContentType.CreditCardSecurityCode
            else -> null
        } ?: return this
    return semantics { contentType = type }
}
