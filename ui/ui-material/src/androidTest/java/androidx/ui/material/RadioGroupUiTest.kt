/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material

import androidx.compose.Composable
import androidx.compose.mutableStateOf
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.foundation.Strings
import androidx.ui.layout.Column
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.assertIsInMutuallyExclusiveGroup
import androidx.ui.test.assertIsSelected
import androidx.ui.test.assertIsUnselected
import androidx.ui.test.assertValueEquals
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class RadioGroupUiTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val itemOne = "Bar"
    private val itemTwo = "Foo"
    private val itemThree = "Sap"

    private fun SemanticsNodeInteraction.assertHasSelectedSemantics(): SemanticsNodeInteraction =
        assertIsInMutuallyExclusiveGroup()
            .assertIsSelected()
            .assertValueEquals(Strings.Selected)

    private fun SemanticsNodeInteraction.assertHasUnSelectedSemantics(): SemanticsNodeInteraction =
        assertIsInMutuallyExclusiveGroup()
            .assertIsUnselected()
            .assertValueEquals(Strings.NotSelected)

    private val options = listOf(itemOne, itemTwo, itemThree)

    @Composable
    fun VerticalRadioGroupforTests(children: @Composable RadioGroupScope.() -> Unit) {
        RadioGroup {
            Column {
                children(p1 = this@RadioGroup)
            }
        }
    }

    @Test
    fun radioGroupTest_defaultSemantics() {
        val selected = mutableStateOf(itemOne)

        composeTestRule.setMaterialContent {
            VerticalRadioGroupforTests {
                options.forEach { item ->
                    RadioGroupTextItem(
                        modifier = Modifier.testTag(item),
                        text = item,
                        selected = (selected.value == item),
                        onSelect = { selected.value = item })
                }
            }
        }

        findByTag(itemOne).assertHasSelectedSemantics()
        findByTag(itemTwo).assertHasUnSelectedSemantics()
        findByTag(itemThree).assertHasUnSelectedSemantics()
    }

    @Test
    fun radioGroupTest_ensureUnselectable() {
        val selected = mutableStateOf(itemOne)

        composeTestRule.setMaterialContent {
            VerticalRadioGroupforTests {
                options.forEach { item ->
                    RadioGroupTextItem(
                        modifier = Modifier.testTag(item),
                        text = item,
                        selected = (selected.value == item),
                        onSelect = { selected.value = item })
                }
            }
        }

        findByTag(itemOne)
            .assertHasSelectedSemantics()
            .doClick()
            .assertHasSelectedSemantics()

        findByTag(itemTwo)
            .assertHasUnSelectedSemantics()

        findByTag(itemThree)
            .assertHasUnSelectedSemantics()
    }

    @Test
    fun radioGroupTest_clickSelect() {
        val selected = mutableStateOf(itemOne)
        composeTestRule.setMaterialContent {
            VerticalRadioGroupforTests {
                options.forEach { item ->
                    RadioGroupTextItem(
                        modifier = Modifier.testTag(item),
                        text = item,
                        selected = (selected.value == item),
                        onSelect = { selected.value = item })
                }
            }
        }
        findByTag(itemTwo)
            .assertHasUnSelectedSemantics()
            .doClick()
            .assertHasSelectedSemantics()

        findByTag(itemOne)
            .assertHasUnSelectedSemantics()

        findByTag(itemThree)
            .assertHasUnSelectedSemantics()
    }

    @Test
    fun radioGroupTest_clickSelectTwoDifferentItems() {
        val selected = mutableStateOf(itemOne)

        composeTestRule.setMaterialContent {
            VerticalRadioGroupforTests {
                options.forEach { item ->
                    RadioGroupTextItem(
                        modifier = Modifier.testTag(item),
                        text = item,
                        selected = (selected.value == item),
                        onSelect = { selected.value = item })
                }
            }
        }

        findByTag(itemTwo)
            .assertHasUnSelectedSemantics()
            .doClick()
            .assertHasSelectedSemantics()

        findByTag(itemOne)
            .assertHasUnSelectedSemantics()

        findByTag(itemThree)
            .assertHasUnSelectedSemantics()
            .doClick()
            .assertHasSelectedSemantics()

        findByTag(itemOne)
            .assertHasUnSelectedSemantics()

        findByTag(itemTwo)
            .assertHasUnSelectedSemantics()
    }

    @Test
    fun radioButton_materialSizes_whenSelected() {
        materialSizesTestForValue(selected = true)
    }

    @Test
    fun radioButton_materialSizes_whenNotSelected() {
        materialSizesTestForValue(selected = false)
    }

    private fun materialSizesTestForValue(selected: Boolean) {
        composeTestRule
            .setMaterialContentAndCollectSizes {
                RadioButton(selected = selected, onSelect = null)
            }
            .assertIsSquareWithSize { 2.dp.toIntPx() * 2 + 20.dp.toIntPx() }
    }
}