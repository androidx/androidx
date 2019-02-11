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
import org.hamcrest.CoreMatchers
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PressReleasedGestureDetectorTest {

    @Test
    fun pointerInputHandler_downConsumedUp_onReleaseNotCalled() {
        val pointerInputChanges = arrayOf(downConsumed, up)
        val releaseCount = pointerInputHandler_onRelease(pointerInputChanges)
        assertThat(releaseCount, `is`(0))
    }

    @Test
    fun pointerInputHandler_downMoveConsumedUp_onReleaseNotCalled() {
        val pointerInputChanges = arrayOf(down, moveConsumed, upAfterMove)
        val releaseCount = pointerInputHandler_onRelease(pointerInputChanges)
        assertThat(releaseCount, `is`(0))
    }

    @Test
    fun pointerInputHandler_downUpConsumed_onReleaseNotCalled() {
        val pointerInputChanges = arrayOf(down, upConsumed)
        val releaseCount = pointerInputHandler_onRelease(pointerInputChanges)
        assertThat(releaseCount, `is`(0))
    }

    @Test
    fun pointerInputHandler_downUp_onReleaseCalledOnce() {
        val pointerInputChanges = arrayOf(down, up)
        val releaseCount = pointerInputHandler_onRelease(pointerInputChanges)
        assertThat(releaseCount, `is`(1))
    }

    @Test
    fun pointerInputHandler_downMoveUp_onReleaseCalledOnce() {
        val pointerInputChanges = arrayOf(down, move, upAfterMove)
        val releaseCount = pointerInputHandler_onRelease(pointerInputChanges)
        assertThat(releaseCount, `is`(1))
    }

    private fun pointerInputHandler_onRelease(
        pointerInputChanges: Array<PointerInputChange>
    ): Int {

        // Arrange

        val pressReleasedGestureRecognizer = PressReleaseGestureRecognizer()

        var callCount = 0
        pressReleasedGestureRecognizer.onRelease = {
            callCount++
        }

        // Act
        for (pointerInputChange in pointerInputChanges) {
            invokeHandler(
                pressReleasedGestureRecognizer.pointerInputHandler,
                pointerInputChange
            )
        }

        return callCount
    }

    @Test
    fun pointerInputHandler_consumeDownOnStartIsDefault_downChangeConsumed() {
        val pressReleasedGestureRecognizer = PressReleaseGestureRecognizer()
        val resultingPointerInputChange = invokeHandler(
            pressReleasedGestureRecognizer.pointerInputHandler,
            down
        )
        assertThat(resultingPointerInputChange.consumed.downChange, CoreMatchers.`is`(true))
    }

    @Test
    fun pointerInputHandler_consumeDownOnStartIsFalse_downChangeNotConsumed() {
        val pressReleasedGestureRecognizer = PressReleaseGestureRecognizer()
        pressReleasedGestureRecognizer.consumeDownOnStart = false

        val resultingPointerInputChange = invokeHandler(
            pressReleasedGestureRecognizer.pointerInputHandler,
            down
        )

        assertThat(resultingPointerInputChange.consumed.downChange, CoreMatchers.`is`(false))
    }
}
