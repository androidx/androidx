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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ButtonScreenshotTest() {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Test
    fun button() {
        rule.setGlimmerThemeContent { Button(onClick = {}) { Text("Send") } }
        rule.assertRootAgainstGolden("button", screenshotRule)
    }

    @Test
    fun button_withIcons() {
        rule.setGlimmerThemeContent {
            Button(
                onClick = {},
                leadingIcon = { Icon(FavoriteIcon, null) },
                trailingIcon = { Icon(FavoriteIcon, null) },
            ) {
                Text("Send")
            }
        }
        rule.assertRootAgainstGolden("button_withIcons", screenshotRule)
    }

    @Test
    fun button_buttonSizeLarge() {
        rule.setGlimmerThemeContent {
            Button(onClick = {}, buttonSize = ButtonSize.Large) { Text("Send") }
        }
        rule.assertRootAgainstGolden("button_large", screenshotRule)
    }

    @Test
    fun button_buttonSizeLarge_withIcons() {
        rule.setGlimmerThemeContent {
            Button(
                onClick = {},
                buttonSize = ButtonSize.Large,
                leadingIcon = { Icon(FavoriteIcon, null) },
                trailingIcon = { Icon(FavoriteIcon, null) },
            ) {
                Text("Send")
            }
        }
        rule.assertRootAgainstGolden("button_large_withIcons", screenshotRule)
    }

    @Test
    fun button_focused() {
        rule.mainClock.autoAdvance = false
        rule.setGlimmerThemeContent {
            Button(onClick = {}, interactionSource = AlwaysFocusedInteractionSource) {
                Text("Send")
            }
        }
        // Advance past the animation
        rule.mainClock.advanceTimeBy(10000)
        rule.assertRootAgainstGolden("button_focused", screenshotRule)
    }

    /**
     * Practically a Button cannot be pressed without also being focused, but we test them in
     * isolation as well to make it easier to identify changes. See [button_focused_and_pressed] for
     * the combined state.
     */
    @Test
    fun button_pressed() {
        rule.mainClock.autoAdvance = false
        rule.setGlimmerThemeContent {
            Button(onClick = {}, interactionSource = AlwaysPressedInteractionSource) {
                Text("Send")
            }
        }
        // Skip until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)
        rule.assertRootAgainstGolden("button_pressed", screenshotRule)
    }

    @Test
    fun button_focused_and_pressed() {
        rule.mainClock.autoAdvance = false
        rule.setGlimmerThemeContent {
            Button(onClick = {}, interactionSource = AlwaysFocusedAndPressedInteractionSource) {
                Text("Send")
            }
        }
        // Advance past the animation
        rule.mainClock.advanceTimeBy(10000)
        rule.assertRootAgainstGolden("button_focused_and_pressed", screenshotRule)
    }
}

/** Icon taken from material-icons-core */
internal val FavoriteIcon: ImageVector =
    ImageVector.Builder(
            name = "Favorite",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        .apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12.0f, 21.35f)
                lineToRelative(-1.45f, -1.32f)
                curveTo(5.4f, 15.36f, 2.0f, 12.28f, 2.0f, 8.5f)
                curveTo(2.0f, 5.42f, 4.42f, 3.0f, 7.5f, 3.0f)
                curveToRelative(1.74f, 0.0f, 3.41f, 0.81f, 4.5f, 2.09f)
                curveTo(13.09f, 3.81f, 14.76f, 3.0f, 16.5f, 3.0f)
                curveTo(19.58f, 3.0f, 22.0f, 5.42f, 22.0f, 8.5f)
                curveToRelative(0.0f, 3.78f, -3.4f, 6.86f, -8.55f, 11.54f)
                lineTo(12.0f, 21.35f)
                close()
            }
        }
        .build()
