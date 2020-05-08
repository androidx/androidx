/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.node

import android.view.MotionEvent
import androidx.test.filters.SmallTest
import androidx.ui.core.PointerInputChange
import androidx.ui.testutils.down
import androidx.ui.testutils.moveTo
import androidx.ui.testutils.up
import androidx.ui.unit.milliseconds
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class PointerInteropUtilsTest {

    @Test(expected = IllegalStateException::class)
    fun toMotionEventScope_emptyList_throws() {
        val list = listOf<PointerInputChange>()
        list.toMotionEventScope {}
    }

    @Test
    fun toMotionEventScope_1stPointerDownEvent_motionEventIsCorrect() {
        val list = listOf(down(1, 2.milliseconds, 3f, 4f))
        val expected =
            MotionEvent(
                2,
                MotionEvent.ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(3f, 4f))
            )
        lateinit var actual: MotionEvent

        list.toMotionEventScope {
            actual = it
        }

        assertEquals(actual, expected)
    }

    @Test
    fun toMotionEventScope_1stPointerUpEvent_motionEventIsCorrect() {
        val list = listOf(down(1, 2.milliseconds, 3f, 4f).up(5.milliseconds))
        val expected =
            MotionEvent(
                5,
                MotionEvent.ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(3f, 4f))
            )
        lateinit var actual: MotionEvent

        list.toMotionEventScope {
            actual = it
        }

        assertEquals(actual, expected)
    }

    @Test
    fun toMotionEventScope_2ndPointerDownEventAs1stPointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2.milliseconds, 3f, 4f).moveTo(7.milliseconds, 3f, 4f)
        val pointer2 = down(8, 7.milliseconds, 10f, 11f)

        val list = listOf(pointer1, pointer2)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(PointerProperties(1), PointerProperties(8)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(10f, 11f))
            )
        lateinit var actual: MotionEvent

        list.toMotionEventScope {
            actual = it
        }

        assertEquals(actual, expected)
    }

    @Test
    fun toMotionEventScope_2ndPointerDownEventAs2ndPointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2.milliseconds, 3f, 4f).moveTo(7.milliseconds, 3f, 4f)
        val pointer2 = down(8, 7.milliseconds, 10f, 11f)

        val list = listOf(pointer2, pointer1)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                0,
                arrayOf(PointerProperties(8), PointerProperties(1)),
                arrayOf(PointerCoords(10f, 11f), PointerCoords(3f, 4f))
            )
        lateinit var actual: MotionEvent

        list.toMotionEventScope {
            actual = it
        }

        assertEquals(actual, expected)
    }

    @Test
    fun toMotionEventScope_2ndPointerUpEventAs1stPointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2.milliseconds, 3f, 4f).moveTo(7.milliseconds, 3f, 4f)
        val pointer2 = down(8, 2.milliseconds, 10f, 11f).up(7.milliseconds)

        val list = listOf(pointer1, pointer2)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_POINTER_UP,
                2,
                1,
                arrayOf(PointerProperties(1), PointerProperties(8)),
                arrayOf(PointerCoords(3f, 4f), PointerCoords(10f, 11f))
            )
        lateinit var actual: MotionEvent

        list.toMotionEventScope {
            actual = it
        }

        assertEquals(actual, expected)
    }

    @Test
    fun toMotionEventScope_2ndPointerUpEventAs2ndPointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2.milliseconds, 3f, 4f).moveTo(7.milliseconds, 3f, 4f)
        val pointer2 = down(8, 2.milliseconds, 10f, 11f).up(7.milliseconds)

        val list = listOf(pointer2, pointer1)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_POINTER_UP,
                2,
                0,
                arrayOf(PointerProperties(8), PointerProperties(1)),
                arrayOf(PointerCoords(10f, 11f), PointerCoords(3f, 4f))
            )
        lateinit var actual: MotionEvent

        list.toMotionEventScope {
            actual = it
        }

        assertEquals(actual, expected)
    }

    @Test
    fun toMotionEventScope_moveEvent1Pointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2.milliseconds, 3f, 4f).moveTo(7.milliseconds, 8f, 9f)

        val list = listOf(pointer1)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(8f, 9f))
            )
        lateinit var actual: MotionEvent

        list.toMotionEventScope {
            actual = it
        }

        assertEquals(actual, expected)
    }

    @Test
    fun toMotionEventScope_moveEvent2Pointers_motionEventIsCorrect() {
        val pointer1 = down(1, 2.milliseconds, 3f, 4f).moveTo(7.milliseconds, 8f, 9f)
        val pointer2 = down(11, 12.milliseconds, 13f, 14f).moveTo(17.milliseconds, 18f, 19f)

        val list = listOf(pointer1, pointer2)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_MOVE,
                2,
                0,
                arrayOf(PointerProperties(1), PointerProperties(11)),
                arrayOf(PointerCoords(8f, 9f), PointerCoords(18f, 19f))
            )
        lateinit var actual: MotionEvent

        list.toMotionEventScope {
            actual = it
        }

        assertEquals(actual, expected)
    }

    @Test(expected = IllegalStateException::class)
    fun toCancelMotionEventScope_emptyList_throws() {
        val list = listOf<PointerInputChange>()
        list.toCancelMotionEventScope {}
    }

    @Test
    fun toCancelMotionEventScope_1Pointer_motionEventIsCorrect() {
        val pointer1 = down(1, 2.milliseconds, 3f, 4f).moveTo(7.milliseconds, 8f, 9f)

        val list = listOf(pointer1)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_CANCEL,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(8f, 9f))
            )
        lateinit var actual: MotionEvent

        list.toCancelMotionEventScope {
            actual = it
        }

        assertEquals(actual, expected)
    }

    @Test
    fun toCancelMotionEventScope_2Pointers_motionEventIsCorrect() {
        val pointer1 = down(1, 2.milliseconds, 3f, 4f).moveTo(7.milliseconds, 8f, 9f)
        val pointer2 = down(11, 12.milliseconds, 13f, 14f).moveTo(17.milliseconds, 18f, 19f)

        val list = listOf(pointer1, pointer2)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_CANCEL,
                2,
                0,
                arrayOf(PointerProperties(1), PointerProperties(11)),
                arrayOf(PointerCoords(8f, 9f), PointerCoords(18f, 19f))
            )
        lateinit var actual: MotionEvent

        list.toCancelMotionEventScope {
            actual = it
        }

        assertEquals(actual, expected)
    }

    @Test
    fun toCancelMotionEventScope_2PointersAltOrder_motionEventIsCorrect() {
        val pointer1 = down(1, 2.milliseconds, 3f, 4f).moveTo(7.milliseconds, 8f, 9f)
        val pointer2 = down(11, 12.milliseconds, 13f, 14f).moveTo(7.milliseconds, 18f, 19f)

        val list = listOf(pointer2, pointer1)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_CANCEL,
                2,
                0,
                arrayOf(PointerProperties(11), PointerProperties(1)),
                arrayOf(PointerCoords(18f, 19f), PointerCoords(8f, 9f))
            )
        lateinit var actual: MotionEvent

        list.toCancelMotionEventScope {
            actual = it
        }

        assertEquals(actual, expected)
    }

    @Test
    fun emptyCancelMotionEventScope_motionEventIsCorrect() {
        val expected =
            MotionEvent(
                76,
                MotionEvent.ACTION_CANCEL,
                1,
                0,
                arrayOf(PointerProperties(0)),
                arrayOf(PointerCoords(0f, 0f)),
                76
            )
        lateinit var actual: MotionEvent

        emptyCancelMotionEventScope(76.milliseconds) {
            actual = it
        }

        assertEquals(actual, expected)
    }
}

private fun PointerProperties(id: Int = 0, toolType: Int = MotionEvent.TOOL_TYPE_UNKNOWN) =
    MotionEvent.PointerProperties().apply {
        this.id = id
        this.toolType = toolType
    }

private fun PointerCoords(x: Float = 0f, y: Float = 0f) =
    MotionEvent.PointerCoords().apply {
        this.x = x
        this.y = y
    }

private fun MotionEvent(
    eventTime: Long,
    action: Int,
    numPointers: Int,
    actionIndex: Int,
    pointerProperties: Array<MotionEvent.PointerProperties>,
    pointerCoords: Array<MotionEvent.PointerCoords>,
    downtime: Long = 0L
) = MotionEvent.obtain(
    downtime,
    eventTime,
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

private fun assertEquals(actual: MotionEvent, expected: MotionEvent) {
    Truth.assertThat(actual.downTime).isEqualTo(expected.downTime)
    Truth.assertThat(actual.eventTime).isEqualTo(expected.eventTime)
    Truth.assertThat(actual.actionMasked).isEqualTo(expected.actionMasked)
    Truth.assertThat(actual.actionIndex).isEqualTo(expected.actionIndex)
    Truth.assertThat(actual.pointerCount).isEqualTo(expected.pointerCount)

    val actualPointerProperties = MotionEvent.PointerProperties()
    val expectedPointerProperties = MotionEvent.PointerProperties()
    repeat(expected.pointerCount) { index ->
        actual.getPointerProperties(index, actualPointerProperties)
        expected.getPointerProperties(index, expectedPointerProperties)
        Truth.assertThat(actualPointerProperties).isEqualTo(expectedPointerProperties)
    }

    val actualPointerCoords = MotionEvent.PointerCoords()
    val expectedPointerCoords = MotionEvent.PointerCoords()
    repeat(expected.pointerCount) { index ->
        actual.getPointerCoords(index, actualPointerCoords)
        expected.getPointerCoords(index, expectedPointerCoords)
        Truth.assertThat(actualPointerCoords.x).isEqualTo(expectedPointerCoords.x)
        Truth.assertThat(actualPointerCoords.y).isEqualTo(expectedPointerCoords.y)
    }
}
