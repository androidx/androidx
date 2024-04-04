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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package androidx.compose.animation.demos.sharedelement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.samples.SharedElementInAnimatedContentSample
import androidx.compose.animation.samples.SharedElementWithFABInOverlaySample
import androidx.compose.animation.samples.SharedElementWithMovableContentSample
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun SharedElementDemos() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val list = listOf<Pair<String, @Composable () -> Unit>>(
        "AnimContent\n List To Details" to { ListToDetailsDemo() },
        "Nested" to { NestedSharedElementDemo() },
        "Expanded Card" to { SwitchBetweenCollapsedAndExpanded() },
        "Container Transform" to { ContainerTransformDemo() },
        "Shared Element\n Caller Managed Vis" to { SharedElementWithCallerManagedVisibility() },
        "AnimVis Extension" to { SharedElementScopeWithAnimatedVisibilityScopeDemo() },
        "Shared Tool Bar" to { SharedToolBarDemo() }
    )

    Column {
        ScrollableTabRow(selectedTab) {
            list.forEachIndexed { index, (text, _) ->
                Tab(
                    index == selectedTab,
                    { selectedTab = index },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Text(text)
                }
            }
        }
        list[selectedTab].second.invoke()
    }
}

@Preview
@Composable
fun SharedElementScopeWithAnimatedVisibilityScopeDemo() {
    var selectFirst by remember { mutableStateOf(true) }
    val sharedBoundsKey = remember { Any() }
    SharedTransitionLayout(
        Modifier
            .fillMaxSize()
            .padding(10.dp)
            .clickable {
                selectFirst = !selectFirst
            }
    ) {
        Box {
            val shape =
                RoundedCornerShape(animateDpAsState(if (selectFirst) 10.dp else 30.dp).value)
            AnimatedVisibility(
                selectFirst,
                enter = slideInHorizontally { -it } + fadeIn(),
                exit = slideOutHorizontally { -it } + fadeOut()
            ) {
                Column {
                    Box(
                        Modifier
                            .layout { m, c ->
                                m
                                    .measure(c)
                                    .run {
                                        layout(width, height) { place(0, 0) }
                                    }
                            }
                            .sharedBounds(
                                rememberSharedContentState(sharedBoundsKey),
                                this@AnimatedVisibility,
                                clipInOverlayDuringTransition = OverlayClip(clipShape = shape)
                            )
                            .background(Color.Red)
                            .size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (!selectFirst) "false" else "true", color = Color.White)
                    }
                    Box(
                        Modifier
                            .size(100.dp)
                            .background(Color.Yellow, RoundedCornerShape(10.dp))
                    )
                    Box(
                        Modifier
                            .size(100.dp)
                            .background(Color.Magenta, RoundedCornerShape(10.dp))
                    )
                }
            }
            AnimatedVisibility(
                !selectFirst,
                enter = slideInHorizontally { -it } + fadeIn(),
                exit = slideOutHorizontally { -it } + fadeOut()
            ) {
                Row {
                    Box(
                        Modifier
                            .offset(180.dp, 180.dp)
                            .sharedBounds(
                                rememberSharedContentState(key = sharedBoundsKey),
                                this@AnimatedVisibility,
                                clipInOverlayDuringTransition = OverlayClip(clipShape = shape)
                            )
                            .background(Color.Blue)
                            .size(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (selectFirst) "false" else "true", color = Color.White)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SharedElementWithMovableContent() {
    SharedElementWithMovableContentSample()
}

@Preview
@Composable
fun SharedElementInAnimatedVisibilityWithFABRenderedInOverlay() {
    SharedElementWithFABInOverlaySample()
}

@Preview
@Composable
fun SharedElementInAnimatedContent() {
    SharedElementInAnimatedContentSample()
}
