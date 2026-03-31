/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.v2.runInternalSkikoComposeUiTest
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.test.assertFalse
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
internal class TooltipAreaTest {

    // https://youtrack.jetbrains.com/issue/CMP-2821
    @Test
    fun simpleTooltipIsShown() = runComposeUiTest {
        setContent {
            SimpleTooltipArea()
        }

        onNodeWithTag("tooltip").assertDoesNotExist()

        onNodeWithTag("tooltipArea").performMouseInput {
            moveTo(Offset(30f, 40f))
        }
        mainClock.advanceTimeToAfterTooltipDelay()
        onNodeWithTag("tooltip").assertExists()
    }

    /**
     * Verify that the tooltip is hidden when the tooltip area is pressed.
     */
    @Test
    fun tooltipHiddenOnPress() = runComposeUiTest {
        setContent {
            SimpleTooltipArea()
        }

        onNodeWithTag("tooltipArea").performMouseInput {
            moveTo(Offset(30f, 40f))
        }
        mainClock.advanceTimeToAfterTooltipDelay()
        onNodeWithTag("tooltip").assertExists()

        onNodeWithTag("tooltipArea").performMouseInput {
            press()
        }
        onNodeWithTag("tooltip").assertDoesNotExist()
    }

    /**
     * Verify that the tooltip is hidden when the mouse leaves the tooltip area.
     */
    @Test
    fun tooltipHiddenOnExit() = runComposeUiTest {
        setContent {
            SimpleTooltipArea()
        }

        onNodeWithTag("tooltipArea").performMouseInput {
            moveTo(Offset(30f, 40f))
        }
        mainClock.advanceTimeToAfterTooltipDelay()
        onNodeWithTag("tooltip").assertExists()

        onNodeWithTag("tooltipArea").performMouseInput {
            moveTo(Offset(150f, 150f))
        }
        onNodeWithTag("tooltip").assertDoesNotExist()
    }

    /**
     * Verify that the tooltip is hidden when the mouse moves into the tooltip, as long as it's
     * still also inside the tooltip.
     */
    @Test
    fun tooltipNotHiddenOnMoveIntoTooltip() = runComposeUiTestWithStandardTestDispatcher {
        var tooltipHidden = false
        setContent {
            TooltipArea(
                tooltip = {
                    Box(Modifier.size(100.dp).testTag("tooltip"))
                    DisposableEffect(Unit) {
                        onDispose {
                            tooltipHidden = true
                        }
                    }
                },
                tooltipPlacement = TooltipPlacement.CursorPoint(
                    offset = DpOffset(x = 0.dp, y = 10.dp)
                ),
            ) {
                Box(Modifier.size(100.dp).testTag("tooltipArea"))
            }
        }

        // Move into the tooltip area
        onNodeWithTag("tooltipArea").performMouseInput {
            moveTo(Offset(30f, 40f))
        }
        mainClock.advanceTimeToAfterTooltipDelay()

        // Move into the tooltip, but still inside the area
        onNodeWithTag("tooltip").let {
            it.assertExists()
            it.performMouseInput {
                moveTo(Offset(10f, 10f))  // Still inside the tooltip area
            }
        }
        waitForIdle()

        // Can't test with `assertExists` because if the tooltip was hidden, it could still be
        // re-shown after a delay. So the test would pass even on the wrong behavior.
        assertFalse(tooltipHidden, "Tooltip was hidden on move into tooltip")

        // Move within the tooltip to a position outside the tooltip area
        onNodeWithTag("tooltip").let {
            it.assertExists()
            it.performMouseInput {
                moveTo(Offset(99f, 99f))  // Outside the tooltip area
            }
        }
        onNodeWithTag("tooltip").assertDoesNotExist()
    }

    /**
     * Verify that the tooltip is shown after the given delay and not beforehand.
     */
    @Test
    fun tooltipShownAfterDelay() = runComposeUiTest {
        mainClock.autoAdvance = false

        setContent {
            SimpleTooltipArea(delayMillis = 200)
        }

        onNodeWithTag("tooltipArea").performMouseInput {
            moveTo(Offset(30f, 40f))
        }
        mainClock.advanceTimeBy(100)
        onNodeWithTag("tooltip").assertDoesNotExist()
        mainClock.advanceTimeBy(101)
        onNodeWithTag("tooltip").assertExists()
    }

    /**
     * Verify that the tooltip is re-shown after press -> release -> move
     */
    @Test
    fun tooltipReshownOnMove() = runComposeUiTest {
        setContent {
            SimpleTooltipArea()
        }

        onNodeWithTag("tooltipArea").performMouseInput {
            moveTo(Offset(30f, 40f))
        }
        mainClock.advanceTimeToAfterTooltipDelay()
        onNodeWithTag("tooltip").assertExists()

        onNodeWithTag("tooltipArea").performMouseInput {
            press()
        }
        onNodeWithTag("tooltip").assertDoesNotExist()

        onNodeWithTag("tooltipArea").performMouseInput {
            release()
            moveBy(Offset(10f, 10f))
        }
        mainClock.advanceTimeToAfterTooltipDelay()
        onNodeWithTag("tooltip").assertExists()
    }

    /**
     * Verify that the tooltip is hidden on exit, even if the pointer is "inside" the tooltip area.
     */
    @Test
    fun tooltipHiddenOnExitEvenIfPointerInside() = runComposeUiTest {
        setContent {
            SimpleTooltipArea()
        }

        onNodeWithTag("tooltipArea").performMouseInput {
            moveTo(Offset(1f, 1f))
        }
        mainClock.advanceTimeToAfterTooltipDelay()
        onNodeWithTag("tooltip").assertExists()

        onNodeWithTag("tooltipArea").performMouseInput {
            exit(Offset(0f, 0f))
        }
        onNodeWithTag("tooltip").assertDoesNotExist()
    }

    /**
     * Verify that the tooltip is hidden when the pointer exits into an overlapping element.
     */
    @Test
    fun tooltipHiddenOnExitIntoOverlappingElement() = runComposeUiTest {
        setContent {
            Box(Modifier.size(100.dp)) {
                SimpleTooltipArea()
                Box(Modifier.size(50.dp).align(Alignment.BottomEnd).pointerInput(Unit) {})
            }
        }

        onNodeWithTag("tooltipArea").performMouseInput {
            moveTo(Offset(1f, 1f))
        }
        mainClock.advanceTimeToAfterTooltipDelay()
        onNodeWithTag("tooltip").assertExists()

        onNodeWithTag("tooltipArea").performMouseInput {
            moveTo(Offset(75f, 75f))
        }
        onNodeWithTag("tooltip").assertDoesNotExist()
    }

    /**
     * Verify that the tooltip is hidden on exit from the tooltip, even if the pointer is "inside"
     * the tooltip area, and the tooltip area itself didn't receive an "enter" event.
     */
    @Test
    fun tooltipHiddenOnExitFromTooltipEvenIfPointerInsideTooltipAreaWithoutEnterEvent() =
        runComposeUiTestWithStandardTestDispatcher() {
            setContent {
                SimpleTooltipArea(
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.TopStart,
                        alignment = Alignment.BottomEnd,
                    )
                )
            }

            onNodeWithTag("tooltipArea").performMouseInput {
                moveTo(Offset(50f, 50f))
            }
            mainClock.advanceTimeToAfterTooltipDelay()
            onNodeWithTag("tooltip").assertExists()

            onNodeWithTag("tooltip").performMouseInput {
                moveTo(Offset(1f, 1f))
            }
            onNodeWithTag("tooltip").assertExists()

            onNodeWithTag("tooltip").performMouseInput {
                exit(Offset(0f, 0f))
            }
            onNodeWithTag("tooltip").assertDoesNotExist()
        }

    /**
     * Verify that the tooltip is not hidden on exit from the tooltip if the pointer is "inside"
     * the tooltip area and the area itself receives an "enter" event.
     */
    @Test
    fun tooltipNotHiddenOnExitFromTooltipIfPointerInsideTooltipAreaAndReceivedEnterEvent() =
        runComposeUiTestWithStandardTestDispatcher {
            setContent {
                SimpleTooltipArea(
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.TopStart,
                        alignment = Alignment.BottomEnd,
                    )
                )
            }

            onNodeWithTag("tooltipArea").performMouseInput {
                moveTo(Offset(50f, 50f))
            }
            mainClock.advanceTimeToAfterTooltipDelay()
            onNodeWithTag("tooltip").assertExists()

            onNodeWithTag("tooltip").performMouseInput {
                moveTo(Offset(1f, 1f))
            }
            onNodeWithTag("tooltip").assertExists()

            onNodeWithTag("tooltip").performMouseInput {
                moveTo(Offset(width + 1f, height.toFloat()))
            }
            onNodeWithTag("tooltip").assertExists()
        }

    /**
     * Verify that the tooltip is hidden when the pointer is moved inside the tooltip, but outside
     * the tooltip area.
     */
    @Test
    fun tooltipHiddenOnExitFromTooltipAreaBoundsWhileInsideTooltip() =
        runComposeUiTestWithStandardTestDispatcher {
            setContent {
                Box(Modifier.size(200.dp)) {
                    SimpleTooltipArea(
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.TopEnd,
                            alignment = Alignment.TopStart,
                            offset = DpOffset(x = 10.dp, y = 0.dp)
                        )
                    )
                }
            }

            onNodeWithTag("tooltipArea").performMouseInput {
                moveTo(Offset(50f, 50f))
            }
            mainClock.advanceTimeToAfterTooltipDelay()
            onNodeWithTag("tooltip").assertExists()

            // Move into the tooltip while still inside the tooltip area
            onNodeWithTag("tooltip").performMouseInput {
                moveTo(Offset(1f, 1f))
            }
            onNodeWithTag("tooltip").assertExists()

            // Move inside the tooltip but outside the bounds of the tooltip area
            onNodeWithTag("tooltip").performMouseInput {
                moveTo(Offset(15f, 1f))
            }
            onNodeWithTag("tooltip").assertDoesNotExist()
        }

    /**
     * Verify that the tooltip is hidden when the pointer is moved inside the area and then outside
     * before the tooltip is actually shown.
     */
    @Test
    fun tooltipHiddenOnExitFromTooltipAreaBeforeTooltipIsShown() =
        runComposeUiTestWithStandardTestDispatcher {
            setContent {
                Box(Modifier.size(200.dp)) {
                    SimpleTooltipArea(
                        tooltipPlacement = TooltipPlacement.ComponentRect(
                            anchor = Alignment.Center,
                            alignment = Alignment.Center,
                        )
                    )
                }
            }

            // Move into the tooltip area
            onNodeWithTag("tooltipArea").performMouseInput {
                moveTo(Offset(50f, 50f))
            }
            // Move outside the tooltip area without waiting
            onNodeWithTag("tooltipArea").performMouseInput {
                exit(Offset(0f, 0f))
            }
            mainClock.advanceTimeToAfterTooltipDelay()
            onNodeWithTag("tooltip").assertDoesNotExist()
        }

    private fun MainTestClock.advanceTimeToAfterTooltipDelay() =
        advanceTimeBy(TooltipDelayMillis + 1L)

    @OptIn(InternalComposeUiApi::class, InternalTestApi::class)
    private fun runComposeUiTestWithStandardTestDispatcher(
        block: suspend ComposeUiTest.() -> Unit
    ) = runInternalSkikoComposeUiTest {
        block()
    }

    @Composable
    private fun SimpleTooltipArea(
        areaSize: Dp = 100.dp,
        tooltipSize: Dp = 20.dp,
        delayMillis: Int = TooltipDelayMillis,
        tooltipPlacement: TooltipPlacement = TooltipPlacement.CursorPoint(
            offset = DpOffset(0.dp, 16.dp)
        )
    ) {
        TooltipArea(
            tooltip = {
                Box(Modifier.size(tooltipSize).testTag("tooltip"))
            },
            delayMillis = delayMillis,
            tooltipPlacement = tooltipPlacement
        ) {
            Box(Modifier.size(areaSize).testTag("tooltipArea"))
        }
    }
}

private const val TooltipDelayMillis = 500