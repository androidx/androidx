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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
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
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class SliderScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    val wrap = Modifier.requiredWidth(200.dp).wrapContentSize(Alignment.TopStart)

    private val wrapperTestTag = "sliderWrapper"

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_origin() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) { Slider(remember { SliderState(0f) }) }
        }
        assertSliderAgainstGolden("slider_origin")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_origin_rtl() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(wrap.testTag(wrapperTestTag)) { Slider(remember { SliderState(0f) }) }
            }
        }
        assertSliderAgainstGolden("slider_origin_rtl")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_withSteps_rtl() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(wrap.testTag(wrapperTestTag)) {
                    Slider(remember { SliderState(value = 0.2f, steps = 4) })
                }
            }
        }
        assertSliderAgainstGolden("slider_withSteps_rtl")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_withSteps_rtl_lookaheadScope() {
        rule.setMaterialContent(lightColorScheme()) {
            LookaheadScope {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(wrap.testTag(wrapperTestTag)) {
                        Slider(remember { SliderState(value = 0.2f, steps = 4) })
                    }
                }
            }
        }
        assertSliderAgainstGolden("sliderTest_withSteps_rtl_lookaheadScope")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_origin_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(remember { SliderState(0f) }, enabled = false)
            }
        }
        assertSliderAgainstGolden("slider_origin_disabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) { Slider(remember { SliderState(0.5f) }) }
        }
        assertSliderAgainstGolden("slider_middle")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_no_gap() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    state = remember { SliderState(0.5f) },
                    track = { SliderDefaults.Track(sliderState = it, thumbTrackGapSize = 0.dp) },
                )
            }
        }
        assertSliderAgainstGolden("slider_middle_no_gap")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_no_inside_corner() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    state = remember { SliderState(0.5f) },
                    track = { SliderDefaults.Track(sliderState = it, trackInsideCornerSize = 0.dp) },
                )
            }
        }
        assertSliderAgainstGolden("slider_middle_no_inside_corner")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_no_stop_indicator() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    state = remember { SliderState(0.5f) },
                    track = { SliderDefaults.Track(sliderState = it, drawStopIndicator = null) },
                )
            }
        }
        assertSliderAgainstGolden("slider_middle_no_stop_indicator")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_dark() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) { Slider(remember { SliderState(0.5f) }) }
        }
        assertSliderAgainstGolden("slider_middle_dark")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_dark_disabled() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(remember { SliderState(0.5f) }, enabled = false)
            }
        }
        assertSliderAgainstGolden("slider_middle_dark_disabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_end() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) { Slider(remember { SliderState(1f) }) }
        }
        assertSliderAgainstGolden("slider_end")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_end_rtl() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(wrap.testTag(wrapperTestTag)) { Slider(remember { SliderState(1f) }) }
            }
        }
        assertSliderAgainstGolden("slider_end_rtl")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_steps() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) { Slider(remember { SliderState(0.5f, steps = 5) }) }
        }
        assertSliderAgainstGolden("slider_middle_steps")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_first_steps() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) { Slider(remember { SliderState(0.1f, steps = 9) }) }
        }
        assertSliderAgainstGolden("sliderTest_first_steps")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_last_steps() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) { Slider(remember { SliderState(0.9f, steps = 9) }) }
        }
        assertSliderAgainstGolden("sliderTest_last_steps")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_steps_dark() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) { Slider(remember { SliderState(0.5f, steps = 5) }) }
        }
        assertSliderAgainstGolden("slider_middle_steps_dark")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_steps_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(remember { SliderState(0.5f, steps = 5) }, enabled = false)
            }
        }
        assertSliderAgainstGolden("slider_middle_steps_disabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_middle_steps_custom_ticks() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    state = remember { SliderState(0.5f, steps = 5) },
                    track = {
                        SliderDefaults.Track(
                            sliderState = it,
                            drawTick = { offset, _ ->
                                drawCircle(
                                    color = Color.Red,
                                    center = offset,
                                    radius = SliderDefaults.TickSize.toPx() / 4,
                                )
                            },
                        )
                    },
                )
            }
        }
        assertSliderAgainstGolden("sliderTest_middle_steps_custom_ticks")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_customColors() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    remember { SliderState(0.5f, steps = 5) },
                    colors =
                        SliderDefaults.colors(
                            thumbColor = Color.Red,
                            activeTrackColor = Color.Blue,
                            activeTickColor = Color.Yellow,
                            inactiveTickColor = Color.Magenta,
                        ),
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
                    colors =
                        SliderDefaults.colors(
                            disabledThumbColor = Color.Blue,
                            disabledActiveTrackColor = Color.Red,
                            disabledInactiveTrackColor = Color.Yellow,
                            disabledActiveTickColor = Color.Magenta,
                            disabledInactiveTickColor = Color.Cyan,
                        ),
                )
            }
        }
        assertSliderAgainstGolden("slider_customColors_disabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun sliderTest_min_corner() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) { Slider(remember { SliderState(0.91f) }) }
        }
        assertSliderAgainstGolden("slider_min_corner")
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun sliderTest_middle_custom_corners_track_icons() {
        rule.setMaterialContent(lightColorScheme()) {
            val sliderState = remember { SliderState(0.5f) }
            val startIcon = rememberVectorPainter(Icons.Filled.Check)
            val endIcon = rememberVectorPainter(Icons.Filled.Clear)
            Box(wrap.testTag(wrapperTestTag)) {
                Slider(
                    state = sliderState,
                    track = {
                        with(LocalDensity.current) {
                            val iconSize = Size(20.dp.toPx(), 20.dp.toPx())
                            val iconPadding = 10.dp.toPx()
                            val thumbTrackGapSize = 6.dp.toPx()
                            val activeIconColor = SliderDefaults.colors().activeTickColor
                            val inactiveIconColor = SliderDefaults.colors().inactiveTickColor
                            val trackIconStart: DrawScope.(Offset, Color) -> Unit =
                                { offset, color ->
                                    translate(offset.x + iconPadding, offset.y) {
                                        with(startIcon) {
                                            draw(iconSize, colorFilter = ColorFilter.tint(color))
                                        }
                                    }
                                }
                            val trackIconEnd: DrawScope.(Offset, Color) -> Unit = { offset, color ->
                                translate(offset.x - iconPadding - iconSize.width, offset.y) {
                                    with(endIcon) {
                                        draw(iconSize, colorFilter = ColorFilter.tint(color))
                                    }
                                }
                            }
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier =
                                    Modifier.height(36.dp).drawWithContent {
                                        drawContent()

                                        val yOffset = size.height / 2 - iconSize.height / 2
                                        val activeTrackStart = 0f
                                        val activeTrackEnd =
                                            size.width * sliderState.coercedValueAsFraction -
                                                thumbTrackGapSize
                                        val inactiveTrackStart =
                                            activeTrackEnd + thumbTrackGapSize * 2
                                        val inactiveTrackEnd = size.width

                                        val activeTrackWidth = activeTrackEnd - activeTrackStart
                                        val inactiveTrackWidth =
                                            inactiveTrackEnd - inactiveTrackStart
                                        if (iconSize.width < activeTrackWidth - iconPadding * 2) {
                                            trackIconStart(
                                                Offset(activeTrackStart, yOffset),
                                                activeIconColor,
                                            )
                                            trackIconEnd(
                                                Offset(activeTrackEnd, yOffset),
                                                activeIconColor,
                                            )
                                        }
                                        if (iconSize.width < inactiveTrackWidth - iconPadding * 2) {
                                            trackIconStart(
                                                Offset(inactiveTrackStart, yOffset),
                                                inactiveIconColor,
                                            )
                                            trackIconEnd(
                                                Offset(inactiveTrackEnd, yOffset),
                                                inactiveIconColor,
                                            )
                                        }
                                    },
                                trackCornerSize = 12.dp,
                                drawStopIndicator = null,
                                thumbTrackGapSize = thumbTrackGapSize.toDp(),
                            )
                        }
                    },
                )
            }
        }
        assertSliderAgainstGolden("sliderTest_middle_custom_corners_track_icons")
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun verticalSliderTest() {
        rule.setMaterialContent(lightColorScheme()) {
            val sliderState = remember { SliderState(0.3f) }
            Box(wrap.testTag(wrapperTestTag)) {
                VerticalSlider(
                    state = sliderState,
                    modifier = Modifier.height(300.dp),
                    track = {
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.width(36.dp),
                            trackCornerSize = 12.dp,
                        )
                    },
                )
            }
        }
        assertSliderAgainstGolden("verticalSliderTest")
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun verticalSliderTest_rtl() {
        rule.setMaterialContent(lightColorScheme()) {
            val sliderState = remember { SliderState(0.3f) }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(wrap.testTag(wrapperTestTag)) {
                    VerticalSlider(
                        state = sliderState,
                        modifier = Modifier.height(300.dp),
                        track = {
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier = Modifier.width(36.dp),
                                trackCornerSize = 12.dp,
                            )
                        },
                    )
                }
            }
        }
        assertSliderAgainstGolden("verticalSliderTest_rtl")
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun verticalSliderTest_reversed() {
        rule.setMaterialContent(lightColorScheme()) {
            val sliderState = remember { SliderState(0.3f) }
            Box(wrap.testTag(wrapperTestTag)) {
                VerticalSlider(
                    state = sliderState,
                    modifier = Modifier.height(300.dp),
                    track = {
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.width(36.dp),
                            trackCornerSize = 12.dp,
                        )
                    },
                    reverseDirection = true,
                )
            }
        }
        assertSliderAgainstGolden("verticalSliderTest_reversed")
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun centeredSliderTest() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                val sliderState = remember { SliderState(value = -25f, valueRange = -50f..50f) }
                Slider(
                    sliderState,
                    track = { SliderDefaults.CenteredTrack(sliderState = sliderState) },
                )
            }
        }
        assertSliderAgainstGolden("centeredSliderTest")
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun centeredSliderTest_dark() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                val sliderState = remember { SliderState(value = -25f, valueRange = -50f..50f) }
                Slider(
                    sliderState,
                    track = { SliderDefaults.CenteredTrack(sliderState = sliderState) },
                )
            }
        }
        assertSliderAgainstGolden("centeredSliderTest_dark")
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun centeredSliderTest_rtl() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(wrap.testTag(wrapperTestTag)) {
                    val sliderState = remember { SliderState(value = -25f, valueRange = -50f..50f) }
                    Slider(
                        sliderState,
                        track = { SliderDefaults.CenteredTrack(sliderState = sliderState) },
                    )
                }
            }
        }
        assertSliderAgainstGolden("centeredSliderTest_rtl")
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun centeredSliderTest_middle() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                val sliderState = remember { SliderState(value = 0f, valueRange = -50f..50f) }
                Slider(
                    sliderState,
                    track = { SliderDefaults.CenteredTrack(sliderState = sliderState) },
                )
            }
        }
        assertSliderAgainstGolden("centeredSliderTest_middle")
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun centeredSliderTest_steps() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                val sliderState = remember {
                    SliderState(value = 30f, valueRange = -50f..50f, steps = 9)
                }
                Slider(
                    sliderState,
                    track = { SliderDefaults.CenteredTrack(sliderState = sliderState) },
                )
            }
        }
        assertSliderAgainstGolden("centeredSliderTest_steps")
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun verticalCenteredSliderTest() {
        rule.setMaterialContent(lightColorScheme()) {
            val sliderState = remember { SliderState(value = -25f, valueRange = -50f..50f) }
            Box(wrap.testTag(wrapperTestTag)) {
                VerticalSlider(
                    state = sliderState,
                    modifier = Modifier.height(300.dp),
                    track = { SliderDefaults.CenteredTrack(sliderState = sliderState) },
                )
            }
        }
        assertSliderAgainstGolden("verticalCenteredSliderTest")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_no_gap() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(
                    state = remember { RangeSliderState(0.5f, 1f) },
                    track = {
                        SliderDefaults.Track(rangeSliderState = it, thumbTrackGapSize = 0.dp)
                    },
                )
            }
        }
        assertSliderAgainstGolden("rangeSlider_middle_no_gap")
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun rangeSliderTest_middle_no_external_corner() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(
                    state = remember { RangeSliderState(0.5f, 1f) },
                    track = { SliderDefaults.Track(rangeSliderState = it, trackCornerSize = 0.dp) },
                )
            }
        }
        assertSliderAgainstGolden("rangeSlider_middle_no_external_corner")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_no_inside_corner() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(
                    state = remember { RangeSliderState(0.5f, 1f) },
                    track = {
                        SliderDefaults.Track(rangeSliderState = it, trackInsideCornerSize = 0.dp)
                    },
                )
            }
        }
        assertSliderAgainstGolden("rangeSlider_middle_no_inside_corner")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_no_inside_corner_rtl() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(wrap.testTag(wrapperTestTag)) {
                    RangeSlider(
                        state = remember { RangeSliderState(0.5f, 1f) },
                        track = {
                            SliderDefaults.Track(
                                rangeSliderState = it,
                                trackInsideCornerSize = 0.dp,
                            )
                        },
                    )
                }
            }
        }
        assertSliderAgainstGolden("rangeSliderTest_middle_no_inside_corner_rtl")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_no_inside_corner_rtl_lookaheadScope() {
        rule.setMaterialContent(lightColorScheme()) {
            LookaheadScope {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(wrap.testTag(wrapperTestTag)) {
                        RangeSlider(
                            state = remember { RangeSliderState(0.5f, 1f) },
                            track = {
                                SliderDefaults.Track(
                                    rangeSliderState = it,
                                    trackInsideCornerSize = 0.dp,
                                )
                            },
                        )
                    }
                }
            }
        }
        assertSliderAgainstGolden("rangeSliderTest_middle_no_inside_corner_rtl_lookaheadScope")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_no_stop_indicator() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(
                    state = remember { RangeSliderState(0.5f, 1f) },
                    track = {
                        SliderDefaults.Track(rangeSliderState = it, drawStopIndicator = null)
                    },
                )
            }
        }
        assertSliderAgainstGolden("rangeSlider_middle_no_stop_indicator")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_no_stop_indicator_rtl() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(wrap.testTag(wrapperTestTag)) {
                    RangeSlider(
                        state = remember { RangeSliderState(0.5f, 1f) },
                        track = {
                            SliderDefaults.Track(rangeSliderState = it, drawStopIndicator = null)
                        },
                    )
                }
            }
        }
        assertSliderAgainstGolden("rangeSlider_middle_no_stop_indicator_rtl")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_steps_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(remember { RangeSliderState(0.5f, 1f, steps = 5) }, enabled = false)
            }
        }
        assertSliderAgainstGolden("rangeSlider_middle_steps_disabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_steps_enabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(remember { RangeSliderState(0.5f, 1f, steps = 5) })
            }
        }
        assertSliderAgainstGolden("rangeSlider_middle_steps_enabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_steps_dark_enabled() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(remember { RangeSliderState(0.5f, 1f, steps = 5) })
            }
        }
        assertSliderAgainstGolden("rangeSlider_middle_steps_dark_enabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_steps_dark_disabled() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(remember { RangeSliderState(0.5f, 1f, steps = 5) }, enabled = false)
            }
        }
        assertSliderAgainstGolden("rangeSlider_middle_steps_dark_disabled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_middle_steps_custom_ticks() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(
                    state = remember { RangeSliderState(0.5f, 1f, steps = 5) },
                    track = {
                        SliderDefaults.Track(
                            rangeSliderState = it,
                            drawTick = { offset, _ ->
                                drawCircle(
                                    color = Color.Red,
                                    center = offset,
                                    radius = SliderDefaults.TickSize.toPx() / 4,
                                )
                            },
                        )
                    },
                )
            }
        }
        assertSliderAgainstGolden("rangeSliderTest_middle_steps_custom_ticks")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_overlappingThumbs() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(remember { RangeSliderState(0.5f, 0.51f) })
            }
        }
        assertSliderAgainstGolden("rangeSlider_overlappingThumbs")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_fullRange() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) { RangeSlider(remember { RangeSliderState(0f, 1f) }) }
        }
        assertSliderAgainstGolden("rangeSlider_fullRange")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_asymmetric_startEnd() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                RangeSlider(remember { RangeSliderState(0.25f, 0.6f) })
            }
        }
        assertSliderAgainstGolden("rangeSliderTest_asymmetric_startEnd")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_asymmetric_startEnd_rtl() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(wrap.testTag(wrapperTestTag)) {
                    RangeSlider(remember { RangeSliderState(0.25f, 0.6f) })
                }
            }
        }
        assertSliderAgainstGolden("rangeSliderTest_asymmetric_startEnd_rtl")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun rangeSliderTest_steps_customColors() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                val state = remember {
                    RangeSliderState(30f, 70f, steps = 9, valueRange = 0f..100f)
                }
                RangeSlider(
                    state = state,
                    colors =
                        SliderDefaults.colors(
                            thumbColor = Color.Blue,
                            activeTrackColor = Color.Red,
                            inactiveTrackColor = Color.Yellow,
                            activeTickColor = Color.Magenta,
                            inactiveTickColor = Color.Cyan,
                        ),
                )
            }
        }
        assertSliderAgainstGolden("rangeSlider_steps_customColors")
    }

    private fun assertSliderAgainstGolden(goldenName: String) {
        rule
            .onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
