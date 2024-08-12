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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TextInputSession

// TODO remove after https://youtrack.jetbrains.com/issue/COMPOSE-740/Implement-BasicTextField2
@Suppress("DEPRECATION")
@OptIn(InternalTextApi::class)
@Composable
internal actual fun legacyTextInputServiceAdapterAndService():
    Pair<LegacyPlatformTextInputServiceAdapter, TextInputService>
{
    val service = LocalTextInputService.current!!
    val adapter = remember(service) {
        object : LegacyPlatformTextInputServiceAdapter() {
            private var session: TextInputSession? = null
            override fun startStylusHandwriting() {}

            override fun startInput(
                value: TextFieldValue,
                imeOptions: ImeOptions,
                onEditCommand: (List<EditCommand>) -> Unit,
                onImeActionPerformed: (ImeAction) -> Unit
            ) {
                session = service.startInput(value, imeOptions, onEditCommand, onImeActionPerformed)
            }

            override fun stopInput() {
                service.stopInput()
                session?.dispose()
                session = null
            }

            override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
                session?.updateState(oldValue, newValue)
            }

            override fun updateTextLayoutResult(
                textFieldValue: TextFieldValue,
                offsetMapping: OffsetMapping,
                textLayoutResult: TextLayoutResult,
                textFieldToRootTransform: (Matrix) -> Unit,
                innerTextFieldBounds: Rect,
                decorationBoxBounds: Rect
            ) {
                session?.updateTextLayoutResult(
                    textFieldValue,
                    offsetMapping,
                    textLayoutResult,
                    textFieldToRootTransform,
                    innerTextFieldBounds,
                    decorationBoxBounds
                )
            }
        }
    }
    return adapter to service
}