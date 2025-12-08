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
@file:Suppress("INVISIBLE_REFERENCE")

package androidx.compose.ui.input

import androidx.compose.foundation.InternalFoundationApi
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.input.key.toComposeEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.w3c.dom.events.KeyboardEvent

@OptIn(InternalFoundationApi::class)
class IsTypedEventTests {
    private fun KeyboardEvent.assertIsTyped(message: String? = null) {
        val composeEvent = toComposeEvent()
        assertTrue(composeEvent.isTypedEvent, message ?: "event ${composeEvent} supposed to be typed but actually is not")
    }

    private fun KeyboardEvent.assertIsNotTyped(message: String? = null) {
        val composeEvent = toComposeEvent()
        assertFalse(composeEvent.isTypedEvent, message ?: "event ${composeEvent} not supposed to be typed but actually is")
    }

    @Test
    fun charsAreTyped() {
        val chars = listOf(
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
            "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",

            "a", "б", "в", "г", "д", "е", "ё", "ж", "з", "и", "й", "к",
            "л", "м", "н", "о", "п", "р", "с", "т", "у", "ф", "х", "ц",
            "ш", "щ", "ь", "ъ", "э", "ю", "я"
        )

        chars.forEach { char -> keyEvent(char).assertIsTyped() }
    }

    @Test
    fun shortcutsAreNotTyped() {
        val keyDownEvents = listOf(
            keyEvent("c", metaKey = true, ctrlKey = true),
            keyEvent("p", metaKey = true, ctrlKey = true),
            keyEvent("v", metaKey = true, ctrlKey = true)
        )

        keyDownEvents.forEach { event -> event.assertIsNotTyped() }
    }

    @Test
    fun shortcutsWithCtrlOnlyAreNotTyped() {
        val keyDownEvents = listOf(
            keyEvent("c", metaKey = false, ctrlKey = true),
            keyEvent("p", metaKey = false, ctrlKey = true),
            keyEvent("v", metaKey = false, ctrlKey = true)
        )

        keyDownEvents.forEach { event -> event.assertIsNotTyped() }
    }

    @Test
    fun shortcutsWithMetaOnlyAreNotTyped() {
        val keyDownEvents = listOf(
            keyEvent("c", metaKey = true, ctrlKey = false),
            keyEvent("p", metaKey = true, ctrlKey = false),
            keyEvent("v", metaKey = true, ctrlKey = false)
        )

        keyDownEvents.forEach { event -> event.assertIsNotTyped() }
    }

    @Test
    fun altProducesATypedEvent() {
        val keyDownEvents = listOf(
            keyEvent("c", altKey = true),
            keyEvent("p", altKey = true),
            keyEvent("v", altKey = true)
        )

        keyDownEvents.forEach { event -> event.assertIsTyped() }
    }

    @Test
    fun functionalsAreNotTyped() {
        val keyDownEvents = listOf(
            keyEvent("Backspace", code="Backspace"),
            keyEvent("Clear", code="Backspace"),
            keyEvent("Home", code="Home"),
            keyEvent("End", code="End"),
            keyEvent("PageUp", code="PageUp"),
            keyEvent("PageDown", code="PageDown"),
            keyEvent("F1", code="F1"),
            keyEvent("F2", code="F2"),
            keyEvent("F3", code="F3"),
            keyEvent("F4", code="F4"),
            keyEvent("F5", code="F5"),
            keyEvent("F6", code="F6"),
            keyEvent("F7", code="F7"),
            keyEvent("F8", code="F8"),
            keyEvent("F9", code="F9"),
            keyEvent("F10", code="F10"),
            keyEvent("F11", code="F11"),
            keyEvent("F12", code="F12"),
            keyEvent("F13", code="F13"),
            keyEvent("F14", code="F14"),
            keyEvent("F15", code="F15"),
            keyEvent("F16", code="F16"),
            keyEvent("F17", code="F17"),
            keyEvent("F18", code="F18"),
            keyEvent("F19", code="F19"),
        )

        keyDownEvents.forEach { event -> event.assertIsNotTyped() }
    }

}