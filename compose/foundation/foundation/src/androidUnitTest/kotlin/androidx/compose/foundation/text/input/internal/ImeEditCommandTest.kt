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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange

internal open class ImeEditCommandTest {
    protected var state = TextFieldState("", TextRange.Zero)
    protected var transformedState = TransformedTextFieldState(state)
    protected var imeScope: ImeEditCommandScope = DefaultImeEditCommandScope(transformedState)

    protected fun initialize(text: String, selection: TextRange) {
        state = TextFieldState(text, selection)
        transformedState = TransformedTextFieldState(state)
        imeScope = DefaultImeEditCommandScope(transformedState)
    }
}
