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

@file:Suppress("DEPRECATION")

package androidx.compose.material3

import android.content.Context
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.internal.HorizontalIconPadding
import androidx.compose.material3.internal.MinFocusedLabelLineHeight
import androidx.compose.material3.internal.MinSupportingTextLineHeight
import androidx.compose.material3.internal.MinTextLineHeight
import androidx.compose.material3.internal.Strings.Companion.DefaultErrorMessage
import androidx.compose.material3.internal.SupportingTopPadding
import androidx.compose.material3.internal.TextFieldAnimationDuration
import androidx.compose.material3.internal.TextFieldPadding
import androidx.compose.material3.internal.getString
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldTest {
    private val ExpectedDefaultTextFieldHeight = TextFieldDefaults.MinHeight
    private val ExpectedDefaultTextFieldWidth = TextFieldDefaults.MinWidth
    private val ExpectedPadding = TextFieldPadding
    private val IconPadding = HorizontalIconPadding
    private val TextFieldWidth = 300.dp
    private val TextFieldTag = "textField"

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testTextField_setSmallHeight() {
        rule.setMaterialContentForSizeAssertions {
            TextField(
                value = "input",
                onValueChange = {},
                modifier = Modifier.height(20.dp)
            )
        }
            .assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun testTextField_setSmallWidth() {
        rule.setMaterialContentForSizeAssertions {
            TextField(
                value = "input",
                onValueChange = {},
                modifier = Modifier.requiredWidth(40.dp)
            )
        }
            .assertWidthIsEqualTo(40.dp)
    }

    @Test
    fun testTextField_defaultHeight() {
        rule.setMaterialContentForSizeAssertions {
            TextField(
                value = "",
                onValueChange = {},
                textStyle = TextStyle(fontSize = 1.sp), // ensure text size is minimum
            )
        }
            .assertHeightIsEqualTo(ExpectedDefaultTextFieldHeight)
    }

    @Test
    fun testTextField_defaultWidth() {
        rule.setMaterialContentForSizeAssertions {
            TextField(
                value = "input",
                onValueChange = {}
            )
        }
            .assertWidthIsEqualTo(ExpectedDefaultTextFieldWidth)
    }

    @Test
    fun testTextFields_singleFocus() {
        val textField1Tag = "TextField1"
        val textField2Tag = "TextField2"
        val interactionSource1 = MutableInteractionSource()
        val interactionSource2 = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setMaterialContent(lightColorScheme()) {
            scope = rememberCoroutineScope()
            Column {
                TextField(
                    modifier = Modifier.testTag(textField1Tag),
                    value = "input1",
                    onValueChange = {},
                    interactionSource = interactionSource1
                )
                TextField(
                    modifier = Modifier.testTag(textField2Tag),
                    value = "input2",
                    onValueChange = {},
                    interactionSource = interactionSource2
                )
            }
        }

        val interactions1 = mutableListOf<Interaction>()
        val interactions2 = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource1.interactions.collect { interactions1.add(it) }
        }
        scope!!.launch {
            interactionSource2.interactions.collect { interactions2.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions1).isEmpty()
            assertThat(interactions2).isEmpty()
        }

        rule.onNodeWithTag(textField1Tag).performClick()

        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions1.filterIsInstance<FocusInteraction.Focus>()).hasSize(1)
            assertThat(interactions2).isEmpty()
        }

        rule.onNodeWithTag(textField2Tag).performClick()

        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions1.filterIsInstance<FocusInteraction.Focus>()).hasSize(1)
            assertThat(interactions1.filterIsInstance<FocusInteraction.Unfocus>()).hasSize(1)
            assertThat(interactions2.filterIsInstance<FocusInteraction.Focus>()).hasSize(1)
            assertThat(interactions2.filterIsInstance<FocusInteraction.Unfocus>()).isEmpty()
        }
    }

    @Test
    fun testTextField_getFocus_whenClickedOnSurfaceArea() {
        val interactionSource = MutableInteractionSource()
        var scope: CoroutineScope? = null

        rule.setMaterialContent(lightColorScheme()) {
            scope = rememberCoroutineScope()
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                value = "input",
                onValueChange = {},
                interactionSource = interactionSource
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        // Click on (2, 2) which is Surface area and outside input area
        rule.onNodeWithTag(TextFieldTag).performTouchInput {
            click(Offset(2f, 2f))
        }

        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<FocusInteraction.Focus>()).hasSize(1)
        }
    }

    @Test
    fun testTextField_showHideKeyboardBasedOnFocus() {
        val (focusRequester, parentFocusRequester) = FocusRequester.createRefs()
        lateinit var hostView: View
        rule.setMaterialContent(lightColorScheme()) {
            hostView = LocalView.current
            Box {
                TextField(
                    modifier = Modifier
                        .focusRequester(parentFocusRequester)
                        .focusTarget()
                        .focusRequester(focusRequester)
                        .testTag(TextFieldTag),
                    value = "input",
                    onValueChange = {}
                )
            }
        }

        // Shows keyboard when the text field is focused.
        rule.runOnIdle { focusRequester.requestFocus() }
        rule.runOnIdle { assertThat(hostView.isSoftwareKeyboardShown).isTrue() }

        // Hides keyboard when the text field is not focused.
        rule.runOnIdle { parentFocusRequester.requestFocus() }
        rule.runOnIdle { assertThat(hostView.isSoftwareKeyboardShown).isFalse() }
    }

    @ExperimentalComposeUiApi
    @Test
    fun testTextField_clickingOnTextAfterDismissingKeyboard_showsKeyboard() {
        val (focusRequester, parentFocusRequester) = FocusRequester.createRefs()
        var softwareKeyboardController: SoftwareKeyboardController? = null
        lateinit var hostView: View
        rule.setMaterialContent(lightColorScheme()) {
            hostView = LocalView.current
            softwareKeyboardController = LocalSoftwareKeyboardController.current
            Box {
                TextField(
                    modifier = Modifier
                        .focusRequester(parentFocusRequester)
                        .focusTarget()
                        .focusRequester(focusRequester)
                        .testTag(TextFieldTag),
                    value = "input",
                    onValueChange = {}
                )
            }
        }

        // Shows keyboard when the text field is focused.
        rule.runOnIdle { focusRequester.requestFocus() }
        rule.runOnIdle { assertThat(hostView.isSoftwareKeyboardShown).isTrue() }

        // Hide keyboard.
        rule.runOnIdle { softwareKeyboardController?.hide() }

        // Clicking on the text field shows the keyboard.
        rule.onNodeWithTag(TextFieldTag).performClick()
        rule.runOnIdle { assertThat(hostView.isSoftwareKeyboardShown).isTrue() }
    }

    @Test
    fun testTextField_labelPosition_initial_singleLine() {
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                singleLine = true,
                label = {
                    Box(
                        Modifier
                            .size(MinTextLineHeight)
                            .onGloballyPositioned {
                                labelPosition.value = it.positionInRoot()
                            }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            // x position is start + padding
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            // y position is centered
            assertThat(labelPosition.value?.y).isWithin(1f).of(
                (ExpectedDefaultTextFieldHeight - MinTextLineHeight).toPx() / 2f
            )
        }
    }

    @Test
    fun testTextField_labelPosition_initial_withDefaultHeight() {
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                label = {
                    Box(
                        Modifier
                            .size(MinTextLineHeight)
                            .onGloballyPositioned {
                                labelPosition.value = it.positionInRoot()
                            }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            // x position is start + padding
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            // y position is top + padding
            assertThat(labelPosition.value?.y).isWithin(1f).of(ExpectedPadding.toPx())
        }
    }

    @Test
    fun testTextField_labelPosition_initial_withCustomHeight() {
        val height = 80.dp
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.height(height),
                label = {
                    Box(
                        Modifier
                            .size(MinTextLineHeight)
                            .onGloballyPositioned {
                                labelPosition.value = it.positionInRoot()
                            }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            // x position is start + padding
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            // y position is top + padding
            assertThat(labelPosition.value?.y).isWithin(1f).of(ExpectedPadding.toPx())
        }
    }

    @Test
    fun testTextField_labelPosition_whenFocused() {
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                value = "",
                onValueChange = {},
                label = {
                    Box(
                        Modifier
                            .size(MinFocusedLabelLineHeight)
                            .onGloballyPositioned {
                                labelPosition.value = it.positionInRoot()
                            }
                    )
                }
            )
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            // x position is start + padding
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            // y position is top + (different) padding
            assertThat(labelPosition.value?.y).isWithin(1f).of(
                TextFieldWithLabelVerticalPadding.toPx()
            )
        }
    }

    @Test
    fun testTextField_labelPosition_whenInput() {
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "input",
                onValueChange = {},
                label = {
                    Box(
                        Modifier
                            .size(MinFocusedLabelLineHeight)
                            .onGloballyPositioned {
                                labelPosition.value = it.positionInRoot()
                            }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            // x position is start + padding
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            // y position is top + (different) padding
            assertThat(labelPosition.value?.y).isWithin(1f).of(
                TextFieldWithLabelVerticalPadding.toPx()
            )
        }
    }

    @Test
    fun testTextField_placeholderPosition_withLabel() {
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                value = "",
                onValueChange = {},
                label = { Box(Modifier.size(MinFocusedLabelLineHeight)) },
                placeholder = {
                    Box(
                        Modifier
                            .size(MinTextLineHeight)
                            .onGloballyPositioned {
                                placeholderPosition.value = it.positionInRoot()
                            }
                    )
                }
            )
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            // x position is start + padding
            assertThat(placeholderPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            // y position is top + padding + label height
            assertThat(placeholderPosition.value?.y).isWithin(1f).of(
                (TextFieldWithLabelVerticalPadding + MinFocusedLabelLineHeight).toPx()
            )
        }
    }

    @Test
    fun testTextField_placeholderPosition_whenNoLabel() {
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                value = "",
                onValueChange = {},
                placeholder = {
                    Box(
                        Modifier
                            .size(MinTextLineHeight)
                            .onGloballyPositioned {
                                placeholderPosition.value = it.positionInRoot()
                            }
                    )
                }
            )
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            // x position is start + padding
            assertThat(placeholderPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            // y position is top + padding
            assertThat(placeholderPosition.value?.y).isWithin(1f).of(ExpectedPadding.toPx())
        }
    }

    @Test
    fun testTextField_noPlaceholder_whenInputNotEmpty() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                value = "input",
                onValueChange = {},
                placeholder = {
                    Text(
                        text = "placeholder",
                        modifier = Modifier.onGloballyPositioned {
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
    fun testTextField_placeholderColorAndTextStyle() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                value = "",
                onValueChange = {},
                placeholder = {
                    Text("placeholder")
                    assertThat(LocalTextStyle.current)
                        .isEqualTo(MaterialTheme.typography.bodyLarge)
                }
            )
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()
    }

    @Test
    fun testTextField_placeholderColor_whenInputEmptyAndFocused() {
        var focused = false
        rule.setMaterialContent(lightColorScheme()) {
            val text = remember { mutableStateOf("") }
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                value = text.value,
                onValueChange = { text.value = it },
                colors = TextFieldDefaults.colors(
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
    fun testTextField_labelAndPlaceholderPosition_whenSmallerThanMinimumHeight() {
        val labelSize = 10.dp
        val labelPosition = Ref<Offset>()
        val placeholderSize = 20.dp
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                value = "",
                onValueChange = {},
                label = {
                    Box(
                        Modifier
                            .size(labelSize)
                            .onGloballyPositioned {
                                labelPosition.value = it.positionInRoot()
                            }
                    )
                },
                placeholder = {
                    Box(
                        Modifier
                            .size(placeholderSize)
                            .onGloballyPositioned {
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

            // label x position is start + padding
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            // label y position is top + padding, then centered within allocated space
            assertThat(labelPosition.value?.y).isWithin(1f).of(
                (TextFieldWithLabelVerticalPadding + (MinFocusedLabelLineHeight - labelSize) / 2)
                    .toPx()
            )

            // placeholder x position is start + padding
            assertThat(placeholderPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            // placeholder y position is top + padding + label height, then centered within
            // allocated space
            assertThat(placeholderPosition.value?.y).isWithin(1f).of(
                (TextFieldWithLabelVerticalPadding + MinFocusedLabelLineHeight +
                    (MinTextLineHeight - placeholderSize) / 2).toPx()
            )
        }
    }

    @Test
    fun testTextField_trailingAndLeading_sizeAndPosition_defaultIcon() {
        val textFieldHeight = 60.dp
        val leadingPosition = Ref<Offset>()
        val leadingSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()
        val density = Density(2f)

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                TextField(
                    value = "text",
                    onValueChange = {},
                    modifier = Modifier.size(TextFieldWidth, textFieldHeight),
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
                    }
                )
            }
        }

        rule.runOnIdle {
            val size = 24.dp // default icon size
            with(density) {
                // leading
                assertThat(leadingSize.value).isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(leadingPosition.value?.x).isEqualTo(IconPadding.roundToPx().toFloat())
                assertThat(leadingPosition.value?.y).isEqualTo(
                    ((textFieldHeight.roundToPx() - leadingSize.value!!.height) / 2f).roundToInt()
                        .toFloat()
                )
                // trailing
                assertThat(trailingSize.value).isEqualTo(
                    IntSize(
                        size.roundToPx(),
                        size.roundToPx()
                    )
                )
                assertThat(trailingPosition.value?.x).isEqualTo(
                    (
                        TextFieldWidth.roundToPx() - IconPadding.roundToPx() -
                            trailingSize.value!!.width
                        ).toFloat()
                )
                assertThat(trailingPosition.value?.y)
                    .isEqualTo(
                        ((textFieldHeight.roundToPx() - trailingSize.value!!.height) / 2f)
                            .roundToInt().toFloat()
                    )
            }
        }
    }

    @Test
    fun testTextField_trailingAndLeading_sizeAndPosition_iconButton() {
        val textFieldHeight = 80.dp
        val density = Density(2f)

        var leadingPosition: Offset? = null
        var leadingSize: IntSize? = null
        var trailingPosition: Offset? = null
        var trailingSize: IntSize? = null

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                TextField(
                    value = "text",
                    onValueChange = {},
                    modifier = Modifier.size(TextFieldWidth, textFieldHeight),
                    leadingIcon = {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.onGloballyPositioned {
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
                            modifier = Modifier.onGloballyPositioned {
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
                assertThat(leadingPosition?.y).isEqualTo(
                    ((textFieldHeight.roundToPx() - leadingSize!!.height) / 2f).roundToInt()
                        .toFloat()
                )
                // trailing
                assertThat(trailingSize).isEqualTo(
                    IntSize(size.roundToPx(), size.roundToPx())
                )
                assertThat(trailingPosition?.x).isEqualTo(
                    (TextFieldWidth.roundToPx() - trailingSize!!.width).toFloat()
                )
                assertThat(trailingPosition?.y)
                    .isEqualTo(
                        ((textFieldHeight.roundToPx() - trailingSize!!.height) / 2f)
                            .roundToInt().toFloat()
                    )
            }
        }
    }

    @Test
    fun testTextField_trailingAndLeading_sizeAndPosition_nonDefaultSizeIcon() {
        val textFieldHeight = 80.dp
        val density = Density(2f)
        val size = 70.dp

        var leadingPosition: Offset? = null
        var leadingSize: IntSize? = null
        var trailingPosition: Offset? = null
        var trailingSize: IntSize? = null

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                TextField(
                    value = "text",
                    onValueChange = {},
                    modifier = Modifier.size(TextFieldWidth, textFieldHeight),
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(size)
                                .onGloballyPositioned {
                                    leadingPosition = it.positionInRoot()
                                    leadingSize = it.size
                                }
                        )
                    },
                    trailingIcon = {
                        Box(
                            Modifier
                                .size(size)
                                .onGloballyPositioned {
                                    trailingPosition = it.positionInRoot()
                                    trailingSize = it.size
                                }
                        )
                    }
                )
            }
        }

        rule.runOnIdle {
            with(density) {
                // leading
                assertThat(leadingSize).isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(leadingPosition?.x).isEqualTo(0f)
                assertThat(leadingPosition?.y).isEqualTo(
                    ((textFieldHeight.roundToPx() - leadingSize!!.height) / 2f).roundToInt()
                        .toFloat()
                )
                // trailing
                assertThat(trailingSize).isEqualTo(
                    IntSize(size.roundToPx(), size.roundToPx())
                )
                assertThat(trailingPosition?.x).isEqualTo(
                    (TextFieldWidth.roundToPx() - trailingSize!!.width).toFloat()
                )
                assertThat(trailingPosition?.y)
                    .isEqualTo(
                        ((textFieldHeight.roundToPx() - trailingSize!!.height) / 2f)
                            .roundToInt().toFloat()
                    )
            }
        }
    }

    @Test
    fun testTextField_prefixAndSuffixPosition_withLabel() {
        val prefixPosition = Ref<Offset>()
        val prefixSize = MinTextLineHeight
        val suffixPosition = Ref<Offset>()
        val suffixSize = MinTextLineHeight
        val density = Density(2f)

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                TextField(
                    value = "text",
                    onValueChange = {},
                    modifier = Modifier.width(TextFieldWidth),
                    label = { Box(Modifier.size(MinFocusedLabelLineHeight)) },
                    prefix = {
                        Box(
                            Modifier
                                .size(prefixSize)
                                .onGloballyPositioned {
                                    prefixPosition.value = it.positionInRoot()
                                }
                        )
                    },
                    suffix = {
                        Box(
                            Modifier
                                .size(suffixSize)
                                .onGloballyPositioned {
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
                assertThat(prefixPosition.value?.y).isWithin(1f).of(
                    (TextFieldWithLabelVerticalPadding + MinFocusedLabelLineHeight).toPx()
                )

                // suffix
                assertThat(suffixPosition.value?.x).isWithin(1f).of(
                    (TextFieldWidth - ExpectedPadding - suffixSize).toPx()
                )
                assertThat(suffixPosition.value?.y).isWithin(1f).of(
                    (TextFieldWithLabelVerticalPadding + MinFocusedLabelLineHeight).toPx()
                )
            }
        }
    }

    @Test
    fun testTextField_prefixAndSuffixPosition_whenNoLabel() {
        val prefixPosition = Ref<Offset>()
        val prefixSize = MinTextLineHeight
        val suffixPosition = Ref<Offset>()
        val suffixSize = MinTextLineHeight
        val density = Density(2f)

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                TextField(
                    value = "text",
                    onValueChange = {},
                    modifier = Modifier.width(TextFieldWidth),
                    prefix = {
                        Box(
                            Modifier
                                .size(prefixSize)
                                .onGloballyPositioned {
                                    prefixPosition.value = it.positionInRoot()
                                }
                        )
                    },
                    suffix = {
                        Box(
                            Modifier
                                .size(suffixSize)
                                .onGloballyPositioned {
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
                assertThat(suffixPosition.value?.x).isWithin(1f).of(
                    (TextFieldWidth - ExpectedPadding - suffixSize).toPx()
                )
                assertThat(suffixPosition.value?.y).isWithin(1f).of(ExpectedPadding.toPx())
            }
        }
    }

    @Test
    fun testTextField_prefixAndSuffixPosition_withIcons() {
        val prefixPosition = Ref<Offset>()
        val prefixSize = MinTextLineHeight
        val suffixPosition = Ref<Offset>()
        val suffixSize = MinTextLineHeight
        val density = Density(2f)

        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalDensity provides density) {
                TextField(
                    value = "text",
                    onValueChange = {},
                    modifier = Modifier.width(TextFieldWidth),
                    prefix = {
                        Box(
                            Modifier
                                .size(prefixSize)
                                .onGloballyPositioned {
                                    prefixPosition.value = it.positionInRoot()
                                }
                        )
                    },
                    suffix = {
                        Box(
                            Modifier
                                .size(suffixSize)
                                .onGloballyPositioned {
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
                assertThat(prefixPosition.value?.x).isWithin(1f).of(
                    (ExpectedPadding + IconPadding + iconSize).toPx()
                )
                assertThat(prefixPosition.value?.y).isWithin(1f).of(ExpectedPadding.toPx())

                // suffix
                assertThat(suffixPosition.value?.x).isWithin(1f).of(
                    (TextFieldWidth - IconPadding - iconSize - ExpectedPadding - suffixSize).toPx()
                )
                assertThat(suffixPosition.value?.y).isWithin(1f).of(ExpectedPadding.toPx())
            }
        }
    }

    @Test
    fun testTextField_labelPositionX_initial_withTrailingAndLeading() {
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                label = {
                    Text(
                        text = "label",
                        modifier = Modifier.onGloballyPositioned {
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
            assertThat(labelPosition.value?.x).isWithin(1f).of(
                (ExpectedPadding + IconPadding + iconSize).toPx()
            )
        }
    }

    @Test
    fun testTextField_labelPositionX_initial_withNullTrailingAndLeading() {
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                label = {
                    Text(
                        text = "label",
                        modifier = Modifier.onGloballyPositioned {
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
    fun testTextField_colorInLeadingTrailing_whenValidInput() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
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
    fun testTextField_colorInLeadingTrailing_whenInvalidInput() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
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
    fun testTextField_supportingTextPosition_withDefaultHeight() {
        val supportingPosition = Ref<Offset>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                textStyle = TextStyle(fontSize = 1.sp), // ensure text size is minimum
                supportingText = {
                    Box(
                        Modifier
                            .size(MinSupportingTextLineHeight)
                            .onGloballyPositioned {
                                supportingPosition.value = it.positionInRoot()
                            }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            assertThat(supportingPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            assertThat(supportingPosition.value?.y).isWithin(1f).of(
                (ExpectedDefaultTextFieldHeight + SupportingTopPadding).toPx()
            )
        }
    }

    @Test
    fun testTextField_supportingText_widthIsNotWiderThanTextField() {
        val tfSize = Ref<IntSize>()
        val supportingSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.onGloballyPositioned {
                    tfSize.value = it.size
                },
                supportingText = {
                    Text(
                        text = "Long long long long long long long long long long long long " +
                            "long long long long long long long long long long long long",
                        modifier = Modifier.onGloballyPositioned {
                            supportingSize.value = it.size
                        }
                    )
                }
            )
        }

        rule.runOnIdleWithDensity {
            assertThat(supportingSize.value!!.width).isAtMost(tfSize.value!!.width)
        }
    }

    @Test
    fun testTextField_supportingText_contributesToTextFieldMeasurements() {
        val tfSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.onGloballyPositioned {
                    tfSize.value = it.size
                },
                supportingText = { Text("Supporting") }
            )
        }

        rule.runOnIdleWithDensity {
            assertThat(tfSize.value!!.height).isGreaterThan(
                ExpectedDefaultTextFieldHeight.roundToPx()
            )
        }
    }

    @Test
    fun testTextField_supportingText_remainsVisibleWithTallInput() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = buildString {
                    repeat(200) {
                        append("line $it\n")
                    }
                },
                onValueChange = {},
                modifier = Modifier.size(width = ExpectedDefaultTextFieldWidth, height = 150.dp),
                supportingText = { Text("Supporting", modifier = Modifier.testTag("Supporting")) }
            )
        }

        rule.onNodeWithTag("Supporting", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun testTextField_supportingText_clickFocusesTextField() {
        var focused = false
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                modifier = Modifier.onFocusChanged { focused = it.isFocused },
                value = "input",
                onValueChange = {},
                supportingText = { Text("Supporting") }
            )
        }

        rule.onNodeWithText("Supporting").performClick()
        rule.runOnIdle {
            assertThat(focused).isTrue()
        }
    }

    @Test
    fun testTextField_supportingText_colorAndStyle() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                supportingText = {
                    assertThat(LocalTextStyle.current)
                        .isEqualTo(MaterialTheme.typography.bodySmall)
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )
        }
    }

    @Test
    fun testTextField_supportingText_error_colorAndStyle() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                isError = true,
                supportingText = {
                    assertThat(LocalTextStyle.current)
                        .isEqualTo(MaterialTheme.typography.bodySmall)
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colorScheme.error)
                }
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun testTextField_imeActionAndKeyboardTypePropagatedDownstream() {
        var editorInfo: EditorInfo? = null
        val interceptor = PlatformTextInputInterceptor { request, _ ->
            EditorInfo().also {
                request.createInputConnection(it)
                editorInfo = it
            }
            awaitCancellation()
        }
        rule.setContent {
            val text = remember { mutableStateOf("") }
            InterceptPlatformTextInput(interceptor) {
                OutlinedTextField(
                    modifier = Modifier.testTag(TextFieldTag),
                    value = text.value,
                    onValueChange = { text.value = it },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go,
                        keyboardType = KeyboardType.Email
                    )
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdle {
            @Suppress("NAME_SHADOWING")
            val editorInfo = editorInfo ?: throw AssertionError("Input session never started")
            val expectedOptions = EditorInfo.IME_ACTION_GO
            val expectedInputType = EditorInfo.TYPE_CLASS_TEXT or
                EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            assertThat(editorInfo.imeOptions and expectedOptions).isEqualTo(expectedOptions)
            assertThat(editorInfo.inputType and expectedInputType).isEqualTo(expectedInputType)
        }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTextField_visualTransformationPropagated() {
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                value = "qwerty",
                onValueChange = {},
                visualTransformation = PasswordVisualTransformation('\u0020'),
                shape = RectangleShape,
                colors = TextFieldDefaults.colors(unfocusedContainerColor = Color.White)
            )
        }

        rule.onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.White,
                shapeColor = Color.White,
                shape = RectangleShape,
                // avoid elevation artifacts
                shapeOverlapPixelCount = with(rule.density) { 3.dp.toPx() }
            )
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTextField_alphaNotApplied_toCustomContainerColorAndTransparentColors() {

        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.background(color = Color.White)) {
                TextField(
                    modifier = Modifier.testTag(TextFieldTag),
                    value = "test",
                    onValueChange = {},
                    label = { Text("label") },
                    shape = RectangleShape,
                    leadingIcon = {
                        Icon(Icons.Default.Favorite, null, tint = Color.Transparent)
                    },
                    trailingIcon = {
                        Icon(Icons.Default.Favorite, null, tint = Color.Transparent)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Blue,
                        unfocusedContainerColor = Color.Blue,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.Transparent,
                        unfocusedTextColor = Color.Transparent,
                        cursorColor = Color.Transparent,
                        focusedLabelColor = Color.Transparent,
                        unfocusedLabelColor = Color.Transparent
                    )
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.White,
                shapeColor = Color.Blue,
                shape = RectangleShape,
                // avoid elevation artifacts
                shapeOverlapPixelCount = with(rule.density) { 1.dp.toPx() }
            )

        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.White,
                shapeColor = Color.Blue,
                shape = RectangleShape,
                // avoid elevation artifacts
                shapeOverlapPixelCount = with(rule.density) { 1.dp.toPx() }
            )
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTextField_transformedTextIsUsed_toDefineLabelPosition() {
        // if non-transformed value were used to check if the text input is empty, the label
        // wouldn't be aligned to the top, as a result it would be obscured by text
        val prefixTransformation = VisualTransformation { text ->
            val prefix = "prefix"
            val transformed = buildAnnotatedString {
                append(prefix)
                append(text)
            }
            val mapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset + prefix.length
                override fun transformedToOriginal(offset: Int) =
                    (offset - prefix.length).coerceAtLeast(0)
            }
            TransformedText(transformed, mapping)
        }
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                visualTransformation = prefixTransformation,
                label = {
                    Text("label", color = Color.Red, modifier = Modifier.background(Color.Red))
                },
                textStyle = TextStyle(color = Color.Blue),
                colors = TextFieldDefaults.colors(unfocusedContainerColor = Color.White)
            )
        }
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), true)
            .captureToImage()
            .assertPixels { Color.Red }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTextField_transformedTextIsUsed_toDefineIfPlaceholderNeeded() {
        // if original value were used to check if the text input is empty, the placeholder would be
        // displayed on top of the text
        val prefixTransformation = VisualTransformation { text ->
            val prefix = "prefix"
            val transformed = buildAnnotatedString {
                append(prefix)
                append(text)
            }
            val mapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset + prefix.length
                override fun transformedToOriginal(offset: Int) =
                    (offset - prefix.length).coerceAtLeast(0)
            }
            TransformedText(transformed, mapping)
        }
        // While surface is not used in TextField, setMaterialContent wraps content in a Surface
        // component which is checked during assertPixels.
        rule.setMaterialContent(lightColorScheme(surface = Color.White)) {
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                value = "",
                onValueChange = {},
                visualTransformation = prefixTransformation,
                placeholder = {
                    Text(
                        text = "placeholder",
                        color = Color.Red,
                        modifier = Modifier.background(Color.Red)
                    )
                },
                textStyle = TextStyle(color = Color.White),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
        rule.onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertPixels {
                Color.White
            }
    }

    @Test
    fun testTextField_errorSemantics_defaultMessage() {
        lateinit var errorMessage: String
        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "test",
                onValueChange = {},
                isError = true
            )
            errorMessage = getString(DefaultErrorMessage)
        }

        rule.onNodeWithText("test")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, errorMessage))
    }

    @Test
    fun testTextField_errorSemantics_messageOverridable() {
        val errorMessage = "Special symbols not allowed"
        lateinit var defaultErrorMessage: String
        rule.setMaterialContent(lightColorScheme()) {
            val isError = remember { mutableStateOf(true) }
            TextField(
                value = "test",
                onValueChange = {},
                modifier = Modifier
                    .testTag(TextFieldTag)
                    .semantics { if (isError.value) error(errorMessage) },
                isError = isError.value
            )
            defaultErrorMessage = getString(DefaultErrorMessage)
        }

        rule.onNodeWithTag(TextFieldTag)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, errorMessage))

        // Check that default error message is overwritten and not lingering in a child node
        rule.onNodeWithTag(TextFieldTag, useUnmergedTree = true)
            .onChildren()
            .fetchSemanticsNodes()
            .forEach { node ->
                assertThat(node.config.getOrNull(SemanticsProperties.Error))
                    .isNotEqualTo(defaultErrorMessage)
            }
    }

    @Test
    fun testTextField_withLabel_doesNotCrash_rowHeightWithMinIntrinsics() {
        var size: IntSize? = null
        var dividerSize: IntSize? = null
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.onGloballyPositioned { size = it.size }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    VerticalDivider(
                        thickness = 10.dp,
                        modifier = Modifier.onGloballyPositioned { dividerSize = it.size }
                    )
                    TextField(
                        value = "",
                        label = { Text(text = "Label") },
                        onValueChange = {}
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
    fun testTextField_withLabel_doesNotCrash_columnWidthWithMinIntrinsics() {
        var textFieldSize: IntSize? = null
        var dividerSize: IntSize? = null
        rule.setMaterialContent(lightColorScheme()) {
            val text = remember { mutableStateOf("") }
            Box {
                Column(Modifier.width(IntrinsicSize.Min)) {
                    HorizontalDivider(
                        thickness = 10.dp,
                        modifier = Modifier.onGloballyPositioned { dividerSize = it.size }
                    )
                    TextField(
                        value = text.value,
                        label = { Text(text = "Label") },
                        onValueChange = { text.value = it },
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
    fun testTextField_labelStyle() {
        val unfocusedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified

        val focusRequester = FocusRequester()

        rule.setMaterialContent(lightColorScheme()) {
            TextField(
                value = "",
                onValueChange = {},
                label = {
                    textStyle = LocalTextStyle.current
                    contentColor = LocalContentColor.current
                },
                modifier = Modifier.focusRequester(focusRequester),
                colors = TextFieldDefaults.colors(
                    unfocusedLabelColor = unfocusedLabelColor, focusedLabelColor = focusedLabelColor
                )
            )
        }

        rule.runOnIdle {
            assertThat(contentColor).isEqualTo(unfocusedLabelColor)
            assertThat(textStyle.color).isEqualTo(Color.Unspecified)
        }

        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(contentColor).isEqualTo(focusedLabelColor)
            assertThat(textStyle.color).isEqualTo(Color.Unspecified)
        }
    }

    @Test
    fun testTextField_labelStyle_whenBodySmallStyleColorProvided() {
        val unfocusedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        val bodySmallColor = Color.Green
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified

        val focusRequester = FocusRequester()

        rule.setMaterialContent(lightColorScheme()) {
            val bodySmall = MaterialTheme.typography.bodySmall.copy(color = bodySmallColor)
            MaterialTheme(typography = Typography(bodySmall = bodySmall)) {
                TextField(
                    value = "",
                    onValueChange = {},
                    label = {
                        textStyle = LocalTextStyle.current
                        contentColor = LocalContentColor.current
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
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

        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(bodySmallColor)
            assertThat(contentColor).isEqualTo(focusedLabelColor)
        }
    }

    @Test
    fun testTextField_labelStyle_middle_whenBodySmallStyleColorProvided() {
        val expectedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified
        val focusRequester = FocusRequester()

        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            val bodySmall = MaterialTheme.typography.bodySmall.copy(color = expectedLabelColor)
            MaterialTheme(typography = Typography(bodySmall = bodySmall)) {
                TextField(
                    value = "",
                    onValueChange = {},
                    label = {
                        textStyle = LocalTextStyle.current
                        contentColor = LocalContentColor.current
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
                        unfocusedLabelColor = expectedLabelColor,
                        focusedLabelColor = focusedLabelColor
                    )
                )
            }
        }

        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        // advance to middle of animation
        rule.mainClock.advanceTimeBy(TextFieldAnimationDuration.toLong() / 2)

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(expectedLabelColor)
            // color should be a lerp between 'start' and 'end' colors. We check here that it's
            // not equal to either of them
            assertThat(contentColor).isNotEqualTo(expectedLabelColor)
            assertThat(contentColor).isNotEqualTo(focusedLabelColor)
        }
    }

    @Test
    fun testTextField_labelStyle_whenBothTypographiesColorProvided() {
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
                TextField(
                    value = "",
                    onValueChange = {},
                    label = {
                        textStyle = LocalTextStyle.current
                        contentColor = LocalContentColor.current
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
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

        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(bodySmallColor)
            assertThat(contentColor).isEqualTo(focusedLabelColor)
        }
    }

    @Test
    fun testTextField_intrinsicHeight_withOnlyEmptyInput() {
        var height = 0
        rule.setMaterialContent(lightColorScheme()) {
            val text = remember { mutableStateOf("") }
            Box(Modifier.onGloballyPositioned {
                height = it.size.height
            }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    TextField(
                        value = text.value,
                        onValueChange = { text.value = it },
                    )
                    VerticalDivider()
                }
            }
        }

        with(rule.density) {
            assertThat(height).isEqualTo((TextFieldDefaults.MinHeight).roundToPx())
        }
    }

    @Test
    fun testTextField_intrinsicHeight_withEmptyInput_andDecorations() {
        var height = 0
        rule.setMaterialContent(lightColorScheme()) {
            val text = remember { mutableStateOf("") }
            Box(Modifier.onGloballyPositioned {
                height = it.size.height
            }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    TextField(
                        value = text.value,
                        onValueChange = { text.value = it },
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
            assertThat(height).isEqualTo((TextFieldDefaults.MinHeight).roundToPx())
        }
    }

    @Test
    fun testTextField_intrinsicHeight_withLongInput_andDecorations() {
        var tfHeightIntrinsic = 0
        var tfHeightNoIntrinsic = 0
        val text = "Long text input. ".repeat(20)
        rule.setMaterialContent(lightColorScheme()) {
            Row {
                Box(
                    Modifier
                        .width(150.dp)
                        .height(IntrinsicSize.Min)
                ) {
                    TextField(
                        value = text,
                        onValueChange = {},
                        modifier = Modifier.onGloballyPositioned {
                            tfHeightIntrinsic = it.size.height
                        },
                        leadingIcon = { Icon(Icons.Default.Favorite, null) },
                        trailingIcon = { Icon(Icons.Default.Favorite, null) },
                        prefix = { Text("P") },
                        suffix = { Text("S") },
                    )
                }

                Box(Modifier.width(150.dp)) {
                    TextField(
                        value = text,
                        onValueChange = {},
                        modifier = Modifier.onGloballyPositioned {
                            tfHeightNoIntrinsic = it.size.height
                        },
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
    fun textField_stringOverload_doesNotCallOnValueChange_whenCompositionUpdatesOnly() {
        var callbackCounter = 0

        rule.setContent {
            val focusManager = LocalFocusManager.current
            val text = remember { mutableStateOf("A") }

            TextField(
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

        rule.onNodeWithTag(TextFieldTag)
            .performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(TextFieldTag)
            .performTextClearance()

        rule.runOnIdle {
            assertThat(callbackCounter).isEqualTo(1)
        }
    }
    @Test
    fun testTextFields_noCrashConstraintsInfinity() {

        rule.setMaterialContent(lightColorScheme()) {
            Column(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .horizontalScroll(
                        rememberScrollState()
                    )
            ) {
                TextField(
                    value = "Cat",
                    onValueChange = {},
                    leadingIcon = { Text("Icon") }
                )
            }
        }
    }
}

private val View.isSoftwareKeyboardShown: Boolean
    get() {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // TODO(b/163742556): This is just a proxy for software keyboard visibility. Find a better
        //  way to check if the software keyboard is shown.
        return inputMethodManager.isAcceptingText()
    }
