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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
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
@OptIn(ExperimentalMaterial3Api::class)
class TimePickerScreenshotTest(private val scheme: ColorSchemeWrapper) {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun timePicker_12h() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(TestTag)) {
                TimePicker(
                    state =
                        rememberTimePickerState(
                            initialHour = 10,
                            initialMinute = 23,
                            is24Hour = false,
                        )
                )
            }
        }

        rule.assertAgainstGolden("timePicker_12h_${scheme.name}")
    }

    @Test
    fun timePicker_12h_rtl() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(TestTag)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    TimePicker(
                        state =
                            rememberTimePickerState(
                                initialHour = 10,
                                initialMinute = 23,
                                is24Hour = false,
                            )
                    )
                }
            }
        }

        rule.assertAgainstGolden("timePicker_12h_rtl_${scheme.name}")
    }

    @Test
    fun timePicker_24h() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(TestTag)) {
                TimePicker(
                    state =
                        rememberTimePickerState(
                            initialHour = 22,
                            initialMinute = 23,
                            is24Hour = true,
                        )
                )
            }
        }

        rule.assertAgainstGolden("timePicker_24h_${scheme.name}")
    }

    @Test
    fun timePicker_24h_rtl() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(Modifier.testTag(TestTag)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    TimePicker(
                        state =
                            rememberTimePickerState(
                                initialHour = 22,
                                initialMinute = 23,
                                is24Hour = true,
                            )
                    )
                }
            }
        }

        rule.assertAgainstGolden("timePicker_24h_rtl_${scheme.name}")
    }

    private fun ComposeContentTestRule.assertAgainstGolden(goldenName: String) {
        this.onNodeWithTag(TestTag).captureToImage().assertAgainstGolden(screenshotRule, goldenName)
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

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}
