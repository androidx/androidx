/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.test.filters.LargeTest
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.testTag
import androidx.ui.test.SemanticsMatcher
import androidx.ui.test.assert
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import androidx.ui.test.runOnIdleCompose
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class AdapterListTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun removeItemsTest() {
        val startingNumItems = 3
        var numItems = startingNumItems
        var numItemsModel by mutableStateOf(numItems)
        val tag = "List"
        composeTestRule.setContent {
            Semantics(container = true, properties = { testTag = tag }) {
                AdapterList((1..numItemsModel).toList()) {
                    Semantics(container = true) {
                        Text("$it")
                    }
                }
            }
        }

        while (numItems >= 0) {
            // Confirm the number of children to ensure there are no extra items
            findByTag(tag).assert(SemanticsMatcher.fromCondition("Has $numItems children") {
                children.size == numItems
            })

            // Confirm the children's content
            for (i in 1..3) {
                findByText("$i").apply {
                    if (i <= numItems) {
                        assertExists()
                    } else {
                        assertDoesNotExist()
                    }
                }
            }
            numItems--
            if (numItems >= 0) {
                // Don't set the model to -1
                runOnIdleCompose { numItemsModel = numItems }
            }
        }
    }

    @Test
    fun changingDataTest() {
        val dataLists = listOf(
            (1..3).toList(),
            (4..8).toList(),
            (3..4).toList()
        )
        var dataModel by mutableStateOf(dataLists[0])
        val tag = "List"
        composeTestRule.setContent {
            Semantics(container = true, properties = { testTag = tag }) {
                AdapterList(dataModel) {
                    Semantics(container = true) {
                        Text("$it")
                    }
                }
            }
        }

        for (data in dataLists) {
            runOnIdleCompose { dataModel = data }

            // Confirm the number of children to ensure there are no extra items
            val numItems = data.size
            findByTag(tag).assert(SemanticsMatcher.fromCondition("Has $numItems children") {
                children.size == numItems
            })

            // Confirm the children's content
            for (item in data) {
                findByText("$item").assertExists()
            }
        }
    }
}