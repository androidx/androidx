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

@file:OptIn(ExperimentalMediaQueryApi::class)

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.MutableStyleState
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.hovered
import androidx.compose.foundation.style.pressed
import androidx.compose.foundation.style.styleable
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiMediaScope.PointerPrecision
import androidx.compose.ui.UiMediaScope.Posture
import androidx.compose.ui.UiMediaScope.ViewingDistance
import androidx.compose.ui.derivedMediaQuery
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.mediaQuery
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import java.util.Locale.getDefault

@Sampled
@Composable
fun MediaQuerySample() {
    Box(Modifier.fillMaxSize().padding(top = 100.dp)) {
        val showDualPane by derivedMediaQuery { windowWidth >= 600.dp }
        val showNavigationRail by derivedMediaQuery {
            windowWidth >= 500.dp && viewingDistance != ViewingDistance.Near
        }

        Column(Modifier.fillMaxSize()) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                if (showNavigationRail) {
                    // Show navigation rail
                    Box(Modifier.width(150.dp).background(Color.Red).fillMaxHeight())
                }

                // Actual content
                Box(Modifier.weight(1f).background(Color.Yellow).fillMaxHeight())

                if (showDualPane) {
                    // Split screen into 2 panes for additional content
                    Box(Modifier.weight(1f).background(Color.Cyan).fillMaxHeight())
                }
            }

            if (!showNavigationRail && !showDualPane) {
                // Show bottom navigation
                Box(Modifier.background(Color.Blue).height(80.dp).fillMaxWidth())
            }
        }
    }
}

@Sampled
@Composable
fun FoldableAwareSample() {
    Column(Modifier.fillMaxSize()) {
        when {
            mediaQuery { windowPosture == Posture.Tabletop } -> {
                // Tabletop mode layout: Two rows separated by hinge
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.weight(1f).background(Color.Red).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Tabletop mode")
                    }
                    Box(
                        Modifier.height(20.dp).background(Color.Black).fillMaxWidth()
                    ) // Hinge visualization
                    Box(Modifier.weight(1f).background(Color.Blue).fillMaxWidth())
                }
            }
            mediaQuery { windowPosture == Posture.Book } -> {
                // Book mode layout: Two columns separated by hinge
                Row(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.weight(1f).background(Color.Red).fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Book mode")
                    }
                    Box(
                        Modifier.width(20.dp).background(Color.Black).fillMaxHeight()
                    ) // Hinge visualization
                    Box(Modifier.weight(1f).background(Color.Blue).fillMaxHeight())
                }
            }
            else -> {
                // Flat mode
                Box(
                    Modifier.background(Color.LightGray).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Flat mode")
                }
            }
        }
    }
}

@Sampled
@Composable
fun AdaptiveButtonSample() {
    Column(Modifier.padding(top = 100.dp).padding(16.dp)) {
        val isTouchPrimary = mediaQuery { pointerPrecision == PointerPrecision.Coarse }
        val isUnreachable = mediaQuery { viewingDistance != ViewingDistance.Near }

        // Adjust button size for touch targets
        val adaptiveSize =
            when {
                isUnreachable -> DpSize(150.dp, 70.dp)
                isTouchPrimary -> DpSize(120.dp, 50.dp)
                else -> DpSize(100.dp, 40.dp)
            }

        // Adjust style for reachability
        val label = "Submit"
        val adaptiveLabel = if (isUnreachable) label.uppercase(getDefault()) else label

        Button(
            modifier =
                Modifier.clip(RoundedCornerShape(8.dp)).size(adaptiveSize).background(Color.Blue),
            onClick = {},
        ) {
            Text(text = adaptiveLabel, color = Color.White, style = TextStyle())
        }
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Sampled
@Composable
fun AdaptiveStylesSample() {
    // Create a styleable clickable box
    @Composable
    fun ClickableStyleableBox(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        style: Style = Style,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val styleState = remember { MutableStyleState(interactionSource) }
        Box(
            modifier =
                modifier
                    .clickable(interactionSource = interactionSource, onClick = onClick)
                    .styleable(styleState, style)
        )
    }

    ClickableStyleableBox(
        onClick = {},
        style = {
            background(Color.Green)

            // Dynamic size based on window Size
            if (mediaQuery { windowWidth > 600.dp && windowHeight > 400.dp }) {
                size(200.dp)
            } else {
                size(150.dp)
            }

            // Hover state for fine pointer input
            if (mediaQuery { pointerPrecision == PointerPrecision.Fine }) {
                hovered { background(Color.Yellow) }
            }
            pressed { background(Color.Red) }
        },
    )
}

@Sampled
@Composable
fun MediaQueryModifierNodeSample() {
    // Example of a custom padding modifier that uses [mediaQuery] within a [Modifier.Node].
    class AdaptivePaddingNode :
        Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val isLargeScreen = mediaQuery { windowWidth > 600.dp && windowHeight > 400.dp }

            // Adjust padding or size based on the query result
            val extraPadding = if (isLargeScreen) 80.dp.roundToPx() else 16.dp.roundToPx()
            val totalPaddingOnAxis = 2 * extraPadding

            // Measure the content with added padding
            val placeable =
                measurable.measure(constraints.offset(-totalPaddingOnAxis, -totalPaddingOnAxis))

            val width = constraints.constrainWidth(placeable.width + totalPaddingOnAxis)
            val height = constraints.constrainHeight(placeable.height + totalPaddingOnAxis)

            return layout(width, height) { placeable.place(extraPadding, extraPadding) }
        }
    }

    class AdaptivePaddingElement : ModifierNodeElement<AdaptivePaddingNode>() {
        override fun create() = AdaptivePaddingNode()

        override fun update(node: AdaptivePaddingNode) {}

        override fun equals(other: Any?) = other === this

        override fun hashCode() = 0
    }

    @Stable fun Modifier.adaptivePadding(): Modifier = this.then(AdaptivePaddingElement())

    Box(Modifier.adaptivePadding().background(Color.Blue).size(400.dp))
}
