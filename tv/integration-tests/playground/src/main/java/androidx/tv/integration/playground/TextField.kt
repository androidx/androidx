/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.integration.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.text.AndroidImeOptions
import androidx.tv.foundation.text.TvKeyboardAlignment
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme

@Composable
fun TextFieldContent() {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                repeat(4) { SampleCardItem() }
            }
        }
        item {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item { SampleTextField(label = "Name") }
                item { SampleTextField(label = "Email", keyboardType = KeyboardType.Email) }
                item { SampleTextField(label = "Password", keyboardType = KeyboardType.Password) }
                item { SampleButton(text = "Submit") }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                repeat(4) { SampleCardItem() }
            }
        }
    }
}

@OptIn(ExperimentalTvFoundationApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun SampleTextField(label: String, keyboardType: KeyboardType = KeyboardType.Text) {
    var text by remember { mutableStateOf("") }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = {
            Text(label)
        },
        singleLine = true,
        placeholder = {
            Text("$label...")
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            platformImeOptions = AndroidImeOptions(TvKeyboardAlignment.Left),
            imeAction = ImeAction.Next
        ),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedBorderColor = MaterialTheme.colorScheme.border,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            errorLabelColor = MaterialTheme.colorScheme.error,
            errorBorderColor = MaterialTheme.colorScheme.error
        )
    )
}

@Composable
fun SampleButton(text: String) {
    Button(
        onClick = { }
    ) {
        Text(text)
    }
}

@Composable
private fun SampleCardItem() {
    Box(
        modifier = Modifier
            .background(Color.Magenta.copy(alpha = 0.3f))
            .width(50.dp)
            .height(50.dp)
            .drawBorderOnFocus()
            .focusable()
    )
}
