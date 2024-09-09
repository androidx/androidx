/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.style.TextAlign
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test

class ConfirmationTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun confirmation_linearText_supports_testtag() {
        rule.setContentWithTheme {
            Confirmation(
                show = true,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
                text = {},
            ) {}
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun confirmation_curvedText_supports_testtag() {
        rule.setContentWithTheme {
            Confirmation(
                show = true,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
                curvedText = {}
            ) {}
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun successConfirmation_supports_testtag() {
        rule.setContentWithTheme {
            SuccessConfirmation(
                show = true,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun failureConfirmation_supports_testtag() {
        rule.setContentWithTheme {
            FailureConfirmation(
                show = true,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun confirmation_linearText_supports_swipeToDismiss() {
        rule.setContentWithTheme {
            var showDialog by remember { mutableStateOf(true) }
            Confirmation(
                modifier = Modifier.testTag(TEST_TAG),
                text = {},
                onDismissRequest = { showDialog = false },
                show = showDialog
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun confirmation_curvedText_supports_swipeToDismiss() {
        rule.setContentWithTheme {
            var showDialog by remember { mutableStateOf(true) }
            Confirmation(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = { showDialog = false },
                show = showDialog,
                curvedText = {}
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun successConfirmation_supports_swipeToDismiss() {
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            var showDialog by remember { mutableStateOf(true) }
            SuccessConfirmation(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = { showDialog = false },
                show = showDialog,
            )
        }
        // Advancing time so that animation will finish its motion.
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun failureConfirmation_supports_swipeToDismiss() {
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            var showDialog by remember { mutableStateOf(true) }
            FailureConfirmation(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = { showDialog = false },
                show = showDialog,
            )
        }
        // Advancing time so that animation will finish its motion.
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun hides_confirmation_linearText_when_show_false() {
        rule.setContentWithTheme {
            Confirmation(
                show = false,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
                text = {},
            ) {}
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun hides_confirmation_curvedText_when_show_false() {
        rule.setContentWithTheme {
            Confirmation(
                show = false,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
                curvedText = {}
            ) {}
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun hides_successConfirmation_when_show_false() {
        rule.setContentWithTheme {
            SuccessConfirmation(
                show = false,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun hides_failureConfirmation_when_show_false() {
        rule.setContentWithTheme {
            FailureConfirmation(
                show = false,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun confirmation_displays_icon_with_linearText() {
        rule.setContentWithTheme {
            Confirmation(
                text = { Text("Text", modifier = Modifier.testTag(TextTestTag)) },
                onDismissRequest = {},
                show = true
            ) {
                TestImage(IconTestTag)
            }
        }
        rule.onNodeWithTag(IconTestTag).assertExists()
        rule.onNodeWithTag(TextTestTag).assertExists()
    }

    @Test
    fun confirmation_displays_icon_with_curvedText() {
        rule.setContentWithTheme {
            Confirmation(
                onDismissRequest = {},
                show = true,
                curvedText = { curvedText(CurvedText) }
            ) {
                TestImage(IconTestTag)
            }
        }
        rule.onNodeWithTag(IconTestTag).assertExists()
        rule.onNodeWithContentDescription(CurvedText).assertExists()
    }

    @Test
    fun successConfirmation_displays_icon_with_text() {
        rule.setContentWithTheme {
            SuccessConfirmation(
                onDismissRequest = {},
                show = true,
                curvedText = ConfirmationDefaults.curvedText(CurvedText)
            ) {
                TestImage(IconTestTag)
            }
        }
        rule.onNodeWithTag(IconTestTag).assertExists()
        rule.onNodeWithContentDescription(CurvedText).assertExists()
    }

    @Test
    fun failureConfirmation_displays_icon_with_text() {
        rule.setContentWithTheme {
            FailureConfirmation(
                onDismissRequest = {},
                show = true,
                curvedText = ConfirmationDefaults.curvedText(CurvedText)
            ) {
                TestImage(IconTestTag)
            }
        }
        rule.onNodeWithTag(IconTestTag).assertExists()
        rule.onNodeWithContentDescription(CurvedText).assertExists()
    }

    @Test
    fun confirmation_linearText_dismissed_after_timeout() {
        var dismissed = false
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            Confirmation(text = {}, onDismissRequest = { dismissed = true }, show = true) {}
        }
        // Timeout longer than default confirmation duration
        rule.mainClock.advanceTimeBy(ConfirmationDefaults.ConfirmationDurationMillis + 1000)
        assert(dismissed)
    }

    @Test
    fun confirmation_curvedText_dismissed_after_timeout() {
        var dismissed = false
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            Confirmation(onDismissRequest = { dismissed = true }, show = true, curvedText = {}) {}
        }
        // Timeout longer than default confirmation duration
        rule.mainClock.advanceTimeBy(ConfirmationDefaults.ConfirmationDurationMillis + 1000)
        assert(dismissed)
    }

    @Test
    fun successConfirmation_dismissed_after_timeout() {
        var dismissed = false
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            SuccessConfirmation(
                onDismissRequest = { dismissed = true },
                show = true,
            )
        }
        // Timeout longer than default confirmation duration
        rule.mainClock.advanceTimeBy(ConfirmationDefaults.ConfirmationDurationMillis + 1000)
        assert(dismissed)
    }

    @Test
    fun failureConfirmation_dismissed_after_timeout() {
        var dismissed = false
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            FailureConfirmation(
                onDismissRequest = { dismissed = true },
                show = true,
            )
        }
        // Timeout longer than default confirmation duration
        rule.mainClock.advanceTimeBy(ConfirmationDefaults.ConfirmationDurationMillis + 1000)
        assert(dismissed)
    }

    @Test
    fun confirmation_linearText_positioning() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            Confirmation(
                show = true,
                text = {
                    Text(
                        "Title",
                        modifier = Modifier.testTag(TextTestTag),
                        textAlign = TextAlign.Center
                    )
                },
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                TestIcon(Modifier.testTag(IconTestTag))
            }
        }

        // Calculating the center of the icon
        val iconCenter =
            rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot().run { (top + bottom) / 2 }
        val textTop = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot().top

        // Stepping down half of the container height with vertical content padding
        textTop.assertIsEqualTo(
            iconCenter + ConfirmationLinearIconContainerSize / 2 + LinearContentSpacing
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun confirmation_linearText_correct_colors() {
        var expectedIconColor: Color = Color.Unspecified
        var expectedIconContainerColor: Color = Color.Unspecified
        var expectedTextColor: Color = Color.Unspecified

        rule.setContentWithTheme {
            Confirmation(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                show = true,
                text = { Text("Text") },
            ) {
                TestIcon(Modifier.testTag(IconTestTag))
            }
            expectedIconColor = MaterialTheme.colorScheme.primary
            expectedIconContainerColor = MaterialTheme.colorScheme.onPrimary
            expectedTextColor = MaterialTheme.colorScheme.onBackground
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedIconColor)

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedIconContainerColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedTextColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun confirmation_curvedText_correct_colors() {
        var expectedIconColor: Color = Color.Unspecified
        var expectedIconContainerColor: Color = Color.Unspecified
        var expectedTextColor: Color = Color.Unspecified
        rule.setContentWithTheme {
            Confirmation(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                show = true,
                curvedText = ConfirmationDefaults.curvedText(CurvedText)
            ) {
                TestIcon(Modifier.testTag(IconTestTag))
            }
            expectedIconColor = MaterialTheme.colorScheme.primary
            expectedIconContainerColor = MaterialTheme.colorScheme.onPrimary
            expectedTextColor = MaterialTheme.colorScheme.onBackground
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedIconColor)

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedIconContainerColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedTextColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun successConfirmation_correct_colors() {
        var expectedIconColor: Color = Color.Unspecified
        var expectedIconContainerColor: Color = Color.Unspecified
        var expectedTextColor: Color = Color.Unspecified

        rule.setContentWithTheme {
            SuccessConfirmation(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                show = true,
            )
            expectedIconColor = MaterialTheme.colorScheme.primary
            expectedIconContainerColor = MaterialTheme.colorScheme.onPrimary
            expectedTextColor = MaterialTheme.colorScheme.onBackground
        }
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedIconColor)

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedIconContainerColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedTextColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun failureConfirmation_correct_colors() {
        var expectedIconColor: Color = Color.Unspecified
        var expectedIconContainerColor: Color = Color.Unspecified
        var expectedTextColor: Color = Color.Unspecified
        val backgroundColor = Color.Black
        rule.setContentWithTheme {
            FailureConfirmation(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG).background(backgroundColor),
                show = true,
            ) {
                TestIcon(Modifier.testTag(IconTestTag))
            }
            expectedIconColor = MaterialTheme.colorScheme.errorContainer
            // As we have .8 alpha, we have to merge this color with background
            expectedIconContainerColor =
                MaterialTheme.colorScheme.onErrorContainer.copy(.8f).compositeOver(backgroundColor)
            expectedTextColor = MaterialTheme.colorScheme.onBackground
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedIconColor)

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedIconContainerColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedTextColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun confirmation_linearText_custom_colors() {
        val customIconColor: Color = Color.Red
        val customIconContainerColor: Color = Color.Green
        val customTextColor: Color = Color.Blue

        rule.setContentWithTheme {
            Confirmation(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                show = true,
                text = { Text("Text") },
                colors =
                    ConfirmationDefaults.confirmationColors(
                        iconColor = customIconColor,
                        iconContainerColor = customIconContainerColor,
                        textColor = customTextColor
                    )
            ) {
                TestIcon(Modifier.testTag(IconTestTag))
            }
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconContainerColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customTextColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun confirmation_curvedText_custom_colors() {
        val customIconColor: Color = Color.Red
        val customIconContainerColor: Color = Color.Green
        val customTextColor: Color = Color.Blue

        rule.setContentWithTheme {
            Confirmation(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                show = true,
                colors =
                    ConfirmationDefaults.confirmationColors(
                        iconColor = customIconColor,
                        iconContainerColor = customIconContainerColor,
                        textColor = customTextColor
                    ),
                curvedText = ConfirmationDefaults.curvedText(CurvedText)
            ) {
                TestIcon(Modifier.testTag(IconTestTag))
            }
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconContainerColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customTextColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun successConfirmation_curvedText_custom_colors() {
        val customIconColor: Color = Color.Red
        val customIconContainerColor: Color = Color.Green
        val customTextColor: Color = Color.Blue

        rule.setContentWithTheme {
            SuccessConfirmation(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                show = true,
                colors =
                    ConfirmationDefaults.successColors(
                        iconColor = customIconColor,
                        iconContainerColor = customIconContainerColor,
                        textColor = customTextColor
                    ),
            ) {
                TestIcon(Modifier.testTag(IconTestTag))
            }
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconContainerColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customTextColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun failureConfirmation_curvedText_custom_colors() {
        val customIconColor: Color = Color.Red
        val customIconContainerColor: Color = Color.Green
        val customTextColor: Color = Color.Blue

        rule.setContentWithTheme {
            FailureConfirmation(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                show = true,
                colors =
                    ConfirmationDefaults.failureColors(
                        iconColor = customIconColor,
                        iconContainerColor = customIconContainerColor,
                        textColor = customTextColor
                    ),
            ) {
                TestIcon(Modifier.testTag(IconTestTag))
            }
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconContainerColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customTextColor)
    }
}

private const val IconTestTag = "icon"
private const val TextTestTag = "text"
private const val CurvedText = "CurvedText"
