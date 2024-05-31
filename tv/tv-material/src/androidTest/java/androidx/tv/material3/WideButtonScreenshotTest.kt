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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTvMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class WideButtonScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(TV_GOLDEN_MATERIAL3)

    @Test
    fun defaultWideButton_lightTheme() {
        rule.setContent { LightMaterialTheme { WideButton(onClick = {}) { Text("Settings") } } }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "wide_button_light_theme")
    }

    @Test
    fun defaultWideButton_darkTheme() {
        rule.setContent { DarkMaterialTheme { WideButton(onClick = {}) { Text("Settings") } } }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "wide_button_dark_theme")
    }

    @Test
    fun disabled_wideButton_lightTheme() {
        rule.setContent {
            LightMaterialTheme { WideButton(onClick = {}, enabled = false) { Text("Settings") } }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "wide_button_disabled_light_theme")
    }

    @Test
    fun disabled_wideButton_darkTheme() {
        rule.setContent {
            DarkMaterialTheme { WideButton(onClick = {}, enabled = false) { Text("Settings") } }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "wide_button_disabled_dark_theme")
    }

    @Test
    fun wideButton_withSubtitle_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                WideButton(
                    onClick = {},
                    title = { Text("Settings") },
                    subtitle = { Text(text = "Update device preferences") },
                )
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "wide_button_with_subtitle_light_theme")
    }

    @Test
    fun wideButton_withSubtitle_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                WideButton(
                    onClick = {},
                    title = { Text("Settings") },
                    subtitle = { Text(text = "Update device preferences") },
                )
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "wide_button_with_subtitle_dark_theme")
    }

    @Test
    fun disabled_wideButton_withSubtitle_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                WideButton(
                    onClick = {},
                    enabled = false,
                    title = { Text("Settings") },
                    subtitle = { Text(text = "Update device preferences") },
                )
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "disabled_wide_button_with_subtitle_light_theme")
    }

    @Test
    fun disabled_wideButton_withSubtitle_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                WideButton(
                    onClick = {},
                    enabled = false,
                    title = { Text("Settings") },
                    subtitle = { Text(text = "Update device preferences") },
                )
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "disabled_wide_button_with_subtitle_dark_theme")
    }

    @Test
    fun wideButton_withIcon_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                WideButton(
                    onClick = {},
                    title = { Text("Settings") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                )
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "wide_button_with_icon_light_theme")
    }

    @Test
    fun wideButton_withIcon_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                WideButton(
                    onClick = {},
                    title = { Text("Settings") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                )
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "wide_button_with_icon_dark_theme")
    }

    @Test
    fun disabled_wideButton_withIcon_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                WideButton(
                    onClick = {},
                    enabled = false,
                    title = { Text("Settings") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                )
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "disabled_wide_button_with_icon_light_theme")
    }

    @Test
    fun disabled_wideButton_withIcon_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                WideButton(
                    onClick = {},
                    enabled = false,
                    title = { Text("Settings") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                )
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "disabled_wide_button_with_icon_dark_theme")
    }

    @Test
    fun wideButton_withSubtitleAndIcon_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                WideButton(
                    onClick = {},
                    title = { Text("Settings") },
                    subtitle = { Text(text = "Update device preferences") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                )
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "wide_button_with_subtitle_and_icon_light_theme")
    }

    @Test
    fun wideButton_withSubtitleAndIcon_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                WideButton(
                    onClick = {},
                    title = { Text("Settings") },
                    subtitle = { Text(text = "Update device preferences") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                )
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "wide_button_with_subtitle_and_icon_dark_theme")
    }

    @Test
    fun disabled_wideButton_withSubtitleAndIcon_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                WideButton(
                    onClick = {},
                    enabled = false,
                    title = { Text("Settings") },
                    subtitle = { Text(text = "Update device preferences") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                )
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "disabled_wide_button_with_subtitle_and_icon_light_theme"
            )
    }

    @Test
    fun disabled_wideButton_withSubtitleAndIcon_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                WideButton(
                    onClick = {},
                    enabled = false,
                    title = { Text("Settings") },
                    subtitle = { Text(text = "Update device preferences") },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                )
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "disabled_wide_button_with_subtitle_and_icon_dark_theme"
            )
    }
}
