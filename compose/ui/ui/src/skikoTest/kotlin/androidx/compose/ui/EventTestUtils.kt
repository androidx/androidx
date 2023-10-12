/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventData
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.test.assertContentEquals

fun Events.assertReceivedNoEvents() =
    require(list.isEmpty()) { "Received events:\n${list.joinToString("\n")}" }

fun Events.assertReceived(type: PointerEventType, offset: Offset) =
    received().assertHas(type, offset)

@OptIn(ExperimentalComposeUiApi::class)
fun Events.assertReceived(type: PointerEventType, vararg pointers: ComposeScene.Pointer) =
    received().assertHas(type, *pointers)

fun Events.assertReceivedLast(type: PointerEventType, offset: Offset) =
    receivedLast().assertHas(type, offset)

@OptIn(ExperimentalComposeUiApi::class)
fun Events.assertReceivedLast(type: PointerEventType, vararg pointers: ComposeScene.Pointer) =
    receivedLast().assertHas(type, *pointers)

fun PointerEvent.assertHas(type: PointerEventType, offset: Offset) {
    assertThat(this.type).isEqualTo(type)
    assertThat(changes.first().position).isEqualTo(offset)
}

@OptIn(ExperimentalComposeUiApi::class)
fun PointerEvent.assertHas(type: PointerEventType, vararg pointers: ComposeScene.Pointer) {
    assertThat(this.type).isEqualTo(type)
    val actualPointers = changes.map {
        ComposeScene.Pointer(
            it.id,
            it.position,
            it.pressed,
            it.type,
            it.pressure
        )
    }
    assertThat(actualPointers).containsExactly(*pointers)
}

@OptIn(ExperimentalComposeUiApi::class)
fun touch(x: Float, y: Float, pressed: Boolean, id: Int = 0) = ComposeScene.Pointer(
    id = PointerId(id.toLong()),
    position = Offset(x, y),
    pressed = pressed,
    type = PointerType.Touch
)

class Events {
    val list = mutableListOf<PointerEvent>()

    fun add(event: PointerEvent) {
        list.add(event)
    }

    fun receivedLast(): PointerEvent {
        require(list.isNotEmpty()) { "The were no events" }
        val event = list.removeFirst()
        require(list.isEmpty()) { "The event $event isn't the last.\nAlso received:\n${list.joinToString("\n")}" }
        return event
    }

    fun received(): PointerEvent {
        require(list.isNotEmpty()) { "The were no events" }
        val event = list.removeFirst()
        require(list.isNotEmpty()) { "The event $event is the last" }
        return event
    }
}

class FillBox(
    private val onClick: () -> Unit = {}
) {
    val events = Events()
    val tag = "background"

    @Composable
    fun Content() {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .collectEvents(events)
                .testTag(tag)
        )
    }
}

class PopupState(
    val bounds: IntRect,
    private val focusable: Boolean = false,
    private val dismissOnClickOutside: Boolean = focusable,
    private val onDismissRequest: () -> Unit = {}
) {
    val origin get() = bounds.topLeft.toOffset()
    val events = Events()
    val tag = "popup"

    @Composable
    fun Content() {
        Popup(
            popupPositionProvider = object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ) = bounds.topLeft
            },
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(
                focusable = focusable,
                dismissOnClickOutside = dismissOnClickOutside,
                clippingEnabled = false
            )
        ) {
            with(LocalDensity.current) {
                Box(
                    Modifier
                        .requiredSize(bounds.width.toDp(), bounds.height.toDp())
                        .collectEvents(events)
                        .testTag(tag)
                )
            }
        }
    }
}

class DialogState(
    val size: IntSize,
    private val dismissOnClickOutside: Boolean = true,
    private val onDismissRequest: () -> Unit = {}
) {
    val events = Events()
    val tag = "dialog"

    @Composable
    fun Content() {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnClickOutside = dismissOnClickOutside
            )
        ) {
            with(LocalDensity.current) {
                Box(
                    Modifier
                        .requiredSize(size.width.toDp(), size.height.toDp())
                        .collectEvents(events)
                        .testTag(tag)
                )
            }
        }
    }
}

fun Modifier.collectEvents(events: Events) = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            events.add(awaitPointerEvent())
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun ImageComposeScene.sendPointerEvent(
    type: PointerEventType,
    vararg pointers: ComposeScene.Pointer
) = sendPointerEvent(type, pointers = pointers.toList())

@OptIn(ExperimentalComposeUiApi::class)
internal fun event(
    type: PointerEventType,
    vararg pointers: Pair<Int, ComposeScene.Pointer>
) = PointerInputEvent(
    type,
    0,
    pointers.map {
        val id = it.first
        val pointer = it.second
        PointerInputEventData(
            PointerId(id.toLong()),
            uptime = 0,
            pointer.position,
            pointer.position,
            pointer.pressed,
            pointer.pressure,
            pointer.type,
            scrollDelta = Offset.Zero,
            historical = emptyList()
        )
    },
)

@OptIn(ExperimentalComposeUiApi::class)
internal fun mouseEvent(
    type: PointerEventType,
    x: Float,
    y: Float,
    pressed: Boolean
) = PointerInputEvent(
    type,
    0,
    listOf(
        PointerInputEventData(
            id = PointerId(0),
            uptime = 0,
            Offset(x, y),
            Offset(x, y),
            down = pressed,
            pressure = 1f,
            type = PointerType.Mouse,
            scrollDelta = Offset.Zero,
            issuesEnterExit = true,
            historical = emptyList()
        )
    ),
    buttons = PointerButtons(isPrimaryPressed = pressed)
)

internal infix fun List<PointerInputEvent>.positionAndDownShouldEqual(
    expected: List<PointerInputEvent>
) {
    assertContentEquals(
        expected.map { it.formatPositionAndDown() },
        map { it.formatPositionAndDown() }
    )
}

internal fun PointerInputEvent.formatPositionAndDown(): String {
    val pointers = if (pointers.size == 1) {
        pointers.first().formatPositionAndDown()
    } else {
        pointers.joinToString(" ") {
            val id = it.id.value
            val data = it.formatPositionAndDown()
            "$id-$data"
        }
    }
    return "$eventType $pointers"
}

internal fun PointerInputEventData.formatPositionAndDown(): String {
    val x = position.x.toInt()
    val y = position.y.toInt()
    val down = if (down) "down" else "up"
    return "$x:$y:$down"
}