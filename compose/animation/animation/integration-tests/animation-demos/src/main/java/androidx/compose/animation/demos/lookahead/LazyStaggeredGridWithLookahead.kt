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

package androidx.compose.animation.demos.lookahead

import android.annotation.SuppressLint
import androidx.compose.animation.demos.layoutanimation.turquoiseColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Preview
@Composable
fun LazyStaggeredGridDemo() {
    var enableLookahead by remember { mutableStateOf(true) }
    if (enableLookahead) {
        LookaheadScope { Content(enableLookahead) { enableLookahead = !enableLookahead } }
    } else {
        Content(enableLookahead) { enableLookahead = !enableLookahead }
    }
}

@SuppressLint("PrimitiveInCollection")
@Composable
fun Content(lookaheadEnabled: Boolean, onLookaheadToggled: () -> Unit) {

    val heights = remember { List(100) { (Random.nextInt(100) + 100).dp } }
    val indices = remember { mutableStateOf(List(100) { it }) }
    var count by remember { mutableIntStateOf(100) }

    Column(Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = { indices.value = indices.value.toMutableList().apply { shuffle() } }
            ) {
                Text(text = "shuffle")
            }
            Button(onClick = onLookaheadToggled) {
                Text(if (lookaheadEnabled) "Lookahead enabled" else "Lookahead disabled")
            }
        }

        val state = rememberLazyStaggeredGridState(initialFirstVisibleItemIndex = 29)

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(100.dp),
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = PaddingValues(vertical = 30.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.End,
            verticalItemSpacing = 10.dp,
            content = {
                items(
                    count,
                    span = {
                        if (it % 10 == 0) StaggeredGridItemSpan.FullLine
                        else StaggeredGridItemSpan.SingleLane
                    },
                    key = { it },
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    val index = indices.value[it % indices.value.size]
                    val color = colors[index % colors.size]
                    Box(
                        modifier =
                            Modifier.animateItem()
                                .height(if (!expanded) heights[index] else heights[index] * 2)
                                .background(color, RoundedCornerShape(5.dp))
                                .clickable { expanded = !expanded }
                    ) {
                        Text("$it", modifier = Modifier.align(Alignment.Center), fontSize = 36.sp)
                    }
                }
            },
        )
    }
}

@SuppressLint("PrimitiveInCollection") private val colors = turquoiseColors
