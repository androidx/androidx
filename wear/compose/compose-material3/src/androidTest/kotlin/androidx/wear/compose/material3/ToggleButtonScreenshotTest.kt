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

package androidx.wear.compose.material3

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ToggleButtonScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule
    val testName = TestName()

    @Test
    fun toggle_button_checked_checkbox() = verifyScreenshot {
        sampleToggleButton(
            checked = true,
            selectionControl = {
                Checkbox(checked = true)
            }
        )
    }

    @Test
    fun toggle_button_unchecked_checkbox() = verifyScreenshot {
        sampleToggleButton(
            checked = false,
            selectionControl = {
                Checkbox(checked = false)
            }
        )
    }

    @Test
    fun toggle_button_checked_switch() = verifyScreenshot {
        sampleToggleButton(
            checked = true,
            selectionControl = {
                Switch(checked = true)
            }
        )
    }

    @Test
    fun toggle_button_unchecked_switch() = verifyScreenshot {
        sampleToggleButton(
            checked = false,
            selectionControl = {
                Switch(checked = false)
            }
        )
    }

    @Test
    fun toggle_button_selected_radio() = verifyScreenshot {
        sampleToggleButton(
            checked = true,
            selectionControl = {
                RadioButton(selected = true)
            }
        )
    }

    @Test
    fun toggle_button_unselected_radio() = verifyScreenshot {
        sampleToggleButton(
            checked = false,
            selectionControl = {
                RadioButton(selected = false)
            }
        )
    }

    @Test
    fun toggle_button_checked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleToggleButton(
                checked = true,
                selectionControl = {
                    Checkbox(checked = true)
                }
            )
        }

    @Test
    fun toggle_button_unchecked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleToggleButton(
                checked = false,
                selectionControl = {
                    Checkbox(checked = false)
                }
            )
        }

    @Test
    fun toggle_button_checked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleToggleButton(
                checked = true,
                selectionControl = {
                    Switch(checked = true)
                }
            )
        }

    @Test
    fun toggle_button_unchecked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleToggleButton(
                checked = false,
                selectionControl = {
                    Switch(checked = false)
                }
            )
        }

    @Test
    fun toggle_button_selected_radio_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleToggleButton(
                checked = true,
                selectionControl = {
                    RadioButton(selected = true)
                }
            )
        }

    @Test
    fun toggle_button_unselected_radio_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleToggleButton(
                checked = false,
                selectionControl = {
                    RadioButton(selected = false)
                }
            )
        }

    @Test
    fun disabled_toggle_button_checked_checkbox() = verifyScreenshot {
        sampleToggleButton(
            checked = true,
            enabled = false,
            selectionControl = {
                Checkbox(checked = true, enabled = false)
            }
        )
    }

    @Test
    fun disabled_toggle_button_unchecked_checkbox() = verifyScreenshot {
        sampleToggleButton(
            checked = false,
            enabled = false,
            selectionControl = {
                Checkbox(checked = false, enabled = false)
            }
        )
    }

    @Test
    fun disabled_toggle_button_checked_switch() = verifyScreenshot {
        sampleToggleButton(
            checked = true,
            enabled = false,
            selectionControl = {
                Switch(checked = true, enabled = false)
            }
        )
    }

    @Test
    fun disabled_toggle_button_unchecked_switch() = verifyScreenshot {
        sampleToggleButton(
            checked = false,
            enabled = false,
            selectionControl = {
                Switch(checked = false, enabled = false)
            }
        )
    }

    @Test
    fun disabled_toggle_button_selected_radio() = verifyScreenshot {
        sampleToggleButton(
            checked = true,
            enabled = false,
            selectionControl = {
                RadioButton(selected = true, enabled = false)
            }
        )
    }

    @Test
    fun disabled_toggle_button_unselected_radio() = verifyScreenshot {
        sampleToggleButton(
            checked = false,
            enabled = false,
            selectionControl = {
                RadioButton(selected = false, enabled = false)
            }
        )
    }

    @Test
    fun disabled_toggle_button_checked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleToggleButton(
                checked = true,
                enabled = false,
                selectionControl = {
                    Checkbox(checked = true, enabled = false)
                }
            )
        }

    @Test
    fun disabled_toggle_button_unchecked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleToggleButton(
                checked = false,
                enabled = false,
                selectionControl = {
                    Checkbox(checked = false, enabled = false)
                }
            )
        }

    @Test
    fun disabled_toggle_button_checked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleToggleButton(
                checked = true,
                enabled = false,
                selectionControl = {
                    Switch(checked = true, enabled = false)
                }
            )
        }

    @Test
    fun disabled_toggle_button_unchecked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleToggleButton(
                checked = false,
                enabled = false,
                selectionControl = {
                    Switch(checked = false, enabled = false)
                }
            )
        }

    @Test
    fun disabled_toggle_button_selected_radio_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleToggleButton(
                checked = true,
                enabled = false,
                selectionControl = {
                    RadioButton(selected = true, enabled = false)
                }
            )
        }

    @Test
    fun disabled_toggle_button_unselected_radio_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleToggleButton(
                checked = false,
                enabled = false,
                selectionControl = {
                    RadioButton(selected = false, enabled = false)
                }
            )
        }

    @Test
    fun split_toggle_button_checked_checkbox() = verifyScreenshot {
        sampleSplitToggleButton(
            checked = true,
            selectionControl = {
                Checkbox(checked = true)
            }
        )
    }

    @Test
    fun split_toggle_button_unchecked_checkbox() = verifyScreenshot {
        sampleSplitToggleButton(
            checked = false,
            selectionControl = {
                Checkbox(checked = false)
            }
        )
    }

    @Test
    fun split_toggle_button_checked_switch() = verifyScreenshot {
        sampleSplitToggleButton(
            checked = true,
            selectionControl = {
                Switch(checked = true)
            }
        )
    }

    @Test
    fun split_toggle_button_unchecked_switch() = verifyScreenshot {
        sampleSplitToggleButton(
            checked = false,
            selectionControl = {
                Switch(checked = false)
            }
        )
    }

    @Test
    fun split_toggle_button_selected_radio() = verifyScreenshot {
        sampleSplitToggleButton(
            checked = true,
            selectionControl = {
                RadioButton(selected = true)
            }
        )
    }

    @Test
    fun split_toggle_button_unselected_radio() = verifyScreenshot {
        sampleSplitToggleButton(
            checked = false,
            selectionControl = {
                RadioButton(selected = false)
            }
        )
    }

    @Test
    fun split_toggle_button_checked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitToggleButton(
                checked = true,
                selectionControl = {
                    Checkbox(checked = true)
                }
            )
        }

    @Test
    fun split_toggle_button_unchecked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitToggleButton(
                checked = false,
                selectionControl = {
                    Checkbox(checked = false)
                }
            )
        }

    @Test
    fun split_toggle_button_checked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitToggleButton(
                checked = true,
                selectionControl = {
                    Switch(checked = true)
                }
            )
        }

    @Test
    fun split_toggle_button_unchecked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitToggleButton(
                checked = false,
                selectionControl = {
                    Switch(checked = false)
                }
            )
        }

    @Test
    fun split_toggle_button_selected_radio_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitToggleButton(
                checked = true,
                selectionControl = {
                    RadioButton(selected = true)
                }
            )
        }

    @Test
    fun split_toggle_button_unselected_radio_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitToggleButton(
                checked = false,
                selectionControl = {
                    RadioButton(selected = false)
                }
            )
        }

    @Test
    fun disabled_split_toggle_button_checked_checkbox() = verifyScreenshot {
        sampleSplitToggleButton(
            checked = true,
            enabled = false,
            selectionControl = {
                Checkbox(checked = true, enabled = false)
            }
        )
    }

    @Test
    fun disabled_split_toggle_button_unchecked_checkbox() = verifyScreenshot {
        sampleSplitToggleButton(
            checked = false,
            enabled = false,
            selectionControl = {
                Checkbox(checked = false, enabled = false)
            }
        )
    }

    @Test
    fun disabled_split_toggle_button_checked_switch() = verifyScreenshot {
        sampleSplitToggleButton(
            checked = true,
            enabled = false,
            selectionControl = {
                Switch(checked = true, enabled = false)
            }
        )
    }

    @Test
    fun disabled_split_toggle_button_unchecked_switch() = verifyScreenshot {
        sampleSplitToggleButton(
            checked = false,
            enabled = false,
            selectionControl = {
                Switch(checked = false, enabled = false)
            }
        )
    }

    @Test
    fun disabled_split_toggle_button_selected_radio() = verifyScreenshot {
        sampleSplitToggleButton(
            checked = true,
            enabled = false,
            selectionControl = {
                RadioButton(selected = true, enabled = false)
            }
        )
    }

    @Test
    fun disabled_split_toggle_button_unselected_radio() = verifyScreenshot {
        sampleSplitToggleButton(
            checked = false,
            enabled = false,
            selectionControl = {
                RadioButton(selected = false, enabled = false)
            }
        )
    }

    @Test
    fun disabled_split_toggle_button_checked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitToggleButton(
                checked = true,
                enabled = false,
                selectionControl = {
                    Checkbox(checked = true, enabled = false)
                }
            )
        }

    @Test
    fun disabled_split_toggle_button_unchecked_checkbox_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitToggleButton(
                checked = false,
                enabled = false,
                selectionControl = {
                    Checkbox(checked = false, enabled = false)
                }
            )
        }

    @Test
    fun disabled_split_toggle_button_checked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitToggleButton(
                checked = true,
                enabled = false,
                selectionControl = {
                    Switch(checked = true, enabled = false)
                }
            )
        }

    @Test
    fun disabled_split_toggle_button_unchecked_switch_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitToggleButton(
                checked = false,
                enabled = false,
                selectionControl = {
                    Switch(checked = false, enabled = false)
                }
            )
        }

    @Test
    fun disabled_split_toggle_button_selected_radio_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitToggleButton(
                checked = true,
                enabled = false,
                selectionControl = {
                    RadioButton(selected = true, enabled = false)
                }
            )
        }

    @Test
    fun disabled_split_toggle_button_unselected_radio_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            sampleSplitToggleButton(
                checked = false,
                enabled = false,
                selectionControl = {
                    RadioButton(selected = false, enabled = false)
                }
            )
        }

    @Composable
    private fun sampleToggleButton(
        enabled: Boolean = true,
        checked: Boolean = true,
        selectionControl: @Composable () -> Unit = {
            Checkbox(checked = checked)
        },
    ) {
        ToggleButton(
            icon = { TestIcon() },
            label = {
                Text("ToggleButton")
            },
            secondaryLabel = {
                Text("Secondary label")
            },
            checked = checked,
            enabled = enabled,
            selectionControl = selectionControl,
            onCheckedChange = {},
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    @Composable
    private fun sampleSplitToggleButton(
        checked: Boolean = true,
        enabled: Boolean = true,
        selectionControl: @Composable () -> Unit = {
        }
    ) {
        SplitToggleButton(
            label = {
                Text("SplitToggleButton")
            },
            secondaryLabel = {
                Text("Secondary label")
            },
            checked = checked,
            enabled = enabled,
            selectionControl = {
                selectionControl()
            },
            onCheckedChange = {},
            onClick = {},
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    private fun verifyScreenshot(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit
    ) {
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                content()
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}
