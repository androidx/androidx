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
import com.nhaarman.mockitokotlin2.verify
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PressReleasedGestureDetectorTest {

    private lateinit var recognizer: PressReleaseGestureRecognizer

    @Before
    fun setup() {
        recognizer = PressReleaseGestureRecognizer()
        recognizer.onRelease = mock()
    }

    // Verification for when onReleased should not be called.

    @Test
    fun pointerInputHandler_down_onReleaseNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(down(0, 0.milliseconds))
        verify(recognizer.onRelease!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downConsumedUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds).consumeDownChange()
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)

        verify(recognizer.onRelease!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveConsumedUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        pointer = pointer.moveTo(100.milliseconds, 5f).consume(5f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        pointer = pointer.up(200.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)

        verify(recognizer.onRelease!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downUpConsumed_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds).consumeDownChange()
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds).consumeDownChange()
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)

        verify(recognizer.onRelease!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsNegativeXUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, -1f, 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onRelease!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsPositiveXUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 1f, 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onRelease!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsNegativeYUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 0f, -1f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onRelease!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsPositiveYUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 0f, 1f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onRelease!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_disabledDownUp_onReleaseNotCalled() {
        recognizer.setEnabled(false)
        var pointer = down(0, 0.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)

        verify(recognizer.onRelease!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_disabledDownEnabledUp_onReleaseNotCalled() {
        recognizer.setEnabled(false)
        var pointer = down(0, 0.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        recognizer.setEnabled(true)
        pointer = pointer.up(100.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)

        verify(recognizer.onRelease!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downDisabledUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        recognizer.setEnabled(false)
        pointer = pointer.up(100.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)

        verify(recognizer.onRelease!!, never()).invoke()
    }

    // Verification for when onReleased should be called.

    @Test
    fun pointerInputHandler_downUp_onReleaseCalledOnce() {
        var pointer = down(0, 0.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)

        verify(recognizer.onRelease!!).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveUp_onReleaseCalledOnce() {
        var pointer = down(0, 0.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        pointer = pointer.moveTo(100.milliseconds, 5f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        pointer = pointer.up(200.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)

        verify(recognizer.onRelease!!).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsUpDownUp_onReleaseCalledOnce() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 0f, 1f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = down(1, duration = 150.milliseconds, x = 0f, y = 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(200.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onRelease!!).invoke()
    }

    @Test
    fun pointerInputHandler_downEnabledUp_onReleaseCalled() {
        var pointer = down(0, 0.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        recognizer.setEnabled(true)
        pointer = pointer.up(100.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)

        verify(recognizer.onRelease!!).invoke()
    }

    // Verification for when the down change should not be consumed.

    @Test
    fun pointerInputHandler_consumeDownOnStartIsFalse_downChangeNotConsumed() {
        recognizer.consumeDownOnStart = false
        val pointerEventChange =
            recognizer.pointerInputHandler.invokeOverAllPasses(down(0, 0.milliseconds))
        assertThat(pointerEventChange.consumed.downChange, `is`(false))
    }

    @Test
    fun pointerInputHandler_disabledAndConsumeDownOnStartIsDefault_downChangeNotConsumed() {
        recognizer.setEnabled(false)
        val pointerEventChange =
            recognizer.pointerInputHandler.invokeOverAllPasses(down(0, 0.milliseconds))
        assertThat(pointerEventChange.consumed.downChange, `is`(false))
    }

    // Verification for when the down change should be consumed.

    @Test
    fun pointerInputHandler_consumeDownOnStartIsDefault_downChangeConsumed() {
        val pointerEventChange =
            recognizer.pointerInputHandler.invokeOverAllPasses(down(0, 0.milliseconds))
        assertThat(pointerEventChange.consumed.downChange, `is`(true))
    }

    // Verification for when the up change should not be consumed.

    @Test
    fun pointerInputHandler_disabledDownUp_upChangeNotConsumed() {
        recognizer.setEnabled(false)
        var pointer = down(0, 0.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds)
        val pointerEventChange = recognizer.pointerInputHandler.invokeOverAllPasses(pointer)

        assertThat(pointerEventChange.consumed.downChange, `is`(false))
    }

    @Test
    fun pointerInputHandler_disabledDownEnabledUp_upChangeNotConsumed() {
        recognizer.setEnabled(false)
        var pointer = down(0, 0.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        recognizer.setEnabled(true)
        pointer = pointer.up(100.milliseconds)
        val pointerEventChange = recognizer.pointerInputHandler.invokeOverAllPasses(pointer)

        assertThat(pointerEventChange.consumed.downChange, `is`(false))
    }

    @Test
    fun pointerInputHandler_downDisabledUp_upChangeNotConsumed() {
        var pointer = down(0, 0.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        recognizer.setEnabled(false)
        pointer = pointer.up(100.milliseconds)
        val pointerEventChange = recognizer.pointerInputHandler.invokeOverAllPasses(pointer)

        assertThat(pointerEventChange.consumed.downChange, `is`(false))
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsNegativeXUp_upChangeNotConsumed() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, -1f, 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        val result =
            recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        assertThat(result.consumed.downChange, `is`(false))
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsPositiveXUp_upChangeNotConsumed() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 1f, 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        val result =
            recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        assertThat(result.consumed.downChange, `is`(false))
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsNegativeYUp_upChangeNotConsumed() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 0f, -1f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)
        val result =
            recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        assertThat(result.consumed.downChange, `is`(false))
    }

    @Test
    fun pointerInputHandler_downMoveOutsideBoundsPositiveYUp_upChangeNotConsumed() {
        var pointer = down(0, 0.milliseconds, x = 0f, y = 0f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.moveTo(50.milliseconds, 0f, 1f)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
        pointer = pointer.up(100.milliseconds)

        val result =
            recognizer.pointerInputHandler.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))

        assertThat(result.consumed.downChange, `is`(false))
    }

    // Verification for when the up change should be consumed.

    @Test
    fun pointerInputHandler_upChangeConsumed() {
        var pointer = down(0, 0.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds)
        val pointerEventChange = recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        assertThat(pointerEventChange.consumed.downChange, `is`(true))
    }

    // Verification for during what pass the changes are consumed.

    @Test
    fun pointerInputHandler_downChangeConsumedDuringPostUp() {
        var pointerEventChange = down(0, 0.milliseconds)
        pointerEventChange = recognizer.pointerInputHandler.invokeOverPasses(
            pointerEventChange,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown
        )
        assertThat(pointerEventChange.consumed.downChange, `is`(false))

        pointerEventChange = recognizer.pointerInputHandler.invokeOverPasses(
            pointerEventChange,
            PointerEventPass.PostUp,
            IntPxSize(0.ipx, 0.ipx)
        )
        assertThat(pointerEventChange.consumed.downChange, `is`(true))
    }

    @Test
    fun pointerInputHandler_upChangeConsumedDuringPostUp() {
        val pointer = down(0, 0.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        var pointerEventChange = pointer.up(100.milliseconds)
        pointerEventChange = recognizer.pointerInputHandler.invokeOverPasses(
            pointerEventChange,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown
        )
        assertThat(pointerEventChange.consumed.downChange, `is`(false))

        pointerEventChange = recognizer.pointerInputHandler.invokeOverPasses(
            pointerEventChange,
            PointerEventPass.PostUp,
            IntPxSize(0.ipx, 0.ipx)
        )
        assertThat(pointerEventChange.consumed.downChange, `is`(true))
    }

    // Verification of correct cancellation behavior.

    @Test
    fun cancelationHandler_downCancelUp_onReleaseNotCalled() {
        var pointer = down(0, 0.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)
        recognizer.cancelHandler()
        pointer = pointer.up(100.milliseconds)
        recognizer.pointerInputHandler.invokeOverAllPasses(pointer)

        verify(recognizer.onRelease!!, never()).invoke()
    }
}