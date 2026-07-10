/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.xr.glimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class IconButtonScreenshotTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Before
    fun setUp() {
        rule.mainClock.autoAdvance = false
    }

    @After
    fun tearDown() {
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun iconButton() {
        rule.setGlimmerThemeContent { IconButton(onClick = {}) { Icon(FavoriteIcon, null) } }
        rule.assertRootAgainstGolden("icon_button", screenshotRule)
    }

    @Test
    fun iconButton_focused() {
        rule.setGlimmerThemeContent {
            DepthWrapper {
                IconButton(onClick = {}, interactionSource = AlwaysFocusedInteractionSource) {
                    Icon(FavoriteIcon, null)
                }
            }
        }
        // Advance past the animation
        rule.mainClock.advanceTimeBy(10_000)
        rule.assertRootAgainstGolden("icon_button_focused", screenshotRule)
    }

    /**
     * Practically a Button cannot be pressed without also being focused, but we test them in
     * isolation as well to make it easier to identify changes. See [iconButton_focused_and_pressed]
     * for the combined state.
     */
    @Test
    fun iconButton_pressed() {
        rule.setGlimmerThemeContent {
            DepthWrapper {
                IconButton(onClick = {}, interactionSource = AlwaysPressedInteractionSource) {
                    Icon(FavoriteIcon, null)
                }
            }
        }
        // Skip until after the animation has finished
        rule.mainClock.advanceTimeBy(10_000)
        rule.assertRootAgainstGolden("icon_button_pressed", screenshotRule)
    }

    @Test
    fun iconButton_focused_and_pressed() {
        rule.setGlimmerThemeContent {
            DepthWrapper {
                IconButton(
                    onClick = {},
                    interactionSource = AlwaysFocusedAndPressedInteractionSource,
                ) {
                    Icon(FavoriteIcon, null)
                }
            }
        }
        // Advance past the animation
        rule.mainClock.advanceTimeBy(10_000)
        rule.assertRootAgainstGolden("icon_button_focused_and_pressed", screenshotRule)
    }

    /** Add an extra box with a white background around the button to allow capturing depth. */
    @Composable
    private fun DepthWrapper(content: @Composable () -> Unit) {
        Box(
            modifier = Modifier.background(color = Color.White, RectangleShape).padding(25.dp),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    }
}
