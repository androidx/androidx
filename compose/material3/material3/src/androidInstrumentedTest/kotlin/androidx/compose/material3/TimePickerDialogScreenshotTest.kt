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
import androidx.compose.material3.TimePickerScreenshotTest.ColorSchemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Dialog() {
        TimePickerDialog(
            modifier = Modifier,
            title = { TimePickerDialogDefaults.Title(displayMode = TimePickerDisplayMode.Picker) },
            onDismissRequest = {},
            confirmButton = { TextButton(onClick = {}) { Text("Ok") } },
            dismissButton = { TextButton(onClick = {}) { Text("Cancel") } },
            modeToggleButton = {
                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {},
                    displayMode = TimePickerDisplayMode.Picker
                )
            }
        ) {
            TimePicker(state = rememberTimePickerState())
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
