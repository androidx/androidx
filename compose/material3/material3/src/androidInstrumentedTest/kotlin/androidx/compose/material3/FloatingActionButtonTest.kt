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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.tokens.ExtendedFabPrimaryTokens
import androidx.compose.material3.tokens.FabBaselineTokens
import androidx.compose.material3.tokens.FabLargeTokens
import androidx.compose.material3.tokens.FabSmallTokens
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FloatingActionButtonTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun fabDefaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FloatingActionButton(modifier = Modifier.testTag("myButton"), onClick = {}) {
                    Icon(Icons.Filled.Favorite, null)
                }
            }
        }

        rule
            .onNodeWithTag("myButton")
            .assertIsEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun extendedFabFindByTextAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "myButton"

        rule.setMaterialContent(lightColorScheme()) {
            Box { ExtendedFloatingActionButton(onClick = onClick, content = { Text(text) }) }
        }

        rule.onNodeWithText(text).performClick()

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
    }

    @Test
    fun fabHasSizeFromSpec() {
        rule
            .setMaterialContentForSizeAssertions {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Filled.Favorite, null, modifier = Modifier.testTag("icon"))
                }
            }
            .assertIsSquareWithSize(FabBaselineTokens.ContainerHeight)

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertHeightIsEqualTo(FabBaselineTokens.IconSize)
            .assertWidthIsEqualTo(FabBaselineTokens.IconSize)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun smallFabHasSizeFromSpec() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    SmallFloatingActionButton(onClick = {}) {
                        Icon(Icons.Filled.Favorite, null, modifier = Modifier.testTag("icon"))
                    }
                }
            }
            // Expecting the size to be equal to the token size.
            .assertIsSquareWithSize(FabSmallTokens.ContainerHeight)

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertHeightIsEqualTo(FabSmallTokens.IconSize)
            .assertWidthIsEqualTo(FabSmallTokens.IconSize)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun smallFabHasMinTouchTarget() {
        rule
            .setMaterialContentForSizeAssertions {
                SmallFloatingActionButton(onClick = {}) { Icon(Icons.Filled.Favorite, null) }
            }
            // Expecting the size to be equal to the minimum touch target.
            .assertTouchWidthIsEqualTo(48.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun largeFabHasSizeFromSpec() {
        rule
            .setMaterialContentForSizeAssertions {
                LargeFloatingActionButton(onClick = {}) {
                    Icon(
                        Icons.Filled.Favorite,
                        null,
                        modifier =
                            Modifier.size(FloatingActionButtonDefaults.LargeIconSize)
                                .testTag("icon"),
                    )
                }
            }
            .assertIsSquareWithSize(FabLargeTokens.ContainerHeight)

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertHeightIsEqualTo(FloatingActionButtonDefaults.LargeIconSize)
            .assertWidthIsEqualTo(FloatingActionButtonDefaults.LargeIconSize)
    }

    @Test
    fun extendedFabLongTextHasHeightFromSpec() {
        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag("FAB"),
                text = { Text("Extended FAB Text") },
                icon = { Icon(Icons.Filled.Favorite, null) },
                onClick = {},
            )
        }

        rule
            .onNodeWithTag("FAB")
            .assertHeightIsEqualTo(ExtendedFabPrimaryTokens.ContainerHeight)
            .assertWidthIsAtLeast(FabBaselineTokens.ContainerHeight)
    }

    @Test
    fun extendedFabShortTextHasMinimumSizeFromSpec() {
        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(
                modifier = Modifier.testTag("FAB"),
                onClick = {},
                content = { Text(".") },
            )
        }

        rule
            .onNodeWithTag("FAB")
            .assertHeightIsEqualTo(ExtendedFabPrimaryTokens.ContainerHeight)
            .assertWidthIsEqualTo(80.dp)
    }

    @Test
    fun fabHasCorrectTextStyle() {
        var fontFamily: FontFamily? = null
        var fontWeight: FontWeight? = null
        var fontSize: TextUnit? = null
        var lineHeight: TextUnit? = null
        var letterSpacing: TextUnit? = null
        var expectedTextStyle: TextStyle? = null

        rule.setMaterialContent(lightColorScheme()) {
            FloatingActionButton(onClick = {}) {
                Icon(Icons.Filled.Favorite, null)
                Text(
                    "Normal FAB with Text",
                    onTextLayout = {
                        fontFamily = it.layoutInput.style.fontFamily
                        fontWeight = it.layoutInput.style.fontWeight
                        fontSize = it.layoutInput.style.fontSize
                        lineHeight = it.layoutInput.style.lineHeight
                        letterSpacing = it.layoutInput.style.letterSpacing
                    },
                )
            }
            expectedTextStyle = ExtendedFabPrimaryTokens.LabelTextFont.value
        }
        rule.runOnIdle {
            assertThat(fontFamily).isEqualTo(expectedTextStyle!!.fontFamily)
            assertThat(fontWeight).isEqualTo(expectedTextStyle!!.fontWeight)
            assertThat(fontSize).isEqualTo(expectedTextStyle!!.fontSize)
            assertThat(lineHeight).isEqualTo(expectedTextStyle!!.lineHeight)
            assertThat(letterSpacing).isEqualTo(expectedTextStyle!!.letterSpacing)
        }
    }

    @Test
    fun extendedFabHasCorrectTextStyle() {
        var fontFamily: FontFamily? = null
        var fontWeight: FontWeight? = null
        var fontSize: TextUnit? = null
        var lineHeight: TextUnit? = null
        var letterSpacing: TextUnit? = null
        var expectedTextStyle: TextStyle? = null

        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(onClick = {}) {
                Text(
                    "Extended FAB",
                    onTextLayout = {
                        fontFamily = it.layoutInput.style.fontFamily
                        fontWeight = it.layoutInput.style.fontWeight
                        fontSize = it.layoutInput.style.fontSize
                        lineHeight = it.layoutInput.style.lineHeight
                        letterSpacing = it.layoutInput.style.letterSpacing
                    },
                )
            }
            expectedTextStyle = ExtendedFabPrimaryTokens.LabelTextFont.value
        }
        rule.runOnIdle {
            assertThat(fontFamily).isEqualTo(expectedTextStyle!!.fontFamily)
            assertThat(fontWeight).isEqualTo(expectedTextStyle!!.fontWeight)
            assertThat(fontSize).isEqualTo(expectedTextStyle!!.fontSize)
            assertThat(lineHeight).isEqualTo(expectedTextStyle!!.lineHeight)
            assertThat(letterSpacing).isEqualTo(expectedTextStyle!!.letterSpacing)
        }
    }

    @Test
    fun fabWeightModifier() {
        var item1Bounds = Rect(0f, 0f, 0f, 0f)
        var buttonBounds = Rect(0f, 0f, 0f, 0f)
        rule.setMaterialContent(lightColorScheme()) {
            Column {
                Spacer(
                    Modifier.requiredSize(10.dp).weight(1f).onGloballyPositioned {
                        item1Bounds = it.boundsInRoot()
                    }
                )

                FloatingActionButton(
                    onClick = {},
                    modifier =
                        Modifier.weight(1f).onGloballyPositioned {
                            buttonBounds = it.boundsInRoot()
                        },
                ) {
                    Text("Button")
                }

                Spacer(Modifier.requiredSize(10.dp).weight(1f))
            }
        }

        assertThat(item1Bounds.top).isNotEqualTo(0f)
        assertThat(buttonBounds.left).isEqualTo(0f)
    }

    @Test
    fun contentIsWrappedAndCentered() {
        var buttonCoordinates: LayoutCoordinates? = null
        var contentCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FloatingActionButton({}, Modifier.onGloballyPositioned { buttonCoordinates = it }) {
                    Box(Modifier.size(2.dp).onGloballyPositioned { contentCoordinates = it })
                }
            }
        }

        rule.runOnIdle {
            val buttonBounds = buttonCoordinates!!.boundsInRoot()
            val contentBounds = contentCoordinates!!.boundsInRoot()
            assertThat(contentBounds.width).isLessThan(buttonBounds.width)
            assertThat(contentBounds.height).isLessThan(buttonBounds.height)
            with(rule.density) {
                assertThat(contentBounds.width).isEqualTo(2.dp.roundToPx().toFloat())
                assertThat(contentBounds.height).isEqualTo(2.dp.roundToPx().toFloat())
            }
            assertWithinOnePixel(buttonBounds.center, contentBounds.center)
        }
    }

    @Test
    fun extendedFabTextIsWrappedAndCentered() {
        var buttonCoordinates: LayoutCoordinates? = null
        var contentCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ExtendedFloatingActionButton(
                    onClick = {},
                    modifier = Modifier.onGloballyPositioned { buttonCoordinates = it },
                ) {
                    Box(Modifier.size(2.dp).onGloballyPositioned { contentCoordinates = it })
                }
            }
        }

        rule.runOnIdle {
            val buttonBounds = buttonCoordinates!!.boundsInRoot()
            val contentBounds = contentCoordinates!!.boundsInRoot()
            assertThat(contentBounds.width).isLessThan(buttonBounds.width)
            assertThat(contentBounds.height).isLessThan(buttonBounds.height)
            with(rule.density) {
                assertThat(contentBounds.width).isEqualTo(2.dp.roundToPx().toFloat())
                assertThat(contentBounds.height).isEqualTo(2.dp.roundToPx().toFloat())
            }
            assertWithinOnePixel(buttonBounds.center, contentBounds.center)
        }
    }

    @Test
    fun extendedFabTextAndIconArePositionedCorrectly() {
        var buttonCoordinates: LayoutCoordinates? = null
        var textCoordinates: LayoutCoordinates? = null
        var iconCoordinates: LayoutCoordinates? = null
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ExtendedFloatingActionButton(
                    text = {
                        Box(Modifier.size(2.dp).onGloballyPositioned { textCoordinates = it })
                    },
                    icon = {
                        Box(Modifier.size(10.dp).onGloballyPositioned { iconCoordinates = it })
                    },
                    onClick = {},
                    modifier = Modifier.onGloballyPositioned { buttonCoordinates = it },
                )
            }
        }

        rule.runOnIdle {
            val buttonBounds = buttonCoordinates!!.boundsInRoot()
            val textBounds = textCoordinates!!.boundsInRoot()
            val iconBounds = iconCoordinates!!.boundsInRoot()
            with(rule.density) {
                assertThat(textBounds.width).isEqualTo(2.dp.roundToPx().toFloat())
                assertThat(textBounds.height).isEqualTo(2.dp.roundToPx().toFloat())
                assertThat(iconBounds.width).isEqualTo(10.dp.roundToPx().toFloat())
                assertThat(iconBounds.height).isEqualTo(10.dp.roundToPx().toFloat())

                assertWithinOnePixel(buttonBounds.center.y, iconBounds.center.y)
                assertWithinOnePixel(buttonBounds.center.y, textBounds.center.y)

                // Assert expanded fab icon has 16.dp of padding.
                assertThat(iconBounds.left - buttonBounds.left)
                    .isEqualTo(16.dp.roundToPx().toFloat())

                val halfPadding = 6.dp.roundToPx().toFloat()
                assertWithinOnePixel(
                    iconBounds.center.x + iconBounds.width / 2 + halfPadding,
                    textBounds.center.x - textBounds.width / 2 - halfPadding,
                )
                // Assert that text and icon have 12.dp padding between them.
                assertThat(textBounds.left - iconBounds.right)
                    .isEqualTo(12.dp.roundToPx().toFloat())
            }
        }
    }

    @Test
    fun expandedExtendedFabTextAndIconHaveSizeFromSpecAndVisible() {
        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(
                expanded = true,
                onClick = {},
                icon = { Icon(Icons.Filled.Favorite, "Add", modifier = Modifier.testTag("icon")) },
                text = { Text(text = "FAB", modifier = Modifier.testTag("text")) },
                modifier = Modifier.testTag("FAB"),
            )
        }

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertHeightIsEqualTo(ExtendedFabPrimaryTokens.IconSize)
            .assertWidthIsEqualTo(ExtendedFabPrimaryTokens.IconSize)

        rule
            .onNodeWithTag("FAB")
            .assertHeightIsEqualTo(ExtendedFabPrimaryTokens.ContainerHeight)
            .assertWidthIsAtLeast(80.dp)

        rule.onNodeWithTag("text", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("icon", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun collapsedExtendedFabTextAndIconHaveSizeFromSpecAndTextNotVisible() {
        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(
                expanded = false,
                onClick = {},
                icon = { Icon(Icons.Filled.Favorite, "Add", modifier = Modifier.testTag("icon")) },
                text = { Text(text = "FAB", modifier = Modifier.testTag("text")) },
                modifier = Modifier.testTag("FAB"),
            )
        }

        rule.onNodeWithTag("FAB").assertIsSquareWithSize(FabBaselineTokens.ContainerHeight)

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertHeightIsEqualTo(ExtendedFabPrimaryTokens.IconSize)
            .assertWidthIsEqualTo(ExtendedFabPrimaryTokens.IconSize)

        rule.onNodeWithTag("text", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("icon", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun extendedFabAnimates() {
        rule.mainClock.autoAdvance = false

        var expanded by mutableStateOf(true)
        rule.setMaterialContent(lightColorScheme()) {
            ExtendedFloatingActionButton(
                expanded = expanded,
                onClick = {},
                icon = { Icon(Icons.Filled.Favorite, "Add", modifier = Modifier.testTag("icon")) },
                text = { Text(text = "FAB", modifier = Modifier.testTag("text")) },
                modifier = Modifier.testTag("FAB"),
            )
        }

        rule
            .onNodeWithTag("FAB")
            .assertHeightIsEqualTo(ExtendedFabPrimaryTokens.ContainerHeight)
            .assertWidthIsAtLeast(80.dp)

        rule.runOnIdle { expanded = false }
        rule.mainClock.advanceTimeBy(200)

        rule
            .onNodeWithTag("FAB")
            .assertIsSquareWithSize(FabBaselineTokens.ContainerHeight)
            .assertHeightIsEqualTo(FabBaselineTokens.ContainerHeight)
            .assertWidthIsEqualTo(FabBaselineTokens.ContainerWidth)
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun expandedLargeExtendedFabTextAndIconHaveSizeFromSpecAndVisible() {
        rule.setMaterialContent(lightColorScheme()) {
            LargeExtendedFloatingActionButton(
                expanded = true,
                onClick = {},
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        "Add",
                        modifier =
                            Modifier.size(FloatingActionButtonDefaults.LargeIconSize)
                                .testTag("icon"),
                    )
                },
                text = { Text(text = "FAB", modifier = Modifier.testTag("text")) },
                modifier = Modifier.testTag("FAB"),
            )
        }

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertHeightIsEqualTo(FloatingActionButtonDefaults.LargeIconSize)
            .assertWidthIsEqualTo(FloatingActionButtonDefaults.LargeIconSize)

        rule.onNodeWithTag("FAB").assertHeightIsEqualTo(96.dp).assertWidthIsAtLeast(112.dp)

        rule.onNodeWithTag("text", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("icon", useUnmergedTree = true).assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun collapsedLargeExtendedFabTextAndIconHaveSizeFromSpecAndTextNotVisible() {
        rule.setMaterialContent(lightColorScheme()) {
            LargeExtendedFloatingActionButton(
                expanded = false,
                onClick = {},
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        "Add",
                        modifier =
                            Modifier.size(FloatingActionButtonDefaults.LargeIconSize)
                                .testTag("icon"),
                    )
                },
                text = { Text(text = "FAB", modifier = Modifier.testTag("text")) },
                modifier = Modifier.testTag("FAB"),
            )
        }

        rule.onNodeWithTag("FAB").assertIsSquareWithSize(96.dp)

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertHeightIsEqualTo(FloatingActionButtonDefaults.LargeIconSize)
            .assertWidthIsEqualTo(FloatingActionButtonDefaults.LargeIconSize)

        rule.onNodeWithTag("text", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("icon", useUnmergedTree = true).assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun largeExtendedFabAnimates() {
        rule.mainClock.autoAdvance = false

        var expanded by mutableStateOf(true)
        rule.setMaterialContent(lightColorScheme()) {
            LargeExtendedFloatingActionButton(
                expanded = expanded,
                onClick = {},
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        "Add",
                        modifier =
                            Modifier.size(FloatingActionButtonDefaults.LargeIconSize)
                                .testTag("icon"),
                    )
                },
                text = { Text(text = "FAB", modifier = Modifier.testTag("text")) },
                modifier = Modifier.testTag("FAB"),
            )
        }

        rule.onNodeWithTag("FAB").assertHeightIsEqualTo(96.dp).assertWidthIsAtLeast(112.dp)

        rule.runOnIdle { expanded = false }
        rule.mainClock.advanceTimeBy(400)

        rule
            .onNodeWithTag("FAB")
            .assertIsSquareWithSize(96.dp)
            .assertHeightIsEqualTo(96.dp)
            .assertWidthIsEqualTo(96.dp)
    }

    @Test
    fun floatingActionButtonElevation_newInteraction() {
        val interactionSource = MutableInteractionSource()
        val defaultElevation = 1.dp
        val pressedElevation = 2.dp
        val hoveredElevation = 3.dp
        val focusedElevation = 4.dp
        var tonalElevation: Dp = Dp.Unspecified
        lateinit var shadowElevation: State<Dp>

        rule.setMaterialContent(lightColorScheme()) {
            val fabElevation =
                FloatingActionButtonDefaults.elevation(
                    defaultElevation = defaultElevation,
                    pressedElevation = pressedElevation,
                    hoveredElevation = hoveredElevation,
                    focusedElevation = focusedElevation,
                )

            tonalElevation = fabElevation.tonalElevation()
            shadowElevation = fabElevation.shadowElevation(interactionSource)
        }

        rule.runOnIdle {
            assertThat(tonalElevation).isEqualTo(defaultElevation)
            assertThat(shadowElevation.value).isEqualTo(defaultElevation)
        }

        rule.runOnIdle { interactionSource.tryEmit(PressInteraction.Press(Offset.Zero)) }

        rule.runOnIdle {
            assertThat(tonalElevation).isEqualTo(defaultElevation)
            assertThat(shadowElevation.value).isEqualTo(pressedElevation)
        }
    }

    @Test
    fun floatingActionButtonElevation_newValue() {
        val interactionSource = MutableInteractionSource()
        var defaultElevation by mutableStateOf(1.dp)
        val pressedElevation = 2.dp
        val hoveredElevation = 3.dp
        val focusedElevation = 4.dp
        var tonalElevation: Dp = Dp.Unspecified
        lateinit var shadowElevation: State<Dp>

        rule.setMaterialContent(lightColorScheme()) {
            val fabElevation =
                FloatingActionButtonDefaults.elevation(
                    defaultElevation = defaultElevation,
                    pressedElevation = pressedElevation,
                    hoveredElevation = hoveredElevation,
                    focusedElevation = focusedElevation,
                )

            tonalElevation = fabElevation.tonalElevation()
            shadowElevation = fabElevation.shadowElevation(interactionSource)
        }

        rule.runOnIdle {
            assertThat(tonalElevation).isEqualTo(defaultElevation)
            assertThat(shadowElevation.value).isEqualTo(defaultElevation)
        }

        rule.runOnIdle { defaultElevation = 5.dp }

        rule.runOnIdle {
            assertThat(tonalElevation).isEqualTo(5.dp)
            assertThat(shadowElevation.value).isEqualTo(5.dp)
        }
    }

    @Test
    fun floatingActionButtonElevation_newValueDuringInteraction() {
        val interactionSource = MutableInteractionSource()
        val defaultElevation = 1.dp
        var pressedElevation by mutableStateOf(2.dp)
        val hoveredElevation = 3.dp
        val focusedElevation = 4.dp
        var tonalElevation: Dp = Dp.Unspecified
        lateinit var shadowElevation: State<Dp>

        rule.setMaterialContent(lightColorScheme()) {
            val fabElevation =
                FloatingActionButtonDefaults.elevation(
                    defaultElevation = defaultElevation,
                    pressedElevation = pressedElevation,
                    hoveredElevation = hoveredElevation,
                    focusedElevation = focusedElevation,
                )

            tonalElevation = fabElevation.tonalElevation()
            shadowElevation = fabElevation.shadowElevation(interactionSource)
        }

        rule.runOnIdle {
            assertThat(tonalElevation).isEqualTo(defaultElevation)
            assertThat(shadowElevation.value).isEqualTo(defaultElevation)
        }

        rule.runOnIdle { interactionSource.tryEmit(PressInteraction.Press(Offset.Zero)) }

        rule.runOnIdle {
            assertThat(tonalElevation).isEqualTo(defaultElevation)
            assertThat(shadowElevation.value).isEqualTo(pressedElevation)
        }

        rule.runOnIdle { pressedElevation = 5.dp }

        // We are still pressed, so we should now show the updated value for the pressed state
        rule.runOnIdle {
            assertThat(tonalElevation).isEqualTo(defaultElevation)
            assertThat(shadowElevation.value).isEqualTo(5.dp)
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun animateFloatingActionButton_hideBottomEnd_scalesAndFadesCorrectly() {
        val visible = mutableStateOf(true)

        rule.mainClock.autoAdvance = false

        rule.setMaterialContent(lightColorScheme()) {
            Box(
                modifier =
                    Modifier.background(Color.Red)
                        .size(100.dp)
                        .testTag(AnimateFloatingActionButtonTestTag)
            ) {
                Box(
                    modifier =
                        Modifier.animateFloatingActionButton(
                                visible = visible.value,
                                alignment = Alignment.BottomEnd,
                                targetScale = 0.2f,
                                scaleAnimationSpec = tween(100, easing = LinearEasing),
                                alphaAnimationSpec = tween(100, easing = LinearEasing),
                            )
                            .background(Color.Blue, CircleShape)
                            .fillMaxSize()
                )
            }
        }

        rule.runOnIdle { visible.value = false }

        // Wait for initial recomposition / measure after state change
        rule.mainClock.advanceTimeByFrame()

        // Run half of the animation
        rule.mainClock.advanceTimeBy(50)

        rule
            .onNodeWithTag(AnimateFloatingActionButtonTestTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = CircleShape,
                shapeColor =
                    Color(
                        ColorUtils.compositeColors(
                            Color.Blue.copy(alpha = 0.5f).toArgb(),
                            Color.Red.toArgb(),
                        )
                    ),
                backgroundColor = Color.Red,
                backgroundSize = with(rule.density) { DpSize(60.dp, 60.dp).toSize() },
                shapeSize = with(rule.density) { DpSize(60.dp, 60.dp).toSize() },
                shapeCenter = with(rule.density) { Offset(70.dp.toPx(), 70.dp.toPx()) },
                backgroundCenter = with(rule.density) { Offset(70.dp.toPx(), 70.dp.toPx()) },
                antiAliasingGap = with(rule.density) { 3.dp.toPx() },
            )
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun animateFloatingActionButton_hideCenter_scalesAndFadesCorrectly() {
        val visible = mutableStateOf(true)

        rule.mainClock.autoAdvance = false

        rule.setMaterialContent(lightColorScheme()) {
            Box(
                modifier =
                    Modifier.background(Color.Red)
                        .size(100.dp)
                        .testTag(AnimateFloatingActionButtonTestTag)
            ) {
                Box(
                    modifier =
                        Modifier.animateFloatingActionButton(
                                visible = visible.value,
                                alignment = Alignment.Center,
                                targetScale = 0.2f,
                                scaleAnimationSpec = tween(100, easing = LinearEasing),
                                alphaAnimationSpec = tween(100, easing = LinearEasing),
                            )
                            .background(Color.Blue, CircleShape)
                            .fillMaxSize()
                )
            }
        }

        rule.runOnIdle { visible.value = false }

        // Wait for initial recomposition / measure after state change
        rule.mainClock.advanceTimeByFrame()

        // Run half of the animation
        rule.mainClock.advanceTimeBy(50)

        rule
            .onNodeWithTag(AnimateFloatingActionButtonTestTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = CircleShape,
                shapeColor =
                    Color(
                        ColorUtils.compositeColors(
                            Color.Blue.copy(alpha = 0.5f).toArgb(),
                            Color.Red.toArgb(),
                        )
                    ),
                backgroundColor = Color.Red,
                backgroundSize = with(rule.density) { DpSize(60.dp, 60.dp).toSize() },
                shapeSize = with(rule.density) { DpSize(60.dp, 60.dp).toSize() },
                shapeCenter = with(rule.density) { Offset(50.dp.toPx(), 50.dp.toPx()) },
                backgroundCenter = with(rule.density) { Offset(50.dp.toPx(), 50.dp.toPx()) },
                antiAliasingGap = with(rule.density) { 2.dp.toPx() },
            )
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun animateFloatingActionButton_hideTopStart_scalesAndFadesCorrectly() {
        val visible = mutableStateOf(true)

        rule.mainClock.autoAdvance = false

        rule.setMaterialContent(lightColorScheme()) {
            Box(
                modifier =
                    Modifier.background(Color.Red)
                        .size(100.dp)
                        .testTag(AnimateFloatingActionButtonTestTag)
            ) {
                Box(
                    modifier =
                        Modifier.animateFloatingActionButton(
                                visible = visible.value,
                                alignment = Alignment.TopStart,
                                targetScale = 0.2f,
                                scaleAnimationSpec = tween(100, easing = LinearEasing),
                                alphaAnimationSpec = tween(100, easing = LinearEasing),
                            )
                            .background(Color.Blue, CircleShape)
                            .fillMaxSize()
                )
            }
        }

        rule.runOnIdle { visible.value = false }

        // Wait for initial recomposition / measure after state change
        rule.mainClock.advanceTimeByFrame()

        // Run half of the animation
        rule.mainClock.advanceTimeBy(50)

        rule
            .onNodeWithTag(AnimateFloatingActionButtonTestTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = CircleShape,
                shapeColor =
                    Color(
                        ColorUtils.compositeColors(
                            Color.Blue.copy(alpha = 0.5f).toArgb(),
                            Color.Red.toArgb(),
                        )
                    ),
                backgroundColor = Color.Red,
                backgroundSize = with(rule.density) { DpSize(60.dp, 60.dp).toSize() },
                shapeSize = with(rule.density) { DpSize(60.dp, 60.dp).toSize() },
                shapeCenter = with(rule.density) { Offset(30.dp.toPx(), 30.dp.toPx()) },
                backgroundCenter = with(rule.density) { Offset(30.dp.toPx(), 30.dp.toPx()) },
                antiAliasingGap = with(rule.density) { 2.dp.toPx() },
            )
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun animateFloatingActionButton_show_noScaleOrFadeAfterAnimation() {
        val visible = mutableStateOf(false)

        rule.mainClock.autoAdvance = false

        rule.setMaterialContent(lightColorScheme()) {
            Box(
                modifier =
                    Modifier.background(Color.Red)
                        .size(100.dp)
                        .testTag(AnimateFloatingActionButtonTestTag)
            ) {
                Box(
                    modifier =
                        Modifier.animateFloatingActionButton(
                                visible = visible.value,
                                alignment = Alignment.BottomEnd,
                                targetScale = 0.2f,
                                scaleAnimationSpec = tween(100, easing = LinearEasing),
                                alphaAnimationSpec = tween(100, easing = LinearEasing),
                            )
                            .background(Color.Blue, CircleShape)
                            .fillMaxSize()
                )
            }
        }

        rule.runOnIdle { visible.value = true }

        // Wait for initial recomposition / measure after state change
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        // Run full animation
        rule.mainClock.advanceTimeBy(100)

        rule
            .onNodeWithTag(AnimateFloatingActionButtonTestTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = CircleShape,
                shapeColor = Color.Blue,
                backgroundColor = Color.Red,
                backgroundSize = with(rule.density) { DpSize(100.dp, 100.dp).toSize() },
                shapeSize = with(rule.density) { DpSize(100.dp, 100.dp).toSize() },
                shapeCenter = with(rule.density) { Offset(50.dp.toPx(), 50.dp.toPx()) },
                backgroundCenter = with(rule.density) { Offset(50.dp.toPx(), 50.dp.toPx()) },
                antiAliasingGap = with(rule.density) { 2.dp.toPx() },
            )
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun animateFloatingActionButton_show_noScaleOrFadeBeforeAnimation() {
        rule.mainClock.autoAdvance = false

        rule.setMaterialContent(lightColorScheme()) {
            Box(
                modifier =
                    Modifier.background(Color.Red)
                        .size(100.dp)
                        .testTag(AnimateFloatingActionButtonTestTag)
            ) {
                Box(
                    modifier =
                        Modifier.animateFloatingActionButton(
                                visible = true,
                                alignment = Alignment.BottomEnd,
                                targetScale = 0.2f,
                                scaleAnimationSpec = tween(100, easing = LinearEasing),
                                alphaAnimationSpec = tween(100, easing = LinearEasing),
                            )
                            .background(Color.Blue, CircleShape)
                            .fillMaxSize()
                )
            }
        }

        rule
            .onNodeWithTag(AnimateFloatingActionButtonTestTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = CircleShape,
                shapeColor = Color.Blue,
                backgroundColor = Color.Red,
                backgroundSize = with(rule.density) { DpSize(100.dp, 100.dp).toSize() },
                shapeSize = with(rule.density) { DpSize(100.dp, 100.dp).toSize() },
                shapeCenter = with(rule.density) { Offset(50.dp.toPx(), 50.dp.toPx()) },
                backgroundCenter = with(rule.density) { Offset(50.dp.toPx(), 50.dp.toPx()) },
                antiAliasingGap = with(rule.density) { 2.dp.toPx() },
            )
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun animateFloatingActionButton_show_consumesClick() {
        var contentBoxClicked = false
        var fabBoxClicked = false

        rule.setMaterialContent(lightColorScheme()) {
            Box(
                modifier =
                    Modifier.background(Color.Red)
                        .size(100.dp)
                        .testTag(AnimateFloatingActionButtonTestTag)
            ) {
                Box(
                    modifier =
                        Modifier.background(Color.Green)
                            .fillMaxSize()
                            .clickable(onClick = { contentBoxClicked = true })
                )

                Box(
                    modifier =
                        Modifier.animateFloatingActionButton(
                                visible = true,
                                alignment = Alignment.BottomEnd,
                                targetScale = 0.2f,
                                scaleAnimationSpec = tween(100, easing = LinearEasing),
                                alphaAnimationSpec = tween(100, easing = LinearEasing),
                            )
                            .background(Color.Blue)
                            .fillMaxSize()
                            .clickable(onClick = { fabBoxClicked = true })
                )
            }
        }

        rule.onNodeWithTag(AnimateFloatingActionButtonTestTag).performClick()

        assertThat(contentBoxClicked).isFalse()
        assertThat(fabBoxClicked).isTrue()
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun animateFloatingActionButton_hide_doesNotConsumesClick() {
        var contentBoxClicked = false
        var fabBoxClicked = false

        rule.setMaterialContent(lightColorScheme()) {
            Box(
                modifier =
                    Modifier.background(Color.Red)
                        .size(100.dp)
                        .testTag(AnimateFloatingActionButtonTestTag)
            ) {
                Box(
                    modifier =
                        Modifier.background(Color.Green)
                            .fillMaxSize()
                            .clickable(onClick = { contentBoxClicked = true })
                )

                Box(
                    modifier =
                        Modifier.animateFloatingActionButton(
                                visible = false,
                                alignment = Alignment.BottomEnd,
                                targetScale = 0.2f,
                                scaleAnimationSpec = tween(100, easing = LinearEasing),
                                alphaAnimationSpec = tween(100, easing = LinearEasing),
                            )
                            .background(Color.Blue)
                            .fillMaxSize()
                            .clickable(onClick = { fabBoxClicked = true })
                )
            }
        }

        rule.onNodeWithTag(AnimateFloatingActionButtonTestTag).performClick()

        assertThat(contentBoxClicked).isTrue()
        assertThat(fabBoxClicked).isFalse()
    }
}

private val AnimateFloatingActionButtonTestTag = "AnimateFloatingActionButton"

fun assertWithinOnePixel(expected: Offset, actual: Offset) {
    assertWithinOnePixel(expected.x, actual.x)
    assertWithinOnePixel(expected.y, actual.y)
}

fun assertWithinOnePixel(expected: Float, actual: Float) {
    val diff = abs(expected - actual)
    assertThat(diff).isLessThan(1.1f)
}
