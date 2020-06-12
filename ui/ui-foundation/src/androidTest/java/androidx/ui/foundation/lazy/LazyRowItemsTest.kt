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

package androidx.ui.foundation.lazy

import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.layout.Spacer
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.preferredWidth
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class LazyRowItemsTest {

    private val LazyRowItemsTag = "LazyRowItemsTag"

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun lazyRowOnlyVisibleItemsAdded() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            Stack(Modifier.preferredWidth(200.dp)) {
                LazyRowItems(items) {
                    Spacer(Modifier.preferredWidth(101.dp).fillMaxHeight().testTag(it))
                }
            }
        }

        findByTag("1")
            .assertIsDisplayed()

        findByTag("2")
            .assertIsDisplayed()

        findByTag("3")
            .assertDoesNotExist()

        findByTag("4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyRowScrollToShowItems123() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            Stack(Modifier.preferredWidth(200.dp)) {
                LazyRowItems(items, Modifier.testTag(LazyRowItemsTag)) {
                    Spacer(Modifier.preferredWidth(101.dp).fillMaxHeight().testTag(it))
                }
            }
        }

        findByTag(LazyRowItemsTag)
            .scrollBy(x = 50.dp, density = composeTestRule.density)

        findByTag("1")
            .assertIsDisplayed()

        findByTag("2")
            .assertIsDisplayed()

        findByTag("3")
            .assertIsDisplayed()

        findByTag("4")
            .assertDoesNotExist()
    }

    @Test
    fun lazyRowScrollToHideFirstItem() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            Stack(Modifier.preferredWidth(200.dp)) {
                LazyRowItems(items, Modifier.testTag(LazyRowItemsTag)) {
                    Spacer(Modifier.preferredWidth(101.dp).fillMaxHeight().testTag(it))
                }
            }
        }

        findByTag(LazyRowItemsTag)
            .scrollBy(x = 102.dp, density = composeTestRule.density)

        findByTag("1")
            .assertDoesNotExist()

        findByTag("2")
            .assertIsDisplayed()

        findByTag("3")
            .assertIsDisplayed()
    }

    @Test
    fun lazyRowScrollToShowItems234() {
        val items = (1..4).map { it.toString() }

        composeTestRule.setContent {
            Stack(Modifier.preferredWidth(200.dp)) {
                LazyRowItems(items, Modifier.testTag(LazyRowItemsTag)) {
                    Spacer(Modifier.preferredWidth(101.dp).fillMaxHeight().testTag(it))
                }
            }
        }

        findByTag(LazyRowItemsTag)
            .scrollBy(x = 150.dp, density = composeTestRule.density)

        findByTag("1")
            .assertDoesNotExist()

        findByTag("2")
            .assertIsDisplayed()

        findByTag("3")
            .assertIsDisplayed()

        findByTag("4")
            .assertIsDisplayed()
    }
}
