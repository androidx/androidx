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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
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
class NavigationDrawerItemScreenshotTest(private val scheme: ColorSchemeWrapper) {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(TV_GOLDEN_MATERIAL3)

    val wrapperModifier = Modifier
        .testTag(NavigationDrawerItemWrapperTag)
        .background(scheme.colorScheme.surface)
        .padding(20.dp)

    @Test
    fun navigationDrawerItem_customColor() {
        rule.setMaterialContent(scheme.colorScheme) {
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize)
                        )
                    },
                    colors = NavigationDrawerItemDefaults.colors(containerColor = Color.Red)
                ) {
                    Text("Favourite")
                }
            }
        }

        assertAgainstGolden("navigationDrawerItem_${scheme.name}_customColor")
    }

    @Test
    fun navigationDrawerItem_oneLine() {
        rule.setMaterialContent(scheme.colorScheme) {
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize)
                        )
                    }
                ) {
                    Text("Favourite")
                }
            }
        }

        assertAgainstGolden("navigationDrawerItem_${scheme.name}_oneLine")
    }

    @Test
    fun navigationDrawerItem_twoLine() {
        rule.setMaterialContent(scheme.colorScheme) {
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize)
                        )
                    },
                    supportingContent = { Text("You like this") }
                ) {
                    Text("Favourite")
                }
            }
        }

        assertAgainstGolden("navigationDrawerItem_${scheme.name}_twoLine")
    }

    @Test
    fun navigationDrawerItem_twoLine_focused() {
        rule.setMaterialContent(scheme.colorScheme) {
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize)
                        )
                    },
                    supportingContent = { Text("You like this") }
                ) {
                    Text("Favourite")
                }
            }
        }

        rule.onNodeWithTag(NavigationDrawerItemWrapperTag)
            .onChild()
            .requestFocus()
        rule.waitForIdle()

        assertAgainstGolden("navigationDrawerItem_${scheme.name}_twoLine_focused")
    }

    @Test
    fun navigationDrawerItem_twoLine_disabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = {},
                    enabled = false,
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize)
                        )
                    },
                    supportingContent = { Text("You like this") }
                ) {
                    Text("Favourite")
                }
            }
        }

        assertAgainstGolden("navigationDrawerItem_${scheme.name}_twoLine_disabled")
    }

    @Test
    fun navigationDrawerItem_twoLine_focusedDisabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = {},
                    enabled = false,
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize)
                        )
                    },
                    supportingContent = { Text("You like this") }
                ) {
                    Text("Favourite")
                }
            }
        }

        rule.onNodeWithTag(NavigationDrawerItemWrapperTag)
            .onChild()
            .requestFocus()
        rule.waitForIdle()

        assertAgainstGolden("navigationDrawerItem_${scheme.name}_twoLine_focusedDisabled")
    }

    @Test
    fun navigationDrawerItem_twoLine_selected() {
        rule.setMaterialContent(scheme.colorScheme) {
            DrawerScope {
                NavigationDrawerItem(
                    selected = true,
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize)
                        )
                    },
                    supportingContent = { Text("You like this") }
                ) {
                    Text("Favourite")
                }
            }
        }

        assertAgainstGolden("navigationDrawerItem_${scheme.name}_twoLine_selected")
    }

    @Test
    fun navigationDrawerItem_twoLine_focusedSelected() {
        rule.setMaterialContent(scheme.colorScheme) {
            DrawerScope {
                NavigationDrawerItem(
                    selected = true,
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize)
                        )
                    },
                    supportingContent = { Text("You like this") }
                ) {
                    Text("Favourite")
                }
            }
        }

        rule.onNodeWithTag(NavigationDrawerItemWrapperTag)
            .onChild()
            .requestFocus()
        rule.waitForIdle()

        assertAgainstGolden("navigationDrawerItem_${scheme.name}_twoLine_focusedSelected")
    }

    @Test
    fun navigationDrawerItem_inactive() {
        rule.setMaterialContent(scheme.colorScheme) {
            DrawerScope(false) {
                NavigationDrawerItem(
                    selected = false,
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize)
                        )
                    },
                ) {
                    Text("Favourite")
                }
            }
        }

        assertAgainstGolden("navigationDrawerItem_${scheme.name}_inactive")
    }

    @Test
    fun navigationDrawerItem_inactive_selected() {
        rule.setMaterialContent(scheme.colorScheme) {
            DrawerScope(false) {
                NavigationDrawerItem(
                    selected = true,
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize)
                        )
                    },
                ) {
                    Text("Favourite")
                }
            }
        }

        assertAgainstGolden("navigationDrawerItem_${scheme.name}_inactive_selected")
    }

    @Test
    fun navigationDrawerItem_twoLine_withTrailingContent() {
        rule.setMaterialContent(scheme.colorScheme) {
            DrawerScope {
                NavigationDrawerItem(
                    selected = false,
                    onClick = {},
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize)
                        )
                    },
                    supportingContent = { Text("You like this") },
                    trailingContent = {
                        NavigationDrawerItemDefaults.TrailingBadge("NEW")
                    }
                ) {
                    Text("Favourite")
                }
            }
        }

        assertAgainstGolden("navigationDrawerItem_${scheme.name}_twoLine_withTrailingContent")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(NavigationDrawerItemWrapperTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    // Provide the ColorScheme and their name parameter in a ColorSchemeWrapper.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = arrayOf(
            ColorSchemeWrapper("lightTheme", lightColorScheme()),
            ColorSchemeWrapper("darkTheme", darkColorScheme()),
        )
    }

    class ColorSchemeWrapper constructor(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }

    @Composable
    private fun DrawerScope(
        doesNavigationDrawerHaveFocus: Boolean = true,
        content: @Composable NavigationDrawerScope.() -> Unit
    ) {
        Box(wrapperModifier) {
            NavigationDrawerScopeImpl(doesNavigationDrawerHaveFocus).apply {
                content()
            }
        }
    }
}

private const val NavigationDrawerItemWrapperTag = "navigationDrawerItem_wrapper"
