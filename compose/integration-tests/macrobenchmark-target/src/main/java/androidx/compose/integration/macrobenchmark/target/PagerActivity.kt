/*
 * Copyright 2022 The Android Open Source Project
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
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

class PagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val benchmarkType = intent.getStringExtra(BenchmarkType.Key)
        val enableTab = intent.getBooleanExtra(BenchmarkType.Tab, false)

        setContent {
            val pagerState = rememberPagerState { ItemCount }

            Column(modifier = Modifier.fillMaxSize()) {
                if (enableTab) {
                    val scope = rememberCoroutineScope()
                    Button(
                        modifier = Modifier.semantics { contentDescription = "Next" },
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    ) {
                        Text("Next Page")
                    }
                }
                when (benchmarkType) {
                    BenchmarkType.Grid -> BenchmarkPagerOfGrids(pagerState)
                    BenchmarkType.List -> BenchmarkPagerOfLists(pagerState)
                    BenchmarkType.WebView -> BenchmarkPagerOfWebViews(pagerState)
                    BenchmarkType.FullScreenImage -> BenchmarkPagerOfFullScreenImages(pagerState)
                    BenchmarkType.FixedSizeImage -> BenchmarkPagerOfFixedSizeImages(pagerState)
                    BenchmarkType.ListOfPager -> BenchmarkListOfPager()
                    else -> throw IllegalStateException("Benchmark Type not known ")
                }
            }
        }

        launchIdlenessTracking()
    }

    @Composable
    fun BenchmarkPagerOfGrids(pagerState: PagerState) {
        FullSizePager(pagerState) {
            LazyVerticalGrid(GridCells.Fixed(4)) {
                items(200) {
                    Card(modifier = Modifier.fillMaxWidth().height(64.dp).padding(8.dp)) {
                        Text(it.toString())
                    }
                }
            }
        }
    }

    @Composable
    fun BenchmarkPagerOfLists(pagerState: PagerState) {
        FullSizePager(pagerState) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(200) {
                    Card(modifier = Modifier.fillMaxWidth().height(64.dp).padding(8.dp)) {
                        Text(it.toString())
                    }
                }
            }
        }
    }

    @Composable
    fun BenchmarkPagerOfWebViews(pagerState: PagerState) {
        FullSizePager(pagerState) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()

                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportZoom(true)
                    }
                },
                update = { webView -> webView.loadUrl("https://www.google.com/") }
            )
        }
    }

    @Composable
    fun BenchmarkPagerOfFullScreenImages(pagerState: PagerState) {
        FullSizePager(pagerState) { page ->
            val pageImage = Images[(page - ItemCount / 2).floorMod(5)]
            Image(
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(pageImage.second),
                contentDescription = stringResource(pageImage.third)
            )
        }
    }

    @Composable
    fun BenchmarkPagerOfFixedSizeImages(pagerState: PagerState) {
        FixedSizePager(pagerState) { page ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val pageImage = Images[(page - ItemCount / 2).floorMod(5)]
                Image(
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.height(200.dp).fillMaxWidth(),
                    painter = painterResource(pageImage.second),
                    contentDescription = stringResource(pageImage.third)
                )
            }
        }
    }

    @Composable
    fun FullSizePager(pagerState: PagerState, content: @Composable PagerScope.(Int) -> Unit) {
        HorizontalPager(
            modifier = Modifier.semantics { contentDescription = "Pager" }.background(Color.White),
            state = pagerState,
            pageSize = PageSize.Fill,
            pageContent = content
        )
    }

    @Composable
    fun FixedSizePager(pagerState: PagerState, content: @Composable PagerScope.(Int) -> Unit) {
        HorizontalPager(
            modifier = Modifier.semantics { contentDescription = "Pager" }.background(Color.White),
            state = pagerState,
            pageSize = PageSize.Fixed(200.dp),
            pageSpacing = 10.dp,
            pageContent = content
        )
    }

    @Composable
    fun BenchmarkListOfPager() {
        LazyColumn(Modifier.semantics { contentDescription = "List" }) {
            items(ItemCount * ItemCount) {
                FixedSizePager(rememberPagerState { ItemCount }) {
                    Box(Modifier.size(200.dp)) { Text("Page ${it.toString()}") }
                }
            }
        }
    }

    companion object {
        const val ItemCount = 100

        object BenchmarkType {
            val Key = "BenchmarkType"
            val Tab = "EnableTab"
            val Grid = "Pager of Grids"
            val List = "Pager of List"
            val WebView = "Pager of WebViews"
            val FullScreenImage = "Pager of Full Screen Images"
            val FixedSizeImage = "Pager of Fixed Size Images"
            val ListOfPager = "Pager Inside A List"
        }

        val Images =
            listOf(
                Triple(0, R.drawable.carousel_image_1, R.string.carousel_image_1_description),
                Triple(1, R.drawable.carousel_image_2, R.string.carousel_image_2_description),
                Triple(2, R.drawable.carousel_image_3, R.string.carousel_image_3_description),
                Triple(3, R.drawable.carousel_image_4, R.string.carousel_image_4_description),
                Triple(4, R.drawable.carousel_image_5, R.string.carousel_image_5_description),
            )
    }
}

private fun Int.floorMod(other: Int): Int =
    when (other) {
        0 -> this
        else -> this - floorDiv(other) * other
    }
