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

package androidx.ui.material

import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.TestTag
import androidx.ui.core.globalPosition
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.contentColor
import androidx.ui.foundation.currentTextStyle
import androidx.ui.layout.Column
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.doGesture
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.sendClick
import androidx.ui.text.FirstBaseline
import androidx.ui.text.TextStyle
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.Px
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.sp
import androidx.ui.unit.toPx
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class FilledTextFieldTest {

    private val ExpectedMinimumTextFieldHeight = 56.dp
    private val ExpectedPadding = 16.dp
    private val IconPadding = 12.dp
    private val ExpectedBaselineOffset = 20.dp
    private val IconColorAlpha = 0.54f

    @get:Rule
    val testRule = createComposeRule()

    @Test
    fun testTextFieldMinimumHeight() {
        testRule
            .setMaterialContentAndCollectSizes {
                FilledTextField(
                    value = "input",
                    onValueChange = {},
                    label = {}
                )
            }
            .assertHeightEqualsTo(ExpectedMinimumTextFieldHeight)
    }

    @Test
    fun testTextField_singleFocus() {
        var textField1Focused = false
        val textField1Tag = "TextField1"

        var textField2Focused = false
        val textField2Tag = "TextField2"

        testRule.setMaterialContent {
            Column {
                TestTag(tag = textField1Tag) {
                    FilledTextField(
                        value = "input1",
                        onValueChange = {},
                        label = {},
                        onFocusChange = { textField1Focused = it }
                    )
                }
                TestTag(tag = textField2Tag) {
                    FilledTextField(
                        value = "input2",
                        onValueChange = {},
                        label = {},
                        onFocusChange = { textField2Focused = it }
                    )
                }
            }
        }

        findByTag(textField1Tag).doClick()

        runOnIdleCompose {
            assertThat(textField1Focused).isTrue()
            assertThat(textField2Focused).isFalse()
        }

        findByTag(textField2Tag).doClick()

        runOnIdleCompose {
            assertThat(textField1Focused).isFalse()
            assertThat(textField2Focused).isTrue()
        }
    }

    @Test
    fun testGetFocus_whenClickedOnSurfaceArea() {
        var focused = false
        testRule.setMaterialContent {
            Box {
                TestTag("textField") {
                    FilledTextField(
                        value = "input",
                        onValueChange = {},
                        label = {},
                        onFocusChange = { focused = it }
                    )
                }
            }
        }

        // Click on (2, 2) which is Surface area and outside input area
        findByTag("textField").doGesture {
            sendClick(PxPosition(2.ipx, 2.ipx))
        }

        testRule.runOnIdleComposeWithDensity {
            assertThat(focused).isTrue()
        }
    }

    @Test
    fun testLabelPosition_initial_withDefaultHeight() {
        val labelSize = Ref<IntPxSize>()
        val labelPosition = Ref<PxPosition>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    value = "",
                    onValueChange = {},
                    label = {
                        Text(text = "label", style = TextStyle(fontSize = 10.sp),
                            modifier = Modifier.onPositioned {
                            labelPosition.value = it.globalPosition
                            labelSize.value = it.size
                        })
                    }
                )
            }
        }

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0.ipx)
            assertThat(labelSize.value?.width).isGreaterThan(0.ipx)
            // centered position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toPx()
            )
            assertThat(labelPosition.value?.y).isEqualTo(
                ((ExpectedMinimumTextFieldHeight.toIntPx() - labelSize.value!!.height) / 2f).toPx()
            )
        }
    }

    @Test
    fun testLabelPosition_initial_withCustomHeight() {
        val height = 80.dp
        val labelSize = Ref<IntPxSize>()
        val labelPosition = Ref<PxPosition>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.preferredHeight(height),
                    label = {
                        Text(text = "label", modifier = Modifier.onPositioned {
                            labelPosition.value = it.globalPosition
                            labelSize.value = it.size
                        })
                    }
                )
            }
        }

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0.ipx)
            assertThat(labelSize.value?.width).isGreaterThan(0.ipx)
            // centered position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toPx()
            )
            assertThat(labelPosition.value?.y).isEqualTo(
                ((height.toIntPx() - labelSize.value!!.height) / 2f).toPx()
            )
        }
    }

    @Test
    fun testLabelPosition_whenFocused() {
        val labelSize = Ref<IntPxSize>()
        val labelPosition = Ref<PxPosition>()
        val baseline = Ref<Px>()
        testRule.setMaterialContent {
            Box {
                TestTag("textField") {
                    FilledTextField(
                        value = "",
                        onValueChange = {},
                        label = {
                            Text(text = "label", modifier = Modifier.onPositioned {
                                labelPosition.value = it.globalPosition
                                labelSize.value = it.size
                                baseline.value =
                                    it[FirstBaseline]!!.toPx() + labelPosition.value!!.y
                            })
                        }
                    )
                }
            }
        }

        // click to focus
        clickAndAdvanceClock("textField", 200)

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0.ipx)
            assertThat(labelSize.value?.width).isGreaterThan(0.ipx)
            // label's top position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toPx()
            )
            assertThat(baseline.value).isEqualTo(
                ExpectedBaselineOffset.toIntPx().toPx()
            )
        }
    }

    @Test
    fun testLabelPosition_whenInput() {
        val labelSize = Ref<IntPxSize>()
        val labelPosition = Ref<PxPosition>()
        val baseline = Ref<Px>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    value = "input",
                    onValueChange = {},
                    label = {
                        Text(text = "label", modifier = Modifier.onPositioned {
                            labelPosition.value = it.globalPosition
                            labelSize.value = it.size
                            baseline.value =
                                it[FirstBaseline]!!.toPx() + labelPosition.value!!.y
                        })
                    }
                )
            }
        }

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0.ipx)
            assertThat(labelSize.value?.width).isGreaterThan(0.ipx)
            // label's top position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toPx()
            )
            assertThat(baseline.value).isEqualTo(
                ExpectedBaselineOffset.toIntPx().toPx()
            )
        }
    }

    @Test
    fun testPlaceholderPosition_withLabel() {
        val placeholderSize = Ref<IntPxSize>()
        val placeholderPosition = Ref<PxPosition>()
        val placeholderBaseline = Ref<Px>()
        testRule.setMaterialContent {
            Box {
                TestTag("textField") {
                    FilledTextField(
                        value = "",
                        onValueChange = {},
                        label = {},
                        placeholder = {
                            Text(text = "placeholder", modifier = Modifier.onPositioned {
                                placeholderPosition.value = it.globalPosition
                                placeholderSize.value = it.size
                                placeholderBaseline.value =
                                    it[FirstBaseline]!!.toPx() + placeholderPosition.value!!.y
                            })
                        }
                    )
                }
            }
        }
        // click to focus
        clickAndAdvanceClock("textField", 200)

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(placeholderSize.value).isNotNull()
            assertThat(placeholderSize.value?.height).isGreaterThan(0.ipx)
            assertThat(placeholderSize.value?.width).isGreaterThan(0.ipx)
            // placeholder's position
            assertThat(placeholderPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toPx()
            )
            assertThat(placeholderBaseline.value).isEqualTo(
                ExpectedBaselineOffset.toIntPx().toPx() * 2
            )
        }
    }

    @Test
    fun testNoPlaceholder_whenInputNotEmpty() {
        val placeholderSize = Ref<IntPxSize>()
        val placeholderPosition = Ref<PxPosition>()
        testRule.setMaterialContent {
            Box {
                TestTag("textField") {
                    FilledTextField(
                        value = "input",
                        onValueChange = {},
                        label = {},
                        placeholder = {
                            Text(text = "placeholder", modifier = Modifier.onPositioned {
                                placeholderPosition.value = it.globalPosition
                                placeholderSize.value = it.size
                            })
                        }
                    )
                }
            }
        }

        // click to focus
        clickAndAdvanceClock("textField", 200)

        testRule.runOnIdleComposeWithDensity {
            assertThat(placeholderSize.value).isNull()
            assertThat(placeholderPosition.value).isNull()
        }
    }

    @Test
    fun testPlaceholderColorAndTextStyle() {
        testRule.setMaterialContent {
            TestTag("textField") {
                FilledTextField(
                    value = "",
                    onValueChange = {},
                    label = {},
                    placeholder = {
                        Text("placeholder")
                        assertThat(contentColor())
                            .isEqualTo(MaterialTheme.colors.onSurface.copy(0.6f))
                        assertThat(currentTextStyle()).isEqualTo(MaterialTheme.typography.subtitle1)
                    }
                )
            }
        }

        // click to focus
        findByTag("textField").doClick()
    }

    @Test
    fun testTrailingAndLeading_sizeAndPosition() {
        val textFieldHeight = 60.dp
        val textFieldWidth = 300.dp
        val size = 30.dp
        val leadingPosition = Ref<PxPosition>()
        val leadingSize = Ref<IntPxSize>()
        val trailingPosition = Ref<PxPosition>()
        val trailingSize = Ref<IntPxSize>()

        testRule.setMaterialContent {
            FilledTextField(
                value = "text",
                onValueChange = {},
                modifier = Modifier.preferredSize(textFieldWidth, textFieldHeight),
                label = {},
                leadingIcon = {
                    Box(Modifier.preferredSize(size).onPositioned {
                        leadingPosition.value = it.globalPosition
                        leadingSize.value = it.size
                    })
                },
                trailingIcon = {
                    Box(Modifier.preferredSize(size).onPositioned {
                        trailingPosition.value = it.globalPosition
                        trailingSize.value = it.size
                    })
                }
            )
        }

        testRule.runOnIdleComposeWithDensity {
            // leading
            assertThat(leadingSize.value).isEqualTo(IntPxSize(size.toIntPx(), size.toIntPx()))
            assertThat(leadingPosition.value?.x).isEqualTo(IconPadding.toIntPx().toPx())
            assertThat(leadingPosition.value?.y).isEqualTo(
                ((textFieldHeight.toIntPx() - leadingSize.value!!.height) / 2f).toPx()
            )
            // trailing
            assertThat(trailingSize.value).isEqualTo(IntPxSize(size.toIntPx(), size.toIntPx()))
            assertThat(trailingPosition.value?.x).isEqualTo(
                (textFieldWidth.toIntPx() - IconPadding.toIntPx() - trailingSize.value!!.width)
                    .toPx()
            )
            assertThat(trailingPosition.value?.y).isEqualTo(
                ((textFieldHeight.toIntPx() - trailingSize.value!!.height) / 2f).toPx()
            )
        }
    }

    @Test
    fun testLabelPositionX_initial_withTrailingAndLeading() {
        val height = 60.dp
        val iconSize = 30.dp
        val labelPosition = Ref<PxPosition>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.preferredHeight(height),
                    label = {
                        Text(text = "label", modifier = Modifier.onPositioned {
                            labelPosition.value = it.globalPosition
                        })
                    },
                    trailingIcon = { Box(Modifier.preferredSize(iconSize)) },
                    leadingIcon = { Box(Modifier.preferredSize(iconSize)) }
                )
            }
        }

        testRule.runOnIdleComposeWithDensity {
            assertThat(labelPosition.value?.x).isEqualTo(
                (ExpectedPadding.toIntPx() + IconPadding.toIntPx() + iconSize.toIntPx()).toPx()
            )
        }
    }

    @Test
    fun testLabelPositionX_initial_withEmptyTrailingAndLeading() {
        val height = 60.dp
        val labelPosition = Ref<PxPosition>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.preferredHeight(height),
                    label = {
                        Text(text = "label", modifier = Modifier.onPositioned {
                            labelPosition.value = it.globalPosition
                        })
                    },
                    trailingIcon = {},
                    leadingIcon = {}
                )
            }
        }

        testRule.runOnIdleComposeWithDensity {
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toPx()
            )
        }
    }

    @Test
    fun testColorInLeadingTrailing_whenValidInput() {
        testRule.setMaterialContent {
            FilledTextField(
                value = "",
                onValueChange = {},
                label = {},
                isErrorValue = false,
                leadingIcon = {
                    assertThat(contentColor())
                        .isEqualTo(MaterialTheme.colors.onSurface.copy(IconColorAlpha))
                },
                trailingIcon = {
                    assertThat(contentColor())
                        .isEqualTo(MaterialTheme.colors.onSurface.copy(IconColorAlpha))
                }
            )
        }
    }

    @Test
    fun testColorInLeadingTrailing_whenInvalidInput() {
        testRule.setMaterialContent {
            FilledTextField(
                value = "",
                onValueChange = {},
                label = {},
                isErrorValue = true,
                leadingIcon = {
                    assertThat(contentColor())
                        .isEqualTo(MaterialTheme.colors.onSurface.copy(IconColorAlpha))
                },
                trailingIcon = {
                    assertThat(contentColor()).isEqualTo(MaterialTheme.colors.error)
                }
            )
        }
    }

    private fun clickAndAdvanceClock(tag: String, time: Long) {
        testRule.clockTestRule.pauseClock()
        findByTag(tag).doClick()
        runOnIdleCompose { }
        testRule.clockTestRule.advanceClock(time)
    }
}