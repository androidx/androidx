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

import android.content.Context
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.TOOL_TYPE_UNKNOWN
import androidx.activity.ComponentActivity
import androidx.test.filters.SmallTest
import androidx.ui.core.PointerEventPass
import androidx.ui.core.consumeAllChanges
import androidx.ui.core.consumeDownChange
import androidx.ui.test.android.AndroidComposeTestRule
import androidx.ui.testutils.consume
import androidx.ui.testutils.down
import androidx.ui.testutils.invokeOverAllPasses
import androidx.ui.testutils.invokeOverPasses
import androidx.ui.testutils.moveBy
import androidx.ui.testutils.moveTo
import androidx.ui.testutils.up
import androidx.ui.unit.milliseconds
import androidx.ui.viewinterop.AndroidViewHolder
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class PointerInteropFilterTest {
    @get:Rule
    val composeTestRule = AndroidComposeTestRule<ComponentActivity>()

    private lateinit var mockViewGroup: MockViewGroup
    private lateinit var pointerInteropFilter: PointerInteropFilter

    @Before
    fun setup() {
        mockViewGroup = MockViewGroup(composeTestRule.activityTestRule.activity)
        pointerInteropFilter = PointerInteropFilter(mockViewGroup)
    }

    // Verification of correct MotionEvents being dispatched (when no events are cancel)

    @Test
    fun onPointerInput_1PointerDown_correctMotionEventDispatched() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val expected =
            MotionEvent(
                2,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(3f, 4f))
            )

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
        assertEquals(mockViewGroup.dispatchedMotionEvents[0], expected)
    }

    @Test
    fun onPointerInput_1PointerUp_correctMotionEventDispatched() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val up = down.up(5.milliseconds)
        val expected =
            MotionEvent(
                5,
                MotionEvent.ACTION_UP,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(3f, 4f))
            )
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(up)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)
        assertEquals(mockViewGroup.dispatchedMotionEvents[1], expected)
    }

    @Test
    fun onPointerInput_2PointersDown_correctMotionEventDispatched() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                1,
                arrayOf(
                    PointerProperties(1),
                    PointerProperties(8)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(10f, 11f)
                )
            )

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove, bDown)

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)
        assertEquals(mockViewGroup.dispatchedMotionEvents[1], expected)
    }

    @Test
    fun onPointerInput_2PointersDownAllPassesAltOrder_correctMotionEventDispatched() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_POINTER_DOWN,
                2,
                0,
                arrayOf(
                    PointerProperties(8),
                    PointerProperties(1)
                ),
                arrayOf(
                    PointerCoords(10f, 11f),
                    PointerCoords(3f, 4f)
                )
            )
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(bDown, aMove)

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)
        assertEquals(mockViewGroup.dispatchedMotionEvents[1], expected)
    }

    @Test
    fun onPointerInput_2Pointers1Up_correctMotionEventDispatched() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(12.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        val aMove2 = aMove1.moveTo(13.milliseconds, 3f, 4f)
        val bUp = bDown.up(13.milliseconds)

        val expected =
            MotionEvent(
                13,
                MotionEvent.ACTION_POINTER_UP,
                2,
                1,
                arrayOf(
                    PointerProperties(1),
                    PointerProperties(8)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(10f, 11f)
                )
            )

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove2, bUp)

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(3)
        assertEquals(mockViewGroup.dispatchedMotionEvents[2], expected)
    }

    @Test
    fun onPointerInput_2Pointers1UpAllPassesAltOrder_correctMotionEventDispatched() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(12.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        val aMove2 = aMove1.moveTo(13.milliseconds, 3f, 4f)
        val bUp = bDown.up(13.milliseconds)

        val expected =
            MotionEvent(
                13,
                MotionEvent.ACTION_POINTER_UP,
                2,
                0,
                arrayOf(
                    PointerProperties(8),
                    PointerProperties(1)
                ),
                arrayOf(
                    PointerCoords(10f, 11f),
                    PointerCoords(3f, 4f)
                )
            )

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(bUp, aMove2)

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(3)
        assertEquals(mockViewGroup.dispatchedMotionEvents[2], expected)
    }

    @Test
    fun onPointerInput_1PointerMove_correctMotionEventDispatched() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val move = down.moveTo(7.milliseconds, 8f, 9f)
        val expected =
            MotionEvent(
                7,
                MotionEvent.ACTION_MOVE,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(8f, 9f))
            )
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(move)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)
        assertEquals(mockViewGroup.dispatchedMotionEvents[1], expected)
    }

    @Test
    fun onPointerInput_2PointersMove_correctMotionEventDispatched() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDown = down(11, 7.milliseconds, 13f, 14f)

        val aMove2 = aMove1.moveTo(15.milliseconds, 8f, 9f)
        val bMove1 = bDown.moveTo(15.milliseconds, 18f, 19f)

        val expected =
            MotionEvent(
                15,
                MotionEvent.ACTION_MOVE,
                2,
                0,
                arrayOf(
                    PointerProperties(1),
                    PointerProperties(11)
                ),
                arrayOf(
                    PointerCoords(8f, 9f),
                    PointerCoords(18f, 19f)
                )
            )

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove2, bMove1)

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(3)
        assertEquals(mockViewGroup.dispatchedMotionEvents[2], expected)
    }

    @Test
    fun onPointerInput_2PointersMoveAltOrder_correctMotionEventDispatched() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDown = down(11, 7.milliseconds, 13f, 14f)

        val aMove2 = aMove1.moveTo(15.milliseconds, 8f, 9f)
        val bMove1 = bDown.moveTo(15.milliseconds, 18f, 19f)

        val expected =
            MotionEvent(
                15,
                MotionEvent.ACTION_MOVE,
                2,
                0,
                arrayOf(
                    PointerProperties(11),
                    PointerProperties(1)
                ),
                arrayOf(
                    PointerCoords(18f, 19f),
                    PointerCoords(8f, 9f)
                )
            )

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(bMove1, aMove2)

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(3)
        assertEquals(mockViewGroup.dispatchedMotionEvents[2], expected)
    }

    // Verification of correct cancel events being dispatched

    @Test
    fun onPointerInput_1PointerUpConsumed_correctCancelDispatched() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val upConsumed = down.up(5.milliseconds).consumeDownChange()
        val expected =
            MotionEvent(
                5,
                ACTION_CANCEL,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(3f, 4f))
            )
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(upConsumed)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)
        assertEquals(mockViewGroup.dispatchedMotionEvents[1], expected)
    }

    @Test
    fun onPointerInput_2PointersDown2ndDownConsumed_correctCancelDispatched() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDownConsumed = down(8, 7.milliseconds, 10f, 11f).consumeDownChange()

        val expected =
            MotionEvent(
                7,
                ACTION_CANCEL,
                2,
                0,
                arrayOf(
                    PointerProperties(1),
                    PointerProperties(8)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(10f, 11f)
                )
            )

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aMove, bDownConsumed)

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)
        assertEquals(mockViewGroup.dispatchedMotionEvents[1], expected)
    }

    @Test
    fun onPointerInput_2Pointers1UpConsumed_correctCancelDispatched() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(12.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        val aMove2 = aMove1.moveTo(13.milliseconds, 3f, 4f)
        val bUpConsumed = bDown.up(13.milliseconds).consumeDownChange()

        val expected =
            MotionEvent(
                13,
                ACTION_CANCEL,
                2,
                0,
                arrayOf(
                    PointerProperties(1),
                    PointerProperties(8)
                ),
                arrayOf(
                    PointerCoords(3f, 4f),
                    PointerCoords(10f, 11f)
                )
            )

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aMove2, bUpConsumed)

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(3)
        assertEquals(mockViewGroup.dispatchedMotionEvents[2], expected)
    }

    @Test
    fun onPointerInput_1PointerMoveConsumed_correctCancelDispatched() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val moveConsumed = down.moveTo(7.milliseconds, 8f, 9f).consume(1f, 0f)
        val expected =
            MotionEvent(
                7,
                ACTION_CANCEL,
                1,
                0,
                arrayOf(PointerProperties(1)),
                arrayOf(PointerCoords(8f, 9f))
            )
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(moveConsumed)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)
        assertEquals(mockViewGroup.dispatchedMotionEvents[1], expected)
    }

    @Test
    fun onPointerInput_2PointersMoveConsumed_correctCancelDispatched() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDown = down(11, 7.milliseconds, 13f, 14f)

        val aMove2 = aMove1.moveTo(15.milliseconds, 8f, 9f)
        val bMoveConsumed = bDown.moveTo(15.milliseconds, 18f, 19f).consume(1f, 0f)

        val expected =
            MotionEvent(
                15,
                ACTION_CANCEL,
                2,
                0,
                arrayOf(
                    PointerProperties(1),
                    PointerProperties(11)
                ),
                arrayOf(
                    PointerCoords(8f, 9f),
                    PointerCoords(18f, 19f)
                )
            )

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aMove2, bMoveConsumed)

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(3)
        assertEquals(mockViewGroup.dispatchedMotionEvents[2], expected)
    }

    // Verification of no longer dispatching to children once we have consumed events

    @Test
    fun onPointerInput_downConsumed_nothingDispatched() {
        val downConsumed = down(1, 2.milliseconds, 3f, 4f).consumeDownChange()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(downConsumed)
        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onPointerInput_downConsumedThenMoveThenUp_nothingDispatched() {
        val aDownConsumed = down(1, 2.milliseconds, 3f, 4f).consumeDownChange()
        val aMove = aDownConsumed.moveTo(5.milliseconds, 6f, 7f)
        val aUp = aMove.up(5.milliseconds)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDownConsumed)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aUp)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onPointerInput_down1ConsumedThenDown2ThenMove2ThenUp2_nothingDispatched() {
        val aDownConsumed = down(1, 2.milliseconds, 3f, 4f).consumeDownChange()
        val aMove1 = aDownConsumed.moveTo(5.milliseconds, 6f, 7f)
        val bDown = down(11, 5.milliseconds, 13f, 14f)
        val aMove2 = aDownConsumed.moveTo(21.milliseconds, 6f, 7f)
        val bMove = bDown.moveTo(21.milliseconds, 22f, 23f)
        val aMove3 = aDownConsumed.moveTo(31.milliseconds, 6f, 7f)
        val bUp = bMove.up(31.milliseconds)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDownConsumed)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove2, bMove)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove3, bUp)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onPointerInput_down1ConsumedThenDown2ThenUp1ThenDown3_nothingDispatched() {
        val aDownConsumed = down(1, 2.milliseconds, 3f, 4f).consumeDownChange()

        val aMove1 = aDownConsumed.moveTo(11.milliseconds, 3f, 4f)
        val bDown = down(21, 22.milliseconds, 23f, 24f)

        val aUp = aMove1.up(31.milliseconds)
        val bMove1 = bDown.moveTo(31.milliseconds, 23f, 24f)

        val bMove2 = bMove1.moveTo(41.milliseconds, 23f, 24f)
        val cDown = down(51, 41.milliseconds, 52f, 53f)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDownConsumed)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aUp, bMove1)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(bMove2, cDown)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onPointerInput_downThenMoveConsumedThenMoveThenUp_afterConsumeNoDispatch() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val move1Consumed = down.moveTo(5.milliseconds, 6f, 7f).consume(0f, 1f)
        val move2 = move1Consumed.moveTo(10.milliseconds, 11f, 12f)
        val up = move2.up(10.milliseconds)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(move1Consumed)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(move2)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(up)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onPointerInput_down1ThenDown2ConsumedThenMoveThenUp1ThenUp2_afterConsumeNoDispatch() {
        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(11.milliseconds, 3f, 4f)
        val bDownConsumed = down(21, 11.milliseconds, 23f, 24f).consumeDownChange()

        val aMove2 = aMove1.moveTo(31.milliseconds, 31f, 32f)
        val bMove = bDownConsumed.moveTo(31.milliseconds, 33f, 34f)

        val aMove3 = aMove2.moveTo(41.milliseconds, 42f, 43f)
        val bUp = bMove.up(41.milliseconds)

        val aUp = aMove3.up(51.milliseconds)

        // Act
        val pointerInputHandler = pointerInteropFilter.pointerInputFilter::onPointerInput
        pointerInputHandler.invokeOverAllPasses(aDown)
        pointerInputHandler.invokeOverAllPasses(aMove1, bDownConsumed)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInputHandler.invokeOverAllPasses(aMove2, bMove)
        pointerInputHandler.invokeOverAllPasses(aMove3, bUp)
        pointerInputHandler.invokeOverAllPasses(aUp)

        // Assert
        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onPointerInput_down1ConsumedThenUp1ThenDown2_finalDownDispatched() {
        val aDownConsumed = down(1, 2.milliseconds, 3f, 4f).consumeDownChange()
        val aUp = aDownConsumed.up(5.milliseconds)
        val bDown = down(11, 12.milliseconds, 13f, 14f)
        val expected =
            MotionEvent(
                12,
                ACTION_DOWN,
                1,
                0,
                arrayOf(
                    PointerProperties(11)
                ),
                arrayOf(
                    PointerCoords(13f, 14f)
                )
            )

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDownConsumed)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aUp)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(bDown)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
        assertEquals(mockViewGroup.dispatchedMotionEvents[0], expected)
    }

    @Test
    fun onPointerInput_down1ConsumedThenDown2ThenUp1ThenUp2ThenDown3_finalDownDispatched() {

        // Arrange

        val aDownConsumed = down(1, 2.milliseconds, 3f, 4f).consumeDownChange()

        val aMove1 = aDownConsumed.moveTo(22.milliseconds, 3f, 4f)
        val bDown = down(21, 22.milliseconds, 23f, 24f)

        val aUp = aMove1.up(31.milliseconds)
        val bMove1 = bDown.moveTo(31.milliseconds, 23f, 24f)

        val bUp = bMove1.up(41.milliseconds)

        val cDown = down(51, 52.milliseconds, 53f, 54f)

        val expected =
            MotionEvent(
                52,
                ACTION_DOWN,
                1,
                0,
                arrayOf(
                    PointerProperties(51)
                ),
                arrayOf(
                    PointerCoords(53f, 54f)
                )
            )

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDownConsumed)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aUp, bMove1)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(bUp)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(cDown)

        // Assert
        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
        assertEquals(mockViewGroup.dispatchedMotionEvents[0], expected)
    }

    // Verification no longer dispatching to children due to the child returning false for
    // dispatchTouchEvent(...)

    @Test
    fun onPointerInput_downViewRetsFalseThenMoveThenUp_noDispatchAfterRetFalse() {
        val aDown = down(1, 2.milliseconds, 3f, 4f)
        val aMove = aDown.moveTo(5.milliseconds, 6f, 7f)
        val aUp = aMove.up(5.milliseconds)
        mockViewGroup.returnValue = false

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aUp)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onPointerInput_down1ViewRetsFalseThenDown2ThenMove2ThenUp2_noDispatchAfterRetFalse() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(5.milliseconds, 6f, 7f)
        val bDown = down(11, 5.milliseconds, 13f, 14f)

        val aMove2 = aDown.moveTo(21.milliseconds, 6f, 7f)
        val bMove = bDown.moveTo(21.milliseconds, 22f, 23f)

        val aMove3 = aDown.moveTo(31.milliseconds, 6f, 7f)
        val bUp = bMove.up(31.milliseconds)

        mockViewGroup.returnValue = false

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove2, bMove)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove3, bUp)

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onPointerInput_down1ViewRetsFalseThenDown2ThenUp1ThenDown3_noDispatchAfterRetFalse() {
        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(11.milliseconds, 3f, 4f)
        val bDown = down(21, 22.milliseconds, 23f, 24f)

        val aUp = aMove1.up(31.milliseconds)
        val bMove1 = bDown.moveTo(31.milliseconds, 23f, 24f)

        val bMove2 = bMove1.moveTo(41.milliseconds, 23f, 24f)
        val cDown = down(51, 41.milliseconds, 52f, 53f)

        mockViewGroup.returnValue = false

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aUp, bMove1)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(bMove2, cDown)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onPointerInput_downThenMoveViewRetsFalseThenMoveThenUp_noDispatchAfterRetFalse() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val move1 = down.moveTo(5.milliseconds, 6f, 7f)
        val move2 = move1.moveTo(10.milliseconds, 11f, 12f)
        val up = move2.up(10.milliseconds)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)
        mockViewGroup.returnValue = false
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(move1)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(move2)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(up)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onPointerInput_down1ThenDown2ViewRetsFalseThenMoveThenUp1ThenUp2_noDispatchAfterRetFalse() {
        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(11.milliseconds, 3f, 4f)
        val bDown = down(21, 11.milliseconds, 23f, 24f)

        val aMove2 = aMove1.moveTo(31.milliseconds, 31f, 32f)
        val bMove = bDown.moveTo(31.milliseconds, 33f, 34f)

        val aMove3 = aMove2.moveTo(41.milliseconds, 42f, 43f)
        val bUp = bMove.up(41.milliseconds)

        val aUp = aMove3.up(51.milliseconds)

        // Act
        val pointerInputHandler = pointerInteropFilter.pointerInputFilter::onPointerInput
        pointerInputHandler.invokeOverAllPasses(aDown)
        mockViewGroup.returnValue = false
        pointerInputHandler.invokeOverAllPasses(aMove1, bDown)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInputHandler.invokeOverAllPasses(aMove2, bMove)
        pointerInputHandler.invokeOverAllPasses(aMove3, bUp)
        pointerInputHandler.invokeOverAllPasses(aUp)

        // Assert
        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onPointerInput_down1ViewRetsFalseThenUp1ThenDown2_finalDownDispatched() {
        val aDown = down(1, 2.milliseconds, 3f, 4f)
        val aUp = aDown.up(5.milliseconds)
        val bDown = down(11, 12.milliseconds, 13f, 14f)
        mockViewGroup.returnValue = false
        val expected =
            MotionEvent(
                12,
                ACTION_DOWN,
                1,
                0,
                arrayOf(
                    PointerProperties(11)
                ),
                arrayOf(
                    PointerCoords(13f, 14f)
                )
            )

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aUp)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(bDown)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
        assertEquals(mockViewGroup.dispatchedMotionEvents[0], expected)
    }

    @Test
    fun onPointerInput_down1ViewRetsFalseThenDown2ThenUp1ThenUp2ThenDown3_finalDownDispatched() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(11.milliseconds, 3f, 4f)
        val bDown = down(21, 11.milliseconds, 23f, 24f)

        val aUp = aMove1.up(31.milliseconds)
        val bMove1 = bDown.moveTo(31.milliseconds, 23f, 24f)

        val bUp = bMove1.up(41.milliseconds)

        val cDown = down(51, 52.milliseconds, 53f, 54f)

        mockViewGroup.returnValue = false

        val expected =
            MotionEvent(
                52,
                ACTION_DOWN,
                1,
                0,
                arrayOf(
                    PointerProperties(51)
                ),
                arrayOf(
                    PointerCoords(53f, 54f)
                )
            )

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aUp, bMove1)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(bUp)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(cDown)

        // Assert
        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
        assertEquals(mockViewGroup.dispatchedMotionEvents[0], expected)
    }

    // Verification of correct consumption due to the return value of View.dispatchTouchEvent(...).
    // If a view returns false, nothing should be consumed.  If it returns true, everything that can
    // be consumed should be consumed.

    @Test
    fun onPointerInput_1PointerDownViewRetsFalse_nothingConsumed() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        mockViewGroup.returnValue = false

        val actual =
            pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        assertThat(actual).isEqualTo(down)
    }

    @Test
    fun onPointerInput_1PointerDownViewRetsTrue_everythingConsumed() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        mockViewGroup.returnValue = true
        val expected = down.consumeAllChanges()

        val actual =
            pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_1PointerUpViewRetsFalse_nothingConsumed() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val up = down.up(5.milliseconds)
        mockViewGroup.returnValue = true
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        mockViewGroup.returnValue = false
        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(up)

        assertThat(actual).isEqualTo(up)
    }

    @Test
    fun onPointerInput_1PointerUpViewRetsTrue_everythingConsumed() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val up = down.up(5.milliseconds)
        val expected = up.consumeAllChanges()
        mockViewGroup.returnValue = true
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(up)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_2PointersDownViewRetsFalse_nothingConsumed() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        mockViewGroup.returnValue = true

        val expected = listOf(aMove, bDown)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)

        // Act

        mockViewGroup.returnValue = false
        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aMove, bDown)

        // Assert

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_2PointersDownViewRetsTrue_everythingConsumed() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        mockViewGroup.returnValue = true

        val expected = listOf(aMove.consumeAllChanges(), bDown.consumeAllChanges())

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)

        // Act

        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aMove, bDown)

        // Assert

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_2Pointers1UpViewRetsFalse_nothingConsumed() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(12.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        val aMove2 = aMove1.moveTo(13.milliseconds, 3f, 4f)
        val bUp = bDown.up(13.milliseconds)

        mockViewGroup.returnValue = true

        val expected = listOf(aMove2, bUp)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        mockViewGroup.returnValue = false
        val actual =
            pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove2, bUp)

        // Assert

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_2Pointers1UpViewRetsTrue_everythingConsumed() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(12.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        val aMove2 = aMove1.moveTo(13.milliseconds, 3f, 4f)
        val bUp = bDown.up(13.milliseconds)

        mockViewGroup.returnValue = true

        val expected = listOf(aMove2.consumeAllChanges(), bUp.consumeAllChanges())

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        val actual =
            pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove2, bUp)

        // Assert

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_1PointerMoveViewRetsFalse_nothingConsumed() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val move = down.moveTo(7.milliseconds, 8f, 9f)
        mockViewGroup.returnValue = true
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        mockViewGroup.returnValue = false
        val actual =
            pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(move)

        assertThat(actual).isEqualTo(move)
    }

    @Test
    fun onPointerInput_1PointerMoveViewRetsTrue_everythingConsumed() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val move = down.moveBy(7.milliseconds, 8f, 9f)
        mockViewGroup.returnValue = true
        val expected = move.consumeAllChanges()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        val actual =
            pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(move)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_2PointersMoveViewRetsFalse_nothingConsumed() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDown = down(11, 7.milliseconds, 13f, 14f)

        val aMove2 = aMove1.moveBy(15.milliseconds, 8f, 9f)
        val bMove1 = bDown.moveBy(15.milliseconds, 18f, 19f)

        mockViewGroup.returnValue = true

        val expected = listOf(aMove2, bMove1)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        mockViewGroup.returnValue = false
        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aMove2, bMove1)

        // Assert

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_2PointersMoveViewRetsTrue_everythingConsumed() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDown = down(11, 7.milliseconds, 13f, 14f)

        val aMove2 = aMove1.moveBy(15.milliseconds, 8f, 9f)
        val bMove1 = bDown.moveBy(15.milliseconds, 18f, 19f)

        mockViewGroup.returnValue = true

        val expected = listOf(aMove2.consumeAllChanges(), bMove1.consumeAllChanges())

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aMove2, bMove1)

        // Assert

        assertThat(actual).isEqualTo(expected)
    }

    // Verification of no further consumption after initial consumption (because if something was
    // consumed, we should prevent view from getting dispatched to and thus nothing additional
    // should be consumed).

    @Test
    fun onPointerInput_downConsumedThenMove_noAdditionalConsumption() {
        val aDownConsumed = down(1, 2.milliseconds, 3f, 4f).consumeDownChange()
        val aMove = aDownConsumed.moveTo(5.milliseconds, 6f, 7f)
        mockViewGroup.returnValue = true

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDownConsumed)
        val actual =
            pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove)

        assertThat(actual).isEqualTo(aMove)
    }

    @Test
    fun onPointerInput_downConsumedThenUp_noAdditionalConsumption() {
        val aDownConsumed = down(1, 2.milliseconds, 3f, 4f).consumeDownChange()
        val aUp = aDownConsumed.up(5.milliseconds)
        mockViewGroup.returnValue = true

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDownConsumed)
        val actual =
            pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aUp)

        assertThat(actual).isEqualTo(aUp)
    }

    @Test
    fun onPointerInput_down1ConsumedThenDown2_noAdditionalConsumption() {
        val aDownConsumed = down(1, 2.milliseconds, 3f, 4f).consumeDownChange()
        val aMove1 = aDownConsumed.moveTo(5.milliseconds, 6f, 7f)
        val bDown = down(11, 5.milliseconds, 13f, 14f)
        val expected = listOf(aMove1, bDown)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDownConsumed)
        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aMove1, bDown)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_down1ConsumedThenDown2ThenMove_noAdditionalConsumption() {
        val aDownConsumed = down(1, 2.milliseconds, 3f, 4f).consumeDownChange()
        val aMove1 = aDownConsumed.moveTo(5.milliseconds, 6f, 7f)
        val bDown = down(11, 5.milliseconds, 13f, 14f)
        val aMove2 = aDownConsumed.moveTo(21.milliseconds, 6f, 7f)
        val bMove = bDown.moveTo(21.milliseconds, 22f, 23f)
        val expected = listOf(aMove2, bMove)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDownConsumed)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)
        val actual =
            pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(
                aMove2,
                bMove
            )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_2Pointers1MoveConsumed_noAdditionalConsumption() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDown = down(11, 7.milliseconds, 13f, 14f)

        val aMove2 = aMove1.moveTo(15.milliseconds, 8f, 9f)
        val bMoveConsumed = bDown.moveTo(15.milliseconds, 18f, 19f).consume(1f, 0f)

        val expected = listOf(aMove2, bMoveConsumed)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aMove2, bMoveConsumed)

        // Assert

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_down1ThenDown2ConsumedThenMove_noAdditionalConsumption() {
        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(11.milliseconds, 3f, 4f)
        val bDownConsumed = down(21, 11.milliseconds, 23f, 24f).consumeDownChange()

        val aMove2 = aMove1.moveTo(31.milliseconds, 31f, 32f)
        val bMove = bDownConsumed.moveTo(31.milliseconds, 33f, 34f)

        val expected = listOf(aMove2, bMove)

        // Act
        val pointerInputHandler = pointerInteropFilter.pointerInputFilter::onPointerInput
        pointerInputHandler.invokeOverAllPasses(aDown)
        pointerInputHandler.invokeOverAllPasses(aMove1, bDownConsumed)
        val actual = pointerInputHandler.invokeOverAllPasses(aMove2, bMove)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    // Verifies resetting of consumption.

    @Test
    fun onPointerInput_down1ConsumedThenUp1ThenDown2_finalDownConsumed() {
        val aDownConsumed = down(1, 2.milliseconds, 3f, 4f).consumeDownChange()
        val aUp = aDownConsumed.up(5.milliseconds)
        val bDown = down(11, 12.milliseconds, 13f, 14f)
        val expected = bDown.consumeAllChanges()

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDownConsumed)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aUp)
        val actual =
            pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(bDown)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_down1ConsumedThenDown2ThenUp1ThenUp2ThenDown3_finalDownConsumed() {

        // Arrange

        val aDownConsumed = down(1, 2.milliseconds, 3f, 4f).consumeDownChange()

        val aMove1 = aDownConsumed.moveTo(22.milliseconds, 3f, 4f)
        val bDown = down(21, 22.milliseconds, 23f, 24f)

        val aUp = aMove1.up(31.milliseconds)
        val bMove1 = bDown.moveTo(31.milliseconds, 23f, 24f)

        val bUp = bMove1.up(41.milliseconds)

        val cDown = down(51, 52.milliseconds, 53f, 54f)

        val expected = cDown.consumeAllChanges()

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDownConsumed)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aUp, bMove1)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(bUp)
        val actual =
            pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(cDown)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    // Verification of consumption when the view rets false and then is set to return true.

    @Test
    fun onPointerInput_viewRetsFalseDownThenViewRetsTrueMove_noConsumptionOfMove() {
        val aDown = down(1, 2.milliseconds, 3f, 4f)
        val aMove = aDown.moveTo(5.milliseconds, 6f, 7f)
        mockViewGroup.returnValue = false

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        mockViewGroup.returnValue = true
        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aMove)

        assertThat(actual).isEqualTo(aMove)
    }

    @Test
    fun onPointerInput_viewRetsFalseDownThenViewRetsTrueUp_noConsumptionOfUp() {
        val aDown = down(1, 2.milliseconds, 3f, 4f)
        val aUp = aDown.up(5.milliseconds)
        mockViewGroup.returnValue = false

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        mockViewGroup.returnValue = true
        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aUp)

        assertThat(actual).isEqualTo(aUp)
    }

    @Test
    fun onPointerInput_viewRestsFalseDown1ThenViewRetsTrueDown2_noConsumptionOfDown2() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(5.milliseconds, 6f, 7f)
        val bDown = down(11, 5.milliseconds, 13f, 14f)

        mockViewGroup.returnValue = false

        val expected = listOf(aMove1, bDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        mockViewGroup.returnValue = true
        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aMove1, bDown)

        // Assert

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_viewRestsFalseDown1ThenViewRetsTrueDown2TheMove_noConsumptionOfMove() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(5.milliseconds, 6f, 7f)
        val bDown = down(11, 5.milliseconds, 13f, 14f)

        val aMove2 = aMove1.moveTo(21.milliseconds, 22f, 23f)
        val bMove1 = bDown.moveBy(21.milliseconds, 24f, 25f)

        mockViewGroup.returnValue = false

        val expected = listOf(aMove2, bMove1)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)
        mockViewGroup.returnValue = true
        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aMove2, bMove1)

        // Assert

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_viewRestsFalseDown1ThenViewRetsTrueDown2TheUp2_noConsumptionOfUp() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(5.milliseconds, 6f, 7f)
        val bDown = down(11, 5.milliseconds, 13f, 14f)

        val aMove2 = aMove1.moveTo(21.milliseconds, 6f, 7f)
        val bUp = bDown.up(21.milliseconds)

        mockViewGroup.returnValue = false

        val expected = listOf(aMove2, bUp)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)
        mockViewGroup.returnValue = true
        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(aMove2, bUp)

        // Assert

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_down1ViewRetsFalseThenViewRestsTrueDown2ThenUp1ThenDown3_down3NotConsumed() {
        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(11.milliseconds, 3f, 4f)
        val bDown = down(21, 22.milliseconds, 23f, 24f)

        val aUp = aMove1.up(31.milliseconds)
        val bMove1 = bDown.moveTo(31.milliseconds, 23f, 24f)

        val bMove2 = bMove1.moveTo(41.milliseconds, 23f, 24f)
        val cDown = down(51, 41.milliseconds, 52f, 53f)

        mockViewGroup.returnValue = false

        val expected = listOf(bMove2, cDown)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aUp, bMove1)
        mockViewGroup.returnValue = true
        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(bMove2, cDown)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_downThenMoveViewRetsFalseThenViewRetsTrueMove_moveNotConsumed() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val move1 = down.moveTo(5.milliseconds, 6f, 7f)
        val move2 = move1.moveTo(10.milliseconds, 11f, 12f)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)
        mockViewGroup.returnValue = false
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(move1)
        mockViewGroup.returnValue = true
        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(move2)

        assertThat(actual).isEqualTo(move2)
    }

    @Test
    fun onPointerInput_downThenMoveViewRetsFalseThenViewRetsTrueThenUp_UpNotConsumed() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val move1 = down.moveTo(5.milliseconds, 6f, 7f)
        val up = move1.up(10.milliseconds)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)
        mockViewGroup.returnValue = false
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(move1)
        mockViewGroup.returnValue = true
        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverAllPasses(up)

        assertThat(actual).isEqualTo(up)
    }

    @Test
    fun onPointerInput_down1ThenDown2ViewRetsFalseThenViewRetsTrueMove_moveNotConsumed() {
        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(11.milliseconds, 3f, 4f)
        val bDown = down(21, 11.milliseconds, 23f, 24f)

        val aMove2 = aMove1.moveTo(31.milliseconds, 31f, 32f)
        val bMove = bDown.moveTo(31.milliseconds, 33f, 34f)

        val expected = listOf(aMove2, bMove)

        // Act
        val pointerInputHandler = pointerInteropFilter.pointerInputFilter::onPointerInput
        pointerInputHandler.invokeOverAllPasses(aDown)
        mockViewGroup.returnValue = false
        pointerInputHandler.invokeOverAllPasses(aMove1, bDown)
        mockViewGroup.returnValue = true
        val actual = pointerInputHandler.invokeOverAllPasses(aMove2, bMove)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_down1ThenDown2ViewRetsFalseThenViewRetsTrueUp2_moveNotConsumed() {
        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(11.milliseconds, 3f, 4f)
        val bDown = down(21, 11.milliseconds, 23f, 24f)

        val aMove2 = aMove1.moveTo(31.milliseconds, 31f, 32f)
        val bUp = bDown.up(31.milliseconds)

        val expected = listOf(aMove2, bUp)

        // Act
        val pointerInputHandler = pointerInteropFilter.pointerInputFilter::onPointerInput
        pointerInputHandler.invokeOverAllPasses(aDown)
        mockViewGroup.returnValue = false
        pointerInputHandler.invokeOverAllPasses(aMove1, bDown)
        mockViewGroup.returnValue = true
        val actual = pointerInputHandler.invokeOverAllPasses(aMove2, bUp)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    // Verification of correct passes being used

    @Test
    fun onPointerInput_1PointerDown_dispatchedDuringInitialTunnel() {
        val down = down(1, 2.milliseconds, 3f, 4f)

        pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverPasses(down, PointerEventPass.InitialDown)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
    }

    @Test
    fun onPointerInput_1PointerUp_dispatchedDuringInitialTunnel() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val up = down.up(5.milliseconds)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverPasses(up, PointerEventPass.InitialDown)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)
    }

    @Test
    fun onPointerInput_2PointersDown_dispatchedDuringInitialTunnel() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            aMove, bDown,
            pointerEventPass = PointerEventPass.InitialDown
        )

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)
    }

    @Test
    fun onPointerInput_2Pointers1Up_dispatchedDuringInitialTunnel() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(12.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        val aMove2 = aMove1.moveTo(13.milliseconds, 3f, 4f)
        val bUp = bDown.up(13.milliseconds)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            aMove2, bUp,
            pointerEventPass = PointerEventPass.InitialDown
        )

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(3)
    }

    @Test
    fun onPointerInput_pointerMove_dispatchedDuringPostTunnel() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val move = down.moveTo(7.milliseconds, 8f, 9f)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            move,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown,
            PointerEventPass.PostUp
        )

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            move,
            PointerEventPass.PostDown
        )

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
    }

    @Test
    fun onPointerInput_downDisallowInterceptRequestedMove_moveDispatchedDuringInitialTunnel() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val move = down.moveTo(7.milliseconds, 8f, 9f)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)
        mockViewGroup.dispatchedMotionEvents.clear()

        mockViewGroup.requestDisallowInterceptTouchEvent(true)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            move,
            PointerEventPass.InitialDown
        )

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
    }

    @Test
    fun onPointerInput_disallowInterceptRequestedUpDownMove_moveDispatchedDuringPostTunnel() {
        val downA = down(1, 2.milliseconds, 3f, 4f)
        val upA = downA.up(11.milliseconds)
        val downB = down(21, 22.milliseconds, 23f, 24f)
        val moveB = downB.moveTo(31.milliseconds, 32f, 33f)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(downA)
        mockViewGroup.requestDisallowInterceptTouchEvent(true)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(upA)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(downB)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            moveB,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown,
            PointerEventPass.PostUp
        )

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            moveB,
            PointerEventPass.PostDown
        )

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
    }

    @Test
    fun onPointerInput_disallowInterceptTrueThenFalseThenMove_moveDispatchedDuringPostTunnel() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val move = down.moveTo(7.milliseconds, 8f, 9f)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)
        mockViewGroup.requestDisallowInterceptTouchEvent(true)
        mockViewGroup.requestDisallowInterceptTouchEvent(false)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            move,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown,
            PointerEventPass.PostUp
        )

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            move,
            PointerEventPass.PostDown
        )

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
    }

    @Test
    fun onPointerInput_1PointerUpConsumed_dispatchDuringInitialTunnel() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val upConsumed = down.up(5.milliseconds).consumeDownChange()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverPasses(upConsumed, PointerEventPass.InitialDown)

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)
    }

    @Test
    fun onPointerInput_2PointersDown2ndDownConsumed_dispatchDuringInitialTunnel() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDownConsumed = down(8, 7.milliseconds, 10f, 11f).consumeDownChange()

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverPasses(aMove, bDownConsumed, pointerEventPass = PointerEventPass.InitialDown)

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)
    }

    @Test
    fun onPointerInput_2Pointers1UpConsumed_dispatchDuringInitialTunnel() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(12.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        val aMove2 = aMove1.moveTo(13.milliseconds, 3f, 4f)
        val bUpConsumed = bDown.up(13.milliseconds).consumeDownChange()

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverPasses(aMove2, bUpConsumed, pointerEventPass = PointerEventPass.InitialDown)

        // Assert

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(3)
    }

    @Test
    fun onPointerInput_1PointerMoveConsumed_dispatchDuringPostTunnel() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val moveConsumed = down.moveTo(7.milliseconds, 8f, 9f).consume(1f, 0f)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            moveConsumed,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown,
            PointerEventPass.PostUp
        )

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            moveConsumed,
            PointerEventPass.PostDown
        )

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)
    }

    @Test
    fun onPointerInput_2PointersMoveConsumed_dispatchDuringPostTunnel() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDown = down(11, 7.milliseconds, 13f, 14f)

        val aMove2 = aMove1.moveTo(15.milliseconds, 8f, 9f)
        val bMoveConsumed = bDown.moveTo(15.milliseconds, 18f, 19f).consume(1f, 0f)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act 1

        pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverPasses(
                listOf(aMove2, bMoveConsumed),
                listOf(
                    PointerEventPass.InitialDown,
                    PointerEventPass.PreUp,
                    PointerEventPass.PreDown,
                    PointerEventPass.PostUp
                )
            )

        // Assert 1

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)

        pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverPasses(
                aMove2, bMoveConsumed,
                pointerEventPass = PointerEventPass.PostDown
            )

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(3)
    }

    @Test
    fun onPointerInput_1PointerDown_consumedDuringInitialTunnel() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val expected = down.consumeAllChanges()

        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverPasses(down, PointerEventPass.InitialDown)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_1PointerUp_consumedDuringInitialTunnel() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val up = down.up(5.milliseconds)
        val expected = up.consumeAllChanges()
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput
            .invokeOverPasses(up, PointerEventPass.InitialDown)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_2PointersDown_consumedDuringInitialTunnel() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove = aDown.moveTo(7.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        val expected = listOf(aMove, bDown).map { it.consumeAllChanges() }

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)

        // Act

        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            aMove, bDown,
            pointerEventPass = PointerEventPass.InitialDown
        )

        // Assert

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_2Pointers1Up_consumedDuringInitialTunnel() {

        // Arrange

        val aDown = down(1, 2.milliseconds, 3f, 4f)

        val aMove1 = aDown.moveTo(12.milliseconds, 3f, 4f)
        val bDown = down(8, 7.milliseconds, 10f, 11f)

        val aMove2 = aMove1.moveTo(13.milliseconds, 3f, 4f)
        val bUp = bDown.up(13.milliseconds)

        val expected = listOf(aMove2, bUp).map { it.consumeAllChanges() }

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aDown)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(aMove1, bDown)

        // Act

        val actual = pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            aMove2, bUp,
            pointerEventPass = PointerEventPass.InitialDown
        )

        // Assert

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun onPointerInput_pointerMove_consumedDuringPostTunnel() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val move = down.moveTo(7.milliseconds, 8f, 9f)

        val expected2 = move.consumeAllChanges()

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        val actual1 = pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            move,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown,
            PointerEventPass.PostUp
        )

        assertThat(actual1).isEqualTo(move)

        val actual2 = pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverPasses(
            move,
            PointerEventPass.PostDown
        )

        assertThat(actual2).isEqualTo(expected2)
    }

    @Test
    fun onCancel_cancelEventIsCorrect() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)

        pointerInteropFilter.pointerInputFilter.onCancel()

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(2)
        val cancelEvent = mockViewGroup.dispatchedMotionEvents[1]

        assertThat(cancelEvent.actionMasked).isEqualTo(ACTION_CANCEL)
        assertThat(cancelEvent.actionIndex).isEqualTo(0)
        assertThat(cancelEvent.pointerCount).isEqualTo(1)

        val actualPointerProperties = MotionEvent.PointerProperties()
        cancelEvent.getPointerProperties(0, actualPointerProperties)
        assertThat(actualPointerProperties.id).isEqualTo(0)
        assertThat(actualPointerProperties.toolType).isEqualTo(TOOL_TYPE_UNKNOWN)

        val actualPointerCoords = MotionEvent.PointerCoords()
        cancelEvent.getPointerCoords(0, actualPointerCoords)
        assertThat(actualPointerCoords.x).isEqualTo(0)
        assertThat(actualPointerCoords.y).isEqualTo(0)
    }

    @Test
    fun onCancel_noPointers_cancelNotDispatched() {
        pointerInteropFilter.pointerInputFilter.onCancel()
        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onCancel_downConsumedCancel_cancelNotDispatched() {
        val downConsumed = down(1, 2.milliseconds, 3f, 4f).consumeDownChange()

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(downConsumed)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter.onCancel()

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onCancel_downViewRetsFalseThenCancel_cancelNotDispatched() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        mockViewGroup.returnValue = false

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter.onCancel()

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onCancel_downThenUpOnCancel_cancelNotDispatched() {
        val down = down(1, 2.milliseconds, 3f, 4f)
        val up = down.up(11.milliseconds)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)
        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(up)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter.onCancel()

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(0)
    }

    @Test
    fun onCancel_downThenOnCancel_cancelDispatchedOnce() {
        val down = down(1, 2.milliseconds, 3f, 4f)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter.onCancel()

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
    }

    @Test
    fun onCancel_downThenOnCancelThenOnCancel_cancelDispatchedOnce() {
        val down = down(1, 2.milliseconds, 3f, 4f)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter.onCancel()
        pointerInteropFilter.pointerInputFilter.onCancel()

        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
    }

    @Test
    fun onCancel_downThenOnCancelThenDownThenOnCancel_cancelDispatchedTwice() {
        val down1 = down(1, 2.milliseconds, 3f, 4f)
        val down2 = down(1, 2.milliseconds, 3f, 4f)

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down1)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter.onCancel()
        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
        mockViewGroup.dispatchedMotionEvents.clear()

        pointerInteropFilter.pointerInputFilter::onPointerInput.invokeOverAllPasses(down2)
        mockViewGroup.dispatchedMotionEvents.clear()
        pointerInteropFilter.pointerInputFilter.onCancel()
        assertThat(mockViewGroup.dispatchedMotionEvents).hasSize(1)
    }
}

internal class MockViewGroup(context: Context) : AndroidViewHolder(context) {
    var dispatchedMotionEvents = mutableListOf<MotionEvent>()
    var returnValue = true

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        dispatchedMotionEvents.add(MotionEvent.obtain(ev))
        return returnValue
    }
}

// Private helper functions

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
    pointerCoords: Array<MotionEvent.PointerCoords>,
    downTime: Long = 0
) = MotionEvent.obtain(
    downTime,
    eventTime.toLong(),
    action + (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
    numPointers,
    // TODO(shepshapard): This is bad and temporary
    pointerProperties.map { PointerProperties(it.id % 32) }.toTypedArray(),
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
    assertThat(actual.downTime).isEqualTo(expected.downTime)
    assertThat(actual.eventTime).isEqualTo(expected.eventTime)
    assertThat(actual.actionMasked).isEqualTo(expected.actionMasked)
    assertThat(actual.actionIndex).isEqualTo(expected.actionIndex)
    assertThat(actual.pointerCount).isEqualTo(expected.pointerCount)

    val actualPointerProperties = MotionEvent.PointerProperties()
    val expectedPointerProperties = MotionEvent.PointerProperties()
    repeat(expected.pointerCount) { index ->
        actual.getPointerProperties(index, actualPointerProperties)
        expected.getPointerProperties(index, expectedPointerProperties)
        assertThat(actualPointerProperties).isEqualTo(expectedPointerProperties)
    }

    val actualPointerCoords = MotionEvent.PointerCoords()
    val expectedPointerCoords = MotionEvent.PointerCoords()
    repeat(expected.pointerCount) { index ->
        actual.getPointerCoords(index, actualPointerCoords)
        expected.getPointerCoords(index, expectedPointerCoords)
        assertThat(actualPointerCoords.x).isEqualTo(expectedPointerCoords.x)
        assertThat(actualPointerCoords.y).isEqualTo(expectedPointerCoords.y)
    }
}