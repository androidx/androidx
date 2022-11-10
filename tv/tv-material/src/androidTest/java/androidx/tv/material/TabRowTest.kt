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

package androidx.tv.material

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class TabRowTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tabRow_firstTabIsSelected() {
        val tabs = constructTabs()
        val firstTab = tabs[0]

        setContent(tabs)

        rule.onNodeWithTag(firstTab).assertIsFocused()
    }

    @Test
    fun tabRow_dPadRightMovesFocusToSecondTab() {
        val tabs = constructTabs()
        val firstTab = tabs[0]
        val secondTab = tabs[1]

        setContent(tabs)

        // First tab should be focused
        rule.onNodeWithTag(firstTab).assertIsFocused()

        rule.waitForIdle()

        // Move to next tab
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        rule.waitForIdle()

        // Second tab should be focused
        rule.onNodeWithTag(secondTab).assertIsFocused()
    }

    @Test
    fun tabRow_dPadLeftMovesFocusToPreviousTab() {
        val tabs = constructTabs()
        val firstTab = tabs[0]
        val secondTab = tabs[1]
        val thirdTab = tabs[2]

        setContent(tabs)

        // First tab should be focused
        rule.onNodeWithTag(firstTab).assertIsFocused()

        rule.waitForIdle()

        // Move to next tab
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        rule.waitForIdle()

        // Second tab should be focused
        rule.onNodeWithTag(secondTab).assertIsFocused()

        // Move to next tab
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        rule.waitForIdle()

        // Third tab should be focused
        rule.onNodeWithTag(thirdTab).assertIsFocused()

        // Move to previous tab
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_LEFT)

        rule.waitForIdle()

        // Second tab should be focused
        rule.onNodeWithTag(secondTab).assertIsFocused()

        // Move to previous tab
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_LEFT)

        rule.waitForIdle()

        // First tab should be focused
        rule.onNodeWithTag(firstTab).assertIsFocused()
    }

    private fun setContent(tabs: List<String>) {
        val fr = FocusRequester()

        rule.setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                var selectedTabIndex by remember { mutableStateOf(0) }

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    separator = { Spacer(modifier = Modifier.width(12.dp)) }
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onSelect = { selectedTabIndex = index },
                            modifier = Modifier
                                .width(100.dp)
                                .height(50.dp)
                                .testTag(tab)
                                .border(2.dp, Color.White, RoundedCornerShape(50))
                        ) {}
                    }
                }

                // Added so that this can get focus and pass it to the tab row
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .focusRequester(fr)
                        .background(Color.White)
                        .focusable()
                )

                // Send focus to button
                LaunchedEffect(Unit) {
                    fr.requestFocus()
                }
            }
        }

        rule.waitForIdle()

        // Move the focus TabRow
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_UP)

        rule.waitForIdle()
    }

    private fun performKeyPress(keyCode: Int, count: Int = 1) {
        for (i in 1..count) {
            InstrumentationRegistry
                .getInstrumentation()
                .sendKeyDownUpSync(keyCode)
        }
    }

    private fun constructTabs(
        count: Int = 3,
        buildTab: (index: Int) -> String = { "Season $it" }
    ): List<String> = (0 until count).map(buildTab)
}