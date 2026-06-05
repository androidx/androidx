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

package androidx.compose.ui.platform

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.BackspaceCommand
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditingBuffer
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.input.key.InternalKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.events.beforeInput
import androidx.compose.ui.events.compositionEnd
import androidx.compose.ui.events.compositionStart
import androidx.compose.ui.events.keyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.KeyboardEvent

// There is a helper for testing:
// - Run the mpp demo in a browser
// - then open http://localhost:8080/input-events/ - you can type in the input and see the events received by it.
// The events logs can be helpful when writing the tests.
// Note: the order of events is different in different browsers (at least for Composite input).
// So try in all of them, and also in mobile browsers :D
class NativeInputEventsProcessorTest {

    /**
     * A mock implementation of ComposeCommandCommunicator for testing
     */
    private open class MockComposeCommandCommunicator(
        startingTextFieldValue: TextFieldValue = TextFieldValue("")
    ) : ComposeCommandCommunicator {

        private val editingBuffer = EditingBuffer(
            text = startingTextFieldValue.annotatedString,
            selection = startingTextFieldValue.selection
        )

        val editCommands = mutableListOf<EditCommand>()
        val keyboardEvents = mutableListOf<KeyEvent>()

        private val fontFamilyResolver = createFontFamilyResolver()

        override fun sendEditCommand(commands: List<EditCommand>) {
            editCommands.addAll(commands)
            commands.forEach { it.applyTo(editingBuffer) }
        }

        override fun sendKeyboardEvent(keyboardEvent: KeyEvent): Boolean {
            keyboardEvents.add(keyboardEvent)
            return true
        }

        override fun currentTextLayoutResult(): TextLayoutResult? {
            val text = editingBuffer.toString()
            val annotatedString = AnnotatedString(text)
            val density = Density(1f)
            val constraints = Constraints()
            val style = TextStyle.Default

            return TextLayoutResult(
                layoutInput = TextLayoutInput(
                    text = annotatedString,
                    style = style,
                    placeholders = emptyList(),
                    maxLines = Int.MAX_VALUE,
                    softWrap = true,
                    overflow = TextOverflow.Clip,
                    density = density,
                    layoutDirection = LayoutDirection.Ltr,
                    fontFamilyResolver = fontFamilyResolver,
                    constraints = constraints
                ),
                multiParagraph = MultiParagraph(
                    annotatedString = annotatedString,
                    style = style,
                    constraints = constraints,
                    density = density,
                    fontFamilyResolver = fontFamilyResolver
                ),
                size = IntSize(0, 0)
            )
        }

        @Suppress("INVISIBLE_REFERENCE")
        fun currentTextFieldValue(): TextFieldValue {
            return TextFieldValue(
                text = editingBuffer.toString(),
                selection = TextRange(editingBuffer.selectionStart, editingBuffer.selectionEnd)
            )
        }
    }

    /**
     * A test implementation of NativeInputEventsProcessor that allows controlling when checkpoints are run
     */
    private class TestNativeInputEventsProcessor(
        composeSender: ComposeCommandCommunicator
    ) : NativeInputEventsProcessor(composeSender) {

        override fun scheduleCheckpoint() {
            isCheckpointScheduled = true
        }

        fun manuallyRunCheckpoint(currentTextFieldValue: TextFieldValue) {
            isCheckpointScheduled = false
            runCheckpoint(currentTextFieldValue)
        }
    }

    @Test
    fun testCheckpointScheduling() {
        val communicator = MockComposeCommandCommunicator()
        val processor = TestNativeInputEventsProcessor(communicator)
        assertFalse(processor.isCheckpointScheduled)

        processor.registerEvent(keyEvent("a"))
        assertTrue(processor.isCheckpointScheduled)

        processor.isCheckpointScheduled = false
        val compositionEvent = compositionStart()
        processor.registerEvent(compositionEvent)
        assertTrue(processor.isCheckpointScheduled)

        processor.isCheckpointScheduled = false
        processor.registerEvent(beforeInput("insertText", "") as InputEvent)
        assertTrue(processor.isCheckpointScheduled)

        assertEquals(3, processor.getCollectedEvents().size)
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())
        assertEquals(0, processor.getCollectedEvents().size)
    }

    @Test
    fun testKeyArrowEventProcessing() {
        val communicator = MockComposeCommandCommunicator()
        val processor = TestNativeInputEventsProcessor(communicator)

        processor.registerEvent(keyEvent("ArrowLeft"))
        processor.registerEvent(keyEvent("ArrowLeft", type = "keyup"))
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(1, communicator.keyboardEvents.size)
        val composeKeyEvent = communicator.keyboardEvents[0].nativeKeyEvent as InternalKeyEvent
        assertEquals("ArrowLeft", (composeKeyEvent.nativeEvent as KeyboardEvent).key)
    }

    @Test
    fun testCompositionEndEventProcessing() {
        val communicator = MockComposeCommandCommunicator()
        val processor = TestNativeInputEventsProcessor(communicator)

        processor.registerEvent(compositionEnd("test"))
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(1, communicator.editCommands.size)
        val command = communicator.editCommands[0]
        assertTrue(command is CommitTextCommand)
        assertEquals("test", command.text)
    }

    @Test
    fun testInsertText() {
        val communicator = MockComposeCommandCommunicator(
            TextFieldValue("qwerty", selection = TextRange( 6))
        )
        val processor = TestNativeInputEventsProcessor(communicator)

        processor.registerEvent(beforeInput("insertText", "a") as InputEvent)
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(1, communicator.editCommands.size)
        val command = communicator.editCommands[0]
        assertTrue(command is CommitTextCommand)
        assertEquals("a", command.text)

        assertEquals("qwertya", communicator.currentTextFieldValue().text)
    }

    @Test
    fun testInsertText_with_deleteContentBackward() {
        val communicator = MockComposeCommandCommunicator(
            TextFieldValue("test", selection = TextRange( 4))
        )
        val processor = TestNativeInputEventsProcessor(communicator)

        processor.registerEvent(
            beforeInput("insertText", "a").asInputEventExt().apply {
                    textRangeStart = 3
                    textRangeEnd = 4
            }
        )
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(2, communicator.editCommands.size)
        val command1 = communicator.editCommands[0]
        assertTrue(command1 is SetSelectionCommand)
        assertEquals(3, command1.start)
        assertEquals(4, command1.end)

        val command2 = communicator.editCommands[1]
        assertTrue(command2 is CommitTextCommand)
        assertEquals("a", command2.text)

        assertEquals("tesa", communicator.currentTextFieldValue().text)
    }

    @Test
    fun testDeleteContentBackward() {
        val communicator = MockComposeCommandCommunicator(
            TextFieldValue("test", selection = TextRange( 4))
        )
        val processor = TestNativeInputEventsProcessor(communicator)

        processor.registerEvent(
            beforeInput("deleteContentBackward", "").asInputEventExt().apply {
                textRangeStart = 3
                textRangeEnd = 4
            }
        )
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(2, communicator.editCommands.size)
        val command = communicator.editCommands[0]
        assertTrue(command is SetSelectionCommand)
        assertEquals(3, command.start)
        assertEquals(4, command.end)

        assertEquals("tes", communicator.currentTextFieldValue().text)
    }

    @Test
    fun testInsertCompositionText() {
        val communicator = MockComposeCommandCommunicator()
        val processor = TestNativeInputEventsProcessor(communicator)

        processor.registerEvent(beforeInput("insertCompositionText", "test") as InputEvent)
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        // Verify that the input event was processed and sent to the communicator
        assertEquals(1, communicator.editCommands.size)
        val command = communicator.editCommands[0]
        assertTrue(command is SetComposingTextCommand)
        assertEquals("test", command.text)

        assertEquals("test", communicator.currentTextFieldValue().text)
    }

    @Test
    fun when_NonCollapsedSelection_And_Backspace_Pressed_then_deleteContentBackward_is_ignored() {
        val communicator = MockComposeCommandCommunicator()
        val processor = TestNativeInputEventsProcessor(communicator)

        val backspaceEvent = keyEvent(
            key = "Backspace",
            code = "Backspace",
            type = "keydown"
        )
        processor.registerEvent(backspaceEvent)

        processor.registerEvent(
            beforeInput("deleteContentBackward", null).asInputEventExt().apply {
                textRangeStart = 3
                textRangeEnd = 4
            }
        )
        processor.manuallyRunCheckpoint(TextFieldValue("test", selection = TextRange(3, 4)))

        assertEquals(1, communicator.keyboardEvents.size, "exactly one key event should be sent")
        assertEquals(0, communicator.editCommands.size, "editCommands should not be sent")

        val sentKeyEvent = communicator.keyboardEvents[0]
        assertEquals(
            "Backspace",
            ((sentKeyEvent.nativeKeyEvent as InternalKeyEvent).nativeEvent as KeyboardEvent).key,
            "keyboardEvent for Backspace should be sent"
        )
    }

    @Test
    fun testInsertReplacementText() {
        val communicator = MockComposeCommandCommunicator(
            TextFieldValue("test text", selection = TextRange( 9))
        )
        val processor = TestNativeInputEventsProcessor(communicator)

        processor.registerEvent(
            beforeInput("insertReplacementText", "replacement").asInputEventExt().apply {
                textRangeStart = 5
                textRangeEnd = 9
            },
        )

        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())
        assertEquals(2, communicator.editCommands.size)

        val deleteCommand = communicator.editCommands[0]
        assertTrue(deleteCommand is SetSelectionCommand)
        assertEquals(5, deleteCommand.start)
        assertEquals(9, deleteCommand.end)

        val commitCommand = communicator.editCommands[1]
        assertTrue(commitCommand is CommitTextCommand)
        assertEquals("replacement", commitCommand.text)

        assertEquals("test replacement", communicator.currentTextFieldValue().text)
    }

    @Test
    fun testAccentDialogInteraction() {
        val communicator = MockComposeCommandCommunicator()
        val processor = TestNativeInputEventsProcessor(communicator)

        // 1. Simulate a key being long-pressed (e.g., 'e' key)
        // First keydown without repeat
        processor.registerEvent(
            event = keyEvent(key = "e", type = "keydown", repeat = false)
        )
        // the first keydown is followed by the insertText
        processor.registerEvent(beforeInput("insertText", "e") as InputEvent)
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(1, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        // Several keydown events with repeat=true to simulate long press
        repeat(3) {
            processor.registerEvent(
                event = keyEvent(key = "e", type = "keydown", repeat = true)
            )
            processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())
        }

        assertEquals(1, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        processor.registerEvent(keyEvent(key = "e", type = "keyup"))
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(1, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        // 2. Simulate selecting an accent by pressing a number key (e.g., '2' for é)
        processor.registerEvent(event = keyEvent(key = "2", code = "Digit2"))

        // 3. Simulate the input event for the accented character
        processor.registerEvent(
            beforeInput("insertText", "é").asInputEventExt().apply {
                textRangeStart = 0
                textRangeEnd = 1
            }
        )

        // Process the events
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        // First check that keydown events were forwarded
        // We expect only non-repeat keydown events to be forwarded
        assertEquals(3, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        val commitCommand1 = communicator.editCommands[0]
        assertTrue(commitCommand1 is CommitTextCommand)
        assertEquals("e", commitCommand1.text)

        val deleteCommand = communicator.editCommands[1]
        assertTrue(deleteCommand is SetSelectionCommand)
        assertEquals(0, deleteCommand.start)
        assertEquals(1, deleteCommand.end)

        val commitCommand2 = communicator.editCommands[2]
        assertTrue(commitCommand2 is CommitTextCommand)
        assertEquals("é", commitCommand2.text)

        assertEquals("é", communicator.currentTextFieldValue().text)
    }

    @Test
    fun testAccentDialogInteractionWithMouseSelection() {
        val communicator = MockComposeCommandCommunicator()
        val processor = TestNativeInputEventsProcessor(communicator)

        // 1. Simulate a key being long-pressed (e.g., 'e' key)
        // First keydown without repeat
        processor.registerEvent(
            event = keyEvent(key = "e", type = "keydown", repeat = false)
        )
        // the first keydown is followed by the insertText
        processor.registerEvent(beforeInput("insertText", "e") as InputEvent)
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(1, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        // Several keydown events with repeat=true to simulate long press
        repeat(3) {
            processor.registerEvent(
                event = keyEvent(key = "e", type = "keydown", repeat = true)
            )
            processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())
        }

        assertEquals(1, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        // Key up event when the accent dialog appears
        processor.registerEvent(keyEvent(key = "e", type = "keyup"))
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(1, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        // 2. Simulate choosing 'é' from the accent dialogues using a mouse, so no keydown events here
        processor.registerEvent(
            beforeInput("insertText", "è").asInputEventExt().apply {
                // to replace `e`
                textRangeStart = 0
                textRangeEnd = 1
            },
        )

        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(3, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        val commitCommand1 = communicator.editCommands[0]
        assertTrue(commitCommand1 is CommitTextCommand)
        assertEquals("e", commitCommand1.text)

        val deleteCommand = communicator.editCommands[1]
        assertTrue(deleteCommand is SetSelectionCommand)
        assertEquals(0, deleteCommand.start)
        assertEquals(1, deleteCommand.end)

        val commitCommand2 = communicator.editCommands[2]
        assertTrue(commitCommand2 is CommitTextCommand)
        assertEquals("è", commitCommand2.text)

        assertEquals("è", communicator.currentTextFieldValue().text)
    }

    @Test
    fun testAccentDialogInteractionWithKeyboardNavigation() {
        val communicator = MockComposeCommandCommunicator()
        val processor = TestNativeInputEventsProcessor(communicator)

        // 1. Simulate a key being long-pressed (e.g., 'e' key)
        // First keydown without repeat
        processor.registerEvent(
            event = keyEvent(key = "e", type = "keydown", repeat = false)
        )
        // the first keydown is followed by the insertText
        processor.registerEvent(beforeInput("insertText", "e") as InputEvent)
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(1, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        // Several keydown events with repeat=true to simulate long press
        repeat(3) {
            processor.registerEvent(
                event = keyEvent(key = "e", type = "keydown", repeat = true)
            )
            processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())
        }

        assertEquals(1, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        // Key up event when the accent dialog appears
        processor.registerEvent(keyEvent(key = "e", type = "keyup"))
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(1, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        // 2. Simulate arrow navigation through accent options
        processor.registerEvent(keyEvent(key = "ArrowRight", code = "ArrowRight", isComposing = true))
        processor.registerEvent(
            beforeInput("insertText", "è", isComposing = true).asInputEventExt().apply {
                textRangeStart = 0
                textRangeEnd = 1
            }
        )
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())
        assertEquals(3, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        processor.registerEvent(keyEvent(key = "ArrowRight", code = "ArrowRight", isComposing = true))
        processor.registerEvent(
            beforeInput("insertCompositionText", "é").asInputEventExt().apply {
                textRangeStart = 0
                textRangeEnd = 1
            }
        )

        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())
        assertEquals(5, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        processor.registerEvent(keyEvent(key = "ArrowRight", code = "ArrowRight", isComposing = true))
        processor.registerEvent(
            beforeInput("insertCompositionText", "ê").asInputEventExt().apply {
                textRangeStart = 0
                textRangeEnd = 1
            }
        )

        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())
        assertEquals(7, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        // Press ArrowLeft once to move back left
        processor.registerEvent(keyEvent(key = "ArrowLeft", code = "ArrowLeft", isComposing = true))
        processor.manuallyRunCheckpoint(TextFieldValue())

        processor.manuallyRunCheckpoint(TextFieldValue())
        assertEquals(7, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        processor.registerEvent(
            beforeInput("insertCompositionText", "é").asInputEventExt().apply {
                textRangeStart = 0
                textRangeEnd = 1
            }
        )

        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())
        assertEquals(9, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        // 3. Press Enter to confirm selection
        processor.registerEvent(keyEvent(key = "Enter", code = "Enter"))

        // 4. Simulate the input event for the selected accented character
        processor.registerEvent(
            beforeInput("insertCompositionText", "é").asInputEventExt().apply {
                textRangeStart = 0
                textRangeEnd = 1
            }
        )

        processor.registerEvent(compositionEnd("é"))
        processor.manuallyRunCheckpoint(TextFieldValue("e"))

        assertEquals(12, communicator.editCommands.size)
        assertEquals(0, communicator.keyboardEvents.size)

        assertTrue(communicator.editCommands[11] is CommitTextCommand)
        assertEquals("é", (communicator.editCommands[11] as CommitTextCommand).text)

        assertEquals("é", communicator.currentTextFieldValue().text)
    }

    @Test
    fun testDeleteContentBackward_with_non_collapsed_selection() {
        // Create a TextFieldValue with a non-collapsed selection
        val textFieldValue = TextFieldValue(
            text = "example text",
            selection = TextRange(2, 7) // Selection from "a" to "e" in "example"
        )
        val communicator = MockComposeCommandCommunicator(textFieldValue)
        val processor = TestNativeInputEventsProcessor(communicator)

        // Add deleteContentBackward event
        processor.registerEvent(
            beforeInput("deleteContentBackward", "") as InputEvent
        )

        // Process the event with a non-collapsed selection
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(1, communicator.editCommands.size)
        val command = communicator.editCommands[0]
        assertTrue(command is BackspaceCommand)

        assertEquals("ex text", communicator.currentTextFieldValue().text)
    }

    @Test
    fun testDeleteContentBackward_with_collapsed_selection() {
        // Create a TextFieldValue with a collapsed selection
        val textFieldValue = TextFieldValue(
            text = "example text",
            selection = TextRange(5) // Cursor after "p" in "example"
        )

        val communicator = MockComposeCommandCommunicator(textFieldValue)
        val processor = TestNativeInputEventsProcessor(communicator)

        // Add deleteContentBackward event
        processor.registerEvent(
            beforeInput("deleteContentBackward", "").asInputEventExt().apply {
                textRangeStart = 3
                textRangeEnd = 5
            },
        )

        // Process the event with a collapsed selection
        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(2, communicator.editCommands.size)
        val command = communicator.editCommands[0]
        assertTrue(command is SetSelectionCommand)
        assertEquals(3, command.start)
        assertEquals(5, command.end)

        assertEquals("exale text", communicator.currentTextFieldValue().text)
    }

    @Test
    fun testDeleteContentBackward_with_Backspace_key_pressed_same_frame() {
        val communicator = MockComposeCommandCommunicator()
        val processor = TestNativeInputEventsProcessor(communicator)

        // First add a keydown event for Backspace
        val backspaceEvent = keyEvent(
            key = "Backspace",
            code = "Backspace",
            type = "keydown"
        )
        processor.registerEvent(backspaceEvent)

        // Then add a deleteContentBackward event
        processor.registerEvent(
            beforeInput("deleteContentBackward", "").asInputEventExt().apply {
                textRangeStart = 0
                textRangeEnd = 1
            },
        )

        // With a non-collapsed selection
        val textFieldValue = TextFieldValue(
            text = "example text",
            selection = TextRange(2, 7) // Selection from "a" to "e" in "example"
        )

        processor.manuallyRunCheckpoint(textFieldValue)

        // The deleteContentBackward event should be ignored since Backspace key was pressed
        assertEquals(1, communicator.keyboardEvents.size)
        assertEquals(0, communicator.editCommands.size)
    }

    @Test
    fun testDeleteContentBackward_with_collapsed_selection_and_Backspace_key_pressed_same_frame() {
        val textFieldValue = TextFieldValue(
            text = "example text",
            selection = TextRange(12) // collapsed selection at the very end
        )

        val communicator = MockComposeCommandCommunicator(textFieldValue)
        val processor = TestNativeInputEventsProcessor(communicator)

        val backspaceEvent = keyEvent(
            key = "Backspace",
            code = "Backspace",
            type = "keydown"
        )
        processor.registerEvent(backspaceEvent)

        processor.registerEvent(
            beforeInput("deleteContentBackward", "").asInputEventExt().apply {
                textRangeStart = 8
                textRangeEnd = 12
            },
        )

        processor.manuallyRunCheckpoint(communicator.currentTextFieldValue())

        assertEquals(1, communicator.keyboardEvents.size)
        assertEquals(2, communicator.editCommands.size)

        val selectionCommand = communicator.editCommands[0]
        assertTrue(selectionCommand is SetSelectionCommand)
        assertEquals(8, selectionCommand.start)
        assertEquals(12, selectionCommand.end)

        assertEquals("example ", communicator.currentTextFieldValue().text)
    }

    @Test
    fun testDeleteContentBackward_with_Backspace_key_pressed_different_frame() {
        val communicator = MockComposeCommandCommunicator()
        val processor = TestNativeInputEventsProcessor(communicator)

        // With a non-collapsed selection
        val textFieldValue = TextFieldValue(
            text = "example text",
            selection = TextRange(2, 7) // Selection from "a" to "e" in "example"
        )

        // First add a keydown event for Backspace
        val backspaceEvent = keyEvent(
            key = "Backspace",
            code = "Backspace",
            type = "keydown"
        )
        processor.registerEvent(backspaceEvent)

        processor.manuallyRunCheckpoint(textFieldValue)

        // Then add a deleteContentBackward event
        processor.registerEvent(
            beforeInput("deleteContentBackward", "").asInputEventExt().apply {
                textRangeStart = 0
                textRangeEnd = 1
            },
        )

        processor.manuallyRunCheckpoint(textFieldValue)

        // The deleteContentBackward event should be ignored since Backspace key was pressed
        assertEquals(1, communicator.keyboardEvents.size)
        assertEquals(0, communicator.editCommands.size)
    }

}
