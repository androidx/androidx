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

package androidx.compose.ui.input.pointer

import android.os.Build
import android.util.SparseLongArray
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_SCROLL
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.AXIS_HSCROLL
import android.view.MotionEvent.AXIS_VSCROLL
import android.view.MotionEvent.TOOL_TYPE_FINGER
import android.view.MotionEvent.TOOL_TYPE_MOUSE
import androidx.annotation.RequiresApi
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MotionEventAdapterTest {

    private lateinit var motionEventAdapter: MotionEventAdapter
    private val positionCalculator =
        object : PositionCalculator {
            override fun screenToLocal(positionOnScreen: Offset): Offset = positionOnScreen

            override fun localToScreen(localPosition: Offset): Offset = localPosition
        }

    @Before
    fun setup() {
        motionEventAdapter = MotionEventAdapter()
    }

    @Test
    fun convertToolType() {
        val types =
            mapOf(
                MotionEvent.TOOL_TYPE_FINGER to PointerType.Touch,
                MotionEvent.TOOL_TYPE_UNKNOWN to PointerType.Unknown,
                MotionEvent.TOOL_TYPE_ERASER to PointerType.Eraser,
                MotionEvent.TOOL_TYPE_STYLUS to PointerType.Stylus,
                MotionEvent.TOOL_TYPE_MOUSE to PointerType.Mouse,
            )
        types.entries.forEach { (toolType, pointerType) ->
            motionEventAdapter = MotionEventAdapter()
            val motionEvent =
                MotionEvent(
                    2894,
                    ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(1000, toolType)),
                    arrayOf(PointerCoords(2967f, 5928f)),
                )
            val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)!!
            assertPointerInputEventData(
                pointerInputEvent.pointers[0],
                PointerId(0),
                true,
                2967f,
                5928f,
                pointerType,
            )
        }
    }

    @Test
    fun hoverEventsStay() {
        // When a hover event happens, the pointer ID should stick around until it is removed.
        val hoverEnter =
            MotionEvent(
                0,
                ACTION_HOVER_ENTER,
                1,
                0,
                arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                arrayOf(PointerCoords(10f, 10f)),
            )
        val hoverEnterEvent = motionEventAdapter.convertToPointerInputEvent(hoverEnter)!!
        assertThat(hoverEnterEvent.pointers).hasSize(1)
        val hoverEnterId = hoverEnterEvent.pointers[0].id

        val hoverExit =
            MotionEvent(
                1,
                ACTION_HOVER_EXIT,
                1,
                0,
                arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                arrayOf(PointerCoords(10f, 10f)),
            )

        val hoverExitEvent = motionEventAdapter.convertToPointerInputEvent(hoverExit)!!
        assertThat(hoverExitEvent.pointers).hasSize(1)
        assertThat(hoverExitEvent.pointers[0].id).isEqualTo(hoverEnterId)

        val down =
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                arrayOf(PointerCoords(10f, 10f)),
            )

        val downEvent = motionEventAdapter.convertToPointerInputEvent(down)!!
        assertThat(downEvent.pointers).hasSize(1)
        assertThat(downEvent.pointers[0].id).isEqualTo(hoverEnterId)

        val up =
            MotionEvent(
                2,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                arrayOf(PointerCoords(10f, 10f)),
            )

        val upEvent = motionEventAdapter.convertToPointerInputEvent(up)!!
        assertThat(upEvent.pointers).hasSize(1)
        assertThat(upEvent.pointers[0].id).isEqualTo(hoverEnterId)

        val hoverEnterEvent2 = motionEventAdapter.convertToPointerInputEvent(hoverEnter)!!
        assertThat(hoverEnterEvent2.pointers).hasSize(1)
        assertThat(hoverEnterEvent2.pointers[0].id).isEqualTo(hoverEnterId)
        motionEventAdapter.convertToPointerInputEvent(hoverExit)!!

        val touchDown =
            MotionEvent(
                3,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(1, TOOL_TYPE_FINGER)),
                arrayOf(PointerCoords(10f, 10f)),
            )
        val touchDownEvent = motionEventAdapter.convertToPointerInputEvent(touchDown)!!
        assertThat(touchDownEvent.pointers).hasSize(1)
        assertThat(touchDownEvent.pointers[0].id).isNotEqualTo(hoverEnterId)
        val touchDownId = touchDownEvent.pointers[0].id

        val touchUp =
            MotionEvent(
                4,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(1, TOOL_TYPE_FINGER)),
                arrayOf(PointerCoords(10f, 10f)),
            )
        val touchUpEvent = motionEventAdapter.convertToPointerInputEvent(touchUp)!!
        assertThat(touchUpEvent.pointers).hasSize(1)
        assertThat(touchUpEvent.pointers[0].id).isEqualTo(touchDownEvent.pointers[0].id)

        val hoverEnterEvent3 = motionEventAdapter.convertToPointerInputEvent(hoverEnter)!!
        assertThat(hoverEnterEvent3.pointers).hasSize(1)
        assertThat(hoverEnterEvent3.pointers[0].id).isNotEqualTo(touchDownId)
        assertThat(hoverEnterEvent3.pointers[0].id).isNotEqualTo(hoverEnterId)
    }

    @Test
    fun robustIdConversion() {
        // When an ID shows up unexpectedly, it shouldn't crash
        val hoverExit =
            MotionEvent(
                3,
                ACTION_HOVER_EXIT,
                1,
                0,
                arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                arrayOf(PointerCoords(10f, 10f)),
            )
        val event = motionEventAdapter.convertToPointerInputEvent(hoverExit)!!
        assertThat(event.pointers).hasSize(1)
    }

    @Test
    fun convertToPointerInputEvent_1pointerActionDown_convertsCorrectly() {
        val motionEvent =
            MotionEvent(
                2894,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(8290)),
                arrayOf(PointerCoords(2967f, 5928f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        val platformEvent = pointerInputEvent.motionEvent
        assertThat(uptime).isEqualTo(2_894L)
        assertThat(pointers).hasSize(1)
        assertPointerInputEventData(pointers[0], PointerId(0), true, 2967f, 5928f)
        assertThat(platformEvent).isSameInstanceAs(motionEvent)
    }

    @Test
    fun convertToPointerInputEvent_1pointerActionMove_convertsCorrectly() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        val motionEvent =
            MotionEvent(
                5,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(6f, 7f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(5L)
        assertThat(pointers).hasSize(1)
        assertPointerInputEventData(pointers[0], PointerId(0), true, 6f, 7f)
    }

    @Test
    fun convertToPointerInputEvent_1pointerActionUp_convertsCorrectly() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                10,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(46)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        val motionEvent =
            MotionEvent(
                34,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(46)),
                arrayOf(PointerCoords(3f, 4f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(34L)
        assertThat(uptime).isEqualTo(34L)
        assertThat(pointers).hasSize(1)
        assertPointerInputEventData(pointers[0], PointerId(0), false, 3f, 4f)
    }

    @Test
    fun convertToPointerInputEvent_2pointers1stPointerActionPointerDown_convertsCorrectly() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        val motionEvent =
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                0,
                arrayOf(PointerProperties(5), PointerProperties(2)),
                arrayOf(PointerCoords(7f, 8f), PointerCoords(3f, 4f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(4L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(pointers[0], PointerId(1), true, 7f, 8f)
        assertPointerInputEventData(pointers[1], PointerId(0), true, 3f, 4f)
    }

    @Test
    fun convertToPointerInputEvent_2pointers2ndPointerActionPointerDown_convertsCorrectly() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        val motionEvent =
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(4L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(pointers[0], PointerId(0), true, 3f, 4f)
        assertPointerInputEventData(pointers[1], PointerId(1), true, 7f, 8f)
    }

    @Test
    fun convertToPointerInputEvent_3pointers1stPointerActionPointerDown_convertsCorrectly() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f)),
            )
        )
        val motionEvent =
            MotionEvent(
                12,
                ACTION_POINTER_DOWN,
                3,
                0,
                arrayOf(PointerProperties(9), PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(10f, 11f), PointerCoords(3f, 4f), PointerCoords(7f, 8f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(12L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(pointers[0], PointerId(2), true, 10f, 11f)
        assertPointerInputEventData(pointers[1], PointerId(0), true, 3f, 4f)
        assertPointerInputEventData(pointers[2], PointerId(1), true, 7f, 8f)
    }

    @Test
    fun convertToPointerInputEvent_3pointers2ndPointerActionPointerDown_convertsCorrectly() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f)),
            )
        )
        val motionEvent =
            MotionEvent(
                12,
                ACTION_POINTER_DOWN,
                3,
                1,
                arrayOf(PointerProperties(2), PointerProperties(9), PointerProperties(5)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(10f, 11f), PointerCoords(7f, 8f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(12L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(pointers[0], PointerId(0), true, 3f, 4f)
        assertPointerInputEventData(pointers[1], PointerId(2), true, 10f, 11f)
        assertPointerInputEventData(pointers[2], PointerId(1), true, 7f, 8f)
    }

    @Test
    fun convertToPointerInputEvent_3pointers3rdPointerActionPointerDown_convertsCorrectly() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f)),
            )
        )
        val motionEvent =
            MotionEvent(
                12,
                ACTION_POINTER_DOWN,
                3,
                2,
                arrayOf(PointerProperties(2), PointerProperties(5), PointerProperties(9)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f), PointerCoords(10f, 11f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(12L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(pointers[0], PointerId(0), true, 3f, 4f)
        assertPointerInputEventData(pointers[1], PointerId(1), true, 7f, 8f)
        assertPointerInputEventData(pointers[2], PointerId(2), true, 10f, 11f)
    }

    @Test
    fun convertToPointerInputEvent_2pointersActionMove_convertsCorrectly() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f)),
            )
        )
        val motionEvent =
            MotionEvent(
                10,
                ACTION_MOVE,
                2,
                0,
                arrayOf(PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(11f, 12f), PointerCoords(13f, 15f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(10L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(pointers[0], PointerId(0), true, 11f, 12f)
        assertPointerInputEventData(pointers[1], PointerId(1), true, 13f, 15f)
    }

    @Test
    fun convertToPointerInputEvent_2pointers1stPointerActionPointerUP_convertsCorrectly() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f)),
            )
        )

        val motionEvent =
            MotionEvent(
                10,
                ACTION_POINTER_UP,
                2,
                0,
                arrayOf(PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f)),
            )
        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(10L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(pointers[0], PointerId(0), false, 3f, 4f)
        assertPointerInputEventData(pointers[1], PointerId(1), true, 7f, 8f)
    }

    @Test
    fun convertToPointerInputEvent_2pointers2ndPointerActionPointerUp_convertsCorrectly() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f)),
            )
        )

        val motionEvent =
            MotionEvent(
                10,
                ACTION_POINTER_UP,
                2,
                1,
                arrayOf(PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f)),
            )
        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(10L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(pointers[0], PointerId(0), true, 3f, 4f)
        assertPointerInputEventData(pointers[1], PointerId(1), false, 7f, 8f)
    }

    @Test
    fun convertToPointerInputEvent_3pointers1stPointerActionPointerUp_convertsCorrectly() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f)),
            )
        )
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                12,
                ACTION_POINTER_DOWN,
                3,
                2,
                arrayOf(PointerProperties(2), PointerProperties(5), PointerProperties(9)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f), PointerCoords(10f, 11f)),
            )
        )

        val motionEvent =
            MotionEvent(
                20,
                ACTION_POINTER_UP,
                3,
                0,
                arrayOf(PointerProperties(2), PointerProperties(5), PointerProperties(9)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f), PointerCoords(10f, 11f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(20L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(pointers[0], PointerId(0), false, 3f, 4f)
        assertPointerInputEventData(pointers[1], PointerId(1), true, 7f, 8f)
        assertPointerInputEventData(pointers[2], PointerId(2), true, 10f, 11f)
    }

    @Test
    fun convertToPointerInputEvent_3pointers2ndPointerActionPointerUp_convertsCorrectly() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f)),
            )
        )
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                12,
                ACTION_POINTER_DOWN,
                3,
                2,
                arrayOf(PointerProperties(2), PointerProperties(5), PointerProperties(9)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f), PointerCoords(10f, 11f)),
            )
        )

        val motionEvent =
            MotionEvent(
                20,
                ACTION_POINTER_UP,
                3,
                1,
                arrayOf(PointerProperties(2), PointerProperties(5), PointerProperties(9)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f), PointerCoords(10f, 11f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(20L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(pointers[0], PointerId(0), true, 3f, 4f)
        assertPointerInputEventData(pointers[1], PointerId(1), false, 7f, 8f)
        assertPointerInputEventData(pointers[2], PointerId(2), true, 10f, 11f)
    }

    @Test
    fun convertToPointerInputEvent_3pointers3rdPointerActionPointerUp_convertsCorrectly() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(PointerProperties(2), PointerProperties(5)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f)),
            )
        )
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                12,
                ACTION_POINTER_DOWN,
                3,
                2,
                arrayOf(PointerProperties(2), PointerProperties(5), PointerProperties(9)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f), PointerCoords(10f, 11f)),
            )
        )

        val motionEvent =
            MotionEvent(
                20,
                ACTION_POINTER_UP,
                3,
                2,
                arrayOf(PointerProperties(2), PointerProperties(5), PointerProperties(9)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(7f, 8f), PointerCoords(10f, 11f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(20L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(pointers[0], PointerId(0), true, 3f, 4f)
        assertPointerInputEventData(pointers[1], PointerId(1), true, 7f, 8f)
        assertPointerInputEventData(pointers[2], PointerId(2), false, 10f, 11f)
    }

    @Test
    fun convertToPointerInputEvent_downUpDownUpDownUpSameMotionEventId_pointerIdsAreUnique() {
        val down1 =
            MotionEvent(
                100,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(10f, 11f)),
            )

        val up1 =
            MotionEvent(
                200,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(10f, 11f)),
            )

        val down2 =
            MotionEvent(
                300,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(20f, 21f)),
            )

        val up2 =
            MotionEvent(
                400,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(20f, 21f)),
            )

        val down3 =
            MotionEvent(
                500,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(30f, 31f)),
            )

        val up3 =
            MotionEvent(
                600,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(30f, 31f)),
            )

        // Test the different events sequentially, since the returned event contains a list that
        // will be reused by convertToPointerInputEvent for performance, so it shouldn't be held
        // for longer than needed during the sequential dispatch.

        val pointerInputEventDown1 = motionEventAdapter.convertToPointerInputEvent(down1)
        assertThat(pointerInputEventDown1).isNotNull()
        assertThat(pointerInputEventDown1!!.pointers[0].id).isEqualTo(PointerId(0))

        val pointerInputEventUp1 = motionEventAdapter.convertToPointerInputEvent(up1)
        assertThat(pointerInputEventUp1).isNotNull()
        assertThat(pointerInputEventUp1!!.pointers[0].id).isEqualTo(PointerId(0))

        val pointerInputEventDown2 = motionEventAdapter.convertToPointerInputEvent(down2)
        assertThat(pointerInputEventDown2).isNotNull()
        assertThat(pointerInputEventDown2!!.pointers[0].id).isEqualTo(PointerId(1))

        val pointerInputEventUp2 = motionEventAdapter.convertToPointerInputEvent(up2)
        assertThat(pointerInputEventUp2).isNotNull()
        assertThat(pointerInputEventUp2!!.pointers[0].id).isEqualTo(PointerId(1))

        val pointerInputEventDown3 = motionEventAdapter.convertToPointerInputEvent(down3)
        assertThat(pointerInputEventDown3).isNotNull()
        assertThat(pointerInputEventDown3!!.pointers[0].id).isEqualTo(PointerId(2))

        val pointerInputEventUp3 = motionEventAdapter.convertToPointerInputEvent(up3)
        assertThat(pointerInputEventUp3).isNotNull()
        assertThat(pointerInputEventUp3!!.pointers[0].id).isEqualTo(PointerId(2))
    }

    @Test
    fun convertToPointerInputEvent_downDownDownRandomMotionEventIds_pointerIdsAreUnique() {
        val down1 =
            MotionEvent(
                100,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(9276)),
                arrayOf(PointerCoords(10f, 11f)),
            )

        val down2 =
            MotionEvent(
                200,
                ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(PointerProperties(9276), PointerProperties(1759)),
                arrayOf(PointerCoords(10f, 11f), PointerCoords(20f, 21f)),
            )

        val down3 =
            MotionEvent(
                300,
                ACTION_POINTER_DOWN,
                3,
                2,
                arrayOf(PointerProperties(9276), PointerProperties(1759), PointerProperties(5043)),
                arrayOf(PointerCoords(10f, 11f), PointerCoords(20f, 21f), PointerCoords(30f, 31f)),
            )

        // Test the different events sequentially, since the returned event contains a list that
        // will be reused by convertToPointerInputEvent for performance, so it shouldn't be held
        // for longer than needed during the sequential dispatch.

        val pointerInputEventDown1 = motionEventAdapter.convertToPointerInputEvent(down1)

        assertThat(pointerInputEventDown1).isNotNull()
        assertThat(pointerInputEventDown1!!.pointers).hasSize(1)
        assertThat(pointerInputEventDown1.pointers[0].id).isEqualTo(PointerId(0))

        val pointerInputEventDown2 = motionEventAdapter.convertToPointerInputEvent(down2)

        assertThat(pointerInputEventDown2).isNotNull()
        assertThat(pointerInputEventDown2!!.pointers).hasSize(2)
        assertThat(pointerInputEventDown2.pointers[0].id).isEqualTo(PointerId(0))
        assertThat(pointerInputEventDown2.pointers[1].id).isEqualTo(PointerId(1))

        val pointerInputEventDown3 = motionEventAdapter.convertToPointerInputEvent(down3)

        assertThat(pointerInputEventDown3).isNotNull()
        assertThat(pointerInputEventDown3!!.pointers).hasSize(3)
        assertThat(pointerInputEventDown2.pointers[0].id).isEqualTo(PointerId(0))
        assertThat(pointerInputEventDown2.pointers[1].id).isEqualTo(PointerId(1))
        assertThat(pointerInputEventDown3.pointers[2].id).isEqualTo(PointerId(2))
    }

    @Test
    fun convertToPointerInputEvent_motionEventOffset_usesRawCoordinatesInsteadOfOffset() {
        val motionEvent =
            MotionEvent(
                0,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(1f, 2f)),
            )

        motionEvent.offsetLocation(10f, 20f)

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val uptime = pointerInputEvent!!.uptime
        val pointers = pointerInputEvent.pointers
        assertThat(uptime).isEqualTo(0L)
        assertThat(pointers).hasSize(1)
        assertPointerInputEventData(
            pointers[0],
            PointerId(0),
            true,
            1f,
            2f,
            originalX = 11f,
            originalY = 22f,
        )
    }

    @Test
    fun convertToPointerInputEvent_actionCancel_returnsNull() {
        val motionEvent =
            MotionEvent(
                0,
                ACTION_CANCEL,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(1f, 2f)),
            )

        motionEvent.offsetLocation(10f, 20f)

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNull()
    }

    @Test
    fun convertToPointerInputEvent_downUp_noPointersTracked() {
        val motionEvent1 =
            MotionEvent(
                2894,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(8290)),
                arrayOf(PointerCoords(2967f, 5928f)),
            )
        val motionEvent2 =
            MotionEvent(
                2894,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(8290)),
                arrayOf(PointerCoords(2967f, 5928f)),
            )

        motionEventAdapter.convertToPointerInputEvent(motionEvent1)
        motionEventAdapter.convertToPointerInputEvent(motionEvent2)

        assertThat(motionEventAdapter.motionEventToComposePointerIdMap.size()).isEqualTo(0)
    }

    @Test
    fun convertToPointerInputEvent_downDown_correctPointersTracked() {
        val motionEvent1 =
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        val motionEvent2 =
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                0,
                arrayOf(PointerProperties(5), PointerProperties(2)),
                arrayOf(PointerCoords(7f, 8f), PointerCoords(3f, 4f)),
            )

        motionEventAdapter.convertToPointerInputEvent(motionEvent1)
        motionEventAdapter.convertToPointerInputEvent(motionEvent2)

        assertThat(motionEventAdapter.motionEventToComposePointerIdMap.toMap())
            .containsExactlyEntriesIn(mapOf(2 to PointerId(0), 5 to PointerId(1)))
    }

    @Test
    fun convertToPointerInputEvent_downDownFirstUp_correctPointerTracked() {
        val motionEvent1 =
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        val motionEvent2 =
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                0,
                arrayOf(PointerProperties(5), PointerProperties(2)),
                arrayOf(PointerCoords(7f, 8f), PointerCoords(3f, 4f)),
            )
        val motionEvent3 =
            MotionEvent(
                10,
                ACTION_POINTER_UP,
                2,
                0,
                arrayOf(PointerProperties(5), PointerProperties(2)),
                arrayOf(PointerCoords(7f, 8f), PointerCoords(3f, 4f)),
            )

        motionEventAdapter.convertToPointerInputEvent(motionEvent1)
        motionEventAdapter.convertToPointerInputEvent(motionEvent2)
        motionEventAdapter.convertToPointerInputEvent(motionEvent3)

        assertThat(motionEventAdapter.motionEventToComposePointerIdMap.toMap())
            .containsExactlyEntriesIn(mapOf(2 to PointerId(0)))
    }

    @Test
    fun convertToPointerInputEvent_downDownSecondUp_correctPointerTracked() {
        val motionEvent1 =
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        val motionEvent2 =
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                0,
                arrayOf(PointerProperties(5), PointerProperties(2)),
                arrayOf(PointerCoords(7f, 8f), PointerCoords(3f, 4f)),
            )
        val motionEvent3 =
            MotionEvent(
                10,
                ACTION_POINTER_UP,
                2,
                1,
                arrayOf(PointerProperties(5), PointerProperties(2)),
                arrayOf(PointerCoords(7f, 8f), PointerCoords(3f, 4f)),
            )

        motionEventAdapter.convertToPointerInputEvent(motionEvent1)
        motionEventAdapter.convertToPointerInputEvent(motionEvent2)
        motionEventAdapter.convertToPointerInputEvent(motionEvent3)

        assertThat(motionEventAdapter.motionEventToComposePointerIdMap.toMap())
            .containsExactlyEntriesIn(mapOf(5 to PointerId(1)))
    }

    @Test
    fun convertToPointerInputEvent_downDownUpUp_noPointersTracked() {
        val motionEvent1 =
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        val motionEvent2 =
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                0,
                arrayOf(PointerProperties(5), PointerProperties(2)),
                arrayOf(PointerCoords(7f, 8f), PointerCoords(3f, 4f)),
            )
        val motionEvent3 =
            MotionEvent(
                10,
                ACTION_POINTER_UP,
                2,
                0,
                arrayOf(PointerProperties(5), PointerProperties(2)),
                arrayOf(PointerCoords(7f, 8f), PointerCoords(3f, 4f)),
            )
        val motionEvent4 =
            MotionEvent(
                20,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )

        motionEventAdapter.convertToPointerInputEvent(motionEvent1)
        motionEventAdapter.convertToPointerInputEvent(motionEvent2)
        motionEventAdapter.convertToPointerInputEvent(motionEvent3)
        motionEventAdapter.convertToPointerInputEvent(motionEvent4)

        assertThat(motionEventAdapter.motionEventToComposePointerIdMap.toMap()).isEmpty()
    }

    @Test
    fun convertToPointerInputEvent_downCancel_noPointersTracked() {
        val motionEvent1 =
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        val motionEvent2 =
            MotionEvent(
                4,
                ACTION_POINTER_DOWN,
                2,
                0,
                arrayOf(PointerProperties(5), PointerProperties(2)),
                arrayOf(PointerCoords(7f, 8f), PointerCoords(3f, 4f)),
            )
        val motionEvent3 =
            MotionEvent(
                10,
                ACTION_CANCEL,
                2,
                0,
                arrayOf(PointerProperties(5), PointerProperties(2)),
                arrayOf(PointerCoords(7f, 8f), PointerCoords(3f, 4f)),
            )
        motionEventAdapter.convertToPointerInputEvent(motionEvent1)
        motionEventAdapter.convertToPointerInputEvent(motionEvent2)
        motionEventAdapter.convertToPointerInputEvent(motionEvent3)

        assertThat(motionEventAdapter.motionEventToComposePointerIdMap.toMap()).isEmpty()
    }

    @Test
    fun convertToPointerInputEvent_doesNotSynchronouslyMutateMotionEvent() {
        val motionEvent =
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        motionEvent.offsetLocation(10f, 100f)

        motionEventAdapter.convertToPointerInputEvent(motionEvent)

        assertThat(motionEvent.x).isEqualTo(13f)
        assertThat(motionEvent.y).isEqualTo(104f)
    }

    @Test
    fun convertToPointerInputEvent_1PointerActionDown_includesMotionEvent() {
        val motionEvent =
            MotionEvent(
                2894,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(8290)),
                arrayOf(PointerCoords(2967f, 5928f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        assertThat(pointerInputEvent!!.motionEvent).isSameInstanceAs(motionEvent)
    }

    @Test
    fun convertToPointerInputEvent_1pointerActionMove_includesMotionEvent() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                1,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        val motionEvent =
            MotionEvent(
                5,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(6f, 7f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        assertThat(pointerInputEvent!!.motionEvent).isSameInstanceAs(motionEvent)
    }

    @Test
    fun convertToPointerInputEvent_1pointerActionUp_includesMotionEvent() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                10,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(46)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        val motionEvent =
            MotionEvent(
                34,
                ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(46)),
                arrayOf(PointerCoords(3f, 4f)),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        assertThat(pointerInputEvent!!.motionEvent).isSameInstanceAs(motionEvent)
    }

    @Test
    fun convertToPointerInputEvent_differentCoordinateSpace_useOriginalPointCoordinate() {
        motionEventAdapter.convertToPointerInputEvent(
            MotionEvent(
                10,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(46)),
                arrayOf(PointerCoords(3f, 4f)),
            )
        )
        val motionEvent =
            MotionEvent(
                34,
                ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(46)),
                arrayOf(PointerCoords(30f, 40f)),
            )

        val positionCalculator =
            object : PositionCalculator by positionCalculator {
                override fun screenToLocal(positionOnScreen: Offset): Offset {
                    return positionOnScreen / 2f
                }
            }

        val pointerInputEvent =
            motionEventAdapter.convertToPointerInputEvent(motionEvent, positionCalculator)
        assertPointerInputEventData(
            pointerInputEvent!!.pointers[0],
            PointerId(0),
            true,
            30f,
            40f,
            originalX = 30f,
            originalY = 40f,
        )
    }

    @Test
    fun convertScrollEvent_horizontalPositive() {
        val motionEvent =
            MotionEvent(
                eventTime = 1,
                action = ACTION_SCROLL,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(2)),
                pointerCoords =
                    arrayOf(PointerCoords(3f, 4f).apply { setAxisValue(AXIS_HSCROLL, 5f) }),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()
        assertThat(pointerInputEvent!!.pointers[0].scrollDelta).isEqualTo(Offset(5f, 0f))
        assertThat(pointerInputEvent.motionEvent).isSameInstanceAs(motionEvent)
    }

    @Test
    fun convertScrollEvent_horizontalNegative() {
        val motionEvent =
            MotionEvent(
                eventTime = 1,
                action = ACTION_SCROLL,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(2)),
                pointerCoords =
                    arrayOf(PointerCoords(3f, 4f).apply { setAxisValue(AXIS_HSCROLL, -5f) }),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()
        assertThat(pointerInputEvent!!.pointers[0].scrollDelta).isEqualTo(Offset(-5f, 0f))
        assertThat(pointerInputEvent.motionEvent).isSameInstanceAs(motionEvent)
    }

    @Test
    fun convertScrollEvent_verticalPositive() {
        val motionEvent =
            MotionEvent(
                eventTime = 1,
                action = ACTION_SCROLL,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(2)),
                pointerCoords =
                    arrayOf(PointerCoords(3f, 4f).apply { setAxisValue(AXIS_VSCROLL, 5f) }),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()
        // Note: y is inverted, per https://r.android.com/2071209
        assertThat(pointerInputEvent!!.pointers[0].scrollDelta).isEqualTo(Offset(0f, -5f))
        assertThat(pointerInputEvent.motionEvent).isSameInstanceAs(motionEvent)
    }

    @Test
    fun convertScrollEvent_verticalNegative() {
        val motionEvent =
            MotionEvent(
                eventTime = 1,
                action = ACTION_SCROLL,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(2)),
                pointerCoords =
                    arrayOf(PointerCoords(3f, 4f).apply { setAxisValue(AXIS_VSCROLL, -5f) }),
            )

        val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()
        // Note: y is inverted, per https://r.android.com/2071209
        assertThat(pointerInputEvent!!.pointers[0].scrollDelta).isEqualTo(Offset(0f, 5f))
        assertThat(pointerInputEvent.motionEvent).isSameInstanceAs(motionEvent)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @OptIn(ExperimentalComposeUiApi::class)
    fun trackpadPanOngoing_resetOnHoverExit() {
        assumeTrue(ComposeUiFlags.isTrackpadPanHoverFixEnabled)
        // Start with normal hover
        val hoverEnter =
            MotionEvent(
                eventTime = 0,
                action = ACTION_HOVER_ENTER,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                pointerCoords = arrayOf(PointerCoords(10f, 10f)),
                classification = MotionEvent.CLASSIFICATION_NONE,
            )
        val event1 = motionEventAdapter.convertToPointerInputEvent(hoverEnter)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isFalse()
        assertThat(event1?.activeGesture).isEqualTo(PointerClassification.None)

        // Swipe starts
        val downSwipe =
            MotionEvent(
                eventTime = 1,
                action = ACTION_DOWN,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_FINGER)),
                pointerCoords = arrayOf(PointerCoords(10f, 10f)),
                classification = MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE,
            )
        val event2 = motionEventAdapter.convertToPointerInputEvent(downSwipe)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isTrue()
        assertThat(event2?.activeGesture).isEqualTo(PointerClassification.Pan)

        // Move event during swipe drops classification to NONE
        val moveSwipe =
            MotionEvent(
                eventTime = 2,
                action = ACTION_MOVE,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_FINGER)),
                pointerCoords = arrayOf(PointerCoords(15f, 15f)),
                classification = MotionEvent.CLASSIFICATION_NONE,
            )
        val event3 = motionEventAdapter.convertToPointerInputEvent(moveSwipe)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isTrue()
        assertThat(event3?.activeGesture).isEqualTo(PointerClassification.Pan)

        // Hover exit with TWO_FINGER_SWIPE classification should not reset
        val hoverExitSwipe =
            MotionEvent(
                eventTime = 3,
                action = ACTION_HOVER_EXIT,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                pointerCoords = arrayOf(PointerCoords(15f, 15f)),
                classification = MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE,
            )
        val event4 = motionEventAdapter.convertToPointerInputEvent(hoverExitSwipe)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isTrue()
        assertThat(event4?.activeGesture).isEqualTo(PointerClassification.Pan)

        // Normal hover exit (classification NONE) should reset
        val hoverExitNormal =
            MotionEvent(
                eventTime = 4,
                action = ACTION_HOVER_EXIT,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                pointerCoords = arrayOf(PointerCoords(15f, 15f)),
                classification = MotionEvent.CLASSIFICATION_NONE,
            )
        val event5 = motionEventAdapter.convertToPointerInputEvent(hoverExitNormal)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isFalse()
        assertThat(event5?.activeGesture).isEqualTo(PointerClassification.None)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @OptIn(ExperimentalComposeUiApi::class)
    fun trackpadPan_coordinatesStationaryAtSwipeStart() {
        assumeTrue(ComposeUiFlags.isTrackpadPanHoverFixEnabled)
        // 1. Hover move to (50f, 60f)
        val hoverMove =
            MotionEvent(
                eventTime = 0,
                action = ACTION_HOVER_MOVE,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                pointerCoords = arrayOf(PointerCoords(50f, 60f)),
                classification = MotionEvent.CLASSIFICATION_NONE,
            )
        motionEventAdapter.convertToPointerInputEvent(hoverMove)

        // 2. Swipe starts at finger position (100f, 200f)
        val downSwipe =
            MotionEvent(
                eventTime = 1,
                action = ACTION_DOWN,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_FINGER)),
                pointerCoords = arrayOf(PointerCoords(100f, 200f)),
                classification = MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE,
            )
        val event2 = motionEventAdapter.convertToPointerInputEvent(downSwipe)

        // The processed position should be stationary at swipe start (100f, 200f), not hover (50f,
        // 60f)
        assertThat(event2).isNotNull()
        assertPointerInputEventData(
            event2!!.pointers[0],
            PointerId(0),
            isDown = false,
            x = 100f,
            y = 200f,
            type = PointerType.Mouse,
            originalX = 100f,
            originalY = 200f,
        )

        // 3. Swipe move to finger position (120f, 220f)
        val moveSwipe =
            MotionEvent(
                eventTime = 2,
                action = ACTION_MOVE,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_FINGER)),
                pointerCoords = arrayOf(PointerCoords(120f, 220f)),
                classification = MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE,
            )
        val event3 = motionEventAdapter.convertToPointerInputEvent(moveSwipe)

        // The processed position should still be stationary at swipe start (100f, 200f)
        assertThat(event3).isNotNull()
        assertPointerInputEventData(
            event3!!.pointers[0],
            PointerId(0),
            isDown = false,
            x = 100f,
            y = 200f,
            type = PointerType.Mouse,
            originalX = 120f,
            originalY = 220f,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun trackpadGestureClassifications() {
        // Test Pinch classification
        val pinchEvent =
            MotionEvent(
                eventTime = 0,
                action = ACTION_DOWN,
                numPointers = 2,
                actionIndex = 0,
                pointerProperties =
                    arrayOf(
                        PointerProperties(1, TOOL_TYPE_FINGER),
                        PointerProperties(2, TOOL_TYPE_FINGER),
                    ),
                pointerCoords = arrayOf(PointerCoords(10f, 10f), PointerCoords(20f, 20f)),
                classification = MotionEvent.CLASSIFICATION_PINCH,
            )
        val pinchResult = motionEventAdapter.convertToPointerInputEvent(pinchEvent)
        assertThat(pinchResult?.activeGesture).isEqualTo(PointerClassification.Pinch)

        // Test Ambiguous classification
        val ambiguousEvent =
            MotionEvent(
                eventTime = 1,
                action = ACTION_DOWN,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_FINGER)),
                pointerCoords = arrayOf(PointerCoords(10f, 10f)),
                classification = MotionEvent.CLASSIFICATION_AMBIGUOUS_GESTURE,
            )
        val ambiguousResult = motionEventAdapter.convertToPointerInputEvent(ambiguousEvent)
        assertThat(ambiguousResult?.activeGesture).isEqualTo(PointerClassification.Ambiguous)

        // Test Deep Press classification
        val deepPressEvent =
            MotionEvent(
                eventTime = 2,
                action = ACTION_DOWN,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_FINGER)),
                pointerCoords = arrayOf(PointerCoords(10f, 10f)),
                classification = MotionEvent.CLASSIFICATION_DEEP_PRESS,
            )
        val deepPressResult = motionEventAdapter.convertToPointerInputEvent(deepPressEvent)
        assertThat(deepPressResult?.activeGesture).isEqualTo(PointerClassification.DeepPress)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @OptIn(ExperimentalComposeUiApi::class)
    fun trackpadPanOngoing_resetOnHoverEnter() {
        assumeTrue(ComposeUiFlags.isTrackpadPanHoverFixEnabled)
        // Start with normal hover
        val hoverEnter =
            MotionEvent(
                eventTime = 0,
                action = ACTION_HOVER_ENTER,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                pointerCoords = arrayOf(PointerCoords(10f, 10f)),
                classification = MotionEvent.CLASSIFICATION_NONE,
            )
        motionEventAdapter.convertToPointerInputEvent(hoverEnter)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isFalse()

        // Swipe starts
        val downSwipe =
            MotionEvent(
                eventTime = 1,
                action = ACTION_DOWN,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_FINGER)),
                pointerCoords = arrayOf(PointerCoords(10f, 10f)),
                classification = MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE,
            )
        motionEventAdapter.convertToPointerInputEvent(downSwipe)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isTrue()

        // Normal hover enter (classification NONE) should reset
        val hoverEnterNormal =
            MotionEvent(
                eventTime = 2,
                action = ACTION_HOVER_ENTER,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                pointerCoords = arrayOf(PointerCoords(15f, 15f)),
                classification = MotionEvent.CLASSIFICATION_NONE,
            )
        motionEventAdapter.convertToPointerInputEvent(hoverEnterNormal)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isFalse()

        // Start another swipe
        motionEventAdapter.convertToPointerInputEvent(downSwipe)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isTrue()

        // End swipe stream with ACTION_UP
        val upSwipe =
            MotionEvent(
                eventTime = 3,
                action = ACTION_UP,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_FINGER)),
                pointerCoords = arrayOf(PointerCoords(10f, 10f)),
                classification = MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE,
            )
        motionEventAdapter.convertToPointerInputEvent(upSwipe)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isFalse()

        // Normal hover move (classification NONE) should reset
        val hoverMoveNormal =
            MotionEvent(
                eventTime = 4,
                action = ACTION_HOVER_MOVE,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                pointerCoords = arrayOf(PointerCoords(15f, 15f)),
                classification = MotionEvent.CLASSIFICATION_NONE,
            )
        motionEventAdapter.convertToPointerInputEvent(hoverMoveNormal)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @OptIn(ExperimentalComposeUiApi::class)
    fun trackpadPanOngoing_noResetOnHoverMoveDuringSwipe() {
        assumeTrue(ComposeUiFlags.isTrackpadPanHoverFixEnabled)
        // Start with normal hover
        val hoverEnter =
            MotionEvent(
                eventTime = 0,
                action = ACTION_HOVER_ENTER,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                pointerCoords = arrayOf(PointerCoords(10f, 10f)),
                classification = MotionEvent.CLASSIFICATION_NONE,
            )
        motionEventAdapter.convertToPointerInputEvent(hoverEnter)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isFalse()

        // Swipe starts
        val downSwipe =
            MotionEvent(
                eventTime = 1,
                action = ACTION_DOWN,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_FINGER)),
                pointerCoords = arrayOf(PointerCoords(10f, 10f)),
                classification = MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE,
            )
        motionEventAdapter.convertToPointerInputEvent(downSwipe)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isTrue()

        // Interleaved hover move (classification NONE) during active touch stream should NOT reset!
        val hoverMoveInterleaved =
            MotionEvent(
                eventTime = 2,
                action = ACTION_HOVER_MOVE,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_MOUSE)),
                pointerCoords = arrayOf(PointerCoords(15f, 15f)),
                classification = MotionEvent.CLASSIFICATION_NONE,
            )
        motionEventAdapter.convertToPointerInputEvent(hoverMoveInterleaved)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @OptIn(ExperimentalComposeUiApi::class)
    fun trackpadPanOngoing_noResetOnSimulatedHoverMoveMidGesture() {
        assumeTrue(ComposeUiFlags.isTrackpadPanHoverFixEnabled)
        // Start mid-gesture with ACTION_MOVE (swipe)
        // This simulates the case where the adapter might have missed ACTION_DOWN,
        // or we want to verify that isTrackpadPanOngoing alone prevents the reset.
        val moveSwipe =
            MotionEvent(
                eventTime = 1,
                action = ACTION_MOVE,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_FINGER)),
                pointerCoords = arrayOf(PointerCoords(10f, 10f)),
                classification = MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE,
            )
        motionEventAdapter.convertToPointerInputEvent(moveSwipe)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isTrue()

        // Simulated hover move (classification NONE) should NOT reset if pan is ongoing,
        // even if isAnyPointerDown is false.
        val simulatedHoverMove =
            MotionEvent(
                eventTime = 2,
                action = ACTION_HOVER_MOVE,
                numPointers = 1,
                actionIndex = 0,
                pointerProperties = arrayOf(PointerProperties(1, TOOL_TYPE_FINGER)),
                pointerCoords = arrayOf(PointerCoords(10f, 10f)),
                classification = MotionEvent.CLASSIFICATION_NONE,
            )
        motionEventAdapter.convertToPointerInputEvent(simulatedHoverMove)
        assertThat(motionEventAdapter.isTrackpadPanOngoing).isTrue()
    }

    private fun MotionEventAdapter.convertToPointerInputEvent(motionEvent: MotionEvent) =
        convertToPointerInputEvent(motionEvent, positionCalculator)

    private fun SparseLongArray.toMap(): Map<Int, PointerId> {
        val map = mutableMapOf<Int, PointerId>()
        for (i in 0 until size()) {
            val key = keyAt(i)
            val value = valueAt(i)
            map[key] = PointerId(value)
        }
        return map
    }
}

// Private helper functions

private fun MotionEvent(
    eventTime: Int,
    action: Int,
    numPointers: Int,
    actionIndex: Int,
    pointerProperties: Array<MotionEvent.PointerProperties>,
    pointerCoords: Array<MotionEvent.PointerCoords>,
    downTime: Long = 0,
) =
    MotionEvent.obtain(
        downTime,
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
        InputDevice.SOURCE_TOUCHSCREEN,
        0,
    )

private fun assertPointerInputEventData(
    actual: PointerInputEventData,
    id: PointerId,
    isDown: Boolean,
    x: Float,
    y: Float,
    type: PointerType = PointerType.Touch,
    originalX: Float = x,
    originalY: Float = y,
) {
    assertThat(actual.id).isEqualTo(id)
    assertThat(actual.down).isEqualTo(isDown)
    assertThat(actual.positionOnScreen.x).isEqualTo(x)
    assertThat(actual.positionOnScreen.y).isEqualTo(y)
    assertThat(actual.originalEventPosition.x).isEqualTo(originalX)
    assertThat(actual.originalEventPosition.y).isEqualTo(originalY)
    assertThat(actual.type).isEqualTo(type)
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun MotionEvent(
    eventTime: Int,
    action: Int,
    numPointers: Int,
    actionIndex: Int,
    pointerProperties: Array<MotionEvent.PointerProperties>,
    pointerCoords: Array<MotionEvent.PointerCoords>,
    classification: Int,
    downTime: Long = 0,
): MotionEvent =
    MotionEvent.obtain(
        downTime,
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
        InputDevice.SOURCE_MOUSE,
        0,
        0,
        classification,
    )!!
