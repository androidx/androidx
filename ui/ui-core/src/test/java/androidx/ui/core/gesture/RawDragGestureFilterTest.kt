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

import androidx.ui.core.PointerEventPass
import androidx.ui.core.anyPositionChangeConsumed
import androidx.ui.core.consumePositionChange
import androidx.ui.testutils.consume
import androidx.ui.testutils.down
import androidx.ui.testutils.invokeOverAllPasses
import androidx.ui.testutils.invokeOverPasses
import androidx.ui.testutils.moveBy
import androidx.ui.testutils.moveTo
import androidx.ui.testutils.up
import androidx.ui.unit.Duration
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import androidx.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO(shepshapard): Write the following tests.
// Test for cases where things are reset when last pointer goes up
// Verify methods called during PostUp and PostDown
// Verify correct behavior when distance is consumed at different moments between passes.
// Verify correct behavior with no DragBlocker

@RunWith(JUnit4::class)
class RawDragGestureFilterTest {

    private lateinit var filter: RawDragGestureFilter
    private lateinit var dragObserver: MockDragObserver
    private lateinit var log: MutableList<LogItem>
    private var dragStartBlocked = true

    @Before
    fun setup() {
        log = mutableListOf()
        dragObserver = MockDragObserver(log)
        filter = RawDragGestureFilter()
        filter.canStartDragging = { !dragStartBlocked }
        filter.dragObserver = dragObserver
    }

    // Verify the circumstances under which onStart/OnDrag should not be called.

    @Test
    fun onPointerInput_blockedAndMove_onStartAndOnDragNotCalled() {

        val down = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(down)
        val move = down.moveBy(10.milliseconds, 1f, 0f)
        filter::onPointerInput.invokeOverAllPasses(move)

        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    @Test
    fun onPointerInput_unblockedNoMove_onStartAndOnDragNotCalled() {
        val down = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(down)
        dragStartBlocked = false

        val move = down.moveBy(10.milliseconds, 0f, 0f)
        filter::onPointerInput.invokeOverAllPasses(move)

        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    @Test
    fun onPointerInput_unblockedMovementConsumed_onStartAndOnDragNotCalled() {

        val down1 = down(1)
        filter::onPointerInput.invokeOverAllPasses(down1)
        dragStartBlocked = false

        val move1 = down1.moveBy(10.milliseconds, 1f, 1f).consume(dx = 1f, dy = 1f)
        filter::onPointerInput.invokeOverAllPasses(move1)

        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    @Test
    fun onPointerInput_unblockedMovementIsInOppositeDirections_onStartAndOnDragNotCalled() {

        val down1 = down(1)
        val down2 = down(2)
        filter::onPointerInput.invokeOverAllPasses(down1, down2)
        dragStartBlocked = false

        val move1 = down1.moveBy(10.milliseconds, 1f, 1f)
        val move2 = down2.moveBy(10.milliseconds, -1f, -1f)
        filter::onPointerInput.invokeOverAllPasses(move1, move2)

        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    @Test
    fun onPointerInput_3PointsMoveAverage0_onStartAndOnDragNotCalled() {

        // Arrange

        val pointers = arrayOf(down(0), down(1), down(2))
        filter::onPointerInput.invokeOverAllPasses(*pointers)
        dragStartBlocked = false

        // Act

        // These movements average to no movement.
        pointers[0] =
            pointers[0].moveBy(
                100.milliseconds,
                -1f,
                -1f
            )
        pointers[1] =
            pointers[1].moveBy(
                100.milliseconds,
                1f,
                -1f
            )
        pointers[2] =
            pointers[2].moveBy(
                100.milliseconds,
                0f,
                2f
            )
        filter::onPointerInput.invokeOverAllPasses(*pointers)

        // Assert
        assertThat(log.filter { it.methodName == "onStart" }).isEmpty()
        assertThat(log.filter { it.methodName == "onDrag" }).isEmpty()
    }

    // Verify the circumstances under which onStart/OnDrag should be called.

    @Test
    fun onPointerInput_unblockedAndMoveOnX_onStartAndOnDragCalledOnce() {

        val down = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(down)
        dragStartBlocked = false

        val move = down.moveBy(10.milliseconds, 1f, 0f)
        filter::onPointerInput.invokeOverAllPasses(move)

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        // onDrag get's called twice because it is called during PostUp and PostDown and nothing
        // consumed the drag distance.
        assertThat(log.filter { it.methodName == "onDrag" }).hasSize(2)
    }

    @Test
    fun onPointerInput_unblockedAndMoveOnY_oonStartAndOnDragCalledOnce() {

        val down = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(down)
        dragStartBlocked = false

        val move = down.moveBy(10.milliseconds, 0f, 1f)
        filter::onPointerInput.invokeOverAllPasses(move)

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        // onDrag get's called twice because it is called during PostUp and PostDown and nothing
        // consumed the drag distance.
        assertThat(log.filter { it.methodName == "onDrag" }).hasSize(2)
    }

    @Test
    fun onPointerInput_unblockedAndMoveConsumedBeyond0_onStartAndOnDragCalledOnce() {

        val down = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(down)
        dragStartBlocked = false

        val move = down.moveBy(10.milliseconds, 1f, 0f).consume(dx = 2f)
        filter::onPointerInput.invokeOverAllPasses(move)

        assertThat(log.filter { it.methodName == "onStart" }).hasSize(1)
        // onDrag get's called twice because it is called during PostUp and PostDown and nothing
        // consumed the drag distance.
        assertThat(log.filter { it.methodName == "onDrag" }).hasSize(2)
    }

    // onDrag called with correct values verification.

    @Test
    fun onPointerInput_unblockedMove_onDragCalledWithTotalDistance() {
        var change = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)
        dragStartBlocked = false

        change = change.moveBy(100.milliseconds, 5f, -2f)
        filter::onPointerInput.invokeOverAllPasses(change)

        val onDragLog = log.filter { it.methodName == "onDrag" }
        assertThat(onDragLog).hasSize(2)
        // OnDrags get's called twice each time because RawDragGestureDetector calls it on both
        // PostUp and PostDown and the distance is not consumed by PostUp.
        assertThat(onDragLog[0].pxPosition).isEqualTo(PxPosition(5f, -2f))
        assertThat(onDragLog[1].pxPosition).isEqualTo(PxPosition(5f, -2f))
    }

    @Test
    fun onPointerInput_unblockedMoveAndMoveConsumed_onDragCalledWithCorrectDistance() {

        // Arrange

        var change = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)
        dragStartBlocked = false

        // Act

        change = change.moveBy(100.milliseconds, 3f, -5f)
        filter::onPointerInput.invokeOverAllPasses(change)
        change = change.moveBy(100.milliseconds, -3f, 7f)
        filter::onPointerInput.invokeOverAllPasses(change)
        change = change.moveBy(100.milliseconds, 11f, 13f)
            .consumePositionChange(5f, 3f)
        filter::onPointerInput.invokeOverAllPasses(change)
        change = change.moveBy(100.milliseconds, -13f, -11f)
            .consumePositionChange(-3f, -5f)
        filter::onPointerInput.invokeOverAllPasses(change)

        // Assert

        val onDragLog = log.filter { it.methodName == "onDrag" }
        assertThat(onDragLog).hasSize(8)
        // OnDrags get's called twice each time because RawDragGestureDetector calls it on both
        // PostUp and PostDown and the distance is not consumed by PostUp.
        assertThat(onDragLog[0].pxPosition).isEqualTo(PxPosition(3f, -5f))
        assertThat(onDragLog[1].pxPosition).isEqualTo(PxPosition(3f, -5f))
        assertThat(onDragLog[2].pxPosition).isEqualTo(PxPosition(-3f, 7f))
        assertThat(onDragLog[3].pxPosition).isEqualTo(PxPosition(-3f, 7f))
        assertThat(onDragLog[4].pxPosition).isEqualTo(PxPosition(6f, 10f))
        assertThat(onDragLog[5].pxPosition).isEqualTo(PxPosition(6f, 10f))
        assertThat(onDragLog[6].pxPosition).isEqualTo(PxPosition(-10f, -6f))
        assertThat(onDragLog[7].pxPosition).isEqualTo(PxPosition(-10f, -6f))
    }

    @Test
    fun onPointerInput_3Down1Moves_onDragCalledWith3rdOfDistance() {

        // Arrange

        var pointer1 = down(0)
        var pointer2 = down(1)
        var pointer3 = down(2)
        filter::onPointerInput.invokeOverAllPasses(pointer1, pointer2, pointer3)
        dragStartBlocked = false

        pointer1 = pointer1.moveBy(100.milliseconds, 9f, -12f)
        pointer2 = pointer2.moveBy(100.milliseconds, 0f, 0f)
        pointer3 = pointer3.moveBy(100.milliseconds, 0f, 0f)

        // Act

        filter::onPointerInput.invokeOverAllPasses(pointer1, pointer2, pointer3)

        // Assert

        val onDragLog = log.filter { it.methodName == "onDrag" }
        assertThat(onDragLog).hasSize(2)
        // 2 onDrags because RawDragGestureDetector calls onDrag on both PostUp and PostDown and the
        // distance is never consumed.
        assertThat(onDragLog[0].pxPosition).isEqualTo(
            PxPosition(3f, -4f)
        )
        assertThat(onDragLog[1].pxPosition).isEqualTo(
            PxPosition(3f, -4f)
        )
    }

    // onStop not called verification

    @Test
    fun onPointerInput_blockedDownMoveUp_onStopNotCalled() {
        var change = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)
        change = change.moveTo(10.milliseconds, 1f, 1f)
        filter::onPointerInput.invokeOverAllPasses(change)
        change = change.up(20.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)

        assertThat(log.filter { it.methodName == "onStop" }).hasSize(0)
    }

    @Test
    fun onPointerInput_unBlockedDownUp_onStopNotCalled() {
        var change = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)
        dragStartBlocked = false
        change = change.up(20.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)

        assertThat(log.filter { it.methodName == "onStop" }).hasSize(0)
    }

    @Test
    fun onPointerInput_unBlockedDownMoveAverage0Up_onStopNotCalled() {
        var change1 = down(1)
        var change2 = down(2)
        filter::onPointerInput.invokeOverAllPasses(change1, change2)
        dragStartBlocked = false
        change1 = change1.moveBy(10.milliseconds, 1f, 1f)
        change2 = change2.moveBy(10.milliseconds, -1f, -1f)
        filter::onPointerInput.invokeOverAllPasses(change1, change2)
        change1 = change1.up(20.milliseconds)
        change2 = change2.up(20.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change1, change2)
        assertThat(log.filter { it.methodName == "onStop" }).isEmpty()
    }

    // onStop called verification

    @Test
    fun onPointerInput_unblockedDownMoveUp_onStopCalledOnce() {
        var change = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)
        dragStartBlocked = false
        change = change.moveTo(10.milliseconds, 1f, 1f)
        filter::onPointerInput.invokeOverAllPasses(change)
        change = change.up(20.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)

        assertThat(log.filter { it.methodName == "onStop" }).hasSize(1)
    }

    // onStop called with correct values verification

    @Test
    fun onPointerInput_flingBeyondSlop_onStopCalledWithCorrectVelocity() {
        onPointerInput_flingBeyondSlop_onStopCalledWithCorrectVelocity(0f, 1f, 0f, 100f)
        onPointerInput_flingBeyondSlop_onStopCalledWithCorrectVelocity(0f, -1f, 0f, -100f)
        onPointerInput_flingBeyondSlop_onStopCalledWithCorrectVelocity(1f, 0f, 100f, 0f)
        onPointerInput_flingBeyondSlop_onStopCalledWithCorrectVelocity(-1f, 0f, -100f, 0f)

        onPointerInput_flingBeyondSlop_onStopCalledWithCorrectVelocity(1f, 1f, 100f, 100f)
        onPointerInput_flingBeyondSlop_onStopCalledWithCorrectVelocity(-1f, 1f, -100f, 100f)
        onPointerInput_flingBeyondSlop_onStopCalledWithCorrectVelocity(1f, -1f, 100f, -100f)
        onPointerInput_flingBeyondSlop_onStopCalledWithCorrectVelocity(-1f, -1f, -100f, -100f)
    }

    private fun onPointerInput_flingBeyondSlop_onStopCalledWithCorrectVelocity(
        incrementPerMilliX: Float,
        incrementPerMilliY: Float,
        expectedPxPerSecondDx: Float,
        expectedPxPerSecondDy: Float
    ) {
        log.clear()

        var change = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)
        dragStartBlocked = false

        repeat(11) {
            change = change.moveBy(
                10.milliseconds,
                incrementPerMilliX,
                incrementPerMilliY
            )
            filter::onPointerInput.invokeOverAllPasses(change)
        }

        change = change.up(20.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)

        val loggedStops = log.filter { it.methodName == "onStop" }
        assertThat(loggedStops).hasSize(1)
        val velocity = loggedStops[0].pxPosition!!
        assertThat(velocity.x).isWithin(.01f).of(expectedPxPerSecondDx)
        assertThat(velocity.y).isWithin(.01f).of(expectedPxPerSecondDy)
    }

    // Verification that callbacks occur in the correct order

    @Test
    fun onPointerInput_unblockDownMoveUp_callBacksOccurInCorrectOrder() {
        var change = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)
        dragStartBlocked = false

        change = change.moveTo(
            10.milliseconds,
            0f,
            1f
        )
        filter::onPointerInput.invokeOverAllPasses(change)
        change = change.up(20.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)

        assertThat(log).hasSize(4)
        assertThat(log[0].methodName).isEqualTo("onStart")
        // 2 onDrags because RawDragGestureDetector calls onDrag on both PostUp and PostDown and the
        // distance is never consumed.
        assertThat(log[1].methodName).isEqualTo("onDrag")
        assertThat(log[2].methodName).isEqualTo("onDrag")
        assertThat(log[3].methodName).isEqualTo("onStop")
    }

    // Verification about what events are, or aren't consumed.

    @Test
    fun onPointerInput_down_downNotConsumed() {
        val result = filter::onPointerInput.invokeOverAllPasses(down(0))
        assertThat(result.consumed.downChange).isFalse()
    }

    @Test
    fun onPointerInput_blockedDownMove_distanceChangeNotConsumed() {

        var change = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)
        change = change.moveTo(
            10.milliseconds,
            1f,
            0f
        )
        dragObserver.dragConsume = PxPosition(7.ipx, (-11).ipx)
        var result = filter::onPointerInput.invokeOverPasses(
            change,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown,
            PointerEventPass.PostUp
        )
        dragObserver.dragConsume = PxPosition.Origin
        result = filter::onPointerInput.invokeOverPasses(
            result,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown,
            PointerEventPass.PostUp
        )

        assertThat(result.anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun onPointerInput_unblockedDownMoveCallBackDoesNotConsume_distanceChangeNotConsumed() {
        dragObserver.dragConsume = PxPosition.Origin

        var change = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)
        dragStartBlocked = false

        change = change.moveTo(
            10.milliseconds,
            1f,
            1f
        )
        val result = filter::onPointerInput.invokeOverAllPasses(change)

        assertThat(result.anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun onPointerInput_unblockedMoveOccursDefaultOnDrag_distanceChangeNotConsumed() {
        dragObserver.dragConsume = null

        var change = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)
        dragStartBlocked = false

        change = change.moveTo(
            10.milliseconds,
            1f,
            1f
        )
        val result = filter::onPointerInput.invokeOverAllPasses(change)

        assertThat(result.anyPositionChangeConsumed()).isFalse()
    }

    @Test
    fun onPointerInput_moveCallBackConsumes_changeDistanceConsumedByCorrectAmount() {
        var change = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change, IntPxSize(0.ipx, 0.ipx))
        dragStartBlocked = false

        change = change.moveTo(
            10.milliseconds,
            3f,
            -5f
        )
        dragObserver.dragConsume = PxPosition(7.ipx, (-11).ipx)
        var result = filter::onPointerInput.invokeOverPasses(
            change,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown,
            PointerEventPass.PostUp
        )
        dragObserver.dragConsume = PxPosition.Origin
        result = filter::onPointerInput.invokeOverPasses(
            result,
            PointerEventPass.PostDown
        )

        assertThat(result.consumed.positionChange.x).isEqualTo(7f)
        assertThat(result.consumed.positionChange.y).isEqualTo(-11f)
    }

    @Test
    fun onPointerInput_onStopConsumesUp() {
        var change = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)
        dragStartBlocked = false

        change = change.moveTo(
            10.milliseconds,
            1f,
            0f
        )
        filter::onPointerInput.invokeOverAllPasses(change)
        change = change.up(20.milliseconds)
        val result = filter::onPointerInput.invokeOverAllPasses(change)

        assertThat(result.consumed.downChange).isTrue()
    }

    @Test
    fun onPointerInput_move_onStartCalledWithDownPosition() {
        val down = down(0, 0.milliseconds, x = 3f, y = 4f)
        filter::onPointerInput.invokeOverAllPasses(down)
        dragStartBlocked = false

        val move = down.moveBy(Duration(milliseconds = 10), 1f, 0f)
        filter::onPointerInput.invokeOverAllPasses(move)

        assertThat(log.first { it.methodName == "onStart" }.pxPosition)
            .isEqualTo(PxPosition(3f, 4f))
    }

    @Test
    fun onPointerInput_3PointsMove_onStartCalledWithDownPositions() {
        var pointer1 = down(1, x = 1f, y = 2f)
        var pointer2 = down(2, x = 5f, y = 4f)
        var pointer3 = down(3, x = 3f, y = 6f)
        filter::onPointerInput.invokeOverAllPasses(pointer1, pointer2, pointer3)
        dragStartBlocked = false

        pointer1 = pointer1.moveBy(100.milliseconds, 1f, 0f)
        pointer2 = pointer2.moveBy(100.milliseconds, 0f, 0f)
        pointer3 = pointer3.moveBy(100.milliseconds, 0f, 0f)

        // Act

        filter::onPointerInput.invokeOverAllPasses(pointer1, pointer2, pointer3)

        assertThat(log.first { it.methodName == "onStart" }.pxPosition)
            // average position
            .isEqualTo(PxPosition(3f, 4f))
    }

    // Tests that verify when onCancel should not be called.

    @Test
    fun onCancel_downCancel_onCancelNotCalled() {
        val down = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(down)
        dragStartBlocked = false
        filter.onCancel()

        assertThat(log.filter { it.methodName == "onCancel" }).isEmpty()
    }

    @Test
    fun onCancel_blockedDownMoveCancel_onCancelNotCalled() {
        val down = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(down)
        dragStartBlocked = true
        val move = down.moveBy(1.milliseconds, 1f, 0f)
        filter::onPointerInput.invokeOverAllPasses(move)
        filter.onCancel()

        assertThat(log.filter { it.methodName == "onCancel" }).isEmpty()
    }

    // Tests that verify when onCancel should be called.

    @Test
    fun onCancel_downMoveCancel_onCancelCalledOnce() {
        val down = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(down)
        dragStartBlocked = false
        val move = down.moveBy(1.milliseconds, 1f, 0f)
        filter::onPointerInput.invokeOverAllPasses(move)
        filter.onCancel()

        assertThat(log.filter { it.methodName == "onCancel" }).hasSize(1)
    }

    // Tests that cancel behavior is correct.

    @Test
    fun onCancel_downCancelDownMove_startPositionIsSecondDown() {
        val down1 = down(1, x = 3f, y = 5f)
        filter::onPointerInput.invokeOverAllPasses(down1)
        dragStartBlocked = false
        filter.onCancel()

        val down2 = down(2, x = 7f, y = 11f)
        filter::onPointerInput.invokeOverAllPasses(down2)

        val move = down2.moveBy(Duration(milliseconds = 10), 1f, 0f)
        filter::onPointerInput.invokeOverAllPasses(move)

        assertThat(log.first { it.methodName == "onStart" }.pxPosition)
            .isEqualTo(PxPosition(7f, 11f))
    }

    @Test
    fun onCancel_downMoveCancelDownMoveUp_flingIgnoresMoveBeforeCancel() {

        // Act.

        // Down, move, cancel.
        var change = down(0, duration = 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)
        dragStartBlocked = false
        repeat(11) {
            change = change.moveBy(
                10.milliseconds,
                -1f,
                -1f
            )
            filter::onPointerInput.invokeOverAllPasses(change)
        }
        filter.onCancel()

        // Down, Move, Up
        change = down(1, duration = 200.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)
        dragStartBlocked = false
        repeat(11) {
            change = change.moveBy(
                10.milliseconds,
                1f,
                1f
            )
            filter::onPointerInput.invokeOverAllPasses(change)
        }
        change = change.up(310.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(change)

        // Assert.

        // Fling velocity should only take account of the second Down, Move, Up.
        val loggedStops = log.filter { it.methodName == "onStop" }
        assertThat(loggedStops).hasSize(1)
        val velocity = loggedStops[0].pxPosition!!
        assertThat(velocity.x).isWithin(.01f).of(100f)
        assertThat(velocity.y).isWithin(.01f).of(100f)
    }

    data class LogItem(
        val methodName: String,
        val pxPosition: PxPosition? = null
    )

    class MockDragObserver(
        private val log: MutableList<LogItem>,
        var dragConsume: PxPosition? = null
    ) : DragObserver {

        override fun onStart(downPosition: PxPosition) {
            log.add(LogItem("onStart", pxPosition = downPosition))
            super.onStart(downPosition)
        }

        override fun onDrag(dragDistance: PxPosition): PxPosition {
            log.add(LogItem("onDrag", pxPosition = dragDistance))
            return dragConsume ?: super.onDrag(dragDistance)
        }

        override fun onStop(velocity: PxPosition) {
            log.add(LogItem("onStop", pxPosition = velocity))
            super.onStop(velocity)
        }

        override fun onCancel() {
            log.add(LogItem("onCancel", pxPosition = null))
            super.onCancel()
        }
    }
}