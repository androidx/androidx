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

package androidx.wear.compose.materialcore

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag_on_button() {
        rule.setContent {
            ButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
            ) {
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun has_clickaction_when_enabled() {
        rule.setContent {
            ButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun has_clickaction_when_disabled() {
        rule.setContent {
            ButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun is_correctly_enabled() {
        rule.setContent {
            ButtonWithDefaults(
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled() {
        rule.setContent {
            ButtonWithDefaults(
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun button_responds_to_click_when_enabled() {
        var clicked = false

        rule.setContent {
            ButtonWithDefaults(
                onClick = { clicked = true },
                enabled = true,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
            }
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle {
            assertEquals(true, clicked)
        }
    }

    @Test
    fun button_does_not_respond_to_click_when_disabled() {
        var clicked = false

        rule.setContent {
            ButtonWithDefaults(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
            }
        }

        rule.onNodeWithTag(TEST_TAG).performClick()

        rule.runOnIdle {
            assertEquals(false, clicked)
        }
    }

    @Test
    fun has_role_button_for_button() {
        rule.setContent {
            ButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG)
            ) {
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button
                )
            )
    }

    @Test
    fun supports_circleshape_under_ltr_for_button() =
        rule.isShape(CircleShape, LayoutDirection.Ltr) {
            ButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
            ) {
            }
        }

    @Test
    fun supports_circleshape_under_rtl_for_button() =
        rule.isShape(CircleShape, LayoutDirection.Rtl) {
            ButtonWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
            ) {
            }
        }

    @Test
    fun extra_small_button_meets_accessibility_tapsize() {
        verifyTapSize(48.dp) {
            ButtonWithDefaults(
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .size(32.dp)
            ) {
            }
        }
    }

    @Test
    fun extra_small_button_has_correct_visible_size() {
        verifyVisibleSize(32.dp) {
            ButtonWithDefaults(
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .requiredSize(32.dp)
            ) {
            }
        }
    }

    @Test
    fun default_button_has_correct_tapsize() {
        // Tap size for Button should be the min button size.
        verifyTapSize(52.dp) {
            ButtonWithDefaults(
                modifier = Modifier
                    .testTag(TEST_TAG)
            ) {
            }
        }
    }

    @Test
    fun default_button_has_correct_visible_size() {
        // Tap size for Button should be the min button size.
        verifyVisibleSize(52.dp) {
            ButtonWithDefaults(
                modifier = Modifier
                    .testTag(TEST_TAG)
                    .size(52.dp)
            ) {
            }
        }
    }

    @Test
    fun allows_custom_button_shape_override() {
        val shape = CutCornerShape(4.dp)

        rule.isShape(shape, LayoutDirection.Ltr) {
            ButtonWithDefaults(
                shape = shape,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_enabled_button_correct_colors() =
        verifyButtonColors(
            status = Status.Enabled,
            enabledBackgroundColor = Color.Green,
            disabledBackgroundColor = Color.Red,
            enabledBorderColor = Color.Blue,
            disabledBorderColor = Color.Yellow,
            expectedBackgroundColor = Color.Green,
            expectedBorderColor = Color.Blue
        )

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_button_correct_colors() =
        verifyButtonColors(
            status = Status.Disabled,
            enabledBackgroundColor = Color.Green,
            disabledBackgroundColor = Color.Red,
            enabledBorderColor = Color.Blue,
            disabledBorderColor = Color.Yellow,
            expectedBackgroundColor = Color.Red,
            expectedBorderColor = Color.Yellow,
        )

    @Test
    fun button_obeys_content_provider_values() {
        var data = -1

        rule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                ButtonWithDefaults(
                    content = {
                        CompositionLocalProvider(
                            LocalContentTestData provides EXPECTED_LOCAL_TEST_DATA
                        ) {
                            data = LocalContentTestData.current
                        }
                    }
                )
            }
        }

        assertEquals(data, EXPECTED_LOCAL_TEST_DATA)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    internal fun verifyButtonColors(
        status: Status,
        enabledBackgroundColor: Color,
        disabledBackgroundColor: Color,
        enabledBorderColor: Color,
        disabledBorderColor: Color,
        expectedBackgroundColor: Color,
        expectedBorderColor: Color,
        backgroundThreshold: Float = 50.0f,
        borderThreshold: Float = 1.0f,
    ) {
        val testBackground = Color.White
        val expectedColor = { color: Color ->
            if (color != Color.Transparent)
                color.compositeOver(testBackground)
            else
                testBackground
        }

        rule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(testBackground)
            ) {
                ButtonWithDefaults(
                    backgroundColor = { enabled ->
                        rememberUpdatedState(
                            if (enabled) enabledBackgroundColor else disabledBackgroundColor
                        )
                    },
                    border = { enabled ->
                        return@ButtonWithDefaults rememberUpdatedState(
                            BorderStroke(
                                2.dp,
                                if (enabled) enabledBorderColor else disabledBorderColor
                            )
                        )
                    },
                    enabled = status.enabled(),
                    modifier = Modifier.testTag(TEST_TAG)
                ) {
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedColor(expectedBackgroundColor), backgroundThreshold)
        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedColor(expectedBorderColor), borderThreshold)
    }

    private fun verifyTapSize(
        expected: Dp,
        content: @Composable () -> Unit
    ) {
        rule.setContent {
            content()
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertTouchHeightIsEqualTo(expected)
            .assertTouchWidthIsEqualTo(expected)
    }

    private fun verifyVisibleSize(
        expected: Dp,
        content: @Composable () -> Unit
    ) {
        rule.setContent {
            content()
        }

        rule.onNodeWithTag(TEST_TAG)
            .assertHeightIsEqualTo(expected)
            .assertWidthIsEqualTo(expected)
    }

    @Composable
    internal fun ButtonWithDefaults(
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
        enabled: Boolean = true,
        backgroundColor: @Composable (enabled: Boolean) -> State<Color> = {
            rememberUpdatedState(DEFAULT_SHAPE_COLOR)
        },
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
        shape: Shape = CircleShape,
        border: @Composable (enabled: Boolean) -> State<BorderStroke?>? = { null },
        content: @Composable BoxScope.() -> Unit
    ) = Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = backgroundColor,
        interactionSource = interactionSource,
        shape = shape,
        border = border,
        buttonSize = 52.dp,
        content = content
    )
}
