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
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class MotionEventAdapterTest {

    lateinit var motionEventAdapter: MotionEventAdapter

    @Before
    fun setup() {
        motionEventAdapter = MotionEventAdapter()
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(2_894_000_000L)
        assertThat(pointers).hasSize(1)
        assertPointerInputEventData(
            pointers[0],
            PointerId(0),
            true,
            2967f,
            5928f
        )
    }

    @Test
    fun processMotionEvent_1pointerActionMove_convertsCorrectly() {
        motionEventAdapter.processMotionEvent(
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(5_000_000L)
        assertThat(pointers).hasSize(1)
        assertPointerInputEventData(
            pointers[0],
            PointerId(0),
            true,
            6f,
            7f
        )
    }

    @Test
    fun processMotionEvent_1pointerActionUp_convertsCorrectly() {
        motionEventAdapter.processMotionEvent(
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(34_000_000L)
        assertThat(uptime.nanoseconds).isEqualTo(34_000_000L)
        assertThat(pointers).hasSize(1)
        assertPointerInputEventData(
            pointers[0],
            PointerId(0),
            false,
            3f,
            4f
        )
    }

    @Test
    fun processMotionEvent_2pointers1stPointerActionPointerDown_convertsCorrectly() {
        motionEventAdapter.processMotionEvent(
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(4_000_000L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(
            pointers[0],
            PointerId(1),
            true,
            7f,
            8f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(0),
            true,
            3f,
            4f
        )
    }

    @Test
    fun processMotionEvent_2pointers2ndPointerActionPointerDown_convertsCorrectly() {
        motionEventAdapter.processMotionEvent(
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(4_000_000L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(
            pointers[0],
            PointerId(0),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(1),
            true,
            7f,
            8f
        )
    }

    @Test
    fun processMotionEvent_3pointers1stPointerActionPointerDown_convertsCorrectly() {
        motionEventAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEventAdapter.processMotionEvent(
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(12_000_000L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(
            pointers[0],
            PointerId(2),
            true,
            10f,
            11f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(0),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[2],
            PointerId(1),
            true,
            7f,
            8f
        )
    }

    @Test
    fun processMotionEvent_3pointers2ndPointerActionPointerDown_convertsCorrectly() {
        motionEventAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEventAdapter.processMotionEvent(
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(12_000_000L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(
            pointers[0],
            PointerId(0),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(2),
            true,
            10f,
            11f
        )
        assertPointerInputEventData(
            pointers[2],
            PointerId(1),
            true,
            7f,
            8f
        )
    }

    @Test
    fun processMotionEvent_3pointers3rdPointerActionPointerDown_convertsCorrectly() {
        motionEventAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEventAdapter.processMotionEvent(
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(12_000_000L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(
            pointers[0],
            PointerId(0),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(1),
            true,
            7f,
            8f
        )
        assertPointerInputEventData(
            pointers[2],
            PointerId(2),
            true,
            10f,
            11f
        )
    }

    @Test
    fun processMotionEvent_2pointersActionMove_convertsCorrectly() {
        motionEventAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEventAdapter.processMotionEvent(
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(10_000_000L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(
            pointers[0],
            PointerId(0),
            true,
            11f,
            12f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(1),
            true,
            13f,
            15f
        )
    }

    @Test
    fun processMotionEvent_2pointers1stPointerActionPointerUP_convertsCorrectly() {
        motionEventAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEventAdapter.processMotionEvent(
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
        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(10_000_000L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(
            pointers[0],
            PointerId(0),
            false,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(1),
            true,
            7f,
            8f
        )
    }

    @Test
    fun processMotionEvent_2pointers2ndPointerActionPointerUp_convertsCorrectly() {
        motionEventAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEventAdapter.processMotionEvent(
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
        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(10_000_000L)
        assertThat(pointers).hasSize(2)
        assertPointerInputEventData(
            pointers[0],
            PointerId(0),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(1),
            false,
            7f,
            8f
        )
    }

    @Test
    fun processMotionEvent_3pointers1stPointerActionPointerUp_convertsCorrectly() {
        motionEventAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEventAdapter.processMotionEvent(
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
        motionEventAdapter.processMotionEvent(
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(20_000_000L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(
            pointers[0],
            PointerId(0),
            false,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(1),
            true,
            7f,
            8f
        )
        assertPointerInputEventData(
            pointers[2],
            PointerId(2),
            true,
            10f,
            11f
        )
    }

    @Test
    fun processMotionEvent_3pointers2ndPointerActionPointerUp_convertsCorrectly() {
        motionEventAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEventAdapter.processMotionEvent(
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
        motionEventAdapter.processMotionEvent(
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(20_000_000L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(
            pointers[0],
            PointerId(0),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(1),
            false,
            7f,
            8f
        )
        assertPointerInputEventData(
            pointers[2],
            PointerId(2),
            true,
            10f,
            11f
        )
    }

    @Test
    fun processMotionEvent_3pointers3rdPointerActionPointerUp_convertsCorrectly() {
        motionEventAdapter.processMotionEvent(
            MotionEvent(
                1,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(2)),
                arrayOf(PointerCoords(3f, 4f))
            )
        )
        motionEventAdapter.processMotionEvent(
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
        motionEventAdapter.processMotionEvent(
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(20_000_000L)
        assertThat(pointers).hasSize(3)
        assertPointerInputEventData(
            pointers[0],
            PointerId(0),
            true,
            3f,
            4f
        )
        assertPointerInputEventData(
            pointers[1],
            PointerId(1),
            true,
            7f,
            8f
        )
        assertPointerInputEventData(
            pointers[2],
            PointerId(2),
            false,
            10f,
            11f
        )
    }

    @Test
    fun processMotionEvent_downUpDownUpDownUpSameMotionEventId_pointerIdsAreUnique() {
        val down1 = MotionEvent(
            100,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(10f, 11f))
        )

        val up1 = MotionEvent(
            200,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(10f, 11f))
        )

        val down2 = MotionEvent(
            300,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(20f, 21f))
        )

        val up2 = MotionEvent(
            400,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(20f, 21f))
        )

        val down3 = MotionEvent(
            500,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(30f, 31f))
        )

        val up3 = MotionEvent(
            600,
            MotionEvent.ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(30f, 31f))
        )

        val pointerInputEventDown1 = motionEventAdapter.processMotionEvent(down1)
        val pointerInputEventUp1 = motionEventAdapter.processMotionEvent(up1)
        val pointerInputEventDown2 = motionEventAdapter.processMotionEvent(down2)
        val pointerInputEventUp2 = motionEventAdapter.processMotionEvent(up2)
        val pointerInputEventDown3 = motionEventAdapter.processMotionEvent(down3)
        val pointerInputEventUp3 = motionEventAdapter.processMotionEvent(up3)

        assertThat(pointerInputEventDown1).isNotNull()
        assertThat(pointerInputEventUp1).isNotNull()
        assertThat(pointerInputEventDown2).isNotNull()
        assertThat(pointerInputEventUp2).isNotNull()
        assertThat(pointerInputEventDown3).isNotNull()
        assertThat(pointerInputEventUp3).isNotNull()

        assertThat(pointerInputEventDown1!!.pointers[0].id).isEqualTo(PointerId(0))
        assertThat(pointerInputEventUp1!!.pointers[0].id).isEqualTo(PointerId(0))
        assertThat(pointerInputEventDown2!!.pointers[0].id).isEqualTo(PointerId(1))
        assertThat(pointerInputEventUp2!!.pointers[0].id).isEqualTo(PointerId(1))
        assertThat(pointerInputEventDown3!!.pointers[0].id).isEqualTo(PointerId(2))
        assertThat(pointerInputEventUp3!!.pointers[0].id).isEqualTo(PointerId(2))
    }

    @Test
    fun processMotionEvent_downDownDownRandomMotionEventIds_pointerIdsAreUnique() {
        val down1 = MotionEvent(
            100,
            MotionEvent.ACTION_DOWN,
            1,
            0,
            arrayOf(
                PointerProperties(9276)
            ),
            arrayOf(
                PointerCoords(10f, 11f)
            )
        )

        val down2 = MotionEvent(
            200,
            MotionEvent.ACTION_POINTER_DOWN,
            2,
            1,
            arrayOf(
                PointerProperties(9276),
                PointerProperties(1759)
            ),
            arrayOf(
                PointerCoords(10f, 11f),
                PointerCoords(20f, 21f)
            )
        )

        val down3 = MotionEvent(
            300,
            MotionEvent.ACTION_POINTER_DOWN,
            3,
            2,
            arrayOf(
                PointerProperties(9276),
                PointerProperties(1759),
                PointerProperties(5043)
            ),
            arrayOf(
                PointerCoords(10f, 11f),
                PointerCoords(20f, 21f),
                PointerCoords(30f, 31f)
            )
        )

        val pointerInputEventDown1 = motionEventAdapter.processMotionEvent(down1)
        val pointerInputEventDown2 = motionEventAdapter.processMotionEvent(down2)
        val pointerInputEventDown3 = motionEventAdapter.processMotionEvent(down3)

        assertThat(pointerInputEventDown1).isNotNull()
        assertThat(pointerInputEventDown2).isNotNull()
        assertThat(pointerInputEventDown3).isNotNull()

        assertThat(pointerInputEventDown1!!.pointers).hasSize(1)
        assertThat(pointerInputEventDown1.pointers[0].id).isEqualTo(PointerId(0))

        assertThat(pointerInputEventDown2!!.pointers).hasSize(2)
        assertThat(pointerInputEventDown2.pointers[0].id).isEqualTo(PointerId(0))
        assertThat(pointerInputEventDown2.pointers[1].id).isEqualTo(PointerId(1))

        assertThat(pointerInputEventDown3!!.pointers).hasSize(3)
        assertThat(pointerInputEventDown2.pointers[0].id).isEqualTo(PointerId(0))
        assertThat(pointerInputEventDown2.pointers[1].id).isEqualTo(PointerId(1))
        assertThat(pointerInputEventDown3.pointers[2].id).isEqualTo(PointerId(2))
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
        assertThat(pointerInputEvent).isNotNull()

        val (uptime, pointers) = pointerInputEvent!!
        assertThat(uptime.nanoseconds).isEqualTo(0L)
        assertThat(pointers).hasSize(1)
        assertPointerInputEventData(pointers[0], PointerId(0), true, 1f, 2f)
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

        val pointerInputEvent = motionEventAdapter.processMotionEvent(motionEvent)
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

        motionEventAdapter.processMotionEvent(motionEvent1)
        motionEventAdapter.processMotionEvent(motionEvent2)

        assertThat(motionEventAdapter.intIdToPointerIdMap).isEmpty()
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

        motionEventAdapter.processMotionEvent(motionEvent1)
        motionEventAdapter.processMotionEvent(motionEvent2)

        assertThat(motionEventAdapter.intIdToPointerIdMap).containsExactlyEntriesIn(
            mapOf(
                2 to PointerId(0),
                5 to PointerId(1)
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

        motionEventAdapter.processMotionEvent(motionEvent1)
        motionEventAdapter.processMotionEvent(motionEvent2)
        motionEventAdapter.processMotionEvent(motionEvent3)

        assertThat(motionEventAdapter.intIdToPointerIdMap).containsExactlyEntriesIn(
            mapOf(2 to PointerId(0))
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

        motionEventAdapter.processMotionEvent(motionEvent1)
        motionEventAdapter.processMotionEvent(motionEvent2)
        motionEventAdapter.processMotionEvent(motionEvent3)

        assertThat(motionEventAdapter.intIdToPointerIdMap).containsExactlyEntriesIn(
            mapOf(5 to PointerId(1))
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

        motionEventAdapter.processMotionEvent(motionEvent1)
        motionEventAdapter.processMotionEvent(motionEvent2)
        motionEventAdapter.processMotionEvent(motionEvent3)
        motionEventAdapter.processMotionEvent(motionEvent4)

        assertThat(motionEventAdapter.intIdToPointerIdMap).isEmpty()
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
        motionEventAdapter.processMotionEvent(motionEvent1)
        motionEventAdapter.processMotionEvent(motionEvent2)
        motionEventAdapter.processMotionEvent(motionEvent3)

        assertThat(motionEventAdapter.intIdToPointerIdMap).isEmpty()
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
