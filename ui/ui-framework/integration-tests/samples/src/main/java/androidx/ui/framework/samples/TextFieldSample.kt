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
import androidx.compose.memo
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.Draw
import androidx.ui.core.EditorModel
import androidx.ui.core.TextField
import androidx.ui.core.toRect
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.input.KeyboardType
import androidx.ui.input.PasswordVisualTransformation
import androidx.ui.text.TextRange

@Sampled
@Composable
fun StringTextFieldSample() {
    val state = +state { "" }
    TextField(
        value = state.value,
        onValueChange = { state.value = it }
    )
}

@Sampled
@Composable
fun EditorModelTextFieldSample() {
    val state = +state { EditorModel() }
    TextField(
        value = state.value,
        onValueChange = { state.value = it }
    )
}

@Sampled
@Composable
fun CompositionEditorModelTextFieldSample() {
    val model = +state { EditorModel() }
    val composition = +state<TextRange?> { null }
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
fun PasswordFieldSample() {
    val state = +state { "" }
    val passwordVisualTransformation = +memo { PasswordVisualTransformation() }
    TextField(
        value = state.value,
        onValueChange = { state.value = it },
        keyboardType = KeyboardType.Password,
        visualTransformation = passwordVisualTransformation
    )
}