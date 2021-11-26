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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.width
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

internal const val ICON_TAG = "icon"
internal const val TITLE_TAG = "Title"
internal const val BODY_TAG = "Body"
internal const val BUTTON_TAG = "Button"
internal const val CHIP_TAG = "Chip"

class DialogBehaviourTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag_on_alertdialog_with_buttons() {
        rule.setContentWithTheme {
            AlertDialog(
                title = {},
                negativeButton = {},
                positiveButton = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun supports_testtag_on_alertdialog_with_chips() {
        rule.setContentWithTheme {
            AlertDialog(
                title = {},
                message = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun supports_testtag_on_confirmationdialog() {
        rule.setContentWithTheme {
            ConfirmationDialog(
                onTimeout = {},
                icon = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_icon_on_alertdialog_with_buttons() {
        rule.setContentWithTheme {
            AlertDialog(
                icon = { TestImage(TEST_TAG) },
                title = {},
                negativeButton = {},
                positiveButton = {},
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_icon_on_alertdialog_with_chips() {
        rule.setContentWithTheme {
            AlertDialog(
                icon = { TestImage(TEST_TAG) },
                title = {},
                message = {},
                content = {},
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_icon_on_confirmationdialog() {
        rule.setContentWithTheme {
            ConfirmationDialog(
                onTimeout = {},
                icon = { TestImage(TEST_TAG) },
                content = {},
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_title_on_alertdialog_with_buttons() {
        rule.setContentWithTheme {
            AlertDialog(
                title = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                negativeButton = {},
                positiveButton = {},
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_title_on_alertdialog_with_chips() {
        rule.setContentWithTheme {
            AlertDialog(
                icon = {},
                title = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                message = {},
                content = {},
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_title_on_confirmationdialog() {
        rule.setContentWithTheme {
            ConfirmationDialog(
                onTimeout = {},
                icon = {},
                content = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_bodymessage_on_alertdialog_with_buttons() {
        rule.setContentWithTheme {
            AlertDialog(
                title = {},
                negativeButton = {},
                positiveButton = {},
                content = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_bodymessage_on_alertdialog_with_chips() {
        rule.setContentWithTheme {
            AlertDialog(
                icon = {},
                title = {},
                message = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                content = {},
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_buttons_on_alertdialog_with_buttons() {
        val buttonTag1 = "Button1"
        val buttonTag2 = "Button2"

        rule.setContentWithTheme {
            AlertDialog(
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
}

class DialogSizeAndPositionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun centers_icon_correctly_on_alertdialog_with_buttons() {
        var bottomPadding = 0.dp
        var topPadding = 0.dp
        var titleSpacing = 0.dp

        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                topPadding = DialogDefaults.ButtonsContentPadding.calculateTopPadding()
                titleSpacing = DialogDefaults.TitlePadding.calculateBottomPadding()
                bottomPadding = DialogDefaults.ButtonsContentPadding.calculateBottomPadding()
                AlertDialog(
                    icon = { TestImage(ICON_TAG) },
                    title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                    negativeButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(BUTTON_TAG)) {}
                    },
                    positiveButton = { Button(onClick = {}) {} },
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }

        val dialogHeight = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().height
        val buttonHeight = rule.onNodeWithTag(BUTTON_TAG).getUnclippedBoundsInRoot().height
        val titleHeight = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().height
        val iconHeight = rule.onNodeWithTag(ICON_TAG).getUnclippedBoundsInRoot().height
        val centering =
            (dialogHeight - bottomPadding - buttonHeight - topPadding - titleSpacing -
                titleHeight - DialogDefaults.IconSpacing - iconHeight) / 2
        rule.onNodeWithTag(ICON_TAG)
            .assertTopPositionInRootIsEqualTo(topPadding + centering)
    }

    @Test
    fun centers_icon_correctly_on_alertdialog_with_chips() {
        var bottomPadding = 0.dp
        var topPadding = 0.dp
        var titleSpacing = 0.dp

        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                topPadding = DialogDefaults.ChipsContentPadding.calculateTopPadding()
                titleSpacing = DialogDefaults.TitlePadding.calculateBottomPadding()
                bottomPadding = DialogDefaults.ChipsContentPadding.calculateBottomPadding()
                AlertDialog(
                    icon = { TestImage(ICON_TAG) },
                    title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                    content = {
                        Chip(
                            label = { Text("Chip") },
                            onClick = {},
                            modifier = Modifier.testTag(CHIP_TAG)
                        )
                    },
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        val dialogHeight = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().height
        val chipHeight = rule.onNodeWithTag(CHIP_TAG).getUnclippedBoundsInRoot().height
        val titleHeight = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().height
        val iconHeight = rule.onNodeWithTag(ICON_TAG).getUnclippedBoundsInRoot().height
        val centering =
            (dialogHeight - bottomPadding - chipHeight - topPadding - titleSpacing -
                titleHeight - DialogDefaults.IconSpacing - iconHeight) / 2

        rule.onNodeWithContentDescription(ICON_TAG, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(topPadding + centering)
    }

    @Test
    fun centers_icon_correctly_on_confirmationdialog() {
        var bottomPadding = 0.dp
        var topPadding = 0.dp
        var titleSpacing = 0.dp

        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                topPadding = DialogDefaults.ConfirmationContentPadding.calculateTopPadding()
                titleSpacing = DialogDefaults.TitleBottomPadding.calculateBottomPadding()
                bottomPadding = DialogDefaults.ConfirmationContentPadding.calculateBottomPadding()
                ConfirmationDialog(
                    onTimeout = {},
                    icon = { TestImage(ICON_TAG) },
                    content = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }

        val dialogHeight = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().height
        val titleHeight = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().height
        val iconHeight = rule.onNodeWithTag(ICON_TAG).getUnclippedBoundsInRoot().height
        val centering =
            (dialogHeight - bottomPadding - topPadding - titleSpacing -
                titleHeight - DialogDefaults.IconSpacing - iconHeight) / 2
        rule.onNodeWithTag(ICON_TAG)
            .assertTopPositionInRootIsEqualTo(topPadding + centering)
    }

    @Test
    fun centers_title_correctly_on_alertdialog_with_buttons() {
        var bottomPadding = 0.dp
        var topPadding = 0.dp
        var titleSpacing = 0.dp

        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                bottomPadding = DialogDefaults.ButtonsContentPadding.calculateBottomPadding()
                topPadding = DialogDefaults.ButtonsContentPadding.calculateTopPadding()
                titleSpacing = DialogDefaults.TitlePadding.calculateBottomPadding()
                AlertDialog(
                    title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                    negativeButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(BUTTON_TAG)) {}
                    },
                    positiveButton = { Button(onClick = {}) {} },
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        val dialogHeight = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().height
        val buttonHeight = rule.onNodeWithTag(BUTTON_TAG).getUnclippedBoundsInRoot().height
        val titleHeight = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().height
        val centering =
            (dialogHeight - bottomPadding - buttonHeight - topPadding - titleSpacing -
                titleHeight) / 2

        rule.onNodeWithTag(TITLE_TAG)
            .assertTopPositionInRootIsEqualTo(topPadding + centering)
    }

    @Test
    fun centers_title_correctly_on_alertdialog_with_chips() {
        var bottomPadding = 0.dp
        var topPadding = 0.dp
        var titleSpacing = 0.dp

        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                bottomPadding = DialogDefaults.ChipsContentPadding.calculateBottomPadding()
                topPadding = DialogDefaults.ChipsContentPadding.calculateTopPadding()
                titleSpacing = DialogDefaults.TitlePadding.calculateBottomPadding()
                AlertDialog(
                    icon = { TestImage(ICON_TAG) },
                    title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                    content = {
                        Chip(
                            label = { Text("Chip") },
                            onClick = {},
                            modifier = Modifier.testTag(CHIP_TAG)
                        )
                    },
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        val iconHeight = rule.onNodeWithTag(ICON_TAG).getUnclippedBoundsInRoot().height
        val dialogHeight = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().height
        val chipHeight = rule.onNodeWithTag(CHIP_TAG).getUnclippedBoundsInRoot().height
        val titleHeight = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().height
        val centering = max(
            0.dp,
            (dialogHeight - topPadding - iconHeight - DialogDefaults.IconSpacing -
                titleHeight - titleSpacing - chipHeight - bottomPadding) / 2)
        rule.onNodeWithTag(TITLE_TAG)
            .assertTopPositionInRootIsEqualTo(
                topPadding + iconHeight + DialogDefaults.IconSpacing + centering)
    }

    @Test
    fun centers_title_correctly_on_confirmationdialog() {
        var bottomPadding = 0.dp
        var topPadding = 0.dp
        var titleSpacing = 0.dp

        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                topPadding = DialogDefaults.ConfirmationContentPadding.calculateTopPadding()
                titleSpacing = DialogDefaults.TitleBottomPadding.calculateBottomPadding()
                bottomPadding = DialogDefaults.ConfirmationContentPadding.calculateBottomPadding()
                ConfirmationDialog(
                    onTimeout = {},
                    icon = { TestImage(ICON_TAG) },
                    content = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }

        val dialogHeight = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().height
        val titleHeight = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().height
        val iconHeight = rule.onNodeWithTag(ICON_TAG).getUnclippedBoundsInRoot().height
        val centering =
            (dialogHeight - bottomPadding - topPadding - titleSpacing -
                titleHeight - DialogDefaults.IconSpacing - iconHeight) / 2
        rule.onNodeWithTag(TITLE_TAG)
            .assertTopPositionInRootIsEqualTo(
                topPadding + centering + iconHeight + DialogDefaults.IconSpacing)
    }

    @Test
    fun centers_bodymessage_correctly_on_alertdialog_with_buttons() {
        var bottomPadding = 0.dp
        var topPadding = 0.dp
        var titleSpacing = 0.dp
        var bodySpacing = 0.dp

        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                bottomPadding = DialogDefaults.ButtonsContentPadding.calculateBottomPadding()
                topPadding = DialogDefaults.ButtonsContentPadding.calculateTopPadding()
                titleSpacing =
                    DialogDefaults.TitlePadding.calculateBottomPadding() +
                    DialogDefaults.BodyPadding.calculateTopPadding()
                bodySpacing = DialogDefaults.BodyPadding.calculateBottomPadding()
                AlertDialog(
                    title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                    negativeButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(BUTTON_TAG)) {}
                    },
                    positiveButton = { Button(onClick = {}) {} },
                    content = { Text("Body", modifier = Modifier.testTag(BODY_TAG)) },
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        val dialogHeight = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().height
        val buttonHeight = rule.onNodeWithTag(BUTTON_TAG).getUnclippedBoundsInRoot().height
        val titleHeight = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().height
        val bodyHeight = rule.onNodeWithTag(BODY_TAG).getUnclippedBoundsInRoot().height
        val centering =
            (dialogHeight - bottomPadding - buttonHeight - topPadding - titleSpacing -
                titleHeight - bodySpacing - bodyHeight) / 2

        rule.onNodeWithTag(BODY_TAG)
            .assertTopPositionInRootIsEqualTo(topPadding + titleHeight + titleSpacing + centering)
    }

    @Test
    fun centers_bodymessage_correctly_on_alertdialog_with_chips() {
        var bottomPadding = 0.dp
        var topPadding = 0.dp
        var titleSpacing = 0.dp
        var bodySpacing = 0.dp

        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                bottomPadding = DialogDefaults.ChipsContentPadding.calculateBottomPadding()
                topPadding = DialogDefaults.ChipsContentPadding.calculateTopPadding()
                titleSpacing =
                    DialogDefaults.TitlePadding.calculateBottomPadding() +
                        DialogDefaults.BodyPadding.calculateTopPadding()
                bodySpacing = DialogDefaults.BodyPadding.calculateBottomPadding()
                AlertDialog(
                    icon = { TestImage(ICON_TAG) },
                    title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                    message = { Text("Message", modifier = Modifier.testTag(BODY_TAG)) },
                    content = {
                        Chip(
                            label = { Text("Chip") },
                            onClick = {},
                            modifier = Modifier.testTag(CHIP_TAG)
                        )
                    },
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        val iconHeight = rule.onNodeWithTag(ICON_TAG).getUnclippedBoundsInRoot().height
        val dialogHeight = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().height
        val chipHeight = rule.onNodeWithTag(CHIP_TAG).getUnclippedBoundsInRoot().height
        val titleHeight = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().height
        val bodyHeight = rule.onNodeWithTag(BODY_TAG).getUnclippedBoundsInRoot().height
        val centering = max(
            0.dp,
            (dialogHeight - bottomPadding - chipHeight - topPadding - iconHeight -
                DialogDefaults.IconSpacing - titleHeight - titleSpacing - bodySpacing -
                bodyHeight) / 2
        )

        rule.onNodeWithTag(BODY_TAG)
            .assertTopPositionInRootIsEqualTo(topPadding + centering + iconHeight +
                DialogDefaults.IconSpacing + titleHeight + titleSpacing)
    }

    @Test
    fun positions_buttons_correctly_on_alertdialog_with_buttons() {
        val buttonTag1 = "Button1"
        val buttonTag2 = "Button2"
        var bottomPadding = 0.dp
        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                bottomPadding = DialogDefaults.ButtonsContentPadding.calculateBottomPadding()
                AlertDialog(
                    icon = {},
                    title = {},
                    negativeButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(buttonTag1)) {}
                    },
                    positiveButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(buttonTag2)) {}
                    },
                    content = {},
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        val dialogBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()
        val button1 = rule.onNodeWithTag(buttonTag1).getUnclippedBoundsInRoot()
        val button2 = rule.onNodeWithTag(buttonTag2).getUnclippedBoundsInRoot()
        rule.onNodeWithTag(buttonTag1, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(dialogBounds.height - button1.height - bottomPadding)
        rule.onNodeWithTag(buttonTag2, useUnmergedTree = true)
            .assertTopPositionInRootIsEqualTo(dialogBounds.height - button2.height - bottomPadding)
        rule.onNodeWithTag(buttonTag1, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(
                (dialogBounds.width - DialogDefaults.ButtonSpacing) / 2 - button1.width)
        rule.onNodeWithTag(buttonTag2, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo(
                (dialogBounds.width + DialogDefaults.ButtonSpacing) / 2)
    }

    @Test
    fun positions_chip_correctly_on_alertdialog_with_chips() {
        var bottomPadding = 0.dp
        var topPadding = 0.dp
        var titleSpacing = 0.dp
        var bodySpacing = 0.dp

        rule
            .setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
                bottomPadding = DialogDefaults.ChipsContentPadding.calculateBottomPadding()
                topPadding = DialogDefaults.ChipsContentPadding.calculateTopPadding()
                titleSpacing =
                    DialogDefaults.TitlePadding.calculateBottomPadding() +
                        DialogDefaults.BodyPadding.calculateTopPadding()
                bodySpacing = DialogDefaults.BodyPadding.calculateBottomPadding()
                AlertDialog(
                    icon = { TestImage(ICON_TAG) },
                    title = { Text("Title", modifier = Modifier.testTag(TITLE_TAG)) },
                    message = { Text("Message", modifier = Modifier.testTag(BODY_TAG)) },
                    content = {
                        Chip(
                            label = { Text("Chip") },
                            onClick = {},
                            modifier = Modifier.testTag(CHIP_TAG)
                        )
                    },
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        val iconHeight = rule.onNodeWithTag(ICON_TAG).getUnclippedBoundsInRoot().height
        val dialogHeight = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().height
        val chipHeight = rule.onNodeWithTag(CHIP_TAG).getUnclippedBoundsInRoot().height
        val titleHeight = rule.onNodeWithTag(TITLE_TAG).getUnclippedBoundsInRoot().height
        val bodyHeight = rule.onNodeWithTag(BODY_TAG).getUnclippedBoundsInRoot().height
        val centering = max(
            0.dp,
            (dialogHeight - bottomPadding - chipHeight - topPadding - iconHeight -
                DialogDefaults.IconSpacing - titleHeight - titleSpacing - bodySpacing -
                bodyHeight) / 2
        )
        val chipTop = max(
            dialogHeight - bottomPadding - chipHeight,
            topPadding + centering + iconHeight + DialogDefaults.IconSpacing + titleHeight +
                titleSpacing + bodyHeight + bodySpacing + centering
        )

        rule.onNodeWithTag(CHIP_TAG).assertTopPositionInRootIsEqualTo(chipTop)
    }
}

class DialogColorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun gives_icon_onbackground_on_alertdialog_for_buttons() {
        var expectedColor = Color.Transparent
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            expectedColor = MaterialTheme.colors.onBackground
            AlertDialog(
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
    fun gives_icon_onbackground_on_alertdialog_for_chips() {
        var expectedColor = Color.Transparent
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            expectedColor = MaterialTheme.colors.onBackground
            AlertDialog(
                icon = { actualColor = LocalContentColor.current },
                title = {},
                message = {},
                content = {},
            )
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun gives_icon_onbackground_on_confirmationdialog() {
        var expectedColor = Color.Transparent
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            expectedColor = MaterialTheme.colors.onBackground
            ConfirmationDialog(
                onTimeout = {},
                icon = { actualColor = LocalContentColor.current },
                content = {},
            )
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun gives_custom_icon_on_alertdialog_for_buttons() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            AlertDialog(
                iconTintColor = overrideColor,
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
    fun gives_custom_icon_on_alertdialog_for_chips() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            AlertDialog(
                iconTintColor = overrideColor,
                icon = { actualColor = LocalContentColor.current },
                title = {},
                message = {},
                content = {},
            )
        }

        assertEquals(overrideColor, actualColor)
    }

    @Test
    fun gives_custom_icon_on_confirmationdialog() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            ConfirmationDialog(
                onTimeout = {},
                iconTintColor = overrideColor,
                icon = { actualColor = LocalContentColor.current },
                content = {},
            )
        }

        assertEquals(overrideColor, actualColor)
    }

    @Test
    fun gives_title_onbackground_on_alertdialog_for_buttons() {
        var expectedColor = Color.Transparent
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            expectedColor = MaterialTheme.colors.onBackground
            AlertDialog(
                title = { actualColor = LocalContentColor.current },
                negativeButton = {},
                positiveButton = {},
                content = {},
            )
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun gives_title_onbackground_on_alertdialog_for_chips() {
        var expectedColor = Color.Transparent
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            expectedColor = MaterialTheme.colors.onBackground
            AlertDialog(
                title = { actualColor = LocalContentColor.current },
                message = {},
                content = {},
            )
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun gives_title_onbackground_on_confirmationdialog() {
        var expectedColor = Color.Transparent
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            expectedColor = MaterialTheme.colors.onBackground
            ConfirmationDialog(
                onTimeout = {},
                content = { actualColor = LocalContentColor.current },
            )
        }

        assertEquals(expectedColor, actualColor)
    }

    @Test
    fun gives_custom_title_on_alertdialog_for_buttons() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            AlertDialog(
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
    fun gives_custom_title_on_alertdialog_for_chips() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            AlertDialog(
                titleColor = overrideColor,
                title = { actualColor = LocalContentColor.current },
                message = {},
                content = {},
            )
        }

        assertEquals(overrideColor, actualColor)
    }

    @Test
    fun gives_custom_title_on_confirmationdialog() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            ConfirmationDialog(
                onTimeout = {},
                contentColor = overrideColor,
                content = { actualColor = LocalContentColor.current },
            )
        }

        assertEquals(overrideColor, actualColor)
    }

    @Test
    fun gives_bodymessage_onbackground_on_alertdialog_for_buttons() {
        var expectedContentColor = Color.Transparent
        var actualContentColor = Color.Transparent

        rule.setContentWithTheme {
            expectedContentColor = MaterialTheme.colors.onBackground
            AlertDialog(
                title = {},
                negativeButton = {},
                positiveButton = {},
                content = { actualContentColor = LocalContentColor.current },
            )
        }

        assertEquals(expectedContentColor, actualContentColor)
    }

    @Test
    fun gives_bodymessage_onbackground_on_alertdialog_for_chips() {
        var expectedContentColor = Color.Transparent
        var actualContentColor = Color.Transparent

        rule.setContentWithTheme {
            expectedContentColor = MaterialTheme.colors.onBackground
            AlertDialog(
                title = {},
                message = { actualContentColor = LocalContentColor.current },
                content = {},
            )
        }

        assertEquals(expectedContentColor, actualContentColor)
    }

    @Test
    fun gives_custom_bodymessage_on_alertdialog_for_buttons() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            AlertDialog(
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
    fun gives_custom_bodymessage_on_alertdialog_for_chips() {
        val overrideColor = Color.Yellow
        var actualColor = Color.Transparent

        rule.setContentWithTheme {
            AlertDialog(
                title = {},
                messageColor = overrideColor,
                message = { actualColor = LocalContentColor.current },
                content = {},
            )
        }

        assertEquals(overrideColor, actualColor)
    }

    @Test
    fun gives_correct_background_color_on_alertdialog_for_buttons() {
        verifyBackgroundColor(expected = { MaterialTheme.colors.background }) {
            AlertDialog(
                title = {},
                negativeButton = {},
                positiveButton = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
    }

    @Test
    fun gives_correct_background_color_on_alertdialog_for_chips() {
        verifyBackgroundColor(expected = { MaterialTheme.colors.background }) {
            AlertDialog(
                title = {},
                message = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG)
            )
        }
    }

    @Test
    fun gives_correct_background_color_on_confirmationdialog() {
        verifyBackgroundColor(expected = { MaterialTheme.colors.background }) {
            ConfirmationDialog(
                onTimeout = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_custom_background_color_on_alertdialog_for_buttons() {
        val overrideColor = Color.Yellow

        rule.setContentWithTheme {
            AlertDialog(
                title = {},
                negativeButton = {},
                positiveButton = {},
                content = {},
                backgroundColor = overrideColor,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(overrideColor, 100.0f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_custom_background_color_on_alertdialog_for_chips() {
        val overrideColor = Color.Yellow

        rule.setContentWithTheme {
            AlertDialog(
                title = {},
                message = {},
                content = {},
                backgroundColor = overrideColor,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(overrideColor, 100.0f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_custom_background_color_on_confirmationDialog() {
        val overrideColor = Color.Yellow

        rule.setContentWithTheme {
            ConfirmationDialog(
                onTimeout = {},
                content = {},
                backgroundColor = overrideColor,
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(overrideColor, 100.0f)
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

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedBackground, 100.0f)
    }
}

class DialogTextStyleTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun gives_title_correct_textstyle_on_alertdialog_for_buttons() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.title3
            AlertDialog(
                title = { actualTextStyle = LocalTextStyle.current },
                negativeButton = {},
                positiveButton = {},
            )
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun gives_title_correct_textstyle_on_alertdialog_for_chips() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.title3
            AlertDialog(
                title = { actualTextStyle = LocalTextStyle.current },
                message = {},
                content = {},
            )
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun gives_body_correct_textstyle_on_alertdialog_for_buttons() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.body2
            AlertDialog(
                title = { Text("Title") },
                negativeButton = {},
                positiveButton = {},
                content = { actualTextStyle = LocalTextStyle.current }
            )
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun gives_body_correct_textstyle_on_alertdialog_for_chips() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.body2
            AlertDialog(
                title = { Text("Title") },
                message = { actualTextStyle = LocalTextStyle.current },
                content = {},
            )
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }

    @Test
    fun gives_title_correct_textstyle_on_confirmationdialog() {
        var actualTextStyle = TextStyle.Default
        var expectedTextStyle = TextStyle.Default

        rule.setContentWithTheme {
            expectedTextStyle = MaterialTheme.typography.title3
            ConfirmationDialog(
                onTimeout = {},
                content = { actualTextStyle = LocalTextStyle.current },
            )
        }

        assertEquals(expectedTextStyle, actualTextStyle)
    }
}
