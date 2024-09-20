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
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.EditorInfo.IME_ACTION_GO
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.internal.AboveLabelBottomPadding
import androidx.compose.material3.internal.AboveLabelHorizontalPadding
import androidx.compose.material3.internal.MinFocusedLabelLineHeight
import androidx.compose.material3.internal.MinSupportingTextLineHeight
import androidx.compose.material3.internal.MinTextLineHeight
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.SupportingTopPadding
import androidx.compose.material3.internal.TextFieldPadding
import androidx.compose.material3.internal.getString
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class OutlinedTextFieldTest {
    private val ExpectedMinimumTextFieldHeight = OutlinedTextFieldDefaults.MinHeight
    private val ExpectedDefaultTextFieldWidth = OutlinedTextFieldDefaults.MinWidth
    private val OutlinedTextFieldTopPadding = 8.sp
    private val ExpectedPadding = TextFieldPadding
    private val IconPadding = 12.dp
    private val TextFieldTag = "textField"

    @get:Rule val rule = createComposeRule()

    @Test
    fun testOutlinedTextField_setSmallWidth() {
        rule
            .setMaterialContentForSizeAssertions {
                OutlinedTextField(
                    state = rememberTextFieldState("input"),
                    modifier = Modifier.requiredWidth(40.dp)
                )
            }
            .assertWidthIsEqualTo(40.dp)
    }

    @Test
    fun testOutlinedTextField_defaultWidth() {
        rule
            .setMaterialContentForSizeAssertions {
                OutlinedTextField(rememberTextFieldState("input"))
            }
            .assertWidthIsEqualTo(ExpectedDefaultTextFieldWidth)
    }

    @Test
    fun testOutlinedTextFields_singleFocus() {
        var textField1Focused = false
        val textField1Tag = "TextField1"

        var textField2Focused = false
        val textField2Tag = "TextField2"

        rule.setMaterialContent(lightColorScheme()) {
            Column {
                OutlinedTextField(
                    modifier =
                        Modifier.testTag(textField1Tag).onFocusChanged {
                            textField1Focused = it.isFocused
                        },
                    state = rememberTextFieldState("input1"),
                )
                OutlinedTextField(
                    modifier =
                        Modifier.testTag(textField2Tag).onFocusChanged {
                            textField2Focused = it.isFocused
                        },
                    state = rememberTextFieldState("input2"),
                )
            }
        }

        rule.onNodeWithTag(textField1Tag).performClick()

        rule.runOnIdle {
            assertThat(textField1Focused).isTrue()
            assertThat(textField2Focused).isFalse()
        }

        rule.onNodeWithTag(textField2Tag).performClick()

        rule.runOnIdle {
            assertThat(textField1Focused).isFalse()
            assertThat(textField2Focused).isTrue()
        }
    }

    @Test
    fun testOutlinedTextField_getFocus_whenClickedOnInternalArea() {
        var focused = false
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedTextField(
                    modifier =
                        Modifier.testTag(TextFieldTag).onFocusChanged { focused = it.isFocused },
                    state = rememberTextFieldState("input"),
                )
            }
        }

        // Click on (2, 2) which is a background area and outside input area
        rule.onNodeWithTag(TextFieldTag).performTouchInput { click(Offset(2f, 2f)) }

        rule.runOnIdleWithDensity { assertThat(focused).isTrue() }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testOutlinedTextField_noTopPadding_ifNoLabel() {
        val density = Density(4f)
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                Box(Modifier.testTag("box").background(Color.Red)) {
                    OutlinedTextField(
                        state = rememberTextFieldState(),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = Color.White
                            ),
                        shape = RectangleShape
                    )
                }
            }
        }

        rule
            .onNodeWithTag("box")
            .captureToImage()
            .assertShape(
                density = density,
                horizontalPadding = 1.dp, // OutlinedTextField border thickness
                verticalPadding = 1.dp, // OutlinedTextField border thickness
                backgroundColor = Color.White, // OutlinedTextField border color
                shapeColor = Color.Red, // Color of background as OutlinedTextField is transparent
                shape = RectangleShape
            )
    }

    @Test
    fun testOutlinedTextField_labelPosition_initial_singleLine() {
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                lineLimits = TextFieldLineLimits.SingleLine,
                label = {
                    Box(
                        Modifier.size(MinTextLineHeight).onGloballyPositioned {
                            labelPosition.value = it.positionInRoot()
                        }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            // x position is start + padding
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            // y position is centered, plus additional padding allowance on top
            assertThat(labelPosition.value?.y)
                .isWithin(1f)
                .of(
                    ((ExpectedMinimumTextFieldHeight - MinTextLineHeight) / 2 +
                            OutlinedTextFieldTopPadding.toDp())
                        .toPx()
                )
        }
    }

    @Test
    fun testOutlinedTextField_labelPosition_initial_withDefaultHeight() {
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = {
                    Box(
                        Modifier.size(MinTextLineHeight).onGloballyPositioned {
                            labelPosition.value = it.positionInRoot()
                        }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            // x position is start + padding
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            // y position is top + default padding + label padding allowance
            assertThat(labelPosition.value?.y)
                .isWithin(1f)
                .of((ExpectedPadding + OutlinedTextFieldTopPadding.toDp()).toPx())
        }
    }

    @Test
    fun testOutlinedTextField_labelPosition_initial_withMultiLineLabel() {
        val textFieldWidth = 200.dp
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.requiredWidth(textFieldWidth),
                label = {
                    Text(
                        text = "long long long long long long long long long long long long",
                        modifier =
                            Modifier.onGloballyPositioned {
                                labelSize.value = it.size
                                labelPosition.value = it.positionInRoot()
                            }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            // label size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width)
                .isEqualTo(textFieldWidth.roundToPx() - 2 * ExpectedPadding.roundToPx())

            // x position is start + padding
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            // y position is top + default padding + label padding allowance
            assertThat(labelPosition.value?.y)
                .isWithin(1f)
                .of((ExpectedPadding + OutlinedTextFieldTopPadding.toDp()).toPx())
        }
    }

    @Test
    fun testOutlinedTextField_labelPosition_whenFocused() {
        val labelPosition = Ref<Offset>()
        val labelSize = MinFocusedLabelLineHeight

        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                modifier = Modifier.testTag(TextFieldTag),
                state = rememberTextFieldState(),
                label = {
                    Box(
                        Modifier.size(MinFocusedLabelLineHeight).onGloballyPositioned {
                            labelPosition.value = it.positionInRoot()
                        }
                    )
                }
            )
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            assertThat(labelPosition.value?.y)
                .isWithin(1f)
                .of(getLabelPosition(labelSize.roundToPx()).toFloat())
        }
    }

    @Test
    fun testOutlinedTextField_labelPosition_whenFocused_withMultiLineLabel() {
        val textFieldWidth = 200.dp
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(textFieldWidth),
                label = {
                    Text(
                        text = "long long long long long long long long long long long long",
                        modifier =
                            Modifier.onGloballyPositioned {
                                labelSize.value = it.size
                                labelPosition.value = it.positionInRoot()
                            }
                    )
                }
            )
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            // label size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width!!.toFloat())
                .isWithin(1f)
                .of((textFieldWidth - ExpectedPadding * 2).toPx())

            // label position
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            assertThat(labelPosition.value?.y)
                .isWithin(1f)
                .of(getLabelPosition(labelSize.value!!.height).toFloat())
        }
    }

    @Test
    fun testOutlinedTextField_labelPosition_whenPositionedAbove() {
        val labelPosition = Ref<Offset>()
        rule
            .setMaterialContentForSizeAssertions {
                OutlinedTextField(
                    state = rememberTextFieldState(),
                    label = {
                        Box(
                            Modifier.size(MinFocusedLabelLineHeight).onGloballyPositioned {
                                labelPosition.value = it.positionInRoot()
                            }
                        )
                    },
                    labelPosition = TextFieldLabelPosition.Above(),
                )
            }
            .assertHeightIsEqualTo(
                MinFocusedLabelLineHeight + AboveLabelBottomPadding + ExpectedMinimumTextFieldHeight
            )

        rule.runOnIdleWithDensity {
            // x position is padding
            assertThat(labelPosition.value?.x).isWithin(1f).of(AboveLabelHorizontalPadding.toPx())
            // y position is 0
            assertThat(labelPosition.value?.y).isEqualTo(0f)
        }
    }

    @Test
    fun testOutlinedTextField_labelPosition_customAlignment() {
        val labelPosition = Ref<Offset>()
        val labelSize = MinFocusedLabelLineHeight
        rule.setMaterialContentForSizeAssertions {
            OutlinedTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(TextFieldTag),
                label = {
                    Box(
                        Modifier.size(labelSize).onGloballyPositioned {
                            labelPosition.value = it.positionInRoot()
                        }
                    )
                },
                labelPosition =
                    TextFieldLabelPosition.Default(
                        minimizedAlignment = Alignment.End,
                        expandedAlignment = Alignment.CenterHorizontally
                    ),
            )
        }

        rule.runOnIdleWithDensity {
            // centered horizontally
            assertThat(labelPosition.value?.x)
                .isWithin(1f)
                .of(((ExpectedDefaultTextFieldWidth - labelSize) / 2).toPx())
        }

        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            // end
            assertThat(labelPosition.value?.x)
                .isWithin(1f)
                .of((ExpectedDefaultTextFieldWidth - TextFieldPadding - labelSize).toPx())
        }
    }

    @Test
    fun testOutlinedTextField_labelHeight_contributesToTextFieldMeasurements_whenUnfocused() {
        val tfSize = Ref<IntSize>()
        val labelHeight = 200.dp
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                modifier =
                    Modifier.testTag(TextFieldTag).onGloballyPositioned { tfSize.value = it.size },
                label = { Box(Modifier.size(width = 50.dp, height = labelHeight)) },
            )
        }

        rule.runOnIdleWithDensity {
            assertThat(tfSize.value!!.height).isAtLeast(labelHeight.roundToPx())
        }
    }

    @Test
    fun testOutlinedTextField_labelWidth_isNotAffectedByTrailingIcon_whenFocused() {
        val textFieldWidth = 100.dp
        val labelRequestedWidth = 65.dp
        val labelSize = Ref<IntSize>()
        val trailingSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(TextFieldTag).requiredWidth(textFieldWidth),
                label = {
                    Text(
                        text = "Label",
                        modifier =
                            Modifier.width(labelRequestedWidth).onGloballyPositioned {
                                labelSize.value = it.size
                            }
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Default.Favorite,
                        null,
                        modifier = Modifier.onGloballyPositioned { trailingSize.value = it.size },
                    )
                },
            )
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            assertThat(labelSize.value).isNotNull()
            assertThat(trailingSize.value).isNotNull()

            // First, check that label's requested size would be too wide if it's on the same line
            // as the icon + padding
            assertThat((labelRequestedWidth + IconPadding).roundToPx() + trailingSize.value!!.width)
                .isGreaterThan(textFieldWidth.roundToPx())

            // Next, assert that the requested size is satisfied anyway because the trailing icon
            // does not affect it.
            assertThat(labelSize.value?.width).isEqualTo(labelRequestedWidth.roundToPx())
        }
    }

    @Test
    fun testOutlinedTextField_labelPosition_whenInput() {
        val labelSize = MinFocusedLabelLineHeight
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState("input"),
                label = {
                    Box(
                        Modifier.size(labelSize).onGloballyPositioned {
                            labelPosition.value = it.positionInRoot()
                        }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            // label position
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            assertThat(labelPosition.value?.y)
                .isWithin(1f)
                .of(getLabelPosition(labelSize.roundToPx()).toFloat())
        }
    }

    @Test
    fun testOutlinedTextField_labelScope_progressAndRecomposition() {
        val progressValue = Ref<Float>()
        var compositionCount = 0
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(TextFieldTag),
                label = {
                    SideEffect { compositionCount++ }

                    // lambda reads `progress` in the draw phase
                    Box(Modifier.graphicsLayer { progressValue.value = progress })
                }
            )
        }

        assertThat(progressValue.value).isEqualTo(0f)
        assertThat(compositionCount).isEqualTo(1)

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()
        rule.waitForIdle()

        assertThat(progressValue.value).isEqualTo(1f)
        assertThat(compositionCount).isEqualTo(1)
    }

    @Test
    fun testOutlinedTextField_transparentComponents_doNotAppearInComposition() {
        // Regression test for b/251162419
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = { Text(text = "Label") },
                placeholder = {
                    Text(text = "Placeholder", modifier = Modifier.testTag("Placeholder"))
                },
                prefix = { Text(text = "Prefix", modifier = Modifier.testTag("Prefix")) },
                suffix = { Text(text = "Suffix", modifier = Modifier.testTag("Suffix")) }
            )
        }

        rule.onNodeWithTag("Placeholder", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("Prefix", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("Suffix", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun testOutlinedTextField_placeholderPosition_withLabel() {
        val placeholderSize = MinTextLineHeight
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedTextField(
                    modifier = Modifier.testTag(TextFieldTag),
                    state = rememberTextFieldState(),
                    label = { Box(Modifier.size(MinFocusedLabelLineHeight)) },
                    placeholder = {
                        Box(
                            Modifier.size(placeholderSize).onGloballyPositioned {
                                placeholderPosition.value = it.positionInRoot()
                            }
                        )
                    }
                )
            }
        }
        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            assertThat(placeholderPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            assertThat(placeholderPosition.value?.y)
                .isWithin(1f)
                .of((ExpectedPadding + OutlinedTextFieldTopPadding.toDp()).toPx())
        }
    }

    @Test
    fun testOutlinedTextField_placeholderPosition_whenNoLabel() {
        val placeholderSize = MinTextLineHeight
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                modifier = Modifier.testTag(TextFieldTag),
                state = rememberTextFieldState(),
                placeholder = {
                    Box(
                        Modifier.size(placeholderSize).onGloballyPositioned {
                            placeholderPosition.value = it.positionInRoot()
                        }
                    )
                }
            )
        }
        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            assertThat(placeholderPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            assertThat(placeholderPosition.value?.y).isWithin(1f).of(ExpectedPadding.toPx())
        }
    }

    @Test
    fun testOutlinedTextField_noPlaceholder_whenInputNotEmpty() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                modifier = Modifier.testTag(TextFieldTag),
                state = rememberTextFieldState("input"),
                placeholder = {
                    Text(
                        text = "placeholder",
                        modifier =
                            Modifier.onGloballyPositioned {
                                placeholderPosition.value = it.positionInRoot()
                                placeholderSize.value = it.size
                            }
                    )
                }
            )
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            assertThat(placeholderSize.value).isNull()
            assertThat(placeholderPosition.value).isNull()
        }
    }

    @Test
    fun testOutlinedTextField_placeholderColorAndTextStyle() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                modifier = Modifier.testTag(TextFieldTag),
                state = rememberTextFieldState(),
                placeholder = {
                    Text("placeholder")
                    assertThat(LocalTextStyle.current).isEqualTo(MaterialTheme.typography.bodyLarge)
                }
            )
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()
    }

    @Test
    fun testOutlinedTextField_placeholderColor_whenInputEmptyAndFocused() {
        var focused = false
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                modifier = Modifier.testTag(TextFieldTag),
                state = rememberTextFieldState(),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedPlaceholderColor = Color.Red,
                        unfocusedPlaceholderColor = Color.Green,
                    ),
                placeholder = {
                    Text("Placeholder")
                    assertThat(LocalContentColor.current)
                        .isEqualTo(if (focused) Color.Red else Color.Green)
                },
            )
        }

        // click to focus
        focused = true
        rule.onNodeWithTag(TextFieldTag).performClick()

        // enter some text (placeholder hidden)
        rule.onNodeWithTag(TextFieldTag).performTextInput("input")
        rule.runOnIdle {}

        // delete the text (placeholder shown)
        rule.onNodeWithTag(TextFieldTag).performTextClearance()
        rule.runOnIdle {}
    }

    @Test
    fun testOutlinedTextField_labelAndPlaceholderPosition_whenSmallerThanMinimumHeight() {
        val labelSize = 10.dp
        val labelPosition = Ref<Offset>()
        val placeholderSize = 20.dp
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                modifier = Modifier.testTag(TextFieldTag),
                state = rememberTextFieldState(),
                label = {
                    Box(
                        Modifier.size(labelSize).onGloballyPositioned {
                            labelPosition.value = it.positionInRoot()
                        }
                    )
                },
                placeholder = {
                    Box(
                        Modifier.size(placeholderSize).onGloballyPositioned {
                            placeholderPosition.value = it.positionInRoot()
                        }
                    )
                }
            )
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            // size
            assertThat(labelSize).isLessThan(MinFocusedLabelLineHeight)
            assertThat(placeholderSize).isLessThan(MinTextLineHeight)

            // label position
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            assertThat(labelPosition.value?.y)
                .isWithin(1f)
                .of(getLabelPosition(labelSize.roundToPx()).toFloat())

            // placeholder position
            assertThat(placeholderPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            // placeholder y position is top + default padding + label padding allowance, then
            // centered within allocated space
            assertThat(placeholderPosition.value?.y)
                .isWithin(1f)
                .of(
                    (ExpectedPadding +
                            OutlinedTextFieldTopPadding.toDp() +
                            (MinTextLineHeight - placeholderSize) / 2)
                        .toPx()
                )
        }
    }

    @Test
    fun testOutlinedTextField_trailingAndLeading_sizeAndPosition_defaultIcon() {
        val textFieldWidth = 300.dp
        val leadingPosition = Ref<Offset>()
        val leadingSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()
        val density = Density(2f)
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                OutlinedTextField(
                    state = rememberTextFieldState("text"),
                    modifier = Modifier.width(textFieldWidth),
                    label = { Text("label") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Favorite,
                            null,
                            Modifier.onGloballyPositioned {
                                leadingPosition.value = it.positionInRoot()
                                leadingSize.value = it.size
                            }
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Favorite,
                            null,
                            Modifier.onGloballyPositioned {
                                trailingPosition.value = it.positionInRoot()
                                trailingSize.value = it.size
                            }
                        )
                    },
                )
            }
        }

        rule.runOnIdle {
            with(density) {
                val minimumHeight = ExpectedMinimumTextFieldHeight.roundToPx()
                val size = 24.dp // default icon size
                // leading
                assertThat(leadingSize.value).isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(leadingPosition.value?.x).isEqualTo(IconPadding.roundToPx().toFloat())
                assertThat(leadingPosition.value?.y)
                    .isEqualTo(
                        ((minimumHeight - leadingSize.value!!.height) / 2f).roundToInt() +
                            8.dp.roundToPx()
                    )
                // trailing
                assertThat(trailingSize.value)
                    .isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(trailingPosition.value?.x)
                    .isEqualTo(
                        (textFieldWidth.roundToPx() -
                                IconPadding.roundToPx() -
                                trailingSize.value!!.width)
                            .toFloat()
                    )
                assertThat(trailingPosition.value?.y)
                    .isEqualTo(
                        ((minimumHeight - trailingSize.value!!.height) / 2f).roundToInt() +
                            8.dp.roundToPx()
                    )
            }
        }
    }

    @Test
    fun testOutlinedTextField_trailingAndLeading_sizeAndPosition_defaultIconButton() {
        val textFieldWidth = 300.dp
        val textFieldHeight = 80.dp
        val density = Density(2f)

        var leadingPosition: Offset? = null
        var leadingSize: IntSize? = null
        var trailingPosition: Offset? = null
        var trailingSize: IntSize? = null

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                OutlinedTextField(
                    state = rememberTextFieldState("text"),
                    modifier = Modifier.width(textFieldWidth).height(textFieldHeight),
                    leadingIcon = {
                        IconButton(
                            onClick = {},
                            modifier =
                                Modifier.onGloballyPositioned {
                                    leadingPosition = it.positionInRoot()
                                    leadingSize = it.size
                                }
                        ) {
                            Icon(Icons.Default.Favorite, null)
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {},
                            modifier =
                                Modifier.onGloballyPositioned {
                                    trailingPosition = it.positionInRoot()
                                    trailingSize = it.size
                                }
                        ) {
                            Icon(Icons.Default.Favorite, null)
                        }
                    }
                )
            }
        }

        rule.runOnIdle {
            val size = 48.dp // default IconButton size

            with(density) {
                // leading
                assertThat(leadingSize).isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(leadingPosition?.x).isEqualTo(0f)
                assertThat(leadingPosition?.y)
                    .isEqualTo(
                        ((textFieldHeight.roundToPx() - leadingSize!!.height) / 2f).roundToInt()
                    )
                // trailing
                assertThat(trailingSize).isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(trailingPosition?.x)
                    .isEqualTo((textFieldWidth.roundToPx() - trailingSize!!.width).toFloat())
                assertThat(trailingPosition?.y)
                    .isEqualTo(
                        ((textFieldHeight.roundToPx() - trailingSize!!.height) / 2f).roundToInt()
                    )
            }
        }
    }

    @Test
    fun testOutlinedTextField_trailingAndLeading_sizeAndPosition_nonDefaultSizeIcon() {
        val textFieldWidth = 300.dp
        val textFieldHeight = 80.dp
        val size = 72.dp
        val density = Density(2f)

        var leadingPosition: Offset? = null
        var leadingSize: IntSize? = null
        var trailingPosition: Offset? = null
        var trailingSize: IntSize? = null

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                OutlinedTextField(
                    state = rememberTextFieldState("text"),
                    modifier = Modifier.width(textFieldWidth).height(textFieldHeight),
                    leadingIcon = {
                        Box(
                            Modifier.size(size).onGloballyPositioned {
                                leadingPosition = it.positionInRoot()
                                leadingSize = it.size
                            }
                        )
                    },
                    trailingIcon = {
                        Box(
                            Modifier.size(size).onGloballyPositioned {
                                trailingPosition = it.positionInRoot()
                                trailingSize = it.size
                            }
                        )
                    },
                )
            }
        }

        rule.runOnIdle {
            with(density) {
                // leading
                assertThat(leadingSize).isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(leadingPosition?.x).isEqualTo(0f)
                assertThat(leadingPosition?.y)
                    .isEqualTo(
                        ((textFieldHeight.roundToPx() - leadingSize!!.height) / 2f).roundToInt()
                    )
                // trailing
                assertThat(trailingSize).isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(trailingPosition?.x)
                    .isEqualTo((textFieldWidth.roundToPx() - trailingSize!!.width).toFloat())
                assertThat(trailingPosition?.y)
                    .isEqualTo(
                        ((textFieldHeight.roundToPx() - trailingSize!!.height) / 2f).roundToInt()
                    )
            }
        }
    }

    @Test
    fun testOutlinedTextField_prefixAndSuffixAndPlaceholder_areNotDisplayed_withLabel_ifLabelCanExpand() {
        val labelText = "Label"
        val prefixText = "Prefix"
        val suffixText = "Suffix"
        val placeholderText = "Placeholder"
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = { Text(labelText) },
                prefix = { Text(prefixText) },
                suffix = { Text(suffixText) },
                placeholder = { Text(placeholderText) },
                labelPosition = TextFieldLabelPosition.Default(alwaysMinimize = false),
            )
        }

        rule.onNodeWithText(labelText).assertIsDisplayed()

        rule.onNodeWithText(prefixText).assertIsNotDisplayed()
        rule.onNodeWithText(suffixText).assertIsNotDisplayed()
        rule.onNodeWithText(placeholderText).assertIsNotDisplayed()
    }

    @Test
    fun testOutlinedTextField_prefixAndSuffixAndPlaceholder_areDisplayed_withLabel_ifLabelCannotExpand() {
        val labelText = "Label"
        val prefixText = "Prefix"
        val suffixText = "Suffix"
        val placeholderText = "Placeholder"
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = { Text(labelText) },
                prefix = { Text(prefixText) },
                suffix = { Text(suffixText) },
                placeholder = { Text(placeholderText) },
                labelPosition = TextFieldLabelPosition.Default(alwaysMinimize = true),
            )
        }

        rule.onNodeWithText(labelText).assertIsDisplayed()
        rule.onNodeWithText(prefixText).assertIsDisplayed()
        rule.onNodeWithText(suffixText).assertIsDisplayed()
        rule.onNodeWithText(placeholderText).assertIsDisplayed()
    }

    @Test
    fun testOutlinedTextField_prefixAndSuffixAndPlaceholder_areDisplayed_withLabel_ifLabelIsAbove() {
        val labelText = "Label"
        val prefixText = "Prefix"
        val suffixText = "Suffix"
        val placeholderText = "Placeholder"
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = { Text(labelText) },
                prefix = { Text(prefixText) },
                suffix = { Text(suffixText) },
                placeholder = { Text(placeholderText) },
                labelPosition = TextFieldLabelPosition.Above(),
            )
        }

        rule.onNodeWithText(labelText).assertIsDisplayed()
        rule.onNodeWithText(prefixText).assertIsDisplayed()
        rule.onNodeWithText(suffixText).assertIsDisplayed()
        rule.onNodeWithText(placeholderText).assertIsDisplayed()
    }

    @Test
    fun testOutlinedTextField_prefixAndSuffixPosition_withLabel() {
        val textFieldWidth = 300.dp
        val prefixPosition = Ref<Offset>()
        val prefixSize = MinTextLineHeight
        val suffixPosition = Ref<Offset>()
        val suffixSize = MinTextLineHeight
        val density = Density(2f)

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                OutlinedTextField(
                    state = rememberTextFieldState("text"),
                    modifier = Modifier.width(textFieldWidth),
                    label = { Box(Modifier.size(MinFocusedLabelLineHeight)) },
                    prefix = {
                        Box(
                            Modifier.size(prefixSize).onGloballyPositioned {
                                prefixPosition.value = it.positionInRoot()
                            }
                        )
                    },
                    suffix = {
                        Box(
                            Modifier.size(suffixSize).onGloballyPositioned {
                                suffixPosition.value = it.positionInRoot()
                            }
                        )
                    }
                )
            }
        }

        rule.runOnIdle {
            with(density) {
                // prefix
                assertThat(prefixPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
                assertThat(prefixPosition.value?.y)
                    .isWithin(1f)
                    .of((ExpectedPadding + OutlinedTextFieldTopPadding.toDp()).toPx())

                // suffix
                assertThat(suffixPosition.value?.x)
                    .isWithin(1f)
                    .of((textFieldWidth - ExpectedPadding - suffixSize).toPx())
                assertThat(suffixPosition.value?.y)
                    .isWithin(1f)
                    .of((ExpectedPadding + OutlinedTextFieldTopPadding.toDp()).toPx())
            }
        }
    }

    @Test
    fun testOutlinedTextField_prefixAndSuffixPosition_whenNoLabel() {
        val textFieldWidth = 300.dp
        val prefixPosition = Ref<Offset>()
        val prefixSize = MinTextLineHeight
        val suffixPosition = Ref<Offset>()
        val suffixSize = MinTextLineHeight
        val density = Density(2f)

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                OutlinedTextField(
                    state = rememberTextFieldState("text"),
                    modifier = Modifier.width(textFieldWidth),
                    prefix = {
                        Box(
                            Modifier.size(prefixSize).onGloballyPositioned {
                                prefixPosition.value = it.positionInRoot()
                            }
                        )
                    },
                    suffix = {
                        Box(
                            Modifier.size(suffixSize).onGloballyPositioned {
                                suffixPosition.value = it.positionInRoot()
                            }
                        )
                    }
                )
            }
        }

        rule.runOnIdle {
            with(density) {
                // prefix
                assertThat(prefixPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
                assertThat(prefixPosition.value?.y).isWithin(1f).of(ExpectedPadding.toPx())

                // suffix
                assertThat(suffixPosition.value?.x)
                    .isWithin(1f)
                    .of((textFieldWidth - ExpectedPadding - suffixSize).toPx())
                assertThat(suffixPosition.value?.y).isWithin(1f).of(ExpectedPadding.toPx())
            }
        }
    }

    @Test
    fun testOutlinedTextField_prefixAndSuffixPosition_withIcons() {
        val textFieldWidth = 300.dp
        val prefixPosition = Ref<Offset>()
        val prefixSize = MinTextLineHeight
        val suffixPosition = Ref<Offset>()
        val suffixSize = MinTextLineHeight
        val density = Density(2f)

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                OutlinedTextField(
                    state = rememberTextFieldState("text"),
                    modifier = Modifier.width(textFieldWidth),
                    prefix = {
                        Box(
                            Modifier.size(prefixSize).onGloballyPositioned {
                                prefixPosition.value = it.positionInRoot()
                            }
                        )
                    },
                    suffix = {
                        Box(
                            Modifier.size(suffixSize).onGloballyPositioned {
                                suffixPosition.value = it.positionInRoot()
                            }
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Favorite, null) },
                    trailingIcon = { Icon(Icons.Default.Favorite, null) },
                )
            }
        }

        rule.runOnIdle {
            with(density) {
                val iconSize = 24.dp // default icon size

                // prefix
                assertThat(prefixPosition.value?.x)
                    .isWithin(1f)
                    .of((ExpectedPadding + IconPadding + iconSize).toPx())
                assertThat(prefixPosition.value?.y).isWithin(1f).of(ExpectedPadding.toPx())

                // suffix
                assertThat(suffixPosition.value?.x)
                    .isWithin(1f)
                    .of(
                        (textFieldWidth - IconPadding - iconSize - ExpectedPadding - suffixSize)
                            .toPx()
                    )
                assertThat(suffixPosition.value?.y).isWithin(1f).of(ExpectedPadding.toPx())
            }
        }
    }

    @Test
    fun testOutlinedTextField_labelPositionX_initial_withTrailingAndLeading() {
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = {
                    Text(
                        text = "label",
                        modifier =
                            Modifier.onGloballyPositioned {
                                labelPosition.value = it.positionInRoot()
                            }
                    )
                },
                trailingIcon = { Icon(Icons.Default.Favorite, null) },
                leadingIcon = { Icon(Icons.Default.Favorite, null) }
            )
        }

        rule.runOnIdleWithDensity {
            val iconSize = 24.dp // default icon size
            assertThat(labelPosition.value?.x)
                .isWithin(1f)
                .of((ExpectedPadding + IconPadding + iconSize).toPx())
        }
    }

    @Test
    fun testOutlinedTextField_labelPositionX_initial_withNullTrailingAndLeading() {
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = {
                    Text(
                        text = "label",
                        modifier =
                            Modifier.onGloballyPositioned {
                                labelPosition.value = it.positionInRoot()
                            }
                    )
                },
                trailingIcon = null,
                leadingIcon = null
            )
        }

        rule.runOnIdleWithDensity {
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
        }
    }

    @Test
    fun testOutlinedTextField_colorInLeadingTrailing_whenValidInput() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                isError = false,
                leadingIcon = {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )
        }
    }

    @Test
    fun testOutlinedTextField_colorInLeadingTrailing_whenInvalidInput() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                isError = true,
                leadingIcon = {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    assertThat(LocalContentColor.current).isEqualTo(MaterialTheme.colorScheme.error)
                }
            )
        }
    }

    @Test
    fun testOutlinedTextField_supportingText_position() {
        val supportingPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                textStyle = TextStyle(fontSize = 1.sp), // ensure text size is minimum
                supportingText = {
                    Box(
                        Modifier.size(MinSupportingTextLineHeight).onGloballyPositioned {
                            supportingPosition.value = it.positionInRoot()
                        }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            assertThat(supportingPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            assertThat(supportingPosition.value?.y)
                .isWithin(1f)
                .of((ExpectedMinimumTextFieldHeight + SupportingTopPadding).toPx())
        }
    }

    @Test
    fun testOutlinedTextField_supportingText_widthIsNotWiderThanTextField() {
        val tfSize = Ref<IntSize>()
        val supportingSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.onGloballyPositioned { tfSize.value = it.size },
                supportingText = {
                    Text(
                        text =
                            "Long long long long long long long long long long long long " +
                                "long long long long long long long long long long long long",
                        modifier = Modifier.onGloballyPositioned { supportingSize.value = it.size }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            assertThat(supportingSize.value!!.width).isAtMost(tfSize.value!!.width)
        }
    }

    @Test
    fun testOutlinedTextField_supportingText_contributesToTextFieldMeasurements() {
        val tfSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.onGloballyPositioned { tfSize.value = it.size },
                supportingText = { Text("Supporting") }
            )
        }

        rule.runOnIdleWithDensity {
            assertThat(tfSize.value!!.height)
                .isGreaterThan(ExpectedMinimumTextFieldHeight.roundToPx())
        }
    }

    @Test
    fun testOutlinedTextField_supportingText_remainsVisibleWithTallInput() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state =
                    rememberTextFieldState(buildString { repeat(200) { append("line $it\n") } }),
                modifier = Modifier.size(width = ExpectedDefaultTextFieldWidth, height = 150.dp),
                supportingText = { Text("Supporting", modifier = Modifier.testTag("Supporting")) }
            )
        }

        rule.onNodeWithTag("Supporting", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testOutlinedTextField_supportingText_clickFocusesTextField() {
        var focused = false
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                modifier = Modifier.onFocusChanged { focused = it.isFocused },
                state = rememberTextFieldState("input"),
                supportingText = { Text("Supporting") }
            )
        }

        rule.onNodeWithText("Supporting").performClick()
        rule.runOnIdle { assertThat(focused).isTrue() }
    }

    @Test
    fun testOutlinedTextField_supportingText_colorAndStyle() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                supportingText = {
                    assertThat(LocalTextStyle.current).isEqualTo(MaterialTheme.typography.bodySmall)
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )
        }
    }

    @Test
    fun testOutlinedTextField_supportingText_error_colorAndStyle() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                isError = true,
                supportingText = {
                    assertThat(LocalTextStyle.current).isEqualTo(MaterialTheme.typography.bodySmall)
                    assertThat(LocalContentColor.current).isEqualTo(MaterialTheme.colorScheme.error)
                }
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun testOutlinedTextField_imeActionAndKeyboardTypePropagatedDownstream() {
        var editorInfo: EditorInfo? = null
        val interceptor = PlatformTextInputInterceptor { request, _ ->
            EditorInfo().also {
                request.createInputConnection(it)
                editorInfo = it
            }
            awaitCancellation()
        }
        rule.setContent {
            InterceptPlatformTextInput(interceptor) {
                OutlinedTextField(
                    modifier = Modifier.testTag(TextFieldTag),
                    state = rememberTextFieldState(),
                    keyboardOptions =
                        KeyboardOptions(imeAction = ImeAction.Go, keyboardType = KeyboardType.Email)
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdle {
            @Suppress("NAME_SHADOWING")
            val editorInfo = editorInfo ?: throw AssertionError("Input session never started")
            val expectedOptions = IME_ACTION_GO
            val expectedInputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            assertThat(editorInfo.imeOptions and expectedOptions).isEqualTo(expectedOptions)
            assertThat(editorInfo.inputType and expectedInputType).isEqualTo(expectedInputType)
        }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testOutlinedTextField_visualTransformationPropagated() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.background(color = Color.White)) {
                OutlinedTextField(
                    modifier = Modifier.testTag(TextFieldTag),
                    state = rememberTextFieldState("qwerty"),
                    outputTransformation = {
                        // transform all chars to blank spaces
                        val size = length
                        delete(0, length)
                        insert(0, " ".repeat(size))
                        placeCursorAtEnd()
                    }
                )
            }
        }

        rule
            .onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.White,
                shapeColor = Color.White,
                shape = RectangleShape,
                // avoid elevation artifacts
                antiAliasingGap = with(rule.density) { 3.dp.toPx() }
            )
    }

    @Test
    fun testOutlinedTextField_errorSemantics_defaultMessage() {
        lateinit var errorMessage: String
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(state = rememberTextFieldState("test"), isError = true)
            errorMessage = getString(Strings.DefaultErrorMessage)
        }

        rule
            .onNodeWithText("test")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, errorMessage))
    }

    @Test
    fun testOutlinedTextField_errorSemantics_messageOverridable() {
        val errorMessage = "Special symbols not allowed"
        lateinit var defaultErrorMessage: String
        rule.setMaterialContent(lightColorScheme()) {
            val isError = remember { mutableStateOf(true) }
            OutlinedTextField(
                state = rememberTextFieldState("test"),
                modifier =
                    Modifier.testTag(TextFieldTag).semantics {
                        if (isError.value) error(errorMessage)
                    },
                isError = isError.value
            )
            defaultErrorMessage = getString(Strings.DefaultErrorMessage)
        }

        rule
            .onNodeWithTag(TextFieldTag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, errorMessage))

        // Check that default error message is overwritten and not lingering in a child node
        rule
            .onNodeWithTag(TextFieldTag, useUnmergedTree = true)
            .onChildren()
            .fetchSemanticsNodes()
            .forEach { node ->
                assertThat(node.config.getOrNull(SemanticsProperties.Error))
                    .isNotEqualTo(defaultErrorMessage)
            }
    }

    @Test
    fun testOutlinedTextField_withLabel_doesNotCrash_rowHeightWithMinIntrinsics() {
        var size: IntSize? = null
        var dividerSize: IntSize? = null
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.onGloballyPositioned { size = it.size }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    VerticalDivider(
                        thickness = 10.dp,
                        modifier = Modifier.onGloballyPositioned { dividerSize = it.size }
                    )
                    OutlinedTextField(
                        state = rememberTextFieldState(),
                        label = { Text(text = "Label") },
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(dividerSize).isNotNull()
            assertThat(size).isNotNull()
            assertThat(dividerSize!!.height).isEqualTo(size!!.height)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testOutlinedTextField_appliesContainerColor() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(TextFieldTag),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Red,
                        unfocusedBorderColor = Color.Red
                    ),
                shape = RectangleShape
            )
        }

        rule.onNodeWithTag(TextFieldTag).captureToImage().assertPixels { Color.Red }
    }

    @Test
    fun testOutlinedTextField_withLabel_doesNotCrash_columnWidthWithMinIntrinsics() {
        var textFieldSize: IntSize? = null
        var dividerSize: IntSize? = null
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                Column(Modifier.width(IntrinsicSize.Min)) {
                    HorizontalDivider(
                        thickness = 10.dp,
                        modifier = Modifier.onGloballyPositioned { dividerSize = it.size }
                    )
                    OutlinedTextField(
                        state = rememberTextFieldState(),
                        label = { Text(text = "Label") },
                        modifier = Modifier.onGloballyPositioned { textFieldSize = it.size }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(dividerSize).isNotNull()
            assertThat(textFieldSize).isNotNull()
            assertThat(dividerSize!!.width).isEqualTo(textFieldSize!!.width)
        }
    }

    @Test
    fun testOutlinedTextField_labelStyle() {
        val unfocusedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified

        val focusRequester = FocusRequester()

        rule.setMaterialContent(lightColorScheme()) {
            OutlinedTextField(
                state = rememberTextFieldState(),
                label = {
                    textStyle = LocalTextStyle.current
                    contentColor = LocalContentColor.current
                },
                modifier = Modifier.focusRequester(focusRequester),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        unfocusedLabelColor = unfocusedLabelColor,
                        focusedLabelColor = focusedLabelColor
                    )
            )
        }

        rule.runOnIdle {
            assertThat(contentColor).isEqualTo(unfocusedLabelColor)
            assertThat(textStyle.color).isEqualTo(Color.Unspecified)
        }

        rule.runOnUiThread { focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(contentColor).isEqualTo(focusedLabelColor)
            assertThat(textStyle.color).isEqualTo(Color.Unspecified)
        }
    }

    @Test
    fun testOutlinedTextField_labelStyle_whenBodySmallStyleColorProvided() {
        val unfocusedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        val bodySmallColor = Color.Green
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified

        val focusRequester = FocusRequester()

        rule.setMaterialContent(lightColorScheme()) {
            val bodySmall = MaterialTheme.typography.bodySmall.copy(color = bodySmallColor)
            MaterialTheme(typography = Typography(bodySmall = bodySmall)) {
                OutlinedTextField(
                    state = rememberTextFieldState(),
                    label = {
                        textStyle = LocalTextStyle.current
                        contentColor = LocalContentColor.current
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            unfocusedLabelColor = unfocusedLabelColor,
                            focusedLabelColor = focusedLabelColor
                        )
                )
            }
        }

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(unfocusedLabelColor)
            assertThat(contentColor).isEqualTo(unfocusedLabelColor)
        }

        rule.runOnUiThread { focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(bodySmallColor)
            assertThat(contentColor).isEqualTo(focusedLabelColor)
        }
    }

    @Test
    fun testOutlinedTextField_labelStyle_middle_whenBodySmallStyleColorProvided() {
        val expectedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified
        val focusRequester = FocusRequester()

        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            val bodySmall = MaterialTheme.typography.bodySmall.copy(color = expectedLabelColor)
            MaterialTheme(typography = Typography(bodySmall = bodySmall)) {
                OutlinedTextField(
                    state = rememberTextFieldState(),
                    label = {
                        textStyle = LocalTextStyle.current
                        contentColor = LocalContentColor.current
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            unfocusedLabelColor = expectedLabelColor,
                            focusedLabelColor = focusedLabelColor
                        )
                )
            }
        }

        rule.runOnUiThread { focusRequester.requestFocus() }

        // advance to middle of animation
        rule.mainClock.advanceTimeBy(TextFieldAnimationDuration)

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(expectedLabelColor)
            // color should be a lerp between 'start' and 'end' colors. We check here that it's
            // not equal to either of them
            assertThat(contentColor).isNotEqualTo(expectedLabelColor)
            assertThat(contentColor).isNotEqualTo(focusedLabelColor)
        }
    }

    @Test
    fun testOutlinedTextField_labelStyle_whenBothTypographiesColorProvided() {
        val unfocusedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        val bodySmallColor = Color.Green
        val bodyLargeColor = Color.Black
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified
        val focusRequester = FocusRequester()

        rule.setMaterialContent(lightColorScheme()) {
            val bodySmall = MaterialTheme.typography.bodySmall.copy(color = bodySmallColor)
            val bodyLarge = MaterialTheme.typography.bodyLarge.copy(color = bodyLargeColor)
            MaterialTheme(typography = Typography(bodySmall = bodySmall, bodyLarge = bodyLarge)) {
                OutlinedTextField(
                    state = rememberTextFieldState(),
                    label = {
                        textStyle = LocalTextStyle.current
                        contentColor = LocalContentColor.current
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            unfocusedLabelColor = unfocusedLabelColor,
                            focusedLabelColor = focusedLabelColor
                        )
                )
            }
        }

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(bodyLargeColor)
            assertThat(contentColor).isEqualTo(unfocusedLabelColor)
        }

        rule.runOnUiThread { focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(bodySmallColor)
            assertThat(contentColor).isEqualTo(focusedLabelColor)
        }
    }

    @Test
    fun testOutlinedTextField_selectionColors_areCustomizable() {
        rule.setMaterialContent(lightColorScheme()) {
            Column {
                // default colors
                OutlinedTextField(
                    state = rememberTextFieldState(),
                    label = {
                        val textSelectionColors = LocalTextSelectionColors.current
                        assertThat(textSelectionColors.handleColor)
                            .isEqualTo(MaterialTheme.colorScheme.primary)
                        assertThat(textSelectionColors.backgroundColor)
                            .isEqualTo(
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = TextSelectionBackgroundOpacity
                                )
                            )
                    }
                )

                // set via `colors()`
                OutlinedTextField(
                    state = rememberTextFieldState(),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            selectionColors =
                                TextSelectionColors(
                                    handleColor = Color.Red,
                                    backgroundColor = Color.Green
                                )
                        ),
                    label = {
                        val textSelectionColors = LocalTextSelectionColors.current
                        assertThat(textSelectionColors.handleColor).isEqualTo(Color.Red)
                        assertThat(textSelectionColors.backgroundColor).isEqualTo(Color.Green)
                    }
                )

                // set via `CompositionLocal`
                CompositionLocalProvider(
                    LocalTextSelectionColors provides
                        TextSelectionColors(
                            handleColor = Color.Magenta,
                            backgroundColor = Color.Yellow
                        )
                ) {
                    OutlinedTextField(
                        state = rememberTextFieldState(),
                        label = {
                            val textSelectionColors = LocalTextSelectionColors.current
                            assertThat(textSelectionColors.handleColor).isEqualTo(Color.Magenta)
                            assertThat(textSelectionColors.backgroundColor).isEqualTo(Color.Yellow)
                        }
                    )
                }
            }
        }
    }

    @Test
    fun testOutlinedTextField_withIntrinsicsMeasurement_getsIdle() {
        rule.setMaterialContent(lightColorScheme()) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                OutlinedTextField(state = rememberTextFieldState(), label = { Text("Label") })
                VerticalDivider()
            }
        }

        rule.onNodeWithText("Label").assertExists().assertIsDisplayed().performTextInput("text")

        rule.onNodeWithText("text").assertExists()
    }

    @Test
    fun testOutlinedTextField_intrinsicHeight_withOnlyEmptyInput() {
        var height = 0
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.onGloballyPositioned { height = it.size.height }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    OutlinedTextField(rememberTextFieldState())
                    VerticalDivider()
                }
            }
        }

        with(rule.density) {
            assertThat(height).isEqualTo((OutlinedTextFieldDefaults.MinHeight).roundToPx())
        }
    }

    @Test
    fun testOutlinedTextField_intrinsicHeight_withEmptyInput_andDecorations() {
        var height = 0
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.onGloballyPositioned { height = it.size.height }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    OutlinedTextField(
                        state = rememberTextFieldState(),
                        leadingIcon = { Icon(Icons.Default.Favorite, null) },
                        trailingIcon = { Icon(Icons.Default.Favorite, null) },
                        prefix = { Text("P") },
                        suffix = { Text("S") },
                    )
                    VerticalDivider()
                }
            }
        }

        with(rule.density) {
            assertThat(height).isEqualTo((OutlinedTextFieldDefaults.MinHeight).roundToPx())
        }
    }

    @Test
    fun testOutlinedTextField_intrinsicHeight_withLongInput_andDecorations() {
        var tfHeightIntrinsic = 0
        var tfHeightNoIntrinsic = 0
        val text = "Long text input. ".repeat(20)
        rule.setMaterialContent(lightColorScheme()) {
            Row {
                Box(Modifier.width(150.dp).height(IntrinsicSize.Min)) {
                    OutlinedTextField(
                        state = rememberTextFieldState(text),
                        modifier =
                            Modifier.onGloballyPositioned { tfHeightIntrinsic = it.size.height },
                        leadingIcon = { Icon(Icons.Default.Favorite, null) },
                        trailingIcon = { Icon(Icons.Default.Favorite, null) },
                        prefix = { Text("P") },
                        suffix = { Text("S") },
                    )
                }

                Box(Modifier.width(150.dp)) {
                    OutlinedTextField(
                        state = rememberTextFieldState(text),
                        modifier =
                            Modifier.onGloballyPositioned { tfHeightNoIntrinsic = it.size.height },
                        leadingIcon = { Icon(Icons.Default.Favorite, null) },
                        trailingIcon = { Icon(Icons.Default.Favorite, null) },
                        prefix = { Text("P") },
                        suffix = { Text("S") },
                    )
                }
            }
        }

        assertThat(tfHeightIntrinsic).isNotEqualTo(0)
        assertThat(tfHeightNoIntrinsic).isNotEqualTo(0)

        assertThat(tfHeightIntrinsic).isEqualTo(tfHeightNoIntrinsic)
    }

    @Test
    fun outlinedTextField_stringOverload_doesNotCallOnValueChange_whenCompositionUpdatesOnly() {
        var callbackCounter = 0

        rule.setContent {
            val focusManager = LocalFocusManager.current
            val text = remember { mutableStateOf("A") }

            OutlinedTextField(
                value = text.value,
                onValueChange = {
                    callbackCounter += 1
                    text.value = it

                    // causes TextFieldValue's composition clearing
                    focusManager.clearFocus(true)
                },
                modifier = Modifier.testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(TextFieldTag).performTextClearance()

        rule.runOnIdle { assertThat(callbackCounter).isEqualTo(1) }
    }

    @Test
    fun testTextFields_noCrashConstraintsInfinity() {

        rule.setMaterialContent(lightColorScheme()) {
            Column(
                modifier =
                    Modifier.height(IntrinsicSize.Min).horizontalScroll(rememberScrollState())
            ) {
                OutlinedTextField(state = rememberTextFieldState(), leadingIcon = { Text("Icon") })
            }
        }
    }

    private fun getLabelPosition(labelHeight: Int): Int {
        val labelHalfHeight = labelHeight / 2
        val paddingTop = with(rule.density) { OutlinedTextFieldTopPadding.toPx() }
        // Vertical position is the default padding - half height.
        // This can be negative, meaning default padding is not enough for the focused label.
        return (paddingTop - labelHalfHeight).roundToInt()
    }
}

// We use springs to animate, so picking an arbitrary duration that work.
private const val TextFieldAnimationDuration = 75L
