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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.test.assertFalse
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
internal class TooltipAreaTest {

    // https://github.com/JetBrains/compose-jb/issues/2821
    @Test
    fun `simple tooltip is shown`() = runComposeUiTest {
        setContent {
            SimpleTooltipArea()
        }

        onNodeWithTag("tooltip").assertDoesNotExist()

        onNodeWithTag("elementWithTooltip").performMouseInput {
            moveTo(Offset(30f, 40f))
        }

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

        onNodeWithTag("elementWithTooltip").performMouseInput {
            moveTo(Offset(30f, 40f))
        }
        onNodeWithTag("tooltip").assertExists()

        onNodeWithTag("elementWithTooltip").performMouseInput {
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

        onNodeWithTag("elementWithTooltip").performMouseInput {
            moveTo(Offset(30f, 40f))
        }
        onNodeWithTag("tooltip").assertExists()

        onNodeWithTag("elementWithTooltip").performMouseInput {
            moveTo(Offset(150f, 150f))
        }
        onNodeWithTag("tooltip").assertDoesNotExist()
    }

    /**
     * Verify that the tooltip is hidden when the mouse moves into the tooltip, as long as it's
     * still also inside the tooltip.
     */
    @Test
    fun tooltipNotHiddenOnMoveIntoTooltip() = runComposeUiTest {
        var tooltipHidden = false
        setContent {
            TooltipArea(
                tooltip = {
                    Box(Modifier.size(100.dp).testTag("tooltip"))
                    DisposableEffect(Unit) {
                        onDispose {
                            println("Tooltip disposed")
                            tooltipHidden = true
                        }
                    }
                },
                tooltipPlacement = TooltipPlacement.CursorPoint(
                    offset = DpOffset(x = 0.dp, y = 10.dp)
                ),
            ) {
                Box(Modifier.size(100.dp).testTag("elementWithTooltip"))
            }
        }

        // Move into the tooltip area
        onNodeWithTag("elementWithTooltip").performMouseInput {
            moveTo(Offset(30f, 40f))
        }

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

        onNodeWithTag("elementWithTooltip").performMouseInput {
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

        onNodeWithTag("elementWithTooltip").performMouseInput {
            moveTo(Offset(30f, 40f))
        }
        onNodeWithTag("tooltip").assertExists()

        onNodeWithTag("elementWithTooltip").performMouseInput {
            press()
        }
        onNodeWithTag("tooltip").assertDoesNotExist()

        onNodeWithTag("elementWithTooltip").performMouseInput {
            release()
            moveBy(Offset(10f, 10f))
        }
        onNodeWithTag("tooltip").assertExists()
    }

    @Composable
    private fun SimpleTooltipArea(
        areaSize: Dp = 100.dp,
        tooltipSize: Dp = 20.dp,
        delayMillis: Int = 500
    ) {
        TooltipArea(
            tooltip = {
                Box(Modifier.size(tooltipSize).testTag("tooltip"))
            },
            delayMillis = delayMillis
        ) {
            Box(Modifier.size(areaSize).testTag("elementWithTooltip"))
        }
    }
}