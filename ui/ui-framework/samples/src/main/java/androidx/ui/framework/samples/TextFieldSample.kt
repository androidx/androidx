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

package androidx.ui.framework.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.PasswordTextField
import androidx.ui.core.TextField
import androidx.ui.text.TextFieldValue
import androidx.ui.text.TextRange

@Sampled
@Composable
fun StringTextFieldSample() {
    val state = state { "" }
    TextField(
        value = state.value,
        onValueChange = { state.value = it }
    )
}

@Sampled
@Composable
fun EditorModelTextFieldSample() {
    val state = state { TextFieldValue() }
    TextField(
        value = state.value,
        onValueChange = { state.value = it }
    )
}

@Sampled
@Composable
fun CompositionEditorModelTextFieldSample() {
    val model = state { TextFieldValue() }
    val composition = state<TextRange?> { null }
    TextField(
        model = model.value,
        compositionRange = composition.value,
        onValueChange = { newModel, newComposition ->
            model.value = newModel
            composition.value = newComposition
        }
    )
}

@Sampled
@Composable
fun PasswordTextFieldSample() {
    val state = state { "" }
    PasswordTextField(
        value = state.value,
        onValueChange = { state.value = it }
    )
}