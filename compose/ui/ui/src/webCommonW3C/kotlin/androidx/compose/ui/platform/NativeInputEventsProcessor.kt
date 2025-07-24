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

import androidx.compose.runtime.TestOnly
import androidx.compose.ui.input.key.toComposeEvent
import androidx.compose.ui.text.input.BackspaceCommand
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.TextFieldValue
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/**
 * Processes native input events and handles their translation to commands
 * for Compose-based input handling and text editing. This class aggregates
 * events such as keyboard, composition, and input events, managing their
 * scheduling and execution in defined checkpoints.
 *
 * @param composeSender The communicator responsible for transmitting edit
 * commands and keyboard events to the Compose system.
 */
internal abstract class NativeInputEventsProcessor(
    private val composeSender: ComposeCommandCommunicator
) {

    private val collectedEvents = mutableListOf<Event>()
    private var isCheckpointScheduled = false
    private var lastCompositionEndTimestamp = 0.0 // Double because of k/wasm where Number.toLong() leads to a compilation error

    /**
     * Schedules a checkpoint for processing input events.
     *
     * Realistically, it would schedule the checkpoint to the next Animation Frame:
     * window.requestAnimationFrame { runCheckpoint(...) }.
     *
     * But we keep it abstract to simplify the testing (unit tests).
     */
    abstract fun scheduleCheckpoint()

    private fun internalScheduleCheckpoint() {
        if (!isCheckpointScheduled) {
            scheduleCheckpoint()
        }
    }

    fun runCheckpoint(currentTextFieldValue: TextFieldValue) {
        isCheckpointScheduled = false

        collectedEvents.sortBy { it.timeStamp.toInt() }

        val isInIMEComposition = collectedEvents.any {
            it.type == "compositionstart"
                || it.type == "compositionupdate"
                || it.type == "compositionend"
                || it.type == "keydown" && (it as KeyboardEvent).isComposing
                || it.type == "beforeinput" && (it as InputEvent).isComposing
        }

        var keydownEvent: KeyboardEvent? = null
        var compositionEndEvt: CompositionEvent? = null

        collectedEvents.forEach { evt ->
            val eventName = evt.type

            when (eventName) {
                "keydown" -> {
                    keydownEvent = evt as KeyboardEvent
                    val isTypedEvent = isTypedEvent(keydownEvent)
                    val isFromLastComposition =
                        keydownEvent.timeStamp.toDouble() < lastCompositionEndTimestamp
                    if (!isInIMEComposition && !isTypedEvent && !isFromLastComposition) {
                        composeSender.sendKeyboardEvent(keydownEvent.toComposeEvent())
                    }
                }

                "compositionend" -> {
                    compositionEndEvt = evt as CompositionEvent
                    lastCompositionEndTimestamp = evt.timeStamp.toDouble()
                    composeSender.sendEditCommand(CommitTextCommand(compositionEndEvt.data, 1))
                }

                "beforeinput" -> {
                    evt as InputEvent
                    val inputType = evt.inputType
                    val data = evt.data

                    val editCommands = mutableListOf<EditCommand>()
                    when (inputType) {
                        "insertFromComposition", "deleteCompositionText" -> {
                            // We see these events in Safari just before 'compositionEnd' event.
                            // We do nothing here, because Safari also sends 'insertCompositionText' which we handle,
                            // and the behavior is as expected atm. We also handle 'compositionEnd'.
                        }

                        "deleteContentBackward" -> {
                            // If it's "Backspace", then it's handled earlier in "keydown" above, so skipping it here
                            if (keydownEvent?.key != "Backspace") {
                                if (!currentTextFieldValue.selection.collapsed) {
                                    // Likely it's on mobile, where the Backspace has Unidentified key value.
                                    // When Compose TextField shows text selection,
                                    // a good UX for deleteContentBackward would be to emulate Backspace
                                    editCommands.add(BackspaceCommand())
                                } else {
                                    // This happens when an autocorrection is applied on mobile:
                                    // The system first tells us to delete the old text,
                                    // and then it would send the "insertText" event.
                                    val deleteSize = evt.deleteContentBackwardSize
                                    if (deleteSize > 0) {
                                        editCommands.add(DeleteSurroundingTextCommand(deleteSize, 0))
                                    }
                                }
                            }
                        }

                        "insertReplacementText" -> if (data != null) {
                            val deleteSize = evt.deleteContentBackwardSize
                            if (deleteSize > 0) {
                                editCommands.add(DeleteSurroundingTextCommand(deleteSize, 0))
                            }
                            editCommands.add(CommitTextCommand(data, 1))
                        }

                        "insertText" -> if (data != null) {
                            val deleteSize = evt.deleteContentBackwardSize
                            if (deleteSize > 0 && currentTextFieldValue.selection.collapsed) {
                                editCommands.add(DeleteSurroundingTextCommand(deleteSize, 0))
                            }

                            editCommands.add(CommitTextCommand(data, 1))
                        }

                        "insertCompositionText" -> if (data != null) {
                            val deleteSize = evt.deleteContentBackwardSize
                            if (deleteSize > 0) {
                                editCommands.add(DeleteSurroundingTextCommand(deleteSize, 0))
                            }
                            editCommands.add(SetComposingTextCommand(data, 1))
                        }
                    }
                    composeSender.sendEditCommand(editCommands)
                }
            }
        }

        collectedEvents.clear()
    }

    internal fun addInputEvent(event: InputEvent, deleteContentBackwardSize: Int = 0) {
        if (deleteContentBackwardSize > 0) {
            event.deleteContentBackwardSize = deleteContentBackwardSize
        }
        collectedEvents.add(event)
        internalScheduleCheckpoint()
    }

    internal fun addKeyEvent(event: KeyboardEvent) {
        collectedEvents.add(event)
        internalScheduleCheckpoint()
    }

    internal fun addCompositionEvent(event: CompositionEvent) {
        collectedEvents.add(event)
        internalScheduleCheckpoint()
    }

    @TestOnly
    internal fun getCollectedEvents(): List<Event> = collectedEvents
}