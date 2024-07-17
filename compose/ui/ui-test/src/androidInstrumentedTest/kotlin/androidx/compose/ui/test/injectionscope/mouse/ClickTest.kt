/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope.mouse

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
import androidx.compose.ui.test.injectionscope.mouse.Common.ClickDuration
import androidx.compose.ui.test.injectionscope.mouse.Common.DefaultDoubleClickTimeMillis
import androidx.compose.ui.test.injectionscope.mouse.Common.DefaultLongClickTimeMillis
import androidx.compose.ui.test.injectionscope.mouse.Common.PrimaryButton
import androidx.compose.ui.test.injectionscope.mouse.Common.PrimarySecondaryButton
import androidx.compose.ui.test.injectionscope.mouse.Common.SecondaryButton
import androidx.compose.ui.test.injectionscope.mouse.Common.runMouseInputInjectionTest
import androidx.compose.ui.test.injectionscope.mouse.Common.verifyMouseEvent
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.tripleClick
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
        runMouseInputInjectionTest(
            mouseInput = {
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
                    { verifyMouseEvent(1 * T, Enter, false, positionIn) },
                    { verifyMouseEvent(1 * T, Press, true, positionIn, PrimaryButton) },
                    { verifyMouseEvent(2 * T, Move, true, positionMove1, PrimaryButton) },
                    { verifyMouseEvent(2 * T, Release, false, positionMove1) },
                )
        )

    @Test
    fun click_pressIn_moveOutIn_releaseIn() =
        runMouseInputInjectionTest(
            mouseInput = {
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
                    { verifyMouseEvent(1 * T, Enter, false, positionIn) },
                    { verifyMouseEvent(1 * T, Press, true, positionIn, PrimaryButton) },
                    { verifyMouseEvent(2 * T, Exit, true, positionOut, PrimaryButton) },
                    { verifyMouseEvent(3 * T, Enter, true, positionMove1, PrimaryButton) },
                    { verifyMouseEvent(3 * T, Release, false, positionMove1) },
                )
        )

    @Test
    fun click_pressIn_releaseOut() =
        runMouseInputInjectionTest(
            mouseInput = {
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
                    { verifyMouseEvent(1 * T, Enter, false, positionIn) },
                    { verifyMouseEvent(1 * T, Press, true, positionIn, PrimaryButton) },
                    { verifyMouseEvent(2 * T, Exit, true, positionOut, PrimaryButton) },
                    { verifyMouseEvent(2 * T, Release, false, positionOut) },
                )
        )

    @Test
    fun click_twoButtons_symmetric() =
        runMouseInputInjectionTest(
            mouseInput = {
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
                    { verifyMouseEvent(1 * T, Enter, false, positionIn) },
                    { verifyMouseEvent(1 * T, Press, true, positionIn, PrimaryButton) },
                    { verifyMouseEvent(2 * T, Move, true, positionMove1, PrimaryButton) },
                    // TODO(b/234439423): Expect more events when b/234439423 is fixed
                    //            { verifyMouseEvent(2 * T, Press, true, positionMove1,
                    // PrimarySecondaryButton) },
                    { verifyMouseEvent(3 * T, Move, true, positionMove2, PrimarySecondaryButton) },
                    //            { verifyMouseEvent(3 * T, Release, true, positionMove2,
                    // PrimaryButton) },
                    { verifyMouseEvent(3 * T, Release, false, positionMove2) },
                )
        )

    @Test
    fun click_twoButtons_staggered() =
        runMouseInputInjectionTest(
            mouseInput = {
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
                    { verifyMouseEvent(1 * T, Enter, false, positionIn) },
                    { verifyMouseEvent(1 * T, Press, true, positionIn, PrimaryButton) },
                    { verifyMouseEvent(2 * T, Move, true, positionMove1, PrimaryButton) },
                    // TODO(b/234439423): Expect more events when b/234439423 is fixed
                    //            { verifyMouseEvent(2 * T, Press, true, positionMove1,
                    // PrimarySecondaryButton) },
                    { verifyMouseEvent(3 * T, Move, true, positionMove2, PrimarySecondaryButton) },
                    //            { verifyMouseEvent(3 * T, Release, true, positionMove2,
                    // SecondaryButton) },
                    { verifyMouseEvent(3 * T, Release, false, positionMove2) },
                )
        )

    @Test
    fun press_alreadyPressed() =
        runMouseInputInjectionTest(
            mouseInput = {
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
                    { verifyMouseEvent(1 * T, Enter, false, positionIn) },
                    { verifyMouseEvent(1 * T, Press, true, positionIn, PrimaryButton) },
                )
        )

    @Test
    fun clickTest() =
        runMouseInputInjectionTest(
            mouseInput = { click() },
            eventVerifiers =
                arrayOf(
                    // t = 0, because click() presses immediately
                    { verifyMouseEvent(0, Enter, false, positionCenter) },
                    { verifyMouseEvent(0, Press, true, positionCenter, PrimaryButton) },
                    { verifyMouseEvent(ClickDuration, Release, false, positionCenter) },
                )
        )

    @Test
    fun rightClickTest() =
        runMouseInputInjectionTest(
            mouseInput = { rightClick() },
            eventVerifiers =
                arrayOf(
                    // t = 0, because click() presses immediately
                    { verifyMouseEvent(0, Enter, false, positionCenter) },
                    { verifyMouseEvent(0, Press, true, positionCenter, SecondaryButton) },
                    { verifyMouseEvent(ClickDuration, Release, false, positionCenter) },
                )
        )

    @Test
    fun doubleClickTest() {
        // Time starts at 0, because doubleClick() presses immediately
        val press1 = 0L
        val release1 = press1 + ClickDuration
        val press2 = release1 + DefaultDoubleClickTimeMillis
        val release2 = press2 + ClickDuration

        runMouseInputInjectionTest(
            mouseInput = { doubleClick() },
            eventVerifiers =
                arrayOf(
                    { verifyMouseEvent(press1, Enter, false, positionCenter) },
                    { verifyMouseEvent(press1, Press, true, positionCenter, PrimaryButton) },
                    { verifyMouseEvent(release1, Release, false, positionCenter) },
                    { verifyMouseEvent(press2, Press, true, positionCenter, PrimaryButton) },
                    { verifyMouseEvent(release2, Release, false, positionCenter) },
                )
        )
    }

    @Test
    fun tripleClickTest() {
        // Time starts at 0, because tripleClick() presses immediately
        val press1 = 0L
        val release1 = press1 + ClickDuration
        val press2 = release1 + DefaultDoubleClickTimeMillis
        val release2 = press2 + ClickDuration
        val press3 = release2 + DefaultDoubleClickTimeMillis
        val release3 = press3 + ClickDuration

        runMouseInputInjectionTest(
            mouseInput = { tripleClick() },
            eventVerifiers =
                arrayOf(
                    { verifyMouseEvent(press1, Enter, false, positionCenter) },
                    { verifyMouseEvent(press1, Press, true, positionCenter, PrimaryButton) },
                    { verifyMouseEvent(release1, Release, false, positionCenter) },
                    { verifyMouseEvent(press2, Press, true, positionCenter, PrimaryButton) },
                    { verifyMouseEvent(release2, Release, false, positionCenter) },
                    { verifyMouseEvent(press3, Press, true, positionCenter, PrimaryButton) },
                    { verifyMouseEvent(release3, Release, false, positionCenter) },
                )
        )
    }

    @Test
    fun longClickTest() =
        runMouseInputInjectionTest(
            mouseInput = { longClick() },
            eventVerifiers =
                arrayOf(
                    // t = 0, because longClick() presses immediately
                    { verifyMouseEvent(0L, Enter, false, positionCenter) },
                    { verifyMouseEvent(0L, Press, true, positionCenter, PrimaryButton) },
                    // longClick adds 100ms to the minimum required time, just to be sure
                    {
                        verifyMouseEvent(
                            DefaultLongClickTimeMillis + 100,
                            Release,
                            false,
                            positionCenter
                        )
                    },
                )
        )

    // Rather than checking the events sent on, for this more complex mouse gesture we
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

        onNodeWithTag("draggable-box").performMouseInput {
            dragAndDrop(center, center + Offset(2f * width, 4f * height))
        }
        waitForIdle()
        assertWithMessage("xOffset").that(xOffsetPx).isWithin(marginPx).of(2 * sizePx)
        assertWithMessage("yOffset").that(yOffsetPx).isWithin(marginPx).of(4 * sizePx)
    }
}
