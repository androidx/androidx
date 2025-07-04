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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ButtonTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun defaultSemantics() {
        rule.setGlimmerThemeContent {
            Box { Button(modifier = Modifier.testTag("button"), onClick = {}) { Text("Send") } }
        }

        rule
            .onNodeWithTag("button")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
    }

    @Test
    fun disabledSemantics() {
        rule.setGlimmerThemeContent {
            Box {
                Button(modifier = Modifier.testTag("button"), onClick = {}, enabled = false) {
                    Text("Send")
                }
            }
        }

        rule
            .onNodeWithTag("button")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
    }

    @Test
    fun findByTextAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "Send"
        rule.setGlimmerThemeContent {
            Box { Button(onClick = onClick, modifier = Modifier.testTag("button")) { Text(text) } }
        }

        rule.onNodeWithText(text).performClick()

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
    }

    @Test
    fun canBeDisabled() {
        rule.setGlimmerThemeContent {
            var enabled by remember { mutableStateOf(true) }
            val onClick = { enabled = false }
            Box {
                Button(
                    modifier = Modifier.testTag("button"),
                    onClick = onClick,
                    enabled = enabled,
                ) {
                    Text("Send")
                }
            }
        }
        rule
            .onNodeWithTag("button")
            // Confirm the button starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .performClick()
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun shapeAndColorFromThemeIsUsed() {
        lateinit var expectedShape: Shape
        val surfaceColor = Color.Blue
        rule.setGlimmerThemeContent {
            GlimmerTheme(Colors(surface = surfaceColor)) {
                expectedShape = GlimmerTheme.shapes.large
                Button(onClick = {}, modifier = Modifier.testTag("button"), border = null) {
                    Box(Modifier.size(10.dp, 10.dp))
                }
            }
        }

        rule
            .onNodeWithTag("button")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = expectedShape,
                shapeColor = surfaceColor,
                backgroundColor = Color.Black,
                antiAliasingGap = with(rule.density) { 1.dp.toPx() },
            )
    }

    @Test
    fun setsLocalTextStyle() {
        lateinit var actualTextStyle: TextStyle
        lateinit var expectedTextStyle: TextStyle
        rule.setGlimmerThemeContent {
            expectedTextStyle = GlimmerTheme.typography.bodySmall
            Button(onClick = {}) { actualTextStyle = LocalTextStyle.current }
        }

        rule.runOnIdle { assertThat(actualTextStyle).isEqualTo(expectedTextStyle) }
    }

    @Test
    fun setsContentColor() {
        var primary = Color.Unspecified
        var leadingIconContentColor = Color.Unspecified
        var trailingIconContentColor = Color.Unspecified
        var contentContentColor = Color.Unspecified
        rule.setGlimmerThemeContent {
            primary = GlimmerTheme.colors.primary
            Button(
                onClick = {},
                leadingIcon = {
                    Box(
                        DelegatableNodeProviderElement {
                            leadingIconContentColor = it?.currentContentColor() ?: Color.Unspecified
                        }
                    )
                },
                trailingIcon = {
                    Box(
                        DelegatableNodeProviderElement {
                            trailingIconContentColor =
                                it?.currentContentColor() ?: Color.Unspecified
                        }
                    )
                },
            ) {
                Box(
                    DelegatableNodeProviderElement {
                        contentContentColor = it?.currentContentColor() ?: Color.Unspecified
                    }
                )
            }
        }

        rule.runOnIdle {
            assertThat(leadingIconContentColor).isEqualTo(primary)
            assertThat(trailingIconContentColor).isEqualTo(primary)
            assertThat(contentContentColor).isEqualTo(Color.White)
        }
    }

    @Test
    fun setsLocalIconSize_buttonSizeMedium() {
        var actualLeadingIconSize: Dp? = null
        var actualTrailingIconSize: Dp? = null
        var expectedIconSize: Dp? = null
        rule.setGlimmerThemeContent {
            expectedIconSize = GlimmerTheme.iconSizes.medium
            Button(
                onClick = {},
                leadingIcon = { actualLeadingIconSize = LocalIconSize.current },
                trailingIcon = { actualTrailingIconSize = LocalIconSize.current },
            ) {}
        }

        rule.runOnIdle {
            assertThat(actualLeadingIconSize!!).isEqualTo(expectedIconSize!!)
            assertThat(actualTrailingIconSize!!).isEqualTo(expectedIconSize)
        }
    }

    @Test
    fun setsLocalIconSize_buttonSizeLarge() {
        var actualLeadingIconSize: Dp? = null
        var actualTrailingIconSize: Dp? = null
        var expectedIconSize: Dp? = null
        rule.setGlimmerThemeContent {
            expectedIconSize = GlimmerTheme.iconSizes.large
            Button(
                onClick = {},
                buttonSize = ButtonSize.Large,
                leadingIcon = { actualLeadingIconSize = LocalIconSize.current },
                trailingIcon = { actualTrailingIconSize = LocalIconSize.current },
            ) {}
        }

        rule.runOnIdle {
            assertThat(actualLeadingIconSize!!).isEqualTo(expectedIconSize!!)
            assertThat(actualTrailingIconSize!!).isEqualTo(expectedIconSize)
        }
    }

    @Test
    fun positioning() {
        rule.setGlimmerThemeContent {
            Button(onClick = { /* Do something! */ }, modifier = Modifier.testTag("button")) {
                Text("Send", modifier = Modifier.testTag("text"))
            }
        }

        val buttonBounds =
            rule.onNodeWithTag("button", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val textBounds =
            rule.onNodeWithTag("text", useUnmergedTree = true).getUnclippedBoundsInRoot()

        (textBounds.left - buttonBounds.left).assertIsEqualTo(
            16.dp,
            "padding between the start of the button and the start of the text.",
        )

        (buttonBounds.right - textBounds.right).assertIsEqualTo(
            16.dp,
            "padding between the end of the text and the end of the button.",
        )

        buttonBounds.height.assertIsEqualTo(56.dp, "height of button.")
    }

    @Test
    fun positioning_buttonSizeLarge() {
        rule.setGlimmerThemeContent {
            Button(
                onClick = { /* Do something! */ },
                modifier = Modifier.testTag("button"),
                buttonSize = ButtonSize.Large,
            ) {
                Text("Send", modifier = Modifier.testTag("text"))
            }
        }

        val buttonBounds =
            rule.onNodeWithTag("button", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val textBounds =
            rule.onNodeWithTag("text", useUnmergedTree = true).getUnclippedBoundsInRoot()

        (textBounds.left - buttonBounds.left).assertIsEqualTo(
            20.dp,
            "padding between the start of the button and the start of the text.",
        )

        (buttonBounds.right - textBounds.right).assertIsEqualTo(
            20.dp,
            "padding between the end of the text and the end of the button.",
        )

        buttonBounds.height.assertIsEqualTo(72.dp, "height of button.")
    }

    @Test
    fun positioning_withIcons() {
        rule.setGlimmerThemeContent {
            Button(
                onClick = { /* Do something! */ },
                modifier = Modifier.testTag("button"),
                leadingIcon = {
                    Icon(
                        FavoriteIcon,
                        contentDescription = "Localized description",
                        modifier = Modifier.testTag("leadingIcon"),
                    )
                },
                trailingIcon = {
                    Icon(
                        FavoriteIcon,
                        contentDescription = "Localized description",
                        modifier = Modifier.testTag("trailingIcon"),
                    )
                },
            ) {
                Text("Send", modifier = Modifier.testTag("text"))
            }
        }

        val textBounds =
            rule.onNodeWithTag("text", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val leadingIconBounds =
            rule.onNodeWithTag("leadingIcon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trailingIconBounds =
            rule.onNodeWithTag("trailingIcon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val buttonBounds =
            rule.onNodeWithTag("button", useUnmergedTree = true).getUnclippedBoundsInRoot()

        (leadingIconBounds.left - buttonBounds.left).assertIsEqualTo(
            16.dp,
            "Padding between start of button and start of leading icon.",
        )

        (textBounds.left - leadingIconBounds.right).assertIsEqualTo(
            8.dp,
            "Padding between end of leading icon and start of text.",
        )

        (trailingIconBounds.left - textBounds.right).assertIsEqualTo(
            8.dp,
            "Padding between end of text and start of trailing icon.",
        )

        (buttonBounds.right - trailingIconBounds.right).assertIsEqualTo(
            16.dp,
            "padding between end of leading icon and end of button.",
        )

        buttonBounds.height.assertIsEqualTo(56.dp, "height of button.")
    }

    @Test
    fun positioning_withIcons_buttonSizeLarge() {
        rule.setGlimmerThemeContent {
            Button(
                onClick = { /* Do something! */ },
                modifier = Modifier.testTag("button"),
                buttonSize = ButtonSize.Large,
                leadingIcon = {
                    Icon(
                        FavoriteIcon,
                        contentDescription = "Localized description",
                        modifier = Modifier.testTag("leadingIcon"),
                    )
                },
                trailingIcon = {
                    Icon(
                        FavoriteIcon,
                        contentDescription = "Localized description",
                        modifier = Modifier.testTag("trailingIcon"),
                    )
                },
            ) {
                Text("Send", modifier = Modifier.testTag("text"))
            }
        }

        val textBounds =
            rule.onNodeWithTag("text", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val leadingIconBounds =
            rule.onNodeWithTag("leadingIcon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trailingIconBounds =
            rule.onNodeWithTag("trailingIcon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val buttonBounds =
            rule.onNodeWithTag("button", useUnmergedTree = true).getUnclippedBoundsInRoot()

        (leadingIconBounds.left - buttonBounds.left).assertIsEqualTo(
            20.dp,
            "Padding between start of button and start of leading icon.",
        )

        (textBounds.left - leadingIconBounds.right).assertIsEqualTo(
            8.dp,
            "Padding between end of leading icon and start of text.",
        )

        (trailingIconBounds.left - textBounds.right).assertIsEqualTo(
            8.dp,
            "Padding between end of text and start of trailing icon.",
        )

        (buttonBounds.right - trailingIconBounds.right).assertIsEqualTo(
            20.dp,
            "padding between end of leading icon and end of button.",
        )

        buttonBounds.height.assertIsEqualTo(72.dp, "height of button.")
    }

    @Test
    fun minHeightAndMinWidthCanBeOverridden() {
        rule.setGlimmerThemeContent {
            Button(
                onClick = {},
                contentPadding = PaddingValues(),
                modifier = Modifier.requiredWidthIn(20.dp).requiredHeightIn(15.dp).testTag("button"),
            ) {
                Spacer(Modifier.requiredSize(10.dp))
            }
        }

        rule.onNodeWithTag("button").apply {
            with(getBoundsInRoot()) {
                width.assertIsEqualTo(20.dp, "width")
                height.assertIsEqualTo(15.dp, "height")
            }
        }
    }

    @Test
    fun minHeightAndMinWidthCanBeOverridden_buttonSizeLarge() {
        rule.setGlimmerThemeContent {
            Button(
                onClick = {},
                buttonSize = ButtonSize.Large,
                contentPadding = PaddingValues(),
                modifier = Modifier.requiredWidthIn(20.dp).requiredHeightIn(15.dp).testTag("button"),
            ) {
                Spacer(Modifier.requiredSize(10.dp))
            }
        }

        rule.onNodeWithTag("button").apply {
            with(getBoundsInRoot()) {
                width.assertIsEqualTo(20.dp, "width")
                height.assertIsEqualTo(15.dp, "height")
            }
        }
    }
}
