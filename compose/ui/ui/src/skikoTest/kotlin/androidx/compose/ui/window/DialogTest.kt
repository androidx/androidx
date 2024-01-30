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

package androidx.compose.ui.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.DialogState
import androidx.compose.ui.FillBox
import androidx.compose.ui.Modifier
import androidx.compose.ui.assertReceived
import androidx.compose.ui.assertReceivedLast
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.touch
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.fail

@OptIn(ExperimentalTestApi::class)
class DialogTest {

    @Test
    fun dialogIsCenteredInWindow() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        val dialog = DialogState(
            IntSize(40, 40)
        )

        setContent {
            Box(Modifier.size(10.dp)) {
                dialog.Content()
            }
        }
        onNodeWithTag(dialog.tag).assertPositionInRootIsEqualTo(30.dp, 30.dp)
    }

    @Test
    fun openDialog() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        val openDialog = mutableStateOf(false)
        val background = FillBox {
            openDialog.value = true
        }
        val dialog = DialogState(
            IntSize(40, 40),
            onDismissRequest = {
                openDialog.value = false
            }
        )

        setContent {
            background.Content()
            if (openDialog.value) {
                dialog.Content()
            }
        }

        onNodeWithTag(dialog.tag).assertDoesNotExist()

        // Click (Press-Release cycle) opens popup and sends all events to "background"
        val buttons = PointerButtons(
            isPrimaryPressed = true
        )
        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f), buttons = buttons, button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(10f, 10f), button = PointerButton.Primary)
        onNodeWithTag(dialog.tag).assertIsDisplayed()

        background.events.assertReceived(PointerEventType.Press, Offset(10f, 10f))
        background.events.assertReceived(PointerEventType.Release, Offset(10f, 10f))
        background.events.assertReceived(PointerEventType.Enter, Offset(10f, 10f))
        background.events.assertReceivedLast(PointerEventType.Exit, Offset(10f, 10f))
    }

    @Test
    fun closeDialog() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        val openDialog = mutableStateOf(false)
        val background = FillBox {
            openDialog.value = true
        }
        val dialog = DialogState(
            IntSize(40, 40),
            onDismissRequest = {
                openDialog.value = false
            }
        )

        setContent {
            background.Content()
            if (openDialog.value) {
                dialog.Content()
            }
        }

        // Moving without popup generates Enter because it's in bounds
        scene.sendPointerEvent(PointerEventType.Move, Offset(15f, 15f))
        background.events.assertReceivedLast(PointerEventType.Enter, Offset(15f, 15f))

        // Open dialog
        openDialog.value = true
        onNodeWithTag(dialog.tag).assertIsDisplayed()
        background.events.assertReceivedLast(PointerEventType.Exit, Offset(15f, 15f))

        // Click (Press-Move-Release cycle) outside closes popup and sends only Enter event to background
        val buttons = PointerButtons(
            isPrimaryPressed = true
        )
        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f), buttons = buttons, button = PointerButton.Primary)
        onNodeWithTag(dialog.tag).assertIsDisplayed() // Close should happen only on Release event

        scene.sendPointerEvent(PointerEventType.Move, Offset(11f, 11f), buttons = buttons)
        scene.sendPointerEvent(PointerEventType.Release, Offset(11f, 11f), button = PointerButton.Primary)
        onNodeWithTag(dialog.tag).assertDoesNotExist()

        background.events.assertReceivedLast(PointerEventType.Enter, Offset(11f, 11f))
    }

    @Test
    fun secondClickDoesNotDismissPopup() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        val background = FillBox()
        val dialog = DialogState(
            IntSize(40, 40),
            onDismissRequest = {
                fail()
            }
        )

        setContent {
            background.Content()
            dialog.Content()
        }

        scene.sendPointerEvent(
            PointerEventType.Press,
            pointers = listOf(
                touch(50f, 50f, pressed = true, id = 1),
            )
        )
        scene.sendPointerEvent(
            PointerEventType.Press,
            pointers = listOf(
                touch(50f, 50f, pressed = true, id = 1),
                touch(10f, 10f, pressed = true, id = 2),
            )
        )
        scene.sendPointerEvent(
            PointerEventType.Release,
            pointers = listOf(
                touch(50f, 50f, pressed = false, id = 1),
                touch(10f, 10f, pressed = true, id = 2),
            )
        )
        scene.sendPointerEvent(
            PointerEventType.Release,
            pointers = listOf(
                touch(10f, 10f, pressed = false, id = 2),
            )
        )
    }

    @Test
    fun nonPrimaryButtonClickDoesNotDismissDialog() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        val background = FillBox()
        val dialog = DialogState(
            IntSize(40, 40),
            onDismissRequest = { fail() }
        )

        setContent {
            background.Content()
            dialog.Content()
        }

        val buttons = PointerButtons(
            isSecondaryPressed = true
        )
        scene.sendPointerEvent(PointerEventType.Press, Offset(10f, 10f), buttons = buttons, button = PointerButton.Secondary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(10f, 10f), button = PointerButton.Secondary)
    }
}