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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun SnapFlingBehaviorSimpleSample() {
    val state = rememberLazyListState()

    LazyRow(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        state = state,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
    ) {
        items(200) {
            Box(
                modifier = Modifier
                    .height(400.dp)
                    .width(200.dp)
                    .padding(8.dp)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(it.toString(), fontSize = 32.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun SnapFlingBehaviorCustomizedSample() {
    val state = rememberLazyListState()

    // If you'd like to customize either the snap behavior or the layout provider
    val snappingLayout = remember(state) { SnapLayoutInfoProvider(state) }
    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)

    LazyRow(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        state = state,
        flingBehavior = flingBehavior
    ) {
        items(200) {
            Box(
                modifier = Modifier
                    .height(400.dp)
                    .width(200.dp)
                    .padding(8.dp)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(it.toString(), fontSize = 32.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun SnapFlingBehaviorSnapPosition() {
    val state = rememberLazyListState()
    val density = LocalDensity.current

    // Illustrate using a custom SnapPosition that will snap to a static location (200dp) after
    // the content padding.
    val snappingLayout = remember(state, density) {
        val snapPosition = object : SnapPosition {
            override fun position(
                layoutSize: Int,
                itemSize: Int,
                beforeContentPadding: Int,
                afterContentPadding: Int,
                itemIndex: Int,
                itemCount: Int
            ): Int {
                return with(density) { beforeContentPadding + 200.dp.roundToPx() }
            }
        }
        SnapLayoutInfoProvider(state, snapPosition)
    }
    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)

    LazyRow(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        state = state,
        flingBehavior = flingBehavior
    ) {
        items(200) {
            Box(
                modifier = Modifier
                    .height(400.dp)
                    .width(200.dp)
                    .padding(8.dp)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(it.toString(), fontSize = 32.sp)
            }
        }
    }
}
