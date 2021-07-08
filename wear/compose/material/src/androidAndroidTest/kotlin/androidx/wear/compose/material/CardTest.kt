/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.wear.compose.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

public class CardBehaviourTest {
    @get:Rule
    public val rule: ComposeContentTestRule = createComposeRule()

    @Test
    public fun supports_test_tag() {
        rule.setContentWithTheme {
            Card(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            Card(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    public fun has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            Card(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertHasClickAction()
    }

    @Test
    public fun is_correctly_enabled_when_enabled_equals_true() {
        rule.setContentWithTheme {
            Card(
                onClick = {},
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertIsEnabled()
    }

    @Test
    public fun is_correctly_disabled_when_enabled_equals_false() {
        rule.setContentWithTheme {
            Card(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).assertIsNotEnabled()
    }

    @Test
    public fun responds_to_click_when_enabled() {
        var clicked = false

        rule.setContentWithTheme {
            Card(
                onClick = { clicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).performClick()

        rule.runOnIdle {
            assertEquals(true, clicked)
        }
    }

    @Test
    public fun does_not_respond_to_click_when_disabled() {
        var clicked = false

        rule.setContentWithTheme {
            Card(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0).performClick()

        rule.runOnIdle {
            assertEquals(false, clicked)
        }
    }

    @Test
    public fun has_role_button_if_explicitly_set() {
        rule.setContentWithTheme {
            Card(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG),
                role = Role.Button
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).onChildAt(0)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )
    }
}

public class CardSizeTest {
    @get:Rule
    public val rule: ComposeContentTestRule = createComposeRule()

    @Test
    public fun gives_base_card_correct_default_max_height(): Unit =
        verifyHeight(
            expectedHeight = 100.dp +
                CardDefaults.ContentPadding.calculateBottomPadding() +
                CardDefaults.ContentPadding.calculateTopPadding(),
            imageModifier = Modifier.requiredHeight(100.dp)
        )

    private fun verifyHeight(expectedHeight: Dp, imageModifier: Modifier = Modifier) {
        rule.verifyHeight(expectedHeight) {
            Card(
                onClick = {},
            ) {
                TestIcon(modifier = imageModifier)
            }
        }
    }
}

public class CardColorTest {
    @get:Rule
    public val rule: ComposeContentTestRule = createComposeRule()

    @Test
    public fun gives_enabled_default_colors(): Unit =
        verifyColors(
            CardStatus.Enabled,
        ) { MaterialTheme.colors.onSurfaceVariant2 }

    @Test
    public fun gives_disabled_default_colors(): Unit =
        verifyColors(
            CardStatus.Disabled,
        ) { MaterialTheme.colors.onSurfaceVariant2 }

    @Test
    public fun app_card_gives_default_colors() {
        var expectedAppImageColor = Color.Transparent
        var expectedAppColor = Color.Transparent
        var expectedTimeColor = Color.Transparent
        var expectedTitleColor = Color.Transparent
        var expectedBodyColor = Color.Transparent
        var actualBodyColor = Color.Transparent
        var actualTitleColor = Color.Transparent
        var actualTimeColor = Color.Transparent
        var actualAppColor = Color.Transparent
        var actualAppImageColor = Color.Transparent
        val testBackground = Color.White

        rule.setContentWithTheme {
            expectedAppImageColor = MaterialTheme.colors.primary
            expectedAppColor = MaterialTheme.colors.primary
            expectedTimeColor = MaterialTheme.colors.onSurfaceVariant
            expectedTitleColor = MaterialTheme.colors.onSurface
            expectedBodyColor = MaterialTheme.colors.onSurfaceVariant2
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(testBackground)
            ) {
                AppCard(
                    onClick = {},
                    appName = { actualAppColor = LocalContentColor.current },
                    appImage = { actualAppImageColor = LocalContentColor.current },
                    time = { actualTimeColor = LocalContentColor.current },
                    body = { actualBodyColor = LocalContentColor.current },
                    title = { actualTitleColor = LocalContentColor.current },
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        assertEquals(expectedAppImageColor, actualAppImageColor)
        assertEquals(expectedAppColor, actualAppColor)
        assertEquals(expectedTimeColor, actualTimeColor)
        assertEquals(expectedTitleColor, actualTitleColor)
        assertEquals(expectedBodyColor, actualBodyColor)
    }

    @Test
    public fun title_card_gives_default_colors() {
        var expectedTimeColor = Color.Transparent
        var expectedTitleColor = Color.Transparent
        var expectedBodyColor = Color.Transparent
        var actualBodyColor = Color.Transparent
        var actualTitleColor = Color.Transparent
        var actualTimeColor = Color.Transparent
        val testBackground = Color.White

        rule.setContentWithTheme {
            expectedTimeColor = MaterialTheme.colors.onSurfaceVariant
            expectedTitleColor = MaterialTheme.colors.onSurface
            expectedBodyColor = MaterialTheme.colors.onSurfaceVariant2
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(testBackground)
            ) {
                TitleCard(
                    onClick = {},
                    time = { actualTimeColor = LocalContentColor.current },
                    body = { actualBodyColor = LocalContentColor.current },
                    title = { actualTitleColor = LocalContentColor.current },
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        assertEquals(expectedTimeColor, actualTimeColor)
        assertEquals(expectedTitleColor, actualTitleColor)
        assertEquals(expectedBodyColor, actualBodyColor)
    }

    private fun verifyColors(
        status: CardStatus,
        contentColor: @Composable () -> Color
    ) {
        var expectedContent = Color.Transparent
        var actualContent = Color.Transparent
        val testBackground = Color.White

        rule.setContentWithTheme {
            expectedContent = contentColor()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(testBackground)
            ) {
                Card(
                    onClick = {},
                    content = { actualContent = LocalContentColor.current },
                    enabled = status.enabled(),
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        assertEquals(expectedContent, actualContent)
    }
}

public class CardFontTest {
    @get:Rule
    public val rule: ComposeContentTestRule = createComposeRule()

    @Test
    public fun gives_correct_text_style_base() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default
        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.button
            Card(
                onClick = {},
                content = {
                    actualTextStyle = LocalTextStyle.current
                },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    public fun app_card_gives_correct_text_style_base() {
        var actualAppTextStyle = TextStyle.Default
        var actualTimeTextStyle = TextStyle.Default
        var actualTitleTextStyle = TextStyle.Default
        var actualBodyTextStyle = TextStyle.Default
        var expectedAppTextStyle = TextStyle.Default
        var expectedTimeTextStyle = TextStyle.Default
        var expectedTitleTextStyle = TextStyle.Default
        var expectedBodyTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedAppTextStyle = MaterialTheme.typography.caption1
            expectedTimeTextStyle = MaterialTheme.typography.caption1
            expectedTitleTextStyle = MaterialTheme.typography.title3
            expectedBodyTextStyle = MaterialTheme.typography.body1

            AppCard(
                onClick = {},
                appName = {
                    actualAppTextStyle = LocalTextStyle.current
                },
                time = {
                    actualTimeTextStyle = LocalTextStyle.current
                },
                title = {
                    actualTitleTextStyle = LocalTextStyle.current
                },
                body = {
                    actualBodyTextStyle = LocalTextStyle.current
                },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        assertEquals(expectedAppTextStyle, actualAppTextStyle)
        assertEquals(expectedTimeTextStyle, actualTimeTextStyle)
        assertEquals(expectedTitleTextStyle, actualTitleTextStyle)
        assertEquals(expectedBodyTextStyle, actualBodyTextStyle)
    }

    @Test
    public fun title_card_gives_correct_text_style_base() {
        var actualTimeTextStyle = TextStyle.Default
        var actualTitleTextStyle = TextStyle.Default
        var actualBodyTextStyle = TextStyle.Default
        var expectedTimeTextStyle = TextStyle.Default
        var expectedTitleTextStyle = TextStyle.Default
        var expectedBodyTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTimeTextStyle = MaterialTheme.typography.caption1
            expectedTitleTextStyle = MaterialTheme.typography.button
            expectedBodyTextStyle = MaterialTheme.typography.body1

            TitleCard(
                onClick = {},
                time = {
                    actualTimeTextStyle = LocalTextStyle.current
                },
                title = {
                    actualTitleTextStyle = LocalTextStyle.current
                },
                body = {
                    actualBodyTextStyle = LocalTextStyle.current
                },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        assertEquals(expectedTimeTextStyle, actualTimeTextStyle)
        assertEquals(expectedTitleTextStyle, actualTitleTextStyle)
        assertEquals(expectedBodyTextStyle, actualBodyTextStyle)
    }
}

private fun ComposeContentTestRule.verifyHeight(expected: Dp, content: @Composable () -> Unit) {
    setContentWithThemeForSizeAssertions {
        content()
    }.assertHeightIsEqualTo(expected, Dp(1.0f))
}

private enum class CardStatus {
    Enabled,
    Disabled;

    fun enabled() = this == Enabled
}
