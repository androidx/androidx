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

import androidx.ui.core.PxPosition
import androidx.ui.core.consumeDownChange
import androidx.ui.core.milliseconds
import androidx.ui.core.millisecondsToTimestamp
import androidx.ui.core.px
import androidx.ui.testutils.consume
import androidx.ui.testutils.down
import androidx.ui.testutils.invokeOverAllPasses
import androidx.ui.testutils.moveTo
import androidx.ui.testutils.up
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

// TODO(shepshapard): Add more tests for:
//  1. More complex multi-pointer scenarios testing how consumption affects firing events
//  2. More complex multi-pointer scenarios testing how pointers effect consumption

@ObsoleteCoroutinesApi
@RunWith(JUnit4::class)
class DoubleTapGestureDetectorTest {

    private val DoubleTapTimeoutMillis = 100.milliseconds
    private val testContext = TestCoroutineContext()
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
        mRecognizer.pointerInputHandler.invokeOverAllPasses(listOf(down()))
        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUp_onDoubleTapNotCalled() {
        val down = down(timestamp = 0L.millisecondsToTimestamp())
        val up = down.up(timestamp = 1L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownWithinTimeout_onLongPressNotCalled() {
        val down1 = down(timestamp = 0L.millisecondsToTimestamp())
        val up = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2 = down(timestamp = 100L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownOutsideTimeout_onLongPressNotCalled() {
        val down1 = down(timestamp = 0L.millisecondsToTimestamp())
        val up = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2 = down(timestamp = 101L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up)
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownOutsideTimeoutUp_onLongPressNotCalled() {
        val down1 = down(timestamp = 0L.millisecondsToTimestamp())
        val up1 = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2 = down(timestamp = 101L.millisecondsToTimestamp())
        val up2 = down2.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downMoveConsumedUpDownInsideTimeoutUp_onLongPressNotCalled() {
        val down1 = down(timestamp = 0L.millisecondsToTimestamp())
        val moveConsumed = down1.moveTo(1L.millisecondsToTimestamp(), x = 1f).consume(dx = 1f)
        val up1 = moveConsumed.up(timestamp = 2L.millisecondsToTimestamp())
        val down2 = down(timestamp = 101L.millisecondsToTimestamp())
        val up2 = down2.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(moveConsumed)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownInsideTimeoutMoveConsumedUp_onLongPressNotCalled() {
        val down1 = down(timestamp = 0L.millisecondsToTimestamp())
        val up1 = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2 = down(timestamp = 100L.millisecondsToTimestamp())
        val moveConsumed = down2.moveTo(101L.millisecondsToTimestamp(), x = 1f).consume(dx = 1f)
        val up2 = moveConsumed.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(moveConsumed)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_2Down1MoveConsumedUpDownInsideTimeoutUp_onLongPressNotCalled() {
        val down1A = down(0, timestamp = 0L.millisecondsToTimestamp())
        val down1B = down(1, timestamp = 0L.millisecondsToTimestamp())
        val moveConsumed1A = down1A.moveTo(1L.millisecondsToTimestamp(), x = 1f).consume(dx = 1f)
        val move1B = down1B.moveTo(1L.millisecondsToTimestamp())
        val up1A = moveConsumed1A.up(timestamp = 2L.millisecondsToTimestamp())
        val up1B = move1B.up(timestamp = 2L.millisecondsToTimestamp())
        val down2 = down(timestamp = 101L.millisecondsToTimestamp())
        val up2 = down2.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1A, down1B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(moveConsumed1A, move1B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1A, up1B)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUp2DownInsideTimeout1MoveConsumedUp_onLongPressNotCalled() {
        val down1 = down(timestamp = 0L.millisecondsToTimestamp())
        val up2 = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2A = down(0, timestamp = 100L.millisecondsToTimestamp())
        val down2B = down(1, timestamp = 100L.millisecondsToTimestamp())
        val moveConsumed2A = down2A.moveTo(101L.millisecondsToTimestamp(), x = 1f).consume(dx = 1f)
        val move2B = down2B.moveTo(101L.millisecondsToTimestamp())
        val up2A = moveConsumed2A.up(timestamp = 102L.millisecondsToTimestamp())
        val up2B = move2B.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2A, down2B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(moveConsumed2A, move2B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2A, up2B)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downConsumedUpDownWithinTimeoutUp_onLongPressNotCalled() {
        val down1 = down(timestamp = 0L.millisecondsToTimestamp()).consumeDownChange()
        val up1 = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2 = down(0, timestamp = 100L.millisecondsToTimestamp())
        val up2 = down2.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpConsumedDownWithinTimeoutUp_onLongPressNotCalled() {
        val down1 = down(timestamp = 0L.millisecondsToTimestamp())
        val up1 = down1.up(timestamp = 1L.millisecondsToTimestamp()).consumeDownChange()
        val down2 = down(0, timestamp = 100L.millisecondsToTimestamp())
        val up2 = down2.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownConsumedWithinTimeoutUp_onLongPressNotCalled() {
        val down1 = down(timestamp = 0L.millisecondsToTimestamp())
        val up1 = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2 = down(0, timestamp = 100L.millisecondsToTimestamp()).consumeDownChange()
        val up2 = down2.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownWithinTimeoutUpConsumed_onLongPressNotCalled() {
        val down1 = down(timestamp = 0L.millisecondsToTimestamp())
        val up1 = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2 = down(0, timestamp = 100L.millisecondsToTimestamp())
        val up2 = down2.up(timestamp = 102L.millisecondsToTimestamp()).consumeDownChange()

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_2down1Up1DownWithinTimeout1Up_onLongPressNotCalled() {
        val down1A = down(0, timestamp = 0L.millisecondsToTimestamp())
        val down1B = down(1, timestamp = 0L.millisecondsToTimestamp())
        val move1A1 = down1A.moveTo(2L.millisecondsToTimestamp())
        val up2B = down1B.up(timestamp = 2L.millisecondsToTimestamp())
        val move1A2 = move1A1.moveTo(101L.millisecondsToTimestamp())
        val down2 = down(id = 1, timestamp = 101L.millisecondsToTimestamp())
        val move1A3 = move1A2.moveTo(102L.millisecondsToTimestamp())
        val up2 = down2.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1A, down1B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move1A1, up2B)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move1A2, down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move1A3, up2)

        verify(onDoubleTap, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_1down1Up2DownWithinTimeout1Up_onLongPressNotCalled() {
        val down1 = down(id = 0, timestamp = 0L.millisecondsToTimestamp())
        val up1 = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2A = down(0, timestamp = 100L.millisecondsToTimestamp())
        val down2B = down(1, timestamp = 100L.millisecondsToTimestamp())
        val move2A = down2A.moveTo(timestamp = 101L.millisecondsToTimestamp())
        val up2B = down2B.up(timestamp = 101L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2A, down2B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move2A, up2B)

        verify(onDoubleTap, never()).invoke(any())
    }

    // Tests that verify conditions under which onDoubleTap will be called.

    @Test
    fun pointerInputHandler_downUpDownInsideTimeoutUp_onLongPressCalled() {
        val down1 = down(timestamp = (0L).millisecondsToTimestamp())
        val up1 = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2 = down(timestamp = 100L.millisecondsToTimestamp())
        val up2 = down2.up(timestamp = 101L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun pointerInputHandler_downMoveUpDownInsideTimeoutUp_onLongPressCalled() {
        val down1 = down(timestamp = (0L).millisecondsToTimestamp())
        val move = down1.moveTo(1L.millisecondsToTimestamp(), x = 1f)
        val up1 = move.up(timestamp = 2L.millisecondsToTimestamp())
        val down2 = down(timestamp = 101L.millisecondsToTimestamp())
        val up2 = down2.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpDownInsideTimeoutMoveUp_onLongPressCalled() {
        val down1 = down(timestamp = (0L).millisecondsToTimestamp())
        val up1 = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2 = down(timestamp = 10L.millisecondsToTimestamp())
        val move = down2.moveTo(101L.millisecondsToTimestamp(), x = 1f)
        val up2 = move.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun pointerInputHandler_2Down1MoveUpDownInsideTimeoutUp_onLongPressCalled() {
        val down1A = down(0, timestamp = 0L.millisecondsToTimestamp())
        val down1B = down(1, timestamp = 0L.millisecondsToTimestamp())
        val move1A = down1A.moveTo(1L.millisecondsToTimestamp(), x = 1f)
        val move1B = down1B.moveTo(1L.millisecondsToTimestamp())
        val up1A = move1A.up(timestamp = 2L.millisecondsToTimestamp())
        val up1B = move1B.up(timestamp = 2L.millisecondsToTimestamp())
        val down2 = down(timestamp = 101L.millisecondsToTimestamp())
        val up2 = down2.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1A, down1B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move1A, move1B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1A, up1B)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUp2DownInsideTimeout1MoveUp_onLongPressCalled() {
        val down1 = down(timestamp = 0L.millisecondsToTimestamp())
        val up1 = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2A = down(0, timestamp = 100L.millisecondsToTimestamp())
        val down2B = down(1, timestamp = 100L.millisecondsToTimestamp())
        val move2A = down2A.moveTo(101L.millisecondsToTimestamp(), x = 1f)
        val move2B = down2B.moveTo(101L.millisecondsToTimestamp())
        val up2A = move2A.up(timestamp = 102L.millisecondsToTimestamp())
        val up2B = move2B.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2A, down2B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move2A, move2B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2A, up2B)

        verify(onDoubleTap).invoke(any())
    }

    // Tests that verify correctness of PxPosition value passed to onDoubleTap

    @Test
    fun pointerInputHandler_downUpDownUpAllAtOrigin_onDoubleTapCalledWithOrigin() {
        val down1 = down(timestamp = (0L).millisecondsToTimestamp())
        val up1 = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2 = down(timestamp = 100L.millisecondsToTimestamp())
        val up2 = down2.up(timestamp = 101L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        verify(onDoubleTap).invoke(PxPosition.Origin)
    }

    @Test
    fun pointerInputHandler_downUpDownMoveUp_onDoubleTapCalledWithFinalMovePosition() {
        val down1 = down(timestamp = (0L).millisecondsToTimestamp())
        val up1 = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2 = down(timestamp = 100L.millisecondsToTimestamp())
        val move2 = down2.moveTo(101L.millisecondsToTimestamp(), 3f, 5f)
        val up2 = move2.up(timestamp = 102L.millisecondsToTimestamp())

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
        val down1 = down(timestamp = (0L).millisecondsToTimestamp())
        val up1 = down1.up(timestamp = 1L.millisecondsToTimestamp())
        val down2A = down(id = 0, timestamp = 100L.millisecondsToTimestamp())
        val down2B = down(id = 1, timestamp = 100L.millisecondsToTimestamp())
        val move2A = down2A.moveTo(101L.millisecondsToTimestamp(), 3f, 5f)
        val move2B1 = down2B.moveTo(101L.millisecondsToTimestamp(), -7f, -11f)
        val up2A = move2A.up(timestamp = 102L.millisecondsToTimestamp())
        val move2B2 = move2B1.moveTo(timestamp = 102L.millisecondsToTimestamp(), x = -7f, y = -11f)
        val up2B = move2B2.up(timestamp = 103L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up1)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2A, down2B)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(move2A, move2B1)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2A, move2B2)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up2B)

        verify(onDoubleTap).invoke(PxPosition((-7).px, (-11).px))
    }

    // Tests that verify that consumption behavior

    @Test
    fun pointerInputHandler_down_downNotConsumed() {
        val down = down()
        val result = mRecognizer.pointerInputHandler.invokeOverAllPasses(down)
        assertThat(result[0].consumed.downChange).isFalse()
    }

    @Test
    fun pointerInputHandler_downUp_upNotConsumed() {
        val down = down()
        val up = down.up(1L.millisecondsToTimestamp())
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down)
        val result = mRecognizer.pointerInputHandler.invokeOverAllPasses(up)
        assertThat(result[0].consumed.downChange).isFalse()
    }

    @Test
    fun pointerInputHandler_downUpDownInsideTimeout_lastDownNotConsumed() {
        val down = down()
        val up = down.up(1L.millisecondsToTimestamp())
        val down2 = down(timestamp = 100L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        val result = mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)

        assertThat(result[0].consumed.downChange).isFalse()
    }

    @Test
    fun pointerInputHandler_downUpDownOutsideTimeoutUp_lastUpNotConsumed() {
        val down = down()
        val up = down.up(1L.millisecondsToTimestamp())
        val down2 = down(timestamp = 101L.millisecondsToTimestamp())
        val up2 = down2.up(timestamp = 102L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up)
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        val result = mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        assertThat(result[0].consumed.downChange).isFalse()
    }

    @Test
    fun pointerInputHandler_downUpDownInsideTimeoutUp_lastUpConsumed() {
        val down = down()
        val up = down.up(1L.millisecondsToTimestamp())
        val down2 = down(timestamp = 100L.millisecondsToTimestamp())
        val up2 = down2.up(timestamp = 101L.millisecondsToTimestamp())

        mRecognizer.pointerInputHandler.invokeOverAllPasses(down)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(up)
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.pointerInputHandler.invokeOverAllPasses(down2)
        val result = mRecognizer.pointerInputHandler.invokeOverAllPasses(up2)

        assertThat(result[0].consumed.downChange).isTrue()
    }
}