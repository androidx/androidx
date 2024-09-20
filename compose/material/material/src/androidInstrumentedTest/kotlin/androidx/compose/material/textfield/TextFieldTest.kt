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

package androidx.compose.material.textfield

import android.content.Context
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.EditorInfo.IME_ACTION_GO
import android.view.inputmethod.EditorInfo.TYPE_CLASS_TEXT
import android.view.inputmethod.EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
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
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Strings.Companion.DefaultErrorMessage
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldPadding
import androidx.compose.material.Typography
import androidx.compose.material.getString
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.runOnIdleWithDensity
import androidx.compose.material.setMaterialContent
import androidx.compose.material.setMaterialContentForSizeAssertions
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.FirstBaseline
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
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldTest {

    private val ExpectedDefaultTextFieldHeight = 56.dp
    private val ExpectedDefaultTextFieldWidth = 280.dp
    private val ExpectedPadding = 16.dp
    private val IconPadding = 12.dp
    private val ExpectedBaselineOffset = 20.dp
    private val TopPaddingFilledTextfield = 2.dp
    private val IconColorAlpha = 0.54f
    private val TextFieldTag = "textField"

    @get:Rule val rule = createComposeRule()

    @Test
    fun testTextField_minimumHeight() {
        rule
            .setMaterialContentForSizeAssertions {
                TextField(
                    state = rememberTextFieldState("input"),
                    modifier = Modifier.height(20.dp)
                )
            }
            .assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun testTextField_setSmallWidth() {
        rule
            .setMaterialContentForSizeAssertions {
                TextField(
                    state = rememberTextFieldState("input"),
                    modifier = Modifier.requiredWidth(40.dp)
                )
            }
            .assertWidthIsEqualTo(40.dp)
    }

    @Test
    fun testTextField_defaultWidth() {
        rule
            .setMaterialContentForSizeAssertions { TextField(rememberTextFieldState("input")) }
            .assertWidthIsEqualTo(ExpectedDefaultTextFieldWidth)
    }

    @Test
    fun testTextFields_singleFocus() {
        val textField1Tag = "TextField1"
        val textField2Tag = "TextField2"
        val interactionSource1 = MutableInteractionSource()
        val interactionSource2 = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setMaterialContent {
            scope = rememberCoroutineScope()
            Column {
                TextField(
                    modifier = Modifier.testTag(textField1Tag),
                    state = rememberTextFieldState("input1"),
                    interactionSource = interactionSource1
                )
                TextField(
                    modifier = Modifier.testTag(textField2Tag),
                    state = rememberTextFieldState("input2"),
                    interactionSource = interactionSource2
                )
            }
        }

        val interactions1 = mutableListOf<Interaction>()
        val interactions2 = mutableListOf<Interaction>()

        scope!!.launch { interactionSource1.interactions.collect { interactions1.add(it) } }
        scope!!.launch { interactionSource2.interactions.collect { interactions2.add(it) } }

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

        rule.setMaterialContent {
            scope = rememberCoroutineScope()
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                state = rememberTextFieldState("input"),
                interactionSource = interactionSource
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        // Click on (2, 2) which is Surface area and outside input area
        rule.onNodeWithTag(TextFieldTag).performTouchInput { click(Offset(2f, 2f)) }

        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<FocusInteraction.Focus>()).hasSize(1)
        }
    }

    @Test
    fun testTextField_showHideKeyboardBasedOnFocus() {
        val (focusRequester, parentFocusRequester) = FocusRequester.createRefs()
        lateinit var hostView: View
        rule.setMaterialContent {
            hostView = LocalView.current
            Box {
                TextField(
                    modifier =
                        Modifier.focusRequester(parentFocusRequester)
                            .focusTarget()
                            .focusRequester(focusRequester)
                            .testTag(TextFieldTag),
                    state = rememberTextFieldState("input"),
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

    @Test
    fun testTextField_clickingOnTextAfterDismissingKeyboard_showsKeyboard() {
        val (focusRequester, parentFocusRequester) = FocusRequester.createRefs()
        var softwareKeyboardController: SoftwareKeyboardController? = null
        lateinit var hostView: View
        rule.setMaterialContent {
            hostView = LocalView.current
            softwareKeyboardController = LocalSoftwareKeyboardController.current
            Box {
                TextField(
                    modifier =
                        Modifier.focusRequester(parentFocusRequester)
                            .focusTarget()
                            .focusRequester(focusRequester)
                            .testTag(TextFieldTag),
                    state = rememberTextFieldState("input"),
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
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent {
            Box {
                TextField(
                    state = rememberTextFieldState(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    label = {
                        Text(
                            text = "label",
                            fontSize = 10.sp,
                            modifier =
                                Modifier.onGloballyPositioned {
                                    labelPosition.value = it.positionInRoot()
                                    labelSize.value = it.size
                                }
                        )
                    },
                    modifier = Modifier.height(56.dp)
                )
            }
        }

        rule.runOnIdleWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // centered position
            assertThat(labelPosition.value?.x).isWithin(1f).of(ExpectedPadding.toPx())
            assertThat(labelPosition.value?.y)
                .isWithin(1f)
                .of((ExpectedDefaultTextFieldHeight.toPx() - labelSize.value!!.height) / 2f)
        }
    }

    @Test
    fun testTextField_labelPosition_initial_withDefaultHeight() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent {
            Box {
                TextField(
                    state = rememberTextFieldState(),
                    label = {
                        Text(
                            text = "label",
                            fontSize = 10.sp,
                            modifier =
                                Modifier.onGloballyPositioned {
                                    labelPosition.value = it.positionInRoot()
                                    labelSize.value = it.size
                                }
                        )
                    },
                    modifier = Modifier.height(56.dp)
                )
            }
        }

        rule.runOnIdleWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // centered position
            assertThat(labelPosition.value?.x).isEqualTo(ExpectedPadding.roundToPx().toFloat())
            assertThat(labelPosition.value?.y).isEqualTo(ExpectedPadding.roundToPx())
        }
    }

    @Test
    fun testTextField_labelPosition_initial_withCustomHeight() {
        val height = 80.dp
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent {
            Box {
                TextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier.height(height),
                    label = {
                        Text(
                            text = "label",
                            modifier =
                                Modifier.onGloballyPositioned {
                                    labelPosition.value = it.positionInRoot()
                                    labelSize.value = it.size
                                }
                        )
                    }
                )
            }
        }

        rule.runOnIdleWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            assertThat(labelPosition.value?.x).isEqualTo(ExpectedPadding.roundToPx().toFloat())
            assertThat(labelPosition.value?.y).isEqualTo(ExpectedPadding.roundToPx())
        }
    }

    @Test
    fun testTextField_labelPosition_whenFocused() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        val baseline = Ref<Float>()
        rule.setMaterialContent {
            Box {
                TextField(
                    modifier = Modifier.testTag(TextFieldTag),
                    state = rememberTextFieldState(),
                    label = {
                        Text(
                            text = "label",
                            modifier =
                                Modifier.onGloballyPositioned {
                                    labelPosition.value = it.positionInRoot()
                                    labelSize.value = it.size
                                    baseline.value =
                                        it[FirstBaseline].toFloat() + labelPosition.value!!.y
                                }
                        )
                    }
                )
            }
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // label's top position
            assertThat(labelPosition.value?.x).isEqualTo(ExpectedPadding.roundToPx().toFloat())
            assertThat(baseline.value).isEqualTo(ExpectedBaselineOffset.roundToPx().toFloat())
        }
    }

    @Test
    fun testTextField_labelPosition_whenInput() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        val baseline = Ref<Float>()
        rule.setMaterialContent {
            Box {
                TextField(
                    state = rememberTextFieldState("input"),
                    label = {
                        Text(
                            text = "label",
                            modifier =
                                Modifier.onGloballyPositioned {
                                    labelPosition.value = it.positionInRoot()
                                    labelSize.value = it.size
                                    baseline.value =
                                        it[FirstBaseline].toFloat() + labelPosition.value!!.y
                                }
                        )
                    }
                )
            }
        }

        rule.runOnIdleWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // label's top position
            assertThat(labelPosition.value?.x).isEqualTo(ExpectedPadding.roundToPx().toFloat())
            assertThat(baseline.value).isEqualTo(ExpectedBaselineOffset.roundToPx().toFloat())
        }
    }

    @Test
    fun testTextField_placeholderPosition_withLabel() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent {
            Box {
                TextField(
                    modifier = Modifier.height(60.dp).testTag(TextFieldTag),
                    state = rememberTextFieldState(),
                    label = { Text("label") },
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
        }
        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            // size
            assertThat(placeholderSize.value).isNotNull()
            assertThat(placeholderSize.value?.height).isGreaterThan(0)
            assertThat(placeholderSize.value?.width).isGreaterThan(0)
            // placeholder's position
            assertThat(placeholderPosition.value?.x)
                .isEqualTo(ExpectedPadding.roundToPx().toFloat())
            assertThat(placeholderPosition.value?.y)
                .isEqualTo(
                    (ExpectedBaselineOffset.roundToPx() + TopPaddingFilledTextfield.roundToPx())
                        .toFloat()
                )
        }
    }

    @Test
    fun testTextField_placeholderPosition_whenNoLabel() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        val height = 60.dp
        rule.setMaterialContent {
            Box {
                TextField(
                    modifier = Modifier.height(height).testTag(TextFieldTag),
                    state = rememberTextFieldState(),
                    placeholder = {
                        Text(
                            text = "placeholder",
                            modifier =
                                Modifier.requiredHeight(20.dp).onGloballyPositioned {
                                    placeholderPosition.value = it.positionInRoot()
                                    placeholderSize.value = it.size
                                }
                        )
                    }
                )
            }
        }
        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()

        rule.runOnIdleWithDensity {
            // size
            assertThat(placeholderSize.value).isNotNull()
            assertThat(placeholderSize.value?.height).isEqualTo(20.dp.roundToPx())
            assertThat(placeholderSize.value?.width).isGreaterThan(0)
            // centered position
            assertThat(placeholderPosition.value?.x)
                .isEqualTo(ExpectedPadding.roundToPx().toFloat())
            assertThat(placeholderPosition.value?.y).isEqualTo(TextFieldPadding.roundToPx())
        }
    }

    @Test
    fun testTextField_noPlaceholder_whenInputNotEmpty() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        rule.setMaterialContent {
            Column {
                TextField(
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
        rule.setMaterialContent {
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                state = rememberTextFieldState(),
                placeholder = {
                    Text("placeholder")
                    assertThat(LocalContentColor.current.copy(alpha = LocalContentAlpha.current))
                        .isEqualTo(MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    assertThat(LocalTextStyle.current).isEqualTo(MaterialTheme.typography.subtitle1)
                }
            )
        }

        // click to focus
        rule.onNodeWithTag(TextFieldTag).performClick()
    }

    @Test
    fun testTextField_trailingAndLeading_sizeAndPosition_defaultIcon() {
        val textFieldHeight = 60.dp
        val textFieldWidth = 300.dp
        val leadingPosition = Ref<Offset>()
        val leadingSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()
        val density = Density(2f)

        rule.setMaterialContent {
            CompositionLocalProvider(LocalDensity provides density) {
                TextField(
                    state = rememberTextFieldState("text"),
                    modifier = Modifier.size(textFieldWidth, textFieldHeight),
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
                assertThat(leadingPosition.value?.y)
                    .isEqualTo(
                        ((textFieldHeight.roundToPx() - leadingSize.value!!.height) / 2f)
                            .roundToInt()
                            .toFloat()
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
                        ((textFieldHeight.roundToPx() - trailingSize.value!!.height) / 2f)
                            .roundToInt()
                            .toFloat()
                    )
            }
        }
    }

    @Test
    fun testTextField_trailingAndLeading_sizeAndPosition_iconButton() {
        val textFieldHeight = 80.dp
        val textFieldWidth = 300.dp
        val density = Density(2f)

        var leadingPosition: Offset? = null
        var leadingSize: IntSize? = null
        var trailingPosition: Offset? = null
        var trailingSize: IntSize? = null

        rule.setMaterialContent {
            CompositionLocalProvider(LocalDensity provides density) {
                TextField(
                    state = rememberTextFieldState("text"),
                    modifier = Modifier.size(textFieldWidth, textFieldHeight),
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
                        ((textFieldHeight.roundToPx() - leadingSize!!.height) / 2f)
                            .roundToInt()
                            .toFloat()
                    )
                // trailing
                assertThat(trailingSize).isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(trailingPosition?.x)
                    .isEqualTo((textFieldWidth.roundToPx() - trailingSize!!.width).toFloat())
                assertThat(trailingPosition?.y)
                    .isEqualTo(
                        ((textFieldHeight.roundToPx() - trailingSize!!.height) / 2f)
                            .roundToInt()
                            .toFloat()
                    )
            }
        }
    }

    @Test
    fun testTextField_trailingAndLeading_sizeAndPosition_nonDefaultSizeIcon() {
        val textFieldHeight = 80.dp
        val textFieldWidth = 300.dp
        val density = Density(2f)
        val size = 70.dp

        var leadingPosition: Offset? = null
        var leadingSize: IntSize? = null
        var trailingPosition: Offset? = null
        var trailingSize: IntSize? = null

        rule.setMaterialContent {
            CompositionLocalProvider(LocalDensity provides density) {
                TextField(
                    state = rememberTextFieldState("text"),
                    modifier = Modifier.size(textFieldWidth, textFieldHeight),
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
                    }
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
                        ((textFieldHeight.roundToPx() - leadingSize!!.height) / 2f)
                            .roundToInt()
                            .toFloat()
                    )
                // trailing
                assertThat(trailingSize).isEqualTo(IntSize(size.roundToPx(), size.roundToPx()))
                assertThat(trailingPosition?.x)
                    .isEqualTo((textFieldWidth.roundToPx() - trailingSize!!.width).toFloat())
                assertThat(trailingPosition?.y)
                    .isEqualTo(
                        ((textFieldHeight.roundToPx() - trailingSize!!.height) / 2f)
                            .roundToInt()
                            .toFloat()
                    )
            }
        }
    }

    @Test
    fun testTextField_labelPositionX_initial_withTrailingAndLeading() {
        val height = 60.dp
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent {
            Box {
                TextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier.height(height),
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
        }

        rule.runOnIdleWithDensity {
            val iconSize = 24.dp // default icon size
            assertThat(labelPosition.value?.x)
                .isEqualTo(
                    (ExpectedPadding.roundToPx() + IconPadding.roundToPx() + iconSize.roundToPx())
                        .toFloat()
                )
        }
    }

    @Test
    fun testTextField_labelPositionX_initial_withNullTrailingAndLeading() {
        val height = 60.dp
        val labelPosition = Ref<Offset>()
        rule.setMaterialContent {
            Box {
                TextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier.height(height),
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
        }

        rule.runOnIdleWithDensity {
            assertThat(labelPosition.value?.x).isEqualTo(ExpectedPadding.roundToPx().toFloat())
        }
    }

    @Test
    fun testTextField_colorInLeadingTrailing_whenValidInput() {
        rule.setMaterialContent {
            TextField(
                state = rememberTextFieldState(),
                isError = false,
                leadingIcon = {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colors.onSurface.copy(IconColorAlpha))
                },
                trailingIcon = {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colors.onSurface.copy(IconColorAlpha))
                }
            )
        }
    }

    @Test
    fun testTextField_colorInLeadingTrailing_whenInvalidInput() {
        rule.setMaterialContent {
            TextField(
                state = rememberTextFieldState(),
                isError = true,
                leadingIcon = {
                    assertThat(LocalContentColor.current)
                        .isEqualTo(MaterialTheme.colors.onSurface.copy(IconColorAlpha))
                },
                trailingIcon = {
                    assertThat(LocalContentColor.current).isEqualTo(MaterialTheme.colors.error)
                }
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
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
            val state = rememberTextFieldState()
            InterceptPlatformTextInput(interceptor) {
                TextField(
                    modifier = Modifier.testTag(TextFieldTag),
                    state = state,
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
    fun testTextField_outputTransformationPropagated() {
        rule.setMaterialContent {
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                state = rememberTextFieldState("qwerty"),
                outputTransformation = {
                    // transform all chars to blank spaces
                    val size = length
                    delete(0, length)
                    insert(0, " ".repeat(size))
                    placeCursorAtEnd()
                },
                shape = RectangleShape,
                colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.White)
            )
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
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTextField_alphaNotApplied_toCustomBackgroundColorAndTransparentColors() {

        rule.setMaterialContent {
            Box(Modifier.background(color = Color.White)) {
                TextField(
                    modifier = Modifier.testTag(TextFieldTag),
                    state = rememberTextFieldState("test"),
                    label = { Text("label") },
                    shape = RectangleShape,
                    leadingIcon = { Icon(Icons.Default.Favorite, null, tint = Color.Transparent) },
                    trailingIcon = { Icon(Icons.Default.Favorite, null, tint = Color.Transparent) },
                    colors =
                        TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Blue,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            textColor = Color.Transparent,
                            cursorColor = Color.Transparent,
                            focusedLabelColor = Color.Transparent,
                            unfocusedLabelColor = Color.Transparent
                        )
                )
            }
        }

        rule
            .onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.White,
                shapeColor = Color.Blue,
                shape = RectangleShape,
                // avoid elevation artifacts
                antiAliasingGap = with(rule.density) { 1.dp.toPx() }
            )

        rule.onNodeWithTag(TextFieldTag).performClick()

        rule
            .onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                backgroundColor = Color.White,
                shapeColor = Color.Blue,
                shape = RectangleShape,
                // avoid elevation artifacts
                antiAliasingGap = with(rule.density) { 1.dp.toPx() }
            )
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTransformedTextIsUsed_toDefineLabelPosition() {
        rule.setMaterialContent {
            TextField(
                state = rememberTextFieldState(),
                // if non-transformed value were used to check if the text input is empty, the label
                // wouldn't be aligned to the top, as a result it would be obscured by text
                outputTransformation = { insert(0, "prefix") },
                label = {
                    Text(
                        text = "label",
                        color = Color.Red,
                        modifier = Modifier.testTag("Label").background(Color.Red)
                    )
                },
                textStyle = TextStyle(color = Color.Blue),
                colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.White)
            )
        }
        // Label's top padding is only TextFieldPadding in the unfocused state,
        // but state should be focused
        val labelBounds = rule.onNodeWithTag("Label", true).getUnclippedBoundsInRoot()
        assertThat(labelBounds.top).isLessThan(TextFieldPadding)
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTransformedTextIsUsed_toDefineIfPlaceholderNeeded() {
        rule.setMaterialContent {
            TextField(
                modifier = Modifier.testTag(TextFieldTag),
                state = rememberTextFieldState(),
                // if original value were used to check if the text input is empty, the placeholder
                // would be displayed on top of the text
                outputTransformation = { insert(0, "prefix") },
                placeholder = {
                    Text(
                        text = "placeholder",
                        color = Color.Red,
                        modifier = Modifier.background(Color.Red)
                    )
                },
                textStyle = TextStyle(color = Color.White),
                colors =
                    TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.White,
                        unfocusedIndicatorColor = Color.Transparent
                    )
            )
        }
        rule.onNodeWithTag(TextFieldTag).captureToImage().assertPixels { Color.White }
    }

    @Test
    fun testErrorSemantics_defaultMessage() {
        lateinit var errorMessage: String
        rule.setMaterialContent {
            TextField(state = rememberTextFieldState("test"), isError = true)
            errorMessage = getString(DefaultErrorMessage)
        }

        rule
            .onNodeWithText("test")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, errorMessage))
    }

    @Test
    fun testErrorSemantics_messageOverridable() {
        val errorMessage = "Special symbols not allowed"
        lateinit var defaultErrorMessage: String
        rule.setMaterialContent {
            val isError = remember { mutableStateOf(true) }
            TextField(
                state = rememberTextFieldState("test"),
                modifier =
                    Modifier.testTag(TextFieldTag).semantics {
                        if (isError.value) error(errorMessage)
                    },
                isError = isError.value
            )
            defaultErrorMessage = getString(DefaultErrorMessage)
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
    fun testTextField_withLabel_doesNotCrash_rowHeightWithMinIntrinsics() {
        var size: IntSize? = null
        var dividerSize: IntSize? = null
        rule.setMaterialContent {
            Box(Modifier.onGloballyPositioned { size = it.size }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Divider(
                        modifier =
                            Modifier.fillMaxHeight().width(10.dp).onGloballyPositioned {
                                dividerSize = it.size
                            }
                    )
                    TextField(state = rememberTextFieldState(), label = { Text(text = "Label") })
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
        rule.setMaterialContent {
            Box {
                Column(Modifier.width(IntrinsicSize.Min)) {
                    Divider(
                        modifier =
                            Modifier.fillMaxWidth().height(10.dp).onGloballyPositioned {
                                dividerSize = it.size
                            }
                    )
                    TextField(
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
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testTextField_label_notUsingErrorColor_notFocused_withoutInput() {
        rule.setMaterialContent {
            Box(Modifier.background(Color.White).padding(10.dp)) {
                TextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier.testTag(TextFieldTag),
                    label = { Text("Label") },
                    isError = true,
                    colors =
                        TextFieldDefaults.textFieldColors(
                            unfocusedLabelColor = Color.White,
                            errorLabelColor = Color.Red,
                            backgroundColor = Color.White,
                            errorIndicatorColor = Color.White
                        )
                )
            }
        }

        rule.onNodeWithTag(TextFieldTag).captureToImage().assertPixels { Color.White }
    }

    @Test
    fun testTextField_labelStyle() {
        val unfocusedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified

        val focusRequester = FocusRequester()

        rule.setMaterialContent {
            TextField(
                state = rememberTextFieldState(),
                label = {
                    textStyle = LocalTextStyle.current
                    contentColor = LocalContentColor.current
                },
                modifier = Modifier.focusRequester(focusRequester),
                colors =
                    TextFieldDefaults.textFieldColors(
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
    fun testTextField_labelStyle_whenCaptionStyleColorProvided() {
        val unfocusedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        val captionColor = Color.Green
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified

        val focusRequester = FocusRequester()

        rule.setMaterialContent {
            val caption = MaterialTheme.typography.caption.copy(color = captionColor)
            MaterialTheme(typography = Typography(caption = caption)) {
                TextField(
                    state = rememberTextFieldState(),
                    label = {
                        textStyle = LocalTextStyle.current
                        contentColor = LocalContentColor.current
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    colors =
                        TextFieldDefaults.textFieldColors(
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
            assertThat(textStyle.color).isEqualTo(captionColor)
            assertThat(contentColor).isEqualTo(focusedLabelColor)
        }
    }

    @Test
    fun testTextField_labelStyle_middle_whenCaptionStyleColorProvided() {
        val expectedLabelColor = Color.Blue
        val focusedLabelColor = Color.Red
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified
        val focusRequester = FocusRequester()

        rule.mainClock.autoAdvance = false
        rule.setMaterialContent {
            val caption = MaterialTheme.typography.caption.copy(color = expectedLabelColor)
            MaterialTheme(typography = Typography(caption = caption)) {
                TextField(
                    state = rememberTextFieldState(),
                    label = {
                        textStyle = LocalTextStyle.current
                        contentColor = LocalContentColor.current
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    colors =
                        TextFieldDefaults.textFieldColors(
                            unfocusedLabelColor = expectedLabelColor,
                            focusedLabelColor = focusedLabelColor
                        )
                )
            }
        }

        rule.runOnUiThread { focusRequester.requestFocus() }

        // animation duration is 150, advancing by 75 to get into middle of animation
        rule.mainClock.advanceTimeBy(75)

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
        val captionColor = Color.Green
        val subtitleColor = Color.Black
        var textStyle = TextStyle()
        var contentColor = Color.Unspecified
        val focusRequester = FocusRequester()

        rule.setMaterialContent {
            val caption = MaterialTheme.typography.caption.copy(color = captionColor)
            val subtitle1 = MaterialTheme.typography.subtitle1.copy(color = subtitleColor)
            MaterialTheme(typography = Typography(caption = caption, subtitle1 = subtitle1)) {
                TextField(
                    state = rememberTextFieldState(),
                    label = {
                        textStyle = LocalTextStyle.current
                        contentColor = LocalContentColor.current
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                    colors =
                        TextFieldDefaults.textFieldColors(
                            unfocusedLabelColor = unfocusedLabelColor,
                            focusedLabelColor = focusedLabelColor
                        )
                )
            }
        }

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(subtitleColor)
            assertThat(contentColor).isEqualTo(unfocusedLabelColor)
        }

        rule.runOnUiThread { focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(textStyle.color).isEqualTo(captionColor)
            assertThat(contentColor).isEqualTo(focusedLabelColor)
        }
    }

    @Test
    fun testTextField_intrinsicHeight_withOnlyEmptyInput() {
        var height = 0
        rule.setMaterialContent {
            Box(Modifier.onGloballyPositioned { height = it.size.height }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    TextField(rememberTextFieldState())
                    Divider(Modifier.fillMaxHeight())
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
        rule.setMaterialContent {
            Box(Modifier.onGloballyPositioned { height = it.size.height }) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    TextField(
                        state = rememberTextFieldState(),
                        leadingIcon = { Icon(Icons.Default.Favorite, null) },
                        trailingIcon = { Icon(Icons.Default.Favorite, null) },
                    )
                    Divider(Modifier.fillMaxHeight())
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
        rule.setMaterialContent {
            Row {
                Box(Modifier.width(150.dp).height(IntrinsicSize.Min)) {
                    TextField(
                        state = rememberTextFieldState(text),
                        modifier =
                            Modifier.onGloballyPositioned { tfHeightIntrinsic = it.size.height },
                        leadingIcon = { Icon(Icons.Default.Favorite, null) },
                        trailingIcon = { Icon(Icons.Default.Favorite, null) },
                    )
                }
                Box(Modifier.width(150.dp)) {
                    TextField(
                        state = rememberTextFieldState(text),
                        modifier =
                            Modifier.onGloballyPositioned { tfHeightNoIntrinsic = it.size.height },
                        leadingIcon = { Icon(Icons.Default.Favorite, null) },
                        trailingIcon = { Icon(Icons.Default.Favorite, null) },
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

        rule.setMaterialContent {
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

        rule.onNodeWithTag(TextFieldTag).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(TextFieldTag).performTextClearance()

        rule.runOnIdle { assertThat(callbackCounter).isEqualTo(1) }
    }

    @Test
    fun testTextFields_noCrashConstraintsInfinity() {

        rule.setMaterialContent {
            Column(
                modifier =
                    Modifier.height(IntrinsicSize.Min).horizontalScroll(rememberScrollState())
            ) {
                TextField(state = rememberTextFieldState("Cat"), leadingIcon = { Text("Icon") })
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
