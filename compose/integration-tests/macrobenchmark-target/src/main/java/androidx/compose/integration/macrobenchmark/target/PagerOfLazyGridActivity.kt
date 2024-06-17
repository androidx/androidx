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

package androidx.compose.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch

class PagerOfLazyGridActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pageCount = intent.getIntExtra(PageCount, 100)
        val gridItemCount = intent.getIntExtra(GridItemCount, 100)

        setContent { MaterialTheme { HorizontalPagerOfLazyGrid(pageCount, gridItemCount) } }

        launchIdlenessTracking()
    }

    companion object {
        const val PageCount = "PAGE_COUNT"
        const val GridItemCount = "GRID_ITEM_COUNT"
    }
}

@Composable
private fun HorizontalPagerOfLazyGrid(pages: Int = 100, gridItems: Int = 100) {
    val pagerState: PagerState = rememberPagerState(initialPage = 1) { pages }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        Button(
            onClick = {
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }
        ) {
            Text("Next")
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.semantics { contentDescription = "Pager" }
        ) { page: Int ->
            Grid(gridItems, page)
        }
    }
}

@Composable
private fun Grid(itemCount: Int, pageNum: Int) {
    val text = remember(pageNum) { "Hello + $pageNum" }
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(3),
    ) {
        items(itemCount, contentType = { "cell" }) { _ ->
            Button(onClick = {}) { Text(text = text) }
        }
    }
}
