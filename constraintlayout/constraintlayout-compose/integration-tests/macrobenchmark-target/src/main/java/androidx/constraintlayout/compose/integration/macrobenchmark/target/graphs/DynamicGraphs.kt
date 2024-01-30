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

package androidx.constraintlayout.compose.integration.macrobenchmark.target.graphs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import kotlin.random.Random

/**
 * Shows how to use MotionLayout to have animated graphs in a LazyColumn, where each graph is
 * animated as it's revealed.
 *
 * Demonstrates how to dynamically create constraints based on input. See [DynamicGraph]. Where
 * constraints are created to lay out the given values into a single graph layout.
 */
@Preview(group = "scroll", device = "spec:shape=Normal,width=480,height=800,unit=dp,dpi=440")
@Composable
fun DynamicGraphsPreview(modifier: Modifier = Modifier) {
    val graphs = remember {
        val list = mutableListOf<List<Float>>()
        repeat(300) {
            val values = FloatArray(10) { Random.nextInt(100).toFloat() + 10f }.asList()
            list.add(values)
        }
        return@remember list
    }
    LazyColumn(modifier.testTag("LazyColumn")) {
        items(graphs.size) {
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
    val scene = remember {
        val scale = values.map { (it * 0.8f) / max }
        val count = values.size
        val widthPercent = 1 / (count * 2f)

        MotionScene {
            val cols = Array(count) { i -> createRefFor("foo$i") }
            val start1 = constraintSet {
                createHorizontalChain(elements = cols)
                cols.forEach {
                    constrain(it) {
                        width = Dimension.percent(widthPercent)
                        height = Dimension.value(1.dp)
                        bottom.linkTo(parent.bottom, 16.dp)
                    }
                }
            }

            val end1 = constraintSet {
                createHorizontalChain(elements = cols)
                cols.forEachIndexed { i, col ->
                    constrain(col) {
                        width = Dimension.percent(widthPercent)
                        height = Dimension.percent(scale[i])
                        bottom.linkTo(parent.bottom, 16.dp)
                    }
                }
            }
            defaultTransition(start1, end1)
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
        for (i in 0..values.size) {
            Box(
                modifier = Modifier
                    .layoutId("foo$i")
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.hsv(i * 240f / values.size, 0.6f, 0.6f))
            )
        }
    }
}
