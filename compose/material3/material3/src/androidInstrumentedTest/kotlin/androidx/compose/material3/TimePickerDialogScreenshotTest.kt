/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.material3.TimePickerScreenshotTest.ColorSchemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class TimePickerDialogScreenshotTest(private val scheme: ColorSchemeWrapper) {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun time_picker_dialog() {
        rule.setMaterialContent(scheme.colorScheme) { Dialog() }

        rule
            .onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "time_picker_dialog_${scheme.name}")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun time_picker_dialog_small() {
        rule.setMaterialContent(scheme.colorScheme) { ContainedDialog(DpSize(500.dp, 280.dp)) }

        rule
            .onNodeWithTag(TestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "time_picker_dialog_small${scheme.name}")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun time_picker_dialog_medium() {
        rule.setMaterialContent(scheme.colorScheme) { ContainedDialog(DpSize(540.dp, 330.dp)) }

        rule
            .onNodeWithTag(TestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "time_picker_dialog_medium${scheme.name}")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun time_picker_dialog_large() {
        rule.setMaterialContent(scheme.colorScheme) { ContainedDialog(DpSize(572.dp, 360.dp)) }

        rule
            .onNodeWithTag(TestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "time_picker_dialog_large${scheme.name}")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun time_picker_dialog_extraLarge() {
        rule.setMaterialContent(scheme.colorScheme) { ContainedDialog(DpSize(572.dp, 384.dp)) }

        rule
            .onNodeWithTag(TestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "time_picker_dialog_xlarge${scheme.name}")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun time_picker_dialog_with_custom_container_color() {
        rule.setMaterialContent(scheme.colorScheme) { Dialog(containerColor = Color.Magenta) }

        rule
            .onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "time_picker_dialog_with_custom_container_color${scheme.name}",
            )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Dialog(containerColor: Color = TimePickerDialogDefaults.containerColor) {
        TimePickerDialog(
            modifier = Modifier,
            title = { TimePickerDialogDefaults.Title(displayMode = TimePickerDisplayMode.Picker) },
            onDismissRequest = {},
            confirmButton = { TextButton(onClick = {}) { Text("Ok") } },
            dismissButton = { TextButton(onClick = {}) { Text("Cancel") } },
            modeToggleButton = {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {},
                    displayMode = TimePickerDisplayMode.Picker,
                )
            },
            containerColor = containerColor,
        ) {
            TimePicker(state = rememberTimePickerState())
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ContainedDialog(size: DpSize) {
        DeviceConfigurationOverride(
            DeviceConfigurationOverride.ForcedSize(size) // Typical phone size
        ) {
            Box {
                TimePickerDialogLayout(
                    modifier = Modifier.testTag(TestTag),
                    title = {
                        TimePickerDialogDefaults.Title(displayMode = TimePickerDisplayMode.Picker)
                    },
                    confirmButton = { TextButton(onClick = {}) { Text("Ok") } },
                    dismissButton = { TextButton(onClick = {}) { Text("Cancel") } },
                    modeToggleButton = {
                        TimePickerDialogDefaults.DisplayModeToggle(
                            onDisplayModeChange = {},
                            displayMode = TimePickerDisplayMode.Picker,
                        )
                    },
                ) {
                    TimePicker(
                        state = rememberTimePickerState(),
                        layoutType = TimePickerLayoutType.Horizontal,
                    )
                }
            }
        }
    }

    companion object {
        private const val TestTag = "testTag"

        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                ColorSchemeWrapper("lightTheme", lightColorScheme()),
                ColorSchemeWrapper("darkTheme", darkColorScheme()),
            )
    }
}
