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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalTestApi::class, ExperimentalTvMaterial3Api::class)
class RadioButtonScreenshotTest(private val scheme: ColorSchemeWrapper) {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(TV_GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.TopStart)
    private val wrapperTestTag = "radioButtonWrapper"

    @Test
    fun radioButton_selected() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = true, onClick = {})
            }
        }
        assertSelectableAgainstGolden("radioButton_${scheme.name}_selected")
    }

    @Test
    fun radioButton_notSelected() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {})
            }
        }
        assertSelectableAgainstGolden("radioButton_${scheme.name}_notSelected")
    }

    @Test
    fun radioButton_hovered() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {})
            }
        }
        rule.onNodeWithTag(wrapperTestTag).performMouseInput {
            enter(center)
        }

        assertSelectableAgainstGolden("radioButton_${scheme.name}_hovered")
    }

    @Test
    fun radioButton_focused() {
        val focusRequester = FocusRequester()
        var localInputModeManager: InputModeManager? = null

        rule.setMaterialContent(scheme.colorScheme) {
            localInputModeManager = LocalInputModeManager.current
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(
                    selected = false,
                    onClick = {},
                    modifier = Modifier
                        .focusRequester(focusRequester)
                )
            }
        }

        rule.runOnIdle {
            @OptIn(ExperimentalComposeUiApi::class)
            localInputModeManager!!.requestInputMode(InputMode.Keyboard)
            focusRequester.requestFocus()
        }

        assertSelectableAgainstGolden("radioButton_${scheme.name}_focused")
    }

    @Test
    fun radioButton_disabled_selected() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = true, onClick = {}, enabled = false)
            }
        }
        assertSelectableAgainstGolden("radioButton_${scheme.name}_disabled_selected")
    }

    @Test
    fun radioButton_disabled_notSelected() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                RadioButton(selected = false, onClick = {}, enabled = false)
            }
        }
        assertSelectableAgainstGolden("radioButton_${scheme.name}_disabled_notSelected")
    }

    private fun assertSelectableAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
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
}