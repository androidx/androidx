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

package androidx.compose.ui.input.specs

import androidx.compose.ui.events.EventsSequence
import androidx.compose.ui.events.beforeInput
import androidx.compose.ui.events.compositionEnd
import androidx.compose.ui.events.compositionStart
import androidx.compose.ui.events.eventKeyCode
import androidx.compose.ui.events.eventsSequence
import androidx.compose.ui.events.keyEvent
import kotlin.test.Test
import kotlin.test.assertIs
import org.w3c.dom.HTMLTextAreaElement

internal interface СompositeInputTestSpec : TextFieldTestSpec {

    fun triggerComposingSequence(inputKey: String, compositionInput: String, typedKey: String): EventsSequence
    fun compositionStartFromInput(inputKey: String, compositionKey: String = inputKey): EventsSequence

    fun String.toProcessKeyDownEvent() = keyEvent("Process", code = eventKeyCode())
    fun String.toProcessKeyUpEvent() = keyEvent("Process", code = eventKeyCode(), type = "keyup")

    @Test
    fun compositeInput() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()

        val backingTextField = getShadowRoot().querySelector("textarea")
        assertIs<HTMLTextAreaElement>(backingTextField)

        triggerComposingSequence("a", "1", "啊").sendToHtmlInput()

        textFieldValue.awaitAndAssertTextEquals("啊")

        sendStandardKeyboardSequence("x")
        textFieldValue.awaitAndAssertTextEquals("啊x")
    }
}


internal interface ChromeCompositeInput : СompositeInputTestSpec {
    override fun compositionStartFromInput(
        inputKey: String,
        compositionKey: String
    ): EventsSequence {
        return eventsSequence(
            keyEvent(inputKey),
            compositionStart(),
            beforeInput("insertCompositionText", inputKey),
            keyEvent(inputKey, type = "keyup", isComposing = true),
        )
    }

    override fun triggerComposingSequence(
        inputKey: String,
        compositionInput: String,
        typedKey: String
    ): EventsSequence {
        return compositionStartFromInput(inputKey, compositionInput).addAll(
            keyEvent(compositionInput),
            beforeInput("insertCompositionText", compositionInput, isComposing = true),
            compositionEnd(typedKey),
            keyEvent(compositionInput, type = "keyup")
        )
    }

    @Test
    fun `input hangul-hol`() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()
        eventsSequence(
            compositionStart(),
            keyEvent(key= "ㅎ", code = "KeyG", keyCode = 229),
            beforeInput(inputType = "compositionStart", data = "null"),
            keyEvent(key= "ㅎ", code = "KeyG", keyCode = 71, type = "keyup", isComposing = true),
            keyEvent(key= "ㅗ", code = "KeyH", keyCode = 229),
            beforeInput(inputType = "insertCompositionText", data = "호"),
            keyEvent(key= "ㅗ", code = "KeyH", keyCode = 72, type = "keyup", isComposing = true),
            keyEvent(key= "ㄹ", code = "KeyF", keyCode = 229),
            beforeInput(inputType = "insertCompositionText", data = "홀"),
            keyEvent(key= "ㄹ", code = "KeyF", keyCode = 70, type = "keyup", isComposing = true),
        ).sendToHtmlInput()

        textFieldValue.awaitAndAssertTextEquals("홀")
    }

}

internal interface FirefoxCompositeInput : СompositeInputTestSpec {
    override fun compositionStartFromInput(
        inputKey: String,
        compositionKey: String
    ): EventsSequence {
        return eventsSequence(
            inputKey.toProcessKeyDownEvent(),
            compositionStart(),
            beforeInput("insertCompositionText", inputKey),
            inputKey.toProcessKeyUpEvent(),
            keyEvent(inputKey, type = "keyup"),
        )
    }

    override fun triggerComposingSequence(
        inputKey: String,
        compositionInput: String,
        typedKey: String
    ): EventsSequence {
        return compositionStartFromInput(inputKey, compositionInput).addAll(
            keyEvent( "Process", code  = compositionInput.eventKeyCode()),
            beforeInput("insertCompositionText", typedKey, isComposing = true),
            compositionEnd(typedKey),
            keyEvent(compositionInput, type = "keyup")
        )
    }
}

internal interface WinCompositeInput : СompositeInputTestSpec {
    override fun triggerComposingSequence(
        inputKey: String,
        compositionInput: String,
        typedKey: String
    ): EventsSequence {
        return eventsSequence(
            keyEvent("Process", code = inputKey.eventKeyCode()),
            compositionStart(),
            beforeInput("insertCompositionText", inputKey),
            inputKey.toProcessKeyDownEvent(),
            keyEvent(inputKey, type = "keyup"),
            keyEvent("ㅅ", code = compositionInput),
            beforeInput("insertCompositionText", typedKey, isComposing = true),
            compositionEnd(typedKey),
            compositionInput.toProcessKeyUpEvent(),
            keyEvent("1", type = "keyup"),
        )
    }

    override fun compositionStartFromInput(inputKey: String, compositionKey: String): EventsSequence {
        return eventsSequence(
            inputKey.toProcessKeyDownEvent(),
            compositionStart(),
            beforeInput("insertCompositionText", compositionKey),
            inputKey.toProcessKeyUpEvent(),
            keyEvent(inputKey, type = "keyup"),
        )
    }

    fun EventsSequence.compositionUpdate(inputKey: String, compositionKey: String = inputKey): EventsSequence {
        return addAll(
            inputKey.toProcessKeyDownEvent(),
            compositionStart(),
            beforeInput("insertCompositionText", compositionKey),
            keyEvent(inputKey, type = "keyup"),
        )
    }

    @Test
    fun `input hangul-hol`() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()
        compositionStartFromInput("g", "ㅎ")
            .compositionUpdate("h", "호")
            .compositionUpdate("f", "홀")
            .add(compositionEnd("홀"))
            .sendToHtmlInput()

        textFieldValue.awaitAndAssertTextEquals("홀")
    }
}

internal interface SafariCompositeInput : СompositeInputTestSpec {
    override fun compositionStartFromInput(
        inputKey: String,
        compositionKey: String
    ): EventsSequence {
        return eventsSequence(
            compositionStart(),
            beforeInput("insertCompositionText", inputKey, isComposing = true),
            keyEvent(inputKey),
            keyEvent(inputKey, type = "keyup"),
        )
    }

    override fun triggerComposingSequence(
        inputKey: String,
        compositionInput: String,
        typedKey: String
    ): EventsSequence {
        return compositionStartFromInput(inputKey, compositionInput).addAll(
            compositionStart(),
            beforeInput("insertCompositionText", inputKey, isComposing = true),
            keyEvent(inputKey),
            keyEvent(inputKey, type = "keyup"),
            beforeInput("deleteCompositionText", null, isComposing = true),
            beforeInput("insertFromComposition", typedKey, isComposing = true),
            compositionEnd(typedKey),
            keyEvent(compositionInput),
            keyEvent(compositionInput, type = "keyup")
        )
    }
}

internal interface IosCompositeInput : СompositeInputTestSpec {
    override fun compositionStartFromInput(
        inputKey: String,
        compositionKey: String
    ): EventsSequence {
        return eventsSequence(
            keyEvent(inputKey, keyCode = 229),
            compositionStart(),
            beforeInput("insertCompositionText", inputKey,isComposing = true),
            keyEvent(inputKey, type = "keyup"),
        )
    }

    override fun triggerComposingSequence(
        inputKey: String,
        compositionInput: String,
        typedKey: String
    ): EventsSequence {
        return compositionStartFromInput(inputKey, compositionInput).addAll(
            beforeInput("insertCompositionText", typedKey, isComposing = true),
            beforeInput("deleteCompositionText", null, isComposing = true),
            beforeInput("insertFromComposition", typedKey, isComposing = true),
            compositionEnd(typedKey),
        )
    }

    @Test
    fun `input hangul-hol`() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()
        eventsSequence(
            keyEvent(key= "ㅎ", code = "Unidentified", keyCode = 0),
            beforeInput("insertText", data = "ㅎ", isComposing = false),
            keyEvent(key= "ㅎ", code = "Unidentified", keyCode = 0, type = "keyup"),
            keyEvent(key= "ㅗ", code = "Unidentified", keyCode = 0),
            beforeInput("deleteContentBackward", data = null, isComposing = false),
            beforeInput("insertText", data = "호", isComposing = false),
            keyEvent(key= "ㅗ", code = "Unidentified", keyCode = 0, type = "keyup"),
            keyEvent(key= "ㄹ", code = "Unidentified", keyCode = 0),
            beforeInput("deleteContentBackward", data = null, isComposing = false),
            beforeInput("insertText", data = "홀", isComposing = false),
            keyEvent(key= "ㄹ", code = "Unidentified", keyCode = 0, type = "keyup")
        ).sendToHtmlInput()

        textFieldValue.awaitAndAssertTextEquals("홀")
    }
}

internal interface AndroidCompositeInput : СompositeInputTestSpec {
    private fun String.toUnidentifiedKeyDownEvent() = keyEvent("Unidentified", code = eventKeyCode())
    private fun String.toUnidentifiedKeyUpEvent() = keyEvent("Unidentified", code = eventKeyCode(), type = "keyup")

    override fun triggerComposingSequence(
        inputKey: String,
        compositionInput: String,
        typedKey: String
    ): EventsSequence {
        return eventsSequence(
            " ".toUnidentifiedKeyDownEvent(),
            beforeInput("insertText", typedKey, isComposing = true),
            " ".toUnidentifiedKeyUpEvent()
        )
    }

    override fun compositionStartFromInput(inputKey: String, compositionKey: String): EventsSequence {
        return eventsSequence(
            inputKey.toUnidentifiedKeyDownEvent(),
            compositionStart(),
            beforeInput("insertCompositionText", compositionKey),
            inputKey.toUnidentifiedKeyUpEvent(),
        )
    }

    fun EventsSequence.compositionUpdate(inputKey: String, compositionKey: String = inputKey): EventsSequence {
        return addAll(
            inputKey.toUnidentifiedKeyDownEvent(),
            beforeInput("insertCompositionText", compositionKey),
            keyEvent(inputKey, type = "keyup"),
        )
    }

    @Test
    fun `input hangul-hol`() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()
        compositionStartFromInput("g", "ㅎ")
            .compositionUpdate("h", "호")
            .compositionUpdate("f", "홀")
            .add(compositionEnd("홀"))
            .sendToHtmlInput()

        textFieldValue.awaitAndAssertTextEquals("홀")
    }

}