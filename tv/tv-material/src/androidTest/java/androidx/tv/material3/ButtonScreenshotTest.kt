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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
class ButtonScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(TV_GOLDEN_MATERIAL3)

    @Test
    fun default_button_light_theme() {
        rule.setContent { LightMaterialTheme { Button(onClick = {}) { Text("Button") } } }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_light_theme")
    }

    @Test
    fun default_button_dark_theme() {
        rule.setContent { DarkMaterialTheme { Button(onClick = {}) { Text("Button") } } }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_dark_theme")
    }

    @Test
    fun disabled_button_light_theme() {
        rule.setContent {
            LightMaterialTheme { Button(onClick = {}, enabled = false) { Text("Button") } }
        }

        rule
            .onNodeWithText("Button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_disabled_light_theme")
    }

    @Test
    fun disabled_button_dark_theme() {
        rule.setContent {
            DarkMaterialTheme { Button(onClick = {}, enabled = false) { Text("Button") } }
        }

        rule
            .onNodeWithText("Button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_disabled_dark_theme")
    }

    @Test
    fun outlined_button_lightTheme() {
        rule.setContent {
            LightMaterialTheme { OutlinedButton(onClick = {}) { Text("Outlined Button") } }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "outlined_button_light_theme")
    }

    @Test
    fun outlined_button_darkTheme() {
        rule.setContent {
            DarkMaterialTheme { OutlinedButton(onClick = {}) { Text("Outlined Button") } }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "outlined_button_dark_theme")
    }

    @Test
    fun disabled_outlined_button_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.testTag("button")
                ) {
                    Text("Outlined Button")
                }
            }
        }

        rule
            .onNodeWithTag("button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "outlined_button_disabled_light_theme")
    }

    @Test
    fun disabled_outlined_button_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.testTag("button")
                ) {
                    Text("Outlined Button")
                }
            }
        }

        rule
            .onNodeWithTag("button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "outlined_button_disabled_dark_theme")
    }

    @Test
    fun button_withIcon_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                Button(
                    onClick = { /* Do something! */ },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Localized description",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Like")
                }
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_withIcon_lightTheme")
    }

    @Test
    fun button_withIcon_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                Button(
                    onClick = { /* Do something! */ },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Localized description",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Like")
                }
            }
        }

        rule
            .onNode(hasClickAction())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_withIcon_darkTheme")
    }

    @Test
    fun disabled_button_withIcon_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                Button(
                    onClick = { /* Do something! */ },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    enabled = false,
                    modifier = Modifier.testTag("button")
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Localized description",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Like")
                }
            }
        }

        rule
            .onNodeWithTag("button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_withIcon_disabled_lightTheme")
    }

    @Test
    fun disabled_button_withIcon_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                Button(
                    onClick = { /* Do something! */ },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    enabled = false,
                    modifier = Modifier.testTag("button")
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Localized description",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Like")
                }
            }
        }

        rule
            .onNodeWithTag("button")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "button_withIcon_disabled_darkTheme")
    }
}
