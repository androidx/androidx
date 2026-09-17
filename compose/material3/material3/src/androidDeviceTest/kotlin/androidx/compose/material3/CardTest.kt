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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.LocalMaterialTheme
import androidx.compose.material3.tokens.ElevatedCardTokens
import androidx.compose.material3.tokens.FilledCardTokens
import androidx.compose.material3.tokens.OutlinedCardTokens
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CardTest {

    @get:Rule val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun customShapeAndColorIsUsed() {
        val shape = CutCornerShape(8.dp)
        val background = Color.Yellow
        val cardColor = Color.Blue
        rule.setMaterialContent(lightColorScheme()) {
            Surface(color = background) {
                Box {
                    Card(
                        modifier = Modifier.semantics(mergeDescendants = true) {}.testTag("card"),
                        shape = shape,
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                    ) {
                        Box(Modifier.size(50.dp, 50.dp))
                    }
                }
            }
        }

        rule
            .onNodeWithTag("card")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = shape,
                shapeColor = cardColor,
                backgroundColor = background,
                antiAliasingGap = with(rule.density) { 1.dp.toPx() },
            )
    }

    @Test
    fun cardColors_customValues() {
        rule.setContent() {
            val colorScheme =
                MaterialTheme.colorScheme.copy(
                    surface = Color.Green,
                    onSurface = Color.Blue,
                    error = Color.Red,
                    onError = Color.Yellow,
                )
            MaterialTheme(colorScheme = colorScheme) {
                var colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
                assert(colors.contentColor == colorScheme.onSurface)
                assert(colors.disabledContentColor == colors.contentColor.copy(DisabledAlpha))

                colors = CardDefaults.cardColors(containerColor = colorScheme.error)
                assert(colors.contentColor == colorScheme.onError)
                assert(colors.disabledContentColor == colors.contentColor.copy(DisabledAlpha))
            }
        }
    }

    @Test
    fun elevatedCardColors_customValues() {
        rule.setContent() {
            val colorScheme =
                MaterialTheme.colorScheme.copy(
                    surface = Color.Green,
                    onSurface = Color.Blue,
                    error = Color.Red,
                    onError = Color.Yellow,
                )
            MaterialTheme(colorScheme = colorScheme) {
                var colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
                assert(colors.contentColor == colorScheme.onSurface)
                assert(colors.disabledContentColor == colors.contentColor.copy(DisabledAlpha))

                colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.error)
                assert(colors.contentColor == colorScheme.onError)
                assert(colors.disabledContentColor == colors.contentColor.copy(DisabledAlpha))
            }
        }
    }

    @Test
    fun outlinedCardColors_customValues() {
        rule.setContent() {
            val colorScheme =
                MaterialTheme.colorScheme.copy(
                    surface = Color.Green,
                    onSurface = Color.Blue,
                    error = Color.Red,
                    onError = Color.Yellow,
                )
            MaterialTheme(colorScheme = colorScheme) {
                var colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
                assert(colors.contentColor == colorScheme.onSurface)
                assert(colors.disabledContentColor == colors.contentColor.copy(DisabledAlpha))

                colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.error)
                assert(colors.contentColor == colorScheme.onError)
                assert(colors.disabledContentColor == colors.contentColor.copy(DisabledAlpha))
            }
        }
    }

    @Test
    fun clickableOverload_semantics() {
        val count = mutableStateOf(0)
        rule.setMaterialContent(lightColorScheme()) {
            Card(modifier = Modifier.testTag("card").clickable { count.value += 1 }) {
                Text("${count.value}")
                Spacer(Modifier.size(30.dp))
            }
        }
        rule
            .onNodeWithTag("card")
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("0")
            .performClick()
            .assertTextEquals("1")
    }

    @Test
    fun clickableOverload_clickAction() {
        val count = mutableStateOf(0f)
        rule.setMaterialContent(lightColorScheme()) {
            Card(modifier = Modifier.testTag("card").clickable { count.value += 1 }) {
                Spacer(Modifier.size(30.dp))
            }
        }
        rule.onNodeWithTag("card").performClick()
        Truth.assertThat(count.value).isEqualTo(1)

        rule.onNodeWithTag("card").performClick().performClick()
        Truth.assertThat(count.value).isEqualTo(3)
    }

    @Test
    fun clickableOverload_enabled_disabled() {
        val count = mutableStateOf(0f)
        val enabled = mutableStateOf(true)
        rule.setMaterialContent(lightColorScheme()) {
            Card(
                modifier =
                    Modifier.testTag("card")
                        .clickable(enabled = enabled.value, onClick = { count.value += 1 })
            ) {
                Spacer(Modifier.size(30.dp))
            }
        }
        rule.onNodeWithTag("card").assertIsEnabled().performClick()

        Truth.assertThat(count.value).isEqualTo(1)
        rule.runOnIdle { enabled.value = false }

        rule.onNodeWithTag("card").assertIsNotEnabled().performClick().performClick()
        Truth.assertThat(count.value).isEqualTo(1)
    }

    @Test
    fun clickableOverload_interactionSource() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Card(
                onClick = {},
                modifier = Modifier.testTag("card"),
                interactionSource = interactionSource,
            ) {
                Spacer(Modifier.size(30.dp))
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { Truth.assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("card").performTouchInput { down(center) }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(1)
            Truth.assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("card").performTouchInput { up() }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(2)
            Truth.assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            Truth.assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            Truth.assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun card_blockClicks() {
        val state = mutableStateOf(0)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Button(
                    modifier = Modifier.fillMaxSize().testTag("clickable"),
                    onClick = { state.value += 1 },
                ) {
                    Text("button fullscreen")
                }
                Card(Modifier.fillMaxSize()) {}
            }
        }
        rule.onNodeWithTag("clickable").assertHasClickAction().performClick()
        // still 0
        Truth.assertThat(state.value).isEqualTo(0)
    }

    @Test
    fun cardStyle_default_tokensResolved() {
        var scope: CardStyleScope? = null
        var colorScheme: ColorScheme? = null
        var shapes: Shapes? = null
        rule.setContent {
            val theme = LocalMaterialTheme.current
            colorScheme = theme.colorScheme
            shapes = theme.shapes
            scope = CardStyleScope(theme = theme).resolve(CardStyle.Default)
        }
        rule.runOnIdle {
            Truth.assertThat(scope!!.shadowElevation).isEqualTo(FilledCardTokens.ContainerElevation)
            Truth.assertThat(scope!!.containerColor)
                .isEqualTo(colorScheme!!.fromToken(FilledCardTokens.ContainerColor))
            Truth.assertThat(scope!!.contentColor)
                .isEqualTo(
                    colorScheme!!.contentColorFor(
                        colorScheme!!.fromToken(FilledCardTokens.ContainerColor)
                    )
                )
            Truth.assertThat(scope!!.shape)
                .isEqualTo(shapes!!.fromToken(FilledCardTokens.ContainerShape))
            Truth.assertThat(scope!!.border).isNull()
        }
    }

    @Test
    fun cardStyle_default_interactionElevations() {
        val elevations = mutableMapOf<Int, Dp>()
        rule.setContent {
            val theme = LocalMaterialTheme.current
            for (state in
                listOf(
                    ComponentState.HOVERED,
                    ComponentState.FOCUSED,
                    ComponentState.PRESSED,
                    ComponentState.DRAGGED,
                )) {
                elevations[state] =
                    CardStyleScope(theme = theme, state = ComponentState.Default.set(state, true))
                        .resolve(CardStyle.Default)
                        .shadowElevation
            }
        }
        rule.runOnIdle {
            Truth.assertThat(elevations[ComponentState.HOVERED])
                .isEqualTo(FilledCardTokens.HoverContainerElevation)
            Truth.assertThat(elevations[ComponentState.FOCUSED])
                .isEqualTo(FilledCardTokens.FocusContainerElevation)
            Truth.assertThat(elevations[ComponentState.PRESSED])
                .isEqualTo(FilledCardTokens.PressedContainerElevation)
            Truth.assertThat(elevations[ComponentState.DRAGGED])
                .isEqualTo(FilledCardTokens.DraggedContainerElevation)
        }
    }

    @Test
    fun cardStyle_default_disabledState() {
        var scope: CardStyleScope? = null
        var colorScheme: ColorScheme? = null
        rule.setContent {
            val theme = LocalMaterialTheme.current
            colorScheme = theme.colorScheme
            scope =
                CardStyleScope(theme = theme, state = ComponentState.Default.enabled(false))
                    .resolve(CardStyle.Default)
        }
        rule.runOnIdle {
            Truth.assertThat(scope!!.shadowElevation)
                .isEqualTo(FilledCardTokens.DisabledContainerElevation)
            // Matches CardDefaults.cardColors(): the disabled container color is composited over
            // the enabled container color so that the card stays opaque.
            Truth.assertThat(scope!!.containerColor)
                .isEqualTo(
                    colorScheme!!
                        .fromToken(FilledCardTokens.DisabledContainerColor)
                        .copy(alpha = FilledCardTokens.DisabledContainerOpacity)
                        .compositeOver(colorScheme!!.fromToken(FilledCardTokens.ContainerColor))
                )
            Truth.assertThat(scope!!.contentColor)
                .isEqualTo(
                    colorScheme!!
                        .contentColorFor(colorScheme!!.fromToken(FilledCardTokens.ContainerColor))
                        .copy(alpha = DisabledAlpha)
                )
        }
    }

    @Test
    fun cardStyle_elevated_tokensResolved() {
        var scope: CardStyleScope? = null
        var colorScheme: ColorScheme? = null
        rule.setContent {
            val theme = LocalMaterialTheme.current
            colorScheme = theme.colorScheme
            scope = CardStyleScope(theme = theme).resolve(CardStyle.Elevated)
        }
        rule.runOnIdle {
            Truth.assertThat(scope!!.shadowElevation)
                .isEqualTo(ElevatedCardTokens.ContainerElevation)
            Truth.assertThat(scope!!.containerColor)
                .isEqualTo(colorScheme!!.fromToken(ElevatedCardTokens.ContainerColor))
        }
    }

    @Test
    fun cardStyle_outlined_tokensResolved() {
        var scope: CardStyleScope? = null
        var colorScheme: ColorScheme? = null
        rule.setContent {
            val theme = LocalMaterialTheme.current
            colorScheme = theme.colorScheme
            scope = CardStyleScope(theme = theme).resolve(CardStyle.Outlined)
        }
        rule.runOnIdle {
            Truth.assertThat(scope!!.border)
                .isEqualTo(
                    BorderStroke(
                        OutlinedCardTokens.OutlineWidth,
                        colorScheme!!.fromToken(OutlinedCardTokens.OutlineColor),
                    )
                )
        }
    }

    @Test
    fun cardStyle_chaining_bfsStatefulPriority() {
        // Verifies CL 4269323: Deeper stateful conditions (disabled) take priority
        // over chained shallower/unconditional base values even when chained via `then`.
        var disabledScope: CardStyleScope? = null
        var enabledScope: CardStyleScope? = null
        rule.setContent {
            val theme = LocalMaterialTheme.current
            val baseStyle = CardStyle { disabled { containerColor(Color.Red) } }
            val chainedStyle = baseStyle then CardStyle { containerColor(Color.Green) }

            disabledScope =
                CardStyleScope(theme = theme, state = ComponentState.Default.enabled(false))
                    .resolve(chainedStyle)

            enabledScope =
                CardStyleScope(theme = theme, state = ComponentState.Default.enabled(true))
                    .resolve(chainedStyle)
        }
        rule.runOnIdle {
            Truth.assertThat(disabledScope!!.containerColor).isEqualTo(Color.Red)
            Truth.assertThat(enabledScope!!.containerColor).isEqualTo(Color.Green)
        }
    }

    @Test
    fun styleableCard_rendersContent() {
        rule.setMaterialContent(lightColorScheme()) {
            StyleableCard(
                modifier = Modifier.testTag("styleable_card"),
                style =
                    CardStyle {
                        containerColor(Color.Magenta)
                        shape(RoundedCornerShape(20.dp))
                        shadowElevation(6.dp)
                    },
            ) {
                Text("Styleable Content", modifier = Modifier.testTag("card_text"))
            }
        }

        rule.onNodeWithTag("styleable_card").assertExists()
        rule.onNodeWithTag("card_text").assertTextEquals("Styleable Content")
    }

    @Test
    fun styleableCard_clickableAndInteraction() {
        val count = mutableStateOf(0)
        rule.setMaterialContent(lightColorScheme()) {
            StyleableCard(
                onClick = { count.value++ },
                modifier = Modifier.testTag("clickable_styleable_card"),
                style = CardStyle.Default,
            ) {
                Text("Clickable Card")
            }
        }

        rule.onNodeWithTag("clickable_styleable_card").assertHasClickAction().performClick()
        Truth.assertThat(count.value).isEqualTo(1)
    }

    @Test
    fun componentProperties_cardProperties_default() {
        val properties = ComponentProperties.Default
        Truth.assertThat(properties.cardProperties.style).isEqualTo(CardStyle.Default)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun styleableCard_customShapeAndColor_pixelVerified() {
        val shape = CutCornerShape(8.dp)
        val background = Color.Yellow
        val cardColor = Color.Magenta
        rule.setMaterialContent(lightColorScheme()) {
            Surface(color = background) {
                Box {
                    StyleableCard(
                        modifier = Modifier.testTag("styleable_shape_card"),
                        style =
                            CardStyle {
                                containerColor(cardColor)
                                shape(shape)
                            },
                    ) {
                        Box(Modifier.size(50.dp, 50.dp))
                    }
                }
            }
        }

        rule
            .onNodeWithTag("styleable_shape_card")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = shape,
                shapeColor = cardColor,
                backgroundColor = background,
                antiAliasingGap = with(rule.density) { 1.dp.toPx() },
            )
    }
}
