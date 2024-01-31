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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class SliderScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    val wrap = Modifier.requiredWidth(70.dp).wrapContentSize(Alignment.TopStart)

    private val wrapperTestTag = "sliderWrapper"

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_origin() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    remember { SliderState(0f) }
                )
            }
        }
        assertSliderAgainstGolden("slider_origin")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_origin_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    remember { SliderState(0f) },
                    enabled = false
                )
            }
        }
        assertSliderAgainstGolden("slider_origin_disabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    remember { SliderState(0.5f) }
                )
            }
        }
        assertSliderAgainstGolden("slider_middle")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_dark() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    remember { SliderState(0.5f) }
                )
            }
        }
        assertSliderAgainstGolden("slider_middle_dark")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_dark_disabled() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    remember { SliderState(0.5f) },
                    enabled = false
                )
            }
        }
        assertSliderAgainstGolden("slider_middle_dark_disabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_end() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    remember { SliderState(1f) }
                )
            }
        }
        assertSliderAgainstGolden("slider_end")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_steps() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    remember { SliderState(0.5f, steps = 5) }
                )
            }
        }
        assertSliderAgainstGolden("slider_middle_steps")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_steps_dark() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    remember { SliderState(0.5f, steps = 5) }
                )
            }
        }
        assertSliderAgainstGolden("slider_middle_steps_dark")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_steps_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    remember { SliderState(0.5f, steps = 5) },
                    enabled = false
                )
            }
        }
        assertSliderAgainstGolden("slider_middle_steps_disabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_customColors() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    remember { SliderState(0.5f, steps = 5) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Red,
                        activeTrackColor = Color.Blue,
                        activeTickColor = Color.Yellow,
                        inactiveTickColor = Color.Magenta
                    )

                )
            }
        }
        assertSliderAgainstGolden("slider_customColors")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_customColors_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    remember { SliderState(0.5f, steps = 5) },
                    enabled = false,
                    // this is intentionally made to appear as enabled in disabled state for a
                    // brighter test
                    colors = SliderDefaults.colors(
                        disabledThumbColor = Color.Blue,
                        disabledActiveTrackColor = Color.Red,
                        disabledInactiveTrackColor = Color.Yellow,
                        disabledActiveTickColor = Color.Magenta,
                        disabledInactiveTickColor = Color.Cyan
                    )

                )
            }
        }
        assertSliderAgainstGolden("slider_customColors_disabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_steps_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(
                    remember {
                        RangeSliderState(0.5f, 1f, steps = 5)
                    },
                    enabled = false
                )
            }
        }
        assertSliderAgainstGolden("rangeSlider_middle_steps_disabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_steps_enabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(
                    remember {
                        RangeSliderState(
                            0.5f,
                            1f,
                            steps = 5
                        )
                    }
                )
            }
        }
        assertSliderAgainstGolden("rangeSlider_middle_steps_enabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_steps_dark_enabled() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(
                    remember {
                        RangeSliderState(
                            0.5f,
                            1f,
                            steps = 5
                        )
                    }
                )
            }
        }
        assertSliderAgainstGolden("rangeSlider_middle_steps_dark_enabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_steps_dark_disabled() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(
                    remember {
                        RangeSliderState(0.5f, 1f, steps = 5)
                    },
                    enabled = false
                )
            }
        }
        assertSliderAgainstGolden("rangeSlider_middle_steps_dark_disabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_overlapingThumbs() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(
                    remember {
                        RangeSliderState(0.5f, 0.51f)
                    }
                )
            }
        }
        assertSliderAgainstGolden("rangeSlider_overlapingThumbs")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_fullRange() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(
                    remember {
                        RangeSliderState(0f, 1f)
                    }
                )
            }
        }
        assertSliderAgainstGolden("rangeSlider_fullRange")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_steps_customColors() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                val state = remember {
                    RangeSliderState(
                        30f,
                        70f,
                        steps = 9,
                        valueRange = 0f..100f
                    )
                }
                RangeSlider(
                    state = state,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Blue,
                        activeTrackColor = Color.Red,
                        inactiveTrackColor = Color.Yellow,
                        activeTickColor = Color.Magenta,
                        inactiveTickColor = Color.Cyan
                    )
                )
            }
        }
        assertSliderAgainstGolden("rangeSlider_steps_customColors")
    }

    private fun assertSliderAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
