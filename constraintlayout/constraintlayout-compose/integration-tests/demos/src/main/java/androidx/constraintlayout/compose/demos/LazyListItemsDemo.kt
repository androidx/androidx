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

@file:OptIn(ExperimentalMotionApi::class)

package androidx.constraintlayout.compose.demos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import kotlin.random.Random

/**
 * Shows how to use MotionLayout to have animated expandable items in a LazyColumn.
 *
 * Where the MotionScene is defined using the DSL.
 */
@Preview(group = "scroll", device = "spec:shape=Normal,width=480,height=800,unit=dp,dpi=440")
@Composable
fun MotionInLazyColumnDslDemo() {
    val scene = MotionScene {
        val title = createRefFor("title")
        val image = createRefFor("image")
        val icon = createRefFor("icon")

        val start1 = constraintSet {
            constrain(title) {
                centerVerticallyTo(icon)
                start.linkTo(icon.end, 16.dp)
            }
            constrain(image) {
                width = Dimension.value(40.dp)
                height = Dimension.value(40.dp)
                centerVerticallyTo(icon)
                end.linkTo(parent.end, 8.dp)
            }
            constrain(icon) {
                top.linkTo(parent.top, 16.dp)
                bottom.linkTo(parent.bottom, 16.dp)
                start.linkTo(parent.start, 16.dp)
            }
        }

        val end1 = constraintSet {
            constrain(title) {
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                scaleX = 0.7f
                scaleY = 0.7f
            }
            constrain(image) {
                width = Dimension.matchParent
                height = Dimension.value(200.dp)
                centerVerticallyTo(parent)
            }
            constrain(icon) {
                top.linkTo(parent.top, 16.dp)
                start.linkTo(parent.start, 16.dp)
            }
        }
        transition(start1, end1, "default") {}
    }

    val model = remember { BooleanArray(100) }

    LazyColumn {
        items(100) {
            Box(modifier = Modifier.padding(3.dp)) {
                var animateToEnd by remember { mutableStateOf(model[it]) }

                val progress by animateFloatAsState(
                    targetValue = if (animateToEnd) 1f else 0f,
                    animationSpec = tween(700)
                )

                MotionLayout(
                    modifier = Modifier
                        .background(Color(0xFF331B1B))
                        .fillMaxWidth()
                        .padding(1.dp),
                    motionScene = scene,
                    progress = progress
                ) {
                    Image(
                        modifier = Modifier.layoutId("image"),
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                    Image(
                        modifier = Modifier
                            .layoutId("icon")
                            .clickable {
                                animateToEnd = !animateToEnd
                                model[it] = animateToEnd
                            },
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                    Text(
                        modifier = Modifier.layoutId("title"),
                        text = "San Francisco $it",
                        fontSize = 30.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Shows how to use MotionLayout to have animated graphs in a LazyColumn, where each graph is
 * animated as it's revealed.
 *
 * Demonstrates how to dynamically create constraints based on input. See [DynamicGraph]. Where
 * constraints are created to lay out the given values into a single graph layout.
 */
@Preview(group = "scroll", device = "spec:shape=Normal,width=480,height=800,unit=dp,dpi=440")
@Composable
fun AnimateGraphsOnRevealDemo() {
    val graphs = mutableListOf<List<Float>>()
    for (i in 0..100) {
        val values = FloatArray(10) { Random.nextInt(100).toFloat() + 10f }.asList()
        graphs.add(values)
    }
    LazyColumn {
        items(100) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .height(200.dp)
            ) {
                DynamicGraph(graphs[it])
            }
        }
    }
}

@Preview(group = "scroll", device = "spec:shape=Normal,width=480,height=800,unit=dp,dpi=440")
@Composable
private fun DynamicGraph(
    values: List<Float> = listOf<Float>(12f, 32f, 21f, 32f, 2f),
    max: Int = 100
) {
    val scale = values.map { (it * 0.8f) / max }
    val count = values.size
    val widthPercent = 1 / (count * 2f)
    val tmpNames = arrayOfNulls<String>(count)
    for (i in tmpNames.indices) {
        tmpNames[i] = "foo$i"
    }
    val names: List<String> = tmpNames.filterNotNull()
    val scene = MotionScene {
        val cols = names.map { createRefFor(it) }.toTypedArray()
        val start1 = constraintSet {
            createHorizontalChain(elements = cols)
            for (i in names.indices) {
                constrain(cols[i]) {
                    width = Dimension.percent(widthPercent)
                    height = Dimension.value(1.dp)
                    bottom.linkTo(parent.bottom, 16.dp)
                }
            }
        }

        val end1 = constraintSet {
            createHorizontalChain(elements = cols)
            for (i in names.indices) {
                constrain(cols[i]) {
                    width = Dimension.percent(widthPercent)
                    height = Dimension.percent(scale[i])
                    bottom.linkTo(parent.bottom, 16.dp)
                }
            }
        }
        transition(start1, end1, "default") {
        }
    }
    var animateToEnd by remember { mutableStateOf(true) }
    val progress = remember { Animatable(0f) }

    // Animate on reveal
    LaunchedEffect(animateToEnd) {
        progress.animateTo(
            if (animateToEnd) 1f else 0f,
            animationSpec = tween(800)
        )
    }

    MotionLayout(
        modifier = Modifier
            .background(Color(0xFF221010))
            .fillMaxSize()
            .clickable { animateToEnd = !animateToEnd }
            .padding(1.dp),
        motionScene = scene,
        progress = progress.value
    ) {
        for (i in 0..count) {
            Box(
                modifier = Modifier
                    .layoutId("foo$i")
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.hsv(i * 240f / count, 0.6f, 0.6f))
            )
        }
    }
}
