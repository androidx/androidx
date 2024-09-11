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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class AlertDialogScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun alert_shortTitle_bottomButton(@TestParameter screenSize: ScreenSize) =
        rule.verifyAlertDialogScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            showIcon = false,
            showContent = false,
            showTwoButtons = false,
            scrollToBottom = false,
            screenSize = screenSize,
            titleText = "Network error",
            messageText = null
        )

    @Test
    fun alert_shortTitle_confirmDismissButtons(@TestParameter screenSize: ScreenSize) =
        rule.verifyAlertDialogScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            showIcon = false,
            showContent = false,
            showTwoButtons = true,
            scrollToBottom = false,
            screenSize = screenSize,
            titleText = "Network error",
            messageText = null
        )

    @Test
    fun alert_title_bottomButton(@TestParameter screenSize: ScreenSize) =
        rule.verifyAlertDialogScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            showIcon = false,
            showContent = false,
            showTwoButtons = false,
            scrollToBottom = false,
            screenSize = screenSize,
            messageText = null,
        )

    @Test
    fun alert_title_confirmDismissButtons(@TestParameter screenSize: ScreenSize) =
        rule.verifyAlertDialogScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            showIcon = false,
            showContent = false,
            showTwoButtons = true,
            scrollToBottom = false,
            screenSize = screenSize,
            messageText = null
        )

    @Test
    fun alert_icon_title_bottomButton(@TestParameter screenSize: ScreenSize) {
        rule.verifyAlertDialogScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            showIcon = true,
            showContent = false,
            showTwoButtons = false,
            scrollToBottom = false,
            screenSize = screenSize,
            messageText = null
        )
    }

    @Test
    fun alert_icon_title_confirmDismissButtons(@TestParameter screenSize: ScreenSize) {
        rule.verifyAlertDialogScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            showIcon = true,
            showContent = false,
            showTwoButtons = true,
            scrollToBottom = false,
            screenSize = screenSize,
            messageText = null
        )
    }

    @Test
    fun alert_icon_title_messageText_bottomButton(@TestParameter screenSize: ScreenSize) {
        rule.verifyAlertDialogScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            showIcon = true,
            showContent = false,
            showTwoButtons = false,
            scrollToBottom = false,
            screenSize = screenSize
        )
    }

    @Test
    fun alert_icon_title_messageText_content_confirmDismissButtons(
        @TestParameter screenSize: ScreenSize
    ) {
        rule.verifyAlertDialogScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            showIcon = true,
            showContent = true,
            showTwoButtons = false,
            scrollToBottom = false,
            screenSize = screenSize
        )
    }

    @Test
    fun alert_icon_title_messageText_content_bottomButton_bottom(
        @TestParameter screenSize: ScreenSize
    ) {
        rule.verifyAlertDialogScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            showIcon = true,
            showContent = true,
            showTwoButtons = false,
            scrollToBottom = true,
            screenSize = screenSize
        )
    }

    @Test
    fun alert_icon_title_messageText_content_confirmDismissButtons_bottom(
        @TestParameter screenSize: ScreenSize
    ) {
        rule.verifyAlertDialogScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            showIcon = true,
            showContent = true,
            showTwoButtons = true,
            scrollToBottom = true,
            screenSize = screenSize
        )
    }

    @Test
    fun alert_title_longMessageText_bottomButton(@TestParameter screenSize: ScreenSize) {
        rule.verifyAlertDialogScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            showIcon = false,
            showContent = false,
            showTwoButtons = false,
            scrollToBottom = false,
            screenSize = screenSize,
            messageText = longMessageText
        )
    }

    @Test
    fun alert_title_longMessageText_confirmDismissButtons(@TestParameter screenSize: ScreenSize) {
        rule.verifyAlertDialogScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            showIcon = false,
            showContent = false,
            showTwoButtons = true,
            scrollToBottom = false,
            screenSize = screenSize,
            messageText = longMessageText
        )
    }

    private fun ComposeContentTestRule.verifyAlertDialogScreenshot(
        testName: TestName,
        screenshotRule: AndroidXScreenshotTestRule,
        showIcon: Boolean,
        showContent: Boolean,
        showTwoButtons: Boolean,
        scrollToBottom: Boolean,
        screenSize: ScreenSize,
        messageText: String? = "Your battery is low. Turn on battery saver.",
        titleText: String = "Mobile network is not currently available"
    ) {
        setContentWithTheme {
            ScreenConfiguration(screenSize.size) {
                AlertDialogHelper(
                    modifier = Modifier.size(screenSize.size.dp).testTag(TEST_TAG),
                    title = { Text(titleText) },
                    icon =
                        if (showIcon) {
                            { Icon(Icons.Filled.Favorite, contentDescription = null) }
                        } else null,
                    showTwoButtons = showTwoButtons,
                    text =
                        if (messageText != null) {
                            { Text(messageText) }
                        } else null,
                    content =
                        if (showContent) {
                            {
                                item {
                                    FilledTonalButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {},
                                        label = { Text("Action 1") },
                                    )
                                }
                                item { AlertDialogDefaults.GroupSeparator() }
                                item {
                                    FilledTonalButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {},
                                        label = { Text("Action 2") },
                                    )
                                }
                            }
                        } else null
                )
            }
        }
        if (scrollToBottom) {
            onNodeWithTag(TEST_TAG).performTouchInput { swipeUp() }
        }
        onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.goldenIdentifier())
    }
}

@Composable
private fun AlertDialogHelper(
    modifier: Modifier,
    title: @Composable () -> Unit,
    icon: @Composable (() -> Unit)?,
    text: @Composable (() -> Unit)?,
    showTwoButtons: Boolean,
    content: (ScalingLazyListScope.() -> Unit)?
) {
    if (showTwoButtons) {
        AlertDialog(
            show = true,
            onDismissRequest = {},
            modifier = modifier,
            title = title,
            icon = icon,
            text = text,
            confirmButton = { AlertDialogDefaults.ConfirmButton({}) },
            dismissButton = { AlertDialogDefaults.DismissButton({}) },
            content = content
        )
    } else {
        AlertDialog(
            show = true,
            onDismissRequest = {},
            modifier = modifier,
            title = title,
            icon = icon,
            text = text,
            bottomButton = { AlertDialogDefaults.BottomButton({}) },
            content = content
        )
    }
}

internal const val longMessageText =
    "Allow Map to access your location even when you're not using the app? Your location is used to automatically map places to activities."
