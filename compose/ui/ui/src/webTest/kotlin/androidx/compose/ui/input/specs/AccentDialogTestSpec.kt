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

import androidx.compose.ui.events.beforeInput
import androidx.compose.ui.events.keyEvent
import kotlin.test.Ignore
import kotlin.test.Test

internal interface AccentDialogTestSpec : TextFieldTestSpec {
    @Ignore
    @Test
    fun repeatedAccent() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()

        sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", type = "keyup"),
            keyEvent("b"),
            beforeInput("insertText", "b"),
            keyEvent("b", type = "keyup"),
            keyEvent("c"),
            beforeInput("insertText", "c"),
            keyEvent("c", type = "keyup")
        )

        // TODO: this does not behave as desktop, ideally we should have "abc" here
        textFieldValue.awaitAndAssertTextEquals(
            "bc",
            "Repeat mode should be resolved as Accent Dialogue"
        )

        sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", type = "keyup"),
            keyEvent("b"),
            beforeInput("insertText", "b"),
            keyEvent("b", type = "keyup"),
            keyEvent("c"),
            beforeInput("insertText", "c"),
            keyEvent("c", type = "keyup")
        )

    }

    @Test
    fun repeatedDefault() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()

        sendToHtmlInput(
            keyEvent("a"),
            beforeInput("insertText", "a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("b"),
            beforeInput(inputType = "insertText", data = "b"),
            keyEvent("c"),
            beforeInput(inputType = "insertText", data = "c"),
        )


        textFieldValue.awaitAndAssertTextMatches( Regex("a+bc"), "Repeat mode should be resolved as Default")
    }

    @Test
    fun repeatedAccentMenuPressed() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()

        sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", type = "keyup"),
            keyEvent("1"),
            beforeInput(inputType = "insertText", data = "à"),
            keyEvent("1", type = "keyup"),
        )

        textFieldValue.awaitAndAssertTextEquals("à", "Choose symbol from Accent Menu")
    }

    @Test
    fun repeatedAccentMenuIgnoreNonTyped() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()

        sendToHtmlInput(
            keyEvent("ArrowLeft", code = "ArrowLeft"),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", type = "keyup"),
            keyEvent("a"),
            beforeInput(inputType = "insertText", data = "a"),
            keyEvent("a", type = "keyup"),
            keyEvent("b"),
            beforeInput(inputType = "insertText", data = "b"),
            keyEvent("b", type = "keyup"),
            keyEvent("c"),
            beforeInput(inputType = "insertText", data = "c"),
            keyEvent("c", type = "keyup"),
        )

        textFieldValue.awaitAndAssertTextEquals("abc", "XXX")
    }

    @Test
    fun repeatedAccentMenuClicked() = runApplicationTest {
        val textFieldValue =  createApplicationWithHolder()

        sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", type = "keyup"),
            beforeInput(inputType = "insertText", data = "æ"),
        )

        textFieldValue.awaitAndAssertTextEquals("æ", "Choose symbol from Accent Menu")
    }

}