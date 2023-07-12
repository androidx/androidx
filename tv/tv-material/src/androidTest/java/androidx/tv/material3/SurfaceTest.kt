/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.testutils.assertShape
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.tv.material3.tokens.Elevation
import com.google.common.truth.Truth
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun assertFloatPrecision(a: Float, b: Float) =
    Truth.assertThat(abs(a - b)).isLessThan(0.0001f)

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalTestApi::class,
    ExperimentalTvMaterial3Api::class
)
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class SurfaceTest {

    @get:Rule
    val rule = createComposeRule()

    private fun Int.toDp(): Dp = with(rule.density) { toDp() }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun originalOrderingWhenTheDefaultElevationIsUsed() {
        rule.setContent {
            Box(
                Modifier
                    .size(10.toDp())
                    .semantics(mergeDescendants = true) {}
                    .testTag("box")
            ) {
                Surface(
                    onClick = {},
                    shape = ClickableSurfaceDefaults.shape(
                        shape = RectangleShape
                    ),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Yellow
                    )
                ) {
                    Box(Modifier.fillMaxSize())
                }
                Surface(
                    onClick = {},
                    shape = ClickableSurfaceDefaults.shape(
                        shape = RectangleShape
                    ),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Green
                    )
                ) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        rule.onNodeWithTag("box").captureToImage().assertShape(
            density = rule.density,
            shape = RectangleShape,
            shapeColor = Color.Green,
            backgroundColor = Color.White
        )
    }

    @Test
    fun absoluteElevationCompositionLocalIsSet() {
        var outerElevation: Dp? = null
        var innerElevation: Dp? = null
        rule.setContent {
            Surface(onClick = {}, tonalElevation = 2.toDp()) {
                outerElevation = LocalAbsoluteTonalElevation.current
                Surface(onClick = {}, tonalElevation = 4.toDp()) {
                    innerElevation = LocalAbsoluteTonalElevation.current
                }
            }
        }

        rule.runOnIdle {
            innerElevation?.let { nnInnerElevation ->
                assertFloatPrecision(nnInnerElevation.value, 6.toDp().value)
            }
            outerElevation?.let { nnOuterElevation ->
                assertFloatPrecision(nnOuterElevation.value, 2.toDp().value)
            }
        }
    }

    /**
     * Tests that composed modifiers applied to Surface are applied within the changes to
     * [LocalContentColor], so they can consume the updated values.
     */
    @Test
    fun contentColorSetBeforeModifier() {
        var contentColor: Color = Color.Unspecified
        val expectedColor = Color.Blue
        rule.setContent {
            CompositionLocalProvider(LocalContentColor provides Color.Red) {
                Surface(
                    modifier = Modifier.composed {
                        contentColor = LocalContentColor.current
                        Modifier
                    },
                    onClick = {},
                    tonalElevation = 2.toDp(),
                    colors = ClickableSurfaceDefaults.colors(contentColor = expectedColor)
                ) {}
            }
        }

        rule.runOnIdle {
            Truth.assertThat(contentColor).isEqualTo(expectedColor)
        }
    }

    @Test
    fun clickableOverload_semantics() {
        val count = mutableStateOf(0)
        rule.setContent {
            Surface(
                modifier = Modifier
                    .testTag("surface"),
                onClick = { count.value += 1 }
            ) {
                Text("${count.value}")
                Spacer(Modifier.size(30.toDp()))
            }
        }
        rule.onNodeWithTag("surface")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertHasClickAction()
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("0")
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .assertTextEquals("1")
    }

    @Test
    fun clickableOverload_customSemantics() {
        val count = mutableStateOf(0)
        rule.setContent {
            Surface(
                modifier = Modifier
                    .semantics { role = Role.Tab }
                    .testTag("surface"),
                onClick = { count.value += 1 },
            ) {
                Text("${count.value}")
                Spacer(Modifier.size(30.toDp()))
            }
        }
        rule.onNodeWithTag("surface")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("0")
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .assertTextEquals("1")
    }

    @Test
    fun clickableOverload_clickAction() {
        val count = mutableStateOf(0)

        rule.setContent {
            Surface(
                modifier = Modifier
                    .testTag("surface"),
                onClick = { count.value += 1 }
            ) {
                Spacer(modifier = Modifier.size(30.toDp()))
            }
        }
        rule.onNodeWithTag("surface")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(count.value).isEqualTo(1)

        rule.onNodeWithTag("surface").performKeyInput { pressKey(Key.DirectionCenter) }
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(count.value).isEqualTo(3)
    }

    @Test
    fun clickableSurface_onDisable_clickFails() {
        val count = mutableStateOf(0f)
        val enabled = mutableStateOf(true)

        rule.setContent {
            Surface(
                modifier = Modifier
                    .testTag("surface"),
                onClick = { count.value += 1 },
                enabled = enabled.value
            ) {
                Spacer(Modifier.size(30.toDp()))
            }
        }
        rule.onNodeWithTag("surface")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsEnabled()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        Truth.assertThat(count.value).isEqualTo(1)
        rule.runOnIdle {
            enabled.value = false
        }

        rule.onNodeWithTag("surface")
            .assertIsNotEnabled()
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(count.value).isEqualTo(1)
    }

    @Test
    fun clickableOverload_interactionSource() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            Surface(
                modifier = Modifier
                    .testTag("surface"),
                onClick = {},
                interactionSource = interactionSource
            ) {
                Spacer(Modifier.size(30.toDp()))
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("surface")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { keyDown(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(2)
            Truth.assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("surface").performKeyInput { keyUp(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(3)
            Truth.assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            Truth.assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
            Truth.assertThat(interactions[2]).isInstanceOf(PressInteraction.Release::class.java)
            Truth.assertThat((interactions[2] as PressInteraction.Release).press)
                .isEqualTo(interactions[1])
        }
    }

    @Test
    fun clickableSurface_reactsToStateChange() {
        val interactionSource = MutableInteractionSource()
        var isPressed by mutableStateOf(false)

        rule.setContent {
            isPressed = interactionSource.collectIsPressedAsState().value
            Surface(
                modifier = Modifier
                    .testTag("surface")
                    .size(100.toDp()),
                onClick = {},
                interactionSource = interactionSource
            ) {}
        }

        with(rule.onNodeWithTag("surface")) {
            performSemanticsAction(SemanticsActions.RequestFocus)
            assertIsFocused()
            performKeyInput { keyDown(Key.DirectionCenter) }
        }

        rule.waitUntil(condition = { isPressed })

        Truth.assertThat(isPressed).isTrue()
    }

    @FlakyTest(bugId = 269229262)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun clickableSurface_onFocus_changesGlowColor() {
        rule.setContent {
            Surface(
                modifier = Modifier
                    .testTag("surface")
                    .size(100.toDp()),
                onClick = {},
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                ),
                glow = ClickableSurfaceDefaults.glow(
                    glow = Glow(
                        elevationColor = Color.Magenta,
                        elevation = Elevation.Level5
                    ),
                    focusedGlow = Glow(
                        elevationColor = Color.Green,
                        elevation = Elevation.Level5
                    )
                )
            ) {}
        }
        rule.onNodeWithTag("surface")
            .captureToImage()
            .assertContainsColor(Color.Magenta)

        rule.onNodeWithTag("surface")
            .performSemanticsAction(SemanticsActions.RequestFocus)

        rule.onNodeWithTag("surface")
            .captureToImage()
            .assertContainsColor(Color.Green)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun clickableSurface_onFocus_changesScaleFactor() {
        rule.setContent {
            Box(
                modifier = Modifier
                    .background(Color.Blue)
                    .size(50.toDp())
            )
            Surface(
                onClick = {},
                modifier = Modifier
                    .size(50.toDp())
                    .testTag("surface"),
                scale = ClickableSurfaceDefaults.scale(
                    focusedScale = 1.5f
                )
            ) {}
        }
        rule.onRoot().captureToImage().assertContainsColor(Color.Blue)

        rule.onNodeWithTag("surface").performSemanticsAction(SemanticsActions.RequestFocus)

        rule.onRoot().captureToImage().assertDoesNotContainColor(Color.Blue)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun clickableSurface_onFocus_showsBorder() {
        rule.setContent {
            Surface(
                onClick = { /* Do something */ },
                modifier = Modifier
                    .size(100.toDp())
                    .testTag("surface"),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(width = 5.toDp(), color = Color.Magenta)
                    )
                ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                )
            ) {}
        }

        val surface = rule.onNodeWithTag("surface")

        surface.captureToImage().assertDoesNotContainColor(Color.Magenta)

        surface.performSemanticsAction(SemanticsActions.RequestFocus)

        surface.captureToImage().assertContainsColor(Color.Magenta)
    }

    @Test
    fun toggleable_semantics() {
        var isChecked by mutableStateOf(false)
        rule.setContent {
            Surface(
                checked = isChecked,
                modifier = Modifier
                    .testTag("surface"),
                onCheckedChange = { isChecked = it }
            ) {
                Text("$isChecked")
                Spacer(Modifier.size(30.toDp()))
            }
        }
        rule.onNodeWithTag("surface")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("false")
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .assertTextEquals("true")
    }

    @Test
    fun toggleable_customSemantics() {
        var isChecked by mutableStateOf(false)
        rule.setContent {
            Surface(
                checked = isChecked,
                modifier = Modifier
                    .semantics { role = Role.Tab }
                    .testTag("surface"),
                onCheckedChange = { isChecked = it }
            ) {
                Text("$isChecked")
                Spacer(Modifier.size(30.toDp()))
            }
        }
        rule.onNodeWithTag("surface")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("false")
            .performKeyInput { pressKey(Key.DirectionCenter) }
            .assertTextEquals("true")
    }

    @Test
    fun toggleable_toggleAction() {
        var isChecked by mutableStateOf(false)

        rule.setContent {
            Surface(
                checked = isChecked,
                modifier = Modifier
                    .testTag("surface"),
                onCheckedChange = { isChecked = it }
            ) {
                Spacer(modifier = Modifier.size(30.toDp()))
            }
        }
        rule.onNodeWithTag("surface")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(isChecked).isTrue()

        rule.onNodeWithTag("surface").performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(isChecked).isFalse()
    }

    @Test
    fun toggleable_enabled_disabled() {
        var isChecked by mutableStateOf(false)
        var enabled by mutableStateOf(true)

        rule.setContent {
            Surface(
                checked = isChecked,
                modifier = Modifier
                    .testTag("surface"),
                onCheckedChange = { isChecked = it },
                enabled = enabled
            ) {
                Spacer(Modifier.size(30.toDp()))
            }
        }
        rule.onNodeWithTag("surface")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsEnabled()
            .performKeyInput { pressKey(Key.DirectionCenter) }

        Truth.assertThat(isChecked).isTrue()
        rule.runOnIdle {
            enabled = false
        }

        rule.onNodeWithTag("surface")
            .assertIsNotEnabled()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        Truth.assertThat(isChecked).isTrue()
    }

    @Test
    fun toggleable_interactionSource() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            Surface(
                checked = false,
                modifier = Modifier
                    .testTag("surface"),
                onCheckedChange = {},
                interactionSource = interactionSource
            ) {
                Spacer(Modifier.size(30.toDp()))
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("surface")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { keyDown(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(2)
            Truth.assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("surface").performKeyInput { keyUp(Key.DirectionCenter) }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(3)
            Truth.assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            Truth.assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
            Truth.assertThat(interactions[2]).isInstanceOf(PressInteraction.Release::class.java)
            Truth.assertThat((interactions[2] as PressInteraction.Release).press)
                .isEqualTo(interactions[1])
        }
    }

    @Test
    fun toggleableSurface_reactsToStateChange() {
        val interactionSource = MutableInteractionSource()
        var isPressed by mutableStateOf(false)

        rule.setContent {
            isPressed = interactionSource.collectIsPressedAsState().value
            Surface(
                checked = false,
                modifier = Modifier
                    .testTag("surface")
                    .size(100.toDp()),
                onCheckedChange = {},
                interactionSource = interactionSource
            ) {}
        }

        with(rule.onNodeWithTag("surface")) {
            performSemanticsAction(SemanticsActions.RequestFocus)
            assertIsFocused()
            performKeyInput { keyDown(Key.DirectionCenter) }
        }

        rule.waitUntil(condition = { isPressed })

        Truth.assertThat(isPressed).isTrue()
    }

    @FlakyTest(bugId = 269229262)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun toggleableSurface_onCheckedChange_changesGlowColor() {
        var isChecked by mutableStateOf(false)
        var focusManager: FocusManager? = null
        rule.setContent {
            focusManager = LocalFocusManager.current
            Surface(
                checked = isChecked,
                modifier = Modifier
                    .testTag("surface")
                    .size(100.toDp()),
                onCheckedChange = { isChecked = it },
                colors = ToggleableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    selectedContainerColor = Color.Transparent
                ),
                glow = ToggleableSurfaceDefaults.glow(
                    glow = Glow(
                        elevationColor = Color.Magenta,
                        elevation = Elevation.Level5
                    ),
                    selectedGlow = Glow(
                        elevationColor = Color.Green,
                        elevation = Elevation.Level5
                    )
                )
            ) {}
        }
        rule.onNodeWithTag("surface")
            .captureToImage()
            .assertContainsColor(Color.Magenta)

        rule.onNodeWithTag("surface")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionCenter) }

        // Remove focused state to reveal selected state
        focusManager?.clearFocus()

        rule.onNodeWithTag("surface")
            .captureToImage()
            .assertContainsColor(Color.Green)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun toggleableSurface_onCheckedChange_changesScaleFactor() {
        var isChecked by mutableStateOf(false)
        var focusManager: FocusManager? = null
        rule.setContent {
            focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .background(Color.Blue)
                    .size(50.toDp())
            )
            Surface(
                checked = isChecked,
                onCheckedChange = { isChecked = it },
                modifier = Modifier
                    .size(50.toDp())
                    .testTag("surface"),
                scale = ToggleableSurfaceDefaults.scale(
                    selectedScale = 1.5f
                )
            ) {}
        }
        rule.onRoot().captureToImage().assertContainsColor(Color.Blue)

        rule.onNodeWithTag("surface")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionCenter) }

        // Remove focused state to reveal selected state
        focusManager?.clearFocus()

        rule.onRoot().captureToImage().assertDoesNotContainColor(Color.Blue)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun toggleableSurface_onCheckedChange_showsOutline() {
        var isChecked by mutableStateOf(false)
        var focusManager: FocusManager? = null
        rule.setContent {
            focusManager = LocalFocusManager.current
            Surface(
                checked = isChecked,
                onCheckedChange = { isChecked = it },
                modifier = Modifier
                    .size(100.toDp())
                    .testTag("surface"),
                border = ToggleableSurfaceDefaults.border(
                    selectedBorder = Border(
                        border = BorderStroke(width = 5.toDp(), color = Color.Magenta)
                    )
                ),
                colors = ToggleableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    selectedContainerColor = Color.Transparent
                )
            ) {}
        }

        val surface = rule.onNodeWithTag("surface")

        surface.captureToImage().assertDoesNotContainColor(Color.Magenta)

        surface
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionCenter) }

        // Remove focused state to reveal selected state
        focusManager?.clearFocus()

        surface.captureToImage().assertContainsColor(Color.Magenta)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun surfaceChangesStyleOnChangingEnabledState() {
        var surfaceEnabled by mutableStateOf(true)

        rule.setContent {
            Surface(
                modifier = Modifier
                    .size(100.toDp())
                    .testTag("surface"),
                onClick = {},
                enabled = surfaceEnabled,
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Green,
                    disabledContainerColor = Color.Red
                )
            ) {}
        }

        // Assert surface is enabled
        rule.onNodeWithTag("surface").captureToImage().assertContainsColor(Color.Green)
        surfaceEnabled = false
        // Assert surface is disabled
        rule.onNodeWithTag("surface").captureToImage().assertContainsColor(Color.Red)
    }
}
