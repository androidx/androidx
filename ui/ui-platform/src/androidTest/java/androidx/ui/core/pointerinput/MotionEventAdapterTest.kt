/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.pointerinput

import android.view.MotionEvent
import androidx.test.filters.SmallTest
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class MotionEventAdapterTest {

    @Test
    fun toPointerInputEvent_1pointerActionDown_convertsCorrectly_old() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(8290)),
            arrayOf(PointerCoords(2967f, 5928f))
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(1))
        assertPointerInputEventData(pointers[0], 8290, true, 2967f, 5928f)
    }

    @Test
    fun toPointerInputEvent_1pointerActionDown_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(8290)),
            arrayOf(PointerCoords(2967f, 5928f))
        )

        val actual = motionEvent.toPointerInputEvent()
        val timestamp = actual.timestamp
        val pointers = actual.pointers

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(1))
        assertPointerInputEventData(pointers[0], 8290, true, 2967f, 5928f)
    }

    @Test
    fun toPointerInputEvent_1pointerActionMove_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(8290)),
            arrayOf(PointerCoords(2967f, 5928f))
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(1))
        assertPointerInputEventData(pointers[0], 8290, true, 2967f, 5928f)
    }

    @Test
    fun toPointerInputEvent_1pointerActionUp_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(8290)),
            arrayOf(PointerCoords(2967f, 5928f))
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(1))
        assertPointerInputEventData(pointers[0], 8290, false, 2967f, 5928f)
    }

    @Test
    fun toPointerInputEvent_2pointers1stPointerActionPointerDown_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            0,
            arrayOf(
                PointerProperties(8290),
                PointerProperties(1516)
            ),
            arrayOf(
                PointerCoords(2967f, 5928f),
                PointerCoords(1942f, 6729f)
            )
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(2))
        assertPointerInputEventData(pointers[0], 8290, true, 2967f, 5928f)
        assertPointerInputEventData(pointers[1], 1516, true, 1942f, 6729f)
    }

    @Test
    fun toPointerInputEvent_2pointers2ndPointerActionPointerDown_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            1,
            arrayOf(
                PointerProperties(8290),
                PointerProperties(1516)
            ),
            arrayOf(
                PointerCoords(2967f, 5928f),
                PointerCoords(1942f, 6729f)
            )
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(2))
        assertPointerInputEventData(pointers[0], 8290, true, 2967f, 5928f)
        assertPointerInputEventData(pointers[1], 1516, true, 1942f, 6729f)
    }

    @Test
    fun toPointerInputEvent_3pointers1stPointerActionPointerDown_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_POINTER_DOWN,
            3,
            0,
            arrayOf(
                PointerProperties(8290),
                PointerProperties(1516),
                PointerProperties(9285)
            ),
            arrayOf(
                PointerCoords(2967f, 5928f),
                PointerCoords(1942f, 6729f),
                PointerCoords(6206f, 1098f)
            )
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(3))
        assertPointerInputEventData(pointers[0], 8290, true, 2967f, 5928f)
        assertPointerInputEventData(pointers[1], 1516, true, 1942f, 6729f)
        assertPointerInputEventData(pointers[2], 9285, true, 6206f, 1098f)
    }

    @Test
    fun toPointerInputEvent_3pointers2ndPointerActionPointerDown_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_POINTER_DOWN,
            3,
            2,
            arrayOf(
                PointerProperties(8290),
                PointerProperties(1516),
                PointerProperties(9285)
            ),
            arrayOf(
                PointerCoords(2967f, 5928f),
                PointerCoords(1942f, 6729f),
                PointerCoords(6206f, 1098f)
            )
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(3))
        assertPointerInputEventData(pointers[0], 8290, true, 2967f, 5928f)
        assertPointerInputEventData(pointers[1], 1516, true, 1942f, 6729f)
        assertPointerInputEventData(pointers[2], 9285, true, 6206f, 1098f)
    }

    @Test
    fun toPointerInputEvent_3pointers3rdPointerActionPointerDown_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_POINTER_DOWN,
            3,
            2,
            arrayOf(
                PointerProperties(8290),
                PointerProperties(1516),
                PointerProperties(9285)
            ),
            arrayOf(
                PointerCoords(2967f, 5928f),
                PointerCoords(1942f, 6729f),
                PointerCoords(6206f, 1098f)
            )
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(3))
        assertPointerInputEventData(pointers[0], 8290, true, 2967f, 5928f)
        assertPointerInputEventData(pointers[1], 1516, true, 1942f, 6729f)
        assertPointerInputEventData(pointers[2], 9285, true, 6206f, 1098f)
    }

    @Test
    fun toPointerInputEvent_2pointersActionMove_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_MOVE,
            2,
            0,
            arrayOf(
                PointerProperties(8290),
                PointerProperties(1516)
            ),
            arrayOf(
                PointerCoords(2967f, 5928f),
                PointerCoords(1942f, 6729f)
            )
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(2))
        assertPointerInputEventData(pointers[0], 8290, true, 2967f, 5928f)
        assertPointerInputEventData(pointers[1], 1516, true, 1942f, 6729f)
    }

    @Test
    fun toPointerInputEvent_2pointers1stPointerActionPointerUP_convertsCorrectly() {

        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_POINTER_UP,
            2,
            0,
            arrayOf(
                PointerProperties(8290),
                PointerProperties(1516)
            ),
            arrayOf(
                PointerCoords(2967f, 5928f),
                PointerCoords(1942f, 6729f)
            )
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(2))
        assertPointerInputEventData(pointers[0], 8290, false, 2967f, 5928f)
        assertPointerInputEventData(pointers[1], 1516, true, 1942f, 6729f)
    }

    @Test
    fun toPointerInputEvent_2pointers2ndPointerActionPointerUp_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_POINTER_UP,
            2,
            1,
            arrayOf(
                PointerProperties(8290),
                PointerProperties(1516)
            ),
            arrayOf(
                PointerCoords(2967f, 5928f),
                PointerCoords(1942f, 6729f)
            )
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(2))
        assertPointerInputEventData(pointers[0], 8290, true, 2967f, 5928f)
        assertPointerInputEventData(pointers[1], 1516, false, 1942f, 6729f)
    }

    @Test
    fun toPointerInputEvent_3pointers1stPointerActionPointerUp_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_POINTER_UP,
            3,
            0,
            arrayOf(
                PointerProperties(8290),
                PointerProperties(1516),
                PointerProperties(9285)
            ),
            arrayOf(
                PointerCoords(2967f, 5928f),
                PointerCoords(1942f, 6729f),
                PointerCoords(6206f, 1098f)
            )
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(3))
        assertPointerInputEventData(pointers[0], 8290, false, 2967f, 5928f)
        assertPointerInputEventData(pointers[1], 1516, true, 1942f, 6729f)
        assertPointerInputEventData(pointers[2], 9285, true, 6206f, 1098f)
    }

    @Test
    fun toPointerInputEvent_3pointers2ndPointerActionPointerUp_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_POINTER_UP,
            3,
            1,
            arrayOf(
                PointerProperties(8290),
                PointerProperties(1516),
                PointerProperties(9285)
            ),
            arrayOf(
                PointerCoords(2967f, 5928f),
                PointerCoords(1942f, 6729f),
                PointerCoords(6206f, 1098f)
            )
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(3))
        assertPointerInputEventData(pointers[0], 8290, true, 2967f, 5928f)
        assertPointerInputEventData(pointers[1], 1516, false, 1942f, 6729f)
        assertPointerInputEventData(pointers[2], 9285, true, 6206f, 1098f)
    }

    @Test
    fun toPointerInputEvent_3pointers3rdPointerActionPointerUp_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_POINTER_UP,
            3,
            2,
            arrayOf(
                PointerProperties(8290),
                PointerProperties(1516),
                PointerProperties(9285)
            ),
            arrayOf(
                PointerCoords(2967f, 5928f),
                PointerCoords(1942f, 6729f),
                PointerCoords(6206f, 1098f)
            )
        )

        val (timestamp, pointers) = motionEvent.toPointerInputEvent()

        assertThat(timestamp.nanoseconds, `is`(2_894_000_000L))
        assertThat(pointers.size, `is`(3))
        assertPointerInputEventData(pointers[0], 8290, true, 2967f, 5928f)
        assertPointerInputEventData(pointers[1], 1516, true, 1942f, 6729f)
        assertPointerInputEventData(pointers[2], 9285, false, 6206f, 1098f)
    }

    // Private help functions.

    private fun PointerProperties(id: Int) =
        MotionEvent.PointerProperties().apply { this.id = id }

    private fun PointerCoords(x: Float, y: Float) =
        MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
        }

    private fun MotionEvent(
        eventTime: Int,
        action: Int,
        numPointers: Int,
        actionIndex: Int,
        pointerProperties: Array<MotionEvent.PointerProperties>,
        pointerCoords: Array<MotionEvent.PointerCoords>
    ) = MotionEvent.obtain(
        0,
        eventTime.toLong(),
        action + (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
        numPointers,
        pointerProperties,
        pointerCoords,
        0,
        0,
        0f,
        0f,
        0,
        0,
        0,
        0
    )

    private fun assertPointerInputEventData(
        actual: PointerInputEventData,
        id: Int,
        isDown: Boolean,
        x: Float,
        y: Float
    ) {
        val pointerInputData = actual.pointerInputData
        assertThat(actual.id, `is`(id))
        assertThat(pointerInputData.down, `is`(isDown))
        assertThat(pointerInputData.position!!.x.value, `is`(x))
        assertThat(pointerInputData.position!!.y.value, `is`(y))
    }
}
