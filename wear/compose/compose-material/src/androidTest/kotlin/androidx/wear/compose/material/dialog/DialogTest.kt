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
package androidx.wear.compose.material.dialog

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.LocalContentColor
import androidx.wear.compose.material.LocalTextStyle
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.TEST_TAG
import androidx.wear.compose.material.TestImage
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.assertContainsColor
import androidx.wear.compose.material.setContentWithTheme
import androidx.wear.compose.material.setContentWithThemeForSizeAssertions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

internal const val ICON_TAG = "icon"
internal const val TITLE_TAG = "Title"
internal const val BODY_TAG = "Body"
internal const val BUTTON_TAG = "Button"
internal const val CHIP_TAG = "Chip"

class DialogBehaviourTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag_on_alert_with_buttons() {
        rule.setContentWithTheme {
            Alert(
                title = {},
                negativeButton = {},
                positiveButton = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun supports_testtag_on_alert_with_chips() {
        rule.setContentWithTheme {
            Alert(
                title = {},
                message = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun supports_testtag_on_confirmation() {
        rule.setContentWithTheme {
            Confirmation(
                onTimeout = {},
                icon = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_icon_on_alert_with_buttons() {
        rule.setContentWithTheme {
            Alert(
                icon = { TestImage(TEST_TAG) },
                title = {},
                negativeButton = {},
                positiveButton = {},
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_icon_on_alert_with_chips() {
        rule.setContentWithTheme {
            Alert(
                icon = { TestImage(TEST_TAG) },
                title = {},
                message = {},
                content = {},
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_icon_on_confirmation() {
        rule.setContentWithTheme {
            Confirmation(
                onTimeout = {},
                icon = { TestImage(TEST_TAG) },
                content = {},
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_title_on_alert_with_buttons() {
        rule.setContentWithTheme {
            Alert(
                title = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                negativeButton = {},
                positiveButton = {},
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_title_on_alert_with_chips() {
        rule.setContentWithTheme {
            Alert(
                icon = {},
                title = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                message = {},
                content = {},
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_title_on_confirmation() {
        rule.setContentWithTheme {
            Confirmation(
                onTimeout = {},
                icon = {},
                content = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_bodymessage_on_alert_with_buttons() {
        rule.setContentWithTheme {
            Alert(
                title = {},
                negativeButton = {},
                positiveButton = {},
                content = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_bodymessage_on_alert_with_chips() {
        rule.setContentWithTheme {
            Alert(
                icon = {},
                title = {},
                message = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                content = {},
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_buttons_on_alert_with_buttons() {
        val buttonTag1 = "Button1"
        val buttonTag2 = "Button2"

        rule.setContentWithTheme {
            Alert(
                title = {},
                negativeButton = {
                    Button(onClick = {}, modifier = Modifier.testTag(buttonTag1), content = {})
                },
                positiveButton = {
                    Button(onClick = {}, modifier = Modifier.testTag(buttonTag2), content = {})
                },
                content = {},
            )
        }

        rule.onNodeWithTag(buttonTag1).assertExists()
        rule.onNodeWithTag(buttonTag2).assertExists()
    }

    @Test
    fun supports_swipetodismiss_on_wrapped_alertdialog_with_buttons() {
        rule.setContentWithTheme {
            Box {
                var showDialog by remember { mutableStateOf(true) }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Start Screen")
                }
                Dialog(
                    showDialog = showDialog,
                    onDismissRequest = { showDialog = false },
                ) {
                    Alert(
                        title = {},
                        negativeButton = { Button(onClick = {}, content = {}) },
                        positiveButton = { Button(onClick = {}, content = {}) },
                        content = { Text("Dialog", modifier = Modifier.testTag(TEST_TAG)) },
                    )
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun supports_swipetodismiss_on_wrapped_alertdialog_with_chips() {
        rule.setContentWithTheme {
            Box {
                var showDialog by remember { mutableStateOf(true) }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Label")
                }
                Dialog(
                    showDialog = showDialog,
                    onDismissRequest = { showDialog = false },
                ) {
                    Alert(
                        icon = {},
                        title = {},
                        message = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                        content = {},
                    )
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun supports_swipetodismiss_on_wrapped_confirmationdialog() {
        rule.setContentWithTheme {
            Box {
                var showDialog by remember { mutableStateOf(true) }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Label")
                }
                Dialog(
                    showDialog = showDialog,
                    onDismissRequest = { showDialog = false },
                ) {
                    Confirmation(
                        onTimeout = { showDialog = false },
                        icon = {},
                        content = { Text("Dialog", modifier = Modifier.testTag(TEST_TAG)) },
                    )
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun shows_dialog_when_showdialog_equals_true() {
        rule.setContentWithTheme {
            Box {
                var showDialog by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Chip(onClick = { showDialog = true }, label = { Text("Show") })
                }
                Dialog(
                    showDialog = showDialog,
                    onDismissRequest = { showDialog = false },
                ) {
                    Text("Text", modifier = Modifier.testTag(TEST_TAG))
                }
            }
        }

        rule.onNode(hasClickAction()).performClick()
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun calls_ondismissrequest_when_dialog_is_swiped() {
        val dismissedText = "Dismissed"
        rule.setContentWithTheme {
            Box {
                var dismissed by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (dismissed) dismissedText else "Label")
                }
                Dialog(
                    showDialog = !dismissed,
                    onDismissRequest = { dismissed = true },
                ) {
                    Alert(
                        icon = {},
                        title = {},
                        message = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                        content = {},
                    )
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.onNodeWithText(dismissedText).assertExists()
    }
}

class DialogContentSizeAndPositionTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun spaces_icon_and_title_correctly_on_alert_with_buttons() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            Alert(
                icon = { TestImage(ICON_TAG) },
                title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                negativeButton = {
                    Button(onClick = {}, modifier = Modifier.testTag(BUTTON_TAG)) {}
                },
                positiveButton = { Button(onClick = {}) {} },
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        val iconBottom = rule.onNodeWithTag(ICON_TAG).getUnclippedBoundsInRoot().bottom
        val titleTop = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().top
        titleTop.assertIsEqualTo(iconBottom + DialogDefaults.IconSpacing)
    }

    @Test
    fun spaces_title_and_buttons_correctly_on_alert_with_buttons() {
        var titlePadding = 0.dp

        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            titlePadding = DialogDefaults.TitlePadding.calculateBottomPadding()
            Alert(
                icon = { TestImage(ICON_TAG) },
                title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                negativeButton = {
                    Button(onClick = {}, modifier = Modifier.testTag(BUTTON_TAG)) {}
                },
                positiveButton = { Button(onClick = {}) {} },
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        val titleBottom = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().bottom
        val buttonTop = rule.onNodeWithTag(BUTTON_TAG).getUnclippedBoundsInRoot().top
        buttonTop.assertIsEqualTo(titleBottom + titlePadding)
    }

    @Test
    fun spaces_icon_and_title_correctly_on_alert_with_chips() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            Alert(
                icon = { TestImage(ICON_TAG) },
                title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                content = {
                    item {
                        Chip(
                            label = { Text("Chip") },
                            onClick = {},
                            modifier = Modifier.testTag(CHIP_TAG)
                        )
                    }
                },
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        val iconBottom = rule.onNodeWithTag(ICON_TAG).getUnclippedBoundsInRoot().bottom
        val titleTop = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().top
        titleTop.assertIsEqualTo(iconBottom + DialogDefaults.IconSpacing)
    }

    @Test
    fun spaces_title_and_chips_correctly_on_alert_with_chips() {
        var titlePadding = 0.dp

        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            titlePadding = DialogDefaults.TitlePadding.calculateBottomPadding()
            Alert(
                icon = { TestImage(ICON_TAG) },
                title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                content = {
                    item {
                        Chip(
                            label = { Text("Chip") },
                            onClick = {},
                            modifier = Modifier.testTag(CHIP_TAG)
                        )
                    }
                },
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        val titleBottom = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().bottom
        val chipTop = rule.onNodeWithTag(CHIP_TAG).getUnclippedBoundsInRoot().top
        chipTop.assertIsEqualTo(titleBottom + titlePadding)
    }

    @Test
    fun spaces_icon_and_title_correctly_on_confirmation() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            Confirmation(
                onTimeout = {},
                icon = { TestImage(ICON_TAG) },
                content = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        val iconBottom = rule.onNodeWithTag(ICON_TAG).getUnclippedBoundsInRoot().bottom
        val titleTop = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().top
        titleTop.assertIsEqualTo(iconBottom + DialogDefaults.IconSpacing)
    }

    @Test
    fun spaces_title_and_body_correctly_on_alert_with_buttons() {
        var titleSpacing = 0.dp

        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            titleSpacing = DialogDefaults.TitlePadding.calculateBottomPadding()
            Alert(
                title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                negativeButton = {
                    Button(onClick = {}, modifier = Modifier.testTag(BUTTON_TAG)) {}
                },
                positiveButton = { Button(onClick = {}) {} },
                content = { Text("Body", modifier = Modifier.testTag(BODY_TAG)) },
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        val titleBottom = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().bottom
        val bodyTop = rule.onNodeWithTag(BODY_TAG).getUnclippedBoundsInRoot().top
        bodyTop.assertIsEqualTo(titleBottom + titleSpacing)
    }

    @Test
    fun spaces_title_and_body_correctly_on_alert_with_chips() {
        var titleSpacing = 0.dp

        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            titleSpacing = DialogDefaults.TitlePadding.calculateBottomPadding()
            Alert(
                icon = { TestImage(ICON_TAG) },
                title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                message = { Text("Message", modifier = Modifier.testTag(BODY_TAG)) },
                content = {
                    item {
                        Chip(
                            label = { Text("Chip") },
                            onClick = {},
                            modifier = Modifier.testTag(CHIP_TAG)
                        )
                    }
                },
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        val titleBottom = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().bottom
        val bodyTop = rule.onNodeWithTag(BODY_TAG).getUnclippedBoundsInRoot().top
        bodyTop.assertIsEqualTo(titleBottom + titleSpacing)
    }

    @Test
    fun spaces_body_and_buttons_correctly_on_alert_with_buttons() {
        var bodyPadding = 0.dp

        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            bodyPadding = DialogDefaults.BodyPadding.calculateBottomPadding()
            Alert(
                icon = {},
                title = {},
                negativeButton = {
                    Button(onClick = {}, modifier = Modifier.testTag(BUTTON_TAG)) {}
                },
                positiveButton = { Button(onClick = {}) {} },
                content = { Text("Body", modifier = Modifier.testTag(BODY_TAG)) },
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        val bodyBottom = rule.onNodeWithTag(BODY_TAG).getUnclippedBoundsInRoot().bottom
        val buttonTop = rule.onNodeWithTag(BUTTON_TAG).getUnclippedBoundsInRoot().top
        buttonTop.assertIsEqualTo(bodyBottom + bodyPadding)
    }

    @Test
    fun spaces_body_and_chips_correctly_on_alert_with_chips() {
        var bodyPadding = 0.dp

        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            bodyPadding = DialogDefaults.BodyPadding.calculateBottomPadding()
            Alert(
                icon = { TestImage(ICON_TAG) },
                title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                message = { Text("Message", modifier = Modifier.testTag(BODY_TAG)) },
                content = {
                    item {
                        Chip(
                            label = { Text("Chip") },
                            onClick = {},
                            modifier = Modifier.testTag(CHIP_TAG)
                        )
                    }
                },
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        val bodyBottom = rule.onNodeWithTag(BODY_TAG).getUnclippedBoundsInRoot().bottom
        val chipTop = rule.onNodeWithTag(CHIP_TAG).getUnclippedBoundsInRoot().top
        chipTop.assertIsEqualTo(bodyBottom + bodyPadding)
    }
}

class DialogContentColorTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun gives_icon_onbackground_on_alert_for_buttons() {
        var expectedColor = Color.Transparent
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            expectedColor = MaterialTheme.colors.onBackground
            Alert(
                icon = { actualColor = LocalContentColor.current },
                title = {},
                negativeButton = {},
                positiveButton = {},
                content = {},
            )
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun gives_icon_onbackground_on_alert_for_chips() {
        var expectedColor = Color.Transparent
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            expectedColor = MaterialTheme.colors.onBackground
            Alert(
                icon = { actualColor = LocalContentColor.current },
                title = {},
                message = {},
                content = {},
            )
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun gives_icon_onbackground_on_confirmation() {
        var expectedColor = Color.Transparent
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            expectedColor = MaterialTheme.colors.onBackground
            Confirmation(
                onTimeout = {},
                icon = { actualColor = LocalContentColor.current },
                content = {},
            )
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun gives_custom_icon_on_alert_for_buttons() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            Alert(
                iconColor = overrideColor,
                icon = { actualColor = LocalContentColor.current },
                title = {},
                negativeButton = {},
                positiveButton = {},
                content = {},
            )
        }

        assertEquals(overrideColor, actualColor)
    }

    @Test
    fun gives_custom_icon_on_alert_for_chips() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            Alert(
                iconColor = overrideColor,
                icon = { actualColor = LocalContentColor.current },
                title = {},
                message = {},
                content = {},
            )
        }

        assertEquals(overrideColor, actualColor)
    }

    @Test
    fun gives_custom_icon_on_confirmation() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            Confirmation(
                onTimeout = {},
                iconColor = overrideColor,
                icon = { actualColor = LocalContentColor.current },
                content = {},
            )
        }

        assertEquals(overrideColor, actualColor)
    }

    @Test
    fun gives_title_onbackground_on_alert_for_buttons() {
        var expectedColor = Color.Transparent
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            expectedColor = MaterialTheme.colors.onBackground
            Alert(
                title = { actualColor = LocalContentColor.current },
                negativeButton = {},
                positiveButton = {},
                content = {},
            )
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun gives_title_onbackground_on_alert_for_chips() {
        var expectedColor = Color.Transparent
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            expectedColor = MaterialTheme.colors.onBackground
            Alert(
                title = { actualColor = LocalContentColor.current },
                message = {},
                content = {},
            )
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun gives_title_onbackground_on_confirmation() {
        var expectedColor = Color.Transparent
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            expectedColor = MaterialTheme.colors.onBackground
            Confirmation(
                onTimeout = {},
                content = { actualColor = LocalContentColor.current },
            )
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun gives_custom_title_on_alert_for_buttons() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            Alert(
                titleColor = overrideColor,
                title = { actualColor = LocalContentColor.current },
                negativeButton = {},
                positiveButton = {},
                content = {},
            )
        }

        assertEquals(overrideColor, actualColor)
    }

    @Test
    fun gives_custom_title_on_alert_for_chips() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            Alert(
                titleColor = overrideColor,
                title = { actualColor = LocalContentColor.current },
                message = {},
                content = {},
            )
        }

        assertEquals(overrideColor, actualColor)
    }

    @Test
    fun gives_custom_title_on_confirmation() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            Confirmation(
                onTimeout = {},
                contentColor = overrideColor,
                content = { actualColor = LocalContentColor.current },
            )
        }

        assertEquals(overrideColor, actualColor)
    }

    @Test
    fun gives_bodymessage_onbackground_on_alert_for_buttons() {
        var expectedContentColor = Color.Transparent
        var actualContentColor = Color.Transparent

        rule.setContentWithTheme {
            expectedContentColor = MaterialTheme.colors.onBackground
            Alert(
                title = {},
                negativeButton = {},
                positiveButton = {},
                content = { actualContentColor = LocalContentColor.current },
            )
        }

        assertEquals(expectedContentColor, actualContentColor)
    }

    @Test
    fun gives_bodymessage_onbackground_on_alert_for_chips() {
        var expectedContentColor = Color.Transparent
        var actualContentColor = Color.Transparent

        rule.setContentWithTheme {
            expectedContentColor = MaterialTheme.colors.onBackground
            Alert(
                title = {},
                message = { actualContentColor = LocalContentColor.current },
                content = {},
            )
        }

        assertEquals(expectedContentColor, actualContentColor)
    }

    @Test
    fun gives_custom_bodymessage_on_alert_for_buttons() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            Alert(
                title = {},
                negativeButton = {},
                positiveButton = {},
                contentColor = overrideColor,
                content = { actualColor = LocalContentColor.current },
            )
        }

        assertEquals(overrideColor, actualColor)
    }

    @Test
    fun gives_custom_bodymessage_on_alert_for_chips() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            Alert(
                title = {},
                messageColor = overrideColor,
                message = { actualColor = LocalContentColor.current },
                content = {},
            )
        }

        assertEquals(overrideColor, actualColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_correct_background_color_on_alert_for_buttons() {
        verifyBackgroundColor(expected = { MaterialTheme.colors.background }) {
            Alert(
                title = {},
                negativeButton = {},
                positiveButton = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_correct_background_color_on_alert_for_chips() {
        verifyBackgroundColor(expected = { MaterialTheme.colors.background }) {
            Alert(title = {}, message = {}, content = {}, modifier = Modifier.testTag(TEST_TAG))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_correct_background_color_on_confirmation() {
        verifyBackgroundColor(expected = { MaterialTheme.colors.background }) {
            Confirmation(
                onTimeout = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_custom_background_color_on_alert_for_buttons() {
        val overrideColor = Color.Yellow

        rule.setContentWithTheme {
            Alert(
                title = {},
                negativeButton = {},
                positiveButton = {},
                content = {},
                backgroundColor = overrideColor,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(overrideColor, 100.0f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_custom_background_color_on_alert_for_chips() {
        val overrideColor = Color.Yellow

        rule.setContentWithTheme {
            Alert(
                title = {},
                message = {},
                content = {},
                backgroundColor = overrideColor,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(overrideColor, 100.0f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_custom_background_color_on_confirmation() {
        val overrideColor = Color.Yellow

        rule.setContentWithTheme {
            Confirmation(
                onTimeout = {},
                content = {},
                backgroundColor = overrideColor,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(overrideColor, 100.0f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    private fun verifyBackgroundColor(
        expected: @Composable () -> Color,
        content: @Composable () -> Unit
    ) {
        val testBackground = Color.White
        var expectedBackground = Color.Transparent

        rule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize().background(testBackground)) {
                expectedBackground = expected()
                content()
            }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedBackground, 100.0f)
    }
}

class DialogTextStyleTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun gives_title_correct_textstyle_on_alert_for_buttons() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.title3
            Alert(
                title = { actualTextStyle = LocalTextStyle.current },
                negativeButton = {},
                positiveButton = {},
            )
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun gives_title_correct_textstyle_on_alert_for_chips() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.title3
            Alert(
                title = { actualTextStyle = LocalTextStyle.current },
                message = {},
                content = {},
            )
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun gives_body_correct_textstyle_on_alert_for_buttons() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.body2
            Alert(
                title = { Text("Title") },
                negativeButton = {},
                positiveButton = {},
                content = { actualTextStyle = LocalTextStyle.current }
            )
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun gives_body_correct_textstyle_on_alert_for_chips() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.body2
            Alert(
                title = { Text("Title") },
                message = { actualTextStyle = LocalTextStyle.current },
                content = {},
            )
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun gives_title_correct_textstyle_on_confirmation() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.title3
            Confirmation(
                onTimeout = {},
                content = { actualTextStyle = LocalTextStyle.current },
            )
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }
}
