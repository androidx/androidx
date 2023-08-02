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

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

public class RepeatableClickable {
    @get:Rule
    public val rule = createComposeRule()

    @Test
    fun touch_hold_shorter_than_threshold() {
        var clickCounter = 0

        boxWithRepeatableClickable(rule, 300) {
            clickCounter++
        }

        assertEquals(0, clickCounter)
    }

    @Test
    fun touch_hold_equals_to_threshold() {
        var clickCounter = 0

        boxWithRepeatableClickable(rule, 500) {
            clickCounter++
        }

        assertEquals(1, clickCounter)
    }

    @Test
    fun touch_hold_longer_than_threshold() {
        var clickCounter = 0

        boxWithRepeatableClickable(rule, 620) {
            clickCounter++
        }

        assertEquals(3, clickCounter)
    }

    @Test
    fun touch_hold_disabled() {
        var clickCounter = 0

        boxWithRepeatableClickable(rule, 500, false) {
            clickCounter++
        }

        assertEquals(0, clickCounter)
    }

    private fun boxWithRepeatableClickable(
        rule: ComposeContentTestRule,
        holdDelay: Long,
        enabled: Boolean = true,
        initialDelay: Long = 500L,
        incrementalDelay: Long = 60L,
        onClick: () -> Unit
    ) {

        rule.setContent {
            Box(
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .repeatableClickable(
                        enabled = enabled,
                        initialDelay = initialDelay,
                        incrementalDelay = incrementalDelay
                    ) {
                        onClick()
                    }
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            advanceEventTime(holdDelay)
            up()
        }
    }
}
