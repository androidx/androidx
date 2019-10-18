/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import androidx.ui.core.IntPxSize
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PxPosition
import androidx.ui.core.consumeDownChange
import androidx.ui.core.ipx
import androidx.ui.core.milliseconds
import androidx.ui.core.millisecondsToTimestamp
import androidx.ui.core.px
import androidx.ui.testutils.consume
import androidx.ui.testutils.down
import androidx.ui.testutils.invokeOverAllPasses
import androidx.ui.testutils.invokeOverPasses
import androidx.ui.testutils.moveBy
import androidx.ui.testutils.moveTo
import androidx.ui.testutils.up
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO(shepshapard): Write tests that verify consumption behavior that blocks ancestors and descendants.

@RunWith(JUnit4::class)
class PressIndicatorGestureDetectorTest {

    private lateinit var recognizer: PressIndicatorGestureRecognizer

    @Before
    fun setup() {
        recognizer = PressIndicatorGestureRecognizer()
        recognizer.onStart = mock()
        recognizer.onStop = mock()
        recognizer.onCancel = mock()
    }

    // Verification of scenarios where onStart should not be called.

    @Test
    fun pointerInputHandler_downConsumed_onStartNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down().consumeDownChange()))
        verify(recognizer.onStart!!, never()).invoke(any())
    }

    // Verification of scenarios where onStart should be called once.

    @Test
    fun pointerInputHandler_down_onStartCalledOnce() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down()))
        verify(recognizer.onStart!!).invoke(any())
    }

    @Test
    fun pointerInputHandler_downDown_onStartCalledOnce() {
        var pointer0 = down(0)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer0))
        pointer0 = pointer0.moveTo(timestamp = 1L.millisecondsToTimestamp())
        val pointer1 = down(1, timestamp = 1L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer0, pointer1))

        verify(recognizer.onStart!!).invoke(any())
    }

    @Test
    fun pointerInputHandler_2Down1Up1Down_onStartCalledOnce() {
        var pointer0 = down(0)
        var pointer1 = down(1)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer0, pointer1))
        pointer0 = pointer0.up(100L.millisecondsToTimestamp())
        pointer1 = pointer1.moveTo(100L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer0, pointer1))
        pointer0 = down(0, 200L.millisecondsToTimestamp())
        pointer1 = pointer1.moveTo(200L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer0, pointer1))

        verify(recognizer.onStart!!).invoke(any())
    }

    @Test
    fun pointerInputHandler_1DownMoveOutside2ndDown_onStartOnlyCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer0, IntPxSize(5.ipx, 5.ipx))
        pointer0 = pointer0.moveTo(100L.millisecondsToTimestamp(), 10f, 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer0, IntPxSize(5.ipx, 5.ipx))
        pointer0 = pointer0.moveTo(200L.millisecondsToTimestamp())
        val pointer1 = down(1, x = 0f, y = 0f)

        recognizer.pointerInputHandler.invokeOverAllPasses(pointer0, pointer1)

        verify(recognizer.onStart!!).invoke(any())
    }

    // Verification of scenarios where onStop should not be called.

    @Test
    fun pointerInputHandler_downMoveConsumedUp_onStopNotCalled() {
        var pointer = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.moveTo(100L.millisecondsToTimestamp(), 5f).consume(1f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.up(200L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))

        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downConsumedUp_onStopNotCalled() {
        var pointer = down().consumeDownChange()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.up(100L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))

        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_2DownUp_onStopNotCalled() {
        var pointer0 = down(0)
        var pointer1 = down(1)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer0, pointer1))
        pointer0 = pointer0.moveTo(100L.millisecondsToTimestamp())
        pointer1 = pointer1.up(100L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer0, pointer1))

        verify(recognizer.onStop!!, never()).invoke()
    }

    // Verification of scenarios where onStop should be called once.

    @Test
    fun pointerInputHandler_downUp_onStopCalledOnce() {
        var pointer = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.up(100L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))

        verify(recognizer.onStop!!).invoke()
    }

    @Test
    fun pointerInputHandler_downUpConsumed_onStopCalledOnce() {
        var pointer = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.up(100L.millisecondsToTimestamp()).consumeDownChange()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))

        verify(recognizer.onStop!!).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveUp_onStopCalledOnce() {
        var pointer = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.moveTo(100L.millisecondsToTimestamp(), 5f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.up(200L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))

        verify(recognizer.onStop!!).invoke()
    }

    @Test
    fun pointerInputHandler_2Down2Up_onStopCalledOnce() {
        var pointer1 = down(0)
        var pointer2 = down(1)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer1, pointer2))
        pointer1 = pointer1.up(100L.millisecondsToTimestamp())
        pointer2 = pointer2.up(100L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer1, pointer2))

        verify(recognizer.onStop!!).invoke()
    }

    // Verification of scenarios where onCancel should not be called.

    @Test
    fun pointerInputHandler_downConsumedMoveConsumed_onCancelNotCalled() {
        var pointer = down().consumeDownChange()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.moveBy(100.milliseconds, 5f).consume(1f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))

        verify(recognizer.onCancel!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downUp_onCancelNotCalled() {
        var pointer = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.up(100L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))

        verify(recognizer.onCancel!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveUp_onCancelNotCalled() {
        var pointer = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.moveTo(100L.millisecondsToTimestamp(), 5f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.up(100L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))

        verify(recognizer.onCancel!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_2DownOneMoveOutsideOfBounds_onCancelNotCalled() {
        var pointer0 = down(0, x = 0f, y = 0f)
        var pointer1 = down(0, x = 4f, y = 4f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(listOf(pointer0, pointer1), IntPxSize(5.ipx, 5.ipx))
        pointer0 = pointer0.moveTo(100L.millisecondsToTimestamp(), 0f, 0f)
        pointer1 = pointer1.moveTo(100L.millisecondsToTimestamp(), 5f, 4f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(listOf(pointer0, pointer1), IntPxSize(5.ipx, 5.ipx))

        verify(recognizer.onCancel!!, never()).invoke()
    }

    // Verification of scenarios where onCancel should be called once.

    @Test
    fun pointerInputHandler_downMoveConsumed_onCancelNotCalled() {
        var pointer = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.moveBy(100.milliseconds, 5f).consume(1f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveConsumedMoveConsumed_onCancelCalledOnce() {
        var pointer = down()
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.moveBy(100.milliseconds, 5f).consume(1f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))
        pointer = pointer.moveBy(100.milliseconds, 5f).consume(1f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_2Down2MoveConsumed_onCancelCalledOnce() {
        var pointer0 = down(0)
        var pointer1 = down(1)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer0, pointer1))
        pointer0 = pointer0.moveBy(100.milliseconds, 5f).consume(1f)
        pointer1 = pointer1.moveBy(100.milliseconds, 5f).consume(1f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer0, pointer1))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_2Down1MoveConsumedTheOtherMoveConsume_onCancelCalledOnce() {
        var pointer0 = down(0)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer0))
        pointer0 = pointer0.moveTo(100L.millisecondsToTimestamp())
        var pointer1 = down(1, 100L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer0, pointer1))
        pointer0 = pointer0.moveBy(100L.milliseconds, 5f).consume(5f)
        pointer1 = pointer1.moveBy(100L.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer0, pointer1))
        pointer0 = pointer0.moveBy(100L.milliseconds)
        pointer1 = pointer1.moveBy(100L.milliseconds, 5f).consume(5f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(pointer0, pointer1))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_1DownMoveOutsideOfBoundsLeft_onCancelCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 0f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(100L.millisecondsToTimestamp(), -1f, 0f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_1DownMoveOutsideOfBoundsRight_onCancelCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 0f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(100L.millisecondsToTimestamp(), 1f, 0f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_1DownMoveOutsideOfBoundsUp_onCancelCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 0f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(100L.millisecondsToTimestamp(), 0f, -1f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_1DownMoveOutsideOfBoundsDown_onCancelCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 0f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(100L.millisecondsToTimestamp(), 0f, 1f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_2DownBothMoveOutsideOfBounds_onCancelCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 4f)
        var pointer1 = down(1, x = 4f, y = 0f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(listOf(pointer0, pointer1), IntPxSize(5.ipx, 5.ipx))
        pointer0 = pointer0.moveTo(100L.millisecondsToTimestamp(), 0f, 5f)
        pointer1 = pointer1.moveTo(100L.millisecondsToTimestamp(), 5f, 0f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(listOf(pointer0, pointer1), IntPxSize(5.ipx, 5.ipx))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_1DownMoveOutsideBoundsThenInsideThenOutside_onCancelCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 0f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(100L.millisecondsToTimestamp(), 0f, 1f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(200L.millisecondsToTimestamp(), 0f, 0f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(300L.millisecondsToTimestamp(), 0f, 1f)
        recognizer.pointerInputHandler
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_1DownMoveOutsideBoundsUpTwice_onCancelCalledTwice() {
        var time = 0L

        repeat(2) {
            var pointer = down(0, x = 0f, y = 0f)
            recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
            pointer = pointer.moveTo(time.millisecondsToTimestamp(), 0f, 1f)
            recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
            pointer = pointer.up(time.millisecondsToTimestamp())
            recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
            time += 100L
        }

        verify(recognizer.onCancel!!, times(2)).invoke()
    }

    // Verification of correct position returned by onStart.

    @Test
    fun pointerInputHandler_down_downPositionIsCorrect() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down(x = 13f, y = 17f)))
        verify(recognizer.onStart!!).invoke(PxPosition(13.px, 17f.px))
    }

    // Verification of correct consumption behavior.

    @Test
    fun pointerInputHandler_downChangeConsumedDuringPostUp() {
        var pointer = down()
        pointer = recognizer.pointerInputHandler.invokeOverPasses(
            listOf(pointer),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown
        ).first()
        assertThat(pointer.consumed.downChange, `is`(false))

        pointer = recognizer.pointerInputHandler.invoke(
            listOf(pointer),
            PointerEventPass.PostUp,
            IntPxSize(0.ipx, 0.ipx)
        ).first()
        assertThat(pointer.consumed.downChange, `is`(true))
    }
}