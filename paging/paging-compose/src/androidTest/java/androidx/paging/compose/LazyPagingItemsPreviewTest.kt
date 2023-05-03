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

package androidx.paging.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.tooling.PreviewActivity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test

class LazyPagingItemsPreviewTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<PreviewActivity>()

    @Test
    fun pagingPreviewTest() {
        composeTestRule.setContent {
            PagingPreview()
        }
        for (i in 0..9) {
            composeTestRule.onNodeWithTag("$i")
                .assertIsDisplayed()
        }
    }

    @Test
    fun emptyPreview() {
        composeTestRule.setContent {
            EmptyPreview()
        }
        composeTestRule.onNodeWithTag("0")
            .assertDoesNotExist()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Preview
@Composable
fun PagingPreview() {
    val data = List(50) { it }
    val flow = MutableStateFlow(PagingData.from(data))
    CompositionLocalProvider(
        LocalInspectionMode provides true,
    ) {
        // Use StandardTestDispatcher so we don't start collecting on PagingData
        val lazyPagingItems = flow.collectAsLazyPagingItems(StandardTestDispatcher())
        LazyColumn(Modifier.height(500.dp)) {
            items(count = lazyPagingItems.itemCount) { index ->
                val item = lazyPagingItems[index]
                Spacer(Modifier.height(50.dp).fillParentMaxWidth().testTag("$item"))
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Preview
@Composable
fun EmptyPreview() {
    val data = emptyList<Int>()
    val flow = MutableStateFlow(PagingData.from(data))
    CompositionLocalProvider(
        LocalInspectionMode provides true,
    ) {
        // Use StandardTestDispatcher so we don't start collecting on PagingData
        val lazyPagingItems = flow.collectAsLazyPagingItems(StandardTestDispatcher())
        LazyColumn(Modifier.height(500.dp)) {
            items(count = lazyPagingItems.itemCount) { index ->
                val item = lazyPagingItems[index]
                Spacer(Modifier.height(50.dp).fillParentMaxWidth().testTag("$item"))
            }
        }
    }
}