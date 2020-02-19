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

package androidx.ui.foundation

import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.ui.core.Text
import androidx.ui.foundation.selection.MutuallyExclusiveSetItem
import androidx.ui.test.assertIsSelected
import androidx.ui.test.assertIsUnselected
import androidx.ui.test.assertSemanticsIsEqualTo
import androidx.ui.test.createComposeRule
import androidx.ui.test.createFullSemantics
import androidx.ui.test.doClick
import androidx.ui.test.find
import androidx.ui.test.findAll
import androidx.ui.test.isInMutuallyExclusiveGroup
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class MutuallyExclusiveSetItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mutuallyExclusiveItem_defaultSemantics() {
        composeTestRule.setContent {
            MutuallyExclusiveSetItem(selected = true, onClick = {}) {
                Text("Text in item")
            }
        }

        Truth.assertThat(findAll(isInMutuallyExclusiveGroup()).size).isEqualTo(1)
        findAll(isInMutuallyExclusiveGroup()).first()
            .assertSemanticsIsEqualTo(
                createFullSemantics(
                    inMutuallyExclusiveGroup = true,
                    isSelected = true
                )
            )
    }

    @Test
    fun mutuallyExclusiveItem_defaultClicks() {
        composeTestRule.setContent {
            val (selected, onSelected) = state { false }
            MutuallyExclusiveSetItem(selected, onClick = { onSelected(!selected) }) {
                Text("Text in item")
            }
        }

        find(isInMutuallyExclusiveGroup())
            .assertIsUnselected()
            .doClick()
            .assertIsSelected()
            .doClick()
            .assertIsUnselected()
    }

    @Test
    fun mutuallyExclusiveItem_noClicksNoChanges() {
        composeTestRule.setContent {
            val (selected, _) = state { false }
            MutuallyExclusiveSetItem(selected, onClick = {}) {
                Text("Text in item")
            }
        }

        find(isInMutuallyExclusiveGroup())
            .assertIsUnselected()
            .doClick()
            .assertIsUnselected()
    }
}