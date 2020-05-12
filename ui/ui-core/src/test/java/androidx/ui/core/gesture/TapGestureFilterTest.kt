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
import androidx.ui.core.PointerId
import androidx.ui.core.consumeDownChange
import androidx.ui.core.gesture.customevents.DelayUpEvent
import androidx.ui.core.gesture.customevents.DelayUpMessage
import androidx.ui.testutils.consume
import androidx.ui.testutils.down
import androidx.ui.testutils.invokeOverAllPasses
import androidx.ui.testutils.invokeOverPasses
import androidx.ui.testutils.moveTo
import androidx.ui.testutils.up
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.ipx
import androidx.ui.unit.milliseconds
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TapGestureFilterTest {

    private lateinit var filter: TapGestureFilter

    @Before
    fun setup() {
        filter = TapGestureFilter()
        filter.onTap = mock()
    }

    // Verification for when onReleased should not be called.

    @Test
    fun pointerInputHandler_down_onReleaseNotCalled() {
        filter::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        verify(filter.onTap, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downConsumedUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds).consumeDownChange()
        filter::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer)

        verify(filter.onTap, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveConsumedUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.moveTo(100.milliseconds, 5f).consume(5f)
        filter::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(200.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer)

        verify(filter.onTap, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downUpConsumed_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds).consumeDownChange()
        filter::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds).consumeDownChange()
        filter::onPointerInput.invokeOverAllPasses(pointer)

        verify(filter.onTap, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsNegativeXUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, -1f, 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        verify(filter.onTap, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsPositiveXUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 1f, 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        verify(filter.onTap, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsNegativeYUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 0f, -1f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        verify(filter.onTap, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsPositiveYUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 0f, 1f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        verify(filter.onTap, never()).invoke()
    }

    // Verification for when onReleased should be called.

    @Test
    fun pointerInputHandler_downUp_onReleaseCalledOnce() {
        var pointer = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer)

        verify(filter.onTap).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveUp_onReleaseCalledOnce() {
        var pointer = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.moveTo(100.milliseconds, 5f)
        filter::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(200.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer)

        verify(filter.onTap).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsUpDownUp_onReleaseCalledOnce() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 0f, 1f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = down(1, duration = 150.milliseconds, x = 0f, y = 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(200.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        verify(filter.onTap).invoke()
    }

    // Verification that the down changes should not be consumed.

    @Test
    fun pointerInputHandler_down_downChangeNotConsumed() {
        val pointerEventChange =
            filter::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        assertThat(pointerEventChange.consumed.downChange, `is`(false))
    }

    // Verification for when the up change should not be consumed.

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsNegativeXUp_upChangeNotConsumed() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, -1f, 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        val result =
            filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        assertThat(result.consumed.downChange, `is`(false))
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsPositiveXUp_upChangeNotConsumed() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 1f, 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        val result =
            filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        assertThat(result.consumed.downChange, `is`(false))
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsNegativeYUp_upChangeNotConsumed() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 0f, -1f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        val result =
            filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        assertThat(result.consumed.downChange, `is`(false))
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsPositiveYUp_upChangeNotConsumed() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 0f, 1f)
        filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)

        val result =
            filter::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        assertThat(result.consumed.downChange, `is`(false))
    }

    // Verification for when the up change should be consumed.

    @Test
    fun pointerInputHandler_upChangeConsumed() {
        var pointer = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds)
        val pointerEventChange = filter::onPointerInput.invokeOverAllPasses(pointer)
        assertThat(pointerEventChange.consumed.downChange, `is`(true))
    }

    // Verification for during what pass the changes are consumed.

    @Test
    fun pointerInputHandler_upChangeConsumedDuringPostUp() {
        val pointer = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer)
        var pointerEventChange = pointer.up(100.milliseconds)
        pointerEventChange = filter::onPointerInput.invokeOverPasses(
            pointerEventChange,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown
        )
        assertThat(pointerEventChange.consumed.downChange, `is`(false))

        pointerEventChange = filter::onPointerInput.invokeOverPasses(
            pointerEventChange,
            PointerEventPass.PostUp,
            IntPxSize(0.ipx, 0.ipx)
        )
        assertThat(pointerEventChange.consumed.downChange, `is`(true))
    }

    // Verification of correct cancellation behavior.

    @Test
    fun cancellationHandler_downCancelUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer)
        filter.onCancel()
        pointer = pointer.up(100.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(pointer)

        verify(filter.onTap, never()).invoke()
    }

    // Verification of correct behavior regarding custom messages.

    @Test
    fun onPointerInput_downDelayUpUp_onTapNotCalled() {
        val down = down(0, 0.milliseconds)
        val delayUp = DelayUpEvent(DelayUpMessage.DelayUp, setOf(PointerId(0)))
        val up = down.up(100.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(down)
        filter::onCustomEvent.invokeOverAllPasses(delayUp)

        filter::onPointerInput.invokeOverAllPasses(up)

        verify(filter.onTap, never()).invoke()
    }

    @Test
    fun onPointerInput_downDelayUpUpDelayUpConsumed_onTapNotCalled() {
        val down = down(0, 0.milliseconds)
        val delayUp = DelayUpEvent(DelayUpMessage.DelayUp, setOf(PointerId(0)))
        val up = down.up(100.milliseconds)
        val delayUpNotConsumed =
            DelayUpEvent(DelayUpMessage.DelayedUpConsumed, setOf(PointerId(0)))
        filter::onPointerInput.invokeOverAllPasses(down)
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerInput.invokeOverAllPasses(up)

        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)

        verify(filter.onTap, never()).invoke()
    }

    @Test
    fun onPointerInput_downDelayUpUpDelayUpNotConsumed_onTapCalled() {
        val down = down(0, 0.milliseconds)
        val delayUp = DelayUpEvent(DelayUpMessage.DelayUp, setOf(PointerId(0)))
        val up = down.up(100.milliseconds)
        val delayUpNotConsumed =
            DelayUpEvent(DelayUpMessage.DelayedUpNotConsumed, setOf(PointerId(0)))
        filter::onPointerInput.invokeOverAllPasses(down)
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerInput.invokeOverAllPasses(up)

        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)

        verify(filter.onTap).invoke()
    }

    @Test
    fun onPointerInput_downDelayUpUp_upNotConsumed() {
        val down = down(0, 0.milliseconds)
        val delayUp = DelayUpEvent(DelayUpMessage.DelayUp, setOf(PointerId(0)))
        val up = down.up(100.milliseconds)
        filter::onPointerInput.invokeOverAllPasses(down)
        filter::onCustomEvent.invokeOverAllPasses(delayUp)

        val resultingChange = filter::onPointerInput.invokeOverAllPasses(up)

        assertThat(resultingChange.consumed.downChange, `is`(false))
    }

    @Test
    fun onPointerInput_downDelayUpUpDelayUpNotConsumed_messageChangedToConsumed() {
        val down = down(0, 0.milliseconds)
        val delayUp = DelayUpEvent(DelayUpMessage.DelayUp, setOf(PointerId(0)))
        val up = down.up(100.milliseconds)
        val delayUpNotConsumed =
            DelayUpEvent(DelayUpMessage.DelayedUpNotConsumed, setOf(PointerId(0)))
        filter::onPointerInput.invokeOverAllPasses(down)
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerInput.invokeOverAllPasses(up)

        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)

        assertThat(delayUpNotConsumed.message, `is`(DelayUpMessage.DelayedUpConsumed))
    }

    // Verification of complex tests where some up events are blocked and others are not

    /**
     * 2 Pointers down, first up but blocked, then first is released.  1 pointer still down so
     * onTap should not be called.
     */
    @Test
    fun onPointerInput_2Down1DelayUpAndUpAndDelayUpNotConsumed_onTapNotCalled() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val delayUp =
            DelayUpEvent(DelayUpMessage.DelayUp, setOf(PointerId(123)))

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val delayUpNotConsumed =
            DelayUpEvent(DelayUpMessage.DelayedUpNotConsumed, setOf(PointerId(123)))

        filter::onPointerInput.invokeOverAllPasses(aDown)
        filter::onPointerInput.invokeOverAllPasses(aMove, bDown)
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerInput.invokeOverAllPasses(aUp, bMove)
        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)

        verify(filter.onTap, never()).invoke()
    }

    /**
     * 2 Pointers down, first up but blocked, then first is released, then 2nd goes up.  2nd
     * pointer went up and nothing was blocked, so on tap should be called (and only once).
     */
    @Test
    fun onPointerInput_2Down1DelayUpAndUpAndDelayUpNotConsumed2ndUp_onTapNotCalledOnce() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val delayUp =
            DelayUpEvent(DelayUpMessage.DelayUp, setOf(PointerId(123)))

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val delayUpNotConsumed =
            DelayUpEvent(DelayUpMessage.DelayedUpNotConsumed, setOf(PointerId(123)))

        val bUp = bMove.up(3.milliseconds)

        filter::onPointerInput.invokeOverAllPasses(aDown)
        filter::onPointerInput.invokeOverAllPasses(aMove, bDown)
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerInput.invokeOverAllPasses(aUp, bMove)
        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)
        filter::onPointerInput.invokeOverAllPasses(bUp)

        verify(filter.onTap).invoke()
        verifyNoMoreInteractions(filter.onTap)
    }

    /**
     * 2 Pointers down, first up but blocked, then second is up.  onTap should fire because the
     * last finger to leave was not blocked.
     */
    @Test
    fun onPointerInput_2Down1DelayUpAndUp2ndUp_onTapCalled() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val delayUp =
            DelayUpEvent(DelayUpMessage.DelayUp, setOf(PointerId(123)))

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val bUp = bMove.up(3.milliseconds)

        filter::onPointerInput.invokeOverAllPasses(aDown)
        filter::onPointerInput.invokeOverAllPasses(aMove, bDown)
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerInput.invokeOverAllPasses(aUp, bMove)
        filter::onPointerInput.invokeOverAllPasses(bUp)

        verify(filter.onTap).invoke()
        verifyNoMoreInteractions(filter.onTap)
    }

    /**
     * 2 Pointers down, first up but blocked, then second is up, then first is unblocked.  onTap
     * should have already fired with the second went up, so we shouldn't fire again.
     */
    @Test
    fun onPointerInput_2Down1DelayUpAndUp2ndUp1stDelayUpNotConsumed_onTapNotCalledAfterLastCall() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val delayUp =
            DelayUpEvent(DelayUpMessage.DelayUp, setOf(PointerId(123)))

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val bUp = bMove.up(3.milliseconds)

        val delayUpNotConsumed =
            DelayUpEvent(DelayUpMessage.DelayedUpNotConsumed, setOf(PointerId(123)))

        filter::onPointerInput.invokeOverAllPasses(aDown)
        filter::onPointerInput.invokeOverAllPasses(aMove, bDown)
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerInput.invokeOverAllPasses(aUp, bMove)
        filter::onPointerInput.invokeOverAllPasses(bUp)
        reset(filter.onTap)
        filter::onCustomEvent.invokeOverAllPasses(delayUpNotConsumed)

        verifyNoMoreInteractions(filter.onTap)
    }

    /**
     * 2 Pointers down, first up, then second up and is blocked. Last to go up was blocked so
     * onTap should not be called.
     */
    @Test
    fun onPointerInput_2Down1stUp2ndDelayUpAndUp_onTapNotCalled() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val delayUp =
            DelayUpEvent(DelayUpMessage.DelayUp, setOf(PointerId(456)))

        val bUp = bMove.up(3.milliseconds)

        filter::onPointerInput.invokeOverAllPasses(aDown)
        filter::onPointerInput.invokeOverAllPasses(aMove, bDown)
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerInput.invokeOverAllPasses(aUp, bMove)
        filter::onCustomEvent.invokeOverAllPasses(delayUp)
        filter::onPointerInput.invokeOverAllPasses(bUp)

        verifyNoMoreInteractions(filter.onTap)
    }

    /**
     * 2 Pointers down, first up, then second up and is blocked, then 2nd released. Last to go up
     * was blocked, and then unblocked, so onTap should be called.
     */
    @Test
    fun onPointerInput_2Down1stUp2ndDelayUpAndUpAndReleased_onTapCalledOnce() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val delayBUp =
            DelayUpEvent(DelayUpMessage.DelayUp, setOf(PointerId(456)))

        val bUp = bMove.up(3.milliseconds)

        val releaseBUp =
            DelayUpEvent(DelayUpMessage.DelayedUpNotConsumed, setOf(PointerId(456)))

        filter::onPointerInput.invokeOverAllPasses(aDown)
        filter::onPointerInput.invokeOverAllPasses(aMove, bDown)
        filter::onPointerInput.invokeOverAllPasses(aUp, bMove)
        filter::onCustomEvent.invokeOverAllPasses(delayBUp)
        filter::onPointerInput.invokeOverAllPasses(bUp)
        filter::onCustomEvent.invokeOverAllPasses(releaseBUp)

        verify(filter.onTap).invoke()
        verifyNoMoreInteractions(filter.onTap)
    }

    // 2 down, then 1 up delayed and up, then same up not consumed.  We are still waiting for the
    // 2nd to go up so onTap should not be called.
    @Test
    fun onPointerInput_2Down1stDelayUpThenUpThenDelayUpNotConsumed_onTapNotCalled() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val delayAUp =
            DelayUpEvent(DelayUpMessage.DelayUp, setOf(PointerId(123)))

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val delayAUpNotConsumed =
            DelayUpEvent(DelayUpMessage.DelayedUpNotConsumed, setOf(PointerId(123)))

        filter::onPointerInput.invokeOverAllPasses(aDown)
        filter::onPointerInput.invokeOverAllPasses(aMove, bDown)
        filter::onCustomEvent.invokeOverAllPasses(delayAUp)
        filter::onPointerInput.invokeOverAllPasses(aUp, bMove)
        filter::onCustomEvent.invokeOverAllPasses(delayAUpNotConsumed)

        verifyNoMoreInteractions(filter.onTap)
    }

    // 2 down, then 1 up delayed and up, then same up not consumed, then 2nd up.  By the time the
    // last pointer went up, the other pointer was delayed up, and then went up, and then was
    // allowed to go up, so by the time the 2nd up happened, we should fire onTap.
    @Test
    fun onPointerInput_2Down1stDelayUpThenUpThenDelayUpNotConsumedThen2ndUp_onTapCalledOnce() {
        val aDown = down(123, 0.milliseconds)

        val aMove = aDown.moveTo(1.milliseconds)
        val bDown = down(456, 1.milliseconds)

        val delayAUp =
            DelayUpEvent(DelayUpMessage.DelayUp, setOf(PointerId(123)))

        val aUp = aMove.up(2.milliseconds)
        val bMove = bDown.moveTo(2.milliseconds)

        val delayAUpNotConsumed =
            DelayUpEvent(DelayUpMessage.DelayedUpNotConsumed, setOf(PointerId(123)))

        val bUp = bMove.up(3.milliseconds)

        filter::onPointerInput.invokeOverAllPasses(aDown)
        filter::onPointerInput.invokeOverAllPasses(aMove, bDown)
        filter::onCustomEvent.invokeOverAllPasses(delayAUp)
        filter::onPointerInput.invokeOverAllPasses(aUp, bMove)
        filter::onCustomEvent.invokeOverAllPasses(delayAUpNotConsumed)
        filter::onPointerInput.invokeOverAllPasses(bUp)

        verify(filter.onTap).invoke()
        verifyNoMoreInteractions(filter.onTap)
    }
}