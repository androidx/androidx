/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.material.textfield

import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.AnimationDuration
import androidx.compose.material.GOLDEN_MATERIAL
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.setMaterialContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class OutlinedTextFieldScreenshotTest {
    private val TextFieldTag = "OutlinedTextField"

    private val longText =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
            "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
            " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
            "fugiat nulla pariatur."

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    @Test
    fun outlinedTextField_withInput() {
        rule.setMaterialContent {
            val text = "Text"
            OutlinedTextField(
                state = rememberTextFieldState(text, TextRange(text.length)),
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_withInput")
    }

    @Test
    fun outlinedTextField_notFocused() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_not_focused")
    }

    @Test
    fun outlinedTextField_focused() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_focused")
    }

    @Test
    fun outlinedTextField_focused_rtl() {
        rule.setMaterialContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                OutlinedTextField(
                    state = rememberTextFieldState(),
                    label = { Text("Label") },
                    modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_focused_rtl")
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun outlinedTextField_error_focused() {
        // No-op interceptor to prevent interference from actual IME
        val inputInterceptor = PlatformTextInputInterceptor { _, _ -> awaitCancellation() }

        // stop animation of blinking cursor
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent {
            val text = "Input"
            InterceptPlatformTextInput(inputInterceptor) {
                OutlinedTextField(
                    state = rememberTextFieldState(text, TextRange(text.length)),
                    label = { Text("Label") },
                    isError = true,
                    modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag).focus()
        rule.mainClock.advanceTimeBy(AnimationDuration.toLong())

        assertAgainstGolden("outlined_textField_focused_errorState")
    }

    @Test
    fun outlinedTextField_error_notFocused() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = { Text("Label") },
                isError = true,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_notFocused_errorState")
    }

    @Test
    fun outlinedTextField_textColor_fallbackToContentColor() {
        rule.setMaterialContent {
            CompositionLocalProvider(LocalContentColor provides Color.Magenta) {
                val text = "Hello, world!"
                OutlinedTextField(
                    state = rememberTextFieldState(text, TextRange(text.length)),
                    modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
                )
            }
        }

        assertAgainstGolden("outlined_textField_textColor_defaultContentColor")
    }

    @Test
    fun outlinedTextField_multiLine_withLabel_textAlignedToTop() {
        rule.setMaterialContent {
            val text = "Text"
            OutlinedTextField(
                state = rememberTextFieldState(text, TextRange(text.length)),
                label = { Text("Label") },
                modifier =
                    Modifier.requiredHeight(300.dp).requiredWidth(280.dp).testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("outlined_textField_multiLine_withLabel_textAlignedToTop")
    }

    @Test
    fun outlinedTextField_multiLine_withoutLabel_textAlignedToTop() {
        rule.setMaterialContent {
            val text = "Text"
            OutlinedTextField(
                state = rememberTextFieldState(text, TextRange(text.length)),
                modifier =
                    Modifier.requiredHeight(300.dp).requiredWidth(280.dp).testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("outlined_textField_multiLine_withoutLabel_textAlignedToTop")
    }

    @Test
    fun outlinedTextField_multiLine_withLabel_placeholderAlignedToTop() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = { Text("Label") },
                placeholder = { Text("placeholder") },
                modifier =
                    Modifier.requiredHeight(300.dp).requiredWidth(280.dp).testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_multiLine_withLabel_placeholderAlignedToTop")
    }

    @Test
    fun outlinedTextField_multiLine_withoutLabel_placeholderAlignedToTop() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(),
                placeholder = { Text("placeholder") },
                modifier =
                    Modifier.requiredHeight(300.dp).requiredWidth(280.dp).testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_multiLine_withoutLabel_placeholderAlignedToTop")
    }

    @Test
    fun outlinedTextField_multiLine_labelAlignedToTop() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = { Text("Label") },
                modifier =
                    Modifier.requiredHeight(300.dp).requiredWidth(280.dp).testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("outlined_textField_multiLine_labelAlignedToTop")
    }

    @Test
    fun outlinedTextField_singleLine_withLabel_textAlignedToTop() {
        rule.setMaterialContent {
            val text = "Text"
            OutlinedTextField(
                state = rememberTextFieldState(text, TextRange(text.length)),
                lineLimits = TextFieldLineLimits.SingleLine,
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_singleLine_withLabel_textAlignedToTop")
    }

    @Test
    fun outlinedTextField_singleLine_withoutLabel_textCenteredVertically() {
        rule.setMaterialContent {
            val text = "Text"
            OutlinedTextField(
                state = rememberTextFieldState(text, TextRange(text.length)),
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_singleLine_withoutLabel_textCenteredVertically")
    }

    @Test
    fun outlinedTextField_singleLine_withLabel_placeholderAlignedToTop() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(),
                placeholder = { Text("placeholder") },
                label = { Text("Label") },
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_singleLine_withLabel_placeholderAlignedToTop")
    }

    @Test
    fun outlinedTextField_singleLine_withoutLabel_placeholderCenteredVertically() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(),
                placeholder = { Text("placeholder") },
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden(
            "outlined_textField_singleLine_withoutLabel_placeholderCenteredVertically"
        )
    }

    @Test
    fun outlinedTextField_singleLine_labelCenteredVertically() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = { Text("Label") },
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_singleLine_labelCenteredVetically")
    }

    @Test
    fun outlinedTextField_disabled() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState("Text"),
                lineLimits = TextFieldLineLimits.SingleLine,
                enabled = false,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlinedTextField_disabled")
    }

    @Test
    fun outlinedTextField_disabled_notFocusable() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState("Text"),
                lineLimits = TextFieldLineLimits.SingleLine,
                enabled = false,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlinedTextField_disabled_notFocusable")
    }

    @Test
    fun outlinedTextField_disabled_notScrolled() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(longText),
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(300.dp),
                enabled = false
            )
        }

        rule.mainClock.autoAdvance = false

        rule.onNodeWithTag(TextFieldTag).performTouchInput { swipeLeft() }

        // wait for swipe to finish
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(250)

        assertAgainstGolden("outlinedTextField_disabled_notScrolled")
    }

    @Test
    fun outlinedTextField_readOnly() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState("Text"),
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp),
                enabled = true,
                readOnly = true
            )
        }

        assertAgainstGolden("outlinedTextField_readOnly")
    }

    @Test
    fun outlinedTextField_readOnly_focused() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState("Text"),
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp),
                enabled = true,
                readOnly = true
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlinedTextField_readOnly_focused")
    }

    @Test
    fun outlinedTextField_readOnly_scrolled() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(longText),
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(300.dp),
                lineLimits = TextFieldLineLimits.SingleLine,
                enabled = true,
                readOnly = true
            )
        }

        rule.mainClock.autoAdvance = false

        rule.onNodeWithTag(TextFieldTag).performTouchInput { swipeLeft() }

        // wait for swipe to finish
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(250)

        assertAgainstGolden("outlinedTextField_readOnly_scrolled")
    }

    @Test
    fun outlinedTextField_textCenterAligned() {
        rule.setMaterialContent {
            val text = "Hello world"
            OutlinedTextField(
                state = rememberTextFieldState(text, TextRange(text.length)),
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                lineLimits = TextFieldLineLimits.SingleLine
            )
        }

        assertAgainstGolden("outlinedTextField_textCenterAligned")
    }

    @Test
    fun outlinedTextField_textAlignedToEnd() {
        rule.setMaterialContent {
            val text = "Hello world"
            OutlinedTextField(
                state = rememberTextFieldState(text, TextRange(text.length)),
                modifier = Modifier.fillMaxWidth().testTag(TextFieldTag),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                lineLimits = TextFieldLineLimits.SingleLine
            )
        }

        assertAgainstGolden("outlinedTextField_textAlignedToEnd")
    }

    private fun SemanticsNodeInteraction.focus() {
        // split click into (down) and (move, up) to enforce a composition in between
        this.performTouchInput { down(center) }
            .performTouchInput {
                move()
                up()
            }
    }

    @Test
    fun outlinedTextField_customShape() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = { Text("Label") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                lineLimits = TextFieldLineLimits.SingleLine,
                shape = CutCornerShape(10.dp)
            )
        }

        assertAgainstGolden("outlinedTextField_customShape")
    }

    @Test
    fun outlinedTextField_labelBecomesNull() {
        lateinit var makeLabelNull: MutableState<Boolean>
        rule.setMaterialContent {
            makeLabelNull = remember { mutableStateOf(false) }
            OutlinedTextField(
                state = rememberTextFieldState("Text"),
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                label =
                    if (makeLabelNull.value) {
                        null
                    } else {
                        { Text("Label") }
                    },
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()
        rule.runOnIdle { makeLabelNull.value = true }

        assertAgainstGolden("outlinedTextField_labelBecomesNull")
    }

    @Test
    fun outlinedTextField_leadingTrailingIcons() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = { Text("Label") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                leadingIcon = { Icon(Icons.Default.Call, null) },
                trailingIcon = { Icon(Icons.Default.Clear, null) }
            )
        }

        assertAgainstGolden("outlinedTextField_leadingTrailingIcons")
    }

    @Test
    fun outlinedTextField_leadingTrailingIcons_error() {
        rule.setMaterialContent {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = { Text("Label") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                leadingIcon = { Icon(Icons.Default.Call, null) },
                trailingIcon = { Icon(Icons.Default.Clear, null) },
                isError = true
            )
        }

        assertAgainstGolden("outlinedTextField_leadingTrailingIcons_error")
    }

    private fun assertAgainstGolden(goldenIdentifier: String) {
        rule
            .onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}
