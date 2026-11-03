/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.paging.compose

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.paging.ItemSnapshotList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ItemSnapshotListTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun lazyPagingColumnShowsItems() {
        rule.setContent {
            val itemSnapshotList =
                ItemSnapshotList(
                    placeholdersBefore = 1,
                    placeholdersAfter = 1,
                    items = listOf(1, 2, 3),
                )
            LazyColumn {
                items(
                    count = itemSnapshotList.size,
                    key = itemSnapshotList.itemKey { it },
                    contentType = itemSnapshotList.itemContentType(null),
                ) { index ->
                    val item = itemSnapshotList[index]
                    BasicText("$item", Modifier.testTag("$index"))
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag("0").assertTextEquals("null")

        rule.onNodeWithTag("1").assertTextEquals("1")

        rule.onNodeWithTag("2").assertTextEquals("2")

        rule.onNodeWithTag("3").assertTextEquals("3")

        rule.onNodeWithTag("4").assertTextEquals("null")
    }
}
