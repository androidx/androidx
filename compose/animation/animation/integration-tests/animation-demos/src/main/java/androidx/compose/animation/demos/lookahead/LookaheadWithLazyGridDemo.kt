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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.demos.gesture.pastelColors
import androidx.compose.animation.demos.layoutanimation.summerColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlinx.coroutines.delay

@Preview
@Composable
fun LookaheadWithLazyGridDemo() {
    LookaheadScope {
        var visible by remember { mutableStateOf(false) }
        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(5.dp)) {
            items(100) {
                Box(
                    Modifier.padding(5.dp)
                        .clickable { visible = !visible }
                        .background(summerColors[it % 4], RoundedCornerShape(10.dp))
                        .padding(top = 40.dp, bottom = 40.dp)
                        .fillMaxWidth()
                ) {
                    AnimatedVisibility(visible) { Box(Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Preview
@Composable
fun LookaheadSmallerThanApproach() {
    LookaheadScope {
        LazyVerticalGrid(
            GridCells.Fixed(2),
            Modifier.layout { m, c ->
                val constraints = if (isLookingAhead) c.copy(maxHeight = c.maxHeight - 100) else c
                m.measure(constraints).run { layout(width, height) { place(0, 0) } }
            }
        ) {
            items(20) {
                Text(
                    "item + $it",
                    Modifier.background(summerColors[it % summerColors.size])
                        .height(100.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("PrimitiveInCollection")
@Preview
@Composable
fun ShuffleLazyGridWithItemAnimationAndLookaheadAnimation() {
    var list by remember {
        mutableStateOf<List<Int>>(mutableListOf<Int>().apply { repeat(20) { add(it) } })
    }
    val percent by
        produceState(1f) {
            while (true) {
                delay(300)
                value = Random.nextDouble(from = 0.3, until = 1.0).toFloat()
                delay(300)
                list = list.shuffled()
            }
        }
    LookaheadScope {
        Column {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                Modifier.animateBounds(this@LookaheadScope, Modifier.fillMaxHeight(percent))
                    .border(BorderStroke(2.dp, Color.Blue))
            ) {
                items(list, key = { it }) {
                    Text(
                        "Item $it",
                        Modifier.animateItem()
                            .padding(5.dp)
                            .height(80.dp)
                            .background(pastelColors[it % pastelColors.size])
                    )
                }
            }
        }
    }
}
//
// @SuppressLint("PrimitiveInCollection")
// @Preview
// @Composable
// fun ListAnimateItemSample() {
//    var list by remember {
//        mutableStateOf<List<Int>>(mutableListOf<Int>().apply { repeat(20) { add(it) } })
//    }
//    LookaheadScope {
//        Column {
//            Button(onClick = { list = list + list.size }) { Text("Add new item") }
//            Button(onClick = { list = list.shuffled() }) { Text("Shuffle") }
//            LazyColumn(
//                Modifier
//                    .background(Color.LightGray)
//                    .layout { m, _ ->
//                        m.measure(
//                            constraints = Constraints.fixed(600, if (isLookingAhead) 600 else 800)
//                        )
//                            .run { layout(width, height) { place(0, 0) } }
//                    }
//            ) {
//                items(list, key = { it }) { Text("Item $it", Modifier.animateItem()) }
//            }
//        }
//    }
// }
