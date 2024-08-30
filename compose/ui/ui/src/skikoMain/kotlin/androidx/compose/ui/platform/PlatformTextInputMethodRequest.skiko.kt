/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue

actual interface PlatformTextInputMethodRequest {
    @ExperimentalComposeUiApi
    val state: TextFieldValue
    @ExperimentalComposeUiApi
    val imeOptions: ImeOptions
    @ExperimentalComposeUiApi
    val onEditCommand: (List<EditCommand>) -> Unit
    @ExperimentalComposeUiApi
    val onImeAction: ((ImeAction) -> Unit)?
}
