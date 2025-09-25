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

package androidx.compose.ui.events

import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.CompositionEventInit
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.KeyboardEventInit
import org.w3c.dom.events.MouseEvent

internal external interface KeyboardEventInitExtended : KeyboardEventInit {
    var keyCode: Int?
}

private fun KeyboardEventInit.keyEvent(type: String) = KeyboardEvent(type, this)


internal fun String.eventKeyCode(): String {
    return when {
        length == 1 && this[0] in '0'..'9' -> "Digit${this}"
        else -> "Key${this[0].uppercase()}"
    }
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
internal fun keyEvent(
    key: String,
    code: String = key.eventKeyCode(),
    keyCode: Int = when(key) {
        // Happens in Firefox, all browsers on Windows
        "Process" -> 229
        // Chrome in Android with virtual keyboard
        "Unidentified" -> 229
        else -> key.uppercase().first().code
    },
    type: String = "keydown",
    ctrlKey: Boolean = false,
    metaKey: Boolean = false,
    altKey: Boolean = false,
    shiftKey: Boolean = false,
    cancelable: Boolean = true,
    repeat: Boolean = false,
    isComposing: Boolean = false
): KeyboardEvent {
    val keyboardEventInit = KeyboardEventInit(
        key = key,
        code = code,
        ctrlKey = ctrlKey,
        metaKey = metaKey,
        altKey = altKey,
        shiftKey = shiftKey,
        cancelable = cancelable,
        repeat = repeat,
        isComposing = isComposing,
    ) as KeyboardEventInitExtended

    keyboardEventInit.keyCode = keyCode

    return keyboardEventInit
        .keyEvent(type)
}

internal fun compositionStart(data: String = "") =
    CompositionEvent("compositionstart", CompositionEventInit(data = data))

internal fun compositionEnd(data: String) =
    CompositionEvent("compositionend", CompositionEventInit(data = data))

internal fun beforeInput(inputType: String, data: String?, isComposing: Boolean = false) =
    InputEvent("beforeinput", InputEventInit(inputType = inputType, data = data, isComposing = isComposing))

private fun DummyTouchEventInit(): TouchEventInit = js("({ changedTouches: [new Touch({identifier: 0, target: document})] })")

internal fun createTouchEvent(type: String): TouchEvent {
    return TouchEvent(type, DummyTouchEventInit())
}

internal fun createMouseEvent(type: String): MouseEvent {
    return MouseEvent(type)
}


internal interface EventsSequence {
    fun add(event: Event): EventsSequence
    fun toList(): List<Event>
    fun addAll(vararg events: Event): EventsSequence
}
private class EventSequenceImplementation(events: List<Event>) : EventsSequence {
    private val events: MutableList<Event> = events.toMutableList()
    override fun add(event: Event): EventsSequence {
        events.add(event)
        return this
    }

    override fun addAll(vararg events: Event): EventsSequence {
        this.events.addAll(events.toList())
        return this
    }

    override fun toList(): List<Event> = this@EventSequenceImplementation.events
}

internal fun eventsSequence(vararg events: Event): EventsSequence = EventSequenceImplementation(events.toList())

