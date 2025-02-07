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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.style.TextAlign
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class ConfirmationDialogTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun confirmation_linearText_supports_testtag() {
        rule.setContentWithTheme {
            ConfirmationDialog(
                visible = true,
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
            ConfirmationDialog(
                visible = true,
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
            SuccessConfirmationTest(
                visible = true,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun failureConfirmation_supports_testtag() {
        rule.setContentWithTheme {
            FailureConfirmationTest(
                visible = true,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun confirmation_linearText_supports_swipeToDismiss() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(true) }
            ConfirmationDialog(
                modifier = Modifier.testTag(TEST_TAG),
                text = {},
                onDismissRequest = {
                    visible = false
                    dismissCounter++
                },
                visible = visible
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @Test
    fun confirmation_curvedText_supports_swipeToDismiss() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(true) }
            ConfirmationDialog(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    visible = false
                    dismissCounter++
                },
                visible = visible,
                curvedText = {}
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @Test
    fun successConfirmation_supports_swipeToDismiss() {
        var dismissCounter = 0
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(true) }
            SuccessConfirmationTest(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    visible = false
                    dismissCounter++
                },
                visible = visible,
            )
        }
        // Advancing time so that animation will finish its motion.
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @Test
    fun failureConfirmation_supports_swipeToDismiss() {
        var dismissCounter = 0
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(true) }
            FailureConfirmationTest(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    visible = false
                    dismissCounter++
                },
                visible = visible,
            )
        }
        // Advancing time so that animation will finish its motion.
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @Test
    fun hides_confirmation_linearText_when_show_false() {
        rule.setContentWithTheme {
            ConfirmationDialog(
                visible = false,
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
            ConfirmationDialog(
                visible = false,
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
            SuccessConfirmationTest(
                visible = false,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun hides_failureConfirmation_when_show_false() {
        rule.setContentWithTheme {
            FailureConfirmationTest(
                visible = false,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun confirmation_linearText_onDismissRequest_not_called_when_hidden() {
        val visible = mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            ConfirmationDialog(
                modifier = Modifier.testTag(TEST_TAG),
                text = {},
                onDismissRequest = { dismissCounter++ },
                // Set very long duration so that it won't be dismissed by the timeout
                durationMillis = 100_000,
                visible = visible.value
            ) {}
        }
        rule.waitForIdle()
        visible.value = false
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(0, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun confirmation_curvedText_onDismissRequest_not_called_when_hidden() {
        val visible = mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            ConfirmationDialog(
                modifier = Modifier.testTag(TEST_TAG),
                curvedText = {},
                onDismissRequest = { dismissCounter++ },
                // Set very long duration so that it won't be dismissed by the timeout
                durationMillis = 100_000,
                visible = visible.value
            ) {}
        }
        rule.waitForIdle()
        visible.value = false
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(0, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun successConfirmation_onDismissRequest_not_called_when_hidden() {
        val visible = mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            SuccessConfirmationTest(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = { dismissCounter++ },
                // Set very long duration so that it won't be dismissed by the timeout
                durationMillis = 100_000,
                visible = visible.value
            )
        }
        rule.waitForIdle()
        visible.value = false
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(0, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun failureConfirmation_onDismissRequest_not_called_when_hidden() {
        val visible = mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            FailureConfirmationTest(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = { dismissCounter++ },
                // Set very long duration so that it won't be dismissed by the timeout
                durationMillis = 100_000,
                visible = visible.value
            )
        }
        rule.waitForIdle()
        visible.value = false
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(0, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun confirmation_linearText_calls_onDismissRequest_on_timeout() {
        val visible = mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            ConfirmationDialog(
                modifier = Modifier.testTag(TEST_TAG),
                text = {},
                onDismissRequest = {
                    dismissCounter++
                    visible.value = false
                },
                durationMillis = 100,
                visible = visible.value
            ) {}
        }
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(1, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun confirmation_curvedText_calls_onDismissRequest_on_timeout() {
        val visible = mutableStateOf(true)
        var dismissCounter = 0

        rule.setContentWithTheme {
            ConfirmationDialog(
                modifier = Modifier.testTag(TEST_TAG),
                curvedText = {},
                onDismissRequest = {
                    dismissCounter++
                    visible.value = false
                },
                durationMillis = 100,
                visible = visible.value
            ) {}
        }
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(1, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun successConfirmation_curvedText_calls_onDismissRequest_on_timeout() {
        val visible = mutableStateOf(true)
        var dismissCounter = 0

        rule.setContentWithTheme {
            SuccessConfirmationTest(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    dismissCounter++
                    visible.value = false
                },
                durationMillis = 100,
                visible = visible.value
            )
        }
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(1, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun failureConfirmation_curvedText_calls_onDismissRequest_on_timeout() {
        val visible = mutableStateOf(true)
        var dismissCounter = 0

        rule.setContentWithTheme {
            FailureConfirmationTest(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    dismissCounter++
                    visible.value = false
                },
                durationMillis = 100,
                visible = visible.value
            )
        }
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(1, dismissCounter)
    }

    @Test
    fun confirmation_displays_icon_with_linearText() {
        rule.setContentWithTheme {
            ConfirmationDialog(
                text = { Text("Text", modifier = Modifier.testTag(TextTestTag)) },
                onDismissRequest = {},
                visible = true
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
            ConfirmationDialog(
                onDismissRequest = {},
                visible = true,
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
            val style = ConfirmationDialogDefaults.curvedTextStyle
            SuccessConfirmationDialog(
                onDismissRequest = {},
                visible = true,
                curvedText = { confirmationDialogCurvedText(CurvedText, style) }
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
            val style = ConfirmationDialogDefaults.curvedTextStyle
            FailureConfirmationDialog(
                onDismissRequest = {},
                visible = true,
                curvedText = { confirmationDialogCurvedText(CurvedText, style) }
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
            ConfirmationDialog(
                text = {},
                onDismissRequest = { dismissed = true },
                visible = true
            ) {}
        }
        // Timeout longer than default confirmation duration
        rule.mainClock.advanceTimeBy(ConfirmationDialogDefaults.DurationMillis + 1000)
        assert(dismissed)
    }

    @Test
    fun confirmation_curvedText_dismissed_after_timeout() {
        var dismissed = false
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            ConfirmationDialog(
                onDismissRequest = { dismissed = true },
                visible = true,
                curvedText = {}
            ) {}
        }
        // Timeout longer than default confirmation duration
        rule.mainClock.advanceTimeBy(ConfirmationDialogDefaults.DurationMillis + 1000)
        assert(dismissed)
    }

    @Test
    fun successConfirmation_dismissed_after_timeout() {
        var dismissed = false
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            SuccessConfirmationTest(
                onDismissRequest = { dismissed = true },
                visible = true,
            )
        }
        // Timeout longer than default confirmation duration
        rule.mainClock.advanceTimeBy(ConfirmationDialogDefaults.DurationMillis + 1000)
        assert(dismissed)
    }

    @Test
    fun failureConfirmation_dismissed_after_timeout() {
        var dismissed = false
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            FailureConfirmationTest(
                onDismissRequest = { dismissed = true },
                visible = true,
            )
        }
        // Timeout longer than default confirmation duration
        rule.mainClock.advanceTimeBy(ConfirmationDialogDefaults.DurationMillis + 1000)
        assert(dismissed)
    }

    @Test
    fun confirmation_linearText_positioning() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ConfirmationDialog(
                visible = true,
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
            ConfirmationDialog(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                visible = true,
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
            val style = ConfirmationDialogDefaults.curvedTextStyle
            ConfirmationDialog(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                visible = true,
                curvedText = { confirmationDialogCurvedText(CurvedText, style) }
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
            SuccessConfirmationTest(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                visible = true,
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
            FailureConfirmationTest(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG).background(backgroundColor),
                visible = true,
            ) {
                TestIcon(Modifier.testTag(IconTestTag))
            }
            expectedIconColor = MaterialTheme.colorScheme.errorDim
            // As we have .8 alpha, we have to merge this color with background
            expectedIconContainerColor =
                MaterialTheme.colorScheme.onError.copy(.8f).compositeOver(backgroundColor)
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
            ConfirmationDialog(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                visible = true,
                text = { Text("Text") },
                colors =
                    ConfirmationDialogDefaults.colors(
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
            val style = ConfirmationDialogDefaults.curvedTextStyle
            ConfirmationDialog(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                visible = true,
                colors =
                    ConfirmationDialogDefaults.colors(
                        iconColor = customIconColor,
                        iconContainerColor = customIconContainerColor,
                        textColor = customTextColor
                    ),
                curvedText = { confirmationDialogCurvedText(CurvedText, style) }
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
            val style = ConfirmationDialogDefaults.curvedTextStyle
            SuccessConfirmationDialog(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                visible = true,
                colors =
                    ConfirmationDialogDefaults.successColors(
                        iconColor = customIconColor,
                        iconContainerColor = customIconContainerColor,
                        textColor = customTextColor
                    ),
                curvedText = { confirmationDialogCurvedText(CurvedText, style) }
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
            val style = ConfirmationDialogDefaults.curvedTextStyle
            FailureConfirmationDialog(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                visible = true,
                colors =
                    ConfirmationDialogDefaults.failureColors(
                        iconColor = customIconColor,
                        iconContainerColor = customIconContainerColor,
                        textColor = customTextColor
                    ),
                curvedText = { confirmationDialogCurvedText(CurvedText, style) }
            ) {
                TestIcon(Modifier.testTag(IconTestTag))
            }
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconContainerColor)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customTextColor)
    }

    @Composable
    private fun SuccessConfirmationTest(
        visible: Boolean,
        onDismissRequest: () -> Unit,
        modifier: Modifier = Modifier,
        durationMillis: Long = ConfirmationDialogDefaults.DurationMillis,
    ) {
        val successText = "Success"
        val successTextStyle = ConfirmationDialogDefaults.curvedTextStyle
        SuccessConfirmationDialog(
            modifier = modifier,
            visible = visible,
            onDismissRequest = onDismissRequest,
            curvedText = { confirmationDialogCurvedText(successText, successTextStyle) },
            durationMillis = durationMillis,
        )
    }

    @Composable
    private fun FailureConfirmationTest(
        visible: Boolean,
        onDismissRequest: () -> Unit,
        modifier: Modifier = Modifier,
        durationMillis: Long = ConfirmationDialogDefaults.DurationMillis,
        content: @Composable () -> Unit = {}
    ) {
        val failureText = "Failure"
        val failureTextStyle = ConfirmationDialogDefaults.curvedTextStyle
        FailureConfirmationDialog(
            modifier = modifier,
            visible = visible,
            onDismissRequest = onDismissRequest,
            curvedText = { confirmationDialogCurvedText(failureText, failureTextStyle) },
            durationMillis = durationMillis,
            content = content
        )
    }
}

private const val IconTestTag = "icon"
private const val TextTestTag = "text"
private const val CurvedText = "CurvedText"
