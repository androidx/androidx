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

package androidx.compose.foundation.text

import androidx.compose.foundation.assertThat
import androidx.compose.foundation.isEqualTo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import kotlin.test.Test
import kotlin.test.assertTrue

class LegacyPlatformTextInputServiceAdapterTest {

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun textLayoutResultIsProvided() = runComposeUiTest {

        var textFromUpdateTextLayoutResult = ""

        val platformTextInputService = object : PlatformTextInputService {

            override fun startInput(
                value: TextFieldValue,
                imeOptions: ImeOptions,
                onEditCommand: (List<EditCommand>) -> Unit,
                onImeActionPerformed: (ImeAction) -> Unit
            ) {}

            override fun stopInput() {}

            override fun showSoftwareKeyboard() {}

            override fun hideSoftwareKeyboard() {}

            override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {}

            override fun updateTextLayoutResult(
                textFieldValue: TextFieldValue,
                offsetMapping: OffsetMapping,
                textLayoutResult: TextLayoutResult,
                textFieldToRootTransform: (Matrix) -> Unit,
                innerTextFieldBounds: Rect,
                decorationBoxBounds: Rect
            ) {
                textFromUpdateTextLayoutResult = textFieldValue.text
            }
        }
        val textInputService = TextInputService(platformTextInputService)

        setContent {
            CompositionLocalProvider(LocalTextInputService provides textInputService) {
                var text by remember { mutableStateOf("") }
                BasicTextField(
                    modifier = Modifier.testTag("input"),
                    value = text,
                    onValueChange = {
                        text = it
                    }
                )
            }
        }

        waitForIdle()

        onNodeWithTag("input").performTextInput("abc")

        waitForIdle()

        assertThat(textFromUpdateTextLayoutResult).isEqualTo("abc")
    }
}