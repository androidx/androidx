/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.material

import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.TestTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.preferredSize
import androidx.ui.layout.size
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.testTag
import androidx.ui.test.createComposeRule
import androidx.ui.test.find
import androidx.ui.test.findByTag
import androidx.ui.test.hasAnyChildThat
import androidx.ui.test.hasTestTag
import androidx.ui.test.isPopup
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.waitForIdle
import androidx.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class MenuTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun menu_canBeTriggered() {
        var expanded by mutableStateOf(false)

        composeTestRule.clockTestRule.pauseClock()
        composeTestRule.setContent {
            DropdownMenu(
                expanded = expanded,
                toggle = {
                    Box(Modifier.size(20.dp).drawBackground(Color.Blue))
                },
                onDismissRequest = {}
            ) {
                TestTag("MenuContent") {
                    DropdownMenuItem(onClick = {}) {
                        Text("Option 1")
                    }
                }
            }
        }
        findByTag("MenuContent").assertDoesNotExist()

        runOnIdleCompose { expanded = true }
        waitForIdle()
        composeTestRule.clockTestRule.advanceClock(InTransitionDuration.toLong())
        findByTag("MenuContent").assertExists()

        runOnIdleCompose { expanded = false }
        waitForIdle()
        composeTestRule.clockTestRule.advanceClock(OutTransitionDuration.toLong())
        findByTag("MenuContent").assertDoesNotExist()

        runOnIdleCompose { expanded = true }
        waitForIdle()
        composeTestRule.clockTestRule.advanceClock(InTransitionDuration.toLong())
        findByTag("MenuContent").assertExists()
    }

    @Test
    fun menu_hasExpectedSize() {
        composeTestRule.setContent {
            DropdownMenu(
                expanded = true,
                toggle = {
                    Box(Modifier.size(20.dp).drawBackground(Color.Blue))
                },
                onDismissRequest = {}
            ) {
                Semantics(properties = { testTag = "MenuContent1" }, container = true) {
                    Box(Modifier.preferredSize(70.dp))
                }
                Semantics(properties = { testTag = "MenuContent2" }, container = true) {
                    Box(Modifier.preferredSize(130.dp))
                }
            }
        }

        findByTag("MenuContent1").assertExists()
        findByTag("MenuContent2").assertExists()
        val node = find(
            isPopup() and hasAnyChildThat(hasTestTag("MenuContent1")) and
                    hasAnyChildThat(hasTestTag("MenuContent2"))
        ).assertExists().fetchSemanticsNode()
        with(composeTestRule.density) {
            assertThat(node.size.width).isEqualTo(130.dp.toIntPx() + MenuElevation.toIntPx() * 2)
            assertThat(node.size.height).isEqualTo(200.dp.toIntPx() +
                    DropdownMenuVerticalPadding.toIntPx() * 2 + MenuElevation.toIntPx() * 2
            )
        }
    }
}
