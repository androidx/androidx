/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.list

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.test.filters.MediumTest
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.setGlimmerThemeContent
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class ListInteractionTest(orientation: Orientation) : BaseListTestWithOrientation(orientation) {

    @Test
    fun performScrollToIndex() {
        rule.setGlimmerThemeContent { TestList { Text("Item ($it)") } }

        // Make sure the far items are not laid out.
        val targetIndex = Int.MAX_VALUE / 2
        rule.onNodeWithText("Item ($targetIndex)").assertDoesNotExist()

        // Scroll to the far index and check that the item become visible.
        rule.onNodeWithTag(LIST_TEST_TAG).performScrollToIndex(targetIndex)
        rule.onNodeWithText("Item ($targetIndex)").assertIsDisplayed()
    }

    @Test
    fun performScrollToKey() {
        val targetIndex = 1000

        rule.setGlimmerThemeContent {
            TestList(keyProvider = { it }, itemContent = { index -> Text("Item ($index)") })
        }

        // Scroll to the target key and check that the item become visible.
        rule.onNodeWithTag(LIST_TEST_TAG).performScrollToKey(targetIndex)
        rule.onNodeWithText("Item ($targetIndex)").assertIsDisplayed()
    }

    @Test
    fun pinnedItem_remainsLaidOut() {
        rule.setGlimmerThemeContent {
            TestList { index ->
                Text("Item ($index)")
                if (index == 3) {
                    // Pin the item #3.
                    val pinnableContainer = requireNotNull(LocalPinnableContainer.current)
                    pinnableContainer.pin()
                }
            }
        }

        // Scroll until the first items are no longer visible.
        rule.onNodeWithTag(LIST_TEST_TAG).performScrollToIndex(5_000)

        // The non-pinned item should not be laid out.
        rule.onNodeWithText("Item (4)").assertIsNotDisplayed().assertDoesNotExist()
        // The pinned item must be laid out but not visible.
        rule.onNodeWithText("Item (3)").assertIsNotDisplayed().assertExists()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }
}
