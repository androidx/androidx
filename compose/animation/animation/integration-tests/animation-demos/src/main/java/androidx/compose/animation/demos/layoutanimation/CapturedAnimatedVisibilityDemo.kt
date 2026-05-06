/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.animation.demos.layoutanimation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.CapturedAnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.demos.statetransition.InfinitePulsingHeart
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Interactive demo comparing [AnimatedVisibility] and [CapturedAnimatedVisibility] side by side
 * across various enter/exit transitions and layout scenarios.
 */
@Preview
@Composable
fun CapturedAnimatedVisibilityDemo() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(true) }
    var isLargeSize by remember { mutableStateOf(false) }

    val tabs =
        listOf("Slide & Fade", "Expand & Shrink Vertically", "Scale & Fade", "Live Counter State")

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 13.sp) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Button(onClick = { visible = !visible }) {
                Text(if (visible) "Hide Content" else "Show Content")
            }

            Spacer(Modifier.padding(horizontal = 6.dp))

            Button(
                enabled = !visible,
                onClick = {
                    visible = true
                    isLargeSize = !isLargeSize
                },
            ) {
                Text("Show & Change Size")
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            when (selectedTab) {
                0 -> SlideAndFadeDemo(visible, isLargeSize)
                1 -> ExpandShrinkVerticallyDemo(visible, isLargeSize)
                2 -> ScaleAndFadeDemo(visible, isLargeSize)
                3 -> LiveCounterStateDemo(visible, isLargeSize)
            }
        }
    }
}

@Composable
private fun SideBySideLayout(
    leftTitle: String,
    leftContent: @Composable () -> Unit,
    rightTitle: String,
    rightContent: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f).padding(end = 6.dp)) {
            Text(
                leftTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            leftContent()
        }

        Column(Modifier.weight(1f).padding(start = 6.dp)) {
            Text(
                rightTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            rightContent()
        }
    }
}

@Composable
private fun SlideAndFadeDemo(visible: Boolean, isLargeSize: Boolean) {
    val height = if (isLargeSize) 210.dp else 140.dp
    SideBySideLayout(
        leftTitle = "AnimatedVisibility",
        leftContent = {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(tween(1000)) + fadeIn(tween(1000)),
                exit = slideOutVertically(tween(1000)) + fadeOut(tween(1000)),
            ) {
                PulsingHeartCard(color = Color(0xFFD0FFF8), height = height)
            }
        },
        rightTitle = "CapturedAnimatedVisibility",
        rightContent = {
            CapturedAnimatedVisibility(
                visible = visible,
                enter = slideInVertically(tween(1000)) + fadeIn(tween(1000)),
                exit = slideOutVertically(tween(1000)) + fadeOut(tween(1000)),
            ) {
                PulsingHeartCard(color = Color(0xFFFFE9D6), height = height)
            }
        },
    )
}

@Composable
private fun ExpandShrinkVerticallyDemo(visible: Boolean, isLargeSize: Boolean) {
    val itemHeight = if (isLargeSize) 100.dp else 60.dp
    SideBySideLayout(
        leftTitle = "AnimatedVisibility",
        leftContent = {
            Column(Modifier.fillMaxWidth()) {
                ListItemBox("Item 1", Color(0xFFE0E0E0))
                Spacer(Modifier.height(4.dp))
                AnimatedVisibility(
                    visible = visible,
                    enter = expandVertically(tween(1000), expandFrom = Alignment.Top),
                    exit = shrinkVertically(tween(1000), shrinkTowards = Alignment.Top),
                ) {
                    ListItemBox("Item 2 (AV)", Color(0xFFE1F5FE), height = itemHeight)
                }
                Spacer(Modifier.height(4.dp))
                ListItemBox("Item 3", Color(0xFFE0E0E0))
            }
        },
        rightTitle = "CapturedAnimatedVisibility",
        rightContent = {
            Column(Modifier.fillMaxWidth()) {
                ListItemBox("Item 1", Color(0xFFE0E0E0))
                Spacer(Modifier.height(4.dp))
                CapturedAnimatedVisibility(
                    visible = visible,
                    enter = expandVertically(tween(1000), expandFrom = Alignment.Top),
                    exit = shrinkVertically(tween(1000), shrinkTowards = Alignment.Top),
                ) {
                    ListItemBox("Item 2 (CAV)", Color(0xFFFFF3E0), height = itemHeight)
                }
                Spacer(Modifier.height(4.dp))
                ListItemBox("Item 3", Color(0xFFE0E0E0))
            }
        },
    )
}

@Composable
private fun ScaleAndFadeDemo(visible: Boolean, isLargeSize: Boolean) {
    val height = if (isLargeSize) 180.dp else 120.dp
    SideBySideLayout(
        leftTitle = "AnimatedVisibility",
        leftContent = {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(tween(1000)) + fadeIn(tween(1000)),
                exit = scaleOut(tween(1000)) + fadeOut(tween(1000)),
            ) {
                Card(
                    backgroundColor = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth().height(height),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "Scale + Fade\n(Live Composition)",
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
        rightTitle = "CapturedAnimatedVisibility",
        rightContent = {
            CapturedAnimatedVisibility(
                visible = visible,
                enter = scaleIn(tween(1000)) + fadeIn(tween(1000)),
                exit = scaleOut(tween(1000)) + fadeOut(tween(1000)),
            ) {
                Card(
                    backgroundColor = Color(0xFFF3E5F5),
                    modifier = Modifier.fillMaxWidth().height(height),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "Scale + Fade\n(Captured Layer)",
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun LiveCounterStateDemo(visible: Boolean, isLargeSize: Boolean) {
    val height = if (isLargeSize) 180.dp else 120.dp
    SideBySideLayout(
        leftTitle = "AnimatedVisibility",
        leftContent = {
            AnimatedVisibility(
                visible = visible,
                enter = expandIn(tween(1000)) + fadeIn(tween(1000)),
                exit = shrinkOut(tween(1000)) + fadeOut(tween(1000)),
            ) {
                CounterCard(color = Color(0xFFE0F7FA), label = "AV Live Counter", height = height)
            }
        },
        rightTitle = "CapturedAnimatedVisibility",
        rightContent = {
            CapturedAnimatedVisibility(
                visible = visible,
                enter = expandIn(tween(1000)) + fadeIn(tween(1000)),
                exit = shrinkOut(tween(1000)) + fadeOut(tween(1000)),
            ) {
                CounterCard(
                    color = Color(0xFFFCE4EC),
                    label = "CAV Frozen Counter",
                    height = height,
                )
            }
        },
    )
}

@Composable
private fun ListItemBox(text: String, color: Color, height: Dp = 60.dp) {
    Card(backgroundColor = color, modifier = Modifier.fillMaxWidth().height(height)) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.Black, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CounterCard(color: Color, label: String, height: Dp = 120.dp) {
    var count by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            count++
        }
    }

    Box(
        Modifier.fillMaxWidth().height(height).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Count: $count",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PulsingHeartCard(color: Color, height: Dp = 140.dp) {
    Box(
        Modifier.fillMaxWidth().height(height).background(color),
        contentAlignment = Alignment.Center,
    ) {
        InfinitePulsingHeart()
    }
}
