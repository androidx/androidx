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

package androidx.ui.core.gesture

import androidx.ui.core.Direction
import androidx.ui.core.Duration
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PxPosition
import androidx.ui.core.anyPositionChangeConsumed
import androidx.ui.core.consumeDownChange
import androidx.ui.core.consumePositionChange
import androidx.ui.core.ipx
import androidx.ui.core.milliseconds
import androidx.ui.core.millisecondsToTimestamp
import androidx.ui.core.px
import androidx.ui.testutils.consume
import androidx.ui.testutils.down
import androidx.ui.testutils.invokeOverAllPasses
import androidx.ui.testutils.moveBy
import androidx.ui.testutils.moveTo
import androidx.ui.testutils.up
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO(shepshapard): Write the following tests.
// Verify correct shape of slop area (should it be a square or circle)?
// Test for cases with more than one pointer
// Test for cases where things are reset when last pointer goes up
// Verify all methods called during onPostUp
// Verify default behavior when no callback provided for recognizer or canDrag

// Changing this value will break tests that expect the value to be 10.
private const val TestTouchSlop = 10

@RunWith(JUnit4::class)
class DragGestureDetectorTest {

    private lateinit var recognizer: DragGestureRecognizer
    private lateinit var canDragMockTrue: MockCanDrag
    private lateinit var log: MutableList<LogItem>

    @Before
    fun setup() {
        log = mutableListOf()
        recognizer = DragGestureRecognizer()
        recognizer.touchSlop = TestTouchSlop.ipx
        canDragMockTrue = MockCanDrag(Direction.values(), log)
    }

    // Verify the circumstances under which canDrag should not be called.

    @Test
    fun pointerInputHandler_down_canDragNotCalled() {
        recognizer.canDrag = canDragMockTrue
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down()))
        assertThat(log).isEmpty()
    }

    @Test
    fun pointerInputHandler_downMoveFullyConsumed_canDragNotCalled() {
        recognizer.canDrag = canDragMockTrue

        val down = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(
            listOf(down.moveBy(Duration(milliseconds = 10), 3f, 5f).consume(3f, 5f))
        )

        assertThat(log).isEmpty()
    }

    // Verify the circumstances under which canDrag should be called.

    @Test
    fun pointerInputHandler_downMove1Dimension_canDragCalledOnce() {
        recognizer.canDrag = canDragMockTrue

        val down = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(
            listOf(down.moveBy(Duration(milliseconds = 10), 3f, 0f))
        )

        // Twice because while under touch slop, DragGestureDetector checks during PostUp and
        // PostDown
        assertThat(log.filter { it.methodName == "canDrag" }).hasSize(2)
    }

    @Test
    fun pointerInputHandler_downMove2Dimensions_canDragCalledTwice() {
        recognizer.canDrag = canDragMockTrue

        val down = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(
            listOf(down.moveBy(Duration(milliseconds = 10), 3f, 5f))
        )

        // 4 times because while under touch slop, DragGestureDetector checks during PostUp and
        // PostDown
        assertThat(log.filter { it.methodName == "canDrag" }).hasSize(4)
    }

    @Test
    fun pointerInputHandler_downMoveOneDimensionPartiallyConsumed_canDragCalledOnce() {
        recognizer.canDrag = canDragMockTrue

        val down = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(
            listOf(down.moveBy(Duration(milliseconds = 10), 0f, 5f).consume(0f, 4f))
        )

        // Twice because while under touch slop, DragGestureDetector checks during PostUp and
        // PostDown
        assertThat(log.filter { it.methodName == "canDrag" }).hasSize(2)
    }

    @Test
    fun pointerInputHandler_downMoveTwoDimensionPartiallyConsumed_canDragCalledTwice() {
        recognizer.canDrag = canDragMockTrue
        val down = down()

        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(
            listOf(down.moveBy(Duration(milliseconds = 10), 3f, 5f).consume(2f, 4f))
        )

        // 4 times because while under touch slop, DragGestureDetector checks during PostUp and
        // PostDown
        assertThat(log.filter { it.methodName == "canDrag" }).hasSize(4)
    }

    @Test
    fun pointerInputHandler_dragPastTouchSlopOneDimensionAndDrag3MoreTimes_canDragCalledOnce() {
        val justBeyondSlop = (TestTouchSlop + 1).toFloat()
        recognizer.canDrag = canDragMockTrue

        val down = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        var move = down.moveTo(10L.millisecondsToTimestamp(), 0f, justBeyondSlop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(move))
        repeat(3) {
            move = move.moveBy(Duration(milliseconds = 10), 0f, 1f)
            recognizer.pointerInputHandler.invokeOverAllPasses(listOf(move))
        }

        // Once because although DragGestureDetector checks during PostUp and PostDown, slop is
        // surpassed during PostUp, and thus isn't checked again.
        assertThat(log.filter { it.methodName == "canDrag" }).hasSize(1)
    }

    @Test
    fun pointerInputHandler_downMoveUnderSlop3Times_canDragCalled3Times() {
        val thirdSlop = TestTouchSlop.toFloat() / 3
        recognizer.canDrag = canDragMockTrue

        var event = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(event))
        repeat(3) {
            event = event.moveBy(Duration(milliseconds = 10), 0f, thirdSlop)
            recognizer.pointerInputHandler.invokeOverAllPasses(listOf(event))
        }

        // 6 times because while under touch slop, DragGestureDetector checks during PostUp and
        // PostDown
        assertThat(log.filter { it.methodName == "canDrag" }).hasSize(6)
    }

    @Test
    fun pointerInputHandler_moveBeyondSlopThenIntoTouchSlopAreaAndOutAgain_canDragCalledOnce() {
        val beyondTouchSlop = (TestTouchSlop + 1).toFloat()
        recognizer.canDrag = canDragMockTrue

        var event = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(event))
        // Out of touch slop region
        event = event.moveBy(Duration(milliseconds = 10), 0f, beyondTouchSlop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(event))
        // Back into touch slop region
        event = event.moveBy(Duration(milliseconds = 10), 0f, -beyondTouchSlop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(event))
        // Out of touch slop region again
        event = event.moveBy(Duration(milliseconds = 10), 0f, beyondTouchSlop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(event))

        // Once because although DragGestureDetector checks during PostUp and PostDown, slop is
        // surpassed during PostUp, and thus isn't checked again.
        assertThat(log.filter { it.methodName == "canDrag" }).hasSize(1)
    }

    // Verification of correctness of values passed to canDrag.

    @Test
    fun pointerInputHandler_canDragCalledWithCorrectDirection() {
        pointerInputHandler_canDragCalledWithCorrectDirection(-1f, 0f, arrayOf(Direction.LEFT))
        pointerInputHandler_canDragCalledWithCorrectDirection(0f, -1f, arrayOf(Direction.UP))
        pointerInputHandler_canDragCalledWithCorrectDirection(1f, 0f, arrayOf(Direction.RIGHT))
        pointerInputHandler_canDragCalledWithCorrectDirection(0f, 1f, arrayOf(Direction.DOWN))

        pointerInputHandler_canDragCalledWithCorrectDirection(
            -1f,
            -1f,
            arrayOf(Direction.LEFT, Direction.UP)
        )
        pointerInputHandler_canDragCalledWithCorrectDirection(
            -1f,
            1f,
            arrayOf(Direction.LEFT, Direction.DOWN)
        )
        pointerInputHandler_canDragCalledWithCorrectDirection(
            1f,
            -1f,
            arrayOf(Direction.RIGHT, Direction.UP)
        )
        pointerInputHandler_canDragCalledWithCorrectDirection(
            1f,
            1f,
            arrayOf(Direction.RIGHT, Direction.DOWN)
        )
    }

    private fun pointerInputHandler_canDragCalledWithCorrectDirection(
        dx: Float,
        dy: Float,
        expectedDirections: Array<Direction>
    ) {
        log.clear()
        recognizer.canDrag = canDragMockTrue

        val down = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(
            listOf(down.moveBy(Duration(milliseconds = 10), dx, dy))
        )

        // Everything here is twice because DragGestureDetector checks during PostUp and PostDown.
        assertThat(log).hasSize(expectedDirections.size * 2)
        expectedDirections.forEach { direction ->
            assertThat(log.count { it == LogItem("canDrag", direction = direction) })
                .isEqualTo(2)
        }
    }

    // Verify the circumstances under which onStart/OnDrag should not be called.

    // TODO(b/129701831): This test assumes that if a pointer moves by slop in both x and y, we are
    // still under slop even though sqrt(slop^2 + slop^2) > slop.  This may be inaccurate and this
    // test may therefore need to be updated.
    @Test
    fun pointerInputHandler_downMoveWithinSlop_onStartAndOnDragNotCalled() {
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        val down = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(
            listOf(
                down.moveBy(
                    Duration(milliseconds = 10),
                    TestTouchSlop.toFloat(),
                    TestTouchSlop.toFloat()
                )
            )
        )

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(0)
        assertThat(log.filter { it.methodName == "onDrag" }).hasSize(0)
    }

    @Test
    fun pointerInputHandler_moveBeyondSlopInUnsupportedDirection_onStartAndOnDragNotCalled() {
        val beyondSlop = (TestTouchSlop + 1).toFloat()
        recognizer.canDrag = MockCanDrag(arrayOf(), log)
        recognizer.dragObserver = MockDragObserver(log)

        val down = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(
            listOf(
                down.moveBy(
                    Duration(milliseconds = 10),
                    beyondSlop,
                    beyondSlop
                )
            )
        )

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(0)
        assertThat(log.filter { it.methodName == "onDrag" }).hasSize(0)
    }

    // TODO(b/129701831): This test assumes that if a pointer moves by slop in both x and y, we are
    // still under slop even though sqrt(slop^2 + slop^2) > slop.  This may be inaccurate and this
    // test may therefore need to be updated.
    @Test
    fun pointerInputHandler_moveAroundWithinSlop_onStartAndOnDragNotCalled() {
        val slop = TestTouchSlop.toFloat()
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        // Go around the border of the touch slop area

        // To top left
        change = change.moveTo(10L.millisecondsToTimestamp(), -slop, -slop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        // To bottom left
        change = change.moveTo(20L.millisecondsToTimestamp(), -slop, slop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        // To bottom right
        change = change.moveTo(30L.millisecondsToTimestamp(), slop, slop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        // To top right
        change = change.moveTo(40L.millisecondsToTimestamp(), slop, -slop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        // Jump from corner to opposite corner and back

        // To bottom left
        change = change.moveTo(50L.millisecondsToTimestamp(), -slop, slop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        // To top right
        change = change.moveTo(60L.millisecondsToTimestamp(), slop, -slop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        // Move the other diagonal

        // To top left
        change = change.moveTo(70L.millisecondsToTimestamp(), -slop, -slop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        // Jump from corner to opposite corner and back

        // To bottom right
        change = change.moveTo(80L.millisecondsToTimestamp(), slop, slop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        // To top left
        change = change.moveTo(90L.millisecondsToTimestamp(), -slop, -slop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(0)
        assertThat(log.filter { it.methodName == "onDrag" }).hasSize(0)
    }

    // Verify the circumstances under which onStart and OnDrag should be called.

    @Test
    fun pointerInputHandler_movePassedSlop_onStartCalledOnceAndOnDragAtLeastOnce() {
        val beyondTouchSlop = (TestTouchSlop + 1).toFloat()
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)
        val down = down()

        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(
            listOf(
                down.moveBy(
                    Duration(milliseconds = 100),
                    beyondTouchSlop,
                    0f
                )
            )
        )

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        assertThat(log.filter { it.methodName == "onDrag" }).isNotEmpty()
    }

    @Test
    fun pointerInputHandler_passSlopThenInSlopAreaThenOut_onStartCalledOnceAndOnDragAtLeastOnce() {
        val beyondTouchSlop = (TestTouchSlop + 1).toFloat()
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        var event = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(event))
        // Out of touch slop region
        event = event.moveBy(Duration(milliseconds = 10), 0f, beyondTouchSlop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(event))
        // Back into touch slop region
        event = event.moveBy(Duration(milliseconds = 10), 0f, -beyondTouchSlop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(event))
        // Out of touch slop region again
        event = event.moveBy(Duration(milliseconds = 10), 0f, beyondTouchSlop)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(event))

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        assertThat(log.filter { it.methodName == "onDrag" }).isNotEmpty()
    }

    @Test
    fun pointerInputHandler_downConsumedMovePassedSlop_onStartCalled1AndOnDragCalledAtLeast1() {
        val beyondTouchSlop = (TestTouchSlop + 1).toFloat()
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)
        val down = down().consumeDownChange()

        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(
            listOf(
                down.moveBy(
                    Duration(milliseconds = 100),
                    beyondTouchSlop,
                    0f
                )
            )
        )

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        assertThat(log.filter { it.methodName == "onDrag" }).isNotEmpty()
    }

    @Test
    fun pointerInputHandler_beyondInUnsupportThenBeyondInSupport_onStart1AndOnDragAtLeast1() {
        val doubleTouchSlop = (TestTouchSlop * 2).toFloat()
        val beyondTouchSlop = (TestTouchSlop + 1).toFloat()
        recognizer.canDrag = MockCanDrag(arrayOf(Direction.UP), log)
        recognizer.dragObserver = MockDragObserver(log)

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveBy(
            Duration(milliseconds = 10),
            0f,
            doubleTouchSlop
        )
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveBy(
            Duration(milliseconds = 10),
            0f,
            -beyondTouchSlop
        )
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        assertThat(log.filter { it.methodName == "onDrag" }).isNotEmpty()
    }

    // Verify the circumstances under which onStart should be called, but onDrag shouldn't.

    @Test
    fun pointerInputHandler_moveBeyondSlopInOppositDirections_onStartCalled1AndOnDragNotCalled() {
        val beyondTouchSlop = (TestTouchSlop + 1).toFloat()
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)
        val down1 = down()
        val down2 = down()

        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down1, down2))
        recognizer.pointerInputHandler.invokeOverAllPasses(
            listOf(
                down1.moveBy(
                    Duration(milliseconds = 100),
                    beyondTouchSlop,
                    0f
                ),
                down2.moveBy(
                    Duration(milliseconds = 100),
                    -beyondTouchSlop,
                    0f
                )
            )
        )

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    // onDrag not called when movement averages to 0 in x and y.

    @Test
    fun pointerInputHandler_2PointsMoveInOpposite_onDragNotCalled() {

        // Arrange

        val beyondTouchSlop = (TestTouchSlop + 1).toFloat()
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        var pointer1 = down()
        var pointer2 = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer1, pointer2))
        pointer1 = pointer1.moveBy(
            Duration(milliseconds = 100),
            beyondTouchSlop,
            0f
        )
        pointer2 = pointer2.moveBy(
            Duration(milliseconds = 100),
            beyondTouchSlop,
            0f
        )
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer1, pointer2))

        log.clear()

        // Act

        pointer1 = pointer1.moveBy(
            Duration(milliseconds = 100),
            1f,
            1f
        )
        pointer2 = pointer2.moveBy(
            Duration(milliseconds = 100),
            -1f,
            -1f
        )
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer1, pointer2))

        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    @Test
    fun pointerInputHandler_3PointsMoveAverage0_onDragNotCalled() {

        // Arrange

        val beyondTouchSlop = (TestTouchSlop + 1).toFloat()
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        val pointers = arrayListOf(down(), down(), down())
        recognizer.pointerInputHandler.invokeOverAllPasses(pointers)

        pointers.forEachIndexed { index, pointerInputChange ->
            pointers[index] =
                pointerInputChange.moveBy(
                    Duration(milliseconds = 100),
                    beyondTouchSlop,
                    0f
                )
        }
        recognizer.pointerInputHandler.invokeOverAllPasses(pointers)
        log.clear()

        // Act

        // These movements average to no movement.
        pointers[0] =
            pointers[0].moveBy(
                Duration(milliseconds = 100),
                -1f,
                -1f
            )
        pointers[1] =
            pointers[1].moveBy(
                Duration(milliseconds = 100),
                1f,
                -1f
            )
        pointers[2] =
            pointers[2].moveBy(
                Duration(milliseconds = 100),
                0f,
                2f
            )
        recognizer.pointerInputHandler.invokeOverAllPasses(pointers)

        // Assert

        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    // onDrag called with correct values verification

    @Test
    fun pointerInputHandler_justPassedSlop_onDragCalledWithTotalDistance() {
        val beyondTouchSlop = (TestTouchSlop + 1).toFloat()
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveBy(
            Duration(milliseconds = 100),
            beyondTouchSlop,
            0f
        )
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        assertThat(log).contains(LogItem("onDrag", pxPosition = PxPosition(11.px, 0.px)))
    }

    @Test
    fun pointerInputHandler_moveAndMoveConsumed_onDragCalledWithCorrectDistance() {
        val beyondTouchSlop = (TestTouchSlop + 1).toFloat()
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveBy(Duration(milliseconds = 100), beyondTouchSlop, 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveBy(Duration(milliseconds = 100), 3f, -5f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveBy(Duration(milliseconds = 100), -3f, 7f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveBy(Duration(milliseconds = 100), 11f, 13f)
            .consumePositionChange(5.px, 3.px)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveBy(Duration(milliseconds = 100), -13f, -11f)
            .consumePositionChange(-3.px, -5.px)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        val onDragLog = log.filter { it.methodName == "onDrag" }
        // 2 onDrags per event because DragGestureDetector calls onDrag on both PostUp and PostDown.
        assertThat(onDragLog).hasSize(10)
        assertThat(onDragLog[0].pxPosition).isEqualTo(PxPosition(11.px, 0.px))
        assertThat(onDragLog[1].pxPosition).isEqualTo(PxPosition(11.px, 0.px))
        assertThat(onDragLog[2].pxPosition).isEqualTo(PxPosition(3.px, -5.px))
        assertThat(onDragLog[3].pxPosition).isEqualTo(PxPosition(3.px, -5.px))
        assertThat(onDragLog[4].pxPosition).isEqualTo(PxPosition(-3.px, 7.px))
        assertThat(onDragLog[5].pxPosition).isEqualTo(PxPosition(-3.px, 7.px))
        assertThat(onDragLog[6].pxPosition).isEqualTo(PxPosition(6.px, 10.px))
        assertThat(onDragLog[7].pxPosition).isEqualTo(PxPosition(6.px, 10.px))
        assertThat(onDragLog[8].pxPosition).isEqualTo(PxPosition((-10).px, -6.px))
        assertThat(onDragLog[9].pxPosition).isEqualTo(PxPosition((-10).px, -6.px))
    }

    @Test
    fun pointerInputHandler_3Down1Moves_onDragCalledWith3rdOfDistance() {

        // Arrange

        val beyondTouchSlop = (TestTouchSlop + 1).toFloat()
        val thirdDistance1 = beyondTouchSlop
        val thirdDistance2 = beyondTouchSlop * 2
        val distance1 = thirdDistance1 * 3
        val distance2 = thirdDistance2 * 3

        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        var pointer1 = down()
        val pointer2 = down()
        val pointer3 = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer1, pointer2, pointer3))

        pointer1 = pointer1.moveBy(100.milliseconds, distance1, distance2)

        // Act

        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer1, pointer2, pointer3))

        // Assert

        val onDragLog = log.filter { it.methodName == "onDrag" }
        assertThat(onDragLog).hasSize(2)
        // 2 onDrags because DragGestureDetector calls onDrag on both PostUp and PostDown.
        assertThat(onDragLog[0].pxPosition).isEqualTo(
            PxPosition(
                thirdDistance1.px,
                thirdDistance2.px
            )
        )
        assertThat(onDragLog[1].pxPosition).isEqualTo(
            PxPosition(
                thirdDistance1.px,
                thirdDistance2.px
            )
        )
    }

    // onStop not called verification

    @Test
    fun pointerInputHandler_downMoveWithinSlopUp_onStopNotCalled() {
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveTo(
            10L.millisecondsToTimestamp(),
            TestTouchSlop.toFloat(),
            TestTouchSlop.toFloat()
        )
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.up(20L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        assertThat(log.filter { it.methodName == "onStop" }).hasSize(0)
    }

    @Test
    fun pointerInputHandler_downMoveBeyondSlop_onStopNotCalled() {
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveTo(
            10L.millisecondsToTimestamp(),
            TestTouchSlop.toFloat() + 1,
            TestTouchSlop.toFloat()
        )
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        assertThat(log.filter { it.methodName == "onStop" }).hasSize(0)
    }

    // onStop called verification

    @Test
    fun pointerInputHandler_downMoveBeyondSlopUp_onStopCalledOnce() {
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveTo(
            10L.millisecondsToTimestamp(),
            TestTouchSlop.toFloat() + 1,
            TestTouchSlop.toFloat()
        )
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.up(20L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        assertThat(log.filter { it.methodName == "onStop" }).hasSize(1)
    }

    // onStop called with correct values verification

    @Test
    fun pointerInputHandler_flingBeyondSlop_onStopCalledWithCorrectVelocity() {
        pointerInputHandler_flingBeyondSlop_onStopCalledWithCorrectVelocity(0f, 1f, 0f, 100f)
        pointerInputHandler_flingBeyondSlop_onStopCalledWithCorrectVelocity(0f, -1f, 0f, -100f)
        pointerInputHandler_flingBeyondSlop_onStopCalledWithCorrectVelocity(1f, 0f, 100f, 0f)
        pointerInputHandler_flingBeyondSlop_onStopCalledWithCorrectVelocity(-1f, 0f, -100f, 0f)

        pointerInputHandler_flingBeyondSlop_onStopCalledWithCorrectVelocity(1f, 1f, 100f, 100f)
        pointerInputHandler_flingBeyondSlop_onStopCalledWithCorrectVelocity(-1f, 1f, -100f, 100f)
        pointerInputHandler_flingBeyondSlop_onStopCalledWithCorrectVelocity(1f, -1f, 100f, -100f)
        pointerInputHandler_flingBeyondSlop_onStopCalledWithCorrectVelocity(-1f, -1f, -100f, -100f)
    }

    private fun pointerInputHandler_flingBeyondSlop_onStopCalledWithCorrectVelocity(
        incrementPerMilliX: Float,
        incrementPerMilliY: Float,
        expectedPxPerSecondDx: Float,
        expectedPxPerSecondDy: Float
    ) {
        log = mutableListOf()
        recognizer = DragGestureRecognizer()
        recognizer.touchSlop = TestTouchSlop.ipx
        recognizer.canDrag = MockCanDrag(Direction.values(), log)
        recognizer.dragObserver = MockDragObserver(log)

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        repeat(11) {
            change = change.moveBy(
                Duration(milliseconds = 10),
                incrementPerMilliX,
                incrementPerMilliY
            )
            recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        }

        change = change.up(20L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        val velocity = log.find { it.methodName == "onStop" }!!.pxPosition!!
        assertThat(velocity.x.value).isWithin(.01f).of(expectedPxPerSecondDx)
        assertThat(velocity.y.value).isWithin(.01f).of(expectedPxPerSecondDy)
    }

    // Verification that callbacks occur in the correct order

    @Test
    fun pointerInputHandler_downMoveBeyondSlopUp_callBacksOccurInCorrectOrder() {
        recognizer.canDrag = MockCanDrag(arrayOf(Direction.DOWN), log)
        recognizer.dragObserver = MockDragObserver(log)

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveTo(
            10L.millisecondsToTimestamp(),
            TestTouchSlop.toFloat(),
            TestTouchSlop.toFloat() + 1
        )
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.up(20L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        assertThat(log).hasSize(6)
        assertThat(log[0].methodName).isEqualTo("canDrag")
        assertThat(log[1].methodName).isEqualTo("canDrag")
        assertThat(log[2].methodName).isEqualTo("onStart")
        // 2 onDrags because DragGestureDetector calls onDrag on both PostUp and PostDown.
        assertThat(log[3].methodName).isEqualTo("onDrag")
        assertThat(log[4].methodName).isEqualTo("onDrag")
        assertThat(log[5].methodName).isEqualTo("onStop")
    }

    // Verification about what events are, or aren't consumed.

    @Test
    fun pointerInputHandler_down_downNotConsumed() {
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        val result = recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down()))

        assertThat(result.first().consumed.downChange).isFalse()
    }

    @Test
    fun pointerInputHandler_downMoveWithinTouchSlop_distanceChangeNotConsumed() {
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveTo(
            10L.millisecondsToTimestamp(),
            TestTouchSlop.toFloat(),
            TestTouchSlop.toFloat()
        )
        val result = recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        assertThat(result.first().anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun pointerInputHandler_downMoveBeyondSlopInUnsupportedDirection_distanceChangeNotConsumed() {
        recognizer.canDrag = MockCanDrag(arrayOf(), log)
        recognizer.dragObserver = MockDragObserver(log)

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveTo(
            10L.millisecondsToTimestamp(),
            TestTouchSlop.toFloat() + 1,
            TestTouchSlop.toFloat() + 1
        )
        val result = recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        assertThat(result.first().anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun pointerInputHandler_doneMoveCallBackDoesNotConsume_distanceChangeNotConsumed() {
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveTo(
            10L.millisecondsToTimestamp(),
            TestTouchSlop.toFloat() + 1,
            TestTouchSlop.toFloat() + 1
        )
        val result = recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        assertThat(result.first().anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun pointerInputHandler_moveOccursObserverDoesNotOverrideOnDrag_distanceChangeNotConsumed() {
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = object : DragObserver {}

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveTo(
            10L.millisecondsToTimestamp(),
            TestTouchSlop.toFloat() + 1,
            TestTouchSlop.toFloat() + 1
        )
        val result = recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        assertThat(result.first().anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun pointerInputHandler_moveOccursObserverNotSet_distanceChangeNotConsumed() {
        recognizer.canDrag = canDragMockTrue

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveTo(
            10L.millisecondsToTimestamp(),
            TestTouchSlop.toFloat() + 1,
            TestTouchSlop.toFloat() + 1
        )
        val result = recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        assertThat(result.first().anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun pointerInputHandler_moveCallBackConsumes_changeDistanceConsumedByCorrectAmount() {
        val thirdTouchSlop = TestTouchSlop.toFloat() / 3
        val quarterTouchSlop = TestTouchSlop.toFloat() / 4
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver =
            MockDragObserver(log, PxPosition(thirdTouchSlop.px, quarterTouchSlop.px))

        var change = down()
        recognizer.pointerInputHandler.invoke(listOf(change), PointerEventPass.PostUp)
        change = change.moveTo(
            10L.millisecondsToTimestamp(),
            TestTouchSlop.toFloat() + 1,
            TestTouchSlop.toFloat() + 1
        )
        val result = recognizer.pointerInputHandler.invoke(listOf(change), PointerEventPass.PostUp)

        assertThat(result.first().consumed.positionChange.x.value).isEqualTo(thirdTouchSlop)
        assertThat(result.first().consumed.positionChange.y.value).isEqualTo(quarterTouchSlop)
    }

    @Test
    fun pointerInputHandler_onStopConsumesUp() {
        recognizer.canDrag = canDragMockTrue
        recognizer.dragObserver = MockDragObserver(log)

        var change = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.moveTo(
            10L.millisecondsToTimestamp(),
            TestTouchSlop.toFloat() + 1,
            TestTouchSlop.toFloat()
        )
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))
        change = change.up(20L.millisecondsToTimestamp())
        val result = recognizer.pointerInputHandler.invokeOverAllPasses(listOf(change))

        assertThat(result.first().consumed.downChange).isTrue()
    }

    class MockCanDrag(
        private val directionsToReturnTrue: Array<Direction>,
        private val log: MutableList<LogItem>
    ) :
            (Direction) -> Boolean {
        override fun invoke(direction: Direction): Boolean {
            log.add(LogItem("canDrag", direction = direction))
            return directionsToReturnTrue.contains(direction)
        }
    }

    data class LogItem(
        val methodName: String,
        val direction: Direction? = null,
        val pxPosition: PxPosition? = null
    )

    class MockDragObserver(
        private val log: MutableList<LogItem>,
        private var dragConsume: PxPosition = PxPosition.Origin
    ) : DragObserver {
        override fun onStart() {
            log.add(LogItem("onStart"))
            super.onStart()
        }

        override fun onDrag(dragDistance: PxPosition): PxPosition {
            log.add(LogItem("onDrag", pxPosition = dragDistance))
            return dragConsume
        }

        override fun onStop(velocity: PxPosition) {
            log.add(LogItem("onStop", pxPosition = velocity))
            super.onStop(velocity)
        }
    }
}