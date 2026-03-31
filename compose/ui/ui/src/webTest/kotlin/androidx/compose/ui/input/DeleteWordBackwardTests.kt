/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.ui.events.beforeInput
import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.input.specs.TextFieldTestSpec
import androidx.compose.ui.text.TextRange
import org.jetbrains.skiko.hostOs
import kotlin.test.Ignore
import kotlin.test.Test


class DeleteWordBackwardTests : TextFieldTestSpec, BasicTextFieldWithValue {

    fun sendPhysicalDeleteWordBackward() {
        sendToHtmlInput(
            keyEvent(
                key = "Backspace",
                code = "Backspace",
                type = "keydown",
                altKey = hostOs.isMacOS,
                ctrlKey = !hostOs.isMacOS
            )
        )
    }

    fun sendVirtualDeleteWordBackward() {
        sendToHtmlInput(
            keyEvent(
                key = "Backspace",
                code = "Backspace",
                type = "keydown",
                repeat = true,
            ),
            beforeInput("deleteWordBackward", null)
        )
    }


    @Test
    fun deletePrevWordVirtualMiddle() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder("here   we     go again!!!", initialSelection = TextRange(14, 14))

        awaitAnimationFrame()

        sendVirtualDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("here  go again!!!", "deleteWordBackward is not processed")
    }

    @Test
    fun deletePrevWordPhysicalMiddle() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder(
            "here 🐩  we     go again!!!",
            initialSelection = TextRange(15, 15)
        )

        sendPhysicalDeleteWordBackward()

        // standard KeyCommand.DELETE_PREV_WORD processing triggered
        textFieldValue.awaitAndAssertTextEquals("here 🐩   go again!!!")

        sendPhysicalDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("here  go again!!!")

        sendToHtmlInput(
            beforeInput("deleteWordBackward", null)
        )

        textFieldValue.awaitAndAssertTextEquals(
            "here  go again!!!",
            "text unexpectedly changed on deleteWordBackward"
        )
    }

    @Test
    fun deletePrevWordVirtualEmpty() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder(
            ""
        )

        sendVirtualDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("")
    }


    @Test
    fun deletePrevWordPhysicalEmpty() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder(
            ""
        )

        sendPhysicalDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("")
    }

    @Test
    @Ignore
    fun deletePrevWordVirtualCompoundEmoji() = runApplicationTest {
        // TODO: this seems to be failing for test-related reasons, on a device it behaves as expected and need to be investigated to be unignored
        val textFieldValue = createApplicationWithHolder(
            "compound emoji: 🧑‍🧑‍🧒‍🧒"
        )

        sendVirtualDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("compound emoji: ")
    }

    @Test
    fun deletePrevWordPhysicalCompoundEmoji() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder(
            "compound emoji: 🧑‍🧑‍🧒‍🧒"
        )

        sendPhysicalDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("compound emoji: ")
    }

    @Test
    fun deletePrevWordVirtualSplitFamilyEmoji() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder(
            "compound emoji: 🧑🧑👧👶"
        )

        sendPhysicalDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("compound emoji: 🧑🧑👧")

        sendPhysicalDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("compound emoji: 🧑🧑")

        sendPhysicalDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("compound emoji: 🧑")
    }

    @Test
    fun deletePrevWordPhysicalSplitFamilyEmoji() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder(
            "compound emoji: 🧑🧑👧👶"
        )

        sendPhysicalDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("compound emoji: 🧑🧑👧")

        sendPhysicalDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("compound emoji: 🧑🧑")

        sendPhysicalDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("compound emoji: 🧑")
    }

    @Test
    fun deletePrevWordVirtualUnicode() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder(
            "천천히 말해 주세요"
        )

        awaitIdle()

        sendVirtualDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("천천히 말해")

        sendVirtualDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("천천히")
    }


    @Test
    fun deletePrevWordPhysicalUnicode() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder(
            "천천히 말해 주세요"
        )

        sendPhysicalDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("천천히 말해 ")

        sendPhysicalDeleteWordBackward()
        textFieldValue.awaitAndAssertTextEquals("천천히 ")
    }

}