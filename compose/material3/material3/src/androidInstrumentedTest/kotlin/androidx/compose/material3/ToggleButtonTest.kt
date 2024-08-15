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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.tokens.ElevatedButtonTokens
import androidx.compose.material3.tokens.FilledButtonTokens
import androidx.compose.material3.tokens.OutlinedButtonTokens
import androidx.compose.material3.tokens.TonalButtonTokens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class ToggleButtonTest {
    @get:Rule val rule = createComposeRule()

    private val ToggleButtonTag = "ToggleButtonTag"
    private val TextTag = "TextTag"
    private val IconTag = "IconTag"

    @Test
    fun default_semantics() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                var checked by remember { mutableStateOf(false) }
                ToggleButton(
                    checked = checked,
                    modifier = Modifier.testTag(ToggleButtonTag),
                    onCheckedChange = { checked = it }
                ) {
                    Text("test")
                }
            }
        }

        rule
            .onNodeWithTag(ToggleButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsEnabled()
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            var checked by remember { mutableStateOf(false) }
            Box {
                ToggleButton(
                    checked = checked,
                    modifier = Modifier.testTag(ToggleButtonTag),
                    onCheckedChange = { checked = it },
                    enabled = false
                ) {
                    Text("test")
                }
            }
        }

        rule
            .onNodeWithTag(ToggleButtonTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsNotEnabled()
            .assertIsOff()
            .performClick()
            .assertIsOff()
    }

    @Test
    fun toggleButton_contentPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            var checked by remember { mutableStateOf(false) }
            Box {
                ToggleButton(
                    checked = checked,
                    modifier = Modifier.testTag(ToggleButtonTag),
                    onCheckedChange = { checked = it },
                    enabled = false
                ) {
                    Text(
                        text = "test",
                        modifier = Modifier.testTag(TextTag).semantics(mergeDescendants = true) {}
                    )
                }
            }
        }

        val textBounds = rule.onNodeWithTag(TextTag).getUnclippedBoundsInRoot()
        val toggleButtonBounds = rule.onNodeWithTag(ToggleButtonTag).getUnclippedBoundsInRoot()

        (textBounds.left - toggleButtonBounds.left).assertIsEqualTo(16.dp)
        (toggleButtonBounds.right - textBounds.right).assertIsEqualTo(16.dp)
    }

    @Test
    fun toggleButtonWithIcon_contentPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ToggleButton(
                    checked = false,
                    modifier = Modifier.testTag(ToggleButtonTag),
                    onCheckedChange = {},
                    enabled = false
                ) {
                    Box(
                        modifier =
                            Modifier.size(ToggleButtonDefaults.IconSize).testTag(IconTag).semantics(
                                mergeDescendants = true
                            ) {}
                    )
                    Spacer(modifier = Modifier.width(ToggleButtonDefaults.IconSpacing))
                    Text(
                        text = "test",
                        modifier = Modifier.testTag(TextTag).semantics(mergeDescendants = true) {}
                    )
                }
            }
        }

        val textBounds = rule.onNodeWithTag(TextTag).getUnclippedBoundsInRoot()
        val toggleButtonBounds = rule.onNodeWithTag(ToggleButtonTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(IconTag).getUnclippedBoundsInRoot()

        (iconBounds.left - toggleButtonBounds.left).assertIsEqualTo(16.dp)
        (textBounds.left - iconBounds.right).assertIsEqualTo(8.dp)
        (toggleButtonBounds.right - textBounds.right).assertIsEqualTo(16.dp)
    }

    @Test
    fun toggleButton_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            assertThat(
                    ToggleButtonDefaults.toggleButtonColors(
                        containerColor = Color.Unspecified,
                        contentColor = Color.Unspecified,
                        disabledContainerColor = Color.Unspecified,
                        disabledContentColor = Color.Unspecified,
                        checkedContainerColor = Color.Unspecified,
                        checkedContentColor = Color.Unspecified,
                    )
                )
                .isEqualTo(
                    ToggleButtonColors(
                        containerColor = FilledButtonTokens.UnselectedContainerColor.value,
                        contentColor = FilledButtonTokens.UnselectedPressedLabelTextColor.value,
                        disabledContainerColor =
                            FilledButtonTokens.DisabledContainerColor.value.copy(
                                alpha = FilledButtonTokens.DisabledContainerOpacity
                            ),
                        disabledContentColor =
                            FilledButtonTokens.DisabledLabelTextColor.value.copy(
                                alpha = FilledButtonTokens.DisabledLabelTextOpacity
                            ),
                        checkedContainerColor = FilledButtonTokens.SelectedContainerColor.value,
                        checkedContentColor = FilledButtonTokens.SelectedPressedLabelTextColor.value
                    )
                )
        }
    }

    @Test
    fun elevatedToggleButton_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            assertThat(
                    ToggleButtonDefaults.elevatedToggleButtonColors(
                        containerColor = Color.Unspecified,
                        contentColor = Color.Unspecified,
                        disabledContainerColor = Color.Unspecified,
                        disabledContentColor = Color.Unspecified,
                        checkedContainerColor = Color.Unspecified,
                        checkedContentColor = Color.Unspecified,
                    )
                )
                .isEqualTo(
                    ToggleButtonColors(
                        containerColor = ElevatedButtonTokens.UnselectedContainerColor.value,
                        contentColor = ElevatedButtonTokens.UnselectedPressedLabelTextColor.value,
                        disabledContainerColor =
                            ElevatedButtonTokens.DisabledContainerColor.value.copy(
                                alpha = ElevatedButtonTokens.DisabledContainerOpacity
                            ),
                        disabledContentColor =
                            ElevatedButtonTokens.DisabledLabelTextColor.value.copy(
                                alpha = ElevatedButtonTokens.DisabledLabelTextOpacity
                            ),
                        checkedContainerColor = ElevatedButtonTokens.SelectedContainerColor.value,
                        checkedContentColor =
                            ElevatedButtonTokens.SelectedPressedLabelTextColor.value
                    )
                )
        }
    }

    @Test
    fun tonalToggleButton_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            assertThat(
                    ToggleButtonDefaults.tonalToggleButtonColors(
                        containerColor = Color.Unspecified,
                        contentColor = Color.Unspecified,
                        disabledContainerColor = Color.Unspecified,
                        disabledContentColor = Color.Unspecified,
                        checkedContainerColor = Color.Unspecified,
                        checkedContentColor = Color.Unspecified,
                    )
                )
                .isEqualTo(
                    ToggleButtonColors(
                        containerColor = TonalButtonTokens.UnselectedContainerColor.value,
                        contentColor = TonalButtonTokens.UnselectedLabelTextColor.value,
                        disabledContainerColor =
                            TonalButtonTokens.DisabledContainerColor.value.copy(
                                alpha = TonalButtonTokens.DisabledContainerOpacity
                            ),
                        disabledContentColor =
                            TonalButtonTokens.DisabledLabelTextColor.value.copy(
                                alpha = TonalButtonTokens.DisabledLabelTextOpacity
                            ),
                        checkedContainerColor = TonalButtonTokens.SelectedContainerColor.value,
                        checkedContentColor = TonalButtonTokens.SelectedLabelTextColor.value
                    )
                )
        }
    }

    @Test
    fun outlinedToggleButton_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            assertThat(
                    ToggleButtonDefaults.outlinedToggleButtonColors(
                        containerColor = Color.Unspecified,
                        contentColor = Color.Unspecified,
                        disabledContainerColor = Color.Unspecified,
                        disabledContentColor = Color.Unspecified,
                        checkedContainerColor = Color.Unspecified,
                        checkedContentColor = Color.Unspecified,
                    )
                )
                .isEqualTo(
                    ToggleButtonColors(
                        containerColor = OutlinedButtonTokens.UnselectedPressedOutlineColor.value,
                        contentColor = OutlinedButtonTokens.UnselectedLabelTextColor.value,
                        disabledContainerColor =
                            OutlinedButtonTokens.DisabledOutlineColor.value.copy(
                                alpha = OutlinedButtonTokens.DisabledContainerOpacity
                            ),
                        disabledContentColor =
                            OutlinedButtonTokens.DisabledLabelTextColor.value.copy(
                                alpha = OutlinedButtonTokens.DisabledLabelTextOpacity
                            ),
                        checkedContainerColor = OutlinedButtonTokens.SelectedContainerColor.value,
                        checkedContentColor = OutlinedButtonTokens.SelectedLabelTextColor.value
                    )
                )
        }
    }

    @Test
    fun buttonShapes_AllRounded_hasRoundedShapesIsTrue() {
        assertThat(
                ButtonShapes(
                        shape = RoundedCornerShape(10.dp),
                        pressedShape = RoundedCornerShape(10.dp),
                        checkedShape = RoundedCornerShape(4.dp),
                    )
                    .hasRoundedCornerShapes
            )
            .isTrue()
    }

    @Test
    fun buttonShapes_mixedShapes_hasRoundedShapesIsFalse() {
        assertThat(
                ButtonShapes(
                        shape = RectangleShape,
                        pressedShape = RoundedCornerShape(10.dp),
                        checkedShape = RoundedCornerShape(4.dp),
                    )
                    .hasRoundedCornerShapes
            )
            .isFalse()
    }
}
