/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ListItemScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Test
    fun listItem() {
        rule.setGlimmerThemeContent { ListItem { Text("Primary Label") } }
        rule.assertRootAgainstGolden("listItem", screenshotRule)
    }

    @Test
    fun listItem_supportingLabel() {
        rule.setGlimmerThemeContent {
            ListItem(supportingLabel = { Text("Supporting Label") }) { Text("Primary Label") }
        }
        rule.assertRootAgainstGolden("listItem_supportingLabel", screenshotRule)
    }

    @Test
    fun listItem_withIcons() {
        rule.setGlimmerThemeContent {
            ListItem(
                leadingIcon = { Icon(FavoriteIcon, contentDescription = null) },
                trailingIcon = { Icon(FavoriteIcon, contentDescription = null) },
            ) {
                Text("Primary Label")
            }
        }
        rule.assertRootAgainstGolden("listItem_withIcons", screenshotRule)
    }

    @Test
    fun listItem_supportingLabel_withIcons() {
        rule.setGlimmerThemeContent {
            ListItem(
                supportingLabel = { Text("Supporting Label") },
                leadingIcon = { Icon(FavoriteIcon, contentDescription = null) },
                trailingIcon = { Icon(FavoriteIcon, contentDescription = null) },
            ) {
                Text("Primary Label")
            }
        }
        rule.assertRootAgainstGolden("listItem_supportingLabel_withIcons", screenshotRule)
    }

    @Test
    fun listItem_withIcons_longText() {
        rule.setGlimmerThemeContent {
            ListItem(
                leadingIcon = { Icon(FavoriteIcon, contentDescription = null) },
                trailingIcon = { Icon(FavoriteIcon, contentDescription = null) },
            ) {
                Text("Primary label with some very long text that will wrap to multiple lines")
            }
        }
        rule.assertRootAgainstGolden("listItem_withIcons_longText", screenshotRule)
    }

    @Test
    fun listItem_supportingLabel_withIcons_longText() {
        rule.setGlimmerThemeContent {
            ListItem(
                supportingLabel = { Text("Supporting Label") },
                leadingIcon = { Icon(FavoriteIcon, contentDescription = null) },
                trailingIcon = { Icon(FavoriteIcon, contentDescription = null) },
            ) {
                Text("Primary label with some very long text that will wrap to multiple lines")
            }
        }
        rule.assertRootAgainstGolden("listItem_supportingLabel_withIcons_longText", screenshotRule)
    }

    @Test
    fun listItem_focused() {
        rule.mainClock.autoAdvance = false
        rule.setGlimmerThemeContent {
            ListItem(interactionSource = AlwaysFocusedInteractionSource) { Text("Primary Label") }
        }
        // Advance past the animation
        rule.mainClock.advanceTimeBy(10000)
        rule.assertRootAgainstGolden("listItem_focused", screenshotRule)
    }

    /**
     * Practically a ListItem cannot be pressed without also being focused, but we test them in
     * isolation as well to make it easier to identify changes. See [listItem_focused_and_pressed]
     * for the combined state.
     */
    @Test
    fun listItem_pressed() {
        rule.mainClock.autoAdvance = false
        rule.setGlimmerThemeContent {
            ListItem(onClick = {}, interactionSource = AlwaysPressedInteractionSource) {
                Text("Primary Label")
            }
        }
        // Skip until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)
        rule.assertRootAgainstGolden("listItem_pressed", screenshotRule)
    }

    @Test
    fun listItem_focused_and_pressed() {
        rule.mainClock.autoAdvance = false
        rule.setGlimmerThemeContent {
            ListItem(onClick = {}, interactionSource = AlwaysFocusedAndPressedInteractionSource) {
                Text("Primary Label")
            }
        }
        // Advance past the animation
        rule.mainClock.advanceTimeBy(10000)
        rule.assertRootAgainstGolden("listItem_focused_and_pressed", screenshotRule)
    }
}
