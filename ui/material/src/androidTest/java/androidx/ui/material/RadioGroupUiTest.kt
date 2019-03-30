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

import androidx.test.filters.MediumTest
import androidx.ui.core.CraneWrapper
import androidx.ui.test.android.AndroidUiTestRunner
import androidx.ui.test.assertIsInMutuallyExclusiveGroup
import androidx.ui.test.assertIsSelected
import androidx.ui.test.assertSemanticsIsEqualTo
import androidx.ui.test.createFullSemantics
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import com.google.r4a.Model
import com.google.r4a.composer
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Model
internal class RadioGroupSelectedState<T>(var selected: T)

@MediumTest
@RunWith(JUnit4::class)
class RadioGroupUiTest : AndroidUiTestRunner() {

    private val idOne = 1
    private val idTwo = 2
    private val idThree = 3

    private val unselectedRadioGroupItemSemantics = createFullSemantics(
        inMutuallyExclusiveGroup = true,
        selected = false
    )
    private val selectedRadioGroupItemSemantics = createFullSemantics(
        inMutuallyExclusiveGroup = true,
        selected = true
    )
    private val options = mapOf(
        idOne to "Bar",
        idTwo to "Foo",
        idThree to "Sap"
    )

    @Test
    fun radioGroupTest_defaultSemantics() {
        val select = RadioGroupSelectedState(idOne)

        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <RadioGroup
                        options
                        selectedOption=select.selected
                        onOptionSelected={ select.selected = it } />
                </MaterialTheme>
            </CraneWrapper>
        }

        findByTag(idOne.toString()).assertSemanticsIsEqualTo(selectedRadioGroupItemSemantics)
        findByTag(idTwo.toString()).assertSemanticsIsEqualTo(unselectedRadioGroupItemSemantics)
        findByTag(idThree.toString()).assertSemanticsIsEqualTo(unselectedRadioGroupItemSemantics)

        findByTag(idOne.toString())
            .assertIsInMutuallyExclusiveGroup()
            .assertIsSelected(true)
        findByTag(idTwo.toString())
            .assertIsInMutuallyExclusiveGroup()
            .assertIsSelected(false)
        findByTag(idThree.toString())
            .assertIsInMutuallyExclusiveGroup()
            .assertIsSelected(false)
    }

    @Test
    fun radioGroupTest_ensureUnselectable() {
        val select = RadioGroupSelectedState(idOne)

        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <RadioGroup
                        options
                        selectedOption=select.selected
                        onOptionSelected={ select.selected = it } />
                </MaterialTheme>
            </CraneWrapper>
        }

        findByTag(idOne.toString())
            .assertSemanticsIsEqualTo(selectedRadioGroupItemSemantics)
            .doClick()
            .assertSemanticsIsEqualTo(selectedRadioGroupItemSemantics)

        findByTag(idTwo.toString())
            .assertSemanticsIsEqualTo(unselectedRadioGroupItemSemantics)

        findByTag(idThree.toString())
            .assertSemanticsIsEqualTo(unselectedRadioGroupItemSemantics)
    }

    @Test
    fun radioGroupTest_clickSelect() {
        val select = RadioGroupSelectedState(idOne)
        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <RadioGroup
                        options
                        selectedOption=select.selected
                        onOptionSelected={ select.selected = it } />
                </MaterialTheme>
            </CraneWrapper>
        }
        findByTag(idTwo.toString())
            .assertSemanticsIsEqualTo(unselectedRadioGroupItemSemantics)
            .doClick()
            .assertSemanticsIsEqualTo(selectedRadioGroupItemSemantics)

        findByTag(idOne.toString())
            .assertSemanticsIsEqualTo(unselectedRadioGroupItemSemantics)

        findByTag(idThree.toString())
            .assertSemanticsIsEqualTo(unselectedRadioGroupItemSemantics)
    }

    @Test
    fun radioGroupTest_clickSelectTwoDifferentItems() {
        val select = RadioGroupSelectedState(idOne)

        setContent {
            <CraneWrapper>
                <MaterialTheme>
                    <RadioGroup
                        options
                        selectedOption=select.selected
                        onOptionSelected={ select.selected = it } />
                </MaterialTheme>
            </CraneWrapper>
        }

        findByTag(idTwo.toString())
            .assertSemanticsIsEqualTo(unselectedRadioGroupItemSemantics)
            .doClick()
            .assertSemanticsIsEqualTo(selectedRadioGroupItemSemantics)

        findByTag(idOne.toString())
            .assertSemanticsIsEqualTo(unselectedRadioGroupItemSemantics)

        findByTag(idThree.toString())
            .assertSemanticsIsEqualTo(unselectedRadioGroupItemSemantics)
            .doClick()
            .assertSemanticsIsEqualTo(selectedRadioGroupItemSemantics)

        findByTag(idOne.toString())
            .assertSemanticsIsEqualTo(unselectedRadioGroupItemSemantics)

        findByTag(idTwo.toString())
            .assertSemanticsIsEqualTo(unselectedRadioGroupItemSemantics)
    }
}