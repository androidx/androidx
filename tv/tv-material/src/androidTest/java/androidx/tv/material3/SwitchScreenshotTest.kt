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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.LayoutDirection
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
@OptIn(ExperimentalTestApi::class, ExperimentalTvMaterial3Api::class)
class SwitchScreenshotTest(private val scheme: ColorSchemeWrapper) {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(TV_GOLDEN_MATERIAL3)
    private val wrapperTestTag = "switchWrapper"

    private val wrapperModifier = Modifier
        .wrapContentSize(Alignment.TopStart)
        .testTag(wrapperTestTag)

    @Test
    fun switchTest_checked() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperModifier) {
                Switch(checked = true, onCheckedChange = { })
            }
        }

        assertToggeableAgainstGolden("switch_${scheme.name}_checked")
    }

    @Test
    fun switchTest_checked_rtl() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperModifier) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Switch(checked = true, onCheckedChange = { })
                }
            }
        }

        assertToggeableAgainstGolden("switch_${scheme.name}_checked_rtl")
    }

    @Test
    fun switchTest_checked_customThumbColor() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperModifier) {
                Switch(
                    checked = true,
                    onCheckedChange = { },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Green)
                )
            }
        }

        assertToggeableAgainstGolden("switch_${scheme.name}_checked_customThumbColor")
    }

    @Test
    fun switchTest_unchecked() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperModifier) {
                Switch(checked = false, onCheckedChange = { })
            }
        }

        assertToggeableAgainstGolden("switch_${scheme.name}_unchecked")
    }

    @Test
    fun switchTest_unchecked_rtl() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperModifier) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Switch(checked = false, onCheckedChange = { })
                }
            }
        }

        assertToggeableAgainstGolden("switch_${scheme.name}_unchecked_rtl")
    }

    @Test
    fun switchTest_disabled_checked() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperModifier) {
                Switch(checked = true, enabled = false, onCheckedChange = { })
            }
        }

        assertToggeableAgainstGolden("switch_${scheme.name}_disabled_checked")
    }

    @Test
    fun switchTest_disabled_unchecked() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperModifier) {
                Switch(checked = false, enabled = false, onCheckedChange = { })
            }
        }

        assertToggeableAgainstGolden("switch_${scheme.name}_disabled_unchecked")
    }

    @Test
    fun switchTest_hover() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperModifier) {
                Switch(
                    checked = true,
                    onCheckedChange = { }
                )
            }
        }

        rule.onNode(isToggleable())
            .performMouseInput { enter(center) }

        rule.waitForIdle()

        assertToggeableAgainstGolden("switch_${scheme.name}_hover")
    }

    @Test
    fun switchTest_focus() {
        val focusRequester = FocusRequester()
        var localInputModeManager: InputModeManager? = null

        rule.setMaterialContent(scheme.colorScheme) {
            localInputModeManager = LocalInputModeManager.current
            Box(wrapperModifier) {
                Switch(
                    checked = true,
                    onCheckedChange = { },
                    modifier = Modifier
                        .testTag("switch")
                        .focusRequester(focusRequester)
                )
            }
        }

        rule.runOnIdle {
            @OptIn(ExperimentalComposeUiApi::class)
            localInputModeManager!!.requestInputMode(InputMode.Keyboard)
            focusRequester.requestFocus()
        }

        rule.waitForIdle()

        assertToggeableAgainstGolden("switch_${scheme.name}_focus")
    }

    @Test
    fun switchTest_checked_icon() {
        rule.setMaterialContent(scheme.colorScheme) {
            val icon: @Composable () -> Unit = {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
            Box(wrapperModifier) {
                Switch(
                    checked = true,
                    onCheckedChange = { },
                    thumbContent = icon
                )
            }
        }

        assertToggeableAgainstGolden("switch_${scheme.name}_checked_icon")
    }

    @Test
    fun switchTest_unchecked_icon() {
        rule.setMaterialContent(scheme.colorScheme) {
            val icon: @Composable () -> Unit = {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
            Box(wrapperModifier) {
                Switch(
                    checked = false,
                    onCheckedChange = { },
                    thumbContent = icon
                )
            }
        }

        assertToggeableAgainstGolden("switch_${scheme.name}_unchecked_icon")
    }

    private fun assertToggeableAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
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