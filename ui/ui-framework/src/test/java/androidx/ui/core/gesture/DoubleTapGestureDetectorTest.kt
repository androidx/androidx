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

import androidx.ui.core.consumeDownChange
import androidx.ui.testutils.consume
import androidx.ui.testutils.down
import androidx.ui.testutils.invokeOverAllPasses
import androidx.ui.testutils.moveTo
import androidx.ui.testutils.up
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import androidx.ui.unit.milliseconds
import androidx.ui.unit.px
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

// TODO(shepshapard): Add more tests for:
//  1. More complex multi-pointer scenarios testing how consumption affects firing events
//  2. More complex multi-pointer scenarios testing how pointers effect consumption

@kotlinx.coroutines.ObsoleteCoroutinesApi
@RunWith(JUnit4::class)
class DoubleTapGestureDetectorTest {

    private val DoubleTapTimeoutMillis = 100.milliseconds
    @Suppress("DEPRECATION")
    private val testContext = kotlinx.coroutines.test.TestCoroutineContext()
    private val onDoubleTap: (PxPosition) -> Unit = mock()
    private lateinit var mRecognizer: DoubleTapGestureRecognizer

    @Before
    fun setup() {
        mRecognizer = DoubleTapGestureRecognizer(testContext)
        mRecognizer.onDoubleTap = onDoubleTap
        mRecognizer.doubleTapTimeout = DoubleTapTimeoutMillis
    }

    // Tests that verify conditions under which onDoubleTap will not be called.

    @Test
    fun pointerInputHandler_down_onDoubleTapNotCalled() {
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down(0, 0.milliseconds))
        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUp_onDoubleTapNotCalled() {
        val down = down(0, 0.milliseconds)
        val up = down.up(duration = 1.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownWithinTimeout_onDoubleTapNotCalled() {
        val down1 = down(1, 0.milliseconds)
        val up = down1.up(duration = 1.milliseconds)
        val down2 = down(2, 100.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownOutsideTimeout_onDoubleTapNotCalled() {
        val down1 = down(1, 0.milliseconds)
        val up = down1.up(duration = 1.milliseconds)
        val down2 = down(2, 101.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up)
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownOutsideTimeoutUp_onDoubleTapNotCalled() {
        val down1 = down(1, 0.milliseconds)
        val up1 = down1.up(duration = 1.milliseconds)
        val down2 = down(2, 101.milliseconds)
        val up2 = down2.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downMoveConsumedUpDownInsideTimeoutUp_onDoubleTapNotCalled() {
        val down1 = down(1, 0.milliseconds)
        val moveConsumed = down1.moveTo(1.milliseconds, x = 1f).consume(dx = 1f)
        val up1 = moveConsumed.up(duration = 2.milliseconds)
        val down2 = down(2, 101.milliseconds)
        val up2 = down2.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(moveConsumed)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownInsideTimeoutMoveConsumedUp_onDoubleTapNotCalled() {
        val down1 = down(1, 0.milliseconds)
        val up1 = down1.up(duration = 1.milliseconds)
        val down2 = down(2, 100.milliseconds)
        val moveConsumed = down2.moveTo(101.milliseconds, x = 1f).consume(dx = 1f)
        val up2 = moveConsumed.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(moveConsumed)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_2Down1MoveConsumedUpDownInsideTimeoutUp_onDoubleTapNotCalled() {
        val down1A = down(0, 0.milliseconds)
        val down1B = down(1, 0.milliseconds)
        val moveConsumed1A = down1A.moveTo(1.milliseconds, x = 1f).consume(dx = 1f)
        val move1B = down1B.moveTo(1.milliseconds)
        val up1A = moveConsumed1A.up(duration = 2.milliseconds)
        val up1B = move1B.up(duration = 2.milliseconds)
        val down2 = down(2, 101.milliseconds)
        val up2 = down2.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1A, down1B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(moveConsumed1A, move1B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1A, up1B)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUp2DownInsideTimeout1MoveConsumedUp_onDoubleTapNotCalled() {
        val down1 = down(1, 0.milliseconds)
        val up2 = down1.up(duration = 1.milliseconds)
        val down2A = down(0, 100.milliseconds)
        val down2B = down(1, 100.milliseconds)
        val moveConsumed2A = down2A.moveTo(101.milliseconds, x = 1f).consume(dx = 1f)
        val move2B = down2B.moveTo(101.milliseconds)
        val up2A = moveConsumed2A.up(duration = 102.milliseconds)
        val up2B = move2B.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2A, down2B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(moveConsumed2A, move2B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2A, up2B)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downConsumedUpDownWithinTimeoutUp_onDoubleTapNotCalled() {
        val down1 = down(1, 0.milliseconds).consumeDownChange()
        val up1 = down1.up(duration = 1.milliseconds)
        val down2 = down(0, 100.milliseconds)
        val up2 = down2.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpConsumedDownWithinTimeoutUp_onDoubleTapNotCalled() {
        val down1 = down(1, 0.milliseconds)
        val up1 = down1.up(duration = 1.milliseconds).consumeDownChange()
        val down2 = down(0, 100.milliseconds)
        val up2 = down2.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownConsumedWithinTimeoutUp_onDoubleTapNotCalled() {
        val down1 = down(1, 0.milliseconds)
        val up1 = down1.up(duration = 1.milliseconds)
        val down2 = down(0, 100.milliseconds).consumeDownChange()
        val up2 = down2.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownWithinTimeoutUpConsumed_onDoubleTapNotCalled() {
        val down1 = down(1, 0.milliseconds)
        val up1 = down1.up(duration = 1.milliseconds)
        val down2 = down(0, 100.milliseconds)
        val up2 = down2.up(duration = 102.milliseconds).consumeDownChange()

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_2down1Up1DownWithinTimeout1Up_onDoubleTapNotCalled() {
        val down1A = down(0, 0.milliseconds)
        val down1B = down(1, 0.milliseconds)
        val move1A1 = down1A.moveTo(2.milliseconds)
        val up2B = down1B.up(duration = 2.milliseconds)
        val move1A2 = move1A1.moveTo(101.milliseconds)
        val down2 = down(1, 101.milliseconds)
        val move1A3 = move1A2.moveTo(102.milliseconds)
        val up2 = down2.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1A, down1B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move1A1, up2B)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move1A2, down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move1A3, up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_1down1Up2DownWithinTimeout1Up_onDoubleTapNotCalled() {
        val down1 = down(0, 0.milliseconds)
        val up1 = down1.up(duration = 1.milliseconds)
        val down2A = down(0, 100.milliseconds)
        val down2B = down(1, 100.milliseconds)
        val move2A = down2A.moveTo(101.milliseconds)
        val up2B = down2B.up(duration = 101.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2A, down2B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move2A, up2B)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downMoveOutOfBoundsUpDownUp_onDoubleTapNotCalled() {
        val down = down(0, 0.milliseconds, 0f, 0f)
        val move = down.moveTo(1.milliseconds, 1f, 1f)
        val up = move.up(duration = 12.milliseconds)
        val down2 = down(0, 13.milliseconds, 0f, 0f)
        val up2 = down2.up(duration = 14.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2, IntPxSize(1.ipx, 1.ipx))

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownMoveOutOfBoundsUp_onDoubleTapNotCalled() {
        val down = down(0, 0.milliseconds, 0f, 0f)
        val up = down.up(duration = 1.milliseconds)
        val down2 = down(0, 2.milliseconds, 0f, 0f)
        val move2 = down2.moveTo(3.milliseconds, 1f, 1f)
        val up2 = down2.up(duration = 4.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move2, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2, IntPxSize(1.ipx, 1.ipx))

        verify(onDoubleTap, never()).invoke(any())
    }

    // Tests that verify conditions under which onDoubleTap will be called.

    @Test
    fun pointerInputHandler_downUpDownInsideTimeoutUp_onDoubleTapCalled() {
        val down1 = down(1, 0.milliseconds)
        val up1 = down1.up(duration = 1.milliseconds)
        val down2 = down(2, 100.milliseconds)
        val up2 = down2.up(duration = 101.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun pointerInputHandler_downMoveUpDownInsideTimeoutUp_onDoubleTapCalled() {
        val down1 = down(1, 0.milliseconds)
        val move = down1.moveTo(1.milliseconds, x = 1f)
        val up1 = move.up(duration = 2.milliseconds)
        val down2 = down(2, 101.milliseconds)
        val up2 = down2.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownInsideTimeoutMoveUp_onDoubleTapCalled() {
        val down1 = down(1, 0.milliseconds)
        val up1 = down1.up(duration = 1.milliseconds)
        val down2 = down(2, 10.milliseconds)
        val move = down2.moveTo(101.milliseconds, x = 1f)
        val up2 = move.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun pointerInputHandler_2Down1MoveUpDownInsideTimeoutUp_onDoubleTapCalled() {
        val down1A = down(0, 0.milliseconds)
        val down1B = down(1, 0.milliseconds)
        val move1A = down1A.moveTo(1.milliseconds, x = 1f)
        val move1B = down1B.moveTo(1.milliseconds)
        val up1A = move1A.up(duration = 2.milliseconds)
        val up1B = move1B.up(duration = 2.milliseconds)
        val down2 = down(2, 101.milliseconds)
        val up2 = down2.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1A, down1B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move1A, move1B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1A, up1B)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUp2DownInsideTimeout1MoveUp_onDoubleTapCalled() {
        val down1 = down(1, 0.milliseconds)
        val up1 = down1.up(duration = 1.milliseconds)
        val down2A = down(0, 100.milliseconds)
        val down2B = down(1, 100.milliseconds)
        val move2A = down2A.moveTo(101.milliseconds, x = 1f)
        val move2B = down2B.moveTo(101.milliseconds)
        val up2A = move2A.up(duration = 102.milliseconds)
        val up2B = move2B.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2A, down2B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move2A, move2B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2A, up2B)

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun pointerInputHandler_downMoveOutOfBoundsUpDownUpDownUp_onDoubleTapCalledOnce() {
        val down = down(0, 0.milliseconds, 0f, 0f)
        val move = down.moveTo(1.milliseconds, 1f, 1f)
        val up = move.up(duration = 2.milliseconds)
        val down2 = down(0, 3.milliseconds, 0f, 0f)
        val up2 = down2.up(duration = 4.milliseconds)
        val down3 = down(0, 5.milliseconds, 0f, 0f)
        val up3 = down3.up(6.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up, IntPxSize(1.ipx, 1.ipx))

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down3, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up3, IntPxSize(1.ipx, 1.ipx))

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownMoveOutOfBoundsUpDownUpDownUp_onDoubleTapCalledOnce() {
        val down = down(0, 0.milliseconds, 0f, 0f)
        val up = down.up(duration = 2.milliseconds)
        val down2 = down(0, 3.milliseconds, 0f, 0f)
        val move2 = down2.moveTo(1.milliseconds, 1f, 1f)
        val up2 = move2.up(duration = 4.milliseconds)
        val down3 = down(0, 5.milliseconds, 0f, 0f)
        val up3 = down3.up(6.milliseconds)
        val down4 = down(0, 7.milliseconds, 0f, 0f)
        val up4 = down4.up(8.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move2, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2, IntPxSize(1.ipx, 1.ipx))

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down3, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up3, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down4, IntPxSize(1.ipx, 1.ipx))
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up4, IntPxSize(1.ipx, 1.ipx))

        verify(onDoubleTap).invoke(any())
    }

    // This test verifies that the 2nd down causes the double tap time out timer to stop such that
    // the second wait doesn't cause the gesture detector to reset to an idle state.
    @Test
    fun pointerInputHandler_downUpWaitHalfDownWaitHalfUp_onDoubleTapCalled() {
        val down1 = down(1, 0.milliseconds)
        val up1 = down1.up(duration = 1.milliseconds)
        val wait1 = 50L
        val down2 = down(2, 51.milliseconds)
        val wait2 = 50L
        val up2 = down2.up(duration = 101.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(wait1, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        testContext.advanceTimeBy(wait2, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(any())
    }

    // Tests that verify correctness of PxPosition value passed to onDoubleTap

    @Test
    fun pointerInputHandler_downUpDownUpAllAtOrigin_onDoubleTapCalledWithOrigin() {
        val down1 = down(1, 0.milliseconds)
        val up1 = down1.up(duration = 1.milliseconds)
        val down2 = down(2, 100.milliseconds)
        val up2 = down2.up(duration = 101.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(PxPosition.Origin)
    }

    @Test
    fun pointerInputHandler_downUpDownMoveUp_onDoubleTapCalledWithFinalMovePosition() {
        val down1 = down(1, 0.milliseconds)
        val up1 = down1.up(duration = 1.milliseconds)
        val down2 = down(2, 100.milliseconds)
        val move2 = down2.moveTo(101.milliseconds, 3f, 5f)
        val up2 = move2.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(PxPosition(3.px, 5.px))
    }

    @Test
    fun pointerInputHandler_downUp2Down2Move1UpThen1Up_onDoubleTapCalledWithFinalFingerPosition() {
        val down1 = down(1, 0.milliseconds)
        val up1 = down1.up(duration = 1.milliseconds)
        val down2A = down(0, 100.milliseconds)
        val down2B = down(1, 100.milliseconds)
        val move2A = down2A.moveTo(101.milliseconds, 3f, 5f)
        val move2B1 = down2B.moveTo(101.milliseconds, 7f, 11f)
        val up2A = move2A.up(duration = 102.milliseconds)
        val move2B2 = move2B1.moveTo(102.milliseconds, x = 7f, y = 11f)
        val up2B = move2B2.up(duration = 103.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2A, down2B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move2A, move2B1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2A, move2B2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2B)

        verify(onDoubleTap).invoke(PxPosition((7).px, (11).px))
    }

    // Tests that verify correct consumption behavior

    @Test
    fun pointerInputHandler_down_downNotConsumed() {
        val down = down(0, 0.milliseconds)
        val result = mRecognizer.pointerInputHandler.invokeOverAllPasses(down)
        assertThat(result.consumed.downChange).isFalse()
    }

    @Test
    fun pointerInputHandler_downUp_upNotConsumed() {
        val down = down(0, 0.milliseconds)
        val up = down.up(1.milliseconds)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down)
        val result = mRecognizer.pointerInputHandler.invokeOverAllPasses(up)
        assertThat(result.consumed.downChange).isFalse()
    }

    @Test
    fun pointerInputHandler_downUpDownInsideTimeout_lastDownNotConsumed() {
        val down = down(0, 0.milliseconds)
        val up = down.up(1.milliseconds)
        val down2 = down(2, 100.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        val result = mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)

        assertThat(result.consumed.downChange).isFalse()
    }

    @Test
    fun pointerInputHandler_downUpDownOutsideTimeoutUp_lastUpNotConsumed() {
        val down = down(0, 0.milliseconds)
        val up = down.up(1.milliseconds)
        val down2 = down(2, 101.milliseconds)
        val up2 = down2.up(duration = 102.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up)
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        val result = mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        assertThat(result.consumed.downChange).isFalse()
    }

    @Test
    fun pointerInputHandler_downUpDownInsideTimeoutUp_lastUpConsumed() {
        val down = down(0, 0.milliseconds)
        val up = down.up(1.milliseconds)
        val down2 = down(2, 100.milliseconds)
        val up2 = down2.up(duration = 101.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        val result = mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        assertThat(result.consumed.downChange).isTrue()
    }

    // Tests that verify correct cancellation behavior

    @Test
    fun cancelHandler_downUpCancelWaitDownUp_onDoubleTapNotCalled() {
        val down1 = down(0, duration = 100.milliseconds)
        val up1 = down1.up(duration = 101.milliseconds)
        val down2 = down(1, duration = 200.milliseconds)
        val up2 = down2.up(duration = 201.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        mRecognizer.cancelHandler()
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun cancelHandler_downUpWaitCancelDownUp_onDoubleTapNotCalled() {
        val down1 = down(1, 100.milliseconds)
        val up1 = down1.up(duration = 101.milliseconds)
        val down2 = down(2, 200.milliseconds)
        val up2 = down2.up(duration = 201.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.cancelHandler()
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun cancelHandler_cancelDownUpDownUp_onDoubleTapCalledOnce() {
        val down1 = down(0, duration = 100.milliseconds)
        val up1 = down1.up(duration = 101.milliseconds)
        val down2 = down(1, duration = 200.milliseconds)
        val up2 = down2.up(duration = 201.milliseconds)

        mRecognizer.cancelHandler()
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun cancelHandler_downCancelDownUpDownUp_onDoubleTapCalledOnce() {
        val down0 = down(0, duration = 99.milliseconds)
        val down1 = down(1, duration = 100.milliseconds)
        val up1 = down1.up(duration = 101.milliseconds)
        val down2 = down(2, duration = 200.milliseconds)
        val up2 = down2.up(duration = 201.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down0)
        mRecognizer.cancelHandler()
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun cancelHandler_downUpCancelDownUpDownUp_onDoubleTapCalledOnce() {
        val down0 = down(0, duration = 98.milliseconds)
        val up0 = down0.up(duration = 99.milliseconds)
        val down1 = down(1, duration = 100.milliseconds)
        val up1 = down1.up(duration = 101.milliseconds)
        val down2 = down(2, duration = 200.milliseconds)
        val up2 = down2.up(duration = 201.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down0)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up0)
        mRecognizer.cancelHandler()
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun cancelHandler_downUpDownCancelDownUpDownUp_onDoubleTapCalledOnce() {
        val down0 = down(0, duration = 97.milliseconds)
        val up0 = down0.up(duration = 98.milliseconds)
        val down1 = down(1, 99.milliseconds)
        val down2 = down(2, 100.milliseconds)
        val up2 = down2.up(duration = 101.milliseconds)
        val down3 = down(3, 200.milliseconds)
        val up3 = down3.up(duration = 201.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down0)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up0)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.cancelHandler()
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down3)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up3)

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun cancelHandler_downUpDownUpCancelDownUpDownUp_onDoubleTapCalledTwice() {
        val down0 = down(0, 0.milliseconds)
        val up0 = down0.up(duration = 1.milliseconds)
        val down1 = down(1, 100.milliseconds)
        val up1 = down1.up(duration = 101.milliseconds)

        val down2 = down(2, 200.milliseconds)
        val up2 = down2.up(duration = 201.milliseconds)
        val down3 = down(3, 300.milliseconds)
        val up3 = down3.up(duration = 301.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down0)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up0)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        mRecognizer.cancelHandler()
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down3)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up3)

        verify(onDoubleTap, times(2)).invoke(any())
    }

    // This test verifies that the cancel event causes the double tap timer to be reset.  If it does
    // not cause it to be reset, then when delay1 is dispatched, the DoubleTapGestureDetector will
    // be forced back into the IDLE state, preventing the double tap that follows cancel from
    // firing.
    @Test
    fun cancelHandler_downUpWaitCancelDownWaitUpDownUp_onDoubleTapCalledOnce() {
        val down0 = down(0, 0.milliseconds)
        val up0 = down0.up(duration = 1.milliseconds)
        val delay0 = 50L
        // Cancel happens here
        val down1 = down(1, 51.milliseconds)
        val delay1 = 50L
        val up1 = down1.up(duration = 101.milliseconds)
        val down2 = down(2, 102.milliseconds)
        val up2 = down2.up(duration = 103.milliseconds)

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down0)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up0)
        testContext.advanceTimeBy(delay0, TimeUnit.MILLISECONDS)
        mRecognizer.cancelHandler()
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        testContext.advanceTimeBy(delay1, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(any())
    }
}