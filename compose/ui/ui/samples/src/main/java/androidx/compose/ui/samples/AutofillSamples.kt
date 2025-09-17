/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.FillableData
import androidx.compose.ui.autofill.contentType
import androidx.compose.ui.autofill.createFrom
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.fillableData
import androidx.compose.ui.semantics.onFillData
import androidx.compose.ui.semantics.semantics

@Sampled
@Composable
fun AutofillableTextFieldWithAutofillModifier() {
    TextField(
        state = rememberTextFieldState(),
        label = { Text("Enter your new username here.") },
        // Set the content type hint with the modifier extension.
        modifier = Modifier.contentType(ContentType.NewUsername),
    )
}

@Sampled
@Composable
fun AutofillableTextFieldWithContentTypeSemantics() {
    TextField(
        state = rememberTextFieldState(),
        label = { Text("Enter your new password here.") },
        // Set the content type hint with semantics.
        modifier = Modifier.semantics { contentType = ContentType.NewPassword },
    )
}

@Sampled
@Composable
fun AutofillableTextFieldWithFillableDataSemantics() {
    val state = rememberTextFieldState()

    TextField(
        state = state,
        label = { Text("Enter your username here.") },
        modifier =
            Modifier.semantics {
                contentType = ContentType.Username
                // Set the fillable data with semantics.
                FillableData.createFrom(state.text)?.let { fillableData = it }
                // Replace the state value with data from the autofill provider.
                onFillData { savedAutofillValue ->
                    savedAutofillValue.textValue?.let { state.edit { replace(0, length, it) } }
                    true
                }
            },
    )
}
