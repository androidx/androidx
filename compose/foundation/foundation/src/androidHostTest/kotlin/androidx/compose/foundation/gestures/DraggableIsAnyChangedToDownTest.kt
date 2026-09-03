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

package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DraggableIsAnyChangedToDownTest {

    private fun createPointerInputChange(
        id: Long = 0,
        pressed: Boolean = true,
        previousPressed: Boolean = false,
        type: PointerType = PointerType.Touch,
        isInitiallyConsumed: Boolean = false,
    ): PointerInputChange =
        PointerInputChange(
            id = PointerId(id),
            uptimeMillis = 10L,
            position = Offset.Zero,
            pressed = pressed,
            previousUptimeMillis = 0L,
            previousPosition = Offset.Zero,
            previousPressed = previousPressed,
            isInitiallyConsumed = isInitiallyConsumed,
            type = type,
        )

    @Test
    fun singlePointer_changedToDown_returnsTrue() {
        val change = createPointerInputChange(id = 0, pressed = true, previousPressed = false)
        val event = PointerEvent(listOf(change))
        assertThat(event.isAnyChangedToDown(requireUnconsumed = false)).isTrue()
        assertThat(event.isAnyChangedToDown(requireUnconsumed = true)).isTrue()
    }

    @Test
    fun singlePointer_notChangedToDown_returnsFalse() {
        val change = createPointerInputChange(id = 0, pressed = true, previousPressed = true)
        val event = PointerEvent(listOf(change))
        assertThat(event.isAnyChangedToDown(requireUnconsumed = false)).isFalse()
        assertThat(event.isAnyChangedToDown(requireUnconsumed = true)).isFalse()
    }

    @Test
    fun singlePointer_up_returnsFalse() {
        val change = createPointerInputChange(id = 0, pressed = false, previousPressed = true)
        val event = PointerEvent(listOf(change))
        assertThat(event.isAnyChangedToDown(requireUnconsumed = false)).isFalse()
        assertThat(event.isAnyChangedToDown(requireUnconsumed = true)).isFalse()
    }

    @Test
    fun singlePointer_consumed_requireUnconsumed() {
        val change =
            createPointerInputChange(
                id = 0,
                pressed = true,
                previousPressed = false,
                isInitiallyConsumed = true,
            )
        val event = PointerEvent(listOf(change))
        assertThat(event.isAnyChangedToDown(requireUnconsumed = true)).isFalse()
        assertThat(event.isAnyChangedToDown(requireUnconsumed = false)).isTrue()
    }

    @Test
    fun multiPointer_hoverPointerAndNewDownPointer_returnsTrue() {
        // Trackpad hover pointer (not pressed) coexisting with a new touch down pointer
        val hoverPointer =
            createPointerInputChange(
                id = 0,
                pressed = false,
                previousPressed = false,
                type = PointerType.Mouse,
            )
        val touchDownPointer =
            createPointerInputChange(
                id = 1,
                pressed = true,
                previousPressed = false,
                type = PointerType.Touch,
            )
        val event = PointerEvent(listOf(hoverPointer, touchDownPointer))
        assertThat(event.isAnyChangedToDown(requireUnconsumed = false)).isTrue()
    }

    @Test
    fun multiPointer_activePressedPointerAndNewDownPointer_returnsFalse() {
        // Existing active pressed touch pointer coexisting with a new touch down pointer
        // (ACTION_POINTER_DOWN)
        val activePointer =
            createPointerInputChange(
                id = 0,
                pressed = true,
                previousPressed = true,
                type = PointerType.Touch,
            )
        val newDownPointer =
            createPointerInputChange(
                id = 1,
                pressed = true,
                previousPressed = false,
                type = PointerType.Touch,
            )
        val event = PointerEvent(listOf(activePointer, newDownPointer))
        // Should return false because there is already an active pressed pointer
        assertThat(event.isAnyChangedToDown(requireUnconsumed = false)).isFalse()
    }

    @Test
    fun mousePointer_onlyPrimaryMouseButton_whenPrimaryPressed_returnsTrue() {
        val mouseChange =
            createPointerInputChange(
                id = 0,
                pressed = true,
                previousPressed = false,
                type = PointerType.Mouse,
            )
        val motionEvent = org.mockito.Mockito.mock(android.view.MotionEvent::class.java)
        org.mockito.Mockito.`when`(motionEvent.buttonState)
            .thenReturn(android.view.MotionEvent.BUTTON_PRIMARY)
        val event = PointerEvent(listOf(mouseChange)).copy(listOf(mouseChange), motionEvent)
        assertThat(
                event.isAnyChangedToDown(requireUnconsumed = false, onlyPrimaryMouseButton = true)
            )
            .isTrue()
    }

    @Test
    fun mousePointer_onlyPrimaryMouseButton_whenSecondaryPressed_returnsFalse() {
        val mouseChange =
            createPointerInputChange(
                id = 0,
                pressed = true,
                previousPressed = false,
                type = PointerType.Mouse,
            )
        val motionEvent = org.mockito.Mockito.mock(android.view.MotionEvent::class.java)
        org.mockito.Mockito.`when`(motionEvent.buttonState)
            .thenReturn(android.view.MotionEvent.BUTTON_SECONDARY)
        val event = PointerEvent(listOf(mouseChange)).copy(listOf(mouseChange), motionEvent)
        // With onlyPrimaryMouseButton = true (Desktop behavior), secondary button returns false
        assertThat(
                event.isAnyChangedToDown(requireUnconsumed = false, onlyPrimaryMouseButton = true)
            )
            .isFalse()
        // With onlyPrimaryMouseButton = false (Android behavior), secondary button returns true
        assertThat(
                event.isAnyChangedToDown(requireUnconsumed = false, onlyPrimaryMouseButton = false)
            )
            .isTrue()
    }

    @Test
    fun mousePointer_onlyPrimaryMouseButton_whenPrimaryNotPressed_returnsFalse() {
        val mouseChange =
            createPointerInputChange(
                id = 0,
                pressed = true,
                previousPressed = false,
                type = PointerType.Mouse,
            )
        // Synthesized PointerEvent without MotionEvent has PointerButtons(0) (isPrimaryPressed =
        // false)
        val event = PointerEvent(listOf(mouseChange))
        assertThat(
                event.isAnyChangedToDown(requireUnconsumed = false, onlyPrimaryMouseButton = true)
            )
            .isFalse()
        assertThat(
                event.isAnyChangedToDown(requireUnconsumed = false, onlyPrimaryMouseButton = false)
            )
            .isTrue()
    }

    @Test
    fun emptyChanges_returnsFalse() {
        val event = PointerEvent(emptyList())
        assertThat(event.isAnyChangedToDown(requireUnconsumed = false)).isFalse()
    }
}
