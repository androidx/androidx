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

package androidx.constraintlayout.compose.demos

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.system.exitProcess

data class ComposeDemo(val title: String, val content: @Composable () -> Unit)

val AllComposeConstraintLayoutDemos: List<ComposeDemo> =
    listOf(
        ComposeDemo("CustomColorInKeyAttributes") { CustomColorInKeyAttributesDemo() },
        ComposeDemo("Simple OnSwipe") { SimpleOnSwipe() },
        ComposeDemo("Multiple OnSwipe") { MultiSwipeDsl() },
        ComposeDemo("AnimatedChainOrientation") { ChainsAnimatedOrientationDemo() },
        ComposeDemo("AnimatedChainOrientation w/ Modifier DSL") {
            ChainsAnimatedOrientationDemo1()
        },
        ComposeDemo("CollapsibleToolbar w/ Column") { ToolBarDslDemo() },
        ComposeDemo("CollapsibleToolbar w/ LazyColumn") { ToolBarLazyDslDemo() },
        ComposeDemo("MotionLayout in LazyList") { MotionInLazyColumnDslDemo() },
        ComposeDemo("Animated Graphs") { AnimateGraphsOnRevealDemo() },
        ComposeDemo("Animated Reactions Selector") { ReactionSelectorDemo() },
        ComposeDemo("Animated Puzzle Pieces") { AnimatedPuzzlePiecesDemo() },
        ComposeDemo("Simple Staggered") { SimpleStaggeredDemo() }
    )

/** Main screen to explore and interact with all demos from [AllComposeConstraintLayoutDemos]. */
@Preview
@Composable
fun ComposeConstraintLayoutDemos() {
    var displayedDemoIndex by remember { mutableIntStateOf(-1) }
    val maxIndex = AllComposeConstraintLayoutDemos.size - 1
    Column {
        Column {
            when (displayedDemoIndex) {
                -1 -> {
                    // Main Title
                    Text(text = "ComposeConstraintLayoutDemos", style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    // Header with back button
                    val composeDemo = AllComposeConstraintLayoutDemos[displayedDemoIndex]
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(50.dp)
                                .background(Color.White)
                                .graphicsLayer(shadowElevation = 2f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier =
                                Modifier.fillMaxHeight().weight(1f, true).clickable {
                                    displayedDemoIndex = -1
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                            Text(text = composeDemo.title)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = {
                                    displayedDemoIndex =
                                        if (displayedDemoIndex == 0) {
                                            maxIndex
                                        } else {
                                            displayedDemoIndex - 1
                                        }
                                }
                            ) {
                                Text("Prev")
                            }
                            Button(
                                onClick = {
                                    displayedDemoIndex =
                                        if (displayedDemoIndex == maxIndex) {
                                            0
                                        } else {
                                            displayedDemoIndex + 1
                                        }
                                }
                            ) {
                                Text("Next")
                            }
                        }
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (displayedDemoIndex) {
                -1 -> {
                    // Display list of demos
                    AllComposeConstraintLayoutDemos.forEachIndexed { index, composeDemo ->
                        ComposeDemoItem(composeDemo.title) { displayedDemoIndex = index }
                    }
                }
                else -> {
                    // Display selected demo
                    Box(Modifier.fillMaxWidth().weight(1.0f, true)) {
                        AllComposeConstraintLayoutDemos[displayedDemoIndex].content()
                    }
                }
            }
        }
    }

    val activity = LocalActivity.current
    // If there's a demo being displayed, return to demo list, otherwise, exit app
    BackHandler {
        if (displayedDemoIndex >= 0) {
            displayedDemoIndex = -1
        } else {
            activity?.finishAffinity() ?: exitProcess(0)
        }
    }
}

@Composable
private fun ComposeDemoItem(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier =
            modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .clickable(onClick = onClick)
                .padding(start = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = title, modifier = Modifier, fontSize = 16.sp)
    }
}
