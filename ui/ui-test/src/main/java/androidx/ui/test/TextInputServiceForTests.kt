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

package androidx.ui.test

import androidx.ui.input.EditOperation
import androidx.ui.input.EditorValue
import androidx.ui.input.ImeAction
import androidx.ui.input.InputSessionToken
import androidx.ui.input.KeyboardType
import androidx.ui.input.PlatformTextInputService
import androidx.ui.input.TextInputService

/**
 * Extra layer that serves as an observer between the text input service and text fields.
 *
 * When a text field gets a focus it calls to the text input service to provide its callback to
 * accept input from the IME. Here we grab that callback so we can fetch it commands the same
 * way IME would do.
 */
internal class TextInputServiceForTests(
    platformTextInputService: PlatformTextInputService
) : TextInputService(platformTextInputService) {

    var onEditCommand: ((List<EditOperation>) -> Unit)? = null
    var onImeActionPerformed: ((ImeAction) -> Unit)? = null

    override fun startInput(
        initModel: EditorValue,
        keyboardType: KeyboardType,
        imeAction: ImeAction,
        onEditCommand: (List<EditOperation>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ): InputSessionToken {
        this.onEditCommand = onEditCommand
        this.onImeActionPerformed = onImeActionPerformed
        return super.startInput(
            initModel,
            keyboardType,
            imeAction,
            onEditCommand,
            onImeActionPerformed
        )
    }

    override fun stopInput(token: InputSessionToken) {
        this.onEditCommand = null
        this.onImeActionPerformed = null
        super.stopInput(token)
    }
}