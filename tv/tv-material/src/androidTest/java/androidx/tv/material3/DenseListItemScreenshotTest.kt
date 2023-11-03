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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
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
class DenseListItemScreenshotTest(private val scheme: ColorSchemeWrapper) {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(TV_GOLDEN_MATERIAL3)

    val wrapperModifier = Modifier
        .testTag(DenseListItemWrapperTag)
        .background(scheme.colorScheme.surface)
        .padding(20.dp)

    @Test
    fun denseListItem_customColor() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                DenseListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Dense list item") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSizeDense)
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Red)
                )
            }
        }

        assertAgainstGolden("denseListItem_${scheme.name}_customColor")
    }

    @Test
    fun denseListItem_oneLine() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                DenseListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("One line dense list item") }
                )
            }
        }

        assertAgainstGolden("denseListItem_${scheme.name}_oneLine")
    }

    @Test
    fun denseListItem_oneLine_withIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                DenseListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("One line dense list item") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSizeDense)
                        )
                    }
                )
            }
        }

        assertAgainstGolden("denseListItem_${scheme.name}_oneLine_withIcon")
    }

    @Test
    fun denseListItem_twoLine() {
        rule.setMaterialContent(scheme.colorScheme) {
            Column(
                modifier = wrapperModifier,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                DenseListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Two line dense list item") },
                    supportingContent = { Text("Secondary text") }
                )
                DenseListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Two line dense list item") },
                    overlineContent = { Text("OVERLINE") }
                )
            }
        }

        assertAgainstGolden("denseListItem_${scheme.name}_twoLine")
    }

    @Test
    fun denseListItem_twoLine_withIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Column(
                modifier = wrapperModifier,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                DenseListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Two line dense list item") },
                    supportingContent = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSizeDense)
                        )
                    }
                )
                DenseListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Two line dense list item") },
                    overlineContent = { Text("OVERLINE") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSizeDense)
                        )
                    }
                )
            }
        }

        assertAgainstGolden("denseListItem_${scheme.name}_twoLine_withIcon")
    }

    @Test
    fun denseListItem_threeLine() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                DenseListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Three line dense list item") },
                    overlineContent = { Text("OVERLINE") },
                    supportingContent = { Text("Secondary text") }
                )
            }
        }

        assertAgainstGolden("denseListItem_${scheme.name}_threeLine")
    }

    @Test
    fun denseListItem_threeLine_withIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                DenseListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Three line dense list item") },
                    overlineContent = { Text("OVERLINE") },
                    supportingContent = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSizeDense)
                        )
                    }
                )
            }
        }

        assertAgainstGolden("denseListItem_${scheme.name}_threeLine_withIcon")
    }

    @Test
    fun denseListItem_oneLine_focused() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                DenseListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Dense list item") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSizeDense)
                        )
                    }
                )
            }
        }

        rule.onNodeWithTag(DenseListItemWrapperTag)
            .onChild()
            .performSemanticsAction(SemanticsActions.RequestFocus)
        rule.waitForIdle()

        assertAgainstGolden("denseListItem_${scheme.name}_oneLine_focused")
    }

    @Test
    fun denseListItem_oneLine_disabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                DenseListItem(
                    selected = false,
                    onClick = {},
                    enabled = false,
                    headlineContent = { Text("Dense list item") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSizeDense)
                        )
                    }
                )
            }
        }

        assertAgainstGolden("denseListItem_${scheme.name}_oneLine_disabled")
    }

    @Test
    fun denseListItem_oneLine_focusedDisabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                DenseListItem(
                    selected = false,
                    onClick = {},
                    enabled = false,
                    headlineContent = { Text("Dense list item") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSizeDense)
                        )
                    }
                )
            }
        }

        rule.onNodeWithTag(DenseListItemWrapperTag)
            .onChild()
            .performSemanticsAction(SemanticsActions.RequestFocus)
        rule.waitForIdle()

        assertAgainstGolden("denseListItem_${scheme.name}_oneLine_focusedDisabled")
    }

    @Test
    fun denseListItem_oneLine_selected() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                DenseListItem(
                    selected = true,
                    onClick = {},
                    headlineContent = { Text("Dense list item") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSizeDense)
                        )
                    }
                )
            }
        }

        assertAgainstGolden("denseListItem_${scheme.name}_oneLine_selected")
    }

    @Test
    fun denseListItem_oneLine_focusedSelected() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                DenseListItem(
                    selected = true,
                    onClick = {},
                    headlineContent = { Text("Dense list item") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSizeDense)
                        )
                    }
                )
            }
        }

        rule.onNodeWithTag(DenseListItemWrapperTag)
            .onChild()
            .performSemanticsAction(SemanticsActions.RequestFocus)
        rule.waitForIdle()

        assertAgainstGolden("denseListItem_${scheme.name}_oneLine_focusedSelected")
    }

    @Test
    fun denseListItem_threeLine_withTrailingContent() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(modifier = wrapperModifier) {
                DenseListItem(
                    selected = false,
                    onClick = {},
                    headlineContent = { Text("Three line dense list item") },
                    overlineContent = { Text("OVERLINE") },
                    supportingContent = { Text("Secondary text") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSizeDense)
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(ListItemDefaults.IconSizeDense)
                        )
                    }
                )
            }
        }

        assertAgainstGolden("denseListItem_${scheme.name}_threeLine_withTrailingContent")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(DenseListItemWrapperTag)
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

private const val DenseListItemWrapperTag = "denseListItem_wrapper"
