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

package androidx.wear.compose.material3

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CardTest {
    @get:Rule val rule: ComposeContentTestRule = createComposeRule()

    @Test
    fun supports_test_tag() {
        rule.setContentWithTheme {
            Card(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) { TestImage() }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun has_clickaction_when_enabled() {
        rule.setContentWithTheme {
            Card(onClick = {}, enabled = true, modifier = Modifier.testTag(TEST_TAG)) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun has_clickaction_when_disabled() {
        rule.setContentWithTheme {
            Card(onClick = {}, enabled = false, modifier = Modifier.testTag(TEST_TAG)) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled_when_enabled_equals_true() {
        rule.setContentWithTheme {
            Card(onClick = {}, enabled = true, modifier = Modifier.testTag(TEST_TAG)) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled_when_enabled_equals_false() {
        rule.setContentWithTheme {
            Card(onClick = {}, enabled = false, modifier = Modifier.testTag(TEST_TAG)) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun responds_to_click_when_enabled() {
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

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle { assertEquals(true, clicked) }
    }

    @Test
    fun does_not_respond_to_click_when_disabled() {
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

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle { assertEquals(false, clicked) }
    }

    @Test
    fun card_responds_to_long_click_when_enabled() {
        var longClicked = false

        rule.setContentWithTheme {
            Card(
                onClick = { /* Do nothing */ },
                onLongClick = { longClicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { longClick() }

        rule.runOnIdle { assertEquals(true, longClicked) }
    }

    @Test
    fun card_does_not_respond_to_long_click_when_disabled() {
        var longClicked = false

        rule.setContentWithTheme {
            Card(
                onClick = { /* Do nothing */ },
                onLongClick = { longClicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { longClick() }

        rule.runOnIdle { assertEquals(false, longClicked) }
    }

    @Test
    fun appCard_responds_to_long_click_when_enabled() {
        var longClicked = false

        rule.setContentWithTheme {
            AppCard(
                onClick = { /* Do nothing */ },
                onLongClick = { longClicked = true },
                appName = {},
                title = {},
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { longClick() }

        rule.runOnIdle { assertEquals(true, longClicked) }
    }

    @Test
    fun appCard_does_not_respond_to_long_click_when_disabled() {
        var longClicked = false

        rule.setContentWithTheme {
            AppCard(
                onClick = { /* Do nothing */ },
                onLongClick = { longClicked = true },
                appName = {},
                title = {},
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { longClick() }

        rule.runOnIdle { assertEquals(false, longClicked) }
    }

    @Test
    fun titleCard_responds_to_long_click_when_enabled() {
        var longClicked = false

        rule.setContentWithTheme {
            TitleCard(
                onClick = { /* Do nothing */ },
                onLongClick = { longClicked = true },
                title = {},
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { longClick() }

        rule.runOnIdle { assertEquals(true, longClicked) }
    }

    @Test
    fun titleCard_does_not_respond_to_long_click_when_disabled() {
        var longClicked = false

        rule.setContentWithTheme {
            TitleCard(
                onClick = { /* Do nothing */ },
                onLongClick = { longClicked = true },
                title = {},
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { longClick() }

        rule.runOnIdle { assertEquals(false, longClicked) }
    }

    @Test
    fun outlinedCard_responds_to_long_click_when_enabled() {
        var longClicked = false

        rule.setContentWithTheme {
            OutlinedCard(
                onClick = { /* Do nothing */ },
                onLongClick = { longClicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { longClick() }

        rule.runOnIdle { assertEquals(true, longClicked) }
    }

    @Test
    fun outlinedCard_does_not_respond_to_long_click_when_disabled() {
        var longClicked = false

        rule.setContentWithTheme {
            OutlinedCard(
                onClick = { /* Do nothing */ },
                onLongClick = { longClicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { longClick() }

        rule.runOnIdle { assertEquals(false, longClicked) }
    }

    @Test
    fun has_role_button_if_explicitly_set() {
        rule.setContentWithTheme {
            Card(
                onClick = {},
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG).semantics { role = Role.Button },
            ) {
                TestImage()
            }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun gives_base_card_with_text_minimum_height(): Unit =
        rule.verifyHeight(64.dp) {
            Card(
                onClick = {},
            ) {
                Text("Card")
            }
        }

    @Test
    fun gives_base_card_correct_default_max_height(): Unit =
        verifyHeight(
            expectedHeight =
                100.dp +
                    CardDefaults.ContentPadding.calculateBottomPadding() +
                    CardDefaults.ContentPadding.calculateTopPadding(),
            imageModifier = Modifier.requiredHeight(100.dp)
        )

    @Test
    fun gives_enabled_default_colors(): Unit =
        verifyColors(
            CardStatus.Enabled,
        ) {
            MaterialTheme.colorScheme.onSurface
        }

    @Test
    fun gives_disabled_default_colors(): Unit =
        verifyColors(
            CardStatus.Disabled,
        ) {
            MaterialTheme.colorScheme.onSurface
        }

    @Test
    fun app_card_gives_default_colors() {
        var expectedAppColor = Color.Transparent
        var expectedTimeColor = Color.Transparent
        var expectedTitleColor = Color.Transparent
        var expectedContentColor = Color.Transparent
        var actualContentColor = Color.Transparent
        var actualTitleColor = Color.Transparent
        var actualTimeColor = Color.Transparent
        var actualAppColor = Color.Transparent
        val testBackground = Color.White

        rule.setContentWithTheme {
            expectedAppColor = MaterialTheme.colorScheme.onSurface
            expectedTimeColor = MaterialTheme.colorScheme.onSurfaceVariant
            expectedTitleColor = MaterialTheme.colorScheme.onSurface
            expectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            Box(modifier = Modifier.fillMaxSize().background(testBackground)) {
                AppCard(
                    onClick = {},
                    appName = { actualAppColor = LocalContentColor.current },
                    time = { actualTimeColor = LocalContentColor.current },
                    title = { actualTitleColor = LocalContentColor.current },
                    modifier = Modifier.testTag(TEST_TAG)
                ) {
                    actualContentColor = LocalContentColor.current
                }
            }
        }

        assertEquals(expectedAppColor, actualAppColor)
        assertEquals(expectedTimeColor, actualTimeColor)
        assertEquals(expectedTitleColor, actualTitleColor)
        assertEquals(expectedContentColor, actualContentColor)
    }

    @Test
    fun title_card_gives_default_colors() {
        var expectedTimeColor = Color.Transparent
        var expectedTitleColor = Color.Transparent
        var expectedContentColor = Color.Transparent
        var actualContentColor = Color.Transparent
        var actualTitleColor = Color.Transparent
        var actualTimeColor = Color.Transparent
        val testBackground = Color.White

        rule.setContentWithTheme {
            expectedTimeColor = MaterialTheme.colorScheme.onSurfaceVariant
            expectedTitleColor = MaterialTheme.colorScheme.onSurface
            expectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            Box(modifier = Modifier.fillMaxSize().background(testBackground)) {
                TitleCard(
                    onClick = {},
                    time = { actualTimeColor = LocalContentColor.current },
                    title = { actualTitleColor = LocalContentColor.current },
                    modifier = Modifier.testTag(TEST_TAG)
                ) {
                    actualContentColor = LocalContentColor.current
                }
            }
        }

        assertEquals(expectedTimeColor, actualTimeColor)
        assertEquals(expectedTitleColor, actualTitleColor)
        assertEquals(expectedContentColor, actualContentColor)
    }

    @Test
    public fun title_card_with_time_and_subtitle_gives_default_colors() {
        var expectedTimeColor = Color.Transparent
        var expectedSubtitleColor = Color.Transparent
        var expectedTitleColor = Color.Transparent
        var actualTimeColor = Color.Transparent
        var actualSubtitleColor = Color.Transparent
        var actualTitleColor = Color.Transparent
        val testBackground = Color.White

        rule.setContentWithTheme {
            expectedTimeColor = MaterialTheme.colorScheme.onSurfaceVariant
            expectedSubtitleColor = MaterialTheme.colorScheme.tertiary
            expectedTitleColor = MaterialTheme.colorScheme.onSurface
            Box(modifier = Modifier.fillMaxSize().background(testBackground)) {
                TitleCard(
                    onClick = {},
                    time = { actualTimeColor = LocalContentColor.current },
                    subtitle = { actualSubtitleColor = LocalContentColor.current },
                    title = { actualTitleColor = LocalContentColor.current },
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        assertEquals(expectedTimeColor, actualTimeColor)
        assertEquals(expectedSubtitleColor, actualSubtitleColor)
        assertEquals(expectedTitleColor, actualTitleColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun outlined_card_has_outlined_border_and_transparent() {
        val outlineColor = Color.Red
        val testBackground = Color.Green

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize().background(testBackground)) {
                OutlinedCard(
                    onClick = {},
                    border = CardDefaults.outlinedCardBorder(outlineColor),
                    modifier = Modifier.testTag(TEST_TAG).size(100.dp).align(Alignment.Center)
                ) {}
            }
        }
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(outlineColor)
        // As the color of the OutlinedCard is transparent, we expect to see a
        // testBackground color covering everything except border.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(testBackground, 93f..97f)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun outlined_titlecard_has_outlined_border_and_transparent() {
        val outlineColor = Color.Red
        val testBackground = Color.Green

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize().background(testBackground)) {
                TitleCard(
                    onClick = {},
                    title = {},
                    border = CardDefaults.outlinedCardBorder(outlineColor),
                    colors = CardDefaults.outlinedCardColors(),
                    modifier = Modifier.testTag(TEST_TAG).size(100.dp).align(Alignment.Center)
                ) {}
            }
        }
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(outlineColor)
        // As the color of the OutlinedCard is transparent, we expect to see a
        // testBackground color covering everything except border.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(testBackground, 93f..97f)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun outlined_appcard_has_outlined_border_and_transparent() {
        val outlineColor = Color.Red
        val testBackground = Color.Green

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize().background(testBackground)) {
                AppCard(
                    onClick = {},
                    appName = {},
                    title = {},
                    border = CardDefaults.outlinedCardBorder(outlineColor),
                    colors = CardDefaults.outlinedCardColors(),
                    modifier = Modifier.testTag(TEST_TAG).size(100.dp).align(Alignment.Center)
                ) {}
            }
        }
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(outlineColor)
        // As the color of the OutlinedCard is transparent, we expect to see a
        // testBackground color covering everything except border.
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertColorInPercentageRange(testBackground, 93f..97f)
    }

    @Test
    fun gives_correct_text_style_base() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default
        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.titleMedium
            Card(
                onClick = {},
                content = { actualTextStyle = LocalTextStyle.current },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun app_card_gives_correct_text_style_base() {
        var actualAppTextStyle = TextStyle.Default
        var actualTimeTextStyle = TextStyle.Default
        var actualTitleTextStyle = TextStyle.Default
        var actuaContentTextStyle = TextStyle.Default
        var expectedAppTextStyle = TextStyle.Default
        var expectedTimeTextStyle = TextStyle.Default
        var expectedTitleTextStyle = TextStyle.Default
        var expectedContentTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedAppTextStyle = MaterialTheme.typography.titleSmall
            expectedTimeTextStyle = MaterialTheme.typography.bodyMedium
            expectedTitleTextStyle = MaterialTheme.typography.titleMedium
            expectedContentTextStyle = MaterialTheme.typography.bodyLarge

            AppCard(
                onClick = {},
                appName = { actualAppTextStyle = LocalTextStyle.current },
                time = { actualTimeTextStyle = LocalTextStyle.current },
                title = { actualTitleTextStyle = LocalTextStyle.current },
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                actuaContentTextStyle = LocalTextStyle.current
            }
        }
        assertEquals(expectedAppTextStyle, actualAppTextStyle)
        assertEquals(expectedTimeTextStyle, actualTimeTextStyle)
        assertEquals(expectedTitleTextStyle, actualTitleTextStyle)
        assertEquals(expectedContentTextStyle, actuaContentTextStyle)
    }

    @Test
    fun title_card_gives_correct_text_style_base() {
        var actualTimeTextStyle = TextStyle.Default
        var actualTitleTextStyle = TextStyle.Default
        var actuaContentTextStyle = TextStyle.Default
        var expectedTimeTextStyle = TextStyle.Default
        var expectedTitleTextStyle = TextStyle.Default
        var expectedContentTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTimeTextStyle = MaterialTheme.typography.bodyMedium
            expectedTitleTextStyle = MaterialTheme.typography.titleMedium
            expectedContentTextStyle = MaterialTheme.typography.bodyLarge

            TitleCard(
                onClick = {},
                time = { actualTimeTextStyle = LocalTextStyle.current },
                title = { actualTitleTextStyle = LocalTextStyle.current },
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                actuaContentTextStyle = LocalTextStyle.current
            }
        }
        assertEquals(expectedTimeTextStyle, actualTimeTextStyle)
        assertEquals(expectedTitleTextStyle, actualTitleTextStyle)
        assertEquals(expectedContentTextStyle, actuaContentTextStyle)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun outlined_app_card_gives_correct_text_style_base() {
        var actualAppTextStyle = TextStyle.Default
        var actualTimeTextStyle = TextStyle.Default
        var actualTitleTextStyle = TextStyle.Default
        var actuaContentTextStyle = TextStyle.Default
        var expectedAppTextStyle = TextStyle.Default
        var expectedTimeTextStyle = TextStyle.Default
        var expectedTitleTextStyle = TextStyle.Default
        var expectedContentTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedAppTextStyle = MaterialTheme.typography.titleSmall
            expectedTimeTextStyle = MaterialTheme.typography.bodyMedium
            expectedTitleTextStyle = MaterialTheme.typography.titleMedium
            expectedContentTextStyle = MaterialTheme.typography.bodyLarge

            AppCard(
                onClick = {},
                appName = { actualAppTextStyle = LocalTextStyle.current },
                time = { actualTimeTextStyle = LocalTextStyle.current },
                title = { actualTitleTextStyle = LocalTextStyle.current },
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                actuaContentTextStyle = LocalTextStyle.current
            }
        }
        rule.onNodeWithTag(TEST_TAG).captureToImage()
        assertEquals(expectedAppTextStyle, actualAppTextStyle)
        assertEquals(expectedTimeTextStyle, actualTimeTextStyle)
        assertEquals(expectedTitleTextStyle, actualTitleTextStyle)
        assertEquals(expectedContentTextStyle, actuaContentTextStyle)
    }

    private fun verifyHeight(expectedHeight: Dp, imageModifier: Modifier = Modifier) {
        rule.verifyHeight(expectedHeight) {
            Card(
                onClick = {},
            ) {
                TestIcon(modifier = imageModifier)
            }
        }
    }

    private fun verifyColors(status: CardStatus, contentColor: @Composable () -> Color) {
        var expectedContent = Color.Transparent
        var actualContent = Color.Transparent
        val testBackground = Color.White

        rule.setContentWithTheme {
            expectedContent = contentColor()
            Box(modifier = Modifier.fillMaxSize().background(testBackground)) {
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

private fun ComposeContentTestRule.verifyHeight(expected: Dp, content: @Composable () -> Unit) {
    setContentWithThemeForSizeAssertions { content() }.assertHeightIsEqualTo(expected, Dp(1.0f))
}

private enum class CardStatus {
    Enabled,
    Disabled;

    fun enabled() = this == Enabled
}
