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

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.expectError
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Exit
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InputDispatcher
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.dragAndDrop
import androidx.compose.ui.test.injectionscope.trackpad.Common.DefaultDoubleClickTimeMillis
import androidx.compose.ui.test.injectionscope.trackpad.Common.DefaultLongClickTimeMillis
import androidx.compose.ui.test.injectionscope.trackpad.Common.PrimaryButton
import androidx.compose.ui.test.injectionscope.trackpad.Common.PrimarySecondaryButton
import androidx.compose.ui.test.injectionscope.trackpad.Common.SecondaryButton
import androidx.compose.ui.test.injectionscope.trackpad.Common.runTrackpadInputInjectionTest
import androidx.compose.ui.test.injectionscope.trackpad.Common.verifyTrackpadEvent
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTrackpadInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.tripleClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.roundToInt
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ClickTest {
    companion object {
        private val T = InputDispatcher.eventPeriodMillis
        private val positionIn = Offset(1f, 1f)
        private val positionMove1 = Offset(2f, 2f)
        private val positionMove2 = Offset(3f, 3f)
        private val positionOut = Offset(101f, 101f)
        private val positionCenter = Offset(50f, 50f)
    }

    @Test
    fun click_pressIn_releaseIn() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                // enter the box
                moveTo(positionIn)
                // press primary button
                press(MouseButton.Primary)
                // move around the box
                moveTo(positionMove1)
                // release primary button
                release(MouseButton.Primary)
            },
            eventVerifiers =
                arrayOf(
                    { verifyTrackpadEvent(1 * T, Enter, false, positionIn) },
                    { verifyTrackpadEvent(1 * T, Press, true, positionIn, PrimaryButton) },
                    { verifyTrackpadEvent(2 * T, Move, true, positionMove1, PrimaryButton) },
                    { verifyTrackpadEvent(2 * T, Release, false, positionMove1) },
                ),
        )

    @Test
    fun click_pressIn_moveOutIn_releaseIn() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                // enter the box
                moveTo(positionIn)
                // press primary button
                press(MouseButton.Primary)
                // move out of the box
                moveTo(positionOut)
                // move back into the box
                moveTo(positionMove1)
                // release primary button in the box
                release(MouseButton.Primary)
            },
            eventVerifiers =
                arrayOf(
                    { verifyTrackpadEvent(1 * T, Enter, false, positionIn) },
                    { verifyTrackpadEvent(1 * T, Press, true, positionIn, PrimaryButton) },
                    { verifyTrackpadEvent(2 * T, Exit, true, positionOut, PrimaryButton) },
                    { verifyTrackpadEvent(3 * T, Enter, true, positionMove1, PrimaryButton) },
                    { verifyTrackpadEvent(3 * T, Release, false, positionMove1) },
                ),
        )

    @Test
    fun click_pressIn_releaseOut() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                // enter the box
                moveTo(positionIn)
                // press primary button
                press(MouseButton.Primary)
                // move out of the box
                moveTo(positionOut)
                // release primary button
                release(MouseButton.Primary)
            },
            eventVerifiers =
                arrayOf(
                    { verifyTrackpadEvent(1 * T, Enter, false, positionIn) },
                    { verifyTrackpadEvent(1 * T, Press, true, positionIn, PrimaryButton) },
                    { verifyTrackpadEvent(2 * T, Exit, true, positionOut, PrimaryButton) },
                    { verifyTrackpadEvent(2 * T, Release, false, positionOut) },
                ),
        )

    @Test
    fun click_twoButtons_symmetric() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                // enter the box
                moveTo(positionIn)
                // press primary button
                press(MouseButton.Primary)
                // move around the box
                moveTo(positionMove1)
                // press secondary button
                press(MouseButton.Secondary)
                // move around a bit more
                moveTo(positionMove2)
                // release secondary button
                release(MouseButton.Secondary)
                // release primary button
                release(MouseButton.Primary)
            },
            eventVerifiers =
                arrayOf(
                    { verifyTrackpadEvent(1 * T, Enter, false, positionIn) },
                    { verifyTrackpadEvent(1 * T, Press, true, positionIn, PrimaryButton) },
                    { verifyTrackpadEvent(2 * T, Move, true, positionMove1, PrimaryButton) },
                    // TODO(b/234439423): Expect more events when b/234439423 is fixed
                    //            { verifyTrackpadEvent(2 * T, Press, true, positionMove1,
                    //     PrimarySecondaryButton) },
                    {
                        verifyTrackpadEvent(
                            3 * T,
                            Move,
                            true,
                            positionMove2,
                            PrimarySecondaryButton,
                        )
                    },
                    //            { verifyTrackpadEvent(3 * T, Release, true, positionMove2,
                    // PrimaryButton) },
                    { verifyTrackpadEvent(3 * T, Release, false, positionMove2) },
                ),
        )

    @Test
    fun click_twoButtons_staggered() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                // enter the box
                moveTo(positionIn)
                // press primary button
                press(MouseButton.Primary)
                // move around the box
                moveTo(positionMove1)
                // press secondary button
                press(MouseButton.Secondary)
                // move around a bit more
                moveTo(positionMove2)
                // release primary button
                release(MouseButton.Primary)
                // release secondary button
                release(MouseButton.Secondary)
            },
            eventVerifiers =
                arrayOf(
                    { verifyTrackpadEvent(1 * T, Enter, false, positionIn) },
                    { verifyTrackpadEvent(1 * T, Press, true, positionIn, PrimaryButton) },
                    { verifyTrackpadEvent(2 * T, Move, true, positionMove1, PrimaryButton) },
                    // TODO(b/234439423): Expect more events when b/234439423 is fixed
                    //            { verifyTrackpadEvent(2 * T, Press, true, positionMove1,
                    //     PrimarySecondaryButton) },
                    {
                        verifyTrackpadEvent(
                            3 * T,
                            Move,
                            true,
                            positionMove2,
                            PrimarySecondaryButton,
                        )
                    },
                    //            { verifyTrackpadEvent(3 * T, Release, true, positionMove2,
                    // SecondaryButton) },
                    { verifyTrackpadEvent(3 * T, Release, false, positionMove2) },
                ),
        )

    @Test
    fun press_alreadyPressed() =
        runTrackpadInputInjectionTest(
            trackpadInput = {
                // enter the box
                moveTo(positionIn)
                // press primary button
                press(MouseButton.Primary)
                // press primary button again
                expectError<IllegalStateException>(
                    expectedMessage =
                        "Cannot send mouse button down event, " +
                            "button ${MouseButton.Primary.buttonId} is already pressed"
                ) {
                    press(MouseButton.Primary)
                }
            },
            eventVerifiers =
                arrayOf(
                    { verifyTrackpadEvent(1 * T, Enter, false, positionIn) },
                    { verifyTrackpadEvent(1 * T, Press, true, positionIn, PrimaryButton) },
                ),
        )

    @Test
    fun clickTest() =
        runTrackpadInputInjectionTest(
            trackpadInput = { click() },
            eventVerifiers =
                arrayOf(
                    // TODO: Difference from mouse/ClickTest.clickTest, we don't see an enter here.
                    //       Should we?
                    // t = 0, because click() presses immediately
                    { verifyTrackpadEvent(0, Press, true, positionCenter, PrimaryButton) },
                    { verifyTrackpadEvent(0, Release, false, positionCenter) },
                    { verifyTrackpadEvent(0, Enter, false, positionCenter) },
                ),
        )

    @Test
    fun rightClickTest() =
        runTrackpadInputInjectionTest(
            trackpadInput = { rightClick() },
            eventVerifiers =
                arrayOf(
                    // TODO: Difference from mouse/ClickTest.rightClickTest, we don't see an enter
                    // here.
                    //       Should we?
                    // t = 0, because click() presses immediately
                    { verifyTrackpadEvent(0, Press, true, positionCenter, SecondaryButton) },
                    { verifyTrackpadEvent(0, Release, false, positionCenter) },
                    { verifyTrackpadEvent(0, Enter, false, positionCenter) },
                ),
        )

    @Test
    fun doubleClickTest() {
        // Time starts at 0, because doubleClick() presses immediately
        val press1 = 0L
        val release1 = press1
        val press2 = release1 + DefaultDoubleClickTimeMillis
        val release2 = press2

        runTrackpadInputInjectionTest(
            trackpadInput = { doubleClick() },
            eventVerifiers =
                arrayOf(
                    // TODO: Difference from mouse/ClickTest.doubleClickTest, we don't see an enter
                    // here.
                    //       Should we?
                    { verifyTrackpadEvent(press1, Press, true, positionCenter, PrimaryButton) },
                    { verifyTrackpadEvent(release1, Release, false, positionCenter) },
                    { verifyTrackpadEvent(release1, Enter, false, positionCenter) },
                    { verifyTrackpadEvent(press2, Press, true, positionCenter, PrimaryButton) },
                    { verifyTrackpadEvent(release2, Release, false, positionCenter) },
                ),
        )
    }

    @Test
    fun tripleClickTest() {
        // Time starts at 0, because tripleClick() presses immediately
        val press1 = 0L
        val release1 = press1
        val press2 = release1 + DefaultDoubleClickTimeMillis
        val release2 = press2
        val press3 = release2 + DefaultDoubleClickTimeMillis
        val release3 = press3

        runTrackpadInputInjectionTest(
            trackpadInput = { tripleClick() },
            eventVerifiers =
                arrayOf(
                    // TODO: Difference from mouse/ClickTest.tripleClickTest, we don't see an enter
                    // here.
                    //       Should we?
                    { verifyTrackpadEvent(press1, Press, true, positionCenter, PrimaryButton) },
                    { verifyTrackpadEvent(release1, Release, false, positionCenter) },
                    { verifyTrackpadEvent(release1, Enter, false, positionCenter) },
                    { verifyTrackpadEvent(press2, Press, true, positionCenter, PrimaryButton) },
                    { verifyTrackpadEvent(release2, Release, false, positionCenter) },
                    { verifyTrackpadEvent(press3, Press, true, positionCenter, PrimaryButton) },
                    { verifyTrackpadEvent(release3, Release, false, positionCenter) },
                ),
        )
    }

    @Test
    fun longClickTest() =
        runTrackpadInputInjectionTest(
            trackpadInput = { longClick() },
            eventVerifiers =
                arrayOf(
                    // TODO: Difference from mouse/ClickTest.longClickTest, we don't see an enter
                    // here.
                    //       Should we?
                    // t = 0, because longClick() presses immediately
                    { verifyTrackpadEvent(0L, Press, true, positionCenter, PrimaryButton) },
                    // longClick adds 100ms to the minimum required time, just to be sure
                    {
                        verifyTrackpadEvent(
                            DefaultLongClickTimeMillis + 100,
                            Release,
                            false,
                            positionCenter,
                        )
                    },
                    {
                        verifyTrackpadEvent(
                            DefaultLongClickTimeMillis + 100,
                            Enter,
                            false,
                            positionCenter,
                        )
                    },
                ),
        )

    // Rather than checking the events sent on, for this more complex trackpad gesture we
    // check if the events actually lead to the expected outcome.
    @Test
    @OptIn(ExperimentalTestApi::class)
    fun dragAndDropTest() = runComposeUiTest {
        val sizeDp = 50.dp
        val sizePx = with(density) { sizeDp.toPx() }
        val marginPx = with(density) { 0.5.dp.toPx() }

        var xOffsetPx by mutableStateOf(0f)
        var yOffsetPx by mutableStateOf(0f)

        setContent {
            Box(Modifier.padding(16.dp).fillMaxSize()) {
                Box(
                    Modifier.testTag("draggable-box")
                        .offset { IntOffset(xOffsetPx.roundToInt(), yOffsetPx.roundToInt()) }
                        .size(sizeDp)
                        .background(Color.Red)
                        .draggable2D(
                            rememberDraggable2DState {
                                xOffsetPx += it.x
                                yOffsetPx += it.y
                            }
                        )
                )
            }
        }

        onNodeWithTag("draggable-box").performTrackpadInput {
            dragAndDrop(center, center + Offset(2f * width, 4f * height))
        }
        waitForIdle()
        @OptIn(ExperimentalComposeUiApi::class)
        if (ComposeUiFlags.isTrackpadGestureHandlingEnabled) {
            assertWithMessage("xOffset").that(xOffsetPx).isWithin(marginPx).of(2 * sizePx)
            assertWithMessage("yOffset").that(yOffsetPx).isWithin(marginPx).of(4 * sizePx)
        }
    }
}
