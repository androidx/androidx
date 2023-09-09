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

package androidx.tv.material3

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalTvMaterial3Api::class)
class TabRowScreenshotTest(private val scheme: ColorSchemeWrapper) {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(TV_GOLDEN_MATERIAL3)
    private val wrapperTestTag = "tabRowWrapper"

    private val wrapperModifier = Modifier
        .testTag(wrapperTestTag)
        .background(if (scheme.name == lightThemeName) Color.White else Color.Black)
        .padding(20.dp)

    @Test
    fun tabRow_withPillIndicator_inactive() {
        val tabs = listOf("Home", "Movies", "Shows")
        rule.setMaterialContent(scheme.colorScheme) {
            var selectedTabIndex by remember { mutableStateOf(0) }

            Surface(
                modifier = wrapperModifier,
                colors = NonInteractiveSurfaceDefaults.colors(
                    containerColor = Color.Transparent
                ),
                shape = RectangleShape
            ) {
                TabRow(selectedTabIndex = selectedTabIndex, containerColor = Color.Transparent) {
                    tabs.forEachIndexed { index, text ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onFocus = { selectedTabIndex = index },
                        ) {
                            Text(
                                text = text,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 6.dp
                                    )
                            )
                        }
                    }
                }
            }
        }

        assertAgainstGolden("tabRow_withPillIndicator_inactive_${scheme.name}")
    }

    @Test
    fun tabRow_withPillIndicator_active() {
        val tabs = listOf("Home", "Movies", "Shows")
        val focusRequester = FocusRequester()
        rule.setMaterialContent(scheme.colorScheme) {
            var selectedTabIndex by remember { mutableStateOf(0) }

            Surface(
                modifier = wrapperModifier.focusRequester(focusRequester),
                colors = NonInteractiveSurfaceDefaults.colors(
                    containerColor = Color.Transparent
                ),
                shape = RectangleShape
            ) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, text ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onFocus = { selectedTabIndex = index },
                        ) {
                            Text(
                                text = text,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 6.dp
                                    )
                            )
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }
        rule.waitForIdle()

        assertAgainstGolden("tabRow_withPillIndicator_active_${scheme.name}")
    }

    @Test
    fun tabRow_withPillIndicator_disabledTabs() {
        val tabs = listOf("Home", "Movies", "Shows")
        val focusRequester = FocusRequester()

        rule.setMaterialContent(scheme.colorScheme) {
            var selectedTabIndex by remember { mutableStateOf(0) }

            Surface(
                modifier = wrapperModifier.focusRequester(focusRequester),
                colors = NonInteractiveSurfaceDefaults.colors(
                    containerColor = Color.Transparent
                ),
                shape = RectangleShape
            ) {
                TabRow(selectedTabIndex = selectedTabIndex, containerColor = Color.Transparent) {
                    tabs.forEachIndexed { index, text ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onFocus = { selectedTabIndex = index },
                            enabled = index != 2, // 3rd tab is disabled
                        ) {
                            Text(
                                text = text,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 6.dp
                                    )
                            )
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }
        rule.waitForIdle()

        assertAgainstGolden("tabRow_withPillIndicator_disabledTabs_${scheme.name}")
    }

    @Test
    fun tabRow_withUnderlinedIndicator_inactive() {
        val tabs = listOf("Home", "Movies", "Shows")
        rule.setMaterialContent(scheme.colorScheme) {
            var selectedTabIndex by remember { mutableStateOf(0) }

            Surface(
                modifier = wrapperModifier,
                colors = NonInteractiveSurfaceDefaults.colors(
                    containerColor = Color.Transparent
                ),
                shape = RectangleShape
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    separator = { Spacer(modifier = Modifier.width(12.dp)) },
                    indicator = { tabPositions, doesTabRowHaveFocus ->
                        TabRowDefaults.UnderlinedIndicator(
                            currentTabPosition = tabPositions[selectedTabIndex],
                            doesTabRowHaveFocus = doesTabRowHaveFocus,
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onFocus = { selectedTabIndex = index },
                            colors = TabDefaults.underlinedIndicatorTabColors(),
                        ) {
                            Text(
                                text = tab,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        assertAgainstGolden("tabRow_withUnderlinedIndicator_inactive_${scheme.name}")
    }

    @Test
    fun tabRow_withUnderlinedIndicator_active() {
        val tabs = listOf("Home", "Movies", "Shows")
        val focusRequester = FocusRequester()
        rule.setMaterialContent(scheme.colorScheme) {
            var selectedTabIndex by remember { mutableStateOf(0) }

            Surface(
                modifier = wrapperModifier.focusRequester(focusRequester),
                colors = NonInteractiveSurfaceDefaults.colors(
                    containerColor = Color.Transparent
                ),
                shape = RectangleShape
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    separator = { Spacer(modifier = Modifier.width(12.dp)) },
                    indicator = { tabPositions, doesTabRowHaveFocus ->
                        TabRowDefaults.UnderlinedIndicator(
                            currentTabPosition = tabPositions[selectedTabIndex],
                            doesTabRowHaveFocus = doesTabRowHaveFocus,
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onFocus = { selectedTabIndex = index },
                            colors = TabDefaults.underlinedIndicatorTabColors(),
                        ) {
                            Text(
                                text = tab,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }
        rule.waitForIdle()

        assertAgainstGolden("tabRow_withUnderlinedIndicator_active_${scheme.name}")
    }

    @Test
    fun tabRow_withUnderlinedIndicator_disabledTabs() {
        val tabs = listOf("Home", "Movies", "Shows")
        val focusRequester = FocusRequester()

        rule.setMaterialContent(scheme.colorScheme) {
            var selectedTabIndex by remember { mutableStateOf(0) }

            Surface(
                modifier = wrapperModifier.focusRequester(focusRequester),
                colors = NonInteractiveSurfaceDefaults.colors(
                    containerColor = Color.Transparent
                ),
                shape = RectangleShape
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    separator = { Spacer(modifier = Modifier.width(12.dp)) },
                    indicator = { tabPositions, doesTabRowHaveFocus ->
                        TabRowDefaults.UnderlinedIndicator(
                            currentTabPosition = tabPositions[selectedTabIndex],
                            doesTabRowHaveFocus = doesTabRowHaveFocus,
                        )
                    },
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onFocus = { selectedTabIndex = index },
                            colors = TabDefaults.underlinedIndicatorTabColors(),
                            enabled = index != 2, // 3rd tab is disabled
                        ) {
                            Text(
                                text = tab,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }
        rule.waitForIdle()

        assertAgainstGolden("tabRow_withUnderlinedIndicator_disabledTabs_${scheme.name}")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    // Provide the ColorScheme and their name parameter in a ColorSchemeWrapper.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        @OptIn(ExperimentalTvMaterial3Api::class)
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = arrayOf(
            ColorSchemeWrapper(lightThemeName, lightColorScheme()),
            ColorSchemeWrapper(darkThemeName, darkColorScheme()),
        )
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    class ColorSchemeWrapper constructor(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}

private const val lightThemeName = "lightTheme"
private const val darkThemeName = "darkTheme"
