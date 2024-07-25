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

package androidx.compose.material3

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MenuTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun menu_canBeTriggered() {
        var expanded by mutableStateOf(false)

        rule.setContent {
            Box(Modifier.requiredSize(20.dp).background(color = Color.Blue)) {
                DropdownMenu(expanded = expanded, onDismissRequest = {}) {
                    DropdownMenuItem(
                        text = { Text("Option 1") },
                        modifier = Modifier.testTag("MenuContent"),
                        onClick = {}
                    )
                }
            }
        }

        rule.onNodeWithTag("MenuContent").assertDoesNotExist()
        rule.mainClock.autoAdvance = false

        rule.runOnUiThread { expanded = true }
        rule.mainClock.advanceTimeByFrame() // Trigger the popup
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag("MenuContent").assertExists()

        rule.runOnUiThread { expanded = false }
        rule.mainClock.advanceTimeByFrame() // Trigger the popup
        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(300)
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("MenuContent").assertDoesNotExist()

        rule.runOnUiThread { expanded = true }
        rule.mainClock.advanceTimeByFrame() // Trigger the popup
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag("MenuContent").assertExists()
    }

    @Test
    fun menu_hasExpectedSize() {
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.requiredSize(20.toDp()).background(color = Color.Blue)) {
                    DropdownMenu(expanded = true, onDismissRequest = {}) {
                        Box(Modifier.testTag("MenuContent1").size(70.toDp()))
                        Box(Modifier.testTag("MenuContent2").size(130.toDp()))
                    }
                }
            }
        }

        rule.onNodeWithTag("MenuContent1").assertExists()
        rule.onNodeWithTag("MenuContent2").assertExists()
        val node =
            rule
                .onNode(
                    isPopup() and
                        hasAnyDescendant(hasTestTag("MenuContent1")) and
                        hasAnyDescendant(hasTestTag("MenuContent2"))
                )
                .assertExists()
                .fetchSemanticsNode()
        with(rule.density) {
            assertThat(node.size.width).isEqualTo(130)
            assertThat(node.size.height)
                .isEqualTo(DropdownMenuVerticalPadding.roundToPx() * 2 + 200)
        }
    }

    @Test
    fun menu_scrolledContent() {
        // TODO This test is disabled on API 33+. See b/335809611. Once popups are handled correctly
        //  with the introduction of a default edge to edge policy, we should be able to re-enable
        //  it.
        Assume.assumeTrue(SDK_INT >= 33)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.requiredSize(20.toDp()).background(color = Color.Blue)) {
                    val scrollState = rememberScrollState()
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = {},
                        scrollState = scrollState
                    ) {
                        repeat(100) {
                            Box(Modifier.testTag("MenuContent ${it + 1}").size(70.toDp()))
                        }
                    }
                    LaunchedEffect(Unit) { scrollState.scrollTo(scrollState.maxValue) }
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag("MenuContent 1").assertIsNotDisplayed()
        rule.onNodeWithTag("MenuContent 100").assertIsDisplayed()
    }

    @Test
    fun dropdownMenuItem_onClick() {
        var clicked = false
        val onClick: () -> Unit = { clicked = true }

        rule.setContent {
            DropdownMenuItem(
                text = { Box(Modifier.requiredSize(40.dp)) },
                onClick,
                modifier = Modifier.testTag("MenuItem").clickable(onClick = onClick),
            )
        }

        rule.onNodeWithTag("MenuItem").performClick()

        rule.runOnIdle { assertThat(clicked).isTrue() }
    }
}
