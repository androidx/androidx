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
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.foundation.Icon
import androidx.ui.foundation.Text
import androidx.ui.foundation.TextFieldValue
import androidx.ui.foundation.currentTextStyle
import androidx.ui.layout.Column
import androidx.ui.layout.padding
import androidx.ui.material.EmphasisAmbient
import androidx.ui.material.FilledTextField
import androidx.ui.material.MaterialTheme
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Favorite
import androidx.ui.material.icons.filled.Info
import androidx.ui.text.TextRange
import androidx.ui.unit.dp

// TODO(b/154799748): remove explicit currentTextStyle() when upstream bug is fixed
@Sampled
@Composable
fun SimpleFilledTextFieldSample() {
    var text by state { "" }

    FilledTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Label", style = currentTextStyle()) }
    )
}

@Sampled
@Composable
fun FilledTextFieldWithIcons() {
    var text by state { "" }

    FilledTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Label", style = currentTextStyle()) },
        leadingIcon = { Icon(Icons.Filled.Favorite) },
        trailingIcon = { Icon(Icons.Filled.Info) }
    )
}

@Sampled
@Composable
fun FilledTextFieldWithPlaceholder() {
    var text by state { "" }

    FilledTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Email", style = currentTextStyle()) },
        placeholder = { Text("example@gmail.com", style = currentTextStyle()) }
    )
}

@Sampled
@Composable
fun FilledTextFieldWithErrorState() {
    var text by state { "" }
    var isValid = text.count() > 5 && '@' in text

    FilledTextField(
        value = text,
        onValueChange = { text = it },
        label = {
            val label = if (isValid) "Email" else "Email*"
            Text(label, style = currentTextStyle())
        },
        isErrorValue = !isValid
    )
}

@Sampled
@Composable
fun TextFieldWithHelperMessage() {
    var text by state { "" }
    var invalidInput = text.count() < 5 || '@' !in text

    Column {
        FilledTextField(
            value = text,
            onValueChange = { text = it },
            label = {
                val label = if (invalidInput) "Email*" else "Email"
                Text(label, style = currentTextStyle())
            },
            isErrorValue = invalidInput
        )
        val textColor = if (invalidInput) {
            MaterialTheme.colors.error
        } else {
            EmphasisAmbient.current.medium.emphasize(MaterialTheme.colors.onSurface)
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
fun FilledTextFieldSample() {
    var text by state { TextFieldValue("example", TextRange(0, 7)) }

    FilledTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Label") }
    )
}