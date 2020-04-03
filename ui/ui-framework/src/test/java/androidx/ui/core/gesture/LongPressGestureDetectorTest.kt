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

import androidx.ui.core.CustomEventDispatcher
import androidx.ui.core.PointerEventPass
import androidx.ui.core.consumeDownChange
import androidx.ui.testutils.consume
import androidx.ui.testutils.down
import androidx.ui.testutils.invokeOverAllPasses
import androidx.ui.testutils.moveBy
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
import com.nhaarman.mockitokotlin2.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

@kotlinx.coroutines.ObsoleteCoroutinesApi
@RunWith(JUnit4::class)
class LongPressGestureDetectorTest {

    private val LongPressTimeoutMillis = 100.milliseconds
    @Suppress("DEPRECATION")
    private val testContext = kotlinx.coroutines.test.TestCoroutineContext()
    private val onLongPress: (PxPosition) -> Unit = mock()
    private val customEventDispatcher: CustomEventDispatcher = mock()
    private lateinit var mRecognizer: LongPressGestureRecognizer

    @Before
    fun setup() {
        mRecognizer = LongPressGestureRecognizer(testContext)
        mRecognizer.onLongPress = onLongPress
        mRecognizer.longPressTimeout = LongPressTimeoutMillis
        mRecognizer.onInit(customEventDispatcher)
    }

    // Tests that verify conditions under which onLongPress will not be called.

    @Test
    fun onPointerInput_down_eventNotFired() {
        mRecognizer::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerInput_downWithinTimeout_eventNotFired() {
        mRecognizer::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerInput_DownMoveConsumed_eventNotFired() {
        val down = down(0)
        val move = down.moveBy(50.milliseconds, 1f, 1f).consume(1f, 0f)

        mRecognizer::onPointerInput.invokeOverAllPasses(down)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(move)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerInput_2Down1MoveConsumed_eventNotFired() {
        val down0 = down(0)
        val down1 = down(1)
        val move0 = down0.moveBy(50.milliseconds, 1f, 1f).consume(1f, 0f)
        val move1 = down0.moveBy(50.milliseconds, 0f, 0f)

        mRecognizer::onPointerInput.invokeOverAllPasses(down0, down1)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(move0, move1)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerInput_DownUpConsumed_eventNotFired() {
        val down = down(0)
        val up = down.up(50.milliseconds).consumeDownChange()

        mRecognizer::onPointerInput.invokeOverAllPasses(down)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(up)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerInput_DownUpNotConsumed_eventNotFired() {
        val down = down(0)
        val up = down.up(50.milliseconds)

        mRecognizer::onPointerInput.invokeOverAllPasses(down)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(up)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerInput_2DownIndependentlyUnderTimeoutAndDoNotOverlap_eventNotFired() {

        // Arrange

        val down0 = down(0)

        val up0 = down0.up(50.milliseconds)

        val down1 = down(1, 51.milliseconds)

        // Act

        mRecognizer::onPointerInput.invokeOverAllPasses(down0)

        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(up0)

        testContext.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(down1)

        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        // Assert

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onPointerInput_downMoveOutOfBoundsWait_eventNotFired() {
        var pointer = down(0, 0.milliseconds)
        mRecognizer::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 1f, 0f)
        mRecognizer::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    // Tests that verify conditions under which onLongPress will be called.

    @Test
    fun onPointerInput_downBeyondTimeout_eventFiredOnce() {
        mRecognizer::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }

    @Test
    fun onPointerInput_2DownBeyondTimeout_eventFiredOnce() {
        mRecognizer::onPointerInput.invokeOverAllPasses(down(0), down(1))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }

    @Test
    fun onPointerInput_downMoveOutOfBoundsWaitUpThenDownWait_eventFiredOnce() {
        var pointer = down(0, 0.milliseconds)
        mRecognizer::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 1f, 0f)
        mRecognizer::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)
        pointer = pointer.up(105.milliseconds)
        mRecognizer::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        pointer = down(1, 200.milliseconds)
        mRecognizer::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }

    @Test
    fun onPointerInput_2DownIndependentlyUnderTimeoutButOverlapTimeIsOver_eventFiredOnce() {

        // Arrange

        val down0 = down(0)

        val move0 = down0.moveTo(50.milliseconds, 0f, 0f)
        val down1 = down(1, 50.milliseconds)

        val up0 = move0.up(75.milliseconds)
        val move1 = down1.moveTo(75.milliseconds, 0f, 0f)

        // Act

        mRecognizer::onPointerInput.invokeOverAllPasses(
            down0
        )

        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(
            move0, down1
        )

        testContext.advanceTimeBy(25, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(
            up0, move1
        )

        testContext.advanceTimeBy(25, TimeUnit.MILLISECONDS)

        // Assert

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }

    @Test
    fun onPointerInput_downMoveNotConsumed_eventFiredOnce() {
        val down = down(0)
        val move = down.moveBy(50.milliseconds, 1f, 1f)

        mRecognizer::onPointerInput.invokeOverAllPasses(down)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(move)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }

    // Tests that verify correctness of PxPosition value passed to onLongPress

    @Test
    fun onPointerInput_down_onLongPressCalledWithDownPosition() {
        val down = down(0, x = 13f, y = 17f)

        mRecognizer::onPointerInput.invokeOverAllPasses(down)
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(PxPosition(13.px, 17.px))
    }

    @Test
    fun onPointerInput_downMove_onLongPressCalledWithMovePosition() {
        val down = down(0, x = 13f, y = 17f)
        val move = down.moveTo(50.milliseconds, 7f, 5f)

        mRecognizer::onPointerInput.invokeOverAllPasses(down)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(move)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(PxPosition((7).px, 5.px))
    }

    @Test
    fun onPointerInput_downThenDown_onLongPressCalledWithFirstDownPosition() {
        val down0 = down(0, x = 13f, y = 17f)

        val move0 = down0.moveBy(50.milliseconds, 0f, 0f)
        val down1 = down(1, 50.milliseconds, 11f, 19f)

        mRecognizer::onPointerInput.invokeOverAllPasses(down0)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(move0, down1)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(PxPosition(13.px, 17.px))
    }

    @Test
    fun onPointerInput_down0ThenDown1ThenUp0_onLongPressCalledWithDown1Position() {
        val down0 = down(0, x = 13f, y = 17f)

        val move0 = down0.moveTo(50.milliseconds, 27f, 29f)
        val down1 = down(1, 50.milliseconds, 11f, 19f)

        val up0 = move0.up(75.milliseconds)
        val move1 = down1.moveBy(25.milliseconds, 0f, 0f)

        mRecognizer::onPointerInput.invokeOverAllPasses(down0)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(move0, down1)
        testContext.advanceTimeBy(25, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(up0, move1)
        testContext.advanceTimeBy(25, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(PxPosition(11.px, 19.px))
    }

    @Test
    fun onPointerInput_down0ThenMove0AndDown1_onLongPressCalledWithMove0Position() {
        val down0 = down(0, x = 13f, y = 17f)

        val move0 = down0.moveTo(50.milliseconds, 27f, 29f)
        val down1 = down(1, 50.milliseconds, 11f, 19f)

        mRecognizer::onPointerInput.invokeOverAllPasses(down0)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(move0, down1)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(PxPosition(27.px, 29.px))
    }

    @Test
    fun onPointerInput_down0Down1Move1Up0_onLongPressCalledWithMove1Position() {
        val down0 = down(0, x = 13f, y = 17f)

        val move0 = down0.moveBy(25.milliseconds, 0f, 0f)
        val down1 = down(1, 25.milliseconds, 11f, 19f)

        val up0 = move0.up(50.milliseconds)
        val move1 = down1.moveTo(50.milliseconds, 27f, 23f)

        mRecognizer::onPointerInput.invokeOverAllPasses(down0)
        testContext.advanceTimeBy(25, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(move0, down1)
        testContext.advanceTimeBy(25, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(up0, move1)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(PxPosition(27.px, 23.px))
    }

    // Tests that verify that consumption behavior

    @Test
    fun onPointerInput_1Down_notConsumed() {
        val down0 = down(0)
        val result = mRecognizer::onPointerInput.invokeOverAllPasses(
                down0
        )
        assertThat(result.consumed.downChange).isFalse()
    }

    @Test
    fun onPointerInput_1DownThen1Down_notConsumed() {

        // Arrange

        val down0 = down(0, 0.milliseconds)
        mRecognizer::onPointerInput.invokeOverAllPasses(
                down0
        )

        // Act

        testContext.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        val move0 = down0.moveTo(10.milliseconds, 0f, 0f)
        val down1 = down(0, 10.milliseconds)
        val result = mRecognizer::onPointerInput.invokeOverAllPasses(
                move0, down1
        )

        // Assert

        assertThat(result[0].consumed.downChange).isFalse()
        assertThat(result[1].consumed.downChange).isFalse()
    }

    @Test
    fun onPointerInput_1DownUnderTimeUp_upNotConsumed() {

        // Arrange

        val down0 = down(0, 0.milliseconds)
        mRecognizer::onPointerInput.invokeOverAllPasses(
                down0
        )

        // Act

        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        val up0 = down0.up(50.milliseconds)
        val result = mRecognizer::onPointerInput.invokeOverAllPasses(
                up0
        )

        // Assert

        assertThat(result.consumed.downChange).isFalse()
    }

    @Test
    fun onPointerInput_1DownOverTimeUp_upConsumedOnInitialDown() {

        // Arrange

        val down0 = down(0, 0.milliseconds)
        mRecognizer::onPointerInput.invokeOverAllPasses(
                down0
        )

        // Act

        testContext.advanceTimeBy(101, TimeUnit.MILLISECONDS)
        val up0 = down0.up(100.milliseconds)
        val result = mRecognizer.onPointerInput(
            listOf(up0),
            PointerEventPass.InitialDown,
            IntPxSize(0.ipx, 0.ipx)
        )

        // Assert

        assertThat(result[0].consumed.downChange).isTrue()
    }

    @Test
    fun onPointerInput_1DownOverTimeMoveConsumedUp_upNotConsumed() {

        // Arrange

        var pointer = down(0, 0.milliseconds)
        mRecognizer::onPointerInput.invokeOverAllPasses(pointer)
        testContext.advanceTimeBy(50, TimeUnit.MILLISECONDS)
        pointer = pointer.moveTo(50.milliseconds, 5f).consume(1f)
        mRecognizer::onPointerInput.invokeOverAllPasses(pointer)

        // Act

        testContext.advanceTimeBy(51, TimeUnit.MILLISECONDS)
        pointer = pointer.up(100.milliseconds)
        val result = mRecognizer::onPointerInput.invokeOverAllPasses(pointer)

        // Assert

        assertThat(result.consumed.downChange).isFalse()
    }

    // Tests that verify correct behavior around cancellation.

    @Test
    fun onCancel_downCancelBeyondTimeout_eventNotFired() {
        mRecognizer::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        mRecognizer.onCancel()
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onCancel_downAlmostTimeoutCancelTimeout_eventNotFired() {
        mRecognizer::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.onCancel()
        testContext.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onCancel_downCancelDownTimeExpires_eventFiredOnce() {
        mRecognizer::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        testContext.advanceTimeBy(99, TimeUnit.MILLISECONDS)
        mRecognizer.onCancel()
        mRecognizer::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }

    // Verify correct behavior around responding to LongPressFiredEvent

    @Test
    fun onCustomEvent_downCustomEventTimeout_eventNotFired() {
        mRecognizer::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        mRecognizer::onCustomEvent.invokeOverAllPasses(LongPressFiredEvent)
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress, never()).invoke(any())
        verify(customEventDispatcher, never()).dispatchCustomEvent(any())
    }

    @Test
    fun onCustomEvent_downCustomEventTimeoutDownTimeout_eventFiredOnce() {
        mRecognizer::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        mRecognizer::onCustomEvent.invokeOverAllPasses(LongPressFiredEvent)
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)
        mRecognizer::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        testContext.advanceTimeBy(100, TimeUnit.MILLISECONDS)

        verify(onLongPress).invoke(any())
        verify(customEventDispatcher).dispatchCustomEvent(LongPressFiredEvent)
    }
}