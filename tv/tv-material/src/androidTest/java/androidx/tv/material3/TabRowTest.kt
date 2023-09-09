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

package androidx.tv.material3

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class TabRowTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tabRow_shouldNotCrashWithOnly1Tab() {
        val tabs = constructTabs(count = 1)

        setContent(tabs)
    }

    @Test
    fun tabRow_shouldNotCrashWithNoTabs() {
        val tabs = constructTabs(count = 0)

        setContent(tabs)
    }

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

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Test
    fun tabRow_changeActiveTabOnClick() {
        val tabs = constructTabs(count = 2)

        val firstPanel = "Panel 1"
        val secondPanel = "Panel 2"

        setContent(
            tabs,
            contentBuilder = @Composable {
                var focusedTabIndex by remember { mutableStateOf(0) }
                var activeTabIndex by remember { mutableStateOf(focusedTabIndex) }
                TabRowSample(
                    tabs = tabs,
                    selectedTabIndex = activeTabIndex,
                    onFocus = { focusedTabIndex = it },
                    onClick = { activeTabIndex = it },
                    buildTabPanel = @Composable { index, _ ->
                        BasicText(text = "Panel ${index + 1}")
                    },
                    indicator = @Composable { tabPositions, doesTabRowHaveFocus ->
                        // FocusedTab's indicator
                        TabRowDefaults.PillIndicator(
                            currentTabPosition = tabPositions[focusedTabIndex],
                            doesTabRowHaveFocus = doesTabRowHaveFocus,
                            activeColor = Color.Blue.copy(alpha = 0.4f),
                            inactiveColor = Color.Transparent,
                        )

                        // SelectedTab's indicator
                        TabRowDefaults.PillIndicator(
                            currentTabPosition = tabPositions[activeTabIndex],
                            doesTabRowHaveFocus = doesTabRowHaveFocus,
                        )
                    }
                )
            }
        )

        rule.onNodeWithText(firstPanel).assertIsDisplayed()

        // Move focus to next tab
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT)

        rule.waitForIdle()

        rule.onNodeWithText(firstPanel).assertIsDisplayed()
        rule.onNodeWithText(secondPanel).assertDoesNotExist()

        // Click on the new focused tab
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_CENTER)

        rule.onNodeWithText(firstPanel).assertDoesNotExist()
        rule.onNodeWithText(secondPanel).assertIsDisplayed()
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    private fun setContent(
        tabs: List<String>,
        contentBuilder: @Composable () -> Unit = {
            var selectedTabIndex by remember { mutableStateOf(0) }
            TabRowSample(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onFocus = { selectedTabIndex = it }
            )
        },
    ) {
        rule.setContent {
            contentBuilder()
        }

        rule.waitForIdle()

        // Move the focus TabRow
        performKeyPress(NativeKeyEvent.KEYCODE_DPAD_DOWN)

        rule.waitForIdle()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TabRowSample(
    tabs: List<String>,
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    onFocus: (index: Int) -> Unit = {},
    onClick: (index: Int) -> Unit = onFocus,
    buildTab: @Composable (TabRowScope.(index: Int, tab: String) -> Unit) =
        @Composable { index, tab ->
            TabSample(
                selected = selectedTabIndex == index,
                onFocus = { onFocus(index) },
                onClick = { onClick(index) },
                modifier = Modifier.testTag(tab),
            )
        },
    indicator: @Composable ((tabPositions: List<DpRect>, isTabRowActive: Boolean) -> Unit)? = null,
    buildTabPanel: @Composable ((index: Int, tab: String) -> Unit) = @Composable { _, tab ->
        BasicText(text = tab)
    },
) {
    val fr = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
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

        if (indicator != null) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = modifier,
                indicator = indicator,
                separator = { Spacer(modifier = Modifier.width(12.dp)) },
            ) {
                tabs.forEachIndexed { index, tab -> buildTab(index, tab) }
            }
        } else {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = modifier,
                separator = { Spacer(modifier = Modifier.width(12.dp)) },
            ) {
                tabs.forEachIndexed { index, tab -> buildTab(index, tab) }
            }
        }

        tabs.elementAtOrNull(selectedTabIndex)?.let { buildTabPanel(selectedTabIndex, it) }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TabRowScope.TabSample(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onClick: () -> Unit = {},
    tag: String = "Tab",
) {
    Tab(
        selected = selected,
        onFocus = onFocus,
        onClick = onClick,
        modifier = modifier
            .width(100.dp)
            .height(50.dp)
            .testTag(tag)
            .border(2.dp, Color.White, RoundedCornerShape(50))
    ) {}
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
): List<String> = List(count, buildTab)
