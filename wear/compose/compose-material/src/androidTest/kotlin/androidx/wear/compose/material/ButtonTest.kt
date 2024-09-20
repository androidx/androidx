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

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

public class ButtonBehaviourTest {
    @get:Rule public val rule = createComposeRule()

    @Test
    public fun supports_testtag_on_button_for_image() {
        rule.setContentWithTheme {
            Button(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) { TestImage() }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun supports_testtag_on_button_for_text() {
        rule.setContentWithTheme {
            Button(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) { Text("Test") }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun supports_testtag_on_compactbutton_for_image() {
        rule.setContentWithTheme {
            CompactButton(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) { TestImage() }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun supports_testtag_on_compactbutton_for_text() {
        rule.setContentWithTheme {
            CompactButton(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) { Text("Test") }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    public fun has_clickaction_when_enabled_for_image() {
        rule.setContentWithTheme {
            Button(onClick = {}, enabled = true, modifier = Modifier.testTag(TEST_TAG)) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    public fun has_clickaction_when_enabled_for_text() {
        rule.setContentWithTheme {
            Button(onClick = {}, enabled = true, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    public fun has_clickaction_when_disabled_for_image() {
        rule.setContentWithTheme {
            Button(onClick = {}, enabled = false, modifier = Modifier.testTag(TEST_TAG)) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    public fun has_clickaction_when_disabled_for_text() {
        rule.setContentWithTheme {
            Button(onClick = {}, enabled = false, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    public fun is_correctly_enabled() {
        rule.setContentWithTheme {
            Button(onClick = {}, enabled = true, modifier = Modifier.testTag(TEST_TAG)) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    public fun is_correctly_disabled() {
        rule.setContentWithTheme {
            Button(onClick = {}, enabled = false, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    public fun compact_button_responds_to_click_when_enabled() {
        var clicked = false

        rule.setContentWithTheme {
            CompactButton(
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
    public fun button_responds_to_click_when_enabled() {
        var clicked = false

        rule.setContentWithTheme {
            Button(
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
    public fun compact_button_does_not_respond_to_click_when_disabled() {
        var clicked = false

        rule.setContentWithTheme {
            CompactButton(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle { assertEquals(false, clicked) }
    }

    @Test
    public fun button_does_not_respond_to_click_when_disabled() {
        var clicked = false

        rule.setContentWithTheme {
            Button(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle { assertEquals(false, clicked) }
    }

    @Test
    public fun has_role_button_for_compact_image() {
        rule.setContentWithTheme {
            CompactButton(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) { TestImage() }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    public fun has_role_button_for_text() {
        rule.setContentWithTheme {
            Button(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) { Text("Test") }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    public fun contains_text_for_button() {
        val text = "Test"
        rule.setContentWithTheme {
            Button(
                onClick = {},
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithText(text).assertExists()
    }

    @Test
    public fun contains_text_for_compact_button() {
        val text = "Test"
        rule.setContentWithTheme {
            CompactButton(
                onClick = {},
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithText(text).assertExists()
    }

    @Test
    public fun matches_has_text_for_button() {
        val text = "Test"
        rule.setContentWithTheme {
            Button(
                onClick = {},
            ) {
                Text("Test")
            }
        }

        rule.onNode(hasText(text)).assertExists()
    }

    @Test
    public fun matches_has_text_for_compactbutton() {
        val text = "Test"
        rule.setContentWithTheme {
            CompactButton(
                onClick = {},
            ) {
                Text("Test")
            }
        }

        rule.onNode(hasText(text)).assertExists()
    }

    @Test
    public fun is_circular_under_ltr_for_button() =
        rule.isCircular(LayoutDirection.Ltr) {
            Button(
                modifier = Modifier.testTag(TEST_TAG),
                onClick = {},
            ) {
                // omit content to allow us to validate the shape by pixel checking.
            }
        }

    @Test
    public fun is_circular_under_rtl_for_button() =
        rule.isCircular(LayoutDirection.Rtl) {
            Button(
                modifier = Modifier.testTag(TEST_TAG),
                onClick = {},
            ) {
                // omit content to allow us to validate the shape by pixel checking.
            }
        }
}

public class ButtonSizeTest {
    @get:Rule public val rule = createComposeRule()

    @Test
    public fun gives_compactbutton_correct_tapsize() {
        rule.verifyTapSize(TapSize.Small) {
            CompactButton(
                onClick = {},
            ) {
                Text("xs")
            }
        }
    }

    @Test
    public fun gives_button_correct_tapsize() {
        rule.verifyTapSize(TapSize.Default) {
            Button(
                onClick = {},
            ) {
                Text("abc")
            }
        }
    }

    @Test
    public fun gives_small_button_correct_tapsize() {
        rule.verifyTapSize(TapSize.Small) {
            Button(onClick = {}, modifier = Modifier.size(ButtonDefaults.SmallButtonSize)) {
                TestImage()
            }
        }
    }

    @Test
    public fun gives_large_button_correct_tapsize() {
        rule.verifyTapSize(TapSize.Large) {
            Button(onClick = {}, modifier = Modifier.size(ButtonDefaults.LargeButtonSize)) {
                TestImage()
            }
        }
    }
}

public class ButtonShapeTest {
    @get:Rule public val rule = createComposeRule()

    @Test
    public fun default_button_shape_is_circle() {
        rule.isShape(CircleShape) { modifier ->
            Button(
                onClick = {},
                enabled = true,
                colors = ButtonDefaults.primaryButtonColors(),
                modifier = modifier
            ) {}
        }
    }

    @Test
    public fun allows_custom_button_shape_override() {
        val shape = CutCornerShape(4.dp)

        rule.isShape(shape) { modifier ->
            Button(
                onClick = {},
                enabled = true,
                colors = ButtonDefaults.primaryButtonColors(),
                modifier = modifier,
                shape = shape
            ) {}
        }
    }

    @Test
    public fun default_compact_button_shape_is_circle() {
        rule.isShape(CircleShape) { modifier ->
            CompactButton(
                onClick = {},
                enabled = true,
                colors = ButtonDefaults.primaryButtonColors(),
                backgroundPadding = 0.dp,
                modifier = modifier
            ) {}
        }
    }

    @Test
    public fun allows_custom_compact_button_shape_override() {
        val shape = CutCornerShape(4.dp)

        rule.isShape(shape) { modifier ->
            CompactButton(
                onClick = {},
                enabled = true,
                colors = ButtonDefaults.primaryButtonColors(),
                backgroundPadding = 0.dp,
                modifier = modifier,
                shape = shape
            ) {}
        }
    }
}

public class ButtonColorTest {
    @get:Rule public val rule = createComposeRule()

    @Test
    public fun gives_enabled_button_primary_colors() =
        verifyButtonColors(
            Status.Enabled,
            { ButtonDefaults.primaryButtonColors() },
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.onPrimary },
        )

    @Test
    public fun gives_enabled_compact_button_primary_colors() =
        verifyCompactButtonColors(
            Status.Enabled,
            { ButtonDefaults.primaryButtonColors() },
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.onPrimary },
        )

    @Test
    public fun gives_disabled_primary_button_contrasting_content_color() =
        verifyButtonColors(
            Status.Disabled,
            { ButtonDefaults.primaryButtonColors() },
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.background },
            applyAlphaForDisabledContent = false
        )

    @Test
    public fun gives_disabled_compact_button_contrasting_content_color() =
        verifyCompactButtonColors(
            Status.Disabled,
            { ButtonDefaults.primaryButtonColors() },
            { MaterialTheme.colors.primary },
            { MaterialTheme.colors.background },
            applyAlphaForDisabledContent = false,
        )

    @Test
    public fun gives_enabled_button_secondary_colors() =
        verifyButtonColors(
            Status.Enabled,
            { ButtonDefaults.secondaryButtonColors() },
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    public fun gives_enabled_compact_button_secondary_colors() =
        verifyCompactButtonColors(
            Status.Enabled,
            { ButtonDefaults.secondaryButtonColors() },
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    public fun gives_disabled_button_secondary_colors() =
        verifyButtonColors(
            Status.Disabled,
            { ButtonDefaults.secondaryButtonColors() },
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    public fun gives_disabled_compact_button_secondary_colors() =
        verifyCompactButtonColors(
            Status.Disabled,
            { ButtonDefaults.secondaryButtonColors() },
            { MaterialTheme.colors.surface },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    public fun gives_enabled_button_icon_colors() =
        verifyButtonColors(
            Status.Enabled,
            { ButtonDefaults.iconButtonColors() },
            { Color.Transparent },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    public fun gives_enabled_button_outlined_colors() =
        verifyOutlinedButtonColors(
            Status.Enabled,
            { ButtonDefaults.outlinedButtonColors() },
            { Color.Transparent },
            { MaterialTheme.colors.primary },
        )

    @Test
    public fun gives_enabled_compact_button_icon_colors() =
        verifyCompactButtonColors(
            Status.Enabled,
            { ButtonDefaults.iconButtonColors() },
            { Color.Transparent },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    public fun gives_disabled_button_icon_colors() =
        verifyButtonColors(
            Status.Disabled,
            { ButtonDefaults.iconButtonColors() },
            { Color.Transparent },
            { MaterialTheme.colors.onSurface },
        )

    @Test
    public fun gives_disabled_button_outlined_colors() =
        verifyOutlinedButtonColors(
            Status.Disabled,
            { ButtonDefaults.outlinedButtonColors() },
            { Color.Transparent },
            { MaterialTheme.colors.primary },
        )

    @Test
    public fun gives_disabled_compact_button_icon_colors() =
        verifyCompactButtonColors(
            Status.Disabled,
            { ButtonDefaults.iconButtonColors() },
            { Color.Transparent },
            { MaterialTheme.colors.onSurface },
        )

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public fun allows_button_custom_enabled_background_color_override() {
        val overrideColor = Color.Yellow
        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(backgroundColor = overrideColor),
                    enabled = true,
                    modifier = Modifier.testTag(TEST_TAG)
                ) {}
            }
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(overrideColor, 50.0f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public fun allows_compactbutton_custom_enabled_background_color_override() {
        val overrideColor = Color.Yellow
        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                CompactButton(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(backgroundColor = overrideColor),
                    enabled = true,
                    modifier = Modifier.testTag(TEST_TAG)
                ) {}
            }
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(overrideColor, 25.0f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public fun allows_button_custom_disabled_background_color_override() {
        val overrideColor = Color.Yellow
        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(disabledBackgroundColor = overrideColor),
                    enabled = false,
                    modifier = Modifier.testTag(TEST_TAG)
                ) {}
            }
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(overrideColor, 50.0f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    public fun allows_compactbutton_custom_disabled_background_color_override() {
        val overrideColor = Color.Yellow
        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                CompactButton(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(disabledBackgroundColor = overrideColor),
                    enabled = false,
                    modifier = Modifier.testTag(TEST_TAG)
                ) {}
            }
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(overrideColor, 25.0f)
    }

    @Test
    public fun allows_button_custom_enabled_content_color_override() {
        val overrideColor = Color.Red
        var actualContentColor = Color.Transparent
        rule.setContentWithTheme {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(contentColor = overrideColor),
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                actualContentColor = LocalContentColor.current
            }
        }

        assertEquals(overrideColor, actualContentColor)
    }

    @Test
    public fun allows_compactbutton_custom_enabled_content_color_override() {
        val overrideColor = Color.Red
        var actualContentColor = Color.Transparent
        rule.setContentWithTheme {
            CompactButton(
                onClick = {},
                colors = ButtonDefaults.buttonColors(contentColor = overrideColor),
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                actualContentColor = LocalContentColor.current
            }
        }

        assertEquals(overrideColor, actualContentColor)
    }

    @Test
    public fun allows_button_custom_disabled_content_color_override() {
        val overrideColor = Color.Yellow
        var actualContentColor = Color.Transparent
        rule.setContentWithTheme {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(disabledContentColor = overrideColor),
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                actualContentColor = LocalContentColor.current
            }
        }

        assertEquals(overrideColor, actualContentColor)
    }

    @Test
    public fun allows_compactbutton_custom_disabled_content_color_override() {
        val overrideColor = Color.Yellow
        var actualContentColor = Color.Transparent
        rule.setContentWithTheme {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(disabledContentColor = overrideColor),
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                actualContentColor = LocalContentColor.current
            }
        }

        assertEquals(overrideColor, actualContentColor)
    }

    public fun verifyButtonColors(
        status: Status,
        buttonColors: @Composable () -> ButtonColors,
        backgroundColor: @Composable () -> Color,
        contentColor: @Composable () -> Color,
        applyAlphaForDisabledContent: Boolean = true,
    ) {
        verifyColors(
            status,
            backgroundColor,
            contentColor,
            applyAlphaForDisabledContent,
        ) {
            var actualColor = Color.Transparent
            Button(
                onClick = {},
                colors = buttonColors(),
                enabled = status.enabled(),
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                actualColor = LocalContentColor.current
            }
            return@verifyColors actualColor
        }
    }

    public fun verifyOutlinedButtonColors(
        status: Status,
        buttonColors: @Composable () -> ButtonColors,
        backgroundColor: @Composable () -> Color,
        contentColor: @Composable () -> Color,
    ) {
        verifyColors(
            status,
            backgroundColor,
            contentColor,
        ) {
            var actualColor = Color.Transparent
            OutlinedButton(
                onClick = {},
                colors = buttonColors(),
                enabled = status.enabled(),
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                actualColor = LocalContentColor.current
            }
            return@verifyColors actualColor
        }
    }

    public fun verifyCompactButtonColors(
        status: Status,
        buttonColors: @Composable () -> ButtonColors,
        backgroundColor: @Composable () -> Color,
        contentColor: @Composable () -> Color,
        applyAlphaForDisabledContent: Boolean = true,
    ) {
        verifyColors(
            status,
            backgroundColor,
            contentColor,
            applyAlphaForDisabledContent,
        ) {
            var actualColor = Color.Transparent
            CompactButton(
                onClick = {},
                backgroundPadding = 0.dp,
                colors = buttonColors(),
                enabled = status.enabled(),
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                actualColor = LocalContentColor.current
            }
            return@verifyColors actualColor
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    private fun verifyColors(
        status: Status,
        backgroundColor: @Composable () -> Color,
        contentColor: @Composable () -> Color,
        applyAlphaForDisabledContent: Boolean = true,
        threshold: Float = 50.0f,
        content: @Composable () -> Color,
    ) {
        val testBackground = Color.White
        var expectedBackground = Color.Transparent
        var expectedContent = Color.Transparent
        var actualContent = Color.Transparent

        rule.setContentWithTheme {
            if (status.enabled()) {
                expectedBackground = backgroundColor()
                expectedContent = contentColor()
            } else {
                expectedBackground =
                    backgroundColor()
                        .copy(alpha = ContentAlpha.disabled)
                        .compositeOver(testBackground)
                expectedContent =
                    if (applyAlphaForDisabledContent)
                        contentColor().copy(alpha = ContentAlpha.disabled)
                    else contentColor()
            }
            Box(modifier = Modifier.fillMaxSize().background(testBackground)) {
                actualContent = content()
            }
        }

        assertEquals(expectedContent, actualContent)

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(
                if (expectedBackground != Color.Transparent) expectedBackground else testBackground,
                threshold
            )
    }
}

public class ButtonTextStyleTest {
    @get:Rule public val rule = createComposeRule()

    @Test
    public fun gives_button_correct_font() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.button
            Button(
                onClick = {},
            ) {
                actualTextStyle = LocalTextStyle.current
            }
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    public fun gives_compactbutton_correct_font() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.button
            CompactButton(
                onClick = {},
            ) {
                actualTextStyle = LocalTextStyle.current
            }
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }
}

private fun ComposeContentTestRule.verifyTapSize(
    expected: TapSize,
    content: @Composable () -> Unit
) {
    setContentWithThemeForSizeAssertions { content() }
        .assertHeightIsEqualTo(expected.size)
        .assertWidthIsEqualTo(expected.size)
}

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
private fun ComposeContentTestRule.isCircular(
    layoutDirection: LayoutDirection,
    padding: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    var background = Color.Transparent
    var surface = Color.Transparent
    setContentWithTheme {
        background = MaterialTheme.colors.primary
        surface = MaterialTheme.colors.surface
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Box(Modifier.padding(padding).background(surface)) { content() }
        }
    }

    onNodeWithTag(TEST_TAG)
        .captureToImage()
        .assertShape(
            density = density,
            shape = CircleShape,
            horizontalPadding = padding,
            verticalPadding = padding,
            backgroundColor = surface,
            shapeColor = background
        )
}

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
private fun ComposeContentTestRule.isShape(
    expectedShape: Shape,
    content: @Composable (Modifier) -> Unit
) {
    var background = Color.Transparent
    var buttonColor = Color.Transparent
    val padding = 0.dp

    setContentWithTheme {
        background = MaterialTheme.colors.surface
        Box(Modifier.background(background)) {
            buttonColor = MaterialTheme.colors.primary
            content(Modifier.testTag(TEST_TAG).padding(padding))
        }
    }

    onNodeWithTag(TEST_TAG)
        .captureToImage()
        .assertShape(
            density = density,
            horizontalPadding = 0.dp,
            verticalPadding = 0.dp,
            shapeColor = buttonColor,
            backgroundColor = background,
            antiAliasingGap = 2.0f,
            shape = expectedShape,
        )
}

internal enum class TapSize(val size: Dp) {
    Small(48.dp),
    Default(52.dp),
    Large(60.dp)
}

public enum class Status {
    Enabled,
    Disabled;

    public fun enabled() = this == Enabled
}
