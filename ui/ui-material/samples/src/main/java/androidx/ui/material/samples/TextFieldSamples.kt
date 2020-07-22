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

package androidx.ui.material.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.ui.core.Modifier
import androidx.compose.foundation.Icon
import androidx.compose.foundation.Text
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.ui.material.EmphasisAmbient
import androidx.ui.material.MaterialTheme
import androidx.ui.material.OutlinedTextField
import androidx.ui.material.TextField
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite
import androidx.ui.material.icons.filled.Info
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.text.TextRange
import androidx.ui.unit.dp

@Sampled
@Composable
fun SimpleFilledTextFieldSample() {
    var text by savedInstanceState { "" }

    TextField(value = text,
        onValueChange = { text = it },
        label = { Text("Label") }
    )
}

@Sampled
@Composable
fun SimpleOutlinedTextFieldSample() {
    var text by savedInstanceState { "" }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Label") }
    )
}

@Sampled
@Composable
fun FilledTextFieldWithIcons() {
    var text by savedInstanceState { "" }

    TextField(value = text,
        onValueChange = { text = it },
        label = { Text("Label") },
        leadingIcon = { Icon(Icons.Filled.Favorite) },
        trailingIcon = { Icon(Icons.Filled.Info) }
    )
}

@Sampled
@Composable
fun FilledTextFieldWithPlaceholder() {
    var text by savedInstanceState { "" }

    TextField(value = text,
        onValueChange = { text = it },
        label = { Text("Email") },
        placeholder = { Text("example@gmail.com") }
    )
}

@Sampled
@Composable
fun FilledTextFieldWithErrorState() {
    var text by savedInstanceState { "" }
    val isValid = text.count() > 5 && '@' in text

    TextField(
        value = text,
        onValueChange = { text = it },
        label = {
            val label = if (isValid) "Email" else "Email*"
            Text(label)
        },
        isErrorValue = !isValid
    )
}

@Sampled
@Composable
fun TextFieldWithHelperMessage() {
    var text by savedInstanceState { "" }
    val invalidInput = text.count() < 5 || '@' !in text

    Column {
        TextField(
            value = text,
            onValueChange = { text = it },
            label = {
                val label = if (invalidInput) "Email*" else "Email"
                Text(label)
            },
            isErrorValue = invalidInput
        )
        val textColor = if (invalidInput) {
            MaterialTheme.colors.error
        } else {
            EmphasisAmbient.current.medium.applyEmphasis(MaterialTheme.colors.onSurface)
        }
        Text(
            text = if (invalidInput) "Requires '@' and at least 5 symbols" else "Helper message",
            style = MaterialTheme.typography.caption.copy(color = textColor),
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Sampled
@Composable
fun PasswordFilledTextField() {
    var password by savedInstanceState { "" }
    TextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Enter password") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardType = KeyboardType.Password
    )
}

@Sampled
@Composable
fun FilledTextFieldSample() {
    var text by savedInstanceState(saver = TextFieldValue.Saver) {
        TextFieldValue("example", TextRange(0, 7))
    }

    TextField(value = text,
        onValueChange = { text = it },
        label = { Text("Label") }
    )
}

@Sampled
@Composable
fun OutlinedTextFieldSample() {
    var text by savedInstanceState(saver = TextFieldValue.Saver) {
        TextFieldValue("example", TextRange(0, 7))
    }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Label") }
    )
}

@Sampled
@Composable
fun TextFieldWithHideKeyboardOnImeAction() {
    var text by savedInstanceState { "" }

    TextField(value = text,
        onValueChange = { text = it },
        label = { Text("Label") },
        imeAction = ImeAction.Done,
        onImeActionPerformed = { action, softwareController ->
            if (action == ImeAction.Done) {
                softwareController?.hideSoftwareKeyboard()
                // do something here
            }
        }
    )
}