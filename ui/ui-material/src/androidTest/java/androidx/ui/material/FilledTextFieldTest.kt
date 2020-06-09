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

import android.os.Build
import androidx.compose.Providers
import androidx.compose.remember
import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.TextInputServiceAmbient
import androidx.ui.core.globalPosition
import androidx.ui.core.onPositioned
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.TextField
import androidx.ui.foundation.TextFieldValue
import androidx.ui.foundation.contentColor
import androidx.ui.foundation.currentTextStyle
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.compositeOver
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.PasswordVisualTransformation
import androidx.ui.input.TextInputService
import androidx.ui.geometry.Offset
import androidx.ui.layout.Column
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.test.assertPixels
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.doGesture
import androidx.ui.test.doSendImeAction
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.sendClick
import androidx.ui.test.sendSwipeDown
import androidx.ui.test.sendSwipeUp
import androidx.ui.text.FirstBaseline
import androidx.ui.text.SoftwareKeyboardController
import androidx.ui.unit.IntSize
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@MediumTest
@RunWith(JUnit4::class)
class FilledTextFieldTest {

    private val ExpectedMinimumTextFieldHeight = 56.dp
    private val ExpectedPadding = 16.dp
    private val IconPadding = 12.dp
    private val ExpectedBaselineOffset = 20.dp
    private val ExpectedLastBaselineOffset = 16.dp
    private val IconColorAlpha = 0.54f

    private val LONG_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
            "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
            " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
            "fugiat nulla pariatur."

    @get:Rule
    val testRule = createComposeRule().also {
        it.clockTestRule.pauseClock()
    }

    @Test
    fun testTextFieldMinimumHeight() {
        testRule
            .setMaterialContentAndCollectSizes {
                FilledTextField(
                    value = "input",
                    onValueChange = {},
                    label = {},
                    modifier = Modifier.preferredHeight(20.dp)
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
                FilledTextField(
                    modifier = Modifier.testTag(textField1Tag),
                    value = "input1",
                    onValueChange = {},
                    label = {},
                    onFocusChange = { textField1Focused = it }
                )
                FilledTextField(
                    modifier = Modifier.testTag(textField2Tag),
                    value = "input2",
                    onValueChange = {},
                    label = {},
                    onFocusChange = { textField2Focused = it }
                )
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
                FilledTextField(
                    modifier = Modifier.testTag("textField"),
                    value = "input",
                    onValueChange = {},
                    label = {},
                    onFocusChange = { focused = it }
                )
            }
        }

        // Click on (2, 2) which is Surface area and outside input area
        findByTag("textField").doGesture {
            sendClick(Offset(2f, 2f))
        }

        testRule.runOnIdleComposeWithDensity {
            assertThat(focused).isTrue()
        }
    }

    @Test
    fun testLabelPosition_initial_withDefaultHeight() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    value = "",
                    onValueChange = {},
                    label = {
                        Text(
                            text = "label",
                            fontSize = 10.sp,
                            modifier = Modifier
                                .onPositioned {
                                    labelPosition.value = it.globalPosition
                                    labelSize.value = it.size
                                }
                        )
                    },
                    modifier = Modifier.preferredHeight(56.dp)
                )
            }
        }

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // centered position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(labelPosition.value?.y).isEqualTo(
                ((ExpectedMinimumTextFieldHeight.toIntPx() - labelSize.value!!.height) / 2f)
                    .roundToInt().toFloat()
            )
        }
    }

    @Test
    fun testLabelPosition_initial_withCustomHeight() {
        val height = 80.dp
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
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
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // centered position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(labelPosition.value?.y).isEqualTo(
                ((height.toIntPx() - labelSize.value!!.height) / 2f).roundToInt().toFloat()
            )
        }
    }

    @Test
    fun testLabelPosition_whenFocused() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        val baseline = Ref<Float>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    modifier = Modifier.testTag("textField"),
                    value = "",
                    onValueChange = {},
                    label = {
                        Text(text = "label", modifier = Modifier.onPositioned {
                            labelPosition.value = it.globalPosition
                            labelSize.value = it.size
                            baseline.value =
                                it[FirstBaseline]!!.toFloat() + labelPosition.value!!.y
                        })
                    }
                )
            }
        }

        // click to focus
        clickAndAdvanceClock("textField", 200)

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // label's top position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(baseline.value).isEqualTo(
                ExpectedBaselineOffset.toIntPx().toFloat()
            )
        }
    }

    @Test
    fun testLabelPosition_whenInput() {
        val labelSize = Ref<IntSize>()
        val labelPosition = Ref<Offset>()
        val baseline = Ref<Float>()
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
                                it[FirstBaseline]!!.toFloat() + labelPosition.value!!.y
                        })
                    }
                )
            }
        }

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(labelSize.value).isNotNull()
            assertThat(labelSize.value?.height).isGreaterThan(0)
            assertThat(labelSize.value?.width).isGreaterThan(0)
            // label's top position
            assertThat(labelPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(baseline.value).isEqualTo(
                ExpectedBaselineOffset.toIntPx().toFloat()
            )
        }
    }

    @Test
    fun testPlaceholderPosition_withLabel() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        val placeholderBaseline = Ref<Float>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    modifier = Modifier
                        .preferredHeight(60.dp)
                        .testTag("textField"),
                    value = "",
                    onValueChange = {},
                    label = { Text("label") },
                    placeholder = {
                        Text(text = "placeholder", modifier = Modifier.onPositioned {
                            placeholderPosition.value = it.globalPosition
                            placeholderSize.value = it.size
                            placeholderBaseline.value =
                                it[FirstBaseline]!!.toFloat() + placeholderPosition.value!!.y
                        })
                    }
                )
            }
        }
        // click to focus
        clickAndAdvanceClock("textField", 200)

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(placeholderSize.value).isNotNull()
            assertThat(placeholderSize.value?.height).isGreaterThan(0)
            assertThat(placeholderSize.value?.width).isGreaterThan(0)
            // placeholder's position
            assertThat(placeholderPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(placeholderBaseline.value)
                .isEqualTo(
                    60.dp.toIntPx().toFloat() -
                            ExpectedLastBaselineOffset.toIntPx().toFloat()
                )
        }
    }

    @Test
    fun testPlaceholderPosition_whenNoLabel() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        val placeholderBaseline = Ref<Float>()
        val height = 60.dp
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    modifier = Modifier.preferredHeight(height).testTag("textField"),
                    value = "",
                    onValueChange = {},
                    label = {},
                    placeholder = {
                        Text(text = "placeholder", modifier = Modifier.onPositioned {
                            placeholderPosition.value = it.globalPosition
                            placeholderSize.value = it.size
                            placeholderBaseline.value =
                                it[FirstBaseline]!!.toFloat() + placeholderPosition.value!!.y
                        })
                    }
                )
            }
        }
        // click to focus
        clickAndAdvanceClock("textField", 200)

        testRule.runOnIdleComposeWithDensity {
            // size
            assertThat(placeholderSize.value).isNotNull()
            assertThat(placeholderSize.value?.height).isGreaterThan(0)
            assertThat(placeholderSize.value?.width).isGreaterThan(0)
            // centered position
            assertThat(placeholderPosition.value?.x).isEqualTo(
                ExpectedPadding.toIntPx().toFloat()
            )
            assertThat(placeholderPosition.value?.y).isEqualTo(
                ((height.toIntPx() - placeholderSize.value!!.height) / 2f).roundToInt().toFloat()
            )
        }
    }

    @Test
    fun testNoPlaceholder_whenInputNotEmpty() {
        val placeholderSize = Ref<IntSize>()
        val placeholderPosition = Ref<Offset>()
        testRule.setMaterialContent {
            Box {
                FilledTextField(
                    modifier = Modifier.testTag("textField"),
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
            FilledTextField(
                modifier = Modifier.testTag("textField"),
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

        // click to focus
        findByTag("textField").doClick()
    }

    @Test
    fun testTrailingAndLeading_sizeAndPosition() {
        val textFieldHeight = 60.dp
        val textFieldWidth = 300.dp
        val size = 30.dp
        val leadingPosition = Ref<Offset>()
        val leadingSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()

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
            assertThat(leadingSize.value).isEqualTo(IntSize(size.toIntPx(), size.toIntPx()))
            assertThat(leadingPosition.value?.x).isEqualTo(IconPadding.toIntPx().toFloat())
            assertThat(leadingPosition.value?.y).isEqualTo(
                ((textFieldHeight.toIntPx() - leadingSize.value!!.height) / 2f).roundToInt()
                    .toFloat()
            )
            // trailing
            assertThat(trailingSize.value).isEqualTo(IntSize(size.toIntPx(), size.toIntPx()))
            assertThat(trailingPosition.value?.x).isEqualTo(
                (textFieldWidth.toIntPx() - IconPadding.toIntPx() - trailingSize.value!!.width)
                    .toFloat()
            )
            assertThat(trailingPosition.value?.y)
                .isEqualTo(((textFieldHeight.toIntPx() - trailingSize.value!!.height) / 2f)
                    .roundToInt().toFloat()
                )
        }
    }

    @Test
    fun testLabelPositionX_initial_withTrailingAndLeading() {
        val height = 60.dp
        val iconSize = 30.dp
        val labelPosition = Ref<Offset>()
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
                (ExpectedPadding.toIntPx() + IconPadding.toIntPx() + iconSize.toIntPx())
                    .toFloat()
            )
        }
    }

    @Test
    fun testLabelPositionX_initial_withEmptyTrailingAndLeading() {
        val height = 60.dp
        val labelPosition = Ref<Offset>()
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
                ExpectedPadding.toIntPx().toFloat()
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

    @Test
    fun testImeActionAndKeyboardTypePropagatedDownstream() {
        val textInputService = mock<TextInputService>()
        testRule.setContent {
            Providers(
                TextInputServiceAmbient provides textInputService
            ) {
                var text = state { TextFieldValue("") }
                FilledTextField(
                    modifier = Modifier.testTag("textField"),
                    value = text.value,
                    onValueChange = { text.value = it },
                    label = {},
                    imeAction = ImeAction.Go,
                    keyboardType = KeyboardType.Email
                )
            }
        }

        clickAndAdvanceClock("textField", 200)

        runOnIdleCompose {
            verify(textInputService, atLeastOnce()).startInput(
                initModel = any(),
                keyboardType = eq(KeyboardType.Email),
                imeAction = eq(ImeAction.Go),
                onEditCommand = any(),
                onImeActionPerformed = any()
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testVisualTransformationPropagated() {
        testRule.setMaterialContent {
            FilledTextField(
                modifier = Modifier.testTag("textField"),
                value = "qwerty",
                onValueChange = {},
                label = {},
                visualTransformation = PasswordVisualTransformation('\u0020'),
                backgroundColor = Color.White,
                shape = RectangleShape
            )
        }

        findByTag("textField")
            .captureToBitmap()
            .assertShape(
                density = testRule.density,
                backgroundColor = Color.White,
                shapeColor = Color.White,
                shape = RectangleShape,
                // avoid elevation artifacts
                shapeOverlapPixelCount = with(testRule.density) { 3.dp.toPx() }
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun test_alphaNotSet_toBackgroundColorAndTransparentColors() {
        val latch = CountDownLatch(1)

        testRule.setMaterialContent {
            Stack(Modifier.drawBackground(Color.White)) {
                FilledTextField(
                    modifier = Modifier.testTag("textField"),
                    value = "",
                    onValueChange = {},
                    label = {},
                    shape = RectangleShape,
                    backgroundColor = Color.Blue,
                    activeColor = Color.Transparent,
                    inactiveColor = Color.Transparent,
                    onFocusChange = { focused ->
                        if (focused) latch.countDown()
                    }
                )
            }
        }

        val expectedColor = Color.Blue.copy(alpha = 0.12f).compositeOver(Color.White)

        findByTag("textField")
            .captureToBitmap()
            .assertShape(
                density = testRule.density,
                backgroundColor = Color.White,
                shapeColor = expectedColor,
                shape = RectangleShape,
                // avoid elevation artifacts
                shapeOverlapPixelCount = with(testRule.density) { 1.dp.toPx() }
            )

        findByTag("textField").doClick()
        assert(latch.await(1, TimeUnit.SECONDS))

        findByTag("textField")
            .captureToBitmap()
            .assertShape(
                density = testRule.density,
                backgroundColor = Color.White,
                shapeColor = expectedColor,
                shape = RectangleShape,
                // avoid elevation artifacts
                shapeOverlapPixelCount = with(testRule.density) { 1.dp.toPx() }
            )
    }

    @Test
    fun testOnTextInputStartedCallback() {
        var controller: SoftwareKeyboardController? = null

        testRule.setMaterialContent {
            FilledTextField(
                modifier = Modifier.testTag("textField"),
                value = "",
                onValueChange = {},
                label = {},
                onTextInputStarted = {
                    controller = it
                }
            )
        }
        assertThat(controller).isNull()

        findByTag("textField")
            .doClick()

        runOnIdleCompose {
            assertThat(controller).isNotNull()
        }
    }

    @Test
    fun testImeActionCallback_withSoftwareKeyboardController() {
        var controller: SoftwareKeyboardController? = null

        testRule.setMaterialContent {
            FilledTextField(
                modifier = Modifier.testTag("textField"),
                value = "",
                onValueChange = {},
                label = {},
                imeAction = ImeAction.Go,
                onImeActionPerformed = { _, softwareKeyboardController ->
                    controller = softwareKeyboardController
                }
            )
        }
        assertThat(controller).isNull()

        findByTag("textField")
            .doSendImeAction()

        runOnIdleCompose {
            assertThat(controller).isNotNull()
        }
    }

    @Test
    fun textField_scrollable_withLongInput() {
        val scrollerPosition = TextFieldScrollerPosition()
        testRule.setContent {
            Stack {
                TextFieldScroller(
                    remember { scrollerPosition },
                    Modifier.fillMaxWidth().preferredHeight(40.dp)
                ) {
                    TextField(
                        value = TextFieldValue(LONG_TEXT),
                        onValueChange = {}
                    )
                }
            }
        }

        runOnIdleCompose {
            assertThat(scrollerPosition.maximum).isLessThan(Float.POSITIVE_INFINITY)
            assertThat(scrollerPosition.maximum).isGreaterThan(0f)
        }
    }

    @Test
    fun textField_notScrollable_withShortInput() {
        val text = "text"
        val scrollerPosition = TextFieldScrollerPosition()
        testRule.setContent {
            Stack {
                TextFieldScroller(
                    remember { scrollerPosition },
                    Modifier.fillMaxWidth().preferredHeight(100.dp)
                ) {
                    TextField(
                        value = TextFieldValue(text),
                        onValueChange = {}
                    )
                }
            }
        }

        runOnIdleCompose {
            assertThat(scrollerPosition.maximum).isEqualTo(0f)
        }
    }

    @Test
    fun textField_scrolledAndClipped() {
        val tag = "testTag"
        val scrollerPosition = TextFieldScrollerPosition()

        val parentSize = 200
        val textFieldSize = 50

        with(testRule.density) {
            testRule.setContent {
                Stack(Modifier
                    .preferredSize(parentSize.toDp())
                    .drawBackground(Color.White)
                    .testTag(tag)
                ) {
                    TextFieldScroller(
                        remember { scrollerPosition },
                        Modifier.preferredSize(textFieldSize.toDp())
                    ) {
                        TextField(
                            value = TextFieldValue(LONG_TEXT),
                            onValueChange = {}
                        )
                    }
                }
            }
        }

        runOnIdleCompose {}

        findByTag(tag)
            .captureToBitmap()
            .assertPixels(expectedSize = IntSize(parentSize, parentSize)) { position ->
                if (position.x > textFieldSize && position.y > textFieldSize) Color.White else null
            }
    }

    @Test
    fun textField_swipe_whenLongInput() {
        val tag = "testTag"
        val scrollerPosition = TextFieldScrollerPosition()

        testRule.setContent {
            Stack {
                TextFieldScroller(
                    remember { scrollerPosition },
                    Modifier.fillMaxWidth().preferredHeight(40.dp).testTag(tag)
                ) {
                    TextField(
                        value = TextFieldValue(LONG_TEXT),
                        onValueChange = {}
                    )
                }
            }
        }

        runOnIdleCompose {
            assertThat(scrollerPosition.current).isEqualTo(0f)
        }

        findByTag(tag)
            .doGesture { sendSwipeDown() }

        val firstSwipePosition = runOnIdleCompose {
            scrollerPosition.current
        }
        assertThat(firstSwipePosition).isGreaterThan(0f)

        findByTag(tag)
            .doGesture { sendSwipeUp() }
        runOnIdleCompose {
            assertThat(scrollerPosition.current).isLessThan(firstSwipePosition)
        }
    }

    private fun clickAndAdvanceClock(tag: String, time: Long) {
        testRule.clockTestRule.pauseClock()
        findByTag(tag).doClick()
        runOnIdleCompose { }
        testRule.clockTestRule.advanceClock(time)
    }
}