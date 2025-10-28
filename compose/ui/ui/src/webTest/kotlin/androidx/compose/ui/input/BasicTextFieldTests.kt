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

package androidx.compose.ui.input

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.TestInputState
import androidx.compose.ui.events.beforeInput
import androidx.compose.ui.events.compositionEnd
import androidx.compose.ui.events.compositionStart
import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.specs.AccentDialogTestSpec
import androidx.compose.ui.input.specs.AndroidCompositeInput
import androidx.compose.ui.input.specs.ChromeCompositeInput
import androidx.compose.ui.input.specs.CopyPasteTestSpec
import androidx.compose.ui.input.specs.FirefoxCompositeInput
import androidx.compose.ui.input.specs.IosCompositeInput
import androidx.compose.ui.input.specs.RegularInputTestSpec
import androidx.compose.ui.input.specs.SafariCompositeInput
import androidx.compose.ui.input.specs.TextFieldTestSpec
import androidx.compose.ui.input.specs.WinCompositeInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test


private class TextFieldValueHolder(private val textFieldValue: MutableState<TextFieldValue>) :
    TestInputState {
    override val text: String
        get() = textFieldValue.value.text

    @Composable
    override fun createBasicTextField(focusRequester: FocusRequester) {
        BasicTextField(
            value = textFieldValue.value,
            onValueChange = { value ->
                textFieldValue.value = value
            },
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}

private class TextFieldStateHolder(private val textFieldState: TextFieldState) : TestInputState {
    override val text: CharSequence
        get() = textFieldState.text

    @Composable
    override fun createBasicTextField(focusRequester: FocusRequester) {
        BasicTextField(
            state = textFieldState,
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}


internal interface BasicTextFieldWithValue : TextFieldTestSpec {
    override suspend fun createTestInputState(
        initialText: String,
        initialSelection: TextRange
    ): TestInputState = TextFieldValueHolder(
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = initialSelection
            )
        )
    )
}

internal interface BasicTextFieldWithState : TextFieldTestSpec {
    override suspend fun createTestInputState(
        initialText: String,
        initialSelection: TextRange
    ): TestInputState = TextFieldStateHolder(TextFieldState(initialText, initialSelection))
}

internal class RegularInputWithValueTests : RegularInputTestSpec, CopyPasteTestSpec,
    AccentDialogTestSpec,
    BasicTextFieldWithValue

internal class RegularInputWithStateTests : RegularInputTestSpec, CopyPasteTestSpec,
    AccentDialogTestSpec,
    BasicTextFieldWithState

internal class ChromeCompositeInputWithValueTests : ChromeCompositeInput, BasicTextFieldWithValue
internal class ChromeCompositeInputWithStateTests : ChromeCompositeInput, BasicTextFieldWithState

internal class FirefoxCompositeInputWithValueTests : FirefoxCompositeInput, BasicTextFieldWithValue
internal class FirefoxCompositeInputWithStateTests : FirefoxCompositeInput, BasicTextFieldWithState

internal class SafariCompositeInputWithValueTests : SafariCompositeInput, BasicTextFieldWithValue
internal class SafariCompositeInputWithStateTests : SafariCompositeInput, BasicTextFieldWithState

internal class IosCompositeInputWithValueTests : IosCompositeInput, BasicTextFieldWithValue
internal class IosCompositeInputWithStateTests : IosCompositeInput, BasicTextFieldWithState

internal class WinCompositeInputWithValueTests : WinCompositeInput, BasicTextFieldWithValue
internal class WinCompositeInputWithStateTests : WinCompositeInput, BasicTextFieldWithState

internal class AndroidCompositeInputWithValueTests : AndroidCompositeInput, BasicTextFieldWithValue
internal class AndroidCompositeInputWithStateTests : AndroidCompositeInput, BasicTextFieldWithState {
    @Test
    fun `suggestion at the end` () = runApplicationTest {
        // https://youtrack.jetbrains.com/issue/CMP-8809
        val textFieldValue = createApplicationWithHolder()

        sendToHtmlInput(
            keyEvent("Unidentified", code=""),
            compositionStart(""),
            beforeInput("insertCompositionText", "h", isComposing = true),
            keyEvent("Unidentified", code="", type = "keyup"),
            keyEvent("Unidentified", code=""),
            beforeInput("insertCompositionText", "he", isComposing = true),
            keyEvent("Unidentified", code="", type = "keyup"),
            keyEvent("Unidentified", code=""),
            beforeInput("insertCompositionText", "her", isComposing = true),
            keyEvent("Unidentified", code="", type = "keyup"),
            keyEvent("Unidentified", code=""),
            beforeInput("insertCompositionText", "here", isComposing = true),
            keyEvent("Unidentified", code="", type = "keyup"),
        )

        textFieldValue.awaitAndAssertTextEquals(
            "here"
        )

        sendToHtmlInput(
            beforeInput("insertCompositionText", "where", isComposing = true),
            compositionEnd("where"),
            keyEvent("Unidentified", code="", type = "keyup"),
            keyEvent("Unidentified", code=""),
            beforeInput("insertCompositionText", " ", isComposing = true),
            keyEvent("Unidentified", code="", type = "keyup"),
        )

        textFieldValue.awaitAndAssertTextEquals(
            "where "
        )
    }

}