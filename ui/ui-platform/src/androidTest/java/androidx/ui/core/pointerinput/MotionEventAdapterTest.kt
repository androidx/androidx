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
import androidx.ui.core.PointerId
import androidx.ui.unit.Uptime
import androidx.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class MotionEventAdapterTest {

    lateinit var motionEvenAdapter: MotionEventAdapter

    @Before
    fun setup() {
        motionEvenAdapter = MotionEventAdapter()
    }

    @Test
    fun processMotionEvent_1pointerActionDown_convertsCorrectly() {
        val motionEvent = MotionEvent(
            2894,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(8290)),
            arrayOf(PointerCoords(2967f, 5928f))
        )

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(2_894_000_000L)
        assertThat(pointers).hasSize(1)
        assertPointerInputEventData(
            pointers[0],
            PointerId(8290, Uptime.Boot + 2894.milliseconds),
            true,
            2967f,
            5928f
        )
    }

    @Test
    fun processMotionEvent_1pointerActionMove_convertsCorrectly() {
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        val motionEvent = MotionEvent(
            5,
            MotionEvent.ACTION_MOVE,
            1,
            0,
            arrayOf(PointerProperties(2)),
            arrayOf(PointerCoords(6f, 7f))
        )

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(5_000_000L)
        assertThat(pointers).hasSize(1)
        assertPointerInputEventData(
            pointers[0],
            PointerId(2, Uptime.Boot + 1.milliseconds),
            true,
            6f,
            7f
        )
    }

    @Test
    fun processMotionEvent_1pointerActionUp_convertsCorrectly() {
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                10,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(46)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        val motionEvent = MotionEvent(
            34,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(46)),
            arrayOf(PointerCoords(3f, 4f))
        )

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(34_000_000L)
        assertThat(uptime.nanoseconds).isEqualTo(34_000_000L)
        assertThat(pointers).hasSize(1)
        assertPointerInputEventData(
            pointers[0],
            PointerId(46, Uptime.Boot + 10.milliseconds),
            false,
            3f,
            4f
        )
    }

    @Test
    fun processMotionEvent_2pointers1stPointerActionPointerDown_convertsCorrectly() {
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        val motionEvent = MotionEvent(
            4,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            0,
            arrayOf(
                PointerProperties(5),
                PointerProperties(2)
            ),
            arrayOf(
                PointerCoords(7f, 8f),
                PointerCoords(3f, 4f)
            )
        )

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(4_000_000L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(
            pointers[1],
            PointerId(2, Uptime.Boot + 1.milliseconds),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[0],
            PointerId(5, Uptime.Boot + 4.milliseconds),
            true,
            7f,
            8f
        )
    }

    @Test
    fun processMotionEvent_2pointers2ndPointerActionPointerDown_convertsCorrectly() {
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        val motionEvent = MotionEvent(
            4,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            1,
            arrayOf(
                PointerProperties(2),
                PointerProperties(5)
            ),
            arrayOf(
                PointerCoords(3f, 4f),
                PointerCoords(7f, 8f)
            )
        )

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(4_000_000L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(
            pointers[0],
            PointerId(2, Uptime.Boot + 1.milliseconds),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(5, Uptime.Boot + 4.milliseconds),
            true,
            7f,
            8f
        )
    }

    @Test
    fun processMotionEvent_3pointers1stPointerActionPointerDown_convertsCorrectly() {
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                4,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(5)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f)
                )
            )
        )
        val motionEvent =
            MotionEvent(
                12,
                MotionEvent.ACTION_POINTER_DOWN,
                3,
                0,
                arrayOf(
                    PointerProperties(9),
                    PointerProperties(2),
                    PointerProperties(5)
                ),
                arrayOf(
                    PointerCoords(10f, 11f),
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f)
                )
            )

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(12_000_000L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(
            pointers[0],
            PointerId(9, Uptime.Boot + 12.milliseconds),
            true,
            10f,
            11f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(2, Uptime.Boot + 1.milliseconds),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[2],
            PointerId(5, Uptime.Boot + 4.milliseconds),
            true,
            7f,
            8f
        )
    }

    @Test
    fun processMotionEvent_3pointers2ndPointerActionPointerDown_convertsCorrectly() {
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                4,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(5)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f)
                )
            )
        )
        val motionEvent =
            MotionEvent(
                12,
                MotionEvent.ACTION_POINTER_DOWN,
                3,
                1,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(9),
                    PointerProperties(5)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(10f, 11f),
                    PointerCoords(7f, 8f)
                )
            )

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(12_000_000L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(
            pointers[0],
            PointerId(2, Uptime.Boot + 1.milliseconds),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(9, Uptime.Boot + 12.milliseconds),
            true,
            10f,
            11f
        )
        assertPointerInputEventData(
            pointers[2],
            PointerId(5, Uptime.Boot + 4.milliseconds),
            true,
            7f,
            8f
        )
    }

    @Test
    fun processMotionEvent_3pointers3rdPointerActionPointerDown_convertsCorrectly() {
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                4,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(5)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f)
                )
            )
        )
        val motionEvent =
            MotionEvent(
                12,
                MotionEvent.ACTION_POINTER_DOWN,
                3,
                2,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(5),
                    PointerProperties(9)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f),
                    PointerCoords(10f, 11f)
                )
            )

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(12_000_000L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(
            pointers[0],
            PointerId(2, Uptime.Boot + 1.milliseconds),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(5, Uptime.Boot + 4.milliseconds),
            true,
            7f,
            8f
        )
        assertPointerInputEventData(
            pointers[2],
            PointerId(9, Uptime.Boot + 12.milliseconds),
            true,
            10f,
            11f
        )
    }

    @Test
    fun processMotionEvent_2pointersActionMove_convertsCorrectly() {
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                4,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(5)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f)
                )
            )
        )
        val motionEvent = MotionEvent(
            10,
            MotionEvent.ACTION_MOVE,
            2,
            0,
            arrayOf(
                PointerProperties(2),
                PointerProperties(5)
            ),
            arrayOf(
                PointerCoords(11f, 12f),
                PointerCoords(13f, 15f)
            )
        )

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(10_000_000L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(
            pointers[0],
            PointerId(2, Uptime.Boot + 1.milliseconds),
            true,
            11f,
            12f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(5, Uptime.Boot + 4.milliseconds),
            true,
            13f,
            15f
        )
    }

    @Test
    fun processMotionEvent_2pointers1stPointerActionPointerUP_convertsCorrectly() {
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                4,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(5)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f)
                )
            )
        )

        val motionEvent = MotionEvent(
            10,
            MotionEvent.ACTION_POINTER_UP,
            2,
            0,
            arrayOf(
                PointerProperties(2),
                PointerProperties(5)
            ),
            arrayOf(
                PointerCoords(3f, 4f),
                PointerCoords(7f, 8f)
            )
        )
        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(10_000_000L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(
            pointers[0],
            PointerId(2, Uptime.Boot + 1.milliseconds),
            false,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(5, Uptime.Boot + 4.milliseconds),
            true,
            7f,
            8f
        )
    }

    @Test
    fun processMotionEvent_2pointers2ndPointerActionPointerUp_convertsCorrectly() {
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                4,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(5)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f)
                )
            )
        )

        val motionEvent = MotionEvent(
            10,
            MotionEvent.ACTION_POINTER_UP,
            2,
            1,
            arrayOf(
                PointerProperties(2),
                PointerProperties(5)
            ),
            arrayOf(
                PointerCoords(3f, 4f),
                PointerCoords(7f, 8f)
            )
        )
        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(10_000_000L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(
            pointers[0],
            PointerId(2, Uptime.Boot + 1.milliseconds),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(5, Uptime.Boot + 4.milliseconds),
            false,
            7f,
            8f
        )
    }

    @Test
    fun processMotionEvent_3pointers1stPointerActionPointerUp_convertsCorrectly() {
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                4,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(5)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f)
                )
            )
        )
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                12,
                MotionEvent.ACTION_POINTER_DOWN,
                3,
                2,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(5),
                    PointerProperties(9)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f),
                    PointerCoords(10f, 11f)
                )
            )
        )

        val motionEvent = MotionEvent(
            20,
            MotionEvent.ACTION_POINTER_UP,
            3,
            0,
            arrayOf(
                PointerProperties(2),
                PointerProperties(5),
                PointerProperties(9)
            ),
            arrayOf(
                PointerCoords(3f, 4f),
                PointerCoords(7f, 8f),
                PointerCoords(10f, 11f)
            )
        )

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(20_000_000L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(
            pointers[0],
            PointerId(2, Uptime.Boot + 1.milliseconds),
            false,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(5, Uptime.Boot + 4.milliseconds),
            true,
            7f,
            8f
        )
        assertPointerInputEventData(
            pointers[2],
            PointerId(9, Uptime.Boot + 12.milliseconds),
            true,
            10f,
            11f
        )
    }

    @Test
    fun processMotionEvent_3pointers2ndPointerActionPointerUp_convertsCorrectly() {
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                4,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(5)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f)
                )
            )
        )
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                12,
                MotionEvent.ACTION_POINTER_DOWN,
                3,
                2,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(5),
                    PointerProperties(9)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f),
                    PointerCoords(10f, 11f)
                )
            )
        )

        val motionEvent = MotionEvent(
            20,
            MotionEvent.ACTION_POINTER_UP,
            3,
            1,
            arrayOf(
                PointerProperties(2),
                PointerProperties(5),
                PointerProperties(9)
            ),
            arrayOf(
                PointerCoords(3f, 4f),
                PointerCoords(7f, 8f),
                PointerCoords(10f, 11f)
            )
        )

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(20_000_000L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(
            pointers[0],
            PointerId(2, Uptime.Boot + 1.milliseconds),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(5, Uptime.Boot + 4.milliseconds),
            false,
            7f,
            8f
        )
        assertPointerInputEventData(
            pointers[2],
            PointerId(9, Uptime.Boot + 12.milliseconds),
            true,
            10f,
            11f
        )
    }

    @Test
    fun processMotionEvent_3pointers3rdPointerActionPointerUp_convertsCorrectly() {
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                4,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(5)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f)
                )
            )
        )
        motionEvenAdapter.processMotionEvent(
            MotionEvent(
                12,
                MotionEvent.ACTION_POINTER_DOWN,
                3,
                2,
                arrayOf(
                    PointerProperties(2),
                    PointerProperties(5),
                    PointerProperties(9)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(7f, 8f),
                    PointerCoords(10f, 11f)
                )
            )
        )

        val motionEvent = MotionEvent(
            20,
            MotionEvent.ACTION_POINTER_UP,
            3,
            2,
            arrayOf(
                PointerProperties(2),
                PointerProperties(5),
                PointerProperties(9)
            ),
            arrayOf(
                PointerCoords(3f, 4f),
                PointerCoords(7f, 8f),
                PointerCoords(10f, 11f)
            )
        )

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(20_000_000L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(
            pointers[0],
            PointerId(2, Uptime.Boot + 1.milliseconds),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(5, Uptime.Boot + 4.milliseconds),
            true,
            7f,
            8f
        )
        assertPointerInputEventData(
            pointers[2],
            PointerId(9, Uptime.Boot + 12.milliseconds),
            false,
            10f,
            11f
        )
    }

    @Test
    fun processMotionEvent_motionEventOffset_usesRawCoordinatesInsteadOfOffset() {
        val motionEvent = MotionEvent(
            0,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(1f, 2f))
        )

        motionEvent.offsetLocation(10f, 20f)

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(0L)
        assertThat(pointers).hasSize(1)
        assertPointerInputEventData(pointers[0], PointerId(0, Uptime.Boot), true, 1f, 2f)
    }

    @Test
    fun processMotionEvent_actionCancel_returnsNull() {
        val motionEvent = MotionEvent(
            0,
            MotionEvent.ACTION_CANCEL,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(1f, 2f))
        )

        motionEvent.offsetLocation(10f, 20f)

        val pointerInputEvent = motionEvenAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNull()
    }

    @Test
    fun processMotionEvent_downUp_noPointersTracked() {
        val motionEvent1 = MotionEvent(
            2894,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(8290)),
            arrayOf(PointerCoords(2967f, 5928f))
        )
        val motionEvent2 = MotionEvent(
            2894,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(8290)),
            arrayOf(PointerCoords(2967f, 5928f))
        )

        motionEvenAdapter.processMotionEvent(motionEvent1)
        motionEvenAdapter.processMotionEvent(motionEvent2)

        assertThat(motionEvenAdapter.intIdToPointerIdMap).isEmpty()
    }

    @Test
    fun processMotionEvent_downDown_correctPointersTracked() {
        val motionEvent1 = MotionEvent(
            1,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(2)),
            arrayOf(PointerCoords(3f, 4f))
        )
        val motionEvent2 = MotionEvent(
            4,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            0,
            arrayOf(
                PointerProperties(5),
                PointerProperties(2)
            ),
            arrayOf(
                PointerCoords(7f, 8f),
                PointerCoords(3f, 4f)
            )
        )

        motionEvenAdapter.processMotionEvent(motionEvent1)
        motionEvenAdapter.processMotionEvent(motionEvent2)

        assertThat(motionEvenAdapter.intIdToPointerIdMap).containsExactlyEntriesIn(
            mapOf(
                2 to PointerId(2, Uptime.Boot + 1.milliseconds),
                5 to PointerId(5, Uptime.Boot + 4.milliseconds)
            )
        )
    }

    @Test
    fun processMotionEvent_downDownFirstUp_correctPointerTracked() {
        val motionEvent1 = MotionEvent(
            1,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(2)),
            arrayOf(PointerCoords(3f, 4f))
        )
        val motionEvent2 = MotionEvent(
            4,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            0,
            arrayOf(
                PointerProperties(5),
                PointerProperties(2)
            ),
            arrayOf(
                PointerCoords(7f, 8f),
                PointerCoords(3f, 4f)
            )
        )
        val motionEvent3 = MotionEvent(
            10,
            MotionEvent.ACTION_POINTER_UP,
            2,
            0,
            arrayOf(
                PointerProperties(5),
                PointerProperties(2)
            ),
            arrayOf(
                PointerCoords(7f, 8f),
                PointerCoords(3f, 4f)
            )
        )

        motionEvenAdapter.processMotionEvent(motionEvent1)
        motionEvenAdapter.processMotionEvent(motionEvent2)
        motionEvenAdapter.processMotionEvent(motionEvent3)

        assertThat(motionEvenAdapter.intIdToPointerIdMap).containsExactlyEntriesIn(
            mapOf(2 to PointerId(2, Uptime.Boot + 1.milliseconds))
        )
    }

    @Test
    fun processMotionEvent_downDownSecondUp_correctPointerTracked() {
        val motionEvent1 = MotionEvent(
            1,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(2)),
            arrayOf(PointerCoords(3f, 4f))
        )
        val motionEvent2 = MotionEvent(
            4,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            0,
            arrayOf(
                PointerProperties(5),
                PointerProperties(2)
            ),
            arrayOf(
                PointerCoords(7f, 8f),
                PointerCoords(3f, 4f)
            )
        )
        val motionEvent3 = MotionEvent(
            10,
            MotionEvent.ACTION_POINTER_UP,
            2,
            1,
            arrayOf(
                PointerProperties(5),
                PointerProperties(2)
            ),
            arrayOf(
                PointerCoords(7f, 8f),
                PointerCoords(3f, 4f)
            )
        )

        motionEvenAdapter.processMotionEvent(motionEvent1)
        motionEvenAdapter.processMotionEvent(motionEvent2)
        motionEvenAdapter.processMotionEvent(motionEvent3)

        assertThat(motionEvenAdapter.intIdToPointerIdMap).containsExactlyEntriesIn(
            mapOf(5 to PointerId(5, Uptime.Boot + 4.milliseconds))
        )
    }

    @Test
    fun processMotionEvent_downDownUpUp_noPointersTracked() {
        val motionEvent1 = MotionEvent(
            1,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(2)),
            arrayOf(PointerCoords(3f, 4f))
        )
        val motionEvent2 = MotionEvent(
            4,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            0,
            arrayOf(
                PointerProperties(5),
                PointerProperties(2)
            ),
            arrayOf(
                PointerCoords(7f, 8f),
                PointerCoords(3f, 4f)
            )
        )
        val motionEvent3 = MotionEvent(
            10,
            MotionEvent.ACTION_POINTER_UP,
            2,
            0,
            arrayOf(
                PointerProperties(5),
                PointerProperties(2)
            ),
            arrayOf(
                PointerCoords(7f, 8f),
                PointerCoords(3f, 4f)
            )
        )
        val motionEvent4 = MotionEvent(
            20,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(2)),
            arrayOf(PointerCoords(3f, 4f))
        )

        motionEvenAdapter.processMotionEvent(motionEvent1)
        motionEvenAdapter.processMotionEvent(motionEvent2)
        motionEvenAdapter.processMotionEvent(motionEvent3)
        motionEvenAdapter.processMotionEvent(motionEvent4)

        assertThat(motionEvenAdapter.intIdToPointerIdMap).isEmpty()
    }

    @Test
    fun processMotionEvent_downCancel_noPointersTracked() {
        val motionEvent1 = MotionEvent(
            1,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(2)),
            arrayOf(PointerCoords(3f, 4f))
        )
        val motionEvent2 = MotionEvent(
            4,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            0,
            arrayOf(
                PointerProperties(5),
                PointerProperties(2)
            ),
            arrayOf(
                PointerCoords(7f, 8f),
                PointerCoords(3f, 4f)
            )
        )
        val motionEvent3 = MotionEvent(
            10,
            MotionEvent.ACTION_CANCEL,
            2,
            0,
            arrayOf(
                PointerProperties(5),
                PointerProperties(2)
            ),
            arrayOf(
                PointerCoords(7f, 8f),
                PointerCoords(3f, 4f)
            )
        )
        motionEvenAdapter.processMotionEvent(motionEvent1)
        motionEvenAdapter.processMotionEvent(motionEvent2)
        motionEvenAdapter.processMotionEvent(motionEvent3)

        assertThat(motionEvenAdapter.intIdToPointerIdMap).isEmpty()
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
        id: PointerId,
        isDown: Boolean,
        x: Float,
        y: Float
    ) {
        val pointerInputData = actual.pointerInputData
        assertThat(actual.id).isEqualTo(id)
        assertThat(pointerInputData.down).isEqualTo(isDown)
        assertThat(pointerInputData.position!!.x.value).isEqualTo(x)
        assertThat(pointerInputData.position!!.y.value).isEqualTo(y)
    }
}
