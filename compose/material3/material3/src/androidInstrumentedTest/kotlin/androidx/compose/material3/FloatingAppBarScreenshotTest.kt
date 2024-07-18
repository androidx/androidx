/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class FloatingAppBarScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun horizontalFloatingAppBar_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                HorizontalFloatingAppBar(expanded = false) { content() }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "horizontalFloatingAppBar_lightTheme")
    }

    @Test
    fun horizontalFloatingAppBar_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                HorizontalFloatingAppBar(expanded = false) { content() }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "horizontalFloatingAppBar_darkTheme")
    }

    @Test
    fun verticalFloatingAppBar_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                VerticalFloatingAppBar(expanded = false) { content() }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "verticalFloatingAppBar_lightTheme")
    }

    @Test
    fun verticalFloatingAppBar_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                VerticalFloatingAppBar(expanded = false) { content() }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "verticalFloatingAppBar_darkTheme")
    }

    @Test
    fun horizontalFloatingAppBar_leading_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                HorizontalFloatingAppBar(expanded = true, leadingContent = { Text("leading") }) {
                    content()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "horizontalFloatingAppBar_leading_lightTheme")
    }

    @Test
    fun horizontalFloatingAppBar_trailing_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                HorizontalFloatingAppBar(expanded = true, trailingContent = { Text("trailing") }) {
                    content()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "horizontalFloatingAppBar_trailing_lightTheme")
    }

    @Test
    fun horizontalFloatingAppBar_leading_trailing_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                HorizontalFloatingAppBar(
                    expanded = true,
                    leadingContent = { Text("leading") },
                    trailingContent = { Text("trailing") }
                ) {
                    content()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingAppBar_leading_trailing_lightTheme"
            )
    }

    @Test
    fun horizontalFloatingAppBar_leading_trailing_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                HorizontalFloatingAppBar(
                    expanded = true,
                    leadingContent = { Text("leading") },
                    trailingContent = { Text("trailing") }
                ) {
                    content()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingAppBar_leading_trailing_darkTheme"
            )
    }

    @Test
    fun horizontalFloatingAppBar_leading_trailing_collapsed_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                HorizontalFloatingAppBar(
                    expanded = false,
                    leadingContent = { Text("leading") },
                    trailingContent = { Text("trailing") }
                ) {
                    content()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingAppBar_leading_trailing_collapsed_lightTheme"
            )
    }

    @Test
    fun horizontalFloatingAppBar_leading_trailing_rtl_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.testTag(FloatingAppBarTestTag)) {
                    HorizontalFloatingAppBar(
                        expanded = true,
                        leadingContent = { Text("leading") },
                        trailingContent = { Text("trailing") }
                    ) {
                        content()
                    }
                }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "horizontalFloatingAppBar_leading_trailing_rtl_lightTheme"
            )
    }

    @Test
    fun verticalFloatingAppBar_leading_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                VerticalFloatingAppBar(
                    expanded = true,
                    leadingContent = { Text(text = "leading") }
                ) {
                    content()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "verticalFloatingAppBar_leading_lightTheme")
    }

    @Test
    fun verticalFloatingAppBar_trailing_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                VerticalFloatingAppBar(
                    expanded = true,
                    trailingContent = { Text(text = "trailing") }
                ) {
                    content()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "verticalFloatingAppBar_trailing_lightTheme")
    }

    @Test
    fun verticalFloatingAppBar_leading_trailing_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                VerticalFloatingAppBar(
                    expanded = true,
                    leadingContent = { Text(text = "leading") },
                    trailingContent = { Text(text = "trailing") }
                ) {
                    content()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingAppBar_leading_trailing_lightTheme"
            )
    }

    @Test
    fun verticalFloatingAppBar_leading_trailing_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                VerticalFloatingAppBar(
                    expanded = true,
                    leadingContent = { Text(text = "leading") },
                    trailingContent = { Text(text = "trailing") }
                ) {
                    content()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingAppBar_leading_trailing_darkTheme"
            )
    }

    @Test
    fun verticalFloatingAppBar_leading_trailing_collapsed_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(FloatingAppBarTestTag)) {
                VerticalFloatingAppBar(
                    expanded = false,
                    leadingContent = { Text(text = "leading") },
                    trailingContent = { Text(text = "trailing") }
                ) {
                    content()
                }
            }
        }

        rule
            .onNodeWithTag(FloatingAppBarTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "verticalFloatingAppBar_leading_trailing_collapsed_lightTheme"
            )
    }

    @Composable
    private fun content() {
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Filled.Check, contentDescription = "Localized description")
        }
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Filled.Edit, contentDescription = "Localized description")
        }
        FilledIconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Outlined.Favorite, contentDescription = "Localized description")
        }
        IconButton(onClick = { /* doSomething() */ }) {
            Icon(Icons.Filled.Add, contentDescription = "Localized description")
        }
    }

    private val FloatingAppBarTestTag = "floatingAppBar"
}
