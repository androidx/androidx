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

package androidx.compose.animation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.keyframesWithSpline
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastForEach

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
private fun AnimateBounds_animateOnContentChange() {
    // Example where the change in content triggers the layout change on the item with animateBounds
    val textShort = remember { "Foo ".repeat(10) }
    val textLong = remember { "Bar ".repeat(50) }

    var toggle by remember { mutableStateOf(true) }

    LookaheadScope {
        Box(
            modifier = Modifier.fillMaxSize().clickable { toggle = !toggle },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (toggle) textShort else textLong,
                modifier =
                    Modifier.fillMaxWidth(0.7f)
                        .background(Color.LightGray)
                        .animateBounds(this@LookaheadScope)
                        .padding(10.dp),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
private fun AnimateBounds_withLayoutModifier() {
    // Example showing the difference between providing a Layout Modifier as a parameter of
    // `animateBounds` and chaining the Layout Modifier.

    // We use `padding` in this example, as it provides an immediate change in layout to its child,
    // but not the parent, which sees the same resulting layout. The difference can be seen in the
    // Text (content under padding) and an accompanying Cyan Box (a sibling, under the same Row
    // parent).
    LookaheadScope {
        val boundsTransform = remember {
            BoundsTransform { _, _ ->
                spring(stiffness = 50f, visibilityThreshold = Rect.VisibilityThreshold)
            }
        }

        var toggleAnimation by remember { mutableStateOf(true) }

        Column(Modifier.clickable { toggleAnimation = !toggleAnimation }) {
            Text(
                "See the difference in animation when the Layout Modifier is a parameter of animateBounds. Padding, in this example."
            )
            Spacer(Modifier.height(12.dp))
            Text("Layout Modifier as a parameter.")
            Row(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.animateBounds(
                            lookaheadScope = this@LookaheadScope,
                            modifier =
                                // By providing this Modifier as a parameter of `animateBounds`,
                                // both content and parent see a gradual/animated change in Layout.
                                Modifier.padding(
                                    horizontal = if (toggleAnimation) 10.dp else 50.dp
                                ),
                            boundsTransform = boundsTransform
                        )
                        .background(Color.Red, RoundedCornerShape(12.dp))
                        .height(50.dp)
                ) {
                    Text("Layout Content", Modifier.align(Alignment.Center))
                }
                Box(Modifier.size(50.dp).background(Color.Cyan, RoundedCornerShape(12.dp)))
            }
            Spacer(Modifier.height(12.dp))
            Text("Layout Modifier after AnimateBounds.")
            Row(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.animateBounds(
                            lookaheadScope = this@LookaheadScope,
                            boundsTransform = boundsTransform
                        )
                        // The content is able to animate the change in padding, but since the
                        // parent Layout sees no difference, the change in position is immediate.
                        .padding(horizontal = if (toggleAnimation) 10.dp else 50.dp)
                        .background(Color.Red, RoundedCornerShape(12.dp))
                        .height(50.dp)
                ) {
                    Text("Layout Content", Modifier.align(Alignment.Center))
                }
                Box(Modifier.size(50.dp).background(Color.Cyan, RoundedCornerShape(12.dp)))
            }
            Spacer(Modifier.height(12.dp))
            Text("Layout Modifier before AnimateBounds.")
            Row(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        // The parent is able to see the change in position and the animated size,
                        // so it can smoothly place both its children, but the content of the Box
                        // cannot see the gradual changes so it remains constant.
                        .padding(horizontal = if (toggleAnimation) 10.dp else 50.dp)
                        .animateBounds(
                            lookaheadScope = this@LookaheadScope,
                            boundsTransform = boundsTransform
                        )
                        .background(Color.Red, RoundedCornerShape(12.dp))
                        .height(50.dp)
                ) {
                    Text("Layout Content", Modifier.align(Alignment.Center))
                }
                Box(Modifier.size(50.dp).background(Color.Cyan, RoundedCornerShape(12.dp)))
            }
        }
    }
}

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalSharedTransitionApi::class,
)
@Sampled
@Composable
private fun AnimateBounds_inFlowRowSample() {
    var itemRowCount by remember { mutableIntStateOf(1) }
    val colors = remember { listOf(Color.Cyan, Color.Magenta, Color.Yellow, Color.Green) }

    // A case showing `animateBounds` being used to animate layout changes driven by a parent Layout
    LookaheadScope {
        Column(Modifier.clickable { itemRowCount = if (itemRowCount != 2) 2 else 1 }) {
            Text("Click to toggle animation.")
            FlowRow(
                modifier =
                    Modifier.fillMaxWidth()
                        // Note that the wrap content size changes for FlowRow as the content
                        // adjusts
                        // to one or two lines, we can simply use `animateContentSize()` to make
                        // sure
                        // all items are visible during their animation.
                        .animateContentSize(),
                // Try changing the arrangement as well!
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // We use the maxItems parameter to change the layout of the FlowRow at different
                // states
                maxItemsInEachRow = itemRowCount
            ) {
                colors.fastForEach {
                    Box(
                        Modifier.animateBounds(this@LookaheadScope)
                            // Note the modifier order, we declare the background after
                            // `animateBounds` to make sure it animates with the rest of the content
                            .background(it, RoundedCornerShape(12.dp))
                            .weight(weight = 1f, fill = true)
                            .height(100.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
private fun AnimateBounds_usingKeyframes() {
    var toggle by remember { mutableStateOf(true) }

    // Example using BoundsTransform to calculate an animation using keyframes with splines.
    LookaheadScope {
        Box(Modifier.fillMaxSize().clickable { toggle = !toggle }) {
            Text(
                text = "Hello, World!",
                textAlign = TextAlign.Center,
                modifier =
                    Modifier.align(if (toggle) Alignment.TopStart else Alignment.TopEnd)
                        .animateBounds(
                            lookaheadScope = this@LookaheadScope,
                            boundsTransform = { initialBounds, targetBounds ->
                                // We'll use a keyframe to emphasize the animation in position and
                                // size.
                                keyframesWithSpline {
                                    durationMillis = 1200

                                    // Emphasize with an increase in size
                                    val size = targetBounds.size.times(2f)

                                    // Emphasize the path with a slight curve at the halfway point
                                    val position =
                                        targetBounds.topLeft
                                            .plus(initialBounds.topLeft)
                                            .times(0.5f)
                                            .plus(
                                                Offset(
                                                    // Consider the increase in size (from the
                                                    // center,
                                                    // to keep the Layout aligned at the keyframe)
                                                    x = -(size.width - targetBounds.width) * 0.5f,
                                                    // Emphasize the path with a vertical offset
                                                    y = size.height * 0.5f
                                                )
                                            )

                                    // Only need to define the intermediate keyframe, initial and
                                    // target are implicit.
                                    Rect(position, size).atFraction(0.5f).using(LinearEasing)
                                }
                            }
                        )
                        .background(Color.LightGray, RoundedCornerShape(50))
                        .padding(10.dp)
                        // Text is laid out with the animated fixed Constraints, relax constraints
                        // back to wrap content to be able to center Align vertically.
                        .wrapContentSize(Alignment.Center)
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
private fun AnimateBounds_withMovableContent() {
    // Example showing how to animate a Layout that can be presented on different Layout Composables
    // as the state changes using `movableContent`.
    var position by remember { mutableIntStateOf(-1) }

    val movableContent = remember {
        // To animate a Layout that can be presented in different Composables, we can use
        // `animateBounds` with `movableContent`.
        movableContentWithReceiverOf<LookaheadScope> {
            Box(
                Modifier.animateBounds(
                        lookaheadScope = this@movableContentWithReceiverOf,
                        boundsTransform = { _, _ ->
                            spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessVeryLow,
                                visibilityThreshold = Rect.VisibilityThreshold
                            )
                        }
                    )
                    // Our movableContent can always fill its container in this example.
                    .fillMaxSize()
                    .background(Color.Cyan, RoundedCornerShape(8.dp))
            )
        }
    }

    LookaheadScope {
        Box(Modifier.fillMaxSize()) {
            // Initial container of our Layout, at the center of the screen.
            Box(
                Modifier.size(200.dp)
                    .border(3.dp, Color.Red, RoundedCornerShape(8.dp))
                    .align(Alignment.Center)
                    .clickable { position = -1 }
            ) {
                if (position < 0) {
                    movableContent()
                }
            }

            repeat(4) { index ->
                // Four additional Boxes where our content may be move to.
                Box(
                    Modifier.size(100.dp)
                        .border(2.dp, Color.Blue, RoundedCornerShape(8.dp))
                        .align { size, space, _ ->
                            val horizontal = if (index % 2 == 0) 0.15f else 0.85f
                            val vertical = if (index < 2) 0.15f else 0.85f

                            Offset(
                                    x = (space.width - size.width) * horizontal,
                                    y = (space.height - size.height) * vertical
                                )
                                .round()
                        }
                        .clickable { position = index }
                ) {
                    if (position == index) {
                        // The call to movable content will trigger `Modifier.animateBounds()` to
                        // animate the content's position and size from its previous state.
                        movableContent()
                    }
                }
            }
        }
    }
}
