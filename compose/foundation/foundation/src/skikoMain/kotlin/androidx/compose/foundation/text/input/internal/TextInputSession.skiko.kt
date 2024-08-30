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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.content.internal.ReceiveContentConfiguration
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.MutableSharedFlow

internal actual suspend fun PlatformTextInputSession.platformSpecificTextInputSession(
    state: TransformedTextFieldState,
    layoutState: TextLayoutState,
    imeOptions: ImeOptions,
    receiveContentConfiguration: ReceiveContentConfiguration?,
    onImeAction: ((ImeAction) -> Unit)?,
    stylusHandwritingTrigger: MutableSharedFlow<Unit>?,
    viewConfiguration: ViewConfiguration?
): Nothing {
    val editProcessor = EditProcessor()
    fun onEditCommand(commands: List<EditCommand>) {
        editProcessor.reset(
            value = with(state.visualText) {
                TextFieldValue(
                    text = toString(),
                    selection = selection,
                    composition = composition
                )
            },
            textInputSession = null
        )

        val newValue = editProcessor.apply(commands)

        state.replaceAll(newValue.text)
        state.editUntransformedTextAsUser {
            val untransformedSelection = state.mapFromTransformed(newValue.selection)
            setSelection(untransformedSelection.start, untransformedSelection.end)

            val composition = newValue.composition
            if (composition == null) {
                commitComposition()
            } else {
                val untransformedComposition = state.mapFromTransformed(composition)
                setComposition(untransformedComposition.start, untransformedComposition.end)
            }
        }
    }

    startInputMethod(
        SkikoPlatformTextInputMethodRequest(
            state = TextFieldValue(
                state.visualText.toString(),
                state.visualText.selection,
                state.visualText.composition,
            ),
            imeOptions = imeOptions,
            onEditCommand = ::onEditCommand,
            onImeAction = onImeAction
        )
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private data class SkikoPlatformTextInputMethodRequest(
    override val state: TextFieldValue,
    override val imeOptions: ImeOptions,
    override val onEditCommand: (List<EditCommand>) -> Unit,
    override val onImeAction: ((ImeAction) -> Unit)?
): PlatformTextInputMethodRequest