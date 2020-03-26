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
import androidx.ui.core.consumeDownChange
import androidx.ui.testutils.down
import androidx.ui.testutils.invokeOverAllPasses
import androidx.ui.testutils.invokeOverPasses
import androidx.ui.testutils.moveBy
import androidx.ui.testutils.moveTo
import androidx.ui.testutils.up
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import androidx.ui.unit.milliseconds
import androidx.ui.unit.px
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RawPressStartGestureDetectorTest {

    private lateinit var recognizer: RawPressStartGestureRecognizer

    @Before
    fun setup() {
        recognizer = RawPressStartGestureRecognizer()
        recognizer.onPressStart = mock()
    }

    // Verification of scenarios where onPressStart should not be called.

    @Test
    fun onPointerInput_downConsumed_onPressStartNotCalled() {
        recognizer::onPointerInput.invokeOverAllPasses(down(0).consumeDownChange())
        verify(recognizer.onPressStart, never()).invoke(any())
    }

    @Test
    fun onPointerInput_downConsumedDown_onPressStartNotCalled() {
        var pointer1 = down(1, duration = 0.milliseconds).consumeDownChange()
        recognizer::onPointerInput.invokeOverAllPasses(pointer1)
        pointer1 = pointer1.moveBy(10.milliseconds)
        val pointer2 = down(2, duration = 10.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer1, pointer2)
        verify(recognizer.onPressStart, never()).invoke(any())
    }

    @Test
    fun onPointerInput_disabledDown_onPressStartNotCalled() {
        recognizer.setEnabled(false)
        recognizer::onPointerInput.invokeOverAllPasses(down(0))
        verify(recognizer.onPressStart, never()).invoke(any())
    }

    @Test
    fun onPointerInput_disabledDownEnabledDown_onPressStartNotCalled() {

        recognizer.setEnabled(false)
        var pointer1 = down(1, duration = 0.milliseconds).consumeDownChange()
        recognizer::onPointerInput.invokeOverAllPasses(pointer1)
        recognizer.setEnabled(true)
        pointer1 = pointer1.moveBy(10.milliseconds)
        val pointer2 = down(2, duration = 10.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer1, pointer2)
        verify(recognizer.onPressStart, never()).invoke(any())
    }

    // Verification of scenarios where onPressStart should be called once.

    @Test
    fun onPointerInput_down_onPressStartCalledOnce() {
        recognizer::onPointerInput.invokeOverAllPasses(down(0))
        verify(recognizer.onPressStart).invoke(any())
    }

    @Test
    fun onPointerInput_downDown_onPressStartCalledOnce() {
        var pointer0 = down(0)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0)
        pointer0 = pointer0.moveTo(1.milliseconds)
        val pointer1 = down(1, 1.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)

        verify(recognizer.onPressStart).invoke(any())
    }

    @Test
    fun onPointerInput_2Down1Up1Down_onPressStartCalledOnce() {
        var pointer0 = down(0)
        var pointer1 = down(1)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)
        pointer0 = pointer0.up(100.milliseconds)
        pointer1 = pointer1.moveTo(100.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)
        pointer0 = down(0, duration = 200.milliseconds)
        pointer1 = pointer1.moveTo(200.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)

        verify(recognizer.onPressStart).invoke(any())
    }

    @Test
    fun onPointerInput_1DownMoveOutside2ndDown_onPressStartOnlyCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 0f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, IntPxSize(5.ipx, 5.ipx))
        pointer0 = pointer0.moveTo(100.milliseconds, 10f, 0f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, IntPxSize(5.ipx, 5.ipx))
        pointer0 = pointer0.moveTo(200.milliseconds)
        val pointer1 = down(1, x = 0f, y = 0f)

        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)

        verify(recognizer.onPressStart).invoke(any())
    }

    // Verification of correct position returned by onPressStart.

    @Test
    fun onPointerInput_down_downPositionIsCorrect() {
        recognizer::onPointerInput.invokeOverAllPasses(down(0, x = 13f, y = 17f))
        verify(recognizer.onPressStart).invoke(PxPosition(13.px, 17f.px))
    }

    // Verification of correct consumption behavior.

    @Test
    fun onPointerInput_disabledDown_noDownChangeConsumed() {
        recognizer.setEnabled(false)
        var pointer = down(0)
        pointer = recognizer::onPointerInput.invokeOverAllPasses(pointer)
        assertThat(pointer.consumed.downChange, `is`(false))
    }

    // Verification of correct cancellation handling.

    @Test
    fun onCancel_justCancel_noCallbacksCalled() {
        recognizer.onCancel()

        verifyNoMoreInteractions(recognizer.onPressStart)
    }

    @Test
    fun onCancel_downCancelDown_onPressStartCalledTwice() {
        recognizer::onPointerInput.invokeOverAllPasses(down(id = 0, duration = 0.milliseconds))
        recognizer.onCancel()
        recognizer::onPointerInput.invokeOverAllPasses(down(id = 0, duration = 1.milliseconds))

        verify(recognizer.onPressStart, times(2)).invoke(any())
    }

    // Verification of correct execution pass behavior

    @Test
    fun onPointerInput_initialDown_behaviorOccursAtCorrectTime() {
        recognizer.setExecutionPass(PointerEventPass.InitialDown)

        val pointer = recognizer::onPointerInput.invokeOverPasses(
            down(0),
            PointerEventPass.InitialDown
        )

        verify(recognizer.onPressStart).invoke(any())
        assertThat(pointer.consumed.downChange, `is`(true))
    }

    @Test
    fun onPointerInput_preUp_behaviorOccursAtCorrectTime() {
        recognizer.setExecutionPass(PointerEventPass.PreUp)

        var pointer = recognizer::onPointerInput.invokeOverPasses(
            down(0),
            PointerEventPass.InitialDown
        )

        verify(recognizer.onPressStart, never()).invoke(any())
        assertThat(pointer.consumed.downChange, `is`(false))

        pointer = recognizer::onPointerInput.invokeOverPasses(
            down(1),
            PointerEventPass.PreUp
        )

        verify(recognizer.onPressStart).invoke(any())
        assertThat(pointer.consumed.downChange, `is`(true))
    }

    @Test
    fun onPointerInput_preDown_behaviorOccursAtCorrectTime() {
        recognizer.setExecutionPass(PointerEventPass.PreDown)

        var pointer = recognizer::onPointerInput.invokeOverPasses(
            down(0),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp
        )

        verify(recognizer.onPressStart, never()).invoke(any())
        assertThat(pointer.consumed.downChange, `is`(false))

        pointer = recognizer::onPointerInput.invokeOverPasses(
            down(1),
            PointerEventPass.PreDown
        )

        verify(recognizer.onPressStart).invoke(any())
        assertThat(pointer.consumed.downChange, `is`(true))
    }

    @Test
    fun onPointerInput_PostUp_behaviorOccursAtCorrectTime() {
        recognizer.setExecutionPass(PointerEventPass.PostUp)

        var pointer = recognizer::onPointerInput.invokeOverPasses(
            down(0),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown
        )

        verify(recognizer.onPressStart, never()).invoke(any())
        assertThat(pointer.consumed.downChange, `is`(false))

        pointer = recognizer::onPointerInput.invokeOverPasses(
            down(1),
            PointerEventPass.PostUp
        )

        verify(recognizer.onPressStart).invoke(any())
        assertThat(pointer.consumed.downChange, `is`(true))
    }

    @Test
    fun onPointerInput_postDown_behaviorOccursAtCorrectTime() {
        recognizer.setExecutionPass(PointerEventPass.PostDown)

        var pointer = recognizer::onPointerInput.invokeOverPasses(
            down(0),
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown,
            PointerEventPass.PostUp
        )

        verify(recognizer.onPressStart, never()).invoke(any())
        assertThat(pointer.consumed.downChange, `is`(false))

        pointer = recognizer::onPointerInput.invokeOverPasses(
            down(1),
            PointerEventPass.PostDown
        )

        verify(recognizer.onPressStart).invoke(any())
        assertThat(pointer.consumed.downChange, `is`(true))
    }

    // Verification of correct cancellation behavior.

    // The purpose of this test is hard to understand, but it proves that the cancel event sets the
    // state of the gesture detector to inactive such that when a new stream of events starts,
    // and the 1st down is already consumed, the gesture detector won't consume the 2nd down.
    @Test
    fun onCancel_downCancelDownConsumedDown_thirdDownNotConsumed() {
        recognizer::onPointerInput
            .invokeOverAllPasses(down(id = 0, duration = 0.milliseconds))
        recognizer.onCancel()
        var pointer1 = down(id = 1, duration = 10.milliseconds).consumeDownChange()
        recognizer::onPointerInput.invokeOverAllPasses(pointer1)
        pointer1 = pointer1.moveTo(20.milliseconds, 0f, 0f)
        val pointer2 = down(id = 2, duration = 20.milliseconds)
        val results = recognizer::onPointerInput.invokeOverAllPasses(pointer1, pointer2)

        assertThat(results[0].consumed.downChange, `is`(false))
        assertThat(results[1].consumed.downChange, `is`(false))
    }
}