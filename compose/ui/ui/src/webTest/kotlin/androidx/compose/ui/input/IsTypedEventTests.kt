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

import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.ui.events.keyDownEvent
import androidx.compose.ui.input.key.toComposeEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.w3c.dom.events.KeyboardEvent

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

        chars.forEach { char -> keyDownEvent(char).assertIsTyped() }
    }

    @Test
    fun shortcutsAreNotTyped() {
        val keyDownEvents = listOf(
            keyDownEvent("c", metaKey = true, ctrlKey = true),
            keyDownEvent("p", metaKey = true, ctrlKey = true),
            keyDownEvent("v", metaKey = true, ctrlKey = true)
        )

        keyDownEvents.forEach { event -> event.assertIsNotTyped() }
    }

    @Test
    fun shortcutsWithCtrlOnlyAreNotTyped() {
        val keyDownEvents = listOf(
            keyDownEvent("c", metaKey = false, ctrlKey = true),
            keyDownEvent("p", metaKey = false, ctrlKey = true),
            keyDownEvent("v", metaKey = false, ctrlKey = true)
        )

        keyDownEvents.forEach { event -> event.assertIsNotTyped() }
    }

    @Test
    fun shortcutsWithMetaOnlyAreNotTyped() {
        val keyDownEvents = listOf(
            keyDownEvent("c", metaKey = true, ctrlKey = false),
            keyDownEvent("p", metaKey = true, ctrlKey = false),
            keyDownEvent("v", metaKey = true, ctrlKey = false)
        )

        keyDownEvents.forEach { event -> event.assertIsNotTyped() }
    }

    @Test
    fun altProducesATypedEvent() {
        val keyDownEvents = listOf(
            keyDownEvent("c", altKey = true),
            keyDownEvent("p", altKey = true),
            keyDownEvent("v", altKey = true)
        )

        keyDownEvents.forEach { event -> event.assertIsTyped() }
    }

    @Test
    fun functionalsAreNotTyped() {
        val keyDownEvents = listOf(
            keyDownEvent("Backspace", code="Backspace"),
            keyDownEvent("Clear", code="Backspace"),
            keyDownEvent("Home", code="Home"),
            keyDownEvent("End", code="End"),
            keyDownEvent("PageUp", code="PageUp"),
            keyDownEvent("PageDown", code="PageDown"),
            keyDownEvent("F1", code="F1"),
            keyDownEvent("F2", code="F2"),
            keyDownEvent("F3", code="F3"),
            keyDownEvent("F4", code="F4"),
            keyDownEvent("F5", code="F5"),
            keyDownEvent("F6", code="F6"),
            keyDownEvent("F7", code="F7"),
            keyDownEvent("F8", code="F8"),
            keyDownEvent("F9", code="F9"),
            keyDownEvent("F10", code="F10"),
            keyDownEvent("F11", code="F11"),
            keyDownEvent("F12", code="F12"),
            keyDownEvent("F13", code="F13"),
            keyDownEvent("F14", code="F14"),
            keyDownEvent("F15", code="F15"),
            keyDownEvent("F16", code="F16"),
            keyDownEvent("F17", code="F17"),
            keyDownEvent("F18", code="F18"),
            keyDownEvent("F19", code="F19"),
        )

        keyDownEvents.forEach { event -> event.assertIsNotTyped() }
    }

}