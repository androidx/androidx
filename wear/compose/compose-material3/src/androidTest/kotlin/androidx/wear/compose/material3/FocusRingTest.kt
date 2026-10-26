/*
 * Copyright 2026 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class FocusRingTest {
    @get:Rule val rule = createComposeRule()

    private val interactionSource = MutableInteractionSource()

    @Test
    fun focusRing_drawsOnFocus_withThemeColors() {
        setFocusRingContent()

        emit(FocusInteraction.Focus())

        assertFocusRingColors(
            outerRingColor = ColorScheme().complementaryColor,
            innerRingColor = ColorScheme().background,
        )
    }

    @Test
    fun focusRing_drawsWithLocalRippleConfigurationColors() {
        setFocusRingContent(rippleConfiguration = RingColorsConfiguration)

        emit(FocusInteraction.Focus())

        assertFocusRingColors(outerRingColor = OuterRingColor, innerRingColor = InnerRingColor)
    }

    @Test
    fun focusRing_drawsWithOnlyOuterStrokeColorConfigured_fallsBackToThemeInnerColor() {
        setFocusRingContent(
            rippleConfiguration =
                RippleConfiguration(
                    focus = RippleConfiguration.Focus.InsetRing(outerStrokeColor = OuterRingColor)
                )
        )

        emit(FocusInteraction.Focus())

        assertFocusRingColors(
            outerRingColor = OuterRingColor,
            innerRingColor = ColorScheme().background,
        )
    }

    @Test
    fun focusRing_drawsWithOnlyInnerStrokeColorConfigured_fallsBackToThemeOuterColor() {
        setFocusRingContent(
            rippleConfiguration =
                RippleConfiguration(
                    focus = RippleConfiguration.Focus.InsetRing(innerStrokeColor = InnerRingColor)
                )
        )

        emit(FocusInteraction.Focus())

        assertFocusRingColors(
            outerRingColor = ColorScheme().complementaryColor,
            innerRingColor = InnerRingColor,
        )
    }

    @Test
    fun focusRing_opacityConfiguration_doesNotDrawRing() {
        setFocusRingContent(
            themeConfiguration = { RippleDefaults.OpacityFocusThemeConfiguration },
            enableFocusIndication = {
                LocalRippleThemeConfiguration.current.insetFocusRing != null
            },
        )

        emit(FocusInteraction.Focus())

        assertFocusRingNotDrawn()
    }

    @OptIn(ExperimentalWearComposeMaterial3Api::class)
    @Test
    fun focusRing_flagDisabled_doesNotDrawRing() {
        val originalFlag = WearComposeMaterial3Flags.isInsetFocusRingEnabled
        WearComposeMaterial3Flags.isInsetFocusRingEnabled = false
        try {
            setFocusRingContent(
                enableFocusIndication = {
                    LocalRippleThemeConfiguration.current.insetFocusRing != null
                }
            )

            emit(FocusInteraction.Focus())

            assertFocusRingNotDrawn()
        } finally {
            WearComposeMaterial3Flags.isInsetFocusRingEnabled = originalFlag
        }
    }

    @Test
    fun focusRing_rectangleShape() {
        setFocusRingContent(
            rippleConfiguration = SingleRingColorConfiguration,
            shape = { RectangleShape },
        )

        emit(FocusInteraction.Focus())

        assertFocusRingShape(RectangleShape)
    }

    @Test
    fun focusRing_circleShape() {
        setFocusRingContent(
            rippleConfiguration = SingleRingColorConfiguration,
            shape = { CircleShape },
        )

        emit(FocusInteraction.Focus())

        assertFocusRingShape(CircleShape)
    }

    @Test
    fun focusRing_shapeChangedWhileFocused_updatesRing() {
        var shape: Shape by mutableStateOf(RectangleShape)
        setFocusRingContent(rippleConfiguration = SingleRingColorConfiguration, shape = { shape })
        emit(FocusInteraction.Focus())

        rule.runOnIdle { shape = CircleShape }

        // The ring is updated in place, so it is still drawn without focusing again.
        assertFocusRingShape(CircleShape)
    }

    @Test
    fun focusRing_clearsOnUnfocus() {
        setFocusRingContent()
        val focus = FocusInteraction.Focus()
        emit(focus)

        emit(FocusInteraction.Unfocus(focus))

        assertFocusRingNotDrawn()
    }

    @Test
    fun focusRing_themeInsetRingChanged_drawsWithNewInsetRing() {
        var themeConfiguration by mutableStateOf(RippleDefaults.InsetFocusRingThemeConfiguration)
        setFocusRingContent(
            themeConfiguration = { themeConfiguration },
            rippleConfiguration = RingColorsConfiguration,
        )

        // Widen the inner stroke to cover the whole box, including its center.
        rule.runOnIdle {
            themeConfiguration =
                RippleThemeConfiguration(
                    RippleThemeConfiguration.Focus.InsetRing(
                        outerStrokeInset = DefaultInsetRing.outerStrokeInset,
                        outerStrokeWidth = DefaultInsetRing.outerStrokeWidth,
                        innerStrokeInset = DefaultInsetRing.innerStrokeInset,
                        innerStrokeWidth = BoxSize,
                    )
                )
        }
        emit(FocusInteraction.Focus())

        assertFocusRingColors(
            outerRingColor = OuterRingColor,
            innerRingColor = InnerRingColor,
            centerColor = InnerRingColor,
        )
    }

    @Test
    fun focusRing_themeChangedToOpacity_removesRing() {
        var themeConfiguration by mutableStateOf(RippleDefaults.InsetFocusRingThemeConfiguration)
        setFocusRingContent(themeConfiguration = { themeConfiguration })
        emit(FocusInteraction.Focus())

        rule.runOnIdle { themeConfiguration = RippleDefaults.OpacityFocusThemeConfiguration }

        assertFocusRingNotDrawn()
    }

    @Test
    fun focusRing_fadesOuterStroke_afterInactivityTimeout() {
        setFocusRingContent(rippleConfiguration = RingColorsConfiguration)
        emit(FocusInteraction.Focus())
        assertOuterStrokeFaded(false)

        rule.mainClock.advanceTimeBy(FocusRingFadeTimeout.inWholeMilliseconds)

        assertOuterStrokeFaded(true)
    }

    @Test
    fun focusRing_transparentOuterStroke_remainsTransparentAfterInactivityTimeout() {
        setFocusRingContent(
            rippleConfiguration =
                RippleConfiguration(
                    focus =
                        RippleConfiguration.Focus.InsetRing(
                            outerStrokeColor = Color.Transparent,
                            innerStrokeColor = InnerRingColor,
                        )
                )
        )
        emit(FocusInteraction.Focus())

        // Transparent outer stroke reveals inner stroke color, as it's drawn on top of it.
        assertFocusRingColors(outerRingColor = InnerRingColor, innerRingColor = InnerRingColor)
        rule.mainClock.advanceTimeBy(FocusRingFadeTimeout.inWholeMilliseconds)
        // Transparent outer stroke remains as is after inactivity timeout.
        assertFocusRingColors(outerRingColor = InnerRingColor, innerRingColor = InnerRingColor)
    }

    @Test
    fun focusRing_press_restoresFadedOuterStroke() {
        setFocusRingContent(rippleConfiguration = RingColorsConfiguration)
        emit(FocusInteraction.Focus())
        rule.mainClock.advanceTimeBy(FocusRingFadeTimeout.inWholeMilliseconds)
        assertOuterStrokeFaded(true)

        emit(PressInteraction.Press(Offset.Zero))

        assertOuterStrokeFaded(false)
    }

    @Test
    fun focusRing_heldPress_postponesFadeUntilReleased() {
        setFocusRingContent(rippleConfiguration = RingColorsConfiguration)
        emit(FocusInteraction.Focus())
        val press = PressInteraction.Press(Offset.Zero)
        emit(press)

        rule.mainClock.advanceTimeBy((FocusRingFadeTimeout * 2).inWholeMilliseconds)
        assertOuterStrokeFaded(false)

        emit(PressInteraction.Release(press))
        assertOuterStrokeFaded(false)

        rule.mainClock.advanceTimeBy(FocusRingFadeTimeout.inWholeMilliseconds)
        assertOuterStrokeFaded(true)
    }

    @Test
    fun focusRing_refocus_restoresFadedOuterStroke() {
        setFocusRingContent(rippleConfiguration = RingColorsConfiguration)
        val focus = FocusInteraction.Focus()
        emit(focus)
        rule.mainClock.advanceTimeBy(FocusRingFadeTimeout.inWholeMilliseconds)
        assertOuterStrokeFaded(true)

        emit(FocusInteraction.Unfocus(focus))
        emit(FocusInteraction.Focus())

        assertOuterStrokeFaded(false)
    }

    @SuppressLint("UnusedBoxWithConstraintsScope")
    @Test
    fun focusRing_movedWithMovableContent_doesNotCrash() {
        var moveContent by mutableStateOf(false)

        rule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalRippleThemeConfiguration provides
                        RippleDefaults.InsetFocusRingThemeConfiguration,
                    LocalRippleConfiguration provides SingleRingColorConfiguration,
                ) {
                    val focusRingContent = remember {
                        movableContentOf {
                            // Subcomposition inside the moved content, so that the ripple node
                            // is updated before it is attached again in its new location.
                            BoxWithConstraints(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                // Change the shape on move so that update is called.
                                val shape = if (moveContent) MovedShape else DefaultShape
                                FocusRingBox(
                                    shape = shape,
                                    modifier =
                                        Modifier.indication(
                                            interactionSource = interactionSource,
                                            indication =
                                                ripple(
                                                    focusRingShape = shape,
                                                    enablePressIndication = false,
                                                    enableFocusIndication = true,
                                                    enableDragIndication = false,
                                                    enableHoverIndication = false,
                                                ),
                                        ),
                                )
                            }
                        }
                    }

                    // Changing the key moves the content to a new location in the composition.
                    key(moveContent) { focusRingContent() }
                }
            }
        }

        val focus = FocusInteraction.Focus()
        emit(focus)
        assertFocusRingColors(outerRingColor = SingleRingColor, innerRingColor = SingleRingColor)
        emit(FocusInteraction.Unfocus(focus))

        // Move the content - the ripple node is detached, updated and attached again.
        rule.runOnIdle { moveContent = true }

        // Verify that the focus ring is still drawn after the move.
        emit(FocusInteraction.Focus())
        assertFocusRingColors(outerRingColor = SingleRingColor, innerRingColor = SingleRingColor)
    }

    @Test
    fun focusRing_iconButton_preservesFocusAndUpdatesShape_whenShapeChanges() {
        var shape by mutableStateOf<CornerBasedShape>(CircleShape)
        rule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalRippleThemeConfiguration provides
                        RippleDefaults.InsetFocusRingThemeConfiguration,
                    LocalRippleConfiguration provides SingleRingColorConfiguration,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.testTag(Tag).size(BoxSize),
                            shapes = IconButtonDefaults.shapes(shape = shape),
                            colors =
                                IconButtonDefaults.iconButtonColors(
                                    containerColor = BackgroundColor
                                ),
                            interactionSource = interactionSource,
                        ) {}
                    }
                }
            }
        }

        emit(FocusInteraction.Focus())

        assertFocusRingShape(CircleShape)

        val newShape = RoundedCornerShape(0.dp)
        rule.runOnIdle { shape = newShape }

        assertFocusRingShape(newShape)
    }

    @Test
    fun focusRing_splitCheckboxButton_preservesFocus_whenShapeChanges() {
        var shape by mutableStateOf<CornerBasedShape>(RoundedCornerShape(percent = 50))
        rule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalRippleThemeConfiguration provides
                        RippleDefaults.InsetFocusRingThemeConfiguration,
                    LocalRippleConfiguration provides RingColorsConfiguration,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SplitCheckboxButton(
                            checked = false,
                            onCheckedChange = {},
                            toggleContentDescription = null,
                            onContainerClick = {},
                            modifier = Modifier.testTag(Tag),
                            shape = shape,
                            colors =
                                CheckboxButtonDefaults.splitCheckboxButtonColors(
                                    uncheckedContainerColor = BackgroundColor
                                ),
                            containerInteractionSource = interactionSource,
                            label = { Text("Label") },
                        )
                    }
                }
            }
        }

        emit(FocusInteraction.Focus())

        val pixelMapBefore = rule.onNodeWithTag(Tag).captureToImage().toPixelMap()
        val outerRingPx = with(rule.density) { OuterRingSampleOffset.roundToPx() }
        val innerRingPx = with(rule.density) { InnerRingSampleOffset.roundToPx() }
        val centerY = pixelMapBefore.height / 2
        assertThat(pixelMapBefore[outerRingPx, centerY]).isEqualTo(OuterRingColor)
        assertThat(pixelMapBefore[innerRingPx, centerY]).isEqualTo(InnerRingColor)

        // Mutate the SplitCheckboxButton shape while focused; active focus must not be lost.
        // Use 8.dp (distinct from SPLIT_SECTIONS_SHAPE = 4.dp) so startSectionShape remains
        // asymmetric on API < 33.
        rule.runOnIdle { shape = RoundedCornerShape(8.dp) }

        val pixelMapAfter = rule.onNodeWithTag(Tag).captureToImage().toPixelMap()
        assertThat(pixelMapAfter[outerRingPx, centerY]).isEqualTo(OuterRingColor)
        assertThat(pixelMapAfter[innerRingPx, centerY]).isEqualTo(InnerRingColor)
        // For an 8.dp corner radius, the 2.dp outer stroke spans radii [6.dp, 8.dp] from the arc
        // center at (8.dp, 8.dp), so along the 45° top-left diagonal its midpoint sits at ~3.dp
        // (8 - 7 * cos(45°) ≈ 3.05.dp), whereas with the 50% pill shape (3.dp, 3.dp) was outside
        // the rounded corner.
        val cornerSamplePx = with(rule.density) { 3.dp.roundToPx() }
        assertThat(pixelMapBefore[cornerSamplePx, cornerSamplePx]).isNotEqualTo(OuterRingColor)
        assertThat(pixelMapAfter[cornerSamplePx, cornerSamplePx]).isEqualTo(OuterRingColor)
    }

    private fun setFocusRingContent(
        themeConfiguration: () -> RippleThemeConfiguration = {
            RippleDefaults.InsetFocusRingThemeConfiguration
        },
        rippleConfiguration: RippleConfiguration? = RippleConfiguration(),
        shape: () -> Shape = { DefaultShape },
        enableFocusIndication: @Composable () -> Boolean = { true },
    ) {
        rule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalRippleThemeConfiguration provides themeConfiguration(),
                    LocalRippleConfiguration provides rippleConfiguration,
                ) {
                    val delegatingShape = remember {
                        object : Shape {
                            override fun createOutline(
                                size: Size,
                                layoutDirection: LayoutDirection,
                                density: Density,
                            ): Outline = shape().createOutline(size, layoutDirection, density)
                        }
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FocusRingBox(
                            shape = delegatingShape,
                            modifier =
                                Modifier.indication(
                                    interactionSource = interactionSource,
                                    indication =
                                        ripple(
                                            focusRingShape = delegatingShape,
                                            enablePressIndication = false,
                                            enableFocusIndication = enableFocusIndication(),
                                            enableDragIndication = false,
                                            enableHoverIndication = false,
                                        ),
                                ),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun FocusRingBox(shape: Shape, modifier: Modifier) {
        Box(
            Modifier.testTag(Tag)
                .size(BoxSize)
                .background(color = BackgroundColor, shape = shape)
                .then(modifier)
        )
    }

    private fun emit(interaction: Interaction) {
        rule.runOnIdle { assertThat(interactionSource.tryEmit(interaction)).isTrue() }
        rule.waitForIdle()
    }

    private fun assertFocusRingNotDrawn() =
        assertFocusRingColors(outerRingColor = BackgroundColor, innerRingColor = BackgroundColor)

    private fun assertFocusRingColors(
        outerRingColor: Color,
        innerRingColor: Color,
        centerColor: Color = BackgroundColor,
    ) {
        val pixelMap = rule.onNodeWithTag(Tag).captureToImage().toPixelMap()
        val centerPx = pixelMap.width / 2
        val outerRingPx = with(rule.density) { OuterRingSampleOffset.roundToPx() }
        val innerRingPx = with(rule.density) { InnerRingSampleOffset.roundToPx() }

        assertThat(pixelMap[centerPx, centerPx]).isEqualTo(centerColor)
        assertThat(pixelMap[outerRingPx, centerPx]).isEqualTo(outerRingColor)
        assertThat(pixelMap[innerRingPx, centerPx]).isEqualTo(innerRingColor)
    }

    private fun assertOuterStrokeFaded(faded: Boolean) {
        rule.waitForIdle()
        val pixelMap = rule.onNodeWithTag(Tag).captureToImage().toPixelMap()
        val outerRingPx = with(rule.density) { OuterRingSampleOffset.roundToPx() }
        val actual = pixelMap[outerRingPx, pixelMap.height / 2]
        val expected =
            if (faded) {
                OuterRingColor.copy(alpha = 0.5f).compositeOver(InnerRingColor)
            } else {
                OuterRingColor
            }

        assertThat(actual.red).isWithin(ColorTolerance).of(expected.red)
        assertThat(actual.green).isWithin(ColorTolerance).of(expected.green)
        assertThat(actual.blue).isWithin(ColorTolerance).of(expected.blue)
    }

    private fun assertFocusRingShape(shape: Shape) {
        val innerSizePx = with(rule.density) { (BoxSize - TotalStrokeWidth * 2).toPx() }
        rule
            .onNodeWithTag(Tag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = shape,
                shapeColor = BackgroundColor,
                shapeSize = Size(innerSizePx, innerSizePx),
                backgroundShape = shape,
                backgroundColor = SingleRingColor,
            )
    }
}

private const val Tag = "FocusRing"
private const val ColorTolerance = 0.02f
private val DefaultShape = RoundedCornerShape(20)
private val MovedShape = RoundedCornerShape(30)
private val BoxSize = 40.dp
private val DefaultInsetRing =
    RippleDefaults.InsetFocusRingThemeConfiguration.focus
        as RippleThemeConfiguration.Focus.InsetRing
private val TotalStrokeWidth = DefaultInsetRing.innerStrokeWidth
private val OuterRingSampleOffset = 1.dp
private val InnerRingSampleOffset = 3.dp

private val BackgroundColor = Color.Blue
private val OuterRingColor = Color.Yellow
private val InnerRingColor = Color.Cyan
private val SingleRingColor = Color.Magenta

private val RingColorsConfiguration =
    RippleConfiguration(
        focus =
            RippleConfiguration.Focus.InsetRing(
                outerStrokeColor = OuterRingColor,
                innerStrokeColor = InnerRingColor,
            )
    )

private val SingleRingColorConfiguration =
    RippleConfiguration(
        focus =
            RippleConfiguration.Focus.InsetRing(
                outerStrokeColor = SingleRingColor,
                innerStrokeColor = SingleRingColor,
            )
    )
