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
import androidx.ui.core.PointerEventPass
import androidx.ui.core.consumeDownChange
import androidx.ui.testutils.consume
import androidx.ui.testutils.down
import androidx.ui.testutils.invokeOverAllPasses
import androidx.ui.testutils.invokeOverPasses
import androidx.ui.testutils.moveBy
import androidx.ui.testutils.moveTo
import androidx.ui.testutils.up
import androidx.ui.unit.Duration
import androidx.ui.unit.milliseconds
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
// Verify default behavior when no callback provided for filter or canDrag

// Changing this value will break tests that expect the value to be 10.
private const val TestTouchSlop = 10

@RunWith(JUnit4::class)
class DragSlopExceededGestureFilterTest {

    private val onDragSlopExceeded: () -> Unit = { onDragSlopExceededCallCount++ }
    private val canDrag: (Direction) -> Boolean = { direction ->
        canDragDirections.add(direction)
        canDragReturn
    }
    private var onDragSlopExceededCallCount: Int = 0
    private var canDragReturn = false
    private var canDragDirections: MutableList<Direction> = mutableListOf()
    private lateinit var filter: DragSlopExceededGestureFilter

    private val TinyNum = .01f

    @Before
    fun setup() {
        onDragSlopExceededCallCount = 0
        canDragReturn = true
        canDragDirections.clear()
        filter =
            DragSlopExceededGestureFilter(TestTouchSlop.toFloat())
        filter.canDrag = canDrag
        filter.onDragSlopExceeded = onDragSlopExceeded
    }

    // Verify the circumstances under which canDrag should not be called.

    @Test
    fun onPointerInputChanges_down_canDragNotCalled() {
        filter::onPointerInput.invokeOverAllPasses(down(0))
        assertThat(canDragDirections).isEmpty()
    }

    @Test
    fun onPointerInputChanges_downUp_canDragNotCalled() {
        val down = down(0, duration = 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(down)
        val up = down.up(10.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(up)

        assertThat(canDragDirections).isEmpty()
    }

    @Test
    fun onPointerInputChanges_downMoveFullyConsumed_canDragNotCalled() {
        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)
        val move = down.moveBy(Duration(milliseconds = 10), 3f, 5f).consume(3f, 5f)
        filter::onPointerInput.invokeOverAllPasses(move)

        assertThat(canDragDirections).isEmpty()
    }

    // Verify the circumstances under which canDrag should be called.

    @Test
    fun onPointerInputChanges_downMove1Dimension_canDragCalledOnce() {
        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)
        val move = down.moveBy(Duration(milliseconds = 10), 3f, 0f)
        filter::onPointerInput.invokeOverAllPasses(move)

        // Twice because while under touch slop, TouchSlopExceededGestureDetector checks during PostUp and PostDown
        assertThat(canDragDirections).hasSize(2)
    }

    @Test
    fun onPointerInputChanges_downMove2Dimensions_canDragCalledTwice() {
        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)
        val move = down.moveBy(Duration(milliseconds = 10), 3f, 5f)
        filter::onPointerInput.invokeOverAllPasses(move)

        // 4 times because while under touch slop, TouchSlopExceededGestureDetector checks during PostUp and
        // PostDown
        assertThat(canDragDirections).hasSize(4)
    }

    @Test
    fun onPointerInputChanges_downMoveOneDimensionPartiallyConsumed_canDragCalledOnce() {
        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)
        val move = down.moveBy(Duration(milliseconds = 10), 0f, 5f).consume(0f, 4f)
        filter::onPointerInput.invokeOverAllPasses(move)

        // Twice because while under touch slop, DragGestureDetector checks during PostUp and
        // PostDown
        assertThat(canDragDirections).hasSize(2)
    }

    @Test
    fun onPointerInputChanges_downMoveTwoDimensionPartiallyConsumed_canDragCalledTwice() {
        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)
        val move = down.moveBy(Duration(milliseconds = 10), 3f, 5f).consume(2f, 4f)
        filter::onPointerInput.invokeOverAllPasses(move)

        // 4 times because while under touch slop, DragGestureDetector checks during PostUp and
        // PostDown
        assertThat(canDragDirections).hasSize(4)
    }

    @Test
    fun onPointerInputChanges_dragPastTouchSlopOneDimensionAndDrag3MoreTimes_canDragCalledOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)
        var move = down.moveTo(10.milliseconds, 0f, beyondSlop)
        filter::onPointerInput.invokeOverAllPasses(move)
        repeat(3) {
            move = move.moveBy(Duration(milliseconds = 10), 0f, 1f)
            filter::onPointerInput.invokeOverAllPasses(move)
        }

        // Once because although DragGestureDetector checks during PostUp and PostDown, slop is
        // surpassed during PostUp, and thus isn't checked again.
        assertThat(canDragDirections).hasSize(1)
    }

    @Test
    fun onPointerInputChanges_downMoveUnderSlop3Times_canDragCalled3Times() {
        val thirdSlop = TestTouchSlop / 3

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)
        var move = down
        repeat(3) {
            move = move.moveBy(Duration(milliseconds = 10), 0f, thirdSlop.toFloat())
            filter::onPointerInput.invokeOverAllPasses(move)
        }

        // 6 times because while under touch slop, DragGestureDetector checks during PostUp and
        // PostDown
        assertThat(canDragDirections).hasSize(6)
    }

    @Test
    fun onPointerInputChanges_moveBeyondSlopThenIntoTouchSlopAreaAndOutAgain_canDragCalledOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        var event = down(0)
        filter::onPointerInput.invokeOverAllPasses(event)
        // Out of touch slop region
        event = event.moveBy(Duration(milliseconds = 10), 0f, beyondSlop)
        filter::onPointerInput.invokeOverAllPasses(event)
        // Back into touch slop region
        event = event.moveBy(Duration(milliseconds = 10), 0f, -beyondSlop)
        filter::onPointerInput.invokeOverAllPasses(event)
        // Out of touch slop region again
        event = event.moveBy(Duration(milliseconds = 10), 0f, beyondSlop)
        filter::onPointerInput.invokeOverAllPasses(event)

        // Once because although DragGestureDetector checks during PostUp and PostDown, slop is
        // surpassed during PostUp, and thus isn't checked again.
        assertThat(canDragDirections).hasSize(1)
    }

    // Verification of correctness of values passed to canDrag.

    @Test
    fun onPointerInputChanges_canDragCalledWithCorrectDirection() {
        onPointerInputChanges_canDragCalledWithCorrectDirection(
            -1f, 0f, arrayOf(Direction.LEFT)
        )
        onPointerInputChanges_canDragCalledWithCorrectDirection(
            0f, -1f, arrayOf(Direction.UP)
        )
        onPointerInputChanges_canDragCalledWithCorrectDirection(
            1f, 0f, arrayOf(Direction.RIGHT)
        )
        onPointerInputChanges_canDragCalledWithCorrectDirection(
            0f, 1f, arrayOf(Direction.DOWN)
        )
        onPointerInputChanges_canDragCalledWithCorrectDirection(
            -1f, -1f, arrayOf(Direction.LEFT, Direction.UP)
        )
        onPointerInputChanges_canDragCalledWithCorrectDirection(
            -1f, 1f, arrayOf(Direction.LEFT, Direction.DOWN)
        )
        onPointerInputChanges_canDragCalledWithCorrectDirection(
            1f, -1f, arrayOf(Direction.RIGHT, Direction.UP)
        )
        onPointerInputChanges_canDragCalledWithCorrectDirection(
            1f, 1f, arrayOf(Direction.RIGHT, Direction.DOWN)
        )
    }

    private fun onPointerInputChanges_canDragCalledWithCorrectDirection(
        dx: Float,
        dy: Float,
        expectedDirections: Array<Direction>
    ) {
        canDragDirections.clear()
        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)
        val move = down.moveBy(Duration(milliseconds = 10), dx, dy)
        filter::onPointerInput.invokeOverAllPasses(move)

        // Everything here is twice because DragGestureDetector checks during PostUp and PostDown.
        assertThat(canDragDirections).hasSize(expectedDirections.size * 2)
        expectedDirections.forEach { direction ->
            assertThat(canDragDirections.count { it == direction })
                .isEqualTo(2)
        }
    }

    // Verify the circumstances under which onTouchSlopExceeded should not be called.

    // TODO(b/129701831): This test assumes that if a pointer moves by slop in both x and y, we are
    // still under slop even though sqrt(slop^2 + slop^2) > slop.  This may be inaccurate and this
    // test may therefore need to be updated.
    @Test
    fun onPointerInputChanges_downMoveWithinSlop_onTouchSlopExceededNotCalled() {
        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)
        val move = down.moveBy(
            Duration(milliseconds = 10),
            TestTouchSlop.toFloat(),
            TestTouchSlop.toFloat()
        )
        filter::onPointerInput.invokeOverAllPasses(move)

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerInputChanges_moveBeyondSlopInUnsupportedDirection_onTouchSlopExceededNotCalled() {
        val beyondSlop = TestTouchSlop + TinyNum
        canDragReturn = false

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)
        val move = down.moveBy(
            Duration(milliseconds = 10),
            beyondSlop,
            beyondSlop
        )
        filter::onPointerInput.invokeOverAllPasses(move)

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerInputChanges_moveBeyondSlopButConsumeUnder_onTouchSlopExceededNotCalled() {

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)

        val move = down.moveBy(10.milliseconds, TestTouchSlop + TinyNum, 0f).consume(dx = 1f)
        filter::onPointerInput.invokeOverAllPasses(move)

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerInputChanges_moveUnderToPostUpThenModOverInOppDir_onTouchSlopExceededNotCalled() {

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)

        val move = down.moveBy(10.milliseconds, TestTouchSlop.toFloat(), 0f)
        filter::onPointerInput.invokeOverPasses(
            listOf(move),
            listOf(
                PointerEventPass.InitialDown,
                PointerEventPass.PreUp,
                PointerEventPass.PreDown,
                PointerEventPass.PostUp
            )
        )
        val move2 = move.consume(dx = (TestTouchSlop * 2f + TinyNum))
        filter::onPointerInput.invokeOverPasses(
            move2,
            PointerEventPass.PostDown
        )

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    // TODO(b/129701831): This test assumes that if a pointer moves by slop in both x and y, we are
    // still under slop even though sqrt(slop^2 + slop^2) > slop.  This may be inaccurate and this
    // test may therefore need to be updated.
    @Test
    fun onPointerInputChanges_moveAroundWithinSlop_onTouchSlopExceededNotCalled() {
        val slop = TestTouchSlop.toFloat()

        var change = down(0)
        filter::onPointerInput.invokeOverAllPasses(change)

        // Go around the border of the touch slop area

        // To top left
        change = change.moveTo(10.milliseconds, -slop, -slop)
        filter::onPointerInput.invokeOverAllPasses(change)
        // To bottom left
        change = change.moveTo(20.milliseconds, -slop, slop)
        filter::onPointerInput.invokeOverAllPasses(change)
        // To bottom right
        change = change.moveTo(30.milliseconds, slop, slop)
        filter::onPointerInput.invokeOverAllPasses(change)
        // To top right
        change = change.moveTo(40.milliseconds, slop, -slop)
        filter::onPointerInput.invokeOverAllPasses(change)

        // Jump from corner to opposite corner and back

        // To bottom left
        change = change.moveTo(50.milliseconds, -slop, slop)
        filter::onPointerInput.invokeOverAllPasses(change)
        // To top right
        change = change.moveTo(60.milliseconds, slop, -slop)
        filter::onPointerInput.invokeOverAllPasses(change)

        // Move the other diagonal

        // To top left
        change = change.moveTo(70.milliseconds, -slop, -slop)
        filter::onPointerInput.invokeOverAllPasses(change)

        // Jump from corner to opposite corner and back

        // To bottom right
        change = change.moveTo(80.milliseconds, slop, slop)
        filter::onPointerInput.invokeOverAllPasses(change)
        // To top left
        change = change.moveTo(90.milliseconds, -slop, -slop)
        filter::onPointerInput.invokeOverAllPasses(change)

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    // Verify the circumstances under which onTouchSlopExceeded should be called.

    @Test
    fun onPointerInputChanges_movePassedSlop_onTouchSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)
        val move = down.moveBy(
            Duration(milliseconds = 100),
            beyondSlop,
            0f
        )
        filter::onPointerInput.invokeOverAllPasses(move)

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerInputChanges_movePassedSlopIn2Events_onTouchSlopExceededCallOnce() {

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)
        val move = down.moveBy(
            Duration(milliseconds = 100),
            TestTouchSlop.toFloat(),
            0f
        )
        filter::onPointerInput.invokeOverAllPasses(move)
        val move2 = down.moveBy(
            Duration(milliseconds = 100),
            1f,
            0f
        )
        filter::onPointerInput.invokeOverAllPasses(move2)

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerInputChanges_passSlopThenInSlopAreaThenOut_onTouchSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        var event = down(0)
        filter::onPointerInput.invokeOverAllPasses(event)
        // Out of touch slop region
        event = event.moveBy(Duration(milliseconds = 10), 0f, beyondSlop)
        filter::onPointerInput.invokeOverAllPasses(event)
        // Back into touch slop region
        event = event.moveBy(Duration(milliseconds = 10), 0f, -beyondSlop)
        filter::onPointerInput.invokeOverAllPasses(event)
        // Out of touch slop region again
        event = event.moveBy(Duration(milliseconds = 10), 0f, beyondSlop)
        filter::onPointerInput.invokeOverAllPasses(event)

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerInputChanges_downConsumedMovePassedSlop_onTouchSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0).consumeDownChange()
        filter::onPointerInput.invokeOverAllPasses(down)
        val move = down.moveBy(Duration(milliseconds = 100), beyondSlop, 0f)
        filter::onPointerInput.invokeOverAllPasses(move)

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerInputChanges_beyondInUnsupportThenBeyondInSupport_onTouchSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        var change = down(0)
        filter::onPointerInput.invokeOverAllPasses(change)
        canDragReturn = false
        change = change.moveBy(
            Duration(milliseconds = 10),
            0f,
            beyondSlop
        )
        // Sanity check that onTouchSlopExceeded has not been called.
        assertThat(onDragSlopExceededCallCount).isEqualTo(0)

        canDragReturn = true
        filter::onPointerInput.invokeOverAllPasses(change)
        change = change.moveBy(
            Duration(milliseconds = 10),
            0f,
            -beyondSlop
        )
        filter::onPointerInput.invokeOverAllPasses(change)

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerInputChanges_2PointsMoveInOpposite_onTouchSlopExceededNotCalled() {

        // Arrange

        val beyondSlop = TestTouchSlop + TinyNum

        var pointer1 = down(1)
        var pointer2 = down(2)
        filter::onPointerInput.invokeOverAllPasses(pointer1, pointer2)

        // Act

        pointer1 = pointer1.moveBy(
            Duration(milliseconds = 100),
            beyondSlop,
            0f
        )
        pointer2 = pointer2.moveBy(
            Duration(milliseconds = 100),
            -beyondSlop,
            0f
        )
        filter::onPointerInput.invokeOverAllPasses(pointer1, pointer2)

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerInputChanges_3PointsMoveAverage0_onDragSlopExceededNotCalled() {

        // Arrange

        val beyondSlop = TestTouchSlop + TinyNum

        val pointers = arrayOf(down(0), down(1), down(2))
        filter::onPointerInput.invokeOverAllPasses(*pointers)

        // Act

        // These movements average to no movement.
        pointers[0] =
            pointers[0].moveBy(
                Duration(milliseconds = 100),
                beyondSlop * -1,
                beyondSlop * -1
            )
        pointers[1] =
            pointers[1].moveBy(
                Duration(milliseconds = 100),
                beyondSlop * 1,
                beyondSlop * -1
            )
        pointers[2] =
            pointers[2].moveBy(
                Duration(milliseconds = 100),
                0f,
                beyondSlop * 2
            )
        filter::onPointerInput.invokeOverAllPasses(*pointers)

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerInputChanges_2Points1MoveJustBeyondSlop_onDragSlopExceededNotCalled() {

        // Arrange

        val beyondSlop = TestTouchSlop + TinyNum

        var pointer1 = down(0)
        var pointer2 = down(1)
        filter::onPointerInput.invokeOverAllPasses(pointer1, pointer2)

        // Act

        // These movements average to no movement.

        pointer1 =
            pointer1.moveBy(
                Duration(milliseconds = 100),
                0f,
                0f
            )
        pointer2 =
            pointer2.moveBy(
                Duration(milliseconds = 100),
                beyondSlop * -1,
                0f
            )
        filter::onPointerInput.invokeOverAllPasses(pointer1, pointer2)

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerInputChanges_2Points1MoveJustUnderTwiceSlop_onDragSlopExceededNotCalled() {

        // Arrange

        val beyondSlop = TestTouchSlop + TinyNum

        var pointer1 = down(0)
        var pointer2 = down(1)
        filter::onPointerInput.invokeOverAllPasses(pointer1, pointer2)

        // Act

        // These movements average to no movement.

        pointer1 =
            pointer1.moveBy(
                Duration(milliseconds = 100),
                0f,
                0f
            )
        pointer2 =
            pointer2.moveBy(
                Duration(milliseconds = 100),
                beyondSlop * 2 - 1,
                0f
            )
        filter::onPointerInput.invokeOverAllPasses(pointer1, pointer2)

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onPointerInputChanges_2Points1MoveToTwiceSlop_onDragSlopExceededNotCalled() {

        // Arrange

        val beyondSlop = TestTouchSlop + TinyNum

        var pointer1 = down(0)
        var pointer2 = down(1)
        filter::onPointerInput.invokeOverAllPasses(pointer1, pointer2)

        // Act

        // These movements average to no movement.

        pointer1 =
            pointer1.moveBy(
                Duration(milliseconds = 100),
                0f,
                0f
            )
        pointer2 =
            pointer2.moveBy(
                Duration(milliseconds = 100),
                beyondSlop * 2,
                0f
            )
        filter::onPointerInput.invokeOverAllPasses(pointer1, pointer2)

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerInputChanges_1PointMovesBeyondSlopAndThenManyTimes_onDragSlopExceededCallOnce() {

        // Arrange

        val beyondSlop = TestTouchSlop + TinyNum

        var pointer = down(0)
        filter::onPointerInput.invokeOverAllPasses(pointer)

        // Act

        repeat(5) {
            pointer = pointer.moveBy(100.milliseconds, beyondSlop, beyondSlop)
            filter::onPointerInput.invokeOverAllPasses(pointer)
        }

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerInputChanges_1ModifiedToMoveBeyondSlopBeforePostUp_onDragSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)

        val move = down.moveBy(10.milliseconds, 0f, 0f).consume(dx = beyondSlop)
        filter::onPointerInput.invokeOverPasses(
            listOf(move),
            listOf(
                PointerEventPass.InitialDown,
                PointerEventPass.PreUp,
                PointerEventPass.PreDown,
                PointerEventPass.PostUp
            )
        )

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerInputChanges_1ModedToMoveBeyondSlopBeforePostDown_onDragSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)

        val move = down.moveBy(10.milliseconds, 0f, 0f)
        filter::onPointerInput.invokeOverPasses(
            listOf(move),
            listOf(
                PointerEventPass.InitialDown,
                PointerEventPass.PreUp,
                PointerEventPass.PreDown,
                PointerEventPass.PostUp
            )
        )

        val moveConsumed = move.consume(dx = beyondSlop)
        filter::onPointerInput.invokeOverPasses(
            moveConsumed,
            PointerEventPass.PostDown
        )

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerInputChanges_moveUnderToPostUpThenModOverToPostDown_onDragSlopExceededCallOnce() {
        val halfSlop = TestTouchSlop / 2
        val restOfSlopAndBeyond = TestTouchSlop - halfSlop + TinyNum

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)

        val move = down.moveBy(10.milliseconds, halfSlop.toFloat(), 0f)
        filter::onPointerInput.invokeOverPasses(
            listOf(move),
            listOf(
                PointerEventPass.InitialDown,
                PointerEventPass.PreUp,
                PointerEventPass.PreDown,
                PointerEventPass.PostUp
            )
        )

        val moveConsumed = move.consume(dx = -restOfSlopAndBeyond)
        filter::onPointerInput.invokeOverPasses(
            moveConsumed,
            PointerEventPass.PostDown
        )

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    @Test
    fun onPointerInputChanges_moveBeyondSlopAllPassesUpToPostUp_onDragSlopExceededCallOnce() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)

        val move = down.moveBy(10.milliseconds, beyondSlop, 0f)
        filter::onPointerInput.invokeOverPasses(
            listOf(move),
            listOf(
                PointerEventPass.InitialDown,
                PointerEventPass.PreUp,
                PointerEventPass.PreDown,
                PointerEventPass.PostUp
            )
        )

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(1)
    }

    // Verification that TouchSlopExceededGestureDetector does not consume any changes.

    @Test
    fun onPointerInputChanges_1Down_nothingConsumed() {

        val result = filter::onPointerInput.invokeOverAllPasses(down(0))

        // Assert

        assertThat(result.consumed.downChange).isFalse()
        assertThat(result.consumed.positionChange.x).isEqualTo(0f)
        assertThat(result.consumed.positionChange.y).isEqualTo(0f)
    }

    @Test
    fun onPointerInputChanges_1MoveUnderSlop_nothingConsumed() {

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)

        val move = down.moveBy(10.milliseconds, TestTouchSlop.toFloat(), TestTouchSlop.toFloat())
        val result = filter::onPointerInput.invokeOverAllPasses(move)

        // Assert

        assertThat(result.consumed.downChange).isFalse()
        assertThat(result.consumed.positionChange.x).isEqualTo(0f)
        assertThat(result.consumed.positionChange.y).isEqualTo(0f)
    }

    @Test
    fun onPointerInputChanges_1MoveUnderSlopThenUp_nothingConsumed() {

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)

        val move = down.moveBy(10.milliseconds, TestTouchSlop.toFloat(), TestTouchSlop.toFloat())
        filter::onPointerInput.invokeOverAllPasses(move)

        val up = move.up(20.milliseconds)
        val result = filter::onPointerInput.invokeOverAllPasses(up)

        // Assert

        assertThat(result.consumed.downChange).isFalse()
        assertThat(result.consumed.positionChange.x).isEqualTo(0f)
        assertThat(result.consumed.positionChange.y).isEqualTo(0f)
    }

    @Test
    fun onPointerInputChanges_1MoveOverSlop_nothingConsumed() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)

        val move = down.moveBy(10.milliseconds, beyondSlop, beyondSlop)
        val result = filter::onPointerInput.invokeOverAllPasses(move)

        // Assert

        assertThat(result.consumed.downChange).isFalse()
        assertThat(result.consumed.positionChange.x).isEqualTo(0f)
        assertThat(result.consumed.positionChange.y).isEqualTo(0f)
    }

    @Test
    fun onPointerInputChanges_1MoveOverSlopThenUp_nothingConsumed() {
        val beyondSlop = TestTouchSlop + TinyNum

        val down = down(0)
        filter::onPointerInput.invokeOverAllPasses(down)

        val move = down.moveBy(10.milliseconds, beyondSlop, beyondSlop)
        filter::onPointerInput.invokeOverAllPasses(move)

        val up = move.up(20.milliseconds)
        val result = filter::onPointerInput.invokeOverAllPasses(up)

        // Assert

        assertThat(result.consumed.downChange).isFalse()
        assertThat(result.consumed.positionChange.x).isEqualTo(0f)
        assertThat(result.consumed.positionChange.y).isEqualTo(0f)
    }

    // Verification that TouchSlopExceededGestureDetector resets after up correctly.

    @Test
    fun onPointerInputChanges_MoveBeyondUpDownMoveBeyond_onDragSlopExceededCalledTwice() {
        val beyondSlop = TestTouchSlop + TinyNum

        repeat(2) {
            val down = down(0)
            filter::onPointerInput.invokeOverAllPasses(down)

            val move = down.moveBy(10.milliseconds, beyondSlop, 0f)
            filter::onPointerInput.invokeOverAllPasses(move)

            val up = move.up(20.milliseconds)
            filter::onPointerInput.invokeOverAllPasses(up)
        }

        assertThat(onDragSlopExceededCallCount).isEqualTo(2)
    }

    // Verification that cancellation behavior is correct.

    @Test
    fun onCancel_underSlopCancelUnderSlop_onDragSlopExceededNotCalled() {
        val underSlop = TestTouchSlop - TinyNum

        // Arrange

        var pointer = down(0, 0.milliseconds, 0f, 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer)

        pointer = pointer.moveTo(
            10.milliseconds,
            underSlop,
            0f
        )
        filter::onPointerInput.invokeOverAllPasses(pointer)

        // Act

        filter.onCancel()

        pointer = down(0, 0.milliseconds, 0f, 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer)

        pointer = pointer.moveTo(
            10.milliseconds,
            underSlop,
            0f
        )
        filter::onPointerInput.invokeOverAllPasses(pointer)

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(0)
    }

    @Test
    fun onCancel_pastSlopCancelPastSlop_onScaleSlopExceededCalledTwice() {
        val overSlop = TestTouchSlop + TinyNum

        // Arrange

        var pointer = down(0, 0.milliseconds, 0f, 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer)

        pointer = pointer.moveTo(
            10.milliseconds,
            overSlop,
            0f
        )
        filter::onPointerInput.invokeOverAllPasses(pointer)

        // Act

        filter.onCancel()

        pointer = down(0, 0.milliseconds, 0f, 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer)

        pointer = pointer.moveTo(
            10.milliseconds,
            overSlop,
            0f
        )
        filter::onPointerInput.invokeOverAllPasses(pointer)

        // Assert

        assertThat(onDragSlopExceededCallCount).isEqualTo(2)
    }
}