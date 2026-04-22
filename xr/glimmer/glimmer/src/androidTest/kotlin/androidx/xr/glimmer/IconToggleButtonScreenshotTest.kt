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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class IconToggleButtonScreenshotTest(private val parameters: ToggleButtonStates) {

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
    fun iconToggleButton() {
        rule.setGlimmerThemeContent {
            DepthWrapper {
                IconToggleButton(
                    checked = parameters.checked,
                    onCheckedChange = {},
                    interactionSource = parameters.interactionSource(),
                ) {
                    Icon(FavoriteIcon, null)
                }
            }
        }
        rule.mainClock.advanceTimeBy(10_000)
        rule.assertRootAgainstGolden(
            "icon_toggle_button_${parameters.goldenNameSuffix()}",
            screenshotRule,
        )
    }

    /** Add an extra box with a white background around the button to allow capturing depth. */
    @Composable
    private fun DepthWrapper(content: @Composable () -> Unit) {
        Box(
            modifier = Modifier.background(color = Color.White, RectangleShape).padding(20.dp),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    }

    class ToggleButtonStates(val checked: Boolean, val focused: Boolean, val pressed: Boolean) {
        fun goldenNameSuffix(): String =
            listOfNotNull(
                    if (checked) "checked" else "unchecked",
                    if (focused) "focused" else "unfocused",
                    if (pressed) "pressed" else null,
                )
                .joinToString("_")

        fun interactionSource(): MutableInteractionSource? {
            if (focused && pressed) return AlwaysFocusedAndPressedInteractionSource
            if (focused) return AlwaysFocusedInteractionSource
            if (pressed) return AlwaysPressedInteractionSource
            return null
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Array<ToggleButtonStates> {
            val params = mutableListOf<ToggleButtonStates>()
            for (checked in listOf(false, true)) {
                for (focused in listOf(false, true)) {
                    for (pressed in listOf(false, true)) {
                        params.add(ToggleButtonStates(checked, focused, pressed))
                    }
                }
            }
            return params.toTypedArray()
        }
    }
}
