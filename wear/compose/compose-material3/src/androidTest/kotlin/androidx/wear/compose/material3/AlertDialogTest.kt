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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.material3.lazy.ResponsiveTransformationSpec
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
class AlertDialogTest(@TestParameter private val contentContainer: ContentContainer) {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun dialog_supports_testtag_with_bottomButton() {
        rule.setContentWithTheme {
            AlertDialogHelper(
                contentContainer = contentContainer,
                visible = true,
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                edgeButton = {
                    AlertDialogDefaults.EdgeButton(
                        onClick = {},
                        modifier = Modifier.testTag(ConfirmButtonTestTag),
                    )
                },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
        rule.onNodeWithTag(ConfirmButtonTestTag).assertExists()
    }

    @Test
    fun dialog_supports_testtag_with_no_buttons() {
        rule.setContentWithTheme {
            AlertDialogHelper(
                contentContainer = contentContainer,
                visible = true,
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun dialog_supports_testtag_with_confirmDismissButtons() {
        rule.setContentWithTheme {
            AlertDialogHelper(
                contentContainer = contentContainer,
                visible = true,
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                confirmButton = {
                    AlertDialogDefaults.ConfirmButton(
                        onClick = {},
                        modifier = Modifier.testTag(ConfirmButtonTestTag),
                    )
                },
                dismissButton = {
                    AlertDialogDefaults.DismissButton(
                        onClick = {},
                        modifier = Modifier.testTag(DismissButtonTestTag),
                    )
                },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
        rule.onNodeWithTag(ConfirmButtonTestTag).assertExists()
        rule.onNodeWithTag(DismissButtonTestTag).assertExists()
    }

    @Test
    fun content_supports_testtag_with_bottomButton() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                edgeButton = {
                    AlertDialogDefaults.EdgeButton(
                        onClick = {},
                        modifier = Modifier.testTag(ConfirmButtonTestTag),
                    )
                },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
        rule.onNodeWithTag(ConfirmButtonTestTag).assertExists()
    }

    @Test
    fun content_supports_testtag_with_no_buttons() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun content_supports_testtag_with_confirmDismissButtons() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                confirmButton = {
                    AlertDialogDefaults.ConfirmButton(
                        onClick = {},
                        modifier = Modifier.testTag(ConfirmButtonTestTag),
                    )
                },
                dismissButton = {
                    AlertDialogDefaults.DismissButton(
                        onClick = {},
                        modifier = Modifier.testTag(DismissButtonTestTag),
                    )
                },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
        rule.onNodeWithTag(ConfirmButtonTestTag).assertExists()
        rule.onNodeWithTag(DismissButtonTestTag).assertExists()
    }

    @Test
    fun displays_icon_with_bottomButton() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                icon = { TestImage(TEST_TAG) },
                title = {},
                edgeButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_icon_with_confirmDismissButtons() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                icon = { TestImage(TEST_TAG) },
                title = {},
                confirmButton = {},
                dismissButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_title_with_bottomButton() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                title = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                edgeButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_title_with_confirmDismissButtons() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                title = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                confirmButton = {},
                dismissButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_messageText_with_bottomButton() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                title = {},
                text = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                edgeButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_messageText_with_confirmDismissButtons() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                title = {},
                text = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                confirmButton = {},
                dismissButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_content_with_bottomButton() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                title = {},
                edgeButton = {},
                slcContent = { item { Text("Text", modifier = Modifier.testTag(TEST_TAG)) } },
                tlcContent = { item { Text("Text", modifier = Modifier.testTag(TEST_TAG)) } },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_content_with_confirmDismissButtons() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                title = {},
                confirmButton = {},
                dismissButton = {},
                slcContent = { item { Text("Text", modifier = Modifier.testTag(TEST_TAG)) } },
                tlcContent = { item { Text("Text", modifier = Modifier.testTag(TEST_TAG)) } },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_confirmButton() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                title = {},
                confirmButton = { Button(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) {} },
                dismissButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_dismissButton() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                title = {},
                confirmButton = {},
                dismissButton = { Button(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) {} },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_bottomButton() {
        rule.setContentWithTheme {
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                title = {},
                edgeButton = { Button(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) {} },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun alert_dialog_dismiss_button_content_description() {
        val description = "Test Description"
        rule.setContentWithTheme {
            AlertDialogHelper(
                contentContainer = contentContainer,
                title = {},
                dismissButton = {
                    AlertDialogDefaults.DismissButton(
                        onClick = {},
                        modifier = Modifier.testTag(TEST_TAG),
                    ) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = description)
                    }
                },
                confirmButton = {},
                onDismissRequest = {},
                visible = true,
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertContentDescriptionEquals(description)
    }

    @Test
    fun alert_dialog_confirm_button_content_description() {
        val description = "Test Description"
        rule.setContentWithTheme {
            AlertDialogHelper(
                contentContainer = contentContainer,
                title = {},
                dismissButton = {},
                confirmButton = {
                    AlertDialogDefaults.ConfirmButton(
                        onClick = {},
                        modifier = Modifier.testTag(TEST_TAG),
                    ) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = description)
                    }
                },
                onDismissRequest = {},
                visible = true,
            )
        }

        rule.onNodeWithContentDescription(description).assertExists().assertHasClickAction()
    }

    @Test
    fun alert_dialog_title_exists_once_only() {
        // Adding this test because we saw client tests failing with the title being found twice.
        val title = "Test Title"
        val description = "Test Description"

        rule.setContentWithTheme {
            AlertDialogHelper(
                contentContainer = contentContainer,
                title = { Text(title) },
                dismissButton = {},
                confirmButton = {
                    AlertDialogDefaults.ConfirmButton(
                        onClick = {},
                        modifier = Modifier.testTag(TEST_TAG),
                    ) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = description)
                    }
                },
                onDismissRequest = {},
                visible = true,
            )
        }

        rule.onNodeWithText(title).assertExists()
    }

    @Test
    fun alert_dialog_confirm_button_exists_once_only() {
        // Adding this test because we saw client tests failing with button being found twice.
        val confirm = "Confirm"

        rule.setContentWithTheme {
            AlertDialogHelper(
                contentContainer = contentContainer,
                title = {},
                dismissButton = {},
                confirmButton = {
                    AlertDialogDefaults.ConfirmButton(
                        onClick = {},
                        modifier = Modifier.testTag(TEST_TAG),
                    ) {
                        Text(confirm)
                    }
                },
                onDismissRequest = {},
                visible = true,
            )
        }

        rule.onNodeWithText(confirm).assertExists()
    }

    @Test
    fun alert_dialog_dismiss_button_exists_once_only() {
        // Adding this test because we saw client tests failing with button being found twice.
        val dismiss = "Dismiss"

        rule.setContentWithTheme {
            AlertDialogHelper(
                contentContainer = contentContainer,
                title = {},
                dismissButton = {
                    AlertDialogDefaults.DismissButton(onClick = {}) { Text(dismiss) }
                },
                confirmButton = {},
                onDismissRequest = {},
                visible = true,
            )
        }

        rule.onNodeWithText(dismiss).assertExists()
    }

    @Test
    fun supports_swipeToDismiss_confirmDismissButtons() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            var showDialog by remember { mutableStateOf(true) }
            AlertDialogHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                dismissButton = {},
                confirmButton = {},
                onDismissRequest = {
                    showDialog = false
                    dismissCounter++
                },
                visible = showDialog,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @Test
    fun supports_swipeToDismiss_bottomButton() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            var showDialog by remember { mutableStateOf(true) }
            AlertDialogHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                edgeButton = {},
                onDismissRequest = {
                    showDialog = false
                    dismissCounter++
                },
                visible = showDialog,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun onDismissRequest_not_called_when_hidden() {
        val show = mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            AlertDialogHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                edgeButton = {},
                onDismissRequest = { dismissCounter++ },
                visible = show.value,
            )
        }
        rule.waitForIdle()
        show.value = false
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(0, dismissCounter)
    }

    @Test
    fun hides_dialog_when_show_false() {
        rule.setContentWithTheme {
            AlertDialogHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                edgeButton = {},
                onDismissRequest = {},
                visible = false,
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun applies_correct_titleProperties() {
        var expectedContentColor: Color = Color.Unspecified
        var expectedTextStyle: TextStyle = TextStyle.Default
        var expectedTextAlign: TextAlign? = null
        var expectedTextMaxLines = 0

        var actualContentColor: Color = Color.Unspecified
        var actualTextStyle: TextStyle = TextStyle.Default
        var actualTextAlign: TextAlign? = TextAlign.Unspecified
        var actualTextMaxLines = 0

        rule.setContentWithTheme {
            expectedContentColor = MaterialTheme.colorScheme.onBackground
            expectedTextStyle = MaterialTheme.typography.titleMedium
            expectedTextAlign = TextAlign.Center
            expectedTextMaxLines = AlertTitleMaxLines
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG),
                title = {
                    Text("Title")
                    actualContentColor = LocalContentColor.current
                    actualTextStyle = LocalTextStyle.current
                    actualTextAlign = LocalTextConfiguration.current.textAlign
                    actualTextMaxLines = LocalTextConfiguration.current.maxLines
                },
                edgeButton = {},
            )
        }

        assertEquals(expectedContentColor, actualContentColor)
        assertEquals(expectedTextStyle, actualTextStyle)
        assertEquals(expectedTextAlign, actualTextAlign)
        assertEquals(expectedTextMaxLines, actualTextMaxLines)
    }

    @Test
    fun applies_correct_textMessage_properties() {
        var expectedContentColor: Color = Color.Unspecified
        var expectedTextStyle: TextStyle = TextStyle.Default
        var expectedTextAlign: TextAlign? = null

        var actualContentColor: Color = Color.Unspecified
        var actualTextStyle: TextStyle = TextStyle.Default
        var actualTextAlign: TextAlign? = TextAlign.Unspecified

        rule.setContentWithTheme {
            expectedContentColor = MaterialTheme.colorScheme.onBackground
            expectedTextStyle = MaterialTheme.typography.bodyMedium
            expectedTextAlign = TextAlign.Center
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                text = {
                    Text("Text")
                    actualContentColor = LocalContentColor.current
                    actualTextStyle = LocalTextStyle.current
                    actualTextAlign = LocalTextConfiguration.current.textAlign
                },
                edgeButton = {},
            )
        }

        assertEquals(expectedContentColor, actualContentColor)
        assertEquals(expectedTextStyle, actualTextStyle)
        assertEquals(expectedTextAlign, actualTextAlign)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun override_customTitleColor() {
        var expectedContentColor: Color = Color.Unspecified

        rule.setContentWithTheme {
            expectedContentColor = Color.Yellow
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG),
                title = { Text("Title", color = expectedContentColor) },
                edgeButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedContentColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun override_customTextMessage_color() {
        var expectedContentColor: Color = Color.Unspecified

        rule.setContentWithTheme {
            expectedContentColor = Color.Yellow
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                text = { Text("Text", color = expectedContentColor) },
                edgeButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedContentColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun override_customBackground_color() {
        var expectedBackgroundColor: Color = Color.Unspecified

        rule.setContentWithTheme {
            expectedBackgroundColor = Color.Yellow
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG).background(expectedBackgroundColor),
                title = {},
                edgeButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedBackgroundColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun override_confirmDismissButton_colors() {
        var expectedConfirmColor: Color = Color.Unspecified
        var expectedDismissColor: Color = Color.Unspecified

        rule.setContentWithTheme {
            expectedConfirmColor = Color.Yellow
            expectedDismissColor = Color.Red
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                confirmButton = {
                    AlertDialogDefaults.ConfirmButton(
                        onClick = {},
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = expectedConfirmColor
                            ),
                    )
                },
                dismissButton = {
                    AlertDialogDefaults.DismissButton(
                        onClick = {},
                        colors =
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = expectedDismissColor
                            ),
                    )
                },
            )
        }
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedConfirmColor)
            .assertContainsColor(expectedDismissColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun override_edgeButton_color() {
        var expectedEdgeButtonColor: Color = Color.Unspecified

        rule.setContentWithTheme {
            expectedEdgeButtonColor = Color.Yellow
            AlertDialogContentHelper(
                contentContainer = contentContainer,
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                edgeButton = {
                    AlertDialogDefaults.EdgeButton(
                        onClick = {},
                        colors =
                            ButtonDefaults.buttonColors(containerColor = expectedEdgeButtonColor),
                    )
                },
            )
        }
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedEdgeButtonColor)
    }

    @Test
    fun confirmDismissButtons_withFixedContent_spacing() {
        var expectedBottomPadding = 0.dp
        var screenHeight = 0.dp
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(AlertScreenSize) {
                screenHeight = LocalConfiguration.current.screenHeightDp.dp
                AlertDialogContentHelper(
                    contentContainer = contentContainer,
                    title = {
                        Text("Title", modifier = Modifier.testTag(TitleTestTag).fillMaxSize())
                    },
                    confirmButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(ConfirmButtonTestTag)) {}
                    },
                    dismissButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(DismissButtonTestTag)) {}
                    },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.size(screenHeight).testTag(TEST_TAG),
                )
                expectedBottomPadding = PaddingDefaults.verticalContentPadding()
            }
        }

        val confirmButtonBottom =
            rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot().bottom
        val confirmButtonTop =
            rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot().top
        val titleBottom = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().bottom

        confirmButtonBottom.assertIsEqualTo(screenHeight - expectedBottomPadding)
        confirmButtonTop.assertIsEqualTo(titleBottom + ConfirmDismissButtonsTopSpacing)
    }

    @Test
    fun confirmDismissButtons_withScrollableContent_spacing() {
        var expectedBottomPadding = 0.dp
        var screenHeight = 0.dp
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(SmallScreenSize) {
                screenHeight = LocalConfiguration.current.screenHeightDp.dp
                AlertDialogContentHelper(
                    contentContainer = contentContainer,
                    title = {
                        Text("Title", modifier = Modifier.testTag(TitleTestTag).fillMaxSize())
                    },
                    confirmButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(ConfirmButtonTestTag)) {}
                    },
                    dismissButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(DismissButtonTestTag)) {}
                    },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.size(screenHeight * 3).testTag(TEST_TAG),
                )
                expectedBottomPadding =
                    screenHeightFraction(ConfirmDismissButtonsBottomSpacingFraction) +
                        PaddingDefaults.verticalContentPadding()
            }
        }
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeUp() }

        val confirmButtonTop =
            rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot().top
        val confirmButtonBottom =
            rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot().bottom
        val titleBottom = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().bottom

        confirmButtonBottom.assertIsEqualTo(screenHeight - expectedBottomPadding)
        confirmButtonTop.assertIsEqualTo(titleBottom + ConfirmDismissButtonsTopSpacing)
    }

    @Test
    fun confirmDismissButtons_with_icon_title_positioning() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(AlertScreenSize) {
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                AlertDialogContentHelper(
                    contentContainer = contentContainer,
                    icon = { TestIcon(iconLabel = IconTestTag) },
                    title = {
                        Text("Title", modifier = Modifier.testTag(TitleTestTag).fillMaxSize())
                    },
                    confirmButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(ConfirmButtonTestTag)) {}
                    },
                    dismissButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(DismissButtonTestTag)) {}
                    },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.size(screenHeight).testTag(TEST_TAG),
                )
            }
        }

        val iconBottom = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot().bottom
        val titleTop = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().top
        val titleBottom = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().bottom
        val confirmButtonTop =
            rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot().top

        titleTop.assertIsEqualTo(iconBottom + AlertIconBottomSpacing)
        confirmButtonTop.assertIsEqualTo(titleBottom + ConfirmDismissButtonsTopSpacing)
    }

    @Test
    fun confirmDismissButtons_with_icon_title_textMessage_positioning() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(AlertScreenSize) {
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                AlertDialogContentHelper(
                    contentContainer = contentContainer,
                    icon = { TestIcon(iconLabel = IconTestTag) },
                    title = { Text("Title", modifier = Modifier.testTag(TitleTestTag)) },
                    text = { Text("Text", modifier = Modifier.fillMaxSize().testTag(TextTestTag)) },
                    confirmButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(ConfirmButtonTestTag)) {}
                    },
                    dismissButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(DismissButtonTestTag)) {}
                    },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.size(screenHeight).testTag(TEST_TAG),
                )
            }
        }

        rule.waitForIdle()

        val iconBottom = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot().bottom
        val titleTop = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().top
        val titleBottom = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().bottom
        val textTop = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot().top
        val textBottom = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot().bottom
        val confirmButtonTop =
            rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot().top

        titleTop.assertIsEqualTo(iconBottom + AlertIconBottomSpacing)
        textTop.assertIsEqualTo(titleBottom + AlertTextMessageTopSpacing)
        confirmButtonTop.assertIsEqualTo(textBottom + ConfirmDismissButtonsTopSpacing)
    }

    @Test
    fun confirmDismissButtons_with_icon_title_textMessage_content_positioning() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(AlertScreenSize) {
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                AlertDialogContentHelper(
                    contentContainer = contentContainer,
                    icon = { TestImage(IconTestTag) },
                    title = { Text("Title", modifier = Modifier.testTag(TitleTestTag)) },
                    text = { Text("Text", modifier = Modifier.testTag(TextTestTag)) },
                    confirmButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(ConfirmButtonTestTag)) {}
                    },
                    dismissButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(DismissButtonTestTag)) {}
                    },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.size(screenHeight).testTag(TEST_TAG),
                    slcContent = {
                        item { Text("ContentText", modifier = Modifier.testTag(ContentTestTag)) }
                    },
                    tlcContent = {
                        item { Text("ContentText", modifier = Modifier.testTag(ContentTestTag)) }
                    },
                    transformingSpec = ResponsiveTransformationSpec.NoOpTransformationSpec,
                )
            }
        }

        val iconBottom = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot().bottom
        val titleTop = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().top
        val titleBottom = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().bottom
        val textTop = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot().top
        val textBottom = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot().bottom
        val contentTop = rule.onNodeWithTag(ContentTestTag).getUnclippedBoundsInRoot().top
        val contentBottom = rule.onNodeWithTag(ContentTestTag).getUnclippedBoundsInRoot().bottom
        val confirmButtonTop =
            rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot().top

        titleTop.assertIsEqualTo(iconBottom + AlertIconBottomSpacing)
        textTop.assertIsEqualTo(titleBottom + AlertTextMessageTopSpacing)
        contentTop.assertIsEqualTo(textBottom + AlertContentTopSpacing)
        confirmButtonTop.assertIsEqualTo(contentBottom + ConfirmDismissButtonsTopSpacing)
    }

    @Test
    fun confirmDismissButtons_with_icon_title_content_positioning() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(AlertScreenSize) {
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                AlertDialogContentHelper(
                    contentContainer = contentContainer,
                    icon = { TestImage(IconTestTag) },
                    title = { Box(modifier = Modifier.size(3.dp).testTag(TitleTestTag)) },
                    confirmButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(ConfirmButtonTestTag)) {}
                    },
                    dismissButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(DismissButtonTestTag)) {}
                    },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.size(screenHeight).testTag(TEST_TAG),
                    slcContent = {
                        item { Text("ContentText", modifier = Modifier.testTag(ContentTestTag)) }
                    },
                    tlcContent = {
                        item { Text("ContentText", modifier = Modifier.testTag(ContentTestTag)) }
                    },
                )
            }
        }

        val iconBottom = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot().bottom
        val titleTop = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().top
        val titleBottom = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().bottom
        val contentTop = rule.onNodeWithTag(ContentTestTag).getUnclippedBoundsInRoot().top
        val contentBottom = rule.onNodeWithTag(ContentTestTag).getUnclippedBoundsInRoot().bottom
        val confirmButtonTop =
            rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot().top

        titleTop.assertIsEqualTo(iconBottom + AlertIconBottomSpacing)
        contentTop.assertIsEqualTo(titleBottom + AlertContentTopSpacing)
        confirmButtonTop.assertIsEqualTo(contentBottom + ConfirmDismissButtonsTopSpacing)
    }

    @Test
    fun noBottomButton_withFixedContent_spacing() {
        var expectedBottomPadding = 0.dp
        var screenHeight = 0.dp
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(AlertScreenSize) {
                screenHeight = LocalConfiguration.current.screenHeightDp.dp
                AlertDialogContentHelper(
                    contentContainer = contentContainer,
                    title = {
                        Text("Title", modifier = Modifier.testTag(TitleTestTag).fillMaxSize())
                    },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.size(screenHeight).testTag(TEST_TAG),
                )
                expectedBottomPadding =
                    if (contentContainer == ContentContainer.TLC) {
                        PaddingDefaults.verticalContentPadding()
                    } else
                        screenHeightFraction(AlertDialogDefaults.noEdgeButtonBottomPaddingFraction)
            }
        }

        val titleBottom = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().bottom
        titleBottom.assertIsEqualTo(screenHeight - expectedBottomPadding)
    }

    @Test
    fun noBottomButton_withScrollableContent_spacing() {
        var expectedBottomPadding = 0.dp
        var screenHeight = 0.dp
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(AlertScreenSize) {
                screenHeight = LocalConfiguration.current.screenHeightDp.dp
                AlertDialogContentHelper(
                    contentContainer = contentContainer,
                    title = {
                        Text(
                            "Title",
                            modifier = Modifier.testTag(TitleTestTag).size(AlertScreenSize.dp * 2),
                        )
                    },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.wrapContentSize().testTag(TEST_TAG),
                )
                expectedBottomPadding =
                    screenHeightFraction(AlertDialogDefaults.noEdgeButtonBottomPaddingFraction)
            }
        }
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeUp()
            swipeUp()
        }

        val titleBottom = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().bottom
        titleBottom.assertIsEqualTo(screenHeight - expectedBottomPadding)
    }

    // TODO: add more positioning tests for EdgeButton.
}

@Composable
fun AlertDialogHelper(
    // Common params
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,

    // Button params
    confirmButton: (@Composable RowScope.() -> Unit)? = null,
    dismissButton: (@Composable RowScope.() -> Unit)? = null,
    edgeButton: (@Composable BoxScope.() -> Unit)? = null,

    // Content params
    contentContainer: ContentContainer,
    slcContent: (ScalingLazyListScope.() -> Unit)? = null,
    tlcContent: (TransformingLazyColumnScope.() -> Unit)? = null,

    // Unified TLC-specific param
    transformingSpec: TransformationSpec = rememberTransformationSpec(),
) {
    when {
        // Case 1: Two-button dialog (confirm button is the trigger)
        confirmButton != null -> {
            val finalDismissButton =
                dismissButton ?: { AlertDialogDefaults.DismissButton(onDismissRequest) }
            if (contentContainer == ContentContainer.TLC) {
                AlertDialog(
                    visible = visible,
                    onDismissRequest = onDismissRequest,
                    confirmButton = confirmButton,
                    dismissButton = finalDismissButton,
                    title = title,
                    transformationSpec = transformingSpec,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    properties = properties,
                    content = tlcContent,
                )
            } else { // SLC
                AlertDialog(
                    visible = visible,
                    onDismissRequest = onDismissRequest,
                    confirmButton = confirmButton,
                    dismissButton = finalDismissButton,
                    title = title,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    properties = properties,
                    content = slcContent,
                )
            }
        }
        // Case 2: Single edge-button dialog
        edgeButton != null -> {
            if (contentContainer == ContentContainer.TLC) {
                AlertDialog(
                    visible = visible,
                    onDismissRequest = onDismissRequest,
                    edgeButton = edgeButton,
                    title = title,
                    transformationSpec = transformingSpec,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    properties = properties,
                    content = tlcContent,
                )
            } else { // SLC
                AlertDialog(
                    visible = visible,
                    onDismissRequest = onDismissRequest,
                    edgeButton = edgeButton,
                    title = title,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    properties = properties,
                    content = slcContent,
                )
            }
        }
        // Case 3: No buttons
        else -> {
            if (contentContainer == ContentContainer.TLC) {
                AlertDialog(
                    visible = visible,
                    onDismissRequest = onDismissRequest,
                    title = title,
                    transformationSpec = transformingSpec,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    properties = properties,
                    content = tlcContent,
                )
            } else { // SLC
                AlertDialog(
                    visible = visible,
                    onDismissRequest = onDismissRequest,
                    title = title,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    properties = properties,
                    content = slcContent,
                )
            }
        }
    }
}

@Composable
fun AlertDialogContentHelper(
    // Common params
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues? = null,

    // Button params
    confirmButton: (@Composable RowScope.() -> Unit)? = null,
    dismissButton: (@Composable RowScope.() -> Unit)? = null,
    edgeButton: (@Composable BoxScope.() -> Unit)? = null,

    // Content params
    contentContainer: ContentContainer,
    slcContent: (ScalingLazyListScope.() -> Unit)? = null,
    tlcContent: (TransformingLazyColumnScope.() -> Unit)? = null,

    // Unified TLC-specific param
    transformingSpec: TransformationSpec = rememberTransformationSpec(),
) {
    when {
        // Case 1: Two-button dialog
        confirmButton != null -> {
            val finalDismissButton =
                dismissButton ?: { AlertDialogDefaults.DismissButton(onClick = {}) }
            val finalContentPadding =
                contentPadding
                    ?: if (icon != null) {
                        AlertDialogDefaults.confirmDismissWithIconContentPadding()
                    } else {
                        AlertDialogDefaults.confirmDismissContentPadding()
                    }

            if (contentContainer == ContentContainer.TLC) {
                AlertDialogContent(
                    confirmButton = confirmButton,
                    title = title,
                    dismissButton = finalDismissButton,
                    transformationSpec = transformingSpec,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    contentPadding = finalContentPadding,
                    content = tlcContent,
                )
            } else { // SLC
                AlertDialogContent(
                    confirmButton = confirmButton,
                    title = title,
                    dismissButton = finalDismissButton,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    contentPadding = finalContentPadding,
                    content = slcContent,
                )
            }
        }
        // Case 2: Single edge-button dialog
        edgeButton != null -> {
            val finalContentPadding =
                contentPadding
                    ?: if (icon != null) {
                        AlertDialogDefaults.contentWithIconPadding()
                    } else {
                        AlertDialogDefaults.contentPadding()
                    }

            if (contentContainer == ContentContainer.TLC) {
                AlertDialogContent(
                    edgeButton = edgeButton,
                    title = title,
                    transformationSpec = transformingSpec,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    contentPadding = finalContentPadding,
                    content = tlcContent,
                )
            } else { // SLC
                AlertDialogContent(
                    edgeButton = edgeButton,
                    title = title,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    contentPadding = finalContentPadding,
                    content = slcContent,
                )
            }
        }
        // Case 3: No buttons
        else -> {
            val tlcContentPadding: @Composable (Boolean) -> PaddingValues =
                if (contentPadding != null) {
                    { _ -> contentPadding }
                } else {
                    { isScrollable ->
                        if (icon != null) {
                            AlertDialogDefaults.buttonStackWithIconContentPadding(isScrollable)
                        } else {
                            AlertDialogDefaults.buttonStackContentPadding(isScrollable)
                        }
                    }
                }
            val slcContentPadding =
                contentPadding
                    ?: if (icon != null) {
                        AlertDialogDefaults.contentWithIconPadding()
                    } else {
                        AlertDialogDefaults.contentPadding()
                    }

            if (contentContainer == ContentContainer.TLC) {
                AlertDialogContent(
                    title = title,
                    transformationSpec = transformingSpec,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    contentPadding = tlcContentPadding,
                    content = tlcContent,
                )
            } else { // SLC
                AlertDialogContent(
                    title = title,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    contentPadding = slcContentPadding,
                    content = slcContent,
                )
            }
        }
    }
}

enum class ContentContainer {
    SLC,
    TLC,
}

private const val IconTestTag = "icon"
private const val TitleTestTag = "title"
private const val TextTestTag = "text"
private const val ContentTestTag = "content"
private const val ConfirmButtonTestTag = "confirmButton"
private const val DismissButtonTestTag = "dismissButton"
private const val AlertScreenSize = 400
private const val SmallScreenSize = 100
