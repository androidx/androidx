/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope.trackpad

import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Exit
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.test.InputDispatcher
import androidx.compose.ui.test.TrackpadButton
import androidx.compose.ui.test.TrackpadInjectionScope
import androidx.compose.ui.test.animateMoveAlong
import androidx.compose.ui.test.animateMoveBy
import androidx.compose.ui.test.animateMoveTo
import androidx.compose.ui.test.injectionscope.trackpad.Common.PrimaryButton
import androidx.compose.ui.test.injectionscope.trackpad.Common.runTrackpadInputInjectionTest
import androidx.compose.ui.test.injectionscope.trackpad.Common.verifyTrackpadEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@Suppress("KotlinConstantConditions") // for "0 * T" (keep for clarity)
class MoveTest {
    companion object {
        private val T = InputDispatcher.eventPeriodMillis
        private val positionIn = Offset(1f, 1f)
        private val positionMove1 = Offset(2f, 2f)
        private val positionMove2 = Offset(3f, 3f)
        private val positionOut = Offset(101f, 101f)

        // For testing the animated movement methods:
        private val steps = 4
        private val distancePerStep = Offset(5f, 5f)
        private val distance = distancePerStep * steps.toFloat()
        private val position1 = Offset.Zero + distance
        private val position2 = Offset(1f, 1f)
        private val curveFromHere = { t: Long -> Offset(t.toFloat(), t.toFloat()) }
        private val curveFromElsewhere = { t: Long -> Offset(1f + t.toFloat(), 1f + t.toFloat()) }
    }

    @Test
    fun moveTo() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                // enter the box
                moveToAndCheck(positionIn, delayMillis = 0L)
                // move around the box
                moveToAndCheck(positionMove1)
                // move around the box with long delay
                moveToAndCheck(positionMove2, delayMillis = 2 * eventPeriodMillis)
                // exit the box
                moveToAndCheck(positionOut)
                // move back in the box
                moveToAndCheck(positionIn)
            },
            eventVerifiers =
                arrayOf(
                    { verifyTrackpadEvent(0 * T, Enter, false, positionIn) },
                    { verifyTrackpadEvent(1 * T, Move, false, positionMove1) },
                    { verifyTrackpadEvent(3 * T, Move, false, positionMove2) },
                    { verifyTrackpadEvent(4 * T, Exit, false, positionOut) },
                    { verifyTrackpadEvent(5 * T, Enter, false, positionIn) },
                ),
        )

    @Test
    fun moveBy() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                // enter the box
                moveByAndCheck(positionIn, delayMillis = 0L)
                // move around the box
                moveByAndCheck(positionMove1 - positionIn)
                // move around the box with long delay
                moveByAndCheck(positionMove2 - positionMove1, delayMillis = 2 * eventPeriodMillis)
                // exit the box
                moveByAndCheck(positionOut - positionMove2)
                // move back in the box
                moveByAndCheck(positionIn - positionOut)
            },
            eventVerifiers =
                arrayOf(
                    { verifyTrackpadEvent(0 * T, Enter, false, positionIn) },
                    { verifyTrackpadEvent(1 * T, Move, false, positionMove1) },
                    { verifyTrackpadEvent(3 * T, Move, false, positionMove2) },
                    { verifyTrackpadEvent(4 * T, Exit, false, positionOut) },
                    { verifyTrackpadEvent(5 * T, Enter, false, positionIn) },
                ),
        )

    @Test
    fun updatePointerTo() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                // move around
                updatePointerToAndCheck(positionIn)
                updatePointerToAndCheck(positionMove1)
                updatePointerToAndCheck(positionMove2)
                // press primary button
                press(TrackpadButton.Primary)
            },
            eventVerifiers =
                arrayOf(
                    // TODO: Difference from mouse/MoveTest.updatePointerTo, we don't see an enter
                    // here.
                    //       Should we?
                    { verifyTrackpadEvent(0, Press, true, positionMove2, PrimaryButton) }),
        )

    @Test
    fun updatePointerBy() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                // move around
                updatePointerByAndCheck(positionIn)
                updatePointerByAndCheck(positionMove1 - positionIn)
                updatePointerByAndCheck(positionMove2 - positionMove1)
                // press primary button
                press(TrackpadButton.Primary)
            },
            eventVerifiers =
                arrayOf(
                    // TODO: Difference from mouse/MoveTest.updatePointerBy, we don't see an enter
                    // here.
                    //       Should we?
                    { verifyTrackpadEvent(0, Press, true, positionMove2, PrimaryButton) }),
        )

    @Test
    fun enter_exit() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                // enter the box
                enter(positionIn)
                // move around the box
                moveTo(positionMove1)
                // exit the box
                exit(positionOut)
            },
            eventVerifiers =
                arrayOf(
                    { verifyTrackpadEvent(1 * T, Enter, false, positionIn) },
                    { verifyTrackpadEvent(2 * T, Move, false, positionMove1) },
                    { verifyTrackpadEvent(3 * T, Exit, false, positionOut) },
                ),
        )

    @Test
    fun enter_alreadyEntered() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                // enter the box
                enter(positionIn)
                // enter again
                expectError<IllegalStateException>(
                    expectedMessage =
                        "Cannot send trackpad hover enter event, trackpad is already hovering"
                ) {
                    enter(positionMove1)
                }
            },
            eventVerifiers = arrayOf({ verifyTrackpadEvent(1 * T, Enter, false, positionIn) }),
        )

    @Test
    fun exit_notEntered() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                // exit the box
                expectError<IllegalStateException>(
                    expectedMessage =
                        "Cannot send trackpad hover exit event, trackpad is not hovering"
                ) {
                    exit(positionOut)
                }
            }
        )

    @Test
    fun animatePointerTo() =
        runTrackpadInputInjectionTest(
            trackpadInput = { animateMoveTo(position1, durationMillis = steps * T) },
            eventVerifiers =
                arrayOf(
                    { verifyTrackpadEvent(1 * T, Enter, false, distancePerStep * 1f) },
                    { verifyTrackpadEvent(2 * T, Move, false, distancePerStep * 2f) },
                    { verifyTrackpadEvent(3 * T, Move, false, distancePerStep * 3f) },
                    { verifyTrackpadEvent(4 * T, Move, false, distancePerStep * 4f) },
                ),
        )

    @Test
    fun animatePointerBy() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                moveTo(position2)
                animateMoveBy(distance, durationMillis = steps * T)
            },
            eventVerifiers =
                arrayOf(
                    { verifyTrackpadEvent(1 * T, Enter, false, position2) },
                    { verifyTrackpadEvent(2 * T, Move, false, position2 + (distancePerStep * 1f)) },
                    { verifyTrackpadEvent(3 * T, Move, false, position2 + (distancePerStep * 2f)) },
                    { verifyTrackpadEvent(4 * T, Move, false, position2 + (distancePerStep * 3f)) },
                    { verifyTrackpadEvent(5 * T, Move, false, position2 + (distancePerStep * 4f)) },
                ),
        )

    @Test
    fun animateAlong_fromCurrentPosition() =
        runTrackpadInputInjectionTest(
            trackpadInput = { animateMoveAlong(curveFromHere, durationMillis = steps * T) },
            eventVerifiers =
                arrayOf(
                    // The curve starts at the current position (0, 0) so we expect no initial
                    // event.
                    { verifyTrackpadEvent(1 * T, Enter, false, curveFromHere(1 * T)) },
                    { verifyTrackpadEvent(2 * T, Move, false, curveFromHere(2 * T)) },
                    { verifyTrackpadEvent(3 * T, Move, false, curveFromHere(3 * T)) },
                    { verifyTrackpadEvent(4 * T, Move, false, curveFromHere(4 * T)) },
                ),
        )

    @Test
    fun animateAlong_fromOtherPosition() =
        runTrackpadInputInjectionTest(
            trackpadInput = { animateMoveAlong(curveFromElsewhere, durationMillis = steps * T) },
            eventVerifiers =
                arrayOf(
                    // The curve doesn't start at the current position (0, 0) so we expect an
                    // initial event
                    { verifyTrackpadEvent(0 * T, Enter, false, curveFromElsewhere(0 * T)) },
                    { verifyTrackpadEvent(1 * T, Move, false, curveFromElsewhere(1 * T)) },
                    { verifyTrackpadEvent(2 * T, Move, false, curveFromElsewhere(2 * T)) },
                    { verifyTrackpadEvent(3 * T, Move, false, curveFromElsewhere(3 * T)) },
                    { verifyTrackpadEvent(4 * T, Move, false, curveFromElsewhere(4 * T)) },
                ),
        )

    private fun TrackpadInjectionScope.moveToAndCheck(
        position: Offset,
        delayMillis: Long = eventPeriodMillis,
    ) {
        moveTo(position, delayMillis)
        assertThat(currentPosition).isEqualTo(position)
    }

    private fun TrackpadInjectionScope.moveByAndCheck(
        delta: Offset,
        delayMillis: Long = eventPeriodMillis,
    ) {
        val expectedPosition = currentPosition + delta
        moveBy(delta, delayMillis)
        assertThat(currentPosition).isEqualTo(expectedPosition)
    }

    private fun TrackpadInjectionScope.updatePointerToAndCheck(position: Offset) {
        updatePointerTo(position)
        assertThat(currentPosition).isEqualTo(position)
    }

    private fun TrackpadInjectionScope.updatePointerByAndCheck(delta: Offset) {
        val expectedPosition = currentPosition + delta
        updatePointerBy(delta)
        assertThat(currentPosition).isEqualTo(expectedPosition)
    }
}
