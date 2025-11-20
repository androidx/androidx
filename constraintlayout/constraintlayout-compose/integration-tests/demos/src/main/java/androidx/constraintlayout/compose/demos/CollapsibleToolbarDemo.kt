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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene

/**
 * A demo of using MotionLayout as a collapsing Toolbar using the DSL to define the MotionScene.
 *
 * This is based on using
 *
 * ```
 * Column(
 *  horizontalAlignment = Alignment.CenterHorizontally,
 *  modifier = Modifier.verticalScroll(scroll)
 * )
 * ```
 *
 * The Column's modifier Modifier.verticalScroll(scroll) will modify scroll.value as it scrolls. We
 * can use this value with a little math to calculate the appropriate progress.
 *
 * When the Column is at the start the MotionLayout sits on top of the Spacer. As the user scrolls
 * up the MotionLayout shrinks with the scrolling Spacer then, stops.
 */
@Suppress("FrequentlyChangingValue")
@Preview(group = "scroll", device = "spec:width=480dp,height=800dp,dpi=440")
@Composable
fun ToolBarDslDemo() {
    val scroll = rememberScrollState(0)
    val big = 250.dp
    val small = 50.dp
    val scene = MotionScene {
        val (title, image, icon) = createRefsFor("title", "image", "icon")

        val start1 = constraintSet {
            constrain(title) {
                bottom.linkTo(image.bottom)
                start.linkTo(image.start)
            }
            constrain(image) {
                width = Dimension.matchParent
                height = Dimension.value(big)
                top.linkTo(parent.top)
                customColor("cover", Color(0xFF000000))
            }
            constrain(icon) {
                top.linkTo(image.top, 16.dp)
                start.linkTo(image.start, 16.dp)
                alpha = 0f
            }
        }
        val end1 = constraintSet {
            constrain(title) {
                bottom.linkTo(image.bottom)
                start.linkTo(icon.end)
                centerVerticallyTo(image)
                scaleX = 0.7f
                scaleY = 0.7f
            }
            constrain(image) {
                width = Dimension.matchParent
                height = Dimension.value(small)
                top.linkTo(parent.top)
                customColor("cover", Color(0xFF0000FF))
            }
            constrain(icon) {
                top.linkTo(image.top, 16.dp)
                start.linkTo(image.start, 16.dp)
            }
        }
        transition(start1, end1, "default") {}
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.verticalScroll(scroll),
    ) {
        Spacer(Modifier.height(big))
        repeat(5) {
            Text(
                text = LoremIpsum(222).values.first(),
                modifier = Modifier.background(Color.White).padding(16.dp),
            )
        }
    }
    val gap = with(LocalDensity.current) { big.toPx() - small.toPx() }
    val progress = minOf(scroll.value / gap, 1f)

    MotionLayout(modifier = Modifier.fillMaxSize(), motionScene = scene, progress = progress) {
        Image(
            modifier = Modifier.layoutId("image").background(customColor("image", "cover")),
            imageVector = Icons.Default.Face,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(Color(0x51FFFFFF)),
        )
        Image(
            modifier = Modifier.layoutId("icon"),
            imageVector = Icons.Default.Menu,
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
        )
        Text(
            modifier = Modifier.layoutId("title"),
            text = "San Francisco",
            fontSize = 30.sp,
            color = Color.White,
        )
    }
}

/**
 * A demo using MotionLayout as a collapsing Toolbar using the DSL to define the MotionScene, where
 * the scrolling of the LazyColumn is obtained with a NestedScrollConnection.
 *
 * ```
 * LazyColumn(
 *   Modifier
 *     .fillMaxWidth()
 *     .nestedScroll(nestedScrollConnection)
 * ) {
 *   items(100) {
 *     Text(text = "item $it", modifier = Modifier.padding(4.dp))
 *   }
 * }
 * ```
 *
 * A NestedScrollConnection object is passed to a LazyColumn Composable via a modifier
 * (Modifier.nestedScroll(nestedScrollConnection)).
 *
 * When the onPreScroll of the NestedScrollConnection is called It returns the amount of "offset" to
 * absorb and uses the offset to collapse the MotionLayout.
 */
@Preview(group = "scroll", device = "spec:width=480dp,height=800dp,dpi=440")
@Composable
fun ToolBarLazyDslDemo() {
    val big = 250.dp
    val small = 50.dp
    val scene = MotionScene {
        val title = createRefFor("title")
        val image = createRefFor("image")
        val icon = createRefFor("icon")

        val start1 = constraintSet {
            constrain(title) {
                bottom.linkTo(image.bottom)
                start.linkTo(image.start)
            }
            constrain(image) {
                width = Dimension.matchParent
                height = Dimension.value(big)
                top.linkTo(parent.top)
                customColor("cover", Color(0xFF000000))
            }
            constrain(icon) {
                top.linkTo(image.top, 16.dp)
                start.linkTo(image.start, 16.dp)
                alpha = 0f
            }
        }

        val end1 = constraintSet {
            constrain(title) {
                bottom.linkTo(image.bottom)
                start.linkTo(icon.end)
                centerVerticallyTo(image)
                scaleX = 0.7f
                scaleY = 0.7f
            }
            constrain(image) {
                width = Dimension.matchParent
                height = Dimension.value(small)
                top.linkTo(parent.top)
                customColor("cover", Color(0xFF0000FF))
            }
            constrain(icon) {
                top.linkTo(image.top, 16.dp)
                start.linkTo(image.start, 16.dp)
            }
        }
        transition(start1, end1, "default") {}
    }

    val maxPx = with(LocalDensity.current) { big.roundToPx().toFloat() }
    val minPx = with(LocalDensity.current) { small.roundToPx().toFloat() }
    val toolbarHeight = remember { mutableFloatStateOf(maxPx) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val height = toolbarHeight.floatValue

                if (height + available.y > maxPx) {
                    toolbarHeight.floatValue = maxPx
                    return Offset(0f, maxPx - height)
                }

                if (height + available.y < minPx) {
                    toolbarHeight.floatValue = minPx
                    return Offset(0f, minPx - height)
                }

                toolbarHeight.floatValue += available.y
                return Offset(0f, available.y)
            }
        }
    }

    val progress = 1 - (toolbarHeight.floatValue - minPx) / (maxPx - minPx)

    Column {
        MotionLayout(modifier = Modifier, motionScene = scene, progress = progress) {
            Image(
                modifier = Modifier.layoutId("image").background(customColor("image", "cover")),
                imageVector = Icons.Default.Face,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(Color(0x51FFFFFF)),
            )
            Image(
                modifier = Modifier.layoutId("icon"),
                imageVector = Icons.Default.Menu,
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
            )
            Text(
                modifier = Modifier.layoutId("title"),
                text = "San Francisco",
                fontSize = 30.sp,
                color = Color.White,
            )
        }
        LazyColumn(Modifier.fillMaxWidth().nestedScroll(nestedScrollConnection)) {
            items(100) { Text(text = "item $it", modifier = Modifier.padding(4.dp)) }
        }
    }
}
