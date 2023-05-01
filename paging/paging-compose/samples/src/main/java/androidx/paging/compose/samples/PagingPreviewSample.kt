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

package androidx.paging.compose.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Sampled
@Preview
@Composable
fun PagingPreview() {
    /**
     * The composable that displays data from LazyPagingItems.
     *
     * This composable is inlined only for the purposes of this sample. In production code,
     * this function should be its own top-level function.
     */
    @Composable
    fun DisplayPaging(flow: Flow<PagingData<String>>) {
        // Flow of real data i.e. flow from a ViewModel, or flow of fake data i.e. from a Preview.
        val lazyPagingItems = flow.collectAsLazyPagingItems()
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .background(Color.Red)) {
            items(count = lazyPagingItems.itemCount) { index ->
                val item = lazyPagingItems[index]
                Text(text = "$item", fontSize = 35.sp, color = Color.Black)
            }
        }
    }

    /**
     * The preview function should be responsible for creating the fake data and passing it to the
     * function that displays it.
     */
    // create list of fake data for preview
    val fakeData = List(10) { "preview item $it" }
    // create pagingData from a list of fake data
    val pagingData = PagingData.from(fakeData)
    // pass pagingData containing fake data to a MutableStateFlow
    val fakeDataFlow = MutableStateFlow(pagingData)
    // pass flow to composable
    DisplayPaging(flow = fakeDataFlow)
}