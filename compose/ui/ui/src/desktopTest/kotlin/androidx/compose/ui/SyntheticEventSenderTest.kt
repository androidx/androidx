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

package androidx.compose.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Exit
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Scroll
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.SyntheticEventSender
import androidx.compose.ui.scene.PointerEventResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class SyntheticEventSenderTest {
    @Test
    fun `mouse, shouldn't generate new events if order is correct`() {
        eventsSentBy(
            mouseEvent(Enter, 10f, 20f, pressed = false),
            mouseEvent(Press, 10f, 20f, pressed = true),
            mouseEvent(Move, 10f, 30f, pressed = true),
            mouseEvent(Release, 10f, 30f, pressed = false),
            mouseEvent(Move, 10f, 40f, pressed = false),
            mouseEvent(Press, 10f, 40f, pressed = true),
            mouseEvent(Release, 10f, 40f, pressed = false),
            mouseEvent(Exit, -1f, -1f, pressed = false),
        ) positionAndDownShouldEqual listOf(
            mouseEvent(Enter, 10f, 20f, pressed = false),
            mouseEvent(Press, 10f, 20f, pressed = true),
            mouseEvent(Move, 10f, 30f, pressed = true),
            mouseEvent(Release, 10f, 30f, pressed = false),
            mouseEvent(Move, 10f, 40f, pressed = false),
            mouseEvent(Press, 10f, 40f, pressed = true),
            mouseEvent(Release, 10f, 40f, pressed = false),
            mouseEvent(Exit, -1f, -1f, pressed = false),
        )
    }

    @Test
    fun `mouse, should generate new move before non-move if position isn't the same`() {
        eventsSentBy(
            mouseEvent(Enter, 10f, 20f, pressed = false),
            mouseEvent(Press, 10f, 25f, pressed = true),
            mouseEvent(Move, 10f, 30f, pressed = true),
            mouseEvent(Release, 10f, 35f, pressed = false),
            mouseEvent(Move, 10f, 40f, pressed = false),
            mouseEvent(Press, 10f, 45f, pressed = true),
            mouseEvent(Release, 10f, 50f, pressed = false),
            mouseEvent(Exit, -1f, -1f, pressed = false),
        ) positionAndDownShouldEqual listOf(
            mouseEvent(Enter, 10f, 20f, pressed = false),
            mouseEvent(Move, 10f, 25f, pressed = false),
            mouseEvent(Press, 10f, 25f, pressed = true),
            mouseEvent(Move, 10f, 30f, pressed = true),
            mouseEvent(Move, 10f, 35f, pressed = true),
            mouseEvent(Release, 10f, 35f, pressed = false),
            mouseEvent(Move, 10f, 40f, pressed = false),
            mouseEvent(Move, 10f, 45f, pressed = false),
            mouseEvent(Press, 10f, 45f, pressed = true),
            mouseEvent(Move, 10f, 50f, pressed = true),
            mouseEvent(Release, 10f, 50f, pressed = false),
            mouseEvent(Exit, -1f, -1f, pressed = false),
        )
    }

    @Test
    fun `touch, shouldn't generate new move before non-move if position isn't the same`() {
        eventsSentBy(
            event(Press, 1 to touch(10f, 25f, pressed = true)),
            event(Move, 1 to touch(10f, 30f, pressed = true)),
            event(Release, 1 to touch(10f, 35f, pressed = false)),
            event(Press, 2 to touch(10f, 45f, pressed = true)),
            event(Release, 2 to touch(10f, 50f, pressed = false)),
        ) positionAndDownShouldEqual listOf(
            event(Press, 1 to touch(10f, 25f, pressed = true)),
            event(Move, 1 to touch(10f, 30f, pressed = true)),
            event(Release, 1 to touch(10f, 35f, pressed = false)),
            event(Press, 2 to touch(10f, 45f, pressed = true)),
            event(Release, 2 to touch(10f, 50f, pressed = false)),
        )
    }

    @Test
    fun `touch, shouldn't generate new events if order is correct, without moves`() {
        eventsSentBy(
            event(Press, 1 to touch(1f, 2f, pressed = true)),
            event(Press, 1 to touch(1f, 2f, pressed = true), 2 to touch(10f, 20f, pressed = true)),
            event(Release, 1 to touch(1f, 2f, pressed = false), 2 to touch(10f, 20f, pressed = true)),
            event(Release, 2 to touch(10f, 20f, pressed = false)),
        ) positionAndDownShouldEqual listOf(
            event(Press, 1 to touch(1f, 2f, pressed = true)),
            event(Press, 1 to touch(1f, 2f, pressed = true), 2 to touch(10f, 20f, pressed = true)),
            event(Release, 1 to touch(1f, 2f, pressed = false), 2 to touch(10f, 20f, pressed = true)),
            event(Release, 2 to touch(10f, 20f, pressed = false)),
        )
    }

    @Test
    fun `touch, shouldn't generate new events if order is correct, with moves`() {
        eventsSentBy(
            event(Press, 1 to touch(1f, 2f, pressed = true)),
            event(Move, 1 to touch(1f, 2f, pressed = true)),
            event(Press, 1 to touch(1f, 2f, pressed = true), 2 to touch(10f, 20f, pressed = true)),
            event(Move, 1 to touch(1f, 2f, pressed = true), 2 to touch(10f, 25f, pressed = true)),
            event(Move, 1 to touch(1f, 3f, pressed = true), 2 to touch(10f, 25f, pressed = true)),
            event(Release, 1 to touch(1f, 3f, pressed = false), 2 to touch(10f, 25f, pressed = true)),
            event(Move, 2 to touch(10f, 30f, pressed = true)),
            event(Release, 2 to touch(10f, 30f, pressed = false)),
        ) positionAndDownShouldEqual listOf(
            event(Press, 1 to touch(1f, 2f, pressed = true)),
            event(Move, 1 to touch(1f, 2f, pressed = true)),
            event(Press, 1 to touch(1f, 2f, pressed = true), 2 to touch(10f, 20f, pressed = true)),
            event(Move, 1 to touch(1f, 2f, pressed = true), 2 to touch(10f, 25f, pressed = true)),
            event(Move, 1 to touch(1f, 3f, pressed = true), 2 to touch(10f, 25f, pressed = true)),
            event(Release, 1 to touch(1f, 3f, pressed = false), 2 to touch(10f, 25f, pressed = true)),
            event(Move, 2 to touch(10f, 30f, pressed = true)),
            event(Release, 2 to touch(10f, 30f, pressed = false))
        )
    }

    @Test
    fun `touch, should generate one press or release at a time`() {
        eventsSentBy(
            event(
                Press,
                1 to touch(1f, 3f, pressed = true),
                2 to touch(10f, 20f, pressed = true),
                3 to touch(100f, 200f, pressed = true),
            ),
            event(
                Release,
                2 to touch(10f, 20f, pressed = false),
                3 to touch(100f, 200f, pressed = true),
            ),
        ) positionAndDownShouldEqual listOf(
            event(
                Press,
                1 to touch(1f, 3f, pressed = true),
                2 to touch(10f, 20f, pressed = false),
                3 to touch(100f, 200f, pressed = false),
            ),
            event(
                Press,
                1 to touch(1f, 3f, pressed = true),
                2 to touch(10f, 20f, pressed = true),
                3 to touch(100f, 200f, pressed = false),
            ),
            event(
                Press,
                1 to touch(1f, 3f, pressed = true),
                2 to touch(10f, 20f, pressed = true),
                3 to touch(100f, 200f, pressed = true),
            ),
            event(
                Release,
                1 to touch(1f, 3f, pressed = false),
                2 to touch(10f, 20f, pressed = true),
                3 to touch(100f, 200f, pressed = true),
            ),
            event(
                Release,
                2 to touch(10f, 20f, pressed = false),
                3 to touch(100f, 200f, pressed = true),
            ),
        )
    }

    @Test
    fun `touch, should generate one press at a time on simultaneous touches press`() {
        eventsSentBy(
            event(
                Press,
                1 to touch(1f, 3f, pressed = true)
            ),
            event(
                Press,
                1 to touch(1f, 3f, pressed = true),
                2 to touch(10f, 20f, pressed = true),
                3 to touch(100f, 200f, pressed = true)
            ),
        ) positionAndDownShouldEqual listOf(
            event(
                Press,
                1 to touch(1f, 3f, pressed = true)
            ),
            event(
                Press,
                1 to touch(1f, 3f, pressed = true),
                2 to touch(10f, 20f, pressed = true),
                3 to touch(100f, 200f, pressed = false),
            ),
            event(
                Press,
                1 to touch(1f, 3f, pressed = true),
                2 to touch(10f, 20f, pressed = true),
                3 to touch(100f, 200f, pressed = true)
            )
        )
    }

    @Test
    fun `touch, should generate one release at a time on simultaneous touches release`() {
        eventsSentBy(
            event(
                Press,
                1 to touch(1f, 3f, pressed = true),
                2 to touch(10f, 20f, pressed = true),
                3 to touch(100f, 200f, pressed = true),
            ),
            event(
                Release,
                1 to touch(1f, 3f, pressed = false),
                2 to touch(10f, 20f, pressed = true),
                3 to touch(100f, 200f, pressed = true),
            ),
            event(
                Release,
                2 to touch(10f, 20f, pressed = false),
                3 to touch(100f, 200f, pressed = false),
            ),
        ) positionAndDownShouldEqual listOf(
            event(
                Press,
                1 to touch(1f, 3f, pressed = true),
                2 to touch(10f, 20f, pressed = false),
                3 to touch(100f, 200f, pressed = false),
            ),
            event(
                Press,
                1 to touch(1f, 3f, pressed = true),
                2 to touch(10f, 20f, pressed = true),
                3 to touch(100f, 200f, pressed = false),
            ),
            event(
                Press,
                1 to touch(1f, 3f, pressed = true),
                2 to touch(10f, 20f, pressed = true),
                3 to touch(100f, 200f, pressed = true),
            ),
            event(
                Release,
                1 to touch(1f, 3f, pressed = false),
                2 to touch(10f, 20f, pressed = true),
                3 to touch(100f, 200f, pressed = true),
            ),
            event(
                Release,
                1 to touch(1f, 3f, pressed = false),
                2 to touch(10f, 20f, pressed = false),
                3 to touch(100f, 200f, pressed = true),
            ),
            event(
                Release,
                2 to touch(10f, 20f, pressed = false),
                3 to touch(100f, 200f, pressed = false),
            )
        )
    }

    @Test
    fun `should consume move event when synthetic events added`() {
        val sender = SyntheticEventSender { event ->
            // Consume only synthetic move event
            PointerEventResult(anyMovementConsumed = event.eventType == Move)
        }

        assertFalse(
            sender.send(mouseEvent(Enter, 10f, 20f, pressed = false))
                .anyMovementConsumed
        )
        assertTrue(
            sender.send(mouseEvent(Press, 10f, 25f, pressed = true))
                .anyMovementConsumed
        )
    }

    @Test
    fun `should not consume event none of synthetic events consumed`() {
        val sender = SyntheticEventSender {
            PointerEventResult(anyMovementConsumed = false)
        }

        assertFalse(
            sender.send(mouseEvent(Enter, 10f, 20f, pressed = false))
                .anyMovementConsumed
        )
        assertFalse(
            sender.send(mouseEvent(Press, 10f, 25f, pressed = true))
                .anyMovementConsumed
        )
    }

    @Test
    fun `should consume event when any synthetic event consumed`() {
        val sender = SyntheticEventSender { event ->
            PointerEventResult(anyMovementConsumed = event.eventType == Move)
        }

        assertFalse(
            sender.send(mouseEvent(Enter, 10f, 20f, pressed = false))
                .anyMovementConsumed
        )
        assertTrue(
            sender.send(mouseEvent(Press, 10f, 25f, pressed = true))
                .anyMovementConsumed
        )
    }

    @Test
    fun `should consume press event when any synthetic event consumed`() {
        val sender = SyntheticEventSender { event ->
            // Consume only first synthetic Press event
            PointerEventResult(
                anyMovementConsumed = event.eventType == Press &&
                    event.pointers.singleOrNull { it.down } != null
            )
        }

        assertTrue(
            sender.send(
                event(
                    Press,
                    1 to touch(1f, 3f, pressed = true),
                    2 to touch(10f, 20f, pressed = true),
                    3 to touch(100f, 200f, pressed = true),
                )
            ).anyMovementConsumed
        )
    }

    @Test
    fun `should consume release event when synthetic events consumed`() {
        val sender = SyntheticEventSender { event ->
            // Consume only first synthetic Release event
            PointerEventResult(
                anyMovementConsumed = event.eventType == Release &&
                    event.pointers.singleOrNull { !it.down } != null
            )
        }

        sender.send(
            event(
                Press,
                1 to touch(1f, 3f, pressed = true),
                2 to touch(10f, 20f, pressed = true),
                3 to touch(100f, 200f, pressed = true),
            )
        )

        assertTrue(
            sender.send(
                event(
                    Release,
                    1 to touch(1f, 3f, pressed = false),
                    2 to touch(10f, 20f, pressed = false),
                    3 to touch(100f, 200f, pressed = false),
                ),
            ).anyMovementConsumed
        )
    }

    @Test
    fun `should update pointer position with move event after hover event`() {
        val received = mutableListOf<PointerInputEvent>()
        val sender = SyntheticEventSender {
            PointerEventResult(received.add(it))
        }
        sender.send(mouseEvent(Enter, 10f, 20f, pressed = false))

        sender.needUpdatePointerPosition = true
        sender.updatePointerPosition()

        received positionAndDownShouldEqual listOf(
            event(Enter, 1 to touch(10f, 20f, pressed = false)),
            event(Move, 1 to touch(10f, 20f, pressed = false))
        )

        received.clear()
        sender.send(mouseEvent(Move, 5f, 15f, pressed = false))

        sender.needUpdatePointerPosition = true
        sender.updatePointerPosition()

        received positionAndDownShouldEqual listOf(
            event(Move, 1 to touch(5f, 15f, pressed = false)),
            event(Move, 1 to touch(5f, 15f, pressed = false))
        )
    }

    @Test
    fun `should update pointer position with move event after pressed event`() {
        val received = mutableListOf<PointerInputEvent>()
        val sender = SyntheticEventSender {
            PointerEventResult(received.add(it))
        }
        sender.send(mouseEvent(Press, 10f, 20f, pressed = true))

        sender.needUpdatePointerPosition = true
        sender.updatePointerPosition()

        received positionAndDownShouldEqual listOf(
            event(Press, 1 to touch(10f, 20f, pressed = true)),
            event(Move, 1 to touch(10f, 20f, pressed = true))
        )

        received.clear()
        sender.send(mouseEvent(Move, 5f, 15f, pressed = true))

        sender.needUpdatePointerPosition = true
        sender.updatePointerPosition()

        received positionAndDownShouldEqual listOf(
            event(Move, 1 to touch(5f, 15f, pressed = true)),
            event(Move, 1 to touch(5f, 15f, pressed = true))
        )
    }

    @Test
    fun `should not update pointer position with move event after touch event`() {
        val received = mutableListOf<PointerInputEvent>()
        val sender = SyntheticEventSender {
            PointerEventResult(received.add(it))
        }
        sender.send(event(Press, 1 to touch(10f, 20f, pressed = true)))

        sender.needUpdatePointerPosition = true
        sender.updatePointerPosition()

        received positionAndDownShouldEqual listOf(
            event(Press, 1 to touch(10f, 20f, pressed = true)),
        )

        received.clear()
        sender.send(event(Move, 1 to touch(5f, 15f, pressed = true)))

        sender.needUpdatePointerPosition = true
        sender.updatePointerPosition()

        received positionAndDownShouldEqual listOf(
            event(Move, 1 to touch(5f, 15f, pressed = true)),
        )
    }

    @Test
    fun `should not re-enter after mouse exit`() {
        val received = mutableListOf<PointerInputEvent>()
        val sender = SyntheticEventSender {
            PointerEventResult(received.add(it))
        }
        sender.send(mouseEvent(Move, 10f, 20f, pressed = false))
        sender.send(mouseEvent(Exit, 10f, 20f, pressed = false))

        sender.needUpdatePointerPosition = true
        sender.updatePointerPosition()

        received positionAndDownShouldEqual listOf(
            mouseEvent(Move, 10f, 20f, pressed = false),
            mouseEvent(Exit, 10f, 20f, pressed = false)
        )
    }

    @Test
    fun `synthetic events should not duplicate scrollDelta`() {
        val received = mutableListOf<PointerInputEvent>()
        val sender = SyntheticEventSender {
            PointerEventResult(received.add(it))
        }

        sender.send(
            mouseEvent(
                Scroll,
                10f,
                20f,
                pressed = false,
                scrollDelta = Offset(0f, 5f)
            )
        )
        sender.send(mouseEvent(Press, 10f, 30f, pressed = true))

        assertEquals(3, received.size)
        val totalScroll = received.fold(Offset.Zero) { acc, event ->
            acc + event.pointers.fold(Offset.Zero) { a, p -> a + p.scrollDelta }
        }
        assertEquals(Offset(0f, 5f), totalScroll)
    }

    @Test
    fun `synthetic events should not duplicate panGestureOffset`() {
        val received = mutableListOf<PointerInputEvent>()
        val sender = SyntheticEventSender {
            PointerEventResult(received.add(it))
        }

        sender.send(
            mouseEvent(
                Scroll,
                10f,
                20f,
                pressed = false,
                panGestureOffset = Offset(0f, 5f)
            )
        )
        sender.send(mouseEvent(Press, 10f, 30f, pressed = true))

        assertEquals(3, received.size)
        val totalScroll = received.fold(Offset.Zero) { acc, event ->
            acc + event.pointers.fold(Offset.Zero) { a, p -> a + p.panGestureOffset }
        }
        assertEquals(Offset(0f, 5f), totalScroll)
    }

    @Test
    fun `synthetic events should not duplicate scaleGestureFactor`() {
        val received = mutableListOf<PointerInputEvent>()
        val sender = SyntheticEventSender {
            PointerEventResult(received.add(it))
        }

        sender.send(
            mouseEvent(
                Scroll,
                10f,
                20f,
                pressed = false,
                scaleGestureFactor = 2.0f
            )
        )
        sender.send(mouseEvent(Press, 10f, 30f, pressed = true))

        assertEquals(3, received.size)
        val totalScaleFactor = received.fold(1f) { acc, event ->
            acc * event.pointers.fold(1f) { a, p -> a * p.scaleGestureFactor }
        }
        assertEquals(2f, totalScaleFactor)
    }

    private fun eventsSentBy(
        vararg inputEvents: PointerInputEvent
    ): List<PointerInputEvent> {
        val received = mutableListOf<PointerInputEvent>()
        val sender = SyntheticEventSender {
            PointerEventResult(received.add(it))
        }
        for (inputEvent in inputEvents) {
            sender.send(inputEvent)
        }
        return received
    }
}
