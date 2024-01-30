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

package androidx.wear.compose.materialcore

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

public class RepeatableClickableTest {
    @get:Rule
    public val rule = createComposeRule()

    @Test
    fun touch_hold_shorter_than_threshold_performs_click() {
        var repeatableClickCounter = 0
        var clicked = false

        boxWithRepeatableClickable(rule,
            holdDelay = INITIAL_DELAY / 2,
            onRepeatableClick = { repeatableClickCounter++ },
            onClick = { clicked = true }
        )
        assertEquals(0, repeatableClickCounter)
        assertEquals(true, clicked)
    }

    @Test
    fun touch_hold_equals_to_threshold_performs_repeatable_click() {
        var repeatableClickCounter = 0
        var clicked = false

        boxWithRepeatableClickable(rule,
            holdDelay = INITIAL_DELAY,
            onRepeatableClick = { repeatableClickCounter++ },
            onClick = { clicked = true }
        )
        assertEquals(1, repeatableClickCounter)
        assertEquals(false, clicked)
    }

    @Test
    fun touch_hold_longer_than_threshold_performs_multiple_repeatable_clicks() {
        var repeatableClickCounter = 0
        var clicked = false

        boxWithRepeatableClickable(rule,
            holdDelay = INITIAL_DELAY + INCREMENTAL_DELAY * 2,
            onRepeatableClick = { repeatableClickCounter++ },
            onClick = { clicked = true }
        )

        assertEquals(3, repeatableClickCounter)
        assertEquals(false, clicked)
    }

    @Test
    fun touch_hold_disabled() {
        var repeatableClickCounter = 0
        var clicked = false

        boxWithRepeatableClickable(rule,
            holdDelay = INITIAL_DELAY,
            enabled = false,
            onRepeatableClick = { repeatableClickCounter++ },
            onClick = { clicked = true }
        )

        assertEquals(0, repeatableClickCounter)
        assertEquals(false, clicked)
    }

    @Test
    fun touch_hold_release_outside_of_bounds_shorter_than_threshold() {
        var repeatableClickCounter = 0
        var clicked = false

        boxWithRepeatableClickable(rule,
            holdDelay = INITIAL_DELAY / 2,
            enabled = true,
            releaseOutsideOfBox = true,
            onRepeatableClick = { repeatableClickCounter++ },
            onClick = { clicked = true }
        )

        assertEquals(0, repeatableClickCounter)
        assertEquals(false, clicked)
    }

    private fun boxWithRepeatableClickable(
        rule: ComposeContentTestRule,
        holdDelay: Long,
        enabled: Boolean = true,
        initialDelay: Long = INITIAL_DELAY,
        incrementalDelay: Long = INCREMENTAL_DELAY,
        releaseOutsideOfBox: Boolean = false,
        onClick: () -> Unit,
        onRepeatableClick: () -> Unit
    ) {
        rule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .testTag(TEST_TAG)
                        .size(50.dp)
                        .align(Alignment.Center)
                        .repeatableClickable(
                            enabled = enabled,
                            initialDelay = initialDelay,
                            incrementalDelay = incrementalDelay,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onClick,
                            onRepeatableClick = onRepeatableClick
                        )
                ) {}
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            advanceEventTime(holdDelay)
            if (releaseOutsideOfBox) {
                // Move to -1f,-1f coordinates which are outside of the current component
                moveTo(Offset(-1f, -1f))
            }
            up()
        }
    }

    companion object {
        private const val INITIAL_DELAY = 500L
        private const val INCREMENTAL_DELAY = 60L
    }
}
