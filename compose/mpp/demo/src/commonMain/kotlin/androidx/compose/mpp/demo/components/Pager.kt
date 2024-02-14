/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.mpp.demo.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

@Composable
fun PagerExample() {
    Column(Modifier.fillMaxSize()) {
        HorisintalPagerExample(Modifier.weight(1f))
        VerticalPagerExample(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorisintalPagerExample(modifier: Modifier) {
    val pagerState = rememberPagerState(pageCount = {
        10
    })
    HorizontalPager(state = pagerState, modifier = modifier) { page ->
        val background = rememberSaveable(key = "HorizontalPager: $page") {
            Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
        }
        Box(Modifier.fillMaxSize().background(background), contentAlignment = Alignment.Center) {
            Text(
                text = "HorizontalPager: $page",
                style = MaterialTheme.typography.h1
            )
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerticalPagerExample(modifier: Modifier) {
    val pagerState = rememberPagerState(pageCount = {
        10
    })
    VerticalPager(state = pagerState, modifier = modifier) { page ->
        val background = rememberSaveable(key = "VerticalPager: $page") {
            Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
        }
        Box(Modifier.fillMaxSize().background(background), contentAlignment = Alignment.Center) {
            Text(
                text = "VerticalPager: $page",
                style = MaterialTheme.typography.h1
            )
        }
    }

}