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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
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
class ListItemScreenshotTest(private val scheme: ColorSchemeWrapper) {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(TV_GOLDEN_MATERIAL3)

    val wrapperModifier = Modifier
        .testTag(ListItemWrapperTag)
        .background(scheme.colorScheme.surface)
        .padding(20.dp)

    @Test
    fun listItem_customColor() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                ListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("One line list item") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSize)
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Red)
                )
            }
        }

        assertAgainstGolden("listItem_${scheme.name}_customColor")
    }

    @Test
    fun listItem_oneLine() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                ListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("One line list item") }
                )
            }
        }

        assertAgainstGolden("listItem_${scheme.name}_oneLine")
    }

    @Test
    fun listItem_oneLine_withIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                ListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("One line list item") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSize)
                        )
                    }
                )
            }
        }

        assertAgainstGolden("listItem_${scheme.name}_oneLine_withIcon")
    }

    @Test
    fun listItem_twoLine() {
        rule.setMaterialContent(scheme.colorScheme) {
            Column(
                modifier = wrapperModifier,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Two line list item") },
                    supportingContent = { Text("Secondary text") }
                )
                ListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Two line list item") },
                    overlineContent = { Text("OVERLINE") }
                )
            }
        }

        assertAgainstGolden("listItem_${scheme.name}_twoLine")
    }

    @Test
    fun listItem_twoLine_withIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Column(
                modifier = wrapperModifier,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Two line list item") },
                    supportingContent = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSize)
                        )
                    }
                )
                ListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Two line list item") },
                    overlineContent = { Text("OVERLINE") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSize)
                        )
                    }
                )
            }
        }

        assertAgainstGolden("listItem_${scheme.name}_twoLine_withIcon")
    }

    @Test
    fun listItem_threeLine() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                ListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Three line list item") },
                    overlineContent = { Text("OVERLINE") },
                    supportingContent = { Text("Secondary text") }
                )
            }
        }

        assertAgainstGolden("listItem_${scheme.name}_threeLine")
    }

    @Test
    fun listItem_threeLine_withIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                ListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Three line list item") },
                    overlineContent = { Text("OVERLINE") },
                    supportingContent = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSize)
                        )
                    }
                )
            }
        }

        assertAgainstGolden("listItem_${scheme.name}_threeLine_withIcon")
    }

    @Test
    fun listItem_threeLine_focused() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                ListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Three line list item") },
                    overlineContent = { Text("OVERLINE") },
                    supportingContent = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSize)
                        )
                    }
                )
            }
        }

        rule.onNodeWithTag(ListItemWrapperTag)
            .onChild()
            .requestFocus()
        rule.waitForIdle()

        assertAgainstGolden("listItem_${scheme.name}_threeLine_focused")
    }

    @Test
    fun listItem_threeLine_disabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                ListItem(
                    selected = false,
                    onClick = {},
                    enabled = false,
                    headlineContent = { Text("Three line list item") },
                    overlineContent = { Text("OVERLINE") },
                    supportingContent = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSize)
                        )
                    }
                )
            }
        }

        assertAgainstGolden("listItem_${scheme.name}_threeLine_disabled")
    }

    @Test
    fun listItem_threeLine_focusedDisabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                ListItem(
                    selected = false,
                    onClick = {},
                    enabled = false,
                    headlineContent = { Text("Three line list item") },
                    overlineContent = { Text("OVERLINE") },
                    supportingContent = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSize)
                        )
                    }
                )
            }
        }

        rule.onNodeWithTag(ListItemWrapperTag)
            .onChild()
            .requestFocus()
        rule.waitForIdle()

        assertAgainstGolden("listItem_${scheme.name}_threeLine_focusedDisabled")
    }

    @Test
    fun listItem_threeLine_selected() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                ListItem(
                    selected = true,
                    onClick = {},
                    headlineContent = { Text("Three line list item") },
                    overlineContent = { Text("OVERLINE") },
                    supportingContent = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSize)
                        )
                    }
                )
            }
        }

        assertAgainstGolden("listItem_${scheme.name}_threeLine_selected")
    }

    @Test
    fun listItem_threeLine_focusedSelected() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                ListItem(
                    selected = true,
                    onClick = {},
                    headlineContent = { Text("Three line list item") },
                    overlineContent = { Text("OVERLINE") },
                    supportingContent = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSize)
                        )
                    }
                )
            }
        }

        rule.onNodeWithTag(ListItemWrapperTag)
            .onChild()
            .requestFocus()
        rule.waitForIdle()

        assertAgainstGolden("listItem_${scheme.name}_threeLine_focusedSelected")
    }

    @Test
    fun listItem_threeLine_withTrailingContent() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                ListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Three line list item") },
                    overlineContent = { Text("OVERLINE") },
                    supportingContent = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSize)
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSize)
                        )
                    }
                )
            }
        }

        assertAgainstGolden("listItem_${scheme.name}_threeLine_withTrailingContent")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(ListItemWrapperTag)
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
            ColorSchemeWrapper("lightTheme", lightColorScheme()),
            ColorSchemeWrapper("darkTheme", darkColorScheme()),
        )
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    class ColorSchemeWrapper constructor(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}

private const val ListItemWrapperTag = "listItem_wrapper"
