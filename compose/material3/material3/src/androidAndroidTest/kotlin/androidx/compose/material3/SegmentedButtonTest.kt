/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.material3.tokens.OutlinedSegmentedButtonTokens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class SegmentedButtonTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun toggleableSegmentedButton_itemsDisplay() {
        val values = listOf("Day", "Month", "Week")

        rule.setMaterialContent(lightColorScheme()) {
            MultiChoiceSegmentedButtonRow {
                values.forEach {
                    SegmentedButton(
                        shape = RectangleShape,
                        checked = false,
                        onCheckedChange = {}
                    ) {
                        Text(it)
                    }
                }
            }
        }

        values.forEach { rule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun selectableSegmentedButton_itemsDisplay() {
        val values = listOf("Day", "Month", "Week")

        rule.setMaterialContent(lightColorScheme()) {
            SingleChoiceSegmentedButtonRow {
                values.forEach {
                    SegmentedButton(
                        shape = RectangleShape,
                        selected = false,
                        onClick = {}
                    ) {
                        Text(it)
                    }
                }
            }
        }

        values.forEach { rule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun segmentedButton_itemsChecked() {
        var checked by mutableStateOf(true)
        rule.setMaterialContent(lightColorScheme()) {
            MultiChoiceSegmentedButtonRow {
                SegmentedButton(
                    onCheckedChange = { checked = it },
                    checked = checked,
                    shape = RectangleShape,
                ) {
                    Text("Day")
                }
                SegmentedButton(
                    onCheckedChange = { checked = it },
                    checked = !checked,
                    shape = RectangleShape,
                ) {
                    Text("Month")
                }
            }
        }

        rule.onNodeWithText("Day").assertIsOn()
        rule.onNodeWithText("Month").assertIsOff()

        rule.runOnIdle {
            checked = false
        }

        rule.onNodeWithText("Day").assertIsOff()
        rule.onNodeWithText("Month").assertIsOn()
    }

    @Test
    fun selectableSegmentedButton_semantics() {
        rule.setMaterialContent(lightColorScheme()) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.testTag("row")) {
                SegmentedButton(
                    selected = false,
                    onClick = {},
                    shape = RectangleShape,
                ) {
                    Text("Day")
                }
                SegmentedButton(
                    selected = false,
                    onClick = {},
                    shape = RectangleShape,
                ) {
                    Text("Month")
                }
            }
        }

        val semanticsNode = rule.onNodeWithTag("row").fetchSemanticsNode()
        val selectableGroup = semanticsNode.config.getOrNull(SemanticsProperties.SelectableGroup)

        assertThat(selectableGroup).isNotNull()
    }

    @Test
    fun segmentedButton_icon() {
        var checked by mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            MultiChoiceSegmentedButtonRow(modifier = Modifier.testTag("row")) {
                SegmentedButton(
                    checked = checked,
                    onCheckedChange = {},
                    icon = { Text(if (checked) "checked" else "unchecked") },
                    shape = RectangleShape,
                ) {
                    Text("Day")
                }
            }
        }

        rule.onNodeWithText("unchecked").assertIsDisplayed()

        rule.runOnIdle { checked = true }
        rule.waitForIdle()

        rule.onNodeWithText("checked").assertIsDisplayed()
    }

    @Test
    fun segmentedButton_Sizing() {
        val itemSize = 60.dp

        rule.setMaterialContentForSizeAssertions(
            parentMaxWidth = 300.dp, parentMaxHeight = 100.dp
        ) {
            MultiChoiceSegmentedButtonRow {
                SegmentedButton(checked = false, onCheckedChange = {}, shape = RectangleShape) {
                    Text(modifier = Modifier.width(60.dp), text = "Day")
                }
                SegmentedButton(checked = false, onCheckedChange = {}, shape = RectangleShape) {
                    Text(modifier = Modifier.width(30.dp), text = "Month")
                }
            }
        }
            .assertWidthIsAtLeast((itemSize + 12.dp * 2) * 2)
            .assertHeightIsEqualTo(OutlinedSegmentedButtonTokens.ContainerHeight)
    }

    @Test
    fun segmentedButtonBorder_default_matchesSpec() {
        lateinit var border: BorderStroke
        var specColor: Color = Color.Unspecified
        rule.setMaterialContent(lightColorScheme()) {
            specColor = OutlinedSegmentedButtonTokens.OutlineColor.value
            border = SegmentedButtonDefaults.Border.borderStroke(
                checked = true,
                enabled = true,
                colors = SegmentedButtonDefaults.colors()
            )
        }

        assertThat((border.brush as SolidColor).value).isEqualTo(specColor)
        assertThat(border.width).isEqualTo(OutlinedSegmentedButtonTokens.OutlineWidth)
    }
}
