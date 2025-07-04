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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isNotFocusable
import androidx.compose.ui.test.isNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.matchers.MSSIMMatcher
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SurfaceTest {

    @get:Rule val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    // Enter non-touch mode for tests, so that clickables can be focused.
    // TODO(b/267253920): Add a compose test API to set/reset InputMode.
    @Before
    fun enterNonTouchMode() = InstrumentationRegistry.getInstrumentation().setInTouchMode(false)

    // TODO(b/267253920): Add a compose test API to set/reset InputMode.
    @After fun resetTouchMode() = InstrumentationRegistry.getInstrumentation().resetInTouchMode()

    @Test
    fun focusableSurface_equality_providedInteractionSource() {
        lateinit var surface: Modifier
        lateinit var surfaceWithSameParameters: Modifier
        lateinit var surfaceWithDifferentParameters: Modifier

        val interactionSource1 = MutableInteractionSource()
        val interactionSource2 = MutableInteractionSource()

        rule.setGlimmerThemeContent {
            surface =
                Modifier.surface(
                    focusable = true,
                    shape = RectangleShape,
                    color = Color.Blue,
                    contentColor = Color.Magenta,
                    border = BorderStroke(1.dp, Color.Red),
                    interactionSource = interactionSource1,
                )
            surfaceWithSameParameters =
                Modifier.surface(
                    focusable = true,
                    shape = RectangleShape,
                    color = Color.Blue,
                    contentColor = Color.Magenta,
                    border = BorderStroke(1.dp, Color.Red),
                    interactionSource = interactionSource1,
                )
            surfaceWithDifferentParameters =
                Modifier.surface(
                    focusable = true,
                    shape = CircleShape,
                    color = Color.Blue,
                    contentColor = Color.Magenta,
                    border = BorderStroke(1.dp, Color.Red),
                    interactionSource = interactionSource2,
                )
        }

        rule.runOnIdle {
            assertThat(surface).isEqualTo(surfaceWithSameParameters)
            assertThat(surface).isNotEqualTo(surfaceWithDifferentParameters)
        }
    }

    @Test
    fun clickableSurface_equality_providedInteractionSource() {
        lateinit var surface: Modifier
        lateinit var surfaceWithSameParameters: Modifier
        lateinit var surfaceWithDifferentParameters: Modifier

        val interactionSource1 = MutableInteractionSource()
        val interactionSource2 = MutableInteractionSource()
        val onClick = {}

        rule.setGlimmerThemeContent {
            surface =
                Modifier.surface(
                    enabled = true,
                    shape = RectangleShape,
                    color = Color.Blue,
                    contentColor = Color.Magenta,
                    border = BorderStroke(1.dp, Color.Red),
                    interactionSource = interactionSource1,
                    onClick = onClick,
                )
            surfaceWithSameParameters =
                Modifier.surface(
                    enabled = true,
                    shape = RectangleShape,
                    color = Color.Blue,
                    contentColor = Color.Magenta,
                    border = BorderStroke(1.dp, Color.Red),
                    interactionSource = interactionSource1,
                    onClick = onClick,
                )
            surfaceWithDifferentParameters =
                Modifier.surface(
                    enabled = true,
                    shape = CircleShape,
                    color = Color.Blue,
                    contentColor = Color.Magenta,
                    border = BorderStroke(1.dp, Color.Red),
                    interactionSource = interactionSource2,
                    onClick = onClick,
                )
        }

        rule.runOnIdle {
            assertThat(surface).isEqualTo(surfaceWithSameParameters)
            assertThat(surface).isNotEqualTo(surfaceWithDifferentParameters)
        }
    }

    /**
     * Test for recomposition equality when interactionSource is not provided. In this case the
     * interaction source will be remembered inside, but it means that calling the same modifier at
     * different call sites will not compare equal, because the interaction source internally
     * differs.
     *
     * However the interaction source internally should be remembered for the same modifier, to make
     * sure we don't cause any work for unrelated recompositions.
     */
    @Test
    fun focusableSurface_equality_noProvidedInteractionSource() {
        val surfaces = mutableListOf<Modifier>()
        lateinit var surfaceWithSameParametersInDifferentCallSite: Modifier

        var invalidation by mutableStateOf(false)

        rule.setGlimmerThemeContent {
            invalidation
            surfaces.add(
                Modifier.surface(
                    focusable = true,
                    shape = RectangleShape,
                    color = Color.Blue,
                    contentColor = Color.Magenta,
                    border = BorderStroke(1.dp, Color.Red),
                )
            )
            surfaceWithSameParametersInDifferentCallSite =
                Modifier.surface(
                    focusable = true,
                    shape = RectangleShape,
                    color = Color.Blue,
                    contentColor = Color.Magenta,
                    border = BorderStroke(1.dp, Color.Red),
                )
        }

        rule.runOnIdle {
            assertThat(surfaces).hasSize(1)
            assertThat(surfaces[0]).isNotEqualTo(surfaceWithSameParametersInDifferentCallSite)
            // force recomposition
            invalidation = !invalidation
        }

        rule.runOnIdle {
            assertThat(surfaces).hasSize(2)
            assertThat(surfaces[0]).isEqualTo(surfaces[1])
        }
    }

    /**
     * Test for recomposition equality when interactionSource is not provided. In this case the
     * interaction source will be remembered inside, but it means that calling the same modifier at
     * different call sites will not compare equal, because the interaction source internally
     * differs.
     *
     * However the interaction source internally should be remembered for the same modifier, to make
     * sure we don't cause any work for unrelated recompositions.
     */
    @Test
    fun clickableSurface_equality_noProvidedInteractionSource() {
        val surfaces = mutableListOf<Modifier>()
        lateinit var surfaceWithSameParametersInDifferentCallSite: Modifier

        var invalidation by mutableStateOf(false)

        rule.setGlimmerThemeContent {
            invalidation
            surfaces.add(
                Modifier.surface(
                    enabled = true,
                    shape = RectangleShape,
                    color = Color.Blue,
                    contentColor = Color.Magenta,
                    border = BorderStroke(1.dp, Color.Red),
                    onClick = {},
                )
            )
            surfaceWithSameParametersInDifferentCallSite =
                Modifier.surface(
                    enabled = true,
                    shape = RectangleShape,
                    color = Color.Blue,
                    contentColor = Color.Magenta,
                    border = BorderStroke(1.dp, Color.Red),
                    onClick = {},
                )
        }

        rule.runOnIdle {
            assertThat(surfaces).hasSize(1)
            assertThat(surfaces[0]).isNotEqualTo(surfaceWithSameParametersInDifferentCallSite)
            // force recomposition
            invalidation = !invalidation
        }

        rule.runOnIdle {
            assertThat(surfaces).hasSize(2)
            assertThat(surfaces[0]).isEqualTo(surfaces[1])
        }
    }

    @Test
    fun focusableSurface_semantics_focusable() {
        val focusRequester = FocusRequester()
        rule.setGlimmerThemeContent {
            Box(Modifier.size(100.dp).focusRequester(focusRequester).surface().testTag("surface"))
        }
        rule.onNodeWithTag("surface").assert(isFocusable()).assert(isNotFocused())

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag("surface").assert(isFocusable()).assert(isFocused())
    }

    @Test
    fun focusableSurface_semantics_not_focusable() {
        val focusRequester = FocusRequester()
        rule.setGlimmerThemeContent {
            Box(
                Modifier.size(100.dp)
                    .focusRequester(focusRequester)
                    .surface(focusable = false)
                    .testTag("surface")
            )
        }
        rule.onNodeWithTag("surface").assert(isNotFocusable())

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag("surface").assert(isNotFocusable())
    }

    @Test
    fun clickableSurface_click() {
        var count by mutableStateOf(0)
        rule.setGlimmerThemeContent {
            Box(Modifier.size(100.dp).surface(onClick = { count++ }).testTag("surface")) {
                Text("$count")
            }
        }
        rule.runOnIdle { assertThat(count).isEqualTo(0) }

        rule.onNodeWithTag("surface").performClick()

        rule.runOnIdle { assertThat(count).isEqualTo(1) }
    }

    @Test
    fun clickableSurface_semantics_enabled() {
        var count by mutableStateOf(0)
        rule.setGlimmerThemeContent {
            Box(Modifier.size(100.dp).surface(onClick = { count++ }).testTag("surface")) {
                Text("$count")
            }
        }
        rule
            .onNodeWithTag("surface")
            .assert(isFocusable())
            .assert(isNotFocused())
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("0")
            .performClick()
            .assertTextEquals("1")
    }

    @Test
    fun clickableSurface_semantics_disabled() {
        var enabled by mutableStateOf(true)
        var count by mutableStateOf(0)
        rule.setGlimmerThemeContent {
            Box(
                Modifier.size(100.dp)
                    .surface(enabled = enabled, onClick = { count++ })
                    .testTag("surface")
            ) {
                Text("$count")
            }
        }
        rule
            .onNodeWithTag("surface")
            .assert(isFocusable())
            .assert(isNotFocused())
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsEnabled()
            .assertTextEquals("0")
            .performClick()
            .assertTextEquals("1")

        rule.runOnIdle { enabled = false }

        rule
            .onNodeWithTag("surface")
            .assert(isNotFocusable())
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsNotEnabled()
            .assertTextEquals("1")
            // Click should not do anything
            .performClick()
            .assertTextEquals("1")

        rule.runOnIdle { enabled = true }

        rule
            .onNodeWithTag("surface")
            .assert(isFocusable())
            .assert(isNotFocused())
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsEnabled()
            .assertTextEquals("1")
            .performClick()
            .assertTextEquals("2")
    }

    @Test
    fun clickableSurface_semantics_customRole() {
        var count by mutableStateOf(0)
        rule.setGlimmerThemeContent {
            Box(
                Modifier.size(100.dp)
                    .semantics { role = Role.Button }
                    .surface(onClick = { count++ })
                    .testTag("surface")
            ) {
                Text("$count")
            }
        }
        rule
            .onNodeWithTag("surface")
            .assert(isFocusable())
            .assert(isNotFocused())
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
            .assertTextEquals("0")
    }

    @Test
    fun focusableSurface_inspectorValue() {
        rule.setContent {
            val modifiers = Modifier.surface().toList()
            assertThat((modifiers[0] as InspectableValue).nameFallback).isEqualTo("graphicsLayer")
            assertThat((modifiers[1] as InspectableValue).nameFallback)
                .isEqualTo("contentColorProvider")
            val surfaceModifier = modifiers[2] as InspectableValue
            assertThat(surfaceModifier.nameFallback).isEqualTo("surface")
            assertThat(surfaceModifier.valueOverride).isNull()
            assertThat(surfaceModifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly("shape", "border", "interactionSource")
            assertThat((modifiers[3] as InspectableValue).nameFallback).isEqualTo("background")
            assertThat((modifiers[4] as InspectableValue).nameFallback).isEqualTo("focusable")
        }
    }

    @Test
    fun clickableSurface_inspectorValue() {
        rule.setContent {
            val modifiers = Modifier.surface(onClick = {}).toList()
            assertThat((modifiers[0] as InspectableValue).nameFallback).isEqualTo("graphicsLayer")
            assertThat((modifiers[1] as InspectableValue).nameFallback)
                .isEqualTo("contentColorProvider")
            val surfaceModifier = modifiers[2] as InspectableValue
            assertThat(surfaceModifier.nameFallback).isEqualTo("surface")
            assertThat(surfaceModifier.valueOverride).isNull()
            assertThat(surfaceModifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly("shape", "border", "interactionSource")
            assertThat((modifiers[3] as InspectableValue).nameFallback).isEqualTo("background")
            assertThat((modifiers[4] as InspectableValue).nameFallback).isEqualTo("clickable")
        }
    }

    @Test
    fun focusableSurface_clipsContent() {
        rule.setGlimmerThemeContent {
            with(LocalDensity.current) {
                val outerSize = 100.toDp()
                val innerSize = 50.toDp()
                Box(Modifier.size(outerSize).testTag("outerBox").background(Color.Red)) {
                    Box(
                        Modifier.size(innerSize)
                            .surface(shape = RectangleShape, color = Color.Blue, border = null)
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
    fun clickableSurface_clipsContent() {
        rule.setGlimmerThemeContent {
            with(LocalDensity.current) {
                val outerSize = 100.toDp()
                val innerSize = 50.toDp()
                Box(Modifier.size(outerSize).testTag("outerBox").background(Color.Red)) {
                    Box(
                        Modifier.size(innerSize)
                            .surface(
                                shape = RectangleShape,
                                color = Color.Blue,
                                border = null,
                                onClick = {},
                            )
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
    fun clickableSurface_clipsInput() {
        var clicks = 0
        rule.setGlimmerThemeContent {
            Box(
                Modifier.size(100.dp)
                    .surface(shape = CircleShape, onClick = { clicks++ })
                    .testTag("surface")
            )
        }
        // Click in the corner, outside of the bounds of the shape
        rule.onNodeWithTag("surface").performTouchInput {
            down(Offset(1f, 1f))
            move()
            up()
        }
        // The click should be ignored
        rule.runOnIdle { assertThat(clicks).isEqualTo(0) }
        // Click in the center, inside the shape
        rule.onNodeWithTag("surface").performTouchInput {
            down(center)
            move()
            up()
        }
        // The click should be handled
        rule.runOnIdle { assertThat(clicks).isEqualTo(1) }
    }

    @Test
    fun surfaceDefaults_cachesBorder() {
        lateinit var defaultBorder: BorderStroke
        lateinit var anotherDefaultBorder: BorderStroke
        lateinit var customBorder: BorderStroke
        rule.setGlimmerThemeContent {
            defaultBorder = SurfaceDefaults.border()
            anotherDefaultBorder = SurfaceDefaults.border()
            customBorder = SurfaceDefaults.border(color = Color.Red)
        }

        rule.runOnIdle {
            assertThat(defaultBorder).isSameInstanceAs(anotherDefaultBorder)
            assertThat(defaultBorder).isNotEqualTo(customBorder)
        }
    }

    @Test
    fun surfaceDefaults_borderValues() {
        lateinit var defaultBorder: BorderStroke
        lateinit var customBorder: BorderStroke
        var outline: Color = Color.Unspecified
        rule.setGlimmerThemeContent {
            outline = GlimmerTheme.colors.outline
            defaultBorder = SurfaceDefaults.border()
            customBorder = SurfaceDefaults.border(color = Color.Red)
        }

        rule.runOnIdle {
            assertThat((defaultBorder.brush as SolidColor).value).isEqualTo(outline)
            assertThat(defaultBorder.width).isEqualTo(2.dp)
            assertThat((customBorder.brush as SolidColor).value).isEqualTo(Color.Red)
            assertThat(customBorder.width).isEqualTo(2.dp)
        }
    }

    @Test
    fun focusableSurface_changeShape_borderChanges() {
        var roundedCorners by mutableStateOf(true)

        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(40f.toDp())
                        .background(Color.Blue)
                        .surface(
                            shape = if (roundedCorners) RoundedCornerShape(5f) else RectangleShape,
                            border = BorderStroke(1f.toDp(), Color.Red),
                        )
                        .testTag("surface")
                )
            }
        }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should be rounded, so the top and bottom of the left edge will be blue, and the
            // center will be red
            assertThat(Color.Blue).isEqualTo(map[0, 0])
            assertThat(Color.Red).isEqualTo(map[0, (height - 1) / 2])
            assertThat(Color.Blue).isEqualTo(map[0, height - 1])
        }

        rule.runOnIdle { roundedCorners = false }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should no longer be rounded, so left edge should be fully red
            assertThat(Color.Red).isEqualTo(map[0, 0])
            assertThat(Color.Red).isEqualTo(map[0, (height - 1) / 2])
            assertThat(Color.Red).isEqualTo(map[0, height - 1])
        }
    }

    @Test
    fun clickableSurface_changeShape_borderChanges() {
        var roundedCorners by mutableStateOf(true)

        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(40f.toDp())
                        .background(Color.Blue)
                        .surface(
                            shape = if (roundedCorners) RoundedCornerShape(5f) else RectangleShape,
                            border = BorderStroke(1f.toDp(), Color.Red),
                            onClick = {},
                        )
                        .testTag("surface")
                )
            }
        }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should be rounded, so the top and bottom of the left edge will be blue, and the
            // center will be red
            assertThat(Color.Blue).isEqualTo(map[0, 0])
            assertThat(Color.Red).isEqualTo(map[0, (height - 1) / 2])
            assertThat(Color.Blue).isEqualTo(map[0, height - 1])
        }

        rule.runOnIdle { roundedCorners = false }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should no longer be rounded, so left edge should be fully red
            assertThat(Color.Red).isEqualTo(map[0, 0])
            assertThat(Color.Red).isEqualTo(map[0, (height - 1) / 2])
            assertThat(Color.Red).isEqualTo(map[0, height - 1])
        }
    }

    @Test
    fun focusableSurface_observableShape_roundedOutline_borderChanges() {
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

        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(40f.toDp())
                        .background(Color.Blue)
                        .surface(
                            shape = roundedCornersShape,
                            border = BorderStroke(1f.toDp(), Color.Red),
                        )
                        .testTag("surface")
                )
            }
        }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should be rounded, so the top and bottom of the left edge will be blue, and the
            // center will be red
            assertThat(Color.Blue).isEqualTo(map[0, 0])
            assertThat(Color.Red).isEqualTo(map[0, (height - 1) / 2])
            assertThat(Color.Blue).isEqualTo(map[0, height - 1])
        }

        rule.runOnIdle { roundedCorners = false }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should no longer be rounded, so left edge should be fully red
            assertThat(Color.Red).isEqualTo(map[0, 0])
            assertThat(Color.Red).isEqualTo(map[0, (height - 1) / 2])
            assertThat(Color.Red).isEqualTo(map[0, height - 1])
        }
    }

    @Test
    fun clickableSurface_observableShape_roundedOutline_borderChanges() {
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

        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(40f.toDp())
                        .background(Color.Blue)
                        .surface(
                            shape = roundedCornersShape,
                            border = BorderStroke(1f.toDp(), Color.Red),
                            onClick = {},
                        )
                        .testTag("surface")
                )
            }
        }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should be rounded, so the top and bottom of the left edge will be blue, and the
            // center will be red
            assertThat(Color.Blue).isEqualTo(map[0, 0])
            assertThat(Color.Red).isEqualTo(map[0, (height - 1) / 2])
            assertThat(Color.Blue).isEqualTo(map[0, height - 1])
        }

        rule.runOnIdle { roundedCorners = false }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should no longer be rounded, so left edge should be fully red
            assertThat(Color.Red).isEqualTo(map[0, 0])
            assertThat(Color.Red).isEqualTo(map[0, (height - 1) / 2])
            assertThat(Color.Red).isEqualTo(map[0, height - 1])
        }
    }

    @Test
    fun focusableSurface_observableShape_genericOutline_samePath_borderChanges() {
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

        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(400f.toDp())
                        .background(Color.Blue)
                        .surface(
                            shape = roundedCornersShape,
                            border = BorderStroke(1f.toDp(), Color.Red),
                        )
                        .testTag("surface")
                )
            }
        }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should be rounded, so the top and bottom of the left edge will be blue, and the
            // center will be red
            assertThat(Color.Blue).isEqualTo(map[0, 0])
            assertThat(Color.Red).isEqualTo(map[0, (height - 1) / 2])
            // The last pixel fails to render properly on some emulators, so just assert the one
            // before instead - b/267371353
            assertThat(Color.Blue).isEqualTo(map[0, height - 2])
        }

        rule.runOnIdle { roundedCorners = false }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should no longer be rounded, so left edge should be fully red
            assertThat(Color.Red).isEqualTo(map[0, 0])
            assertThat(Color.Red).isEqualTo(map[0, (height - 1) / 2])
            // The last pixel fails to render properly on some emulators, so just assert the one
            // before instead - b/267371353
            assertThat(Color.Red).isEqualTo(map[0, height - 2])
        }
    }

    @Test
    fun clickableSurface_observableShape_genericOutline_samePath_borderChanges() {
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

        rule.setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(400f.toDp())
                        .background(Color.Blue)
                        .surface(
                            shape = roundedCornersShape,
                            border = BorderStroke(1f.toDp(), Color.Red),
                            onClick = {},
                        )
                        .testTag("surface")
                )
            }
        }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should be rounded, so the top and bottom of the left edge will be blue, and the
            // center will be red
            assertThat(Color.Blue).isEqualTo(map[0, 0])
            assertThat(Color.Red).isEqualTo(map[0, (height - 1) / 2])
            // The last pixel fails to render properly on some emulators, so just assert the one
            // before instead - b/267371353
            assertThat(Color.Blue).isEqualTo(map[0, height - 2])
        }

        rule.runOnIdle { roundedCorners = false }

        rule.onNodeWithTag("surface").captureToImage().run {
            val map = toPixelMap()
            // We should no longer be rounded, so left edge should be fully red
            assertThat(Color.Red).isEqualTo(map[0, 0])
            assertThat(Color.Red).isEqualTo(map[0, (height - 1) / 2])
            // The last pixel fails to render properly on some emulators, so just assert the one
            // before instead - b/267371353
            assertThat(Color.Red).isEqualTo(map[0, height - 2])
        }
    }

    @Test
    fun focusableSurface_providesContentColor_default() {
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
    fun clickableSurface_providesContentColor_default() {
        var color: Color = Color.Unspecified
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(onClick = {})
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
    fun focusableSurface_providesContentColor_calculatedFromBackground() {
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
    fun clickableSurface_providesContentColor_calculatedFromBackground() {
        var node: DelegatableNode? = null
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(color = Color.White, onClick = {})
                    .then(DelegatableNodeProviderElement { node = it })
            )
        }

        // Surface color is white, so the content color should be black
        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(Color.Black) }
    }

    @Test
    fun focusableSurface_providesContentColor_updates_backgroundColor() {
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
    fun clickableSurface_providesContentColor_updates_backgroundColor() {
        var backgroundColor by mutableStateOf(Color.White)
        var node: DelegatableNode? = null
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(color = backgroundColor, onClick = {})
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
    fun focusableSurface_providesContentColor_updates_contentColor() {
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
    fun clickableSurface_providesContentColor_updates_contentColor() {
        var expectedColor by mutableStateOf(Color.White)
        var node: DelegatableNode? = null
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(contentColor = expectedColor, onClick = {})
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
    fun focusableSurface_emitsFocusInteractions() {
        val interactionSource = MutableInteractionSource()
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        lateinit var scope: CoroutineScope

        rule.setGlimmerThemeContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(focusRequester)
                        .surface(interactionSource = interactionSource)
                        .testTag("surface")
                )
                Box(Modifier.size(100.dp).focusRequester(otherFocusRequester).surface())
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        rule.runOnIdle { otherFocusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun clickableSurface_emitsFocusInteractions() {
        val interactionSource = MutableInteractionSource()
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        lateinit var scope: CoroutineScope

        rule.setGlimmerThemeContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(focusRequester)
                        .surface(interactionSource = interactionSource, onClick = {})
                        .testTag("surface")
                )
                Box(Modifier.size(100.dp).focusRequester(otherFocusRequester).surface(onClick = {}))
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        rule.runOnIdle { otherFocusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun focusableSurface_resetsFocusInteractions_whenNoLongerFocusable() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        var focusable by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.setGlimmerThemeContent {
            scope = rememberCoroutineScope()
            Box(
                Modifier.size(100.dp)
                    .focusRequester(focusRequester)
                    .surface(focusable = focusable, interactionSource = interactionSource)
                    .testTag("surface")
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        // Make surface no longer focusable, Interaction should be gone
        rule.runOnIdle { focusable = false }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun clickableSurface_resetsFocusInteractions_whenNoLongerEnabled() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        var enabled by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.setGlimmerThemeContent {
            scope = rememberCoroutineScope()
            Box(
                Modifier.size(100.dp)
                    .focusRequester(focusRequester)
                    .surface(enabled = enabled, interactionSource = interactionSource, onClick = {})
                    .testTag("surface")
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        // Make surface no longer enabled, Interaction should be gone
        rule.runOnIdle { enabled = false }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun focusableSurface_focusHighlight_appearsAndDisappearsWithFocusChange() {
        rule.mainClock.autoAdvance = false

        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        rule.setGlimmerThemeContent {
            Column {
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(focusRequester)
                        .surface(shape = RectangleShape, border = BorderStroke(2.dp, Color.Red))
                        .testTag("surface")
                )
                Box(Modifier.size(100.dp).focusRequester(otherFocusRequester).surface())
            }
        }

        // Border should be red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(Color.Red)
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // There is an enter animation, so advance a small time after the animation starts
        rule.mainClock.advanceTimeBy(50)

        // The focused highlight should show, so the start of the border will not be fully red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isNotEqualTo(Color.Red)
        }

        rule.runOnIdle { otherFocusRequester.requestFocus() }

        // Advance past the exit animation
        rule.mainClock.advanceTimeBy(500)

        // Focused highlight should disappear, so the border should be red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(Color.Red)
        }
    }

    @Test
    fun clickableSurface_focusHighlight_appearsAndDisappearsWithFocusChange() {
        rule.mainClock.autoAdvance = false

        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        rule.setGlimmerThemeContent {
            Column {
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(focusRequester)
                        .surface(
                            shape = RectangleShape,
                            border = BorderStroke(2.dp, Color.Red),
                            onClick = {},
                        )
                        .testTag("surface")
                )
                Box(Modifier.size(100.dp).focusRequester(otherFocusRequester).surface(onClick = {}))
            }
        }

        // Border should be red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(Color.Red)
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // There is an enter animation, so advance a small time after the animation starts
        rule.mainClock.advanceTimeBy(50)

        // The focused highlight should show, so the start of the border will not be fully red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isNotEqualTo(Color.Red)
        }

        rule.runOnIdle { otherFocusRequester.requestFocus() }

        // Advance past the exit animation
        rule.mainClock.advanceTimeBy(500)

        // Focused highlight should disappear, so the border should be red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(Color.Red)
        }
    }

    @Test
    fun focusableSurface_focusHighlight_animationPlaysOnce() {
        rule.mainClock.autoAdvance = false

        val matcher = MSSIMMatcher()
        val focusRequester = FocusRequester()

        rule.setGlimmerThemeContent {
            Column {
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(focusRequester)
                        .surface(shape = RectangleShape, border = BorderStroke(2.dp, Color.Red))
                        .testTag("surface")
                )
            }
        }

        // Border should be red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(Color.Red)
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // Capture the initial focus state before the animation starts
        val initialFrame = rule.onNodeWithTag("surface").captureToImage()

        rule.mainClock.advanceTimeBy(1000)

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

        // Advance past the end of the animation
        rule.mainClock.advanceTimeBy(7000)

        // Capture the focus state after the animation has settled
        val afterAnimation = rule.onNodeWithTag("surface").captureToImage()

        // Advance a bit forward again to make sure there is no change
        rule.mainClock.advanceTimeBy(1000)

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
    fun clickableSurface_focusHighlight_animationPlaysOnce() {
        rule.mainClock.autoAdvance = false

        val matcher = MSSIMMatcher()
        val focusRequester = FocusRequester()

        rule.setGlimmerThemeContent {
            Column {
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(focusRequester)
                        .surface(
                            shape = RectangleShape,
                            border = BorderStroke(2.dp, Color.Red),
                            onClick = {},
                        )
                        .testTag("surface")
                )
            }
        }

        // Border should be red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(Color.Red)
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // Capture the initial focus state before the animation starts
        val initialFrame = rule.onNodeWithTag("surface").captureToImage()

        rule.mainClock.advanceTimeBy(1000)

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

        // Advance past the end of the animation
        rule.mainClock.advanceTimeBy(7000)

        // Capture the focus state after the animation has settled
        val afterAnimation = rule.onNodeWithTag("surface").captureToImage()

        // Advance a bit forward again to make sure there is no change
        rule.mainClock.advanceTimeBy(1000)

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
    fun focusableSurface_focusHighlight_animationResetsWhenBecomingFocusedAgain() {
        rule.mainClock.autoAdvance = false

        val matcher = MSSIMMatcher()
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        rule.setGlimmerThemeContent {
            Column {
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(focusRequester)
                        .surface(shape = RectangleShape, border = BorderStroke(2.dp, Color.Red))
                        .testTag("surface")
                )
                Box(Modifier.size(100.dp).focusRequester(otherFocusRequester).surface())
            }
        }

        // Border should be red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(Color.Red)
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // Capture the initial focus state before the animation starts
        val initialFrame = rule.onNodeWithTag("surface").captureToImage()

        rule.mainClock.advanceTimeBy(1000)

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

        // Move focus back to the initial surface
        rule.runOnIdle { focusRequester.requestFocus() }

        // Capture the initial focus state before the animation starts
        val initialFrame2 = rule.onNodeWithTag("surface").captureToImage()

        rule.mainClock.advanceTimeBy(1000)

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
    fun clickableSurface_focusHighlight_animationResetsWhenBecomingFocusedAgain() {
        rule.mainClock.autoAdvance = false

        val matcher = MSSIMMatcher()
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        rule.setGlimmerThemeContent {
            Column {
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(focusRequester)
                        .surface(
                            shape = RectangleShape,
                            border = BorderStroke(2.dp, Color.Red),
                            onClick = {},
                        )
                        .testTag("surface")
                )
                Box(Modifier.size(100.dp).focusRequester(otherFocusRequester).surface(onClick = {}))
            }
        }

        // Border should be red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(Color.Red)
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // Capture the initial focus state before the animation starts
        val initialFrame = rule.onNodeWithTag("surface").captureToImage()

        rule.mainClock.advanceTimeBy(1000)

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

        // Move focus back to the initial surface
        rule.runOnIdle { focusRequester.requestFocus() }

        // Capture the initial focus state before the animation starts
        val initialFrame2 = rule.onNodeWithTag("surface").captureToImage()

        rule.mainClock.advanceTimeBy(1000)

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
    fun focusableSurface_focusHighlight_resetWhenChangingInteractionSource() {
        rule.mainClock.autoAdvance = false

        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()
        var interactionSource by mutableStateOf(MutableInteractionSource())

        rule.setGlimmerThemeContent {
            Column {
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(focusRequester)
                        .surface(
                            shape = RectangleShape,
                            border = BorderStroke(2.dp, Color.Red),
                            interactionSource = interactionSource,
                        )
                        .testTag("surface")
                )
                Box(Modifier.size(100.dp).focusRequester(otherFocusRequester).surface())
            }
        }

        // Border should be red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(Color.Red)
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // There is an enter animation, so advance a small time after the animation starts
        rule.mainClock.advanceTimeBy(50)

        // The focused highlight should show, so the start of the border will not be fully red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isNotEqualTo(Color.Red)
        }

        // Change the interaction source - even though the node is technically still focused, we
        // should reset the highlight as the interaction source changed. In the future if we
        // directly delegate to focusable we would be able to maintain focus in that case
        rule.runOnIdle { interactionSource = MutableInteractionSource() }

        // Advance past the exit animation
        rule.mainClock.advanceTimeBy(500)

        // Focused highlight should disappear, so the border should be red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(Color.Red)
        }

        // Move focus away from and back to the surface
        rule.runOnIdle { otherFocusRequester.requestFocus() }
        rule.runOnIdle { focusRequester.requestFocus() }

        // There is an enter animation, so advance a small time after the animation starts
        rule.mainClock.advanceTimeBy(50)

        // The new interaction source will see the new focus, so the focused highlight should show
        // again
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isNotEqualTo(Color.Red)
        }
    }

    @Test
    fun clickableSurface_focusHighlight_resetWhenChangingInteractionSource() {
        rule.mainClock.autoAdvance = false

        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()
        var interactionSource by mutableStateOf(MutableInteractionSource())

        rule.setGlimmerThemeContent {
            Column {
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(focusRequester)
                        .surface(
                            shape = RectangleShape,
                            border = BorderStroke(2.dp, Color.Red),
                            interactionSource = interactionSource,
                            onClick = {},
                        )
                        .testTag("surface")
                )
                Box(Modifier.size(100.dp).focusRequester(otherFocusRequester).surface(onClick = {}))
            }
        }

        // Border should be red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(Color.Red)
        }

        rule.runOnIdle { focusRequester.requestFocus() }

        // There is an enter animation, so advance a small time after the animation starts
        rule.mainClock.advanceTimeBy(50)

        // The focused highlight should show, so the start of the border will not be fully red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isNotEqualTo(Color.Red)
        }

        // Change the interaction source - even though the node is technically still focused, we
        // should reset the highlight as the interaction source changed. In the future if we
        // directly delegate to clickable we would be able to maintain focus in that case
        rule.runOnIdle { interactionSource = MutableInteractionSource() }

        // Advance past the exit animation
        rule.mainClock.advanceTimeBy(500)

        // Focused highlight should disappear, so the border should be red
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isEqualTo(Color.Red)
        }

        // Move focus away from and back to the surface
        rule.runOnIdle { otherFocusRequester.requestFocus() }
        rule.runOnIdle { focusRequester.requestFocus() }

        // There is an enter animation, so advance a small time after the animation starts
        rule.mainClock.advanceTimeBy(50)

        // The new interaction source will see the new focus, so the focused highlight should show
        // again
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(1, 1)).isNotEqualTo(Color.Red)
        }
    }

    @Test
    fun clickableSurface_emitsPressInteractions() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.setGlimmerThemeContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    Modifier.size(100.dp)
                        .surface(interactionSource = interactionSource, onClick = {})
                        .testTag("surface")
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("surface").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("surface").performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun clickableSurface_resetsPressInteractions_whenNoLongerEnabled() {
        val interactionSource = MutableInteractionSource()
        var enabled by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.setGlimmerThemeContent {
            scope = rememberCoroutineScope()
            Box(
                Modifier.size(100.dp)
                    .surface(enabled = enabled, interactionSource = interactionSource, onClick = {})
                    .testTag("surface")
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("surface").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Make surface no longer enabled, Interaction should be gone
        rule.runOnIdle { enabled = false }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun clickableSurface_pressedOverlay_appearsAndDisappearsWithPressChange() {
        rule.mainClock.autoAdvance = false

        rule.setGlimmerThemeContent {
            Column { Box(Modifier.size(100.dp).surface(onClick = {}).testTag("surface")) }
        }

        // The center of the surface should be black
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(Color.Black)
        }

        // Start a press
        rule.onNodeWithTag("surface").performTouchInput { down(center) }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should be showing
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            val expectedColor = Color.White.copy(alpha = 0.16f).compositeOver(Color.Black)
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedColor)
        }

        // Release press
        rule.onNodeWithTag("surface").performTouchInput { up() }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should disappear, so the center of the surface should be black again
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(Color.Black)
        }
    }

    @Test
    fun clickableSurface_pressedOverlay_resetWhenChangingInteractionSource() {
        rule.mainClock.autoAdvance = false

        var interactionSource by mutableStateOf(MutableInteractionSource())

        rule.setGlimmerThemeContent {
            Column {
                Box(
                    Modifier.size(100.dp)
                        .surface(interactionSource = interactionSource, onClick = {})
                        .testTag("surface")
                )
            }
        }

        // The center of the surface should be black
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(Color.Black)
        }

        // Start a press
        rule.onNodeWithTag("surface").performTouchInput { down(center) }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should be showing
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            val expectedColor = Color.White.copy(alpha = 0.16f).compositeOver(Color.Black)
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedColor)
        }

        // Change the interaction source - this should cause us to animate away from pressed
        rule.runOnIdle { interactionSource = MutableInteractionSource() }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should disappear, so the center of the surface should be black again
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(Color.Black)
        }

        // Release and start another press
        rule.onNodeWithTag("surface").performTouchInput {
            up()
            down(center)
        }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should be showing again
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            val expectedColor = Color.White.copy(alpha = 0.16f).compositeOver(Color.Black)
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedColor)
        }
    }

    /**
     * Even though the focusable surface doesn't handle clicks itself, it should still respond to
     * externally provided PressInteractions, for example when used with a separate gesture
     * modifier.
     */
    @Test
    fun focusableSurface_pressedOverlay_appearsAndDisappearsWithPressChange() {
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

        // The center of the surface should be black
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(Color.Black)
        }

        val press = PressInteraction.Press(Offset.Zero)

        // Send press interaction
        rule.runOnIdle { scope.launch { interactionSource.emit(press) } }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should be showing
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            val expectedColor = Color.White.copy(alpha = 0.16f).compositeOver(Color.Black)
            assertThat(get(width / 2, height / 2)).isEqualTo(expectedColor)
        }

        // Send release interaction
        rule.runOnIdle { scope.launch { interactionSource.emit(PressInteraction.Release(press)) } }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should disappear, so the center of the surface should be black again
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(Color.Black)
        }
    }

    @Test
    fun clickableSurface_pressedOverlay_hasAMinimumDuration() {
        rule.mainClock.autoAdvance = false

        rule.setGlimmerThemeContent {
            Column { Box(Modifier.size(100.dp).surface(onClick = {}).testTag("surface")) }
        }

        // The center of the surface should be black
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(Color.Black)
        }

        // Start a press, and immediately release
        rule.onNodeWithTag("surface").performTouchInput {
            down(center)
            up()
        }

        // Advance a short amount of time
        rule.mainClock.advanceTimeBy(150)

        // The press overlay should continue to animate for a minimum duration, and then fade out.
        // If there was no minimum duration, the animation would have ended already - so
        // make sure the color is not equal to the base color.
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isNotEqualTo(Color.Black)
        }

        // Advance until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)

        // The press overlay should disappear after the minimum duration, so the center of the
        // surface should be black again
        rule.onNodeWithTag("surface").captureToImage().toPixelMap().run {
            assertThat(get(width / 2, height / 2)).isEqualTo(Color.Black)
        }
    }
}

private fun ImageBitmap.toIntArray(): IntArray {
    val bitmapArray = IntArray(width * height)
    asAndroidBitmap().getPixels(bitmapArray, 0, width, 0, 0, width, height)
    return bitmapArray
}
