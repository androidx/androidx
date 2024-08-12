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
package androidx.compose.material3

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class SplitButtonTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun basicSplitButton_contentDisplay() {
        rule.setMaterialContent(lightColorScheme()) {
            SplitButton(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leadingButton")
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Leading Icon",
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("My Button")
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        modifier = Modifier.size(34.dp).testTag("trailingButton"),
                        onClick = {},
                        shape = SplitButtonDefaults.trailingButtonShape(),
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                }
            )
        }

        rule.onNodeWithText("My Button").assertIsDisplayed()
        rule.onNodeWithTag("leadingButton").assertHasClickAction()
        rule.onNodeWithTag("trailingButton").assertHasClickAction()
        rule.onNodeWithContentDescription("Leading Icon").assertIsDisplayed()
        rule.onNodeWithContentDescription("Trailing Icon").assertIsDisplayed()
    }

    @Test
    fun basicSplitButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            SplitButton(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leading button")
                    ) {
                        Text("My Button")
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        modifier = Modifier.size(34.dp).testTag("trailing button"),
                        onClick = {},
                        shape = SplitButtonDefaults.trailingButtonShape(),
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                }
            )
        }

        rule.onNodeWithTag("leading button").apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsEnabled()
        }
        rule.onNodeWithTag("trailing button").apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsEnabled()
        }
    }

    @Test
    fun basicSplitButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            SplitButton(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leading button"),
                        enabled = false
                    ) {
                        Text("My Button")
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        modifier = Modifier.size(34.dp).testTag("trailing button"),
                        onClick = {},
                        shape = SplitButtonDefaults.trailingButtonShape(),
                        enabled = false,
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                }
            )
        }

        rule.onNodeWithTag("leading button").apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsNotEnabled()
        }
        rule.onNodeWithTag("trailing button").apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsNotEnabled()
        }
    }

    @Test
    fun FilledSplitButton_contentDisplay() {
        rule.setMaterialContent(lightColorScheme()) {
            FilledSplitButton(
                onLeadingButtonClick = {},
                checked = false,
                onTrailingButtonClick = {},
                leadingContent = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Leading Icon",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("My Button", fontSize = 15.sp)
                },
                trailingContent = {
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                }
            )
        }

        rule.onNodeWithText("My Button").assertIsDisplayed()
        rule.onNodeWithContentDescription("Leading Icon").assertIsDisplayed()
        rule.onNodeWithContentDescription("Trailing Icon").assertIsDisplayed()
    }

    @Test
    fun TonalSplitButton_contentDisplay() {
        rule.setMaterialContent(lightColorScheme()) {
            TonalSplitButton(
                onLeadingButtonClick = {},
                checked = false,
                onTrailingButtonClick = {},
                leadingContent = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Leading Icon",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("My Button", fontSize = 15.sp)
                },
                trailingContent = {
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                }
            )
        }

        rule.onNodeWithText("My Button").assertIsDisplayed()
        rule.onNodeWithContentDescription("Leading Icon").assertIsDisplayed()
        rule.onNodeWithContentDescription("Trailing Icon").assertIsDisplayed()
    }

    @Test
    fun ElevatedSplitButton_contentDisplay() {
        rule.setMaterialContent(lightColorScheme()) {
            ElevatedSplitButton(
                onLeadingButtonClick = {},
                checked = false,
                onTrailingButtonClick = {},
                leadingContent = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Leading Icon",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("My Button", fontSize = 15.sp)
                },
                trailingContent = {
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                }
            )
        }

        rule.onNodeWithText("My Button").assertIsDisplayed()
        rule.onNodeWithContentDescription("Leading Icon").assertIsDisplayed()
        rule.onNodeWithContentDescription("Trailing Icon").assertIsDisplayed()
    }

    @Test
    fun OutlinedSplitButton_contentDisplay() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedSplitButton(
                onLeadingButtonClick = {},
                checked = false,
                onTrailingButtonClick = {},
                leadingContent = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Leading Icon",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("My Button", fontSize = 15.sp)
                },
                trailingContent = {
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                }
            )
        }

        rule.onNodeWithText("My Button").assertIsDisplayed()
        rule.onNodeWithContentDescription("Leading Icon").assertIsDisplayed()
        rule.onNodeWithContentDescription("Trailing Icon").assertIsDisplayed()
    }
}
