/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.internal.defaultPlatformTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class OutlinedTextFieldScreenshotTest {
    private val TextFieldTag = "OutlinedTextField"

    private val longText = TextFieldValue(
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
            "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
            " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
            "fugiat nulla pariatur."
    )

    private val platformTextStyle = defaultPlatformTextStyle()

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun outlinedTextField_withInput() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Text"
            OutlinedTextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_withInput")
    }

    @Test
    fun outlinedTextField_notFocused() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_not_focused")
    }

    @Test
    fun outlinedTextField_focused() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_focused")
    }

    @Test
    fun outlinedTextField_focused_rtl() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Label") },
                    modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_focused_rtl")
    }

    @Test
    fun outlinedTextField_error_focused() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Input"
            OutlinedTextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                label = { Text("Label") },
                isError = true,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_focused_errorState")
    }

    @Test
    fun outlinedTextField_error_notFocused() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                isError = true,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_notFocused_errorState")
    }

    @Test
    fun outlinedTextField_textColor_customTextColor() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Hello, world!"
            OutlinedTextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.Magenta),
            )
        }

        assertAgainstGolden("outlined_textField_textColor_customTextColor")
    }

    @Test
    fun outlinedTextField_textSelectionColor_customColors() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Hello, world!"
            OutlinedTextField(
                value = TextFieldValue(text = text, selection = TextRange(0, text.length)),
                onValueChange = {},
                modifier = Modifier.requiredWidth(280.dp).testTag(TextFieldTag),
                colors = OutlinedTextFieldDefaults.colors(
                    // We can only test the background color because popups, which includes the
                    // selection handles, do not appear in screenshots
                    selectionColors = TextSelectionColors(
                        handleColor = Color.Black,
                        backgroundColor = Color.Green,
                    )
                )
            )
        }

        assertAgainstGolden("outlined_textField_textSelectionColor_customColors")
    }

    @Test
    fun outlinedTextField_multiLine_withLabel_textAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Text"
            OutlinedTextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("outlined_textField_multiLine_withLabel_textAlignedToTop")
    }

    @Test
    fun outlinedTextField_multiLine_withoutLabel_textAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Text"
            OutlinedTextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("outlined_textField_multiLine_withoutLabel_textAlignedToTop")
    }

    @Test
    fun outlinedTextField_multiLine_withLabel_placeholderAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                placeholder = { Text("placeholder") },
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_multiLine_withLabel_placeholderAlignedToTop")
    }

    @Test
    fun outlinedTextField_multiLine_withoutLabel_placeholderAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("placeholder") },
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_multiLine_withoutLabel_placeholderAlignedToTop")
    }

    @Test
    fun outlinedTextField_multiLine_labelAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.requiredHeight(300.dp)
                    .requiredWidth(280.dp)
                    .testTag(TextFieldTag)
            )
        }

        assertAgainstGolden("outlined_textField_multiLine_labelAlignedToTop")
    }

    @Test
    fun outlinedTextField_singleLine_withLabel_textAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Text"
            OutlinedTextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                singleLine = true,
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_singleLine_withLabel_textAlignedToTop")
    }

    @Test
    fun outlinedTextField_singleLine_withoutLabel_textCenteredVertically() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Text"
            OutlinedTextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                singleLine = true,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_singleLine_withoutLabel_textCenteredVertically")
    }

    @Test
    fun outlinedTextField_singleLine_withLabel_placeholderAlignedToTop() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("placeholder") },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_singleLine_withLabel_placeholderAlignedToTop")
    }

    @Test
    fun outlinedTextField_singleLine_withoutLabel_placeholderCenteredVertically() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("placeholder") },
                singleLine = true,
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
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_singleLine_labelCenteredVetically")
    }

    @Test
    fun outlinedTextField_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = TextFieldValue("Text"),
                onValueChange = {},
                singleLine = true,
                enabled = false,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlinedTextField_disabled")
    }

    @Test
    fun outlinedTextField_disabled_notFocusable() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = TextFieldValue("Text"),
                onValueChange = {},
                singleLine = true,
                enabled = false,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlinedTextField_disabled_notFocusable")
    }

    @Test
    fun outlinedTextField_disabled_notScrolled() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = longText,
                onValueChange = { },
                singleLine = true,
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
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = TextFieldValue("Text"),
                onValueChange = {},
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp),
                enabled = true,
                readOnly = true
            )
        }

        assertAgainstGolden("outlinedTextField_readOnly")
    }

    @Test
    fun outlinedTextField_readOnly_focused() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = TextFieldValue("Text"),
                onValueChange = {},
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
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = longText,
                onValueChange = { },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(300.dp),
                singleLine = true,
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
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Hello world"
            OutlinedTextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    platformStyle = platformTextStyle
                ),
                singleLine = true
            )
        }

        assertAgainstGolden("outlinedTextField_textCenterAligned")
    }

    @Test
    fun outlinedTextField_textAlignedToEnd() {
        rule.setMaterialContent(lightColorScheme()) {
            val text = "Hello world"
            OutlinedTextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().testTag(TextFieldTag),
                textStyle = TextStyle(textAlign = TextAlign.End, platformStyle = platformTextStyle),
                singleLine = true
            )
        }

        assertAgainstGolden("outlinedTextField_textAlignedToEnd")
    }

    @Test
    fun outlinedTextField_customShape() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                singleLine = true,
                shape = CutCornerShape(10.dp)
            )
        }

        assertAgainstGolden("outlinedTextField_customShape")
    }

    @Test
    fun outlinedTextField_supportingText() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.testTag(TextFieldTag).fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Supporting text") }
            )
        }

        assertAgainstGolden("outlinedTextField_supportingText")
    }

    @Test
    fun outlinedTextField_errorSupportingText() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                isError = true,
                modifier = Modifier.testTag(TextFieldTag).fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Error supporting text") }
            )
        }

        assertAgainstGolden("outlinedTextField_errorSupportingText")
    }

    @Test
    fun outlinedTextField_labelBecomesNull() {
        lateinit var makeLabelNull: MutableState<Boolean>
        rule.setMaterialContent(lightColorScheme()) {
            makeLabelNull = remember { mutableStateOf(false) }
            OutlinedTextField(
                value = "Text",
                onValueChange = {},
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                label = if (makeLabelNull.value) {
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
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
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
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                leadingIcon = { Icon(Icons.Default.Call, null) },
                trailingIcon = { Icon(Icons.Default.Clear, null) },
                isError = true
            )
        }

        assertAgainstGolden("outlinedTextField_leadingTrailingIcons_error")
    }

    @Test
    fun outlinedTextField_prefixSuffix_withLabelAndInput() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "Text",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                prefix = { Text("P:") },
                suffix = { Text(":S") },
            )
        }

        assertAgainstGolden("outlinedTextField_prefixSuffix_withLabelAndInput")
    }

    @Test
    fun outlinedTextField_prefixSuffix_withLabelAndInput_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            OutlinedTextField(
                value = "Text",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                prefix = { Text("P:") },
                suffix = { Text(":S") },
            )
        }

        assertAgainstGolden("outlinedTextField_prefixSuffix_withLabelAndInput_darkTheme")
    }

    @Test
    fun outlinedTextField_prefixSuffix_withLabelAndInput_focused() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "Text",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                prefix = { Text("P:") },
                suffix = { Text(":S") },
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlinedTextField_prefixSuffix_withLabelAndInput_focused")
    }

    @Test
    fun outlinedTextField_prefixSuffix_withPlaceholder() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Placeholder") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                prefix = { Text("P:") },
                suffix = { Text(":S") },
            )
        }

        assertAgainstGolden("outlinedTextField_prefixSuffix_withPlaceholder")
    }

    @Test
    fun outlinedTextField_prefixSuffix_withLeadingTrailingIcons() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                value = "Text",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                prefix = { Text("P:") },
                suffix = { Text(":S") },
                leadingIcon = { Icon(Icons.Default.Call, null) },
                trailingIcon = { Icon(Icons.Default.Clear, null) },
            )
        }

        assertAgainstGolden("outlinedTextField_prefixSuffix_withLeadingTrailingIcons")
    }

    @Test
    fun outlinedTextField_withInput_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            val text = "Text"
            OutlinedTextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlined_textField_withInput_dark")
    }

    @Test
    fun outlinedTextField_focused_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_focused_dark")
    }

    @Test
    fun outlinedTextField_error_focused_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            val text = "Input"
            OutlinedTextField(
                value = TextFieldValue(text = text, selection = TextRange(text.length)),
                onValueChange = {},
                label = { Text("Label") },
                isError = true,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        rule.onNodeWithTag(TextFieldTag).focus()

        assertAgainstGolden("outlined_textField_focused_errorState_dark")
    }

    @Test
    fun outlinedTextField_disabled_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            OutlinedTextField(
                value = TextFieldValue("Text"),
                onValueChange = {},
                singleLine = true,
                enabled = false,
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(280.dp)
            )
        }

        assertAgainstGolden("outlinedTextField_disabled_dark")
    }

    @Test
    fun outlinedTextField_leadingTrailingIcons_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Label") },
                modifier = Modifier.width(300.dp).testTag(TextFieldTag),
                leadingIcon = { Icon(Icons.Default.Call, null) },
                trailingIcon = { Icon(Icons.Default.Clear, null) }
            )
        }

        assertAgainstGolden("outlinedTextField_leadingTrailingIcons_dark")
    }

    private fun SemanticsNodeInteraction.focus() {
        // split click into (down) and (move, up) to enforce a composition in between
        this.performTouchInput { down(center) }.performTouchInput { move(); up() }
    }

    private fun assertAgainstGolden(goldenIdentifier: String) {
        rule.onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}
