/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.toList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.glimmer.testutils.captureToImage
import androidx.xr.glimmer.testutils.createGlimmerRule
import androidx.xr.glimmer.testutils.toIntArray
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class SurfaceTest {

    @get:Rule(0) val rule = createComposeRule(ComposeUiTestConfig(inputMode = InputMode.Keyboard))

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun surface_equality_providedInteractionSource() {
        lateinit var surface: Modifier
        lateinit var surfaceWithSameParameters: Modifier
        lateinit var surfaceWithDifferentParameters: Modifier

        val interactionSource1 = MutableInteractionSource()
        val interactionSource2 = MutableInteractionSource()

        rule.setGlimmerThemeContent {
            surface =
                Modifier.surface(
                    shape = RectangleShape,
                    color = Color.Blue,
                    contentColor = Color.Magenta,
                    interactionSource = interactionSource1,
                )
            surfaceWithSameParameters =
                Modifier.surface(
                    shape = RectangleShape,
                    color = Color.Blue,
                    contentColor = Color.Magenta,
                    interactionSource = interactionSource1,
                )
            surfaceWithDifferentParameters =
                Modifier.surface(
                    shape = CircleShape,
                    color = Color.Blue,
                    contentColor = Color.Magenta,
                    interactionSource = interactionSource2,
                )
        }

        rule.runOnIdle {
            assertThat(surface).isEqualTo(surfaceWithSameParameters)
            assertThat(surface).isNotEqualTo(surfaceWithDifferentParameters)
        }
    }

    @Test
    fun surface_equality_providedFocusedColor() {
        lateinit var surface: Modifier
        lateinit var surfaceWithSameParameters: Modifier
        lateinit var surfaceWithDifferentParameters: Modifier

        rule.setGlimmerThemeContent {
            surface = Modifier.surface(color = Color.Blue, focusedColor = Color.Red)
            surfaceWithSameParameters =
                Modifier.surface(color = Color.Blue, focusedColor = Color.Red)
            surfaceWithDifferentParameters =
                Modifier.surface(color = Color.Blue, focusedColor = Color.Green)
        }

        rule.runOnIdle {
            assertThat(surface).isEqualTo(surfaceWithSameParameters)
            assertThat(surface).isNotEqualTo(surfaceWithDifferentParameters)
        }
    }

    @Test
    fun surface_inspectorValue() {
        rule.setContent {
            val modifiers = Modifier.surface().toList()
            assertThat(modifiers.size).isEqualTo(2)
            assertThat((modifiers[0] as InspectableValue).nameFallback).isEqualTo("graphicsLayer")
            val surfaceModifier = modifiers[1] as InspectableValue
            assertThat(surfaceModifier.nameFallback).isEqualTo("surface")
            assertThat(surfaceModifier.valueOverride).isNull()
            assertThat(surfaceModifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "enabled",
                    "color",
                    "focusedColor",
                    "contentColor",
                    "focusedContentColor",
                    "shape",
                    "interactionSource",
                )
        }
    }

    @Test
    fun surface_focusedColor_usedWhenFocused() {
        rule.mainClock.autoAdvance = false

        val focusRequester = FocusRequester()
        val interactionSource = MutableInteractionSource()
        val surfaceColor = Color.Blue
        val focusedSurfaceColor = Color.Yellow

        rule.setGlimmerThemeContent {
            Box(
                Modifier.size(100.dp)
                    .surface(
                        color = surfaceColor,
                        focusedColor = focusedSurfaceColor,
                        interactionSource = interactionSource,
                    )
                    .focusRequester(focusRequester)
                    .focusable(interactionSource = interactionSource)
                    .testTag("surface")
            )
        }

        // Unfocused: center of surface should be surfaceColor
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(surfaceColor)
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // Advance past enter animation
        rule.mainClock.advanceTimeBy(1000)

        // When focused (focusProgress == 1f), color is
        // focusOverlayColor.compositeOver(focusedSurfaceColor)
        val expectedFocusedColor =
            Color(red = 1f, green = 1f, blue = 1f, alpha = 0.16f).compositeOver(focusedSurfaceColor)

        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertColorsEqualWithTolerance(expectedFocusedColor, get(width / 2, height / 2))
        }
    }

    @Test
    fun surface_clipsContent() {
        rule.setGlimmerThemeContent {
            with(LocalDensity.current) {
                val outerSize = 100.toDp()
                val innerSize = 50.toDp()
                Box(Modifier.size(outerSize).testTag("outerBox").background(Color.Red)) {
                    Box(
                        Modifier.size(innerSize)
                            .surface(shape = RectangleShape, color = Color.Blue)
                            .drawWithContent {
                                // Try and draw a rect that would fill the outerSize, if there was
                                // no clipping
                                drawRect(color = Color.Green, size = Size(100f, 100f))
                            }
                    )
                }
            }
        }
        rule.onNodeWithTag("outerBox").captureToImage().assertPixels(
            expectedSize = IntSize(100, 100)
        ) {
            if (it.x < 50 && it.y < 50) {
                // The inner surface should all be green
                Color.Green
            } else {
                // The outer box should be red, as the inner surface should clip the green
                Color.Red
            }
        }
    }

    @Test
    fun surface_changeShape_borderChanges() {
        var roundedCorners by mutableStateOf(true)

        rule.setGlimmerThemeContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(40f.toDp())
                        .background(Color.Blue)
                        .surface(
                            shape = if (roundedCorners) RoundedCornerShape(5f) else RectangleShape
                        )
                        .testTag("surface")
                )
            }
        }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should be rounded, so top-left and bottom-left will be blue, and center will not
            // be blue
            assertThat(Color.Blue).isEqualTo(map[0, 0])
            assertThat(map[0, (height - 1) / 2]).isNotEqualTo(Color.Blue)
            assertThat(Color.Blue).isEqualTo(map[0, height - 1])
        }

        rule.runOnIdle { roundedCorners = false }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should no longer be rounded, so left edge should be fully surface (not blue)
            assertThat(map[0, 0]).isNotEqualTo(Color.Blue)
            assertThat(map[0, (height - 1) / 2]).isNotEqualTo(Color.Blue)
            assertThat(map[0, height - 1]).isNotEqualTo(Color.Blue)
        }
    }

    @Test
    fun surface_observableShape_roundedOutline_borderChanges() {
        var roundedCorners by mutableStateOf(true)
        val roundedCornersShape =
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline {
                    val roundRect =
                        if (roundedCorners) {
                            RoundRect(Rect(Offset.Zero, size), cornerRadius = CornerRadius(5f))
                        } else {
                            RoundRect(Rect(Offset.Zero, size), cornerRadius = CornerRadius.Zero)
                        }
                    return Outline.Rounded(roundRect)
                }
            }

        rule.setGlimmerThemeContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(40f.toDp())
                        .background(Color.Blue)
                        .surface(shape = roundedCornersShape)
                        .testTag("surface")
                )
            }
        }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should be rounded, so top-left and bottom-left will be blue, and center will not
            // be blue
            assertThat(Color.Blue).isEqualTo(map[0, 0])
            assertThat(map[0, (height - 1) / 2]).isNotEqualTo(Color.Blue)
            assertThat(Color.Blue).isEqualTo(map[0, height - 1])
        }

        rule.runOnIdle { roundedCorners = false }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should no longer be rounded, so left edge should be fully surface (not blue)
            assertThat(map[0, 0]).isNotEqualTo(Color.Blue)
            assertThat(map[0, (height - 1) / 2]).isNotEqualTo(Color.Blue)
            assertThat(map[0, height - 1]).isNotEqualTo(Color.Blue)
        }
    }

    @Test
    fun surface_observableShape_genericOutline_samePath_borderChanges() {
        var roundedCorners by mutableStateOf(true)
        val roundedCornersShape =
            object : Shape {
                val path = Path()

                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline {
                    val roundRect =
                        if (roundedCorners) {
                            RoundRect(Rect(Offset.Zero, size), cornerRadius = CornerRadius(50f))
                        } else {
                            RoundRect(Rect(Offset.Zero, size), cornerRadius = CornerRadius.Zero)
                        }
                    path.reset()
                    path.addRoundRect(roundRect)
                    return Outline.Generic(path)
                }
            }

        rule.setGlimmerThemeContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(400f.toDp())
                        .background(Color.Blue)
                        .surface(shape = roundedCornersShape)
                        .testTag("surface")
                )
            }
        }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should be rounded, so top-left and bottom-left will be blue, and center will not
            // be blue
            assertThat(Color.Blue).isEqualTo(map[0, 0])
            assertThat(map[0, (height - 1) / 2]).isNotEqualTo(Color.Blue)
            // The last pixel fails to render properly on some emulators, so just assert the one
            // before instead - b/267371353
            assertThat(Color.Blue).isEqualTo(map[0, height - 2])
        }

        rule.runOnIdle { roundedCorners = false }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should no longer be rounded, so left edge should be fully surface (not blue)
            assertThat(map[0, 0]).isNotEqualTo(Color.Blue)
            assertThat(map[0, (height - 1) / 2]).isNotEqualTo(Color.Blue)
            // The last pixel fails to render properly on some emulators, so just assert the one
            // before instead - b/267371353
            assertThat(map[0, height - 2]).isNotEqualTo(Color.Blue)
        }
    }

    @Test
    fun surface_providesContentColor_default() {
        var color: Color = Color.Unspecified
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface()
                    .then(
                        DelegatableNodeProviderElement {
                            color = it?.currentContentColor() ?: Color.Unspecified
                        }
                    )
            )
        }

        rule.runOnIdle { assertThat(color).isEqualTo(Color.White) }
    }

    @Test
    fun surface_providesContentColor_calculatedFromBackground() {
        var node: DelegatableNode? = null
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(color = Color.White)
                    .then(DelegatableNodeProviderElement { node = it })
            )
        }

        // Surface color is white, so the content color should be black
        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(Color.Black) }
    }

    @Test
    fun surface_providesContentColor_updates_backgroundColor() {
        var backgroundColor by mutableStateOf(Color.White)
        var node: DelegatableNode? = null
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(color = backgroundColor)
                    .then(DelegatableNodeProviderElement { node = it })
            )
        }

        rule.runOnIdle {
            // Surface color is white, so the content color should be black
            assertThat(node!!.currentContentColor()).isEqualTo(Color.Black)
            backgroundColor = Color.Black
        }

        rule.runOnIdle {
            // Surface color is now black, so the content color should be white
            assertThat(node!!.currentContentColor()).isEqualTo(Color.White)
        }
    }

    @Test
    fun surface_providesContentColor_updates_contentColor() {
        var expectedColor by mutableStateOf(Color.White)
        var node: DelegatableNode? = null
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(contentColor = expectedColor)
                    .then(DelegatableNodeProviderElement { node = it })
            )
        }

        rule.runOnIdle {
            assertThat(node!!.currentContentColor()).isEqualTo(Color.White)
            expectedColor = Color.Blue
        }

        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(Color.Blue) }
    }

    @Test
    fun surface_focusedContentColor_usedWhenFocused() {
        val focusRequester = FocusRequester()
        val interactionSource = MutableInteractionSource()
        val contentColor = Color.Red
        val focusedContentColor = Color.Green

        var node: DelegatableNode? = null

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            Box(
                Modifier.size(100.dp)
                    .surface(
                        contentColor = contentColor,
                        focusedContentColor = focusedContentColor,
                        interactionSource = interactionSource,
                    )
                    .then(DelegatableNodeProviderElement { node = it })
                    .focusRequester(focusRequester)
                    .focusable(interactionSource = interactionSource)
            )
        }

        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(contentColor) }

        rule.runOnIdle { focusRequester.requestFocus() }
        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(focusedContentColor) }
    }

    @Test
    fun surface_focusedColor_resolvesFocusedContentColor() {
        val focusRequester = FocusRequester()
        val interactionSource = MutableInteractionSource()
        val focusedColor = Color.Green

        var node: DelegatableNode? = null

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            Box(
                Modifier.size(100.dp)
                    .surface(focusedColor = focusedColor, interactionSource = interactionSource)
                    .then(DelegatableNodeProviderElement { node = it })
                    .focusRequester(focusRequester)
                    .focusable(interactionSource = interactionSource)
            )
        }

        rule.runOnIdle { focusRequester.requestFocus() }
        // Focused background is Green, so calculated content color should be Black
        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(Color.Black) }
    }

    @Test
    fun surface_depthEffect_focusChange_newDepthEffectIsRendered() {
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        val surfaceDepthEffect =
            SurfaceDepthEffect(
                depthEffect =
                    DepthEffect(
                        Shadow(color = Color.Red, spread = 100.dp, radius = 100.dp),
                        Shadow(color = Color.Red, spread = 100.dp, radius = 100.dp),
                    ),
                focusedDepthEffect =
                    DepthEffect(
                        layer1 = Shadow(color = Color.Blue, spread = 100.dp, radius = 100.dp),
                        layer2 = Shadow(color = Color.Blue, spread = 100.dp, radius = 100.dp),
                    ),
            )

        rule.setGlimmerThemeContent {
            Box(Modifier.testTag("outerBox")) {
                Box(Modifier.size(100.dp).focusRequester(otherFocusRequester).focusTarget())
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    Modifier.padding(40.dp)
                        .size(20.dp)
                        .surface(
                            depthEffect = surfaceDepthEffect,
                            interactionSource = interactionSource,
                        )
                        .focusRequester(focusRequester)
                        .focusable(interactionSource = interactionSource)
                )
            }
        }

        rule.onNodeWithTag("outerBox").captureToImage().run {
            val map = toPixelMap()
            val outsideSurface = map[width / 3, height / 2]
            // Base depth effect should be rendered
            assertColorsEqualWithTolerance(Color.Red, outsideSurface)
        }

        // Request focus for the surface
        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag("outerBox").captureToImage().run {
            val map = toPixelMap()
            val outsideSurface = map[width / 3, height / 2]
            // The focused depth effect should be rendered
            assertColorsEqualWithTolerance(Color.Blue, outsideSurface)
        }

        // Request focus for the other target, moving focus away from the surface
        rule.runOnIdle { otherFocusRequester.requestFocus() }

        rule.onNodeWithTag("outerBox").captureToImage().run {
            val map = toPixelMap()
            val outsideSurface = map[width / 3, height / 2]
            // Base depth effect should be rendered again
            assertColorsEqualWithTolerance(Color.Red, outsideSurface)
        }
    }

    @Test
    fun surface_depthEffect_focusChange_depthEffectChangesAreAnimated() {
        rule.mainClock.autoAdvance = false

        val focusRequester = FocusRequester()

        val shadow = Shadow(color = Color.Red, spread = 100.dp, radius = 100.dp)
        val surfaceDepthEffect =
            SurfaceDepthEffect(
                depthEffect = null,
                focusedDepthEffect = DepthEffect(layer1 = shadow, layer2 = shadow),
            )

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            Box(Modifier.testTag("outerBox")) {
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    Modifier.padding(40.dp)
                        .size(20.dp)
                        .surface(
                            depthEffect = surfaceDepthEffect,
                            interactionSource = interactionSource,
                        )
                        .focusRequester(focusRequester)
                        .focusable(interactionSource = interactionSource)
                )
            }
        }

        rule.onNodeWithTag("outerBox").captureToImage().run {
            val map = toPixelMap()
            val outsideSurface = map[width / 3, height / 2]
            // No depth effect should be rendered
            assertThat(outsideSurface).isEqualTo(Color.Black)
        }

        // Request focus for the surface
        rule.runOnIdle { focusRequester.requestFocus() }

        // There is an enter animation, so advance a small time after the animation starts
        rule.mainClock.advanceTimeBy(50)

        rule.onNodeWithTag("outerBox").captureToImage().run {
            val map = toPixelMap()
            val outsideSurface = map[width / 3, height / 2]
            // The focused depth effect should be partially rendered, so there should be some red
            // channel.
            assertThat(outsideSurface.red).isGreaterThan(0)
            assertThat(outsideSurface.green).isEqualTo(0)
            assertThat(outsideSurface.blue).isEqualTo(0)
        }

        // Advance past the animation
        rule.mainClock.advanceTimeBy(1000)

        rule.onNodeWithTag("outerBox").captureToImage().run {
            val map = toPixelMap()
            val outsideSurface = map[width / 3, height / 2]
            // The focused depth effect should be fully rendered
            assertColorsEqualWithTolerance(Color.Red, outsideSurface)
        }
    }

    @Test
    fun surface_depthEffect_focusedDepthEffectHasHigherZIndex() {
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        val surfaceDepthEffect =
            SurfaceDepthEffect(
                depthEffect =
                    DepthEffect(
                        Shadow(color = Color.Red, spread = 100.dp, radius = 100.dp),
                        Shadow(color = Color.Red, spread = 100.dp, radius = 100.dp),
                    ),
                focusedDepthEffect =
                    DepthEffect(
                        layer1 = Shadow(color = Color.Blue, spread = 100.dp, radius = 100.dp),
                        layer2 = Shadow(color = Color.Blue, spread = 100.dp, radius = 100.dp),
                    ),
            )

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            Column {
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    Modifier.padding(40.dp)
                        .size(20.dp)
                        .surface(
                            depthEffect = surfaceDepthEffect,
                            interactionSource = interactionSource,
                        )
                        .focusRequester(focusRequester)
                        .focusable(interactionSource = interactionSource)
                )
                Box(
                    Modifier.testTag("greenBox")
                        .size(100.dp)
                        .background(Color.Green)
                        .focusRequester(otherFocusRequester)
                        .focusTarget()
                )
            }
        }

        // Default draw order is based on placement order. The green box is second in the column,
        // so it should draw over the first box - as a result the depth effect will not be visible,
        // and the entire box will be green.
        rule.onNodeWithTag("greenBox").captureToImage().assertPixels { Color.Green }

        // Request focus for the surface
        rule.runOnIdle { focusRequester.requestFocus() }

        // The surface is focused, so it should now have a higher zIndex set, which will cause it to
        // draw over the sibling box. The blue shadow should now overlap the green box, causing the
        // sampled pixel to have a blue channel.
        rule.onNodeWithTag("greenBox").captureToImage().run {
            val map = toPixelMap()
            val topMiddle = map[width / 2, height / 4]
            assertThat(topMiddle.blue).isGreaterThan(0)
        }

        // Request focus for the other target, moving focus away from the surface
        rule.runOnIdle { otherFocusRequester.requestFocus() }

        // zIndex should be reset so the green box should draw on top once again
        rule.onNodeWithTag("greenBox").captureToImage().assertPixels { Color.Green }
    }

    @Test
    fun focusableSurface_focusHighlight_appearsAndDisappearsWithFocusChange() {
        rule.mainClock.autoAdvance = false

        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        rule.setGlimmerThemeContent {
            Column {
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    Modifier.size(100.dp)
                        .surface(shape = RectangleShape, interactionSource = interactionSource)
                        .focusRequester(focusRequester)
                        .focusable(interactionSource = interactionSource)
                        .testTag("surface")
                )
                Box(Modifier.size(100.dp).focusRequester(otherFocusRequester).focusable())
            }
        }

        var unfocusedBorderColor = Color.Unspecified
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            unfocusedBorderColor = get(1, 1)
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // There is an enter animation, so advance a small time after the animation starts
        rule.mainClock.advanceTimeBy(50)

        // The focused highlight should show, so the start of the border will not be
        // unfocusedBorderColor
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isNotEqualTo(unfocusedBorderColor)
        }

        rule.runOnIdle { otherFocusRequester.requestFocus() }

        // Advance past the exit animation
        rule.mainClock.advanceTimeBy(1_000)

        // Focused highlight should disappear, so the border should be unfocusedBorderColor
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(unfocusedBorderColor)
        }
    }

    @Test
    fun surface_focusHighlight_animationPlaysOnce() {
        rule.mainClock.autoAdvance = false

        val matcher = MSSIMMatcher()
        val focusRequester = FocusRequester()

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            Column {
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    Modifier.size(100.dp)
                        .surface(shape = RectangleShape, interactionSource = interactionSource)
                        .focusRequester(focusRequester)
                        .focusable(interactionSource = interactionSource)
                        .testTag("surface")
                )
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // Capture the initial focus state before the animation starts
        val initialFrame = rule.onNodeWithTag("surface").captureToImage()

        rule.mainClock.advanceTimeBy(300)

        // Capture the focus state during the animation
        val midAnimation = rule.onNodeWithTag("surface").captureToImage()

        rule.runOnIdle {
            // Initial state and mid animation should be different
            val result =
                matcher.compareBitmaps(
                    initialFrame.toIntArray(),
                    midAnimation.toIntArray(),
                    initialFrame.width,
                    initialFrame.height,
                )
            assertThat(result.matches).isFalse()
        }

        // Advance past the enter animation (800ms total), but before ambient delay (1800ms)
        rule.mainClock.advanceTimeBy(800)

        // Capture the focus state after the enter animation has settled
        val afterAnimation = rule.onNodeWithTag("surface").captureToImage()

        // Advance a bit forward again (before ambient delay at 1800ms) to make sure there is no
        // change
        rule.mainClock.advanceTimeBy(100)

        // Capture a second image after the extra delay - this should be the same
        val afterAnimation2 = rule.onNodeWithTag("surface").captureToImage()

        rule.runOnIdle {
            // Both images after the animation has finished should be the same
            val afterAnimationResult =
                matcher.compareBitmaps(
                    afterAnimation.toIntArray(),
                    afterAnimation2.toIntArray(),
                    afterAnimation.width,
                    afterAnimation.height,
                )
            assertThat(afterAnimationResult.matches).isTrue()
        }
    }

    @Test
    fun surface_focusHighlight_animationResetsWhenBecomingFocusedAgain() {
        rule.mainClock.autoAdvance = false

        val matcher = MSSIMMatcher()
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            Column {
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    Modifier.size(100.dp)
                        .surface(shape = RectangleShape, interactionSource = interactionSource)
                        .focusRequester(focusRequester)
                        .focusable(interactionSource = interactionSource)
                        .testTag("surface")
                )
                Box(Modifier.size(100.dp).focusRequester(otherFocusRequester).focusable())
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // Capture the initial focus state before the animation starts
        val initialFrame = rule.onNodeWithTag("surface").captureToImage()

        rule.mainClock.advanceTimeBy(300)

        // Capture the focus state during the animation
        val midAnimation = rule.onNodeWithTag("surface").captureToImage()

        rule.runOnIdle {
            // Initial state and mid animation should be different
            val result =
                matcher.compareBitmaps(
                    initialFrame.toIntArray(),
                    midAnimation.toIntArray(),
                    initialFrame.width,
                    initialFrame.height,
                )
            assertThat(result.matches).isFalse()
            // Move focus away
            otherFocusRequester.requestFocus()
        }

        // Advance past exit animation (500ms)
        rule.mainClock.advanceTimeBy(1000)

        // Move focus back to the initial surface
        rule.runOnIdle { focusRequester.requestFocus() }

        // Capture the initial focus state before the animation starts
        val initialFrame2 = rule.onNodeWithTag("surface").captureToImage()

        rule.mainClock.advanceTimeBy(300)

        // Capture the focus state during the animation
        val midAnimation2 = rule.onNodeWithTag("surface").captureToImage()

        rule.runOnIdle {
            // The initial state and mid animation state the first time the surface was focused
            // should match the state the second time it was focused
            val initialResult =
                matcher.compareBitmaps(
                    initialFrame.toIntArray(),
                    initialFrame2.toIntArray(),
                    initialFrame.width,
                    initialFrame.height,
                )
            assertThat(initialResult.matches).isTrue()
            val midResult =
                matcher.compareBitmaps(
                    midAnimation.toIntArray(),
                    midAnimation2.toIntArray(),
                    midAnimation.width,
                    midAnimation.height,
                )
            assertThat(midResult.matches).isTrue()
        }
    }

    @Test
    fun surface_focusHighlight_resetWhenChangingInteractionSource() {
        rule.mainClock.autoAdvance = false

        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()
        var interactionSource by mutableStateOf(MutableInteractionSource())

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            Column {
                Box(
                    Modifier.size(100.dp)
                        .surface(shape = RectangleShape, interactionSource = interactionSource)
                        .focusRequester(focusRequester)
                        .focusable(interactionSource = interactionSource)
                        .testTag("surface")
                )
                Box(Modifier.size(100.dp).focusRequester(otherFocusRequester).focusable())
            }
        }

        var unfocusedBorderColor = Color.Unspecified
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            unfocusedBorderColor = get(1, 1)
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // There is an enter animation, so advance a small time after the animation starts
        rule.mainClock.advanceTimeBy(50)

        // The focused highlight should show, so the start of the border will not be
        // unfocusedBorderColor
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isNotEqualTo(unfocusedBorderColor)
        }

        // Change the interaction source - even though the node is technically still focused, we
        // should reset the highlight as the interaction source changed. In the future if we
        // directly delegate to focusable we would be able to maintain focus in that case
        rule.runOnIdle { interactionSource = MutableInteractionSource() }

        // Advance past the exit animation
        rule.mainClock.advanceTimeBy(1_000)

        // Focused highlight should disappear, so the border should be unfocusedBorderColor
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(unfocusedBorderColor)
        }

        // Move focus away from and back to the surface
        rule.runOnIdle { otherFocusRequester.requestFocus() }
        rule.runOnIdle { focusRequester.requestFocus() }

        // There is an enter animation, so advance a small time after the animation starts
        rule.mainClock.advanceTimeBy(50)

        // The new interaction source will see the new focus, so the focused highlight should show
        // again
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isNotEqualTo(unfocusedBorderColor)
        }
    }

    @Test
    fun surface_pressedOverlay_resetWhenChangingInteractionSource() {
        rule.mainClock.autoAdvance = false

        var interactionSource by mutableStateOf(MutableInteractionSource())

        lateinit var scope: CoroutineScope
        var surfaceColor = Color.Unspecified
        rule.setGlimmerThemeContent {
            surfaceColor = GlimmerTheme.colors.surface
            scope = rememberCoroutineScope()
            Column {
                Box(
                    Modifier.size(100.dp)
                        .surface(interactionSource = interactionSource)
                        .testTag("surface")
                )
            }
        }

        // The center of the surface should be the surface color
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(surfaceColor)
        }

        var unpressedEdgeColor = Color.Unspecified
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            unpressedEdgeColor = get(width / 2, 8)
        }

        // Send press interaction
        rule.runOnIdle {
            scope.launch { interactionSource.emit(PressInteraction.Press(Offset.Zero)) }
        }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should be showing, so edge pixel changes
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, 8)).isNotEqualTo(unpressedEdgeColor)
        }

        // Change the interaction source - this should cause us to animate away from pressed
        rule.runOnIdle { interactionSource = MutableInteractionSource() }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should disappear, so edge pixel returns to unpressedEdgeColor
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, 8)).isEqualTo(unpressedEdgeColor)
        }

        // Send press interaction again
        rule.runOnIdle {
            scope.launch { interactionSource.emit(PressInteraction.Press(Offset.Zero)) }
        }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should be showing again
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, 8)).isNotEqualTo(unpressedEdgeColor)
        }
    }

    /**
     * Even though the surface doesn't handle clicks itself, it should still respond to externally
     * provided PressInteractions.
     */
    @Test
    fun surface_pressedOverlay_appearsAndDisappearsWithPressChange() {
        rule.mainClock.autoAdvance = false

        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope
        rule.setGlimmerThemeContent {
            scope = rememberCoroutineScope()
            Column {
                Box(
                    Modifier.size(100.dp)
                        .surface(interactionSource = interactionSource)
                        .testTag("surface")
                )
            }
        }

        var unpressedEdgeColor = Color.Unspecified
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            unpressedEdgeColor = get(width / 2, 8)
        }

        val press = PressInteraction.Press(Offset.Zero)

        // Send press interaction
        rule.runOnIdle { scope.launch { interactionSource.emit(press) } }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should be showing
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, 8)).isNotEqualTo(unpressedEdgeColor)
        }

        // Send release interaction
        rule.runOnIdle { scope.launch { interactionSource.emit(PressInteraction.Release(press)) } }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should disappear
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, 8)).isEqualTo(unpressedEdgeColor)
        }
    }

    @Test
    fun surface_pressedOverlay_hasAMinimumDuration() {
        rule.mainClock.autoAdvance = false

        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope
        rule.setGlimmerThemeContent {
            scope = rememberCoroutineScope()
            Column {
                Box(
                    Modifier.size(100.dp)
                        .surface(interactionSource = interactionSource)
                        .testTag("surface")
                )
            }
        }

        var unpressedEdgeColor = Color.Unspecified
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            unpressedEdgeColor = get(width / 2, 8)
        }

        val press = PressInteraction.Press(Offset.Zero)
        val release = PressInteraction.Release(press)

        // Start a press, and immediately release
        rule.runOnIdle {
            scope.launch {
                interactionSource.emit(press)
                interactionSource.emit(release)
            }
        }

        // Advance a short amount of time
        rule.mainClock.advanceTimeBy(150)

        // The press overlay should continue to animate for a minimum duration, and then fade out.
        // If there was no minimum duration, the animation would have ended already - so
        // make sure the edge color is not equal to the unpressed edge color.
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, 8)).isNotEqualTo(unpressedEdgeColor)
        }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should disappear after the minimum duration
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, 8)).isEqualTo(unpressedEdgeColor)
        }
    }

    @Test
    fun surface_disabled_rendersDisabledOverlay() {
        var surfaceColor = Color.Unspecified
        rule.setGlimmerThemeContent {
            surfaceColor = GlimmerTheme.colors.surface
            Box(Modifier.size(100.dp).surface(enabled = false).testTag("surface"))
        }

        val expectedColor = DisabledOverlayColor.compositeOver(surfaceColor)
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertColorsEqualWithTolerance(expectedColor, get(width / 2, height / 2))
        }
    }

    @Test
    fun surfaceDefaults_colors() {
        rule.setGlimmerThemeContent {
            assertThat(SurfaceDefaults.color())
                .isEqualTo(SurfaceDefaults.color(GlimmerTheme.colors.surface))
            assertThat(SurfaceDefaults.focusedColor())
                .isEqualTo(SurfaceDefaults.focusedColor(GlimmerTheme.colors.surface))
        }
    }
}

/**
 * Asserts that [expected] and [actual] are mostly equal, to avoid test failures due to minor
 * rendering differences across devices.
 */
private fun assertColorsEqualWithTolerance(
    expected: Color,
    actual: Color,
    tolerance: Float = 0.05f,
) {
    assertEquals(expected.red, actual.red, tolerance)
    assertEquals(expected.green, actual.green, tolerance)
    assertEquals(expected.blue, actual.blue, tolerance)
    assertEquals(expected.alpha, actual.alpha, tolerance)
}
