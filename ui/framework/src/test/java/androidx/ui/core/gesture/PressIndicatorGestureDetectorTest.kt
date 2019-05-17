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

import androidx.ui.core.PointerEventPass
import androidx.ui.core.PxPosition
import androidx.ui.core.consumeDownChange
import androidx.ui.core.millisecondsToTimestamp
import androidx.ui.core.px
import androidx.ui.testutils.consume
import androidx.ui.testutils.down
import androidx.ui.testutils.invokeOverAllPasses
import androidx.ui.testutils.invokeOverPasses
import androidx.ui.testutils.moveTo
import androidx.ui.testutils.up
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PressIndicatorGestureDetectorTest {

    private lateinit var recognizer: PressIndicatorGestureRecognizer
    private val down = down(0)
    private val downConsumed = down.consumeDownChange()
    private val move = down.moveTo(100L.millisecondsToTimestamp(), x = 100f)
    private val moveConsumed = move.consume(dx = 1f)
    private val up = down.up(100L.millisecondsToTimestamp())
    private val upConsumed = up.consumeDownChange()
    private val upAfterMove = move.up(200L.millisecondsToTimestamp())

    @Before
    fun setup() {
        recognizer = PressIndicatorGestureRecognizer()
        recognizer.onStart = mock()
        recognizer.onStop = mock()
        recognizer.onCancel = mock()
    }

    @Test
    fun pointerInputHandler_down_onStartCalledOnce() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        verify(recognizer.onStart!!).invoke(any())
    }

    @Test
    fun pointerInputHandler_downDown_onStartCalledOnce() {
        val down0 = down(0)
        val down1 = down(1)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down0))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down1))
        verify(recognizer.onStart!!).invoke(any())
    }

    @Test
    fun pointerInputHandler_downDownUpDown_onStartCalledOnce() {
        val down0 = down(0)
        val down1 = down(1)
        val up0 = down1.up(100L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down0))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down1))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(up0))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down0))
        verify(recognizer.onStart!!).invoke(any())
    }

    @Test
    fun pointerInputHandler_downChangeConsumedDuringPostUp() {
        var pointerEventChange = down
        pointerEventChange = recognizer.pointerInputHandler.invokeOverPasses(
            listOf(pointerEventChange),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown).first()
        assertThat(pointerEventChange.consumed.downChange, `is`(false))

        pointerEventChange = recognizer.pointerInputHandler.invoke(
            listOf(pointerEventChange),
            PointerEventPass.PostUp).first()
        assertThat(pointerEventChange.consumed.downChange, `is`(true))
    }

    @Test
    fun pointerInputHandler_downConsumed_onStartNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down.consumeDownChange()))
        verify(recognizer.onStart!!, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUp_onStopCalledOnce() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(up))
        verify(recognizer.onStop!!).invoke()
    }

    @Test
    fun pointerInputHandler_downUpConsumed_onStopCalledOnce() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(upConsumed))
        verify(recognizer.onStop!!).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveUp_onStopCalledOnce() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(move))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(upAfterMove))
        verify(recognizer.onStop!!).invoke()
    }

    @Test
    fun pointerInputHandler_downDownUpUp_onStopCalledOnce() {
        val down0 = down(0)
        val down1 = down(1)
        val up0 = down0.up(100L.millisecondsToTimestamp())
        val up1 = down1.up(100L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down0))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down1))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(up0))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(up1))
        verify(recognizer.onStop!!).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveConsumedUp_onStopNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(moveConsumed))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(upAfterMove))
        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downConsumedUp_onStopNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(downConsumed))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(up))
        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downConsumedMoveConsumed_onStopNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(downConsumed))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(moveConsumed))
        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downDownUp_onStopNotCalled() {
        val down0 = down(0)
        val down1 = down(1)
        val up0 = down0.up(100L.millisecondsToTimestamp())
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down0))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down1))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(up0))
        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveConsumed_onCancelCalledOnce() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(moveConsumed))
        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveConsumedMoveConsumed_onCancelCalledOnce() {
        val down = down(x = 0f)
        val move1 = down.moveTo(timestamp = 100L.millisecondsToTimestamp(), x = 5f)
        val move2 = move1.moveTo(timestamp = 200L.millisecondsToTimestamp(), x = 10f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(move1.consume(dx = 1f)))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(move2.consume(dx = 1f)))
        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_downDownMoveConsumedMoveConsumed_onCancelCalledOnce() {
        val down1 = down(x = 0f)
        val down2 = down(x = 100f)
        val move1 = down1.moveTo(timestamp = 100L.millisecondsToTimestamp(), x = 5f)
        val move2 = down2.moveTo(timestamp = 100L.millisecondsToTimestamp(), x = 105f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down1))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down2))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(move1.consume(dx = 1f)))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(move2.consume(dx = 1f)))
        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_downDownMoveMoveConsumed_onCancelCalledOnce() {
        val down1 = down(x = 0f)
        val down2 = down(x = 100f)
        val move1 = down1.moveTo(timestamp = 100L.millisecondsToTimestamp(), x = 5f)
        val move2 = down2.moveTo(timestamp = 100L.millisecondsToTimestamp(), x = 105f)
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down1))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down2))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(move1))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(move2.consume(dx = 1f)))
        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_downConsumedMoveConsumed_onCancelNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(downConsumed))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(moveConsumed))
        verify(recognizer.onCancel!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downUp_onCancelNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(up))
        verify(recognizer.onCancel!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveUp_onCancelNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(move))
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(upAfterMove))
        verify(recognizer.onCancel!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_down_downPositionIsCorrect() {
        recognizer.pointerInputHandler.invokeOverAllPasses(listOf(down(x = 13f, y = 17f)))
        verify(recognizer.onStart!!).invoke(PxPosition(13.px, 17f.px))
    }
}