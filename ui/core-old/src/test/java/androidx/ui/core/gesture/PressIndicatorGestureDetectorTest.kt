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

import androidx.ui.core.pointerinput.PointerInputChange
import androidx.ui.engine.geometry.Offset
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PressIndicatorGestureDetectorTest {

    @Test
    fun pointerInputHandler_down_onStartCalledOnce() {
        val pointerInputChanges = arrayOf(down)
        val callbackLog = pointerInputHandler_onStartAndOnRelease(pointerInputChanges)
        assertThat(callbackLog.filter { it.type === LogEntryType.START }.size, `is`(1))
    }

    @Test
    fun pointerInputHandler_downConsumed_onStartNotCalled() {
        val pointerInputChanges = arrayOf(downConsumed)
        val callbackLog = pointerInputHandler_onStartAndOnRelease(pointerInputChanges)
        assertThat(callbackLog.filter { it.type === LogEntryType.START }.size, `is`(0))
    }

    @Test
    fun pointerInputHandler_downUp_onStopCalledOnce() {
        val pointerInputChanges = arrayOf(down, up)
        val callbackLog = pointerInputHandler_onStartAndOnRelease(pointerInputChanges)
        assertThat(callbackLog.filter { it.type === LogEntryType.STOP }.size, `is`(1))
    }

    @Test
    fun pointerInputHandler_downUpConsumed_onStopCalledOnce() {
        val pointerInputChanges = arrayOf(down, upConsumed)
        val callbackLog = pointerInputHandler_onStartAndOnRelease(pointerInputChanges)
        assertThat(callbackLog.filter { it.type === LogEntryType.STOP }.size, `is`(1))
    }

    @Test
    fun pointerInputHandler_downMoveUp_onStopCalledOnce() {
        val pointerInputChanges = arrayOf(down, move, upAfterMove)
        val callbackLog = pointerInputHandler_onStartAndOnRelease(pointerInputChanges)
        assertThat(callbackLog.filter { it.type === LogEntryType.STOP }.size, `is`(1))
    }

    @Test
    fun pointerInputHandler_downMoveConsumedUp_onStopNotCalled() {
        val pointerInputChanges = arrayOf(down, moveConsumed, upAfterMove)
        val callbackLog = pointerInputHandler_onStartAndOnRelease(pointerInputChanges)
        assertThat(callbackLog.filter { it.type === LogEntryType.STOP }.size, `is`(0))
    }

    @Test
    fun pointerInputHandler_downConsumedUp_onStopNotCalled() {
        val pointerInputChanges = arrayOf(downConsumed, up)
        val callbackLog = pointerInputHandler_onStartAndOnRelease(pointerInputChanges)
        assertThat(callbackLog.filter { it.type === LogEntryType.STOP }.size, `is`(0))
    }

    @Test
    fun pointerInputHandler_downConsumedMoveConsumed_onStopNotCalled() {
        val pointerInputChanges = arrayOf(downConsumed, moveConsumed)
        val callbackLog = pointerInputHandler_onStartAndOnRelease(pointerInputChanges)
        assertThat(callbackLog.filter { it.type === LogEntryType.STOP }.size, `is`(0))
    }

    @Test
    fun pointerInputHandler_downMoveConsumed_onCancelCalledOnce() {
        val pointerInputChanges = arrayOf(down, moveConsumed)
        val callbackLog = pointerInputHandler_onStartAndOnRelease(pointerInputChanges)
        assertThat(callbackLog.filter { it.type === LogEntryType.CANCEL }.size, `is`(1))
    }

    @Test
    fun pointerInputHandler_downConsumedMoveConsumed_onCancelNotCalled() {
        val pointerInputChanges = arrayOf(downConsumed, moveConsumed)
        val callbackLog = pointerInputHandler_onStartAndOnRelease(pointerInputChanges)
        assertThat(callbackLog.filter { it.type === LogEntryType.CANCEL }.size, `is`(0))
    }

    @Test
    fun pointerInputHandler_downUp_onCancelNotCalled() {
        val pointerInputChanges = arrayOf(down, up)
        val callbackLog = pointerInputHandler_onStartAndOnRelease(pointerInputChanges)
        assertThat(callbackLog.filter { it.type === LogEntryType.CANCEL }.size, `is`(0))
    }

    @Test
    fun pointerInputHandler_downMoveUp_onCancelNotCalled() {
        val pointerInputChanges = arrayOf(down, move, upAfterMove)
        val callbackLog = pointerInputHandler_onStartAndOnRelease(pointerInputChanges)
        assertThat(callbackLog.filter { it.type === LogEntryType.CANCEL }.size, `is`(0))
    }

    @Test
    fun pointerInputHandler_down_downPositionIsCorrect() {
        val pointerInputChanges =
            arrayOf(PointerInputChange(0f, 0f, false, 13f, 17f, true, 0f, 0f, false))
        val callbackLog = pointerInputHandler_onStartAndOnRelease(pointerInputChanges)
        assertThat(callbackLog[0].offset, `is`(equalTo(Offset(13f, 17f))))
    }

    private fun pointerInputHandler_onStartAndOnRelease(
        pointerInputChanges: Array<PointerInputChange>
    ): List<CallBackLogEntry> {

        // Arrange

        val pressIndicatorGestureRecognizer = PressIndicatorGestureRecognizer()

        val callBackLog: MutableList<CallBackLogEntry> = mutableListOf()
        pressIndicatorGestureRecognizer.onStart = { offset ->
            callBackLog.add(CallBackLogEntry(LogEntryType.START, offset))
        }
        pressIndicatorGestureRecognizer.onStop = {
            callBackLog.add(CallBackLogEntry(LogEntryType.STOP))
        }
        pressIndicatorGestureRecognizer.onCancel = {
            callBackLog.add(CallBackLogEntry(LogEntryType.CANCEL))
        }

        // Act
        for (pointerInputChange in pointerInputChanges) {
            invokeHandler(
                pressIndicatorGestureRecognizer.pointerInputHandler,
                pointerInputChange
            )
        }

        return callBackLog
    }

    private data class CallBackLogEntry(val type: LogEntryType, val offset: Offset? = null)
}
