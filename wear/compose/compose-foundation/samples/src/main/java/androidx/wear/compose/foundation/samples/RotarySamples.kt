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

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryDefaults
import androidx.wear.compose.foundation.rotary.RotaryScrollableAdapter
import androidx.wear.compose.foundation.rotary.rotary

@OptIn(ExperimentalWearFoundationApi::class)
@Sampled
@Composable
fun RotaryScrollSample() {
    val scrollableState = rememberLazyListState()
    val focusRequester = rememberActiveFocusRequester()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .rotary(
                rotaryBehavior = RotaryDefaults.scrollBehavior(scrollableState),
                focusRequester = focusRequester
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        state = scrollableState
    ) {
        items(300) {
            BasicText(
                text = "item $it",
                modifier = Modifier.background(Color.Gray),
                style = TextStyle.Default.copy()
            )
        }
    }
}

@OptIn(ExperimentalWearFoundationApi::class)
@Sampled
@Composable
fun RotarySnapSample() {
    val scrollableState = rememberLazyListState()
    val focusRequester = rememberActiveFocusRequester()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .rotary(
                rotaryBehavior = RotaryDefaults.snapBehavior(
                    // This sample has a custom implementation of RotaryScrollableAdapter,
                    // which is required for snapping behavior. ScalingLazyColumn has it built-in,
                    // so it's not required there.
                    remember(scrollableState) {
                        object : RotaryScrollableAdapter {
                            override val scrollableState: ScrollableState = scrollableState

                            override fun averageItemSize(): Float {
                                val items = scrollableState.layoutInfo.visibleItemsInfo
                                return (items.fastSumBy { it.size } / items.size).toFloat()
                            }

                            override fun currentItemIndex(): Int =
                                scrollableState.firstVisibleItemIndex

                            override fun currentItemOffset(): Float =
                                scrollableState.firstVisibleItemScrollOffset.toFloat()

                            override fun totalItemsCount(): Int =
                                scrollableState.layoutInfo.totalItemsCount
                        }
                    }
                ),
                focusRequester = focusRequester
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        state = scrollableState
    ) {
        items(300) {
            BasicText(
                text = "item $it",
                modifier = Modifier
                    .background(Color.Gray)
                    .height(30.dp)
            )
        }
    }
}
